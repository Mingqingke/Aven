package com.aven.app.core.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.aven.app.BuildConfig
import com.aven.app.core.Prefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

@Serializable
data class UpdateManifest(
    val packageName: String = "",
    val versionName: String = "",
    val versionCode: Int = 0,
    val releaseDate: String = "",
    val changelog: List<String> = emptyList(),
    val apkSize: Long = 0L,
    val downloadUrls: List<String> = emptyList(),
    val sha256: String = "",
    val signingCertificateSha256: String = "",
    val minSdkVersion: Int = 26,
    val releaseChannel: String = "stable",
    val mandatory: Boolean = false,
)

sealed interface CheckResult {
    data class Available(val manifest: UpdateManifest, val source: String) : CheckResult
    data class UpToDate(val latest: String) : CheckResult
    data class Failed(val message: String) : CheckResult
}

/**
 * 更新引擎 只用真实请求与真实文件
 * 清单优先 静态地址为主 GitHub API 为备用
 */
object UpdateManager {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .build()

    data class SourceInfo(
        val id: String,
        val name: String,
        val configured: Boolean,
        val url: String,
    )

    fun sources(): List<SourceInfo> = listOf(
        SourceInfo("manifest", "官方版本清单", Prefs.updateManifestUrl.value.isNotBlank(), Prefs.updateManifestUrl.value),
        SourceInfo("backup", "备用清单", Prefs.updateBackupUrl.value.isNotBlank(), Prefs.updateBackupUrl.value),
        SourceInfo("github", "GitHub Releases", Prefs.updateGithubRepo.value.isNotBlank(), Prefs.updateGithubRepo.value),
    )

