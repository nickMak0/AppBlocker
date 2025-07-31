// File: app/src/main/java/com/example/appblocker/ManageAppsActivity.kt
package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.AppListAdapter
import com.example.appblocker.databinding.ActivityManageAppsBinding
import com.example.appblocker.dialogs.AppScheduleDialog
import com.example.appblocker.utils.AppScheduleStorage
import com.example.appblocker.utils.PinUtils

class ManageAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageAppsBinding
    private lateinit var adapter: AppListAdapter
    private lateinit var allApps: List<ApplicationInfo>
    private lateinit var blockedApps: MutableSet<String>

    private val sharedPrefs by lazy {
        getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load blocked apps from SharedPreferences
        blockedApps = sharedPrefs.getStringSet("blockedApps", emptySet())!!.toMutableSet()

        // Get installed user apps
        allApps = getUserInstalledApps()

        // Set up adapter
        adapter = AppListAdapter(this, allApps, blockedApps, 
            onToggleChanged = { pkg, isBlocked ->
                if (isBlocked) {
                    blockedApps.add(pkg)
                } else {
                    blockedApps.remove(pkg)
                    // Remove schedule when app is unblocked
                    AppScheduleStorage.removeAppSchedule(this, pkg)
                }
                sharedPrefs.edit().putStringSet("blockedApps", blockedApps).apply()
                updateStats(allApps.size, blockedApps.size)
            },
            onScheduleClicked = { packageName, appName ->
                showScheduleDialog(packageName, appName)
            }
        )

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appsRecyclerView.adapter = adapter
        
        // Back button functionality
        binding.backButton.setOnClickListener { finish() }
        
        // Search button functionality (placeholder for now)
        binding.searchButton.setOnClickListener {
            // TODO: Implement search functionality
        }



        updateStats(allApps.size, blockedApps.size)
    }

    private fun getUserInstalledApps(): List<ApplicationInfo> {
        val allPackages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        
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
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
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

    private fun filterApps(query: String) {
        val filtered = if (query.isBlank()) {
            allApps
        } else {
            allApps.filter {
                it.loadLabel(packageManager).toString().contains(query, ignoreCase = true)
            }
        }
        adapter.updateApps(filtered)

        // Show or hide empty state
        if (filtered.isEmpty()) {
            binding.emptyStateLayout.visibility = android.view.View.VISIBLE
        } else {
            binding.emptyStateLayout.visibility = android.view.View.GONE
        }
    }

    private fun updateStats(totalApps: Int, blockedCount: Int) {
        binding.totalAppsCount.text = totalApps.toString()
        binding.blockedCount.text = blockedCount.toString()
    }

    private fun showScheduleDialog(packageName: String, appName: String) {
        val currentSchedule = AppScheduleStorage.getAppSchedule(this, packageName)
        
        AppScheduleDialog.show(this, packageName, appName, currentSchedule) { schedule ->
            if (schedule.isEnabled && schedule.timeRanges.isNotEmpty()) {
                AppScheduleStorage.saveAppSchedule(this, schedule)
            } else {
                AppScheduleStorage.removeAppSchedule(this, packageName)
            }
            // Refresh the adapter to show updated schedule info
            adapter.refreshScheduleInfo()
        }
    }

    override fun onStart() {
        super.onStart()
        if (PinUtils.isPinSetup(this)) {
            PinLockActivity.launch(this, this)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PinLockActivity.PIN_REQUEST_CODE && resultCode != RESULT_OK) {
            finish()
        }
    }
}
