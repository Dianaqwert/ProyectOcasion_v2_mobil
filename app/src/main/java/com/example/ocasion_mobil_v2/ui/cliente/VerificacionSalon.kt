package com.example.ocasion_mobil_v2.ui.cliente

import android.content.res.ColorStateList
import android.widget.TextView
import com.example.ocasion_mobil_v2.R

internal fun TextView.mostrarVerificacion(verificado: Boolean) {
    setText(if (verificado) R.string.salon_verificado else R.string.salon_no_verificado)
    val fondo = if (verificado) R.color.verificacion_salon_fondo else R.color.gris_claro
    backgroundTintList = ColorStateList.valueOf(context.getColor(fondo))
    setTextColor(context.getColor(if (verificado) R.color.verificacion_salon_texto else R.color.texto_secundario))
}
