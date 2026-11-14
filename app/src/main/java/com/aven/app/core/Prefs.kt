package com.aven.app.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * 设置存储。键只增不删，升级不重置用户选择。
 * 所有设置都是真实的本地状态，界面开关直接写这里。
 */
object Prefs {

    private const val FILE = "aven_prefs"
    private lateinit var sp: SharedPreferences

    /** 个人与学业档案 */
    val nickname = MutableStateFlow("同学")
    val stage = MutableStateFlow(1)          // 0 初中 / 1 高中
    val grade = MutableStateFlow("高二")
    val term = MutableStateFlow("上学期")
    val province = MutableStateFlow("")
    val city = MutableStateFlow("")
    val school = MutableStateFlow("")
    val electives = MutableStateFlow("")     // 选科，逗号分隔
    val gaokaoDate = MutableStateFlow(0L)    // 高考日期，0=按默认推算

    /** 考试设置 */
    val defaultExamType = MutableStateFlow("月考")
    val rankReference = MutableStateFlow(0)  // 0 年级 / 1 班级 / 2 全省
    val compareRule = MutableStateFlow(0)    // 0 同类优先 / 1 最近两次 / 2 综合
    val compareBand = MutableStateFlow(2.0f) // 中性波动区间，百分点
    val convertedEnabled = MutableStateFlow(true)

    /** 外观 */
    val themeMode = MutableStateFlow(0)      // 0 跟随系统 / 1 浅色 / 2 深色
    val reduceMotion = MutableStateFlow(false)
    val cardA = MutableStateFlow("latest")   // 右侧上卡
    val cardB = MutableStateFlow("gap")      // 右侧下卡

    /** AI */
    val apiBase = MutableStateFlow("https://api.deepseek.com")
    val aiModel = MutableStateFlow("deepseek-flash")
    val aiConsent = MutableStateFlow(false)  // 允许发送学业数据
    val visionConsent = MutableStateFlow(false) // 允许上传成绩单图片

    val onboarded = MutableStateFlow(false)

    /** 更新 */
    val updateManifestUrl = MutableStateFlow(Project.MANIFEST_URL)
    val updateBackupUrl = MutableStateFlow(Project.BACKUP_MANIFEST_URL)
    val updateGithubRepo = MutableStateFlow(Project.OWNER_REPO)
    val autoUpdate = MutableStateFlow(true)
    val ignoredVersionCode = MutableStateFlow(0)
    val lastCheckAt = MutableStateFlow(0L)
    val lastCheckSummary = MutableStateFlow("")

    fun init(context: Context) {
        sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        nickname.value = sp.getString("nickname", "同学") ?: "同学"
        stage.value = sp.getInt("stage", 1)
        grade.value = sp.getString("grade", if (stage.value == 1) "高二" else "初三") ?: "高二"
        term.value = sp.getString("term", "上学期") ?: "上学期"
        province.value = sp.getString("province", "") ?: ""
        city.value = sp.getString("city", "") ?: ""
        school.value = sp.getString("school", "") ?: ""
        electives.value = sp.getString("electives", "") ?: ""
        gaokaoDate.value = sp.getLong("gaokaoDate", 0L)
        defaultExamType.value = sp.getString("defaultExamType", "月考") ?: "月考"
        rankReference.value = sp.getInt("rankReference", 0)
        compareRule.value = sp.getInt("compareRule", 0)
        compareBand.value = sp.getFloat("compareBand", 2.0f)
        convertedEnabled.value = sp.getBoolean("convertedEnabled", true)
        themeMode.value = sp.getInt("themeMode", 0)
        reduceMotion.value = sp.getBoolean("reduceMotion", false)
        cardA.value = sp.getString("cardA", "latest") ?: "latest"
        cardB.value = sp.getString("cardB", "gap") ?: "gap"
        apiBase.value = sp.getString("apiBase", "https://api.deepseek.com") ?: "https://api.deepseek.com"
        aiModel.value = sp.getString("aiModel", "deepseek-flash") ?: "deepseek-flash"
        aiConsent.value = sp.getBoolean("aiConsent", false)
        visionConsent.value = sp.getBoolean("visionConsent", false)
        onboarded.value = sp.getBoolean("onboarded", false)
        updateManifestUrl.value = sp.getString("updateManifestUrl", Project.MANIFEST_URL) ?: Project.MANIFEST_URL
        updateBackupUrl.value = sp.getString("updateBackupUrl", Project.BACKUP_MANIFEST_URL) ?: Project.BACKUP_MANIFEST_URL
        updateGithubRepo.value = sp.getString("updateGithubRepo", Project.OWNER_REPO) ?: Project.OWNER_REPO
        autoUpdate.value = sp.getBoolean("autoUpdate", true)
        ignoredVersionCode.value = sp.getInt("ignoredVersionCode", 0)
        lastCheckAt.value = sp.getLong("lastCheckAt", 0L)
        lastCheckSummary.value = sp.getString("lastCheckSummary", "") ?: ""
    }

