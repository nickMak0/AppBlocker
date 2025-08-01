package com.example.appblocker.utils

import android.content.Context
import androidx.biometric.BiometricManager

object BiometricUtils {
    private const val PREFS_NAME = "AppBlockerPrefs"
    private const val BIOMETRIC_ENABLED_KEY = "biometric_enabled"

    fun isBiometricEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(BIOMETRIC_ENABLED_KEY, false)
    }

    fun setBiometricEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(BIOMETRIC_ENABLED_KEY, enabled)
            .apply()
    }

    fun isBiometricAvailable(context: Context): Boolean {
        return BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}