package com.example.appblocker.utils

import android.content.Context
import android.util.Base64
import java.security.MessageDigest

object PinUtils {
    private const val PREFS_NAME = "AppBlockerPrefs"
    private const val PIN_KEY = "pin_code"
    private const val SETUP_KEY = "pin_setup_done"

    fun isPinSetup(context: Context): Boolean {
        val isSetup = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(SETUP_KEY, false)
        android.util.Log.d("PinUtils", "isPinSetup: $isSetup")
        return isSetup
    }

    fun savePin(context: Context, pin: String) {
        val hashed = hash(pin)
        android.util.Log.d("PinUtils", "Saving PIN hash: ${hashed.take(10)}...")
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(PIN_KEY, hashed)
            .putBoolean(SETUP_KEY, true)
            .apply()
        android.util.Log.d("PinUtils", "PIN saved successfully")
    }

    fun verifyPin(context: Context, input: String): Boolean {
        val hashedInput = hash(input)
        val storedHash = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(PIN_KEY, null)
        android.util.Log.d("PinUtils", "Verifying PIN - Input hash: ${hashedInput.take(10)}..., Stored hash: ${storedHash?.take(10)}...")
        val isValid = hashedInput == storedHash
        android.util.Log.d("PinUtils", "PIN verification result: $isValid")
        return isValid
    }

    private fun hash(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }
}
