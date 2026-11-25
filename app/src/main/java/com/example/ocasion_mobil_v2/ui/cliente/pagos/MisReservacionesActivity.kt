package com.example.ocasion_mobil_v2.ui.cliente.pagos

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Intent
import android.content.DialogInterface
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.model.MovimientoReserva
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.google.android.material.card.MaterialCardView
import com.example.ocasion_mobil_v2.data.repository.PaseQr
import com.example.ocasion_mobil_v2.data.repository.ReglasPagoCliente
import com.example.ocasion_mobil_v2.data.repository.TransportePaypal
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MisReservacionesActivity : ComponentActivity() {
    private val vm: PagosClienteViewModel by viewModels()
    private lateinit var contenido: LinearLayout
    private lateinit var encabezado: TextView
    private lateinit var actualizar: MaterialButton
    private lateinit var scroll: ScrollView
    private lateinit var historial: LinearLayout
    private lateinit var avisoHistorial: TextView
    private lateinit var vacioHistorial: TextView
    private lateinit var progresoHistorial: com.google.android.material.progressindicator.LinearProgressIndicator
    private val adaptador by lazy { ReservasClienteAdapter(vm::seleccionar, vm::descargarPdf) { mostrarQr(it.id) } }
    private var visorQr: Pair<PaseQr, Dialog>? = null
    private var ultimaSeleccion: Long? = null
    private val moneda = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX"))
    private val formatoFecha = DateTimeFormatter.ofPattern("d 'de' MMM 'de' yyyy", Locale.forLanguageTag("es-MX"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!vm.sesionVigente()) {
            Toast.makeText(this, "Inicia sesión como cliente para ver tus reservaciones.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val pagina = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(251, 249, 242))
        }
        val barra = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), dp(9), dp(12), dp(10))
            setBackgroundColor(getColor(R.color.header_footer_dark))
        }
        val volver = botonBarra("←") { onBackPressedDispatcher.onBackPressed() }.apply {
            textSize = 22f
            contentDescription = "Volver"
            minWidth = 0
            setPadding(0, 0, 0, 0)
        }
        barra.addView(volver, LinearLayout.LayoutParams(dp(48), dp(48)))
        encabezado = etiqueta("Mis reservaciones", 20, destacado = true).apply {
            setPadding(dp(10), 0, dp(8), 0)
            setAccessibilityHeading(true)
        }
        barra.addView(encabezado, LinearLayout.LayoutParams(0, -2, 1f))
        actualizar = botonBarra("Actualizar") { vm.actualizar() }
        barra.addView(actualizar, LinearLayout.LayoutParams(-2, -2))
        pagina.addView(barra)
        contenido = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(18), dp(16), dp(24))
        }
        scroll = ScrollView(this).apply {
            isFillViewport = true
            addView(contenido)
        }
        historial = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        progresoHistorial = com.google.android.material.progressindicator.LinearProgressIndicator(this).apply {
            isIndeterminate = true
        }
        historial.addView(progresoHistorial, LinearLayout.LayoutParams(-1, -2))
        avisoHistorial = etiqueta("", 14).apply {
            setPadding(dp(16), dp(8), dp(16), dp(8))
            setBackgroundColor(getColor(R.color.header_footer_light))
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        historial.addView(avisoHistorial, LinearLayout.LayoutParams(-1, -2))
        vacioHistorial = etiqueta("Aún no tienes reservaciones. Tus solicitudes aparecerán aquí.", 16, secundario = true).apply {
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(28), dp(24), dp(12))
        }
        historial.addView(vacioHistorial, LinearLayout.LayoutParams(-1, -2))
        val lista = RecyclerView(this).apply {
            id = R.id.listaDashboardReservas
            layoutManager = LinearLayoutManager(this@MisReservacionesActivity)
            adapter = adaptador
            clipToPadding = false
            setPadding(dp(16), dp(16), dp(16), dp(14))
        }
        historial.addView(lista, LinearLayout.LayoutParams(-1, 0, 1f))
        pagina.addView(historial, LinearLayout.LayoutParams(-1, 0, 1f))
        pagina.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        ViewCompat.setOnApplyWindowInsetsListener(pagina) { view, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            view.updatePadding(left = barras.left, right = barras.right, bottom = barras.bottom)
            barra.updatePadding(top = barras.top + dp(9))
            insets
        }
        setContentView(pagina)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (vm.state.value.cargando && vm.seleccion != null) return
                if (!vm.volver()) finish()
            }
        })
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.state.collect { render(it) }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                vm.state.map { it.documento }.filterNotNull().collect { archivo ->
                    vm.documentoAbierto()
                    if (vm.sesionVigente()) abrirDocumento(archivo)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!vm.sesionVigente()) {
            finish()
            return
        }
    }

    private fun render(state: PagosClienteState) {
        if (!vm.sesionVigente()) { finish(); return }
        visorQr?.let { (pase, dialogo) -> if (pase !in state.pases) dialogo.dismiss() }
        val id = vm.seleccion
        val posicion = if (ultimaSeleccion == id) scroll.scrollY else 0
        ultimaSeleccion = id
        encabezado.text = if (id == null) "Mis reservaciones" else "Detalle de reserva"
        actualizar.isEnabled = !state.cargando

        historial.isVisible = id == null
        scroll.isVisible = id != null
        if (id == null) {
            mostrarHistorial(state)
            return
        }

        contenido.removeAllViews()
        if (state.cargando) contenido.addView(com.google.android.material.progressindicator.LinearProgressIndicator(this).apply {
            isIndeterminate = true
        }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(12) })
        state.aviso?.let { tarjeta(destacada = true) { addView(etiqueta(it, 14)) } }

        mostrarDetalle(state, id)

        scroll.post { if (ultimaSeleccion == id) scroll.scrollTo(0, posicion) }
    }

    private fun mostrarHistorial(state: PagosClienteState) {
        progresoHistorial.isVisible = state.cargando
        avisoHistorial.text = state.aviso
        avisoHistorial.isVisible = state.aviso != null
        val pases = state.pases.associateBy { it.id }
        val reservas = state.reservas
        val ids = reservas.map { it.id }.toSet()
        val fichas = reservas.map { reserva ->
            FichaHistorial(reserva.id, reserva, pases[reserva.id], state.cargando, state.descargando)
        } + state.pases.filterNot { it.id in ids }.sortedByDescending { it.fecha }.map { pase ->
            FichaHistorial(pase.id, null, pase, state.cargando, state.descargando)
        }
        vacioHistorial.isVisible = fichas.isEmpty() && !state.cargando && state.aviso == null
        adaptador.submitList(fichas)
    }

    private fun abrirDocumento(archivo: File) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.reservas.pdf", archivo)
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                clipData = ClipData.newRawUri("Documento de la reserva", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(this, "Instala un lector de PDF para abrir el documento.", Toast.LENGTH_LONG).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "No pudimos dar acceso al lector de PDF.", Toast.LENGTH_LONG).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "No pudimos abrir el PDF.", Toast.LENGTH_LONG).show()
        }
    }

    private fun mostrarDetalle(state: PagosClienteState, id: Long) {
        val reserva = state.detalle
        if (reserva != null) {
            tarjeta(destacada = true) {
                rotuloEstado(reserva)
                addView(etiqueta(nombreSalon(reserva), 26, destacado = true))
                addView(etiqueta("Reserva #${reserva.id}", 14, secundario = true))
                reserva.ubicacionSalon?.let { addView(etiqueta(it, 15, secundario = true)) }
            }
            tarjeta("Datos del evento") {
                fila("Fecha", fechaEvento(reserva))
                fila("Horario", horarioEvento(reserva) ?: "Por definir")
                fila("Invitados", reserva.numeroInvitados?.toString() ?: "Sin dato")
                reserva.nombreCliente?.let { fila("Cliente", it) }
                reserva.correoCliente?.let { fila("Correo", it) }
                reserva.telefonoCliente?.let { fila("Teléfono", it) }
                importe(reserva.precioHoraSalon)?.let { fila("Tarifa por hora", it) }
                reserva.observaciones?.let { fila("Observaciones", it) }
            }
            tarjeta("Cotización y saldo") {
                fila(etiquetaTotal(reserva), importe(reserva.costoTotal) ?: "Por definir")
                importe(reserva.totalPagado)?.let { fila("Total pagado", it) }
                importe(reserva.saldoPendiente)?.takeUnless { reserva.estado == "SOLICITUD_NUEVA" }
                    ?.let { fila("Saldo pendiente", it) }

                if (reserva.estado == "ESPERANDO_PAGO") {
                    accion("Aceptar cotización", !state.cargando) {
                        MaterialAlertDialogBuilder(this@MisReservacionesActivity).setTitle("Aceptar cotización")
                            .setMessage("Después podrás elegir cuánto pagar con PayPal.")
                            .setNegativeButton("Volver", null)
                            .setPositiveButton("Aceptar") { _, _ -> vm.responderCotizacion(reserva, true) }.show()
                    }
                    accion("Rechazar cotización", !state.cargando, secundario = true) {
                        MaterialAlertDialogBuilder(this@MisReservacionesActivity).setTitle("¿Rechazar la cotización?")
                            .setMessage("Se cancelará esta reservación. Para retomarla tendrás que enviar otra solicitud.")
                            .setNegativeButton("Volver", null)
                            .setPositiveButton("Rechazar") { _, _ -> vm.responderCotizacion(reserva, false) }.show()
                    }
                }
                if (ReglasPagoCliente.puedePagar(reserva)) {
                    val saldo = ReglasPagoCliente.saldo(reserva)
                    if (vm.transporte == TransportePaypal.BLOQUEADO) {
                        addView(etiqueta("El servicio de pago necesita una conexión HTTPS con D3.", secundario = true))
                    } else if (saldo != null && state.orden == null) {
                        val minimo = ReglasPagoCliente.minimo(reserva)
                        if (minimo.signum() > 0) {
                            fila(
                                "Anticipo mínimo (${ReglasPagoCliente.PORCENTAJE_ANTICIPO} %)",
                                importe(minimo.toPlainString()).orEmpty()
                            )
                        }
                        accion("Pagar con PayPal", !state.cargando) { opcionesPago(saldo, minimo) }
                    }
                }
            }
        }

        state.orden?.let { orden ->
            val enlace = vm.enlacePaypal(orden)
            val errorOrden = reserva?.let {
                runCatching { ReglasPagoCliente.validarOrden(it, orden) }.exceptionOrNull()?.message
            }
            val habilitado = !state.cargando && vm.transporte != TransportePaypal.BLOQUEADO && enlace != null &&
                reserva != null && errorOrden == null
            tarjeta("Orden de PayPal", destacada = true) {
                fila("Importe", importe(orden.monto.toPlainString()) ?: "Sin dato")
                errorOrden?.let { addView(etiqueta(it, secundario = true)) }
                if (orden.capturaSolicitada) {
                    addView(etiqueta("Si PayPal muestra un cargo y el saldo no se actualiza, conserva la orden y consulta con soporte."))
                }
                accion("Abrir PayPal", habilitado) {
                    try {
                        CustomTabsIntent.Builder().build().launchUrl(this@MisReservacionesActivity, Uri.parse(enlace))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(this@MisReservacionesActivity, "Instala un navegador para abrir PayPal.", Toast.LENGTH_LONG).show()
                    }
                }
                accion("Confirmar tras aprobar en PayPal", habilitado, secundario = true) { vm.verificar() }
                if (!orden.capturaSolicitada) accion("Descartar orden", !state.cargando, secundario = true) { vm.descartarOrden() }
            }
        }

        val pase = state.pases.firstOrNull { it.id == id }
        if (pase != null || reserva?.checkInRealizado == true) {
            tarjeta("Acceso al evento", destacada = true) {
                if (reserva?.checkInRealizado == true) {
                    fila("Llegada", reserva.fechaCheckIn?.let(::fechaMovimiento) ?: "Registrada")
                }
                pase?.let {
                    if (it.id in state.pasesGuardados) {
                        addView(etiqueta("Pase disponible sin conexión.", secundario = true))
                    }
                    accion("Mostrar código QR") { mostrarQr(it.id) }
                }
            }
        }
        if (reserva != null) mostrarDesglose(reserva)
    }

    private fun mostrarDesglose(reserva: Reservacion) {
        tarjeta("Servicios del salón") {
            if (reserva.servicios.isEmpty()) addView(etiqueta("Sin servicios registrados.", secundario = true))
            reserva.servicios.forEach { servicio ->
                val nombre = servicio.nombre ?: servicio.id?.let { "Servicio #$it" } ?: "Servicio"
                subfila(nombre, listOfNotNull(servicio.cantidad?.let { "Cantidad: $it" },
                    importe(servicio.precio)?.let { "$it por unidad" },
                    importe(servicio.subtotal)?.let { "Subtotal: $it" }))
            }
        }
        tarjeta("Mobiliario asignado") {
            if (reserva.inventario.isEmpty()) addView(etiqueta("Sin mobiliario asignado.", secundario = true))
            reserva.inventario.forEach { recurso ->
                val nombre = recurso.nombre ?: recurso.id?.let { "Recurso #$it" } ?: "Recurso"
                subfila(nombre, listOfNotNull(recurso.cantidad?.let { "$it unidades" }))
            }
        }
        tarjeta("Historial de pagos") {
            if (reserva.pagos.isEmpty()) addView(etiqueta("Sin movimientos registrados.", secundario = true))
            reserva.pagos.sortedWith(compareByDescending<MovimientoReserva> { it.fecha }
                .thenByDescending { it.id }).forEach { pago ->
                subfila(listOfNotNull(pago.tipo?.let(::etiquetaCodigo) ?: "Pago", importe(pago.monto)).joinToString(" · "),
                    listOfNotNull(pago.fecha?.let(::fechaMovimiento), pago.metodo?.let { if (it == "PAYPAL") "PayPal" else etiquetaCodigo(it) },
                        pago.estado?.let(::etiquetaCodigo)))
            }
        }
    }

    private fun opcionesPago(saldo: BigDecimal, minimo: BigDecimal) {
        val opciones = mutableListOf("Liquidar ${saldo.toPlainString()} MXN")
        if (minimo < saldo && saldo > BigDecimal("0.01")) {
            opciones += if (minimo.signum() > 0) "Pagar un anticipo" else "Hacer un abono"
        }
        MaterialAlertDialogBuilder(this).setTitle("Pagar con PayPal")
            .setItems(opciones.toTypedArray()) { _, opcion ->
                if (opcion == 0) vm.iniciar(saldo) else pedirImporte(saldo, minimo)
            }.setNegativeButton("Cancelar", null).show()
    }

    private fun pedirImporte(saldo: BigDecimal, minimo: BigDecimal) {
        val input = TextInputEditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            hint = "Importe en MXN"
        }
        val dialogo = MaterialAlertDialogBuilder(this).setTitle(if (minimo.signum() > 0) "Anticipo" else "Abono")
            .setView(input).setPositiveButton("Continuar", null).setNegativeButton("Cancelar", null).show()
        dialogo.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val monto = ReglasPagoCliente.monto(input.text?.toString().orEmpty())
            if (monto == null || monto < minimo || monto >= saldo) {
                input.error = if (minimo.signum() > 0) "Ingresa al menos ${minimo.toPlainString()} MXN, sin llegar al saldo."
                    else "Ingresa un importe mayor a cero y menor al saldo."
            } else {
                dialogo.dismiss()
                vm.iniciar(monto)
            }
        }
    }

    private fun mostrarQr(id: Long) {
        if (!vm.sesionVigente() || visorQr != null) return
        val pase = vm.state.value.pases.firstOrNull { it.id == id } ?: return
        val qr = try {
            QRCodeWriter().encode(pase.token, BarcodeFormat.QR_CODE, 640, 640,
                mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
        } catch (_: com.google.zxing.WriterException) {
            Toast.makeText(this, "No pudimos mostrar el pase. Actualiza tus reservaciones.", Toast.LENGTH_LONG).show()
            return
        }
        val pixels = IntArray(qr.width * qr.height) { i ->
            if (qr[i % qr.width, i / qr.width]) Color.BLACK else Color.WHITE
        }
        val imagen = Bitmap.createBitmap(pixels, qr.width, qr.height, Bitmap.Config.ARGB_8888)
        val pantalla = Dialog(this, android.R.style.Theme_Material_Light_NoActionBar)
        pantalla.setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(20), dp(24), dp(20), dp(24))
            setBackgroundColor(Color.WHITE)
            addView(etiqueta("Pase de acceso", 24, destacado = true).apply {
                gravity = Gravity.CENTER
                setBackgroundColor(getColor(R.color.header_footer_light))
                setPadding(dp(12), dp(12), dp(12), dp(12))
            }, LinearLayout.LayoutParams(-1, -2))
            addView(etiqueta(pase.salon ?: "Reservación #${pase.id}", 18, destacado = true).apply {
                gravity = Gravity.CENTER
            })
            pase.fecha?.let { addView(etiqueta(fecha(it), 14, secundario = true)) }
            addView(ImageView(context).apply {
                contentDescription = "Código QR de acceso"
                scaleType = ImageView.ScaleType.FIT_CENTER
                setImageBitmap(imagen)
            }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f))
            accion("Cerrar", secundario = true) { pantalla.dismiss() }
        })
        pantalla.setOnDismissListener { visorQr = null }
        visorQr = pase to pantalla
        pantalla.window?.apply {
            addFlags(WindowManager.LayoutParams.FLAG_SECURE or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            attributes = attributes.apply { screenBrightness = 1f }
        }
        pantalla.show()
        pantalla.window?.apply {
            setLayout(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
            WindowCompat.getInsetsController(this, decorView).apply {
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                hide(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    override fun onDestroy() {
        visorQr?.second?.dismiss()
        super.onDestroy()
    }

    private fun tarjeta(titulo: String? = null, destacada: Boolean = false, configurar: LinearLayout.() -> Unit) {
        val tarjeta = MaterialCardView(this).apply {
            radius = dp(18).toFloat()
            cardElevation = dp(1).toFloat()
            strokeWidth = dp(1)
            strokeColor = getColor(R.color.borde_input)
            setCardBackgroundColor(if (destacada) Color.rgb(255, 245, 214) else Color.WHITE)
        }
        val cuerpo = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        titulo?.let { cuerpo.addView(etiqueta(it, 19, destacado = true).apply { setAccessibilityHeading(true) }) }
        cuerpo.configurar()
        tarjeta.addView(cuerpo, LayoutParams(-1, -2))
        contenido.addView(tarjeta, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
    }

    private fun LinearLayout.rotuloEstado(reserva: Reservacion) {
        val estado = EstadoVisualReserva.de(reserva.estado, ReglasPagoCliente.saldo(reserva)?.signum() == 1)
        val fondo = estado?.fondo ?: Color.LTGRAY
        addView(etiqueta(estado?.texto ?: etiquetaCodigo(reserva.estado), 13, destacado = true).apply {
            setPadding(dp(10), dp(5), dp(10), dp(5))
            setTextColor(estado?.tinta ?: getColor(R.color.texto_principal))
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(fondo) }
        }, LinearLayout.LayoutParams(-2, -2).apply { bottomMargin = dp(6) })
    }

    private fun LinearLayout.fila(nombre: String, valor: String) {
        val linea = LinearLayout(context).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.TOP }
        linea.addView(etiqueta(nombre, 14, secundario = true), LinearLayout.LayoutParams(0, -2, 2f))
        linea.addView(etiqueta(valor, 14, destacado = true).apply {
            gravity = Gravity.END
            setPadding(dp(8), paddingTop, 0, paddingBottom)
        }, LinearLayout.LayoutParams(0, -2, 3f))
        addView(linea)
    }

    private fun LinearLayout.subfila(titulo: String, datos: List<String>) {
        addView(etiqueta(titulo, 15, destacado = true))
        if (datos.isNotEmpty()) addView(etiqueta(datos.joinToString(" · "), 13, secundario = true))
    }

    private fun etiqueta(valor: String, tamano: Int = 15, destacado: Boolean = false, secundario: Boolean = false) =
        TextView(this).apply {
            text = valor
            textSize = tamano.toFloat()
            if (destacado) setTypeface(typeface, Typeface.BOLD)
            setPadding(0, dp(4), 0, dp(4))
            setTextColor(getColor(if (secundario) R.color.texto_secundario else R.color.texto_principal))
        }

    private fun crearBoton(titulo: String, secundario: Boolean, click: () -> Unit): MaterialButton {
        val boton = if (secundario) {
            layoutInflater.inflate(R.layout.boton_reserva_secundario, null) as MaterialButton
        } else MaterialButton(this)
        return boton.apply {
            text = titulo
            isAllCaps = false
            cornerRadius = dp(12)
            minHeight = dp(48)
            setOnClickListener { click() }
        }
    }

    private fun botonBarra(titulo: String, click: () -> Unit) =
        crearBoton(titulo, secundario = true, click = click).apply {
            backgroundTintList = ColorStateList.valueOf(getColor(R.color.header_footer_light))
            strokeWidth = 0
            setTextColor(ColorStateList(
                arrayOf(intArrayOf(-android.R.attr.state_enabled), intArrayOf()),
                intArrayOf(getColor(R.color.texto_secundario), getColor(R.color.texto_principal))
            ))
        }

    private fun LinearLayout.accion(titulo: String, habilitado: Boolean = true, secundario: Boolean = false, click: () -> Unit) {
        val boton = crearBoton(titulo, secundario, click).apply { isEnabled = habilitado }
        addView(boton, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(5) })
    }

    private fun nombreSalon(reserva: Reservacion) =
        reserva.nombreSalon ?: reserva.idSalon?.let { "Salón #$it" } ?: "Salón por confirmar"

    private fun etiquetaTotal(reserva: Reservacion) =
        if (reserva.estado == "SOLICITUD_NUEVA") "Importe estimado" else "Total de la reserva"

    private fun fecha(valor: String) = runCatching { LocalDate.parse(valor).format(formatoFecha) }.getOrDefault(valor)

    private fun fechaEvento(reserva: Reservacion) =
        listOfNotNull(reserva.fechaInicio, reserva.fechaFin).distinct().joinToString(" al ", transform = ::fecha)
            .ifEmpty { "Por definir" }

    private fun horarioEvento(reserva: Reservacion): String? {
        val inicio = reserva.horaInicio?.take(5)
        val fin = reserva.horaFin?.take(5)
        return when {
            inicio != null && fin != null -> "$inicio a $fin h"
            inicio != null -> "Desde $inicio h"
            fin != null -> "Hasta $fin h"
            else -> null
        }
    }

    private fun fechaMovimiento(valor: String): String =
        listOfNotNull(fecha(valor.substringBefore('T')), valor.substringAfter('T', "").take(5).takeIf { it.isNotEmpty() })
            .joinToString(" · ")

    private fun importe(valor: String?) = valor?.toBigDecimalOrNull()?.let { "${moneda.format(it)} MXN" }

    private fun etiquetaCodigo(valor: String): String =
        valor.replace('_', ' ').lowercase().replaceFirstChar { it.titlecase() }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}
