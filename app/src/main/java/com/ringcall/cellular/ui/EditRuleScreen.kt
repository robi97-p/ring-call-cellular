package com.ringcall.cellular.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import com.ringcall.cellular.domain.RuleError
import com.ringcall.cellular.domain.RuleValidator
import java.time.LocalTime

fun newRuleTemplate() = Rule(
    name = "",
    start = LocalTime.of(23, 0),
    end = LocalTime.of(7, 0),
    days = Format.WEEK.toSet(),
    mode = RingMode.VIBRATE,
)

private fun errorText(e: RuleError) = when (e) {
    RuleError.EMPTY_NAME -> "צריך לתת לכלל שם"
    RuleError.NAME_TOO_LONG -> "השם ארוך מדי (עד ${RuleValidator.MAX_NAME} תווים)"
    RuleError.SAME_START_END -> "שעת ההתחלה והסיום זהות"
    RuleError.NO_DAYS -> "צריך לבחור לפחות יום אחד"
    RuleError.BAD_VOLUME -> "עוצמה לא תקינה"
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditRuleScreen(
    initial: Rule,
    onSave: (Rule) -> Unit,
    onDelete: (Rule) -> Unit,
    onCancel: () -> Unit,
) {
    val isNew = initial.id == 0L
    var rule by remember { mutableStateOf(initial) }
    var showErrors by remember { mutableStateOf(false) }
    var pickingStart by remember { mutableStateOf<Boolean?>(null) }
    val errors = RuleValidator.validate(rule)

    BackHandler(onBack = onCancel)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNew) "כלל חדש" else "עריכת כלל") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "חזרה")
                    }
                },
                actions = {
                    if (!isNew) IconButton(onClick = { onDelete(initial) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "מחיקה")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            OutlinedTextField(
                value = rule.name,
                onValueChange = { rule = rule.copy(name = it) },
                label = { Text("שם הכלל") },
                placeholder = { Text("למשל: לילה, עבודה, שבת") },
                singleLine = true,
                isError = showErrors && (RuleError.EMPTY_NAME in errors || RuleError.NAME_TOO_LONG in errors),
                modifier = Modifier.fillMaxWidth(),
            )

            Section("שעות") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TimeBox("מ-", rule.start, Modifier.weight(1f)) { pickingStart = true }
                    TimeBox("עד", rule.end, Modifier.weight(1f)) { pickingStart = false }
                }
                if (rule.crossesMidnight) {
                    Text(
                        "הכלל עובר חצות ומסתיים למחרת ב-${Format.time(rule.end)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }

            Section("ימים") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Format.WEEK.forEach { day ->
                        FilterChip(
                            selected = day in rule.days,
                            onClick = {
                                rule = rule.copy(days = if (day in rule.days) rule.days - day else rule.days + day)
                            },
                            label = { Text(Format.shortDay(day)) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = { rule = rule.copy(days = Format.WEEK.toSet()) }, label = { Text("כל יום") })
                    AssistChip(onClick = { rule = rule.copy(days = Format.WORKDAYS) }, label = { Text("א׳–ה׳") })
                    AssistChip(onClick = { rule = rule.copy(days = Format.WEEKEND) }, label = { Text("סופ״ש") })
                }
            }

            Section("מצב") {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    RingMode.entries.forEachIndexed { i, m ->
                        SegmentedButton(
                            selected = rule.mode == m,
                            onClick = {
                                rule = rule.copy(
                                    mode = m,
                                    ringVolumePercent = if (m == RingMode.NORMAL) rule.ringVolumePercent else null,
                                )
                            },
                            shape = SegmentedButtonDefaults.itemShape(i, RingMode.entries.size),
                        ) { Text(Format.mode(m)) }
                    }
                }
            }

            if (rule.mode == RingMode.NORMAL) {
                Section("עוצמת צלצול") {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("לשנות עוצמה", modifier = Modifier.weight(1f))
                        Switch(
                            checked = rule.ringVolumePercent != null,
                            onCheckedChange = { rule = rule.copy(ringVolumePercent = if (it) 50 else null) },
                        )
                    }
                    rule.ringVolumePercent?.let { v ->
                        Slider(
                            value = v.toFloat(),
                            onValueChange = { rule = rule.copy(ringVolumePercent = it.toInt()) },
                            valueRange = 0f..100f,
                        )
                        Text("$v%", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (showErrors && errors.isNotEmpty()) {
                Text(
                    errors.joinToString("\n") { errorText(it) },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = { if (errors.isEmpty()) onSave(rule.copy(name = rule.name.trim())) else showErrors = true },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text("שמירה", fontWeight = FontWeight.Bold) }
        }
    }

    pickingStart?.let { isStart ->
        val t = if (isStart) rule.start else rule.end
        val pickerState = rememberTimePickerState(t.hour, t.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { pickingStart = null },
            confirmButton = {
                TextButton(onClick = {
                    val picked = LocalTime.of(pickerState.hour, pickerState.minute)
                    rule = if (isStart) rule.copy(start = picked) else rule.copy(end = picked)
                    pickingStart = null
                }) { Text("אישור") }
            },
            dismissButton = { TextButton(onClick = { pickingStart = null }) { Text("ביטול") } },
            title = { Text(if (isStart) "שעת התחלה" else "שעת סיום") },
            text = { TimePicker(state = pickerState) },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun TimeBox(label: String, time: LocalTime, modifier: Modifier, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(2.dp))
            Text(Format.time(time), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}
