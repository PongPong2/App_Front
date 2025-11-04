package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.databinding.LoginBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: LoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            performLogin()
        }

        binding.btnSignup.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val loginId = binding.inputId.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()

        if (loginId.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = LoginRequest(
            loginId = loginId,
            password = password
        )

        binding.btnLogin.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitClient.authService.login(request)

                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true

                    if (response.isSuccessful) {
                        val loginResponse = response.body()
                        val token = loginResponse?.accessToken

                        Toast.makeText(this@LoginActivity, "로그인 성공! 사용자: ${loginResponse?.name}", Toast.LENGTH_LONG).show()

                        // 로그인 성공 후 메인 페이지로 이동
                        val intent = Intent(this@LoginActivity, MainPageActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "알 수 없는 오류"
                        Toast.makeText(this@LoginActivity, "로그인 실패: ${response.code()}", Toast.LENGTH_LONG).show()
                        Log.e("LOGIN_FAIL", "Error Code: ${response.code()}, Body: $errorBody")
                    }
                }
            } catch (e: Exception) {
                Log.e("LOGIN_ERROR", "Exception: ${e.message}")
                withContext(Dispatchers.Main) {
                    binding.btnLogin.isEnabled = true
                    Toast.makeText(this@LoginActivity, "네트워크 연결 오류가 발생했습니다.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}