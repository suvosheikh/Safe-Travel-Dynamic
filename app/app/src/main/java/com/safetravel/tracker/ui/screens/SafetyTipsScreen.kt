package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseSafetyTip
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTipsScreen(
    vm: SafeTravelViewModel,
    onBack: () -> Unit,
    onTipClick: (SupabaseSafetyTip) -> Unit
) {
    val tips by vm.safetyTips.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        vm.refreshSafetyTips()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Tips", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = { vm.refreshSafetyTips(force = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Emerald400)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        containerColor = Slate900
    ) { padding ->
        if (isLoading && tips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = Emerald400, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("নিরাপত্তা টিপস লোড হচ্ছে...", color = Slate400, fontSize = 13.sp, fontFamily = HindSiliguriFontFamily)
                }
            }
        } else if (tips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Slate600, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("কোনো নিরাপত্তা টিপস পাওয়া যায়নি", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = HindSiliguriFontFamily)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("পুনরায় লোড করতে নিচের বাটনে চাপ দিন", color = Slate400, fontSize = 12.sp, fontFamily = HindSiliguriFontFamily)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { vm.refreshSafetyTips(force = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("পুনরায় চেষ্টা করুন", color = Color.White, fontFamily = HindSiliguriFontFamily)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "আপনার যাত্রা নিরাপদ রাখতে নিচের টিপসগুলো মনোযোগ দিয়ে পড়ুন।",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        fontFamily = HindSiliguriFontFamily,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                items(tips) { tip ->
                    SafetyTipCard(tip = tip, onClick = { onTipClick(tip) })
                }

                item {
                    val profile by vm.userProfile.collectAsState()
                    com.safetravel.tracker.ads.AdBannerView(
                        modifier = Modifier.padding(top = 12.dp),
                        isPremium = profile?.isPremium == true
                    )
                }
            }
        }
    }
}

@Composable
fun SafetyTipCard(tip: SupabaseSafetyTip, onClick: () -> Unit) {
    val (icon, iconColor) = getTipIconAndColor(tip.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (tip.isRead) Color(0xFF10B981).copy(alpha = 0.35f) else Slate700)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    color = iconColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = tip.category,
                        color = iconColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = HindSiliguriFontFamily,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = tip.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = HindSiliguriFontFamily
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tip.description,
                    color = Color.White.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = HindSiliguriFontFamily,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            if (tip.isRead) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Read",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF06B6D4))
                )
            }
        }
    }
}

private fun getTipIconAndColor(category: String): Pair<ImageVector, Color> {
    return when (category.lowercase()) {
        "pre-journey" -> Pair(Icons.Default.Assignment, Color(0xFFF59E0B))
        "transport" -> Pair(Icons.Default.DirectionsBus, Color(0xFF3B82F6))
        "emergency" -> Pair(Icons.Default.Security, Color(0xFFEF4444))
        "night safety" -> Pair(Icons.Default.Shield, Color(0xFF8B5CF6))
        else -> Pair(Icons.Default.Lightbulb, Color(0xFF10B981))
    }
}
