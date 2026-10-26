package com.example.ui.scene3d

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import com.example.model.GraphicsQuality
import com.example.model.WeatherSceneType
import com.example.ui.sensor.TiltVector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private data class Particle3D(
    val seedX: Float,
    val seedY: Float,
    val depthZ: Float,
    val speed: Float,
    val radius: Float
)

private data class CloudCluster3D(
    val baseX: Float,
    val baseY: Float,
    val depthZ: Float,
    val scale: Float,
    val speed: Float,
    val shade: Float
)

/**
 * Interactive real-time 3D weather environment renderer.
 * Supports all 8 weather scenes (Sunny, Cloudy, Rainy, Thunderstorm, Snowy, Foggy, Sunset, Night)
 * with 3D perspective depth projection, hardware tilt parallax, interactive touch orbit,
 * and automatic 2D battery-saver fallback.
 */
@Composable
fun Weather3DSceneView(
    sceneType: WeatherSceneType,
    graphicsQuality: GraphicsQuality,
    tiltVector: TiltVector,
    modifier: Modifier = Modifier,
    interactiveTouch: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "atmos_3d_loop")
    val timePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_phase"
    )
    val pulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    // Smoothly transition sky colors when switching weather conditions or cities
    val skyTop by animateColorAsState(
        targetValue = Color(sceneType.skyTopHex),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "sky_top"
    )
    val skyMid by animateColorAsState(
        targetValue = Color(sceneType.skyMidHex),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "sky_mid"
    )
    val skyBottom by animateColorAsState(
        targetValue = Color(sceneType.skyBottomHex),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "sky_bottom"
    )
    val accentColor by animateColorAsState(
        targetValue = Color(sceneType.accentColorHex),
        animationSpec = tween(700),
        label = "accent_color"
    )

    var touchYaw by remember { mutableFloatStateOf(0f) }
    var touchPitch by remember { mutableFloatStateOf(0f) }

    val particles = remember(graphicsQuality) {
        val count = (95 * graphicsQuality.particleMultiplier).toInt().coerceAtLeast(22)
        List(count) { idx ->
            val hash1 = ((idx * 73 + 19) % 100) / 100f
            val hash2 = ((idx * 41 + 53) % 100) / 100f
            val depth = 0.25f + (((idx * 29 + 11) % 100) / 100f) * 1.75f
            Particle3D(
                seedX = hash1 * 2f - 1f,
                seedY = hash2 * 2f - 1f,
                depthZ = depth,
                speed = 0.6f + (idx % 7) * 0.18f,
                radius = 1.8f + (idx % 5) * 1.1f
            )
        }
    }

    val clouds = remember {
        listOf(
            CloudCluster3D(-0.55f, -0.32f, 1.55f, 1.15f, 0.015f, 0.96f),
            CloudCluster3D(0.18f, -0.26f, 1.25f, 1.35f, 0.022f, 0.90f),
            CloudCluster3D(-0.15f, -0.16f, 0.95f, 1.50f, 0.030f, 0.84f),
            CloudCluster3D(0.50f, -0.20f, 0.80f, 1.25f, 0.035f, 0.78f),
            CloudCluster3D(-0.42f, -0.08f, 0.65f, 1.40f, 0.042f, 0.72f)
        )
    }

    val gestureModifier = if (interactiveTouch) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    touchYaw *= 0.5f
                    touchPitch *= 0.5f
                }
            ) { change, dragAmount ->
                change.consume()
                touchYaw = (touchYaw + dragAmount.x * 0.0025f).coerceIn(-0.65f, 0.65f)
                touchPitch = (touchPitch + dragAmount.y * 0.0025f).coerceIn(-0.45f, 0.45f)
            }
        }
    } else {
        Modifier
    }

    Box(modifier = modifier.then(gestureModifier)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val camX = (tiltVector.roll * 0.35f) + touchYaw
            val camY = (tiltVector.pitch * 0.25f) + touchPitch
            val is2DFallback = graphicsQuality == GraphicsQuality.FALLBACK_2D

            // 1. Dynamic 3D Atmospheric Sky Dome
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to skyTop,
                    0.52f to skyMid,
                    1.0f to skyBottom
                ),
                size = size
            )

            // 2. Scene-specific Celestial & Volumetric Background Elements
            when (sceneType) {
                WeatherSceneType.SUNNY -> {
                    drawSunny3DEnvironment(w, h, camX, camY, timePhase, pulsePhase, accentColor, is2DFallback)
                    drawVolumetricClouds3D(w, h, clouds.take(2), camX, camY, timePhase, isStorm = false, isNight = false, isSunset = false)
                }
                WeatherSceneType.SUNSET -> {
                    drawSunset3DEnvironment(w, h, camX, camY, timePhase, pulsePhase)
                    drawVolumetricClouds3D(w, h, clouds.take(4), camX, camY, timePhase, isStorm = false, isNight = false, isSunset = true)
                }
                WeatherSceneType.NIGHT -> {
                    drawNight3DEnvironment(w, h, particles, camX, camY, timePhase, pulsePhase, is2DFallback)
                    drawVolumetricClouds3D(w, h, clouds.take(3), camX, camY, timePhase, isStorm = false, isNight = true, isSunset = false)
                }
                WeatherSceneType.CLOUDY -> {
                    drawAmbientSunGlow(w, h, camX, camY, Color(0xFF7DD3FC).copy(alpha = 0.28f))
                    drawVolumetricClouds3D(w, h, clouds, camX, camY, timePhase, isStorm = false, isNight = false, isSunset = false)
                }
                WeatherSceneType.RAINY -> {
                    drawVolumetricClouds3D(w, h, clouds, camX, camY, timePhase, isStorm = true, isNight = false, isSunset = false)
                    drawRain3DParticlesAndWetGlass(w, h, particles, camX, camY, timePhase, isHeavyStorm = false, is2DFallback = is2DFallback)
                }
                WeatherSceneType.THUNDERSTORM -> {
                    drawLightningAndStormFlash(w, h, camX, camY, timePhase, pulsePhase)
                    drawVolumetricClouds3D(w, h, clouds, camX, camY, timePhase, isStorm = true, isNight = false, isSunset = false)
                    drawRain3DParticlesAndWetGlass(w, h, particles, camX, camY, timePhase, isHeavyStorm = true, is2DFallback = is2DFallback)
                }
                WeatherSceneType.SNOWY -> {
                    drawVolumetricClouds3D(w, h, clouds.take(3), camX, camY, timePhase, isStorm = false, isNight = false, isSunset = false)
                    drawSnow3DEnvironment(w, h, particles, camX, camY, timePhase, is2DFallback)
                }
                WeatherSceneType.FOGGY -> {
                    drawVolumetricClouds3D(w, h, clouds.take(3), camX, camY, timePhase, isStorm = false, isNight = false, isSunset = false)
                    drawFoggy3DLayers(w, h, camX, camY, timePhase, pulsePhase)
                }
            }

            // 3. Subtle 3D Perspective Horizon Grid / Atmospheric Depth Floor
            if (!is2DFallback) {
                drawAtmosphericDepthHorizon(w, h, camX, camY, accentColor)
            }
        }
    }
}

