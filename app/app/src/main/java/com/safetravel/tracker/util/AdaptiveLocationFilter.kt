package com.safetravel.tracker.util

import android.location.Location
import android.util.Log
import kotlin.math.cos
import kotlin.math.max

/**
 * AdaptiveLocationFilter
 *
 * A multi-stage mathematical GPS smoothing and jitter elimination engine.
 * Eliminates multipath reflection jumps (across the street / building corners)
 * and stillness drift (traffic stops, red lights, walking pause) WITHOUT forced road snapping.
 * 
 * Preserves genuine real-time trajectory for footpaths, alleys, and roads alike.
 */
class AdaptiveLocationFilter {

    companion object {
        private const val TAG = "AdaptiveLocationFilter"

        // Transport Modes
        const val MODE_WALK = 0
        const val MODE_CYCLE = 1
        const val MODE_CAR = 2
        const val MODE_BUS = 3
    }

    // State Variables for Lat/Lng (Metric local tangent space)
    private var stateLat: Double = 0.0
    private var stateLng: Double = 0.0
    private var varianceP: Double = 64.0 // Initial covariance in meters squared
    private var lastTimestampMs: Long = 0L
    private var lastAcceptedLocation: Location? = null

    /**
     * Resets filter state (called on trip start/finish or major tracking reset).
     */
    @Synchronized
    fun reset() {
        stateLat = 0.0
        stateLng = 0.0
        varianceP = 64.0
        lastTimestampMs = 0L
        lastAcceptedLocation = null
        RouteCorridorFilter.reset()
        Log.d(TAG, "AdaptiveLocationFilter reset.")
    }

