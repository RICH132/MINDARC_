package com.example.mindarc.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.room.Room
import com.example.mindarc.data.dao.ActivityRecordDao
import com.example.mindarc.data.dao.QuizQuestionDao
import com.example.mindarc.data.dao.ReadingContentDao
import com.example.mindarc.data.dao.ReadingReflectionDao
import com.example.mindarc.data.dao.RestrictedAppDao
import com.example.mindarc.data.dao.UnlockSessionDao
import com.example.mindarc.data.dao.UserProgressDao
import com.example.mindarc.data.database.MindArcDatabase
import com.example.mindarc.data.model.ActivityRecord
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.data.model.ReadingContent
import com.example.mindarc.data.model.ReadingReflection
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.data.model.UnlockSession
import com.example.mindarc.data.model.UserProgress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar

class MindArcRepository(context: Context) {
    private val database: MindArcDatabase = Room.databaseBuilder(
        context,
        MindArcDatabase::class.java,
        "mindarc_database"
    ).fallbackToDestructiveMigration().build()

    private val restrictedAppDao: RestrictedAppDao = database.restrictedAppDao()
    private val activityRecordDao: ActivityRecordDao = database.activityRecordDao()
    private val unlockSessionDao: UnlockSessionDao = database.unlockSessionDao()
    private val userProgressDao: UserProgressDao = database.userProgressDao()
    private val readingContentDao: ReadingContentDao = database.readingContentDao()
    private val quizQuestionDao: QuizQuestionDao = database.quizQuestionDao()
    private val readingReflectionDao: ReadingReflectionDao = database.readingReflectionDao()
    private val packageManager: PackageManager = context.packageManager

    // Restricted Apps
    fun getAllApps(): Flow<List<RestrictedApp>> = restrictedAppDao.getAllApps()
    fun getBlockedApps(): Flow<List<RestrictedApp>> = restrictedAppDao.getBlockedApps()
    suspend fun insertApp(app: RestrictedApp) = restrictedAppDao.insertApp(app)
    suspend fun updateApp(app: RestrictedApp) = restrictedAppDao.updateApp(app)
    suspend fun deleteApp(packageName: String) = restrictedAppDao.deleteAppByPackageName(packageName)

    suspend fun getInstalledApps(): List<RestrictedApp> {
        val installedApps = packageManager.getInstalledPackages(0)
        return installedApps
            .mapNotNull { packageInfo ->
                packageInfo.applicationInfo
                    ?.takeIf { (it.flags and ApplicationInfo.FLAG_SYSTEM) == 0 }
                    ?.let { appInfo ->
                        val appName = packageManager.getApplicationLabel(appInfo).toString()
                        RestrictedApp(
                            packageName = packageInfo.packageName,
                            appName = appName,
                            isBlocked = false
                        )
                    }
            }
    }

    // Activities
    fun getAllActivities(): Flow<List<ActivityRecord>> = activityRecordDao.getAllActivities()
    suspend fun insertActivity(activity: ActivityRecord): Long = activityRecordDao.insertActivity(activity)

    suspend fun calculateUnlockDuration(pushups: Int): Int {
        // 10 pushups = 15 minutes, scales linearly
        return (pushups * 15) / 10
    }

    suspend fun calculatePoints(pushups: Int): Int {
        // 1 pushup = 1 point
        return pushups
    }

    suspend fun calculateReadingUnlockDuration(minutes: Int): Int {
        // Reading time = unlock time (1:1 ratio)
        return minutes
    }

    suspend fun calculateReadingPoints(minutes: Int): Int {
        // 1 minute of reading = 2 points
        return minutes * 2
    }

    // Reading Reflections
    suspend fun insertReflection(reflection: ReadingReflection) {
        readingReflectionDao.insertReflection(reflection)
    }

    // Unlock Sessions
    suspend fun getActiveSession(): UnlockSession? = unlockSessionDao.getActiveSession()
    fun getAllSessions(): Flow<List<UnlockSession>> = unlockSessionDao.getAllSessions()
    
    suspend fun createUnlockSession(activityRecordId: Long, durationMinutes: Int): UnlockSession {
        val startTime = System.currentTimeMillis()
        val endTime = startTime + (durationMinutes * 60 * 1000L)
        
        // Deactivate any existing sessions
        unlockSessionDao.deactivateAllSessions()
        
        val session = UnlockSession(
            activityRecordId = activityRecordId,
            startTime = startTime,
            endTime = endTime,
            isActive = true
        )
        unlockSessionDao.insertSession(session)
        return session
    }

