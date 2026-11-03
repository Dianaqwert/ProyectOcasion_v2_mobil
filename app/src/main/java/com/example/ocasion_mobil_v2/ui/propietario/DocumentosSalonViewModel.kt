package com.example.ocasion_mobil_v2.ui.propietario

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.ocasion_mobil_v2.data.remote.model.SalonPropietarioDTO
import com.example.ocasion_mobil_v2.data.repository.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class DocumentosSalonState(
    val salon: SalonPropietarioDTO? = null,
    val archivos: Map<TipoDocumento, List<ArchivoDocumento>> = emptyMap(),
    val ocupado: Boolean = false,
    val fase: String = "",
    val error: String? = null,
    val enviado: Boolean = false
) {
    val revision get() = salon?.documentosValidacionSalonEntity?.estadoRevisionAdmin
    val bloqueado get() = revision in listOf("PENDIENTE", "APROBADO")
    val completo get() = TipoDocumento.entries.all { archivos[it].orEmpty().size in it.minimo..it.maximo }
    val bytes get() = archivos.values.flatten().sumOf { it.bytes }
}

class DocumentosSalonViewModel(app: Application, private val saved: SavedStateHandle) : AndroidViewModel(app) {
    private val repo = DocumentosSalonRepository(app)
    private val mutable = MutableStateFlow(DocumentosSalonState())
    val state = mutable.asStateFlow()
    private var id = -1

    fun iniciar(idSalon: Int) {
        if (id > 0) return
        id = idSalon
        viewModelScope.launch {
            tarea("Cargando expediente…") {
                refrescar()
                val restaurados = TipoDocumento.entries.associateWith { tipo ->
                    saved.get<ArrayList<String>>(tipo.campo).orEmpty().mapNotNull {
                        try { repo.inspeccionar(Uri.parse(it), tipo) }
                        catch (e: CancellationException) { throw e }
                        catch (_: Exception) { null }
                    }
                }
                mutable.update { it.copy(archivos = restaurados) }
            }
        }
    }

    fun actualizar() {
        if (state.value.ocupado) return
        viewModelScope.launch { tarea("Consultando estado…") { refrescar() } }
    }

    private suspend fun refrescar() {
        val salon = repo.salon(id)
        mutable.update { it.copy(salon = salon, enviado = salon.documentosValidacionSalonEntity?.estadoRevisionAdmin == "PENDIENTE") }
    }

    fun seleccionar(tipo: TipoDocumento, uris: List<Uri>) {
        if (state.value.bloqueado || uris.isEmpty()) return
        // Un resultado del selector puede llegar mientras se restaura la pantalla y consulta el servidor.
        if (state.value.ocupado) {
            viewModelScope.launch {
                state.first { !it.ocupado }
                seleccionar(tipo, uris)
            }
            return
        }
        viewModelScope.launch {
            tarea("Revisando archivos…") {
                val anteriores = if (tipo.maximo == 1) emptyList() else state.value.archivos[tipo].orEmpty()
                val nuevos = (anteriores.map { it.uri } + uris).distinct()
                require(nuevos.size <= tipo.maximo) { "${tipo.titulo}: máximo ${tipo.maximo} archivos." }
                val archivos = nuevos.map { repo.inspeccionar(it, tipo) }
                val seleccion = state.value.archivos + (tipo to archivos)
                require(seleccion.values.sumOf { it.size } <= 100) { "El servidor admite hasta 100 archivos en una solicitud." }
                require(seleccion.values.flatten().sumOf { it.bytes } <= DocumentosSalonRepository.MAX_TOTAL) { "La selección supera los 99 MB. Reduce el tamaño de las fotos." }
                mutable.update { it.copy(archivos = seleccion) }
                guardarSeleccion()
            }
        }
    }

    fun quitar(tipo: TipoDocumento, archivo: ArchivoDocumento) {
        if (state.value.ocupado || state.value.bloqueado) return
        mutable.update { it.copy(archivos = it.archivos + (tipo to it.archivos[tipo].orEmpty().filterNot { f -> f.uri == archivo.uri }), error = null) }
        guardarSeleccion()
    }

    private fun guardarSeleccion() {
        saved["documentosSubidos"] = false
        TipoDocumento.entries.forEach { tipo -> saved[tipo.campo] = ArrayList(state.value.archivos[tipo].orEmpty().map { it.uri.toString() }) }
    }

    fun enviar() {
        if (state.value.ocupado || state.value.bloqueado) return
        viewModelScope.launch {
            tarea("Consultando estado…") {
                refrescar()
                check(!state.value.bloqueado) { "El expediente ya está en revisión o aprobado." }
                val yaGuardados = state.value.revision == "SIN_ENVIAR" &&
                    (saved.get<Boolean>("documentosSubidos") == true || state.value.archivos.values.all { it.isEmpty() })
                if (!yaGuardados) {
                    check(state.value.completo) {
                        "Completa los cuatro documentos, de 1 a 10 fotos del salón y al menos 3 fotos del propietario. Para corregir, vuelve a seleccionar todo el expediente."
                    }
                    mutable.update { it.copy(fase = "Subiendo documentos…") }
                    // Revalidar permisos, contenido y tamaños antes de comenzar la petición.
                    val seleccion = state.value.archivos.mapValues { (tipo, lista) -> lista.map { repo.inspeccionar(it.uri, tipo) } }
                    require(seleccion.values.flatten().sumOf { it.bytes } <= DocumentosSalonRepository.MAX_TOTAL) { "La selección supera los 99 MB." }
                    if (state.value.salon?.estado != "PUBLICADO") {
                        check(state.value.salon?.estado in listOf("BORRADOR", "RECHAZADO")) { "Este salón no admite publicación ni carga de documentos en su estado actual." }
                        mutable.update { it.copy(fase = "Publicando salón…") }
                        repo.publicar(id)
                        refrescar()
                    }
                    mutable.update { it.copy(fase = "Subiendo documentos…") }
                    repo.subir(id, seleccion)
                    saved["documentosSubidos"] = true
                    refrescar()
                }
                mutable.update { it.copy(fase = "Enviando a revisión…") }
                repo.enviar(id)
                mutable.update { estado -> estado.copy(enviado = true, salon = estado.salon?.copy(
                    documentosValidacionSalonEntity = estado.salon.documentosValidacionSalonEntity?.copy(estadoRevisionAdmin = "PENDIENTE"))) }
                refrescar()
            }
        }
    }

    private suspend fun tarea(fase: String, bloque: suspend () -> Unit) {
        mutable.update { it.copy(ocupado = true, fase = fase, error = null) }
        try { bloque() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) {
            mutable.update { it.copy(error = if (e is java.io.IOException) "No se pudo conectar. Tus selecciones se conservan; puedes reintentar." else e.message ?: "No se pudo completar la operación.") }
        } finally { mutable.update { it.copy(ocupado = false, fase = "") } }
    }
}
