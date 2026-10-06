package com.safetravel.tracker.supabase

import com.squareup.moshi.Json

data class SupabaseHotline(
    @Json(name = "id") val id: Int? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "phone") val phone: String? = null,
    @Json(name = "category") val category: String? = null,
    @Json(name = "image_url") val imageUrl: String? = null
)
