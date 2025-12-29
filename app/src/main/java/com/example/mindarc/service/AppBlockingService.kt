package com.example.mindarc.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.app.ActivityManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.repository.MindArcRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppBlockingService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var repository: MindArcRepository

    override fun onServiceConnected() {
        super.onServiceConnected()
        repository = MindArcRepository(this)
        
        // Start monitoring
        serviceScope.launch {
            monitorAndBlockApps()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Monitor app launches
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            event.packageName?.let { packageName ->
                serviceScope.launch {
                    checkAndBlockApp(packageName.toString())
                }
            }
        }
    }

    override fun onInterrupt() {
        // Service interrupted
    }

    private suspend fun checkAndBlockApp(packageName: String) {
        val blockedApps = repository.getBlockedApps().first()
        val isBlocked = blockedApps.any { it.packageName == packageName && it.isBlocked }
        
        if (isBlocked) {
            val activeSession = repository.getActiveSession()
            val isUnlocked = activeSession != null && 
                            activeSession.isActive && 
                            System.currentTimeMillis() < activeSession.endTime
            
            if (!isUnlocked) {
                // Block the app by bringing MindArc to foreground
                blockApp(packageName)
            }
        }
    }

    private fun blockApp(packageName: String) {
        try {
            val intent = Intent(this, com.example.mindarc.MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("blocked_package", packageName)
            }
            startActivity(intent)
            
            // Force stop the blocked app
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                forceStopApp(packageName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun forceStopApp(packageName: String) {
        try {
            val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.killBackgroundProcesses(packageName)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun monitorAndBlockApps() {
        while (true) {
            repository.checkAndDeactivateExpiredSessions()
            delay(5000) // Check every 5 seconds
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}

