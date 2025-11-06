package com.example.myapplication.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.data_model.LoginRequest // DTO 경로 확인
import com.example.myapplication.data_model.LoginResponse // LoginResponse DTO import 필요
import com.example.myapplication.databinding.LoginBinding
import com.example.myapplication.util.SharedPrefsManager // SharedPrefsManager import
import com.example.myapplication.util.PREFS_NAME // 💡 AppConstants.kt에서 import
import com.example.myapplication.util.KEY_AUTO_LOGIN // 💡 AppConstants.kt에서 import
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding
    private lateinit var sharedPrefsManager: SharedPrefsManager // 매니저 선언

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = LoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            sharedPrefsManager = SharedPrefsManager(this) // 매니저 초기화

            binding.btnLogin.setOnClickListener {
                performLogin()
            }

            binding.btnSignup.setOnClickListener {
                // SignUpActivity 경로는 이미 파일에 import 되어 있음
                val intent = Intent(this, SignUpActivity::class.java)
                startActivity(intent)
            }

            Log.e("LOGIN_INIT_SUCCESS", "LoginActivity initialized successfully. Ready for clicks.")

        } catch (e: Exception) {
            Log.e("LOGIN_INIT_CRASH", "FATAL CRASH in onCreate: ${e.message}", e)
            Toast.makeText(this, "앱 초기화 중 심각한 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
        }
    }

    private fun performLogin() {
        Log.e("LOGIN_DEBUG", "--- performLogin() reached. Starting validation. ---")
        val loginId = binding.inputId.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            Log.d("LOGIN_DEBUG", "Input fields are empty, returning.")
            return
        }

        val request = LoginRequest(
            loginId = loginId,
            password = password
        )

        binding.btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // RetrofitClient.apiService.login 호출 (authService가 정의되어 있다면 변경 권장)
                val response = RetrofitClient.apiService.login(request)

                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true

                    try {
                        if (response.isSuccessful) {
                            val loginResponse = response.body()

                            if (loginResponse == null) {
                                Toast.makeText(this@LoginActivity, "로그인 응답을 받지 못했습니다. (Body가 null)", Toast.LENGTH_SHORT).show()
                                return@withContext
                            }

                            val token = loginResponse.accessToken
                            val silverId = loginResponse.loginId // SilverId로 사용
                            val savedName = loginResponse.name.takeIf { !it.isNullOrEmpty() } ?: ""
                            val savedGender = loginResponse.gender.takeIf { !it.isNullOrEmpty() } ?: "알 수 없음"
                            val autoLoginChecked = binding.checkAutoLogin.isChecked // 체크박스 상태 획득

                            // 토큰, SilverId 필수 확인
                            if (token == null || silverId.isEmpty()) {
                                Toast.makeText(this@LoginActivity, "로그인 정보(토큰 또는 ID)가 없습니다.", Toast.LENGTH_SHORT).show()
                                return@withContext
                            }

                            // SharedPrefsManager를 사용하여 세션 정보 저장 (토큰, ID, 이름, 성별)
                            sharedPrefsManager.saveUserSession(
                                silverId = silverId,
                                username = savedName,
                                gender = savedGender,
                                accessToken = token
                            )

                            // 자동 로그인 설정만 별도로 SharedPreferences에 저장
                            saveAutoLoginSetting(this@LoginActivity, autoLoginChecked)

                            Log.d("LOGIN_SUCCESS", "세션 저장 완료. SilverId: $silverId")

                            Toast.makeText(
                                this@LoginActivity,
                                "안녕하세요! ${savedName.ifEmpty { silverId }}님!",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent = Intent(this@LoginActivity, MainPageActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                            Toast.makeText(this@LoginActivity, "로그인 실패: ${response.code()}", Toast.LENGTH_LONG).show()
                            Log.e("LOGIN_FAIL", "Error Code: ${response.code()}, Body: $errorBody")
                        }
                    } catch (e: Exception) {
                        Log.e("LOGIN_PARSE_CRASH", "DTO 파싱 또는 UI 처리 중 오류 발생: ${e.message}", e)
                        Toast.makeText(this@LoginActivity, "로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("LOGIN_ERROR", "네트워크/통신 예외: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, "네트워크 연결 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * 자동 로그인 여부만 저장하는 헬퍼 함수
     */
    private fun saveAutoLoginSetting(context: Context, autoLogin: Boolean) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putBoolean(KEY_AUTO_LOGIN, autoLogin)
            commit()
        }
    }
}