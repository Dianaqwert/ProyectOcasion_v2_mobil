package com.example.ocasion_mobil_v2.ui.legal

import android.graphics.Rect
import android.view.View
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import com.example.ocasion_mobil_v2.R

internal enum class ContextoAviso(
    @StringRes val resumen: Int,
    @StringRes val aceptacion: Int
) {
    REGISTRO(R.string.legal_resumen_registro, R.string.legal_acepto_registro),
    SALON(R.string.legal_resumen_salon, R.string.legal_acepto_salon),
    DOCUMENTOS(R.string.legal_resumen_documentos, R.string.legal_acepto_documentos)
}

internal class ConfirmacionLegal(panel: View, contexto: ContextoAviso) {
    private val casilla = panel.findViewById<CompoundButton>(R.id.casillaConfirmacionLegal)
    private val error = panel.findViewById<View>(R.id.errorConfirmacionLegal)

    init {
        val registro = contexto == ContextoAviso.REGISTRO
        panel.findViewById<TextView>(R.id.tituloConfirmacionLegal)
            .setText(if (registro) R.string.legal_centro else R.string.legal_aviso_breve)
        panel.findViewById<TextView>(R.id.resumenConfirmacionLegal).setText(contexto.resumen)
        vincular(panel, R.id.btnPrivacidadIntegral, DocumentoLegal.PRIVACIDAD, true)
        vincular(panel, R.id.btnTerminosUso, DocumentoLegal.TERMINOS, registro)
        vincular(panel, R.id.btnAvisoLegal, DocumentoLegal.AVISO, registro)
        casilla.setText(contexto.aceptacion)
        casilla.setOnCheckedChangeListener { _, aceptado -> if (aceptado) error.isVisible = false }
    }

    fun verificar(): Boolean {
        if (casilla.isChecked) return true
        error.isVisible = true
        error.doOnLayout { it.requestRectangleOnScreen(Rect(0, 0, it.width, it.height), false) }
        return false
    }

    private fun vincular(panel: View, id: Int, documento: DocumentoLegal, visible: Boolean) {
        panel.findViewById<View>(id).apply {
            isVisible = visible
            setOnClickListener { documento.abrir(context) }
        }
    }
}
