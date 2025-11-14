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
import com.example.myapplication.util.BirthDayTextWatcher
import com.example.myapplication.viewmodel.SignUpViewModel
import com.example.myapplication.repository.UserRepositoryImpl
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding

    // 💡 여러 이미지를 받을 수 있으므로 'List' 타입으로 선언 (올바름)
    private var selectedImageUris = listOf<Uri>()

    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    // 💡 ViewModel 초기화: 커스텀 Factory를 사용하여 의존성 주입
    // (로그에 보이던 $Proxy2 ClassCastException 오류를 해결하는 올바른 방식)
    private val viewModel: SignUpViewModel by viewModels {
        SignUpViewModelFactory(application, RetrofitClient.userService)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ... (기존 코드와 동일) ...
        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivityResultLaunchers() // 이미지 런처 초기화

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
        // ... (기존 코드와 동일) ...
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
            // 💡 '여러' 이미지를 가져오는 계약(Contract) 사용
            ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri>? -> // 💡 반환 타입이 'List<Uri>'임
            uris?.let {
                if (it.isNotEmpty()) {
                    // 💡 selectedImageUris 변수에 'List' 자체를 저장 (올바름)
                    selectedImageUris = it
                    binding.imgProfile.setImageURI(it[0]) // 첫 번째 이미지를 프로필로 표시
                    Toast.makeText(this, "이미지 ${uris.size}장이 선택되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    selectedImageUris = emptyList()
                }
            }
        }

        permissionLauncher = registerForActivityResult(
            // ... (기존 코드와 동일) ...
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
        // ... (기존 코드와 동일) ...
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
        // ... (기존 코드와 동일) ...
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
        // ... (기존 코드와 동일: 유효성 검사 등) ...
        val name = binding.inputName.text.toString().trim()
        val loginId = binding.inputId.text.toString().trim()
        val password = binding.inputPassword.text.toString().trim()
        val passwordConfirm = binding.inputPasswordConfirm.text.toString().trim()
        val birthdayStr = binding.birthdayInput.text.toString().trim()
        val gender: String? = when (binding.radioGroupGender.checkedRadioButtonId) {
            binding.radioMale.id -> "M"
            binding.radioFemale.id -> "F"
            else -> null
        }

        // ... (유효성 검사 로직) ...
        if (name.isEmpty() || loginId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || birthdayStr.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (gender == null) {
            Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            Toast.makeText(this, "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
            return
        }
        val profileUri: Uri? = selectedImageUris.firstOrNull()

        val request = UserRegistrationRequest(
            loginId = loginId,
            password = password,
            name = name,
            gender = gender,
            birthday = birthdayStr,
            caregiverId = null
        )

        // 💡 ViewModel으로 '단일 Uri' (또는 null)를 전달합니다. (올바름)
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
                        finish()
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

// 💡 [핵심 수정 완료된 부분]
// (로그에 보이던 $Proxy2 ClassCastException 및 NoSuchMethodException 오류를 해결하는 올바른 ViewModel Factory 구현)
class SignUpViewModelFactory(
    private val application: Application,
    private val userService: UserService // 💡 Retrofit Service를 직접 받음
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SignUpViewModel::class.java)) {
            val gson = Gson()
            // 💡 Repository 생성 시 필요한 userService를 정확히 주입
            val userRepository = UserRepositoryImpl(
                userService,
                application.applicationContext,
                gson
            )
            // 💡 ViewModel 생성 시 Repository와 Application을 주입
            return SignUpViewModel(userRepository, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}