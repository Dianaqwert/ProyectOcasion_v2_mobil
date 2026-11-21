package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import com.example.ocasion_mobil_v2.ui.login.LoginActivity
import com.example.ocasion_mobil_v2.util.JwtUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

internal fun Context.dp(n: Int): Int =
    (n * resources.displayMetrics.density).toInt()

internal fun Context.columna(): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL

        setPadding(
            context.dp(16),
            context.dp(8),
            context.dp(16),
            context.dp(8)
        )

        layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

internal fun LinearLayout.texto(
    valor: String,
    tam: Float = 16f
): TextView =
    TextView(context).also {
        it.text = valor
        it.textSize = tam
        it.setTextColor(Color.rgb(46, 42, 31))
        it.setPadding(0, context.dp(6), 0, context.dp(6))

        addView(
            it,
            LinearLayout.LayoutParams(-1, -2)
        )
    }

internal fun LinearLayout.boton(
    valor: String,
    accion: () -> Unit
): MaterialButton =
    MaterialButton(context).also {
        it.text = valor
        it.isAllCaps = false
        it.minHeight = context.dp(48)
        it.setTextColor(Color.WHITE)

        it.backgroundTintList = ColorStateList(
            arrayOf(
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf()
            ),
            intArrayOf(
                Color.LTGRAY,
                Color.rgb(54, 126, 24)
            )
        )

        addView(
            it,
            LinearLayout.LayoutParams(-1, -2)
        )

        it.setOnClickListener { accion() }
    }

internal data class Entrada(
    val campo: TextInputLayout,
    val input: TextInputEditText
)

internal fun LinearLayout.entrada(
    etiqueta: String,
    tipo: Int
): Entrada {

    val campo = TextInputLayout(context).apply {
        hint = etiqueta
        boxBackgroundMode =
            TextInputLayout.BOX_BACKGROUND_OUTLINE
    }

    val input = TextInputEditText(campo.context).apply {
        inputType = tipo
        setTextColor(Color.rgb(46, 42, 31))
        setSingleLine()
    }

    campo.addView(
        input,
        ViewGroup.LayoutParams(-1, -2)
    )

    addView(
        campo,
        LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = context.dp(8)
        }
    )

    return Entrada(campo, input)
}

internal fun TextView.mostrarError(mensaje: String?) {
    text = mensaje

    visibility =
        if (mensaje == null) View.GONE else View.VISIBLE

    setTextColor(Color.rgb(170, 28, 28))
}

abstract class PantallaPropietario : FragmentActivity() {

    protected lateinit var raiz: LinearLayout
    private var tokenInicial: String? = null

    protected fun preparar(titulo: String): Boolean {
        val token = SessionManager(this).obtenerToken()

        if (token == null) {
            startActivity(
                Intent(this, LoginActivity::class.java)
            )
            finish()
            return false
        }

        if (
            !ReservacionesNotifications.esPropietario(
                JwtUtils.obtenerTipoUser(token)
            )
        ) {
            finish()
            return false
        }

        val destinatario =
            intent.getIntExtra("propietarioDestino", -1)

        if (
            destinatario != -1 &&
            JwtUtils.obtenerIdentidadUsuario(token)?.id !=
            destinatario
        ) {
            finish()
            return false
        }

        tokenInicial = token

        raiz = columna().apply {
            setBackgroundColor(Color.WHITE)
        }

        setContentView(
            raiz,
            ViewGroup.LayoutParams(-1, -1)
        )

        ViewCompat.setOnApplyWindowInsetsListener(raiz) {
                view, insets ->

            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.ime() or
                        WindowInsetsCompat.Type.displayCutout()
            )

            view.updatePadding(
                left = dp(16) + bars.left,
                right = dp(16) + bars.right,
                top = bars.top,
                bottom = bars.bottom
            )

            insets
        }

        raiz.boton("Volver") { finish() }
        raiz.texto(titulo, 24f)

        return true
    }

    override fun onResume() {
        super.onResume()

        if (
            tokenInicial != null &&
            SessionManager(this).obtenerToken() != tokenInicial
        ) {
            finish()
        }
    }
}

class ReservasPdfProvider :
    androidx.core.content.FileProvider()