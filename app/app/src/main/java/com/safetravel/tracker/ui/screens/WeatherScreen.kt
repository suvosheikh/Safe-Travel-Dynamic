package com.safetravel.tracker.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.safetravel.tracker.ui.components.shimmerEffect
import com.safetravel.tracker.data.model.DailyDisplayData
import com.safetravel.tracker.data.model.HourlyDisplayData
import com.safetravel.tracker.ui.theme.*
import com.safetravel.tracker.viewmodel.WeatherViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun WeatherScreen(
    lat: Double,
    lng: Double,
    cityName: String = "Current Location",
    onBack: () -> Unit
) {
    val vm: WeatherViewModel = viewModel()
    val weatherData by vm.weatherData.collectAsState()
    val hourlyList by vm.hourlyList.collectAsState()
    val dailyList by vm.dailyList.collectAsState()
    val isLoading by vm.isLoading.collectAsState()

    LaunchedEffect(lat, lng) {
        vm.fetchWeather(lat, lng)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Slate900)
    ) {
        if (isLoading) {
            WeatherSkeleton()
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 1. Location Indicator
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Slate800.copy(alpha = 0.4f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cityName,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 2. Current Temp Section
                item {
                    weatherData?.currentWeather?.let { current ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = getWeatherIcon(current.weatherCode),
                                contentDescription = null,
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(80.dp)
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Text(
                                text = "${current.temperature.toInt()}°",
                                color = Color.White,
                                fontSize = 72.sp,
                                fontWeight = FontWeight.Light
                            )
                            Spacer(modifier = Modifier.width(20.dp))
                            Column {
                                val daily = weatherData?.daily
                                val max = daily?.temperaturesMax?.get(0)?.toInt() ?: 0
                                val min = daily?.temperaturesMin?.get(0)?.toInt() ?: 0
                                Text(text = "$max°", color = Color.White, fontSize = 20.sp)
                                Divider(modifier = Modifier.width(30.dp).padding(vertical = 4.dp), color = Slate700)
                                Text(text = "$min°", color = Slate400, fontSize = 20.sp)
                            }
                        }
                    }
                }

                // 3. Hourly Forecast Card
                item {
                    Spacer(modifier = Modifier.height(40.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Slate800.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        LazyRow(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            items(hourlyList) { hourly ->
                                HourlyItem(hourly)
                            }
                        }
                    }
                }

                // 4. Daily Forecast List
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Slate800.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Slate700)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            dailyList.forEach { daily ->
                                DailyItem(daily)
                                if (dailyList.last() != daily) {
                                    Divider(color = Slate700, modifier = Modifier.padding(vertical = 12.dp))
                                }
                            }
                        }
                    }
                }
                
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
fun HourlyItem(data: HourlyDisplayData) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = data.time, color = Slate300, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Icon(
            imageVector = getWeatherIcon(data.code),
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(10.dp))
            Text(text = "${data.precip}%", color = Color(0xFF3B82F6), fontSize = 10.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(10.dp))
            Text(text = "${data.wind.toInt()} m/s", color = Color(0xFF10B981), fontSize = 10.sp)
        }
    }
}

@Composable
fun DailyItem(data: DailyDisplayData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = data.day, color = Color.White, fontSize = 16.sp, modifier = Modifier.width(60.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF3B82F6), modifier = Modifier.size(14.dp))
            Text(text = "${data.precip}%", color = Color(0xFF3B82F6), fontSize = 12.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Icon(Icons.Default.Air, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
            Text(text = "${data.wind.toInt()} m/s", color = Color(0xFF10B981), fontSize = 12.sp)
        }

        Icon(
            imageVector = getWeatherIcon(data.code),
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(24.dp)
        )
        
        Text(
            text = "${data.tempMax.toInt()}°/${data.tempMin.toInt()}°",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.width(60.dp)
        )
    }
}

fun getWeatherIcon(code: Int): ImageVector {
    return when (code) {
        0 -> Icons.Default.WbSunny
        1, 2 -> Icons.Default.WbCloudy
        3 -> Icons.Default.Cloud
        45, 48 -> Icons.Default.CloudQueue // Using CloudQueue for Fog
        51, 53, 55, 61, 63, 65, 80, 81, 82 -> Icons.Default.Opacity // Using Opacity for Rain
        71, 73, 75 -> Icons.Default.AcUnit
        95 -> Icons.Default.Thunderstorm
        else -> Icons.Default.WbSunny
    }
}

@Composable
fun WeatherSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        // 0. Location Indicator Skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmerEffect()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        // 1. Current Temp Skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Box(modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)).shimmerEffect())
            Spacer(modifier = Modifier.width(20.dp))
            Box(modifier = Modifier.size(80.dp, 72.dp).clip(RoundedCornerShape(8.dp)).shimmerEffect())
            Spacer(modifier = Modifier.width(20.dp))
            Column {
                Box(modifier = Modifier.size(40.dp, 20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
                Spacer(modifier = Modifier.height(8.dp))
                Box(modifier = Modifier.size(40.dp, 20.dp).clip(RoundedCornerShape(4.dp)).shimmerEffect())
            }
        }

        // 3. Hourly Forecast Skeleton
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmerEffect()
        )

        // 4. Daily Forecast Skeleton
        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .shimmerEffect()
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
fun WeatherScreenPreview() {
    SafeTravelTheme {
        WeatherScreen(
            lat = 23.7104,
            lng = 90.4074,
            cityName = "Dhaka, Bangladesh",
            onBack = {}
        )
    }
}
