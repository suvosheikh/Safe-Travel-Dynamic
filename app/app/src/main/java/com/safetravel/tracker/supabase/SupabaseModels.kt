package com.safetravel.tracker.supabase

import com.squareup.moshi.Json
import java.util.UUID

// --- AUTH MODELS ---
data class SignupData(
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String,
    @Json(name = "role") val role: String = "user"
)

data class SignupRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String,
    @Json(name = "data") val data: SignupData
)

data class LoginRequest(
    @Json(name = "email") val email: String,
    @Json(name = "password") val password: String
)

data class SupabaseUser(
    @Json(name = "id") val id: String,
    @Json(name = "email") val email: String?,
    @Json(name = "phone") val phone: String?
)

data class AuthResponse(
    @Json(name = "access_token") val accessToken: String?,
    @Json(name = "refresh_token") val refreshToken: String? = null,
    @Json(name = "user") val user: SupabaseUser?
)

// --- PROFILE & SOCIAL MODELS ---
data class SupabaseProfile(
    @Json(name = "id") val id: String,
    @Json(name = "full_name") val fullName: String,
    @Json(name = "phone_number") val phoneNumber: String?,
    @Json(name = "is_premium") val isPremium: Boolean? = false,
    @Json(name = "trip_credits") val tripCredits: Int = 0,
    @Json(name = "points_balance") val pointsBalance: Int = 0,
    @Json(name = "role") val role: String = "user",
    @Json(name = "avatar_url") val avatarUrl: String? = null,
    
    // Personal Fields
    @Json(name = "dob") val dob: String? = null,
    @Json(name = "gender") val gender: String? = null,
    @Json(name = "nid_passport") val nidPassport: String? = null,
    @Json(name = "occupation") val occupation: String? = null,
    @Json(name = "present_address") val presentAddress: String? = null,
    @Json(name = "permanent_address") val permanentAddress: String? = null,
    
    // Medical Fields
    @Json(name = "blood_group") val bloodGroup: String? = null,
    @Json(name = "allergies") val allergies: String? = null,
    @Json(name = "chronic_conditions") val chronicConditions: String? = null,
    @Json(name = "current_medications") val currentMedications: String? = null,
    @Json(name = "height_weight") val heightWeight: String? = null,
    @Json(name = "special_needs") val specialNeeds: String? = null,
    @Json(name = "sos_settings") val sosSettings: Map<String, Any>? = null,
    @Json(name = "premium_until") val premiumUntil: String? = null
)

data class Guardian(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "user_id") val userId: String,
    @Json(name = "name") val name: String,
    @Json(name = "relationship") val relationship: String,
    @Json(name = "phone") val phone: String,
    @Json(name = "avatar_index") val avatarIndex: Int = 0,
    @Json(name = "default_notify") val defaultNotify: Boolean? = true,
    @Json(name = "sos_permission") val sosPermission: Boolean? = true
)

// --- TRIP & TELEMETRY MODELS ---
data class SupabaseTrip(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "start_address") val startAddress: String? = null,
    @Json(name = "end_address") val endAddress: String? = null,
    @Json(name = "start_coords") val startCoords: String? = null,
    @Json(name = "end_coords") val endCoords: String? = null,
    @Json(name = "transport_mode") val transportMode: String? = null,
    @Json(name = "start_battery_level") val startBatteryLevel: Int? = null,
    @Json(name = "device_model") val deviceModel: String? = null,
    @Json(name = "safety_status") val status: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "vehicle_plate_number") val vehiclePlateNumber: String? = null,
    @Json(name = "vehicle_description") val vehicleDescription: String? = null,
    @Json(name = "vehicle_photo_url") val vehiclePhotoUrl: String? = null,
    @Json(name = "end_time") val endTime: String? = null,
    @Json(name = "end_battery_level") val endBatteryLevel: Int? = null,
    @Json(name = "total_distance") val totalDistance: Double? = null,
    @Json(name = "route_path_log") val routePathLog: List<Map<String, Double>>? = null,
    @Json(name = "planned_route_log") val plannedRouteLog: List<Map<String, Double>>? = null,
    @Json(name = "tracking_code") val trackingCode: String? = null,
    @Json(name = "notified_guardians") val notifiedGuardians: List<String>? = null,
    @Json(name = "current_lat") val currentLat: Double? = null,
    @Json(name = "current_lng") val currentLng: Double? = null,
    @Json(name = "estimated_duration_min") val estimatedDurationMin: Double? = null,
    @Json(name = "estimated_distance_km") val estimatedDistanceKm: Double? = null,
    @Json(name = "audio_clip_url") val audioClipUrl: String? = null,
    @Json(name = "sos_activity_logs") val sosActivityLogs: List<Map<String, Any>>? = null
) {
    val startLocation: String get() = startAddress ?: ""
    val endLocation: String get() = endAddress ?: ""

    val durationString: String get() {
        if (createdAt == null || endTime == null) return "---"
        return try {
            val start = com.safetravel.tracker.util.DateTimeUtils.parseIsoToDate(createdAt)
            val end = com.safetravel.tracker.util.DateTimeUtils.parseIsoToDate(endTime)
            if (start == null || end == null) return "---"
            val diff = end.time - start.time
            val minutes = (diff / (1000 * 60)).toInt()
            if (minutes < 60) "${minutes}m" else "${minutes / 60}h ${minutes % 60}m"
        } catch (e: Exception) {
            "---"
        }
    }

    val totalDistanceFormatted: String get() = if (totalDistance != null) "%.1f km".format(totalDistance) else "--- km"
    
    val batteryConsumed: String get() {
        val start = startBatteryLevel ?: return "---"
        val end = endBatteryLevel ?: return "---"
        return "${start - end}%"
    }
}

