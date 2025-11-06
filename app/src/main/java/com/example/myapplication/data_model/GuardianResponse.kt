package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName

data class GuardianResponse(
    val name: String,
    val tel: String,
    val relationship: String,
    val address: String,
    val profileImageUrl: String? = null
)