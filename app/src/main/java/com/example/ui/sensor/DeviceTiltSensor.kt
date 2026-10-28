package com.example.ui.sensor

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput

/**
 * 3D orientation & camera perspective state derived from hardware rotation/tilt sensors
 * (SensorEventListener in MainActivity) combined with interactive touch gestures.
 */
data class TiltVector(
    val pitch: Float = 0f,   // X-axis tilt (-1f..1f, normalized from radians)
    val roll: Float = 0f,    // Y-axis tilt (-1f..1f, normalized from radians)
    val azimuth: Float = 0f  // Z-axis yaw/heading (-1f..1f, normalized from radians)
)

/**
 * Modifier that applies real-time 3D camera perspective tilt and interactive 3D movable offset
 * to buttons and frosted glass cards based on device sensor tilt and finger drag.
 */
@Composable
fun Modifier.interactive3DTilt(
    tiltVector: TiltVector = TiltVector(),
    maxRotationDeg: Float = 9f,
    parallaxTranslationPx: Float = 12f,
    enableDragMove: Boolean = false
): Modifier {
    var dragOffsetX by remember { mutableFloatStateOf(0f) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }

    val dragModifier = if (enableDragMove) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    // Smoothly spring back toward center while keeping subtle offset
                    dragOffsetX *= 0.35f
                    dragOffsetY *= 0.35f
                }
            ) { change, dragAmount ->
                change.consume()
                dragOffsetX = (dragOffsetX + dragAmount.x).coerceIn(-48f, 48f)
                dragOffsetY = (dragOffsetY + dragAmount.y).coerceIn(-36f, 36f)
            }
        }
    } else {
        Modifier
    }

    return this
        .then(dragModifier)
        .graphicsLayer {
            val combinedRoll = (tiltVector.roll + (dragOffsetX / 60f)).coerceIn(-1.2f, 1.2f)
            val combinedPitch = (tiltVector.pitch - (dragOffsetY / 60f)).coerceIn(-1.2f, 1.2f)
            rotationY = combinedRoll * maxRotationDeg
            rotationX = combinedPitch * maxRotationDeg
            translationX = (tiltVector.roll * parallaxTranslationPx) + dragOffsetX
            translationY = (-tiltVector.pitch * parallaxTranslationPx) + dragOffsetY
            cameraDistance = 14f * density
        }
}
