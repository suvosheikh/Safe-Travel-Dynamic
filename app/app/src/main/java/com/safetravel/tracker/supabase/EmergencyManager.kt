package com.safetravel.tracker.supabase

import android.util.Log

object EmergencyManager {
    private const val TAG = "EmergencyManager"

    fun getEmergencyService(): EmergencyService? {
        return SupabaseClient.getService(EmergencyService::class.java)
    }

    suspend fun fetchHotlines(): Result<List<SupabaseHotline>> {
        if (SupabaseClient.isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                
                val response = getEmergencyService()?.getHotlines(auth)
                
                if (response?.isSuccessful == true && response.body() != null) {
                    return Result.success(response.body()!!)
                }
                Log.w(TAG, "Failed to fetch hotlines from server: ${response?.code()}. Using high-quality offline data.")
                return Result.success(getFallbackHotlines())
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching emergency hotlines, using high-quality offline data", e)
                return Result.success(getFallbackHotlines())
            }
        }
        Log.w(TAG, "Supabase not configured. Using offline hotlines.")
        return Result.success(getFallbackHotlines())
    }

    fun getFallbackHotlines(): List<SupabaseHotline> {
        return listOf(
            SupabaseHotline(
                id = 1,
                name = "জাতীয় জরুরি সেবা (National Emergency Service)",
                description = "পুলিশ, ফায়ার সার্ভিস ও অ্যাম্বুলেন্স সেবা পেতে ডায়াল করুন",
                phone = "999",
                category = "National"
            ),
            SupabaseHotline(
                id = 2,
                name = "তথ্য ও সেবা হেল্পলাইন (National Helpline)",
                description = "সরকারি সেবা ও তথ্য হেল্পলাইন",
                phone = "333",
                category = "Information"
            ),
            SupabaseHotline(
                id = 3,
                name = "নারী ও শিশু নির্যাতন প্রতিরোধ সেল (Violence Against Women & Children)",
                description = "নারী ও শিশু নির্যাতন রোধে দ্রুত সহায়তা",
                phone = "109",
                category = "Women & Children"
            ),
            SupabaseHotline(
                id = 4,
                name = "চাইল্ড হেল্পলাইন (Child Helpline)",
                description = "শিশুদের যেকোনো সুরক্ষায় সহায়ক ফোন লাইন",
                phone = "1098",
                category = "Children"
            ),
            SupabaseHotline(
                id = 5,
                name = "স্বাস্থ্য বাতায়ন (Health Helpline)",
                description = "২৪ ঘণ্টা ডাক্তার পরামর্শ ও চিকিৎসা সহায়তা",
                phone = "16263",
                category = "Health"
            )
        )
    }
}

