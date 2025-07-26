// File: app/src/main/java/com/example/appblocker/FullUsageListActivity.kt
package com.example.appblocker

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.UsageStatsAdapter
import com.example.appblocker.databinding.ActivityFullUsageListBinding
import com.example.appblocker.model.UsageStatItem
import java.util.*
import java.util.concurrent.TimeUnit

class FullUsageListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullUsageListBinding
    private lateinit var usageStatsAdapter: UsageStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullUsageListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        usageStatsAdapter = UsageStatsAdapter(emptyList(), packageManager)
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = usageStatsAdapter

        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Grant Usage Access permission to view stats", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadUsageStats()
        }

        binding.backButton.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        if (hasUsageAccessPermission()) {
            loadUsageStats()
        }
    }

    private fun loadUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            startTime,
            endTime
        )

        // Map usage by package name
        val usageMap = usageStats.associateBy { it.packageName }

        // Get ALL installed apps, not just launchable ones
        val allApps = getAllInstalledApps()

        val usageItems = allApps.map { appInfo ->
            val minutes = usageMap[appInfo.packageName]?.let {
                TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground)
            } ?: 0L

            val label = try {
                appInfo.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                appInfo.packageName
            }
            UsageStatItem(appInfo.packageName, label, minutes)
        }.sortedByDescending { it.minutesUsed }

        usageStatsAdapter.updateData(usageItems)
    }

    private fun getAllInstalledApps(): List<ApplicationInfo> {
        val packageManager = packageManager

        // Get all installed packages
        val allPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

        // Filter apps
        return allPackages
            .filter { appInfo ->
                // Exclude your own app
                appInfo.packageName != this.packageName &&
                        // Include ALL user-installed apps OR apps with launcher intent OR important system apps
                        (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0 ||
                                hasLauncherIntent(appInfo.packageName) ||
                                isImportantSystemApp(appInfo.packageName)) &&
                        // Make sure the app has a proper label
                        try {
                            val label = appInfo.loadLabel(packageManager).toString()
                            label.isNotBlank() && label != appInfo.packageName
                        } catch (e: Exception) {
                            false
                        }
            }
            .distinctBy { it.packageName }
    }

    private fun hasLauncherIntent(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            intent != null
        } catch (e: Exception) {
            false
        }
    }

    private fun isImportantSystemApp(packageName: String): Boolean {
        // More comprehensive matching for popular apps
        return packageName.contains("youtube", ignoreCase = true) ||
                packageName.contains("instagram", ignoreCase = true) ||
                packageName.contains("facebook", ignoreCase = true) ||
                packageName.contains("twitter", ignoreCase = true) ||
                packageName.contains("snapchat", ignoreCase = true) ||
                packageName.contains("tiktok", ignoreCase = true) ||
                packageName.contains("whatsapp", ignoreCase = true) ||
                packageName.contains("spotify", ignoreCase = true) ||
                packageName.contains("netflix", ignoreCase = true) ||
                packageName.contains("chrome", ignoreCase = true) ||
                packageName.contains("gmail", ignoreCase = true) ||
                packageName.contains("maps", ignoreCase = true) ||
                packageName.contains("photos", ignoreCase = true) ||
                packageName.contains("music", ignoreCase = true) ||
                packageName.contains("telegram", ignoreCase = true) ||
                packageName.contains("discord", ignoreCase = true) ||
                packageName.contains("reddit", ignoreCase = true) ||
                packageName.contains("pinterest", ignoreCase = true) ||
                packageName.contains("linkedin", ignoreCase = true) ||
                packageName.contains("amazon", ignoreCase = true) ||
                packageName.contains("games", ignoreCase = true) ||
                packageName.contains("browser", ignoreCase = true) ||
                packageName.contains("messenger", ignoreCase = true) ||
                packageName.contains("play", ignoreCase = true)
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