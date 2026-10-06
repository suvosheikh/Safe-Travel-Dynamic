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
import com.safetravel.tracker.supabase.SupabaseFireStation
import com.safetravel.tracker.util.OsrmDistanceHelper
import com.safetravel.tracker.ui.components.GlassBottomSheetContainer
import com.safetravel.tracker.ui.components.GlassDragHandle
import com.safetravel.tracker.ui.components.DetailHeaderBanner
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.SafeTravelViewModel
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireStationScreen(vm: SafeTravelViewModel, onBack: () -> Unit) {
    BackHandler { onBack() }
    val stations by vm.fireStations.collectAsState()
    val userPosState by vm.currentPosition.collectAsState()
    
    // CAPTURE LOCATION: We wait for the first valid GPS lock.
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

    // Pagination State
    var visibleItemsCount by remember { mutableIntStateOf(20) }
    val listState = rememberLazyListState()

    // Bottom Sheet State
    var selectedStationData by remember { mutableStateOf<Pair<SupabaseFireStation, Double?>?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(Unit) {
        vm.refreshFireStations()
    }
    
    LaunchedEffect(initialUserPos) {
        initialUserPos?.let { (lat, lng) ->
            vm.updateCurrentLocationName(context, lat, lng)
        }
    }

    // Load more items when scrolling to the bottom
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

    // Distance calculation logic (Haversine Formula)
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    val filteredStations = remember(stations, initialUserPos, searchQuery) {
        stations.map { station ->
            val sLat = station.latitude
            val sLon = station.longitude
            
            val userPos = initialUserPos
            val distance = if (userPos != null && sLat != null && sLon != null) {
                calculateDistance(userPos.first, userPos.second, sLat, sLon)
            } else null

            station to distance
        }.filter { (station, _) ->
            searchQuery.isBlank() || 
                               station.name?.contains(searchQuery, ignoreCase = true) == true ||
                               station.thana?.contains(searchQuery, ignoreCase = true) == true ||
                               station.district?.contains(searchQuery, ignoreCase = true) == true
        }.sortedWith(compareBy({ it.second ?: Double.MAX_VALUE }, { it.first.name }))
    }

    // State to store real road distances fetched from OSRM
    val realDistances = remember { mutableStateMapOf<Int, Double>() }

    // Final sorted list based on Road Distance (OSRM) falling back to Heuristic (Haversine)
    val finalSortedStations = remember(filteredStations, realDistances.size) {
        filteredStations.sortedWith(compareBy(
            { stationPair -> 
                val stationId = stationPair.first.id
                if (stationId != null) realDistances[stationId] ?: stationPair.second ?: Double.MAX_VALUE 
                else stationPair.second ?: Double.MAX_VALUE 
            },
            { it.first.name }
        ))
    }

    // Fetch real road distances for the top candidates in background
    val candidatesForOsrm = filteredStations.take(50)

    LaunchedEffect(candidatesForOsrm, initialUserPos) {
        val currentUserPos = initialUserPos
        if (currentUserPos != null && candidatesForOsrm.isNotEmpty()) {
            val destinations = candidatesForOsrm.mapNotNull { (station, _) ->
                val lat = station.latitude
                val lng = station.longitude
                if (lat != null && lng != null) Pair(lat, lng) else null
            }
            if (destinations.isNotEmpty()) {
                val results = OsrmDistanceHelper.fetchRealRoadDistances(
                    userLat = currentUserPos.first,
                    userLng = currentUserPos.second,
                    destinations = destinations
                )
                // Map the results back to station IDs
                var resultIdx = 0
                candidatesForOsrm.forEach { (station, _) ->
                    val lat = station.latitude
                    val lng = station.longitude
                    val id = station.id
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

    // Paginated view of the final sorted list
    val paginatedStations = finalSortedStations.take(visibleItemsCount)

    Box(modifier = Modifier.fillMaxSize().background(Slate900)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Location Indicator & Search Bar
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFFF97316), // Fire Orange
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
                        visibleItemsCount = 20 // Reset pagination
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    placeholder = { Text("Search fire stations...", color = Slate500, fontSize = 13.sp) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFF97316),
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
                    items(6) { FireStationSkeletonCard() }
                }
            } else if (filteredStations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                        Icon(Icons.Default.SearchOff, contentDescription = null, tint = Slate700, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) 
                                "No fire stations found matching \"$searchQuery\"." 
                            else 
                                "No fire stations available in the database.",
                            color = Slate500,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { vm.refreshFireStations() },
                            colors = ButtonDefaults.buttonColors(containerColor = Slate800)
                        ) {
                            Text("Retry Refresh", color = Color.White)
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    itemsIndexed(paginatedStations) { index, (station, haversineDistance) ->
                        val displayDist = realDistances[station.id] ?: haversineDistance
                        FireStationCard(
                            station = station,
                            distance = displayDist,
                            onCardClick = { selectedStationData = station to haversineDistance }
                        )
                        if ((index + 1) % adInterval == 0) {
                            com.safetravel.tracker.ads.AdCardView(
                                modifier = Modifier.padding(vertical = 4.dp),
                                isPremium = isPremium
                            )
                        }
                    }
                    
                    if (visibleItemsCount < filteredStations.size) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Color(0xFFF97316), modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet
    if (selectedStationData != null) {
        val station = selectedStationData!!.first
        val haversineDist = selectedStationData!!.second
        val displayDist = realDistances[station.id] ?: haversineDist
        FireStationDetailSheet(
            station = station,
            distance = displayDist,
            sheetState = sheetState,
            onDismiss = { selectedStationData = null }
        )
    }
}

@Composable
fun FireStationCard(station: SupabaseFireStation, distance: Double?, onCardClick: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Slate700),
                contentAlignment = Alignment.Center
            ) {
                if (station.imageUrl != null) {
                    AsyncImage(
                        model = station.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name ?: "Fire Station",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = (station.name ?: "").appFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = station.address ?: "${station.thana}, ${station.district}",
                    color = Slate400,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = station.number ?: "No Phone",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clickable {
                            station.number?.let { num ->
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                                context.startActivity(intent)
                            }
                        }
                    )
                    
                    if (distance != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${"%.1f".format(distance)} km away",
                            color = Color(0xFF60A5FA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FireStationDetailSheet(
    station: SupabaseFireStation,
    distance: Double?,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp)
                    .navigationBarsPadding()
            ) {
                DetailHeaderBanner(
                    imageUrl = station.imageUrl,
                    title = station.name ?: "Fire Station",
                    badgeText = "FIRE & RESCUE",
                    icon = Icons.Default.LocalFireDepartment,
                    accentColor = Color(0xFFF97316)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = station.name ?: "Fire Station",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 23.sp
                )
                Text(
                    text = station.address ?: "${station.thana}, ${station.district}",
                    color = Slate400,
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Row
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = station.number ?: "No Phone Contact",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Website Row (Extra Field)
                if (!station.website.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = station.website,
                            color = Color(0xFF60A5FA),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(if (station.website.startsWith("http")) station.website else "https://${station.website}"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "QUICK ACTIONS",
                    color = Slate500,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DetailActionChip(
                        icon = Icons.Default.Directions,
                        label = "Directions",
                        onClick = {
                            val uri = if (!station.googleMapsUrl.isNullOrBlank()) {
                                Uri.parse(station.googleMapsUrl)
                            } else {
                                Uri.parse("google.navigation:q=${station.latitude},${station.longitude}")
                            }
                            val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                            mapIntent.setPackage("com.google.android.apps.maps")
                            context.startActivity(mapIntent)
                        },
                        modifier = Modifier.weight(1f)
                    )

                    DetailActionChip(
                        icon = Icons.Default.Share,
                        label = "Share",
                        onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "Fire Station: ${station.name}\nPhone: ${station.number}\nAddress: ${station.address}")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, null))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        station.number?.let { num ->
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$num"))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF97316)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Emergency Call", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FireStationSkeletonCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate800),
        shape = RoundedCornerShape(16.dp)
    ) {
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
