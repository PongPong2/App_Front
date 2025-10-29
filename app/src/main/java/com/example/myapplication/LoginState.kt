package com.example.myapplication
sealed interface LoginState {

    val isLoggedIn: Boolean
        get() = this is Success

    data object Idle : LoginState {
        override val isLoggedIn: Boolean = false
    }

    data object Loading : LoginState {
        override val isLoggedIn: Boolean = false
    }

    data class Success(
        val accessToken: String?,
        val username: String?
    ) : LoginState {
        override val isLoggedIn: Boolean = true
    }

    data class Error(
        val errorMessage: String
    ) : LoginState {
        override val isLoggedIn: Boolean = false
    }
}