package com.ringcall.cellular.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ringcall.cellular.RingCallApp
import com.ringcall.cellular.domain.Rule
import com.ringcall.cellular.domain.ScheduleCalculator
import java.time.ZonedDateTime

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as RingCallApp
        val store = app.store
        val scheduler = app.scheduler
        scheduler.scheduleNext()

        setContent {
            RingTheme {
                val rules by store.rules.collectAsStateWithLifecycle()
                val defaultMode by store.defaultMode.collectAsStateWithLifecycle()
                var editing by remember { mutableStateOf<Rule?>(null) }
                // Bumped on resume so permission state and "now" are re-read.
                var tick by remember { mutableIntStateOf(0) }
                LifecycleResumeEffect(Unit) {
                    tick++
                    scheduler.scheduleNext()
                    onPauseOrDispose { }
                }

                val current = editing
                if (current != null) {
                    EditRuleScreen(
                        initial = current,
                        onSave = { store.upsert(it); scheduler.refresh(); editing = null },
                        onDelete = { store.delete(it.id); scheduler.refresh(); editing = null },
                        onCancel = { editing = null },
                    )
                } else {
                    @Suppress("UNUSED_EXPRESSION") tick
                    val now = ZonedDateTime.now()
                    HomeScreen(
                        state = HomeUiState(
                            now = now,
                            rules = rules,
                            current = ScheduleCalculator.targetStateAt(rules, now, store.defaultState),
                            next = ScheduleCalculator.nextTransition(rules, now, store.defaultState),
                            defaultMode = defaultMode,
                            hasDndAccess = app.ringer.hasPolicyAccess(),
                            canScheduleExact = scheduler.canScheduleExact(),
                        ),
                        onAddRule = { editing = newRuleTemplate() },
                        onEditRule = { editing = it },
                        onToggleRule = { r, on -> store.setEnabled(r.id, on); scheduler.refresh() },
                        onDefaultModeChange = { store.setDefaultMode(it); scheduler.refresh() },
                        onOpenDndSettings = {
                            startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
                        },
                        onOpenExactAlarmSettings = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                startActivity(
                                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                        .setData(Uri.parse("package:$packageName"))
                                )
                            }
                        },
                    )
                }
            }
        }
    }
}
