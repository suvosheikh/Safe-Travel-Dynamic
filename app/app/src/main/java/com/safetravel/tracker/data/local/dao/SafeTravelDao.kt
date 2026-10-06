package com.safetravel.tracker.data.local.dao

import androidx.room.*
import com.safetravel.tracker.data.local.entities.OfflineBanner
import com.safetravel.tracker.data.local.entities.OfflineNews
import com.safetravel.tracker.data.local.entities.OfflineTrip
import kotlinx.coroutines.flow.Flow

@Dao
interface SafeTravelDao {
    // Banners
    @Query("SELECT * FROM banners ORDER BY displayOrder ASC")
    fun getAllBanners(): Flow<List<OfflineBanner>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBanners(banners: List<OfflineBanner>)

    @Query("DELETE FROM banners")
    suspend fun clearBanners()

    // Safety News
    @Query("SELECT * FROM safety_news ORDER BY publishedAt DESC")
    fun getAllNews(): Flow<List<OfflineNews>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNews(news: List<OfflineNews>)

    @Query("DELETE FROM safety_news")
    suspend fun clearNews()

    // Trips
    @Query("SELECT * FROM trips WHERE userId = :userId ORDER BY startTime DESC")
    suspend fun getTripsForUser(userId: String): List<OfflineTrip>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrip(trip: OfflineTrip)

    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: String): OfflineTrip?
}
