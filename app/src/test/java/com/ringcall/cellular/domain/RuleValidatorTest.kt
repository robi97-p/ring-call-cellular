package com.ringcall.cellular.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleValidatorTest {
    private val ok = Rule(0, "לילה", LocalTime.of(23, 0), LocalTime.of(7, 0), setOf(DayOfWeek.MONDAY), RingMode.VIBRATE)

    @Test fun `valid rule has no errors`() = assertTrue(RuleValidator.validate(ok).isEmpty())
    @Test fun `blank name`() = assertEquals(listOf(RuleError.EMPTY_NAME), RuleValidator.validate(ok.copy(name = "  ")))
    @Test fun `name length limit`() {
        assertTrue(RuleValidator.validate(ok.copy(name = "א".repeat(40))).isEmpty())
        assertEquals(listOf(RuleError.NAME_TOO_LONG), RuleValidator.validate(ok.copy(name = "א".repeat(41))))
    }
    @Test fun `same start and end`() =
        assertEquals(listOf(RuleError.SAME_START_END), RuleValidator.validate(ok.copy(end = ok.start)))
    @Test fun `no days`() = assertEquals(listOf(RuleError.NO_DAYS), RuleValidator.validate(ok.copy(days = emptySet())))
    @Test fun `volume bounds`() {
        val normal = ok.copy(mode = RingMode.NORMAL)
        assertTrue(RuleValidator.validate(normal.copy(ringVolumePercent = 0)).isEmpty())
        assertTrue(RuleValidator.validate(normal.copy(ringVolumePercent = 100)).isEmpty())
        assertEquals(listOf(RuleError.BAD_VOLUME), RuleValidator.validate(normal.copy(ringVolumePercent = 101)))
        assertEquals(listOf(RuleError.BAD_VOLUME), RuleValidator.validate(normal.copy(ringVolumePercent = -1)))
    }
    @Test fun `volume only allowed in normal mode`() =
        assertEquals(listOf(RuleError.BAD_VOLUME), RuleValidator.validate(ok.copy(ringVolumePercent = 50)))
}
