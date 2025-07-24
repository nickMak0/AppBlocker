// File: app/src/main/java/com/example/appblocker/ManageAppsActivity.kt
package com.example.appblocker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.databinding.ActivityManageAppsBinding

class ManageAppsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageAppsBinding
    private lateinit var adapter: AppListAdapter

    private val sharedPrefs by lazy {
        getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load previously blocked apps
        val blockedApps = sharedPrefs.getStringSet("blockedApps", emptySet())?.toMutableSet()
            ?: mutableSetOf()

        // Get list of user-installed launchable apps
        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                app.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                        packageManager.getLaunchIntentForPackage(app.packageName) != null
            }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        // Setup adapter with toggle logic
        adapter = AppListAdapter(
            context = this,
            apps = installedApps,
            blockedApps = blockedApps
        ) { packageName, isBlocked ->
            if (isBlocked) {
                blockedApps.add(packageName)
            } else {
                blockedApps.remove(packageName)
            }
            sharedPrefs.edit().putStringSet("blockedApps", blockedApps).apply()
        }

        // Bind to RecyclerView
        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appsRecyclerView.adapter = adapter
    }
}
