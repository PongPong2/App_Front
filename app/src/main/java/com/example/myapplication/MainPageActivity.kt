package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class MainPageActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.main)

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
    }
}