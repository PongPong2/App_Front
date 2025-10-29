package com.example.myapplication

import android.os.Bundle
<<<<<<< HEAD
<<<<<<< HEAD
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import com.example.myapplication.databinding.SignupBinding
import com.example.myapplication.data.request.UserRegistrationRequest
import com.example.myapplication.viewmodel.SignUpViewModel
import com.example.myapplication.viewmodel.RegistrationState
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private val viewModel: SignUpViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSignup.setOnClickListener {
            attemptRegistration()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        observeRegistrationState()
    }

    private fun attemptRegistration() {
        val name = binding.inputName.text.toString().trim()
        val loginId = binding.inputId.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val passwordConfirm = binding.inputPasswordConfirm.text.toString().trim()

        val gender = when (binding.radioGroupGender.checkedRadioButtonId) {
            binding.radioMale.id -> "M"
            binding.radioFemale.id -> "F"
            else -> ""
        }

        if (name.isEmpty() || loginId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            binding.inputPassword.text?.clear()
            binding.inputPasswordConfirm.text?.clear()
            return
        }

        if (gender.isEmpty()) {
            Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val request = UserRegistrationRequest(
            loginId = loginId,
            password = password,
            username = name,
            gender = gender,
            caregiverId = null
        )

        viewModel.register(request)
    }

    private fun observeRegistrationState() {
        lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is RegistrationState.Loading -> {
                    }
                    is RegistrationState.Success -> {
                        Toast.makeText(this@SignUpActivity, "회원가입 성공! 로그인 화면으로 이동합니다.", Toast.LENGTH_LONG).show()
                        finish()
                    }
                    is RegistrationState.Error -> {
                        Toast.makeText(this@SignUpActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    is RegistrationState.Idle -> { }
                }
            }
        }
    }
}
=======
=======
>>>>>>> 1c8f4a748c5d7dc25ee28cd1f656b8de3b80ea50
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        val backButton: Button = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        val signUpButton: Button = findViewById(R.id.btn_signup)
        signUpButton.setOnClickListener {
            finish()
        }
    }
}
<<<<<<< HEAD
>>>>>>> 051e103096799e51629f301552ffd1c0542ca37d
=======
>>>>>>> 1c8f4a748c5d7dc25ee28cd1f656b8de3b80ea50
