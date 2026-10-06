package com.safetravel.tracker.supabase

import com.squareup.moshi.Json
import java.util.UUID

/**
 * Advanced Travel Activity Model for High-Security Tracking.
 * This file is kept separate to prevent bloating of the main SupabaseManager.
 */
data class SupabaseTravelActivity(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    
    // Journey Basics
    @Json(name = "transport_mode") val transportMode: String,
    @Json(name = "start_address") val startAddress: String? = null,
    @Json(name = "end_address") val endAddress: String? = null,
    @Json(name = "start_coords") val startCoords: String? = null,
    @Json(name = "end_coords") val endCoords: String? = null,
    @Json(name = "start_time") val startTime: String? = null,
    @Json(name = "end_time") val endTime: String? = null,
    @Json(name = "total_distance") val totalDistance: Double? = 0.0,
    
    // Transport & Evidence
    @Json(name = "vehicle_plate_number") val vehiclePlateNumber: String? = null,
    @Json(name = "vehicle_description") val vehicleDescription: String? = null,
    @Json(name = "vehicle_photo_url") val vehiclePhotoUrl: String? = null,
    @Json(name = "driver_name_manual") val driverNameManual: String? = null,
    
    // Safety & Telemetry
    @Json(name = "safety_status") val safetyStatus: String = "ongoing",
    @Json(name = "sos_triggered_at") val sosTriggeredAt: String? = null,
    @Json(name = "route_path_log") val routePathLog: List<Map<String, Double>>? = null,
    @Json(name = "route_deviation_count") val routeDeviationCount: Int = 0,
    @Json(name = "unexpected_stop_logs") val unexpectedStopLogs: List<String>? = null,
    
    // Device Health
    @Json(name = "start_battery_level") val startBatteryLevel: Int? = null,
    @Json(name = "end_battery_level") val endBatteryLevel: Int? = null,
    @Json(name = "is_gps_lost") val isGpsLost: Boolean? = false,
    @Json(name = "device_model") val deviceModel: String? = null,
    
    // Social Safety
    @Json(name = "notified_guardians") val notifiedGuardians: List<String>? = null,
    @Json(name = "guardian_check_in_count") val guardianCheckInCount: Int = 0,
    
    // Advanced Features
    @Json(name = "audio_clip_url") val audioClipUrl: String? = null,
    @Json(name = "impact_detected") val impactDetected: Boolean? = false,
    @Json(name = "area_safety_score") val areaSafetyScore: Double? = null
)

/**
 * Helper to build initial activity data before starting a journey.
 */
object TravelActivityFactory {
    fun createInitial(
        userId: String,
        mode: String,
        startAddr: String,
        endAddr: String,
        startCoords: String,
        battery: Int,
        device: String
    ): SupabaseTravelActivity {
        return SupabaseTravelActivity(
            userId = userId,
            transportMode = mode,
            startAddress = startAddr,
            endAddress = endAddr,
            startCoords = startCoords,
            startBatteryLevel = battery,
            deviceModel = device,
            safetyStatus = "ongoing"
        )
    }
}
