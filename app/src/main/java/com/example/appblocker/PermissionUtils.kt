// app/src/main/java/com/example/appblocker/PermissionUtils.kt
package com.example.appblocker

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.accessibility.AccessibilityManager

object PermissionUtils {

    fun isUsageAccessGranted(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOpsManager.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error checking usage access", e)
            false
        }
    }

    fun isAccessibilityServiceEnabled(context: Context, servicePackageName: String): Boolean {
        return try {
            val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""

            // Check if our service is in the enabled services list
            return enabledServices.contains("$servicePackageName/")
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error checking accessibility service", e)
            false
        }
    }

    fun openUsageAccessSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error opening usage access settings", e)
            (context as? MainActivity)?.showToast("Failed to open usage access settings")
        }
    }

    fun openAccessibilitySettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error opening accessibility settings", e)
            (context as? MainActivity)?.showToast("Failed to open accessibility settings")
        }
    }

    fun openAppSettings(context: Context, packageName: String) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(Constants.TAG, "Error opening app settings", e)
            (context as? MainActivity)?.showToast("Failed to open app settings")
        }
    }
}