package com.example.myapplication.data_model

data class UserRegistrationRequest(
    val loginId: String,
    val password: String,
    val username: String,
    val gender: String,
    val birthday: String,
    val caregiverId: Long?
)