private fun DrawScope.drawSunny3DEnvironment(
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    timePhase: Float,
    pulsePhase: Float,
    accentColor: Color,
    is2DFallback: Boolean
) {
    val sunCenter = Offset(
        x = w * 0.72f - camX * w * 0.18f,
        y = h * 0.22f - camY * h * 0.14f
    )
    val baseRadius = minOf(w, h) * 0.14f

    // Volumetric rotating solar ray shafts
    if (!is2DFallback) {
        val rayCount = 12
        for (i in 0 until rayCount) {
            val angleDeg = (i * (360f / rayCount)) + (timePhase * 6f)
            rotate(degrees = angleDeg, pivot = sunCenter) {
                val rayPath = Path().apply {
                    moveTo(sunCenter.x, sunCenter.y)
                    lineTo(sunCenter.x - baseRadius * 0.42f, sunCenter.y + baseRadius * 3.8f)
                    lineTo(sunCenter.x + baseRadius * 0.42f, sunCenter.y + baseRadius * 3.8f)
                    close()
                }
                drawPath(
                    path = rayPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB).copy(alpha = 0.20f),
                            accentColor.copy(alpha = 0.06f),
                            Color.Transparent
                        ),
                        startY = sunCenter.y,
                        endY = sunCenter.y + baseRadius * 3.8f
                    )
                )
            }
        }
    }

    // Outer corona halo
    val coronaScale = 1f + sin(pulsePhase) * 0.08f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFBEB).copy(alpha = 0.75f),
                 Color(0xFFFBBF24).copy(alpha = 0.38f),
                Color(0xFFF59E0B).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = sunCenter,
            radius = baseRadius * 2.8f * coronaScale
        ),
        radius = baseRadius * 2.8f * coronaScale,
        center = sunCenter
    )

    // 3D shaded solar sphere
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFEF08A),
                Color(0xFFF59E0B)
            ),
            center = Offset(sunCenter.x - baseRadius * 0.25f, sunCenter.y - baseRadius * 0.25f),
            radius = baseRadius * 1.1f
        ),
        radius = baseRadius,
        center = sunCenter
    )

    // Lens flare bokeh orbs along 3D diagonal axis
    if (!is2DFallback) {
        val centerScreen = Offset(w * 0.5f, h * 0.45f)
        val flareVec = centerScreen - sunCenter
        listOf(0.45f, 0.85f, 1.25f, 1.6f).forEachIndexed { idx, factor ->
            val orbCenter = sunCenter + (flareVec * factor)
            val orbRadius = baseRadius * (0.18f + idx * 0.07f)
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.12f - idx * 0.02f),
                radius = orbRadius,
                center = orbCenter
            )
        }
    }
}

