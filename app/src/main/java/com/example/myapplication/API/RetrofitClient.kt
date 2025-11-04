package com.example.myapplication.API

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.102:8080"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val requestWithUserAgent = originalRequest.newBuilder()
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.0.0 Safari/537.36")
                .build()
            chain.proceed(requestWithUserAgent)
        }
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

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
            // 💡 서버가 "2025-11-03 06:15:22.0" 같은 형식을 보낼 경우, DateTimeFormatter 수정 필요
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonSerializer<LocalDateTime> { src, _, _ ->
                    src?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)?.let { com.google.gson.JsonPrimitive(it) }
                }
            )
            .registerTypeAdapter(
                LocalDateTime::class.java,
                JsonDeserializer<LocalDateTime> { json, _, _ ->
                    // 💡 기본 Gson이 파싱할 수 있는 표준 ISO 형식으로 가정
                    LocalDateTime.parse(json.asString, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                }
            )
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            // 💡 3. [수정] 기본 Gson 대신 커스텀 Gson을 사용하도록 변경
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }

    // 💡 4. [수정] 사용할 API 서비스 (ApiService)
    //    (UserService가 별도로 필요하지 않다면 이전에 정의한 ApiService만 사용)
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    val userService: UserService by lazy {
         retrofit.create(UserService::class.java)
    }
}