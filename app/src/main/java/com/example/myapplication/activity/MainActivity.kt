package com.example.myapplication.activity

import android.Manifest
import android.app.Activity
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
import androidx.compose.runtime.remember
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
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.util.FallDetectionService
import com.example.myapplication.data_state.LoginState
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.R
import com.example.myapplication.util.SharedPrefsManager
import com.example.myapplication.activity.SignUpActivity
import com.example.myapplication.activity.MainPageActivity
import com.example.myapplication.activity.LoginActivity
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
import com.example.myapplication.workers.DailyBloodPressureWorker
import java.time.Duration
import java.time.LocalTime
import com.example.myapplication.util.PREFS_NAME
import com.example.myapplication.util.KEY_NAME
import com.example.myapplication.util.KEY_BIRTHDAY
import com.example.myapplication.util.KEY_AUTO_LOGIN
import com.example.myapplication.util.KEY_PROFILE_IMAGE_URL
import java.util.Calendar


class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var requestFallPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var sharedPrefsManager: SharedPrefsManager

    companion object {
        var isAutoLoginCheckedState: Boolean = false
        fun startLogout(context: Context) {
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            with(sharedPreferences.edit()) { clear(); apply() }
            Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()

            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
            (context as? Activity)?.finish()
        }
    }

    // Health Connect 권한 목록
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

    // 낙상 감지 서비스 필수 위험 권한 목록
    private val FALL_DETECTION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.POST_NOTIFICATIONS,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Manifest.permission.FOREGROUND_SERVICE_LOCATION else ""
    ).filter { it.isNotEmpty() }.toTypedArray()


    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        RetrofitClient.initialize(applicationContext)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        healthConnectManager = HealthConnectManager(this)
        sharedPrefsManager = SharedPrefsManager(this) // SharedPrefsManager 초기화

        // 권한 요청 런처 정의 (HC 권한 요청 후 호출)
        requestPermissionLauncher = registerForActivityResult(
            healthConnectManager.requestPermissionsActivityContract()
        ) { granted ->
            if (granted.containsAll(HC_PERMISSIONS)) {
                Toast.makeText(this, "Health Connect 권한 획득 성공", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Health Connect 권한 부족", Toast.LENGTH_LONG).show()
            }
            requestFallDetectionPermissions() // HC 권한 획득 후, 낙상 감지 권한 요청으로 연결
        }

        // 낙상 감지 서비스 권한 요청 런처 정의 (최종 권한 요청 후 호출)
        requestFallPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Toast.makeText(this, "위치/SMS 권한 획득 완료", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "구조 요청 기능이 제한됩니다.", Toast.LENGTH_LONG).show()
            }
            // 모든 권한 체크 완료 후 다음 단계 (로그인 확인 또는 UI 설정)로 이동
            handlePostPermissionCheck()
        }

        // 앱 시작 시 권한 체크를 먼저 시작
        checkHealthConnectAndRequestPermissions()
    }

    // 권한 체크 완료 후 다음 단계를 결정하는 함수 (로그인 분기)
    private fun handlePostPermissionCheck() {
        if (isAutoLoggedIn(this)) {
            // 자동 로그인 성공 시, 서비스 시작 후 메인 페이지로 이동
            startFallDetectionService()
            val intent = Intent(this, MainPageActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // 자동 로그인 실패 시, 로그인 UI를 설정
            setupContent()
        }
    }

    // 서비스 및 워커 로직

    private fun startFallDetectionService() {
        schedulePeriodicSync()
        scheduleDailyBloodPressureSync()

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
            repeatInterval = 10,
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "HealthSyncTag",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun scheduleDailyBloodPressureSync() {
        val DAILY_BP_WORKER_TAG = "DailyBloodPressureSync"
        val targetHour = 23
        val targetMinute = 50

        val currentTime = LocalTime.now()
        val targetTime = LocalTime.of(targetHour, targetMinute)

        var delay = Duration.between(currentTime, targetTime)
        if (delay.isNegative) {
            delay = delay.plusDays(1)
        }

        val syncRequest = PeriodicWorkRequestBuilder<DailyBloodPressureWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = TimeUnit.DAYS
        )
            .setInitialDelay(delay.toMinutes(), TimeUnit.MINUTES)
            .addTag(DAILY_BP_WORKER_TAG)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            DAILY_BP_WORKER_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        Log.d("WORKER_SCHEDULE", "Daily BP Worker 스케줄링 완료. 초기 지연 시간: ${delay.toHours()}시간 ${delay.toMinutes() % 60}분")
    }

    // 권한 요청 흐름

    private fun requestFallDetectionPermissions() {
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        val needsRequest = !(hasFineLocation && hasSendSms)

        if (!needsRequest) {
            handlePostPermissionCheck()
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

    /**
     * 앱의 기본 UI (로그인 화면)를 설정하는 함수
     */
    private fun setupContent() {
        setContent {
            MyApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val viewModel: LoginViewModel = viewModel()
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding),
                        viewModel = viewModel
                    )

                    // LoginObserver에 서비스 시작 및 페이지 전환 로직 전달
                    LoginObserver(
                        viewModel = viewModel,
                        sharedPrefsManager = sharedPrefsManager,
                        onLoginSuccess = { context ->
                            startFallDetectionService()
                            val intent = Intent(context, MainPageActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            context.startActivity(intent)
                            (context as? ComponentActivity)?.finish()
                        }
                    )
                }
            }
        }
    }
}

