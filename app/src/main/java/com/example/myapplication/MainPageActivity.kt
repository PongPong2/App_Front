package com.example.myapplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.databinding.MainBinding

// Constants.kt 파일의 상수를 사용한다고 가정
// PREFS_NAME, KEY_NAME, KEY_GENDER 상수가 같은 패키지 내에 정의되어 있어야 합니다.
// (생략된 상수 정의가 있다고 가정)

class MainPageActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setUserInfoFromSharedPreferences()

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

        binding.loginOut.setOnClickListener {
            MainActivity.startLogout(this)
        }
    }

    private fun setUserInfoFromSharedPreferences() {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val Name = sharedPreferences.getString(KEY_NAME, "이름 정보를 불러올 수 없습니다") ?: "이름 없음"
        val Gender = sharedPreferences.getString(KEY_GENDER, "성별 정보를 불러올 수 없습니다") ?: "성별 정보 없음"

        Log.d("MAIN_PAGE_DEBUG", "화면에 설정될 이름: $Name, 성별: $Gender")

        // 화면에 설정
        binding.MAName.text = Name
        binding.MAGender.text = Gender
    }
}