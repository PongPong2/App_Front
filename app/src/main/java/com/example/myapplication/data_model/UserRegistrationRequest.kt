package com.example.myapplication.data_model

import com.google.gson.annotations.SerializedName

data class UserRegistrationRequest(
    @SerializedName("loginId") // 서버의 #{loginId}와 매칭
    val loginId: String,

    @SerializedName("password") // 서버의 #{password}와 매칭
    val password: String,

    @SerializedName("name") // 서버의 #{name}과 매칭되도록 "username"에서 "name"으로 변경
    val username: String,

    @SerializedName("birthday") // 서버의 #{birthdate}와 매칭되도록 "birth"에서 "birthdate"로 변경
    val birthday: String,

    @SerializedName("gender") // 서버의 #{gender}와 매칭
    val gender: String,

    @SerializedName("caregiverId") // 서버의 #{caregiverId}와 매칭
    val caregiverId: Long?
)
