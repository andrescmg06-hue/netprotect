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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.auth.AuthRepository
import com.netprotect.app.core.auth.RestoreResult
import com.netprotect.app.core.auth.authorized
import com.netprotect.app.core.auth.RolePreference
import com.netprotect.app.core.network.CurrentUser
import com.netprotect.app.core.network.InfrastructureHealth
import com.netprotect.app.core.network.InfrastructureHealthClient
import com.netprotect.app.core.network.RoleClient
import com.netprotect.app.core.network.UiError
import com.netprotect.app.core.network.toUiError
import com.netprotect.app.feature.supervised.SupervisedScreen
import com.netprotect.app.feature.tutor.TutorScreen
import kotlinx.coroutines.launch

private const val ROLE_TUTOR = "TUTOR"
private const val ROLE_SUPERVISADO = "SUPERVISADO"

private sealed interface HomeState {
    data object Loading : HomeState
    data class SignedOut(val error: String? = null) : HomeState
    data class SelectingRole(val user: CurrentUser, val error: String? = null) : HomeState
    data class InTutorMode(val user: CurrentUser) : HomeState
    data class InSupervisedMode(val user: CurrentUser) : HomeState
}

private sealed interface InfraState {
    data object Checking : InfraState
    data class Ready(val health: InfrastructureHealth) : InfraState
    data class Error(val message: String) : InfraState
}

/** Top-level router: Loading -> SignedOut -> SelectingRole -> TutorMode/SupervisedMode.
 *
 * The role choice is a local shortcut ([RolePreference]) remembered per install so returning
 * users skip straight to their mode; the backend grant (via [RoleClient]) is what actually
 * authorizes tutor/supervised actions.
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

    fun stateAfterSignIn(user: CurrentUser): HomeState =
        when (RolePreference.read(context)) {
            ROLE_TUTOR -> HomeState.InTutorMode(user)
            ROLE_SUPERVISADO -> HomeState.InSupervisedMode(user)
            else -> HomeState.SelectingRole(user)
        }

    suspend fun signOut() {
        authRepository.signOut()
        RolePreference.clear(context)
        state = HomeState.SignedOut()
    }

    fun signIn() {
        scope.launch {
            state = try {
                stateAfterSignIn(authRepository.signIn(context))
            } catch (_: GetCredentialCancellationException) {
                HomeState.SignedOut()
            } catch (exception: Exception) {
                HomeState.SignedOut(exception.message ?: "No se pudo iniciar sesión")
            }
        }
    }

    fun selectRole(user: CurrentUser, roleCode: String) {
        scope.launch {
            state = try {
                authRepository.session.authorized { token -> roleClient.selectRole(token, roleCode) }
                RolePreference.write(context, roleCode)
                if (roleCode == ROLE_TUTOR) HomeState.InTutorMode(user) else HomeState.InSupervisedMode(user)
            } catch (exception: Exception) {
                HomeState.SelectingRole(user, exception.toUiError().message)
            }
        }
    }

    fun switchMode(user: CurrentUser) {
        RolePreference.clear(context)
        state = HomeState.SelectingRole(user)
    }

    suspend fun restore() {
        state = when (val result = authRepository.restoreSession()) {
            is RestoreResult.Restored -> stateAfterSignIn(result.user)
            RestoreResult.NoSession -> HomeState.SignedOut()
            RestoreResult.Unreachable -> HomeState.SignedOut(UiError.Offline.message)
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
                InfraState.Error(exception.message ?: "No fue posible contactar la API")
            }
        }
    }

    // The Surface keeps the old dark colour only for the Tutor/Supervised screens that have not been
    // redesigned yet (S44/S48); the three screens below paint their own light background.
    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B10)) {
        when (val current = state) {
            HomeState.Loading -> LoadingScreen()
            is HomeState.SignedOut -> LoginScreen(
                error = current.error,
                service = when (infraState) {
                    InfraState.Checking -> ServiceStatus.Checking
                    is InfraState.Ready -> ServiceStatus.Ready
                    is InfraState.Error -> ServiceStatus.Unavailable
                },
                onSignIn = ::signIn,
                onRetryService = { infraAttempt++ },
            )
            is HomeState.SelectingRole -> RoleSelectionScreen(
                displayName = current.user.displayName,
                email = current.user.email,
                error = current.error,
                onSelectTutor = { selectRole(current.user, ROLE_TUTOR) },
                onSelectSupervised = { selectRole(current.user, ROLE_SUPERVISADO) },
                onSignOut = { scope.launch { signOut() } },
            )
            is HomeState.InTutorMode -> TutorScreen(
                baseUrl = BuildConfig.API_BASE_URL,
                session = authRepository.session,
                onSignOut = ::signOut,
                onSwitchMode = { switchMode(current.user) },
            )
            is HomeState.InSupervisedMode -> SupervisedScreen(
                baseUrl = BuildConfig.API_BASE_URL,
                session = authRepository.session,
                onSignOut = ::signOut,
                onSwitchMode = { switchMode(current.user) },
            )
        }
    }
}
