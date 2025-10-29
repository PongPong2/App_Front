package com.example.myapplication

import android.Manifest
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

// SharedPreferences 상수
const val PREFS_NAME = "LOGIN_PREFS"
const val KEY_NAME = "user_name"
const val KEY_GENDER = "user_gender"
const val KEY_AUTO_LOGIN = "auto_login"

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>
    private lateinit var requestFallPermissionsLauncher: ActivityResultLauncher<Array<String>>

    companion object {
        var isAutoLoginCheckedState: Boolean = false
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
        Manifest.permission.POST_NOTIFICATIONS
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        // 자동 로그인 체크 (로그인 로직)
        if (isAutoLoggedIn(this)) {
            val intent = Intent(this, MainPageActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        healthConnectManager = HealthConnectManager(this)

        // Health Connect 권한 요청 런처 정의
        requestPermissionLauncher = registerForActivityResult(
            healthConnectManager.requestPermissionsActivityContract()
        ) { granted ->
            if (granted.containsAll(HC_PERMISSIONS)) {
                Toast.makeText(this, "Health Connect 권한 획득 성공", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Health Connect 권한 부족", Toast.LENGTH_LONG).show()
            }
            requestFallDetectionPermissions() // HC 권한 처리 후, 낙상 감지 권한 요청으로 연결
        }

        // 낙상 감지 서비스 권한 요청 런처 정의
        requestFallPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Toast.makeText(this, "위치/SMS 권한 획득 완료", Toast.LENGTH_SHORT).show()
                startFallDetectionService() // 권한 성공 시 서비스 시작
                setupContent() // 서비스 시작 후 UI 설정 (Login)
            } else {
                Toast.makeText(this, "구조 요청 기능이 제한됩니다.", Toast.LENGTH_LONG).show()
                setupContent() // 서비스 시작 실패 시에도 UI는 표시
            }
        }

        // 로직 시작: Health Connect 가용성 확인 및 권한 요청으로 시작
        checkHealthConnectAndRequestPermissions()
    }

    private fun startFallDetectionService() {
        // Health Connect Worker 예약 (15분 주기)
        schedulePeriodicSync()

        // Fall Detection Service 시작 (포그라운드)
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
            repeatInterval = 15, // 15분으로 설정 (10분 주기를 위해선 10분으로 조정 필요)
            repeatIntervalTimeUnit = TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "HealthSyncTag",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    private fun requestFallDetectionPermissions() {
        // 필수 권한이 모두 있는지 확인
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        val needsRequest = !(hasFineLocation && hasSendSms)

        if (!needsRequest) {
            startFallDetectionService()
            setupContent() // 모든 권한이 있다면 여기서 UI 설정 시작
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
                        requestFallDetectionPermissions() // HC 권한이 있다면 바로 낙상 감지 권한 요청으로 연결
                    }
                }
            }
            HealthConnectAvailability.NOT_INSTALLED, HealthConnectAvailability.NOT_SUPPORTED -> {
                if (availability == HealthConnectAvailability.NOT_INSTALLED) {
                    Toast.makeText(this, "Health Connect 설치/업데이트 필요.", Toast.LENGTH_LONG).show()
                }
                requestFallDetectionPermissions() // Health Connect 사용 불가 시에도 낙상 감지 권한 요청은 실행
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
                    // [통합] View Model 기반 LoginScreen과 Observer 호출
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

//  독립 함수 및 Composable (로그인 및 UI)

fun isAutoLoggedIn(context: Context): Boolean {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val isChecked = sharedPreferences.getBoolean(KEY_AUTO_LOGIN, false)
    val userNameSaved = sharedPreferences.getString(KEY_NAME, null)

    return isChecked && userNameSaved != null
}

fun saveLoginInfo(context: Context, name: String, gender: String, autoLogin: Boolean) {
    val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    with(sharedPreferences.edit()) {
        putString(KEY_NAME, name)
        putString(KEY_GENDER, gender)
        putBoolean(KEY_AUTO_LOGIN, autoLogin)
        apply()
    }
}

@Composable
fun LoginObserver(viewModel: LoginViewModel) {
    val loginState = viewModel.loginState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(loginState.value.isLoggedIn) {
        when (val state = loginState.value) {
            is LoginState.Success -> {
                val name = "김환자"
                val gender = "남"

                val autoLoginState = MainActivity.isAutoLoginCheckedState
                saveLoginInfo(context, name, gender, autoLoginState)

                val intent = Intent(context, MainPageActivity::class.java)
                context.startActivity(intent)

                val activity = context as? ComponentActivity
                activity?.finish()
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

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = {
            val view = LayoutInflater.from(context).inflate(R.layout.login, null, false)

            val signUpButton = view.findViewById<MaterialButton>(R.id.btn_signup)
            signUpButton?.setOnClickListener {
                val intent = Intent(context, SignUpActivity::class.java)
                context.startActivity(intent)
            }

            val loginButton = view.findViewById<MaterialButton>(R.id.btn_login)
            loginButton?.setOnClickListener {
                val loginId = view.findViewById<TextInputEditText>(R.id.input_id)?.text?.toString() ?: ""
                val password = view.findViewById<TextInputEditText>(R.id.input_password)?.text?.toString() ?: ""

                val autoLoginCheckBox = view.findViewById<CheckBox>(R.id.check_auto_login)
                val isChecked = autoLoginCheckBox?.isChecked ?: false

                MainActivity.isAutoLoginCheckedState = isChecked

                if (loginId.isEmpty() || password.isEmpty()) {
                    Toast.makeText(context, "아이디와 비밀번호를 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                } else {
                    // ViewModel 로그인 호출
                    viewModel.login(loginId, password)
                }
            }
            view
        },
        update = { view ->
        }
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    MyApplicationTheme {
        // LoginScreen(viewModel = /* Preview ViewModel */)
        LoginScreen()
    }
}