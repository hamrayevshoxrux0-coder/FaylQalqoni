package uz.faylqalqoni.shield.core

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.File

/**
 * Barcha aniqlash usullarini birlashtiruvchi asosiy dvigatel:
 *  1) Fayl nomidagi ikki karra kengaytma firibgarligi ("rasm.jpeg.apk")
 *  2) Fayl nomi va uning haqiqiy ichki formati (magic bytes) mos kelmasligi
 *  3) SHA-256 xesh bo'yicha ma'lum zararli fayllar bazasidan qidirish (signature)
 *  4) APK bo'lsa — undagi ruxsatlarni xavf darajasiga qarab tahlil qilish
 *
 * Natijada har bir tekshirilgan fayl/ilova uchun bitta [ScanResult] qaytadi.
 */
class ScanEngine(private val context: Context) {

    private val signatureDb by lazy { SignatureDatabase.get(context) }

    /** Faqat ushbu kengaytmalar chuqur tekshiriladi (rasm/hujjat/video niqobidagilar + apk/zip). */
    private val SCANNABLE_EXTENSIONS = setOf(
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "mp4", "mov", "mp3", "avi", "mkv", "3gp",
        "zip", "rar", "apk"
    )

    private val MAX_FILE_SIZE_FOR_FULL_SCAN = 200L * 1024 * 1024 // 200 MB

    fun shouldScan(file: File): Boolean {
        if (!file.isFile) return false
        val ext = FileTypeSniffer.declaredExtension(file.name) ?: return false
        return ext in SCANNABLE_EXTENSIONS
    }

    fun scanFile(file: File): ScanResult {
        val reasons = mutableListOf<String>()
        var level = ThreatLevel.SAFE

        fun escalate(newLevel: ThreatLevel, reason: String) {
            reasons += reason
            if (newLevel.ordinal > level.ordinal) level = newLevel
        }

        val declaredExt = FileTypeSniffer.declaredExtension(file.name)

        // 1) Fayl nomi naqshi: "*.jpeg.apk" kabi ikki karra kengaytma
        if (FileTypeSniffer.hasDoubleExtensionPattern(file.name)) {
            escalate(
                ThreatLevel.DANGEROUS,
                "Fayl nomi rasm/hujjat ko'rinishida, lekin aslida o'rnatiladigan dastur " +
                    "(.apk) — bu firibgarlarning eng ko'p ishlatadigan usuli"
            )
        }

        // 2) Haqiqiy fayl turini "magic bytes" orqali aniqlash
        val sniff = FileTypeSniffer.sniff(file)
        val realType = sniff?.realType ?: "UNKNOWN"

        if (sniff != null && FileTypeSniffer.isExecutableDisguisedAsMedia(declaredExt, realType)) {
            escalate(
                ThreatLevel.DANGEROUS,
                "Fayl kengaytmasi \".$declaredExt\" deb ko'rsatilgan, lekin faylning " +
                    "ichki tarkibi aslida ijro etiladigan dastur (${realType}) ekanligi aniqlandi"
            )
        }

        // 3) SHA-256 xesh bo'yicha ma'lum zararli fayllar bazasidan tekshirish
        var sha256: String? = null
        if (file.length() in 1..MAX_FILE_SIZE_FOR_FULL_SCAN) {
            sha256 = SignatureDatabase.sha256Of(file)
            val knownName = sha256?.let { signatureDb.lookup(it) }
            if (knownName != null) {
                escalate(
                    ThreatLevel.DANGEROUS,
                    "Bu fayl ma'lum zararli dasturlar bazasida qayd etilgan: \"$knownName\""
                )
            }
        }

        // 4) Agar haqiqatda APK bo'lsa — ruxsatlarni tahlil qilish
        if (realType == "ZIP/APK" || declaredExt == "apk") {
            val permInfo = readApkPermissions(file)
            if (permInfo != null) {
                for (finding in PermissionRiskAnalyzer.analyze(permInfo)) {
                    escalate(finding.level, finding.reason)
                }
            }
        }

        if (reasons.isEmpty()) {
            reasons += "Shubhali belgilar topilmadi"
        }

        return ScanResult(
            path = file.absolutePath,
            displayName = file.name,
            level = level,
            reasons = reasons,
            sha256 = sha256,
            declaredExtension = declaredExt,
            realFileType = realType,
            isInstalledApp = false
        )
    }

    /** APK faylidan (hali o'rnatilmagan bo'lsa ham) so'ralayotgan ruxsatlarni o'qiydi. */
    private fun readApkPermissions(file: File): List<String>? {
        return try {
            val pm = context.packageManager
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
            } else null

            @Suppress("DEPRECATION")
            val info = if (flags != null) {
                pm.getPackageArchiveInfo(file.absolutePath, flags)
            } else {
                pm.getPackageArchiveInfo(file.absolutePath, PackageManager.GET_PERMISSIONS)
            }
            info?.requestedPermissions?.toList()
        } catch (e: Exception) {
            Log.e("ScanEngine", "APK ruxsatlarini o'qib bo'lmadi: ${file.path}", e)
            null
        }
    }

    /** Allaqachon o'rnatilgan bitta ilovani (paket nomi bo'yicha) tekshiradi. */
    fun scanInstalledPackage(packageName: String): ScanResult? {
        return try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            val perms = info.requestedPermissions?.toList() ?: emptyList()
            val appLabel = try {
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            } catch (e: Exception) {
                packageName
            }

            val reasons = mutableListOf<String>()
            var level = ThreatLevel.SAFE
            for (finding in PermissionRiskAnalyzer.analyze(perms)) {
                reasons += finding.reason
                if (finding.level.ordinal > level.ordinal) level = finding.level
            }
            if (reasons.isEmpty()) reasons += "Shubhali ruxsatlar kombinatsiyasi topilmadi"

            ScanResult(
                path = packageName,
                displayName = appLabel,
                level = level,
                reasons = reasons,
                isInstalledApp = true
            )
        } catch (e: Exception) {
            Log.e("ScanEngine", "Paketni tekshirib bo'lmadi: $packageName", e)
            null
        }
    }

    /**
     * Berilgan papkani (va ichki papkalarni, chegaralangan chuqurlikda) aylanib chiqib,
     * har bir mos keladigan faylni tekshiradi. Katta fayl tizimlarida bloklanib
     * qolmaslik uchun chuqurlik va fayl-soni chegaralari qo'yilgan.
     */
    fun scanDirectory(
        root: File,
        maxDepth: Int = 6,
        maxFiles: Int = 5000,
        onlyThreats: Boolean = false,
        onResult: (ScanResult) -> Unit
    ) {
        var scanned = 0
        fun walk(dir: File, depth: Int) {
            if (depth > maxDepth || scanned >= maxFiles) return
            val children = dir.listFiles() ?: return
            for (child in children) {
                if (scanned >= maxFiles) return
                if (child.isDirectory) {
                    // Yashirin tizim papkalarini o'tkazib yuborish (masalan .thumbnails)
                    if (!child.name.startsWith(".")) {
                        walk(child, depth + 1)
                    }
                } else if (shouldScan(child)) {
                    scanned++
                    val result = scanFile(child)
                    if (!onlyThreats || result.isThreat) {
                        onResult(result)
                    }
                }
            }
        }
        walk(root, 0)
    }
}
