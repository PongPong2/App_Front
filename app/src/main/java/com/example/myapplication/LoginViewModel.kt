package com.example.myapplication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class LoginViewModel : ViewModel() {

    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState

    private val authService = RetrofitClient.authService

    fun login(loginId: String, password: String) {
        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            try {
                val request = LoginRequest(loginId, password)
                val response = authService.login(request)

                if (response.isSuccessful) {
                    val body = response.body()
                    _loginState.value = LoginState.Success(body?.accessToken, body?.name)
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