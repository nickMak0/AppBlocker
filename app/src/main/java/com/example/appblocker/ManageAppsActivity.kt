// File: app/src/main/java/com/example/appblocker/ManageAppsActivity.kt
package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.adapters.AppListAdapter
import com.example.appblocker.databinding.ActivityManageAppsBinding
import com.example.appblocker.dialogs.AppScheduleDialog
import com.example.appblocker.utils.AppScheduleStorage
import com.example.appblocker.utils.BiometricUtils

class ManageAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageAppsBinding
    private lateinit var adapter: AppListAdapter
    private lateinit var allApps: List<ApplicationInfo>
    private lateinit var blockedApps: MutableSet<String>
    private var isAuthVerified = false

    private val sharedPrefs by lazy {
        getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
    }
    
    private val biometricLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            isAuthVerified = true
            initializeActivity()
        } else {
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check biometric authentication first
        if (BiometricUtils.isBiometricEnabled(this) && !isAuthVerified) {
            val intent = Intent(this, BiometricAuthActivity::class.java)
            biometricLauncher.launch(intent)
        } else {
            initializeActivity()
        }
    }
    
    private fun initializeActivity() {
        // Load blocked apps from SharedPreferences
        blockedApps = sharedPrefs.getStringSet("blockedApps", emptySet())!!.toMutableSet()

        // Get installed user apps
        allApps = getUserInstalledApps()

        // Set up adapter
        adapter = AppListAdapter(this, allApps, blockedApps, 
            onToggleChanged = { pkg, isBlocked ->
                if (isBlocked) {
                    blockedApps.add(pkg)
                    sharedPrefs.edit().putStringSet("blockedApps", blockedApps).apply()
                    updateStats(allApps.size, blockedApps.size)
                    
                    // Broadcast update to other activities
                    val intent = Intent("com.example.appblocker.STATS_UPDATED")
                    sendBroadcast(intent)
                } else {
                    // Show confirmation dialog when trying to unblock an app
                    val appName = try {
                        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
                    } catch (e: Exception) {
                        pkg
                    }
                    
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Unblock App?")
                        .setMessage("Are you sure you want to unblock $appName? This will allow unrestricted access to this app.")
                        .setPositiveButton("Yes, Unblock") { _, _ ->
                            // Add 5 minute delay
                            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                blockedApps.remove(pkg)
                                AppScheduleStorage.removeAppSchedule(this, pkg)
                                sharedPrefs.edit().putStringSet("blockedApps", blockedApps).apply()
                                updateStats(allApps.size, blockedApps.size)
                                
                                // Broadcast update to other activities
                                val intent = Intent("com.example.appblocker.STATS_UPDATED")
                                sendBroadcast(intent)
                                
                                // Refresh adapter to show updated state
                                adapter.notifyDataSetChanged()
                                android.widget.Toast.makeText(this, "$appName unblocked", android.widget.Toast.LENGTH_SHORT).show()
                            }, 300000) // 5 minutes = 300,000 milliseconds
                            android.widget.Toast.makeText(this, "Unblocking in 5 minutes...", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Cancel") { _, _ ->
                            // Refresh adapter to reset toggle state
                            adapter.notifyDataSetChanged()
                        }
                        .show()
                }
            },
            onScheduleClicked = { packageName, appName ->
                showScheduleDialog(packageName, appName)
            }
        )

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appsRecyclerView.adapter = adapter
        
        // Back button functionality
        binding.backButton.setOnClickListener { finish() }
        
        // Search functionality
        setupSearchFunctionality()

        updateStats(allApps.size, blockedApps.size)
    }

    private fun setupSearchFunctionality() {
        // Toggle search bar visibility
        binding.searchButton.setOnClickListener {
            if (binding.searchContainer.visibility == android.view.View.GONE) {
                binding.searchContainer.visibility = android.view.View.VISIBLE
                binding.searchEditText.requestFocus()
            } else {
                binding.searchContainer.visibility = android.view.View.GONE
                binding.searchEditText.text.clear()
                filterApps("")
            }
        }

        // Clear search
        binding.clearSearchButton.setOnClickListener {
            binding.searchEditText.text.clear()
            binding.searchContainer.visibility = android.view.View.GONE
            filterApps("")
        }

        // Search text watcher
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
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


}
