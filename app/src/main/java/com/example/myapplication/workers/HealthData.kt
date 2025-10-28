package com.example.myapplication.workers // 또는 공통 data 패키지

import com.example.myapplication.data.HeartRateData

/**
 * Health Connect에서 읽은 10분 단위 데이터를 서버로 전송하기 위한 모델
 * 이 클래스의 필드명은 서버의 API 규격 및 LSTM 입력 형식에 맞춰야 함
 */
data class HealthData(
    val walkingSteps: Int,
    val totalCaloriesBurned: Double,
    val spo2: Double,
    val heartRateAvg: Double,
    // 수면 단계 필드
    val sleepDurationMin: Long,
    val sleepStageWakeMin: Long,
    val sleepStageDeepMin: Long,
    val sleepStageRemMin: Long,
    val sleepStageLightMin: Long
)