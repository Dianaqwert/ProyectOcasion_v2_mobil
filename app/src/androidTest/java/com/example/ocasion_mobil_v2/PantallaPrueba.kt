package com.example.ocasion_mobil_v2

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

internal fun abrirAppParaPrueba() {
    val prueba = InstrumentationRegistry.getInstrumentation()
    val paquete = prueba.targetContext.packageName
    val salida = prueba.uiAutomation.executeShellCommand("am start -W -n $paquete/.ui.login.LoginActivity")
    ParcelFileDescriptor.AutoCloseInputStream(salida).use { it.readBytes() }
    prueba.waitForIdleSync()
}
