package com.example.myapplication.api

import com.example.myapplication.domain.HealthRequest
import com.example.myapplication.domain.HealthResponse
import com.example.myapplication.workers.HealthData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface HealthService {
    @POST("/api/health")
    suspend fun createHealthData(@Body request: HealthData): Response<HealthResponse>
}