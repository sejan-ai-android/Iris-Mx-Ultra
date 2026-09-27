package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.IrisCyanPrimary
import com.example.ui.theme.IrisEmeraldAccent

@Composable
fun AudioWaveformBar(
    isActive: Boolean,
    isSpeaking: Boolean,
    audioRmsDb: Float = 0f,
    modifier: Modifier = Modifier
) {
    val barColor = if (isSpeaking) IrisEmeraldAccent else IrisCyanPrimary
    val barCount = 18

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val transition = rememberInfiniteTransition(label = "WaveTransition")

        for (i in 0 until barCount) {
            val baseDuration = 400 + (i * 45)
            val animatedHeight by transition.animateFloat(
                initialValue = 4f,
                targetValue = if (isActive) {
                    val rmsFactor = (audioRmsDb * 2.5f).coerceIn(4f, 26f)
                    val variance = if (i % 3 == 0) 1.2f else if (i % 2 == 0) 0.8f else 1.0f
                    (rmsFactor * variance).coerceIn(4f, 26f)
                } else if (isSpeaking) {
                    12f + (i % 5) * 2.5f
                } else {
                    4f
                },
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = baseDuration, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "BarHeight_$i"
            )

            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (isActive || isSpeaking) barColor else Color(0xFF26335C))
            )
        }
    }
}
