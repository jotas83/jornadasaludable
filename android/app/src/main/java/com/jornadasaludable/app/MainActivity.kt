package com.jornadasaludable.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Host único de la app. Aloja el NavHostFragment definido en
 * res/layout/activity_main.xml; toda la navegación entre pantallas pasa por
 * res/navigation/nav_graph.xml (start = LoginFragment).
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
