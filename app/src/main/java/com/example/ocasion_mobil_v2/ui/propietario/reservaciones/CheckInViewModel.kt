package com.example.ocasion_mobil_v2.ui.propietario.reservaciones

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientReservaciones
import com.example.ocasion_mobil_v2.data.repository.CheckInPropietario
import com.example.ocasion_mobil_v2.data.repository.ResultadoCheckIn
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.notifications.ReservacionesNotifications
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class CheckInViewModel(app: Application) : AndroidViewModel(app) {
    data class Estado(val validando: Boolean = false, val resultado: ResultadoCheckIn? = null)

    private val tokenInicial = ReservacionesNotifications.tokenPropietario(app)
    private val sesion = SessionManager(app)
    private val checkIn = CheckInPropietario(RetrofitClientReservaciones.getService(app), ::sesionVigente)
    private val mutable = MutableStateFlow(Estado())
    val estado = mutable.asStateFlow()

    fun sesionVigente(): Boolean = tokenInicial != null && tokenInicial == sesion.obtenerToken()

    fun validar(token: String) {
        if (estado.value.validando || !sesionVigente()) return
        mutable.value = Estado(validando = true)
        viewModelScope.launch {
            mutable.value = Estado(resultado = checkIn.validar(token))
        }
    }

    fun mostrarProblema(motivo: String) {
        if (!estado.value.validando) mutable.value = Estado(resultado = ResultadoCheckIn.SinConfirmar(motivo))
    }
}
