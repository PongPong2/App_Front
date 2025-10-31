package com.example.myapplication.API

import com.example.myapplication.data_model.UserRegistrationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface UserService {

    @POST("api/users")
    suspend fun registerUser(@Body request: UserRegistrationRequest): Response<Void>
}