private fun DrawScope.drawSunset3DEnvironment(
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    timePhase: Float,
    pulsePhase: Float
) {
    val sunCenter = Offset(
        x = w * 0.52f - camX * w * 0.16f,
        y = h * 0.36f - camY * h * 0.12f
    )
    val sunRadius = minOf(w, h) * 0.17f

    // Warm horizon atmospheric glow
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFB923C).copy(alpha = 0.65f),
                Color(0xFFEC4899).copy(alpha = 0.28f),
                Color.Transparent
            ),
            center = sunCenter,
            radius = sunRadius * 3.1f
        ),
        radius = sunRadius * 3.1f,
        center = sunCenter
    )

    // 3D Synth/Atmospheric sliced sunset sphere
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFEF08A),
                Color(0xFFF97316),
                Color(0xFFBE185D)
            ),
            startY = sunCenter.y - sunRadius,
            endY = sunCenter.y + sunRadius
        ),
        radius = sunRadius,
        center = sunCenter
    )

    // Subtle twilight light shimmer bands across the lower sky
    for (i in 0..4) {
        val yOffset = sunCenter.y + sunRadius * (0.25f + i * 0.22f)
        val shimmerWidth = sunRadius * (1.7f - i * 0.22f) * (1f + 0.05f * sin(pulsePhase + i))
        drawLine(
            color = Color(0xFFFFEDD5).copy(alpha = 0.25f - i * 0.04f),
            start = Offset(sunCenter.x - shimmerWidth, yOffset),
            end = Offset(sunCenter.x + shimmerWidth, yOffset),
            strokeWidth = 3f
        )
    }
}

