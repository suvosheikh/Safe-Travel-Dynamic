package com.safetravel.tracker.supabase

import android.util.Log

object PoliceStationManager {
    private const val TAG = "PoliceStationManager"

    fun getPoliceStationService(): PoliceStationService? {
        return SupabaseClient.getService(PoliceStationService::class.java)
    }

    suspend fun fetchPoliceStations(): Result<List<SupabasePoliceStation>> {
        Log.d(TAG, "Fetching police stations... Supabase configured: ${SupabaseClient.isConfigured}")
        if (SupabaseClient.isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                
                val response = getPoliceStationService()?.getPoliceStations(auth)
                Log.d(TAG, "Response Code: ${response?.code()}")
                
                if (response?.isSuccessful == true && response.body() != null) {
                    val stations = response.body()!!
                    Log.d(TAG, "Successfully fetched ${stations.size} police stations")
                    return Result.success(stations)
                }
                val errorBody = response?.errorBody()?.string()
                Log.w(TAG, "Failed to fetch stations. Code: ${response?.code()}, Error: $errorBody. Using high-quality offline data.")
                return Result.success(getFallbackPoliceStations())
            } catch (e: Exception) {
                Log.w(TAG, "Exception fetching police stations, using high-quality offline data", e)
                return Result.success(getFallbackPoliceStations())
            }
        }
        Log.w(TAG, "Supabase not configured. Using offline police stations.")
        return Result.success(getFallbackPoliceStations())
    }

    fun getFallbackPoliceStations(): List<SupabasePoliceStation> {
        return listOf(
            SupabasePoliceStation(
                id = 1,
                name = "শাহবাগ থানা (Shahbag Police Station)",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Shahbag",
                type = "Metropolitan",
                number = "01320-039750",
                latitude = 23.7378,
                longitude = 90.3950,
                address = "শাহবাগ মোড়, ঢাকা ১০০০",
                imageUrl = null
            ),
            SupabasePoliceStation(
                id = 2,
                name = "মিরপুর মডেল থানা (Mirpur Model Police Station)",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Mirpur",
                type = "Metropolitan Model",
                number = "01320-040220",
                latitude = 23.8055,
                longitude = 90.3686,
                address = "মিরপুর-২, ঢাকা ১২১৬",
                imageUrl = null
            ),
            SupabasePoliceStation(
                id = 3,
                name = "উত্তরা পশ্চিম থানা (Uttara West Police Station)",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Uttara",
                type = "Metropolitan",
                number = "01320-041440",
                latitude = 23.8729,
                longitude = 90.3952,
                address = "সেক্টর-৩, উত্তরা, ঢাকা ১২৩০",
                imageUrl = null
            ),
            SupabasePoliceStation(
                id = 4,
                name = "ধানমন্ডি মডেল থানা (Dhanmondi Model Police Station)",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Dhanmondi",
                type = "Metropolitan Model",
                number = "01320-039880",
                latitude = 23.7430,
                longitude = 90.3830,
                address = "রোড নং-৮, ধানমন্ডি আ/এ, ঢাকা ১২০৫",
                imageUrl = null
            ),
            SupabasePoliceStation(
                id = 5,
                name = "গুলশান মডেল থানা (Gulshan Model Police Station)",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Gulshan",
                type = "Metropolitan Model",
                number = "01320-041300",
                latitude = 23.7925,
                longitude = 90.4150,
                address = "রোড নং-৭৯, গুলশান-২, ঢাকা ১২১২",
                imageUrl = null
            )
        )
    }
}
