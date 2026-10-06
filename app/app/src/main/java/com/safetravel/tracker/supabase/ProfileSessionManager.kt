package com.safetravel.tracker.supabase

import android.content.SharedPreferences
import android.util.Log

/**
 * Dedicated support manager for securely caching, serializing,
 * and recovering full Supabase user profiles across app launches.
 */
object ProfileSessionManager {
    private const val TAG = "ProfileSessionManager"

    fun saveProfile(prefs: SharedPreferences, profile: SupabaseProfile) {
        try {
            val json = SupabaseClient.moshi.adapter(SupabaseProfile::class.java).toJson(profile)
            prefs.edit().apply {
                putString("prof_json", json)
                putString("prof_id", profile.id)
                putString("prof_name", profile.fullName)
                putString("prof_phone", profile.phoneNumber)
                putString("prof_avatar", profile.avatarUrl)
                putBoolean("prof_premium", profile.isPremium == true)
                putInt("prof_credits", profile.tripCredits)
                putInt("prof_points", profile.pointsBalance)
                putString("prof_role", profile.role)
                putString("prof_dob", profile.dob)
                putString("prof_gender", profile.gender)
                putString("prof_nid", profile.nidPassport)
                putString("prof_occupation", profile.occupation)
                putString("prof_present_addr", profile.presentAddress)
                putString("prof_permanent_addr", profile.permanentAddress)
                putString("prof_blood", profile.bloodGroup)
                putString("prof_allergies", profile.allergies)
                putString("prof_chronic", profile.chronicConditions)
                putString("prof_meds", profile.currentMedications)
                putString("prof_height_weight", profile.heightWeight)
                putString("prof_special_needs", profile.specialNeeds)
                putString("prof_premium_until", profile.premiumUntil)
                apply()
            }
            Log.d(TAG, "Full profile session saved successfully for user: ${profile.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to serialize and save full profile", e)
        }
    }

    fun loadProfile(prefs: SharedPreferences, userId: String?): SupabaseProfile? {
        val json = prefs.getString("prof_json", null)
        if (!json.isNullOrBlank()) {
            try {
                val profile = SupabaseClient.moshi.adapter(SupabaseProfile::class.java).fromJson(json)
                if (profile != null && (userId == null || profile.id == userId)) {
                    Log.d(TAG, "Full profile loaded from cached JSON for user: ${profile.id}")
                    return profile
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse cached profile JSON", e)
            }
        }

        val pId = prefs.getString("prof_id", userId) ?: return null
        return SupabaseProfile(
            id = pId,
            fullName = prefs.getString("prof_name", "User") ?: "User",
            phoneNumber = prefs.getString("prof_phone", null),
            avatarUrl = prefs.getString("prof_avatar", null),
            isPremium = prefs.getBoolean("prof_premium", false),
            tripCredits = prefs.getInt("prof_credits", 0),
            pointsBalance = prefs.getInt("prof_points", 0),
            role = prefs.getString("prof_role", "user") ?: "user",
            dob = prefs.getString("prof_dob", null),
            gender = prefs.getString("prof_gender", null),
            nidPassport = prefs.getString("prof_nid", null),
            occupation = prefs.getString("prof_occupation", null),
            presentAddress = prefs.getString("prof_present_addr", null),
            permanentAddress = prefs.getString("prof_permanent_addr", null),
            bloodGroup = prefs.getString("prof_blood", null),
            allergies = prefs.getString("prof_allergies", null),
            chronicConditions = prefs.getString("prof_chronic", null),
            currentMedications = prefs.getString("prof_meds", null),
            heightWeight = prefs.getString("prof_height_weight", null),
            specialNeeds = prefs.getString("prof_special_needs", null),
            premiumUntil = prefs.getString("prof_premium_until", null)
        )
    }

    fun clear(prefs: SharedPreferences) {
        prefs.edit().apply {
            remove("prof_json")
            remove("prof_id")
            remove("prof_name")
            remove("prof_phone")
            remove("prof_avatar")
            remove("prof_premium")
            remove("prof_credits")
            remove("prof_points")
            remove("prof_role")
            remove("prof_dob")
            remove("prof_gender")
            remove("prof_nid")
            remove("prof_occupation")
            remove("prof_present_addr")
            remove("prof_permanent_addr")
            remove("prof_blood")
            remove("prof_allergies")
            remove("prof_chronic")
            remove("prof_meds")
            remove("prof_height_weight")
            remove("prof_special_needs")
            remove("prof_premium_until")
            apply()
        }
    }
}
