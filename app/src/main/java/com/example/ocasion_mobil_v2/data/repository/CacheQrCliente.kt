package com.example.ocasion_mobil_v2.data.repository

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.example.ocasion_mobil_v2.data.remote.model.Reservacion
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class PaseQr(val id: Long, val salon: String?, val fecha: String?, val token: String)

internal fun Reservacion.paseQr(): PaseQr? = ReglasPagoCliente.qr(this)?.let {
    PaseQr(id, nombreSalon, fechaInicio, it)
}

internal fun combinarPasesQr(
    anteriores: List<PaseQr>,
    cambios: Map<Long, PaseQr?>,
    idsVigentes: Set<Long>?
): List<PaseQr> {
    val pases = anteriores.associateBy { it.id }.toMutableMap()
    if (idsVigentes != null) pases.keys.retainAll(idsVigentes)
    cambios.forEach { (id, pase) -> if (pase == null) pases.remove(id) else pases[id] = pase }
    return pases.values.toList()
}

/** Los pases se conservan al actualizar la app y se excluyen de las copias de seguridad. */
class CacheQrCliente(context: Context, private val cuenta: Int) {
    private val archivo = AtomicFile(File(context.noBackupFilesDir, "pases_reserva_$cuenta"))
    private val gson = Gson()

    suspend fun leer(): List<PaseQr> = withContext(Dispatchers.IO) {
        synchronized(lock) { leerLocal() }
    }

    suspend fun actualizar(cambios: Map<Long, PaseQr?>, idsVigentes: Set<Long>? = null): List<PaseQr> =
        withContext(Dispatchers.IO) {
            synchronized(lock) {
                val anteriores = leerLocal()
                val resultado = combinarPasesQr(anteriores, cambios, idsVigentes)
                if (resultado == anteriores) return@synchronized resultado
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, clave())
                cipher.updateAAD(cuenta.toString().toByteArray())
                val datos = cipher.iv + cipher.doFinal(gson.toJson(resultado).toByteArray())
                val salida = archivo.startWrite()
                try {
                    salida.write(datos)
                    archivo.finishWrite(salida)
                } catch (e: Exception) {
                    archivo.failWrite(salida)
                    throw e
                }
                resultado
            }
        }

    private fun leerLocal(): List<PaseQr> {
        val datos = try {
            archivo.openRead().use { it.readBytes() }
        } catch (_: FileNotFoundException) {
            return emptyList()
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, datos.copyOfRange(0, 12)))
        cipher.updateAAD(cuenta.toString().toByteArray())
        return gson.fromJson(cipher.doFinal(datos, 12, datos.size - 12).decodeToString(), Array<PaseQr>::class.java)
            .toList()
    }

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        return generador.generateKey()
    }

    private companion object {
        const val ALIAS = "ocasion_pases_reserva_v1"
        val lock = Any()
    }
}
