package com.example.musicroom.presentation.player

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AudioVisualizer(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val animationState = remember { mutableStateOf(0f) }
    
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val animation = TargetBasedAnimation(
                animationSpec = infiniteRepeatable(
                    animation = tween(2000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                typeConverter = Float.VectorConverter,
                initialValue = 0f,
                targetValue = 1f
            )
            
            val startTime = withFrameNanos { it }
            
            do {
                val playTime = withFrameNanos { it } - startTime
                val animationValue = animation.getValueFromNanos(playTime)
                animationState.value = animationValue
            } while (isPlaying && animation.isInfinite)
        }
    }
    
    Canvas(modifier = modifier) {
        if (isPlaying) {
            drawAudioBars(animationState.value)
        }
    }
}

private fun DrawScope.drawAudioBars(animationValue: Float) {
    val barCount = 20
    val barWidth = size.width / (barCount * 2)
    val maxHeight = size.height
    
    for (i in 0 until barCount) {
        val x = i * barWidth * 2 + barWidth / 2
        
        // Create different frequencies for each bar
        val frequency = (i + 1) * 0.5f
        val height = maxHeight * (0.3f + 0.7f * abs(sin(animationValue * 6.28f * frequency)))
        
        val y = (size.height - height) / 2
        
        drawRect(
            color = Color(0xFF1DB954),
            topLeft = androidx.compose.ui.geometry.Offset(x, y),
            size = androidx.compose.ui.geometry.Size(barWidth, height)
        )
    }
}
