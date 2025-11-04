package com.example.myapplication.activity

import android.Manifest
import android.app.AlertDialog
import android.app.Application
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.icu.util.Calendar
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.api.UserService
import com.example.myapplication.data_model.UserRegistrationRequest
import com.example.myapplication.data_state.RegistrationState
import com.example.myapplication.databinding.SignupBinding
import com.example.myapplication.util.BirthDayTextWatcher // 💡 Import
import com.example.myapplication.viewmodel.SignUpViewModel
import com.example.myapplication.repository.UserRepositoryImpl // 💡 Import
import com.google.gson.Gson // 💡 Import
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException // 💡 Import
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    private var selectedImageUris = listOf<Uri>()
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>


    // 💡 ViewModel 초기화: 커스텀 Factory를 사용하여 의존성 주입
    private val viewModel: SignUpViewModel by viewModels {
        SignUpViewModelFactory(application, RetrofitClient.userService)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivityResultLaunchers() // 이미지 런처 초기화

        // 💡 ViewBinding 사용
        binding.birthdayInput.addTextChangedListener(BirthDayTextWatcher(binding.birthdayInput))
        setupBirthdayField() // DatePickerDialog 설정

        binding.cardProfileImage.setOnClickListener {
            checkPermissionAndOpenGallery() // 이미지 선택 플로우 시작
        }

        binding.btnSignup.setOnClickListener {
            attemptRegistration()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        observeRegistrationState()
    }

    private fun setupBirthdayField() {
        // 💡 [수정] ViewBinding 사용
        val inputBirthday = binding.birthdayInput
        val calendar = Calendar.getInstance()

        inputBirthday.setOnClickListener {
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    val selectedDate = Calendar.getInstance().apply {
                        set(selectedYear, selectedMonth, selectedDay)
                    }.time

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val formattedDate = dateFormat.format(selectedDate)

                    inputBirthday.setText(formattedDate)
                },
                year,
                month,
                day
            )
            datePickerDialog.show()
        }
    }

    private fun initActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri>? ->
            uris?.let {
                if (it.isNotEmpty()) {
                    selectedImageUris = it
                    binding.imgProfile.setImageURI(it[0]) // 첫 번째 이미지를 프로필로 표시
                    Toast.makeText(this, "이미지 ${uris.size}장이 선택되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    selectedImageUris = emptyList()
                }
            }
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                openGallery()
            } else {
                Toast.makeText(this, "저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        }
        else if (shouldShowRequestPermissionRationale(permission)) {
            showRationaleDialog(permission)
        } else {
            permissionLauncher.launch(permission)
        }
    }

    private fun showRationaleDialog(permission: String) {
        AlertDialog.Builder(this)
            .setTitle("권한 필요")
            .setMessage("프로필 이미지를 등록하고 서버로 전송하려면 사진첩 접근 권한이 필요합니다.")
            .setPositiveButton("권한 요청") { _, _ ->
                permissionLauncher.launch(permission)
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun attemptRegistration() {
        val name = binding.inputName.text.toString().trim()
        val loginId = binding.inputId.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val passwordConfirm = binding.inputPasswordConfirm.text.toString().trim()

        // [수정] ViewBinding 사용 (birthdayInput)
        val birthdayStr = binding.birthdayInput.text.toString().trim()

        // 💡 [수정] 성별 타입을 Char 대신 String?으로 변경
        val gender: String? = when (binding.radioGroupGender.checkedRadioButtonId) {
            binding.radioMale.id -> "M"
            binding.radioFemale.id -> "F"
            else -> null
        }

        // 1. 유효성 검사
        if (name.isEmpty() || loginId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || birthdayStr.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            binding.inputPassword.text?.clear()
            binding.inputPasswordConfirm.text?.clear()
            return
        }

        if (gender == null) {
            Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 2. 생년월일 형식 검사
        try {
            LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            Toast.makeText(this, "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
            return
        }

        // 3. 요청 객체 생성 및 ViewModel 호출
        val profileUri: Uri? = selectedImageUris.firstOrNull()

        val request = UserRegistrationRequest(
            loginId = loginId,
            password = password,
            username = name,
            gender = gender,
            birthday = birthdayStr,
            caregiverId = null
        )

        viewModel.register(request, profileUri)
    }

    private fun observeRegistrationState() {
        lifecycleScope.launch {
            viewModel.registrationState.collect { state ->
                when (state) {
                    is RegistrationState.Loading -> {
                        binding.btnSignup.isEnabled = false
                        binding.btnSignup.text = "가입 진행 중..."
                    }
                    is RegistrationState.Success -> {
                        Toast.makeText(this@SignUpActivity, "회원가입 성공! 로그인 화면으로 이동합니다.", Toast.LENGTH_LONG).show()

                        val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        startActivity(intent)
                        finish() // 현재 Activity를 종료하고 스택에서 제거
                    }
                    is RegistrationState.Error -> {
                        Toast.makeText(this@SignUpActivity, state.message, Toast.LENGTH_LONG).show()
                        binding.btnSignup.isEnabled = true
                        binding.btnSignup.text = "회원가입"
                    }
                    is RegistrationState.Idle -> {
                        binding.btnSignup.isEnabled = true
                        binding.btnSignup.text = "회원가입"
                    }
                }
            }
        }
    }
}

// ## ⚙ViewModel Factory

/**
 * SignUpViewModel에 필요한 의존성(UserRepository, Application)을 수동으로 주입하기 위한 팩토리입니다.
 * 이 팩토리는 SignUpActivity 내부에 위치해도 되지만, 별도 파일에 위치하는 것이 일반적입니다.
 */
class SignUpViewModelFactory(
    private val application: Application,
    private val userService: UserService // Retrofit Service Interface
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            val gson = Gson()
            val userRepository = UserRepositoryImpl(
                userService,
                application.applicationContext,
                gson
            )
            // SignUpViewModel의 생성자에 UserRepository와 Application 객체를 전달
            return SignUpViewModel(userRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}