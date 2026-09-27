package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NeuralOrbVisualizer(
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    isSpeaking: Boolean = false,
    audioRmsDb: Float = 0f,
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    // Rotation angle
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isProcessing) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "OrbRotation"
    )

    // Pulse scale
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "OrbPulse"
    )

    // Dynamic color depending on state
    val coreGlowColor = when {
        isListening -> IrisAlertRed // Microphone active
        isProcessing -> IrisVioletSecondary // Swarm reasoning
        isSpeaking -> IrisEmeraldAccent // Neural voice active
        else -> IrisCyanPrimary // Idle ready
    }

    Box(
        modifier = modifier
            .size(size)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = (this.size.minDimension / 2f) * 0.72f * pulse
            val rmsBoost = (audioRmsDb * 3f).coerceIn(0f, 25f)
            val dynamicRadius = baseRadius + rmsBoost

            // 1. Outer Holographic Glow Aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(coreGlowColor.copy(alpha = 0.35f), Color.Transparent),
                    center = center,
                    radius = dynamicRadius * 1.35f
                ),
                radius = dynamicRadius * 1.35f,
                center = center
            )

            // 2. Rotating Segmented Cybernetic Rings
            val ringRadius = dynamicRadius * 0.92f
            val segments = 12
            val sweepAngle = 20f
            for (i in 0 until segments) {
                val startAngle = rotation + (i * (360f / segments))
                drawArc(
                    color = coreGlowColor.copy(alpha = if (i % 2 == 0) 0.85f else 0.45f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = androidx.compose.ui.geometry.Size(ringRadius * 2, ringRadius * 2)
                )
            }

            // 3. Counter-Rotating Inner Cybernetic Ring
            val innerRingRadius = dynamicRadius * 0.68f
            for (i in 0 until 8) {
                val startAngle = -rotation * 1.5f + (i * (360f / 8))
                drawArc(
                    color = IrisCyanPrimary.copy(alpha = 0.6f),
                    startAngle = startAngle,
                    sweepAngle = 25f,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                    topLeft = Offset(center.x - innerRingRadius, center.y - innerRingRadius),
                    size = androidx.compose.ui.geometry.Size(innerRingRadius * 2, innerRingRadius * 2)
                )
            }

            // 4. Central Neural Iris Core
            val pupilRadius = dynamicRadius * 0.42f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        coreGlowColor,
                        coreGlowColor.copy(alpha = 0.8f),
                        IrisBackground
                    ),
                    center = center,
                    radius = pupilRadius
                ),
                radius = pupilRadius,
                center = center
            )

            // 5. Orbital Particles / Neural Synapse Nodes
            val particleCount = 6
            for (p in 0 until particleCount) {
                val angle = Math.toRadians((rotation * 2 + (p * 60)).toDouble())
                val orbitDist = dynamicRadius * 0.82f
                val px = center.x + (orbitDist * cos(angle)).toFloat()
                val py = center.y + (orbitDist * sin(angle)).toFloat()

                drawCircle(
                    color = Color.White,
                    radius = 2.5.dp.toPx(),
                    center = Offset(px, py)
                )
            }
        }
    }
}
