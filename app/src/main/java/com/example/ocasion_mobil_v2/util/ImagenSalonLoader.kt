package com.example.ocasion_mobil_v2.util

import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.load.model.Headers
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.example.ocasion_mobil_v2.R
import com.example.ocasion_mobil_v2.data.remote.RetrofitClientSalones
import com.example.ocasion_mobil_v2.data.session.SessionManager
import okhttp3.HttpUrl.Companion.toHttpUrl

object ImagenSalonLoader {
    internal data class ImagenRemota(val url: String, val headers: Map<String, String>)

    internal fun modelo(valor: String?, baseUrl: String, token: String?): Any? {
        val fuente = valor?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (fuente.startsWith("data:image/")) return fuente
        if (fuente.startsWith("content://") || fuente.startsWith("file://")) return fuente
        val base = baseUrl.toHttpUrl()
        val url = base.resolve(fuente) ?: return null
        val headers = mutableMapOf<String, String>()
        if (url.scheme == base.scheme && url.host == base.host && url.port == base.port && !token.isNullOrBlank()) {
            headers["Authorization"] = "Bearer $token"
        }
        return ImagenRemota(url.toString(), headers)
    }

    fun cargar(vista: ImageView, fuente: String?, esquinasRedondeadasPx: Int = 0) {
        val modelo = modelo(fuente, RetrofitClientSalones.BASE_URL_SALONES,
            SessionManager(vista.context).obtenerToken())
        val solicitud = Glide.with(vista)
            .load(if (modelo is ImagenRemota) GlideUrl(modelo.url, Headers { modelo.headers }) else modelo)
            // Respeta Cache-Control: no-store del endpoint de fotos privadas.
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .timeout(30_000)
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .centerCrop()
        if (esquinasRedondeadasPx > 0) {
            solicitud.transform(CenterCrop(), RoundedCorners(esquinasRedondeadasPx))
        }
        solicitud.into(vista)
    }
}
