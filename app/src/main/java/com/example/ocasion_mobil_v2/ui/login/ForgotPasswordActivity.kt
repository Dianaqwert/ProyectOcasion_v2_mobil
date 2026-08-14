package com.example.ocasion_mobil_v2.ui.login

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R

class ForgotPasswordActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.forgot_password)

        val tvBackToLogin = findViewById<android.widget.TextView>(
            R.id.tvBackToLogin
        )

        tvBackToLogin.setOnClickListener {
            finish()
        }
    }
}