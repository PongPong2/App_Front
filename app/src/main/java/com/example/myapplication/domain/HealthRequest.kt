package com.example.myapplication.domain

import java.time.LocalDateTime


//보류
data class HealthRequest (
    val silverId: String,
    val age: Int,
    val gender: Char,
    val rhr: Double,
    val walkingSteps: Int,
    val totalCaloriesBurned: Double,
    val spo2: Int,
    val heartRateAvg: Double,
    val logDate: LocalDateTime,

//    val totalSleepDurationMin: Int,
    val sleepDurationMin: Int,
    val sleepStageWakeMin: Int,
    val sleepStageDeepMin: Int,
    val sleepStageRemMin: Int,
    val sleepStageLightMin: Int
)