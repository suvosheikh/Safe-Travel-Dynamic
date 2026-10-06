package com.safetravel.tracker.supabase

import com.squareup.moshi.Json

data class SupabaseSafetyTip(
    @Json(name = "id") val id: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    @Json(name = "content") val content: String,
    @Json(name = "category") val category: String,
    @Json(name = "image_url") val imageUrl: String? = null,
    @Json(name = "is_published") val isPublished: Boolean? = true,
    @Json(name = "created_at") val createdAt: String? = null,
    
    // UI Local state
    var isRead: Boolean = false
)

data class UserTipProgress(
    @Json(name = "id") val id: String? = null,
    @Json(name = "user_id") val userId: String,
    @Json(name = "tip_id") val tipId: String,
    @Json(name = "is_read") val isRead: Boolean? = true,
    @Json(name = "read_at") val readAt: String? = null
)
