package com.ringcall.cellular.data

import androidx.test.core.app.ApplicationProvider
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.DayOfWeek
import java.time.LocalTime

@RunWith(RobolectricTestRunner::class)
class RuleStoreTest {
    private val ctx = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val rule = Rule(0, "לילה", LocalTime.of(23, 0), LocalTime.of(7, 0), setOf(DayOfWeek.MONDAY), RingMode.VIBRATE)

    @Test fun `insert assigns ids and persists across instances`() {
        val store = RuleStore(ctx)
        val a = store.upsert(rule)
        val b = store.upsert(rule.copy(name = "עבודה"))
        assertEquals(1L, a.id)
        assertEquals(2L, b.id)
        assertEquals(listOf(a, b), RuleStore(ctx).rules.value)
    }

    @Test fun `update, toggle and delete`() {
        val store = RuleStore(ctx)
        val a = store.upsert(rule)
        store.upsert(a.copy(name = "שינה"))
        assertEquals("שינה", store.rules.value.single().name)
        store.setEnabled(a.id, false)
        assertFalse(store.rules.value.single().enabled)
        store.delete(a.id)
        assertTrue(store.rules.value.isEmpty())
    }

    @Test fun `default mode persists`() {
        assertEquals(RingMode.NORMAL, RuleStore(ctx).defaultMode.value)
        RuleStore(ctx).setDefaultMode(RingMode.VIBRATE)
        assertEquals(RingMode.VIBRATE, RuleStore(ctx).defaultMode.value)
    }
}
