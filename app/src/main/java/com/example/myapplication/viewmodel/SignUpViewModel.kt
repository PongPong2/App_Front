package com.example.myapplication.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data_model.UserRegistrationRequest
import com.example.myapplication.data_state.RegistrationState
import com.example.myapplication.repository.UserRepository // 💡 UserRepository import 필요
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// 💡 ViewModelFactory를 통해 UserRepository를 주입받도록 수정하는 것이 권장됩니다.
// 여기서는 간결성을 위해 생성자 인자로 받도록 처리합니다.
class SignUpViewModel(
    private val userRepository: UserRepository, // 💡 UserRepository 주입
    application: Application
) : ViewModel() {

    private val _registrationState = MutableStateFlow<RegistrationState>(RegistrationState.Idle)
    val registrationState: StateFlow<RegistrationState> = _registrationState

    // 💡 register 함수 수정: imageUri 파라미터 추가 및 UserRepository 호출
    fun register(request: UserRegistrationRequest, imageUri: Uri?) {
        _registrationState.value = RegistrationState.Loading

        viewModelScope.launch {
            try {
                // 💡 UserRepository를 통해 파일과 데이터를 함께 전송하도록 위임
                val response = userRepository.registerUser(request, imageUri)

                if (response.isSuccessful) {
                    _registrationState.value = RegistrationState.Success
                } else {
                    // 서버 오류 (4xx, 5xx) 처리
                    _registrationState.value = RegistrationState.Error("이미 존재하는 아이디 입니다. ERROR ${response.code()}")
                }

            } catch (e: HttpException) {
                _registrationState.value = RegistrationState.Error("HTTP 오류: ${e.code()}")
            } catch (e: IOException) {
                // 네트워크 오류 또는 파일 I/O 오류 (UserRepository 내에서 발생 가능)
                _registrationState.value = RegistrationState.Error("네트워크 연결 오류입니다.")
            } catch (e: Exception) {
                _registrationState.value = RegistrationState.Error("알 수 없는 오류: ${e.message}")
            }
        }
    }
}