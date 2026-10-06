package com.safetravel.tracker.supabase

import android.util.Log

object SafetyTipManager {
    private const val TAG = "SafetyTipManager"

    fun getSafetyTipService(): SafetyTipService? {
        return SupabaseClient.getService(SafetyTipService::class.java)
    }

    suspend fun fetchTipsWithStatus(userId: String?): Result<List<SupabaseSafetyTip>> {
        if (!SupabaseClient.isConfigured) {
            Log.w(TAG, "Supabase not configured. Using offline safety tips.")
            return Result.success(getFallbackTips())
        }

        try {
            val auth = "Bearer ${SupabaseClient.supabaseAnonKey}"
            val tipsResponse = getSafetyTipService()?.getSafetyTips(auth)
            
            if (tipsResponse?.isSuccessful == true && tipsResponse.body() != null) {
                val tips = tipsResponse.body()!!
                
                // If user is logged in, fetch their progress
                if (userId != null) {
                    val userAuth = SupabaseClient.getValidAuthHeader()
                    val progressResponse = getSafetyTipService()?.getUserTipProgress(userAuth, "eq.$userId")
                    
                    if (progressResponse?.isSuccessful == true && progressResponse.body() != null) {
                        val progressList = progressResponse.body()!!
                        val readTipIds = progressList.map { it.tipId }.toSet()
                        
                        tips.forEach { tip ->
                            tip.isRead = readTipIds.contains(tip.id)
                        }
                    }
                }
                
                return Result.success(tips)
            }
            Log.w(TAG, "Failed to fetch safety tips from server: ${tipsResponse?.code()}. Using high-quality offline data.")
            return Result.success(getFallbackTips())
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching safety tips, using high-quality offline data", e)
            return Result.success(getFallbackTips())
        }
    }

    suspend fun markAsRead(userId: String, tipId: String): Result<Unit> {
        if (!SupabaseClient.isConfigured) return Result.success(Unit)

        try {
            val userAuth = SupabaseClient.getValidAuthHeader()
            val progress = UserTipProgress(userId = userId, tipId = tipId, isRead = true)
            val response = getSafetyTipService()?.markTipAsRead(userAuth, progress)
            
            return if (response?.isSuccessful == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to mark as read: ${response?.code()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking tip as read", e)
            return Result.failure(e)
        }
    }

    fun getFallbackTips(): List<SupabaseSafetyTip> {
        return listOf(
            SupabaseSafetyTip(
                id = "tip-1",
                title = "ডকুমেন্ট যাচাই",
                description = "আপনার প্রয়োজনীয় ডকুমেন্টের ডিজিটাল কপি ফোনে সেভ রাখুন।",
                content = "আপনার NID, পাসপোর্ট বা প্রয়োজনীয় ডকুমেন্টের একটি করে ডিজিটাল কপি ফোনে সেভ রাখুন। ভ্রমণের সময় হারানো বা চুরির ক্ষেত্রে এটি খুবই কাজে লাগে।",
                category = "Pre-Journey"
            ),
            SupabaseSafetyTip(
                id = "tip-2",
                title = "জরুরি কন্টাক্ট",
                description = "বিশ্বস্ত মানুষকে গন্তব্য এবং ফেরার সময় জানিয়ে রাখুন।",
                content = "আপনার গন্তব্য এবং প্রত্যাশিত ফেরার সময় অন্তত একজন বিশ্বস্ত মানুষকে জানিয়ে রাখুন। আপনার অবস্থান সম্পর্কে তারা নিশ্চিত হতে পারবে।",
                category = "Pre-Journey"
            ),
            SupabaseSafetyTip(
                id = "tip-3",
                title = "ডিভাইস চার্জ",
                description = "ফোনের ব্যাটারি অন্তত ৮০% চার্জ নিশ্চিত করুন।",
                content = "ফোনের ব্যাটারি অন্তত ৮০% চার্জ আছে কি না নিশ্চিত করুন এবং একটি পাওয়ার ব্যাংক সাথে রাখুন। জরুরি যোগাযোগের জন্য ফোন সচল রাখা আবশ্যিক।",
                category = "Pre-Journey"
            ),
            SupabaseSafetyTip(
                id = "tip-4",
                title = "লোকেশন শেয়ারিং",
                description = "প্রিয়জনের সাথে লাইভ লোকেশন শেয়ার অন করুন।",
                content = "গুগল ম্যাপ বা আমাদের অ্যাপের মাধ্যমে আপনার প্রিয়জনের সাথে লাইভ লোকেশন শেয়ার অন করুন। এটি আপনার নিরাপত্তা বহুগুণ বাড়িয়ে দেয়।",
                category = "Pre-Journey"
            ),
            SupabaseSafetyTip(
                id = "tip-5",
                title = "অপরিচিত খাবার",
                description = "অপরিচিত কারো দেওয়া খাবার বা পানীয় গ্রহণ করবেন না।",
                content = "বাসে বা ট্রেনে অপরিচিত কারো দেওয়া পানি, পানীয় বা খাবার গ্রহণ করা থেকে বিরত থাকুন। চেতনানাশক ড্রাগ মিশ্রিত খাবারের ঝুঁকি থাকে।",
                category = "Transport"
            ),
            SupabaseSafetyTip(
                id = "tip-6",
                title = "ব্যাগ নিরাপত্তা",
                description = "ভিড়ের মধ্যে ব্যাগ সবসময় সামনের দিকে রাখুন।",
                content = "ভিড়ের মধ্যে ব্যাগ সবসময় সামনের দিকে ঝুলিয়ে রাখুন। দামী জিনিস পকেটে না রেখে ব্যাগের চেইন দেওয়া পকেটে রাখুন।",
                category = "Transport"
            )
        )
    }
}

