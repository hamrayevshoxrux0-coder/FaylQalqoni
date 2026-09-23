package uz.faylqalqoni.shield.core

import java.io.File
import java.io.RandomAccessFile

/**
 * Fayl kengaytmasiga emas, balki uning ichidagi haqiqiy baytlariga ("magic bytes")
 * qarab qanday fayl ekanini aniqlaydi. Firibgarlar fayl nomini "rasm.jpeg" deb
 * qo'yishlari mumkin, lekin faylning o'zi butunlay boshqa formatda (masalan APK)
 * bo'lishi mumkin — buni faqat baytlarni o'qib bilib olish mumkin.
 */
object FileTypeSniffer {

    // Har bir yozuv: (imzo baytlari, ushbu turning nomi)
    private val SIGNATURES: List<Pair<ByteArray, String>> = listOf(
        byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) to "JPEG",
        byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) to "PNG",
        byteArrayOf(0x47, 0x49, 0x46, 0x38) to "GIF",
        byteArrayOf(0x25, 0x50, 0x44, 0x46) to "PDF",
        byteArrayOf(0x50, 0x4B, 0x03, 0x04) to "ZIP/APK",
        byteArrayOf(0x50, 0x4B, 0x05, 0x06) to "ZIP/APK", // bo'sh arxiv
        byteArrayOf(0x52, 0x61, 0x72, 0x21) to "RAR",
        byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte()) to "7Z",
        byteArrayOf(0x4D, 0x5A) to "EXE/DLL",
        byteArrayOf(0x64, 0x65, 0x78, 0x0A) to "DEX"
    )

    /** MP4/MOV kabi ISO BMFF formatlar 4-8 baytdan boshlab "ftyp" so'zini o'z ichiga oladi. */
    private fun looksLikeIsoMedia(header: ByteArray): Boolean {
        if (header.size < 12) return false
        val tag = String(header, 4, 4, Charsets.US_ASCII)
        return tag == "ftyp"
    }

    data class Sniff(val realType: String, val header: ByteArray)

    fun sniff(file: File): Sniff? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            RandomAccessFile(file, "r").use { raf ->
                val header = ByteArray(16)
                val read = raf.read(header)
                if (read <= 0) return null
                for ((sig, name) in SIGNATURES) {
                    if (read >= sig.size && header.copyOf(sig.size).contentEquals(sig)) {
                        return Sniff(name, header)
                    }
                }
                if (looksLikeIsoMedia(header)) return Sniff("MP4/MOV", header)
                Sniff("UNKNOWN", header)
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Fayl nomidan kengaytmani ajratib oladi (kichik harflarda, nuqtasiz). */
    fun declaredExtension(fileName: String): String? {
        val idx = fileName.lastIndexOf('.')
        if (idx < 0 || idx == fileName.length - 1) return null
        return fileName.substring(idx + 1).lowercase()
    }

    private val IMAGE_LIKE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "bmp", "webp")
    private val DOCUMENT_LIKE_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt")
    private val MEDIA_LIKE_EXTENSIONS = setOf("mp4", "mov", "mp3", "avi", "mkv", "3gp")

    /**
     * Fayl nomi "rasm/hujjat/video" ko'rinishida bo'lib, lekin haqiqiy tarkibi
     * ijro etiladigan/o'rnatiladigan fayl (APK, EXE, DEX) bo'lsa — bu eng klassik
     * "ikki karra kengaytma" firibgarligi (masalan photo.jpg.apk).
     */
    fun isExecutableDisguisedAsMedia(declaredExt: String?, realType: String): Boolean {
        val executableTypes = setOf("ZIP/APK", "EXE/DLL", "DEX")
        if (realType !in executableTypes) return false
        if (declaredExt == null) return false
        return declaredExt in IMAGE_LIKE_EXTENSIONS ||
            declaredExt in DOCUMENT_LIKE_EXTENSIONS ||
            declaredExt in MEDIA_LIKE_EXTENSIONS
    }

    /** Fayl nomida "*.jpeg.apk" kabi ikki karra kengaytma bor-yo'qligini tekshiradi. */
    private val DOUBLE_EXTENSION_REGEX = Regex(
        "\\.(jpe?g|png|gif|bmp|webp|pdf|docx?|xlsx?|pptx?|txt|mp4|mov|mp3|avi|mkv|3gp|zip|rar)\\.(apk)$",
        RegexOption.IGNORE_CASE
    )

    fun hasDoubleExtensionPattern(fileName: String): Boolean =
        DOUBLE_EXTENSION_REGEX.containsMatchIn(fileName)
}
