package uz.faylqalqoni.shield.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import uz.faylqalqoni.shield.core.ScanEngine
import uz.faylqalqoni.shield.core.ScanResult
import uz.faylqalqoni.shield.core.ThreatLevel
import uz.faylqalqoni.shield.notification.NotificationHelper
import uz.faylqalqoni.shield.quarantine.QuarantineManager
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Fon rejimida ishlab, telefonga tushayotgan yangi fayllarni (Yuklab olishlar,
 * WhatsApp, Telegram papkalari) real vaqtda kuzatib turadigan xizmat.
 *
 * Yangi fayl paydo bo'lishi bilanoq (yozish tugagach) uni ScanEngine orqali
 * tekshiradi. Agar xavfli deb topilsa — bildirishnoma ko'rsatadi va (DANGEROUS
 * darajasida) faylni avtomatik karantinga oladi.
 */
class FileMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingScans = ConcurrentHashMap<String, Runnable>()

    private lateinit var scanEngine: ScanEngine
    private lateinit var quarantineManager: QuarantineManager
    private val observers = mutableListOf<RecursiveFileObserver>()

    override fun onCreate() {
        super.onCreate()
        scanEngine = ScanEngine(applicationContext)
        quarantineManager = QuarantineManager(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        NotificationHelper.ensureChannels(this)
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID,
            NotificationHelper.buildForegroundServiceNotification(this))

        if (observers.isEmpty()) {
            startWatching()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        for (o in observers) o.stop()
        observers.clear()
        serviceJob.cancel()
    }

    private fun startWatching() {
        for (dir in monitoredDirectories()) {
            if (!dir.exists()) continue
            val observer = RecursiveFileObserver(dir, maxDepth = 5) { file ->
                scheduleScan(file)
            }
            observer.start()
            observers.add(observer)
            Log.i(TAG, "Kuzatilmoqda: ${dir.path}")
        }
    }

    /**
     * Fayl hali to'liq yozilib bo'lmagan bo'lishi mumkin (ayniqsa katta fayllar
     * yuklanayotganda), shuning uchun har bir hodisadan keyin kutish (debounce)
     * qo'llaniladi — 900ms ichida qayta hodisa kelmasa, fayl tayyor deb hisoblanadi.
     */
    private fun scheduleScan(file: File) {
        val key = file.absolutePath
        pendingScans[key]?.let { mainHandler.removeCallbacks(it) }

        val runnable = Runnable {
            pendingScans.remove(key)
            serviceScope.launch {
                try {
                    if (!scanEngine.shouldScan(file)) return@launch
                    val result = scanEngine.scanFile(file)
                    handleResult(result, file)
                } catch (e: Exception) {
                    Log.e(TAG, "Skanerlashda xato: ${file.path}", e)
                }
            }
        }
        pendingScans[key] = runnable
        mainHandler.postDelayed(runnable, DEBOUNCE_MS)
    }

    private fun handleResult(result: ScanResult, file: File) {
        if (!result.isThreat) return

        Log.w(TAG, "XAVF TOPILDI: ${result.displayName} -> ${result.reasons}")
        NotificationHelper.showThreatAlert(applicationContext, result)

        if (result.level == ThreatLevel.DANGEROUS) {
            val quarantined = quarantineManager.quarantine(file)
            Log.i(TAG, "Karantinga olindi (${quarantined}): ${file.path}")
        }
    }

    private fun monitoredDirectories(): List<File> {
        val base = Environment.getExternalStorageDirectory()
        val list = mutableListOf<File>()

        // Yuklab olishlar papkasi
        list += Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)

        // WhatsApp (yangi va eski joylashuvlar)
        list += File(base, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Documents")
        list += File(base, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images")
        list += File(base, "WhatsApp/Media/WhatsApp Documents")

        // Telegram (yangi va eski joylashuvlar)
        list += File(base, "Android/media/org.telegram.messenger/Telegram/Telegram Documents")
        list += File(base, "Telegram/Telegram Documents")

        return list.distinct()
    }

    companion object {
        private const val TAG = "FileMonitorService"
        private const val DEBOUNCE_MS = 900L

        fun start(context: Context) {
            val intent = Intent(context, FileMonitorService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FileMonitorService::class.java))
        }
    }
}
