package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.domain.DailyHealthLogRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.time.ZoneId
import android.widget.TextView // TextView 임포트 추가

class MainPageActivity : AppCompatActivity() {

    private lateinit var prefsManager: SharedPrefsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

        prefsManager = SharedPrefsManager(this)


        val nameTextView = findViewById<TextView>(R.id.M_A_name) // 이름 TextView ID
        val genderTextView = findViewById<TextView>(R.id.M_A_gender) // 성별/나이 TextView ID (XML 이름 기준)

        val username = prefsManager.getUsername()
        val gender = prefsManager.getGender()

        nameTextView?.text = username
        genderTextView?.text = gender

        Log.d("MPA", "MainPage loaded. User: $username, Gender: $gender")


        val yoyangsaButton = findViewById<Button>(R.id.M_Show_A_Info)
        yoyangsaButton.setOnClickListener {
            val intent = Intent(this, YoyangsaActivity::class.java)
            startActivity(intent)
        }

        val bohojaButton = findViewById<Button>(R.id.M_Show_P_Info)
        bohojaButton.setOnClickListener {
            val intent = Intent(this, BohojaActivity::class.java)
            startActivity(intent)
        }

        val sosButton = findViewById<MaterialButton>(R.id.M_SOS)
        sosButton.setOnClickListener {
            Toast.makeText(this, "긴급 SOS 호출!", Toast.LENGTH_SHORT).show()
        }

        val inputSugar = findViewById<TextInputEditText>(R.id.M_Input_Sugar)
        val inputBodyTemp = findViewById<TextInputEditText>(R.id.M_Input_BodyTemp)
        val submitButton = findViewById<Button>(R.id.M_Input_Submit)

        submitButton?.setOnClickListener {
            handleManualInput(inputSugar, inputBodyTemp)
        }
    }

    private fun handleManualInput(inputSugar: TextInputEditText, inputBodyTemp: TextInputEditText) {

        val silverId = prefsManager.getSilverId()
        if (silverId.isNullOrEmpty()) {
            Toast.makeText(this, "로그인 정보 오류: 사용자 ID를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        val sugarText = inputSugar.text?.toString()
        val tempText = inputBodyTemp.text?.toString()

        val bloodSugar = sugarText?.toIntOrNull()
        val bodyTemperature = tempText?.toDoubleOrNull()

        if (bloodSugar == null && bodyTemperature == null) {
            Toast.makeText(this, "혈당 또는 체온 값을 최소 하나 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val today = LocalDate.now(ZoneId.systemDefault())
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

        val request = DailyHealthLogRequest(
            silverId = silverId,
            bloodSugar = bloodSugar,
            bodyTemperature = bodyTemperature,
            logDate = today.format(dateFormatter)
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = RetrofitClient.healthService.sendDailyHealthLog(request)

                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    if (apiResponse != null && apiResponse.result == "success") {
                        runOnUiThread {
                            Toast.makeText(this@MainPageActivity, "건강 데이터 입력 성공!", Toast.LENGTH_SHORT).show()
                            inputSugar.text?.clear()
                            inputBodyTemp.text?.clear()
                        }
                    } else {
                        val errorMessage = apiResponse?.message ?: "응답 본문에 메시지 없음"
                        runOnUiThread {
                            Toast.makeText(this@MainPageActivity, "입력 실패: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Log.e("MANUAL_INPUT", "수동 입력 API 실패, HTTP Code: ${response.code()}")
                    runOnUiThread {
                        Toast.makeText(this@MainPageActivity, "서버 전송 실패: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MANUAL_INPUT", "수동 입력 API 호출 실패", e)
                runOnUiThread {
                    Toast.makeText(this@MainPageActivity, "네트워크 오류 발생.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}