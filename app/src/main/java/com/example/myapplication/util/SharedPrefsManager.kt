package com.example.myapplication.util

import android.content.Context

class SharedPrefsManager(context: Context) {

    private val PREFS_FILE_NAME = "user_session"
    private val KEY_USERNAME = "username"
    private val KEY_SILVER_ID = "silverId"
    private val KEY_BIRTHDAY = "birthday"
    private val KEY_ACCESS_TOKEN = "accessToken" // 토큰 키

    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    fun saveUserSession(silverId: String, username: String, birthday: String, accessToken: String) {
        prefs.edit().apply {
            putString(KEY_SILVER_ID, silverId)
            putString(KEY_USERNAME, username)
            putString(KEY_BIRTHDAY, birthday)
            putString(KEY_ACCESS_TOKEN, accessToken) // 토큰 저장
            apply()
        }
    }

    fun saveString(key: String, value: String?) {
        prefs.edit().apply {
            if (value.isNullOrEmpty()) {
                remove(key)
            } else {
                putString(key, value)
            }
            apply()
        }
    }

    fun getStoredString(key: String): String? {
        // KEY_PROFILE_IMAGE_URL 조회 시 사용됨
        return prefs.getString(key, null)
    }

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "환자 이름 없음") ?: "환자 이름 없음"
    fun getGender(): String = prefs.getString(KEY_GENDER, "정보 없음") ?: "정보 없음"
    fun getBirthday(): String = prefs.getString(KEY_BIRTHDAY, "정보 없음") ?: "정보 없음"

    /**
     * 저장된 Silver ID를 불러옵니다. (GuardianActivity 등에서 사용)
     * (Source 1의 getSilverId()와 동일한 기능)
     */
    fun getSilverId(): String? = prefs.getString(KEY_SILVER_ID, null)

    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null) // 토큰 조회 메서드

    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}