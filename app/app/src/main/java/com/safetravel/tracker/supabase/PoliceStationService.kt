package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface PoliceStationService {
    @GET("rest/v1/police_stations")
    suspend fun getPoliceStations(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "policestation_name.asc",
        @Query("limit") limit: Int = 1000
    ): Response<List<SupabasePoliceStation>>
}
