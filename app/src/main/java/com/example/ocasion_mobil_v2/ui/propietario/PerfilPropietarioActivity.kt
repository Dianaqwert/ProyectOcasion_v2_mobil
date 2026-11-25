package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import com.example.ocasion_mobil_v2.ui.login.LoginActivity

class PerfilPropietarioActivity : ComponentActivity() {

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
        findViewById<View>(R.id.btnPerfil).isSelected = true
        findViewById<TextView>(R.id.navCuentaEtiqueta).setTextColor(getColor(R.color.verde_primario))
    }

    override fun onStart() {
        super.onStart()
        if (ReservacionesNotifications.tokenPropietario(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
            finish()
        }
    }
}
