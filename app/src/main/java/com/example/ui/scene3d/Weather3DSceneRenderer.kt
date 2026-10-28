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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import com.example.model.GraphicsQuality
import com.example.model.WeatherSceneType
import com.example.model.WhatIfSimulationState
import com.example.ui.sensor.TiltVector
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
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
 * 3D Perspective Camera state calculated from MainActivity's SensorEventListener
 * (device pitch, roll, azimuth) and touch orbit gestures.
 */
private data class Camera3DPerspective(
    val yaw: Float,
    val pitch: Float,
    val rollDeg: Float,
    val fovScale: Float
) {
    fun project(
        worldX: Float,
        worldY: Float,
        depthZ: Float,
        viewportWidth: Float,
        viewportHeight: Float
    ): Triple<Float, Float, Float> {
        val safeZ = depthZ.coerceAtLeast(0.22f)
        val invZ = fovScale / safeZ
        val displacedX = worldX - yaw * (1.15f / safeZ)
        val displacedY = worldY - pitch * (0.95f / safeZ)

        val screenX = viewportWidth * 0.5f + displacedX * viewportWidth * 0.52f * invZ
        val screenY = viewportHeight * 0.36f + displacedY * viewportHeight * 0.42f * invZ
        return Triple(screenX, screenY, invZ)
    }
}

/**
 * Interactive real-time 3D weather environment renderer.
 * Synchronized with actual weather conditions and local hour of day, and respects
 * reduced-motion accessibility preferences and low-performance 2D device mode.
 */
