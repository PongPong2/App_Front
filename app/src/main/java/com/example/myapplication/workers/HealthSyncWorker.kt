package com.example.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.*
// WorkManager에서 HealthConnectManager와 DataStore를 사용
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.data.HealthDataStore
import com.example.myapplication.data.HeartRateData
import androidx.health.connect.client.records.SleepSessionRecord // SleepSessionRecord 상수 참조를 위해 추가
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.time.Duration
import java.time.ZoneId
import java.time.LocalTime


class HealthSyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val healthConnectManager = HealthConnectManager(appContext)
    private val dataStore = HealthDataStore(appContext) // DataStore 인스턴스

    override suspend fun doWork(): Result {
        Log.d("WORKER", "HealthSyncWorker 실행됨 (10분 주기)")

        val endTime = Instant.now()
        val startTime = endTime.minus(10, ChronoUnit.MINUTES)

        try {
            // 걸음수 (Steps) 처리
            val previousStepsTotal = dataStore.getPreviousStepsTotal() // Long 로드
            val currentStepsTotal = healthConnectManager.readCumulativeTotalSteps() // Long 읽기

            var newSteps = currentStepsTotal - previousStepsTotal
            if (newSteps < 0) {
                newSteps = currentStepsTotal // 자정 리셋 시 현재 총합을 10분 걸음수로 간주
            }

            // 심박수 (Heart Rate) 처리: 평균 BPM 계산
            val heartRateSamples = healthConnectManager.readHeartRateSamples(startTime, endTime)
            val heartRateAvgBpm: Double = heartRateSamples
                .map { it.beatsPerMinute.toDouble() }
                .average() // 10분간의 평균 BPM

            // 소모 칼로리 (Calories) 처리: 안전 로직 적용
            val previousCaloriesTotal = dataStore.getPreviousCaloriesTotal() // Double 로드
            val currentCaloriesTotal = healthConnectManager.readCumulativeTotalCalories() // Double 읽기

            var newCalories = currentCaloriesTotal - previousCaloriesTotal

            // 개선된 자정 리셋 및 동기화 오류 처리
            if (newCalories < 0.0) {
                val now = Instant.now().atZone(ZoneId.systemDefault()).toLocalTime()
                val isNearMidnight = now.isBefore(LocalTime.of(0, 30))

                if (isNearMidnight) {
                    newCalories = currentCaloriesTotal
                    Log.i("WORKER", "칼로리: 정상 자정 리셋. 10분 변화량: $newCalories")
                } else {
                    newCalories = 0.0 // 비정상적인 값 감소는 0으로 처리
                    Log.w("WORKER", "칼로리: 비정상적인 누적 값 감소 감지. 변화량을 0.0으로 설정.")
                }
            }
            if (newCalories < 0.0) newCalories = 0.0 // 최종 안전 장치


            // 수면 단계 데이터 조회 및 처리 (단계별 시간 계산)
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
                        // SleepStageRecord 상수를 SleepSessionRecord의 상수로 변경
                        SleepSessionRecord.STAGE_TYPE_AWAKE -> sleepStageWakeMin += duration
                        SleepSessionRecord.STAGE_TYPE_DEEP -> sleepStageDeepMin += duration
                        SleepSessionRecord.STAGE_TYPE_REM -> sleepStageRemMin += duration
                        SleepSessionRecord.STAGE_TYPE_LIGHT -> sleepStageLightMin += duration // 얕은 수면/핵심 수면으로 간주
                        // 기타 단계는 무시
                    }
                }
            }


            //  서버 전송 데이터 객체 생성
            val fakeSpO2 = healthConnectManager.getFakeOxygenSaturation() // 가짜 SpO2 사용

            val healthData = HealthData( // 서버 전송용 데이터 객체 (HealthData 구조는 별도 정의 필요)
                walkingSteps = newSteps.toInt(),
                totalCaloriesBurned = newCalories,
                spo2 = fakeSpO2,
                heartRateAvg = heartRateAvgBpm,

                // 수면 단계별 시간
                sleepDurationMin = totalSleepDurationMin,
                sleepStageWakeMin = sleepStageWakeMin,
                sleepStageDeepMin = sleepStageDeepMin,
                sleepStageRemMin = sleepStageRemMin,
                sleepStageLightMin = sleepStageLightMin
            )

            // RetrofitClient.apiService.sendHealthData(healthData) // 이 코드를 활성화하여 서버로 전송
            Log.d("WORKER", "서버 전송 데이터 (10분 변화량) - 걸음: $newSteps 보, 칼로리: ${"%.2f".format(newCalories)} kcal, 수면: ${totalSleepDurationMin}분")

            // 다음 실행을 위해 현재 누적 총합 저장
            dataStore.savePreviousStepsTotal(currentStepsTotal)
            dataStore.savePreviousCaloriesTotal(currentCaloriesTotal)

            return Result.success()

        } catch (e: Exception) {
            Log.e("WORKER", "백그라운드 데이터 동기화 에러", e)
            return Result.retry() // 에러 발생 시 재시도 요청
        }
    }
}
