package com.example.myapplication.workers // 또는 공통 data 패키지

import com.example.myapplication.data.HeartRateData

/**
 * Health Connect에서 읽은 10분 단위 데이터를 서버로 전송하기 위한 모델
 * 이 클래스의 필드명은 서버의 API 규격 및 LSTM 입력 형식에 맞춰야 함
 */
data class HealthData(
    // WorkManager에서 계산/생성한 값
    val walkingSteps: Int, // 10분간의 걸음수 변화량
    val totalCaloriesBurned: Double, // 10분간의 칼로리 변화량
    val spo2: Double, // 산소 포화도 (테스트용 또는 결측치 포함)
    val heartRate: List<HeartRateData>, // 10분간의 심박수 변화
)