package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import android.util.AtomicFile
import com.example.ocasion_mobil_v2.data.remote.model.OrdenPendientePaypal
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.security.MessageDigest

class OrdenesPaypalLocales(context: Context, cuenta: Int, servidor: String) {
    private val carpeta = context.noBackupFilesDir
    private val gson = Gson()
    private val ambito = MessageDigest.getInstance("SHA-256").digest("$servidor|$cuenta".toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun archivo(idReserva: Long) = AtomicFile(File(carpeta, "paypal_${ambito}_$idReserva"))

    suspend fun leer(idReserva: Long): OrdenPendientePaypal? = withContext(Dispatchers.IO) {
        try {
            archivo(idReserva).openRead().bufferedReader().use {
                gson.fromJson(it, OrdenPendientePaypal::class.java)
            }
        } catch (_: FileNotFoundException) { null }
    }

    suspend fun guardar(idReserva: Long, orden: OrdenPendientePaypal) = withContext(Dispatchers.IO) {
        val archivo = archivo(idReserva)
        val salida = archivo.startWrite()
        try {
            salida.write(gson.toJson(orden).toByteArray())
            archivo.finishWrite(salida)
        } catch (e: Exception) {
            archivo.failWrite(salida)
            throw e
        }
    }

    suspend fun borrar(idReserva: Long) = withContext(Dispatchers.IO) { archivo(idReserva).delete() }
}
