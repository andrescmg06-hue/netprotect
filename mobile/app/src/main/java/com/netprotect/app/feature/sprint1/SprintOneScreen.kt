package com.netprotect.app.feature.sprint1

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.BuildConfig
import com.netprotect.app.core.network.InfrastructureHealth
import com.netprotect.app.core.network.InfrastructureHealthClient
import kotlinx.coroutines.launch

private sealed interface InfrastructureState {
    data object Checking : InfrastructureState
    data class Ready(val health: InfrastructureHealth) : InfrastructureState
    data class Error(val message: String) : InfrastructureState
}

@Composable
fun SprintOneScreen() {
    val client = remember { InfrastructureHealthClient(BuildConfig.API_BASE_URL) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<InfrastructureState>(InfrastructureState.Checking) }

    suspend fun checkInfrastructure() {
        state = InfrastructureState.Checking
        state = try {
            InfrastructureState.Ready(client.check())
        } catch (exception: Exception) {
            InfrastructureState.Error(exception.message ?: "No fue posible contactar la API")
        }
    }

    LaunchedEffect(Unit) {
        checkInfrastructure()
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF090B10),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "NETPROTECT · SPRINT 1",
                color = Color(0xFF6BE3BF),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Arquitectura y entorno",
                color = Color.White,
                fontSize = 34.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Una sola aplicación Android para los futuros modos Tutor y Supervisado. " +
                    "En este sprint se valida Android → Backend → PostgreSQL y la disponibilidad de Redis.",
                color = Color(0xFFABB5C4),
                fontSize = 17.sp,
                lineHeight = 25.sp,
            )
            Spacer(modifier = Modifier.height(28.dp))

            StatusCard(state)

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "API: ${BuildConfig.API_BASE_URL}",
                color = Color(0xFF7D899A),
                fontSize = 12.sp,
            )
            Spacer(modifier = Modifier.height(18.dp))
            Button(
                onClick = { scope.launch { checkInfrastructure() } },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1D6E5A),
                    contentColor = Color.White,
                ),
            ) {
                Text("Volver a comprobar")
            }
        }
    }
}

@Composable
private fun StatusCard(state: InfrastructureState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121722),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            when (state) {
                InfrastructureState.Checking -> {
                    Text("Comprobando infraestructura…", color = Color.White)
                }
                is InfrastructureState.Error -> {
                    Text(
                        text = "Infraestructura no disponible",
                        color = Color(0xFFFFB4AB),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(state.message, color = Color(0xFFABB5C4))
                }
                is InfrastructureState.Ready -> {
                    Text(
                        text = "Incremento funcional",
                        color = Color(0xFF6BE3BF),
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    StatusRow("Android → Backend", state.health.backend)
                    StatusRow("Backend → PostgreSQL", state.health.database)
                    StatusRow("Backend → Redis", state.health.redis)
                }
            }
        }
    }
}

@Composable
private fun StatusRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, color = Color(0xFFD8E0EA))
        Text(value.uppercase(), color = Color(0xFF6BE3BF), fontWeight = FontWeight.Bold)
    }
}
