package com.example.musicroom.presentation.devices

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.musicroom.data.service.DelegatedDevice
import com.example.musicroom.data.service.MyDevice
import com.example.musicroom.presentation.theme.*

/**
 * Music Control Delegation (V.2.2 of the subject): manage which friends can control
 * this account's devices, and send play/pause/skip commands to devices delegated to me.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceControlScreen(
    onBack: () -> Unit,
    viewModel: DeviceControlViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var delegatingDeviceId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadData() }

    LaunchedEffect(uiState.error, uiState.message) {
        (uiState.error ?: uiState.message)?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Music Control Delegation", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("My devices", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Choose which friends can play, pause or skip on each device.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (uiState.myDevices.isEmpty() && !uiState.isLoading) {
                item { Text("No devices registered yet - open the app on this device to register it.", color = TextSecondary) }
            }

            items(uiState.myDevices) { device ->
                MyDeviceCard(
                    device = device,
                    onAddDelegate = { delegatingDeviceId = device.deviceId },
                    onRevoke = { userId -> viewModel.revokeControl(device.deviceId, userId) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Delegated to me", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Devices friends have given you control over.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }

            if (uiState.delegatedToMe.isEmpty() && !uiState.isLoading) {
                item { Text("No one has delegated control to you yet.", color = TextSecondary) }
            }

            items(uiState.delegatedToMe) { device ->
                DelegatedDeviceCard(
                    device = device,
                    onCommand = { action -> viewModel.sendCommand(device.deviceId, action) }
                )
            }
        }
    }

    delegatingDeviceId?.let { deviceId ->
        DelegateEmailDialog(
            onDismiss = { delegatingDeviceId = null },
            onConfirm = { email ->
                viewModel.delegateControl(deviceId, email)
                delegatingDeviceId = null
            }
        )
    }

}

@Composable
private fun MyDeviceCard(
    device: MyDevice,
    onAddDelegate: () -> Unit,
    onRevoke: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(device.name.ifBlank { device.deviceId }, color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(device.platform, color = TextSecondary, fontSize = 12.sp)
                }
                TextButton(onClick = onAddDelegate) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Delegate control", tint = PrimaryPurple)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delegate", color = PrimaryPurple)
                }
            }

            if (device.delegates.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                device.delegates.forEach { delegate ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${delegate.name} (${delegate.email})", color = TextSecondary, fontSize = 13.sp)
                        IconButton(onClick = { onRevoke(delegate.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Revoke", tint = DarkError)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DelegatedDeviceCard(
    device: DelegatedDevice,
    onCommand: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(device.name.ifBlank { device.deviceId }, color = TextPrimary, fontWeight = FontWeight.Bold)
            Text("${device.ownerName}'s device", color = TextSecondary, fontSize = 12.sp)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(onClick = { onCommand("play") }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Play", tint = PrimaryPurple)
                }
                IconButton(onClick = { onCommand("pause") }) {
                    Icon(Icons.Default.Pause, contentDescription = "Pause", tint = PrimaryPurple)
                }
                IconButton(onClick = { onCommand("skip") }) {
                    Icon(Icons.Default.SkipNext, contentDescription = "Skip", tint = PrimaryPurple)
                }
            }
        }
    }
}

@Composable
private fun DelegateEmailDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delegate control") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Friend's email") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(onClick = { if (email.isNotBlank()) onConfirm(email.trim()) }) {
                Text("Grant control")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
