package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class YoyangsaActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.yoyangsa)

        val backButton = findViewById<Button>(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }
    }
}