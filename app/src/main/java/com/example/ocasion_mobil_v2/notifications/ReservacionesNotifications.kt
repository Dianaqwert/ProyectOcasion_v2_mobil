package com.example.ocasion_mobil_v2.notifications

import android.Manifest
import android.app.Activity
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientReservaciones
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.example.ocasion_mobil_v2.data.repository.ErrorReservaciones
import com.example.ocasion_mobil_v2.data.repository.ReservacionesRepository
import com.example.ocasion_mobil_v2.data.session.SessionManager
import com.example.ocasion_mobil_v2.ui.login.LoginActivity
import com.example.ocasion_mobil_v2.util.JwtUtils
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/** Consulta mientras la app está visible. Android cancela el sondeo al salir de la app. */
class OcasionApplication : Application(), Application.ActivityLifecycleCallbacks {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var visibles = 0
    private var sondeo: Job? = null
    override fun onCreate() {
        super.onCreate()
        ReservacionesNotifications.crearCanal(this)
        registerActivityLifecycleCallbacks(this)
    }
    override fun onActivityStarted(activity: Activity) {
        visibles++
        if (ReservacionesNotifications.tokenPropietario(this) == null) return
        ReservacionesNotifications.programar(this)
        if (sondeo?.isActive != true) sondeo = scope.launch {
            while (isActive) {
                try { ReservacionesNotifications.consultar(this@OcasionApplication) }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { /* La bandeja ofrece actualización y errores visibles. */ }
                delay(30_000)
            }
        }
    }
    override fun onActivityStopped(activity: Activity) { if (--visibles == 0) { sondeo?.cancel(); sondeo = null } }
    override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}

object ReservacionesNotifications {
    const val CHANNEL_ID = "solicitudes_propietario_v1"
    const val EXTRA_BANDEJA = "abrirBandejaReservaciones"
    private const val WORK_NAME = "reservaciones_propietario"
    private const val NOTIFICATION_TAG = "reservacion_pendiente"
    fun esPropietario(rol: String?) = rol in setOf("VENDEDOR", "PROPIETARIO")
    fun esEntradaBandeja(intent: Intent) = intent.getBooleanExtra(EXTRA_BANDEJA, false)
    fun tokenPropietario(context: Context): String? = SessionManager(context).obtenerToken()
        ?.takeIf { esPropietario(JwtUtils.obtenerTipoUser(it)) }

    fun crearCanal(context: Context) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reserva_canal), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.reserva_canal_descripcion)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 250, 120, 250, 120, 450)
                setSound(Uri.parse("android.resource://${context.packageName}/${R.raw.nueva_solicitud}"),
                    AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            }
        )
    }

    fun programar(context: Context) {
        if (tokenPropietario(context) == null) return
        val request = PeriodicWorkRequestBuilder<ReservacionesWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun detener(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        context.getSharedPreferences("ocasion_alertas_reservas", Context.MODE_PRIVATE).edit().clear().apply()
        context.getSystemService(NotificationManager::class.java).activeNotifications
            .filter { it.tag?.startsWith("$NOTIFICATION_TAG:") == true }.forEach { NotificationManagerCompat.from(context).cancel(it.tag, it.id) }
    }

    suspend fun consultar(context: Context) {
        val token = tokenPropietario(context) ?: return
        val pendientes = ReservacionesRepository(RetrofitClientReservaciones.getService(context)).pendientes()
        publicarPendientes(context, pendientes, token)
    }

    @Synchronized fun publicarPendientes(context: Context, pendientes: List<Reservacion>, token: String) {
        if (token != tokenPropietario(context)) return // Ignora respuestas de una sesión anterior.
        val prefs = context.getSharedPreferences("ocasion_alertas_reservas", Context.MODE_PRIVATE)
        val conocidos = prefs.getStringSet("solicitudes_vistas", emptySet()).orEmpty()
        val ids = pendientes.map { it.id.toString() }.toSet()
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.activeNotifications.filter { it.tag?.startsWith("$NOTIFICATION_TAG:") == true && it.tag.removePrefix("$NOTIFICATION_TAG:") !in ids }
            .forEach { manager.cancel(it.tag, it.id) }
        // Conserva los IDs sólo después de mostrarlos o cuando ya no están pendientes.
        // Si el permiso está desactivado se podrán alertar al habilitarlo más adelante.
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        crearCanal(context)
        if (manager.getNotificationChannel(CHANNEL_ID)?.importance == NotificationManager.IMPORTANCE_NONE) return
        pendientes.filter { it.id.toString() !in conocidos }.forEach { r -> mostrar(context, r) }
        prefs.edit().putStringSet("solicitudes_vistas", ids).apply()
    }

    fun quitar(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel("$NOTIFICATION_TAG:$id", 0)
    }

    private fun mostrar(context: Context, reserva: Reservacion) {
        val id = reserva.id.toInt()
        val entrada = Intent(context, LoginActivity::class.java).putExtra(EXTRA_BANDEJA, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, id, entrada, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val body = context.getString(R.string.reserva_push_cuerpo, reserva.nombreSalon ?: "tu salón")
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_reservacion_notification).setContentTitle(context.getString(R.string.reserva_push_titulo))
            .setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pending).setAutoCancel(true).setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).build()
        try { NotificationManagerCompat.from(context).notify("$NOTIFICATION_TAG:${reserva.id}", 0, notification) }
        catch (_: SecurityException) { /* Permiso revocado durante el envío. */ }
    }
}

class ReservacionesWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result = try {
        ReservacionesNotifications.consultar(applicationContext)
        Result.success()
    } catch (e: CancellationException) { throw e }
    catch (e: ErrorReservaciones) {
        if (e.codigo == 401 || e.codigo == 403) Result.failure() else Result.retry()
    } catch (_: Exception) { Result.retry() }
}
