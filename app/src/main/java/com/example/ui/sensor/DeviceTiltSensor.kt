package com.example.ui.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

data class TiltVector(
    val pitch: Float = 0f,
    val roll: Float = 0f
)

/**
 * Listens to hardware accelerometer / gravity sensors to provide smooth 3D parallax
 * when the user tilts or moves their Android device. Gracefully returns (0f, 0f)
 * on devices or test environments without hardware motion sensors.
 */
@Composable
fun rememberDeviceTiltState(enabled: Boolean = true): State<TiltVector> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(TiltVector()) }

    DisposableEffect(context, enabled) {
        if (!enabled) {
            onDispose { }
        } else {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            val sensor = sensorManager?.getDefaultSensor(Sensor.TYPE_GRAVITY)
                ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

            val listener = object : SensorEventListener {
                private var smoothedRoll = 0f
                private var smoothedPitch = 0f

                override fun onSensorChanged(event: SensorEvent?) {
                    val values = event?.values ?: return
                    if (values.size >= 2) {
                        val rawRoll = (values[0] / 9.81f).coerceIn(-1f, 1f)
                        val rawPitch = ((values[1] - 4.5f) / 9.81f).coerceIn(-1f, 1f)
                        smoothedRoll += (rawRoll - smoothedRoll) * 0.12f
                        smoothedPitch += (rawPitch - smoothedPitch) * 0.12f
                        tiltState.value = TiltVector(pitch = smoothedPitch, roll = smoothedRoll)
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }

            if (sensorManager != null && sensor != null) {
                sensorManager.registerListener(
                    listener,
                    sensor,
                    SensorManager.SENSOR_DELAY_GAME
                )
            }

            onDispose {
                sensorManager?.unregisterListener(listener)
            }
        }
    }

    return tiltState
}
