package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.WeatherSceneType
import com.example.ui.sensor.TiltVector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Provides the live device tilt/rotation vector from MainActivity's SensorEventListener
 * to any glass card or interactive 3D button in the composition tree.
 */
val LocalDeviceTilt = compositionLocalOf { TiltVector() }

@Composable
fun FrostedGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    containerAlpha: Float = 0.14f,
    accentTint: Color = Color.White,
    enable3DPerspective: Boolean = true,
    content: @Composable BoxScope.() -> Unit
) {
    val tilt = LocalDeviceTilt.current
    val shape = RoundedCornerShape(cornerRadius)
    val perspectiveModifier = if (enable3DPerspective) {
        Modifier.graphicsLayer {
            rotationX = (tilt.pitch * 4.5f).coerceIn(-7f, 7f)
            rotationY = (tilt.roll * 5.5f).coerceIn(-8f, 8f)
            translationX = tilt.roll * 7f
            translationY = -tilt.pitch * 6f
            cameraDistance = 16f * density
        }
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .then(perspectiveModifier)
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = containerAlpha + 0.04f),
                        Color(0xFF0F172A).copy(alpha = 0.48f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentTint.copy(alpha = 0.38f),
                        Color.White.copy(alpha = 0.08f)
                    )
                ),
                shape = shape
            )
            .padding(16.dp),
        content = content
    )
}

/**
 * Custom animated vector/canvas icon matching each of the 8 WeatherSceneType conditions.
 */
