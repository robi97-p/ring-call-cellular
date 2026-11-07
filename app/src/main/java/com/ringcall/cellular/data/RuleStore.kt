package com.ringcall.cellular.data

import android.content.Context
import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import com.ringcall.cellular.domain.TargetState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Rules + settings persisted in SharedPreferences (MVP; Room/DataStore planned in issue #2). */
class RuleStore(context: Context) {
    private val prefs = context.getSharedPreferences("ring_rules", Context.MODE_PRIVATE)

    private val _rules = MutableStateFlow(RuleJson.decode(prefs.getString(KEY_RULES, null)))
    val rules: StateFlow<List<Rule>> = _rules.asStateFlow()

    private val _defaultMode = MutableStateFlow(
        prefs.getString(KEY_DEFAULT_MODE, null)?.let(RingMode::valueOf) ?: RingMode.NORMAL
    )
    val defaultMode: StateFlow<RingMode> = _defaultMode.asStateFlow()

    val defaultState: TargetState get() = TargetState(_defaultMode.value)

    fun upsert(rule: Rule): Rule {
        val current = _rules.value
        val saved = if (rule.id == 0L) rule.copy(id = (current.maxOfOrNull { it.id } ?: 0L) + 1) else rule
        val updated = if (current.any { it.id == saved.id }) {
            current.map { if (it.id == saved.id) saved else it }
        } else {
            current + saved
        }
        persist(updated)
        return saved
    }

    fun delete(id: Long) = persist(_rules.value.filterNot { it.id == id })

    fun setEnabled(id: Long, enabled: Boolean) =
        persist(_rules.value.map { if (it.id == id) it.copy(enabled = enabled) else it })

    fun setDefaultMode(mode: RingMode) {
        prefs.edit().putString(KEY_DEFAULT_MODE, mode.name).apply()
        _defaultMode.value = mode
    }

    private fun persist(rules: List<Rule>) {
        prefs.edit().putString(KEY_RULES, RuleJson.encode(rules)).apply()
        _rules.value = rules
    }

    private companion object {
        const val KEY_RULES = "rules"
        const val KEY_DEFAULT_MODE = "default_mode"
    }
}
