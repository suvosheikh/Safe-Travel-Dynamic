package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface HospitalService {
    @GET("rest/v1/hospitals")
    suspend fun getHospitals(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseHospital>>
}
