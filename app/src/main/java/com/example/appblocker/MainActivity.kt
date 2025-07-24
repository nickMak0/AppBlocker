// File: app/src/main/java/com/example/appblocker/MainActivity.kt
package com.example.appblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var permissionStatusText: TextView
    private lateinit var toggleSwitch: MaterialSwitch
    private lateinit var manageAppsButton: Button

    private val prefs by lazy {
        getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.Theme_AppBlocker)
        setContentView(R.layout.activity_main)

        // 🔗 Bind UI elements
        permissionStatusText = findViewById(R.id.permissionStatusText)
        toggleSwitch = findViewById(R.id.blockToggleSwitch)
        manageAppsButton = findViewById(R.id.manageAppsButton)

        // ✅ Handle usage access permission
        if (!hasUsageAccessPermission()) {
            permissionStatusText.text = "Usage Access: ❌ Not Granted"
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            permissionStatusText.text = "Usage Access: ✅ Granted"
        }

        // 🔄 Restore switch state from shared preferences
        val isBlockingEnabled = prefs.getBoolean("blocking_enabled", true)
        toggleSwitch.isChecked = isBlockingEnabled

        if (isBlockingEnabled) {
            startService(Intent(this, AppMonitorService::class.java))
        }

        // 🔁 Toggle app blocking on/off
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

        // 🔗 Navigate to Manage Blocked Apps screen
        manageAppsButton.setOnClickListener {
            startActivity(Intent(this, ManageAppsActivity::class.java))
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
