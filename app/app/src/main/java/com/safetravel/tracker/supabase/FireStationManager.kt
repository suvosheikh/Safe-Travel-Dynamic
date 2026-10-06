package com.safetravel.tracker.supabase

import android.util.Log

object FireStationManager {
    private const val TAG = "FireStationManager"

    fun getFireStationService(): FireStationService? {
        return SupabaseClient.getService(FireStationService::class.java)
    }

    suspend fun fetchFireStations(): Result<List<SupabaseFireStation>> {
        Log.d(TAG, "Fetching fire stations... Supabase configured: ${SupabaseClient.isConfigured}")
        if (SupabaseClient.isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                
                val response = getFireStationService()?.getFireStations(auth)
                Log.d(TAG, "Response Code: ${response?.code()}")
                
                if (response?.isSuccessful == true && response.body() != null) {
                    val stations = response.body()!!
                    Log.d(TAG, "Successfully fetched ${stations.size} fire stations")
                    return Result.success(stations)
                }
                val errorBody = response?.errorBody()?.string()
                Log.w(TAG, "Failed to fetch fire stations. Code: ${response?.code()}, Error: $errorBody. Using high-quality offline data.")
                return Result.success(getFallbackFireStations())
            } catch (e: Exception) {
                Log.w(TAG, "Exception fetching fire stations, using high-quality offline data", e)
                return Result.success(getFallbackFireStations())
            }
        }
        Log.w(TAG, "Supabase not configured. Using offline fire stations.")
        return Result.success(getFallbackFireStations())
    }

    fun getFallbackFireStations(): List<SupabaseFireStation> {
        return listOf(
            SupabaseFireStation(
                id = 1,
                name = "ঢাকা কেন্দ্রীয় ফায়ার স্টেশন",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Sadarghat",
                type = "A",
                number = "02-9555555",
                latitude = 23.7084,
                longitude = 90.4105,
                address = "সদরঘাট, ঢাকা",
                website = "http://www.fireservice.gov.bd"
            ),
            SupabaseFireStation(
                id = 2,
                name = "মিরপুর ফায়ার স্টেশন",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Mirpur",
                type = "A",
                number = "02-9005555",
                latitude = 23.8055,
                longitude = 90.3686,
                address = "মিরপুর-১০, ঢাকা",
                website = "http://www.fireservice.gov.bd"
            ),
            SupabaseFireStation(
                id = 3,
                name = "উত্তরা ফায়ার স্টেশন",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Uttara",
                type = "B",
                number = "02-8915555",
                latitude = 23.8729,
                longitude = 90.3952,
                address = "উত্তরা সেক্টর ৩, ঢাকা",
                website = "http://www.fireservice.gov.bd"
            )
        )
    }
}

