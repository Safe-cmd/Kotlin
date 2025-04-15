package com.example.myapplication.model

data class WeatherResponse(
    val name: String,              // 城市名称
    val main: Main,
    val weather: List<WeatherDetail>
)

data class Main(
    val temp: Double,              // 温度
    val humidity: Int              // 湿度
)

data class WeatherDetail(
    val description: String        // 天气描述，如 "clear sky"
)
