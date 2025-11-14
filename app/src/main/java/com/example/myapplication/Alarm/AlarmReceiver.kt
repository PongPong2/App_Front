package com.example.myapplication.Alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

// AlarmReceiver.kt
// AlarmReceiver는 AlarmManager로부터 예약된 시간에 호출됩니다.
class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_TIME = "extra_time"
        const val ACTION_ALARM_TRIGGER = "com.example.myapplication.ACTION_ALARM_TRIGGER"
        private const val TAG = "AlarmReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // 널 체크: Context나 Intent가 null인 경우 처리를 중단합니다.
        if (context == null || intent == null) {
            Log.e(TAG, "Context or Intent is null, skipping alarm logic.")
            return
        }

        // 폰 부팅 시 알람 재등록 로직 (BOOT_COMPLETED 액션)
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed. Re-setting alarms.")
            try {
                // AlarmScheduler를 호출하여 모든 알람을 재설정
                AlarmScheduler.setAllDailyAlarms(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to re-set alarms on boot: ${e.message}")
            }
            return
        }

        // 일반 알람 수신 로직
        val alarmTime = intent.getStringExtra(EXTRA_TIME) ?: "약 복용"
        Log.d(TAG, "Alarm received for: $alarmTime")

        // 1. 시스템 상태 표시줄 알림 띄우기 (NotificationHelper 호출)
        try {
            // NotificationHelper는 시스템 알림을 표시합니다.
            NotificationHelper.createNotification(context, alarmTime)
            Log.d(TAG, "Notification successfuly created for: $alarmTime")
        } catch (e: Exception) {
            // 알림 채널 또는 리소스 오류 시 발생할 수 있습니다.
            Log.e(TAG, "Failed to create system notification: ${e.message}")
        }

        // 2. 앱 내부 알림을 위해 이벤트 버스에 이벤트 발행 (인앱 다이얼로그용)
        // GlobalScope는 BroadcastReceiver가 onReceive를 마친 후에도 코루틴이 완료될 시간을 제공합니다.
        GlobalScope.launch {
            try {
                AlarmEventBus.emitAlarmEvent(alarmTime)
                Log.d(TAG, "Event emitted to AlarmEventBus: $alarmTime")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to emit alarm event: ${e.message}")
            }
        }
    }
}