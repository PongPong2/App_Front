package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.request.UserRegistrationRequest
import com.example.myapplication.RetrofitClient
import com.example.myapplication.network.service.UserService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

class SignUpViewModel : ViewModel() {

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    private val userService: UserService = RetrofitClient.userService

    fun register(request: UserRegistrationRequest) {
        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                // Retrofit 서비스 호출: POST /api/users
                val response = userService.registerUser(request)

                if (response.isSuccessful) {
                    _registrationState.value = RegistrationState.Success
                } else {
                    val errorBody = response.errorBody()?.string() ?: "알 수 없는 서버 오류"
                    _registrationState.value = RegistrationState.Error("회원가입 실패: ${response.code()}")
                }

            } catch (e: HttpException) {
                _registrationState.value = RegistrationState.Error("HTTP 오류: ${e.code()}")
            } catch (e: IOException) {
                _registrationState.value = RegistrationState.Error("네트워크 연결 오류입니다.")
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("알 수 없는 오류: ${e.message}")
            }
        }
    }
}