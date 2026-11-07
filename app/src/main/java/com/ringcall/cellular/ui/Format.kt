package com.ringcall.cellular.ui

import com.ringcall.cellular.domain.RingMode
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Hebrew display formatting. Weeks start on Sunday. */
object Format {
    val WEEK: List<DayOfWeek> = listOf(
        DayOfWeek.SUNDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
    )
    val WORKDAYS: Set<DayOfWeek> = WEEK.take(5).toSet()
    val WEEKEND: Set<DayOfWeek> = setOf(DayOfWeek.FRIDAY, DayOfWeek.SATURDAY)

    private val SHORT = listOf("א׳", "ב׳", "ג׳", "ד׳", "ה׳", "ו׳", "ש׳")
    private val LONG = listOf("ראשון", "שני", "שלישי", "רביעי", "חמישי", "שישי", "שבת")
    private val TIME = DateTimeFormatter.ofPattern("HH:mm")

    fun shortDay(d: DayOfWeek) = SHORT[WEEK.indexOf(d)]

    fun days(days: Set<DayOfWeek>): String = when {
        days.size == 7 -> "כל יום"
        days == WORKDAYS -> "א׳–ה׳"
        days == WEEKEND -> "סופ״ש"
        days.isEmpty() -> "אף יום"
        else -> WEEK.filter { it in days }.joinToString(" ") { shortDay(it) }
    }

    fun time(t: LocalTime): String = t.format(TIME)

    fun range(start: LocalTime, end: LocalTime): String = "${time(start)}–${time(end)}"

    fun mode(m: RingMode): String = when (m) {
        RingMode.NORMAL -> "צלצול"
        RingMode.VIBRATE -> "רטט"
        RingMode.SILENT -> "שקט"
    }

    /** "היום ב-07:00" / "מחר ב-07:00" / "ביום שלישי ב-07:00". */
    fun relative(at: ZonedDateTime, now: ZonedDateTime): String {
        val days = ChronoUnit.DAYS.between(now.toLocalDate(), at.toLocalDate())
        val t = time(at.toLocalTime())
        return when (days) {
            0L -> "היום ב-$t"
            1L -> "מחר ב-$t"
            else -> "ביום ${LONG[WEEK.indexOf(at.dayOfWeek)]} ב-$t"
        }
    }
}
