// File: app/src/main/java/com/example/appblocker/model/UsageStatItem.kt
package com.example.appblocker.model

data class UsageStatItem(
    val packageName: String,
    val appName: String,
    val minutesUsed: Long
)
