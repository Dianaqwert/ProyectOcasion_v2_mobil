package com.example.ocasion_mobil_v2

import android.content.Intent
import android.view.View
import android.widget.EditText
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ocasion_mobil_v2.ui.propietario.SalonFormActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FormularioEstabilidadTest {
    @Test fun editarSalonRechazaNumerosInvalidosAlGuardar() {
        abrirAppParaPrueba()
        val contexto = InstrumentationRegistry.getInstrumentation().targetContext
        val intent = Intent(contexto, SalonFormActivity::class.java)
            .putExtra("modoEdicion", true).putExtra("idSalon", "1")
            .putExtra("nombreSalon", "Prueba").putExtra("descripcion", "Descripción")
            .putExtra("precioHora", "250").putExtra("capacidadPersonas", "10")
        ActivityScenario.launch<SalonFormActivity>(intent).use { escenario ->
            escenario.onActivity { pantalla ->
                val capacidad = pantalla.findViewById<EditText>(R.id.etCapacidad)
                val guardar = pantalla.findViewById<View>(R.id.btnContinuarUbicacion)
                for (texto in listOf("", "abc", "0", "99999999999999999999")) {
                    capacidad.setText(texto)
                    guardar.performClick()
                    assertFalse(pantalla.isFinishing)
                }
                capacidad.setText("10")
                pantalla.findViewById<EditText>(R.id.etPrecioHora).setText(".")
                guardar.performClick()
            }
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            escenario.onActivity {
                assertEquals(View.VISIBLE, it.findViewById<View>(R.id.tvErrorPrecio).visibility)
            }
        }
    }
}
