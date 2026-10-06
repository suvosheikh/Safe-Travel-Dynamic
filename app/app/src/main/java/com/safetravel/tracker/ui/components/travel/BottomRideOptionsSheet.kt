// File: /app/src/main/java/com/safetravel/tracker/ui/components/travel/BottomRideOptionsSheet.kt
package com.safetravel.tracker.ui.components.travel

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.safetravel.tracker.supabase.Guardian
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.util.AddressUtils

data class TransportModeItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

val transportModesList = listOf(
    TransportModeItem(0, "Walk", "Buddy & Pace", Icons.Default.DirectionsWalk),
    TransportModeItem(1, "Cycle", "Gear & Route", Icons.Default.DirectionsBike),
    TransportModeItem(2, "Car", "Plate & Driver", Icons.Default.DirectionsCar),
    TransportModeItem(3, "Bus", "Route & Counter", Icons.Default.DirectionsBus)
)

/**
 * Production-level Cyber Glassmorphic Sheet for Voyage Setup & Transport Configuration.
 * Fully responsive, ultra-clean cyber dark-glass aesthetics, with no visual clutter.
 */
@Composable
fun SelectTransportModeSheet(
    transportMode: Int,
    onTransportModeChange: (Int) -> Unit,
    vehiclePlate: String,
    onVehiclePlateChange: (String) -> Unit,
    vehicleDescription: String,
    onVehicleDescriptionChange: (String) -> Unit,
    emergencyPhone: String = "",
    onEmergencyPhoneChange: (String) -> Unit = {},
    vehiclePhotoUploaded: Boolean = false,
    photoLoadingState: Boolean = false,
    onUploadPhotoClick: () -> Unit = {},
    onPickContactClick: () -> Unit = {},
    destinationInput: String,
    calculatedTime: String,
    calculatedDistance: String,
    guardians: List<Guardian>,
    selectedGuardiansToShare: MutableList<String>,
    availableRoutes: List<RouteInfo> = emptyList(),
    selectedRouteIndex: Int = 0,
    onSelectRoute: (Int) -> Unit = {},
    onStartVoyageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    // Local safety checks
    var soloWalkerBuddyAlert by remember { mutableStateOf(true) }
    var cycleHelmetChecked by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Section Header: High-tech title + Telemetry badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TRIP SAFETY SETUP",
                color = Slate400,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Surface(
                color = Color(0x2006B6D4),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                    Text(
                        text = "Safety Ready",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. Sleek 4-Mode Horizontal Selector (Fully Responsive)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transportModesList.forEach { mode ->
                val isSelected = transportMode == mode.id
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(74.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) {
                                Brush.verticalGradient(
                                    listOf(Color(0x3306B6D4), Color(0x180F172A))
                                )
                            } else {
                                Brush.verticalGradient(
                                    listOf(Color(0x1F1E293B), Color(0x100F172A))
                                )
                            }
                        )
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF06B6D4) else Color(0x1FFFFFFF),
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable {
                            onTransportModeChange(mode.id)
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(2.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF38BDF8))
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = mode.icon,
                            contentDescription = mode.title,
                            tint = if (isSelected) Color(0xFF38BDF8) else Slate400,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = mode.title,
                            color = if (isSelected) Color.White else Slate300,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = mode.subtitle,
                            color = if (isSelected) Color(0xFF38BDF8) else Slate500,
                            fontSize = 8.5.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Mode-Tailored Security Airlock Panel (Cyber Glassmorphism)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1F111827))
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color(0x2E38BDF8), Color(0x0DFFFFFF))
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .padding(14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (transportMode) {
                    0 -> {
                        // 🚶 WALK MODE
                        Text(
                            text = "🚶 WALKING SAFETY DETAILS",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )

                        CyberInputField(
                            value = vehiclePlate,
                            onValueChange = onVehiclePlateChange,
                            label = "Walking Companion (Optional)",
                            placeholder = "e.g. Walking alone or Friend's Name",
                            leadingIcon = Icons.Default.Person
                        )

                        CyberToggleRow(
                            icon = Icons.Default.Timer,
                            title = "Inactivity Alert",
                            subtitle = "Alerts guardians if no movement for > 5 min",
                            checked = soloWalkerBuddyAlert,
                            onCheckedChange = { soloWalkerBuddyAlert = it }
                        )
                    }

                    1 -> {
                        // 🚴 CYCLE MODE
                        Text(
                            text = "🚴 CYCLING SAFETY CHECK",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )

                        CyberInputField(
                            value = vehiclePlate,
                            onValueChange = onVehiclePlateChange,
                            label = "Bicycle / Motorbike Info (Optional)",
                            placeholder = "e.g. Red Veloce Bike / Reg #12",
                            leadingIcon = Icons.Default.DirectionsBike
                        )

                        CyberToggleRow(
                            icon = Icons.Default.Security,
                            title = "Safety Helmet & Lights Equipped",
                            subtitle = "Confirm safety gear before departure",
                            checked = cycleHelmetChecked,
                            onCheckedChange = { cycleHelmetChecked = it }
                        )
                    }

                    2 -> {
                        // 🚗 CAR / RIDE-SHARE MODE
                        Text(
                            text = "🚗 VEHICLE & DRIVER DETAILS",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CyberInputField(
                                value = vehiclePlate,
                                onValueChange = onVehiclePlateChange,
                                label = "Vehicle License Plate",
                                placeholder = "e.g. DHAKA METRO GA-4212",
                                leadingIcon = Icons.Default.Pin,
                                modifier = Modifier.weight(1f)
                            )

                            // Vehicle Photo Upload Button
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (vehiclePhotoUploaded) Color(0x2810B981) else Color(0x1F1E293B))
                                    .border(
                                        1.dp,
                                        if (vehiclePhotoUploaded) Color(0xFF10B981) else Color(0x25FFFFFF),
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { onUploadPhotoClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                if (photoLoadingState) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = Color(0xFF38BDF8),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (vehiclePhotoUploaded) Icons.Default.CheckCircle else Icons.Default.CameraAlt,
                                        contentDescription = "Vehicle Photo",
                                        tint = if (vehiclePhotoUploaded) Color(0xFF10B981) else Color(0xFF38BDF8),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        CyberInputField(
                            value = vehicleDescription,
                            onValueChange = onVehicleDescriptionChange,
                            label = "Driver Name / Vehicle Details",
                            placeholder = "e.g. Uber / Driver: Rahim (017xxx)",
                            leadingIcon = Icons.Default.Badge
                        )
                    }

                    3 -> {
                        // BUS MODE (Seat crowdedness completely removed)
                        Text(
                            text = "BUS & TRANSIT DETAILS",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )

                        CyberInputField(
                            value = vehiclePlate,
                            onValueChange = onVehiclePlateChange,
                            label = "Bus Name & Number",
                            placeholder = "e.g. Raida Paribahan / Bus #42",
                            leadingIcon = Icons.Default.DirectionsBus
                        )

                        CyberInputField(
                            value = vehicleDescription,
                            onValueChange = onVehicleDescriptionChange,
                            label = "Boarding Stoppage / Counter",
                            placeholder = "e.g. House Building Counter",
                            leadingIcon = Icons.Default.Place
                        )
                    }
                }
            }
        }

        // 3. Live Guardian Network Telemetry Link
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1A111827))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "SHARE TRIP WITH GUARDIANS",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                    if (guardians.isNotEmpty()) {
                        Surface(
                            color = Color(0x2006B6D4),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "${selectedGuardiansToShare.size}/${guardians.size} Sharing",
                                color = Color(0xFF38BDF8),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (guardians.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(guardians) { guardian ->
                            val isSelected = selectedGuardiansToShare.contains(guardian.phone)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isSelected) selectedGuardiansToShare.remove(guardian.phone)
                                    else selectedGuardiansToShare.add(guardian.phone)
                                },
                                label = {
                                    Text(
                                        text = guardian.name,
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color(0xFF020617) else Color.White,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) Color(0xFF020617) else Slate400
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF06B6D4),
                                    containerColor = Color(0x1F1E293B)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) Color(0xFF06B6D4) else Color(0x22FFFFFF)
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "No guardians enrolled yet.",
                            color = Slate400,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Add in Profile 🛡️",
                            color = Color(0xFF38BDF8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3.5. Preferred Route Selection (When multiple alternatives exist)
        if (availableRoutes.size > 1) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "SELECT PREFERRED ROUTE",
                    color = Slate400,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )
                if (availableRoutes.size <= 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableRoutes.forEachIndexed { idx, route ->
                            val isSelected = selectedRouteIndex == idx
                            val summaryText = route.summary.ifBlank { if (idx == 0) "Shortest" else "Alt ${idx + 1}" }
                            val timeStr = if (route.durationMin < 60) "${route.durationMin.toInt()}m" else "${(route.durationMin/60).toInt()}h ${(route.durationMin%60).toInt()}m"
                            val distStr = String.format(java.util.Locale.US, "%.1f km", route.distanceKm)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0x33E11D48) else Color(0x1F1E293B))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFE11D48) else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectRoute(idx)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = summaryText,
                                        color = if (isSelected) Color.White else Slate300,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$timeStr • $distStr",
                                        color = if (isSelected) Color(0xFFFB7185) else Slate400,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(availableRoutes) { idx, route ->
                            val isSelected = selectedRouteIndex == idx
                            val summaryText = route.summary.ifBlank { if (idx == 0) "Shortest" else "Alt ${idx + 1}" }
                            val timeStr = if (route.durationMin < 60) "${route.durationMin.toInt()}m" else "${(route.durationMin/60).toInt()}h ${(route.durationMin%60).toInt()}m"
                            val distStr = String.format(java.util.Locale.US, "%.1f km", route.distanceKm)

                            Box(
                                modifier = Modifier
                                    .widthIn(min = 110.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0x33E11D48) else Color(0x1F1E293B))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) Color(0xFFE11D48) else Color(0x22FFFFFF),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectRoute(idx)
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = summaryText,
                                        color = if (isSelected) Color.White else Slate300,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$timeStr • $distStr",
                                        color = if (isSelected) Color(0xFFFB7185) else Slate400,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Destination & Live ETA HUD Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0x1F111827))
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0x2206B6D4)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = Color(0xFF06B6D4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DESTINATION",
                            color = Slate400,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = AddressUtils.formatDisplayAddress(destinationInput).ifBlank { "Tap map to select destination" },
                            color = Color.White,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0x2006B6D4),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(12.dp))
                            Text(calculatedTime, color = Color(0xFF38BDF8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Surface(
                        color = Color(0x2010B981),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Directions, null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                            Text(calculatedDistance, color = Color(0xFF10B981), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 5. Primary Action Button: START SECURED JOURNEY
        Button(
            onClick = onStartVoyageClick,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .shadow(12.dp, shape = RoundedCornerShape(16.dp), spotColor = Color(0xFF06B6D4))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF06B6D4), Color(0xFF0284C7))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "START JOURNEY",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 13.5.sp,
                        letterSpacing = 0.8.sp
                    )
                }
            }
        }
    }
}

