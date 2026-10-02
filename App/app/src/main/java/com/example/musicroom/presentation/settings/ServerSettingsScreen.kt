package com.example.musicroom.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.musicroom.data.network.NetworkConfig
import com.example.musicroom.presentation.theme.*

/**
 * Lets a tester point the app at any backend at runtime, without rebuilding
 * ("the back-end's address must be configurable on the application for tests" - V.5).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSettingsScreen(onBack: () -> Unit) {
    var urlText by remember { mutableStateOf(NetworkConfig.getBaseUrlOverride() ?: NetworkConfig.BASE_URL) }
    var savedMessage by remember { mutableStateOf<String?>(null) }
    val isOverridden = NetworkConfig.getBaseUrlOverride() != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryTeal,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundGradient)
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Point this app at a different backend for testing (e.g. a local server or a QA " +
                    "environment). Changes apply immediately and persist across restarts.",
                color = TextSecondary
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = GlassWhite),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        if (isOverridden) "Using a custom backend" else "Using the built-in backend",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("Backend URL") },
                        placeholder = { Text("http://192.168.1.10:8000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryTeal,
                            focusedLabelColor = PrimaryTeal,
                            cursorColor = PrimaryTeal
                        )
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = {
                                NetworkConfig.setBaseUrlOverride(urlText)
                                savedMessage = "Backend URL saved: ${NetworkConfig.BASE_URL}"
                            },
                            enabled = urlText.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                        ) {
                            Text("Save")
                        }

                        OutlinedButton(onClick = {
                            NetworkConfig.setBaseUrlOverride(null)
                            urlText = NetworkConfig.getDefaultBaseUrl()
                            savedMessage = "Reset to default: ${NetworkConfig.getDefaultBaseUrl()}"
                        }) {
                            Text("Reset to default")
                        }
                    }

                    savedMessage?.let {
                        Text(it, color = PrimaryTeal)
                    }
                }
            }
        }
    }
}