private fun DrawScope.drawNight3DEnvironment(
    w: Float,
    h: Float,
    particles: List<Particle3D>,
    camX: Float,
    camY: Float,
    timePhase: Float,
    pulsePhase: Float,
    is2DFallback: Boolean
) {
    // 1. 3D Parallax Starfield
    particles.forEachIndexed { idx, star ->
        val depthScale = 1f / star.depthZ
        val sx = ((star.seedX * 0.5f + 0.5f) * w - camX * 45f * depthScale + w) % w
        val sy = ((star.seedY * 0.5f + 0.5f) * h * 0.65f - camY * 30f * depthScale + h * 0.65f) % (h * 0.65f)
        val twinkle = 0.35f + 0.65f * ((sin(pulsePhase * star.speed + idx) + 1f) * 0.5f)
        drawCircle(
            color = Color(0xFFF8FAFC).copy(alpha = twinkle.coerceIn(0.15f, 0.95f)),
            radius = (star.radius * 0.65f * depthScale).coerceIn(1.2f, 4.0f),
            center = Offset(sx, sy)
        )
    }

    // 2. Subtle Auroral Ribbon in the upper atmosphere
    if (!is2DFallback) {
        val auroraPath = Path().apply {
            val baseY = h * 0.18f - camY * 25f
            moveTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                val waveY = baseY + sin(x * 0.012f + timePhase * 0.4f) * 24f
                lineTo(x, waveY)
                x += 24f
            }
            lineTo(w, baseY + 75f)
            lineTo(0f, baseY + 75f)
            close()
        }
        drawPath(
            path = auroraPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF22D3EE).copy(alpha = 0.22f),
                    Color(0xFF818CF8).copy(alpha = 0.08f),
                    Color.Transparent
                )
            )
        )
    }

    // 3. 3D Cratered Moon Sphere
    val moonCenter = Offset(
        x = w * 0.74f - camX * w * 0.15f,
        y = h * 0.20f - camY * h * 0.12f
    )
    val moonRadius = minOf(w, h) * 0.11f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFBAE6FD).copy(alpha = 0.36f),
                Color(0xFF38BDF8).copy(alpha = 0.10f),
                Color.Transparent
            ),
            center = moonCenter,
            radius = moonRadius * 2.5f
        ),
        radius = moonRadius * 2.5f,
        center = moonCenter
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFF8FAFC),
                Color(0xFFCBD5E1),
                Color(0xFF64748B)
            ),
            center = Offset(moonCenter.x - moonRadius * 0.3f, moonCenter.y - moonRadius * 0.3f),
            radius = moonRadius * 1.25f
        ),
        radius = moonRadius,
        center = moonCenter
    )

    // Subtle 3D lunar craters
    drawCircle(
        color = Color(0xFF94A3B8).copy(alpha = 0.45f),
        radius = moonRadius * 0.22f,
        center = Offset(moonCenter.x + moonRadius * 0.25f, moonCenter.y - moonRadius * 0.15f)
    )
    drawCircle(
        color = Color(0xFF94A3B8).copy(alpha = 0.35f),
        radius = moonRadius * 0.15f,
        center = Offset(moonCenter.x - moonRadius * 0.2f, moonCenter.y + moonRadius * 0.28f)
    )
}

private fun DrawScope.drawAmbientSunGlow(
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    glowColor: Color
) {
    val center = Offset(w * 0.65f - camX * 50f, h * 0.22f - camY * 35f)
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(glowColor, Color.Transparent),
            center = center,
            radius = minOf(w, h) * 0.45f
        ),
        radius = minOf(w, h) * 0.45f,
        center = center
    )
}

