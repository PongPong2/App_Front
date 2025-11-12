package com.example.myapplication.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.data.HealthConnectManager
import androidx.health.connect.client.records.BloodPressureRecord
import androidx.health.connect.client.units.Pressure
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.util.SharedPrefsManager
import com.example.myapplication.domain.DailyHealthLogRequest
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class DailyBloodPressureWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val healthConnectManager = HealthConnectManager(appContext)
    private val prefsManager = SharedPrefsManager(appContext)

    val today = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate()
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE // yyyy-MM-dd 형식
    val logDateString = today.format(dateFormatter)

    override suspend fun doWork(): Result {
        val silverId = prefsManager.getSilverId()
        if (silverId.isNullOrEmpty()) {
            Log.w("WORKER", "HealthSyncWorker 실행 실패: 로그인된 사용자 ID를 찾을 수 없습니다.")
            return Result.failure() // ID 없으면 실패 처리
        }
        Log.d("DAILY_WORKER", "일일 건강 마감 Worker 실행됨 (23:50)")

        val today = ZonedDateTime.now(ZoneId.systemDefault()).toLocalDate()
        val startTime = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endTime = Instant.now()

        try {
            // 혈압 데이터 수집 및 처리 (가장 최신 기록 사용)
            val bpRecords = healthConnectManager.readBloodPressureRecords(startTime, endTime)

            val latestSystolic: Int?
            val latestDiastolic: Int?

            if (bpRecords.isNotEmpty()) {
                val lastRecord = bpRecords.maxByOrNull { it.time }
                latestSystolic = lastRecord?.systolic?.inMillimetersOfMercury?.toInt()
                latestDiastolic = lastRecord?.diastolic?.inMillimetersOfMercury?.toInt()
                if (lastRecord == null) {
                    Log.w("DAILY_WORKER", "혈압 기록은 있지만 최신 레코드를 찾을 수 없습니다.")
                }
            } else {
                latestSystolic = null
                latestDiastolic = null
                Log.d("DAILY_WORKER", "오늘 측정된 혈압 기록 없음.")
            }

            val latestWeightRecord = healthConnectManager.readLatestWeight()

            val latestWeightKg: Double? = if (latestWeightRecord != null) {
                latestWeightRecord.weight.inKilograms // Kilograms 단위로 변환
            } else {
                Log.d("DAILY_WORKER", "최근 체중 기록 없음.")
                null
            }
            // 수면 점수 계산 및 처리
            // HealthConnectManager 내의 별도 함수를 호출하여 어젯밤 수면 점수를 계산 (Int 타입)
            val sleepScore: Int? = healthConnectManager.calculateSleepScoreForPreviousNight()
            if (sleepScore == null) {
                Log.w("DAILY_WORKER", "어젯밤 수면 데이터 부족으로 점수 계산 불가.")
            }

            // DTO 구성 및 전송
            val summaryRequest = DailyHealthLogRequest(
                silverId = silverId,
                systolicBloodPressure = latestSystolic,
                diastolicBloodPressure = latestDiastolic,
                sleepScore = sleepScore,
                weight = latestWeightKg,
                logDate = logDateString
            )

            // RetrofitClient.healthService에 sendDailyHealthLog(request: DailyHealthLogRequest)가 구현되어 있어야 함
            RetrofitClient.healthService.sendDailyHealthLog(summaryRequest)

            Log.d("DAILY_WORKER", "일일 건강 로그 전송 완료. BP: $latestSystolic/$latestDiastolic, SleepScore: $sleepScore")

            return Result.success()

        } catch (e: Exception) {
            Log.e("DAILY_WORKER", "일일 건강 로그 동기화 중 예외 발생", e)
            return Result.retry()
        }
    }
}