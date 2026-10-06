package com.safetravel.tracker.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.supabase.SupabasePaymentTransaction
import com.safetravel.tracker.supabase.SupabaseSubscriptionPlan
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    vm: SafeTravelViewModel? = null,
    onBack: () -> Unit
) {
    BackHandler { onBack() }
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val profile = vm?.userProfile?.collectAsState()?.value
    val isPremium = profile?.isPremium == true
    val premiumUntil = profile?.premiumUntil

    // Dynamic Plans & Transactions from Supabase
    var plans by remember { mutableStateOf<List<SupabaseSubscriptionPlan>>(emptyList()) }
    var userTransactions by remember { mutableStateOf<List<SupabasePaymentTransaction>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Selected plan for checkout modal
    var checkoutPlan by remember { mutableStateOf<SupabaseSubscriptionPlan?>(null) }
    var selectedMethod by remember { mutableStateOf("bkash") }
    var senderNumber by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var isSubmittingPayment by remember { mutableStateOf(false) }

    // Fetch initial data
    LaunchedEffect(Unit) {
        isLoading = true
        val plansResult = SupabaseManager.fetchSubscriptionPlans()
        if (plansResult.isSuccess) {
            plans = plansResult.getOrDefault(emptyList()).filter { it.isActive }
        }

        if (profile?.id != null) {
            val txResult = SupabaseManager.fetchUserPaymentTransactions(profile.id)
            if (txResult.isSuccess) {
                userTransactions = txResult.getOrDefault(emptyList())
            }
        }
        isLoading = false
    }

    val pendingTx = userTransactions.firstOrNull { it.status == "pending" }

    // Admin Numbers from Remote Config
    val bkashNum = remember { SupabaseManager.getCachedConfig("payment_manual_bkash", "01700000000") }
    val nagadNum = remember { SupabaseManager.getCachedConfig("payment_manual_nagad", "01800000000") }
    val rocketNum = remember { SupabaseManager.getCachedConfig("payment_manual_rocket", "01900000000") }
    val instructions = remember { 
        SupabaseManager.getCachedConfig(
            "payment_instructions", 
            "Send Money to our verified number, copy your TrxID, and submit below for verification."
        ) 
    }

    val activeAdminNumber = when (selectedMethod) {
        "nagad" -> nagadNum
        "rocket" -> rocketNum
        else -> bkashNum
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(top = 24.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Navigation & Title
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(38.dp)
                            .background(Slate800, CircleShape)
                            .border(1.dp, Slate700, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = "Safety Premium Pass",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Elite Travel & Crisis Protection",
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Membership Status Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isPremium) Slate800 else Slate850),
                    border = BorderStroke(1.dp, if (isPremium) PremiumGold.copy(alpha = 0.5f) else Slate700)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isPremium) Brush.radialGradient(listOf(PremiumGold, Color(0xFFD97706)))
                                    else Brush.radialGradient(listOf(Slate700, Slate800))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPremium) Icons.Default.WorkspacePremium else Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isPremium) Slate950 else Slate300,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPremium) "ACTIVE PREMIUM MEMBER" else "STANDARD ACCOUNT",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isPremium) PremiumGold else Color.White
                            )

                            if (isPremium && !premiumUntil.isNullOrBlank()) {
                                Text(
                                    text = "Valid Until: $premiumUntil",
                                    fontSize = 11.sp,
                                    color = Emerald400,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            } else {
                                Text(
                                    text = "Upgrade for unlimited protection & priority response.",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // Pending Transaction Alert Card
            if (pendingTx != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2210)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.6f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.HourglassTop,
                                    contentDescription = null,
                                    tint = Color(0xFFF59E0B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PAYMENT VERIFICATION PENDING",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF59E0B)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Your manual mobile payment submission is under review by our operations room.",
                                fontSize = 12.sp,
                                color = Slate200,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Amount: BDT ${pendingTx.amount.toInt()}",
                                    fontSize = 11.sp,
                                    color = Slate300,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "TrxID: ${pendingTx.transactionId}",
                                    fontSize = 11.sp,
                                    color = Cyan400,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(22.dp))
                }
            }

            // Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Choose Your",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Safety Plan",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = PremiumGold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Plans List
            if (isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Cyan400,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } else if (plans.isEmpty()) {
                item {
                    Text(
                        text = "No subscription plans available at this moment.",
                        fontSize = 12.sp,
                        color = Slate400,
                        modifier = Modifier.padding(vertical = 30.dp)
                    )
                }
            } else {
                items(plans) { plan ->
                    val hasDiscount = plan.discountPrice != null && plan.discountPrice < plan.price
                    val effectivePrice = plan.discountPrice ?: plan.price

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate800),
                        border = BorderStroke(
                            1.dp,
                            if (plan.isPopular) PremiumGold.copy(alpha = 0.8f) else Slate700
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            // Top Row: Badges
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (plan.isPopular) {
                                    Surface(
                                        color = PremiumGold.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(6.dp),
                                        border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = PremiumGold,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "MOST POPULAR",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = PremiumGold
                                            )
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.width(1.dp))
                                }

                                Surface(
                                    color = Slate700.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    val badgeLabel = when {
                                        plan.billingPeriod == "pay_per_trip" -> "PAY-PER-TRIP"
                                        plan.durationDays == 1 -> "1 DAY PASS"
                                        else -> "${plan.durationDays} DAYS PASS"
                                    }
                                    Text(
                                        text = badgeLabel,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate300,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Plan Title
                            Text(
                                text = plan.name,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Price
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "BDT ${effectivePrice.toInt()}",
                                    fontSize = 24.sp,
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
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Text(
                                    text = " / ${plan.durationDays} days",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Features
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                plan.features.forEach { feat ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Emerald400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text(
                                            text = feat,
                                            fontSize = 12.sp,
                                            color = Slate300
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            // CTA Subscribe Button
                            Button(
                                onClick = {
                                    checkoutPlan = plan
                                    senderNumber = profile?.phoneNumber ?: ""
                                    transactionId = ""
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (plan.isPopular) PremiumGold else Cyan500
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Payment,
                                        contentDescription = null,
                                        tint = Slate950,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Subscribe via Mobile Banking",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Slate950
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Checkout & Manual Payment Modal
        if (checkoutPlan != null) {
            val effectivePrice = (checkoutPlan!!.discountPrice ?: checkoutPlan!!.price).toInt()
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = { if (!isSubmittingPayment) checkoutPlan = null },
                sheetState = sheetState,
                containerColor = Color.Transparent,
                scrimColor = Color.Black.copy(alpha = 0.70f),
                dragHandle = null,
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
            ) {
                GlassBottomSheetContainer(
                    topStartRadius = 28.dp,
                    topEndRadius = 28.dp,
                    maxHeightFraction = 0.90f,
                    scrollable = true
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp)
                            .imePadding()
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(Cyan500.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                                        .border(1.dp, Cyan500.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = null,
                                        tint = Cyan400,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "Upgrade Plan Checkout",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Manual Mobile Payment & Verification",
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                            }

                            IconButton(
                                onClick = { if (!isSubmittingPayment) checkoutPlan = null },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Slate800.copy(alpha = 0.7f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Slate400,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Plan Summary Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Slate900.copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, PremiumGold.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(PremiumGold.copy(alpha = 0.15f), CircleShape)
                                            .border(1.dp, PremiumGold.copy(alpha = 0.4f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WorkspacePremium,
                                            contentDescription = null,
                                            tint = PremiumGold,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = checkoutPlan!!.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${checkoutPlan!!.durationDays} Days Full Protection",
                                            fontSize = 11.sp,
                                            color = Slate400
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    if (checkoutPlan!!.discountPrice != null && checkoutPlan!!.discountPrice!! < checkoutPlan!!.price) {
                                        Text(
                                            text = "BDT ${checkoutPlan!!.price.toInt()}",
                                            fontSize = 11.sp,
                                            color = Slate400,
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                    }
                                    Text(
                                        text = "BDT $effectivePrice",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PremiumGold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Method Selector Chips
                        Text(
                            text = "SELECT PAYMENT GATEWAY",
                            fontSize = 10.sp,
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "bkash" to ("bKash" to Color(0xFFE2136E)),
                                "nagad" to ("Nagad" to Color(0xFFF7941D)),
                                "rocket" to ("Rocket" to Color(0xFF8C3494))
                            ).forEach { (key, info) ->
                                val (label, brandColor) = info
                                val isSelected = selectedMethod == key

                                Surface(
                                    color = if (isSelected) brandColor.copy(alpha = 0.22f) else Slate800.copy(alpha = 0.6f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) brandColor else Slate700
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            selectedMethod = key
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = brandColor,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = label,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else Slate300
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Number Display Box with Copy Button
                        Surface(
                            color = Slate800.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Slate700)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${selectedMethod.uppercase()} NUMBER (Send Money)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Cyan400,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = activeAdminNumber,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Button(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("Payment Number", activeAdminNumber))
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        Toast.makeText(context, "Number copied: $activeAdminNumber", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Cyan500.copy(alpha = 0.2f)),
                                    border = BorderStroke(1.dp, Cyan500.copy(alpha = 0.5f)),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = null,
                                        tint = Cyan400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "COPY",
                                        fontSize = 11.sp,
                                        color = Cyan400,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 3-Step Instruction Box
                        Surface(
                            color = Slate950.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Slate800)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Slate400,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "3-Step Payment Instructions:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Slate300
                                    )
                                }

                                Text(
                                    text = "1. Open your ${selectedMethod.replaceFirstChar { it.uppercase() }} app and select 'Send Money'.",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "2. Send exactly BDT $effectivePrice to $activeAdminNumber.",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    lineHeight = 15.sp
                                )
                                Text(
                                    text = "3. Copy the TrxID (Transaction ID) from the confirmation and paste it below.",
                                    fontSize = 11.sp,
                                    color = Slate400,
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Input: Sender Mobile Number
                        OutlinedTextField(
                            value = senderNumber,
                            onValueChange = { senderNumber = it },
                            label = { Text("Your Sender Mobile Number") },
                            placeholder = { Text("01XXXXXXXXX") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900.copy(alpha = 0.7f),
                                unfocusedContainerColor = Slate900.copy(alpha = 0.5f),
                                focusedBorderColor = Cyan500,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Cyan400,
                                unfocusedLabelColor = Slate400
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Input: Transaction ID (TrxID)
                        OutlinedTextField(
                            value = transactionId,
                            onValueChange = { transactionId = it.trim().uppercase() },
                            label = { Text("Transaction ID (TrxID) *") },
                            placeholder = { Text("e.g. 9J87K2L1") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.ReceiptLong,
                                    contentDescription = null,
                                    tint = Cyan400,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Slate900.copy(alpha = 0.7f),
                                unfocusedContainerColor = Slate900.copy(alpha = 0.5f),
                                focusedBorderColor = Cyan500,
                                unfocusedBorderColor = Slate700,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedLabelColor = Cyan400,
                                unfocusedLabelColor = Slate400
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (transactionId.isBlank()) {
                                    Toast.makeText(context, "Please enter Transaction ID (TrxID)", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (senderNumber.isBlank()) {
                                    Toast.makeText(context, "Please enter sender mobile number", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (profile?.id == null) {
                                    Toast.makeText(context, "User session not active", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSubmittingPayment = true
                                scope.launch {
                                    val result = SupabaseManager.submitManualPayment(
                                        userId = profile.id,
                                        planId = checkoutPlan!!.id,
                                        amount = effectivePrice.toDouble(),
                                        paymentMethod = selectedMethod,
                                        senderNumber = senderNumber.trim(),
                                        transactionId = transactionId.trim()
                                    )

                                    isSubmittingPayment = false
                                    if (result.isSuccess) {
                                        Toast.makeText(context, "Payment submitted! Awaiting admin review.", Toast.LENGTH_LONG).show()
                                        checkoutPlan = null
                                        // Refresh transactions
                                        val txResult = SupabaseManager.fetchUserPaymentTransactions(profile.id)
                                        if (txResult.isSuccess) {
                                            userTransactions = txResult.getOrDefault(emptyList())
                                        }
                                    } else {
                                        Toast.makeText(context, "Submission failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                            },
                            enabled = !isSubmittingPayment,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Emerald400)
                        ) {
                            if (isSubmittingPayment) {
                                CircularProgressIndicator(
                                    color = Slate950,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Slate950,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Submit TrxID for Verification",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Slate950
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
