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
        adapter = AppListAdapter(this, allApps, blockedApps) { pkg, isBlocked ->
            if (isBlocked) {
                blockedApps.add(pkg)
            } else {
                blockedApps.remove(pkg)
            }
            sharedPrefs.edit().putStringSet("blockedApps", blockedApps).apply()
            updateStats(allApps.size, blockedApps.size)
        }

        binding.appsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.appsRecyclerView.adapter = adapter

        // Search functionality
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterApps(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        updateStats(allApps.size, blockedApps.size)
    }

    private fun getUserInstalledApps(): List<ApplicationInfo> {
        return packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                it.packageName != packageName &&
                        (it.flags and ApplicationInfo.FLAG_SYSTEM == 0) &&
                        packageManager.getLaunchIntentForPackage(it.packageName) != null
            }
            .sortedBy { it.loadLabel(packageManager).toString().lowercase() }
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
