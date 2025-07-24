package com.example.appblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.appblocker.utils.PinUtils
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var permissionStatusText: TextView
    private lateinit var toggleSwitch: MaterialSwitch
    private lateinit var manageAppsButton: Button
    private lateinit var dashboardButton: Button
    private lateinit var scheduleButton: Button

    private val prefs by lazy {
        getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
    }

    private var wasPinVerified = false

    private val pinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            wasPinVerified = true
            initMainUI()
        } else {
            finish() // Close app if PIN not verified
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 🔐 Show PIN screen first if setup
        if (PinUtils.isPinSetup(this)) {
            pinLauncher.launch(Intent(this, PinLockActivity::class.java))
        } else {
            wasPinVerified = true
            initMainUI()
        }
    }

    private fun initMainUI() {
        setTheme(R.style.Theme_AppBlocker)
        setContentView(R.layout.activity_main)

        permissionStatusText = findViewById(R.id.permissionStatusText)
        toggleSwitch = findViewById(R.id.blockToggleSwitch)
        manageAppsButton = findViewById(R.id.manageAppsButton)
        dashboardButton = findViewById(R.id.dashboardButton)
        scheduleButton = findViewById(R.id.scheduleButton)

        // 🔒 Check usage access permission
        if (!hasUsageAccessPermission()) {
            permissionStatusText.text = "Usage Access: ❌ Not Granted"
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            permissionStatusText.text = "Usage Access: ✅ Granted"
        }

        // 🛑 Load toggle state and start service
        val isBlockingEnabled = prefs.getBoolean("blocking_enabled", true)
        toggleSwitch.isChecked = isBlockingEnabled
        if (isBlockingEnabled) {
            startService(Intent(this, AppMonitorService::class.java))
        }

        toggleSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("blocking_enabled", isChecked).apply()
            if (isChecked) {
                startService(Intent(this, AppMonitorService::class.java))
                Toast.makeText(this, "Blocking Enabled", Toast.LENGTH_SHORT).show()
            } else {
                stopService(Intent(this, AppMonitorService::class.java))
                Toast.makeText(this, "Blocking Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        manageAppsButton.setOnClickListener {
            startActivity(Intent(this, ManageAppsActivity::class.java))
        }

        dashboardButton.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
        }

        scheduleButton.setOnClickListener {
            startActivity(Intent(this, ScheduleSettingsActivity::class.java))
        }
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}
