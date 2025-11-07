package com.example.myapplication.api

import com.example.myapplication.data_model.LoginRequest
import com.example.myapplication.data_model.LoginResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import com.example.myapplication.data_model.CaregiverResponse

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

    @GET("api/caregivers/by-silver/{silver_login_id}")
    suspend fun getCaregiverBySilverId(
        @Path("silver_login_id") silverLoginId: String
    ): CaregiverResponse

}