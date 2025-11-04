package com.example.myapplication.repository

import android.net.Uri
import com.example.myapplication.data_model.LoginResponse
import com.example.myapplication.data_model.UserRegistrationRequest
import retrofit2.Response

interface UserRepository {
    suspend fun registerUser(
        request: UserRegistrationRequest,
        imageUri: Uri?
    ): Response<LoginResponse>
}