package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String?,
    @SerializedName("username") val username: String?,
    @SerializedName("gender") val gender: String?
)