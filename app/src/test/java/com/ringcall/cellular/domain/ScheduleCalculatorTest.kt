package com.ringcall.cellular.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ScheduleCalculatorTest {
    private val zone = ZoneId.of("Asia/Jerusalem")
    private val default = TargetState(RingMode.NORMAL)
    private val all = DayOfWeek.entries.toSet()

    // 2026-11-06 is a Friday.
    private fun at(day: Int, h: Int, m: Int = 0, month: Int = 11) =
        ZonedDateTime.of(LocalDateTime.of(2026, month, day, h, m), zone)

    private fun rule(
        id: Long, start: String, end: String, mode: RingMode,
        days: Set<DayOfWeek> = all, volume: Int? = null, enabled: Boolean = true,
    ) = Rule(id, "r$id", LocalTime.parse(start), LocalTime.parse(end), days, mode, volume, enabled)

    private fun stateAt(rules: List<Rule>, t: ZonedDateTime) = ScheduleCalculator.targetStateAt(rules, t, default)

    @Test fun `simple window boundaries are start-inclusive, end-exclusive`() {
        val r = listOf(rule(1, "09:00", "17:00", RingMode.VIBRATE))
        assertEquals(RingMode.NORMAL, stateAt(r, at(6, 8, 59)).mode)
        assertEquals(RingMode.VIBRATE, stateAt(r, at(6, 9, 0)).mode)
        assertEquals(RingMode.VIBRATE, stateAt(r, at(6, 16, 59)).mode)
        assertEquals(RingMode.NORMAL, stateAt(r, at(6, 17, 0)).mode)
        assertEquals(1L, stateAt(r, at(6, 10)).sourceRuleId)
        assertNull(stateAt(r, at(6, 18)).sourceRuleId)
    }

    @Test fun `overnight rule is active after midnight on the following day`() {
        val r = listOf(rule(1, "22:00", "07:00", RingMode.SILENT, days = setOf(FRIDAY)))
        assertEquals(RingMode.NORMAL, stateAt(r, at(6, 21, 59)).mode)
        assertEquals(RingMode.SILENT, stateAt(r, at(6, 23)).mode)     // Friday night
        assertEquals(RingMode.SILENT, stateAt(r, at(7, 6, 59)).mode)  // Saturday morning
        assertEquals(RingMode.NORMAL, stateAt(r, at(7, 7)).mode)
        assertEquals(RingMode.NORMAL, stateAt(r, at(7, 23)).mode)     // Saturday not selected
        assertEquals(RingMode.NORMAL, stateAt(r, at(6, 3)).mode)      // Thursday not selected
    }

    @Test fun `overnight rule wraps the week from Saturday to Sunday`() {
        val r = listOf(rule(1, "23:00", "06:00", RingMode.VIBRATE, days = setOf(SATURDAY)))
        assertEquals(RingMode.VIBRATE, stateAt(r, at(8, 2)).mode) // Sunday 02:00
        assertEquals(RingMode.NORMAL, stateAt(r, at(8, 6)).mode)
    }

    @Test fun `quieter mode wins on overlap`() {
        val r = listOf(
            rule(1, "08:00", "18:00", RingMode.VIBRATE),
            rule(2, "12:00", "13:00", RingMode.SILENT),
        )
        assertEquals(RingMode.VIBRATE, stateAt(r, at(6, 11)).mode)
        assertEquals(TargetState(RingMode.SILENT, null, 2), stateAt(r, at(6, 12, 30)))
    }

    @Test fun `lowest explicit volume wins among normal rules`() {
        val r = listOf(
            rule(1, "08:00", "18:00", RingMode.NORMAL, volume = 70),
            rule(2, "08:00", "18:00", RingMode.NORMAL, volume = 30),
            rule(3, "08:00", "18:00", RingMode.NORMAL, volume = null),
        )
        assertEquals(30, stateAt(r, at(6, 10)).ringVolumePercent)
        assertEquals(50, stateAt(listOf(r[2], rule(4, "08:00", "18:00", RingMode.NORMAL, volume = 50)), at(6, 10)).ringVolumePercent)
    }

    @Test fun `disabled rules are ignored`() {
        val r = listOf(rule(1, "00:00", "23:59", RingMode.SILENT, enabled = false))
        assertEquals(default, stateAt(r, at(6, 12)))
        assertNull(ScheduleCalculator.nextTransition(r, at(6, 12), default))
    }

    @Test fun `next transition is the end of the active rule`() {
        val r = listOf(rule(1, "22:00", "07:00", RingMode.VIBRATE))
        val next = ScheduleCalculator.nextTransition(r, at(6, 23), default)!!
        assertEquals(at(7, 7), next.at)
        assertEquals(RingMode.NORMAL, next.state.mode)
    }

    @Test fun `next transition is tomorrow's start when today's window passed`() {
        val r = listOf(rule(1, "09:00", "10:00", RingMode.SILENT, days = setOf(SATURDAY)))
        val next = ScheduleCalculator.nextTransition(r, at(6, 12), default)!!
        assertEquals(at(7, 9), next.at)
        assertEquals(RingMode.SILENT, next.state.mode)
    }

    @Test fun `adjacent rules with the same effect produce a single transition`() {
        val r = listOf(
            rule(1, "08:00", "10:00", RingMode.VIBRATE),
            rule(2, "10:00", "12:00", RingMode.VIBRATE),
        )
        val next = ScheduleCalculator.nextTransition(r, at(6, 9), default)!!
        assertEquals(at(6, 12), next.at)
    }

    @Test fun `no rules means no transition`() {
        assertNull(ScheduleCalculator.nextTransition(emptyList(), at(6, 9), default))
    }

    @Test fun `default state is used when no rule is active`() {
        val vibrateDefault = TargetState(RingMode.VIBRATE)
        val r = listOf(rule(1, "09:00", "10:00", RingMode.NORMAL, days = setOf(SUNDAY)))
        assertEquals(vibrateDefault, ScheduleCalculator.targetStateAt(r, at(6, 9, 30), vibrateDefault))
    }

    @Test fun `DST spring forward shifts a non-existent start time`() {
        // Israel moved clocks 02:00 -> 03:00 on 2026-03-27.
        val r = listOf(rule(1, "02:30", "04:00", RingMode.SILENT))
        val before = at(27, 1, 0, month = 3)
        val next = ScheduleCalculator.nextTransition(r, before, default)!!
        assertEquals(LocalTime.of(3, 30), next.at.toLocalTime())
        assertEquals(RingMode.SILENT, stateAt(r, at(27, 3, 45, month = 3)).mode)
    }

    @Test fun `computation follows the zone of now`() {
        val r = listOf(rule(1, "09:00", "10:00", RingMode.SILENT))
        val ny = ZonedDateTime.of(LocalDateTime.of(2026, 11, 6, 9, 30), ZoneId.of("America/New_York"))
        assertEquals(RingMode.SILENT, ScheduleCalculator.targetStateAt(r, ny, default).mode)
    }
}
