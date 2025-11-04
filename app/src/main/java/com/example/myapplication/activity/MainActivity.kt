package com.example.myapplication.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.data.HealthConnectAvailability
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.workers.HealthSyncWorker
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.FallDetectionService
import com.example.myapplication.KEY_AUTO_LOGIN
import com.example.myapplication.KEY_NAME
import com.example.myapplication.PREFS_NAME
import com.example.myapplication.KEY_PROFILE_IMAGE_URL
import com.example.myapplication.R
import com.example.myapplication.data_state.LoginState
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.util.BirthDayTextWatcher
import coil.load

// 💡 [추가] SharedPreferences 키 정의 (메인 페이지 정보 표시에 사용)
const val KEY_GENDER = "user_gender"
const val KEY_BIRTHDAY = "user_birthday"
// KEY_PROFILE_IMAGE_URL, KEY_NAME 등은 이미 import 되어 있습니다.


class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var requestFallPermissionsLauncher: ActivityResultLauncher<Array<String>>

    companion object {
        var isAutoLoginCheckedState: Boolean = false
        fun startLogout(context: Context) {
            performLogout(context)
        }

        private fun performLogout(context: Context) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                clear()
                apply()
            }

            Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)

            val activity = context as? AppCompatActivity
            activity?.finish()
        }
    }

    private val HC_PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
        HealthPermission.getWritePermission(BloodGlucoseRecord::class),
        HealthPermission.getWritePermission(BloodPressureRecord::class),
        HealthPermission.getWritePermission(BodyTemperatureRecord::class),
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND"
    )

    private val FALL_DETECTION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.POST_NOTIFICATIONS
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        if (isAutoLoggedIn(this)) {
            val intent = Intent(this, MainPageActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        healthConnectManager = HealthConnectManager(this)

        // ... (권한 요청 런처 및 체크 로직 생략)
        requestPermissionLauncher = registerForActivityResult(
            healthConnectManager.requestPermissionsActivityContract()
        ) { granted ->
            if (granted.containsAll(HC_PERMISSIONS)) {
                Toast.makeText(this, "Health Connect 권한 획득 성공", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Health Connect 권한 부족", Toast.LENGTH_LONG).show()
            }
            requestFallDetectionPermissions()
        }

        requestFallPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Toast.makeText(this, "위치/SMS 권한 획득 완료", Toast.LENGTH_SHORT).show()
                startFallDetectionService()
                setupContent()
            } else {
                Toast.makeText(this, "구조 요청 기능이 제한됩니다.", Toast.LENGTH_LONG).show()
                setupContent()
            }
        }

        checkHealthConnectAndRequestPermissions()
    }

    private fun startFallDetectionService() {
        schedulePeriodicSync()

        val serviceIntent = Intent(this, FallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.d("SERVICE_START", "FallDetectionService 시작됨")
    }

    private fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<HealthSyncWorker>(
            repeatInterval = 15,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "HealthSyncTag",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun requestFallDetectionPermissions() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        val needsRequest = !(hasFineLocation && hasSendSms)

        if (!needsRequest) {
            startFallDetectionService()
            setupContent()
        } else {
            requestFallPermissionsLauncher.launch(FALL_DETECTION_PERMISSIONS)
        }
    }

    private fun checkHealthConnectAndRequestPermissions() {
        healthConnectManager.checkAvailability()
        val availability = healthConnectManager.availability.value

        when (availability) {
            HealthConnectAvailability.INSTALLED -> {
                CoroutineScope(Dispatchers.Main).launch {
                    if (!healthConnectManager.hasAllPermissions(HC_PERMISSIONS)) {
                        requestPermissionLauncher.launch(HC_PERMISSIONS)
                    } else {
                        requestFallDetectionPermissions()
                    }
                }
            }
            HealthConnectAvailability.NOT_INSTALLED, HealthConnectAvailability.NOT_SUPPORTED -> {
                if (availability == HealthConnectAvailability.NOT_INSTALLED) {
                    Toast.makeText(this, "Health Connect 설치/업데이트 필요.", Toast.LENGTH_LONG).show()
                }
                requestFallDetectionPermissions()
            }
        }
    }

    private fun setupContent() {
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: LoginViewModel = viewModel()
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )

                    LoginObserver(viewModel = viewModel)
                }
            }
        }
    }
}

