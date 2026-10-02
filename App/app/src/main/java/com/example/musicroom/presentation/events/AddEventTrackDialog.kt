package com.example.musicroom.presentation.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicroom.data.models.Song
import com.example.musicroom.data.service.MusicApiService
import com.example.musicroom.presentation.theme.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AddEventTrackUiState {
    object Idle : AddEventTrackUiState()
    object Loading : AddEventTrackUiState()
    data class Results(val songs: List<Song>) : AddEventTrackUiState()
    data class Error(val message: String) : AddEventTrackUiState()
}

@HiltViewModel
class AddEventTrackViewModel @Inject constructor(
    private val musicApiService: MusicApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow<AddEventTrackUiState>(AddEventTrackUiState.Idle)
    val uiState: StateFlow<AddEventTrackUiState> = _uiState.asStateFlow()

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = AddEventTrackUiState.Idle
            return
        }
        viewModelScope.launch {
            _uiState.value = AddEventTrackUiState.Loading
            musicApiService.searchSongsByName(query).fold(
                onSuccess = { songs -> _uiState.value = AddEventTrackUiState.Results(songs) },
                onFailure = { e -> _uiState.value = AddEventTrackUiState.Error(e.message ?: "Search failed") }
            )
        }
    }
}

@Composable
fun AddEventTrackDialog(
    onDismiss: () -> Unit,
    onAddTrack: (song: Song, onSuccess: () -> Unit, onError: (String) -> Unit) -> Unit,
    viewModel: AddEventTrackViewModel = hiltViewModel()
) {
    var query by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()
    var addingTrackId by remember { mutableStateOf<String?>(null) }
    var addedTrackIds by remember { mutableStateOf(setOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add a Track",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        viewModel.search(it)
                    },
                    label = { Text("Search for a song or artist") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = PrimaryPurple) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryPurple,
                        focusedLabelColor = PrimaryPurple,
                        cursorColor = PrimaryPurple
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                errorMessage?.let { error ->
                    Text(text = error, color = Color.Red, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when (val state = uiState) {
                        is AddEventTrackUiState.Idle -> {
                            Text(
                                text = "Search for a track to add it to this event",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is AddEventTrackUiState.Loading -> {
                            CircularProgressIndicator(
                                color = PrimaryPurple,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is AddEventTrackUiState.Error -> {
                            Text(
                                text = state.message,
                                color = Color.Red,
                                fontSize = 14.sp,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        is AddEventTrackUiState.Results -> {
                            if (state.songs.isEmpty()) {
                                Text(
                                    text = "No results for \"$query\"",
                                    color = TextSecondary,
                                    fontSize = 14.sp,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(state.songs, key = { it.id }) { song ->
                                        SearchedSongRow(
                                            song = song,
                                            isAdding = addingTrackId == song.id,
                                            isAdded = song.id in addedTrackIds,
                                            onAdd = {
                                                addingTrackId = song.id
                                                onAddTrack(
                                                    song,
                                                    {
                                                        addingTrackId = null
                                                        addedTrackIds = addedTrackIds + song.id
                                                    },
                                                    { error ->
                                                        addingTrackId = null
                                                        errorMessage = error
                                                    }
                                                )
                                            }
                                        )
                                    }
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
private fun SearchedSongRow(
    song: Song,
    isAdding: Boolean,
    isAdded: Boolean,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.name,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist_name,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        when {
            isAdding -> CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = PrimaryPurple,
                strokeWidth = 2.dp
            )
            isAdded -> Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Added",
                tint = PrimaryPurple
            )
            else -> IconButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to event",
                    tint = PrimaryPurple
                )
            }
        }
    }
}
