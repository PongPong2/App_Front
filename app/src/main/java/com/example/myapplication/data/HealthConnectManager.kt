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

// Health Connect를 사용할 수 있는 최소 Android 레벨
const val MIN_SUPPORTED_SDK = Build.VERSION_CODES.O_MR1

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
    suspend fun readCumulativeTotalSteps(): Long { // 인자 (day: ZonedDateTime) 제거
        val now = Instant.now()
        // 시스템 기본 시간대를 사용하여 정확한 '오늘 자정' Instant 계산
        val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant()

        val request = AggregateRequest(
            metrics = setOf(StepsRecord.COUNT_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
        )

        val response = healthConnectClient.aggregate(request)
        return response[StepsRecord.COUNT_TOTAL] ?: 0L
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
    suspend fun readCumulativeTotalCalories(): Double { // 인자 (day: ZonedDateTime) 제거
        val now = Instant.now()
        // 시스템 기본 시간대를 사용하여 정확한 '오늘 자정' Instant 계산
        val startOfDay = ZonedDateTime.now(ZoneId.systemDefault())
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant()

        val request = AggregateRequest(
            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
            timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
        )

        val response = healthConnectClient.aggregate(request)
        // Double로 반환 (Worker의 차감 로직에 필요)
        return response[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
    }

    /**
     * [테스트용] 95.0% ~ 100.0% 사이의 임의의 산소 포화도 값을 반환 => 산소포화도 10분마다 못받아오니까 수정 필요
     */
    fun getFakeOxygenSaturation(): Double {
        val randomValue = (Random.nextDouble(95.0, 100.0) * 100).toLong() / 100.0
        return randomValue
    }

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

    // 데이터 기록하기 (WRITE 전용)
    // (체중, 혈당, 혈압)

    /**
     * [WeightRecord]를 Health Connect에 기록
     */
    suspend fun writeWeightInput(weightInput: Double) {
        try {
            val time = Instant.now()
            val weightRecord = WeightRecord(
                weight = Mass.kilograms(weightInput),
                time = time,
                zoneOffset = ZonedDateTime.now().offset,
                metadata = Metadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(weightRecord))
            Toast.makeText(context, "체중 ${weightInput}kg 기록 성공", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HC_WRITE", "체중 기록 실패: ${e.message}")
            Toast.makeText(context, "체중 기록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * [BloodGlucoseRecord]를 Health Connect에 기록
     * @param concentration 혈당 농도 (mg/dL 단위)
     */
    suspend fun writeBloodGlucose(concentration: Double) {
        try {
            val time = Instant.now()
            val glucoseRecord = BloodGlucoseRecord(
                level = BloodGlucose.milligramsPerDeciliter(concentration),
                specimenSource = BloodGlucoseRecord.SPECIMEN_SOURCE_CAPILLARY_BLOOD,
                mealType = BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
                time = time,
                zoneOffset = ZonedDateTime.now().offset,
                metadata = Metadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(glucoseRecord))
            Toast.makeText(context, "혈당 ${concentration}mg/dL 기록 성공", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HC_WRITE", "혈당 기록 실패: ${e.message}")
            Toast.makeText(context, "혈당 기록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * [BloodPressureRecord]를 Health Connect에 기록
     * @param systolic 수축기 혈압 (mmHg)
     * @param diastolic 이완기 혈압 (mmHg)
     */
    suspend fun writeBloodPressure(systolic: Double, diastolic: Double) {
        try {
            val time = Instant.now()
            val bpRecord = BloodPressureRecord(
                systolic = Pressure.millimetersOfMercury(systolic),
                diastolic = Pressure.millimetersOfMercury(diastolic),
                bodyPosition = BloodPressureRecord.BODY_POSITION_SITTING_DOWN,
                measurementLocation = BloodPressureRecord.MEASUREMENT_LOCATION_LEFT_UPPER_ARM,
                time = time,
                zoneOffset = ZonedDateTime.now().offset,
                metadata = Metadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(bpRecord))
            Toast.makeText(context, "혈압 ${systolic}/${diastolic}mmHg 기록 성공", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HC_WRITE", "혈압 기록 실패: ${e.message}")
            Toast.makeText(context, "혈압 기록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * [BodyTemperatureRecord]를 Health Connect에 기록
     * @param temperatureInput 체온 값 (섭씨 또는 화씨, SDK 단위에 맞춤)
     */
    suspend fun writeBodyTemperature(temperatureInput: Double) {
        try {
            val time = Instant.now()
            // Health Connect는 BodyTemperature을 주로 섭씨(DegreesCelsius)로 처리합니다.
            val tempRecord = BodyTemperatureRecord(
                temperature = Temperature.celsius(temperatureInput),
                time = time,
                zoneOffset = ZonedDateTime.now().offset,
                measurementLocation = MEASUREMENT_LOCATION_EAR, // 필요에 따라 변경
                metadata = Metadata.manualEntry()
            )
            healthConnectClient.insertRecords(listOf(tempRecord))
            Toast.makeText(context, "체온 ${temperatureInput}°C 기록 성공", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("HC_WRITE", "체온 기록 실패: ${e.message}")
            Toast.makeText(context, "체온 기록 실패: ${e.message}", Toast.LENGTH_SHORT).show()
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