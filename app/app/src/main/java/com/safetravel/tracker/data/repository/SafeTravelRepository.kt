package com.safetravel.tracker.data.repository

import com.safetravel.tracker.data.local.dao.SafeTravelDao
import com.safetravel.tracker.data.local.entities.toOffline
import com.safetravel.tracker.data.local.entities.toSupabase
import com.safetravel.tracker.supabase.SupabaseHomepageBanner
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.supabase.SupabaseSafetyNews
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SafeTravelRepository(private val dao: SafeTravelDao) {

    // Banners Flow: Always emits from local DB
    val allBanners: Flow<List<SupabaseHomepageBanner>> = dao.getAllBanners().map { list ->
        list.map { it.toSupabase() }
    }

    // News Flow: Always emits from local DB
    val allNews: Flow<List<SupabaseSafetyNews>> = dao.getAllNews().map { list ->
        list.map { it.toSupabase() }
    }

    // Refresh Banners from Supabase and save to Room
    suspend fun refreshBanners(): Result<Unit> {
        return try {
            val result = SupabaseManager.getLiveHomepageBanners()
            if (result.isSuccess) {
                val banners = result.getOrNull() ?: emptyList()
                if (banners.isNotEmpty()) {
                    dao.insertBanners(banners.map { it.toOffline() })
                }
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Refresh News from Supabase and save to Room
    suspend fun refreshNews(): Result<Unit> {
        return try {
            val result = SupabaseManager.getSafetyNews()
            if (result.isSuccess) {
                val news = result.getOrNull() ?: emptyList()
                if (news.isNotEmpty()) {
                    dao.insertNews(news.map { it.toOffline() })
                }
                Result.success(Unit)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
