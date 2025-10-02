package com.example.musicroom.presentation.events

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.service.EventsApiService
import com.example.musicroom.data.service.User
import com.example.musicroom.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI States for invite users
 */
sealed class InviteUsersUiState {
    object Loading : InviteUsersUiState()
    data class Success(val users: List<User>) : InviteUsersUiState()
    data class Error(val message: String) : InviteUsersUiState()
}

sealed class InviteResult {
    data class Success(val userName: String) : InviteResult()
    data class Error(val message: String) : InviteResult()
}

@HiltViewModel
class InviteUsersViewModel @Inject constructor(
    private val eventsApiService: EventsApiService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<InviteUsersUiState>(InviteUsersUiState.Loading)
    val uiState: StateFlow<InviteUsersUiState> = _uiState.asStateFlow()
    
    private val _inviteResult = MutableStateFlow<InviteResult?>(null)
    val inviteResult: StateFlow<InviteResult?> = _inviteResult.asStateFlow()
    
    private val _isInviting = MutableStateFlow(false)
    val isInviting: StateFlow<Boolean> = _isInviting.asStateFlow()
    
    init {
        loadUsers()
    }
    
    fun loadUsers() {
        viewModelScope.launch {
            try {
                _uiState.value = InviteUsersUiState.Loading
                Log.d("InviteUsersVM", "👥 Loading users for invitation")
                
                val result = eventsApiService.getAllUsers()
                if (result.isSuccess) {
                    val users = result.getOrThrow()
                    Log.d("InviteUsersVM", "✅ Loaded ${users.size} users")
                    _uiState.value = InviteUsersUiState.Success(users)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("InviteUsersVM", "❌ Failed to load users: $error")
                    _uiState.value = InviteUsersUiState.Error("Failed to load users: $error")
                }
                
            } catch (e: Exception) {
                Log.e("InviteUsersVM", "❌ Error loading users", e)
                _uiState.value = InviteUsersUiState.Error("Failed to load users: ${e.message}")
            }
        }
    }
    
    fun inviteUser(eventId: String, user: User, role: String = "attendee") {
        viewModelScope.launch {
            try {
                _isInviting.value = true
                Log.d("InviteUsersVM", "📧 Inviting user ${user.name} to event $eventId")
                
                val result = eventsApiService.inviteUserToEvent(eventId, user.id, role)
                if (result.isSuccess) {
                    Log.d("InviteUsersVM", "✅ Successfully invited user ${user.name}")
                    _inviteResult.value = InviteResult.Success(user.name)
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    Log.e("InviteUsersVM", "❌ Failed to invite user: $error")
                    _inviteResult.value = InviteResult.Error("Failed to invite ${user.name}: $error")
                }
                
            } catch (e: Exception) {
                Log.e("InviteUsersVM", "❌ Error inviting user", e)
                _inviteResult.value = InviteResult.Error("Failed to invite ${user.name}: ${e.message}")
            } finally {
                _isInviting.value = false
            }
        }
    }
    
    fun clearInviteResult() {
        _inviteResult.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteUsersDialog(
    eventId: String,
    eventTitle: String,
    onDismiss: () -> Unit,
    viewModel: InviteUsersViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val inviteResult by viewModel.inviteResult.collectAsState()
    val isInviting by viewModel.isInviting.collectAsState()
    
    var selectedRole by remember { mutableStateOf("attendee") }
    val roles = mapOf(
        "attendee" to "Attendee",
        "manager" to "Manager"
    )
    
    // Handle invite result
    LaunchedEffect(inviteResult) {
        inviteResult?.let { result ->
            when (result) {
                is InviteResult.Success -> {
                    // Show success for 2 seconds then clear
                    kotlinx.coroutines.delay(2000)
                    viewModel.clearInviteResult()
                }
                is InviteResult.Error -> {
                    // Show error for 3 seconds then clear
                    kotlinx.coroutines.delay(3000)
                    viewModel.clearInviteResult()
                }
            }
        }
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Invite Users",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "to $eventTitle",
                            color = TextSecondary,
                            fontSize = 14.sp
                        )
                    }
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Role selection
                Text(
                    text = "Invite as:",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    roles.forEach { (roleKey, roleDisplayName) ->
                        val isSelected = selectedRole == roleKey
                        FilterChip(
                            onClick = { selectedRole = roleKey },
                            label = {
                                Text(
                                    text = roleDisplayName,
                                    color = if (isSelected) Color.White else TextSecondary
                                )
                            },
                            selected = isSelected,
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = if (isSelected) PrimaryPurple else DarkBackground,
                                selectedContainerColor = PrimaryPurple
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Invite result notification
                inviteResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (result) {
                                is InviteResult.Success -> Color.Green.copy(alpha = 0.1f)
                                is InviteResult.Error -> Color.Red.copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (result) {
                                    is InviteResult.Success -> Icons.Default.CheckCircle
                                    is InviteResult.Error -> Icons.Default.Error
                                },
                                contentDescription = null,
                                tint = when (result) {
                                    is InviteResult.Success -> Color.Green
                                    is InviteResult.Error -> Color.Red
                                },
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (result) {
                                    is InviteResult.Success -> "Invited ${result.userName} successfully!"
                                    is InviteResult.Error -> result.message
                                },
                                color = when (result) {
                                    is InviteResult.Success -> Color.Green
                                    is InviteResult.Error -> Color.Red
                                },
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Users list
                when (val currentState = uiState) {
                    is InviteUsersUiState.Loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PrimaryPurple)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Loading users...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    is InviteUsersUiState.Success -> {
                        if (currentState.users.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "No users available",
                                        color = TextSecondary,
                                        fontSize = 16.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(currentState.users) { user ->
                                    UserCard(
                                        user = user,
                                        role = selectedRole,
                                        isInviting = isInviting,
                                        onInvite = { viewModel.inviteUser(eventId, user, selectedRole) }
                                    )
                                }
                            }
                        }
                    }
                    
                    is InviteUsersUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error",
                                    tint = Color.Red,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Failed to load users",
                                    color = Color.Red,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = currentState.message,
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.loadUsers() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Text("Try Again")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UserCard(
    user: User,
    role: String,
    isInviting: Boolean,
    onInvite: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // User avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    color = PrimaryPurple,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // User info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user.name,
                    color = TextPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (user.username.isNotBlank()) {
                    Text(
                        text = "@${user.username}",
                        color = TextSecondary,
                        fontSize = 8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Invite button
            Button(
                onClick = onInvite,
                enabled = !isInviting,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                modifier = Modifier.height(36.dp)
            ) {
                if (isInviting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Invite",
                        modifier = Modifier.size(16.dp)
                    )
                    // Spacer(modifier = Modifier.width(4.dp))
                    // Text("Invite", fontSize = 12.sp)
                }
            }
        }
    }
}