package com.example.ocasion_mobil_v2.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.example.ocasion_mobil_v2.data.remote.model.ValidateRegisterRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException

class VerifyCodeActivity : ComponentActivity() {

    private lateinit var tvEmailInfo: TextView
    private lateinit var etCodigo: EditText
    private lateinit var tvError: TextView
    private lateinit var btnVerificar: Button
    private lateinit var progressVerificar: ProgressBar
    private lateinit var tvBackToRegister: TextView

    private var email: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_code)

        email = intent.getStringExtra(EXTRA_EMAIL) ?: ""

        bindViews()
        mostrarCorreo()
        setupVerifyButton()
        setupBackToRegister()
    }

    private fun bindViews() {
        tvEmailInfo = findViewById(R.id.tvEmailInfo)
        etCodigo = findViewById(R.id.etCodigo)
        tvError = findViewById(R.id.tvError)
        btnVerificar = findViewById(R.id.btnVerificar)
        progressVerificar = findViewById(R.id.progressVerificar)
        tvBackToRegister = findViewById(R.id.tvBackToRegister)
    }

    private fun mostrarCorreo() {
        if (email.isNotEmpty()) {
            tvEmailInfo.text = "Ingresa el código de 6 dígitos que enviamos a:\n$email"
        }
    }

    private fun setupVerifyButton() {
        btnVerificar.setOnClickListener {
            hideError()

            val codigo = etCodigo.text.toString().trim()

            if (codigo.isEmpty()) {
                mostrarError("Ingresa el código de verificación")
                etCodigo.requestFocus()
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                mostrarError("No se encontró el correo, regresa e intenta de nuevo")
                return@setOnClickListener
            }

            validarCodigo(codigo)
        }
    }

    private fun setupBackToRegister() {
        tvBackToRegister.setOnClickListener {
            finish()
        }
    }

    /**
     * Llama al endpoint real: POST /api/v1/auth/registro-validar
     * Body: { "gmail": "...", "codigo": "..." }
     */
    private fun validarCodigo(codigo: String) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.registroValidar(
                    ValidateRegisterRequest(gmail = email, codigo = codigo)
                )

                mostrarCargando(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@VerifyCodeActivity,
                        "Cuenta verificada correctamente. Ya puedes iniciar sesión.",
                        Toast.LENGTH_LONG
                    ).show()

                    // Limpia todo el stack (Register + VerifyCode) y regresa al login
                    val intent = Intent(this@VerifyCodeActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()

                } else {
                    manejarError(response.code(), response.errorBody()?.string())
                }

            } catch (e: IOException) {
                mostrarCargando(false)
                mostrarError("No se pudo conectar al servidor. Verifica tu conexión VPN.")
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
            400 -> mensajeBackend ?: "Código incorrecto o expirado"
            404 -> mensajeBackend ?: "No se encontró un registro pendiente con ese correo"
            else -> mensajeBackend ?: "Error del servidor ($codigoHttp)"
        }

        mostrarError(mensaje)
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = TextView.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = TextView.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        progressVerificar.visibility = if (cargando) ProgressBar.VISIBLE else ProgressBar.GONE
        btnVerificar.isEnabled = !cargando
        btnVerificar.text = if (cargando) "" else "Verificar código"
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}
