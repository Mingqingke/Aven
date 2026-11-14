package com.aven.app.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.coroutines.resume

/**
 * 本地离线 OCR（ML Kit 中文识别，模型随 APK 一起安装，不联网、不上传图片）。
 */
object Ocr {

    private val recognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognize(context: Context, path: String): Result<String> = withContext(Dispatchers.IO) {
        val uri = if (path.startsWith("content://")) Uri.parse(path) else Uri.fromFile(File(path))
        runCatching {
            val image = InputImage.fromFilePath(context, uri)
            suspendCancellableCoroutine { cont ->
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        val sb = StringBuilder()
                        for (block in result.textBlocks) {
                            for (line in block.lines) sb.append(line.text).append('\n')
                        }
                        cont.resume(sb.toString())
                    }
                    .addOnFailureListener { e -> cont.resumeWith(Result.failure(e)) }
            }
        }
    }

    suspend fun recognizeMany(context: Context, paths: List<String>): Pair<String, Int> =
        withContext(Dispatchers.IO) {
            val sb = StringBuilder()
            var ok = 0
            for (p in paths) {
                recognize(context, p).onSuccess {
                    ok += 1
                    sb.append(it).append('\n')
                }
            }
            sb.toString() to ok
        }


    /**
     * 云端识别用的切片 长截图按竖向切块 避免整体缩得太小导致文字不可读
     * 每块宽度不超过 1200 高度不超过 2000 最多 4 块
     */
    suspend fun encodeVisionSlices(path: String): List<String> = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return@runCatching emptyList<String>()
            var sample = 1
            while (bounds.outWidth / sample > 1200) sample *= 2
            val bmp = BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
                ?: return@runCatching emptyList<String>()
            val w = bmp.width
            val h = bmp.height
            val sliceH = 2000
            val slices = mutableListOf<String>()
            var top = 0
            while (top < h && slices.size < 4) {
                val bottom = minOf(h, top + sliceH)
                val part = Bitmap.createBitmap(bmp, 0, top, w, bottom - top)
                val out = ByteArrayOutputStream()
                part.compress(Bitmap.CompressFormat.JPEG, 88, out)
                slices += Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                if (bottom >= h) break
                top = bottom - 100
            }
            bmp.recycle()
            slices
        }.getOrDefault(emptyList())
    }

    /** 上传前压缩：长边 ≤ 1600，JPEG 85，减少流量也减少请求体超限风险。 */
    suspend fun encodeJpeg(path: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            if (bounds.outWidth <= 0) return@runCatching null
            var sample = 1
            var longest = maxOf(bounds.outWidth, bounds.outHeight)
            while (longest / sample > 1600) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(path, opts) ?: return@runCatching null
            val out = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
            bmp.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        }.getOrNull()
    }
}
