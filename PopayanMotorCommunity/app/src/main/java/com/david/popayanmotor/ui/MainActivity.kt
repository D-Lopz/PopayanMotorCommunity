package com.david.popayanmotor.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.david.popayanmotor.R
import com.david.popayanmotor.data.SessionManager
import com.david.popayanmotor.databinding.ActivityMainBinding

/** Contenedor de las pestañas inferiores; cada pestaña es un Fragment. */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        SessionManager.init(this)  // por si Android recreó la app directamente en esta pantalla
        // Sesiones guardadas antes de la v1.2 no tienen el id del miembro: se pide login una vez.
        if (SessionManager.id(this) == null) {
            logout()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> show(EventsFragment.home())
                R.id.nav_events -> show(EventsFragment())
                R.id.nav_garage -> show(GarageFragment())
                R.id.nav_profile -> show(ProfileFragment())
            }
            true
        }
        // Al rotar, el FragmentManager restaura la pestaña solo; únicamente la primera vez se elige Inicio.
        if (savedInstanceState == null) binding.bottomNav.selectedItemId = R.id.nav_home
    }

    /** Accesos del Perfil ("Mi garaje", "Administración") que llevan a otra pestaña. */
    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    /** Cierra la sesión y vuelve al login (botón de Perfil o token vencido → 401). */
    fun logout() {
        SessionManager.clear(this)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
