package com.example.mindarc.domain

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.*

class ScreenTimeManager(private val context: Context) {

    /**
     * Returns total screen time in milliseconds for the current day.
     */
    fun getTodayTotalScreenTime(): Long {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()

        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, 
            startTime, 
            endTime
        )

        return stats?.sumOf { it.totalTimeInForeground } ?: 0L
    }

    /**
     * Formats milliseconds into "Hh Mm" string.
     */
    fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis / (1000 * 60)) % 60
        return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
    }
}
