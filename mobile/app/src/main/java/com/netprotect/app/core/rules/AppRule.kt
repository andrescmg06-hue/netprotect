package com.netprotect.app.core.rules

/** Mirrors the backend's rule_type enum (app/models/rule.py). As of Sprint 9, ALLOW does have an
 * effect: it approves an app on a device whose default policy is BLOCK — see RuleEvaluator.
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

/** What a device does with an app that has no rule of its own: ALLOW is blocklist mode (the
 * Sprint 8 behavior), BLOCK is allowlist mode (only approved apps run).
 */
enum class DefaultAppPolicy(val wireValue: String) {
    ALLOW("ALLOW"),
    BLOCK("BLOCK");

    companion object {
        fun fromWire(value: String): DefaultAppPolicy? = entries.find { it.wireValue == value }
    }
}

/** Why an app was blocked. Deliberately not RuleType (which Sprint 8 reused for this): as of
 * Sprint 9 an app can be blocked with no rule at all, because the device's default policy says
 * so, and DEFAULT_POLICY is not something a tutor can create as a rule. The backend draws the
 * same line (RuleType vs. AppliedRuleType in app/schemas/rule.py).
 */
enum class BlockReason(val wireValue: String) {
    BLOCK("BLOCK"),
    DAILY_LIMIT("DAILY_LIMIT"),
    SCHEDULE("SCHEDULE"),
    DEFAULT_POLICY("DEFAULT_POLICY"),
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
