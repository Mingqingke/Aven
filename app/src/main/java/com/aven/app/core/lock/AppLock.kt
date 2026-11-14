package com.aven.app.core.lock

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import androidx.biometric.BiometricManager
import kotlinx.coroutines.flow.MutableStateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** 应用锁配置 密码只保存加盐 PBKDF2 结果 */
object LockPrefs {
    private const val FILE = "aven_lock"
    private const val ITERATIONS = 60_000

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun enabled(ctx: Context) = sp(ctx).getBoolean("enabled", false)
    fun setEnabled(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("enabled", v).apply()

    fun biometric(ctx: Context) = sp(ctx).getBoolean("biometric", false)
    fun setBiometric(ctx: Context, v: Boolean) = sp(ctx).edit().putBoolean("biometric", v).apply()

    fun timeoutSec(ctx: Context) = sp(ctx).getInt("timeout", 300)
    fun setTimeoutSec(ctx: Context, v: Int) = sp(ctx).edit().putInt("timeout", v).apply()

    fun hasPassword(ctx: Context) = sp(ctx).getString("hash", null) != null

    fun setPassword(ctx: Context, password: String): Boolean = runCatching {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = pbkdf2(password, salt)
        sp(ctx).edit()
            .putString("salt", Base64.getEncoder().encodeToString(salt))
            .putString("hash", Base64.getEncoder().encodeToString(hash))
            .apply()
        true
    }.getOrDefault(false)

    fun verify(ctx: Context, password: String): Boolean = runCatching {
        val salt = Base64.getDecoder().decode(sp(ctx).getString("salt", null) ?: return false)
        val expected = Base64.getDecoder().decode(sp(ctx).getString("hash", null) ?: return false)
        MessageDigest.isEqual(pbkdf2(password, salt), expected)
    }.getOrDefault(false)

    private fun pbkdf2(password: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun canBiometric(ctx: Context): Boolean =
        BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
}

/**
 * 锁定状态
 * 进程被杀后重新进入会重新锁定 配置变更与短暂切后台不误锁
 */
object AppLock {
    val locked = MutableStateFlow(false)

    private var started = 0
    private var backgroundAt = 0L
    private var fails = 0
    private var blockedUntil = 0L

    fun init(context: Context) {
        locked.value = LockPrefs.enabled(context)
    }

    fun attach(app: Application) {
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                if (started == 0) {
                    val enabled = LockPrefs.enabled(activity)
                    if (enabled) {
                        val timeout = LockPrefs.timeoutSec(activity) * 1000L
                        if (backgroundAt == 0L || System.currentTimeMillis() - backgroundAt >= timeout) {
                            locked.value = true
                        }
                    }
                }
                started++
            }

            override fun onActivityStopped(activity: Activity) {
                if (activity.isChangingConfigurations) return
                started = (started - 1).coerceAtLeast(0)
                if (started == 0) backgroundAt = System.currentTimeMillis()
            }

            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
            override fun onActivityResumed(activity: Activity) {}
            override fun onActivityPaused(activity: Activity) {}
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
            override fun onActivityDestroyed(activity: Activity) {}
        })
    }

    fun unlock() {
        locked.value = false
        backgroundAt = 0L
        fails = 0
        blockedUntil = 0L
    }

    sealed interface VerifyResult {
        data object Ok : VerifyResult
        data class Wrong(val remaining: Int) : VerifyResult
        data class Blocked(val seconds: Int) : VerifyResult
    }

    fun verifyPassword(context: Context, password: String): VerifyResult {
        val now = System.currentTimeMillis()
        if (blockedUntil > now) return VerifyResult.Blocked(((blockedUntil - now) / 1000).toInt() + 1)
        return if (LockPrefs.verify(context, password)) {
            fails = 0
            VerifyResult.Ok
        } else {
            fails++
            if (fails >= 5) {
                blockedUntil = now + 30_000
                fails = 0
                VerifyResult.Blocked(30)
            } else VerifyResult.Wrong(5 - fails)
        }
    }
}
