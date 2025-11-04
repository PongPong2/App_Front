package com.example.myapplication.activity

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.R

class BohojaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.bohoja)

        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        supportActionBar?.let {
            it.title = "보호자 정보"
            it.setDisplayHomeAsUpEnabled(true)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}