package com.safetravel.tracker.data.model

import com.squareup.moshi.Json

data class WeatherResponse(
    val latitude: Double,
    val longitude: Double,
    @Json(name = "current_weather") val currentWeather: CurrentWeather?,
    val hourly: HourlyForecast?,
    val daily: DailyForecast?
)

data class CurrentWeather(
    val temperature: Double,
    @Json(name = "windspeed") val windSpeed: Double,
    @Json(name = "weathercode") val weatherCode: Int,
    val time: String
)

data class HourlyForecast(
    val time: List<String>,
    @Json(name = "temperature_2m") val temperatures: List<Double>,
    @Json(name = "weathercode") val weatherCodes: List<Int>,
    @Json(name = "precipitation_probability") val precipitationProbability: List<Int>,
    @Json(name = "windspeed_10m") val windSpeeds: List<Double>
)

data class DailyForecast(
    val time: List<String>,
    @Json(name = "weathercode") val weatherCodes: List<Int>,
    @Json(name = "temperature_2m_max") val temperaturesMax: List<Double>,
    @Json(name = "temperature_2m_min") val temperaturesMin: List<Double>,
    @Json(name = "precipitation_probability_max") val precipitationProbabilityMax: List<Int>,
    @Json(name = "windspeed_10m_max") val windSpeedsMax: List<Double>
)

data class HourlyDisplayData(
    val time: String,
    val temp: Double,
    val code: Int,
    val precip: Int,
    val wind: Double
)

data class DailyDisplayData(
    val day: String,
    val code: Int,
    val tempMax: Double,
    val tempMin: Double,
    val precip: Int,
    val wind: Double
)
