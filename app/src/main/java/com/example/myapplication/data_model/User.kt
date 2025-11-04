package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName

//* [DB 조회용 DTO]
//* DB에서 데이터를 읽어올 때 사용
data class User(
    val id: Int,

    @SerializedName("caregiver_id")
    val caregiverId: Int?,

    @SerializedName("login_id")
    val loginId: String?,

    val name: String?,
    val gender: String?,
    @SerializedName("birthday")
    val birthday: String?,

    @SerializedName("created_at")
    val createdAt: String?
)