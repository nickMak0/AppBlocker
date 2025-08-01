package com.example.appblocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.appblocker.utils.BiometricUtils
import com.example.appblocker.utils.StatsManager
import com.example.appblocker.utils.StatsUpdateReceiver
import android.content.BroadcastReceiver
import android.content.IntentFilter
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch

/**
 * Main entry point for the AppBlocker application.
 * Handles navigation, permissions, and displays key stats.
 */
class MainActivity : AppCompatActivity() {

    // UI Components
    private lateinit var appBlockingSwitch: MaterialSwitch
    private lateinit var vpnBlockingSwitch: MaterialSwitch
    private lateinit var strictModeSwitch: MaterialSwitch
    private lateinit var breakModeButton: MaterialButton
    private lateinit var emergencyDisableButton: MaterialButton
    private lateinit var scheduleModeButton: MaterialButton
    private lateinit var manageAppsCard: MaterialCardView
    private lateinit var dashboardCard: MaterialCardView
    private lateinit var scheduleCard: MaterialCardView
    private lateinit var settingsCard: MaterialCardView
    private lateinit var permissionCard: android.view.View
    private lateinit var permissionIcon: ImageView
    private lateinit var permissionStatusText: TextView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var overallStatus: TextView
    private lateinit var usageAccessIcon: ImageView
    private lateinit var accessibilityIcon: ImageView
    private lateinit var settingsButton: ImageView
    private lateinit var appsBlockedCount: TextView
    private lateinit var sitesBlockedCount: TextView
    // focusTimeCount removed
    private lateinit var streakCount: TextView

    private lateinit var overallProtectionStatus: TextView

    // Preferences and State
    private lateinit var prefs: SharedPreferences
    private var wasPinVerified = false
    
