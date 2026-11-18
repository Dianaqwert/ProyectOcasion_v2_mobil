package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import com.example.ocasion_mobil_v2.ui.login.LoginActivity
import com.example.ocasion_mobil_v2.ui.propietario.BottomNav
import com.example.ocasion_mobil_v2.util.JwtUtils

class ReservacionesActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = SessionManager(this).obtenerToken()
        if (token == null) { iniciarSesion(); return }
        if (!ReservacionesNotifications.esPropietario(JwtUtils.obtenerTipoUser(token))) { finish(); return }
        setContentView(R.layout.activity_reservaciones)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootReservaciones)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            insets
        }
        BottomNav.configurar(this)
        findViewById<Button>(R.id.btnVolverReservas).setOnClickListener { finish() }
        ReservacionesNotifications.programar(this)
    }

    fun iniciarSesion() {
        SessionManager(this).invalidarSesion()
        startActivity(Intent(this, LoginActivity::class.java).putExtra(ReservacionesNotifications.EXTRA_BANDEJA, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
