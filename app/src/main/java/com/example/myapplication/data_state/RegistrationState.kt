package com.example.myapplication.data_state

sealed interface RegistrationState {
    data object Idle : RegistrationState
    data object Loading : RegistrationState
    data object Success : RegistrationState
    data class Error(val message: String) : RegistrationState
}