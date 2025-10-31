package com.example.myapplication

data class LoginResponse(
    val accessToken: String,
    val userId: Long,
    val username: String,
    val gender: String
)