    suspend fun checkAndDeactivateExpiredSessions() {
        val activeSession = unlockSessionDao.getActiveSession()
        activeSession?.let {
            if (System.currentTimeMillis() >= it.endTime) {
                unlockSessionDao.deactivateAllSessions()
            }
        }
    }

    // User Progress
    fun getProgress(): Flow<UserProgress?> = userProgressDao.getProgress()
    suspend fun getProgressSync(): UserProgress? = userProgressDao.getProgressSync()

    suspend fun updateProgressAfterActivity(points: Int) {
        val progress = userProgressDao.getProgressSync() ?: UserProgress()
        val calendar = Calendar.getInstance()
        val today = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val todayStart = calendar.timeInMillis

        val lastActivityDate = progress.lastActivityDate
        val newStreak = if (lastActivityDate != null) {
            val lastDate = Calendar.getInstance().apply { timeInMillis = lastActivityDate }
            val daysDiff = (today - lastActivityDate) / (1000 * 60 * 60 * 24)
            
            when {
                daysDiff == 0L -> progress.currentStreak // Same day
                daysDiff == 1L -> progress.currentStreak + 1 // Consecutive day
                else -> 1 // Reset streak
            }
        } else {
            1 // First activity
        }

        val updatedProgress = progress.copy(
            totalPoints = progress.totalPoints + points,
            currentStreak = newStreak,
            longestStreak = maxOf(progress.longestStreak, newStreak),
            lastActivityDate = today,
            totalActivities = progress.totalActivities + 1
        )
        userProgressDao.updateProgress(updatedProgress)
    }

    suspend fun updateProgressAfterUnlock() {
        val progress = userProgressDao.getProgressSync() ?: UserProgress()
        val updatedProgress = progress.copy(
            totalUnlockSessions = progress.totalUnlockSessions + 1
        )
        userProgressDao.updateProgress(updatedProgress)
    }

    // Reading Content
    suspend fun getRandomReadingContent(): ReadingContent? = readingContentDao.getRandomContent()
    suspend fun getReadingContentById(id: Long): ReadingContent? = readingContentDao.getContentById(id)
    suspend fun insertReadingContent(content: ReadingContent): Long = readingContentDao.insertContent(content)

    // Quiz Questions
    suspend fun getQuestionsForContent(contentId: Long, limit: Int = 3): List<QuizQuestion> {
        return quizQuestionDao.getRandomQuestionsForContent(contentId, limit)
    }

    suspend fun insertQuizQuestions(questions: List<QuizQuestion>) {
        quizQuestionDao.insertQuestions(questions)
    }

    // Initialize default data
    suspend fun initializeDefaultData() {
        // Initialize user progress if not exists
        val progress = userProgressDao.getProgressSync()
        if (progress == null) {
            userProgressDao.insertProgress(UserProgress())
        }

        // Initialize default reading content if empty
        val contentCount = readingContentDao.getAllContent().first().size
        if (contentCount == 0) {
            initializeDefaultReadingContent()
        }
    }

