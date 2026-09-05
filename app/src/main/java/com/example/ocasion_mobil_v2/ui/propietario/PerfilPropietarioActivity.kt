package com.example.ocasion_mobil_v2.ui.propietario

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.example.ocasion_mobil_v2.R

class PerfilPropietarioActivity  : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.owner_profile)

        BottomNav.configurar(this)
    }
}