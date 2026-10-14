package com.netprotect.app.core.network

import org.json.JSONObject

/** Reads and grants the signed-in user's roles (`/api/v1/users/me/roles`).
 *
 * The app is supervised-only: it is not a user choice anymore. After sign-in the app lists the
 * roles and, when the account has none (or a tutor explicitly asks to use the phone as a
 * supervised one), grants SUPERVISADO. The backend's POST is idempotent: it only inserts the
 * role when missing and audits `ROLE_GRANTED` the first time.
 */
class RoleClient(baseUrl: String) : HttpJsonClient(baseUrl) {
    suspend fun selectRole(accessToken: String, roleCode: String) {
        sendJson(
            "/api/v1/users/me/roles",
            "POST",
            JSONObject().put("role_code", roleCode),
            accessToken,
        )
    }

    /** Role codes the account currently holds, e.g. `["SUPERVISADO"]`; empty when it has none. */
    suspend fun listRoles(accessToken: String): List<String> {
        val roles = getJson("/api/v1/users/me/roles", accessToken).getJSONArray("roles")
        return List(roles.length()) { index -> roles.getJSONObject(index).getString("role_code") }
    }
}
