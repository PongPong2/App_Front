package com.example.myapplication.api

import android.content.Context
import com.dasom.app.network.GuardianApiService
import com.example.myapplication.util.SharedPrefsManager
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
import com.example.myapplication.util.BASE_URL

/**
 * Retrofit 인스턴스를 관리하는 싱글톤 객체입니다.
 * 🚨 사용하기 전에 반드시 Application 클래스 등에서 RetrofitClient.initialize(context)를 호출해야 합니다.
 */
object RetrofitClient {

    // 🚨 인증 인터셉터에서 SharedPrefsManager를 사용하기 위해 Application Context가 필요합니다.
    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        this.applicationContext = context.applicationContext
    }


    // 1. Gson 설정 (LocalDate, LocalDateTime 처리를 위한 커스텀 어댑터 포함)
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

    // 2. OkHttpClient 설정 (인증 헤더 추가, 로깅, 타임아웃 설정)
    private val client: OkHttpClient by lazy {

        // a) 🚨 인증 인터셉터: API 요청 시 헤더에 자동으로 AccessToken(JWT)을 추가합니다.
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // 🚨 Context가 초기화되어야만 SharedPrefsManager 사용 가능
            val token = SharedPrefsManager(applicationContext).getAccessToken()

            val requestBuilder = originalRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Safari/537.36")

            // 토큰이 있는 경우에만 "Authorization" 헤더 추가
            token?.let {
                requestBuilder.header("Authorization", "Bearer $it")
            }

            chain.proceed(requestBuilder.build())
        }

        // b) 로깅 인터셉터 (개발용: Logcat에서 API 통신 내역 확인)
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // c) OkHttpClient 빌드: 타임아웃 및 인터셉터 적용
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor) // 🚨 인증 헤더를 추가하는 인터셉터 적용
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }


    // 3. Retrofit 인스턴스 (Base URL, OkHttpClient, Gson 변환기 적용)
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL) // 🚨 BASE_URL (예: "http://10.0.2.2:8080/")
            .client(client) // 🚨 위에서 설정한 OkHttpClient (인증 헤더 포함) 적용
            .addConverterFactory(GsonConverterFactory.create(gson)) // 🚨 날짜 파싱용 커스텀 Gson 적용
            .build()
    }

    // 4. 🚨 Activity에서 호출할 서비스들 정의
    // (Activity에서는 RetrofitClient.apiService.메서드() 형태로 호출)

    // 🚨 YoyangsaActivity에서 요양사 정보를 조회할 때 사용할 서비스
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val userService: UserService by lazy {
        retrofit.create(UserService::class.java)
    }

    val healthService: HealthService by lazy {
        retrofit.create(HealthService::class.java)
    }

    // 🚨 BohojaActivity에서 보호자 정보를 조회할 때 사용할 서비스
    val guardianApiService: GuardianApiService by lazy {
        retrofit.create(GuardianApiService::class.java)
    }
}