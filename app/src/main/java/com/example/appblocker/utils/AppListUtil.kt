package com.example.appblocker.utils

import android.content.pm.PackageManager
import android.content.Intent
import com.example.appblocker.model.AppItem

object AppListUtil {

    fun getInstalledApps(packageManager: PackageManager): List<AppItem> {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)

        val resolvedApps = packageManager.queryIntentActivities(intent, 0)

        return resolvedApps
            .map {
                val appName = it.loadLabel(packageManager).toString()
                val packageName = it.activityInfo.packageName
                AppItem(packageName, appName)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.appName.lowercase() }
    }
}
