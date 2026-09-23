package uz.faylqalqoni.shield.core

/**
 * Aniqlangan xavf darajasi.
 */
enum class ThreatLevel {
    SAFE,        // Shubha yo'q
    SUSPICIOUS,  // Ehtiyot bo'lish kerak, lekin tasdiqlanmagan
    DANGEROUS    // Yuqori ishonch bilan xavfli deb topilgan
}
