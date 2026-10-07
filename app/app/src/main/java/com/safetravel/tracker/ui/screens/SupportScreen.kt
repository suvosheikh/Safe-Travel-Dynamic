package com.safetravel.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.ui.theme.*

@Composable
fun SupportScreen(onBack: () -> Unit) {
    SupportScreenContent(onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportScreenContent(onBack: () -> Unit) {
    val context = LocalContext.current
    var activePolicyTitle by remember { mutableStateOf<String?>(null) }
    var activePolicyContent by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Help & Support",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        // Scrollable Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Header Icon
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Amber400.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.QuestionAnswer,
                    contentDescription = null,
                    tint = Amber400,
                    modifier = Modifier.size(38.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "We Are Here To Help!",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Find answers, contact our safety hotline, or review policies.",
                fontSize = 12.sp,
                color = Slate400,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Support Section
            SupportSectionCard(
                title = "Support",
                items = listOf(
                    SupportItemData(
                        label = "Live Chat & Assistance",
                        icon = Icons.Outlined.Chat,
                        iconColor = Color(0xFFF59E0B),
                        onClick = {
                            activePolicyTitle = "Live Chat & Support"
                            activePolicyContent = "Our dedicated customer care team is available 24/7 for emergency trip queries and technical assistance. You can also reach our safety dispatch center directly through email or phone hotline."
                        }
                    ),
                    SupportItemData(
                        label = "Call Helpline (16263 / 999)",
                        icon = Icons.Outlined.Call,
                        iconColor = Color(0xFF3B82F6),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:16263"))
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    ),
                    SupportItemData(
                        label = "Contact Us (Email)",
                        icon = Icons.Outlined.Email,
                        iconColor = Color(0xFFEF4444),
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("mailto:support@safetravel.app")
                                    putExtra(Intent.EXTRA_SUBJECT, "SafeTravel User Support Request")
                                }
                                context.startActivity(Intent.createChooser(intent, "Send Email"))
                            } catch (_: Exception) {}
                        }
                    )
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Legal Section
            SupportSectionCard(
                title = "Legal & Privacy",
                items = listOf(
                    SupportItemData(
                        label = "Terms & Conditions",
                        icon = Icons.Outlined.Description,
                        iconColor = Slate400,
                        onClick = {
                            activePolicyTitle = "Terms & Conditions"
                            activePolicyContent = "SafeTravel provides proactive journey monitoring and emergency safety telemetry. By using SafeTravel, you agree that GPS tracking and audio blackbox data are stored strictly for your safety and guardian alerts, conforming to all applicable local safety guidelines."
                        }
                    ),
                    SupportItemData(
                        label = "Privacy Policy",
                        icon = Icons.Outlined.Lock,
                        iconColor = Slate400,
                        onClick = {
                            activePolicyTitle = "Privacy Policy"
                            activePolicyContent = "We respect your privacy. Real-time GPS locations and trip breadcrumbs are encrypted and shared exclusively with your designated guardians during active trips or SOS situations. You can delete your evidence logs and account data at any time from the app settings."
                        }
                    ),
                    SupportItemData(
                        label = "Cancellation & Refund Policy",
                        icon = Icons.Outlined.AssignmentReturn,
                        iconColor = Slate400,
                        onClick = {
                            activePolicyTitle = "Cancellation Policy"
                            activePolicyContent = "SafeTravel Premium passes are non-recurring, one-time activations. If a transaction verification fails or an erroneous payment occurs, our billing team will review and refund the transaction to the original payment channel within 3-5 business days upon contact."
                        }
                    )
                )
            )

            // Generous bottom spacer so the entire list scrolls comfortably above the bottom navigation bar
            Spacer(modifier = Modifier.height(130.dp))
        }
    }

    // Policy / Information Dialog
    if (activePolicyTitle != null && activePolicyContent != null) {
        AlertDialog(
            onDismissRequest = {
                activePolicyTitle = null
                activePolicyContent = null
            },
            title = {
                Text(
                    text = activePolicyTitle!!,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = activePolicyContent!!,
                    color = Slate300,
                    fontSize = 13.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        activePolicyTitle = null
                        activePolicyContent = null
                    }
                ) {
                    Text("Close", color = Emerald400, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Slate800,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
fun SupportSectionCard(title: String, items: List<SupportItemData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Slate700.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )

            HorizontalDivider(
                color = Slate700.copy(alpha = 0.6f),
                thickness = 0.5.dp,
                modifier = Modifier.padding(horizontal = 18.dp)
            )

            items.forEachIndexed { index, item ->
                SupportRowItem(item)
                if (index < items.size - 1) {
                    HorizontalDivider(
                        color = Slate700.copy(alpha = 0.4f),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 64.dp, end = 18.dp)
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
            .clickable { data.onClick() }
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(data.iconColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = data.icon,
                contentDescription = null,
                tint = data.iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = data.label,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Slate500,
            modifier = Modifier.size(18.dp)
        )
    }
}

data class SupportItemData(
    val label: String,
    val icon: ImageVector,
    val iconColor: Color,
    val onClick: () -> Unit = {}
)