    /**
     * Filters incoming raw GPS fix.
     * Returns a smoothed Location object, or null if the fix is an outlier (multipath jump).
     */
    @Synchronized
    fun filterLocation(rawLocation: Location, transportMode: Int = MODE_CAR): Location? {
        val rawLat = rawLocation.latitude
        val rawLng = rawLocation.longitude
        val rawAccuracy = if (rawLocation.hasAccuracy()) rawLocation.accuracy else 25.0f
        val currentTimestamp = rawLocation.time

        // Ignore completely invalid fixes
        if (rawLat == 0.0 && rawLng == 0.0) return null

        // First fix initialization
        if (lastAcceptedLocation == null || stateLat == 0.0 || stateLng == 0.0) {
            stateLat = rawLat
            stateLng = rawLng
            varianceP = (rawAccuracy * rawAccuracy).toDouble().coerceAtLeast(16.0)
            lastTimestampMs = currentTimestamp
            lastAcceptedLocation = Location(rawLocation)
            Log.d(TAG, "First fix established: $rawLat, $rawLng (accuracy: ${rawAccuracy}m)")
            return rawLocation
        }

        val prev = lastAcceptedLocation!!
        val deltaTimeSec = if (lastTimestampMs > 0L) {
            ((currentTimestamp - lastTimestampMs) / 1000.0).coerceAtLeast(0.1)
        } else 1.0

        // If app was paused or GPS lost for > 30 seconds, re-anchor without interpolation
        if (deltaTimeSec > 30.0) {
            Log.d(TAG, "Time gap (${deltaTimeSec}s) > 30s. Re-anchoring filter to fresh fix.")
            stateLat = rawLat
            stateLng = rawLng
            varianceP = (rawAccuracy * rawAccuracy).toDouble().coerceAtLeast(16.0)
            lastTimestampMs = currentTimestamp
            lastAcceptedLocation = Location(rawLocation)
            return rawLocation
        }

        // Mode-Adaptive Parameter Matrix
        val maxAllowedAccuracy: Float
        val maxPlausibleSpeedMps: Float
        val stillSpeedThresholdMps: Float
        val stillDistanceThresholdMeters: Float
        val processNoiseQ: Double

        when (transportMode) {
            MODE_WALK -> {
                maxAllowedAccuracy = 30.0f
                maxPlausibleSpeedMps = 10.0f // ~36 km/h max human sprint
                stillSpeedThresholdMps = 0.5f
                stillDistanceThresholdMeters = 2.0f
                processNoiseQ = 8.0 // Responsive tracking to keep pace with footsteps
            }
            MODE_CYCLE -> {
                maxAllowedAccuracy = 32.0f
                maxPlausibleSpeedMps = 22.0f // ~80 km/h max cycle sprint
                stillSpeedThresholdMps = 0.6f
                stillDistanceThresholdMeters = 2.5f
                processNoiseQ = 10.0
            }
            MODE_BUS -> {
                maxAllowedAccuracy = 35.0f
                maxPlausibleSpeedMps = 42.0f // ~150 km/h
                stillSpeedThresholdMps = 0.8f
                stillDistanceThresholdMeters = 3.5f
                val currentSpeed = if (rawLocation.hasSpeed()) rawLocation.speed else 0f
                processNoiseQ = if (currentSpeed > 6.0f) 18.0 else 12.0
            }
            else -> { // MODE_CAR (Default)
                maxAllowedAccuracy = 35.0f
                maxPlausibleSpeedMps = 50.0f // ~180 km/h
                stillSpeedThresholdMps = 0.8f
                stillDistanceThresholdMeters = 3.5f
                val currentSpeed = if (rawLocation.hasSpeed()) rawLocation.speed else 0f
                processNoiseQ = if (currentSpeed > 6.0f) 20.0 else 14.0
            }
        }

        // STAGE 1: Outlier Rejection (Signal Integrity & Urban Canyon Multipath Guard)
        val rawDistResults = FloatArray(1)
        Location.distanceBetween(prev.latitude, prev.longitude, rawLat, rawLng, rawDistResults)
        val rawDistanceMoved = rawDistResults[0]
        val impliedSpeedMps = rawDistanceMoved / deltaTimeSec

        // 1.1 Degraded Accuracy Check (bounced radio waves under Metro Rail/tall buildings)
        val prevSpeed = if (prev.hasSpeed()) prev.speed else impliedSpeedMps.toFloat()
        val effectiveMaxAccuracy = if (prevSpeed < 3.0f) {
            minOf(maxAllowedAccuracy, 28.0f)
        } else {
            maxAllowedAccuracy
        }

        if (rawAccuracy > effectiveMaxAccuracy) {
            Log.d(TAG, "Dropped outlier: accuracy (${rawAccuracy}m) exceeds dynamic threshold (${effectiveMaxAccuracy}m)")
            return null
        }

        // 1.2 Speed-Aware Dynamic Teleportation Check (Catches 20-30m jumps during slow travel)
        val maxPlausibleAccelerationMps2 = 4.5f
        val dynamicPlausibleSpeedMps = maxOf(
            4.0f,
            prevSpeed + (maxPlausibleAccelerationMps2 * deltaTimeSec.toFloat())
        ).coerceAtMost(maxPlausibleSpeedMps)

        val maxAllowedDistanceForInterval = maxOf(
            8.0f,
            (dynamicPlausibleSpeedMps * deltaTimeSec.toFloat()) + (rawAccuracy * 0.35f)
        )

        if (rawDistanceMoved > maxAllowedDistanceForInterval && impliedSpeedMps > dynamicPlausibleSpeedMps) {
            Log.w(TAG, "Dropped multipath jump: dist=${rawDistanceMoved}m in ${deltaTimeSec}s (implied speed=${impliedSpeedMps}m/s > max=${dynamicPlausibleSpeedMps}m/s)")
            return null
        }

        // Dynamic deadband threshold:
        // Indoors or in dense urban canyons where accuracy is degraded (accuracy > 10m),
        // GPS multipath bounce produces 2-5m false jumps. We scale the deadband dynamically.
        // Outdoors with high satellite lock (accuracy <= 8m), the deadband remains tight (2.0m)
        // so actual walking footsteps are tracked without delay.
        val dynamicStillDistanceMeters = if (rawAccuracy > 10.0f) {
            maxOf(stillDistanceThresholdMeters, minOf(rawAccuracy * 0.30f, 7.5f))
        } else {
            stillDistanceThresholdMeters
        }

        // STAGE 2: Stillness / Micro-Jitter Deadband (Eliminate indoor dancing and false 4 km/h speed)
        val hasHwSpeed = rawLocation.hasSpeed()
        val hwSpeed = if (hasHwSpeed) rawLocation.speed else 0.0f
        val isHwStationary = hasHwSpeed && hwSpeed < stillSpeedThresholdMps
        val isImpliedStationary = impliedSpeedMps < stillSpeedThresholdMps

        val isStationary = (isHwStationary || isImpliedStationary) && rawDistanceMoved < dynamicStillDistanceMeters

        if (isStationary) {
            // User/vehicle is stationary. Hold previous position rock-solid to eliminate map dancing
            lastTimestampMs = currentTimestamp
            // Explicitly clamp speed to 0 so speedometer HUD stays at 0 km/h
            return Location(prev).apply {
                speed = 0.0f
            }
        }

        // STAGE 3: 2D Adaptive Kalman Filter
        // Project to local metric coordinates (Flat Earth approximation around current latitude)
        val metersPerDegLat = 111132.0
        val radLat = Math.toRadians(stateLat)
        val metersPerDegLng = metersPerDegLat * max(0.1, cos(radLat))

        val dyMeters = (rawLat - stateLat) * metersPerDegLat
        val dxMeters = (rawLng - stateLng) * metersPerDegLng

        // Predict step: variance increases with elapsed time & process noise
        varianceP += processNoiseQ * deltaTimeSec

        // Measurement noise R = accuracy^2 (clamped to at least 3.0m)
        val effectiveAccuracy = max(rawAccuracy, 3.0f)
        val measurementNoiseR = (effectiveAccuracy * effectiveAccuracy).toDouble()

        // Kalman Gain K with responsive clamp so location tracks real footsteps without lagging
        val rawKalmanGainK = varianceP / (varianceP + measurementNoiseR)
        val kalmanGainK = rawKalmanGainK.coerceIn(0.40, 0.95)

        // Correction step
        val smoothedDy = dyMeters * kalmanGainK
        val smoothedDx = dxMeters * kalmanGainK

        stateLat += (smoothedDy / metersPerDegLat)
        stateLng += (smoothedDx / metersPerDegLng)

        // Update error covariance
        varianceP = (1.0 - kalmanGainK) * varianceP
        lastTimestampMs = currentTimestamp

        // Create smoothed location output
        val smoothedLocation = Location(rawLocation).apply {
            latitude = stateLat
            longitude = stateLng
            // Smoothed accuracy estimate
            if (hasAccuracy()) {
                accuracy = (effectiveAccuracy * (1.0f - (kalmanGainK * 0.4f).toFloat())).coerceAtLeast(2.0f)
            }
            // Clamp speed if displacement is below stillness threshold
            if (rawDistanceMoved < dynamicStillDistanceMeters || (hasSpeed() && speed < stillSpeedThresholdMps)) {
                speed = 0.0f
            }
        }

        lastAcceptedLocation = smoothedLocation
        Log.d(TAG, "Accepted Smoothed Location: [${stateLat.take(8)}, ${stateLng.take(8)}], K=${"%.2f".format(kalmanGainK)}, dist=${"%.1f".format(rawDistanceMoved)}m")
        return smoothedLocation
    }

    private fun Double.take(digits: Int): String {
        val s = this.toString()
        return if (s.length > digits) s.substring(0, digits) else s
    }
}