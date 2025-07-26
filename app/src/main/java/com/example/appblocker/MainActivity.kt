// app/src/main/java/com/example/appblocker/MainActivity.kt
package com.example.appblocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appblocker.utils.PinUtils
import com.example.appblocker.utils.StatsManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    // UI Components - Controls
    private lateinit var appBlockingSwitch: MaterialSwitch
    private lateinit var vpnBlockingSwitch: MaterialSwitch
    private lateinit var emergencyDisableButton: MaterialButton
    private lateinit var scheduleModeButton: MaterialButton

    // UI Components - Navigation
    private lateinit var manageAppsCard: MaterialCardView
    private lateinit var dashboardCard: MaterialCardView
    private lateinit var scheduleCard: MaterialCardView
    private lateinit var settingsCard: MaterialCardView

    // UI Components - Permissions
    private lateinit var permissionCard: View
    private lateinit var permissionIcon: ImageView
    private lateinit var permissionStatusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var overallStatus: TextView
    private lateinit var usageAccessIcon: ImageView
    private lateinit var accessibilityIcon: ImageView
    private lateinit var settingsButton: ImageView

    // UI Components - Stats (Only detailed stats remain)
    private lateinit var appsBlockedCount: TextView
    private lateinit var sitesBlockedCount: TextView
    private lateinit var focusTimeCount: TextView
    private lateinit var streakCount: TextView
    private lateinit var statsDate: TextView // This is the date in the header of the stats card
    private lateinit var overallProtectionStatus: TextView // This is for the overall status, likely in activity_main.xml

    // Preferences and State
    private lateinit var prefs: SharedPreferences
    private var wasPinVerified = false

    // Activity Result Launchers
    private lateinit var pinLauncher: ActivityResultLauncher<Intent>
    private lateinit var vpnRequestLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            initializeApp()
            setupActivityResultLaunchers()

            if (PinUtils.isPinSetup(this)) {
                showPinScreen()
            } else {
                initializeMainInterface()
            }
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error in onCreate", e)
            showErrorAndFinish("Failed to initialize app")
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure views are initialized before updating UI, especially after PIN verification
        // Check if `statsDate` or `overallProtectionStatus` are initialized to ensure `initializeMainInterface` ran
        if (wasPinVerified && ::statsDate.isInitialized) { // Use a reliable initialized view
            try {
                updateAllUI()
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error updating UI in onResume", e)
            }
        }
    }

    private fun initializeApp() {
        StatsManager.init(this)
        setTheme(R.style.Theme_AppBlocker)
        prefs = getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun setupActivityResultLaunchers() {
        pinLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handlePinResult(result.resultCode)
        }

        vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleVpnPermissionResult(result.resultCode)
        }
    }

    private fun showPinScreen() {
        pinLauncher.launch(Intent(this, PinLockActivity::class.java))
    }

    private fun handlePinResult(resultCode: Int) {
        if (resultCode == RESULT_OK) {
            wasPinVerified = true
            initializeMainInterface()
        } else {
            finish()
        }
    }

    private fun initializeMainInterface() {
        wasPinVerified = true
        setContentView(R.layout.activity_main) // Assuming activity_main.xml includes the layout_stats_section

        try {
            initializeViews()
            setupEventListeners()
            loadSavedStates()
            updateAllUI()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error initializing main interface", e)
            showErrorAndFinish("Failed to load interface")
        }
    }

    private fun initializeViews() {
        initializeControlViews()
        initializeNavigationViews()
        initializePermissionViews()
        overallProtectionStatus = findViewById(R.id.overallProtectionStatus) // Ensure this ID is still valid in activity_main.xml
    }

    private fun initializeControlViews() {
        val controlsInclude = findViewById<View>(R.id.includeControls)
        appBlockingSwitch = controlsInclude.findViewById(R.id.appBlockingSwitch)
        vpnBlockingSwitch = controlsInclude.findViewById(R.id.vpnBlockingSwitch)
        emergencyDisableButton = controlsInclude.findViewById(R.id.emergencyDisableButton)
        scheduleModeButton = controlsInclude.findViewById(R.id.scheduleModeButton)
    }

    private fun initializeNavigationViews() {
        val quickActionsInclude = findViewById<View>(R.id.includeQuickActions)
        dashboardCard = quickActionsInclude.findViewById(R.id.dashboardCard)
        manageAppsCard = quickActionsInclude.findViewById(R.id.manageAppsCard)
        scheduleCard = quickActionsInclude.findViewById(R.id.scheduleCard)
        settingsCard = quickActionsInclude.findViewById(R.id.settingsCard)
    }

    private fun initializePermissionViews() {
        val permissionInclude = findViewById<View>(R.id.includePermission)
        permissionCard = permissionInclude
        permissionIcon = permissionInclude.findViewById(R.id.permissionIcon)
        permissionStatusText = permissionInclude.findViewById(R.id.permissionStatusText)
        accessibilityStatusText = permissionInclude.findViewById(R.id.accessibilityStatusText)
        overallStatus = permissionInclude.findViewById(R.id.overallStatus)
        usageAccessIcon = permissionInclude.findViewById(R.id.usageAccessIcon)
        accessibilityIcon = permissionInclude.findViewById(R.id.accessibilityIcon)
        settingsButton = permissionInclude.findViewById(R.id.settingsButton)
    }


    private fun setupEventListeners() {
        setupSwitchListeners()
        setupButtonListeners()
        setupNavigationListeners()
        setupPermissionListeners()
    }

    private fun setupSwitchListeners() {
        appBlockingSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleAppBlockingToggle(isChecked)
        }

        vpnBlockingSwitch.setOnCheckedChangeListener { _, isChecked ->
            handleVpnBlockingToggle(isChecked)
        }
    }

    private fun setupButtonListeners() {
        emergencyDisableButton.setOnClickListener {
            handleEmergencyDisable()
        }

        scheduleModeButton.setOnClickListener {
            navigateToActivity(ScheduleSettingsActivity::class.java)
        }
    }

    private fun setupNavigationListeners() {
        dashboardCard.setOnClickListener { navigateToActivity(DashboardActivity::class.java) }
        manageAppsCard.setOnClickListener { navigateToActivity(ManageAppsActivity::class.java) }
        scheduleCard.setOnClickListener { navigateToActivity(ScheduleSettingsActivity::class.java) }
        settingsCard.setOnClickListener { PermissionUtils.openAppSettings(this, packageName) }
    }

    private fun setupPermissionListeners() {
        permissionCard.setOnClickListener { openPermissionSettings() }
        settingsButton.setOnClickListener { openPermissionSettings() }
    }

    private fun loadSavedStates() {
        val isBlockingEnabled = PreferenceHelper.getBlockingEnabled(prefs)
        val isVpnEnabled = PreferenceHelper.getVpnBlockingEnabled(prefs)

        appBlockingSwitch.isChecked = isBlockingEnabled
        vpnBlockingSwitch.isChecked = isVpnEnabled

        if (isVpnEnabled) {
            handleVpnServiceStart()
        }
    }

    private fun handleAppBlockingToggle(isChecked: Boolean) {
        try {
            PreferenceHelper.setBlockingEnabled(prefs, isChecked)

            if (isChecked && !PermissionUtils.isAccessibilityServiceEnabled(this, packageName)) {
                showToast("Please enable accessibility service")
                PermissionUtils.openAccessibilitySettings(this)
            }
            updateProtectionStatus()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error handling app blocking toggle", e)
            showToast("Failed to update app blocking")
        }
    }

    private fun handleVpnBlockingToggle(isChecked: Boolean) {
        try {
            if (isChecked) {
                if (VpnServiceHelper.prepareVpnService(this, vpnRequestLauncher)) {
                    // Permission already granted, start service
                    VpnServiceHelper.startVpnService(this)
                    PreferenceHelper.setVpnBlockingEnabled(prefs, true)
                }
                // Else, prepareVpnService already launched the activity, result handled in launcher
            } else {
                VpnServiceHelper.stopVpnService(this)
                PreferenceHelper.setVpnBlockingEnabled(prefs, false)
            }
            updateProtectionStatus()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error handling VPN blocking toggle", e)
            showToast("Failed to update VPN blocking")
            vpnBlockingSwitch.isChecked = false // Reset switch on error
        }
    }

    private fun handleVpnPermissionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            try {
                VpnServiceHelper.startVpnService(this)
                PreferenceHelper.setVpnBlockingEnabled(prefs, true)
                vpnBlockingSwitch.isChecked = true
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error starting VPN service after permission", e)
                showToast("Failed to start VPN service.")
                vpnBlockingSwitch.isChecked = false
                PreferenceHelper.setVpnBlockingEnabled(prefs, false)
            }
        } else {
            vpnBlockingSwitch.isChecked = false
            PreferenceHelper.setVpnBlockingEnabled(prefs, false)
            showToast("VPN permission denied")
        }
        updateProtectionStatus()
    }

    private fun handleVpnServiceStart() {
        // Check if VPN permission is still valid and service needs to be running
        if (PreferenceHelper.getVpnBlockingEnabled(prefs) && !VpnServiceHelper.isVpnServiceRunning(this)) {
            try {
                // If VpnService.prepare returns null, it means we have permission, so start it
                if (VpnServiceHelper.prepareVpnService(this, vpnRequestLauncher)) {
                    VpnServiceHelper.startVpnService(this)
                } else {
                    // Permission was revoked or needs re-approval
                    vpnBlockingSwitch.isChecked = false
                    PreferenceHelper.setVpnBlockingEnabled(prefs, false)
                }
            } catch (e: Exception) {
                Log.e(Constants.TAG, "Error re-starting VPN service", e)
                vpnBlockingSwitch.isChecked = false
                PreferenceHelper.setVpnBlockingEnabled(prefs, false)
            }
        }
    }

    private fun handleEmergencyDisable() {
        try {
            appBlockingSwitch.isChecked = false
            vpnBlockingSwitch.isChecked = false

            PreferenceHelper.setBlockingEnabled(prefs, false)
            PreferenceHelper.setVpnBlockingEnabled(prefs, false)

            VpnServiceHelper.stopVpnService(this)
            showToast("All blocking disabled")
            updateProtectionStatus()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error in emergency disable", e)
            showToast("Failed to disable all blocking")
        }
    }

    private fun updateAllUI() {
        updateStatsUI()
        updatePermissionStatus()
        updateProtectionStatus()
    }

    private fun updateStatsUI() {
        try {
            val appsBlocked = StatsManager.getAppsBlocked(this)
            val sitesBlocked = StatsManager.getSitesBlocked(this)
            val focusTime = StatsManager.getFocusTimeFormatted(this)
            val streak = StatsManager.getStreakDays(this)

            // Removed heroAppsBlocked, heroFocusTime, heroStreak assignments

            appsBlockedCount.text = appsBlocked.toString()
            sitesBlockedCount.text = sitesBlocked.toString()
            focusTimeCount.text = focusTime
            streakCount.text = "$streak" // Changed to just number as "Day Streak" is in UI

            updateStatsDate()
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error updating stats UI", e)
            setDefaultStatsValues()
        }
    }

    private fun updateStatsDate() {
        try {
            // Using current date: Saturday, July 26, 2025
            val dateFormat = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            statsDate.text = currentDate // "Saturday, July 26"
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error updating stats date", e)
            statsDate.text = "Today"
        }
    }

    private fun updateProtectionStatus() {
        try {
            val isBlockingEnabled = PreferenceHelper.getBlockingEnabled(prefs)
            val isVpnEnabled = PreferenceHelper.getVpnBlockingEnabled(prefs)

            val (statusText, statusColorResId) = when {
                isBlockingEnabled && isVpnEnabled -> {
                    Pair("Full Protection", android.R.color.holo_green_dark)
                }
                isBlockingEnabled || isVpnEnabled -> {
                    Pair("Partial Protection", android.R.color.holo_orange_dark)
                }
                else -> {
                    Pair("Disabled", android.R.color.holo_red_dark)
                }
            }

            if (::overallProtectionStatus.isInitialized) {
                overallProtectionStatus.text = statusText
                overallProtectionStatus.setTextColor(ContextCompat.getColor(this, statusColorResId))
            } else {
                Log.w(Constants.TAG, "overallProtectionStatus (activity_main) not initialized, cannot update.")
            }

            // Removed `statsCardProtectionStatus` update as it's no longer in the XML
            // if (::statsCardProtectionStatus.isInitialized) { ... }

        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error updating protection status", e)
            if (::overallProtectionStatus.isInitialized) {
                overallProtectionStatus.text = "Unknown"
                overallProtectionStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            }
            // Removed `statsCardProtectionStatus` error handling
        }
    }

    private fun updatePermissionStatus() {
        try {
            val usageAccessGranted = PermissionUtils.isUsageAccessGranted(this)
            val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(this, packageName)

            updatePermissionTexts(usageAccessGranted, accessibilityEnabled)
            updatePermissionIcons(usageAccessGranted, accessibilityEnabled)
            updateOverallPermissionStatus(usageAccessGranted, accessibilityEnabled)

        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error updating permission status", e)
            setDefaultPermissionValues()
        }
    }

    private fun updatePermissionTexts(usageGranted: Boolean, accessibilityEnabled: Boolean) {
        permissionStatusText.text = if (usageGranted) "Usage Access: ✓ Granted" else "Usage Access: ✗ Required"
        accessibilityStatusText.text = if (accessibilityEnabled) "Accessibility: ✓ Enabled" else "Accessibility: ✗ Required"
    }

    private fun updatePermissionIcons(usageGranted: Boolean, accessibilityEnabled: Boolean) {
        // Update usage access icon
        val usageIconRes = if (usageGranted) android.R.drawable.ic_menu_info_details else android.R.drawable.ic_dialog_alert
        val usageIconColor = if (usageGranted) android.R.color.holo_green_dark else android.R.color.holo_red_dark
        usageAccessIcon.setImageResource(usageIconRes)
        usageAccessIcon.setColorFilter(ContextCompat.getColor(this, usageIconColor))

        // Update accessibility icon
        val accessibilityIconRes = if (accessibilityEnabled) android.R.drawable.ic_menu_preferences else android.R.drawable.ic_dialog_alert
        val accessibilityIconColor = if (accessibilityEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark
        accessibilityIcon.setImageResource(accessibilityIconRes)
        accessibilityIcon.setColorFilter(ContextCompat.getColor(this, accessibilityIconColor))
    }

    private fun updateOverallPermissionStatus(usageGranted: Boolean, accessibilityEnabled: Boolean) {
        val allPermissionsGranted = usageGranted && accessibilityEnabled

        if (allPermissionsGranted) {
            overallStatus.text = "All permissions granted"
            overallStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            permissionIcon.setImageResource(android.R.drawable.ic_dialog_info)
            permissionIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        } else {
            val pendingCount = listOf(usageGranted, accessibilityEnabled).count { !it }
            overallStatus.text = "$pendingCount permission${if (pendingCount > 1) "s" else ""} required"
            overallStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            permissionIcon.setImageResource(android.R.drawable.ic_dialog_alert)
            permissionIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        }
    }

    private fun openPermissionSettings() {
        val usageAccessGranted = PermissionUtils.isUsageAccessGranted(this)
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(this, packageName)

        when {
            !usageAccessGranted -> PermissionUtils.openUsageAccessSettings(this)
            !accessibilityEnabled -> PermissionUtils.openAccessibilitySettings(this)
            else -> PermissionUtils.openAppSettings(this, packageName)
        }
    }

    private fun setDefaultStatsValues() {
        try {
            // Removed heroAppsBlocked, heroFocusTime, heroStreak default assignments
            appsBlockedCount.text = "0"
            sitesBlockedCount.text = "0"
            focusTimeCount.text = "0m"
            streakCount.text = "0" // Changed to just number
            statsDate.text = "Today"
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error setting default stats values", e)
        }
    }

    private fun setDefaultPermissionValues() {
        try {
            permissionStatusText.text = "Usage Access: Unknown"
            accessibilityStatusText.text = "Accessibility: Unknown"
            overallStatus.text = "Permission status unknown"
            overallStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error setting default permission values", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }
}