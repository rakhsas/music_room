package com.example.musicroom.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.musicroom.presentation.theme.DarkError
import kotlinx.coroutines.delay

/**
 * Auth error toast: pinned to the top so it's visible without scrolling, and
 * self-dismisses after a few seconds instead of sitting there until the next
 * screen's stale state overwrites it.
 */
@Composable
fun BoxScope.AuthErrorBanner(authState: AuthState, onDismiss: () -> Unit) {
    LaunchedEffect(authState) {
        if (authState is AuthState.Error) {
            delay(2500)
            onDismiss()
        }
    }

    AnimatedVisibility(
        visible = authState is AuthState.Error,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 48.dp, start = 16.dp, end = 16.dp),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        val message = (authState as? AuthState.Error)?.message.orEmpty()
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkError),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.White)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = message, color = Color.White)
            }
        }
    }
}
