package uz.faylqalqoni.shield.core

import android.content.Context
import android.util.Log
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/**
 * Ma'lum zararli fayllarning SHA-256 xeshlari bazasi ("signature" asosidagi aniqlash).
 *
 * Baza ikki manbadan yig'iladi:
 *  1) assets/malware_signatures.json — ilova bilan birga keladigan boshlang'ich ro'yxat
 *  2) fayl tizimidagi "custom_signatures.json" — foydalanuvchi/administrator qo'shgan
 *     qo'shimcha xeshlar (masalan, qo'lda tekshirilib, zararli deb topilgan fayllar)
 *
 * Eslatma: bu oddiy, lokal signature-baza — professional antivirus kompaniyalarining
 * milliondan ortiq yozuvli bulutli bazalarini almashtirmaydi. Lekin u aynan
 * "bitta ma'lum zararli faylni qayta-qayta tarqatish" holatlariga (masalan shu
 * "to'ydan arxiv" firibgarligining aniq nusxalari) qarshi samarali ishlaydi.
 */
class SignatureDatabase private constructor(
    private val knownBad: MutableMap<String, String>
) {
    fun lookup(sha256: String): String? = knownBad[sha256.lowercase()]

    fun addCustomSignature(context: Context, sha256: String, name: String) {
        val key = sha256.lowercase()
        knownBad[key] = name
        persistCustom(context)
    }

    private fun persistCustom(context: Context) {
        try {
            val file = File(context.filesDir, CUSTOM_FILE_NAME)
            val arr = JSONArray()
            for ((hash, name) in knownBad) {
                val obj = org.json.JSONObject()
                obj.put("sha256", hash)
                obj.put("name", name)
                arr.put(obj)
            }
            file.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Signature bazasini saqlab bo'lmadi", e)
        }
    }

    companion object {
        private const val TAG = "SignatureDatabase"
        private const val ASSET_FILE_NAME = "malware_signatures.json"
        private const val CUSTOM_FILE_NAME = "custom_signatures.json"

        @Volatile private var instance: SignatureDatabase? = null

        fun get(context: Context): SignatureDatabase {
            instance?.let { return it }
            synchronized(this) {
                instance?.let { return it }
                val map = mutableMapOf<String, String>()
                loadFromAssets(context, map)
                loadFromCustomFile(context, map)
                val db = SignatureDatabase(map)
                instance = db
                return db
            }
        }

        private fun loadFromAssets(context: Context, into: MutableMap<String, String>) {
            try {
                context.assets.open(ASSET_FILE_NAME).use { input ->
                    val text = input.bufferedReader().readText()
                    val arr = JSONArray(text)
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val hash = obj.optString("sha256").lowercase()
                        val name = obj.optString("name", "Noma'lum zararli fayl")
                        if (hash.length == 64) into[hash] = name
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "assets/$ASSET_FILE_NAME o'qishda xato", e)
            }
        }

        private fun loadFromCustomFile(context: Context, into: MutableMap<String, String>) {
            try {
                val file = File(context.filesDir, CUSTOM_FILE_NAME)
                if (!file.exists()) return
                val arr = JSONArray(file.readText())
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val hash = obj.optString("sha256").lowercase()
                    val name = obj.optString("name", "Foydalanuvchi qo'shgan yozuv")
                    if (hash.length == 64) into[hash] = name
                }
            } catch (e: Exception) {
                Log.e(TAG, "$CUSTOM_FILE_NAME o'qishda xato", e)
            }
        }

        /** Faylning SHA-256 xesh summasini hisoblaydi. Katta fayllarda ham xotira tejaydi. */
        fun sha256Of(file: File): String? {
            return try {
                val digest = MessageDigest.getInstance("SHA-256")
                FileInputStream(file).use { fis ->
                    val buffer = ByteArray(1 shl 16) // 64 KB
                    while (true) {
                        val read = fis.read(buffer)
                        if (read <= 0) break
                        digest.update(buffer, 0, read)
                    }
                }
                digest.digest().joinToString("") { "%02x".format(it) }
            } catch (e: Exception) {
                Log.e(TAG, "SHA-256 hisoblashda xato: ${file.path}", e)
                null
            }
        }
    }
}
