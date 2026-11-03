package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.repository.*
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.util.Locale

class DocumentosSalonActivity : ComponentActivity() {
    private val model: DocumentosSalonViewModel by viewModels()
    private var tipoPendiente = TipoDocumento.INE
    private val pickerUnico = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) recibirArchivos(listOf(uri))
    }
    private val picker = registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        recibirArchivos(uris)
    }

    private fun recibirArchivos(uris: List<android.net.Uri>) {
        uris.forEach { uri ->
            try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            catch (_: SecurityException) { /* Algunos proveedores conceden solo acceso temporal. */ }
        }
        model.seleccionar(tipoPendiente, uris)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentos_salon)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootDocumentosSalon)) { view, insets ->
            val barras = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }
        tipoPendiente = savedInstanceState?.getString("tipoPendiente")?.let { runCatching { TipoDocumento.valueOf(it) }.getOrNull() } ?: TipoDocumento.INE
        val id = intent.getIntExtra("idSalon", -1)
        if (id <= 0) { finish(); return }
        findViewById<TextView>(R.id.tvNombreDocumentos).text = intent.getStringExtra("nombreSalon") ?: "Tu salón"
        findViewById<View>(R.id.btnVolverDocumentos).setOnClickListener { volver() }
        findViewById<MaterialButton>(R.id.btnActualizarDocumentos).setOnClickListener { model.actualizar() }
        findViewById<MaterialButton>(R.id.btnMisSalonesDocumentos).setOnClickListener {
            startActivity(Intent(this, MisSalonesActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
        findViewById<MaterialButton>(R.id.btnEnviarDocumentos).setOnClickListener {
            val publicado = model.state.value.salon?.estado == "PUBLICADO"
            MaterialAlertDialogBuilder(this)
                .setTitle(if (publicado) "Enviar expediente al administrador" else "Publicar y solicitar verificación")
                .setMessage(if (publicado) "Se enviará el expediente para revisión. Si seleccionaste archivos nuevos, reemplazarán todos los documentos anteriores."
                    else "El servidor publicará primero el salón y después recibirá los documentos. Podrá aparecer ante los clientes antes de ser verificado. ¿Deseas continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Continuar") { _, _ -> model.enviar() }.show()
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = volver()
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) { model.state.collect { render(it) } }
        }
        model.iniciar(id)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("tipoPendiente", tipoPendiente.name)
        super.onSaveInstanceState(outState)
    }

    private fun volver() {
        if (model.state.value.ocupado) {
            Toast.makeText(this, "Espera a que termine la operación.", Toast.LENGTH_SHORT).show()
        } else finish()
    }

    private fun render(state: DocumentosSalonState) {
        state.salon?.nombreSalon?.let { findViewById<TextView>(R.id.tvNombreDocumentos).text = it }
        findViewById<TextView>(R.id.tvEstadoDocumentos).text = when (state.revision) {
            "APROBADO" -> "Verificado por el administrador"
            "PENDIENTE" -> "Expediente enviado · Pendiente de revisión"
            "RECHAZADO" -> "Expediente rechazado · Corrige los documentos"
            "SIN_ENVIAR" -> "Documentos guardados · Falta enviar a revisión"
            else -> "Sin expediente enviado"
        }
        findViewById<TextView>(R.id.tvObservacionesDocumentos).apply {
            text = state.salon?.documentosValidacionSalonEntity?.observacionesAdmin?.let { "Observaciones del administrador: $it" }
            isVisible = !text.isNullOrBlank()
        }
        findViewById<View>(R.id.progresoDocumentos).isVisible = state.ocupado
        findViewById<TextView>(R.id.tvErrorDocumentos).apply { text = state.error; isVisible = state.error != null }
        findViewById<TextView>(R.id.tvTotalDocumentos).text = String.format(Locale.getDefault(),
            "%d archivos · %.1f MB / 99 MB", state.archivos.values.sumOf { it.size }, state.bytes / 1048576.0)
        findViewById<MaterialButton>(R.id.btnActualizarDocumentos).isEnabled = !state.ocupado
        findViewById<MaterialButton>(R.id.btnMisSalonesDocumentos).isEnabled = !state.ocupado
        findViewById<MaterialButton>(R.id.btnEnviarDocumentos).apply {
            isVisible = !state.bloqueado
            isEnabled = !state.ocupado && state.salon != null &&
                (state.completo || (state.revision == "SIN_ENVIAR" && state.archivos.values.all { it.isEmpty() }))
            text = if (state.ocupado) state.fase
                else if (state.revision == "SIN_ENVIAR") "Enviar a revisión / reintentar"
                else if (state.salon?.estado != "PUBLICADO") "Publicar y enviar a revisión"
                else "Enviar documentos a revisión"
        }
        val container = findViewById<LinearLayout>(R.id.containerDocumentos)
        container.removeAllViews()
        if (state.bloqueado) return
        TipoDocumento.entries.forEach { tipo ->
            val archivos = state.archivos[tipo].orEmpty()
            val card = MaterialCardView(this).apply {
                radius = dp(16).toFloat()
                layoutParams = LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(14) }
                setCardBackgroundColor(getColor(R.color.blanco))
            }
            val contenido = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(12), dp(16), dp(12)) }
            contenido.addView(TextView(this).apply {
                text = tipo.titulo; textSize = 17f; setTextColor(getColor(R.color.texto_principal))
                setTypeface(typeface, android.graphics.Typeface.BOLD)
            })
            contenido.addView(TextView(this).apply {
                text = when (tipo) {
                    TipoDocumento.ESTABLECIMIENTO -> "${archivos.size}/10 fotos · mínimo 1"
                    TipoDocumento.PROPIETARIO -> "${archivos.size} fotos · mínimo 3"
                    else -> if (archivos.isEmpty()) "Obligatorio · selecciona un archivo" else "Archivo seleccionado"
                }
            })
            archivos.forEach { archivo ->
                contenido.addView(LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    addView(MaterialButton(this@DocumentosSalonActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                        text = archivo.nombre + String.format(Locale.getDefault(), " (%.2f MB)", archivo.bytes / 1048576.0)
                        isAllCaps = false
                        contentDescription = "Abrir ${archivo.nombre}"
                        setOnClickListener { abrir(archivo) }
                    }, LinearLayout.LayoutParams(0, -2, 1f))
                    addView(MaterialButton(this@DocumentosSalonActivity, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                        text = "Quitar"; isAllCaps = false; isEnabled = !state.ocupado
                        contentDescription = "Quitar ${archivo.nombre}"
                        setOnClickListener { model.quitar(tipo, archivo) }
                    })
                })
            }
            contenido.addView(MaterialButton(this).apply {
                text = if (tipo.maximo == 1 && archivos.isNotEmpty()) "Cambiar archivo" else "Seleccionar ${if (tipo.maximo == 1) "archivo" else "fotos"}"
                isAllCaps = false; isEnabled = !state.ocupado
                setOnClickListener {
                    tipoPendiente = tipo
                    val formatos = if (tipo.permitePdf) arrayOf("application/pdf", "image/jpeg", "image/png") else arrayOf("image/jpeg", "image/png")
                    if (tipo.maximo == 1) pickerUnico.launch(formatos) else picker.launch(formatos)
                }
            })
            card.addView(contenido); container.addView(card)
        }
    }

    private fun abrir(archivo: ArchivoDocumento) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(archivo.uri, archivo.mime)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
        } catch (_: Exception) { Toast.makeText(this, "No hay una aplicación disponible para abrir este archivo.", Toast.LENGTH_LONG).show() }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
