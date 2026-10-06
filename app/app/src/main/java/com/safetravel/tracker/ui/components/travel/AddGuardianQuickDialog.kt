package com.safetravel.tracker.ui.components.travel

import android.content.Intent
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.Guardian

@Composable
fun AddGuardianQuickDialog(
    guardians: List<Guardian>,
    notifiedPhones: List<String>,
    tripId: String,
    onAddGuardians: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val newlySelectedPhones = remember { mutableStateListOf<String>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .clickable(enabled = false) {},
            shape = RoundedCornerShape(24.dp),
            color = Color(0xF20F172A),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x33FFFFFF)),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Share Live Journey",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF94A3B8))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quick Share Link via WhatsApp / External
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x1F10B981))
                        .border(1.dp, Color(0x4D10B981), RoundedCornerShape(12.dp))
                        .clickable {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "🚨 I'm traveling! Track my live journey safely here: https://safetravel.app/track/$tripId"
                                )
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Live Journey Link"))
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Copy / Share Tracking Link", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("Send via WhatsApp, SMS, or Messenger", color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Add From Saved Guardians",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (guardians.isEmpty()) {
                    Text(
                        text = "No guardians saved in your profile yet.",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                    ) {
                        items(guardians) { guardian ->
                            val isAlreadyWatching = notifiedPhones.contains(guardian.phone)
                            val isSelected = newlySelectedPhones.contains(guardian.phone) || isAlreadyWatching

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0x1F06B6D4) else Color.White.copy(alpha = 0.03f))
                                    .clickable(enabled = !isAlreadyWatching) {
                                        if (newlySelectedPhones.contains(guardian.phone)) {
                                            newlySelectedPhones.remove(guardian.phone)
                                        } else {
                                            newlySelectedPhones.add(guardian.phone)
                                        }
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color(0xFF06B6D4) else Color(0xFF334155)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = guardian.name.firstOrNull()?.toString()?.uppercase() ?: "G",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(guardian.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                    Text(guardian.phone, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                }

                                if (isAlreadyWatching) {
                                    Text("Watching", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                } else {
                                    Checkbox(
                                        checked = newlySelectedPhones.contains(guardian.phone),
                                        onCheckedChange = { checked ->
                                            if (checked) newlySelectedPhones.add(guardian.phone)
                                            else newlySelectedPhones.remove(guardian.phone)
                                        },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF06B6D4),
                                            uncheckedColor = Color(0xFF64748B)
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action Buttons
                Button(
                    onClick = {
                        if (newlySelectedPhones.isNotEmpty()) {
                            onAddGuardians(newlySelectedPhones.toList())
                        }
                        onDismiss()
                    },
                    enabled = newlySelectedPhones.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF06B6D4),
                        disabledContainerColor = Color(0x3306B6D4)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (newlySelectedPhones.isEmpty()) "Select Guardians" else "Share with ${newlySelectedPhones.size} Guardian(s)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
