package com.netprotect.app.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.AuthRepository
import com.netprotect.app.core.auth.RestoreResult
import com.netprotect.app.core.auth.authorized
import com.netprotect.app.core.auth.SupervisedRoleCache
import com.netprotect.app.core.network.CurrentUser
import com.netprotect.app.core.network.InfrastructureHealth
import com.netprotect.app.core.network.InfrastructureHealthClient
import com.netprotect.app.core.network.RoleClient
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toHealthUiError
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.feature.supervised.SupervisedShell
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private sealed interface HomeState {
    data object Loading : HomeState
    data class SignedOut(val error: String? = null) : HomeState
    data class TutorAccount(
        val user: CurrentUser,
        val error: String? = null,
        val granting: Boolean = false,
    ) : HomeState
    data class RoleCheckFailed(val user: CurrentUser, val message: String) : HomeState
    data class InSupervisedMode(val user: CurrentUser) : HomeState
}

private sealed interface InfraState {
    data object Checking : InfraState
    data class Ready(val health: InfrastructureHealth) : InfraState
    data class Error(val error: UiError) : InfraState
}

/** Top-level router. The app is supervised-only (Sprint 61, B-07): there is no mode choice.
 *
 * Loading -> SignedOut -> (role check) -> SupervisedMode, or TutorAccount / RoleCheckFailed.
 *
 * After sign-in or a restored session: if [SupervisedRoleCache] already holds this user's id the
 * app enters supervised mode with no network call, so the protection services never wait on an
 * extra request at start. Otherwise it lists the roles ([RoleClient.listRoles]) and applies
 * [decideRole]: SUPERVISADO enters, no role grants SUPERVISADO and enters, and a tutor-only
 * account gets [TutorAccountScreen], where the role is granted only if the user asks for it.
 * The cache is a local shortcut; the backend still authorizes every supervised request.
 */
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authRepository = remember {
        AuthRepository(
            applicationContext = context.applicationContext,
            baseUrl = BuildConfig.API_BASE_URL,
            googleWebClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
        )
    }
    val roleClient = remember { RoleClient(BuildConfig.API_BASE_URL) }
    val healthClient = remember { InfrastructureHealthClient(BuildConfig.API_BASE_URL) }

    var state by remember { mutableStateOf<HomeState>(HomeState.Loading) }
    var infraState by remember { mutableStateOf<InfraState>(InfraState.Checking) }
    // Sprint 43: bumped by "Reintentar"; one tap = one new /health/ready check, never a loop.
    var infraAttempt by remember { mutableIntStateOf(0) }

    fun failureState(user: CurrentUser, exception: Exception): HomeState {
        val error = exception.toUiError()
        // An expired session goes back to login with its reason, as from any other screen.
        return if (error == UiError.SessionExpired) {
            HomeState.SignedOut(error.message)
        } else {
            HomeState.RoleCheckFailed(user, error.message)
        }
    }

    suspend fun grantSupervised(user: CurrentUser) {
        authRepository.session.authorized { token -> roleClient.selectRole(token, ROLE_SUPERVISADO) }
        SupervisedRoleCache.write(context, user.id)
    }

    suspend fun resolveRole(user: CurrentUser) {
        if (SupervisedRoleCache.isConfirmedFor(context, user.id)) {
            state = HomeState.InSupervisedMode(user)
            return
        }
        state = HomeState.Loading
        state = try {
            val roles = authRepository.session.authorized { token -> roleClient.listRoles(token) }
            when (decideRole(roles)) {
                RoleOutcome.Supervised -> {
                    SupervisedRoleCache.write(context, user.id)
                    HomeState.InSupervisedMode(user)
                }
                RoleOutcome.GrantSupervised -> {
                    grantSupervised(user)
                    HomeState.InSupervisedMode(user)
                }
                RoleOutcome.TutorAccount -> HomeState.TutorAccount(user)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            failureState(user, exception)
        }
    }

    suspend fun signOut() {
        authRepository.signOut()
        SupervisedRoleCache.clear(context)
        state = HomeState.SignedOut()
    }

    fun signIn() {
        scope.launch {
            val user = try {
                authRepository.signIn(context)
            } catch (_: GetCredentialCancellationException) {
                state = HomeState.SignedOut()
                return@launch
            } catch (exception: Exception) {
                state = HomeState.SignedOut(signInErrorMessage(exception))
                return@launch
            }
            resolveRole(user)
        }
    }

    /** Only from [TutorAccountScreen], on the user's explicit request. */
    fun useAsSupervised(current: HomeState.TutorAccount) {
        // Reads the live state, not the captured one: two taps before recomposition send one request.
        if ((state as? HomeState.TutorAccount)?.granting != false) return
        state = current.copy(granting = true, error = null)
        scope.launch {
            state = try {
                grantSupervised(current.user)
                HomeState.InSupervisedMode(current.user)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                val error = exception.toUiError()
                if (error == UiError.SessionExpired) {
                    HomeState.SignedOut(error.message)
                } else {
                    current.copy(granting = false, error = error.message)
                }
            }
        }
    }

    suspend fun restore() {
        when (val result = authRepository.restoreSession()) {
            is RestoreResult.Restored -> resolveRole(result.user)
            RestoreResult.NoSession -> state = HomeState.SignedOut()
            is RestoreResult.Unreachable -> state = HomeState.SignedOut(result.error.message)
        }
    }

    LaunchedEffect(Unit) { restore() }

    // Sprint 41: the session ended by itself (refresh token rejected — revoked, or expired after
    // its lifetime), from any screen or background service. Back to login, with the reason.
    val sessionExpired by authRepository.session.expired.collectAsState()
    LaunchedEffect(sessionExpired) {
        if (sessionExpired && state !is HomeState.SignedOut && state !is HomeState.Loading) {
            state = HomeState.SignedOut(UiError.SessionExpired.message)
        }
    }

    // Only worth checking (and showing) while the user is stuck at the sign-in screen: it's
    // the one place "no se pudo iniciar sesión" is ambiguous between a bad login and a
    // backend that simply isn't reachable yet.
    LaunchedEffect(state, infraAttempt) {
        if (state is HomeState.SignedOut) {
            infraState = InfraState.Checking
            infraState = try {
                InfraState.Ready(healthClient.check()).also {
                    // Sprint 41: a session kept while the backend was unreachable at startup
                    // resumes on its own once the backend answers again.
                    if (authRepository.session.hasStoredSession()) restore()
                }
            } catch (exception: Exception) {
                InfraState.Error(exception.toHealthUiError())
            }
        }
    }

    // Theme background (Sky Ground) behind every screen; the login/loading/role screens paint their own.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (val current = state) {
            HomeState.Loading -> LoadingScreen()
            is HomeState.SignedOut -> LoginScreen(
                // Sprint 63: with no error of its own, the login shows the health check's cause.
                error = current.error ?: (infraState as? InfraState.Error)?.error?.message,
                service = when (infraState) {
                    InfraState.Checking -> ServiceStatus.Checking
                    is InfraState.Ready -> ServiceStatus.Ready
                    is InfraState.Error -> ServiceStatus.Unavailable
                },
                onSignIn = ::signIn,
                onRetryService = { infraAttempt++ },
            )
            is HomeState.TutorAccount -> TutorAccountScreen(
                loading = current.granting,
                error = current.error,
                onUseAsSupervised = { useAsSupervised(current) },
                onSignOut = { scope.launch { signOut() } },
            )
            is HomeState.RoleCheckFailed -> RoleCheckFailedScreen(
                message = current.message,
                onRetry = { scope.launch { resolveRole(current.user) } },
                onSignOut = { scope.launch { signOut() } },
            )
            is HomeState.InSupervisedMode -> SupervisedShell(
                baseUrl = BuildConfig.API_BASE_URL,
                session = authRepository.session,
                onSignOut = ::signOut,
            )
        }
    }
}
