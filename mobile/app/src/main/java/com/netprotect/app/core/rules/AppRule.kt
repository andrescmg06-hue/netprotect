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
    // Sprint 10: the package had no rule of its own, but its assigned category did.
    CATEGORY("CATEGORY"),
    DEFAULT_POLICY("DEFAULT_POLICY"),
}

/** The fixed 11-category catalog — see backend app/models/category.py for why it's a fixed set
 * of constants rather than a table with rows a tutor could delete out from under an assignment,
 * and docs/sprint-10.md for why these specific 11 (the original spec's list isn't in this repo).
 */
enum class Category(val wireValue: String) {
    SOCIAL_MEDIA("SOCIAL_MEDIA"),
    GAMES("GAMES"),
    STREAMING("STREAMING"),
    EDUCATION("EDUCATION"),
    PRODUCTIVITY("PRODUCTIVITY"),
    COMMUNICATION("COMMUNICATION"),
    NEWS("NEWS"),
    SHOPPING("SHOPPING"),
    FINANCE("FINANCE"),
    UTILITIES("UTILITIES"),
    ADULT_CONTENT("ADULT_CONTENT");

    companion object {
        fun fromWire(value: String): Category? = entries.find { it.wireValue == value }
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

/** Which category a package was assigned to, on this device (Sprint 10). */
data class CategoryAssignment(val packageName: String, val category: Category)

/** Same shape as AppRule, just keyed by category instead of package_name — only consulted for
 * a package that has no AppRule of its own (see RuleEvaluator).
 */
data class CategoryRule(
    val category: Category,
    val ruleType: RuleType,
    val dailyLimitMinutes: Int?,
    val scheduleStartMinute: Int?,
    val scheduleEndMinute: Int?,
    val scheduleDaysMask: Int?,
)
