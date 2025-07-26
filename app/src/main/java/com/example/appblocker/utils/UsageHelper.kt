package com.example.appblocker.utils

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*
import kotlin.math.roundToInt

object UsageHelper {

    fun getUsedMinutesToday(context: Context, packageName: String): Int {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val statsList = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, startTime, endTime
        )

        val totalTimeInForeground = statsList
            ?.find { it.packageName == packageName }
            ?.totalTimeInForeground ?: 0L

        return (totalTimeInForeground / 60000.0).roundToInt()
    }
}
