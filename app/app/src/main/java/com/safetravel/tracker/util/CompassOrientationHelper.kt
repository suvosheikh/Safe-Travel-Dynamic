package com.safetravel.tracker.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * Lightweight hardware orientation sensor listener.
 * Provides live azimuth/heading in degrees (0..360) for Google Maps style directional cone.
 */
@Composable
fun rememberDeviceHeading(gpsBearing: Float = 0f): State<Float> {
    val context = LocalContext.current
    val targetHeading = remember { mutableStateOf(gpsBearing) }
    val animatedHeading = remember { Animatable(gpsBearing) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val rotationSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager?.getDefaultSensor(Sensor.TYPE_ORIENTATION)

        if (sensorManager == null || rotationSensor == null) {
            targetHeading.value = gpsBearing
            return@DisposableEffect onDispose {}
        }

        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    val azimuthRad = orientationAngles[0]
                    val azimuthDeg = ((Math.toDegrees(azimuthRad.toDouble()) + 360.0) % 360.0).toFloat()
                    targetHeading.value = azimuthDeg
                } else if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                    val azimuthDeg = (event.values[0] + 360f) % 360f
                    targetHeading.value = azimuthDeg
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, rotationSensor, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Blend GPS bearing when moving with speed
    LaunchedEffect(gpsBearing) {
        if (gpsBearing > 0f) {
            targetHeading.value = gpsBearing
        }
    }

    // Smooth shortest-angle rotation animation (prevents 360 spin when crossing 0 deg)
    LaunchedEffect(targetHeading.value) {
        val target = targetHeading.value
        val current = (animatedHeading.value % 360f + 360f) % 360f
        val diff = ((target - current + 540f) % 360f) - 180f
        animatedHeading.animateTo(
            targetValue = animatedHeading.value + diff,
            animationSpec = tween(durationMillis = 180)
        )
    }

    val resultState = remember { derivedStateOf { (animatedHeading.value % 360f + 360f) % 360f } }
    return resultState
}
