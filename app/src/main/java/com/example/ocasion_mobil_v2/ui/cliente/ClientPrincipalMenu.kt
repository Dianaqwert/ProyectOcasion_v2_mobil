package com.example.ocasion_mobil_v2.ui.cliente

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ocasion_mobil_v2.R

class ClientPrincipalMenu : ComponentActivity() {

    private lateinit var rvSalones: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var btnFiltros: ImageButton
    private lateinit var adapter: SalonAdapterClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cliente_menu_principal)

        rvSalones = findViewById(R.id.rvSalonesHorizontales)
        searchView = findViewById(R.id.searchView)
        btnFiltros = findViewById(R.id.btnFiltros)

        setupRecyclerView()
        setupSearchView()
        setupFiltros()
    }

    private fun setupRecyclerView() {
        // Configuración requerida: LinearLayoutManager.HORIZONTAL
        rvSalones.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        adapter = SalonAdapterClient(emptyList()) { salonSeleccionado ->
            Toast.makeText(this, "Seleccionado: ${salonSeleccionado.nombre}", Toast.LENGTH_SHORT).show()
        }
        rvSalones.adapter = adapter

        // Aquí pasarás la lista que venga de tu backend Retrofit
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Filtrar lista o disparar llamada al endpoint
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filtrar en tiempo real
                return true
            }
        })
    }

    private fun setupFiltros() {
        btnFiltros.setOnClickListener {
            // Abrir BottomSheetDialogFragment de filtros
            Toast.makeText(this, "Abrir panel de filtros", Toast.LENGTH_SHORT).show()
        }
    }
}