package uz.faylqalqoni.shield.core

/**
 * Bitta fayl yoki o'rnatilgan ilova uchun skanerlash natijasi.
 *
 * @param path Faylning to'liq manzili (o'rnatilgan ilova uchun paket nomi)
 * @param displayName Foydalanuvchiga ko'rsatiladigan nom
 * @param level Umumiy xavf darajasi (eng yuqori sababga qarab)
 * @param reasons Nima sababdan shubhali/xavfli deb topilgani (foydalanuvchiga tushunarli tilda)
 * @param sha256 Faylning SHA-256 xesh summasi (agar hisoblangan bo'lsa)
 * @param declaredExtension Fayl nomidagi kengaytma (masalan "jpeg")
 * @param realFileType Fayl "magic bytes"i orqali aniqlangan haqiqiy turi (masalan "APK/ZIP")
 * @param isInstalledApp true bo'lsa, bu allaqachon o'rnatilgan ilova, fayl emas
 */
data class ScanResult(
    val path: String,
    val displayName: String,
    val level: ThreatLevel,
    val reasons: List<String>,
    val sha256: String? = null,
    val declaredExtension: String? = null,
    val realFileType: String? = null,
    val isInstalledApp: Boolean = false,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    val isThreat: Boolean get() = level != ThreatLevel.SAFE
}
