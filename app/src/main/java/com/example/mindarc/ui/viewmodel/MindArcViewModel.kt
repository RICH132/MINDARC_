package com.example.mindarc.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress
import com.example.mindarc.data.repository.MindArcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MindArcViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MindArcRepository(application)

    private val _restrictedApps = MutableStateFlow<List<RestrictedApp>>(emptyList())
    val restrictedApps: StateFlow<List<RestrictedApp>> = _restrictedApps.asStateFlow()

    private val _userProgress = MutableStateFlow<UserProgress?>(null)
    val userProgress: StateFlow<UserProgress?> = _userProgress.asStateFlow()

    private val _activeSession = MutableStateFlow<UnlockSession?>(null)
    val activeSession: StateFlow<UnlockSession?> = _activeSession.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeDefaultData()
            _isInitialized.value = true
        }
        
        viewModelScope.launch {
            repository.getAllApps().collect { apps ->
                _restrictedApps.value = apps
            }
        }

        viewModelScope.launch {
            repository.getProgress().collect { progress ->
                _userProgress.value = progress
            }
        }

        viewModelScope.launch {
            checkActiveSession()
        }
    }

    suspend fun loadInstalledApps(): List<RestrictedApp> {
        return repository.getInstalledApps()
    }

    fun addRestrictedApp(app: RestrictedApp) {
        viewModelScope.launch {
            repository.insertApp(app.copy(isBlocked = true))
        }
    }

    fun removeRestrictedApp(packageName: String) {
        viewModelScope.launch {
            repository.deleteApp(packageName)
        }
    }

    fun toggleAppBlocked(app: RestrictedApp) {
        viewModelScope.launch {
            repository.updateApp(app.copy(isBlocked = !app.isBlocked))
        }
    }

    suspend fun completePushupsActivity(pushups: Int): Long {
        val points = repository.calculatePoints(pushups)
        val unlockDuration = repository.calculateUnlockDuration(pushups)
        
        val activity = ActivityRecord(
            activityType = ActivityType.PUSHUPS,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )
        
        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        
        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()
        
        return activityId
    }

    suspend fun completeReadingActivity(
        activityType: ActivityType,
        readingMinutes: Int,
        readingContentId: Long? = null,
        userReadingTitle: String? = null
    ): Long {
        val points = repository.calculateReadingPoints(readingMinutes)
        val unlockDuration = repository.calculateReadingUnlockDuration(readingMinutes)
        
        val activity = ActivityRecord(
            activityType = activityType,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration,
            readingContentId = readingContentId,
            userReadingTitle = userReadingTitle
        )
        
        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        
        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()
        
        return activityId
    }

    suspend fun checkActiveSession() {
        repository.checkAndDeactivateExpiredSessions()
        _activeSession.value = repository.getActiveSession()
    }

    fun isAppUnlocked(packageName: String): Boolean {
        val session = _activeSession.value
        return session != null && 
               session.isActive && 
               System.currentTimeMillis() < session.endTime &&
               _restrictedApps.value.any { it.packageName == packageName && it.isBlocked }
    }
}

