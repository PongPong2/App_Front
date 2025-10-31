package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    @SerializedName("accessToken") val accessToken: String,
    @SerializedName("name") val name: String,
    @SerializedName("gender") val gender: String
)