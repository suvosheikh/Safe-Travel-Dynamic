package com.safetravel.tracker.supabase

import android.util.Log
import com.safetravel.tracker.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    var sharedPrefs: android.content.SharedPreferences? = null

    val supabaseUrl: String
        get() {
            val savedUrl = sharedPrefs?.getString("supabase_url_override", null)
            if (!savedUrl.isNullOrBlank()) return savedUrl.trim()
            return try { BuildConfig.SUPABASE_URL.trim() } catch (e: Exception) { "" }
        }

    val supabaseAnonKey: String
        get() {
            val savedKey = sharedPrefs?.getString("supabase_anon_key_override", null)
            if (!savedKey.isNullOrBlank()) return savedKey.trim()
            return try { BuildConfig.SUPABASE_ANON_KEY.trim() } catch (e: Exception) { "" }
        }

    val isConfigured: Boolean
        get() = supabaseUrl.isNotBlank() && !supabaseUrl.contains("placeholder") &&
                supabaseAnonKey.isNotBlank() && !supabaseAnonKey.contains("dummy") &&
                supabaseAnonKey.length > 10 // Basic length check for valid-looking keys

    val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private var _okHttpClient: OkHttpClient? = null
    private var _retrofit: Retrofit? = null
    private var _authService: SupabaseAuthService? = null
    private var _dbService: SupabaseDbService? = null

    fun getValidAuthHeader(): String {
        val token = SupabaseManager.currentSessionToken
        return if (!token.isNullOrBlank() && token.startsWith("eyJ")) {
            "Bearer $token"
        } else {
            "Bearer $supabaseAnonKey"
        }
    }

    @Synchronized
    fun rebuildClient() {
        _okHttpClient = null
        _retrofit = null
        _authService = null
        _dbService = null

        if (isConfigured) {
            try {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                val client = OkHttpClient.Builder()
                    .addInterceptor(loggingInterceptor)
                    .addInterceptor(SupabaseAuthInterceptor())
                    .build()
                
                _okHttpClient = client
                val baseUrl = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"
                val retrofitBuilder = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()
                
                _retrofit = retrofitBuilder
                _authService = retrofitBuilder.create(SupabaseAuthService::class.java)
                _dbService = retrofitBuilder.create(SupabaseDbService::class.java)
                Log.d(TAG, "Supabase client rebuilt successfully with URL: $supabaseUrl")
            } catch (e: Exception) {
                Log.e(TAG, "Error rebuilding Supabase client", e)
            }
        }
    }

    fun refreshSessionSync(): Boolean {
        val rt = SupabaseManager.currentRefreshToken ?: return false
        val url = supabaseUrl
        val key = supabaseAnonKey
        if (url.isBlank() || key.isBlank()) return false

        try {
            val client = OkHttpClient.Builder().build()
            val payload = mapOf("refresh_token" to rt)
            val json = moshi.adapter(Map::class.java).toJson(payload)
            
            val request = Request.Builder()
                .url("${url.trimEnd('/')}/auth/v1/token?grant_type=refresh_token")
                .post(json.toRequestBody("application/json".toMediaTypeOrNull()))
                .addHeader("apikey", key)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val authData = moshi.adapter(AuthResponse::class.java).fromJson(bodyStr)
                if (authData?.accessToken != null) {
                    SupabaseManager.saveSession(authData.accessToken, SupabaseManager.currentUserId ?: "", null, authData.refreshToken)
                    return true
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during token refresh", e)
        }
        return false
    }

    val authService: SupabaseAuthService?
        get() {
            if (_authService == null && isConfigured) rebuildClient()
            return _authService
        }

    val dbService: SupabaseDbService?
        get() {
            if (_dbService == null && isConfigured) rebuildClient()
            return _dbService
        }

    fun <T> getService(serviceClass: Class<T>): T? {
        if (_retrofit == null && isConfigured) rebuildClient()
        return _retrofit?.create(serviceClass)
    }
}
