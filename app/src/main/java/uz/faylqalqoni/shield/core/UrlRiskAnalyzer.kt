package uz.faylqalqoni.shield.core

import java.net.URI

/**
 * Havolani (URL) tarmoqqa so'rov yubormasdan, faqat uning tuzilishiga qarab
 * baholaydigan qoidalar to'plami. Bu "signature"siz, ya'ni hali hech qayerda
 * qayd etilmagan yangi firibgarlik saytlarini ham (100% kafolatsiz) ushlab
 * qolishga yordam beradi.
 *
 * Eslatma: domen tahlili soddalashtirilgan (eTLD+1'ni to'liq to'g'ri
 * hisoblamaydi, masalan ".co.uk" kabi murakkab holatlarda) — bu chuqur emas,
 * keng qamrovli tekshiruv uchun mo'ljallangan.
 */
object UrlRiskAnalyzer {

    data class Finding(val level: ThreatLevel, val reason: String)

    // Ko'p ishlatiladigan havola qisqartirgichlar — asl manzilni yashiradi
    private val URL_SHORTENERS = setOf(
        "bit.ly", "tinyurl.com", "cutt.ly", "t.co", "is.gd", "goo.gl",
        "ow.ly", "rebrand.ly", "clck.ru", "short.io", "shorturl.at", "v.gd"
    )

    // Brend nomi -> shu brendning haqiqiy domenlari.
    // Agar URL manzilida brend nomi ishlatilgan bo'lsa-yu, lekin haqiqiy
    // domen shu ro'yxatga mos kelmasa — bu klassik "brendga taqlid" firibgarligi.
    private val BRAND_OFFICIAL_DOMAINS = mapOf(
        "whatsapp" to setOf("whatsapp.com"),
        "telegram" to setOf("telegram.org", "t.me"),
        "instagram" to setOf("instagram.com"),
        "facebook" to setOf("facebook.com", "fb.com"),
        "google" to setOf("google.com", "google.co.uz", "gmail.com"),
        "paypal" to setOf("paypal.com"),
        "humo" to setOf("humocard.uz"),
        "uzcard" to setOf("uzcard.uz"),
        "click" to setOf("click.uz"),
        "payme" to setOf("payme.uz"),
        "hamkorbank" to setOf("hamkorbank.uz"),
        "kapitalbank" to setOf("kapitalbank.uz"),
        "ipotekabank" to setOf("ipotekabank.uz"),
        "asakabank" to setOf("asakabank.uz"),
        "aloqabank" to setOf("aloqabank.uz"),
        "agrobank" to setOf("agrobank.uz"),
        "xalqbank" to setOf("xalq-bank.uz", "xb.uz"),
        "tbc" to setOf("tbcbank.uz")
    )

    private val SUSPICIOUS_KEYWORDS = setOf(
        "login", "verify", "secure", "update", "confirm", "account",
        "bonus", "gift", "prize", "win", "free", "kirish", "tasdiqlash",
        "yutuq", "sovg'a", "bonus"
    )

    private val IPV4_REGEX = Regex("^(\\d{1,3}\\.){3}\\d{1,3}$")

    fun analyze(rawUrl: String): List<Finding> {
        val findings = mutableListOf<Finding>()

        val normalized = if (rawUrl.contains("://")) rawUrl else "https://$rawUrl"
        val uri = try {
            URI(normalized)
        } catch (e: Exception) {
            findings += Finding(ThreatLevel.SUSPICIOUS, "Havola formati tushunarsiz yoki buzilgan")
            return findings
        }

        val host = uri.host?.lowercase() ?: run {
            findings += Finding(ThreatLevel.SUSPICIOUS, "Havolada domen (host) topilmadi")
            return findings
        }

        // 1) IP-manzil domen sifatida
        if (IPV4_REGEX.matches(host)) {
            findings += Finding(
                ThreatLevel.DANGEROUS,
                "Havola oddiy domen nomi emas, balki IP-manzil orqali berilgan — " +
                    "bu qonuniy saytlarda kamdan-kam uchraydi"
            )
        }

        // 2) Punycode / IDN — lotin bo'lmagan/almashtirilgan harflar orqali domenni taqlid qilish
        if (host.contains("xn--")) {
            findings += Finding(
                ThreatLevel.SUSPICIOUS,
                "Domen nomida maxsus kodlangan (IDN/punycode) belgilar bor — " +
                    "harflari o'xshash boshqa alifbo bilan almashtirilgan bo'lishi mumkin"
            )
        }

        // 3) Ma'lum havola-qisqartirgichlar
        if (host in URL_SHORTENERS) {
            findings += Finding(
                ThreatLevel.SUSPICIOUS,
                "Bu qisqartirilgan havola — asl manzil yashiringan, qayerga olib " +
                    "borishini oldindan bilib bo'lmaydi"
            )
        }

        // 4) Brendga taqlid qilish
        val labels = host.split(".")
        val registrableDomain = if (labels.size >= 2) labels.takeLast(2).joinToString(".") else host
        for ((brand, officialDomains) in BRAND_OFFICIAL_DOMAINS) {
            if (host.contains(brand) && officialDomains.none { host == it || host.endsWith(".$it") }) {
                findings += Finding(
                    ThreatLevel.DANGEROUS,
                    "Havola \"$brand\" nomidan foydalanadi, lekin haqiqiy domeni " +
                        "($registrableDomain) shu tashkilotning rasmiy saytiga mos kelmaydi — " +
                        "bu soxta/firibgar sayt belgisi"
                )
                break // bitta mos kelgan brend yetarli, qo'shimcha shovqin kerak emas
            }
        }

        // 5) Juda ko'p chiziqcha yoki subdomen darajasi
        val hyphenCount = host.count { it == '-' }
        val subdomainLevels = labels.size
        if (hyphenCount >= 3 || subdomainLevels >= 5) {
            findings += Finding(
                ThreatLevel.SUSPICIOUS,
                "Domen nomi g'ayrioddiy murakkab (ko'p chiziqcha/subdomen) — " +
                    "firibgar saytlarda ko'p uchraydigan uslub"
            )
        }

        // 6) Shubhali kalit so'zlar (ayniqsa brend nomi bilan birga bo'lsa)
        val fullUrlLower = normalized.lowercase()
        val keywordHits = SUSPICIOUS_KEYWORDS.filter { fullUrlLower.contains(it) }
        if (keywordHits.isNotEmpty() && findings.any { it.level == ThreatLevel.DANGEROUS }) {
            findings += Finding(
                ThreatLevel.DANGEROUS,
                "Manzilda \"${keywordHits.first()}\" kabi ishontiruvchi so'zlar bilan birga " +
                    "shubhali domen ishlatilgan — tasdiqlash/kirish sahifasiga taqlid qilinayotgan bo'lishi mumkin"
            )
        } else if (keywordHits.size >= 2) {
            findings += Finding(
                ThreatLevel.SUSPICIOUS,
                "Manzilda bir nechta \"ishontiruvchi\" so'z bor (${keywordHits.joinToString(", ")})"
            )
        }

        return findings
    }
}
