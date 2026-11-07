package com.ringcall.cellular.domain

import java.time.ZonedDateTime

data class TargetState(
    val mode: RingMode,
    val ringVolumePercent: Int? = null,
    /** Id of the rule that produced this state; null = default state. */
    val sourceRuleId: Long? = null,
)

data class Transition(val at: ZonedDateTime, val state: TargetState)

/** Pure scheduling logic: what the ringer should be at a given moment, and when it next changes. */
object ScheduleCalculator {

    private const val LOOKAHEAD_DAYS = 8L

    private data class Window(val rule: Rule, val start: ZonedDateTime, val end: ZonedDateTime)

    /** All windows of enabled [rules] that start on a day in [fromDay, toDay] (relative to [now]). */
    private fun windows(rules: List<Rule>, now: ZonedDateTime, fromDay: Long, toDay: Long): List<Window> {
        val zone = now.zone
        val today = now.toLocalDate()
        val out = mutableListOf<Window>()
        for (offset in fromDay..toDay) {
            val date = today.plusDays(offset)
            for (rule in rules) {
                if (!rule.enabled || date.dayOfWeek !in rule.days) continue
                val endDate = if (rule.crossesMidnight) date.plusDays(1) else date
                out += Window(
                    rule,
                    ZonedDateTime.of(date, rule.start, zone),
                    ZonedDateTime.of(endDate, rule.end, zone),
                )
            }
        }
        return out
    }

    fun targetStateAt(rules: List<Rule>, now: ZonedDateTime, default: TargetState): TargetState {
        val active = windows(rules, now, -1, 0)
            .filter { !now.isBefore(it.start) && now.isBefore(it.end) }
            .map { it.rule }
            .distinctBy { it.id }
        return combine(active) ?: default
    }

    /** Quietest mode wins; among NORMAL rules the lowest explicit volume wins. */
    private fun combine(active: List<Rule>): TargetState? {
        if (active.isEmpty()) return null
        val quietest = active.maxOf { it.mode }
        val candidates = active.filter { it.mode == quietest }
        if (quietest != RingMode.NORMAL) {
            val r = candidates.minBy { it.id }
            return TargetState(quietest, null, r.id)
        }
        val withVolume = candidates.filter { it.ringVolumePercent != null }
        val r = withVolume.minByOrNull { it.ringVolumePercent!! } ?: candidates.minBy { it.id }
        return TargetState(RingMode.NORMAL, r.ringVolumePercent, r.id)
    }

    fun nextTransition(rules: List<Rule>, now: ZonedDateTime, default: TargetState): Transition? {
        val current = targetStateAt(rules, now, default)
        val boundaries = windows(rules, now, -1, LOOKAHEAD_DAYS)
            .flatMap { listOf(it.start, it.end) }
            .filter { it.isAfter(now) }
            .map { it.toInstant() }
            .distinct()
            .sorted()
        for (instant in boundaries) {
            val at = instant.atZone(now.zone)
            val state = targetStateAt(rules, at, default)
            if (!sameEffect(state, current)) return Transition(at, state)
        }
        return null
    }

    /** Two states are equivalent if they put the phone in the same mode/volume. */
    fun sameEffect(a: TargetState, b: TargetState): Boolean =
        a.mode == b.mode && a.ringVolumePercent == b.ringVolumePercent
}
