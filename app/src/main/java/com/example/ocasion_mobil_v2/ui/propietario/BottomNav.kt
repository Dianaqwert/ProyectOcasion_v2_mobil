package com.example.ocasion_mobil_v2.ui.propietario

import android.content.Intent
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R

object BottomNav {

    fun configurar(activity: ComponentActivity) {

        val navHome =
            activity.findViewById<LinearLayout>(R.id.navHome)

        val navSalons =
            activity.findViewById<LinearLayout>(R.id.navSalons)

        val navDispo =
            activity.findViewById<LinearLayout>(R.id.navDispo)

        val btnPerfil =
            activity.findViewById<LinearLayout>(R.id.btnPerfil)


        // INICIO

        navHome.setOnClickListener {

            if (activity !is OwnerHomeActivity) {

                val intent = Intent(
                    activity,
                    OwnerHomeActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                activity.startActivity(intent)
            }
        }


        // SALONES

        navSalons.setOnClickListener {

            if (activity !is MisSalonesActivity) {

                val intent = Intent(
                    activity,
                    MisSalonesActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                activity.startActivity(intent)
            }
        }


        // DISPONIBILIDAD

        navDispo.setOnClickListener {

            /*
             * La disponibilidad pertenece a un salón específico.
             *
             * Por ahora mandamos a Mis Salones para que el propietario
             * seleccione primero qué salón quiere administrar.
             *
             * Después agregaremos:
             *
             * MisSalones → Salón → Disponibilidad
             */

            if (activity !is MisSalonesActivity) {

                val intent = Intent(
                    activity,
                    MisSalonesActivity::class.java
                )

                intent.putExtra(
                    "abrirDisponibilidad",
                    true
                )

                activity.startActivity(intent)
            }
        }

        // PERFIL

        btnPerfil.setOnClickListener {

            if (activity !is PerfilPropietarioActivity) {

                val intent = Intent(
                    activity,
                    PerfilPropietarioActivity::class.java
                )

                intent.flags =
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP

                activity.startActivity(intent)
            }
        }
    }
}