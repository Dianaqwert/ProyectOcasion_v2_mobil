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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.MainActivity
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ApiErrorResponse
import com.example.ocasion_mobil_v2.data.remote.model.GoogleLoginRequest
import com.example.ocasion_mobil_v2.data.remote.model.LoginRequest
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.ui.cliente.ClientPrincipalMenu
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.gson.Gson
import kotlinx.coroutines.launch
import java.io.IOException
import com.example.ocasion_mobil_v2.ui.propietario.OwnerHomeActivity
import com.example.ocasion_mobil_v2.util.JwtUtils

class LoginActivity : ComponentActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var ivTogglePassword: ImageView
    private lateinit var tvError: TextView
    private lateinit var tvForgotPassword: TextView
    private lateinit var tvGoToRegister: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnGoogleSignIn: Button
    private lateinit var progressLogin: ProgressBar

    private lateinit var sessionManager: SessionManager
    private lateinit var googleSignInClient: GoogleSignInClient
    private var isPasswordVisible = false

    /** Maneja el resultado del intent de selección de cuenta de Google. */
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account.idToken

            if (idToken != null) {
                realizarLoginGoogle(idToken)
            } else {
                mostrarError("No se pudo obtener el token de Google")
            }
        } catch (e: ApiException) {
            mostrarError("Falló el inicio de sesión con Google (código ${e.statusCode})")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        sessionManager = SessionManager(this)

        bindViews()
        setupGoogleSignInClient()
        setupPasswordToggle()
        setupLoginButton()
        setupGoogleButton()
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
        btnGoogleSignIn = findViewById(R.id.btnGoogleSignIn)
        progressLogin = findViewById(R.id.progressLogin)
    }

    /**
     * IMPORTANTE: reemplaza R.string.google_server_client_id por tu propio
     * "Web client ID" generado en Google Cloud Console (ver instrucciones
     * que te pasé aparte). Sin ese ID configurado, el botón de Google
     * truena o nunca regresa un idToken válido.
     */
    private fun setupGoogleSignInClient() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.google_server_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
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

    private fun setupGoogleButton() {
        btnGoogleSignIn.setOnClickListener {
            hideError()
            // Cierra cualquier sesión previa de Google para forzar que
            // siempre aparezca el selector de cuenta.
            googleSignInClient.signOut().addOnCompleteListener {
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            }
        }
    }

    private fun setupSecondaryActions() {
        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvGoToRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    /**
     * Llama al endpoint real: POST /api/v1/auth/login
     */
    private fun realizarLogin(email: String, password: String) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.login(
                    LoginRequest(email = email, password = password)
                )

                mostrarCargando(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        //sessionManager.guardarToken(body.token)
                        //irAPantallaPrincipal()
                        val tipoUsuario = JwtUtils.obtenerTipoUser(body.token)
                        sessionManager.guardarSesion(body.token, tipoUsuario)
                        navegarPorPerfil(tipoUsuario)
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

    /**
     * Llama al endpoint real: POST /api/v1/auth/login-google
     * Body: { "token": "<idToken de Google>" }
     *
     * NOTA/SUPUESTO: asumo que un 404 significa "cuenta nueva, no existe
     * todavía" y por eso mando al usuario a completar su registro. Si tu
     * backend usa otro código para ese caso (ej. 401 o un campo específico
     * en el JSON de error), ajusta la condición de abajo.
     */
    private fun realizarLoginGoogle(idToken: String) {
        mostrarCargando(true)

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.loginGoogle(
                    GoogleLoginRequest(token = idToken)
                )

                mostrarCargando(false)

                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null) {
                        //sessionManager.guardarToken(body.token)
                        //irAPantallaPrincipal()
                        val tipoUsuario = JwtUtils.obtenerTipoUser(body.token)
                        sessionManager.guardarSesion(body.token, tipoUsuario)
                        navegarPorPerfil(tipoUsuario)

                    } else {
                        mostrarError("Respuesta vacía del servidor")
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    val mensajeBackend = try {
                        errorBodyString?.let {
                            val error = Gson().fromJson(it, ApiErrorResponse::class.java)
                            error.detalle ?: error.message
                        }
                    } catch (e: Exception) {
                        null
                    }

                    val esUsuarioNuevo = mensajeBackend?.contains("NO REGISTRADO", ignoreCase = true) == true
                            || errorBodyString?.contains("NO REGISTRADO", ignoreCase = true) == true

                    if (esUsuarioNuevo) {
                        // Cuenta nueva: falta completar datos antes de registrar
                        val intent = Intent(this@LoginActivity, CompleteGoogleRegisterActivity::class.java)
                        intent.putExtra(CompleteGoogleRegisterActivity.EXTRA_GOOGLE_TOKEN, idToken)
                        startActivity(intent)
                    } else {
                        mostrarError(mensajeBackend ?: "Error del servidor (${response.code()})")
                    }
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
            403 -> mensajeBackend ?: "Credenciales inválidas o sesión no autorizada"
            401 -> mensajeBackend ?: "Correo o contraseña incorrectos"
            400 -> mensajeBackend ?: "Datos inválidos, revisa el formulario"
            else -> mensajeBackend ?: "Error del servidor ($codigoHttp)"
        }

        mostrarError(mensaje)
    }

    //dependiendo del tipo de usuario se abre una pantalla diferente
    private fun irAPantallaPrincipal() {
        Toast.makeText(this, "Login exitoso ✅", Toast.LENGTH_SHORT).show()
        // Cambiamos MainActivity por OwnerHomeActivity
        startActivity(Intent(this, OwnerHomeActivity::class.java))
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
        btnGoogleSignIn.isEnabled = !cargando
        btnLogin.text = if (cargando) "" else getString(R.string.login_button_text)
    }

    private fun navegarPorPerfil(tipoUsuario: String?) {
        //conversion a mayusculas
        val rolFormato=tipoUsuario?.uppercase()
        val destino:Class<*>

        if( rolFormato == "CLIENTE"){
            destino = ClientPrincipalMenu::class.java
        }else if(rolFormato=="VENDEDOR"){
            destino= OwnerHomeActivity::class.java
            startActivity(Intent(this, OwnerHomeActivity::class.java))
            finish()
        }else {
            mostrarError("Tipo de cuenta no reconocido: $tipoUsuario")
            return
        }

        //Demostracion de exito:
        Toast.makeText(this, "Login exitoso ✅", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, destino).apply {
            // Limpia el historial para que no regrese al Login al presionar Atrás
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }
}
