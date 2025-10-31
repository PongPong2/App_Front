package com.example.myapplication.API

import com.example.myapplication.data_model.LoginRequest
import com.example.myapplication.data_model.LoginResponse
import com.example.myapplication.data_model.Post
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @GET("api/posts")
    suspend fun getPosts(): Response<List<Post>>

    @GET("api/posts/{id}")
    suspend fun getPostById(@Path("id") postId: Int): Response<Post>

    @POST("/api/login")
    suspend fun login(
        @Body loginRequest: LoginRequest
    ): Response<LoginResponse>


}