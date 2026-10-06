package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface SafetyTipService {
    @GET("rest/v1/safety_tips")
    suspend fun getSafetyTips(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("is_published") isPublished: String = "eq.true"
    ): Response<List<SupabaseSafetyTip>>

    @GET("rest/v1/user_tip_progress")
    suspend fun getUserTipProgress(
        @Header("Authorization") userAuth: String,
        @Query("user_id") userIdQuery: String
    ): Response<List<UserTipProgress>>

    @POST("rest/v1/user_tip_progress")
    suspend fun markTipAsRead(
        @Header("Authorization") userAuth: String,
        @Body progress: UserTipProgress,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<UserTipProgress>>
}
