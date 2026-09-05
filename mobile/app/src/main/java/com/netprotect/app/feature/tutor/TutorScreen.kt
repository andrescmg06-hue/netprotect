package com.netprotect.app.feature.tutor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.netprotect.app.core.network.DeviceClient
import com.netprotect.app.core.network.DeviceSummary
import com.netprotect.app.core.network.PairingClient
import com.netprotect.app.core.network.PairingCode
import kotlinx.coroutines.launch

private sealed interface DevicesState {
    data object Loading : DevicesState
    data class Loaded(val devices: List<DeviceSummary>) : DevicesState
    data class Error(val message: String) : DevicesState
}

@Composable
fun TutorScreen(
    baseUrl: String,
    accessToken: String,
    onSignOut: suspend () -> Unit,
    onSwitchMode: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pairingClient = remember { PairingClient(baseUrl) }
    val deviceClient = remember { DeviceClient(baseUrl) }

    var devicesState by remember { mutableStateOf<DevicesState>(DevicesState.Loading) }
    var activeCode by remember { mutableStateOf<PairingCode?>(null) }
    var codeError by remember { mutableStateOf<String?>(null) }
    var renamingDeviceId by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }

    suspend fun reloadDevices() {
        devicesState = try {
            DevicesState.Loaded(deviceClient.listDevices(accessToken))
        } catch (exception: Exception) {
            DevicesState.Error(exception.message ?: "No se pudo cargar la lista")
        }
    }

    LaunchedEffect(Unit) { reloadDevices() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Modo Tutor",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold,
            )
            TextButton(onClick = onSwitchMode) { Text("Cambiar de modo") }
        }
        Spacer(modifier = Modifier.height(20.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF121722),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                val code = activeCode
                if (code == null) {
                    Text("Vincula un dispositivo nuevo generando un código temporal.", color = Color(0xFFABB5C4))
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                codeError = null
                                try {
                                    activeCode = pairingClient.generateCode(accessToken)
                                } catch (exception: Exception) {
                                    codeError = exception.message ?: "No se pudo generar el código"
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1D6E5A)),
                    ) {
                        Text("Generar código de vinculación")
                    }
                } else {
                    Text("Código de vinculación", color = Color(0xFF6BE3BF), fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(code.code, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Válido por ${code.expiresInSeconds / 60} minutos. Uso único.",
                        color = Color(0xFFABB5C4),
                        fontSize = 13.sp,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row {
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { pairingClient.revokeCurrentCode(accessToken) }
                                activeCode = null
                            }
                        }) { Text("Revocar", color = Color(0xFFFFB4AB)) }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { scope.launch { reloadDevices() } }) {
                            Text("¿Ya se vinculó? Actualizar lista")
                        }
                    }
                }
                codeError?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(it, color = Color(0xFFFFB4AB), fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("Dispositivos vinculados", color = Color.White, fontWeight = FontWeight.Bold)
            TextButton(onClick = { scope.launch { reloadDevices() } }) { Text("Actualizar") }
        }
        Spacer(modifier = Modifier.height(8.dp))

        when (val state = devicesState) {
            DevicesState.Loading -> Text("Cargando…", color = Color(0xFFABB5C4))
            is DevicesState.Error -> Text(state.message, color = Color(0xFFFFB4AB))
            is DevicesState.Loaded -> {
                if (state.devices.isEmpty()) {
                    Text("Todavía no hay dispositivos vinculados.", color = Color(0xFFABB5C4))
                } else {
                    LazyColumn {
                        items(state.devices, key = { it.id }) { device ->
                            DeviceRow(
                                device = device,
                                isRenaming = renamingDeviceId == device.id,
                                renameText = renameText,
                                onRenameTextChange = { renameText = it },
                                onStartRename = {
                                    renamingDeviceId = device.id
                                    renameText = device.name
                                },
                                onConfirmRename = {
                                    scope.launch {
                                        runCatching {
                                            deviceClient.renameDevice(accessToken, device.id, renameText)
                                        }
                                        renamingDeviceId = null
                                        reloadDevices()
                                    }
                                },
                                onCancelRename = { renamingDeviceId = null },
                                onUnlink = {
                                    scope.launch {
                                        runCatching { pairingClient.unlinkDevice(accessToken, device.id) }
                                        reloadDevices()
                                    }
                                },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = { scope.launch { onSignOut() } }) {
            Text("Cerrar sesión", color = Color(0xFFABB5C4))
        }
    }
}

@Composable
private fun DeviceRow(
    device: DeviceSummary,
    isRenaming: Boolean,
    renameText: String,
    onRenameTextChange: (String) -> Unit,
    onStartRename: () -> Unit,
    onConfirmRename: () -> Unit,
    onCancelRename: () -> Unit,
    onUnlink: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF121722),
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (isRenaming) {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = onRenameTextChange,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    TextButton(onClick = onConfirmRename) { Text("Guardar") }
                    TextButton(onClick = onCancelRename) { Text("Cancelar") }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(device.name, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(device.platform, color = Color(0xFF7D899A), fontSize = 12.sp)
                    }
                    StatusPill(status = device.status)
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row {
                    TextButton(onClick = onStartRename) { Text("Renombrar") }
                    TextButton(onClick = onUnlink) { Text("Desvincular", color = Color(0xFFFFB4AB)) }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: String) {
    val color = when (status) {
        "ONLINE" -> Color(0xFF6BE3BF)
        "OFFLINE" -> Color(0xFF7D899A)
        "ALERT" -> Color(0xFFFFB4AB)
        else -> Color(0xFFABB5C4)
    }
    Text(status, color = color, fontWeight = FontWeight.Bold, fontSize = 12.sp)
}