    fun setNickname(v: String) = putString("nickname", v, nickname)
    fun setStage(v: Int) = putInt("stage", v, stage)
    fun setGrade(v: String) = putString("grade", v, grade)
    fun setTerm(v: String) = putString("term", v, term)
    fun setProvince(v: String) = putString("province", v, province)
    fun setCity(v: String) = putString("city", v, city)
    fun setSchool(v: String) = putString("school", v, school)
    fun setElectives(v: String) = putString("electives", v, electives)
    fun setGaokaoDate(v: Long) = putLong("gaokaoDate", v, gaokaoDate)
    fun setDefaultExamType(v: String) = putString("defaultExamType", v, defaultExamType)
    fun setRankReference(v: Int) = putInt("rankReference", v, rankReference)
    fun setCompareRule(v: Int) = putInt("compareRule", v, compareRule)
    fun setCompareBand(v: Float) = putFloat("compareBand", v, compareBand)
    fun setConvertedEnabled(v: Boolean) = putBool("convertedEnabled", v, convertedEnabled)
    fun setThemeMode(v: Int) = putInt("themeMode", v, themeMode)
    fun setReduceMotion(v: Boolean) = putBool("reduceMotion", v, reduceMotion)
    fun setCardA(v: String) = putString("cardA", v, cardA)
    fun setCardB(v: String) = putString("cardB", v, cardB)
    fun setApiBase(v: String) = putString("apiBase", v, apiBase)
    fun setAiModel(v: String) = putString("aiModel", v, aiModel)
    fun setAiConsent(v: Boolean) = putBool("aiConsent", v, aiConsent)
    fun setVisionConsent(v: Boolean) = putBool("visionConsent", v, visionConsent)
    fun setOnboarded(v: Boolean) = putBool("onboarded", v, onboarded)
    fun setUpdateManifestUrl(v: String) = putString("updateManifestUrl", v, updateManifestUrl)
    fun setUpdateBackupUrl(v: String) = putString("updateBackupUrl", v, updateBackupUrl)
    fun setUpdateGithubRepo(v: String) = putString("updateGithubRepo", v, updateGithubRepo)
    fun setAutoUpdate(v: Boolean) = putBool("autoUpdate", v, autoUpdate)
    fun setIgnoredVersionCode(v: Int) = putInt("ignoredVersionCode", v, ignoredVersionCode)
    fun setLastCheckAt(v: Long) = putLong("lastCheckAt", v, lastCheckAt)
    fun setLastCheckSummary(v: String) = putString("lastCheckSummary", v, lastCheckSummary)

    private fun putString(k: String, v: String, f: MutableStateFlow<String>) {
        f.value = v; sp.edit().putString(k, v).apply()
    }
    private fun putInt(k: String, v: Int, f: MutableStateFlow<Int>) {
        f.value = v; sp.edit().putInt(k, v).apply()
    }
    private fun putLong(k: String, v: Long, f: MutableStateFlow<Long>) {
        f.value = v; sp.edit().putLong(k, v).apply()
    }
    private fun putFloat(k: String, v: Float, f: MutableStateFlow<Float>) {
        f.value = v; sp.edit().putFloat(k, v).apply()
    }
    private fun putBool(k: String, v: Boolean, f: MutableStateFlow<Boolean>) {
        f.value = v; sp.edit().putBoolean(k, v).apply()
    }
}
