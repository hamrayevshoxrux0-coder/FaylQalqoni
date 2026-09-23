package uz.faylqalqoni.shield.quarantine

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * Shubhali/xavfli deb topilgan fayllarni darhol o'chirib yubormasdan,
 * ilovaning o'z xavfsiz papkasiga ko'chiradi ("karantin"). Bu yondashuv
 * xato musbat (false positive) holatlarida foydalanuvchiga faylni
 * qaytarish imkonini beradi.
 *
 * Karantin papkasi ilovaning ichki xotirasida (context.filesDir) joylashadi —
 * bu yerga boshqa ilovalar (jumladan o'sha APK, agar u qandaydir yo'l bilan
 * ishga tushishga harakat qilsa ham) kira olmaydi.
 */
class QuarantineManager(private val context: Context) {

    private val quarantineDir: File by lazy {
        File(context.filesDir, "quarantine").apply { mkdirs() }
    }

    private val manifestFile: File by lazy { File(quarantineDir, "manifest.json") }

    data class QuarantinedItem(
        val id: String,
        val originalPath: String,
        val originalName: String,
        val quarantinedAt: Long
    )

    /** Faylni asl joyidan olib, karantin papkasiga ko'chiradi. Muvaffaqiyat holatida true. */
    fun quarantine(file: File): Boolean {
        return try {
            val id = UUID.randomUUID().toString()
            val target = File(quarantineDir, id)
            val copied = file.copyTo(target, overwrite = true)
            val deleted = file.delete()
            if (!deleted) {
                // Asl faylni o'chirib bo'lmadi (masalan ruxsat yo'q) — baribir nusxa
                // karantinda saqlanadi va foydalanuvchiga ogohlantirish ko'rsatiladi.
                Log.w(TAG, "Asl faylni o'chirib bo'lmadi: ${file.path}")
            }
            appendToManifest(
                QuarantinedItem(
                    id = id,
                    originalPath = file.absolutePath,
                    originalName = file.name,
                    quarantinedAt = System.currentTimeMillis()
                )
            )
            copied.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Karantinga ko'chirishda xato: ${file.path}", e)
            false
        }
    }

    fun listQuarantined(): List<QuarantinedItem> {
        if (!manifestFile.exists()) return emptyList()
        return try {
            val arr = JSONArray(manifestFile.readText())
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                QuarantinedItem(
                    id = obj.getString("id"),
                    originalPath = obj.getString("originalPath"),
                    originalName = obj.getString("originalName"),
                    quarantinedAt = obj.getLong("quarantinedAt")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Manifestni o'qishda xato", e)
            emptyList()
        }
    }

    fun deletePermanently(id: String): Boolean {
        val target = File(quarantineDir, id)
        val ok = !target.exists() || target.delete()
        if (ok) removeFromManifest(id)
        return ok
    }

    private fun appendToManifest(item: QuarantinedItem) {
        val current = listQuarantined().toMutableList()
        current.add(item)
        writeManifest(current)
    }

    private fun removeFromManifest(id: String) {
        val current = listQuarantined().filterNot { it.id == id }
        writeManifest(current)
    }

    private fun writeManifest(items: List<QuarantinedItem>) {
        try {
            val arr = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("originalPath", item.originalPath)
                obj.put("originalName", item.originalName)
                obj.put("quarantinedAt", item.quarantinedAt)
                arr.put(obj)
            }
            manifestFile.writeText(arr.toString())
        } catch (e: Exception) {
            Log.e(TAG, "Manifestni yozishda xato", e)
        }
    }

    companion object {
        private const val TAG = "QuarantineManager"
    }
}