private fun DrawScope.drawVolumetricClouds3D(
    w: Float,
    h: Float,
    clusters: List<CloudCluster3D>,
    camX: Float,
    camY: Float,
    timePhase: Float,
    isStorm: Boolean,
    isNight: Boolean,
    isSunset: Boolean
) {
    clusters.sortedByDescending { it.depthZ }.forEach { cloud ->
        val invZ = 1f / cloud.depthZ
        val drift = ((cloud.baseX + timePhase * cloud.speed) % 2.4f)
        val normX = if (drift > 1.2f) drift - 2.4f else if (drift < -1.2f) drift + 2.4f else drift

        val cx = w * 0.5f + (normX - camX * 0.45f) * w * 0.55f * invZ
        val cy = h * 0.32f + (cloud.baseY - camY * 0.30f) * h * 0.38f * invZ
        val baseRadius = minOf(w, h) * 0.11f * cloud.scale * invZ

        val topColor = when {
            isStorm -> Color(0xFF475569).copy(alpha = 0.88f)
            isNight -> Color(0xFF334155).copy(alpha = 0.78f)
            isSunset -> Color(0xFFFDA4AF).copy(alpha = 0.85f)
            else -> Color(0xFFFFFFFF).copy(alpha = 0.88f * cloud.shade)
        }
        val bottomColor = when {
            isStorm -> Color(0xFF0F172A).copy(alpha = 0.92f)
            isNight -> Color(0xFF0F172A).copy(alpha = 0.85f)
            isSunset -> Color(0xFF4C1D95).copy(alpha = 0.82f)
            else -> Color(0xFF94A3B8).copy(alpha = 0.75f * cloud.shade)
        }

        // Draw 5 overlapping 3D volumetric puffs per cloud cluster
        val puffs = listOf(
            Triple(-0.95f, 0.18f, 0.72f),
            Triple(-0.45f, -0.12f, 0.92f),
            Triple(0.05f, -0.22f, 1.08f),
            Triple(0.55f, -0.06f, 0.88f),
            Triple(0.95f, 0.20f, 0.68f)
        )
        puffs.forEach { (ox, oy, rScale) ->
            val px = cx + ox * baseRadius
            val py = cy + oy * baseRadius
            val pr = baseRadius * rScale
            drawCircle(
                brush = Brush.verticalGradient(
                    colors = listOf(topColor, bottomColor),
                    startY = py - pr,
                    endY = py + pr
                ),
                radius = pr,
                center = Offset(px, py)
            )
        }
    }
}

private fun DrawScope.drawRain3DParticlesAndWetGlass(
    w: Float,
    h: Float,
    particles: List<Particle3D>,
    camX: Float,
    camY: Float,
    timePhase: Float,
    isHeavyStorm: Boolean,
    is2DFallback: Boolean
) {
    val windSlant = (if (isHeavyStorm) -26f else -14f) - camX * 22f

    // 1. 3D Falling Rain Streaks with depth perspective
    particles.forEach { drop ->
        val invZ = (1.35f / drop.depthZ).coerceIn(0.45f, 2.2f)
        val progress = ((drop.seedY * 0.5f + 0.5f) + timePhase * drop.speed * 0.45f) % 1f
        val startX = ((drop.seedX * 0.5f + 0.5f) * w + progress * windSlant * 4f + w) % w
        val startY = progress * h
        val length = (if (isHeavyStorm) 28f else 20f) * invZ

        drawLine(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFBAE6FD).copy(alpha = (0.32f * invZ).coerceAtMost(0.75f))
                ),
                startY = startY,
                endY = startY + length
            ),
            start = Offset(startX, startY),
            end = Offset(startX + windSlant * 0.4f, startY + length),
            strokeWidth = (1.6f * invZ).coerceAtMost(3.2f),
            cap = StrokeCap.Round
        )
    }

    // 2. Wet Glass Lens Droplets & Ground Reflection Ripples
    if (!is2DFallback) {
        for (i in 0 until 12) {
            val gx = ((i * 83 + 29) % 100) / 100f * w - camX * 15f
            val slide = ((timePhase * 0.04f * (1 + i % 3) + i * 0.17f) % 1f)
            val gy = (slide * h * 0.75f) + h * 0.08f
            val r = 3.5f + (i % 4) * 1.8f

            // Specular droplet body
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.42f),
                        Color(0xFF38BDF8).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(gx - r * 0.3f, gy - r * 0.3f),
                    radius = r * 1.6f
                ),
                radius = r * 1.4f,
                center = Offset(gx, gy)
            )
        }
    }
}

