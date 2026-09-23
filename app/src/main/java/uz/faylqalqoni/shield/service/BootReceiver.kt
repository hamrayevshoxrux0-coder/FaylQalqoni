package uz.faylqalqoni.shield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Qurilma qayta yoqilganda, agar foydalanuvchi monitoringni avval yoqqan bo'lsa,
 * FileMonitorService xizmatini avtomatik qayta ishga tushiradi.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val monitoringEnabled = prefs.getBoolean(KEY_MONITORING_ENABLED, false)
        if (monitoringEnabled) {
            FileMonitorService.start(context)
        }
    }

    companion object {
        const val PREFS_NAME = "faylqalqoni_prefs"
        const val KEY_MONITORING_ENABLED = "monitoring_enabled"
    }
}
