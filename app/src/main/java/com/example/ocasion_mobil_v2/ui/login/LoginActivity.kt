package com.example.ocasion_mobil_v2.ui.login

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.MotionEvent
import com.example.ocasion_mobil_v2.R
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity


/**
 * Pantalla de inicio de sesión — SOLO PARTE VISUAL / FRONTEND.
 *
 * Aquí únicamente se maneja:
 *  - Validaciones básicas de formato en el cliente (campos vacíos, formato de correo).
 *  - Mostrar/ocultar contraseña.
 *  - Estados visuales de carga y error.
 *  - Navegación hacia otras pantallas (registro, recuperar contraseña).
 *
 * La conexión real con el servicio REST de autenticación (POST /login, manejo de
 * token, cabecera Authorization: Bearer, etc.) NO está implementada aquí.
 * Debes conectarla tú (o el equipo de backend) donde se indica con el TODO.
 */
class LoginActivity : ComponentActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvGoToRegister: TextView
    private lateinit var btnLogin: Button
    private lateinit var progressLogin: ProgressBar

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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

    /** Alterna mostrar/ocultar la contraseña al tocar el ícono del ojo. */
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

            mostrarCargando(true)

            // ==========================================================
            // TODO (BACKEND): Aquí va la llamada real al servicio REST.
            //
            // Ejemplo de lo que se conectaría después (NO implementado):
            //   POST /api/auth/login
            //   body: { "email": email, "password": password }
            //
            //   - 200 OK  -> guardar token de sesión (rol de cliente) y
            //                navegar a la pantalla principal.
            //   - 403     -> token/credenciales inválidas -> mostrarError(...)
            //   - error de validación -> leer JSON estandarizado del backend
            //                y mostrar el mensaje correspondiente en tvError.
            // ==========================================================

            // Simulación puramente visual para que el flujo se pueda probar
            // sin backend. Bórralo cuando conectes el servicio real.
            btnLogin.postDelayed({
                mostrarCargando(false)
                Toast.makeText(
                    this,
                    "UI lista. Conecta aquí tu servicio de autenticación.",
                    Toast.LENGTH_SHORT
                ).show()
            }, 1200)
        }
    }

    private fun setupSecondaryActions() {
        tvForgotPassword.setOnClickListener {
            // TODO: navegar a la pantalla de recuperación de contraseña
            Toast.makeText(this, "Ir a recuperar contraseña", Toast.LENGTH_SHORT).show()
        }

        tvGoToRegister.setOnClickListener {
            // TODO: navegar a la pantalla de registro
            Toast.makeText(this, "Ir a pantalla de registro", Toast.LENGTH_SHORT).show()
        }
    }

    /** Validaciones únicamente de formato/campos vacíos, del lado del cliente. */
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
