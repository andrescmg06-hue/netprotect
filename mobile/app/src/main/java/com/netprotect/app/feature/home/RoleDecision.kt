package com.netprotect.app.feature.home

const val ROLE_TUTOR = "TUTOR"
const val ROLE_SUPERVISADO = "SUPERVISADO"

/** What the app does with the account's roles after sign-in. The app is supervised-only. */
enum class RoleOutcome {
    /** Already supervised: enter supervised mode. */
    Supervised,

    /** No tutor role and no supervised role yet: grant SUPERVISADO, then enter. */
    GrantSupervised,

    /** A tutor account: never granted silently, the user is told to use the web panel. */
    TutorAccount,
}

/** Pure decision from the role codes returned by `GET /users/me/roles`. */
fun decideRole(roles: Collection<String>): RoleOutcome = when {
    ROLE_SUPERVISADO in roles -> RoleOutcome.Supervised
    ROLE_TUTOR in roles -> RoleOutcome.TutorAccount
    else -> RoleOutcome.GrantSupervised
}
