package com.safetravel.tracker.supabase

import com.squareup.moshi.Json

data class SupabaseHospital(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "hospital_name") val name: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "specialty") val specialty: String? = null,
    @Json(name = "division") val division: String? = null,
    @Json(name = "district") val district: String? = null,
    @Json(name = "thana") val thana: String? = null,
    @Json(name = "address") val address: String? = null,
    @Json(name = "phone_number") val phoneNumber: String? = null,
    @Json(name = "ambulance_number") val ambulanceNumber: String? = null,
    @Json(name = "latitude") val latitude: Double? = null,
    @Json(name = "longitude") val longitude: Double? = null,
    @Json(name = "has_blood_bank") val hasBloodBank: Boolean? = false,
    @Json(name = "is_24_7") val is247: Boolean? = true,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "services") val services: Any? = null // Can be String or List depending on Moshi adapter
)
