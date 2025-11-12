package com.example.myapplication.data

import android.content.Context
import android.health.connect.datatypes.BodyTemperatureMeasurementLocation.MEASUREMENT_LOCATION_EAR
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.mutableStateOf
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectClient.Companion.SDK_AVAILABLE
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.*
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.*
import java.time.Instant
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.time.Duration
import java.time.ZoneId
import kotlin.random.Random
import kotlin.random.nextInt

// Health Connect를 사용할 수 있는 최소 Android 레벨
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1
private const val SAMSUNG_HEALTH_PACKAGE = "com.sec.android.app.health"
/**
 * Health Connect의 읽기 및 쓰기
 */
class HealthConnectManager(private val context: Context) {

    // HealthConnectClient 인스턴스는 Activity의 Context가 유효한 시점에 사용되므로 lazy로 정의
    private val healthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    var availability = mutableStateOf(HealthConnectAvailability.NOT_SUPPORTED)
        private set


    fun checkAvailability() {
        availability.value = when {
            HealthConnectClient.getSdkStatus(context) == SDK_AVAILABLE -> HealthConnectAvailability.INSTALLED
            isSupported() -> HealthConnectAvailability.NOT_INSTALLED
            else -> HealthConnectAvailability.NOT_SUPPORTED
        }
    }

    suspend fun hasAllPermissions(permissions: Set<String>): Boolean {
        return healthConnectClient.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    fun requestPermissionsActivityContract(): ActivityResultContract<Set<String>, Set<String>> {
        return PermissionController.createRequestPermissionResultContract()
    }

    fun isFeatureAvailable(feature: Int): Boolean{
        return healthConnectClient
            .features
            .getFeatureStatus(feature) == HealthConnectFeatures.FEATURE_STATUS_AVAILABLE
    }

    //
    // (걸음수, 심박수, 산소 포화도, 소모 칼로리, 수면)

    /**
     * 지정된 날짜의 총 걸음 수를 집계하여 반환
     */
    suspend fun readTotalStepsForDay(day: ZonedDateTime): Long? {
        val startOfDay = day.truncatedTo(ChronoUnit.DAYS).toInstant()
        // 하루의 끝 (다음 날 자정 직전)
        val endOfDay = startOfDay.plus(Duration.ofDays(1)).minusMillis(1)

        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )

        val response = healthConnectClient.aggregate(request)
        return response[StepsRecord.COUNT_TOTAL]
    }

