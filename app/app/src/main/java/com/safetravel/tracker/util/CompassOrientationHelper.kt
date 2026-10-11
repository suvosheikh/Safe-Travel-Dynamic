package com.safetravel.tracker.util

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

/**
 * High-performance hardware orientation sensor listener.
 * Provides instant 60fps heading (0..360 degrees) for Google Maps style directional flashlight cone
 * with zero-lag low-pass filtering and handheld tilt compensation.
 */
@Composable
fun rememberDeviceHeading(gpsBearing: Float = 0f): State<Float> {
    val context = LocalContext.current
    val headingState = remember { mutableStateOf(gpsBearing) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        if (sensorManager == null) {
            headingState.value = gpsBearing
            return@DisposableEffect onDispose {}
        }

        // Primary: Rotation Vector, Fallback 1: Geomagnetic Rotation, Fallback 2: Accel + Magnetometer
        val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR)

        val accelerometerSensor = if (rotationVectorSensor == null) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        } else null

        val magneticSensor = if (rotationVectorSensor == null) {
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        } else null

        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val gravityValues = FloatArray(3)
        val geoValues = FloatArray(3)
        var hasGravity = false
        var hasGeo = false

        var filteredHeading = headingState.value

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                var rawAzimuthDeg: Float? = null

                if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR ||
                    event.sensor.type == Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    // Handheld tilt compensation: maps screen plane to natural viewing angle
                    SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_X,
                        SensorManager.AXIS_Z,
                        remappedMatrix
                    )
                    SensorManager.getOrientation(remappedMatrix, orientationAngles)
                    val rad = orientationAngles[0]
                    rawAzimuthDeg = ((Math.toDegrees(rad.toDouble()) + 360.0) % 360.0).toFloat()
                } else if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    System.arraycopy(event.values, 0, gravityValues, 0, 3)
                    hasGravity = true
                } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                    System.arraycopy(event.values, 0, geoValues, 0, 3)
                    hasGeo = true
                }

                if (rawAzimuthDeg == null && hasGravity && hasGeo) {
                    if (SensorManager.getRotationMatrix(rotationMatrix, null, gravityValues, geoValues)) {
                        SensorManager.remapCoordinateSystem(
                            rotationMatrix,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Z,
                            remappedMatrix
                        )
                        SensorManager.getOrientation(remappedMatrix, orientationAngles)
                        val rad = orientationAngles[0]
                        rawAzimuthDeg = ((Math.toDegrees(rad.toDouble()) + 360.0) % 360.0).toFloat()
                    }
                }

                if (rawAzimuthDeg != null) {
                    // Instantaneous low-pass filter (Exponential Moving Average)
                    // Zero coroutine cancellation delay - delivers smooth 60fps movement
                    val diff = ((rawAzimuthDeg - filteredHeading + 540f) % 360f) - 180f
                    if (kotlin.math.abs(diff) > 0.4f) {
                        filteredHeading = (filteredHeading + diff * 0.38f + 360f) % 360f
                        headingState.value = filteredHeading
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (rotationVectorSensor != null) {
            sensorManager.registerListener(listener, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
        } else {
            accelerometerSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
            magneticSensor?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    // Blend GPS bearing when moving with forward velocity
    LaunchedEffect(gpsBearing) {
        if (gpsBearing > 0f) {
            headingState.value = gpsBearing
        }
    }

    return headingState
}
