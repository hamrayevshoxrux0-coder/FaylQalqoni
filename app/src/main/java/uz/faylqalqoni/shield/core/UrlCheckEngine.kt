package uz.faylqalqoni.shield.core

import android.content.Context

/**
 * Link tekshiruvining ikkala bosqichini birlashtiradi:
 *  1) UrlRiskAnalyzer — tarmoqsiz, tezkor, tuzilma asosidagi qoidalar
 *  2) SafeBrowsingClient — Google'ning ma'lum xavfli saytlar bazasi (API kaliti bo'lsa)
 */
class UrlCheckEngine(private val context: Context) {

    suspend fun check(rawUrl: String): LinkCheckResult {
        val trimmed = rawUrl.trim()
        val normalized = if (trimmed.contains("://")) trimmed else "https://$trimmed"

        val reasons = mutableListOf<String>()
        var level = ThreatLevel.SAFE

        fun escalate(newLevel: ThreatLevel, reason: String) {
            reasons += reason
            if (newLevel.ordinal > level.ordinal) level = newLevel
        }

        for (finding in UrlRiskAnalyzer.analyze(normalized)) {
            escalate(finding.level, finding.reason)
        }

        var safeBrowsingChecked = false
        val apiKey = SettingsStore.getSafeBrowsingApiKey(context)
        if (apiKey.isNotBlank()) {
            when (val outcome = SafeBrowsingClient.check(apiKey, normalized)) {
                is SafeBrowsingClient.Outcome.Threat -> {
                    safeBrowsingChecked = true
                    val desc = outcome.threatTypes.joinToString(", ") {
                        SafeBrowsingClient.describeThreatType(it)
                    }
                    escalate(
                        ThreatLevel.DANGEROUS,
                        "Google Safe Browsing bazasida qayd etilgan: $desc"
                    )
                }
                is SafeBrowsingClient.Outcome.Clean -> {
                    safeBrowsingChecked = true
                }
                is SafeBrowsingClient.Outcome.Error -> {
                    // Tekshirib bo'lmadi (internet yo'q, kalit xato va h.k.) — lokal
                    // natija bilan davom etamiz, xato foydalanuvchini qo'rqitmasligi kerak.
                    safeBrowsingChecked = false
                }
            }
        }

        if (reasons.isEmpty()) {
            reasons += if (safeBrowsingChecked) {
                "Shubhali belgilar topilmadi (Google Safe Browsing va lokal qoidalar bo'yicha tekshirildi)"
            } else {
                "Shubhali belgilar topilmadi (faqat lokal qoidalar bo'yicha — Safe Browsing kaliti kiritilmagan)"
            }
        }

        return LinkCheckResult(
            url = normalized,
            level = level,
            reasons = reasons,
            safeBrowsingChecked = safeBrowsingChecked
        )
    }
}
