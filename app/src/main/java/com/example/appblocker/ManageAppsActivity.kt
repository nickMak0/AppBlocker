package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appblocker.databinding.ActivityManageAppsBinding
import com.example.appblocker.utils.PinUtils

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

        val blockedApps = sharedPrefs.getStringSet("blockedApps", emptySet())?.toMutableSet()
            ?: mutableSetOf()

        val installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { app ->
                app.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                        packageManager.getLaunchIntentForPackage(app.packageName) != null
            }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }

        adapter = AppListAdapter(
            context = this,
            apps = installedApps,
            blockedApps = blockedApps
        ) { packageName, isBlocked ->
            if (isBlocked) blockedApps.add(packageName) else blockedApps.remove(packageName)
            sharedPrefs.edit().putStringSet("blockedApps", blockedApps).apply()
        }

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appsRecyclerView.adapter = adapter
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
