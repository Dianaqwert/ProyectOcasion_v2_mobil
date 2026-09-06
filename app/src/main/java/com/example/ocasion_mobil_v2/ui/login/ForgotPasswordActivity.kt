package com.example.ocasion_mobil_v2.ui.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException

class ForgotPasswordActivity : ComponentActivity() {

    private lateinit var etEmail: EditText
    private lateinit var tvError: TextView
    private lateinit var tvSuccess: TextView
    private lateinit var btnSendRecovery: Button
    private lateinit var progressRecovery: ProgressBar
    private lateinit var tvBackToLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.forgot_password)

        bindViews()
        setupSendButton()
        setupBackToLogin()
    }

    private fun bindViews() {
        etEmail = findViewById(R.id.etEmail)
        tvError = findViewById(R.id.tvError)
        tvSuccess = findViewById(R.id.tvSuccess)
        btnSendRecovery = findViewById(R.id.btnSendRecovery)
        progressRecovery = findViewById(R.id.progressRecovery)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    private fun setupSendButton() {
        btnSendRecovery.setOnClickListener {
            hideMessages()

            val email = etEmail.text.toString().trim()

            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                mostrarError("Ingresa un correo válido")
                etEmail.requestFocus()
                return@setOnClickListener
            }

            solicitarCodigo(email)
        }
    }

    private fun setupBackToLogin() {
        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    /**
     * Llama al endpoint real: POST /api/v1/auth/reset-password?email=...
     * Este endpoint solo dispara el envío del correo con el código.
     * El siguiente paso (confirmar código + nueva contraseña) se hace
     * en NewPasswordActivity, que todavía no tiene endpoint conectado
     * (ver TODO ahí).
     */
    private fun solicitarCodigo(email: String) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.solicitarResetPassword(email)
                mostrarCargando(false)

                if (response.isSuccessful) {
                    // Solo mostramos el mensaje. El usuario cerrará la app para ir a su correo.
                    mostrarExito("Enlace enviado. Por favor, revisa tu correo (incluyendo bandeja de Spam) para continuar.")
                    btnSendRecovery.isEnabled = false // Evitar múltiples envíos
                } else {
                    manejarError(response.code(), response.errorBody()?.string())
                }
            } catch (e: IOException) {
                mostrarCargando(false)
                mostrarError("No se pudo conectar al servidor. Verifica tu conexión.")
            } catch (e: Exception) {
                mostrarCargando(false)
                mostrarError("Ocurrió un error inesperado: ${e.localizedMessage}")
            }
        }
    }

    private fun manejarError(codigoHttp: Int, errorBodyJson: String?) {
        val mensajeBackend = try {
            errorBodyJson?.let {
                val error = Gson().fromJson(it, ApiErrorResponse::class.java)
                error.detalle ?: error.message
            }
        } catch (e: Exception) {
            null
        }

        val mensaje = when (codigoHttp) {
            404 -> mensajeBackend ?: "No existe una cuenta con ese correo"
            else -> mensajeBackend ?: "Error del servidor ($codigoHttp)"
        }

        mostrarError(mensaje)
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = TextView.VISIBLE
    }

    private fun mostrarExito(mensaje: String) {
        tvSuccess.text = mensaje
        tvSuccess.visibility = TextView.VISIBLE
    }

    private fun hideMessages() {
        tvError.visibility = TextView.GONE
        tvSuccess.visibility = TextView.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        progressRecovery.visibility = if (cargando) ProgressBar.VISIBLE else ProgressBar.GONE
        btnSendRecovery.isEnabled = !cargando
        btnSendRecovery.text = if (cargando) "" else "Enviar enlace de recuperación"
    }
}
