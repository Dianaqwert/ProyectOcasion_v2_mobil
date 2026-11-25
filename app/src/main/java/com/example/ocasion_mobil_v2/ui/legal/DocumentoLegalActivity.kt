package com.example.ocasion_mobil_v2.ui.legal

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.lifecycle.lifecycleScope
import com.example.ocasion_mobil_v2.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

internal enum class DocumentoLegal(@StringRes val titulo: Int, @RawRes val pdf: Int) {
    PRIVACIDAD(R.string.legal_privacidad, R.raw.aviso_privacidad_integral),
    TERMINOS(R.string.legal_terminos, R.raw.terminos_de_uso),
    AVISO(R.string.legal_aviso, R.raw.aviso_legal);

    fun abrir(context: Context) {
        context.startActivity(Intent(context, DocumentoLegalActivity::class.java).putExtra(EXTRA_DOCUMENTO, name))
    }

    fun renderizar(contexto: Context, indice: Int): Pair<Int, Bitmap> {
        val copia = File.createTempFile("documento-legal-", ".pdf", contexto.cacheDir)
        try {
            contexto.resources.openRawResource(pdf).use { origen ->
                copia.outputStream().use { origen.copyTo(it) }
            }
            val descriptor = ParcelFileDescriptor.open(copia, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = try {
                PdfRenderer(descriptor)
            } catch (e: Exception) {
                descriptor.close()
                throw e
            }
            return renderer.use { documento ->
                documento.openPage(indice).use { hoja ->
                    val bitmap = Bitmap.createBitmap(hoja.width * 2, hoja.height * 2, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    hoja.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    documento.pageCount to bitmap
                }
            }
        } finally {
            copia.delete()
        }
    }

    companion object {
        const val EXTRA_DOCUMENTO = "documentoLegal"

        fun mostrarMenu(context: Context) {
            val titulos = entries.map { context.getString(it.titulo) }.toTypedArray()
            MaterialAlertDialogBuilder(context)
                .setTitle(R.string.legal_centro)
                .setItems(titulos) { _, posicion -> entries[posicion].abrir(context) }
                .setNegativeButton(R.string.legal_volver, null)
                .show()
        }
    }
}

class DocumentoLegalActivity : ComponentActivity() {
    private var pagina = 0
    private var total = 0
    private var ampliada = false
    private var cargando = false
    private lateinit var documento: DocumentoLegal
    private lateinit var imagen: ImageView
    private lateinit var desplazamiento: ScrollView
    private lateinit var ancho: HorizontalScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val nombre = intent.getStringExtra(DocumentoLegal.EXTRA_DOCUMENTO)
        documento = DocumentoLegal.entries.firstOrNull { it.name == nombre } ?: run { finish(); return }
        pagina = savedInstanceState?.getInt("pagina") ?: 0
        ampliada = savedInstanceState?.getBoolean("ampliada") ?: false
        setContentView(R.layout.activity_documento_legal)
        val raiz = findViewById<View>(R.id.raizLegal)
        ViewCompat.setOnApplyWindowInsetsListener(raiz) { vista, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            vista.updatePadding(left = barras.left, top = barras.top, right = barras.right, bottom = barras.bottom)
            insets
        }
        titulo(R.id.tituloDocumentoLegal).setText(documento.titulo)
        imagen = findViewById(R.id.imagenDocumentoLegal)
        desplazamiento = findViewById(R.id.visorLegalVertical)
        ancho = findViewById(R.id.visorLegalHorizontal)
        boton(R.id.btnCerrarLegal).setOnClickListener { finish() }
        boton(R.id.btnPaginaAnteriorLegal).setOnClickListener { mostrarPagina(pagina - 1) }
        boton(R.id.btnPaginaSiguienteLegal).setOnClickListener { mostrarPagina(pagina + 1) }
        boton(R.id.btnReintentarLegal).setOnClickListener { mostrarPagina(pagina) }
        boton(R.id.btnAmpliarLegal).setOnClickListener {
            ampliada = !ampliada
            ajustarZoom()
        }
        mostrarPagina(pagina)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("pagina", pagina)
        outState.putBoolean("ampliada", ampliada)
        super.onSaveInstanceState(outState)
    }

    private fun mostrarPagina(indice: Int) {
        if (cargando || indice < 0 || total > 0 && indice >= total) return
        pagina = indice
        cargando = true
        ajustarControles(error = false)
        lifecycleScope.launch {
            try {
                val (paginas, bitmap) = withContext(Dispatchers.IO) {
                    documento.renderizar(applicationContext, indice)
                }
                total = paginas
                imagen.setImageBitmap(bitmap)
                imagen.contentDescription = getString(R.string.legal_pagina, pagina + 1, total)
                desplazamiento.scrollTo(0, 0)
                ancho.scrollTo(0, 0)
                cargando = false
                ajustarControles(error = false)
                ajustarZoom()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                cargando = false
                ajustarControles(error = true)
            }
        }
    }

    private fun ajustarControles(error: Boolean) {
        findViewById<View>(R.id.progresoLegal).isVisible = cargando
        boton(R.id.btnReintentarLegal).isVisible = error
        imagen.isVisible = !error && !cargando
        boton(R.id.btnPaginaAnteriorLegal).isEnabled = !cargando && pagina > 0
        boton(R.id.btnPaginaSiguienteLegal).isEnabled = !cargando && pagina + 1 < total
        boton(R.id.btnAmpliarLegal).isEnabled = !cargando && !error && total > 0
        titulo(R.id.paginaDocumentoLegal).apply {
            text = when {
                error -> getString(R.string.legal_error_lectura)
                total > 0 -> getString(R.string.legal_pagina, pagina + 1, total)
                else -> ""
            }
        }
    }

    private fun ajustarZoom() {
        boton(R.id.btnAmpliarLegal).setText(if (ampliada) R.string.legal_ajustar else R.string.legal_ampliar)
        ancho.doOnLayout {
            imagen.updateLayoutParams { width = if (ampliada) ancho.width * 2 else ancho.width }
            if (!ampliada) ancho.scrollTo(0, 0)
        }
    }

    private fun boton(id: Int) = findViewById<Button>(id)
    private fun titulo(id: Int) = findViewById<TextView>(id)
}
