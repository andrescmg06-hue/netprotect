package com.netprotect.app.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.netprotect.app.core.network.AuthClient
import com.netprotect.app.core.network.CurrentUser

class AuthRepository(
    applicationContext: Context,
    baseUrl: String,
    private val googleWebClientId: String,
) {
    private val authClient = AuthClient(baseUrl)
    private val tokenStore = TokenStore(applicationContext)
    private val credentialManager = CredentialManager.create(applicationContext)

    var accessToken: String? = null
        private set

    /** [activityContext] must be an Activity: the account picker UI needs it. */
    suspend fun signIn(activityContext: Context): CurrentUser {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleWebClientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val result = credentialManager.getCredential(activityContext, request)
        val credential = result.credential

        val idToken = if (
            credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            error("Credencial inesperada del selector de cuentas de Google")
        }

        val tokens = authClient.loginWithGoogle(idToken)
        tokenStore.saveRefreshToken(tokens.refreshToken)
        accessToken = tokens.accessToken
        return authClient.fetchCurrentUser(tokens.accessToken)
    }

    /** Tries to resume a session from the token saved on a previous run. Never throws. */
    suspend fun restoreSession(): CurrentUser? {
        val storedRefreshToken = tokenStore.readRefreshToken() ?: return null
        return try {
            val tokens = authClient.refresh(storedRefreshToken)
            tokenStore.saveRefreshToken(tokens.refreshToken)
            accessToken = tokens.accessToken
            authClient.fetchCurrentUser(tokens.accessToken)
        } catch (_: Exception) {
            tokenStore.clear()
            accessToken = null
            null
        }
    }

    suspend fun signOut() {
        val storedRefreshToken = tokenStore.readRefreshToken()
        tokenStore.clear()
        accessToken = null
        if (storedRefreshToken != null) {
            runCatching { authClient.logout(storedRefreshToken) }
        }
    }
}
