package com.netprotect.app.core.rules

/** Mirrors the backend's rule_type enum (app/models/rule.py). ALLOW is included for parity with
 * the wire format even though it has no effect yet on this device — see RuleEvaluator.
 */
enum class RuleType(val wireValue: String) {
    ALLOW("ALLOW"),
    BLOCK("BLOCK"),
    DAILY_LIMIT("DAILY_LIMIT"),
    SCHEDULE("SCHEDULE");

    companion object {
        fun fromWire(value: String): RuleType? = entries.find { it.wireValue == value }
    }
}

data class AppRule(
    val packageName: String,
    val ruleType: RuleType,
    val dailyLimitMinutes: Int?,
    // Minutes since local midnight on this device's own clock — not normalized to a timezone
    // (see the backend model's docstring). start > end is a valid overnight window.
    val scheduleStartMinute: Int?,
    val scheduleEndMinute: Int?,
    // Bitmask, bit 0 = Monday ... bit 6 = Sunday, matching the backend.
    val scheduleDaysMask: Int?,
)
