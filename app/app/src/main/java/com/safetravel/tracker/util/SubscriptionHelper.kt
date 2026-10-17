package com.safetravel.tracker.util

import com.safetravel.tracker.supabase.SupabasePaymentTransaction
import com.safetravel.tracker.supabase.SupabaseProfile
import com.safetravel.tracker.supabase.SupabasePromoCode
import com.safetravel.tracker.supabase.SupabaseSubscriptionPlan
import java.util.Date

/**
 * Dedicated helper for Subscription, Trip Credit, and Promo Code business logic.
 * Enforces zero-mock data policy, precise validity calculations, and priority ordering.
 */
object SubscriptionHelper {

    data class PromoCalculationResult(
        val isValid: Boolean,
        val errorMessage: String? = null,
        val discountAmount: Double = 0.0,
        val finalPrice: Double = 0.0,
        val bonusDays: Int = 0,
        val bonusCredits: Int = 0,
        val promoCode: SupabasePromoCode? = null
    )

    /**
     * Checks if the user currently holds an active unlimited Time Pass.
     */
    fun isTimePassActive(profile: SupabaseProfile?): Boolean {
        if (profile == null) return false
        if (profile.isPremium != true) return false
        val expiryStr = profile.premiumUntil ?: return true
        val expiryDate = DateTimeUtils.parseIsoToDate(expiryStr) ?: return true
        return expiryDate.after(Date())
    }

    /**
     * Checks if the user has available and unexpired Trip Credits.
     */
    fun hasValidTripCredits(profile: SupabaseProfile?): Boolean {
        if (profile == null) return false
        if (profile.tripCredits <= 0) return false
        val validityStr = profile.creditsValidUntil ?: return true
        val validityDate = DateTimeUtils.parseIsoToDate(validityStr) ?: return true
        return validityDate.after(Date())
    }

    /**
     * Determines if a trip credit should be deducted upon trip completion.
     * Priority Rule: Active Time Pass users have unlimited trips, so NO credits are deducted.
     */
    fun shouldDeductTripCredit(profile: SupabaseProfile?): Boolean {
        if (profile == null) return false
        if (isTimePassActive(profile)) return false
        return hasValidTripCredits(profile)
    }

    /**
     * Validates and applies a promo code to a selected subscription plan.
     */
    fun calculatePromoDiscount(
        promo: SupabasePromoCode?,
        plan: SupabaseSubscriptionPlan
    ): PromoCalculationResult {
        val basePrice = (plan.discountPrice ?: plan.price)

        if (promo == null) {
            return PromoCalculationResult(
                isValid = true,
                finalPrice = basePrice,
                discountAmount = 0.0
            )
        }

        if (!promo.isActive) {
            return PromoCalculationResult(
                isValid = false,
                errorMessage = "This promo code is no longer active.",
                finalPrice = basePrice
            )
        }

        // Expiry check
        if (!promo.validUntil.isNullOrBlank()) {
            val expDate = DateTimeUtils.parseIsoToDate(promo.validUntil)
            if (expDate != null && expDate.before(Date())) {
                return PromoCalculationResult(
                    isValid = false,
                    errorMessage = "This promo code has expired.",
                    finalPrice = basePrice
                )
            }
        }

        // Usage limit check
        if (promo.maxUses != null && promo.usedCount >= promo.maxUses) {
            return PromoCalculationResult(
                isValid = false,
                errorMessage = "Promo code usage limit has been reached.",
                finalPrice = basePrice
            )
        }

        // Applicable plan types check
        val planType = if (plan.planType.isNotBlank()) plan.planType else "time_pass"
        if (promo.applicablePlanTypes.isNotEmpty() && !promo.applicablePlanTypes.contains(planType)) {
            val required = if (planType == "trip_credits") "time passes" else "trip credit packs"
            return PromoCalculationResult(
                isValid = false,
                errorMessage = "This promo code is only valid for $required.",
                finalPrice = basePrice
            )
        }

        // Min order check
        if (basePrice < promo.minOrderAmount) {
            return PromoCalculationResult(
                isValid = false,
                errorMessage = "Minimum plan price of BDT ${promo.minOrderAmount.toInt()} required for this code.",
                finalPrice = basePrice
            )
        }

        var discountAmount = 0.0
        var bonusDays = promo.bonusDays
        var bonusCredits = promo.bonusCredits

        when (promo.discountType.lowercase()) {
            "percent" -> {
                val rawDiscount = basePrice * (promo.discountValue / 100.0)
                discountAmount = if (promo.maxDiscountAmount != null && promo.maxDiscountAmount > 0) {
                    rawDiscount.coerceAtMost(promo.maxDiscountAmount)
                } else {
                    rawDiscount
                }
            }
            "fixed" -> {
                discountAmount = promo.discountValue.coerceAtMost(basePrice)
            }
            "bonus_days" -> {
                bonusDays = if (promo.bonusDays > 0) promo.bonusDays else promo.discountValue.toInt()
            }
            "bonus_credits" -> {
                bonusCredits = if (promo.bonusCredits > 0) promo.bonusCredits else promo.discountValue.toInt()
            }
            else -> {
                // Default fallback to flat discount
                discountAmount = promo.discountValue.coerceAtMost(basePrice)
            }
        }

        val finalPrice = (basePrice - discountAmount).coerceAtLeast(0.0)

        return PromoCalculationResult(
            isValid = true,
            discountAmount = discountAmount,
            finalPrice = finalPrice,
            bonusDays = bonusDays,
            bonusCredits = bonusCredits,
            promoCode = promo
        )
    }

    /**
     * Formats clean human-readable validity text for UI badges.
     */
    fun formatPlanSubtitle(plan: SupabaseSubscriptionPlan): String {
        return if (plan.planType == "trip_credits") {
            "${plan.creditsCount} Safe Trips (${plan.durationDays} Days Validity)"
        } else {
            "${plan.durationDays} Days Full Protection"
        }
    }
}
