package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.data.HealthConnectAvailability
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.android.material.button.MaterialButton

// WorkManager 및 시간 관련 임포트
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.WorkManager
import com.example.myapplication.workers.HealthSyncWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.util.Log
import com.kakao.sdk.common.util.Utility
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

class MainActivity : ComponentActivity() {

    private lateinit var healthConnectManager: HealthConnectManager
    // Health Connect 권한 요청 런처
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<Set<String>>
    // 낙상 감지 서비스 권한 요청 런처
    private lateinit var requestFallPermissionsLauncher: ActivityResultLauncher<Array<String>>

    // Health Connect 권한 목록 (앱에 맞춰 조절하세요)
    private val HC_PERMISSIONS = setOf(
        // READ 전용 권한 (삼성 헬스 등에서 가져올 데이터)
        HealthPermission.getReadPermission(StepsRecord::class),             // 걸음수
        HealthPermission.getReadPermission(HeartRateRecord::class),         // 심박수
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),  // 산소 포화도
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),// 소모 칼로리
        HealthPermission.getReadPermission(SleepSessionRecord::class),      // 수면

        // WRITE 전용 권한 (우리 앱에서 기록할 데이터)
        HealthPermission.getWritePermission(WeightRecord::class),           // 체중
        HealthPermission.getWritePermission(BloodGlucoseRecord::class),     // 혈당
        HealthPermission.getWritePermission(BloodPressureRecord::class),    // 혈압
        HealthPermission.getWritePermission(BodyTemperatureRecord::class), // 체온
        "android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND" // 백그라운드 권한 부여하려면 필요한거
    )

    // 낙상 감지 서비스 필수 위험 권한 목록
    private val FALL_DETECTION_PERMISSIONS = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.SEND_SMS,
        Manifest.permission.POST_NOTIFICATIONS // Android 13+ 알림용
    )

    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Key Hash 확인 코드 (확인 후 반드시 주석 처리하거나 삭제하기)


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
            // Health Connect 권한 처리 후, 낙상 감지 권한 요청으로 연결
            requestFallDetectionPermissions()
        }

        // 낙상 감지 서비스 권한 요청 런처 정의
        requestFallPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                Toast.makeText(this, "위치/SMS 권한 획득 완료", Toast.LENGTH_SHORT).show()
                startFallDetectionService() // 권한 성공 시 서비스 시작
            } else {
                Toast.makeText(this, "구조 요청 기능이 제한됩니다.", Toast.LENGTH_LONG).show()
                setupContent() // 서비스 시작 실패 시에도 UI는 표시
            }
        }

        // 로직 시작: Health Connect 가용성 확인 및 권한 요청으로 시작
        checkHealthConnectAndRequestPermissions()
    }

    private fun startFallDetectionService() {
        // Health Connect Worker 예약
        schedulePeriodicSync()

        // Fall Detection Service 시작 (포그라운드)
        val serviceIntent = Intent(this, FallDetectionService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent)
        } else {
            startService(serviceIntent)
        }
        Log.d("SERVICE_START", "FallDetectionService 시작됨")
        setupContent() // 서비스 시작 후 UI 설정
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
        // 필수 권한이 모두 있는지 확인
        val hasFineLocation = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasSendSms = ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED

        val needsRequest = !(hasFineLocation && hasSendSms)

        if (!needsRequest) {
            startFallDetectionService()
        } else {
            // 권한이 부족하다면 런처를 통해 요청
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
                        // HC 권한이 있다면 바로 낙상 감지 권한 요청으로 연결
                        requestFallDetectionPermissions()
                    }
                }
            }
            HealthConnectAvailability.NOT_INSTALLED, HealthConnectAvailability.NOT_SUPPORTED -> {
                if (availability == HealthConnectAvailability.NOT_INSTALLED) {
                    // Play 스토어로 이동 안내 로직 (선택적)
                    Toast.makeText(this, "Health Connect 설치/업데이트 필요.", Toast.LENGTH_LONG).show()
                }
                // Health Connect 사용 불가 시에도 낙상 감지 권한 요청은 실행
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
                    LoginScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

// 기존 LoginScreen Composable 함수는 그대로 유지
@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val view = LayoutInflater.from(context).inflate(R.layout.login, null, false)
            val signUpButton = view.findViewById<MaterialButton>(R.id.btn_signup)
            signUpButton?.setOnClickListener {
                val intent = Intent(context, SignUpActivity::class.java)
                context.startActivity(intent)
            }
            val loginButton = view.findViewById<MaterialButton>(R.id.btn_login)
            loginButton?.setOnClickListener {
                val intent = Intent(context, MainPageActivity::class.java)
                context.startActivity(intent)
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
        LoginScreen()
    }
}