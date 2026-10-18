package com.safetravel.tracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseSubscriptionPlan
import com.safetravel.tracker.ui.theme.*

/**
 * Modern cyber frosted-glass card for SafeTravel premium subscription packages and credit passes.
 * Features dynamic neon gradient borders, prominent pricing with instant discount calculations,
 * and high-contrast contextual action buttons.
 */
@Composable
fun PremiumPlanCard(
    plan: SupabaseSubscriptionPlan,
    isPremium: Boolean,
    currentActivePlan: SupabaseSubscriptionPlan?,
    daysRemaining: Long?,
    hasPending: Boolean,
    onSelectPlan: (SupabaseSubscriptionPlan) -> Unit,
    onUpgradePlan: (SupabaseSubscriptionPlan) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasDiscount = plan.discountPrice != null && plan.discountPrice < plan.price
    val effectivePrice = plan.discountPrice ?: plan.price
    val isCurrentActivePlan = isPremium && (plan.id == currentActivePlan?.id)
    val isLowerTier = isPremium && !isCurrentActivePlan && currentActivePlan != null && plan.durationDays < currentActivePlan.durationDays
    val isHigherTier = isPremium && !isCurrentActivePlan && currentActivePlan != null && plan.durationDays >= currentActivePlan.durationDays

    // Card border brush based on state
    val borderBrush = when {
        isCurrentActivePlan -> Brush.horizontalGradient(
            listOf(
                Emerald400,
                Color(0xFF34D399),
                Emerald400.copy(alpha = 0.5f)
            )
        )
        plan.isPopular -> Brush.horizontalGradient(
            listOf(
                PremiumGold,
                Color(0xFFFBBF24),
                Color(0xFFD97706),
                PremiumGold.copy(alpha = 0.4f)
            )
        )
        else -> Brush.horizontalGradient(
            listOf(
                Cyan500.copy(alpha = 0.5f),
                Slate700.copy(alpha = 0.5f),
                Slate800
            )
        )
    }

    val borderWidth = when {
        isCurrentActivePlan || plan.isPopular -> 1.5.dp
        else -> 1.dp
    }

    // Deep frosted glass card background
    val cardBackgroundBrush = Brush.verticalGradient(
        colors = when {
            isCurrentActivePlan -> listOf(
                Color(0xFF0F241D).copy(alpha = 0.85f),
                Color(0xFF091410).copy(alpha = 0.95f)
            )
            plan.isPopular -> listOf(
                Color(0xFF1E1B10).copy(alpha = 0.85f),
                Color(0xFF0F101A).copy(alpha = 0.95f)
            )
            else -> listOf(
                Color(0xFF0F172A).copy(alpha = 0.85f),
                Color(0xFF090D1A).copy(alpha = 0.95f)
            )
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(borderWidth, borderBrush)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardBackgroundBrush)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Row: Badges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        isCurrentActivePlan -> {
                            Surface(
                                color = Emerald400.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "CURRENT ACTIVE PLAN",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Emerald400,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        plan.isPopular -> {
                            Surface(
                                color = PremiumGold.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.6f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = PremiumGold,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = "MOST POPULAR",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PremiumGold,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        else -> {
                            Surface(
                                color = Cyan500.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.3f))
                            ) {
                                Text(
                                    text = "SECURE PASS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Cyan400,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Duration or Credits Pill
                    Surface(
                        color = Slate800.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f))
                    ) {
                        val badgeLabel = when {
                            plan.planType == "trip_credits" || plan.billingPeriod == "pay_per_trip" -> "${plan.creditsCount} TRIPS PACK"
                            plan.durationDays == 1 -> "24 HOURS PASS"
                            else -> "${plan.durationDays} DAYS PASS"
                        }
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (plan.planType == "trip_credits") Icons.Default.DirectionsCar else Icons.Default.Schedule,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = badgeLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate200
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Plan Title
                Text(
                    text = plan.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 0.3.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Price Section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "BDT",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Cyan400,
                        modifier = Modifier.padding(bottom = 3.dp, end = 4.dp)
                    )
                    Text(
                        text = "${effectivePrice.toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )

                    if (hasDiscount) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "BDT ${plan.price.toInt()}",
                            fontSize = 14.sp,
                            color = Slate500,
                            textDecoration = TextDecoration.LineThrough,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))
                        val savings = (plan.price - effectivePrice).toInt()
                        Surface(
                            color = Emerald400.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = "SAVE BDT $savings",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Emerald400,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val subText = if (plan.planType == "trip_credits" || plan.billingPeriod == "pay_per_trip") {
                        " / ${plan.creditsCount} trips"
                    } else {
                        " / ${plan.durationDays} days"
                    }
                    Text(
                        text = subText,
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                    )
                }

                // Daily or Per-Trip Cost breakdown
                val unitCost = when {
                    plan.planType == "trip_credits" && plan.creditsCount > 0 -> (effectivePrice / plan.creditsCount).toInt()
                    plan.durationDays > 1 -> (effectivePrice / plan.durationDays).toInt()
                    else -> null
                }
                if (unitCost != null) {
                    Text(
                        text = "~ BDT $unitCost ${if (plan.planType == "trip_credits") "per single trip" else "per day coverage"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate400,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Hairline Divider
                HorizontalDivider(
                    thickness = 1.dp,
                    color = Slate700.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Features Checklist
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    plan.features.forEach { feat ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(Emerald500.copy(alpha = 0.18f))
                                    .border(1.dp, Emerald400.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Emerald400,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = feat,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Normal,
                                color = Slate200
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contextual Action Button
                when {
                    isCurrentActivePlan -> {
                        if (daysRemaining != null && daysRemaining <= 7) {
                            Button(
                                onClick = { onSelectPlan(plan) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                contentPadding = PaddingValues()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(listOf(Emerald500, Cyan500))
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Autorenew,
                                            contentDescription = null,
                                            tint = Color(0xFF090D1A),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Renew Plan (+${plan.durationDays} Days)",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFF090D1A)
                                        )
                                    }
                                }
                            }
                        } else {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Slate800.copy(alpha = 0.9f),
                                border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxSize(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Emerald400,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Active Plan (${daysRemaining ?: 0} Days Remaining)",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Emerald400
                                    )
                                }
                            }
                        }
                    }
                    isLowerTier -> {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Slate800.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, Slate700.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = Slate400,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Included in Active Plan",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Slate400
                                )
                            }
                        }
                    }
                    isHigherTier -> {
                        Button(
                            onClick = { onUpgradePlan(plan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(listOf(Cyan500, Color(0xFF0284C7)))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Upgrade,
                                        contentDescription = null,
                                        tint = Color(0xFF090D1A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Upgrade to ${plan.name}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFF090D1A)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Button(
                            onClick = { onSelectPlan(plan) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            val buttonGradient = when {
                                hasPending -> Brush.horizontalGradient(listOf(Slate800, Slate800))
                                plan.isPopular -> Brush.horizontalGradient(
                                    listOf(PremiumGold, Color(0xFFFBBF24), Color(0xFFD97706))
                                )
                                else -> Brush.horizontalGradient(listOf(Cyan500, Emerald500))
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(buttonGradient),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hasPending) Icons.Default.HourglassTop else Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = if (hasPending) Color(0xFFF59E0B) else Color(0xFF090D1A),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (hasPending) "Verification In Progress" else "Subscribe via Mobile Banking",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (hasPending) Color(0xFFF59E0B) else Color(0xFF090D1A)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
