package com.example.myapplication.data_model
import com.google.gson.annotations.SerializedName
import java.util.Date

//* [백엔드 INSERT 요청용 DTO]
//* 클라이언트가 서버로 보내야 하는 최소한의 정보를 정의합니다.

data class RegisterRequest(
    val loginId: String,
    val password: String,
    val name: String,
    val gender: Char,
    @SerializedName("birthday")
    val birthday: String?,
    val caregiverId: Long? = null
)