package com.ringcall.cellular.data

import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalTime

class RuleJsonTest {
    @Test fun `round trip keeps every field`() {
        val rules = listOf(
            Rule(1, "לילה", LocalTime.of(23, 15), LocalTime.of(6, 45), setOf(DayOfWeek.SUNDAY, DayOfWeek.SATURDAY), RingMode.SILENT),
            Rule(2, "עבודה", LocalTime.of(0, 0), LocalTime.of(23, 59), DayOfWeek.entries.toSet(), RingMode.NORMAL, 35, enabled = false),
            Rule(3, "x", LocalTime.of(9, 0), LocalTime.of(10, 0), emptySet(), RingMode.VIBRATE),
        )
        assertEquals(rules, RuleJson.decode(RuleJson.encode(rules)))
    }

    @Test fun `empty input decodes to empty list`() {
        assertTrue(RuleJson.decode(null).isEmpty())
        assertTrue(RuleJson.decode("").isEmpty())
    }
}
