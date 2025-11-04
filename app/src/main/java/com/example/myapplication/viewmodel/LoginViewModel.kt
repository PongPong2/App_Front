package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.API.RetrofitClient
import com.example.myapplication.data_model.LoginRequest
import com.example.myapplication.data_state.LoginState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val apiService = RetrofitClient.apiService

    fun login(loginId: String, password: String, gender: String = "") {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val request = LoginRequest(loginId, password)
                val response = apiService.login(request)
                Log.d("LoginViewModel", "Response: $response")

                if (response.isSuccessful) {
                    val body = response.body()


                } else {
                    val errorMsg = response.errorBody()?.string() ?: "인증 실패"
                    _loginState.value = LoginState.Error(errorMsg)
                }
            } catch (e: HttpException) {
                _loginState.value = LoginState.Error("서버 오류: ${e.code()}")
            } catch (e: IOException) {
                _loginState.value = LoginState.Error("네트워크 오류가 발생했습니다.")
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("알 수 없는 오류: ${e.message}")
            }
        }
    }
}
