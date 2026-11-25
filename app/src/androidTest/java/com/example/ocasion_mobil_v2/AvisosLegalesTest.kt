package com.example.ocasion_mobil_v2

import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ocasion_mobil_v2.ui.legal.ConfirmacionLegal
import com.example.ocasion_mobil_v2.ui.legal.ContextoAviso
import com.example.ocasion_mobil_v2.ui.legal.DocumentoLegal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AvisosLegalesTest {
    private val instrumentacion = InstrumentationRegistry.getInstrumentation()

    @Test fun cadaAvisoExigeAceptacionExplicita() {
        instrumentacion.runOnMainSync {
            val contexto = ContextThemeWrapper(instrumentacion.targetContext, R.style.Theme_Ocasion_mobil_v2)
            for (tipo in ContextoAviso.entries) {
                val panel = LayoutInflater.from(contexto).inflate(R.layout.tarjeta_confirmacion_legal, null)
                val aviso = ConfirmacionLegal(panel, tipo)
                val casilla = panel.findViewById<CheckBox>(R.id.casillaConfirmacionLegal)
                val error = panel.findViewById<View>(R.id.errorConfirmacionLegal)
                assertFalse(casilla.isChecked)
                assertFalse(aviso.verificar())
                assertEquals(View.VISIBLE, error.visibility)
                casilla.performClick()
                assertTrue(aviso.verificar())
                assertEquals(View.GONE, error.visibility)
            }
        }
    }

    @Test fun renderizaLaPrimeraYUltimaPaginaDeCadaOriginal() {
        val contexto = instrumentacion.targetContext
        for ((documento, paginas) in listOf(DocumentoLegal.PRIVACIDAD to 7, DocumentoLegal.TERMINOS to 2, DocumentoLegal.AVISO to 2)) {
            for (indice in listOf(0, paginas - 1)) {
                val (total, imagen) = documento.renderizar(contexto, indice)
                try {
                    assertEquals(paginas, total)
                    assertTrue(imagen.width > 1000)
                    assertTrue(imagen.height > 1000)
                } finally {
                    imagen.recycle()
                }
            }
        }
    }
}
