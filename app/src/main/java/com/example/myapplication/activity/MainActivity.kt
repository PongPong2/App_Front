package com.example.myapplication.activity

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
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.FallDetectionService
import com.example.myapplication.KEY_AUTO_LOGIN
import com.example.myapplication.KEY_GENDER
import com.example.myapplication.KEY_NAME
import com.example.myapplication.activity.MainPageActivity
import com.example.myapplication.PREFS_NAME
import com.example.myapplication.R
import com.example.myapplication.activity.SignUpActivity
import com.example.myapplication.data_state.LoginState
import com.example.myapplication.viewmodel.LoginViewModel
import com.example.myapplication.util.BirthDateTextWatcher


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

    AndroidView(modifier = modifier.fillMaxSize(),
        factory = {
            val view = LayoutInflater.from(it).inflate(R.layout.login, null, false)
            // factory 상단에서 모든 뷰를 한 번만 찾습니다.
            val loginIdInput = view.findViewById<TextInputEditText>(R.id.input_id)
            val passwordInput = view.findViewById<TextInputEditText>(R.id.input_password)
            val birthDateInput = view.findViewById<TextInputEditText>(R.id.input_birth_date)
            val autoLoginCheckBox = view.findViewById<CheckBox>(R.id.check_auto_login)
            val loginButton = view.findViewById<MaterialButton>(R.id.btn_login)
            val signUpButton = view.findViewById<MaterialButton>(R.id.btn_signup)
            birthDateInput?.addTextChangedListener(BirthDateTextWatcher(birthDateInput))

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
            // factory에서 모든 리스너가 설정되었으므로, update 블록은 비워둡니다.
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