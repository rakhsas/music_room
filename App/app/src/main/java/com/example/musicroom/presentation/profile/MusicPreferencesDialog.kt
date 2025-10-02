package com.example.musicroom.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.musicroom.data.models.MusicPreferences
import com.example.musicroom.presentation.theme.*

@Composable
fun MusicPreferencesDialog(
    musicPreferences: MusicPreferences,
    onDismiss: () -> Unit,
    onSave: (MusicPreferences) -> Unit
) {
    var favoriteGenres by remember { mutableStateOf(musicPreferences.favoriteGenres.toMutableList()) }
    var favoriteArtists by remember { mutableStateOf(musicPreferences.favoriteArtists.toMutableList()) }
    var musicMood by remember { mutableStateOf(musicPreferences.musicMood) }
    var explicitContent by remember { mutableStateOf(musicPreferences.explicitContent) }

    var newGenre by remember { mutableStateOf("") }
    var newArtist by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Music Preferences",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Favorite Genres Section
                Text(
                    text = "Favorite Genres",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(favoriteGenres) { genre ->
                        FilterChip(
                            onClick = {
                                favoriteGenres.remove(genre)
                                favoriteGenres = favoriteGenres.toMutableList()
                            },
                            label = { Text(genre) },
                            selected = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newGenre,
                        onValueChange = { newGenre = it },
                        label = { Text("Add Genre") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryPurple,
                            unfocusedLabelColor = TextSecondary,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                    Button(
                        onClick = {
                            if (newGenre.isNotBlank() && !favoriteGenres.contains(newGenre)) {
                                favoriteGenres.add(newGenre)
                                favoriteGenres = favoriteGenres.toMutableList()
                                newGenre = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Favorite Artists Section
                Text(
                    text = "Favorite Artists",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(favoriteArtists) { artist ->
                        FilterChip(
                            onClick = {
                                favoriteArtists.remove(artist)
                                favoriteArtists = favoriteArtists.toMutableList()
                            },
                            label = { Text(artist) },
                            selected = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Remove",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newArtist,
                        onValueChange = { newArtist = it },
                        label = { Text("Add Artist") },
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedLabelColor = PrimaryPurple,
                            unfocusedLabelColor = TextSecondary,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = TextSecondary
                        )
                    )
                    Button(
                        onClick = {
                            if (newArtist.isNotBlank() && !favoriteArtists.contains(newArtist)) {
                                favoriteArtists.add(newArtist)
                                favoriteArtists = favoriteArtists.toMutableList()
                                newArtist = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Add")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Music Mood Section
                Text(
                    text = "Music Mood",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val moods = listOf("Energetic", "Relaxed", "Happy", "Sad", "Focused", "Party")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(moods) { mood ->
                        FilterChip(
                            onClick = { musicMood = mood },
                            label = { Text(mood) },
                            selected = musicMood == mood
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Explicit Content Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Allow Explicit Content",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Switch(
                        checked = explicitContent,
                        onCheckedChange = { explicitContent = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            onSave(
                                MusicPreferences(
                                    favoriteGenres = favoriteGenres.toList(),
                                    favoriteArtists = favoriteArtists.toList(),
                                    musicMood = musicMood,
                                    explicitContent = explicitContent
                                )
                            )
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}