@Composable
fun AnimatedWeatherConditionIcon(
    sceneType: WeatherSceneType,
    modifier: Modifier = Modifier.size(36.dp)
) {
    val transition = rememberInfiniteTransition(label = "icon_anim")
    val animAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "icon_angle"
    )
    val bobbing by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_bob"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val r = minOf(w, h) * 0.28f

        when (sceneType) {
            WeatherSceneType.SUNNY -> {
                rotate(degrees = animAngle, pivot = Offset(cx, cy)) {
                    for (i in 0 until 8) {
                        val rad = (i * 45.0) * PI / 180.0
                        val sx = cx + cos(rad).toFloat() * (r * 1.25f)
                        val sy = cy + sin(rad).toFloat() * (r * 1.25f)
                        val ex = cx + cos(rad).toFloat() * (r * 1.65f)
                        val ey = cy + sin(rad).toFloat() * (r * 1.65f)
                        drawLine(
                            color = Color(0xFFFBBF24),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = 2.5f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))
                    ),
                    radius = r,
                    center = Offset(cx, cy)
                )
            }
            WeatherSceneType.CLOUDY -> {
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = r * 0.75f,
                    center = Offset(cx - r * 0.45f, cy - r * 0.35f)
                )
                drawCloudShape(cx + bobbing, cy + r * 0.15f, r, Color(0xFFE2E8F0))
            }
            WeatherSceneType.RAINY -> {
                drawCloudShape(cx, cy - r * 0.2f, r, Color(0xFF94A3B8))
                for (i in -1..1) {
                    val rx = cx + i * r * 0.55f
                    val ry = cy + r * 0.55f + ((animAngle * 0.08f + i * 3f) % 6f)
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(rx, ry),
                        end = Offset(rx - 3f, ry + r * 0.55f),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherSceneType.THUNDERSTORM -> {
                drawCloudShape(cx, cy - r * 0.25f, r, Color(0xFF64748B))
                val bolt = Path().apply {
                    moveTo(cx + 2f, cy + r * 0.15f)
                    lineTo(cx - 5f, cy + r * 0.75f)
                    lineTo(cx + 1f, cy + r * 0.75f)
                    lineTo(cx - 3f, cy + r * 1.35f)
                }
                drawPath(
                    path = bolt,
                    color = Color(0xFFFACC15),
                    style = Stroke(width = 2.8f, cap = StrokeCap.Round)
                )
            }
            WeatherSceneType.SNOWY -> {
                drawCloudShape(cx, cy - r * 0.2f, r, Color(0xFFCBD5E1))
                for (i in -1..1) {
                    drawCircle(
                        color = Color(0xFF67E8F9),
                        radius = 3.0f,
                        center = Offset(cx + i * r * 0.55f, cy + r * 0.8f + bobbing)
                    )
                }
            }
            WeatherSceneType.FOGGY -> {
                for (i in -1..1) {
                    val y = cy + i * r * 0.55f
                    drawLine(
                        color = Color(0xFFCBD5E1),
                        start = Offset(cx - r * 1.2f + bobbing, y),
                        end = Offset(cx + r * 1.2f + bobbing, y),
                        strokeWidth = 3.2f,
                        cap = StrokeCap.Round
                    )
                }
            }
            WeatherSceneType.SUNSET -> {
                drawArc(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFFBBF24), Color(0xFFEA580C))
                    ),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - r * 1.1f, cy - r * 0.8f),
                    size = Size(r * 2.2f, r * 2.2f)
                )
                drawLine(
                    color = Color(0xFFF472B6),
                    start = Offset(cx - r * 1.4f, cy + r * 0.35f),
                    end = Offset(cx + r * 1.4f, cy + r * 0.35f),
                    strokeWidth = 2.8f,
                    cap = StrokeCap.Round
                )
            }
            WeatherSceneType.NIGHT -> {
                drawCircle(
                    color = Color(0xFF38BDF8),
                    radius = r * 0.95f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFF0F172A),
                    radius = r * 0.75f,
                    center = Offset(cx + r * 0.35f, cy - r * 0.25f)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloudShape(
    cx: Float,
    cy: Float,
    r: Float,
    color: Color
) {
    drawCircle(color = color, radius = r * 0.65f, center = Offset(cx - r * 0.5f, cy + r * 0.1f))
    drawCircle(color = color, radius = r * 0.82f, center = Offset(cx, cy - r * 0.15f))
    drawCircle(color = color, radius = r * 0.60f, center = Offset(cx + r * 0.55f, cy + r * 0.15f))
}

/**
 * 3D Wind Direction Compass Dial
 */
@Composable
fun WindCompassDial(
    windDirectionDeg: Int,
    modifier: Modifier = Modifier.size(56.dp)
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = minOf(cx, cy) * 0.85f

        drawCircle(
            color = Color.White.copy(alpha = 0.18f),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2f)
        )

        rotate(degrees = windDirectionDeg.toFloat(), pivot = Offset(cx, cy)) {
            val arrowPath = Path().apply {
                moveTo(cx, cy - radius * 0.75f)
                lineTo(cx - 6f, cy + radius * 0.45f)
                lineTo(cx, cy + radius * 0.2f)
                lineTo(cx + 6f, cy + radius * 0.45f)
                close()
            }
            drawPath(path = arrowPath, color = Color(0xFF38BDF8))
        }
    }
}

/**
 * Orbital Solar Sunrise/Sunset Arc Gauge
 */
@Composable
fun SolarHorizonArc(
    isDay: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val arcRectSize = Size(w * 0.84f, h * 1.4f)
        val topLeft = Offset(w * 0.08f, h * 0.18f)

        drawArc(
            color = Color.White.copy(alpha = 0.18f),
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcRectSize,
            style = Stroke(width = 3f, cap = StrokeCap.Round)
        )

        val sweep = if (isDay) 115f else 180f
        drawArc(
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFBBF24), Color(0xFFFB923C))
            ),
            startAngle = 180f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcRectSize,
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        val angleRad = (180.0 + sweep) * PI / 180.0
        val rx = arcRectSize.width / 2f
        val ry = arcRectSize.height / 2f
        val sunX = (topLeft.x + rx) + cos(angleRad).toFloat() * rx
        val sunY = (topLeft.y + ry) + sin(angleRad).toFloat() * ry

        drawCircle(
            color = Color(0xFFFEF08A),
            radius = 7f,
            center = Offset(sunX, sunY)
        )
    }
}
