package com.example.ocasion_mobil_v2

import android.content.Intent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.example.ocasion_mobil_v2.ui.cliente.DetalleSalonActivity
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.ReservaActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DetalleSalonNavegacionTest {
    @Test fun calendarioYReservaTienenAccesosIndependientes() {
        abrirAppParaPrueba()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val intent = Intent(instrumentation.targetContext, DetalleSalonActivity::class.java)
            .putExtra(DetalleSalonActivity.EXTRA_ID_SALON, 2)
        ActivityScenario.launch<DetalleSalonActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.btnDisponibilidadDetalle).performClick()
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.panelCalendarioDetalle).visibility)
            }
            val monitor = instrumentation.addMonitor(ReservaActivity::class.java.name, null, false)
            try {
                scenario.onActivity { it.findViewById<View>(R.id.btnIniciarReserva).performClick() }
                val reserva = instrumentation.waitForMonitorWithTimeout(monitor, 5000)
                assertNotNull("El botón debe abrir la reserva", reserva)
                try {
                    assertEquals(2, reserva.intent.getIntExtra("idSalon", -1))
                } finally {
                    instrumentation.runOnMainSync { reserva.finish() }
                }
            } finally {
                instrumentation.removeMonitor(monitor)
            }
        }
    }
}
