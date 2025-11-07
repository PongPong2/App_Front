package com.example.myapplication.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import coil.load
import coil.transform.CircleCropTransformation
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient
import com.example.myapplication.data_model.CaregiverResponse
import com.example.myapplication.util.BASE_URL
import com.example.myapplication.util.SharedPrefsManager
import kotlinx.coroutines.launch

class YoyangsaActivity : AppCompatActivity() {

    // 1. 뷰 변수 선언 (BohojaActivity와 동일 스타일)
    private lateinit var btnBack: Button
    private lateinit var mAName: TextView
    private lateinit var mAGender: TextView
    private lateinit var mATel: TextView
    private lateinit var mACenter: TextView
    private lateinit var ivProfilePicture1: ImageView

    private val BASE_URL_FOR_IMAGES = BASE_URL.trimEnd('/')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.yoyangsa)

        setupViews()

        // 2. BohojaActivity처럼 SharedPrefsManager에서 '보호대상자 ID'를 가져옵니다.
        val prefs = SharedPrefsManager(this)
        val savedSilverId = prefs.getSilverId() // "ppp1234" 같은 ID를 가져옴

        // 3. 보호대상자 ID 유효성 검사
        if (savedSilverId.isNullOrEmpty()) {
            Log.e("YoyangsaActivity", "SharedPrefs에 silver_id가 없습니다.")
            Toast.makeText(this, "보호대상자 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
        } else {
            // 4. 보호대상자 ID로 요양사 데이터 로드 함수 호출
            fetchCaregiverData(savedSilverId)
        }
    }

    /**
     * 뷰 초기화 및 버튼 리스너 설정
     */
    private fun setupViews() {
        btnBack = findViewById(R.id.btn_back)
        mAName = findViewById(R.id.M_A_name)
        mAGender = findViewById(R.id.M_A_Gender)
        mATel = findViewById(R.id.M_A_Tel)
        mACenter = findViewById(R.id.M_A_Center)
        ivProfilePicture1 = findViewById(R.id.iv_profile_picture1)

        btnBack.setOnClickListener {
            finish()
        }

        supportActionBar?.let {
            it.title = "요양사 정보"
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    /**
     * 🚨 Coroutine으로 '보호대상자 ID'를 이용해 '요양사 정보'를 가져옵니다.
     * (String을 받도록 변경)
     */
    private fun fetchCaregiverData(silverLoginId: String) { // 🚨 파라미터가 String으로 변경
        lifecycleScope.launch {
            try {
                // 🚨 새로 만든 API 함수(getCaregiverBySilverId)를 호출합니다.
                val caregiverData = RetrofitClient.apiService.getCaregiverBySilverId(silverLoginId)
                updateUI(caregiverData)

            } catch (e: Exception) {
                Log.e("YoyangsaActivity", "API 호출 실패: ${e.message}", e)
                Toast.makeText(this@YoyangsaActivity, "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * API 응답 데이터를 뷰에 바인딩합니다.
     * (Coil 및 URL 처리 로직 적용)
     */
    private fun updateUI(caregiver: CaregiverResponse) {
        mAName.text = caregiver.name
        mAGender.text = when (caregiver.gender) {
            "M" -> "남성"
            "F" -> "여성"
            else -> "정보 없음"
        }
        mATel.text = caregiver.tel
        mACenter.text = caregiver.affiliation

        val relativePath = caregiver.profileImageUrl

        if (!relativePath.isNullOrEmpty()) {
            val cleanedPath = relativePath.replace(Regex("/+"), "/")
            val finalPath = if (cleanedPath.startsWith("/uploads/uploads/")) {
                cleanedPath.substring("/uploads".length)
            } else {
                cleanedPath
            }

            val fullImageUrl = BASE_URL_FOR_IMAGES + finalPath
            Log.d("YoyangsaActivityImage", "Loading image (Combined): $fullImageUrl")

            ivProfilePicture1.load(fullImageUrl) {
                placeholder(R.drawable.yoyangsa)
                error(R.drawable.yoyangsa)
                transformations(CircleCropTransformation())
            }
        } else {
            Log.d("YoyangsaActivityImage", "No image URL found. Using default.")
            ivProfilePicture1.setImageResource(R.drawable.yoyangsa)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}