//  독립 함수 및 Composable (로그인 및 UI)

fun isAutoLoggedIn(context: Context): Boolean {
    // SharedPrefsManager를 사용하여 자동 로그인 상태 확인으로 업데이트 필요
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val isChecked = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false)
    val userNameSaved = sharedPreferences.getString(KEY_NAME, null)

    return isChecked && userNameSaved != null
}

fun saveLoginInfo(context: Context, name: String, birthday: String, autoLogin: Boolean) {
    // 자동 로그인 설정 저장을 위해 유지
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    with(sharedPreferences.edit()) {
        putString(KEY_NAME, name)
        putString(KEY_BIRTHDAY, birthday)
        putBoolean(KEY_AUTO_LOGIN, autoLogin)
        apply()
    }
}

@Composable
fun LoginObserver(viewModel: LoginViewModel, sharedPrefsManager: SharedPrefsManager, onLoginSuccess: (Context) -> Unit) {
    val loginState = viewModel.loginState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(loginState.value.isLoggedIn) {
        when (val state = loginState.value) {
            is LoginState.Success -> {
                val response = state.loginResponse
                val actualSilverId = response?.loginId ?: "UNKNOWN_ID"
                val name = response?.name ?: "환자"
                val token = response?.accessToken ?: ""
                val birthYearString = response?.birthday
                var ageString: String
                if (!birthYearString.isNullOrEmpty() && birthYearString != "정보 없음") {
                    try {
                        // 1. 현재 년도 가져오기 (호환성을 위해 Calendar 사용)
                        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                        // 2. 생년월일(String)을 숫자(Int)로 변환
                        val birthYear = birthYearString.toInt()
                        // 3. 나이 계산
                        val age = currentYear - birthYear
                        ageString = "만 $age" // 결과: "26"

                    } catch (e: NumberFormatException) {
                        Log.e("AgeCalcError", "Birth year format error: $birthYearString")
                        ageString = "정보 없음" // "1999" 형식이 아닌 경우
                    }
                } else {
                    ageString = "정보 없음" // "정보 없음" 또는 null인 경우
                }


                val autoLoginState = MainActivity.isAutoLoginCheckedState
                val profileImageUrl = response?.images?.firstOrNull()

                Log.d("IMAGE_DEBUG", "서버 반환 이미지 URL 조각: $profileImageUrl")

                // 세션 및 자동 로그인 정보 저장 (토큰 포함)
                sharedPrefsManager.saveUserSession(actualSilverId, name, ageString, token)
                saveLoginInfo(context, name, ageString, autoLoginState) // 기존 자동 로그인 설정 저장

                if (!profileImageUrl.isNullOrEmpty()) {
                    sharedPrefsManager.saveString(KEY_PROFILE_IMAGE_URL, profileImageUrl)
                }



                // 로그인 성공 시 서비스 시작 및 페이지 전환 로직 호출
                onLoginSuccess(context)
            }
            is LoginState.Error -> {
                Toast.makeText(context, "로그인 실패: ${state.errorMessage}", Toast.LENGTH_LONG).show()
            }
            else -> Unit
        }
    }
}

@Composable
fun LoginScreen(modifier: Modifier = Modifier, viewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current

    AndroidView(modifier = modifier.fillMaxSize(),
        factory = {
            val view = LayoutInflater.from(it).inflate(R.layout.login, null, false)
            val loginIdInput = view.findViewById<TextInputEditText>(R.id.input_id)
            val passwordInput = view.findViewById<TextInputEditText>(R.id.input_password)
            val autoLoginCheckBox = view.findViewById<CheckBox>(R.id.check_auto_login)
            val loginButton = view.findViewById<MaterialButton>(R.id.btn_login)
            val signUpButton = view.findViewById<MaterialButton>(R.id.btn_signup)

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