data class SupabaseSosRecord(
    @Json(name = "id") val id: String,
    @Json(name = "trip_id") val tripId: String,
    @Json(name = "status") val status: String,
    @Json(name = "triggered_at") val triggeredAt: String? = null
)

data class SupabaseSafetyAudioLog(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String? = null,
    @Json(name = "trip_id") val tripId: String? = null,
    @Json(name = "audio_url") val audioUrl: String,
    @Json(name = "cloudinary_public_id") val cloudinaryPublicId: String? = null,
    @Json(name = "duration_sec") val durationSec: Int = 0,
    @Json(name = "file_size_bytes") val fileSizeBytes: Long = 0L,
    @Json(name = "recorded_address") val recordedAddress: String? = null,
    @Json(name = "recorded_coords") val recordedCoords: String? = null,
    @Json(name = "source_trigger") val sourceTrigger: String? = "manual_toolkit",
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "is_deleted_by_user") val isDeletedByUser: Boolean? = false,
    @Json(name = "user_deleted_at") val userDeletedAt: String? = null
)

data class SupabaseRemoteConfig(
    @Json(name = "key") val key: String,
    @Json(name = "value") val value: String,
    @Json(name = "description") val description: String? = null
)

data class SupabasePointsLog(
    @Json(name = "id") val id: String,
    @Json(name = "user_id") val userId: String,
    @Json(name = "points_added") val pointsAdded: Int,
    @Json(name = "source") val source: String,
    @Json(name = "timestamp") val timestamp: String? = null
)

// --- CMS & CONTENT MODELS ---
data class SupabaseHomepageBanner(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "action_url") val actionUrl: String? = null,
    @Json(name = "display_order") val displayOrder: Int = 0,
    @Json(name = "is_active") val isActive: Boolean? = true,
    @Json(name = "created_at") val createdAt: String? = null
)

data class SupabaseSafetyNews(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "content") val content: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "category") val category: String = "General",
    @Json(name = "published_at") val publishedAt: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class SupabaseFeedback(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String?,
    @Json(name = "rating") val rating: Int,
    @Json(name = "feedback_type") val feedbackType: String,
    @Json(name = "subject") val subject: String?,
    @Json(name = "message") val message: String,
    @Json(name = "created_at") val createdAt: String? = null
)

// --- POLICE STATION MODELS ---
data class SupabasePoliceStation(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "policestation_name") val name: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "thana") val thana: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "policestation_number") val number: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "policestation_image") val imageUrl: String? = null
)

// --- FIRE STATION MODELS ---
data class SupabaseFireStation(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "firestation_name") val name: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "thana") val thana: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "firestation_number") val number: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "firestation_image") val imageUrl: String? = null,
    @Json(name = "website") val website: String? = null,
    @Json(name = "maps_category") val mapsCategory: String? = null,
    @Json(name = "google_maps_url") val googleMapsUrl: String? = null,
    @Json(name = "source_query") val sourceQuery: String? = null
)

data class SupabaseTripShare(
    @Json(name = "id") val id: String = UUID.randomUUID().toString(),
    @Json(name = "trip_id") val tripId: String,
    @Json(name = "shared_by") val sharedBy: String,
    @Json(name = "shared_with_phone") val sharedWithPhone: String,
    @Json(name = "shared_with_user_id") val sharedWithUserId: String? = null,
    @Json(name = "status") val status: String = "active",
    @Json(name = "created_at") val createdAt: String? = null,
    // Joined data for UI
    @Json(name = "travel_activities") val tripDetails: SupabaseTrip? = null,
    @Json(name = "profiles") val sharedByProfile: SupabaseProfile? = null
)

// --- SUBSCRIPTION & PAYMENT MODELS ---
data class SupabaseSubscriptionPlan(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "billing_period") val billingPeriod: String = "monthly",
    @Json(name = "duration_days") val durationDays: Int = 30,
    @Json(name = "price") val price: Double = 0.0,
    @Json(name = "discount_price") val discountPrice: Double? = null,
    @Json(name = "currency") val currency: String = "BDT",
    @Json(name = "features") val features: List<String> = emptyList(),
    @Json(name = "is_active") val isActive: Boolean = true,
    @Json(name = "is_popular") val isPopular: Boolean = false,
    @Json(name = "display_order") val displayOrder: Int = 1
)

data class SupabasePaymentTransaction(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "plan_id") val planId: String? = null,
    @Json(name = "amount") val amount: Double = 0.0,
    @Json(name = "currency") val currency: String = "BDT",
    @Json(name = "payment_method") val paymentMethod: String = "bkash",
    @Json(name = "sender_number") val senderNumber: String,
    @Json(name = "transaction_id") val transactionId: String,
    @Json(name = "status") val status: String = "pending",
    @Json(name = "admin_notes") val adminNotes: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

data class SupabaseVersionLog(
    @Json(name = "id") val id: String? = null,
    @Json(name = "version") val version: String,
    @Json(name = "version_code") val versionCode: Int = 1,
    @Json(name = "release_date") val releaseDate: String,
    @Json(name = "changes") val changes: List<String> = emptyList(),
    @Json(name = "is_critical") val isCritical: Boolean? = false,
    @Json(name = "download_url") val downloadUrl: String? = null,
    @Json(name = "created_at") val createdAt: String? = null
)