    suspend fun check(context: Context): CheckResult = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        for (src in sources()) {
            if (!src.configured) {
                errors += "${src.name} 未配置"
                continue
            }
            val result = runCatching {
                when (src.id) {
                    "github" -> fetchGithub(context, src.url)
                    else -> fetchStatic(context, src.id, src.url)
                }
            }.getOrElse { CheckResult.Failed(it.message ?: "请求失败") }
            when (result) {
                is CheckResult.Available -> {
                    Prefs.setLastCheckAt(System.currentTimeMillis())
                    Prefs.setLastCheckSummary("${src.name} ${result.manifest.versionName}")
                    return@withContext result.copy(source = src.name)
                }
                is CheckResult.UpToDate -> {
                    Prefs.setLastCheckAt(System.currentTimeMillis())
                    Prefs.setLastCheckSummary("${src.name} 已是最新")
                    return@withContext result
                }
                is CheckResult.Failed -> errors += "${src.name} ${result.message}"
            }
        }
        CheckResult.Failed(if (errors.isEmpty()) "尚未配置更新源" else errors.joinToString("  "))
    }

    private fun fetchStatic(context: Context, id: String, url: String): CheckResult {
        val sp = context.getSharedPreferences("aven_update", Context.MODE_PRIVATE)
        val key = "cache_${id}"
        val etagKey = "etag_${id}"
        val cached = sp.getString(key, null)
        val etag = sp.getString(etagKey, null)
        val builder = Request.Builder().url(url).get()
        if (!etag.isNullOrBlank()) builder.header("If-None-Match", etag)
        client.newCall(builder.build()).execute().use { resp ->
            if (resp.code == 304 && cached != null) return parseManifest(cached)
            if (!resp.isSuccessful) return CheckResult.Failed("HTTP ${resp.code}")
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) return CheckResult.Failed("清单为空")
            resp.header("ETag")?.let { sp.edit().putString(etagKey, it).apply() }
            sp.edit().putString(key, body).apply()
            return parseManifest(body)
        }
    }

    private fun parseManifest(body: String): CheckResult {
        val manifest = runCatching { json.decodeFromString<UpdateManifest>(body) }
            .getOrElse { return CheckResult.Failed("清单格式错误") }
        if (manifest.packageName.isNotBlank() && manifest.packageName != BuildConfig.APPLICATION_ID) {
            return CheckResult.Failed("包名不一致")
        }
        if (manifest.versionCode <= BuildConfig.VERSION_CODE) {
            return CheckResult.UpToDate(manifest.versionName)
        }
        if (manifest.downloadUrls.isEmpty()) return CheckResult.Failed("缺少下载地址")
        return CheckResult.Available(manifest, "版本清单")
    }

    private fun fetchGithub(context: Context, repo: String): CheckResult {
        val url = "https://api.github.com/repos/${repo.trim().trim('/')}/releases/latest"
        val req = Request.Builder().url(url)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Aven-Android")
            .get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return CheckResult.Failed("HTTP ${resp.code}")
            val body = resp.body?.string().orEmpty()
            val o = runCatching { JSONObject(body) }.getOrElse { return CheckResult.Failed("返回格式错误") }
            val tag = o.optString("tag_name").removePrefix("v")
            val notes = o.optString("body")
            val code = Regex("code\\s*=\\s*(\\d+)").find(notes)?.groupValues?.get(1)?.toIntOrNull()
                ?: return CheckResult.Failed("Release 未标注 versionCode")
            if (code <= BuildConfig.VERSION_CODE) return CheckResult.UpToDate(tag)
            val assets = o.optJSONArray("assets")
            val apkUrls = mutableListOf<String>()
            var size = 0L
            if (assets != null) for (i in 0 until assets.length()) {
                val a = assets.optJSONObject(i) ?: continue
                val name = a.optString("name")
                if (name.endsWith(".apk", true)) {
                    apkUrls += a.optString("browser_download_url")
                    size = a.optLong("size", 0L)
                }
            }
            if (apkUrls.isEmpty()) return CheckResult.Failed("Release 没有 APK")
            val sha = Regex("sha256\\s*[=:]\\s*([0-9a-fA-F]{64})").find(notes)?.groupValues?.get(1).orEmpty()
            val manifest = UpdateManifest(
                packageName = BuildConfig.APPLICATION_ID,
                versionName = tag,
                versionCode = code,
                releaseDate = o.optString("published_at").take(10),
                changelog = notes.lines().filter { it.isNotBlank() }.take(6),
                apkSize = size,
                downloadUrls = apkUrls,
                sha256 = sha,
                releaseChannel = "stable",
            )
            return CheckResult.Available(manifest, "GitHub Releases")
        }
    }

    suspend fun download(
        context: Context,
        manifest: UpdateManifest,
        onProgress: (downloaded: Long, total: Long, speed: Long) -> Unit,
    ): Result<File> = withContext(Dispatchers.IO) {
        val dir = File(context.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "aven-${manifest.versionCode}.apk")
        if (file.exists()) file.delete()
        var lastError: Throwable? = null
        for (url in manifest.downloadUrls) {
            if (!url.startsWith("http")) continue
            try {
                val req = Request.Builder().url(url).header("User-Agent", "Aven-Android").get().build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) error("HTTP ${resp.code}")
                    val body = resp.body ?: error("响应为空")
                    val total = if (manifest.apkSize > 0) manifest.apkSize else body.contentLength()
                    body.byteStream().use { input ->
                        file.outputStream().use { out ->
                            val buf = ByteArray(64 * 1024)
                            var read = 0L
                            var lastTime = System.currentTimeMillis()
                            var lastBytes = 0L
                            while (true) {
                                val n = input.read(buf)
                                if (n <= 0) break
                                out.write(buf, 0, n)
                                read += n
                                val now = System.currentTimeMillis()
                                if (now - lastTime >= 250) {
                                    val speed = (read - lastBytes) * 1000 / (now - lastTime)
                                    onProgress(read, total, speed)
                                    lastTime = now
                                    lastBytes = read
                                }
                            }
                            onProgress(read, total, 0)
                        }
                    }
                }
                if (file.length() > 0) return@withContext Result.success(file)
            } catch (t: Throwable) {
                lastError = t
                runCatching { file.delete() }
            }
        }
        Result.failure(lastError ?: IllegalStateException("没有可用下载地址"))
    }

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buf)
                if (n <= 0) break
                md.update(buf, 0, n)
            }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    fun verify(context: Context, file: File, manifest: UpdateManifest): String? {
        if (!file.exists() || file.length() == 0L) return "安装包不存在"
        if (manifest.apkSize > 0 && file.length() != manifest.apkSize) return "安装包大小不一致"
        if (manifest.sha256.isNotBlank()) {
            val actual = sha256(file)
            if (!actual.equals(manifest.sha256.trim(), true)) return "SHA-256 校验失败"
        }
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val archive = runCatching { pm.getPackageArchiveInfo(file.absolutePath, flags) }.getOrNull()
            ?: return "无法读取安装包"
        if (archive.packageName != BuildConfig.APPLICATION_ID) return "包名不一致"
        val code = if (Build.VERSION.SDK_INT >= 28) archive.longVersionCode.toInt() else archive.versionCode
        if (code != manifest.versionCode) return "版本号不一致"
        val archiveSigner = signerSha256(archive)
        if (archiveSigner.isNullOrBlank()) return "无法读取签名"
        val installed = runCatching { pm.getPackageInfo(context.packageName, flags) }.getOrNull()
        val installedSigner = installed?.let { signerSha256(it) }
        if (!installedSigner.isNullOrBlank() && !installedSigner.equals(archiveSigner, true)) return "签名与已安装版本不一致"
        if (manifest.signingCertificateSha256.isNotBlank() &&
            !manifest.signingCertificateSha256.equals(archiveSigner, true)
        ) return "签名与清单不一致"
        return null
    }

    private fun signerSha256(info: android.content.pm.PackageInfo): String? {
        val certs = if (Build.VERSION.SDK_INT >= 28) {
            info.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION") info.signatures
        } ?: return null
        val cert = certs.firstOrNull() ?: return null
        return MessageDigest.getInstance("SHA-256").digest(cert.toByteArray()).joinToString("") { "%02x".format(it) }
    }

    fun installedSignerSha256(context: Context): String? {
        val pm = context.packageManager
        val flags = if (Build.VERSION.SDK_INT >= 28) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = runCatching { pm.getPackageInfo(context.packageName, flags) }.getOrNull() ?: return null
        return signerSha256(info)
    }

    /** 交给系统安装器 不绕过任何授权 */
    fun install(context: Context, file: File): String? {
        if (Build.VERSION.SDK_INT >= 26 && !context.packageManager.canRequestPackageInstalls()) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            return "需要允许安装未知来源应用"
        }
        return runCatching {
            val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            null
        }.getOrElse { "无法启动安装器 ${it.message}" }
    }
}

