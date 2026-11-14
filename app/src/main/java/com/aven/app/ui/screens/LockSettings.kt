package com.aven.app.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.aven.app.core.lock.AppLock
import com.aven.app.core.lock.LockPrefs
import com.aven.app.ui.components.BoxCard
import com.aven.app.ui.components.Divider
import com.aven.app.ui.components.LabeledInput
import com.aven.app.ui.components.PrimaryButton
import com.aven.app.ui.components.ScreenTitle
import com.aven.app.ui.components.SectionLabel
import com.aven.app.ui.components.SoftButton
import com.aven.app.ui.components.ToggleRow
import com.aven.app.ui.components.ValueRow
import com.aven.app.ui.theme.LocalAven
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog

@Composable
fun LockSettingsScreen(onBack: () -> Unit) {
    val c = LocalAven.current
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(LockPrefs.enabled(context)) }
    var biometric by remember { mutableStateOf(LockPrefs.biometric(context)) }
    var timeout by remember { mutableStateOf(LockPrefs.timeoutSec(context)) }
    var dialog by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var askEnroll by remember { mutableStateOf(false) }

    fun refresh() {
        enabled = LockPrefs.enabled(context)
        biometric = LockPrefs.biometric(context)
        timeout = LockPrefs.timeoutSec(context)
    }

    fun enableBiometric() {
        val activity = context as? FragmentActivity ?: return
        if (!LockPrefs.canBiometric(context)) {
            askEnroll = true
            return
        }
        val prompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    LockPrefs.setBiometric(context, true)
                    refresh()
                    message = "面容解锁已开启"
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    message = "已取消"
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("面容识别")
            .setSubtitle("确认开启面容解锁")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setNegativeButtonText("取消")
            .build()
        runCatching { prompt.authenticate(info) }.onFailure { message = "无法启动系统认证" }
    }

    Column(Modifier.fillMaxWidth()) {
        ScreenTitle("应用锁", "保护成绩与报告", onBack = onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 40.dp),
        ) {
            item {
                BoxCard {
                    ToggleRow("应用锁", "打开应用时验证身份", enabled) { on ->
                        if (on) {
                            if (LockPrefs.hasPassword(context)) {
                                LockPrefs.setEnabled(context, true)
                                refresh()
                                message = "应用锁已开启"
                            } else dialog = "set"
                        } else {
                            dialog = "disable"
                        }
                    }
                    Divider()
                    ToggleRow("面容解锁", if (enabled) "使用系统面容快速解锁" else "需先开启应用锁", biometric && enabled) { on ->
                        if (on) {
                            if (!enabled) message = "请先开启应用锁" else enableBiometric()
                        } else {
                            LockPrefs.setBiometric(context, false)
                            refresh()
                        }
                    }
                }
            }

            item { SectionLabel("自动锁定", accent = c.blue) }
            item {
                BoxCard {
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            listOf(
                                "立即" to 0,
                                "30 秒" to 30,
                                "1 分钟" to 60,
                                "5 分钟" to 300,
                                "15 分钟" to 900,
                            )
                        ) { (label, value) ->
                            Chip(label, timeout == value) {
                                LockPrefs.setTimeoutSec(context, value)
                                refresh()
                            }
                        }
                    }
                    Text("离开应用超过该时间后需要重新验证", fontSize = 9.5.sp, color = c.secondary)
                }
            }

            item { SectionLabel("密码", accent = c.violet) }
            item {
                BoxCard(insidePadding = 0.dp) {
                    ValueRow(
                        "修改密码",
                        if (LockPrefs.hasPassword(context)) "已设置" else "未设置",
                        onClick = { dialog = if (LockPrefs.hasPassword(context)) "change" else "set" },
                    )
                    Divider()
                    ValueRow(
                        "关闭应用锁",
                        if (enabled) "需要验证" else "已关闭",
                        onClick = { if (enabled) dialog = "disable" else message = "应用锁未开启" },
                    )
                }
            }
            if (message.isNotBlank()) {
                item {
                    Spacer(Modifier.height(10.dp))
                    Text(message, fontSize = 10.5.sp, color = c.secondary)
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    "密码只保存加盐散列 面容认证由系统完成",
                    fontSize = 9.5.sp, color = c.secondary, lineHeight = 14.sp,
                )
            }
        }
    }

    when (dialog) {
        "set" -> PasswordDialog(mode = "set", onClose = { dialog = "" }) { old, new ->
            when {
                new.length != 6 -> "密码需为 6 位数字"
                old.isNotBlank() && !LockPrefs.verify(context, old) -> "旧密码不正确"
                !LockPrefs.setPassword(context, new) -> "保存失败"
                else -> {
                    LockPrefs.setEnabled(context, true)
                    refresh()
                    dialog = ""
                    message = "密码已保存 应用锁已开启"
                    null
                }
            }
        }
        "change" -> PasswordDialog(mode = "change", onClose = { dialog = "" }) { old, new ->
            when {
                old.length != 6 || !LockPrefs.verify(context, old) -> "旧密码不正确"
                new.length != 6 -> "新密码需为 6 位数字"
                !LockPrefs.setPassword(context, new) -> "保存失败"
                else -> {
                    dialog = ""
                    message = "密码已更新"
                    null
                }
            }
        }
        "disable" -> PasswordDialog(mode = "disable", onClose = { dialog = "" }) { old, _ ->
            if (!LockPrefs.verify(context, old)) "密码不正确"
            else {
                LockPrefs.setEnabled(context, false)
                LockPrefs.setBiometric(context, false)
                AppLock.unlock()
                refresh()
                dialog = ""
                message = "应用锁已关闭"
                null
            }
        }
    }

    if (askEnroll) {
        OverlayDialog(
            show = true,
            title = "未录入面容",
            summary = "请先在系统设置中录入面容或指纹",
            onDismissRequest = { askEnroll = false },
        ) {
            Column {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SoftButton("稍后", { askEnroll = false }, modifier = Modifier.weight(1f))
                    PrimaryButton("前往设置", {
                        askEnroll = false
                        runCatching {
                            val intent = if (Build.VERSION.SDK_INT >= 30) {
                                Intent(Settings.ACTION_BIOMETRIC_ENROLL)
                            } else {
                                Intent(Settings.ACTION_SECURITY_SETTINGS)
                            }
                            context.startActivity(intent)
                        }
                    }, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    mode: String,
    onClose: () -> Unit,
    onSave: (old: String, new: String) -> String?,
) {
    val c = LocalAven.current
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    OverlayDialog(
        show = true,
        title = when (mode) {
            "disable" -> "关闭应用锁"
            "change" -> "修改密码"
            else -> "设置密码"
        },
        summary = "6 位数字 用于解锁 Aven",
        onDismissRequest = onClose,
    ) {
        Column {
            if (mode != "set") {
                LabeledInput("当前密码", old, { old = it.filter { ch -> ch.isDigit() }.take(6) }, numeric = true)
                Spacer(Modifier.height(10.dp))
            }
            if (mode != "disable") {
                LabeledInput("新密码", new, { new = it.filter { ch -> ch.isDigit() }.take(6) }, numeric = true)
                Spacer(Modifier.height(10.dp))
                LabeledInput("确认密码", confirm, { confirm = it.filter { ch -> ch.isDigit() }.take(6) }, numeric = true)
            }
            if (error.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 10.5.sp, color = c.negative)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftButton("取消", onClose, modifier = Modifier.weight(1f))
                PrimaryButton("保存", {
                    if (mode != "disable" && new != confirm) {
                        error = "两次输入不一致"
                    } else {
                        val result = onSave(old, new)
                        if (result != null) error = result
                    }
                }, modifier = Modifier.weight(1f))
            }
        }
    }
}
