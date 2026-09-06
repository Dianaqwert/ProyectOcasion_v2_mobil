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
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.example.ocasion_mobil_v2.data.remote.model.RegisterRequest
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException

class RegisterActivity : ComponentActivity() {

    private lateinit var etNombres: EditText
    private lateinit var etApellidoPaterno: EditText
    private lateinit var etApellidoMaterno: EditText
    private lateinit var etNombreUsuario: EditText
    private lateinit var etCurp: EditText
    private lateinit var etEmail: EditText
    private lateinit var etTelefono: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var tvGoToLogin: TextView
    private lateinit var btnRegister: Button
    private lateinit var progressRegister: ProgressBar

    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        bindViews()
        setupPasswordToggle()
        setupRegisterButton()
        setupGoToLogin()
    }

    private fun bindViews() {
        etNombres = findViewById(R.id.etNombres)
        etApellidoPaterno = findViewById(R.id.etApellidoPaterno)
        etApellidoMaterno = findViewById(R.id.etApellidoMaterno)
        etNombreUsuario = findViewById(R.id.etNombreUsuario)
        etCurp = findViewById(R.id.etCurp)
        etEmail = findViewById(R.id.etEmail)
        etTelefono = findViewById(R.id.etTelefono)
        etPassword = findViewById(R.id.etPassword)
        ivTogglePassword = findViewById(R.id.ivTogglePassword)
        tvError = findViewById(R.id.tvError)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        btnRegister = findViewById(R.id.btnRegister)
        progressRegister = findViewById(R.id.progressRegister)
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

    private fun setupRegisterButton() {
        btnRegister.setOnClickListener {
            hideError()

            val nombres = etNombres.text.toString().trim()
            val apellidoPaterno = etApellidoPaterno.text.toString().trim()
            val apellidoMaterno = etApellidoMaterno.text.toString().trim()
            val nombreUsuario = etNombreUsuario.text.toString().trim()
            val curp = etCurp.text.toString().trim().uppercase()
            val email = etEmail.text.toString().trim()
            val telefono = etTelefono.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (!validarFormulario(
                    nombres, apellidoPaterno, apellidoMaterno,
                    nombreUsuario, curp, email, telefono, password
                )
            ) return@setOnClickListener

            realizarRegistro(
                email,
                RegisterRequest(
                    nombre = nombres,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    nombreUser = nombreUsuario,
                    curp = curp,
                    gmail = email,
                    telefono = telefono,
                    password = password
                )
            )
        }
    }

    private fun setupGoToLogin() {
        tvGoToLogin.setOnClickListener {
            finish() // regresa a LoginActivity
        }
    }

    /**
     * Llama al endpoint real: POST /api/v1/auth/registro
     * Si sale bien, el backend manda un código al correo y navegamos
     * a VerifyCodeActivity para que el usuario lo confirme.
     */
    private fun realizarRegistro(email: String, request: RegisterRequest) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.registro(request)

                mostrarCargando(false)

                if (response.isSuccessful) {
                    Toast.makeText(
                        this@RegisterActivity,
                        "Te enviamos un código de verificación a tu correo.",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(this@RegisterActivity, VerifyCodeActivity::class.java)
                    intent.putExtra(VerifyCodeActivity.EXTRA_EMAIL, email)
                    startActivity(intent)

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
            409 -> mensajeBackend ?: "Ese correo, usuario o CURP ya está registrado"
            400 -> mensajeBackend ?: "Datos inválidos, revisa el formulario"
            else -> mensajeBackend ?: "Error del servidor ($codigoHttp)"
        }

        mostrarError(mensaje)
    }

    private fun validarFormulario(
        nombres: String,
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombreUsuario: String,
        curp: String,
        email: String,
        telefono: String,
        password: String
    ): Boolean {
        if (nombres.isEmpty()) {
            mostrarError("Ingresa tu nombre"); etNombres.requestFocus(); return false
        }
        if (apellidoPaterno.isEmpty()) {
            mostrarError("Ingresa tu apellido paterno"); etApellidoPaterno.requestFocus(); return false
        }
        if (apellidoMaterno.isEmpty()) {
            mostrarError("Ingresa tu apellido materno"); etApellidoMaterno.requestFocus(); return false
        }
        if (nombreUsuario.isEmpty()) {
            mostrarError("Ingresa un nombre de usuario"); etNombreUsuario.requestFocus(); return false
        }
        if (curp.length != 18) {
            mostrarError("El CURP debe tener 18 caracteres"); etCurp.requestFocus(); return false
        }
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            mostrarError("Ingresa un correo válido"); etEmail.requestFocus(); return false
        }
        if (telefono.length != 10) {
            mostrarError("El teléfono debe tener 10 dígitos"); etTelefono.requestFocus(); return false
        }
        if (password.length < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres"); etPassword.requestFocus(); return false
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
        progressRegister.visibility = if (cargando) ProgressBar.VISIBLE else ProgressBar.GONE
        btnRegister.isEnabled = !cargando
        btnRegister.text = if (cargando) "" else "Registrarse"
    }
}
