package com.example.myapplication.data.request

data class UserRegistrationRequest(
    val loginId: String,
    val password: String,
    val username: String,
    val gender: String,
    val caregiverId: Long?
)