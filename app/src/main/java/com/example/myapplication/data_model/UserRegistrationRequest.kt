package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName
import java.time.LocalDate
import java.util.Date

data class UserRegistrationRequest(
    @SerializedName("loginId")
    val loginId: String,

    @SerializedName("password")
    val password: String,

    @SerializedName("name")
    val name: String,

    @SerializedName("birthday")
    val birthday: String,

    @SerializedName("gender")
    val gender: Char,

    @SerializedName("caregiverId")
    val caregiverId: Long?
)