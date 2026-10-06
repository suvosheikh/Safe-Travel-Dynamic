package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface BloodBankService {
    @GET("rest/v1/blood_banks")
    suspend fun getBloodBanks(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseBloodBank>>
}
