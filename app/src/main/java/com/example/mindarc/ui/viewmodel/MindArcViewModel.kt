package com.example.mindarc.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.domain.SocialMediaScreenTime
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.domain.ScreenTimeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MindArcViewModel @Inject constructor(
    private val repository: MindArcRepository,
    private val screenTimeManager: ScreenTimeManager
) : ViewModel() {
    private val _restrictedApps = MutableStateFlow<List<RestrictedApp>>(emptyList())
    val restrictedApps: StateFlow<List<RestrictedApp>> = _restrictedApps.asStateFlow()

    private val _displayedApps = MutableStateFlow<List<RestrictedApp>>(emptyList())
    val displayedApps: StateFlow<List<RestrictedApp>> = _displayedApps.asStateFlow()

    private val _userProgress = MutableStateFlow<UserProgress?>(null)
    val userProgress: StateFlow<UserProgress?> = _userProgress.asStateFlow()

    private val _activeSession = MutableStateFlow<UnlockSession?>(null)
    val activeSession: StateFlow<UnlockSession?> = _activeSession.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _todayScreenTime = MutableStateFlow("0m")
    val todayScreenTime: StateFlow<String> = _todayScreenTime.asStateFlow()

    private val _commonDailyLimitMillis = MutableStateFlow(0L)
    val commonDailyLimitMillis: StateFlow<Long> = _commonDailyLimitMillis.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                repository.initializeDefaultData()
                updateScreenTime()
            } catch (e: Exception) {
                Log.e("MindArcViewModel", "Error initializing data or updating screen time", e)
            } finally {
                _isInitialized.value = true
            }
        }

        viewModelScope.launch {
            repository.getAllApps().collect { apps ->
                _restrictedApps.value = apps
                loadAndMergeApps()
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

        viewModelScope.launch {
            _commonDailyLimitMillis.value = repository.getCommonDailyLimitMillis()
        }
    }

    fun setCommonDailyLimitMillis(millis: Long) {
        viewModelScope.launch {
            repository.setCommonDailyLimitMillis(millis)
            _commonDailyLimitMillis.value = millis
        }
    }

    private var cachedInstalledApps: List<RestrictedApp>? = null
    private var installedAppsCacheTimeMs: Long = 0
    private val installedAppsCacheTtlMs = 30_000L

    private fun loadAndMergeApps() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val allInstalledApps = if (cachedInstalledApps != null && (now - installedAppsCacheTimeMs) < installedAppsCacheTtlMs) {
                cachedInstalledApps!!
            } else {
                withContext(Dispatchers.IO) { repository.getInstalledApps() }.also {
                    cachedInstalledApps = it
                    installedAppsCacheTimeMs = now
                }
            }
            val restrictedAppsFromDb = _restrictedApps.value
            val restrictedAppsMap = restrictedAppsFromDb.associateBy { it.packageName }
            val mergedList = allInstalledApps.map { installedApp ->
                restrictedAppsMap[installedApp.packageName]?.let { restrictedInfo ->
                    restrictedInfo.copy(usageTodayInMillis = installedApp.usageTodayInMillis)
                } ?: installedApp
            }
            _displayedApps.value = mergedList
        }
    }

    fun refreshDisplayedApps() {
        cachedInstalledApps = null
        loadAndMergeApps()
    }

    fun updateScreenTime() {
        viewModelScope.launch {
            try {
                val millis = withContext(Dispatchers.IO) {
                    screenTimeManager.getTodayTotalScreenTime()
                }
                _todayScreenTime.value = screenTimeManager.formatTime(millis)
            } catch (e: Exception) {
                _todayScreenTime.value = "Need Permission"
            }
        }
    }

    /** Today's screen time for social media apps (WhatsApp, Instagram, etc.) that are installed. */
    suspend fun getSocialMediaScreenTime(): SocialMediaScreenTime = withContext(Dispatchers.IO) {
        screenTimeManager.getTodaySocialMediaScreenTime()
    }

    suspend fun loadInstalledApps(): List<RestrictedApp> {
        val now = System.currentTimeMillis()
        return if (cachedInstalledApps != null && (now - installedAppsCacheTimeMs) < installedAppsCacheTtlMs) {
            cachedInstalledApps!!
        } else {
            repository.getInstalledApps().also {
                cachedInstalledApps = it
                installedAppsCacheTimeMs = now
            }
        }
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

    fun updateDailyLimit(packageName: String, limitInMillis: Long) {
        viewModelScope.launch {
            val app = _restrictedApps.value.find { it.packageName == packageName }
            app?.let {
                repository.updateApp(it.copy(dailyLimitInMillis = limitInMillis))
            }
        }
    }

    fun spendPointsToUnlock(points: Int, durationMinutes: Int = 15) {
        viewModelScope.launch {
            val progress = repository.getProgressSync() ?: return@launch
            if (progress.totalPoints >= points) {
                val updatedProgress = progress.copy(
                    totalPoints = progress.totalPoints - points
                )
                repository.updateProgress(updatedProgress)

                val activityId = repository.insertActivity(
                    ActivityRecord(
                        activityType = ActivityType.PUSHUPS,
                        pointsEarned = -points,
                        unlockDurationMinutes = durationMinutes
                    )
                )

                val session = repository.createUnlockSession(activityId, durationMinutes)
                _activeSession.value = session
                repository.updateProgressAfterUnlock()
            }
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

    suspend fun completeSquatsActivity(squats: Int): Long {
        val points = repository.calculatePoints(squats)
        val unlockDuration = repository.calculateUnlockDuration(squats)

        val activity = ActivityRecord(
            activityType = ActivityType.SQUATS,
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
        repository.updateProgressAfterActivity(activity)

        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()

        return activityId
    }

    suspend fun completeSpeedDialActivity(callDurationMinutes: Int): Long {
        // Fixed reward: 10 points + 10 minutes unlock for a 5+ minute call
        val points = 10
        val unlockDuration = 10

        val activity = ActivityRecord(
            activityType = ActivityType.SPEED_DIAL,
            pointsEarned = points,
            unlockDurationMinutes = unlockDuration
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)

        val session = repository.createUnlockSession(activityId, unlockDuration)
        _activeSession.value = session

        // Award the "Human Connection" badge
        repository.awardBadge(com.example.mindarc.data.model.Badge.HUMAN_CONNECTION)

        repository.updateProgressAfterUnlock()

        return activityId
    }

    suspend fun completePongActivity(points: Int, unlockMinutes: Int): Long {
        val activity = ActivityRecord(
            activityType = ActivityType.PONG_GAME,
            pointsEarned = points,
            unlockDurationMinutes = unlockMinutes
        )

        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)

        val session = repository.createUnlockSession(activityId, unlockMinutes)
        _activeSession.value = session
        repository.updateProgressAfterUnlock()

        return activityId
    }

    /** Trace-to-Earn: unlockMinutes = 5 (excellent), 1 (average), or 0 (needs improvement). */
    suspend fun completeTraceToEarnActivity(unlockMinutes: Int): Long {
        val points = when (unlockMinutes) {
            5 -> 5
            1 -> 1
            else -> 0
        }
        val activity = ActivityRecord(
            activityType = ActivityType.TRACE_TO_EARN,
            pointsEarned = points,
            unlockDurationMinutes = unlockMinutes
        )
        val activityId = repository.insertActivity(activity)
        repository.updateProgressAfterActivity(points)
        if (unlockMinutes > 0) {
            val session = repository.createUnlockSession(activityId, unlockMinutes)
            _activeSession.value = session
            repository.updateProgressAfterUnlock()
        }
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
               System.currentTimeMillis() < session.endTime
    }
}
