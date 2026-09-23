package uz.faylqalqoni.shield.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import uz.faylqalqoni.shield.core.ScanEngine
import uz.faylqalqoni.shield.core.ThreatLevel
import uz.faylqalqoni.shield.notification.NotificationHelper

/**
 * Har safar yangi ilova o'rnatilganda yoki yangilanganda ishga tushadi va
 * uning so'ragan ruxsatlarini darhol tekshiradi. Shu tarzda, agar foydalanuvchi
 * qandaydir yo'l bilan (masalan fayl skaneridan tashqarida, brauzerdan
 * yuklab) shubhali ilovani o'rnatib qo'ysa ham, darhol ogohlantirish oladi.
 */
class PackageInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action != Intent.ACTION_PACKAGE_ADDED && action != Intent.ACTION_PACKAGE_REPLACED) return

        // O'zimizning ilovamiz uchun ishlamasin
        val packageName = intent.data?.schemeSpecificPart ?: return
        if (packageName == context.packageName) return

        val appContext = context.applicationContext
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val engine = ScanEngine(appContext)
                val result = engine.scanInstalledPackage(packageName)
                if (result != null && result.level != ThreatLevel.SAFE) {
                    NotificationHelper.ensureChannels(appContext)
                    NotificationHelper.showThreatAlert(appContext, result)
                    Log.w(TAG, "Yangi o'rnatilgan ilovada xavf: $packageName -> ${result.reasons}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Yangi ilovani tekshirishda xato: $packageName", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "PackageInstallReceiver"
    }
}
