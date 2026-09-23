package uz.faylqalqoni.shield.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import uz.faylqalqoni.shield.MainActivity
import uz.faylqalqoni.shield.R
import uz.faylqalqoni.shield.core.ScanResult
import uz.faylqalqoni.shield.core.ThreatLevel

object NotificationHelper {

    const val CHANNEL_ALERTS = "faylqalqoni_alerts"
    const val CHANNEL_SERVICE = "faylqalqoni_service"

    private const val ALERT_NOTIFICATION_ID_BASE = 5000
    const val SERVICE_NOTIFICATION_ID = 1

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val alerts = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.channel_alerts_desc)
        }

        val service = NotificationChannel(
            CHANNEL_SERVICE,
            context.getString(R.string.channel_service_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.channel_service_desc)
        }

        manager.createNotificationChannel(alerts)
        manager.createNotificationChannel(service)
    }

    fun buildForegroundServiceNotification(context: Context): android.app.Notification {
        val openIntent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(context, CHANNEL_SERVICE)
            .setContentTitle(context.getString(R.string.service_notification_title))
            .setContentText(context.getString(R.string.service_notification_text))
            .setSmallIcon(R.drawable.ic_shield)
            .setOngoing(true)
            .setContentIntent(pending)
            .build()
    }

    fun showThreatAlert(context: Context, result: ScanResult) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, result.path.hashCode(), openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = when (result.level) {
            ThreatLevel.DANGEROUS -> context.getString(R.string.alert_title_dangerous)
            ThreatLevel.SUSPICIOUS -> context.getString(R.string.alert_title_suspicious)
            ThreatLevel.SAFE -> return // xavfsiz bo'lsa bildirishnoma kerak emas
        }

        val text = "${result.displayName}: ${result.reasons.firstOrNull() ?: ""}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(result.reasons.joinToString("\n")))
            .setSmallIcon(R.drawable.ic_shield)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        val id = ALERT_NOTIFICATION_ID_BASE + (result.path.hashCode() and 0xFFF)
        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
