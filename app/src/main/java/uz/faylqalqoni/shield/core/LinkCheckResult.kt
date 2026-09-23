package uz.faylqalqoni.shield.core

/**
 * Bitta havola (URL) uchun tekshiruv natijasi.
 *
 * @param url Tekshirilgan (normalizatsiya qilingan) havola
 * @param level Umumiy xavf darajasi
 * @param reasons Xavf/shubha sabablari (foydalanuvchiga tushunarli tilda)
 * @param safeBrowsingChecked Google Safe Browsing bazasi orqali tekshirilganmi
 * (API kaliti kiritilmagan bo'lsa, faqat lokal qoidalar bilan tekshiriladi)
 */
data class LinkCheckResult(
    val url: String,
    val level: ThreatLevel,
    val reasons: List<String>,
    val safeBrowsingChecked: Boolean
)
