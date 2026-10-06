package com.safetravel.tracker.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.safetravel.tracker.supabase.SupabaseHomepageBanner
import com.safetravel.tracker.supabase.SupabaseSafetyNews
import com.safetravel.tracker.supabase.SupabaseTrip

@Entity(tableName = "banners")
data class OfflineBanner(
    @PrimaryKey val id: String,
    val title: String,
    val imageUrl: String?,
    val actionUrl: String?,
    val displayOrder: Int,
    val isActive: Boolean,
    val createdAt: String?
)

@Entity(tableName = "safety_news")
data class OfflineNews(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val content: String?,
    val imageUrl: String?,
    val category: String,
    val publishedAt: String?,
    val createdAt: String?
)

@Entity(tableName = "trips")
data class OfflineTrip(
    @PrimaryKey val id: String,
    val userId: String?,
    val transportMode: String?,
    val startAddress: String?,
    val endAddress: String?,
    val startCoords: String?,
    val endCoords: String?,
    val startTime: String?,
    val endTime: String?,
    val totalDistance: Double?,
    val vehiclePlateNumber: String?,
    val vehicleDescription: String?,
    val status: String?,
    val startBatteryLevel: Int?,
    val endBatteryLevel: Int?,
    val trackingCode: String?
)

// Extension functions to convert between Supabase models and Room entities
fun SupabaseHomepageBanner.toOffline() = OfflineBanner(
    id = id, title = title, imageUrl = imageUrl, actionUrl = actionUrl,
    displayOrder = displayOrder, isActive = isActive == true, createdAt = createdAt
)

fun OfflineBanner.toSupabase() = SupabaseHomepageBanner(
    id = id, title = title, imageUrl = imageUrl, actionUrl = actionUrl,
    displayOrder = displayOrder, isActive = isActive, createdAt = createdAt
)

fun SupabaseSafetyNews.toOffline() = OfflineNews(
    id = id, title = title, description = description, content = content,
    imageUrl = imageUrl, category = category, publishedAt = publishedAt, createdAt = createdAt
)

fun OfflineNews.toSupabase() = SupabaseSafetyNews(
    id = id, title = title, description = description, content = content,
    imageUrl = imageUrl, category = category, publishedAt = publishedAt, createdAt = createdAt
)

fun SupabaseTrip.toOffline() = OfflineTrip(
    id = id,
    userId = userId,
    transportMode = transportMode,
    startAddress = startAddress,
    endAddress = endAddress,
    startCoords = startCoords,
    endCoords = endCoords,
    startTime = createdAt,
    endTime = endTime,
    totalDistance = totalDistance,
    vehiclePlateNumber = vehiclePlateNumber,
    vehicleDescription = vehicleDescription,
    status = status,
    startBatteryLevel = startBatteryLevel,
    endBatteryLevel = endBatteryLevel,
    trackingCode = trackingCode
)

fun OfflineTrip.toSupabase() = SupabaseTrip(
    id = id,
    userId = userId ?: "00000000-0000-0000-0000-000000000001",
    transportMode = transportMode ?: "driving",
    startAddress = startAddress,
    endAddress = endAddress,
    startCoords = startCoords,
    endCoords = endCoords,
    createdAt = startTime,
    endTime = endTime,
    totalDistance = totalDistance,
    vehiclePlateNumber = vehiclePlateNumber,
    vehicleDescription = vehicleDescription,
    status = status ?: "ongoing",
    startBatteryLevel = startBatteryLevel,
    endBatteryLevel = endBatteryLevel,
    trackingCode = trackingCode,
    routePathLog = emptyList(),
    notifiedGuardians = emptyList()
)
