package com.dasom.app.network

import com.example.myapplication.data_model.GuardianResponse
import retrofit2.http.GET
import retrofit2.http.Path

interface GuardianApiService {
    @GET("api/guardians/{silverId}")
    suspend fun getGuardians(
        @Path("silverId") silverId: String
    ): List<GuardianResponse>
}