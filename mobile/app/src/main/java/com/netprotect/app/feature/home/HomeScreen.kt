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
import androidx.compose.runtime.getValue
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
import com.netprotect.app.core.auth.RolePreference
import com.netprotect.app.core.network.CurrentUser
import com.netprotect.app.core.network.InfrastructureHealth
import com.netprotect.app.core.network.InfrastructureHealthClient
import com.netprotect.app.core.network.RoleClient
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
        val accessToken = authRepository.accessToken ?: run {
            state = HomeState.SignedOut()
            return
        }
        scope.launch {
            state = try {
                roleClient.selectRole(accessToken, roleCode)
                RolePreference.write(context, roleCode)
                if (roleCode == ROLE_TUTOR) HomeState.InTutorMode(user) else HomeState.InSupervisedMode(user)
            } catch (exception: Exception) {
                HomeState.SelectingRole(user, exception.message ?: "No se pudo guardar el modo")
            }
        }
    }

    fun switchMode(user: CurrentUser) {
        RolePreference.clear(context)
        state = HomeState.SelectingRole(user)
    }

    LaunchedEffect(Unit) {
        state = authRepository.restoreSession()?.let(::stateAfterSignIn) ?: HomeState.SignedOut()
    }

    // Only worth checking (and showing) while the user is stuck at the sign-in screen: it's
    // the one place "no se pudo iniciar sesión" is ambiguous between a bad login and a
    // backend that simply isn't reachable yet.
    LaunchedEffect(state) {
        if (state is HomeState.SignedOut) {
            infraState = try {
                InfraState.Ready(healthClient.check())
            } catch (exception: Exception) {
                InfraState.Error(exception.message ?: "No fue posible contactar la API")
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF090B10)) {
        when (val current = state) {
            HomeState.Loading -> LoadingContent()
            is HomeState.SignedOut -> SignedOutContent(
                error = current.error,
                infraState = infraState,
                onSignIn = ::signIn,
            )
            is HomeState.SelectingRole -> RoleSelectionContent(
                user = current.user,
                error = current.error,
                onSelectTutor = { selectRole(current.user, ROLE_TUTOR) },
                onSelectSupervised = { selectRole(current.user, ROLE_SUPERVISADO) },
                onSignOut = { scope.launch { signOut() } },
            )
            is HomeState.InTutorMode -> TutorScreen(
                baseUrl = BuildConfig.API_BASE_URL,
                accessToken = authRepository.accessToken.orEmpty(),
                onSignOut = ::signOut,
                onSwitchMode = { switchMode(current.user) },
            )
            is HomeState.InSupervisedMode -> SupervisedScreen(
                baseUrl = BuildConfig.API_BASE_URL,
                accessToken = authRepository.accessToken.orEmpty(),
                onSignOut = ::signOut,
                onSwitchMode = { switchMode(current.user) },
            )
        }
    }
}

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Cargando…", color = Color.White, fontSize = 18.sp)
    }
}

@Composable
private fun SignedOutContent(error: String?, infraState: InfraState, onSignIn: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "NETPROTECT",
            color = Color(0xFF6BE3BF),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Control parental",
            color = Color.White,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Inicia sesión con tu cuenta de Google para continuar como tutor o como " +
                "dispositivo supervisado.",
            color = Color(0xFFABB5C4),
            fontSize = 16.sp,
            lineHeight = 23.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121722),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                error?.let {
                    Text(it, color = Color(0xFFFFB4AB), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1D6E5A),
                        contentColor = Color.White,
                    ),
                ) {
                    Text("Iniciar sesión con Google")
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        InfraStatusLine(infraState)
    }
}

@Composable
private fun InfraStatusLine(state: InfraState) {
    when (state) {
        InfraState.Checking -> Text(
            "Comprobando conexión con el servidor…",
            color = Color(0xFF7D899A),
            fontSize = 12.sp,
        )
        is InfraState.Error -> Text(
            "Servidor no disponible: ${state.message}",
            color = Color(0xFFFFB4AB),
            fontSize = 12.sp,
        )
        is InfraState.Ready -> Text(
            "Servidor: ${state.health.backend.uppercase()} · " +
                "BD: ${state.health.database.uppercase()} · " +
                "Redis: ${state.health.redis.uppercase()}",
            color = Color(0xFF6BE3BF),
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun RoleSelectionContent(
    user: CurrentUser,
    error: String?,
    onSelectTutor: () -> Unit,
    onSelectSupervised: () -> Unit,
    onSignOut: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Hola, ${user.displayName ?: user.email}",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "¿Cómo vas a usar este dispositivo?",
            color = Color(0xFFABB5C4),
            fontSize = 16.sp,
        )
        Spacer(modifier = Modifier.height(24.dp))

        RoleOptionCard(
            title = "Soy tutor",
            description = "Superviso otros dispositivos desde este teléfono o tablet.",
            onClick = onSelectTutor,
        )
        Spacer(modifier = Modifier.height(14.dp))
        RoleOptionCard(
            title = "Este es el dispositivo supervisado",
            description = "Este teléfono es el que un tutor va a supervisar.",
            onClick = onSelectSupervised,
        )

        error?.let {
            Spacer(modifier = Modifier.height(14.dp))
            Text(it, color = Color(0xFFFFB4AB), fontSize = 13.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onSignOut) {
                Text("Cerrar sesión", color = Color(0xFFABB5C4))
            }
        }
    }
}

@Composable
private fun RoleOptionCard(title: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121722),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(description, color = Color(0xFFABB5C4), fontSize = 14.sp, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1D6E5A),
                    contentColor = Color.White,
                ),
            ) {
                Text("Elegir")
            }
        }
    }
}
