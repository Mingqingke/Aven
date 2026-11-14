package com.aven.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Dates {

    private val zone: ZoneId get() = ZoneId.systemDefault()
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy.MM.dd")
    private val shortFmt = DateTimeFormatter.ofPattern("M月d日")
    private val monthFmt = DateTimeFormatter.ofPattern("yyyy.MM")

    fun of(y: Int, m: Int, d: Int): Long =
        LocalDate.of(y, m, d).atStartOfDay(zone).toInstant().toEpochMilli()

    fun date(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    fun fmt(millis: Long): String = date(millis).format(dateFmt)

    fun fmtShort(millis: Long): String = date(millis).format(shortFmt)

    fun fmtMonth(millis: Long): String = date(millis).format(monthFmt)

    fun today(): Long = LocalDate.now().atStartOfDay(zone).toInstant().toEpochMilli()

    fun now(): Long = System.currentTimeMillis()

    fun daysBetween(from: Long, to: Long): Long =
        java.time.temporal.ChronoUnit.DAYS.between(date(from), date(to))

    /** 下一次高考（6 月 7 日）：已过就取明年。 */
    fun defaultGaokao(): Long {
        val now = LocalDate.now()
        var y = now.year
        var d = LocalDate.of(y, 6, 7)
        if (d.isBefore(now)) { y += 1; d = LocalDate.of(y, 6, 7) }
        return d.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    fun gaokaoDate(): Long = if (Prefs.gaokaoDate.value > 0) Prefs.gaokaoDate.value else defaultGaokao()

    fun greeting(): String {
        val h = LocalDateTime.now().hour
        return when (h) {
            in 5..10 -> "早上好"
            in 11..13 -> "中午好"
            in 14..17 -> "下午好"
            in 18..22 -> "晚上好"
            else -> "夜深了"
        }
    }

    fun weekday(millis: Long = System.currentTimeMillis()): String =
        when (date(millis).dayOfWeek.value) {
            1 -> "星期一"; 2 -> "星期二"; 3 -> "星期三"; 4 -> "星期四"
            5 -> "星期五"; 6 -> "星期六"; else -> "星期日"
        }

    fun headerDate(millis: Long = System.currentTimeMillis()): String {
        val d = date(millis)
        return "${d.monthValue} 月 ${d.dayOfMonth} 日 · ${weekday(millis)}"
    }
}
