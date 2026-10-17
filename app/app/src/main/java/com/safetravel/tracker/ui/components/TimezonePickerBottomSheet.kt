package com.safetravel.tracker.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import com.safetravel.tracker.supabase.SupabaseManager
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.DateTimeUtils
import kotlinx.coroutines.launch
import java.util.Date
import java.util.TimeZone

data class PopularTimezone(
    val label: String,
    val timeZoneId: String,
    val offsetLabel: String
)

private val CyberCyan = Color(0xFF06B6D4)

private val POPULAR_TIMEZONES = listOf(
    PopularTimezone("Bangladesh (BST)", "Asia/Dhaka", "GMT+06:00"),
    PopularTimezone("India (IST)", "Asia/Kolkata", "GMT+05:30"),
    PopularTimezone("Saudi Arabia", "Asia/Riyadh", "GMT+03:00"),
    PopularTimezone("UAE / Dubai", "Asia/Dubai", "GMT+04:00"),
    PopularTimezone("Qatar", "Asia/Qatar", "GMT+03:00"),
    PopularTimezone("Singapore", "Asia/Singapore", "GMT+08:00"),
    PopularTimezone("UK / London", "Europe/London", "GMT+00:00"),
    PopularTimezone("US Eastern", "America/New_York", "GMT-05:00"),
    PopularTimezone("US Pacific", "America/Los_Angeles", "GMT-08:00")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezonePickerBottomSheet(
    userId: String?,
    onDismiss: () -> Unit,
    onTimezoneChanged: (String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var selectedTimezone by remember { mutableStateOf(DateTimeUtils.userTimeZoneId) }
    var searchQuery by remember { mutableStateOf("") }

    val allTimezones = remember {
        TimeZone.getAvailableIDs().sorted()
    }

    val filteredTimezones = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            allTimezones.filter { it.contains(searchQuery.trim(), ignoreCase = true) }.take(25)
        }
    }

    // Live preview formatted with currently selected timezone
    val livePreviewText = remember(selectedTimezone) {
        val tempSdf = java.text.SimpleDateFormat(DateTimeUtils.STANDARD_PATTERN, java.util.Locale.US).apply {
            timeZone = try { TimeZone.getTimeZone(selectedTimezone) } catch (_: Exception) { DateTimeUtils.BST_TIMEZONE }
        }
        tempSdf.format(Date())
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        contentColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Timezone & Regional Time",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Customize app date & time format for your location",
                        fontSize = 12.sp,
                        color = Slate400
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .background(Slate800, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Slate300,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Live Preview Card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Slate800.copy(alpha = 0.85f),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, CyberCyan.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(CyberCyan.copy(alpha = 0.3f), Color.Transparent))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "LIVE TIME PREVIEW",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = CyberCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = livePreviewText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                        Text(
                            text = "Timezone: $selectedTimezone",
                            fontSize = 11.sp,
                            color = Slate400,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }

            // Popular Timezones Section
            Text(
                text = "POPULAR REGIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Slate400,
                letterSpacing = 0.8.sp
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Device default chip
                val deviceTz = TimeZone.getDefault().id
                item {
                    val isSelected = selectedTimezone == deviceTz
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTimezone = deviceTz },
                        label = { Text("Device Time", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            containerColor = Slate800,
                            labelColor = Slate300
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) CyberCyan else Slate700,
                            selectedBorderColor = CyberCyan,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }

                items(POPULAR_TIMEZONES) { pop ->
                    val isSelected = selectedTimezone == pop.timeZoneId
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTimezone = pop.timeZoneId },
                        label = {
                            Text(
                                text = "${pop.label} (${pop.offsetLabel})",
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CyberCyan.copy(alpha = 0.2f),
                            selectedLabelColor = CyberCyan,
                            containerColor = Slate800,
                            labelColor = Slate300
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = if (isSelected) CyberCyan else Slate700,
                            selectedBorderColor = CyberCyan,
                            enabled = true,
                            selected = isSelected
                        )
                    )
                }
            }

            // Search Bar for All Timezones
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search city or timezone (e.g. Dubai, London)...", fontSize = 12.sp, color = Slate500) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = Slate400,
                        modifier = Modifier.size(18.dp)
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Slate400, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Slate800,
                    unfocusedContainerColor = Slate850,
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = Slate700,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                )
            )

            // Search Results List
            if (filteredTimezones.isNotEmpty()) {
                Text(
                    text = "SEARCH RESULTS (${filteredTimezones.size})",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 160.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredTimezones) { tzId ->
                        val isSelected = selectedTimezone == tzId
                        val offset = try {
                            val tz = TimeZone.getTimeZone(tzId)
                            val rawOffsetMinutes = tz.rawOffset / (1000 * 60)
                            val hours = rawOffsetMinutes / 60
                            val minutes = Math.abs(rawOffsetMinutes % 60)
                            String.format("GMT%+03d:%02d", hours, minutes)
                        } catch (_: Exception) { "" }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyberCyan.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f))
                                .clickable { selectedTimezone = tzId }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = tzId,
                                fontSize = 12.sp,
                                color = if (isSelected) CyberCyan else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = offset,
                                fontSize = 11.sp,
                                color = if (isSelected) CyberCyan else Slate400,
                                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Save and Apply Button
            Button(
                onClick = {
                    DateTimeUtils.setUserTimeZone(context, selectedTimezone)
                    onTimezoneChanged(selectedTimezone)
                    if (!userId.isNullOrBlank()) {
                        coroutineScope.launch {
                            SupabaseManager.saveUserPreference(userId, selectedTimezone)
                        }
                    }
                    Toast.makeText(context, "Timezone updated to $selectedTimezone", Toast.LENGTH_SHORT).show()
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CyberCyan)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Slate950,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Apply Timezone",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate950
                    )
                }
            }
        }
    }
}
