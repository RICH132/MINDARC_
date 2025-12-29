package com.example.mindarc.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.data.model.ReadingContent
import com.example.mindarc.data.model.ReflectionQuestion
import com.example.mindarc.data.repository.MindArcRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReadingViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MindArcRepository(application)

    private val _currentContent = MutableStateFlow<ReadingContent?>(null)
    val currentContent: StateFlow<ReadingContent?> = _currentContent.asStateFlow()

    private val _quizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val quizQuestions: StateFlow<List<QuizQuestion>> = _quizQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _userAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, String>> = _userAnswers.asStateFlow()

    private val _quizCompleted = MutableStateFlow(false)
    val quizCompleted: StateFlow<Boolean> = _quizCompleted.asStateFlow()

    private val _readingMode = MutableStateFlow<ReadingMode>(ReadingMode.SELECTION)
    val readingMode: StateFlow<ReadingMode> = _readingMode.asStateFlow()

    private val _userReadingTitle = MutableStateFlow("")
    val userReadingTitle: StateFlow<String> = _userReadingTitle.asStateFlow()

    private val _userReadingReflection = MutableStateFlow("")
    val userReadingReflection: StateFlow<String> = _userReadingReflection.asStateFlow()

    fun loadRandomContent() {
        viewModelScope.launch {
            _currentContent.value = repository.getRandomReadingContent()
            _currentContent.value?.let { content ->
                _quizQuestions.value = repository.getQuestionsForContent(content.id, 3)
            }
            _currentQuestionIndex.value = 0
            _userAnswers.value = emptyMap()
            _quizCompleted.value = false
        }
    }

    fun selectAnswer(questionIndex: Int, answer: String) {
        _userAnswers.value = _userAnswers.value.toMutableMap().apply {
            put(questionIndex, answer)
        }
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _quizQuestions.value.size - 1) {
            _currentQuestionIndex.value = _currentQuestionIndex.value + 1
        } else {
            _quizCompleted.value = true
        }
    }

    fun checkQuizAnswers(): Boolean {
        val questions = _quizQuestions.value
        val answers = _userAnswers.value
        
        if (answers.size != questions.size) return false
        
        return questions.allIndexed { index, question ->
            answers[index] == question.correctAnswer
        }
    }

    fun setReadingMode(mode: ReadingMode) {
        _readingMode.value = mode
        if (mode == ReadingMode.APP_PROVIDED) {
            loadRandomContent()
        }
    }

    fun setUserReadingTitle(title: String) {
        _userReadingTitle.value = title
    }

    fun setUserReadingReflection(reflection: String) {
        _userReadingReflection.value = reflection
    }

    fun reset() {
        _currentContent.value = null
        _quizQuestions.value = emptyList()
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _quizCompleted.value = false
        _readingMode.value = ReadingMode.SELECTION
        _userReadingTitle.value = ""
        _userReadingReflection.value = ""
    }

    fun validateUserReading(): Boolean {
        // Basic validation - check if reflection has meaningful content
        val reflection = _userReadingReflection.value.trim()
        return reflection.length >= 20 // At least 20 characters
    }

    enum class ReadingMode {
        SELECTION,
        APP_PROVIDED,
        USER_PROVIDED
    }
}

// Helper extension function
private fun <T> List<T>.allIndexed(predicate: (Int, T) -> Boolean): Boolean {
    forEachIndexed { index, element ->
        if (!predicate(index, element)) return false
    }
    return true
}

