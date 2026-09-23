package uz.faylqalqoni.shield

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uz.faylqalqoni.shield.core.ScanEngine
import uz.faylqalqoni.shield.core.ScanResult
import uz.faylqalqoni.shield.core.UrlCheckEngine
import uz.faylqalqoni.shield.databinding.ActivityMainBinding
import uz.faylqalqoni.shield.quarantine.QuarantineManager
import uz.faylqalqoni.shield.service.BootReceiver
import uz.faylqalqoni.shield.service.FileMonitorService
import uz.faylqalqoni.shield.ui.LinkResultDialogPresenter
import uz.faylqalqoni.shield.ui.ThreatAdapter
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var scanEngine: ScanEngine
    private lateinit var quarantineManager: QuarantineManager
    private lateinit var urlCheckEngine: UrlCheckEngine
    private val adapter = ThreatAdapter()

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* natija e'tiborga olinmaydi, onResume tekshiradi */ }

    private val storagePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissionBanner() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        scanEngine = ScanEngine(applicationContext)
        quarantineManager = QuarantineManager(applicationContext)
        urlCheckEngine = UrlCheckEngine(applicationContext)

        binding.resultsRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        binding.resultsRecyclerView.adapter = adapter

        binding.grantPermissionButton.setOnClickListener { requestStoragePermission() }
        binding.scanButton.setOnClickListener { runManualScan() }
        binding.quarantineButton.setOnClickListener {
            startActivity(Intent(this, QuarantineActivity::class.java))
        }
        binding.settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.checkLinkButton.setOnClickListener { checkLinkFromInput() }
        binding.linkInputEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                checkLinkFromInput()
                true
            } else {
                false
            }
        }

        binding.monitoringSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            if (isChecked && !hasAllRequiredPermissions()) {
                // Ruxsatlar yo'q bo'lsa, avval so'raymiz va foydalanuvchi ruxsat
                // bergandan keyin o'zi qaytadan yoqishi kerak bo'ladi.
                switchView.isChecked = false
                requestAllPermissions()
                return@setOnCheckedChangeListener
            }
            setMonitoringEnabled(isChecked)
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionBanner()
        refreshMonitoringUi()
        refreshQuarantineCount()
    }

    // ---------- Ruxsatlar ----------

    private fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }

    private fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun hasAllRequiredPermissions(): Boolean = hasAllFilesAccess() && hasNotificationPermission()

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun requestAllPermissions() {
        requestNotificationPermissionIfNeeded()
        if (!hasAllFilesAccess()) requestStoragePermission()
    }

    private fun refreshPermissionBanner() {
        binding.permissionBanner.visibility = if (hasAllFilesAccess()) View.GONE else View.VISIBLE
    }

    // ---------- Monitoring ----------

    private fun prefs() = getSharedPreferences(BootReceiver.PREFS_NAME, Context.MODE_PRIVATE)

    private fun setMonitoringEnabled(enabled: Boolean) {
        prefs().edit().putBoolean(BootReceiver.KEY_MONITORING_ENABLED, enabled).apply()
        if (enabled) {
            FileMonitorService.start(this)
        } else {
            FileMonitorService.stop(this)
        }
        refreshMonitoringUi()
    }

    private fun refreshMonitoringUi() {
        val enabled = prefs().getBoolean(BootReceiver.KEY_MONITORING_ENABLED, false)
        binding.monitoringSwitch.setOnCheckedChangeListener(null)
        binding.monitoringSwitch.isChecked = enabled
        binding.monitoringStatusText.setText(
            if (enabled) R.string.status_monitoring_on else R.string.status_monitoring_off
        )
        binding.monitoringSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            if (isChecked && !hasAllRequiredPermissions()) {
                switchView.isChecked = false
                requestAllPermissions()
                return@setOnCheckedChangeListener
            }
            setMonitoringEnabled(isChecked)
        }
    }

    private fun refreshQuarantineCount() {
        val count = quarantineManager.listQuarantined().size
        binding.quarantineButton.text = getString(R.string.btn_open_quarantine, count)
    }

    // ---------- Qo'lda skanerlash ----------

    private fun runManualScan() {
        if (!hasAllFilesAccess()) {
            requestStoragePermission()
            return
        }

        binding.scanProgressBar.visibility = View.VISIBLE
        binding.scanButton.isEnabled = false
        binding.resultsHeaderText.text = getString(R.string.scan_progress)

        val found = mutableListOf<ScanResult>()
        adapter.submitList(emptyList())

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                for (dir in scanTargets()) {
                    if (!dir.exists()) continue
                    scanEngine.scanDirectory(dir, onlyThreats = true) { result ->
                        found.add(result)
                        runOnUiThread { adapter.addOrUpdate(result) }
                    }
                }
            }

            binding.scanProgressBar.visibility = View.GONE
            binding.scanButton.isEnabled = true
            binding.resultsHeaderText.text = if (found.isEmpty()) {
                getString(R.string.scan_complete_safe)
            } else {
                getString(R.string.scan_complete_threats, found.size)
            }
        }
    }

    // ---------- Link tekshirish ----------

    private fun checkLinkFromInput() {
        val raw = binding.linkInputEditText.text?.toString()?.trim().orEmpty()
        if (raw.isEmpty()) {
            Toast.makeText(this, R.string.link_input_empty, Toast.LENGTH_SHORT).show()
            return
        }

        binding.checkLinkButton.isEnabled = false
        lifecycleScope.launch {
            val result = urlCheckEngine.check(raw)
            binding.checkLinkButton.isEnabled = true
            LinkResultDialogPresenter.show(
                activity = this@MainActivity,
                result = result,
                onOpenAnyway = { url ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, R.string.link_open_failed, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }

    private fun scanTargets(): List<File> {
        val base = Environment.getExternalStorageDirectory()
        return listOf(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(base, "WhatsApp"),
            File(base, "Android/media/com.whatsapp/WhatsApp"),
            File(base, "Telegram"),
            File(base, "Android/media/org.telegram.messenger/Telegram")
        )
    }
}
