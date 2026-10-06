package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface EmergencyService {
    @GET("rest/v1/emergency_hotlines")
    suspend fun getHotlines(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseHotline>>
}
