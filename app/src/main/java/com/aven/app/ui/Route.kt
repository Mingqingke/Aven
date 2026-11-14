package com.aven.app.ui

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.Serializable
import top.yukonga.miuix.kmp.nav.core.NavKey

@Serializable
sealed interface Route : NavKey {
    @Serializable data object Home : Route
    @Serializable data object Records : Route
    @Serializable data object Insights : Route
    @Serializable data object Plans : Route

    @Serializable data class ExamDetail(val id: Long) : Route
    @Serializable data class SubjectDetail(val subject: String) : Route
    @Serializable data class ReportDetail(val id: Long) : Route
    @Serializable data class EditExam(val id: Long) : Route
    @Serializable data object AddExam : Route
    @Serializable data object AiChat : Route
    @Serializable data object Reports : Route

    @Serializable data object Settings : Route
    @Serializable data object SettingsProfile : Route
    @Serializable data object SettingsExam : Route
    @Serializable data object SettingsAi : Route
    @Serializable data object SettingsData : Route
    @Serializable data object SettingsAppearance : Route
    @Serializable data object SettingsPrivacy : Route
    @Serializable data object SettingsUpdate : Route
    @Serializable data object Update : Route
    @Serializable data object About : Route
    @Serializable data object AboutComponents : Route
    @Serializable data object AboutTerms : Route
    @Serializable data object AboutUpdate : Route
}

object AppState {
    val toast = MutableStateFlow("")
    fun say(msg: String) { toast.value = msg }
}
