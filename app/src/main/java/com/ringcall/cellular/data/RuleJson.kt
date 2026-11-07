package com.ringcall.cellular.data

import com.ringcall.cellular.domain.RingMode
import com.ringcall.cellular.domain.Rule
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalTime

/** JSON (de)serialization for rules. Times are stored as minutes from midnight, days as a bitmask. */
object RuleJson {
    fun encode(rules: List<Rule>): String = JSONArray().apply {
        rules.forEach { r ->
            put(JSONObject().apply {
                put("id", r.id)
                put("name", r.name)
                put("start", r.start.hour * 60 + r.start.minute)
                put("end", r.end.hour * 60 + r.end.minute)
                put("days", r.days.fold(0) { acc, d -> acc or (1 shl d.ordinal) })
                put("mode", r.mode.name)
                if (r.ringVolumePercent != null) put("volume", r.ringVolumePercent)
                put("enabled", r.enabled)
            })
        }
    }.toString()

    fun decode(json: String?): List<Rule> {
        if (json.isNullOrBlank()) return emptyList()
        val arr = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            val mask = o.getInt("days")
            Rule(
                id = o.getLong("id"),
                name = o.getString("name"),
                start = minutes(o.getInt("start")),
                end = minutes(o.getInt("end")),
                days = DayOfWeek.entries.filter { mask and (1 shl it.ordinal) != 0 }.toSet(),
                mode = RingMode.valueOf(o.getString("mode")),
                ringVolumePercent = if (o.has("volume")) o.getInt("volume") else null,
                enabled = o.getBoolean("enabled"),
            )
        }
    }

    private fun minutes(m: Int): LocalTime = LocalTime.of(m / 60, m % 60)
}
