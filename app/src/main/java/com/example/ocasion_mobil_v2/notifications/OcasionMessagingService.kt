package com.example.ocasion_mobil_v2.notifications

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.ui.login.LoginActivity
import com.example.ocasion_mobil_v2.ui.propietario.reservaciones.PanelPropietarioActivity
import com.example.ocasion_mobil_v2.util.JwtUtils
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class OcasionMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Guardarlo localmente no lo registra en el backend.
        getSharedPreferences(
            "ocasion_fcm",
            MODE_PRIVATE
        )
            .edit()
            .putString("token_pendiente", token)
            .apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val tokenSesion =
            ReservacionesNotifications.tokenPropietario(this)
                ?: return

        val identidad =
            JwtUtils.obtenerIdentidadUsuario(tokenSesion)
                ?: return

        val propietarioDestino =
            message.data["idPropietario"]?.toIntOrNull()
                ?: return

        if (propietarioDestino != identidad.id) return

        val idReserva = message.data["idReservacion"]
            ?.toLongOrNull()
            ?.takeIf { it > 0 }
            ?: return

        val nueva =
            message.data["estado"] == "SOLICITUD_NUEVA"

        if (
            !nueva &&
            message.data["estado"] != "COTIZACION_PENDIENTE"
        ) return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val notificaciones =
            NotificationManagerCompat.from(this)

        if (!notificaciones.areNotificationsEnabled()) return

        ReservacionesNotifications.crearCanal(this)

        val manager = getSystemService(
            NotificationManager::class.java
        )

        if (
            manager.getNotificationChannel(
                ReservacionesNotifications.CHANNEL_ID
            )?.importance ==
            NotificationManager.IMPORTANCE_NONE
        ) return

        if (
            tokenSesion !=
            ReservacionesNotifications.tokenPropietario(this)
        ) return

        val destino =
            if (nueva) {
                PanelPropietarioActivity::class.java
            } else {
                LoginActivity::class.java
            }

        val intent = Intent(this, destino).apply {
            putExtra(
                ReservacionesNotifications.EXTRA_BANDEJA,
                true
            )

            putExtra("soloNuevas", nueva)

            putExtra(
                "propietarioDestino",
                propietarioDestino
            )

            data = Uri.parse(
                "ocasion://solicitudes/" +
                        "$propietarioDestino/$idReserva"
            )

            addFlags(
                if (nueva) {
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
                } else {
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
            )
        }

        val pendiente = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notificacion = NotificationCompat.Builder(
            this,
            ReservacionesNotifications.CHANNEL_ID
        )
            .setSmallIcon(R.drawable.ic_reservacion_notification)

            .setContentTitle(
                if (nueva) {
                    "Nueva solicitud de evento"
                } else {
                    "Nueva cotización pendiente"
                }
            )

            .setContentText(
                "Abre tu bandeja para revisar la solicitud."
            )

            .setContentIntent(pendiente)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()

        try {
            notificaciones.notify(
                if (nueva) {
                    "solicitud_nueva:$idReserva"
                } else {
                    "reservacion_pendiente:$idReserva"
                },
                0,
                notificacion
            )
        } catch (_: SecurityException) {
            // El permiso pudo revocarse durante la operación.
        }
    }
}