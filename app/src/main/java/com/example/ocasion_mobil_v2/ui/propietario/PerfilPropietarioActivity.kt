package com.example.ocasion_mobil_v2.ui.propietario

import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClient
import com.example.ocasion_mobil_v2.data.remote.model.ActualizarPerfilRequest
import com.example.ocasion_mobil_v2.data.remote.model.PerfilUsuario
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.ui.login.LoginActivity
import com.example.ocasion_mobil_v2.util.JwtUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.JsonParseException
import kotlinx.coroutines.launch
import java.io.IOException

class PerfilPropietarioActivity : ComponentActivity() {

    private val session by lazy { SessionManager(applicationContext) }
    private var perfil: PerfilUsuario? = null
    private var editor: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.owner_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootPerfilPropietario)) { view, insets ->
            val barras = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.updatePadding(left = barras.left, top = barras.top, right = barras.right, bottom = barras.bottom)
            insets
        }
        BottomNav.configurar(this)
    }

    override fun onStart() {
        super.onStart()
        cargarSesion()
    }

    override fun onDestroy() {
        editor?.dismiss()
        super.onDestroy()
    }

    private fun cargarSesion() {
        val token = session.obtenerToken()
        val identidad = token?.let(JwtUtils::obtenerIdentidadUsuario)
        if (token == null || identidad == null) {
            pedirInicioDeSesion()
            return
        }

        perfil = session.obtenerPerfilPropietario(identidad.id, identidad.correo)
        mostrarPerfil(identidad.correo)
        findViewById<TextView>(R.id.btnEditProfile).apply {
            setText(R.string.perfil_editar)
            setOnClickListener { editarPerfil(token, identidad) }
        }
    }

    private fun mostrarPerfil(correo: String?) {
        val perfilActual = perfil
        val nombreCompleto = listOfNotNull(
            perfilActual?.nombres, perfilActual?.apellidoPaterno, perfilActual?.apellidoMaterno
        ).filter { it.isNotBlank() }.joinToString(" ")
        findViewById<TextView>(R.id.tvProfileName).apply {
            text = nombreCompleto
            isVisible = nombreCompleto.isNotEmpty()
        }
        mostrarDato(R.id.tvNames, perfilActual?.nombres)
        mostrarDato(R.id.tvLastName, perfilActual?.apellidoPaterno)
        mostrarDato(R.id.tvMotherLastName, perfilActual?.apellidoMaterno)
        mostrarDato(R.id.tvUsername, perfilActual?.nombreUser)
        mostrarDato(R.id.tvEmail, perfilActual?.gmail ?: correo)
        mostrarDato(R.id.tvPhone, perfilActual?.telefono)
        mostrarDato(R.id.tvCurp, perfilActual?.curp)
        findViewById<TextView>(R.id.tvEstadoPerfil).apply {
            isVisible = perfilActual == null
            setText(if (correo == null) R.string.perfil_sesion_requerida else R.string.perfil_primera_carga)
        }
    }

    private fun mostrarDato(id: Int, dato: String?) {
        findViewById<TextView>(id).text = dato?.takeIf { it.isNotBlank() }
            ?: getString(R.string.perfil_sin_dato)
    }

    private fun pedirInicioDeSesion() {
        perfil = null
        mostrarPerfil(correo = null)
        findViewById<TextView>(R.id.btnEditProfile).apply {
            setText(R.string.login_button_text)
            setOnClickListener {
                session.invalidarSesion()
                startActivity(Intent(this@PerfilPropietarioActivity, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        }
    }

    private fun editarPerfil(token: String, identidad: JwtUtils.IdentidadUsuario) {
        val formulario = layoutInflater.inflate(R.layout.dialog_editar_perfil_propietario, null)
        val nombres = formulario.findViewById<TextInputEditText>(R.id.etPerfilNombres)
        val paterno = formulario.findViewById<TextInputEditText>(R.id.etPerfilApellidoPaterno)
        val materno = formulario.findViewById<TextInputEditText>(R.id.etPerfilApellidoMaterno)
        val telefono = formulario.findViewById<TextInputEditText>(R.id.etPerfilTelefono)
        val error = formulario.findViewById<TextView>(R.id.tvErrorPerfil)
        nombres.setText(perfil?.nombres)
        paterno.setText(perfil?.apellidoPaterno)
        materno.setText(perfil?.apellidoMaterno)
        telefono.setText(perfil?.telefono)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.perfil_editar)
            .setView(formulario)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.perfil_guardar, null)
            .show()
        editor = dialog
        dialog.setCanceledOnTouchOutside(false)
        dialog.setOnDismissListener { editor = null }

        val guardar = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val cancelar = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        val campos = listOf(nombres, paterno, materno, telefono)
        guardar.setOnClickListener {
            error.isVisible = false
            if (session.obtenerToken() != token) {
                dialog.dismiss()
                cargarSesion()
                return@setOnClickListener
            }
            val request = ActualizarPerfilRequest(
                nombre = nombres.text.toString().trim(),
                apellidoPaterno = paterno.text.toString().trim(),
                apellidoMaterno = materno.text.toString().trim(),
                telefono = telefono.text.toString().trim()
            )
            when {
                request.nombre.isBlank() -> nombres.mostrarError(R.string.perfil_nombre_requerido)
                request.apellidoPaterno.isBlank() -> paterno.mostrarError(R.string.perfil_apellido_requerido)
                !request.telefono.matches(Regex("[0-9]{10}")) -> telefono.mostrarError(R.string.perfil_telefono_invalido)
                else -> lifecycleScope.launch {
                    guardar.isEnabled = false
                    guardar.setText(R.string.perfil_guardando)
                    cancelar.isEnabled = false
                    dialog.setCancelable(false)
                    campos.forEach { it.isEnabled = false }
                    try {
                        val response = RetrofitClient.apiService.actualizarPerfil(
                            "Bearer $token", identidad.id, request
                        )
                        if (session.obtenerToken() != token) {
                            dialog.dismiss()
                            cargarSesion()
                            return@launch
                        }
                        if (response.code() == 401) {
                            dialog.dismiss()
                            session.invalidarSesion()
                            pedirInicioDeSesion()
                            return@launch
                        }

                        val actualizado = response.body()
                        when {
                            !response.isSuccessful -> error.mostrarError(mensajeError(response.code()))
                            actualizado?.id != identidad.id ||
                                !actualizado.gmail.equals(identidad.correo, ignoreCase = true) ->
                                error.mostrarError(R.string.perfil_error_respuesta)
                            else -> {
                                session.guardarPerfilPropietario(actualizado)
                                perfil = actualizado
                                mostrarPerfil(identidad.correo)
                                dialog.dismiss()
                                Toast.makeText(
                                    this@PerfilPropietarioActivity, R.string.perfil_guardado, Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    } catch (_: IOException) {
                        error.mostrarError(R.string.perfil_error_conexion)
                    } catch (_: JsonParseException) {
                        error.mostrarError(R.string.perfil_error_respuesta)
                    } finally {
                        campos.forEach { it.isEnabled = true }
                        dialog.setCancelable(true)
                        cancelar.isEnabled = true
                        guardar.isEnabled = true
                        guardar.setText(R.string.perfil_guardar)
                    }
                }
            }
        }
    }

    private fun TextInputEditText.mostrarError(@StringRes mensaje: Int) {
        error = getString(mensaje)
        requestFocus()
    }

    private fun TextView.mostrarError(@StringRes mensaje: Int) {
        setText(mensaje)
        isVisible = true
    }

    @StringRes
    private fun mensajeError(codigoHttp: Int): Int = when (codigoHttp) {
        400, 422 -> R.string.perfil_error_datos
        403 -> R.string.perfil_error_permiso
        404 -> R.string.perfil_error_no_encontrado
        else -> R.string.perfil_error_guardar
    }
}
