package com.example.myapplication.data_model

import androidx.health.connect.client.records.ExerciseRouteResult
import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.util.Date

data class LoginResponse(
    val success: Boolean,
    val message: String?,
    val accessToken: String?,
    val loginId: String?,
    val name: String?,
    val gender: String?,
    @SerializedName("birthday")
    val birthday: String?,
    val images: List<String>?
)
