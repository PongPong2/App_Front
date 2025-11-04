package com.example.myapplication

import com.google.gson.annotations.SerializedName

data class LoginResponse(
    val success: Boolean,
    val message: String,

    @SerializedName("accessToken")
    val accessToken: String,

    @SerializedName("loginId")
    val loginId: String,

    @SerializedName("name")
    val name: String,


    @SerializedName("gender")
    val gender: String,

    @SerializedName("birthday")
    val birthday: String? = null,

    @SerializedName("images")
    val images: List<String>? = null
)