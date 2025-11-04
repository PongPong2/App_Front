package com.example.myapplication.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.API.RetrofitClient
import com.example.myapplication.data_model.LoginRequest
import com.example.myapplication.databinding.LoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.myapplication.KEY_GENDER
import com.example.myapplication.KEY_NAME
import com.example.myapplication.KEY_TOKEN
import com.example.myapplication.KEY_LOGIN_ID
import com.example.myapplication.KEY_AUTO_LOGIN
import com.example.myapplication.PREFS_NAME

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = LoginBinding.inflate(layoutInflater)
            setContentView(binding.root)

            binding.btnLogin.setOnClickListener {
                performLogin()
            }

            binding.btnSignup.setOnClickListener {
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
                val response = RetrofitClient.apiService.login(request)

                // 💡 [수정] Main 스레드 내에서 파싱 및 UI 작업 중 발생하는 예외를 잡기 위해 try-catch 추가
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true

                    try {
                        if (response.isSuccessful) {
                            val loginResponse = response.body()
                            if (loginResponse == null) {
                                Toast.makeText(this@LoginActivity, "로그인 응답을 받지 못했습니다. (Body가 null)", Toast.LENGTH_SHORT).show()
                                Log.e("LOGIN_FAIL", "Response successful but body is null")
                                return@withContext
                            }

                            // DTO 필드가 Non-nullable인데 서버에서 null을 보냈을 때 여기서 파싱 오류 발생 가능
                            val token = loginResponse.accessToken
                            val savedLoginId = loginResponse.loginId ?: ""
                            val savedName = loginResponse.name.takeIf { !it.isNullOrEmpty() } ?: ""
                            val savedGender = loginResponse.gender.takeIf { !it.isNullOrEmpty() } ?: "알 수 없음"
                            val autoLoginChecked = binding.checkAutoLogin.isChecked

                            Log.e("PARSING_CHECK", "Raw Name: ${loginResponse.name}, Assigned Name: $savedName")
                            Log.e("PARSING_CHECK", "Raw Gender: ${loginResponse.gender}, Assigned Gender: $savedGender")

                            if (token == null || savedLoginId.isEmpty()) {
                                Toast.makeText(this@LoginActivity, "로그인 정보(토큰 또는 ID)가 없습니다.", Toast.LENGTH_SHORT).show()
                                Log.e("LOGIN_FAIL", "Access token or Login ID is null or empty")
                                return@withContext
                            }

                            // 💡 [핵심 로그 확인] 이 로그가 보인다면 화면 전환 실패는 DTO 파싱 이후에 발생한 것임
                            Log.d("LOGIN_SUCCESS", "서버 응답 ID: $savedLoginId, 이름: $savedName, 성별: $savedGender. 화면 전환 시도.")

                            // 💡 수정된 부분: 변수들이 정의된 후, 로그인 성공 시점에 호출됩니다.
                            saveLoginInfo(this@LoginActivity, token, savedLoginId, savedName, savedGender, autoLoginChecked)

                            Toast.makeText(
                                this@LoginActivity,
                                "안녕하세요! ${savedName.ifEmpty { savedLoginId }}님!",
                                Toast.LENGTH_LONG
                            ).show()

                            val intent = Intent(this@LoginActivity, MainPageActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                            Toast.makeText(
                                this@LoginActivity,
                                "로그인 실패: $errorBody",
                                Toast.LENGTH_LONG
                            ).show()
                            Log.e("LOGIN_FAIL", "Error Code: ${response.code()}, Body: $errorBody")
                        }
                    } catch (e: Exception) {
                        // 💡 DTO 파싱 오류, NullPointerException 등 메인 스레드에서 발생하는 오류 포착
                        Log.e("LOGIN_PARSE_CRASH", "DTO 파싱 또는 UI 처리 중 오류 발생: ${e.message}", e)
                        Toast.makeText(this@LoginActivity, "로그인 처리 중 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                // 네트워크 오류 (연결, 타임아웃 등) 포착
                Log.e("LOGIN_ERROR", "네트워크/통신 예외: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, "네트워크 연결 오류가 발생했습니다.", Toast.LENGTH_LONG)
                        .show()
                }
            }
        }
    }

    private fun saveLoginInfo(context: Context, token: String, loginId: String, name: String, gender: String, autoLogin: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        Log.d("SAVING_DEBUG", "SHP에 저장될 이름: $name, SHP에 저장될 성별: $gender")
        with(prefs.edit()) {
            putString(KEY_TOKEN, token)
            putString(KEY_LOGIN_ID, loginId)
            putString(KEY_NAME, name)
            putString(KEY_GENDER, gender)
            putBoolean(KEY_AUTO_LOGIN, autoLogin)
            commit()
        }
    }
}