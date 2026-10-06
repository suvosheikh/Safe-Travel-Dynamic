package com.safetravel.tracker.supabase

import android.util.Log

object BloodBankManager {
    private const val TAG = "BloodBankManager"

    fun getBloodBankService(): BloodBankService? {
        return SupabaseClient.getService(BloodBankService::class.java)
    }

    suspend fun fetchBloodBanks(): Result<List<SupabaseBloodBank>> {
        if (SupabaseClient.isConfigured) {
            try {
                val auth = SupabaseClient.getValidAuthHeader()
                
                val response = getBloodBankService()?.getBloodBanks(auth)
                
                if (response?.isSuccessful == true && response.body() != null) {
                    return Result.success(response.body()!!)
                }
                Log.w(TAG, "Failed to fetch blood banks from server: ${response?.code()}. Using high-quality offline data.")
                return Result.success(getFallbackBloodBanks())
            } catch (e: Exception) {
                Log.w(TAG, "Error fetching blood banks, using high-quality offline data", e)
                return Result.success(getFallbackBloodBanks())
            }
        }
        Log.w(TAG, "Supabase not configured. Using offline blood banks.")
        return Result.success(getFallbackBloodBanks())
    }

    fun getFallbackBloodBanks(): List<SupabaseBloodBank> {
        return listOf(
            SupabaseBloodBank(
                id = 1,
                name = "বাংলাদেশ রেড ক্রিসেন্ট সোসাইটি ব্লাড ব্যাংক",
                type = "NGO",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Mohammadpur",
                address = "৭/৮, আওরঙ্গজেব রোড, মোহাম্মদপুর, ঢাকা",
                phoneNumber = "02-9116563",
                emergencyPhone = "01811-458521",
                latitude = 23.7660,
                longitude = 90.3620
            ),
            SupabaseBloodBank(
                id = 2,
                name = "কোয়ান্টাম ফাউন্ডেশন ব্লাড সেন্টার",
                type = "NGO",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Shanti Nagar",
                address = "৩১/ভি, শিল্পাচার্য জয়নুল আবেদীন সড়ক, শান্তিনগর, ঢাকা",
                phoneNumber = "02-9351969",
                emergencyPhone = "01714-047870",
                latitude = 23.7380,
                longitude = 90.4130
            ),
            SupabaseBloodBank(
                id = 3,
                name = "সন্ধানী ব্লাড ব্যাংক (DMCH Unit)",
                type = "NGO",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Shahbag",
                address = "ঢাকা মেডিকেল কলেজ, ঢাকা",
                phoneNumber = "02-9668690",
                emergencyPhone = "01552-302325",
                latitude = 23.7275,
                longitude = 90.3980
            ),
            SupabaseBloodBank(
                id = 4,
                name = "পুলিশ ব্লাড ব্যাংক",
                type = "Govt",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Malibagh",
                address = "পুলিশ লাইন্স, মালিবাগ, ঢাকা",
                phoneNumber = "02-9330869",
                emergencyPhone = "01713-398487",
                latitude = 23.7480,
                longitude = 90.4110
            ),
            SupabaseBloodBank(
                id = 5,
                name = "বাধঁন ব্লাড ব্যাংক (TSC)",
                type = "NGO",
                division = "Dhaka",
                district = "Dhaka",
                thana = "Shahbag",
                address = "টিএসসি, ঢাকা বিশ্ববিদ্যালয়, ঢাকা",
                phoneNumber = "02-8629042",
                emergencyPhone = "01534-510825",
                latitude = 23.7330,
                longitude = 90.3960
            )
        )
    }
}

