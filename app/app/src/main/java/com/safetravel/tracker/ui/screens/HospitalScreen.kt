package com.safetravel.tracker.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.safetravel.tracker.supabase.SupabaseHospital
import com.safetravel.tracker.util.OsrmDistanceHelper
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.components.GlassDragHandle
import com.safetravel.tracker.ui.components.DetailHeaderBanner
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalScreen(vm: SafeTravelViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    val hospitals by vm.hospitals.collectAsState()
    val userPosState by vm.currentPosition.collectAsState()
    
    var initialUserPos by remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var isLocating by remember { mutableStateOf(true) }

    LaunchedEffect(userPosState) {
        if (initialUserPos == null && userPosState != null) {
            initialUserPos = userPosState
            isLocating = false
        }
    }
    
    val currentLocationName by vm.currentLocationName.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val userProfile by vm.userProfile.collectAsState()
    val isPremium = userProfile?.isPremium == true
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val adInterval = remember { com.safetravel.tracker.ads.AdMobManager.getDirectoryCardInterval(context) }

    var visibleItemsCount by remember { mutableIntStateOf(20) }
    val listState = rememberLazyListState()

    var selectedHospitalData by remember { mutableStateOf<Pair<SupabaseHospital, Double?>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.refreshHospitals()
    }
    
    LaunchedEffect(initialUserPos) {
        initialUserPos?.let { (lat, lng) ->
            vm.updateCurrentLocationName(context, lat, lng)
        }
    }

    val shouldLoadMore = remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= visibleItemsCount - 5
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            visibleItemsCount += 10
        }
    }

    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    val filteredHospitals = remember(hospitals, initialUserPos, searchQuery) {
        hospitals.map { hospital ->
            val sLat = hospital.latitude
            val sLon = hospital.longitude
            
            val userPos = initialUserPos
            val distance = if (userPos != null && sLat != null && sLon != null) {
                calculateDistance(userPos.first, userPos.second, sLat, sLon)
            } else null

            hospital to distance
        }.filter { (hospital, _) ->
            searchQuery.isBlank() || 
                               hospital.name?.contains(searchQuery, ignoreCase = true) == true ||
                               hospital.thana?.contains(searchQuery, ignoreCase = true) == true ||
                               hospital.district?.contains(searchQuery, ignoreCase = true) == true
        }.sortedWith(compareBy({ it.second ?: Double.MAX_VALUE }, { it.first.name }))
    }

    val realDistances = remember { mutableStateMapOf<Int, Double>() }
    val candidatesForOsrm = filteredHospitals.take(50)

    LaunchedEffect(candidatesForOsrm, initialUserPos) {
        val currentUserPos = initialUserPos
        if (currentUserPos != null && candidatesForOsrm.isNotEmpty()) {
            val destinations = candidatesForOsrm.mapNotNull { (hospital, _) ->
                val lat = hospital.latitude
                val lng = hospital.longitude
                if (lat != null && lng != null) Pair(lat, lng) else null
            }
            if (destinations.isNotEmpty()) {
                val results = OsrmDistanceHelper.fetchRealRoadDistances(
                    userLat = currentUserPos.first,
                    userLng = currentUserPos.second,
                    destinations = destinations
                )
                var resultIdx = 0
                candidatesForOsrm.forEach { (hospital, Haversine) ->
                    val lat = hospital.latitude
                    val lng = hospital.longitude
                    val id = hospital.id
                    if (lat != null && lng != null && id != null && resultIdx < results.size) {
                        val realDist = results[resultIdx]
                        if (realDist != null) {
                            realDistances[id] = realDist
                        }
                        resultIdx++
                    }
                }
            }
        }
    }

    val paginatedHospitals = filteredHospitals.take(visibleItemsCount).sortedWith(compareBy(
        { it.first.id?.let { id -> realDistances[id] } ?: it.second ?: Double.MAX_VALUE },
        { it.first.name }
    ))

    Box(modifier = Modifier.fillMaxSize().background(Slate900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Area: ", color = Slate400, fontSize = 12.sp)
                    Text(
                        text = if (currentLocationName.isNotBlank()) currentLocationName else "Detecting...",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        visibleItemsCount = 20
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Search hospitals...", color = Slate500, fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Slate800,
                        focusedContainerColor = Slate800,
                        unfocusedContainerColor = Slate800,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }

            if (isLoading || isLocating) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(6) { HospitalSkeletonCard() }
                }
            } else if (filteredHospitals.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate700, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No hospitals found matching \"$searchQuery\"." else "No hospitals available.",
                            color = Slate500,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    itemsIndexed(paginatedHospitals) { index, (hospital, haversineDistance) ->
                        val displayDist = realDistances[hospital.id] ?: haversineDistance
                        HospitalCard(
                            hospital = hospital,
                            distance = displayDist,
                            onCardClick = { selectedHospitalData = hospital to haversineDistance }
                        )
                        if ((index + 1) % adInterval == 0) {
                            com.safetravel.tracker.ads.AdCardView(
                                modifier = Modifier.padding(vertical = 4.dp),
                                isPremium = isPremium
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedHospitalData != null) {
        val hospital = selectedHospitalData!!.first
        val haversineDist = selectedHospitalData!!.second
        val displayDist = realDistances[hospital.id] ?: haversineDist
        HospitalDetailSheet(
            hospital = hospital,
            distance = displayDist,
            sheetState = sheetState,
            onDismiss = { selectedHospitalData = null }
        )
    }
}

@Composable
fun HospitalCard(hospital: SupabaseHospital, distance: Double?, onCardClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(Slate700),
                contentAlignment = Alignment.Center
            ) {
                if (hospital.imageUrl != null) {
                    AsyncImage(model = hospital.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = hospital.name ?: "Hospital", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = (hospital.name ?: "").appFontFamily, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = "${hospital.type} • ${hospital.specialty}", color = Slate400, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = hospital.phoneNumber ?: "No Phone",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable { hospital.phoneNumber?.let { val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it")); context.startActivity(intent) } }
                    )
                    if (distance != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "${"%.1f".format(distance)} km away", color = Color(0xFF60A5FA), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HospitalDetailSheet(hospital: SupabaseHospital, distance: Double?, sheetState: SheetState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.65f),
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        GlassBottomSheetContainer(
            topStartRadius = 28.dp,
            topEndRadius = 28.dp
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp).navigationBarsPadding()) {
                DetailHeaderBanner(
                    imageUrl = hospital.imageUrl,
                    title = hospital.name ?: "Hospital",
                    badgeText = "MEDICAL EMERGENCY",
                    icon = Icons.Default.LocalHospital,
                    accentColor = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = hospital.name ?: "Hospital", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp)
                Text(text = hospital.address ?: "Unknown Address", color = Slate400, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = hospital.phoneNumber ?: "No Phone", color = Color.White, fontSize = 15.sp)
                }
                if (hospital.ambulanceNumber != null) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Ambulance: ${hospital.ambulanceNumber}", color = Color(0xFFEF4444), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailActionChip(icon = Icons.Default.Directions, label = "Directions", onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=${hospital.latitude},${hospital.longitude}")).setPackage("com.google.android.apps.maps")) }, modifier = Modifier.weight(1f))
                    DetailActionChip(icon = Icons.Default.Share, label = "Share", onClick = { context.startActivity(Intent.createChooser(Intent().apply { action = Intent.ACTION_SEND; putExtra(Intent.EXTRA_TEXT, "Hospital: ${hospital.name}\nPhone: ${hospital.phoneNumber}\nAddress: ${hospital.address}"); type = "text/plain" }, null)) }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(onClick = { hospital.phoneNumber?.let { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$it"))) } }, modifier = Modifier.fillMaxWidth().height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)), shape = RoundedCornerShape(14.dp)) {
                    Text("Call Reception", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HospitalSkeletonCard() {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Slate800), shape = RoundedCornerShape(16.dp)) {
        Row(modifier = Modifier.padding(10.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp)).background(shimmerBrush()))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(0.6f).height(16.dp).background(shimmerBrush(), RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.4f).height(12.dp).background(shimmerBrush(), RoundedCornerShape(4.dp)))
            }
        }
    }
}
