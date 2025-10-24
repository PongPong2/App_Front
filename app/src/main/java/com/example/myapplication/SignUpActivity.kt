package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SignUpActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.signup)

        val backButton: Button = findViewById(R.id.btn_back)
        backButton.setOnClickListener {
            finish()
        }

        val signUpButton: Button = findViewById(R.id.btn_signup)
        signUpButton.setOnClickListener {
            finish()
        }
    }
}
