package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName
data class SilverMedication(
    @SerializedName("id")
    val id: Long,
    @SerializedName("silverId")
    val silverId: String,
    @SerializedName("medicationType")
    val medicationType: String,
    @SerializedName("notes")
    val notes: String
)