private fun DrawScope.drawLightningAndStormFlash(
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    timePhase: Float,
    pulsePhase: Float
) {
    // Periodic atmospheric lightning trigger
    val flashTrigger = sin(pulsePhase * 2.2f) + cos(timePhase * 0.9f)
    if (flashTrigger > 1.35f) {
        val intensity = ((flashTrigger - 1.35f) / 0.65f).coerceIn(0f, 1f)
        // Sky illumination flash
        drawRect(
            color = Color(0xFFE0E7FF).copy(alpha = 0.25f * intensity),
            size = size
        )

        // Branching 3D lightning bolt
        val boltStartX = w * 0.58f - camX * 60f
        val boltPath = Path().apply {
            moveTo(boltStartX, h * 0.12f)
            lineTo(boltStartX - 28f, h * 0.24f)
            lineTo(boltStartX + 14f, h * 0.27f)
            lineTo(boltStartX - 36f, h * 0.42f)
            lineTo(boltStartX - 8f, h * 0.44f)
            lineTo(boltStartX - 48f, h * 0.62f)
        }
        drawPath(
            path = boltPath,
            color = Color(0xFF818CF8).copy(alpha = 0.55f * intensity),
            style = Stroke(width = 10f, cap = StrokeCap.Round)
        )
        drawPath(
            path = boltPath,
            color = Color.White.copy(alpha = 0.95f * intensity),
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawSnow3DEnvironment(
    w: Float,
    h: Float,
    particles: List<Particle3D>,
    camX: Float,
    camY: Float,
    timePhase: Float,
    is2DFallback: Boolean
) {
    particles.forEachIndexed { idx, flake ->
        val invZ = (1.3f / flake.depthZ).coerceIn(0.4f, 2.2f)
        val progressY = ((flake.seedY * 0.5f + 0.5f) + timePhase * flake.speed * 0.12f) % 1f
        val swayX = sin(timePhase * 0.35f + idx) * 26f * invZ
        val fx = ((flake.seedX * 0.5f + 0.5f) * w + swayX - camX * 45f * invZ + w) % w
        val fy = progressY * h
        val radius = (flake.radius * 1.15f * invZ).coerceIn(2.0f, 7.5f)

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.92f),
                    Color(0xFFBAE6FD).copy(alpha = 0.45f),
                    Color.Transparent
                ),
                center = Offset(fx, fy),
                radius = radius * 1.5f
            ),
            radius = radius * 1.5f,
            center = Offset(fx, fy)
        )
    }

    if (!is2DFallback) {
        // Soft 3D snow-covered horizon contour at bottom of viewport
        val driftPath = Path().apply {
            val baseY = h * 0.88f
            moveTo(0f, h)
            lineTo(0f, baseY)
            cubicTo(
                w * 0.3f, baseY - 28f - camY * 15f,
                w * 0.7f, baseY + 18f - camY * 15f,
                w, baseY - 12f
            )
            lineTo(w, h)
            close()
        }
        drawPath(
            path = driftPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFE0F2FE).copy(alpha = 0.22f),
                    Color(0xFF38BDF8).copy(alpha = 0.05f)
                )
            )
        )
    }
}

private fun DrawScope.drawFoggy3DLayers(
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    timePhase: Float,
    pulsePhase: Float
) {
    for (layer in 0 until 5) {
        val depthFactor = 0.4f + layer * 0.25f
        val bandY = h * (0.22f + layer * 0.13f) - camY * 22f * depthFactor
        val shiftX = sin(timePhase * 0.15f + layer * 1.3f) * w * 0.12f - camX * 35f * depthFactor

        drawRoundRect(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFE2E8F0).copy(alpha = 0.18f + (layer % 2) * 0.06f),
                    Color(0xFFCBD5E1).copy(alpha = 0.24f),
                    Color.Transparent
                ),
                startX = -w * 0.2f + shiftX,
                endX = w * 1.2f + shiftX
            ),
            topLeft = Offset(-w * 0.2f + shiftX, bandY),
            size = Size(w * 1.4f, h * 0.14f),
            cornerRadius = CornerRadius(60f, 60f)
        )
    }
}

private fun DrawScope.drawAtmosphericDepthHorizon(
    w: Float,
    h: Float,
    camX: Float,
    camY: Float,
    accentColor: Color
) {
    // Subtle futuristic 3D horizon rings providing spatial orientation
    val horizonY = h * 0.52f - camY * 24f
    val centerX = w * 0.5f - camX * 30f
    drawOval(
        brush = Brush.horizontalGradient(
            colors = listOf(
                Color.Transparent,
                accentColor.copy(alpha = 0.16f),
                Color.Transparent
            )
        ),
        topLeft = Offset(centerX - w * 0.44f, horizonY),
        size = Size(w * 0.88f, h * 0.12f),
        style = Stroke(width = 1.2f)
    )
}
