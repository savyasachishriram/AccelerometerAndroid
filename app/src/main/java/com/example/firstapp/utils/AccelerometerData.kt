package com.example.firstapp.utils

data class AccelerometerData(
    var timestamp: String? = null,
    var lat: Double? = null,
    var long: Double? = null,
    var xAxis: Float? = 0.0F,
    var yAxis: Float? = 0.0F,
    var zAxis: Float? = 0.0F
)
