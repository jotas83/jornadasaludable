package com.jornadasaludable.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jornadasaludable.app.MainActivity
import com.jornadasaludable.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Servicio de notificaciones locales para alertas legales (PRL).
 *
 * Canal único `ALERTAS_PRL` con importancia HIGH (heads-up + sonido).
 * El canal se crea idempotentemente desde Application.onCreate vía
 * ensureChannel(); en API < 26 es no-op.
 *
 * showAlerta() comprueba el permiso POST_NOTIFICATIONS (necesario en API 33+);
 * si no se ha concedido, hace un log y omite — no fallamos el flujo de fichaje
 * por una notificación que el usuario decidió no recibir.
 */
@Singleton
class AlertaNotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        const val CHANNEL_ID   = "ALERTAS_PRL"
        const val CHANNEL_NAME = "Alertas PRL"
        const val CHANNEL_DESC = "Alertas legales sobre tu jornada laboral (Art. 34 ET y derivados)."
    }

    fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = CHANNEL_DESC
            enableLights(true)
            enableVibration(true)
        }
        nm.createNotificationChannel(channel)
    }

    /**
     * Muestra una notificación. `notificationId` debe ser único por alerta —
     * uuid.hashCode() es lo recomendado para que no se solapen ni dupliquen.
     */
    fun showAlerta(
        notificationId: Int,
        tituloTipo: String,
        mensaje: String,
        baseLegal: String?,
    ) {
        if (!hasPostNotificationsPermission()) return

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pi = PendingIntent.getActivity(
            context,
            notificationId,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val bigText = buildString {
            append(mensaje)
            if (!baseLegal.isNullOrBlank()) {
                append("\n\n")
                append(baseLegal)
            }
        }

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_nav_alertas)
            .setContentTitle(tituloTipo)
            .setContentText(mensaje)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationId, notif)
        }
    }

    private fun hasPostNotificationsPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }
}
