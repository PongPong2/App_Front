package com.example.myapplication.util

import android.Manifest
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.domain.HealthSyncLogic // 💡 HealthSyncLogic 임포트
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit // 💡 TimeUnit 임포트
import kotlin.math.sqrt

class FallDetectionService : Service(), SensorEventListener {

    private val TAG = "FallDetectionService"

    // 센서 및 위치 클라이언트
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Health Connect 및 코루틴 설정
    private lateinit var healthConnectManager: HealthConnectManager
    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob) // 서비스 범위 코루틴

    // 보호자 전화번호 (사용자 입력값)
    private val GUARDIAN_PHONE_NUMBER = BuildConfig.PHONE_NUMBER

    private var lastSensorUpdateTime: Long = 0
    private var isFalling: Boolean = false
    private var fallConfirmed: Boolean = false
    private var fallStartTime: Long = 0

    // 💡 Health Sync 관련 필드 추가
    private lateinit var healthSyncLogic: HealthSyncLogic
    private val SYNC_INTERVAL_MS = TimeUnit.MINUTES.toMillis(10) // 10분 주기

    private lateinit var handler: Handler // 기존 Handler 필드를 재사용

    private val CHANNEL_ID_SERVICE = "FallDetectionServiceChannel"
    private val NOTIFICATION_ID_SERVICE = 1
    private val CHANNEL_ID_ALERT = "FallAlertChannel"
    private val NOTIFICATION_ID_ALERT = 2

    // 수정된 임계치: 민감도 조정
    private val IMPACT_THRESHOLD = 35.0f
    private val STILLNESS_THRESHOLD = 11.0f
    private val STILLNESS_TIME_MS = 2000L
    private val FALL_CONFIRMATION_DELAY_MS = 10000L

    // 심박수/SpO2 위험 임계치 (테스트용)
    private val HR_CRITICAL_HIGH = 120.0
    private val HR_CRITICAL_LOW = 40.0
    private val SPO2_CRITICAL_LOW = 90.0
    private val MONITORING_INTERVAL_MS = 60000L // 1분 주기 모니터링

    private val SENT_ACTION = "SMS_SENT_STATUS"

    companion object {
        const val ACTION_CANCEL_FALL = "com.example.myapplication.CANCEL_FALL"
    }

    // 💡 10분 주기 Health Sync 타이머 Runnable 추가
    private val syncRunnable = object : Runnable {
        override fun run() {
            // ServiceScope (IO Dispatcher) 내에서 Health Sync 로직 실행
            serviceScope.launch(Dispatchers.IO) {
                Log.d(TAG, "10분 주기 Health Sync 시작 (Foreground Timer)")
                healthSyncLogic.performSync()
            }
            // 10분 후에 다시 실행되도록 스케줄링
            handler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    private val fallAlertRunnable = Runnable {
        if (isFalling && fallConfirmed) {
            Log.e(TAG, "10 seconds elapsed. Final Fall Alert triggered.")
            getLocationAndSendAlert()
            resetFallState()
        }
    }

    // SMS 전송 결과를 처리할 BroadcastReceiver 정의
    private val smsSentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (resultCode) {
                Activity.RESULT_OK -> Log.i(TAG, "SMS 최종 전송 성공: 통신사 발신 완료")
                SmsManager.RESULT_ERROR_GENERIC_FAILURE -> Log.e(TAG, "SMS 최종 전송 실패: 일반적인 오류 (GENERIC_FAILURE)")
                SmsManager.RESULT_ERROR_NO_SERVICE -> Log.e(TAG, "SMS 최종 전송 실패: 서비스 지역 아님 (NO_SERVICE)")
                SmsManager.RESULT_ERROR_NULL_PDU -> Log.e(TAG, "SMS 최종 전송 실패: PDU 오류 (NULL_PDU)")
                SmsManager.RESULT_ERROR_RADIO_OFF -> Log.e(TAG, "SMS 최종 전송 실패: 라디오 꺼짐 (RADIO_OFF)")
                else -> Log.e(TAG, "SMS 최종 전송 실패: 기타 오류 (Code: $resultCode)")
            }
        }
    }


    //  Service Lifecycle

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service Created")

        // HealthConnectManager 및 HealthSyncLogic 초기화
        healthConnectManager = HealthConnectManager(applicationContext)
        healthSyncLogic = HealthSyncLogic(applicationContext) // 💡 HealthSyncLogic 초기화

        handler = Handler(Looper.getMainLooper())
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        // SMS 리시버 등록
        val receiverFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.RECEIVER_NOT_EXPORTED
        } else {
            0
        }

        registerReceiver(smsSentReceiver, IntentFilter(SENT_ACTION), receiverFlags)

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            Log.d(TAG, "Accelerometer Listener Registered")
        } else {
            Log.e(TAG, "Accelerometer not found on device.")
        }

        // 1분 주기 심박수 모니터링 시작
        startHeartRateMonitoring()

        // 💡 10분 주기 Health Sync 타이머 시작
        handler.post(syncRunnable)
        Log.d(TAG, "10분 주기 Health Sync 타이머 시작됨.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL_FALL) {
            cancelFallDetection()
            return START_STICKY
        }
        startForeground(NOTIFICATION_ID_SERVICE, createServiceNotification())
        Log.d(TAG, "Service started in foreground")
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()

        // 💡 10분 주기 Health Sync 타이머 중단
        handler.removeCallbacks(syncRunnable)

        handler.removeCallbacks(fallAlertRunnable)
        sensorManager.unregisterListener(this)

        // 코루틴 작업 취소 및 리소스 해제
        serviceScope.cancel()

        // 서비스 종료 시 리시버 해제
        try {
            unregisterReceiver(smsSentReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "SMS receiver already unregistered.")
        }

        Log.d(TAG, "Service Destroyed")
    }

    //  Heart Rate / SpO2 Monitoring Logic
    // ... (startHeartRateMonitoring 함수는 수정 없이 유지) ...
    /** 1분마다 심박수와 SpO2를 체크하여 위험 임계치를 벗어나는지 확인합니다. */
    private fun startHeartRateMonitoring() {
        serviceScope.launch {
            while (true) {
                // 심박수 및 SpO2 조회 (최근 1분 데이터)
                val avgBpm = healthConnectManager.readLatestHeartRateAvg(1)
                val avgSpO2 = healthConnectManager.getFakeOxygenSaturation() // 가짜 데이터 사용

                Log.d(TAG, "HR Monitor: BPM=$avgBpm, SpO2=$avgSpO2")

                // 심박수 위험 임계치 체크
                if (avgBpm > HR_CRITICAL_HIGH || (avgBpm > 0.0 && avgBpm < HR_CRITICAL_LOW)) {
                    val message = if (avgBpm > HR_CRITICAL_HIGH) " 심박수 급격한 상승 감지: ${"%.1f".format(avgBpm)} BPM"
                    else "심박수 급격한 하락 감지: ${"%.1f".format(avgBpm)} BPM"
                    Log.e(TAG, message)
                    getLocationAndSendAlert(isImmediate = true, customMessage = message)
                }

                // SpO2 위험 임계치 체크
                if (avgSpO2 > 0.0 && avgSpO2 < SPO2_CRITICAL_LOW) {
                    val message = "산소포화도 임계치 이하 감지: ${"%.1f".format(avgSpO2)}%"
                    Log.e(TAG, message)
                    getLocationAndSendAlert(isImmediate = true, customMessage = message)
                }

                delay(MONITORING_INTERVAL_MS) // 1분 대기
            }
        }
    }


    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val currentTime = System.currentTimeMillis()

            if ((currentTime - lastSensorUpdateTime) < 50 ) {
                return
            }
            lastSensorUpdateTime = currentTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt(x * x + y * y + z * z)

            detectFall(magnitude, currentTime)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // 사용하지 않음
    }

    private fun detectFall(magnitude: Float, currentTime: Long) {
        if (!isFalling) {
            if (magnitude > IMPACT_THRESHOLD) {
                Log.w(TAG, "Step 1: High Impact Detected! Mag: $magnitude")
                isFalling = true
                fallStartTime = currentTime
                return
            }
        } else {
            val elapsedTime = currentTime - fallStartTime

            if (!fallConfirmed) {
                if (elapsedTime >= STILLNESS_TIME_MS) {
                    if (magnitude < STILLNESS_THRESHOLD) {
                        fallConfirmed = true
                        Log.i(TAG, "Step 3: Fall Confirmed. Starting 10-second countdown.")

                        triggerConfirmationNotification()
                        handler.postDelayed(fallAlertRunnable, FALL_CONFIRMATION_DELAY_MS)
                    } else {
                        resetFallState()
                    }
                }
            }
            if (elapsedTime > 30000L) {
                resetFallState()
            }
        }
    }

    //  State Management (동일)

    /** 낙상 감지 상태를 초기화하고 타이머를 취소 */
    private fun resetFallState() {
        isFalling = false
        fallConfirmed = false
        fallStartTime = 0
        handler.removeCallbacks(fallAlertRunnable)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(NOTIFICATION_ID_ALERT)
        Log.d(TAG, "Fall detection state reset.")
    }

    /** Activity에서 '괜찮아요' 버튼을 눌렀을 때 호출되어 알림 전송을 취소 */
    private fun cancelFallDetection() {
        if (isFalling && fallConfirmed) {
            Log.i(TAG, "Fall Alert CANCELLED by user.")
            resetFallState()
        }
    }

    //  Alerting and Location
    // ... (나머지 로직은 수정 없이 유지) ...

    /** 최종적으로 위치를 획득하고 SMS/Kakao 알림을 전송하는 함수 */
    private fun getLocationAndSendAlert(isImmediate: Boolean = false, customMessage: String? = null) {
        val alertType = if (isImmediate) "IMMEDIATE ALERT (HR/SpO2)" else "FINAL FALL ALERT"
        Log.e(TAG, "--- $alertType TRIGGERED ---")

        // 최종 메시지를 결정합니다.
        val baseMessage = customMessage ?: "긴급 낙상 감지!"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "위치 권한이 없어 알림을 전송할 수 없습니다.")
            sendSms(GUARDIAN_PHONE_NUMBER, "$baseMessage 위치 권한 부족으로 위치 정보 획득 실패.")
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                val latitude = location?.latitude ?: 0.0
                val longitude = location?.longitude ?: 0.0

                if (location != null) {
                    val mapLink = "http://maps.google.com/maps?q=$latitude,$longitude"
                    val fullMessage = "$baseMessage 현재 위치: $mapLink"
                    sendSms(GUARDIAN_PHONE_NUMBER, fullMessage)
                    // sendKakaoAlert(latitude, longitude)
                } else {
                    Log.w(TAG, "위치 정보를 가져올 수 없습니다. 일반 메시지만 전송합니다.")
                    sendSms(GUARDIAN_PHONE_NUMBER, "$baseMessage 위치 정보 획득 실패.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "위치 정보를 가져오는 데 실패했습니다: ${e.message}")
                sendSms(GUARDIAN_PHONE_NUMBER, "$baseMessage 위치 정보 획득 중 오류 발생.")
            }
    }

    private fun sendSms(phoneNumber: String, message: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "SMS 권한이 없어 문자를 전송할 수 없습니다.")
            return
        }

        // Coroutine을 사용하여 전송 로직을 비동기로 실행
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    this@FallDetectionService.getSystemService(SmsManager::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    SmsManager.getDefault()
                }

                val sentIntent = Intent(SENT_ACTION).let { intent ->
                    PendingIntent.getBroadcast(this@FallDetectionService, 0, intent,
                        PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
                }

                val parts = smsManager.divideMessage(message)

                // 타입 오류 해결: MutableList로 명시적 변환
                val sentIntents = List(parts.size) { sentIntent }.toMutableList()

                // sendMultipartTextMessage을 사용하여 메시지 분할 전송
                smsManager.sendMultipartTextMessage(
                    phoneNumber,
                    null,
                    parts,
                    sentIntents as ArrayList<PendingIntent?>?,
                    null
                )

                Log.i(TAG, "구조 요청 문자 전송 시도 완료 (Multipart). 수신자: $phoneNumber")

            } catch (e: Exception) {
                Log.e(TAG, "문자 전송 실패 (Coroutine Catch): ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun sendKakaoAlert(latitude: Double, longitude: Double) {
        Log.d(TAG, "Kakao Alert implementation needed.")
    }

    //  Notification Handlers (동일)

    /** 포그라운드 서비스 알림 (백그라운드 실행 유지용) */
    private fun createServiceNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID_SERVICE,
                "낙상 감지 백그라운드 서비스",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }

        return NotificationCompat.Builder(this, CHANNEL_ID_SERVICE)
            .setContentTitle("안전 감지 서비스 실행 중")
            .setContentText("낙상 감지를 위해 센서를 사용 중입니다.")
            .setSmallIcon(R.drawable.appicon)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /* 낙상 확정 시 10초 대기 알림 (취소 버튼 포함) */
    private fun triggerConfirmationNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val alertChannel = NotificationChannel(
                CHANNEL_ID_ALERT,
                "낙상 경고 및 취소",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(alertChannel)
        }

        val cancelIntent = Intent(this, FallDetectionService::class.java).apply {
            action = ACTION_CANCEL_FALL
        }
        val cancelPendingIntent = PendingIntent.getService(
            this,
            0,
            cancelIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID_ALERT)
            .setContentTitle("낙상 감지됨!")
            .setContentText("10초 후 보호자에게 자동으로 구조 요청을 보냅니다.")
            .setSmallIcon(R.drawable.appicon)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(
                R.drawable.appicon,
                "괜찮아요 (취소)",
                cancelPendingIntent
            )
            .setOngoing(true)
            .setTimeoutAfter(FALL_CONFIRMATION_DELAY_MS)
            .build()

        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID_ALERT, notification)
    }
}