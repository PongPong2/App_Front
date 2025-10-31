package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.API.RetrofitClient
import kotlinx.coroutines.launch
import java.io.IOException

class MainViewModel : ViewModel() {

    fun fetchPosts() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getPosts()

                if (response.isSuccessful) {
                    val postList = response.body()

                    if (postList != null) {
                        println("데이터 로드 성공: ${postList.size}개")
                    } else {
                        println("데이터 로드 성공 (목록 없음)")
                    }
                } else {
                    println("API 요청 실패: ${response.code()}")
                }

            } catch (e: IOException) {
                println("네트워크 연결 실패: ${e.message}")
            } catch (e: Exception) {
                println("API 호출 또는 처리 실패: ${e.message}")
            }
        }
    }
}