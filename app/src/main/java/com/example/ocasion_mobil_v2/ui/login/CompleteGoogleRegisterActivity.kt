package com.example.ocasion_mobil_v2.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.MainActivity
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.example.ocasion_mobil_v2.data.remote.model.GoogleRegisterRequest
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Se abre cuando alguien inicia sesión con Google por primera vez.
 * El backend (login-google) no encontró la cuenta, así que hay que
 * completar los datos que Google no proporciona (CURP, teléfono, etc.)
 * y mandarlos junto con el mismo idToken a /auth/registro-google.
 */
class CompleteGoogleRegisterActivity : ComponentActivity() {

    private lateinit var etApellidoPaterno: EditText
    private lateinit var etApellidoMaterno: EditText
    private lateinit var etNombreUsuario: EditText
    private lateinit var etCurp: EditText
    private lateinit var etTelefono: EditText
    private lateinit var tvError: TextView
    private lateinit var btnCompletar: Button
    private lateinit var progressCompletar: ProgressBar

    private lateinit var sessionManager: SessionManager
    private var googleIdToken: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_complete_google_register)

        sessionManager = SessionManager(this)
        googleIdToken = intent.getStringExtra(EXTRA_GOOGLE_TOKEN) ?: ""

        bindViews()
        setupCompletarButton()
    }

    private fun bindViews() {
        etApellidoPaterno = findViewById(R.id.etApellidoPaterno)
        etApellidoMaterno = findViewById(R.id.etApellidoMaterno)
        etNombreUsuario = findViewById(R.id.etNombreUsuario)
        etCurp = findViewById(R.id.etCurp)
        etTelefono = findViewById(R.id.etTelefono)
        tvError = findViewById(R.id.tvError)
        btnCompletar = findViewById(R.id.btnCompletar)
        progressCompletar = findViewById(R.id.progressCompletar)
    }

    private fun setupCompletarButton() {
        btnCompletar.setOnClickListener {
            hideError()

            val apellidoPaterno = etApellidoPaterno.text.toString().trim()
            val apellidoMaterno = etApellidoMaterno.text.toString().trim()
            val nombreUsuario = etNombreUsuario.text.toString().trim()
            val curp = etCurp.text.toString().trim().uppercase()
            val telefono = etTelefono.text.toString().trim()

            if (!validarFormulario(apellidoPaterno, apellidoMaterno, nombreUsuario, curp, telefono)) {
                return@setOnClickListener
            }

            if (googleIdToken.isEmpty()) {
                mostrarError("Se perdió la sesión de Google, intenta de nuevo desde el login")
                return@setOnClickListener
            }

            completarRegistro(
                GoogleRegisterRequest(
                    token = googleIdToken,
                    apellidoPaterno = apellidoPaterno,
                    apellidoMaterno = apellidoMaterno,
                    nombreUser = nombreUsuario,
                    curp = curp,
                    telefono = telefono
                )
            )
        }
    }

    private fun completarRegistro(request: GoogleRegisterRequest) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.registroGoogle(request)

                mostrarCargando(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        sessionManager.guardarToken(body.token)
                        val intent = Intent(this@CompleteGoogleRegisterActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        mostrarError("Respuesta vacía del servidor")
                    }
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
        mostrarError(mensajeBackend ?: "No se pudo completar el registro ($codigoHttp)")
    }

    private fun validarFormulario(
        apellidoPaterno: String,
        apellidoMaterno: String,
        nombreUsuario: String,
        curp: String,
        telefono: String
    ): Boolean {
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
        if (telefono.length != 10) {
            mostrarError("El teléfono debe tener 10 dígitos"); etTelefono.requestFocus(); return false
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
        progressCompletar.visibility = if (cargando) ProgressBar.VISIBLE else ProgressBar.GONE
        btnCompletar.isEnabled = !cargando
        btnCompletar.text = if (cargando) "" else "Completar registro"
    }

    companion object {
        const val EXTRA_GOOGLE_TOKEN = "extra_google_token"
    }
}
