package com.example.ocasion_mobil_v2.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.MainActivity
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.example.ocasion_mobil_v2.data.remote.model.LoginRequest
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException
import com.example.ocasion_mobil_v2.ui.login.RegisterActivity

class LoginActivity : ComponentActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvGoToRegister: TextView
    private lateinit var btnLogin: Button
    private lateinit var progressLogin: ProgressBar

    private lateinit var sessionManager: SessionManager
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        bindViews()
        setupPasswordToggle()
        setupLoginButton()
        setupSecondaryActions()
    }

    private fun bindViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvError = findViewById(R.id.tvError)
        tvForgotPassword = findViewById(R.id.tvForgotPassword)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
        btnLogin = findViewById(R.id.btnLogin)
        progressLogin = findViewById(R.id.progressLogin)
    }

    private fun setupPasswordToggle() {
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            val cursorPosition = etPassword.selectionStart

            etPassword.inputType = if (isPasswordVisible) {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }

            ivTogglePassword.setImageResource(
                if (isPasswordVisible) android.R.drawable.ic_menu_close_clear_cancel
                else android.R.drawable.ic_menu_view
            )

            etPassword.setSelection(cursorPosition)
        }
    }

    private fun setupLoginButton() {
        btnLogin.setOnClickListener {
            hideError()

            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!validarFormulario(email, password)) return@setOnClickListener

            realizarLogin(email, password)
        }
    }

    private fun setupSecondaryActions() {
        tvForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    /**
     * Llama al endpoint real: POST /api/v1/auth/login
     * Body: { "gmail": "...", "password": "..." }
     * Respuesta 200: { "token": "..." }
     */
    private fun realizarLogin(email: String, password: String) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(gmail = email, password = password)
                )

                mostrarCargando(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        sessionManager.guardarToken(body.token)
                        irAPantallaPrincipal()
                    } else {
                        mostrarError("Respuesta vacía del servidor")
                    }
                } else {
                    manejarError(response.code(), response.errorBody()?.string())
                }

            } catch (e: IOException) {
                // Sin conexión / VPN desconectada / servidor no alcanzable
                mostrarCargando(false)
                mostrarError("No se pudo conectar al servidor. Verifica tu conexión VPN.")
            } catch (e: Exception) {
                mostrarCargando(false)
                mostrarError("Ocurrió un error inesperado: ${e.localizedMessage}")
            }
        }
    }

    /** Interpreta el código de error HTTP y el JSON de error del backend. */
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
            403 -> mensajeBackend ?: "Credenciales inválidas o sesión no autorizada"
            401 -> mensajeBackend ?: "Correo o contraseña incorrectos"
            400 -> mensajeBackend ?: "Datos inválidos, revisa el formulario"
            else -> mensajeBackend ?: "Error del servidor ($codigoHttp)"
        }

        mostrarError(mensaje)
    }

    private fun irAPantallaPrincipal() {
        Toast.makeText(this, "Login exitoso ✅", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun validarFormulario(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            mostrarError("Ingresa tu correo electrónico")
            etEmail.requestFocus()
            return false
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("El formato del correo no es válido")
            etEmail.requestFocus()
            return false
        }
        if (password.isEmpty()) {
            mostrarError("Ingresa tu contraseña")
            etPassword.requestFocus()
            return false
        }
        if (password.length < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres")
            etPassword.requestFocus()
            return false
        }
        return true
    }

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = TextView.VISIBLE
    }

    private fun hideError() {
        tvError.visibility = TextView.GONE
    }

    private fun mostrarCargando(cargando: Boolean) {
        progressLogin.visibility = if (cargando) ProgressBar.VISIBLE else ProgressBar.GONE
        btnLogin.isEnabled = !cargando
        btnLogin.text = if (cargando) "" else getString(R.string.login_button_text)
    }

}
