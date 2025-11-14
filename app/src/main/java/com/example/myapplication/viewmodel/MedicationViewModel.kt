package com.example.myapplication.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.Alarm.AlarmScheduler
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.data_model.SilverMedication
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

// Application context에 접근해야 하므로 AndroidViewModel을 상속합니다.
class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * 서버에서 특정 사용자(silverId)의 약물 정보를 비동기적으로 가져옵니다.
     */
    fun fetchMedications(silverId: String) {
        viewModelScope.launch {
            try {
                // 1. API 호출
                val response = RetrofitClient.medicationService.getMedications(silverId)

                if (response.isSuccessful) {
                    val medications = response.body()

                    // 2. 응답 성공 처리 (HTTP 200-299)
                    if (medications.isNullOrEmpty()) {
                        // 등록된 약물이 없는 경우
                        handleNoMedications()
                    } else {
                        // ⭐️ DB에 등록된 약물이 있는 경우
                        handleMedicationsFound(medications)
                    }
                } else {
                    // 3. 서버 응답 코드 오류 (예: 404, 500)
                    handleApiError(response.code(), response.message())
                }
            } catch (e: HttpException) {
                // 4. HTTP 통신 중 예외 발생
                Log.e("MedicationViewModel", "HTTP Exception: ${e.message}")
                handleNetworkError("HTTP 통신 오류가 발생했습니다.")
            } catch (e: IOException) {
                // 5. 네트워크 연결 관련 예외 발생 (인터넷 끊김 등)
                Log.e("MedicationViewModel", "IO Exception: ${e.message}")
                handleNetworkError("네트워크 연결을 확인해주세요.")
            } catch (e: Exception) {
                // 6. 기타 예외 처리
                Log.e("MedicationViewModel", "Unknown Error: ${e.message}")
                handleNetworkError("알 수 없는 오류가 발생했습니다.")
            }
        }
    }

    /**
     * 약물 목록이 성공적으로 조회되었을 때 처리하고 알람을 설정합니다.
     */
    private fun handleMedicationsFound(medications: List<SilverMedication>) {
        Log.d("MedicationViewModel", "Medication info successfully received: ${medications.size} items")

        // ⭐️ AndroidViewModel의 application property를 사용하여 Context에 접근합니다.
        val context = this.getApplication<Application>().applicationContext

        // 매일 3회 알람 설정 시작
        AlarmScheduler.setAllDailyAlarms(context)

        // UI 업데이트 로직 (LiveData 등을 사용)
        println("약물 정보 수신 성공, 매일 3회 알람이 설정되었습니다.")
    }

    /**
     * 등록된 약물 정보가 없을 때 처리합니다.
     */
    private fun handleNoMedications() {
        Log.i("MedicationViewModel", "No registered medication information. Skipping alarm setting.")
        // 필요하다면, 기존에 설정된 알람을 취소하는 로직을 추가합니다.
        // AlarmScheduler.cancelAllAlarms(getApplication<Application>().applicationContext)
    }

    // --- 에러 처리 함수 (사용자에게 피드백 제공) ---

    private fun handleApiError(code: Int, message: String) {
        Log.e("MedicationViewModel", "API Error - Code $code: $message")
        // TODO: 사용자에게 API 오류 메시지를 보여주는 로직 추가
    }

    private fun handleNetworkError(errorMessage: String) {
        Log.e("MedicationViewModel", "Network Error: $errorMessage")
        // TODO: 사용자에게 네트워크 오류 메시지를 보여주는 로직 추가
    }
}