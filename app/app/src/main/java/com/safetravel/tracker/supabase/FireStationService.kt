package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface FireStationService {
    @GET("rest/v1/fire_stations")
    suspend fun getFireStations(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "firestation_name.asc",
        @Query("limit") limit: Int = 1000
    ): Response<List<SupabaseFireStation>>
}