/**
 * Reusable cyber-dark-glass input field matching app theme
 */
@Composable
private fun CyberInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = Slate400) },
        placeholder = { Text(placeholder, fontSize = 11.sp, color = Slate500) },
        leadingIcon = {
            Icon(leadingIcon, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
        },
        trailingIcon = trailingIcon,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0x1A0F172A),
            unfocusedContainerColor = Color(0x140F172A),
            unfocusedBorderColor = Color(0x25FFFFFF),
            focusedBorderColor = Color(0xFF38BDF8),
            focusedLabelColor = Color(0xFF38BDF8),
            unfocusedLabelColor = Slate400,
            cursorColor = Color(0xFF38BDF8),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

/**
 * Reusable high-tech toggle switch row
 */
@Composable
private fun CyberToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14FFFFFF))
            .border(1.dp, if (checked) Color(0x3338BDF8) else Color(0x12FFFFFF), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (checked) Color(0x2238BDF8) else Color(0x1A334155)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) Color(0xFF38BDF8) else Slate400,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column {
                Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Slate400, fontSize = 10.sp, lineHeight = 13.sp)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF06B6D4),
                checkedTrackColor = Color(0xFF06B6D4).copy(alpha = 0.3f),
                uncheckedThumbColor = Slate400,
                uncheckedTrackColor = Color(0x22FFFFFF)
            )
        )
    }
}
