package uz.faylqalqoni.shield

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import uz.faylqalqoni.shield.core.SettingsStore
import uz.faylqalqoni.shield.databinding.ActivitySettingsBinding

/**
 * Google Safe Browsing API kalitini kiritish/saqlash ekrani.
 * Kalit ilovaning shaxsiy SharedPreferences'ida (boshqa ilovalar o'qiy olmaydigan
 * joyda) saqlanadi.
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val current = SettingsStore.getSafeBrowsingApiKey(applicationContext)
        binding.apiKeyEditText.setText(current)
        updateStatus(current)

        binding.saveApiKeyButton.setOnClickListener {
            val value = binding.apiKeyEditText.text?.toString().orEmpty()
            SettingsStore.setSafeBrowsingApiKey(applicationContext, value)
            updateStatus(value)
        }
    }

    private fun updateStatus(value: String) {
        binding.apiKeyStatusText.text = if (value.isBlank()) {
            getString(R.string.settings_status_missing)
        } else {
            getString(R.string.settings_status_set)
        }
    }
}
