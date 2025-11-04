package com.example.myapplication

import android.content.Context
import com.example.myapplication.api.HealthService
import com.example.myapplication.network.service.UserService
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor

object RetrofitClient {

    private lateinit var applicationContext: Context

    private const val BASE_URL = "http://10.32.25.81:8080"

    fun initialize(context: Context) {
        this.applicationContext = context.applicationContext
    }

    // 3. OkHttpClient 및 Interceptor 정의
    private val client: OkHttpClient by lazy {

        // a) 인증 Interceptor: 저장된 토큰을 가져와 헤더에 추가
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // SharedPrefsManager를 사용하여 저장된 토큰을 가져옵니다.
            val token = SharedPrefsManager(applicationContext).getAccessToken()

            // 토큰이 존재하면 Authorization 헤더를 추가합니다.
            val requestBuilder = originalRequest.newBuilder()
            token?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }

            chain.proceed(requestBuilder.build())
        }

        // b) 로깅 Interceptor (선택 사항이지만 디버깅에 유용)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // c) OkHttpClient 빌드
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)  // 인증 인터셉터 추가 (403 해결)
            .addInterceptor(loggingInterceptor) // 로깅 인터셉터 추가
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }


    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // OkHttpClient 적용
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