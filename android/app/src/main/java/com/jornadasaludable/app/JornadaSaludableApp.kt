package com.jornadasaludable.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application root para Hilt. La anotación @HiltAndroidApp genera el código
 * que arranca el contenedor de DI y lo asocia al ciclo de vida del proceso.
 *
 * Cuando se añadan los modules (di/), aquí no hace falta tocar nada — Hilt
 * los descubre automáticamente vía @InstallIn.
 */
@HiltAndroidApp
class JornadaSaludableApp : Application()
