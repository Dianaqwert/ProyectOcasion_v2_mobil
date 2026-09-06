package com.example.ocasion_mobil_v2.ui.login

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.example.ocasion_mobil_v2.data.remote.model.ResetPasswordRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException
/**
 * Pantalla para ingresar la nueva contraseña.
 *
 * ⚠️ IMPORTANTE: esta pantalla NO tiene todavía conexión real al backend.
 *
 * Antes de poder terminar esto, pregunta al equipo de backend:
 *   1) ¿Cuál es la URL del endpoint para confirmar el cambio de contraseña?
 *   2) ¿Qué campos espera en el body? (ej. "email", "newPassword")
 *   3) ¿Qué responde en éxito (200) y en error?
 *
 * Mientras tanto, la UI ya valida los campos localmente y está lista:
 * solo falta reemplazar el bloque marcado con TODO por la llamada real,
 * usando el mismo patrón que ya ves en LoginActivity/RegisterActivity
 * (RetrofitClient.apiService.tuMetodoNuevo(...) dentro de lifecycleScope.launch).
 */
class NewPasswordActivity : ComponentActivity() {

    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivToggleNewPassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var btnChangePassword: Button
    private lateinit var progressChangePassword: ProgressBar
    private lateinit var tvBackToLogin: TextView

    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    // Almacenaremos el token extraído de la URL
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_password)

        // Capturar el Deep Link (URL) y extraer el token
        val uri = intent.data
        if (uri != null) {
            // Asume que el correo envía un link como: http://172.30.120.90:8080/reset-password?token=TUTOKEN
            token = uri.getQueryParameter("token")
        }

        bindViews()
        setupPasswordToggles()
        setupChangeButton()
        setupBackToLogin()

        // Si por error alguien abre la vista sin token, deshabilitar el botón
        if (token.isNullOrEmpty()) {
            mostrarError("Enlace inválido o expirado. Vuelve a solicitar el cambio de contraseña.")
            btnChangePassword.isEnabled = false
        }
    }

    private fun bindViews() {
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        tvError = findViewById(R.id.tvError)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        progressChangePassword = findViewById(R.id.progressChangePassword)
        tvBackToLogin = findViewById(R.id.tvBackToLogin)
    }

    private fun setupPasswordToggles() {
        ivToggleNewPassword.setOnClickListener {
            isNewPasswordVisible = !isNewPasswordVisible
            togglePasswordField(etNewPassword, isNewPasswordVisible)
            ivToggleNewPassword.setImageResource(
                if (isNewPasswordVisible) android.R.drawable.ic_menu_close_clear_cancel
                else android.R.drawable.ic_menu_view
            )
        }

        ivToggleConfirmPassword.setOnClickListener {
            isConfirmPasswordVisible = !isConfirmPasswordVisible
            togglePasswordField(etConfirmPassword, isConfirmPasswordVisible)
            ivToggleConfirmPassword.setImageResource(
                if (isConfirmPasswordVisible) android.R.drawable.ic_menu_close_clear_cancel
                else android.R.drawable.ic_menu_view
            )
        }
    }

    private fun togglePasswordField(field: EditText, visible: Boolean) {
        val cursorPosition = field.selectionStart
        field.inputType = if (visible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        field.setSelection(cursorPosition)
    }

    private fun setupChangeButton() {
        btnChangePassword.setOnClickListener {
            hideError()

            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (!validarFormulario(newPassword, confirmPassword)) return@setOnClickListener

            if (token != null) {
                cambiarPasswordEnBackend(token!!, newPassword)
            }
        }
    }

    private fun cambiarPasswordEnBackend(tokenExtraido: String, nuevaPass: String) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val request = ResetPasswordRequest(
                    token = tokenExtraido,
                    nuevaPassword = nuevaPass
                )
                val response = RetrofitClient.apiService.resetPassword(request)

                mostrarCargando(false)

                if (response.isSuccessful) {
                    Toast.makeText(this@NewPasswordActivity, "Contraseña actualizada exitosamente", Toast.LENGTH_LONG).show()

                    // Volver al Login limpiando el stack de actividades
                    // Asegúrate de cambiar 'LoginActivity::class.java' por el nombre real de tu clase de Login
                    val intent = Intent(this@NewPasswordActivity, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    manejarError(response.code(), response.errorBody()?.string())
                }
            } catch (e: IOException) {
                mostrarCargando(false)
                mostrarError("No se pudo conectar al servidor. Verifica tu conexión.")
            } catch (e: Exception) {
                mostrarCargando(false)
                mostrarError("Ocurrió un error: ${e.localizedMessage}")
            }
        }
    }

    private fun manejarError(codigoHttp: Int, errorBodyJson: String?) {
        val mensajeBackend = try {
            errorBodyJson?.let {
                val error = Gson().fromJson(it, ApiErrorResponse::class.java)
                error.detalle ?: error.message
            }
        } catch (e: Exception) { null }

        mostrarError(mensajeBackend ?: "Ocurrió un error al intentar cambiar la contraseña ($codigoHttp)")
    }
    private fun setupBackToLogin() {
        tvBackToLogin.setOnClickListener {
            finish()
        }
    }

    private fun validarFormulario(newPassword: String, confirmPassword: String): Boolean {
        if (newPassword.length < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres")
            etNewPassword.requestFocus()
            return false
        }
        if (newPassword != confirmPassword) {
            mostrarError("Las contraseñas no coinciden")
            etConfirmPassword.requestFocus()
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
        progressChangePassword.visibility = if (cargando) ProgressBar.VISIBLE else ProgressBar.GONE
        btnChangePassword.isEnabled = !cargando
        btnChangePassword.text = if (cargando) "" else "Cambiar contraseña"
    }

    companion object {
        const val EXTRA_EMAIL = "extra_email"
    }
}