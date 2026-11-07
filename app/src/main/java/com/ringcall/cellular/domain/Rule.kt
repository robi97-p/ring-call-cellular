package com.ringcall.cellular.domain

import java.time.DayOfWeek
import java.time.LocalTime

/** Ringer modes, ordered from loudest to quietest. */
enum class RingMode { NORMAL, VIBRATE, SILENT }

/**
 * A time-window rule.
 *
 * The rule is active on `[start, end)`. If `end < start` the window crosses midnight
 * and ends on the following day. [days] are the days on which the window *starts*.
 */
data class Rule(
    val id: Long = 0,
    val name: String,
    val start: LocalTime,
    val end: LocalTime,
    val days: Set<DayOfWeek>,
    val mode: RingMode,
    /** 0..100, only meaningful for [RingMode.NORMAL]; null = leave volume unchanged. */
    val ringVolumePercent: Int? = null,
    val enabled: Boolean = true,
) {
    val crossesMidnight: Boolean get() = end < start
}

enum class RuleError { EMPTY_NAME, NAME_TOO_LONG, SAME_START_END, NO_DAYS, BAD_VOLUME }

object RuleValidator {
    const val MAX_NAME = 40

    fun validate(rule: Rule): List<RuleError> = buildList {
        if (rule.name.isBlank()) add(RuleError.EMPTY_NAME)
        if (rule.name.length > MAX_NAME) add(RuleError.NAME_TOO_LONG)
        if (rule.start == rule.end) add(RuleError.SAME_START_END)
        if (rule.days.isEmpty()) add(RuleError.NO_DAYS)
        val v = rule.ringVolumePercent
        if (v != null && (v !in 0..100 || rule.mode != RingMode.NORMAL)) add(RuleError.BAD_VOLUME)
    }
}
