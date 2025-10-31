package com.example.myapplication.data_model

data class Post(
    val userId: Int,
    val caregiber_id: Int,
    val login_id: String,
    val password: String,
    val name: String,
    val gender: String,
    val created_at: String
)