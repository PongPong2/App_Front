package com.example.myapplication.network

import com.example.myapplication.data_model.SilverMedication // DTO 경로에 맞게 수정
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface MedicationApiService {

    @GET("/api/medications/{silverId}")
    suspend fun getMedications(
        @Path("silverId") silverId: String
    ): Response<List<SilverMedication>>
}