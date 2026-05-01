package com.jornadasaludable.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint

/**
 * Host único. Aloja:
 *   - NavHostFragment con el nav_graph (start = login).
 *   - BottomNavigationView con dos destinos top-level: Dashboard y Fichaje.
 *
 * El bottom nav se oculta en la pantalla de login (donde el usuario aún no
 * tiene sesión) y se muestra a partir de Dashboard/Fichaje.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)
        bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            // Sólo visible cuando hay sesión iniciada.
            bottomNav.isVisible = destination.id != R.id.loginFragment
        }
    }
}
