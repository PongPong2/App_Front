package com.example.myapplication.util

import android.content.Context

class SharedPrefsManager(context: Context) {

    private val PREFS_FILE_NAME = "user_session"
    private val KEY_USERNAME = "username"
    private val KEY_GENDER = "gender"
    private val KEY_SILVER_ID = "silverId"
    private val KEY_ACCESS_TOKEN = "accessToken" // 토큰 키

    private val prefs = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)

    /**
     * 로그인 성공 시 사용자 세션 정보(ID, 이름, 성별, 토큰)를 한 번에 저장합니다.
     */
    fun saveUserSession(silverId: String, username: String, gender: String, accessToken: String) {
        prefs.edit().apply {
            putString(KEY_SILVER_ID, silverId)
            putString(KEY_USERNAME, username)
            putString(KEY_GENDER, gender)
            putString(KEY_ACCESS_TOKEN, accessToken) // 토큰 저장
            apply()
        }
    }

    /**
     * (프로필 이미지 URL 등) 추가적인 문자열 데이터를 저장합니다.
     */
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

    /**
     * key에 해당하는 문자열 데이터를 불러옵니다.
     */
    fun getStoredString(key: String): String? {
        // (예: KEY_PROFILE_IMAGE_URL 조회 시 사용)
        return prefs.getString(key, null)
    }

    // --- 사용자 정보 조회 ---

    fun getUsername(): String = prefs.getString(KEY_USERNAME, "환자 이름 없음") ?: "환자 이름 없음"
    fun getGender(): String = prefs.getString(KEY_GENDER, "정보 없음") ?: "정보 없음"

    /**
     * 저장된 Silver ID를 불러옵니다. (GuardianActivity 등에서 사용)
     * (Source 1의 getSilverId()와 동일한 기능)
     */
    fun getSilverId(): String? = prefs.getString(KEY_SILVER_ID, null)

    /**
     * 저장된 Access Token을 불러옵니다. (RetrofitClient의 Interceptor에서 사용)
     * (Source 1의 getAccessToken()와 동일한 기능)
     */
    fun getAccessToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    /**
     * 로그아웃 시 모든 저장된 데이터를 삭제합니다.
     * (Source 1의 clear()와 동일한 기능)
     */
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
}