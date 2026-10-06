package com.safetravel.tracker.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.safetravel.tracker.data.model.DailyDisplayData
import com.safetravel.tracker.data.model.HourlyDisplayData
import com.safetravel.tracker.data.model.WeatherResponse
import com.safetravel.tracker.data.remote.WeatherRetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class WeatherViewModel : ViewModel() {

    private val _weatherData = MutableStateFlow<WeatherResponse?>(null)
    val weatherData = _weatherData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _hourlyList = MutableStateFlow<List<HourlyDisplayData>>(emptyList())
    val hourlyList = _hourlyList.asStateFlow()

    private val _dailyList = MutableStateFlow<List<DailyDisplayData>>(emptyList())
    val dailyList = _dailyList.asStateFlow()

    private val apiDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.US)
    private val apiDayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val hourFormat = SimpleDateFormat("HH:mm", Locale.US)
    private val dayNameFormat = SimpleDateFormat("EEE", Locale.US)

    fun fetchWeather(lat: Double, lng: Double) {
        android.util.Log.d("WeatherVM", "Fetching for: $lat, $lng")
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = WeatherRetrofitClient.api.getFullWeather(lat, lng)
                if (response.isSuccessful) {
                    val data = response.body()
                    _weatherData.value = data
                    processHourlyData(data)
                    processDailyData(data)
                    android.util.Log.d("WeatherVM", "Data fetched successfully")
                } else {
                    android.util.Log.e("WeatherVM", "API Error: ${response.errorBody()?.string()}")
                    loadMockData()
                }
            } catch (e: Exception) {
                android.util.Log.e("WeatherVM", "Exception: ${e.message}")
                loadMockData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadMockData() {
        // Fallback mock data if API fails
        val mockHourly = mutableListOf<HourlyDisplayData>()
        val calendar = Calendar.getInstance()
        for (i in 0..23) {
            mockHourly.add(HourlyDisplayData(
                time = if (i == 0) "Now" else hourFormat.format(calendar.time),
                temp = 28.0 + (Math.random() * 5),
                code = 1,
                precip = (Math.random() * 100).toInt(),
                wind = 5.0 + Math.random()
            ))
            calendar.add(Calendar.HOUR_OF_DAY, 1)
        }
        _hourlyList.value = mockHourly

        val mockDaily = mutableListOf<DailyDisplayData>()
        calendar.time = Date()
        for (i in 0..6) {
            mockDaily.add(DailyDisplayData(
                day = if (i == 0) "Today" else dayNameFormat.format(calendar.time),
                tempMax = 32.0,
                tempMin = 24.0,
                code = 0,
                precip = 10,
                wind = 4.0
            ))
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }
        _dailyList.value = mockDaily
    }

    private fun processHourlyData(data: WeatherResponse?) {
        val hourly = data?.hourly ?: return
        val list = mutableListOf<HourlyDisplayData>()
        val now = Calendar.getInstance().time
        
        for (i in hourly.time.indices) {
            val timeStr = hourly.time[i]
            val itemTime = apiDateFormat.parse(timeStr) ?: continue
            
            // Only show from current hour onwards (buffer 1 hour), limit to next 24 hours
            val bufferTime = Calendar.getInstance().apply { 
                this.time = now
                add(Calendar.HOUR_OF_DAY, -1) 
            }.time

            if (itemTime.after(bufferTime) && list.size < 24) {
                list.add(
                    HourlyDisplayData(
                        time = if (list.isEmpty()) "Now" else hourFormat.format(itemTime),
                        temp = hourly.temperatures[i],
                        code = hourly.weatherCodes[i],
                        precip = hourly.precipitationProbability[i],
                        wind = hourly.windSpeeds[i]
                    )
                )
            }
        }
        _hourlyList.value = list
    }

    private fun processDailyData(data: WeatherResponse?) {
        val daily = data?.daily ?: return
        val list = mutableListOf<DailyDisplayData>()
        
        for (i in daily.time.indices) {
            val dateStr = daily.time[i]
            val date = apiDayFormat.parse(dateStr) ?: continue
            val dayName = if (i == 0) "Today" else dayNameFormat.format(date)
            
            list.add(
                DailyDisplayData(
                    day = dayName,
                    code = daily.weatherCodes[i],
                    tempMax = daily.temperaturesMax[i],
                    tempMin = daily.temperaturesMin[i],
                    precip = daily.precipitationProbabilityMax[i],
                    wind = daily.windSpeedsMax[i]
                )
            )
        }
        _dailyList.value = list
    }
}
