package com.example.myapplication.Alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.R
import kotlin.random.Random

object NotificationHelper {

    private const val CHANNEL_ID = "medication_reminder_channel"

    fun createNotification(context: Context, time: String) {
        // 시스템 서비스 가져오기
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val reminderMessage = "$time 오늘 약 드셨나요?"

        // 1. 알림 채널 생성 (Android 8.0/Oreo 이상 필수)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, // 상수 사용
                "약 복용 알림", // 사용자에게 표시되는 채널 이름
                NotificationManager.IMPORTANCE_HIGH // 중요도 높게 설정
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 2. 알림 빌드
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("약 복용 시간입니다")
            .setContentText(reminderMessage)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        // 3. 알림 띄우기 (알림 ID는 각기 다르게 부여)
        val notificationId = when (time) {
            "오전 8시" -> 800
            "오후 12시" -> 1200
            "오후 6시" -> 1800
            else -> Random.nextInt(1000) // 기타 시간은 0~999 사이의 랜덤 ID 부여
        }
        notificationManager.notify(notificationId, notification)
    }
}