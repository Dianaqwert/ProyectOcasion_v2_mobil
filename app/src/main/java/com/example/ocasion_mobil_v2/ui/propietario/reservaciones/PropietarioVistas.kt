package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.FragmentActivity
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import com.example.ocasion_mobil_v2.ui.login.LoginActivity
import com.example.ocasion_mobil_v2.util.JwtUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun Context.dp(n: Int): Int =
    (n * resources.displayMetrics.density).toInt()

private val localeMx = Locale.forLanguageTag("es-MX")
private val formatoFechaEvento = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", localeMx)

internal fun Reservacion.fechaYHorario(): String {
    val fechas = listOfNotNull(fechaInicio, fechaFin).distinct().joinToString(" al ") {
        ReglasPropietario.fecha(it)?.format(formatoFechaEvento) ?: it
    }.ifBlank { "Fecha por definir" }
    val horario = listOfNotNull(horaInicio?.take(5), horaFin?.take(5)).joinToString(" a ")
    return listOf(fechas, horario, numeroInvitados?.let { "$it invitados" }.orEmpty())
        .filter { it.isNotBlank() }.joinToString(" · ")
}

internal fun importeReserva(importe: String?): String? =
    importe?.toBigDecimalOrNull()?.let { "${NumberFormat.getCurrencyInstance(localeMx).format(it)} MXN" }

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
    secundario: Boolean = false,
    accion: () -> Unit
): MaterialButton =
    (if (secundario) LayoutInflater.from(context)
        .inflate(R.layout.boton_reserva_secundario, this, false) as MaterialButton
    else MaterialButton(context)).also {
        it.text = valor
        it.isAllCaps = false
        it.minHeight = context.dp(48)
        it.cornerRadius = context.dp(12)
        it.letterSpacing = 0f

        if (!secundario) {
            it.setTextColor(androidx.core.content.ContextCompat.getColorStateList(context, R.color.boton_texto))
            it.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(context, R.color.boton_principal_fondo)
        }

        addView(
            it,
            LinearLayout.LayoutParams(-1, -2)
        )

        it.setOnClickListener { accion() }
    }

internal fun LinearLayout.seccion(titulo: String? = null, destacada: Boolean = false): LinearLayout {
    val tarjeta = MaterialCardView(context).apply {
        radius = context.dp(18).toFloat()
        cardElevation = context.dp(1).toFloat()
        strokeWidth = context.dp(1)
        strokeColor = context.getColor(R.color.borde_input)
        setCardBackgroundColor(context.getColor(if (destacada) R.color.reserva_superficie_destacada else R.color.blanco))
    }
    val contenido = context.columna().apply {
        setPadding(context.dp(16), context.dp(12), context.dp(16), context.dp(12))
    }
    titulo?.let {
        contenido.texto(it, 19f).apply {
            setTypeface(typeface, Typeface.BOLD)
            setAccessibilityHeading(true)
        }
    }
    tarjeta.addView(contenido)
    addView(tarjeta, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = context.dp(12) })
    return contenido
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
        LinearLayout.LayoutParams(-1, -2)
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
    if (mensaje != null) {
        setPadding(context.dp(12), context.dp(8), context.dp(12), context.dp(8))
        background = GradientDrawable().apply {
            cornerRadius = context.dp(12).toFloat()
            setColor(Color.rgb(252, 226, 225))
        }
    }
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
            setBackgroundColor(getColor(R.color.reserva_fondo))
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

        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(8), dp(14), dp(8))
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(getColor(R.color.header_footer_light))
            }
        }
        val volver = MaterialButton(this).apply {
            text = "←"
            textSize = 22f
            contentDescription = "Volver"
            cornerRadius = dp(12)
            minWidth = 0
            setPadding(0, 0, 0, 0)
            stateListAnimator = null
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.header_footer_dark))
            setTextColor(getColor(R.color.texto_principal))
            setOnClickListener { finish() }
        }
        barra.addView(volver, LinearLayout.LayoutParams(dp(48), dp(48)))
        barra.addView(TextView(this).apply {
            text = titulo
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.texto_principal))
            setPadding(dp(12), 0, 0, 0)
            setAccessibilityHeading(true)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        raiz.addView(barra, LinearLayout.LayoutParams(-1, -2).apply {
            topMargin = dp(6)
            bottomMargin = dp(8)
        })

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
