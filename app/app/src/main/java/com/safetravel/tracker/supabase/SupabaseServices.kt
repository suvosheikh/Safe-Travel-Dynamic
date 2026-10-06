package com.safetravel.tracker.supabase

import retrofit2.Response
import retrofit2.http.*

interface SupabaseAuthService {
    @POST("auth/v1/signup")
    suspend fun signUp(@Body body: SignupRequest): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=password")
    suspend fun login(@Body body: LoginRequest): Response<AuthResponse>

    @POST("auth/v1/token?grant_type=refresh_token")
    suspend fun refreshToken(@Body body: Map<String, String>): Response<AuthResponse>
}

interface SupabaseDbService {
    // Profiles table REST endpoints
    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") idQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<List<SupabaseProfile>>

    @GET("rest/v1/profiles")
    suspend fun getProfileByPhone(
        @Query("phone_number") phoneQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<List<SupabaseProfile>>

    @GET("rest/v1/profiles")
    suspend fun getProfilesWithFilter(
        @Query("or") orQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<List<SupabaseProfile>>

    @POST("rest/v1/profiles")
    suspend fun createProfile(
        @Body profile: SupabaseProfile,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseProfile>>

    @PATCH("rest/v1/profiles")
    suspend fun updateProfile(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseProfile>>

    // Travel Activities
    @GET("rest/v1/travel_activities")
    suspend fun getTrips(
        @Query("user_id") userIdQuery: String,
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseTrip>>

    @GET("rest/v1/travel_activities")
    suspend fun getTripById(
        @Query("id") idQuery: String,
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseTrip>>

    @POST("rest/v1/travel_activities")
    suspend fun createTrip(
        @Body trip: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseTrip>>

    @PATCH("rest/v1/travel_activities")
    suspend fun updateTrip(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseTrip>>

    // SOS records
    @GET("rest/v1/sos_records")
    suspend fun getSosRecords(
        @Header("Authorization") userAuth: String
    ): Response<List<SupabaseSosRecord>>

    @POST("rest/v1/sos_records")
    suspend fun createSosRecord(
        @Body body: Map<String, @JvmSuppressWildcards String>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseSosRecord>>

    @PATCH("rest/v1/sos_records")
    suspend fun updateSosRecord(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards String>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseSosRecord>>

    // Points log
    @GET("rest/v1/points_log")
    suspend fun getPointsLogs(
        @Header("Authorization") userAuth: String,
        @Query("user_id") userIdQuery: String? = null,
        @Query("order") order: String = "timestamp.desc",
        @Query("limit") limit: Int = 20
    ): Response<List<SupabasePointsLog>>

    @POST("rest/v1/points_log")
    suspend fun createPointsLog(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabasePointsLog>>

    // Safety Audio Blackbox Logs
    @GET("rest/v1/safety_audio_logs")
    suspend fun getSafetyAudioLogs(
        @Query("user_id") userIdQuery: String,
        @Header("Authorization") userAuth: String,
        @Query("is_deleted_by_user") isDeletedQuery: String = "neq.true",
        @Query("order") order: String = "created_at.desc",
        @Query("select") select: String = "*"
    ): Response<List<SupabaseSafetyAudioLog>>

    @POST("rest/v1/safety_audio_logs")
    suspend fun createSafetyAudioLog(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseSafetyAudioLog>>

    @PATCH("rest/v1/safety_audio_logs")
    suspend fun updateSafetyAudioLog(
        @Query("id") idQuery: String,
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseSafetyAudioLog>>

    // App Remote Configurations (Mapbox, Cloudinary, etc.)
    @GET("rest/v1/app_remote_configs")
    suspend fun getRemoteConfigs(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*"
    ): Response<List<SupabaseRemoteConfig>>

    // Homepage banners
    @GET("rest/v1/banners")
    suspend fun getHomepageBanners(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "display_order.asc"
    ): Response<List<SupabaseHomepageBanner>>

    @POST("rest/v1/banners")
    suspend fun createHomepageBanner(
        @Body banner: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseHomepageBanner>>

    @DELETE("rest/v1/banners")
    suspend fun deleteHomepageBanner(
        @Query("id") idQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<Unit>

    // Safety News
    @GET("rest/v1/safety_news")
    suspend fun getSafetyNews(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "published_at.desc"
    ): Response<List<SupabaseSafetyNews>>

    // Guardian Network
    @GET("rest/v1/guardians")
    suspend fun getGuardians(
        @Query("user_id") userIdQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<List<Guardian>>

    @POST("rest/v1/guardians")
    suspend fun createGuardian(
        @Body guardian: Guardian,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<Guardian>>

    @DELETE("rest/v1/guardians")
    suspend fun deleteGuardian(
        @Query("id") idQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<Unit>

    // Trip Shares
    @GET("rest/v1/trip_shares")
    suspend fun getTripShares(
        @Query("shared_with_phone") phoneQuery: String,
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*,travel_activities(*),profiles!shared_by(*)"
    ): Response<List<SupabaseTripShare>>

    @GET("rest/v1/trip_shares")
    suspend fun getTripSharesWithFilter(
        @Query("or") orQuery: String,
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*,travel_activities(*),profiles!shared_by(*)"
    ): Response<List<SupabaseTripShare>>

    @GET("rest/v1/trip_shares")
    suspend fun getTripSharesByUserId(
        @Query("shared_with_user_id") userIdQuery: String,
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*,travel_activities(*),profiles!shared_by(*)"
    ): Response<List<SupabaseTripShare>>

    @POST("rest/v1/trip_shares")
    suspend fun createTripShare(
        @Body share: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseTripShare>>

    @PATCH("rest/v1/trip_shares")
    suspend fun updateTripShare(
        @Query("id") idQuery: String,
        @Body updates: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String
    ): Response<List<SupabaseTripShare>>

    // Feedback
    @POST("rest/v1/feedback")
    suspend fun createFeedback(
        @Body feedback: SupabaseFeedback,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabaseFeedback>>

    // User Devices / FCM tokens
    @GET("rest/v1/user_devices")
    suspend fun getUserDevices(
        @Query("user_id") userIdQuery: String,
        @Header("Authorization") userAuth: String
    ): Response<List<Map<String, @JvmSuppressWildcards Any>>>

    @POST("rest/v1/user_devices")
    suspend fun upsertUserDevice(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "resolution=merge-duplicates"
    ): Response<Unit>


    // Subscription Plans
    @GET("rest/v1/subscription_plans")
    suspend fun getSubscriptionPlans(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("is_active") isActive: String = "eq.true",
        @Query("order") order: String = "display_order.asc"
    ): Response<List<SupabaseSubscriptionPlan>>

    // Payment Transactions
    @GET("rest/v1/payment_transactions")
    suspend fun getPaymentTransactions(
        @Header("Authorization") userAuth: String,
        @Query("user_id") userIdQuery: String,
        @Query("order") order: String = "created_at.desc"
    ): Response<List<SupabasePaymentTransaction>>

    @POST("rest/v1/payment_transactions")
    suspend fun submitPaymentTransaction(
        @Body body: Map<String, @JvmSuppressWildcards Any>,
        @Header("Authorization") userAuth: String,
        @Header("Prefer") prefer: String = "return=representation"
    ): Response<List<SupabasePaymentTransaction>>

    // App Version Logs
    @GET("rest/v1/app_version_logs")
    suspend fun getVersionLogs(
        @Header("Authorization") userAuth: String,
        @Query("select") select: String = "*",
        @Query("order") order: String = "version_code.desc"
    ): Response<List<SupabaseVersionLog>>
}

