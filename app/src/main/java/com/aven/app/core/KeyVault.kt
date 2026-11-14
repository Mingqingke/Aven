package com.aven.app.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API Key 不写进源码与明文配置。
 * 用 Android Keystore 里的 AES-GCM 密钥加密后，只保存密文与 IV。
 * 需要明确：客户端加密不能阻止运行环境内的密钥提取，只提高静态读取门槛。
 */
object KeyVault {

    private const val ALIAS = "aven_api_key_v1"
    private const val SP_KEY = "api_key_sealed"
    private const val SP_FILE = "aven_secret"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    fun save(context: Context, plain: String): Boolean {
        if (plain.isBlank()) { clear(context); return true }
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val iv = cipher.iv
            val body = cipher.doFinal(plain.trim().toByteArray(Charsets.UTF_8))
            val packed = Base64.encodeToString(iv, Base64.NO_WRAP) + ":" + Base64.encodeToString(body, Base64.NO_WRAP)
            context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit().putString(SP_KEY, packed).apply()
            true
        }.getOrDefault(false)
    }

    fun read(context: Context): String {
        val packed = context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE)
            .getString(SP_KEY, null) ?: return ""
        return runCatching {
            val parts = packed.split(":", limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE, key(),
                GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP))
            )
            String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun has(context: Context): Boolean = read(context).isNotBlank()

    fun clear(context: Context) {
        context.getSharedPreferences(SP_FILE, Context.MODE_PRIVATE).edit().remove(SP_KEY).apply()
    }

    /** 只用于界面展示的掩码，永远不把完整密钥回填到输入框。 */
    fun masked(context: Context): String {
        val k = read(context)
        if (k.isBlank()) return ""
        return if (k.length <= 10) "••••••" else k.take(6) + "••••••" + k.takeLast(4)
    }
}
