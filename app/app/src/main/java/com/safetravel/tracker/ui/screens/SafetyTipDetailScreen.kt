package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.SupabaseSafetyTip
import com.safetravel.tracker.ui.theme.Slate900
import com.safetravel.tracker.ui.theme.HindSiliguriFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyTipDetailScreen(
    tip: SupabaseSafetyTip,
    onBack: () -> Unit,
    onMarkAsRead: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tip Details", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate900)
            )
        },
        containerColor = Slate900,
        bottomBar = {
            if (!tip.isRead) {
                Button(
                    onClick = { onMarkAsRead(tip.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("I've Read This", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = tip.title,
                color = Color.White,
                fontSize = 18.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = HindSiliguriFontFamily
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Surface(
                color = Color(0xFFF59E0B).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = tip.category,
                    color = Color(0xFFF59E0B),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = HindSiliguriFontFamily
                )
            }
            
            Spacer(modifier = Modifier.height(18.dp))
            
            Text(
                text = tip.content,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontFamily = HindSiliguriFontFamily
            )
        }
    }
}
