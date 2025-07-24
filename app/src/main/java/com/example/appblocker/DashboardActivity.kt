package com.example.appblocker

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.UsageStatsAdapter // Fixed import
import com.example.appblocker.databinding.ActivityDashboardBinding
import com.example.appblocker.model.UsageStatItem
import java.util.*
import java.util.concurrent.TimeUnit

class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private lateinit var usageStatsAdapter: UsageStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize adapter with empty list and PackageManager
        usageStatsAdapter = UsageStatsAdapter(emptyList(), packageManager)
        binding.usageStatsRecyclerView.layoutManager = LinearLayoutManager(this) // Fixed ID
        binding.usageStatsRecyclerView.adapter = usageStatsAdapter // Fixed ID

        // Set up button click listeners
        binding.manageAppsButton.setOnClickListener {
            loadUsageStats() // Refresh data when manage apps is clicked
        }

        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadUsageStats()
        }
    }

    private fun loadUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1) // Past 24 hours
        val startTime = calendar.timeInMillis

        val stats: List<UsageStats> = usageStatsManager
            .queryUsageStats(UsageStatsManager.INTERVAL_DAILY, startTime, endTime)
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }

        val usageItems = stats.map { stat ->
            // Get app name from PackageManager
            val appName = try {
                val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                packageManager.getApplicationLabel(appInfo).toString()
            } catch (e: PackageManager.NameNotFoundException) {
                stat.packageName // Fallback to package name
            }

            // Convert milliseconds to minutes
            val minutesUsed = TimeUnit.MILLISECONDS.toMinutes(stat.totalTimeInForeground)

            UsageStatItem(
                packageName = stat.packageName,
                appName = appName,
                minutesUsed = minutesUsed
            )
        }

        // Update total screen time
        val totalMinutes = usageItems.sumOf { it.minutesUsed }
        binding.totalScreenTimeText.text = "Total Screen Time: $totalMinutes min"

        // Create new adapter with updated data
        usageStatsAdapter = UsageStatsAdapter(usageItems, packageManager)
        binding.usageStatsRecyclerView.adapter = usageStatsAdapter
    }

    private fun hasUsageAccessPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}