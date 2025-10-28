package com.example.myapplication.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// DataStore 인스턴스를 애플리케이션 Context에 연결
// 파일 이름은 "health_sync_prefs"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "health_sync_prefs")

private val PREF_PREVIOUS_STEPS_TOTAL = doublePreferencesKey("previous_steps_total")
private val PREF_PREVIOUS_CALORIES_TOTAL = doublePreferencesKey("previous_calories_total")

/**
 * WorkManager가 걸음수와 칼로리의 누적 총합을 저장하고 불러오는 클래스
 */
class HealthDataStore(private val context: Context) {

    // --- 걸음 수 저장/로드 ---

    /**
     * 이전에 저장된 총 걸음수(Double 형태로 저장된 값)\
     */
    suspend fun getPreviousStepsTotal(): Long {
        // Double로 저장된 값을 Long으로 변환하여 반환 (Int Overflow 방지를 위해 Double 사용)
        return context.dataStore.data
            .map { preferences ->
                preferences[PREF_PREVIOUS_STEPS_TOTAL]?.toLong() ?: 0L
            }.first()
    }

    /**
     * 현재 총 걸음수를 저장
     */
    suspend fun savePreviousStepsTotal(totalSteps: Long) {
        context.dataStore.edit { preferences ->
            // DataStore에 Long을 직접 저장하는 대신, Double로 변환하여 저장
            // (LongKey가 특정 버전에서 불안정할 수 있어, 일반적으로 Double을 사용)
            preferences[PREF_PREVIOUS_STEPS_TOTAL] = totalSteps.toDouble()
        }
    }

    // --- 칼로리 저장/로드 ---

    /**
     * 이전에 저장된 총 소모 칼로리(Double)
     */
    suspend fun getPreviousCaloriesTotal(): Double {
        return context.dataStore.data
            .map { preferences ->
                preferences[PREF_PREVIOUS_CALORIES_TOTAL] ?: 0.0
            }.first()
    }

    /**
     * 현재 총 소모 칼로리
     */
    suspend fun savePreviousCaloriesTotal(totalCalories: Double) {
        context.dataStore.edit { preferences ->
            preferences[PREF_PREVIOUS_CALORIES_TOTAL] = totalCalories
        }
    }
}