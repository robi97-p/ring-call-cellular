package com.ringcall.cellular.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import com.ringcall.cellular.domain.ScheduleCalculator
import com.ringcall.cellular.domain.TargetState
import org.junit.Rule as JUnitRule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Renders the main screens to PNG (app/build/outputs/roborazzi) so the UI can be reviewed without a device. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi", sdk = [34])
class ScreenshotTest {
    @get:JUnitRule val compose = createComposeRule()

    private val now = ZonedDateTime.of(2026, 11, 6, 23, 30, 0, 0, ZoneId.of("Asia/Jerusalem"))
    private val rules = listOf(
        Rule(1, "לילה", LocalTime.of(23, 0), LocalTime.of(7, 0), Format.WEEK.toSet(), RingMode.VIBRATE),
        Rule(2, "עבודה", LocalTime.of(9, 0), LocalTime.of(17, 0), Format.WORKDAYS, RingMode.NORMAL, 40),
        Rule(3, "שבת", LocalTime.of(18, 0), LocalTime.of(20, 0), setOf(java.time.DayOfWeek.FRIDAY), RingMode.SILENT, enabled = false),
    )

    private fun home(rules: List<Rule>, dnd: Boolean) = HomeUiState(
        now = now,
        rules = rules,
        current = ScheduleCalculator.targetStateAt(rules, now, TargetState(RingMode.NORMAL)),
        next = ScheduleCalculator.nextTransition(rules, now, TargetState(RingMode.NORMAL)),
        defaultMode = RingMode.NORMAL,
        hasDndAccess = dnd,
        canScheduleExact = true,
    )

    private fun shoot(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent { RingTheme(dark = false, content = content) }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun home() = shoot("1-home") {
        HomeScreen(home(rules, dnd = true), {}, {}, { _, _ -> }, {}, {}, {})
    }

    @Test fun homeEmptyWithWarning() = shoot("2-home-empty") {
        HomeScreen(home(emptyList(), dnd = false), {}, {}, { _, _ -> }, {}, {}, {})
    }

    @Test fun editRule() = shoot("3-edit") {
        EditRuleScreen(rules[1], {}, {}, {})
    }

    @Test fun newRule() = shoot("4-new") {
        EditRuleScreen(newRuleTemplate().copy(name = "לילה"), {}, {}, {})
    }
}
