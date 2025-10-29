package com.example.myapplication.viewmodel

sealed interface RegistrationState {
    data object Idle : RegistrationState
    data object Loading : RegistrationState
    data object Success : RegistrationState
    data class Error(val message: String) : RegistrationState
}