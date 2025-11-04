package com.example.myapplication.api

import android.content.Context
import com.example.myapplication.api.ApiService
import com.example.myapplication.util.SharedPrefsManager // 이 클래스는 실제 경로에 따라 수정 필요
import com.example.myapplication.api.HealthService
import com.example.myapplication.api.UserService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

// AuthService, UserService, HealthService 등의 인터페이스 경로는 프로젝트 구조에 맞게 수정해주세요.
// 현재는 임시로 com.example.myapplication 패키지 아래에 있다고 가정합니다.

object RetrofitClient {

    private lateinit var applicationContext: Context

    // BASE_URL은 팀원과 내 것 중 통신이 잘되는 것으로 최종 선택해야 합니다.
    public const val BASE_URL = "http://192.168.1.109:8080"

    /**
     * Context를 초기화하는 함수. Application 클래스에서 반드시 호출되어야 함.
     */
    fun initialize(context: Context) {
        this.applicationContext = context.applicationContext
    }

    // 1. Gson 설정 (팀원 코드 반영: LocalDate, LocalDateTime 어댑터)
    private val gson: Gson by lazy {
        GsonBuilder()
            // LocalDate 어댑터 (예: "1999-03-12")
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonSerializer<LocalDate> { src, _, _ ->
                    src?.format(DateTimeFormatter.ISO_LOCAL_DATE)?.let { com.google.gson.JsonPrimitive(it) }
                }
            )
            .registerTypeAdapter(
                LocalDate::class.java,
                JsonDeserializer<LocalDate> { json, _, _ ->
                    LocalDate.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE)
                }
            )
            // LocalDateTime 어댑터 (예: "2025-11-03T10:00:00")
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonSerializer<LocalDateTime> { src, _, _ ->
                    src?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)?.let { com.google.gson.JsonPrimitive(it) }
                }
            )
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonDeserializer<LocalDateTime> { json, _, _ ->
                    LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
            )
            .create()
    }

    // OkHttpClient 설정 (내 코드 반영: 인증 Interceptor)
    private val client: OkHttpClient by lazy {

        // a) 인증 Interceptor: 저장된 토큰을 가져와 헤더에 추가
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // SharedPrefsManager는 Context가 초기화되었을 때만 작동
            val token = SharedPrefsManager(applicationContext).getAccessToken()

            val requestBuilder = originalRequest.newBuilder()
                // 팀원 코드의 User-Agent 헤더 추가
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Safari/537.36")

            // 토큰이 존재하면 Authorization 헤더를 추가
            token?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }

            chain.proceed(requestBuilder.build())
        }

        // 로깅 Interceptor
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // OkHttpClient 빌드: 타임아웃 설정 통합
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS) // 팀원 코드 기준
            .readTimeout(30, TimeUnit.SECONDS)    // 팀원 코드 기준
            .writeTimeout(30, TimeUnit.SECONDS)   // 팀원 코드 기준
            .build()
    }


    // Retrofit Builder 및 Service 정의
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson)) // 💡 커스텀 Gson 적용
            .build()
    }

    // 모든 서비스 정의


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
