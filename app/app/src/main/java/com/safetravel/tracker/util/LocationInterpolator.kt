package com.safetravel.tracker.util

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import kotlin.math.*

/**
 * Utility for smooth, 60fps location gliding and coordinate interpolation.
 * Prevents discrete jumping between periodic GPS fixes (mimicking Google Maps navigation).
 */
object LocationInterpolator {

    /**
     * Calculates distance in meters between two lat/lng pairs using the Haversine formula.
     */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Linearly interpolates between two coordinates based on fraction t (0.0 to 1.0).
     */
    fun interpolate(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double,
        fraction: Float
    ): Pair<Double, Double> {
        val f = fraction.coerceIn(0f, 1f).toDouble()
        val lat = startLat + (endLat - startLat) * f
        val lng = startLng + (endLng - startLng) * f
        return Pair(lat, lng)
    }
}

/**
 * Remembers a smoothly interpolated coordinate pair that glides across screen frames
 * from the previous GPS fix to the new GPS fix instead of teleporting instantaneously.
 */
@Composable
fun rememberSmoothLocation(
    targetCoords: Pair<Double, Double>?,
    animationDurationMs: Int = 1100
): Pair<Double, Double>? {
    if (targetCoords == null || targetCoords.first == 0.0 || targetCoords.second == 0.0) {
        return targetCoords
    }

    var currentDisplayCoords by remember { mutableStateOf(targetCoords) }
    var previousTarget by remember { mutableStateOf(targetCoords) }
    var lastUpdateTimeMs by remember { mutableLongStateOf(0L) }
    val animProgress = remember { Animatable(1f) }

    LaunchedEffect(targetCoords) {
        val now = System.currentTimeMillis()
        val delta = if (lastUpdateTimeMs > 0L) (now - lastUpdateTimeMs).toInt() else animationDurationMs
        lastUpdateTimeMs = now

        val prev = previousTarget
        previousTarget = targetCoords

        val dist = LocationInterpolator.distanceMeters(
            prev.first, prev.second,
            targetCoords.first, targetCoords.second
        )

        // Teleport guard (> 500m) or standing-still jitter guard (< 0.25m):
        // Snap immediately without animating
        if (dist > 500.0 || dist < 0.25) {
            currentDisplayCoords = targetCoords
            animProgress.snapTo(1f)
            return@LaunchedEffect
        }

        // Dynamically match animation duration to the actual incoming GPS fix interval (800ms - 2500ms)
        val effectiveDuration = delta.coerceIn(800, 2500)
        val startLat = currentDisplayCoords.first
        val startLng = currentDisplayCoords.second

        animProgress.snapTo(0f)
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = effectiveDuration,
                easing = LinearEasing
            )
        ) {
            val (interpLat, interpLng) = LocationInterpolator.interpolate(
                startLat, startLng,
                targetCoords.first, targetCoords.second,
                value
            )
            currentDisplayCoords = Pair(interpLat, interpLng)
        }
    }

    return currentDisplayCoords
}
