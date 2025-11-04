package com.example.myapplication.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.PREFS_NAME
import com.example.myapplication.R
import com.example.myapplication.databinding.MainBinding
import coil.load

private val MainPageActivity.KEY_PROFILE_IMAGE_URL: String
    get() = "user_profile_image_url"

class MainPageActivity : AppCompatActivity() {

    private lateinit var binding: MainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 💡 프로필 이미지 로딩 시작 (URL이 있으면 로드, 없으면 기본 이미지)
        displayProfileImage()

        // 💡 '요양사 정보' 버튼 리스너
        binding.MShowAInfo.setOnClickListener {
            val intent = Intent(this, YoyangsaActivity::class.java)
            startActivity(intent)
        }

        // 💡 '보호자 정보' 버튼 리스너
        binding.MShowPInfo.setOnClickListener {
            val intent = Intent(this, BohojaActivity::class.java)
            startActivity(intent)
        }

        // 💡 'SOS' 버튼 리스너
        binding.MSOS.setOnClickListener {
            Toast.makeText(this, "긴급 SOS 호출!", Toast.LENGTH_SHORT).show()
        }

        // 💡 로그아웃 버튼 리스너
        binding.loginOut.setOnClickListener {
            MainActivity.startLogout(this)
        }
    }

    // --- 프로필 이미지 로딩 로직 ---

    // 💡 SharedPreferences에서 저장된 이미지 URL을 가져오는 함수
    private fun loadProfileImageUrl(): String? {
        val sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(KEY_PROFILE_IMAGE_URL, null)
    }

    // 💡 Coil 라이브러리를 사용해 ImageView에 이미지를 로드하고 처리하는 함수
    private fun displayProfileImage() {
        val imageUrl = loadProfileImageUrl()

        imageUrl?.let {
            // 💡 Coil의 load 함수를 사용해 URL 이미지 로드
            binding.silverImage.load(it) {
                error(R.drawable.noin)  // 로딩 실패 시 기본 이미지
                placeholder(R.drawable.noin) // 로딩 중 표시할 이미지
                // transform(CircleCropTransformation())
            }
        } ?: run {
            // 💡 URL이 없을 경우, 기본 리소스 이미지 설정
            binding.silverImage.setImageResource(R.drawable.noin)
        }
    }
}