package uz.faylqalqoni.shield.core

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Google Safe Browsing API v4 ("threatMatches:find") bilan ishlaydigan mijoz.
 *
 * Bu xizmat Google tomonidan doimiy yangilanadigan, millionlab ma'lum
 * firibgarlik/zararli dastur saytlari bazasi ustidan tekshiradi — bu lokal
 * qoidalar (UrlRiskAnalyzer) bera olmaydigan, haqiqiy "signature" darajasidagi
 * himoyani beradi.
 *
 * API kaliti kerak (bepul): https://console.cloud.google.com/ -> "Safe Browsing API"
 * yoqiladi -> "Credentials" bo'limidan API kalit olinadi. Sozlamalar ekranida kiritiladi.
 */
object SafeBrowsingClient {

    private const val TAG = "SafeBrowsingClient"
    private const val ENDPOINT = "https://safebrowsing.googleapis.com/v4/threatMatches:find"
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    sealed class Outcome {
        object Clean : Outcome()
        data class Threat(val threatTypes: List<String>) : Outcome()
        data class Error(val message: String) : Outcome()
    }

    suspend fun check(apiKey: String, url: String): Outcome = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext Outcome.Error("API kalit kiritilmagan")

        try {
            val body = buildRequestBody(url)
            val request = Request.Builder()
                .url("$ENDPOINT?key=$apiKey")
                .post(body.toString().toRequestBody(JSON))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Outcome.Error("Server javobi: ${response.code}")
                }
                val text = response.body?.string().orEmpty()
                if (text.isBlank()) return@withContext Outcome.Clean

                val json = JSONObject(text)
                val matches = json.optJSONArray("matches")
                if (matches == null || matches.length() == 0) {
                    Outcome.Clean
                } else {
                    val types = mutableListOf<String>()
                    for (i in 0 until matches.length()) {
                        types += matches.getJSONObject(i).optString("threatType", "NOMA'LUM")
                    }
                    Outcome.Threat(types.distinct())
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "Safe Browsing so'rovida tarmoq xatosi", e)
            Outcome.Error("Internet aloqasi yo'q yoki server javob bermadi")
        } catch (e: Exception) {
            Log.e(TAG, "Safe Browsing so'rovida kutilmagan xato", e)
            Outcome.Error("Tekshiruvda kutilmagan xato: ${e.message}")
        }
    }

    private fun buildRequestBody(url: String): JSONObject {
        val client = JSONObject().apply {
            put("clientId", "uz.faylqalqoni.shield")
            put("clientVersion", "1.0.0")
        }
        val threatEntry = JSONObject().apply { put("url", url) }
        val threatInfo = JSONObject().apply {
            put("threatTypes", JSONArray(listOf(
                "MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"
            )))
            put("platformTypes", JSONArray(listOf("ANY_PLATFORM")))
            put("threatEntryTypes", JSONArray(listOf("URL")))
            put("threatEntries", JSONArray().put(threatEntry))
        }
        return JSONObject().apply {
            put("client", client)
            put("threatInfo", threatInfo)
        }
    }

    /** Xavf turlarini foydalanuvchiga tushunarli o'zbekcha tilga o'giradi. */
    fun describeThreatType(type: String): String = when (type) {
        "MALWARE" -> "zararli dastur tarqatadi"
        "SOCIAL_ENGINEERING" -> "firibgarlik/fishing sayti sifatida qayd etilgan"
        "UNWANTED_SOFTWARE" -> "istalmagan dastur o'rnatadi"
        "POTENTIALLY_HARMFUL_APPLICATION" -> "potensial xavfli ilova tarqatadi"
        else -> "xavfli sayt sifatida qayd etilgan"
    }
}
