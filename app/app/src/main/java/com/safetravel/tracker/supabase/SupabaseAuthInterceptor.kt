package com.safetravel.tracker.supabase

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

class SupabaseAuthInterceptor : Interceptor {
    private val TAG = "SupabaseAuthInterceptor"

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val requestBuilder = original.newBuilder()

        // 1. Add API Key & Content-Type
        val anonKey = SupabaseClient.supabaseAnonKey
        if (anonKey.isNotBlank()) {
            requestBuilder.header("apikey", anonKey)
        }
        requestBuilder.header("Content-Type", "application/json")

        // 2. Add Authorization if missing
        val authHeader = original.header("Authorization")
        if (authHeader == null) {
            requestBuilder.header("Authorization", SupabaseClient.getValidAuthHeader())
        }

        var response = chain.proceed(requestBuilder.build())

        // 3. Handle 401 Unauthorized directly inside Interceptor (works even without WWW-Authenticate header)
        if (response.code == 401 && SupabaseManager.currentRefreshToken != null) {
            Log.d(TAG, "Detected HTTP 401 Unauthorized. Direct Interceptor intercepting to refresh token...")
            synchronized(this) {
                // Check if another thread already refreshed the token
                val previousToken = authHeader ?: SupabaseClient.getValidAuthHeader()
                val currentToken = "Bearer ${SupabaseManager.currentSessionToken}"

                var refreshSuccess = false
                if (previousToken == currentToken) {
                    // Token hasn't been refreshed yet, do it now
                    refreshSuccess = SupabaseClient.refreshSessionSync()
                } else {
                    // Already refreshed by another thread
                    refreshSuccess = true
                }

                if (refreshSuccess) {
                    response.close() // Close the original 401 response
                    val retryRequest = original.newBuilder()
                        .header("apikey", anonKey)
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer ${SupabaseManager.currentSessionToken}")
                        .build()
                    Log.d(TAG, "Token refresh succeeded. Retrying request with the updated token.")
                    return chain.proceed(retryRequest)
                } else {
                    Log.e(TAG, "Token refresh failed. Broadcasting session expiration event.")
                    SupabaseManager.sessionEvents.tryEmit("Session expired. Please log in again.")
                }
            }
        } else if (response.code == 401) {
            Log.w(TAG, "Detected 401 Unauthorized but no refresh token is stored.")
        }

        return response
    }
}
