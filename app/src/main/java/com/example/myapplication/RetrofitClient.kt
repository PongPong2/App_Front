package com.example.myapplication

import com.example.myapplication.api.HealthService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.myapplication.network.service.UserService
object RetrofitClient {
//    private const val BASE_URL = "http://10.32.31.66:8080/"



    
    //이거 바꾸셔야함
    private const val BASE_URL = "http://192.168.1.109:8080"

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val authService: AuthService by lazy {
        retrofit.create(AuthService::class.java)
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val userService: UserService by lazy {
        retrofit.create(UserService::class.java)
    }

    val healthService: HealthService by lazy {
        retrofit.create(HealthService::class.java)
    }
}