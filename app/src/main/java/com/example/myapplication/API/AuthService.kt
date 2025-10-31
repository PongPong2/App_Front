package com.example.myapplication.API

import com.example.myapplication.data_model.LoginRequest
import com.example.myapplication.data_model.LoginResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthService {

    @POST("/api/auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>
}