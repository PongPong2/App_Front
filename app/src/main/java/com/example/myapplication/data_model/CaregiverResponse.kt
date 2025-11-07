package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName

// 백엔드 DTO와 필드 이름을 일치시킵니다.
data class CaregiverResponse(
    val name: String,
    val tel: String,
    val gender: String,
    val affiliation: String,
    @SerializedName("storedFilename")
    val profileImageUrl: String?
)