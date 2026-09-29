package com.netprotect.app.core.auth

import android.content.Context
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.network.AuthClient

/** Sprint 41: the process-wide [TokenSession]. Every component — screens, foreground services,
 * WorkManager workers — runs in the app's single process, so one instance (and its Mutex) is
 * enough to serialize every refresh-token rotation. */
object TokenProvider {
    @Volatile private var instance: TokenSession? = null

    fun get(context: Context): TokenSession =
        instance ?: synchronized(this) {
            instance ?: AuthClient(BuildConfig.API_BASE_URL).let { authClient ->
                TokenSession(
                    store = KeystoreRefreshTokenStore(TokenStore(context.applicationContext)),
                    refreshCall = authClient::refresh,
                    revokeCall = authClient::logout,
                )
            }.also { instance = it }
        }
}

private class KeystoreRefreshTokenStore(private val tokenStore: TokenStore) : RefreshTokenStore {
    override fun read(): String? = tokenStore.readRefreshToken()
    override fun save(token: String) = tokenStore.saveRefreshToken(token)
    override fun clear() = tokenStore.clear()
}
