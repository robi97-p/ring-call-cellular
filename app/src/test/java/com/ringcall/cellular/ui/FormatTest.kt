package com.ringcall.cellular.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.WEDNESDAY
import java.time.ZoneId
import java.time.ZonedDateTime

class FormatTest {
    @Test fun days() {
        assertEquals("כל יום", Format.days(Format.WEEK.toSet()))
        assertEquals("א׳–ה׳", Format.days(Format.WORKDAYS))
        assertEquals("סופ״ש", Format.days(Format.WEEKEND))
        assertEquals("א׳ ב׳ ד׳", Format.days(setOf(WEDNESDAY, SUNDAY, MONDAY)))
    }

    @Test fun relative() {
        val z = ZoneId.of("Asia/Jerusalem")
        val now = ZonedDateTime.of(2026, 11, 6, 20, 0, 0, 0, z) // Friday
        assertEquals("היום ב-22:00", Format.relative(now.withHour(22), now))
        assertEquals("מחר ב-07:00", Format.relative(now.plusDays(1).withHour(7), now))
        assertEquals("ביום שני ב-07:00", Format.relative(now.plusDays(3).withHour(7), now))
    }
}
