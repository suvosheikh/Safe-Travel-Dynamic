package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.theme.*

@Composable
fun SupportScreen(onBack: () -> Unit) {
    SupportScreenContent(onBack = onBack)
}

@Composable
fun SupportScreenContent(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // Header Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFFDE68A).copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QuestionAnswer,
                contentDescription = null,
                tint = Amber400,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "We Are Here To Help!",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Support Section
        SupportSectionCard(
            title = "Support",
            items = listOf(
                SupportItemData("Live Chat", Icons.Outlined.Chat, Color(0xFFF59E0B)),
                SupportItemData("Call Helpline", Icons.Outlined.Call, Color(0xFF3B82F6)),
                SupportItemData("Contact Us", Icons.Outlined.Email, Color(0xFFEF4444))
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Legal Section
        SupportSectionCard(
            title = "Legal",
            items = listOf(
                SupportItemData("Terms & Conditions", Icons.Outlined.Description, Color(0xFF94A3B8)),
                SupportItemData("Privacy Policy", Icons.Outlined.Lock, Color(0xFF94A3B8)),
                SupportItemData("Cancellation Policy", Icons.Outlined.AssignmentReturn, Color(0xFF94A3B8))
            )
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SupportSectionCard(title: String, items: List<SupportItemData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(color = Slate700, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 20.dp))

            items.forEachIndexed { index, item ->
                SupportRowItem(item)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = Slate700, 
                        thickness = 0.5.dp, 
                        modifier = Modifier.padding(start = 68.dp, end = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SupportRowItem(data: SupportItemData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* Action */ }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(data.iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = data.iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = data.label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Slate500,
            modifier = Modifier.size(20.dp)
        )
    }
}

data class SupportItemData(val label: String, val icon: ImageVector, val iconColor: Color)

@Preview(showBackground = true)
@Composable
fun SupportScreenPreview() {
    SafeTravelTheme {
        SupportScreenContent(onBack = {})
    }
}
