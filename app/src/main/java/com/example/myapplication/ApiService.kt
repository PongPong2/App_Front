package com.example.myapplication

import com.example.myapplication.Post
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface ApiService {

    @GET("/api/posts")
    suspend fun getPosts(): Response<List<Post>>

    @GET("/api/posts/{id}")
    suspend fun getPostById(@Path("id") postId: Int): Response<Post>

}