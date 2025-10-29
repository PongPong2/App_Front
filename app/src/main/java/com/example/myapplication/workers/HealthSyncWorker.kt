package com.example.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.data.HealthDataStore
import androidx.health.connect.client.records.SleepSessionRecord
import java.time.*
import java.time.temporal.ChronoUnit
import kotlin.math.max

class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val healthConnectManager = HealthConnectManager(appContext)
    private val dataStore = HealthDataStore(appContext)

    private val silverId = "test1234"
    private val gender = 'F'
    private val age = 70

    override suspend fun doWork(): Result {
        Log.d("WORKER", "HealthSyncWorker 실행됨 (10분 주기)")

        val endTime = Instant.now()
        val startTime = endTime.minus(10, ChronoUnit.MINUTES)

        try {
            // =========================
            // 걸음수 (Steps)
            // =========================
            val previousStepsTotal = dataStore.getPreviousStepsTotal()
            val currentStepsTotal = healthConnectManager.readCumulativeTotalSteps()
            var newSteps = currentStepsTotal - previousStepsTotal
            if (newSteps < 0) newSteps = currentStepsTotal // 자정 리셋 시
            newSteps = max(newSteps, 0) // 음수 방지

            // =========================
            // 심박수 (Heart Rate)
            // =========================
            val heartRateSamples = healthConnectManager.readHeartRateSamples(startTime, endTime)
            val heartRateAvgBpm = heartRateSamples.map { it.beatsPerMinute.toDouble() }.average()
            val safeHeartRate = if (heartRateAvgBpm.isNaN()) 0.0 else heartRateAvgBpm

            // =========================
            // 칼로리 (Calories)
            // =========================
            val previousCaloriesTotal = dataStore.getPreviousCaloriesTotal()
            val currentCaloriesTotal = healthConnectManager.readCumulativeTotalCalories()
            var newCalories = currentCaloriesTotal - previousCaloriesTotal

            if (newCalories < 0.0) {
                val now = LocalTime.now()
                val isNearMidnight = now.isBefore(LocalTime.of(0, 30))
                newCalories = if (isNearMidnight) {
                    currentCaloriesTotal // 자정 리셋 시
                } else {
                    Log.w("WORKER", "칼로리 누적값 감소 감지, 0으로 보정")
                    0.0
                }
            }
            newCalories = max(newCalories, 0.0)

            // =========================
            // 수면 데이터
            // =========================
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

            // =========================
            // SpO2 (가짜 값)
            // =========================
            val fakeSpO2 = try {
                healthConnectManager.getFakeOxygenSaturation()
            } catch (e: Exception) {
                Log.w("WORKER", "SpO2 가져오기 실패, 98%로 대체", e)
                98.0
            }

            val nowDateTime = LocalDateTime.now(ZoneId.systemDefault())

            // =========================
            // 서버 전송용 로그 (안전 문자열)
            // =========================
            val logMessage = try {
                "서버 전송 데이터 - 걸음: ${newSteps}보, 칼로리: %.2f kcal, 심박수: %.1f bpm, 수면: ${totalSleepDurationMin}분"
                    .format(newCalories.toDouble(), safeHeartRate)
            } catch (e: Exception) {
                Log.w("WORKER", "로그 포맷팅 실패", e)
                "서버 전송 데이터 - 걸음: ${newSteps}, 칼로리: ${newCalories}, 심박수: ${safeHeartRate}, 수면: ${totalSleepDurationMin}분"
            }
            Log.d("WORKER", logMessage)

            // =========================
            // 상태 저장
            // =========================
            dataStore.savePreviousStepsTotal(currentStepsTotal)
            dataStore.savePreviousCaloriesTotal(currentCaloriesTotal)

            return Result.success()

        } catch (e: Exception) {
            Log.e("WORKER", "백그라운드 데이터 동기화 중 예외 발생", e)
            return Result.retry()
        }
    }
}
