package uz.faylqalqoni.shield

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import uz.faylqalqoni.shield.core.UrlCheckEngine
import uz.faylqalqoni.shield.ui.LinkResultDialogPresenter

/**
 * Foydalanuvchi Telegram/WhatsApp/brauzerda biror linkni "Ulashish" (Share)
 * orqali ushbu ilovaga yuborganda ishga tushadi. Ekranga hech narsa
 * chizmaydi — faqat natija dialogini ko'rsatadi va yopilgach tugaydi.
 */
class LinkCheckActivity : AppCompatActivity() {

    private lateinit var engine: UrlCheckEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        engine = UrlCheckEngine(applicationContext)

        val sharedText = when (intent?.action) {
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)
            Intent.ACTION_VIEW -> intent.dataString
            else -> null
        }

        val url = extractFirstUrl(sharedText)
        if (url == null) {
            Toast.makeText(this, R.string.link_not_found, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        checkAndShow(url)
    }

    private fun extractFirstUrl(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val matcher = Patterns.WEB_URL.matcher(text)
        return if (matcher.find()) text.substring(matcher.start(), matcher.end()) else null
    }

    private fun checkAndShow(url: String) {
        val progressDialog = AlertDialog.Builder(this)
            .setTitle(R.string.link_checking_title)
            .setMessage(url)
            .setCancelable(false)
            .show()

        lifecycleScope.launch {
            val result = engine.check(url)
            progressDialog.dismiss()
            LinkResultDialogPresenter.show(
                activity = this@LinkCheckActivity,
                result = result,
                onOpenAnyway = { openUrl(it) },
                onClosed = { finish() }
            )
        }
    }

    private fun openUrl(url: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: Exception) {
            Toast.makeText(this, R.string.link_open_failed, Toast.LENGTH_SHORT).show()
        }
    }
}
