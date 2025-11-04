package com.example.myapplication.domain

import com.google.gson.annotations.SerializedName

data class DailyHealthLogRequest(
    val silverId: String,

    // 혈압 (INT로 서버와 통신)
    @SerializedName("systolicBloodPressure")
    val systolicBloodPressure: Int? = null,

    @SerializedName("diastolicBloodPressure")
    val diastolicBloodPressure: Int? = null,

    // 혈당 (INT)
    @SerializedName("bloodSugar")
    val bloodSugar: Int? = null,

    // 체중 (DECIMAL -> Double)
    @SerializedName("weight")
    val weight: Double? = null,

    // 체온 (DECIMAL -> Double)
    @SerializedName("bodyTemperature")
    val bodyTemperature: Double? = null,

    // 수면 점수 (INT)
    @SerializedName("sleepScore")
    val sleepScore: Int? = null,

    @SerializedName("logDate")
    val logDate: String
)
