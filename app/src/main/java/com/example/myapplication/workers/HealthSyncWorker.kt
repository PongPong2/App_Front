package com.example.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.*
// WorkManager에서 HealthConnectManager와 DataStore를 사용
import com.example.myapplication.data.HealthConnectManager
import com.example.myapplication.data.HealthDataStore
import com.example.myapplication.data.HeartRateData
import java.time.Instant
import java.time.temporal.ChronoUnit


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
            // 걸음수 차감 로직 (기존 구현)
            val previousStepsTotal = dataStore.getPreviousStepsTotal() // Long 로드
            val currentStepsTotal = healthConnectManager.readCumulativeTotalSteps() // Long 읽기

            var newSteps = currentStepsTotal - previousStepsTotal
            if (newSteps < 0) {
                newSteps = currentStepsTotal // 자정 리셋 시 현재 총합을 10분 걸음수로 간주
            }
            val heartRateSamples = healthConnectManager.readHeartRateSamples(startTime, endTime)

            val heartRateDataList = heartRateSamples.map { sample ->
                // BPM과 Time을 서버 전송 포맷에 맞게 변환
                HeartRateData(
                    bpm = sample.beatsPerMinute.toDouble(),
                    time = sample.time.toString()
                )
            }
            // 소모 칼로리 차감 로직 (새로운 구현)
            val previousCaloriesTotal = dataStore.getPreviousCaloriesTotal() // Double 로드
            val currentCaloriesTotal = healthConnectManager.readCumulativeTotalCalories() // Double 읽기

            // 10분간의 순수 칼로리 변화량 (총 소모 칼로리)
            var newCalories = currentCaloriesTotal - previousCaloriesTotal

            // 자정 리셋 처리: 현재 총합이 이전 값보다 작으면 자정이 지난 것
            if (newCalories < 0) {
                newCalories = currentCaloriesTotal
            }

            // 데이터가 음수로 나오는 것을 방지 (간혹 동기화 오류로 음수가 나올 수 있음)
            if (newCalories < 0.0) newCalories = 0.0


            // LSTM 데이터 레코드 생성 및 서버 전송

            // 기타 시계열 데이터 읽기 (심박수, SpO2)
            // val heartRate = healthConnectManager.readHeartRateData(start, end)
            val fakeSpO2 = healthConnectManager.getFakeOxygenSaturation() // 가짜 SpO2 사용

            val healthData = HealthData( // 서버 전송용 데이터 객체
                walkingSteps = newSteps.toInt(),
                totalCaloriesBurned = newCalories, // 10분간의 칼로리 변화량 전송
                spo2 = fakeSpO2,
                heartRate = heartRateDataList
            )

            // RetrofitClient.apiService.sendHealthData(healthData)
            Log.d("WORKER", "서버 전송 데이터 (10분 변화량) - 걸음: $newSteps 보, 칼로리: ${"%.2f".format(newCalories)} kcal")

            // 다음 실행을 위해 현재 누적 총합 저장
            dataStore.savePreviousStepsTotal(currentStepsTotal)
            dataStore.savePreviousCaloriesTotal(currentCaloriesTotal) // 현재 칼로리 총합 저장

            return Result.success()

        } catch (e: Exception) {
            Log.e("WORKER", "백그라운드 데이터 동기화 에러", e)
            return Result.retry() // 에러 발생 시 재시도 요청
        }
    }
}