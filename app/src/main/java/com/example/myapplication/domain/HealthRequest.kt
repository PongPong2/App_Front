package com.example.myapplication.domain

import java.time.LocalDateTime


data class HealthRequest (
    val silverId: String,
    val walkingSteps: Int,
    val totalCaloriesBurned: Double,
    val spo2: Int,
    val heartRateAvg: Long,

    // 수면 단계 필드
    val sleepDurationMin: Long,
    val sleepStageWakeMin: Long,
    val sleepStageDeepMin: Long,
    val sleepStageRemMin: Long,
    val sleepStageLightMin: Long,
)