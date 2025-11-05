package com.example.myapplication.data_state

import com.example.myapplication.data_model.LoginResponse

sealed interface LoginState {

    // LoginResponse 객체 또는 null을 가질 수 있음을 선언
    val loginResponse: LoginResponse?

    // UI 상태 변화 감지를 위한 플래그
    val isLoggedIn: Boolean
        get() = this is Success

    data object Idle : LoginState {
        override val loginResponse: LoginResponse? = null
        override val isLoggedIn: Boolean = false
    }

    data object Loading : LoginState {
        override val loginResponse: LoginResponse? = null
        override val isLoggedIn: Boolean = false
    }

    data class Success(
        override val loginResponse: LoginResponse?
    ) : LoginState {
        override val isLoggedIn: Boolean = true
    }

    data class Error(
        val errorMessage: String
    ) : LoginState {
        override val loginResponse: LoginResponse? = null
        override val isLoggedIn: Boolean = false
    }
}