fun isAutoLoggedIn(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val isChecked = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false)
    val userNameSaved = sharedPreferences.getString(KEY_NAME, null)

    return isChecked && userNameSaved != null
}

@Composable
fun LoginObserver(viewModel: LoginViewModel) {
    val loginState = viewModel.loginState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(loginState.value.isLoggedIn) {
        when (val state = loginState.value) {
            is LoginState.Success -> {
                val response = state.loginResponse

                if (response != null) {
                    try {
                        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                        with(sharedPreferences.edit()) {
                            putBoolean(KEY_AUTO_LOGIN, MainActivity.isAutoLoginCheckedState)
                            putString(KEY_NAME, response.name)
                            putString(KEY_GENDER, response.gender?.toString())
                            putString(KEY_BIRTHDAY, response.birthday?.toString())

                            val profileImageUrl = response.images?.firstOrNull()
                            putString(KEY_PROFILE_IMAGE_URL, profileImageUrl)

                            apply() // 👈 로그인 정보 저장 완료
                        }

                        // 🚀 [화면 전환] 메인 페이지로 이동 시 스택 정리 플래그 사용
                        val intent = Intent(context, MainPageActivity::class.java)
                        // 💡 [핵심 수정] 새로운 태스크로 시작하고 기존 스택(MainActivity 포함)을 모두 클리어합니다.
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        Log.d("NAV_SUCCESS", "Starting MainPageActivity after successful login.")
                        context.startActivity(intent)

                        // 🚨 [필요없음] FLAG_ACTIVITY_CLEAR_TASK 사용 시 finish()는 불필요하지만,
                        // 안전을 위해 context가 Activity인 경우 호출하는 것은 무방합니다.
                        // 이 경우, `finish()` 대신 `FLAG_ACTIVITY_CLEAR_TASK`가 스택 정리를 보장합니다.
                        // val activity = context as? ComponentActivity
                        // activity?.finish()

                    } catch (e: Exception) {
                        Log.e("FATAL_NAV_ERROR", "화면 전환 실패 (저장 오류): ${e.message}", e)
                        Toast.makeText(context, "로그인 성공했으나 화면 전환 실패: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Log.e("FATAL_NAV_ERROR", "Login successful but response body is null.")
                    Toast.makeText(context, "로그인 성공했으나 응답 데이터가 없습니다.", Toast.LENGTH_LONG).show()
                }
            }
            is LoginState.Error -> {
                Toast.makeText(context, "로그인 실패: ${state.errorMessage}", Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }
}

@SuppressLint("MissingInflatedId")
@Composable
fun LoginScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current

    AndroidView(modifier = modifier.fillMaxSize(),
        factory = {
            val view = LayoutInflater.from(it).inflate(R.layout.login, null, false)
            val loginIdInput = view.findViewById<TextInputEditText>(R.id.input_id)
            val passwordInput = view.findViewById<TextInputEditText>(R.id.input_password)
            val birthdayInput = view.findViewById<TextInputEditText>(R.id.birthdayInput)
            val autoLoginCheckBox = view.findViewById<CheckBox>(R.id.check_auto_login)
            val loginButton = view.findViewById<MaterialButton>(R.id.btn_login)
            val signUpButton = view.findViewById<MaterialButton>(R.id.btn_signup)
            birthdayInput?.addTextChangedListener(BirthDayTextWatcher(birthdayInput))

            // 회원가입 버튼 리스너
            signUpButton?.setOnClickListener {
                val intent = Intent(context, SignUpActivity::class.java)
                context.startActivity(intent)
            }

            // 로그인 버튼 리스너
            loginButton?.setOnClickListener {
                val loginId = loginIdInput?.text?.toString() ?: ""
                val password = passwordInput?.text?.toString() ?: ""
                val isChecked = autoLoginCheckBox?.isChecked ?: false

                MainActivity.isAutoLoginCheckedState = isChecked

                if (loginId.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.login(loginId, password)
                }
            }
            view
        },
        update = {
        }
    )
}


@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyApplicationTheme {
        LoginScreen()
    }
}