/** 更新流程的全局状态 离开页面也不中断 */
object UpdateState {
    enum class Phase { IDLE, CHECKING, DOWNLOADING, VERIFYING, READY, FAILED }

    val phase = MutableStateFlow(Phase.IDLE)
    val manifest = MutableStateFlow<UpdateManifest?>(null)
    val sourceName = MutableStateFlow("")
    val downloaded = MutableStateFlow(0L)
    val total = MutableStateFlow(0L)
    val speed = MutableStateFlow(0L)
    val message = MutableStateFlow("")
    val file = MutableStateFlow<File?>(null)
    val showDialog = MutableStateFlow(false)

    fun reset() {
        phase.value = Phase.IDLE
        downloaded.value = 0L
        total.value = 0L
        speed.value = 0L
        message.value = ""
        file.value = null
    }
}

/** 更新流程执行器 使用独立协程 离开页面也不中断 */
object UpdateRunner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun check(context: Context) {
        if (UpdateState.phase.value == UpdateState.Phase.CHECKING) return
        scope.launch {
            UpdateState.phase.value = UpdateState.Phase.CHECKING
            UpdateState.message.value = "正在检查更新"
            when (val r = UpdateManager.check(context)) {
                is CheckResult.Available -> {
                    UpdateState.manifest.value = r.manifest
                    UpdateState.sourceName.value = r.source
                    UpdateState.phase.value = UpdateState.Phase.IDLE
                    UpdateState.message.value = "发现新版本 ${r.manifest.versionName}"
                    if (r.manifest.versionCode != Prefs.ignoredVersionCode.value) {
                        UpdateState.showDialog.value = true
                    }
                }
                is CheckResult.UpToDate -> {
                    UpdateState.phase.value = UpdateState.Phase.IDLE
                    UpdateState.message.value = "已是最新版本 ${r.latest}"
                }
                is CheckResult.Failed -> {
                    UpdateState.phase.value = UpdateState.Phase.IDLE
                    UpdateState.message.value = r.message
                }
            }
        }
    }

    fun startDownload(context: Context, force: Boolean = false) {
        val manifest = UpdateState.manifest.value ?: return
        val phase = UpdateState.phase.value
        if (!force && (phase == UpdateState.Phase.DOWNLOADING ||
                phase == UpdateState.Phase.VERIFYING ||
                phase == UpdateState.Phase.READY)
        ) return
        scope.launch {
            UpdateState.phase.value = UpdateState.Phase.DOWNLOADING
            UpdateState.downloaded.value = 0L
            UpdateState.total.value = manifest.apkSize
            UpdateState.speed.value = 0L
            UpdateState.message.value = "正在下载"
            val result = UpdateManager.download(context, manifest) { done, total, speed ->
                UpdateState.downloaded.value = done
                UpdateState.total.value = total
                UpdateState.speed.value = speed
            }
            result.onSuccess { file ->
                UpdateState.phase.value = UpdateState.Phase.VERIFYING
                UpdateState.message.value = "正在校验安装包"
                val error = withContext(Dispatchers.IO) { UpdateManager.verify(context, file, manifest) }
                if (error == null) {
                    UpdateState.file.value = file
                    UpdateState.phase.value = UpdateState.Phase.READY
                    UpdateState.message.value = "校验通过 可以安装"
                } else {
                    UpdateState.phase.value = UpdateState.Phase.FAILED
                    UpdateState.message.value = error
                }
            }.onFailure {
                UpdateState.phase.value = UpdateState.Phase.FAILED
                UpdateState.message.value = it.message ?: "下载失败"
            }
        }
    }

    fun install(context: Context) {
        val file = UpdateState.file.value ?: return
        val error = UpdateManager.install(context, file)
        if (error != null) {
            UpdateState.message.value = error
            UpdateState.phase.value = UpdateState.Phase.FAILED
        }
    }
}