@Composable
fun Weather3DSceneView(
    sceneType: WeatherSceneType,
    graphicsQuality: GraphicsQuality,
    tiltVector: TiltVector,
    modifier: Modifier = Modifier,
    localHourOfDay: Int = 12,
    reducedMotion: Boolean = false,
    interactiveTouch: Boolean = true,
    whatIfSimulation: WhatIfSimulationState? = null
) {
    val activeSim = whatIfSimulation?.takeIf { it.isActive }
    val effectiveSceneType = activeSim?.derivedSceneType ?: sceneType

    val infiniteTransition = rememberInfiniteTransition(label = "atmos_3d_loop")
    val rawTimePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time_phase"
    )
    val rawPulsePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_phase"
    )

    // Freeze continuous motion when Reduced Motion accessibility preference is active
    val windMultiplier = activeSim?.let { 0.35f + it.windStrength * 2.15f } ?: 1.0f
    val timePhase = if (reducedMotion) 12.5f else rawTimePhase * windMultiplier
    val pulsePhase = if (reducedMotion) 0.8f else rawPulsePhase

    val skyTop by animateColorAsState(
        targetValue = Color(effectiveSceneType.skyTopHex),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "sky_top"
    )
    val skyMid by animateColorAsState(
        targetValue = Color(effectiveSceneType.skyMidHex),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "sky_mid"
    )
    val skyBottom by animateColorAsState(
        targetValue = Color(effectiveSceneType.skyBottomHex),
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "sky_bottom"
    )
    val accentColor by animateColorAsState(
        targetValue = Color(effectiveSceneType.accentColorHex),
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
            CloudCluster3D(-0.55f, -0.32f, 1.65f, 1.15f, 0.015f, 0.96f),
            CloudCluster3D(0.18f, -0.26f, 1.30f, 1.35f, 0.022f, 0.90f),
            CloudCluster3D(-0.15f, -0.16f, 0.98f, 1.50f, 0.030f, 0.84f),
            CloudCluster3D(0.50f, -0.20f, 0.78f, 1.25f, 0.035f, 0.78f),
            CloudCluster3D(-0.42f, -0.08f, 0.58f, 1.40f, 0.042f, 0.72f)
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
                touchYaw = (touchYaw + dragAmount.x * 0.0028f).coerceIn(-0.75f, 0.75f)
                touchPitch = (touchPitch + dragAmount.y * 0.0028f).coerceIn(-0.55f, 0.55f)
            }
        }
    } else {
        Modifier
    }

    val effectiveTilt = if (reducedMotion) TiltVector() else tiltVector
    val camera = Camera3DPerspective(
        yaw = (effectiveTilt.roll * 0.55f) + (effectiveTilt.azimuth * 0.15f) + touchYaw,
        pitch = (effectiveTilt.pitch * 0.45f) + touchPitch,
        rollDeg = if (reducedMotion) 0f else (effectiveTilt.roll * -4.5f).coerceIn(-8f, 8f),
        fovScale = 1.0f + (effectiveTilt.pitch * 0.06f)
    )

    // Compute solar/lunar horizontal & vertical position synchronized with the city's local hour or What-If Lab hour
    val effectiveHour = activeSim?.timeOfDayHour?.roundToInt()?.coerceIn(0, 23) ?: when (effectiveSceneType) {
        WeatherSceneType.SUNNY -> if (localHourOfDay in 8..16) localHourOfDay else 12
        WeatherSceneType.SUNSET -> 18
        WeatherSceneType.NIGHT -> if (localHourOfDay !in 6..18) localHourOfDay else 22
        else -> localHourOfDay
    }
    val normalizedDayProgress = ((effectiveHour.coerceIn(0, 23) - 6) / 12f).coerceIn(-0.5f, 1.5f)
    val celestialWorldX = (normalizedDayProgress - 0.5f) * 0.72f
    val celestialWorldY = -0.38f + kotlin.math.abs(normalizedDayProgress - 0.5f) * 0.22f

    Box(
        modifier = modifier
            .then(gestureModifier)
            .graphicsLayer {
                if (!reducedMotion) {
                    rotationX = (-camera.pitch * 4.5f).coerceIn(-6f, 6f)
                    rotationY = (camera.yaw * 5.5f).coerceIn(-7f, 7f)
                    scaleX = 1.04f
                    scaleY = 1.04f
                    cameraDistance = 16f * density
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .testTag("active_3d_scene_${effectiveSceneType.name.lowercase()}")
        ) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas

            val camX = camera.yaw
            val camY = camera.pitch
            val is2DFallback = graphicsQuality == GraphicsQuality.FALLBACK_2D

            // 1. Dynamic 3D Atmospheric Sky Dome with perspective gradient shift
            val horizonShift = (0.52f - camY * 0.14f).coerceIn(0.28f, 0.76f)
            drawRect(
                brush = Brush.verticalGradient(
                    0.0f to skyTop,
                    horizonShift to skyMid,
                    1.0f to skyBottom
                ),
                size = size
            )

            // 2. Scene-specific Celestial & Volumetric Background Elements with 3D Camera Perspective
            rotate(degrees = if (is2DFallback || reducedMotion) 0f else camera.rollDeg, pivot = Offset(w * 0.5f, h * 0.5f)) {
                if (activeSim != null) {
                    val simHour = activeSim.timeOfDayHour
                    val isNightSim = simHour < 5.5f || simHour >= 19.5f
                    val isSunsetSim = simHour in 16.5f..19.5f || simHour in 5.5f..7.0f

                    if (isNightSim) {
                        drawNight3DEnvironment(w, h, particles, camera, celestialWorldX, timePhase, pulsePhase, is2DFallback)
                    } else if (isSunsetSim && activeSim.sunlightIntensity >= 0.18f) {
                        drawSunset3DEnvironment(w, h, camera, timePhase, pulsePhase)
                    } else if (activeSim.sunlightIntensity >= 0.15f) {
                        drawSunny3DEnvironment(
                            w = w,
                            h = h,
                            camera = camera,
                            celestialWorldX = celestialWorldX,
                            celestialWorldY = celestialWorldY,
                            timePhase = timePhase,
                            pulsePhase = pulsePhase,
                            accentColor = accentColor.copy(alpha = activeSim.sunlightIntensity.coerceIn(0.2f, 1f)),
                            is2DFallback = is2DFallback
                        )
                    }

                    val simCloudCount = (activeSim.cloudDensity * clouds.size).roundToInt().coerceIn(0, clouds.size)
                    if (simCloudCount > 0) {
                        val scaledClouds = clouds.take(simCloudCount).map {
                            it.copy(
                                scale = it.scale * (0.75f + activeSim.cloudDensity * 0.55f),
                                speed = it.speed * (0.4f + activeSim.windStrength * 1.9f)
                            )
                        }
                        drawVolumetricClouds3D(
                            w = w,
                            h = h,
                            clusters = scaledClouds,
                            camera = camera,
                            timePhase = timePhase,
                            isStorm = activeSim.rainIntensity >= 0.65f || activeSim.cloudDensity >= 0.80f,
                            isNight = isNightSim,
                            isSunset = isSunsetSim
                        )
                    }

                    if (activeSim.rainIntensity >= 0.68f && activeSim.windStrength >= 0.55f) {
                        drawLightningAndStormFlash(w, h, camX, camY, timePhase, pulsePhase, reducedMotion)
                    }

                    if (activeSim.rainIntensity >= 0.08f) {
                        val rainCount = (particles.size * activeSim.rainIntensity.coerceIn(0.15f, 1f)).roundToInt().coerceAtLeast(8)
                        drawRain3DParticlesAndWetGlass(
                            w = w,
                            h = h,
                            particles = particles.take(rainCount),
                            camX = camX + (activeSim.windStrength - 0.35f) * 0.45f,
                            camY = camY,
                            timePhase = timePhase,
                            isHeavyStorm = activeSim.rainIntensity >= 0.60f || activeSim.windStrength >= 0.65f,
                            is2DFallback = is2DFallback
                        )
                    }

                    if (activeSim.fogDensity >= 0.12f) {
                        drawFoggy3DLayers(
                            w = w,
                            h = h,
                            camX = camX,
                            camY = camY,
                            timePhase = timePhase * (0.5f + activeSim.fogDensity),
                            pulsePhase = pulsePhase
                        )
                    }
                } else {
                    when (effectiveSceneType) {
                        WeatherSceneType.SUNNY -> {
                            drawSunny3DEnvironment(w, h, camera, celestialWorldX, celestialWorldY, timePhase, pulsePhase, accentColor, is2DFallback)
                            drawVolumetricClouds3D(w, h, clouds.take(2), camera, timePhase, isStorm = false, isNight = false, isSunset = false)
                        }
                        WeatherSceneType.SUNSET -> {
                            drawSunset3DEnvironment(w, h, camera, timePhase, pulsePhase)
                            drawVolumetricClouds3D(w, h, clouds.take(4), camera, timePhase, isStorm = false, isNight = false, isSunset = true)
                        }
                        WeatherSceneType.NIGHT -> {
                            drawNight3DEnvironment(w, h, particles, camera, celestialWorldX, timePhase, pulsePhase, is2DFallback)
                            drawVolumetricClouds3D(w, h, clouds.take(3), camera, timePhase, isStorm = false, isNight = true, isSunset = false)
                        }
                        WeatherSceneType.CLOUDY -> {
                            drawAmbientSunGlow(w, h, camX, camY, Color(0xFF7DD3FC).copy(alpha = 0.34f))
                            drawVolumetricClouds3D(w, h, clouds, camera, timePhase, isStorm = false, isNight = false, isSunset = false)
                        }
                        WeatherSceneType.RAINY -> {
                            drawVolumetricClouds3D(w, h, clouds, camera, timePhase, isStorm = true, isNight = false, isSunset = false)
                            drawRain3DParticlesAndWetGlass(w, h, particles, camX, camY, timePhase, isHeavyStorm = false, is2DFallback = is2DFallback)
                        }
                        WeatherSceneType.THUNDERSTORM -> {
                            drawLightningAndStormFlash(w, h, camX, camY, timePhase, pulsePhase, reducedMotion)
                            drawVolumetricClouds3D(w, h, clouds, camera, timePhase, isStorm = true, isNight = false, isSunset = false)
                            drawRain3DParticlesAndWetGlass(w, h, particles, camX, camY, timePhase, isHeavyStorm = true, is2DFallback = is2DFallback)
                        }
                        WeatherSceneType.SNOWY -> {
                            drawVolumetricClouds3D(w, h, clouds.take(3), camera, timePhase, isStorm = false, isNight = false, isSunset = false)
                            drawSnow3DEnvironment(w, h, particles, camX, camY, timePhase, is2DFallback)
                        }
                        WeatherSceneType.FOGGY -> {
                            drawVolumetricClouds3D(w, h, clouds.take(3), camera, timePhase, isStorm = false, isNight = false, isSunset = false)
                            drawFoggy3DLayers(w, h, camX, camY, timePhase, pulsePhase)
                        }
                    }
                }

                // 3. 3D Perspective Horizon Grid & Depth Planes responsive to camera pitch/yaw
                if (!is2DFallback) {
                    drawAtmosphericDepthHorizon(w, h, camera, accentColor)
                }
            }
        }
    }
}

