package com.example.myapplication.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.KEY_GENDER
import com.example.myapplication.KEY_NAME
import com.example.myapplication.PREFS_NAME
import com.example.myapplication.activity.YoyangsaActivity
import com.example.myapplication.databinding.MainBinding

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
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val userName = sharedPreferences.getString(KEY_NAME, "이름 정보를 불러올 수 없습니다") ?: "이름 없음"
        val Gender = sharedPreferences.getString(KEY_GENDER, "성별 정보를 불러올 수 없습니다") ?: "성별 정보 없음"

        Log.d("MAIN_PAGE_DEBUG", "화면에 설정될 이름: $userName, 성별: $Gender")

        // 화면에 설정
        binding.MAName.text = userName
        binding.MAGender.text = Gender
    }
}