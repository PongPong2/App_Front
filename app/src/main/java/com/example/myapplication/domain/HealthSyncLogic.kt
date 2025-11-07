package com.example.myapplication.domain

import android.content.Context
import android.util.Log
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.data.HealthDataStore
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.util.SharedPrefsManager
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.max
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.Duration
import java.time.LocalTime // LocalTime 임포트 추가
import java.time.ZonedDateTime

// 작업 결과를 나타내는 자체 Enum 정의 (WorkManager.Result 대체)
enum class SyncResult {
    SUCCESS,
    FAILURE,
    RETRY
}

class HealthSyncLogic(private val context: Context) {

    private val healthConnectManager = HealthConnectManager(context)
    private val dataStore = HealthDataStore(context)
    private val prefsManager = SharedPrefsManager(context)

    // 심박수/칼로리/걸음수 계산 기준 (10분 주기)
    private val SYNC_INTERVAL_MINUTES = 10L

    // HealthSyncWorker의 doWork() 내용을 이곳으로 옮김
    suspend fun performSync(): SyncResult { // 반환 타입을 SyncResult로 변경
        val silverId = prefsManager.getSilverId()
        val gender = prefsManager.getGender()

        if (silverId.isNullOrEmpty()) {
            Log.w("SYNC_LOGIC", "HealthSync 실행 실패: 로그인된 사용자 ID를 찾을 수 없습니다.")
            return SyncResult.FAILURE // Result.failure() 대신 사용
        }

        Log.d("SYNC_LOGIC", "HealthSync 실행됨 (${SYNC_INTERVAL_MINUTES}분 주기)")

        val endTime = Instant.now()
        val startTime = endTime.minus(SYNC_INTERVAL_MINUTES, ChronoUnit.MINUTES)

        val today = ZonedDateTime.now(ZoneId.systemDefault())
        try {
            // --- 걸음수 (Steps) ---
            val previousStepsTotal = dataStore.getPreviousStepsTotal()
            val currentStepsTotalNullable = healthConnectManager.readTotalStepsForDay(today)
            val currentStepsTotal = currentStepsTotalNullable ?: 0L
            Log.d("SYNC_LOGIC", "이전 걸음수: $previousStepsTotal, 현재 걸음수: $currentStepsTotal")
            var newSteps = currentStepsTotal - previousStepsTotal
//            if (newSteps < 0) newSteps = currentStepsTotal
            newSteps = max(newSteps, 0)

            // --- 심박수 (Heart Rate) ---
            val heartRateSamples = healthConnectManager.readHeartRateSamples(startTime, endTime)
            val heartRateAvgBpm = heartRateSamples.map { it.beatsPerMinute.toDouble() }.average()
            val safeHeartRate = if (heartRateAvgBpm.isNaN()) 0.0 else heartRateAvgBpm

            // --- 칼로리 (Calories) ---
            val previousCaloriesTotal = dataStore.getPreviousCaloriesTotal()
            val currentCaloriesTotalNullable = healthConnectManager.readTotalCaloriesForDay(today)
            val currentCaloriesTotal = currentCaloriesTotalNullable ?: 0.0
            Log.d("SYNC_LOGIC", "이전 칼로리: $previousCaloriesTotal, 현재 칼로리: $currentCaloriesTotal")
            var newCalories = currentCaloriesTotal - previousCaloriesTotal

            if (newCalories < 0.0) {
                val now = LocalTime.now()
                val isNearMidnight = now.isBefore(LocalTime.of(0, 30))
                newCalories = if (isNearMidnight) {
                    currentCaloriesTotal
                } else {
                    Log.w("SYNC_LOGIC", "칼로리 누적값 감소 감지, 0으로 보정")
                    0.0
                }
            }
            newCalories = max(newCalories, 0.0)

            // --- 수면 데이터 (Sleep) ---
            val sleepSessions = healthConnectManager.readSleepSessions(startTime, endTime)
            var totalSleepDurationMin = 0L
            var sleepStageWakeMin = 0L
            var sleepStageDeepMin = 0L
            var sleepStageRemMin = 0L
            var sleepStageLightMin = 0L

            for (session in sleepSessions) {
                totalSleepDurationMin += Duration.between(session.startTime, session.endTime).toMinutes()
                for (stage in session.stages) {
                    val duration = Duration.between(stage.startTime, stage.endTime).toMinutes()
                    when (stage.stage) {
                        SleepSessionRecord.STAGE_TYPE_AWAKE -> sleepStageWakeMin += duration
                        SleepSessionRecord.STAGE_TYPE_DEEP -> sleepStageDeepMin += duration
                        SleepSessionRecord.STAGE_TYPE_REM -> sleepStageRemMin += duration
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> sleepStageLightMin += duration
                    }
                }
            }

            // --- SpO2 (가짜 값) ---
            val fakeSpo2 = try {
                healthConnectManager.getFakeOxygenSaturation()
            } catch (e: Exception) {
                Log.w("SYNC_LOGIC", "SpO2 가져오기 실패, 98%로 대체", e)
                98.0
            }


            val healthRequest = HealthRequest(
                silverId = silverId,
                walkingSteps = newSteps.toInt(),
                totalCaloriesBurned = newCalories,
                spo2 = fakeSpo2.toInt(),
                heartRateAvg = safeHeartRate,

                sleepDurationMin = totalSleepDurationMin,
                sleepStageWakeMin = sleepStageWakeMin,
                sleepStageDeepMin = sleepStageDeepMin,
                sleepStageRemMin = sleepStageRemMin,
                sleepStageLightMin = sleepStageLightMin
            )

            // --- 서버 전송 ---
            try {
                // RetrofitClient.healthService.createHealthData(healthRequest) // 실제 서버 호출
                val logMessage = "서버 전송 데이터 - 걸음: ${newSteps}보, 칼로리: %.2f kcal, 심박수: %.1f bpm"
                    .format(newCalories.toDouble(), safeHeartRate)
                Log.d("SYNC_LOGIC", logMessage)
            } catch (apiError: Exception) {
                Log.e("SYNC_LOGIC", "API 서버 전송 실패", apiError)
                return SyncResult.FAILURE // Result.failure() 대신 사용
            }

            // --- 상태 저장 ---
            dataStore.savePreviousStepsTotal(currentStepsTotal)
            dataStore.savePreviousCaloriesTotal(currentCaloriesTotal)

            return SyncResult.SUCCESS // Result.success() 대신 사용

        } catch (e: Exception) {
            Log.e("SYNC_LOGIC", "Health Connect 데이터 읽기 실패", e)
            return SyncResult.RETRY // Result.retry() 대신 사용
        }
    }
}