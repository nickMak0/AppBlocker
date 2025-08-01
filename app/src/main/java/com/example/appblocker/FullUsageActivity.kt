package com.example.appblocker

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.UsageStatsAdapter
import com.example.appblocker.databinding.ActivityFullUsageBinding
import com.example.appblocker.model.UsageStatItem
import java.util.*
import java.util.concurrent.TimeUnit

class FullUsageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFullUsageBinding
    private lateinit var usageStatsAdapter: UsageStatsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFullUsageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupButtons()
        
        if (!hasUsageAccessPermission()) {
            Toast.makeText(this, "Please grant Usage Access permission", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } else {
            loadAllUsageStats()
        }
    }

    private fun setupRecyclerView() {
        usageStatsAdapter = UsageStatsAdapter(emptyList(), packageManager)
        binding.usageStatsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.usageStatsRecyclerView.adapter = usageStatsAdapter
    }

    private fun setupButtons() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadAllUsageStats() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        val startTime = calendar.timeInMillis

        val usageStats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val usageMap = usageStats.associateBy { it.packageName }
        val launchableApps = getLaunchableApps()

        val usageItems = launchableApps.map { app ->
            val minutesUsed = usageMap[app.packageName]?.let {
                TimeUnit.MILLISECONDS.toMinutes(it.totalTimeInForeground)
            } ?: 0L

            val appName = try {
                app.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                app.packageName
            }

            UsageStatItem(app.packageName, appName, minutesUsed)
        }.sortedByDescending { it.minutesUsed }

        val filteredItems = usageItems.filter { it.minutesUsed > 0 }
        usageStatsAdapter.updateData(filteredItems)
        binding.totalAppsCount.text = "${filteredItems.size} apps"
    }

    private fun getLaunchableApps(): List<ApplicationInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val launchables = packageManager.queryIntentActivities(intent, 0)
        return launchables.map { it.activityInfo.applicationInfo }
            .distinctBy { it.packageName }
            .filter { it.packageName != this.packageName }
            .sortedBy {
                try {
                    it.loadLabel(packageManager).toString().lowercase()
                } catch (e: Exception) {
                    it.packageName
                }
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