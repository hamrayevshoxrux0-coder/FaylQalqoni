package uz.faylqalqoni.shield.ui

import android.app.Activity
import androidx.appcompat.app.AlertDialog
import uz.faylqalqoni.shield.R
import uz.faylqalqoni.shield.core.LinkCheckResult
import uz.faylqalqoni.shield.core.ThreatLevel

/**
 * Link tekshiruvi natijasini AlertDialog sifatida ko'rsatadigan umumiy yordamchi.
 * Ham LinkCheckActivity (Ulashish orqali), ham MainActivity (qo'lda kiritilgan
 * link) uchun bir xil ko'rinishni ta'minlaydi.
 */
object LinkResultDialogPresenter {

    fun show(
        activity: Activity,
        result: LinkCheckResult,
        onOpenAnyway: (String) -> Unit,
        onClosed: () -> Unit = {}
    ) {
        val (titleRes, iconEmoji) = when (result.level) {
            ThreatLevel.DANGEROUS -> R.string.link_result_dangerous to "⛔"
            ThreatLevel.SUSPICIOUS -> R.string.link_result_suspicious to "⚠️"
            ThreatLevel.SAFE -> R.string.link_result_safe to "✅"
        }

        val message = buildString {
            append(result.url)
            append("\n\n")
            append(result.reasons.joinToString("\n\n") { "• $it" })
        }

        val dialog = AlertDialog.Builder(activity)
            .setTitle("$iconEmoji ${activity.getString(titleRes)}")
            .setMessage(message)
            .setNegativeButton(R.string.action_close) { d, _ ->
                d.dismiss()
                onClosed()
            }
            .setPositiveButton(R.string.action_open_anyway) { d, _ ->
                d.dismiss()
                onOpenAnyway(result.url)
                onClosed()
            }
            .setOnCancelListener { onClosed() }
            .show()

        if (result.level == ThreatLevel.DANGEROUS) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(activity.getColor(R.color.level_dangerous))
        }
    }
}
