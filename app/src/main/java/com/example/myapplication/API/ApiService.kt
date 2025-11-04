package com.example.myapplication.API

import com.example.myapplication.data_model.LoginRequest
import com.example.myapplication.data_model.LoginResponse
import com.example.myapplication.data_model.User
import com.example.myapplication.data_model.UserRegistrationRequest
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    @POST("/api/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>

    @Multipart
    @POST("/api/signup")
    suspend fun signup(
        @Part("user") user: RequestBody, // JSON 데이터를 담는 파트
        @Part imageFiles: List<MultipartBody.Part> // 이미지 파일 목록을 담는 파트
    ): Response<LoginResponse>

}