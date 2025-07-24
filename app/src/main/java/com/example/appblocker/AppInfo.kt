// File: app/src/main/java/com/example/appblocker/AppInfo.kt
package com.example.appblocker

import android.graphics.drawable.Drawable

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable,
    var isBlocked: Boolean
)
