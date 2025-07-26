// app/src/main/java/com/example/appblocker/VpnServiceHelper.kt
package com.example.appblocker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import com.example.appblocker.vpn.AdultSiteBlockerVpnService

object VpnServiceHelper {

    /**
     * Prepares for VPN service. If permission is not granted, launches the VPN permission request.
     * @return true if VPN permission is already granted, false if a permission request was launched.
     */
    fun prepareVpnService(activity: Activity, vpnRequestLauncher: ActivityResultLauncher<Intent>): Boolean {
        try {
            val vpnIntent = VpnService.prepare(activity)
            if (vpnIntent != null) {
                vpnRequestLauncher.launch(vpnIntent)
                return false // Permission request launched
            }
            return true // Permission already granted
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error preparing VPN service", e)
            (activity as? MainActivity)?.showToast("Failed to prepare VPN service")
            return false // Indicate failure or request launched
        }
    }

    fun startVpnService(context: Context) {
        try {
            context.startService(Intent(context, AdultSiteBlockerVpnService::class.java))
            Log.d(Constants.TAG, "VPN service started.")
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error starting VPN service", e)
            throw e // Re-throw to be handled by the caller (e.g., MainActivity)
        }
    }

    fun stopVpnService(context: Context) {
        try {
            context.stopService(Intent(context, AdultSiteBlockerVpnService::class.java))
            Log.d(Constants.TAG, "VPN service stopped.")
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error stopping VPN service", e)
        }
    }

    fun isVpnServiceRunning(context: Context): Boolean {
        // This is a simple check; a more robust check might involve
        // checking the service's own state or using a Binder.
        // For now, we assume if VpnService.prepare returns null, it's running.
        return VpnService.prepare(context) == null
    }
}