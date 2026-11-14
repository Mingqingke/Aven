package com.aven.app.core.ai

import android.content.Context
import com.aven.app.core.KeyVault
import com.aven.app.core.Prefs
import com.aven.app.core.ocr.Ocr
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * DeepSeek 官方接口封装（OpenAI 兼容格式）。
 * 目前官方模型：deepseek-flash（支持图片输入）、deepseek-v4-pro（仅文本）。
 * 所有失败都转成明确中文提示，绝不伪造返回值。
 */
object Ai {

    sealed interface Out {
        data class Ok(val text: String) : Out
        data class Err(val message: String, val code: Int? = null) : Out
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .callTimeout(240, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun base(context: Context): String =
        Prefs.apiBase.value.trim().trimEnd('/').ifBlank { "https://api.deepseek.com" }

    fun hasKey(context: Context): Boolean = KeyVault.has(context)

    fun supportsVision(model: String): Boolean = model.trim().startsWith("deepseek-flash")

    fun errorText(code: Int, body: String): String = when (code) {
        400 -> "请求格式被拒绝"
        401 -> "API Key 无效或已失效"
        402 -> "账户余额不足 请先充值"
        403 -> "该密钥没有访问权限"
        404 -> "接口地址不存在"
        422 -> "参数不被接受"
        429 -> "请求过于频繁"
        500, 502, 503, 504 -> "服务暂时不可用 HTTP $code"
        else -> "请求失败 HTTP $code " + if (body.isNotBlank()) body.take(120) else ""
    }

    suspend fun listModels(context: Context): Out = withContext(Dispatchers.IO) {
        val key = KeyVault.read(context)
        if (key.isBlank()) return@withContext Out.Err("还没有填写 API Key")
        val req = Request.Builder()
            .url("${base(context)}/models")
            .header("Authorization", "Bearer $key")
            .get()
            .build()
        runCatching {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use Out.Err(errorText(resp.code, body), resp.code)
                val arr = JSONObject(body).optJSONArray("data") ?: JSONArray()
                val names = (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("id") }
                Out.Ok(if (names.isEmpty()) "连接成功" else names.joinToString(" "))
            }
        }.getOrElse { Out.Err(networkText(it)) }
    }

    suspend fun chat(
        context: Context,
        messages: List<Pair<String, String>>,
        jsonMode: Boolean = false,
        modelOverride: String? = null,
        maxTokens: Int = 3000,
        temperature: Double = 0.5,
    ): Out = withContext(Dispatchers.IO) {
        val key = KeyVault.read(context)
        if (key.isBlank()) return@withContext Out.Err("还没有填写 API Key")
        val model = (modelOverride ?: Prefs.aiModel.value).ifBlank { "deepseek-flash" }
        val arr = JSONArray()
        messages.forEach { (role, content) ->
            arr.put(JSONObject().put("role", role).put("content", content))
        }
        val bodyJson = JSONObject()
            .put("model", model)
            .put("messages", arr)
            .put("temperature", temperature)
            .put("max_tokens", maxTokens)
            .put("stream", false)
        if (jsonMode) bodyJson.put("response_format", JSONObject().put("type", "json_object"))
        post(context, key, bodyJson, model)
    }

    suspend fun vision(
        context: Context,
        images: List<String>,
        prompt: String,
        modelOverride: String? = null,
    ): Out = withContext(Dispatchers.IO) {
        val key = KeyVault.read(context)
        if (key.isBlank()) return@withContext Out.Err("还没有填写 API Key")
        val model = (modelOverride ?: Prefs.aiModel.value).ifBlank { "deepseek-flash" }
        if (!supportsVision(model)) {
            return@withContext Out.Err("当前模型不支持图片输入 请切换到 deepseek-flash")
        }
        val content = JSONArray()
        content.put(JSONObject().put("type", "text").put("text", prompt))
        var attached = 0
        for (path in images.take(3)) {
            for (slice in Ocr.encodeVisionSlices(path).take(4)) {
                if (attached >= 6) break
                content.put(
                    JSONObject().put("type", "image_url").put(
                        "image_url",
                        JSONObject().put("url", "data:image/jpeg;base64,$slice")
                    )
                )
                attached += 1
            }
        }
        if (attached == 0) return@withContext Out.Err("图片读取失败 可重新选择")
        val messages = JSONArray().put(JSONObject().put("role", "user").put("content", content))
        val bodyJson = JSONObject()
            .put("model", model)
            .put("messages", messages)
            .put("temperature", 0.0)
            .put("max_tokens", 4096)
            .put("stream", false)
        post(context, key, bodyJson, model)
    }

    private fun post(context: Context, key: String, bodyJson: JSONObject, model: String): Out {
        val req = Request.Builder()
            .url("${base(context)}/chat/completions")
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(bodyJson.toString().toRequestBody(JSON))
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) return@use Out.Err(errorText(resp.code, body), resp.code)
                val message = JSONObject(body)
                    .optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")
                var text = message?.optString("content").orEmpty()
                if (text.isBlank()) text = message?.optString("reasoning_content").orEmpty()
                if (text.isBlank()) Out.Err("模型没有返回内容 可重试") else Out.Ok(text)
            }
        }.getOrElse { Out.Err(networkText(it)) }
    }

    private fun networkText(t: Throwable): String = when (t) {
        is SocketTimeoutException -> "请求超时 请检查网络"
        is UnknownHostException -> "无法解析服务器地址"
        is ConnectException -> "连接失败 请检查网络"
        else -> "网络异常 ${t.message ?: t.javaClass.simpleName}"
    }
}