private fun DrawScope.drawSunny3DEnvironment(
    w: Float,
    h: Float,
    camera: Camera3DPerspective,
    celestialWorldX: Float,
    celestialWorldY: Float,
    timePhase: Float,
    pulsePhase: Float,
    accentColor: Color,
    is2DFallback: Boolean
) {
    val projectedSun = camera.project(
        worldX = celestialWorldX.coerceIn(-0.45f, 0.55f),
        worldY = celestialWorldY.coerceIn(-0.45f, -0.18f),
        depthZ = 1.45f,
        viewportWidth = w,
        viewportHeight = h
    )
    val sunCenter = Offset(projectedSun.first, projectedSun.second)
    val baseRadius = minOf(w, h) * 0.14f * camera.fovScale

    if (!is2DFallback) {
        val rayCount = 12
        for (i in 0 until rayCount) {
            val angleDeg = (i * (360f / rayCount)) + (timePhase * 6f) + camera.yaw * 18f
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

    if (!is2DFallback) {
        val centerScreen = Offset(w * (0.5f + camera.yaw * 0.18f), h * (0.45f + camera.pitch * 0.18f))
        val flareVec = centerScreen - sunCenter
        listOf(0.45f, 0.85f, 1.25f, 1.6f).forEachIndexed { idx, factor ->
            val orbCenter = sunCenter + (flareVec * factor)
            val orbRadius = baseRadius * (0.18f + idx * 0.07f)
            drawCircle(
                color = Color(0xFFFEF08A).copy(alpha = 0.14f - idx * 0.025f),
                radius = orbRadius,
                center = orbCenter
            )
        }
    }
}

private fun DrawScope.drawSunset3DEnvironment(
    w: Float,
    h: Float,
    camera: Camera3DPerspective,
    timePhase: Float,
    pulsePhase: Float
) {
    val projectedSun = camera.project(worldX = 0.05f, worldY = 0.0f, depthZ = 1.50f, viewportWidth = w, viewportHeight = h)
    val sunCenter = Offset(projectedSun.first, projectedSun.second)
    val sunRadius = minOf(w, h) * 0.17f * camera.fovScale

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
    camera: Camera3DPerspective,
    celestialWorldX: Float,
    timePhase: Float,
    pulsePhase: Float,
    is2DFallback: Boolean
) {
    val camX = camera.yaw
    val camY = camera.pitch

    particles.forEachIndexed { idx, star ->
        val depthScale = 1f / star.depthZ
        val sx = ((star.seedX * 0.5f + 0.5f) * w - camX * 65f * depthScale + w) % w
        val sy = ((star.seedY * 0.5f + 0.5f) * h * 0.65f - camY * 48f * depthScale + h * 0.65f) % (h * 0.65f)
        val twinkle = 0.35f + 0.65f * ((sin(pulsePhase * star.speed + idx) + 1f) * 0.5f)
        drawCircle(
            color = Color(0xFFF8FAFC).copy(alpha = twinkle.coerceIn(0.15f, 0.95f)),
            radius = (star.radius * 0.65f * depthScale).coerceIn(1.2f, 4.0f),
            center = Offset(sx, sy)
        )
    }

    if (!is2DFallback) {
        val auroraPath = Path().apply {
            val baseY = h * 0.18f - camY * 38f
            moveTo(0f, baseY)
            var x = 0f
            while (x <= w) {
                val waveY = baseY + sin(x * 0.012f + timePhase * 0.4f + camX * 2f) * 24f
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

    val projectedMoon = camera.project(
        worldX = (-celestialWorldX).coerceIn(-0.42f, 0.48f),
        worldY = -0.38f,
        depthZ = 1.40f,
        viewportWidth = w,
        viewportHeight = h
    )
    val moonCenter = Offset(projectedMoon.first, projectedMoon.second)
    val moonRadius = minOf(w, h) * 0.11f * camera.fovScale

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
    val center = Offset(w * 0.65f - camX * 65f, h * 0.22f - camY * 45f)
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
    camera: Camera3DPerspective,
    timePhase: Float,
    isStorm: Boolean,
    isNight: Boolean,
    isSunset: Boolean
) {
    clusters.sortedByDescending { it.depthZ }.forEach { cloud ->
        val drift = ((cloud.baseX + timePhase * cloud.speed) % 2.4f)
        val normX = if (drift > 1.2f) drift - 2.4f else if (drift < -1.2f) drift + 2.4f else drift

        val projected = camera.project(
            worldX = normX,
            worldY = cloud.baseY,
            depthZ = cloud.depthZ,
            viewportWidth = w,
            viewportHeight = h
        )
        val cx = projected.first
        val cy = projected.second
        val invZ = projected.third
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
    val windSlant = (if (isHeavyStorm) -26f else -14f) - camX * 32f

    particles.forEach { drop ->
        val invZ = (1.35f / drop.depthZ).coerceIn(0.45f, 2.2f)
        val progress = ((drop.seedY * 0.5f + 0.5f) + timePhase * drop.speed * 0.45f) % 1f
        val startX = ((drop.seedX * 0.5f + 0.5f) * w + progress * windSlant * 4f - camX * 40f * invZ + w) % w
        val startY = (progress * h - camY * 25f * invZ + h) % h
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

    if (!is2DFallback) {
        for (i in 0 until 12) {
            val gx = ((i * 83 + 29) % 100) / 100f * w - camX * 24f
            val slide = ((timePhase * 0.04f * (1 + i % 3) + i * 0.17f) % 1f)
            val gy = (slide * h * 0.75f) + h * 0.08f - camY * 18f
            val r = 3.5f + (i % 4) * 1.8f

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

        // Expanding 3D water ripples on the reflective horizon plane
        for (rIdx in 0 until 5) {
            val rippleProgress = ((timePhase * 0.18f + rIdx * 0.21f) % 1f)
            val rx = ((rIdx * 37 + 15) % 80 + 10) / 100f * w - camX * 30f
            val ry = h * (0.76f + (rIdx % 3) * 0.06f) - camY * 20f
            val rippleW = (24f + rippleProgress * 92f)
            val rippleH = rippleW * 0.28f
            val alpha = (1f - rippleProgress) * 0.36f
            drawOval(
                color = Color(0xFF7DD3FC).copy(alpha = alpha.coerceIn(0f, 0.4f)),
                topLeft = Offset(rx - rippleW * 0.5f, ry - rippleH * 0.5f),
                size = Size(rippleW, rippleH),
                style = Stroke(width = 1.6f)
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
    pulsePhase: Float,
    reducedMotion: Boolean
) {
    val wave = (sin(pulsePhase * 2.2f) + cos(timePhase * 0.9f) + 2f) * 0.25f
    val primaryIntensity = if (reducedMotion) 0.72f else (0.45f + wave * 0.55f).coerceIn(0.42f, 1f)

    if (!reducedMotion && wave > 0.72f) {
        val flashAlpha = ((wave - 0.72f) / 0.28f).coerceIn(0f, 1f) * 0.24f
        drawRect(
            color = Color(0xFFE0E7FF).copy(alpha = flashAlpha),
            size = size
        )
    }

    val boltStartX = w * 0.62f - camX * 75f
    val boltStartY = h * 0.10f - camY * 35f
    val boltPath = Path().apply {
        moveTo(boltStartX, boltStartY)
        lineTo(boltStartX - 28f, boltStartY + h * 0.11f)
        lineTo(boltStartX + 14f, boltStartY + h * 0.14f)
        lineTo(boltStartX - 36f, boltStartY + h * 0.28f)
        lineTo(boltStartX - 8f, boltStartY + h * 0.31f)
        lineTo(boltStartX - 48f, boltStartY + h * 0.48f)
    }
    val branchPath = Path().apply {
        moveTo(boltStartX - 20f, boltStartY + h * 0.22f)
        lineTo(boltStartX + 26f, boltStartY + h * 0.30f)
        lineTo(boltStartX + 42f, boltStartY + h * 0.39f)
    }

    drawPath(
        path = boltPath,
        color = Color(0xFF818CF8).copy(alpha = 0.55f * primaryIntensity),
        style = Stroke(width = 11f, cap = StrokeCap.Round)
    )
    drawPath(
        path = boltPath,
        color = Color.White.copy(alpha = 0.95f * primaryIntensity),
        style = Stroke(width = 3.2f, cap = StrokeCap.Round)
    )
    drawPath(
        path = branchPath,
        color = Color(0xFFC4B5FD).copy(alpha = 0.78f * primaryIntensity),
        style = Stroke(width = 2.2f, cap = StrokeCap.Round)
    )
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
        val fx = ((flake.seedX * 0.5f + 0.5f) * w + swayX - camX * 58f * invZ + w) % w
        val fy = (progressY * h - camY * 34f * invZ + h) % h
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
        val driftPath = Path().apply {
            val baseY = h * 0.88f - camY * 22f
            moveTo(0f, h)
            lineTo(0f, baseY)
            cubicTo(
                w * 0.3f - camX * 30f, baseY - 28f - camY * 18f,
                w * 0.7f - camX * 30f, baseY + 18f - camY * 18f,
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
        val depthFactor = 0.4f + layer * 0.28f
        val bandY = h * (0.22f + layer * 0.13f) - camY * 32f * depthFactor
        val shiftX = sin(timePhase * 0.15f + layer * 1.3f) * w * 0.12f - camX * 52f * depthFactor

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
    camera: Camera3DPerspective,
    accentColor: Color
) {
    val horizonY = h * 0.52f - camera.pitch * 42f
    val centerX = w * 0.5f - camera.yaw * 48f

    for (ring in 1..2) {
        val ringScale = ring * 0.44f * camera.fovScale
        val verticalForeshorten = (0.11f + camera.pitch * 0.03f).coerceIn(0.05f, 0.20f)
        drawOval(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    accentColor.copy(alpha = 0.18f / ring),
                    Color.Transparent
                )
            ),
            topLeft = Offset(centerX - w * ringScale, horizonY + (ring - 1) * 34f),
            size = Size(w * ringScale * 2f, h * verticalForeshorten),
            style = Stroke(width = 1.3f)
        )
    }
}
