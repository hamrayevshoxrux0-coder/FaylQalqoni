package uz.faylqalqoni.shield.core

/**
 * APK so'raydigan ruxsatlar (permissions) ro'yxatiga qarab xavf darajasini baholaydi.
 *
 * Bitta ruxsatning o'zi hali xavfli degani emas (masalan ko'plab qonuniy ilovalar
 * kontaktlarni o'qiydi). Lekin ma'lum KOMBINATSIYALAR — ayniqsa kichik, noma'lum
 * ishlab chiqaruvchidan kelgan, "rasm/arxiv" niqobidagi ilovada — juda xarakterli
 * josuslik/bank-firibgarlik belgisi hisoblanadi.
 */
object PermissionRiskAnalyzer {

    // SMS orqali bank bir martalik kodlarini (OTP) o'g'irlash uchun ishlatiladigan ruxsatlar
    private val SMS_INTERCEPT = setOf(
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_SMS",
        "android.permission.SEND_SMS"
    )

    // Ekranni "qoplab" soxta login oynalarini ko'rsatish yoki foydalanuvchi
    // nomidan avtomatik bosishlar qilish uchun ishlatiladi
    private val OVERLAY_AND_CONTROL = setOf(
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.BIND_ACCESSIBILITY_SERVICE"
    )

    private val CONTACTS = setOf(
        "android.permission.READ_CONTACTS",
        "android.permission.WRITE_CONTACTS"
    )

    private val CALL_LOG = setOf(
        "android.permission.READ_CALL_LOG",
        "android.permission.PROCESS_OUTGOING_CALLS"
    )

    private val DEVICE_ADMIN = setOf(
        "android.permission.BIND_DEVICE_ADMIN"
    )

    private val INSTALL_MORE_APPS = setOf(
        "android.permission.REQUEST_INSTALL_PACKAGES"
    )

    private val NOTIFICATION_ACCESS = setOf(
        "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    )

    data class RiskFinding(val level: ThreatLevel, val reason: String)

    fun analyze(requestedPermissions: List<String>): List<RiskFinding> {
        val perms = requestedPermissions.toSet()
        val findings = mutableListOf<RiskFinding>()

        val smsHits = SMS_INTERCEPT.intersect(perms)
        if (smsHits.size >= 2) {
            findings += RiskFinding(
                ThreatLevel.DANGEROUS,
                "SMS xabarlarni o'qish/yuborish huquqlarini birga so'raydi — " +
                    "bank tasdiqlash kodlarini (OTP) o'g'irlash uchun tipik usul"
            )
        } else if (smsHits.isNotEmpty()) {
            findings += RiskFinding(
                ThreatLevel.SUSPICIOUS,
                "SMS xabarlarga kirish huquqini so'raydi"
            )
        }

        if (OVERLAY_AND_CONTROL.all { it in perms }) {
            findings += RiskFinding(
                ThreatLevel.DANGEROUS,
                "Ekran ustiga oyna chiqarish VA maxsus imkoniyatlar (Accessibility) " +
                    "xizmatini birga so'raydi — soxta login oynasi ko'rsatib parol/karta " +
                    "ma'lumotlarini o'g'irlash yoki ekranni avtomatik boshqarish uchun ishlatiladi"
            )
        } else if (perms.any { it in OVERLAY_AND_CONTROL }) {
            findings += RiskFinding(
                ThreatLevel.SUSPICIOUS,
                "Ekran ustidan oyna chiqarish yoki maxsus imkoniyatlar xizmatini so'raydi"
            )
        }

        if (perms.any { it in DEVICE_ADMIN }) {
            findings += RiskFinding(
                ThreatLevel.DANGEROUS,
                "Qurilma administratori huquqini so'raydi — o'chirilishga qarshilik " +
                    "ko'rsatishi yoki qurilmani qulflab qo'yishi mumkin"
            )
        }

        if (perms.any { it in NOTIFICATION_ACCESS }) {
            findings += RiskFinding(
                ThreatLevel.DANGEROUS,
                "Barcha bildirishnomalarni o'qish huquqini so'raydi — bank va " +
                    "messenjer bildirishnomalaridagi kodlarni ushlab qolishi mumkin"
            )
        }

        if (perms.any { it in INSTALL_MORE_APPS }) {
            findings += RiskFinding(
                ThreatLevel.SUSPICIOUS,
                "Boshqa ilovalarni o'rnatish huquqini so'raydi"
            )
        }

        val contactHits = CONTACTS.intersect(perms)
        if (contactHits.isNotEmpty() && smsHits.isNotEmpty()) {
            findings += RiskFinding(
                ThreatLevel.SUSPICIOUS,
                "Kontaktlar ro'yxatiga va SMS'ga birga kirish huquqini so'raydi — " +
                    "o'zini kontaktlaringizga avtomatik yuborishi mumkin"
            )
        }

        if (perms.any { it in CALL_LOG }) {
            findings += RiskFinding(
                ThreatLevel.SUSPICIOUS,
                "Qo'ng'iroqlar tarixini o'qish/chaqiruvlarni kuzatish huquqini so'raydi"
            )
        }

        return findings
    }
}
