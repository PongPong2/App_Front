package com.example.myapplication.Alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar

// AlarmScheduler.kt
// 알람 예약 및 취소를 담당합니다.
object AlarmScheduler {

    // 알림 스케줄링 대상 시간 목록 (24시간 형식)
    private val SCHEDULES = listOf(
        Pair(8, 0),    // 08:00 (오전 8시)
        Pair(12, 0),  // 12:00 (오전12시 00분)
        Pair(18, 0)   // 18:00 (오후 6시)
    )

    fun setAllDailyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        SCHEDULES.forEach { (hour, minute) ->
            setDailyAlarm(context, alarmManager, hour, minute)
        }
    }

    private fun setDailyAlarm(context: Context, alarmManager: AlarmManager, hour: Int, minute: Int) {
        // 1. 알람 호출 시 실행될 Intent 설정
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ACTION_ALARM_TRIGGER
            val timeString = when {
                hour == 8 && minute == 0 -> "오전 8시"
                hour == 12 && minute == 0 -> "오전 12시"
                hour == 18 && minute == 0 -> "오후 6시"
                else -> "${hour}시 ${minute}분" // 그 외의 경우는 시/분 표시
            }
            // 알림 내용 생성을 위해 EXTRA_TIME을 Receiver로 전달
            putExtra(AlarmReceiver.EXTRA_TIME, timeString)
        }

        // 2. PendingIntent 생성 (알람 시간별 고유한 요청 코드 사용)
        val requestCode = hour * 100 + minute

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. 첫 알람 시간 설정
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)

            // 만약 설정 시간이 현재 시간보다 이전이라면, 다음 날로 설정
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // 4. 알람 예약 (매일 반복)
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, // 기기가 잠자기 상태여도 깨워서 알림
            calendar.timeInMillis,    // 알람 시작 시간
            AlarmManager.INTERVAL_DAY, // 24시간 간격으로 반복
            pendingIntent
        )
    }

    fun cancelAlarm(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val requestCode = hour * 100 + minute

        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}