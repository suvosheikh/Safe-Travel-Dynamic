package com.safetravel.tracker.supabase

import android.util.Log

object HospitalManager {
    private const val TAG = "HospitalManager"

    fun getHospitalService(): HospitalService? {
        return SupabaseClient.getService(HospitalService::class.java)
    }

    suspend fun fetchHospitals(): Result<List<SupabaseHospital>> {
        if (SupabaseClient.isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                
                val response = getHospitalService()?.getHospitals(auth)
                
                if (response?.isSuccessful == true && response.body() != null) {
                    return Result.success(response.body()!!)
                }
                Log.w(TAG, "Failed to fetch hospitals from server: ${response?.code()}. Using high-quality offline data.")
                return Result.success(getFallbackHospitals())
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching hospitals, using high-quality offline data", e)
                return Result.success(getFallbackHospitals())
            }
        }
        Log.w(TAG, "Supabase not configured. Using offline hospitals.")
        return Result.success(getFallbackHospitals())
    }

    fun getFallbackHospitals(): List<SupabaseHospital> {
        return listOf(
            SupabaseHospital(
                id = 1,
                name = "ঢাকা মেডিকেল কলেজ হাসপাতাল",
                type = "Govt",
                specialty = "General",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Shahbag",
                address = "সচিবালয় রোড, শাহবাগ, ঢাকা ১০০০",
                phoneNumber = "02-55165088",
                ambulanceNumber = "01711-403870",
                latitude = 23.7275,
                longitude = 90.3980,
                hasBloodBank = true,
                is247 = true,
                services = listOf("ICU", "Oxygen", "Trauma Center", "Blood Bank")
            ),
            SupabaseHospital(
                id = 2,
                name = "বঙ্গবন্ধু শেখ মুজিব মেডিকেল বিশ্ববিদ্যালয় (BSMMU)",
                type = "Govt",
                specialty = "Specialized",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Shahbag",
                address = "শাহবাগ, ঢাকা ১০০০",
                phoneNumber = "02-9661068",
                ambulanceNumber = "02-9661068",
                latitude = 23.7378,
                longitude = 90.3950,
                hasBloodBank = true,
                is247 = true,
                services = listOf("ICU", "CCU", "Diagnostic", "Surgery")
            ),
            SupabaseHospital(
                id = 3,
                name = "স্কয়ার হাসপাতাল লিমিটেড",
                type = "Private",
                specialty = "General",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Panthapath",
                address = "১৮/এফ বীর উত্তম কাজী নুরুজ্জামান সড়ক, ঢাকা ১২০৫",
                phoneNumber = "02-8144400",
                ambulanceNumber = "01713-332448",
                latitude = 23.7512,
                longitude = 90.3850,
                hasBloodBank = true,
                is247 = true,
                services = listOf("ICU", "Cardiac", "Emergency", "Luxury Cabin")
            ),
            SupabaseHospital(
                id = 4,
                name = "এভারকেয়ার হাসপাতাল ঢাকা",
                type = "Private",
                specialty = "Specialized",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Bashundhara",
                address = "প্লট ৮১, ব্লক ই, বসুন্ধরা আ/এ, ঢাকা ১২২৯",
                phoneNumber = "02-8431661",
                ambulanceNumber = "01714-090000",
                latitude = 23.8090,
                longitude = 90.4300,
                hasBloodBank = true,
                is247 = true,
                services = listOf("World-class ICU", "Pediatrics", "Emergency")
            ),
            SupabaseHospital(
                id = 5,
                name = "আনোয়ার খান মডার্ন মেডিকেল কলেজ হাসপাতাল",
                type = "Private",
                specialty = "General",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Dhanmondi",
                address = "রোড ৮, ধানমন্ডি, ঢাকা ১২০৫",
                phoneNumber = "02-9670295",
                ambulanceNumber = "01711-562916",
                latitude = 23.7430,
                longitude = 90.3830,
                hasBloodBank = true,
                is247 = true,
                services = listOf("Emergency", "Blood Bank", "OPD")
            )
        )
    }
}

