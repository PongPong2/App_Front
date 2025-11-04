package com.example.myapplication.activity

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.API.RetrofitClient
import com.example.myapplication.API.UserService
import com.example.myapplication.data_model.UserRegistrationRequest
import com.example.myapplication.data_state.RegistrationState
import com.example.myapplication.databinding.SignupBinding
import com.example.myapplication.util.BirthDayTextWatcher
import com.example.myapplication.viewmodel.SignUpViewModel
import com.example.myapplication.repository.UserRepositoryImpl
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import androidx.appcompat.app.AlertDialog
// java.time.ZoneId, java.util.Date 는 더 이상 필요 없으나, import는 그대로 유지했습니다.
import java.time.ZoneId
import java.util.Date

class SignUpActivity : AppCompatActivity() {

    private lateinit var binding: SignupBinding
    // 💡 선택된 이미지 목록 (List<Uri>)
    private var selectedImageUris = listOf<Uri>()
    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var permissionLauncher: ActivityResultLauncher<String>


    // 💡 ViewModel 초기화: 커스텀 Factory를 사용하여 의존성 주입
    private val viewModel: SignUpViewModel by viewModels {
        SignUpViewModelFactory(application, RetrofitClient.userService) // RetrofitClient.userService를 팩토리에 직접 전달
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = SignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initActivityResultLaunchers()

        binding.birthdayInput.addTextChangedListener(BirthDayTextWatcher(binding.birthdayInput))

        binding.cardProfileImage.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.btnSignup.setOnClickListener {
            attemptRegistration()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }

        observeRegistrationState()
    }

    private fun initActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(
            ActivityResultContracts.GetMultipleContents()
        ) { uris: List<Uri>? ->
            uris?.let {
                // 💡 [선택된 이미지 처리] 이미지가 있다면 첫 번째 이미지를 프로필로 표시
                if (it.isNotEmpty()) {
                    selectedImageUris = it
                    binding.imgProfile.setImageURI(it[0])
                    Toast.makeText(this, "이미지 ${uris.size}장이 선택되었습니다.", Toast.LENGTH_SHORT).show()
                } else {
                    // 💡 선택 취소 시 리스트를 비움 (이미지 미선택 허용)
                    selectedImageUris = emptyList()
                }
            }
        }

        // 💡 런타임 권한 요청 결과 처리
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
        // 💡 OS 버전에 따라 적절한 미디어 권한을 사용
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        }
        // 💡 권한 거부 시, 필요한 이유를 설명하는 다이얼로그 표시
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

        // 💡 텍스트 입력 필드의 내용을 올바르게 가져옵니다.
        val birthdayStr = binding.birthdayInput.text.toString().trim()

        // Char 타입으로 변환
        val gender: Char? = when (binding.radioGroupGender.checkedRadioButtonId) {
            binding.radioMale.id -> 'M'
            binding.radioFemale.id -> 'F'
            else -> null
        }

        if (name.isEmpty() || loginId.isEmpty() || password.isEmpty() || passwordConfirm.isEmpty() || birthdayStr.isEmpty()) {
            Toast.makeText(this, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 💡 [수정] localDateBirthday 변수 선언은 유효성 검사 목적으로만 사용합니다.
        try {
            // "YYYY-MM-DD" 형식이 올바른지 확인
            LocalDate.parse(birthdayStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: DateTimeParseException) {
            Toast.makeText(this, "생년월일 형식이 올바르지 않습니다. (YYYY-MM-DD)", Toast.LENGTH_SHORT).show()
            return
        }

        // 💡 [제거] java.util.Date로 변환하는 불필요한 로직이 제거되었습니다.

        if (password != passwordConfirm) {
            Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        if (gender == null) {
            Toast.makeText(this, "성별을 선택해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 💡 [안전성 확보] 선택된 이미지 리스트에서 첫 번째 URI를 안전하게 추출 (null 허용)
        val profileUri: Uri? = selectedImageUris.firstOrNull()


        val request = UserRegistrationRequest(
            loginId = loginId,
            password = password,
            name = name,
            gender = gender,
            // 💡 [수정] String 타입인 DTO 필드에 검증된 문자열을 직접 전달합니다.
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

                        // 💡 회원가입 성공 후 로그인 화면으로 이동
                        val intent = Intent(this@SignUpActivity, LoginActivity::class.java)
                        // 현재 Activity를 스택에서 제거하고 새 Activity 시작
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

// -----------------------------------------------------------------------------
// ## ⚙️ ViewModel Factory
// -----------------------------------------------------------------------------

/**
 * SignUpViewModel에 필요한 의존성(UserRepository, Application)을 수동으로 주입하기 위한 팩토리입니다.
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