    private suspend fun initializeDefaultReadingContent() {
        val contents = listOf(
            ReadingContent(
                title = "The Power of Small Habits",
                content = """
                    Small habits can have a profound impact on our lives. When we commit to doing something small every day, 
                    we create a compound effect that leads to significant change over time. 
                    
                    The key is consistency. It's not about doing something perfectly or for a long time. 
                    It's about showing up every day, even if it's just for a few minutes.
                    
                    Research shows that it takes an average of 66 days to form a new habit. 
                    But the journey is more important than the destination. Each day you practice, 
                    you're strengthening the neural pathways that make the habit easier to maintain.
                    
                    Start small. If you want to read more, start with just 5 minutes a day. 
                    If you want to exercise, start with just 10 pushups. The size doesn't matter 
                    as much as the consistency.
                    
                    Remember, every expert was once a beginner. Every pro was once an amateur. 
                    The difference is they kept going when others gave up.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 3,
                category = "Self-Improvement"
            ),
            ReadingContent(
                title = "Digital Wellness in the Modern Age",
                content = """
                    In today's hyperconnected world, managing our relationship with technology 
                    has become more important than ever. Digital wellness isn't about completely 
                    avoiding technology, but about using it mindfully and intentionally.
                    
                    The average person checks their phone over 150 times a day. Many of these 
                    checks are automatic, driven by habit rather than necessity. This constant 
                    connectivity can lead to increased stress, decreased focus, and reduced 
                    quality of sleep.
                    
                    The solution isn't to eliminate technology, but to be more intentional about 
                    when and how we use it. Setting boundaries, taking regular breaks, and 
                    being present in the moment are all crucial skills for digital wellness.
                    
                    One effective strategy is to link screen time to productive activities. 
                    By earning your screen time through exercise or reading, you create a 
                    positive feedback loop that encourages healthy habits.
                    
                    Remember, technology is a tool. Like any tool, it's most effective when 
                    used with intention and purpose, not as a default escape from boredom.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 4,
                category = "Wellness"
            ),
            ReadingContent(
                title = "The Science of Focus",
                content = """
                    Focus is not just about willpower—it's a skill that can be developed and 
                    strengthened over time. Understanding how focus works can help us improve 
                    our ability to concentrate on what matters.
                    
                    The brain has two main modes: focused mode and diffuse mode. Focused mode 
                    is when we're actively concentrating on a task. Diffuse mode is when our 
                    mind wanders and makes connections between different ideas.
                    
                    Both modes are important. Focused mode helps us learn and solve problems, 
                    while diffuse mode helps us see the bigger picture and make creative connections.
                    
                    However, constant distractions can prevent us from entering either mode 
                    effectively. When we're constantly switching between tasks or checking 
                    notifications, we never give our brain a chance to fully engage.
                    
                    To improve focus, we need to create an environment that minimizes distractions. 
                    This might mean turning off notifications, setting specific times for 
                    focused work, or using techniques like the Pomodoro method.
                    
                    Remember, focus is like a muscle. The more you practice, the stronger it gets.
                """.trimIndent(),
                estimatedReadingTimeMinutes = 5,
                category = "Productivity"
            )
        )

        contents.forEach { content ->
            val contentId = readingContentDao.insertContent(content)
            
            // Add quiz questions for each content
            when (content.title) {
                "The Power of Small Habits" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "How long does it take on average to form a new habit?",
                            correctAnswer = "66 days",
                            option1 = "21 days",
                            option2 = "66 days",
                            option3 = "90 days",
                            option4 = "100 days"
                        ),
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What matters more than the size of a habit?",
                            correctAnswer = "Consistency",
                            option1 = "Perfection",
                            option2 = "Duration",
                            option3 = "Consistency",
                            option4 = "Intensity"
                        ),
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What is the key to building habits according to the text?",
                            correctAnswer = "Showing up every day",
                            option1 = "Doing it perfectly",
                            option2 = "Showing up every day",
                            option3 = "Doing it for long periods",
                            option4 = "Having the right tools"
                        )
                    ))
                }
                "Digital Wellness in the Modern Age" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "How many times does the average person check their phone per day?",
                            correctAnswer = "Over 150 times",
                            option1 = "50 times",
                            option2 = "100 times",
                            option3 = "Over 150 times",
                            option4 = "200 times"
                        ),
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What is the recommended approach to technology use?",
                            correctAnswer = "Using it mindfully and intentionally",
                            option1 = "Avoiding it completely",
                            option2 = "Using it mindfully and intentionally",
                            option3 = "Using it as much as possible",
                            option4 = "Only using it for work"
                        ),
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What is technology according to the text?",
                            correctAnswer = "A tool",
                            option1 = "A necessity",
                            option2 = "A distraction",
                            option3 = "A tool",
                            option4 = "An addiction"
                        )
                    ))
                }
                "The Science of Focus" -> {
                    quizQuestionDao.insertQuestions(listOf(
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What are the two main modes of the brain mentioned?",
                            correctAnswer = "Focused mode and diffuse mode",
                            option1 = "Active mode and passive mode",
                            option2 = "Focused mode and diffuse mode",
                            option3 = "Conscious mode and subconscious mode",
                            option4 = "Work mode and rest mode"
                        ),
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What is focus compared to in the text?",
                            correctAnswer = "A muscle",
                            option1 = "A skill",
                            option2 = "A muscle",
                            option3 = "A habit",
                            option4 = "A tool"
                        ),
                        QuizQuestion(
                            readingContentId = contentId,
                            question = "What prevents us from entering focused or diffuse mode effectively?",
                            correctAnswer = "Constant distractions",
                            option1 = "Lack of willpower",
                            option2 = "Constant distractions",
                            option3 = "Too much work",
                            option4 = "Lack of sleep"
                        )
                    ))
                }
            }
        }
    }
}