    // Stats update receiver
    private val statsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            android.util.Log.d("MainActivity", "Stats update broadcast received")
            if (wasPinVerified) {
                updateStatsUI()
            }
        }
    }

    // Activity Result Launchers
    private lateinit var biometricLauncher: ActivityResultLauncher<Intent>
    private lateinit var vpnRequestLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatsManager.init(this)
        setTheme(R.style.Theme_AppBlocker)
        prefs = getSharedPreferences("AppBlockerPrefs", Context.MODE_PRIVATE)
        setupActivityResultLaunchers()

        if (BiometricUtils.isBiometricEnabled(this)) {
            biometricLauncher.launch(Intent(this, BiometricAuthActivity::class.java))
        } else {
            initializeMainInterface()
        }
    }

    override fun onResume() {
        super.onResume()
        if (wasPinVerified) {
            updateAllUI()
        }
    }
    
    override fun onStart() {
        super.onStart()
        if (wasPinVerified) {
            updateStatsUI()
        }
        // Register for stats updates
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(statsReceiver, IntentFilter("com.example.appblocker.STATS_UPDATED"), Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(statsReceiver, IntentFilter("com.example.appblocker.STATS_UPDATED"))
        }
    }
    
    override fun onStop() {
        super.onStop()
        try {
            unregisterReceiver(statsReceiver)
        } catch (e: Exception) {
            // Receiver not registered
        }
    }

    /**
     * Registers activity result launchers for PIN and VPN permission.
     */
    private fun setupActivityResultLaunchers() {
        biometricLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                wasPinVerified = true
                initializeMainInterface()
            } else {
                finish()
            }
        }
        vpnRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            handleVpnPermissionResult(result.resultCode)
        }
    }

    /**
     * Initializes the main UI after PIN verification.
     */
    private fun initializeMainInterface() {
        wasPinVerified = true
        setContentView(R.layout.activity_main)
        initializeViews()
        setupEventListeners()
        loadSavedStates()
        updateAllUI()
    }

    private fun initializeViews() {
        // Controls
        val controlsInclude = findViewById<android.view.View>(R.id.includeControls)
        appBlockingSwitch = controlsInclude.findViewById(R.id.appBlockingSwitch)
        vpnBlockingSwitch = controlsInclude.findViewById(R.id.vpnBlockingSwitch)
        strictModeSwitch = controlsInclude.findViewById(R.id.strictModeSwitch)
        breakModeButton = controlsInclude.findViewById(R.id.breakModeButton)
        emergencyDisableButton = controlsInclude.findViewById(R.id.emergencyDisableButton)
        scheduleModeButton = controlsInclude.findViewById(R.id.scheduleModeButton)

        // Navigation
        val quickActionsInclude = findViewById<android.view.View>(R.id.includeQuickActions)
        dashboardCard = quickActionsInclude.findViewById(R.id.dashboardCard)
        manageAppsCard = quickActionsInclude.findViewById(R.id.manageAppsCard)
        scheduleCard = quickActionsInclude.findViewById(R.id.scheduleCard)
        settingsCard = quickActionsInclude.findViewById(R.id.settingsCard)

        // Permissions
        val permissionInclude = findViewById<android.view.View>(R.id.includePermission)
        permissionCard = permissionInclude
        permissionIcon = permissionInclude.findViewById(R.id.permissionIcon)
        permissionStatusText = permissionInclude.findViewById(R.id.permissionStatusText)
        accessibilityStatusText = permissionInclude.findViewById(R.id.accessibilityStatusText)
        overallStatus = permissionInclude.findViewById(R.id.overallStatus)
        usageAccessIcon = permissionInclude.findViewById(R.id.usageAccessIcon)
        accessibilityIcon = permissionInclude.findViewById(R.id.accessibilityIcon)
        settingsButton = permissionInclude.findViewById(R.id.settingsButton)

        // Stats
        appsBlockedCount = findViewById(R.id.appsBlockedCount)
        sitesBlockedCount = findViewById(R.id.sitesBlockedCount)
        overallProtectionStatus = findViewById(R.id.overallProtectionStatus)
    }

    private fun setupEventListeners() {
        appBlockingSwitch.setOnCheckedChangeListener { _, isChecked -> handleAppBlockingToggle(isChecked) }
        vpnBlockingSwitch.setOnCheckedChangeListener { _, isChecked -> handleVpnBlockingToggle(isChecked) }
        strictModeSwitch.setOnCheckedChangeListener { _, isChecked -> handleStrictModeToggle(isChecked) }
        breakModeButton.setOnClickListener { handleBreakMode() }
        emergencyDisableButton.setOnClickListener { handleEmergencyDisable() }
        scheduleModeButton.setOnClickListener { navigateToActivity(ScheduleSettingsActivity::class.java) }
        dashboardCard.setOnClickListener { navigateToActivity(DashboardActivity::class.java) }
        manageAppsCard.setOnClickListener { navigateToActivity(ManageAppsActivity::class.java) }
        scheduleCard.setOnClickListener { navigateToActivity(ScheduleSettingsActivity::class.java) }
        settingsCard.setOnClickListener { navigateToActivity(BiometricSettingsActivity::class.java) }
        permissionCard.setOnClickListener { openPermissionSettings() }
        settingsButton.setOnClickListener { openPermissionSettings() }
        
        // Test button - click to increment stats
        appsBlockedCount.setOnClickListener {
            android.util.Log.d("MainActivity", "Apps blocked count clicked - testing increment")
            StatsManager.testIncrementStats(this)
            showToast("Test stats incremented")
        }
    }

    private fun loadSavedStates() {
        appBlockingSwitch.isChecked = PreferenceHelper.getBlockingEnabled(prefs)
        vpnBlockingSwitch.isChecked = PreferenceHelper.getVpnBlockingEnabled(prefs)
        strictModeSwitch.isChecked = prefs.getBoolean("strict_mode_enabled", false)
        if (vpnBlockingSwitch.isChecked) handleVpnServiceStart()
    }

    private fun handleAppBlockingToggle(isChecked: Boolean) {
        if (!isChecked) {
            // Show confirmation dialog when trying to disable
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Disable App Blocking?")
                .setMessage("Are you sure you want to disable app blocking? This will allow access to all blocked apps.")
                .setPositiveButton("Yes, Disable") { _, _ ->
                    // Add 5 minute delay to make it harder
                    appBlockingSwitch.isEnabled = false
                    appBlockingSwitch.postDelayed({
                        PreferenceHelper.setBlockingEnabled(prefs, false)
                        updateProtectionStatus()
                        appBlockingSwitch.isEnabled = true
                        showToast("App blocking disabled")
                    }, 300000) // 5 minutes = 300,000 milliseconds
                    showToast("Disabling in 5 minutes...")
                }
                .setNegativeButton("Cancel") { _, _ ->
                    appBlockingSwitch.isChecked = true
                }
                .show()
        } else {
            PreferenceHelper.setBlockingEnabled(prefs, true)
            if (!PermissionUtils.isAccessibilityServiceEnabled(this, packageName)) {
                showToast(getString(R.string.enable_accessibility_service))
                PermissionUtils.openAccessibilitySettings(this)
            }
            updateProtectionStatus()
        }
    }

    private fun handleVpnBlockingToggle(isChecked: Boolean) {
        if (isChecked) {
            if (VpnServiceHelper.prepareVpnService(this, vpnRequestLauncher)) {
                VpnServiceHelper.startVpnService(this)
                PreferenceHelper.setVpnBlockingEnabled(prefs, true)
            }
        } else {
            // Show confirmation dialog when trying to disable
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Disable Website Blocking?")
                .setMessage("Are you sure you want to disable website blocking? This will allow access to blocked websites.")
                .setPositiveButton("Yes, Disable") { _, _ ->
                    vpnBlockingSwitch.isEnabled = false
                    vpnBlockingSwitch.postDelayed({
                        VpnServiceHelper.stopVpnService(this)
                        PreferenceHelper.setVpnBlockingEnabled(prefs, false)
                        updateProtectionStatus()
                        vpnBlockingSwitch.isEnabled = true
                        showToast("Website blocking disabled")
                    }, 300000) // 5 minutes = 300,000 milliseconds
                    showToast("Disabling in 5 minutes...")
                }
                .setNegativeButton("Cancel") { _, _ ->
                    vpnBlockingSwitch.isChecked = true
                }
                .show()
        }
        updateProtectionStatus()
    }

    private fun handleVpnPermissionResult(resultCode: Int) {
        if (resultCode == Activity.RESULT_OK) {
            startService(Intent(this, SiteBlockerVpnService::class.java))
            PreferenceHelper.setVpnBlockingEnabled(prefs, true)
            vpnBlockingSwitch.isChecked = true
            showToast("Site blocking enabled")
        } else {
            vpnBlockingSwitch.isChecked = false
            PreferenceHelper.setVpnBlockingEnabled(prefs, false)
            showToast("VPN permission denied")
        }
        updateProtectionStatus()
    }

    private fun handleVpnServiceStart() {
        if (PreferenceHelper.getVpnBlockingEnabled(prefs) && !VpnServiceHelper.isVpnServiceRunning(this)) {
            if (VpnServiceHelper.prepareVpnService(this, vpnRequestLauncher)) {
                VpnServiceHelper.startVpnService(this)
            } else {
                vpnBlockingSwitch.isChecked = false
                PreferenceHelper.setVpnBlockingEnabled(prefs, false)
            }
        }
    }

    private fun handleStrictModeToggle(isChecked: Boolean) {
        if (!isChecked) {
            // Show confirmation dialog when trying to disable
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Disable Strict Mode?")
                .setMessage("Are you sure you want to disable strict mode? This will reduce blocking effectiveness.")
                .setPositiveButton("Yes, Disable") { _, _ ->
                    prefs.edit().putBoolean("strict_mode_enabled", false).apply()
                    showToast("Strict mode disabled")
                    updateProtectionStatus()
                }
                .setNegativeButton("Cancel") { _, _ ->
                    strictModeSwitch.isChecked = true
                }
                .show()
        } else {
            prefs.edit().putBoolean("strict_mode_enabled", true).apply()
            showToast("Strict mode enabled")
            updateProtectionStatus()
        }
    }

    private fun handleBreakMode() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Take a 5-Minute Break?")
            .setMessage("This will temporarily disable app blocking for 5 minutes. Use this time wisely!")
            .setPositiveButton("Start Break") { _, _ ->
                prefs.edit().putBoolean("break_mode_active", true).apply()
                startActivity(Intent(this, BreakModeActivity::class.java))
                showToast("5-minute break started")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun handleEmergencyDisable() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Emergency Disable")
            .setMessage("This will disable ALL blocking features. Are you sure this is an emergency?")
            .setPositiveButton("Yes, Emergency") { _, _ ->
                // Add 5 second delay for emergency disable
                emergencyDisableButton.isEnabled = false
                emergencyDisableButton.text = "Disabling..."
                emergencyDisableButton.postDelayed({
                    appBlockingSwitch.isChecked = false
                    vpnBlockingSwitch.isChecked = false
                    strictModeSwitch.isChecked = false
                    PreferenceHelper.setBlockingEnabled(prefs, false)
                    PreferenceHelper.setVpnBlockingEnabled(prefs, false)
                    prefs.edit().putBoolean("strict_mode_enabled", false).apply()
                    VpnServiceHelper.stopVpnService(this)
                    
                    showToast(getString(R.string.all_blocking_disabled))
                    updateProtectionStatus()
                    emergencyDisableButton.isEnabled = true
                    emergencyDisableButton.text = "Emergency"
                }, 5000)
                showToast("Emergency disable in 5 seconds...")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateAllUI() {
        updateStatsUI()
        updatePermissionStatus()
        updateProtectionStatus()
    }

    private fun updateStatsUI() {
        if (!::appsBlockedCount.isInitialized) return
        try {
            StatsManager.init(this)
            val appsBlocked = StatsManager.getAppsBlocked(this)
            val sitesBlocked = StatsManager.getSitesBlocked(this)
            
            android.util.Log.d("MainActivity", "Updating UI - Apps Blocked Today: $appsBlocked, Sites Blocked Today: $sitesBlocked")
            
            appsBlockedCount.text = appsBlocked.toString()
            sitesBlockedCount.text = sitesBlocked.toString()
            updateStatsDate()
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error updating stats UI", e)
            appsBlockedCount.text = "0"
            sitesBlockedCount.text = "0"
        }
    }

    private fun updateStatsDate() {
        // Stats date functionality removed for compilation
    }

    private fun updateProtectionStatus() {
        val isBlockingEnabled = PreferenceHelper.getBlockingEnabled(prefs)
        val isVpnEnabled = PreferenceHelper.getVpnBlockingEnabled(prefs)
        val isStrictMode = prefs.getBoolean("strict_mode_enabled", false)
        val (statusText, statusColorResId) = when {
            isBlockingEnabled && isVpnEnabled && isStrictMode -> "Maximum Protection" to android.R.color.holo_red_dark
            isBlockingEnabled && isVpnEnabled -> getString(R.string.full_protection) to android.R.color.holo_green_dark
            isBlockingEnabled || isVpnEnabled -> getString(R.string.partial_protection) to android.R.color.holo_orange_dark
            else -> getString(R.string.disabled) to android.R.color.holo_red_dark
        }
        overallProtectionStatus.text = statusText
        overallProtectionStatus.setTextColor(ContextCompat.getColor(this, statusColorResId))
    }

    private fun updatePermissionStatus() {
        val usageAccessGranted = PermissionUtils.isUsageAccessGranted(this)
        val accessibilityEnabled = PermissionUtils.isAccessibilityServiceEnabled(this, packageName)
        permissionStatusText.text = if (usageAccessGranted) getString(R.string.usage_access_granted) else getString(R.string.usage_access_required)
        accessibilityStatusText.text = if (accessibilityEnabled) getString(R.string.accessibility_enabled) else getString(R.string.accessibility_required)
        updatePermissionIcons(usageAccessGranted, accessibilityEnabled)
        updateOverallPermissionStatus(usageAccessGranted, accessibilityEnabled)
    }

    private fun updatePermissionIcons(usageGranted: Boolean, accessibilityEnabled: Boolean) {
        val usageIconRes = if (usageGranted) android.R.drawable.ic_menu_info_details else android.R.drawable.ic_dialog_alert
        val usageIconColor = if (usageGranted) android.R.color.holo_green_dark else android.R.color.holo_red_dark
        usageAccessIcon.setImageResource(usageIconRes)
        usageAccessIcon.setColorFilter(ContextCompat.getColor(this, usageIconColor))

        val accessibilityIconRes = if (accessibilityEnabled) android.R.drawable.ic_menu_preferences else android.R.drawable.ic_dialog_alert
        val accessibilityIconColor = if (accessibilityEnabled) android.R.color.holo_green_dark else android.R.color.holo_red_dark
        accessibilityIcon.setImageResource(accessibilityIconRes)
        accessibilityIcon.setColorFilter(ContextCompat.getColor(this, accessibilityIconColor))
    }

    private fun updateOverallPermissionStatus(usageGranted: Boolean, accessibilityEnabled: Boolean) {
        val allPermissionsGranted = usageGranted && accessibilityEnabled
        if (allPermissionsGranted) {
            overallStatus.text = getString(R.string.all_permissions_granted)
            overallStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            permissionIcon.setImageResource(android.R.drawable.ic_dialog_info)
            permissionIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white))
        } else {
            val pendingCount = listOf(usageGranted, accessibilityEnabled).count { !it }
            overallStatus.text = resources.getQuantityString(R.plurals.permissions_required, pendingCount, pendingCount)
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

    // Removed getBlockedAppsCount - no longer needed

    private fun setDefaultStatsValues() {
        if (!::appsBlockedCount.isInitialized) return
        appsBlockedCount.text = "0"
        sitesBlockedCount.text = "0"
        // Focus time removed
        // Streak removed
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun navigateToActivity(clazz: Class<*>) {
        startActivity(Intent(this, clazz))
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up any resources if needed
    }
}