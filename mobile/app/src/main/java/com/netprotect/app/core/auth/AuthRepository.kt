package com.netprotect.app.core.auth

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.netprotect.app.core.network.AuthClient
import com.netprotect.app.core.network.CurrentUser
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** Outcome of resuming a saved session at startup. [Unreachable] keeps the saved session: being
 * offline (or the backend being down) is not the same as being signed out. */
sealed interface RestoreResult {
    data class Restored(val user: CurrentUser) : RestoreResult
    data object NoSession : RestoreResult
    data object Unreachable : RestoreResult
}

class AuthRepository(
    applicationContext: Context,
    baseUrl: String,
    private val googleWebClientId: String,
) {
    private val authClient = AuthClient(baseUrl)
    private val credentialManager = CredentialManager.create(applicationContext)

    /** Sprint 41: the process-wide session every screen and service renews through. */
    val session: TokenSession = TokenProvider.get(applicationContext)

    /** [activityContext] must be an Activity: the account picker UI needs it. */
    suspend fun signIn(activityContext: Context): CurrentUser {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(googleWebClientId)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val result = try {
            credentialManager.getCredential(activityContext, request)
        } catch (_: NoCredentialException) {
            // No Google account on this phone (lint CredentialManagerMisuse, L-01): say so
            // instead of surfacing the system's English message.
            throw IllegalStateException(
                "No hay una cuenta de Google en este teléfono. Agrégala en Ajustes e intenta de nuevo."
            )
        }
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
        session.install(tokens)
        return authClient.fetchCurrentUser(tokens.accessToken)
    }

    /** Tries to resume a session from the token saved on a previous run. Never throws.
     *
     * Sprint 41: only a rejected refresh token ends the session (TokenSession clears it). Before,
     * any failure here — no network, or losing a renewal race against a background service —
     * wiped the stored token and signed the whole device out. */
    suspend fun restoreSession(): RestoreResult {
        if (!session.hasStoredSession()) return RestoreResult.NoSession
        return try {
            val accessToken = session.validAccessToken()
            RestoreResult.Restored(authClient.fetchCurrentUser(accessToken))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: SessionExpiredException) {
            RestoreResult.NoSession
        } catch (_: IOException) {
            RestoreResult.Unreachable
        } catch (_: Exception) {
            // Backend answered with something unexpected (e.g. 5xx): keep the session, retry later.
            RestoreResult.Unreachable
        }
    }

    suspend fun signOut() {
        val storedRefreshToken = session.signOut()
        if (storedRefreshToken != null) {
            runCatching { authClient.logout(storedRefreshToken) }
        }
    }
}
