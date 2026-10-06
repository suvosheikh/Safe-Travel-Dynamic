package com.safetravel.tracker.ui.screens

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseSafetyTip
import com.safetravel.tracker.ui.theme.Slate700
import com.safetravel.tracker.ui.theme.Slate800
import com.safetravel.tracker.ui.theme.HindSiliguriFontFamily
import com.safetravel.tracker.ui.theme.Slate900
import com.safetravel.tracker.viewmodel.SafeTravelViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTipsScreen(
    vm: SafeTravelViewModel,
    onBack: () -> Unit,
    onTipClick: (SupabaseSafetyTip) -> Unit
) {
    val tips by vm.safetyTips.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Safety Tips", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        containerColor = Slate900
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "আপনার যাত্রা নিরাপদ রাখতে নিচের টিপসগুলো মনোযোগ দিয়ে পড়ুন।",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontFamily = HindSiliguriFontFamily,
                    modifier = Modifier.padding(bottom = 8.dp)
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

@Composable
fun SafetyTipCard(tip: SupabaseSafetyTip, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (tip.isRead) Color(0xFF10B981).copy(alpha = 0.3f) else Slate700)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF59E0B).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFFF59E0B),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = HindSiliguriFontFamily
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = tip.description,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = HindSiliguriFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (tip.isRead) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Read",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
