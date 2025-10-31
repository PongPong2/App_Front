package com.example.myapplication.util

import android.content.Context

class SharedPrefsManager(context: Context) {
    private val prefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    fun saveUserData(username: String, gender: String) {
        prefs.edit().putString("username", username).apply()
        prefs.edit().putString("gender", gender).apply()
    }

    fun getUsername(): String = prefs.getString("username", "환자 이름 없음") ?: "환자 이름 없음"
    fun getGender(): String = prefs.getString("gender", "정보 없음") ?: "정보 없음"
}