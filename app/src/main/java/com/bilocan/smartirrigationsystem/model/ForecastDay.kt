package com.bilocan.smartirrigationsystem.model

data class ForecastDay(
    val dayName: String,
    val iconResId: Int,
    val minTemp: Int,
    val maxTemp: Int,
    val desc: String
) 