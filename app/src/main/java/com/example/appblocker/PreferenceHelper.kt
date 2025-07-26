// app/src/main/java/com/example/appblocker/PreferenceHelper.kt
package com.example.appblocker

import android.content.SharedPreferences

object PreferenceHelper {

    fun getBlockingEnabled(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(Constants.KEY_BLOCKING_ENABLED, true)
    }

    fun setBlockingEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_BLOCKING_ENABLED, enabled).apply()
    }

    fun getVpnBlockingEnabled(prefs: SharedPreferences): Boolean {
        return prefs.getBoolean(Constants.KEY_VPN_BLOCKING_ENABLED, false)
    }

    fun setVpnBlockingEnabled(prefs: SharedPreferences, enabled: Boolean) {
        prefs.edit().putBoolean(Constants.KEY_VPN_BLOCKING_ENABLED, enabled).apply()
    }
}