package com.example.myapplication.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.R
import com.example.myapplication.util.SharedPrefsManager
import com.example.myapplication.activity.YoyangsaActivity
import com.example.myapplication.activity.BohojaActivity
import com.example.myapplication.domain.DailyHealthLogRequest // DTO 경로 확인
import com.example.myapplication.databinding.MainBinding
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.util.KEY_PROFILE_IMAGE_URL
import com.example.myapplication.util.PREFS_NAME
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.example.myapplication.util.BASE_URL
class MainPageActivity : AppCompatActivity() {


    private lateinit var prefsManager: SharedPrefsManager
    private lateinit var binding: MainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefsManager = SharedPrefsManager(this)

        // 사용자 정보 표시
        val username = prefsManager.getUsername()
        val birthday = prefsManager.getBirthday()

        // XML ID에 맞춰 binding 사용
        binding.mAName.text = username
        binding.mAbirthday.text = birthday

        Log.d("MPA", "MainPage loaded. User: $username, birthday: $birthday")

        // 프로필 이미지 로드
        displayProfileImage()

        // 버튼 리스너 설정
        binding.MShowAInfo.setOnClickListener {
            val intent = Intent(this, YoyangsaActivity::class.java)
            startActivity(intent)
        }

        binding.MShowPInfo.setOnClickListener {
            val intent = Intent(this, BohojaActivity::class.java)
            startActivity(intent)
        }

        binding.MSOS.setOnClickListener {
            Toast.makeText(this, "긴급 SOS 호출!", Toast.LENGTH_SHORT).show()
        }

        // 최종 확정된 로그아웃 버튼 ID 사용
        binding.loginOut.setOnClickListener {
            // MainActivity의 정적 startLogout 메서드를 사용한다고 가정
            MainActivity.startLogout(this)
        }

        // 수동 입력 버튼 리스너
        binding.MInputSubmit.setOnClickListener {
            // M_Input_Sugar (혈당), M_Input_BodyTemp (체온)
            handleManualInput(binding.MInputSugar, binding.MInputBodyTemp)
        }
    }

    // 수동 입력 및 서버 통신 로직

    private fun handleManualInput(inputBloodSugar: TextInputEditText, inputBodyTemp: TextInputEditText) {
        // 파라미터 이름을 역할에 맞게 변경 (M_Input_Sugar는 이제 혈당을 의미함)

        val silverId = prefsManager.getSilverId()
        if (silverId.isNullOrEmpty()) {
            Toast.makeText(this, "로그인 정보 오류: 사용자 ID를 찾을 수 없습니다.", Toast.LENGTH_LONG).show()
            return
        }

        // M_Input_Sugar (XML에서 혈당) 값을 bloodSugar로 파싱합니다.
        val bloodSugar = inputBloodSugar.text?.toString()?.toIntOrNull()
        val bodyTemperature = inputBodyTemp.text?.toString()?.toDoubleOrNull()

        if (bloodSugar == null && bodyTemperature == null) {
            Toast.makeText(this, "혈당 또는 체온 값을 최소 하나 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }

        val now = LocalDateTime.now(ZoneId.systemDefault())
        val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

        // DTO 필드에 맞게 정확한 데이터를 전송합니다.
        val request = DailyHealthLogRequest(
            silverId = silverId,
            bloodSugar = bloodSugar, // 혈당 값 전송
            bodyTemperature = bodyTemperature,
            logDate = now.format(dateFormatter)
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // RetrofitClient.healthService의 upsertDailyHealthLog 메서드 호출 가정
                val response = RetrofitClient.healthService.sendDailyHealthLog(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val apiResponse = response.body()

                        if (apiResponse != null && apiResponse.result == "success") {
                            Toast.makeText(this@MainPageActivity, "건강 데이터 입력 성공!", Toast.LENGTH_SHORT).show()
                            inputBloodSugar.text?.clear()
                            inputBodyTemp.text?.clear()
                        } else {
                            val errorMessage = apiResponse?.message ?: "응답 본문에 메시지 없음"
                            Toast.makeText(this@MainPageActivity, "입력 실패: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Log.e("MANUAL_INPUT", "수동 입력 API 실패, HTTP Code: ${response.code()}")
                        Toast.makeText(this@MainPageActivity, "서버 전송 실패: ${response.code()}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MANUAL_INPUT", "수동 입력 API 호출 실패", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainPageActivity, "네트워크 오류 발생.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // 이미지 로딩 로직

    private fun loadProfileImageUrl(): String? {
        val urlFragment = prefsManager.getStoredString(KEY_PROFILE_IMAGE_URL)

        if (urlFragment.isNullOrEmpty()) return null

        return BASE_URL + urlFragment
    }

    private fun displayProfileImage() {
        val imageUrl = loadProfileImageUrl()

        Log.d("ProfileImageCheck", "Final Combined Image URL: $imageUrl")
        // XML에서 silver_image ID 사용
        imageUrl?.let {
            binding.silverImage.load(it) {
                error(R.drawable.noin)
                placeholder(R.drawable.noin)
                transformations(CircleCropTransformation())
            }
        } ?: run {
            binding.silverImage.setImageResource(R.drawable.noin)
        }
    }
}