    /**
     * 지정된 기간의 [HeartRateRecord.Sample] 목록
     */
    suspend fun readHeartRateSamples(start: Instant, end: Instant): List<HeartRateRecord.Sample> {
        val request = ReadRecordsRequest(
            recordType = HeartRateRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        val response = healthConnectClient.readRecords(request)
        return response.records.flatMap { it.samples }
    }

    /*
    *  테스트용 가짜 심박수 데이터
    * */
    fun getFakeHeartRateSamples(start: Instant, end: Instant): List<HeartRateRecord.Sample> {
        val durationSeconds = Duration.between(start, end).seconds
        if (durationSeconds <= 0) return emptyList()

        val samples = mutableListOf<HeartRateRecord.Sample>()
        val numSamples = 10 // 10분 동안 10개 샘플 (1분당 1개 꼴)
        val intervalSeconds = durationSeconds / numSamples

        for (i in 0 until numSamples) {
            val sampleTime = start.plusSeconds(i * intervalSeconds)
            // 60~85 BPM 사이의 정상적인 심박수 생성
            val bpm = Random.nextInt(60, 86).toLong()

            samples.add(
                HeartRateRecord.Sample(
                    beatsPerMinute = bpm,
                    time = sampleTime
                )
            )
        }
        return samples
    }

    /**
     * 지정된 기간의 [OxygenSaturationRecord] 목록
     */
//    suspend fun readOxygenSaturation(start: Instant, end: Instant): List<OxygenSaturationRecord> {
//        val request = ReadRecordsRequest(
//            recordType = OxygenSaturationRecord::class,
//            timeRangeFilter = TimeRangeFilter.between(start, end)
//        )
//        return healthConnectClient.readRecords(request).records
//    }

    /**
     * 지정된 기간의 총 소모 칼로리를 집계
     */
    suspend fun readTotalCaloriesForDay(day: ZonedDateTime): Double? {
        val startOfDay = day.truncatedTo(ChronoUnit.DAYS).toInstant()
        val endOfDay = startOfDay.plus(Duration.ofDays(1)).minusMillis(1) // 하루의 끝

        val request = AggregateRequest(
            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
        )

        val response = healthConnectClient.aggregate(request)
        // Double 타입인 Energy 객체에서 Kilocalories 값을 가져와 Double? 로 반환
        return response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories
    }

    /**
     * [테스트용] 95% ~ 100% 사이의 임의의 산소 포화도 값을 반환 => 산소포화도 10분마다 못받아오니까 수정 필요
     */
    fun getFakeOxygenSaturation(): Int {
        val randomValue = (Random.nextInt(95, 100))
        return randomValue
    }

    /**
     * 지정된 기간의 [SleepSessionRecord] 목록
     */


    /**
     * 지정된 기간의 [SleepSessionRecord] 목록
     */
    suspend fun readSleepSessions(start: Instant, end: Instant): List<SleepSessionRecord> {
        val request = ReadRecordsRequest(
            recordType = SleepSessionRecord::class,
            timeRangeFilter = TimeRangeFilter.between(start, end)
        )
        return healthConnectClient.readRecords(request).records
    }



    /**
     * [실시간 모니터링용] 지정된 기간 (예: 최근 1분)의 평균 심박수를 반환
     */
    suspend fun readLatestHeartRateAvg(durationMinutes: Long = 1): Double {
        val endTime = Instant.now()
        val startTime = endTime.minus(durationMinutes, ChronoUnit.MINUTES)

        val samples = getFakeHeartRateSamples(startTime, endTime) // 가짜 샘플 사용

        if (samples.isEmpty()) {
            return 0.0 // 데이터가 없으면 0 반환
        }

        // 모든 샘플의 BPM 평균을 계산
        return samples.map { it.beatsPerMinute.toDouble() }.average()
    }
    // 데이터 기록하기 (WRITE 전용)
    // (체중, 혈당, 혈압)




    /**
     * [BloodPressureRecord]를 Health Connect에 기록
     * @param systolic 수축기 혈압 (mmHg)
     * @param diastolic 이완기 혈압 (mmHg)
     */
    suspend fun readBloodPressureRecords(startTime: Instant, endTime: Instant): List<BloodPressureRecord> {
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(
                    BloodPressureRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
                )
            )
            response.records
        } catch (e: Exception) {
            Log.e("HC_MANAGER", "혈압 기록 읽기 실패", e)
            emptyList()
        }
    }




    // 수면점수 계산
    suspend fun calculateSleepScoreForPreviousNight(): Int? {
        // 어제 날짜의 시작 시간 (자정 00:00:00)부터 오늘 아침 12:00까지의 수면 세션 검색
        val zone = ZoneId.systemDefault()
        val yesterday = ZonedDateTime.now(zone).minusDays(1).toLocalDate()
        val startTime = yesterday.atStartOfDay(zone).toInstant()
        val endTime = yesterday.atTime(12, 0).atZone(zone).toInstant() // 정오까지

        val sleepSessions = readSleepSessions(startTime, endTime) // 기존 readSleepSessions 사용

        if (sleepSessions.isEmpty()) {
            return null // 수면 기록 없음
        }

        var totalDeepMinutes = 0L
        var totalSleepDuration = 0L

        for (session in sleepSessions) {
            // 총 수면 시간 계산
            totalSleepDuration += Duration.between(session.startTime, session.endTime).toMinutes()

            // 깊은 수면 시간 계산
            for (stage in session.stages) {
                if (stage.stage == SleepSessionRecord.STAGE_TYPE_DEEP) {
                    totalDeepMinutes += Duration.between(stage.startTime, stage.endTime).toMinutes()
                }
            }
        }

        if (totalSleepDuration == 0L) return null

        // 점수 계산: (총 수면 시간 + 깊은 수면 시간 * 2) / 목표 수면 시간 (480분=8시간)을 기준으로 환산
        val targetSleepMinutes = 480
        val score = (totalSleepDuration + totalDeepMinutes * 0.5).toDouble() / targetSleepMinutes * 100

        // 점수는 0~100 사이, 최대 100으로 제한
        return minOf(score.toInt(), 100)
    }

    suspend fun readLatestWeight(): WeightRecord? {
        val endTime = Instant.now()
        val startTime = endTime.minus(30, ChronoUnit.DAYS)

        return try {
            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime),
                // 최신 기록을 먼저 가져오기 위해 내림차순 정렬
                ascendingOrder = false,
            )

            val response = healthConnectClient.readRecords(request)
            response.records.firstOrNull()

        } catch (e: Exception) {
            Log.e("HC_MANAGER", "최신 체중 기록 읽기 실패", e)
            null // 오류 발생 시 null 반환
        }
    }

    // --- 유틸리티 함수 ---
    private fun isSupported() = Build.VERSION.SDK_INT >= MIN_SUPPORTED_SDK
}

/**
 * Health Connect의 가용성 상태
 */
enum class HealthConnectAvailability {
    INSTALLED,
    NOT_INSTALLED,
    NOT_SUPPORTED
}