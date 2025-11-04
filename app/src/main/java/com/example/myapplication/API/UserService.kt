package com.example.myapplication.API

import com.example.myapplication.data_model.LoginResponse
import com.example.myapplication.data_model.UserRegistrationRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UserService {
    @Multipart
    @POST("api/signup")
    suspend fun registerUser(
        @Part parts: List<MultipartBody.Part>
    ): Response<LoginResponse>
}