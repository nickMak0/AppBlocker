// app/src/main/java/com/example/appblocker/ActivityExtensions.kt
package com.example.appblocker

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

fun Context.showToast(message: String) {
    try {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Log.e(Constants.TAG, "Error showing toast: $message", e)
    }
}

fun AppCompatActivity.navigateToActivity(activityClass: Class<*>) {
    try {
        startActivity(Intent(this, activityClass))
    } catch (e: Exception) {
        Log.e(Constants.TAG, "Error navigating to ${activityClass.simpleName}", e)
        showToast("Failed to open ${activityClass.simpleName}")
    }
}

fun AppCompatActivity.showErrorAndFinish(message: String) {
    showToast(message)
    finish()
}