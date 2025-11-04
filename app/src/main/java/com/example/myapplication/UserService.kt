package com.example.myapplication.network.service

import com.example.myapplication.data.request.UserRegistrationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {

    @POST("api/signup")
    suspend fun registerUser(@Body request: UserRegistrationRequest): Response<Void>
}