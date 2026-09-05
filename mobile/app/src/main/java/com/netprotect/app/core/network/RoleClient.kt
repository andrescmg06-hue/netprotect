package com.netprotect.app.core.network

import org.json.JSONObject

class RoleClient(baseUrl: String) : HttpJsonClient(baseUrl) {
    suspend fun selectRole(accessToken: String, roleCode: String) {
        sendJson(
            "/api/v1/users/me/roles",
            "POST",
            JSONObject().put("role_code", roleCode),
            accessToken,
        )
    }
}
