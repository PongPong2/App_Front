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
import com.example.myapplication.R
import com.example.myapplication.api.RetrofitClient // BASE_URL 사용
import kotlinx.coroutines.launch
import com.example.myapplication.data_model.GuardianResponse
import com.example.myapplication.util.SharedPrefsManager
import com.example.myapplication.util.BASE_URL

class BohojaActivity : AppCompatActivity() {

    private lateinit var name1: TextView
    private lateinit var addr1: TextView
    private lateinit var rel1: TextView
    private lateinit var tel1: TextView
    private lateinit var name2: TextView
    private lateinit var addr2: TextView
    private lateinit var rel2: TextView
    private lateinit var tel2: TextView
    private lateinit var name3: TextView
    private lateinit var addr3: TextView
    private lateinit var rel3: TextView
    private lateinit var tel3: TextView

    private lateinit var profile1: ImageView
    private lateinit var profile2: ImageView
    private lateinit var profile3: ImageView

    // RetrofitClient.kt에 정의된 IP 주소를 사용합니다.
    private val BASE_URL_FOR_IMAGES = BASE_URL.trimEnd('/')

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.bohoja)

        setupViews()

        val prefs = SharedPrefsManager(this)
        val savedSilverId = prefs.getSilverId()

        if (savedSilverId != null) {
            fetchGuardianData(savedSilverId)
        } else {
            Toast.makeText(this, "로그인 정보가 없습니다. 다시 로그인해주세요.", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun setupViews() {
        name1 = findViewById(R.id.M_G_Name1)
        addr1 = findViewById(R.id.M_G_Addr1)
        rel1 = findViewById(R.id.M_G_Relation1)
        tel1 = findViewById(R.id.M_G_Tel1)
        name2 = findViewById(R.id.M_G_Name2)
        addr2 = findViewById(R.id.M_G_Addr2)
        rel2 = findViewById(R.id.M_G_Relation2)
        tel2 = findViewById(R.id.M_G_Tel2)
        name3 = findViewById(R.id.M_G_Name3)
        addr3 = findViewById(R.id.M_G_Addr3)
        rel3 = findViewById(R.id.M_G_Relation3)
        tel3 = findViewById(R.id.M_G_Tel3)

        profile1 = findViewById(R.id.bohojaProfile1)
        profile2 = findViewById(R.id.bohojaProfile2)
        profile3 = findViewById(R.id.bohojaProfile3)

        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        supportActionBar?.let {
            it.title = "보호자 정보"
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun fetchGuardianData(silverId: String) {
        lifecycleScope.launch {
            try {
                val guardianList = RetrofitClient.guardianApiService.getGuardians(silverId)
                updateUI(guardianList)

            } catch (e: Exception) {
                Log.e("BohojaActivity", "API 호출 실패: ${e.message}", e)
                Toast.makeText(this@BohojaActivity, "데이터 로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateUI(guardians: List<GuardianResponse>) {

        // 보호자 1
        if (guardians.isNotEmpty()) {
            val g1 = guardians[0]
            name1.text = g1.name
            addr1.text = g1.address
            rel1.text = g1.relationship
            tel1.text = g1.tel

            val relativePath = g1.profileImageUrl

            if (!relativePath.isNullOrEmpty()) {

                // [!! 경로 중복 및 결합 로직 !!]
                // 1. 중복된 슬래시를 하나로 정리 (예: //uploads//uploads/ -> /uploads/uploads/)
                val cleanedPath = relativePath.replace(Regex("/+"), "/")

                // 2. BASE_URL과 결합 시, 경로 중복을 방지하며 IP를 추가합니다.
                //    (BASE_URL/uploads/uploads/ 로 되는 경우, 첫번째 /uploads/를 제거)
                val finalPath = if (cleanedPath.startsWith("/uploads/uploads/")) {
                    cleanedPath.substring("/uploads".length) // /uploads/uploads/ -> /uploads/
                } else {
                    cleanedPath
                }

                val fullImageUrl = BASE_URL_FOR_IMAGES + finalPath

                Log.d("BohojaActivity", "Loading image for Guardian 1 (Combined): $fullImageUrl")

                profile1.load(fullImageUrl) {
                    placeholder(R.drawable.girl)
                    error(R.drawable.girl)
                }
            } else {
                Log.d("BohojaActivity", "No image URL found for Guardian 1. Using default.")
                profile1.setImageResource(R.drawable.girl)
            }
        }

        // 보호자 2
        if (guardians.size >= 2) {
            val g2 = guardians[1]
            name2.text = g2.name
            addr2.text = g2.address
            rel2.text = g2.relationship
            tel2.text = g2.tel

            val relativePath = g2.profileImageUrl

            if (!relativePath.isNullOrEmpty()) {

                val cleanedPath = relativePath.replace(Regex("/+"), "/")
                val finalPath = if (cleanedPath.startsWith("/uploads/uploads/")) {
                    cleanedPath.substring("/uploads".length)
                } else {
                    cleanedPath
                }

                val fullImageUrl = BASE_URL_FOR_IMAGES + finalPath

                Log.d("BohojaActivity", "Loading image for Guardian 2 (Combined): $fullImageUrl")

                profile2.load(fullImageUrl) {
                    placeholder(R.drawable.men)
                    error(R.drawable.men)
                }
            } else {
                Log.d("BohojaActivity", "No image URL found for Guardian 2. Using default.")
                profile2.setImageResource(R.drawable.men)
            }
        }

        // 보호자 3
        if (guardians.size >= 3) {
            val g3 = guardians[2]
            name3.text = g3.name
            addr3.text = g3.address
            rel3.text = g3.relationship
            tel3.text = g3.tel

            val relativePath = g3.profileImageUrl

            if (!relativePath.isNullOrEmpty()) {

                val cleanedPath = relativePath.replace(Regex("/+"), "/")
                val finalPath = if (cleanedPath.startsWith("/uploads/uploads/")) {
                    cleanedPath.substring("/uploads".length)
                } else {
                    cleanedPath
                }

                val fullImageUrl = BASE_URL_FOR_IMAGES + finalPath

                Log.d("BohojaActivity", "Loading image for Guardian 3 (Combined): $fullImageUrl")

                profile3.load(fullImageUrl) {
                    placeholder(R.drawable.girl2)
                    error(R.drawable.girl2)
                }
            } else {
                Log.d("BohojaActivity", "No image URL found for Guardian 3. Using default.")
                profile3.setImageResource(R.drawable.girl2)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}