package com.ringcall.cellular.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import com.ringcall.cellular.domain.TargetState
import com.ringcall.cellular.domain.Transition
import java.time.ZonedDateTime

data class HomeUiState(
    val now: ZonedDateTime,
    val rules: List<Rule>,
    val current: TargetState,
    val next: Transition?,
    val defaultMode: RingMode,
    val hasDndAccess: Boolean,
    val canScheduleExact: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onAddRule: () -> Unit,
    onEditRule: (Rule) -> Unit,
    onToggleRule: (Rule, Boolean) -> Unit,
    onDefaultModeChange: (RingMode) -> Unit,
    onOpenDndSettings: () -> Unit,
    onOpenExactAlarmSettings: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("צלצול לפי שעות", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddRule,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("כלל חדש") },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!state.hasDndAccess) item {
                WarningCard(
                    "כדי לעבור למצב שקט צריך לאשר גישה ל״נא לא להפריע״.",
                    "לאישור",
                    onOpenDndSettings,
                )
            }
            if (!state.canScheduleExact) item {
                WarningCard(
                    "התראות מדויקות כבויות, ולכן השינויים עלולים להתעכב בכמה דקות.",
                    "להפעלה",
                    onOpenExactAlarmSettings,
                )
            }
            item { StatusCard(state) }
            item {
                Text(
                    "הכללים שלי",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.rules.isEmpty()) item {
                Text(
                    "עדיין אין כללים. הוסף כלל ראשון, למשל ״לילה: רטט מ-23:00 עד 07:00״.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            items(state.rules, key = { it.id }) { rule ->
                RuleCard(rule, active = state.current.sourceRuleId == rule.id, onEditRule, onToggleRule)
            }
            item { DefaultModeRow(state.defaultMode, onDefaultModeChange) }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState) {
    val ruleName = state.rules.firstOrNull { it.id == state.current.sourceRuleId }?.name
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("עכשיו", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    Format.mode(state.current.mode),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                )
                state.current.ringVolumePercent?.let {
                    Spacer(Modifier.width(8.dp))
                    Text("· עוצמה $it%", style = MaterialTheme.typography.titleMedium)
                }
            }
            Text(
                if (ruleName != null) "לפי הכלל ״$ruleName״" else "ברירת מחדל: אין כלל פעיל",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            val next = state.next
            Text(
                if (next == null) "אין שינויים מתוזמנים"
                else "השינוי הבא: ${Format.mode(next.state.mode)}, ${Format.relative(next.at, state.now)}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun RuleCard(
    rule: Rule,
    active: Boolean,
    onEdit: (Rule) -> Unit,
    onToggle: (Rule, Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onEdit(rule) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (active) {
                        Spacer(Modifier.width(8.dp))
                        Text("פעיל עכשיו", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${Format.range(rule.start, rule.end)} · ${Format.days(rule.days)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                ModeBadge(rule.mode, rule.ringVolumePercent)
            }
            Switch(checked = rule.enabled, onCheckedChange = { onToggle(rule, it) })
        }
    }
}

@Composable
fun ModeBadge(mode: RingMode, volume: Int? = null) {
    val (bg, fg) = modeColors(mode, isSystemInDarkTheme())
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(50)) {
        Text(
            Format.mode(mode) + (volume?.let { " · $it%" } ?: ""),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultModeRow(mode: RingMode, onChange: (RingMode) -> Unit) {
    Column(Modifier.padding(top = 16.dp)) {
        Text("כשאין כלל פעיל", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            RingMode.entries.forEachIndexed { i, m ->
                SegmentedButton(
                    selected = m == mode,
                    onClick = { onChange(m) },
                    shape = SegmentedButtonDefaults.itemShape(i, RingMode.entries.size),
                ) { Text(Format.mode(m)) }
            }
        }
    }
}

@Composable
private fun WarningCard(text: String, action: String, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            TextButton(onClick = onClick) { Text(action) }
        }
    }
}
