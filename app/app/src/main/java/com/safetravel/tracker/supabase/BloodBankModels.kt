package com.safetravel.tracker.supabase

import com.squareup.moshi.Json

data class SupabaseBloodBank(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "bank_name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "thana") val thana: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "emergency_phone") val emergencyPhone: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)
