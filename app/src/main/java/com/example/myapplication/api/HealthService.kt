package com.example.myapplication.api

import com.example.myapplication.domain.BloodPressureSummaryResponse
import com.example.myapplication.domain.DailyHealthLogRequest
import com.example.myapplication.domain.HealthRequest
import com.example.myapplication.domain.HealthResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HealthService {
    @POST("/api/health/data")
    suspend fun createHealthData(@Body request: HealthRequest): Response<HealthResponse>

    @POST("/api/health/daily-log")
    suspend fun sendDailyHealthLog(@Body request: DailyHealthLogRequest): Response<BloodPressureSummaryResponse>
}