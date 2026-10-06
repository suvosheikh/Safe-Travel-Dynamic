package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import com.safetravel.tracker.ads.AdMobManager
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.supabase.SupabasePointsLog
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WalletScreen(vm: SafeTravelViewModel) {
    val profile by vm.userProfile.collectAsState()
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    // Remote configs for points
    val baseTripPoints = remember { SupabaseManager.getPointConfig("points_per_trip", 50) }
    val premiumTripPoints = remember { SupabaseManager.getPointConfig("points_per_trip_premium", 100) }
    val pointsPerKm = remember { SupabaseManager.getPointConfig("points_per_km", 2) }
    val costPremium7d = remember { SupabaseManager.getPointConfig("points_redeem_premium_7d", 500) }
    val costPremium30d = remember { SupabaseManager.getPointConfig("points_redeem_premium_30d", 1500) }
    val costCredit = remember { SupabaseManager.getPointConfig("points_redeem_credit", 50) }

    // Real Points Logs
    var pointsLogs by remember { mutableStateOf<List<SupabasePointsLog>>(emptyList()) }
    var isLoadingLogs by remember { mutableStateOf(false) }
    var isRedeeming by remember { mutableStateOf(false) }

    val currentPoints = profile?.pointsBalance ?: 0
    val currentCredits = profile?.tripCredits ?: 0
    val isPremium = profile?.isPremium == true

    LaunchedEffect(profile?.id) {
        val uid = profile?.id
        if (!uid.isNullOrBlank()) {
            isLoadingLogs = true
            SupabaseManager.fetchPointsLogs(uid).onSuccess {
                pointsLogs = it
            }
            isLoadingLogs = false
        }
    }

    var isLoadingAd by remember { mutableStateOf(false) }

    fun handleRedeem(benefitType: String, cost: Int) {
        val uid = profile?.id ?: return
        if (currentPoints < cost) {
            vm.actionFeedbackMessage.value = "Insufficient points! (Required: $cost PTS)"
            return
        }
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        isRedeeming = true
        vm.viewModelScope.launch {
            val result = SupabaseManager.redeemPoints(uid, cost, benefitType, currentPoints, currentCredits)
            result.onSuccess {
                vm.syncProfile()
                SupabaseManager.fetchPointsLogs(uid).onSuccess { pointsLogs = it }
                vm.actionFeedbackMessage.value = "Redeemed successfully! Rewards unlocked."
            }.onFailure {
                vm.actionFeedbackMessage.value = "Redemption failed: ${it.localizedMessage}"
            }
            isRedeeming = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 600.dp)
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp)
        ) {
            // 1. Summary Card layout
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate800),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.5f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E293B)
                                    )
                                )
                            )
                            .padding(18.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("SafeTravel Points Wallet", fontSize = 12.sp, color = Slate300, fontWeight = FontWeight.Bold)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isPremium) PremiumGold else Slate700)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        if (isPremium) "PREMIUM TIER" else "STANDARD TIER",
                                        fontSize = 8.sp,
                                        color = if (isPremium) Color.Black else Color.White,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "$currentPoints PTS",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Safe Trip Reward: +$baseTripPoints PTS (${if (isPremium) "+$premiumTripPoints Premium" else "+$pointsPerKm PTS/km bonus"})",
                                fontSize = 10.sp,
                                color = Emerald400,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 2. Redeem Rewards & Premium Access
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = PremiumGold, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Redeem Points for Premium & Perks",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold
                    )
                }
                Text(
                    text = "Exchange your safe travel loyalty points to unlock premium safety features and emergency credits.",
                    fontSize = 11.sp,
                    color = Slate300,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Option 1: 7-Day Premium
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (currentPoints >= costPremium7d) PremiumGold.copy(alpha = 0.4f) else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("7 Days Full Premium Access", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Unlimited guardians, extended audio blackbox, route deviation alerts.", fontSize = 10.sp, color = Slate300)
                            }
                            Button(
                                onClick = { handleRedeem("premium_7d", costPremium7d) },
                                enabled = currentPoints >= costPremium7d && !isRedeeming,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, disabledContainerColor = Slate700),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$costPremium7d PTS", color = if (currentPoints >= costPremium7d) Color.Black else Slate400, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // Option 2: 30-Day Premium
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (currentPoints >= costPremium30d) PremiumGold.copy(alpha = 0.4f) else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("30 Days Full Premium Access", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Full 1-month VIP safety protocols and priority dispatch.", fontSize = 10.sp, color = Slate300)
                            }
                            Button(
                                onClick = { handleRedeem("premium_30d", costPremium30d) },
                                enabled = currentPoints >= costPremium30d && !isRedeeming,
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumGold, disabledContainerColor = Slate700),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$costPremium30d PTS", color = if (currentPoints >= costPremium30d) Color.Black else Slate400, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }

                    // Option 3: Emergency Trip Credit
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (currentPoints >= costCredit) Emerald400.copy(alpha = 0.4f) else Slate700)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("+1 Emergency Trip Credit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("Current Balance: $currentCredits credits.", fontSize = 10.sp, color = Slate300)
                            }
                            Button(
                                onClick = { handleRedeem("credit", costCredit) },
                                enabled = currentPoints >= costCredit && !isRedeeming,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald400, disabledContainerColor = Slate700),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("$costCredit PTS", color = if (currentPoints >= costCredit) Slate900 else Slate400, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // 3. Earn section
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 2.dp)
                ) {
                    Icon(Icons.Default.Stars, contentDescription = "Earn", tint = Emerald400, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Earn Instant Loyalty Rewards",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400
                    )
                }
                Text(
                    text = "Claim +50 loyalty travel credits immediately by watching sponsor compliance ad alerts.",
                    fontSize = 11.sp,
                    color = Slate300,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val isRewardedAdAvailable = remember(isPremium) {
                    AdMobManager.shouldShowAd(context, "rewarded", isPremium)
                }
                val rewardCredits = remember { AdMobManager.getRewardCreditsAmount(context) }

                if (isRewardedAdAvailable) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.dp, Slate700), RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Slate800)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("স্পন্সর ভিডিও বিজ্ঞাপন", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Text("ভিডিও দেখলে পাবেন +$rewardCredits জরুরি SOS ক্রেডিট ও +50 লয়্যালটি পয়েন্ট", fontSize = 10.sp, color = Slate300)
                            }
                            Button(
                                onClick = {
                                    val activity = AdMobManager.findActivity(context) ?: return@Button
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    isLoadingAd = true
                                    AdMobManager.loadAndShowRewardedAd(
                                        activity = activity,
                                        onLoading = { isLoadingAd = true },
                                        onRewardEarned = { earnedCredits ->
                                            isLoadingAd = false
                                            profile?.let { currentProfile ->
                                                vm.viewModelScope.launch {
                                                    val rewardPoints = 50
                                                    val newBalance = currentProfile.pointsBalance + rewardPoints
                                                    val newCredits = currentProfile.tripCredits + earnedCredits
                                                    SupabaseManager.updateProfileFields(
                                                        currentProfile.id,
                                                        mapOf(
                                                            "points_balance" to newBalance,
                                                            "trip_credits" to newCredits
                                                        )
                                                    )
                                                    SupabaseManager.logPoints(currentProfile.id, rewardPoints, "AdMob Rewarded Safety Ad")
                                                    vm.refreshData()
                                                    vm.syncProfile()
                                                    SupabaseManager.fetchPointsLogs(currentProfile.id).onSuccess { pointsLogs = it }
                                                    vm.actionFeedbackMessage.value = "পুরস্কার সম্পন্ন! +$earnedCredits জরুরি SOS ক্রেডিট যোগ হয়েছে।"
                                                }
                                            }
                                        },
                                        onFailedOrDismissed = { reason ->
                                            isLoadingAd = false
                                            if (reason != "dismissed_early") {
                                                vm.actionFeedbackMessage.value = "বিজ্ঞাপন লোড হতে সমস্যা হয়েছে। অনুগ্রহ করে কিছুক্ষণ পর চেষ্টা করুন।"
                                            }
                                        }
                                    )
                                },
                                enabled = !isLoadingAd,
                                colors = ButtonDefaults.buttonColors(containerColor = Emerald400),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isLoadingAd) {
                                    CircularProgressIndicator(color = Slate900, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Text("ভিডিও দেখুন", color = Slate900, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // 4. Real Points Transaction Logs
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = Emerald400, modifier = Modifier.size(18.dp))
                    Text(
                        text = "Points Transaction Logs",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald400
                    )
                }

                if (isLoadingLogs) {
                    Box(modifier = Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Emerald400, modifier = Modifier.size(24.dp))
                    }
                } else if (pointsLogs.isEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.MilitaryTech, contentDescription = "No Points", tint = Slate500, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No Points Transactions Yet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Slate300)
                            Text("Complete a safe trip to earn your first loyalty reward.", fontSize = 10.sp, color = Slate400, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    pointsLogs.forEach { log ->
                        val isPositive = log.pointsAdded >= 0
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = Slate800)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(log.source, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(log.timestamp ?: "", fontSize = 9.sp, color = Slate300)
                                }
                                Text(
                                    text = if (isPositive) "+${log.pointsAdded} pts" else "${log.pointsAdded} pts",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isPositive) PremiumGold else Rose500
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

    }
}
