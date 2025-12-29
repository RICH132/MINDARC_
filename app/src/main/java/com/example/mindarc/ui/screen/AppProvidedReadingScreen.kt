package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.QuizQuestion
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.viewmodel.ReadingViewModel
import com.example.mindarc.viewmodel.ReadingViewModelFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProvidedReadingScreen(navController: NavController) {
    val context = LocalContext.current
    val repository = MindArcRepository(context)
    val viewModel: ReadingViewModel = viewModel(factory = ReadingViewModelFactory(repository))
    val readingContent by viewModel.readingContent.collectAsState()
    val quizQuestions by viewModel.quizQuestions.collectAsState()

    val totalTimeInSeconds = (readingContent?.estimatedReadingTimeMinutes ?: 0) * 60
    var timeLeft by remember { mutableStateOf(totalTimeInSeconds) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val isTimerFinished by remember { derivedStateOf { timeLeft <= 0 } }

    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswer by remember { mutableStateOf<String?>(null) }
    var score by remember { mutableStateOf(0) }
    val isQuizFinished by remember { derivedStateOf { currentQuestionIndex >= quizQuestions.size } }

    LaunchedEffect(key1 = isTimerRunning, key2 = timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    LaunchedEffect(totalTimeInSeconds) {
        timeLeft = totalTimeInSeconds
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(readingContent?.title ?: "Read an Article") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (readingContent != null) {
                if (!isTimerRunning && !isTimerFinished) {
                    Text("You have ${readingContent!!.estimatedReadingTimeMinutes} minutes to read the article. Start the timer when you are ready.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { isTimerRunning = true }) {
                        Text("Start Reading")
                    }
                } else if (isTimerRunning && !isTimerFinished) {
                    Text(
                        text = "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                        style = MaterialTheme.typography.headlineLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = readingContent!!.content, modifier = Modifier.verticalScroll(rememberScrollState()))
                } else if (!isQuizFinished) {
                    QuizQuestion(quizQuestions[currentQuestionIndex], selectedAnswer) { selectedAnswer = it }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedAnswer == quizQuestions[currentQuestionIndex].correctAnswer) {
                                score += 10
                            }
                            selectedAnswer = null
                            currentQuestionIndex++
                        },
                        enabled = selectedAnswer != null
                    ) {
                        Text(if (currentQuestionIndex < quizQuestions.size - 1) "Next Question" else "Finish Quiz")
                    }
                } else {
                    Text("Quiz Finished!", style = MaterialTheme.typography.headlineMedium)
                    Text("You scored $score points!", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            viewModel.saveAppProvidedReading(totalTimeInSeconds / 60, score)
                            navController.navigate(Screen.ActivitySelection.route) {
                                popUpTo(Screen.ActivitySelection.route) {
                                    inclusive = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Continue")
                    }
                }
            } else {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun QuizQuestion(question: QuizQuestion, selectedOption: String?, onOptionSelected: (String) -> Unit) {
    val options = listOf(question.option1, question.option2, question.option3, question.option4).shuffled()

    Column {
        Text(question.question, style = MaterialTheme.typography.bodyLarge)
        Spacer(modifier = Modifier.height(16.dp))
        options.forEach { option ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = (option == selectedOption)) { onOptionSelected(option) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (option == selectedOption),
                    onClick = { onOptionSelected(option) }
                )
                Text(text = option, modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}
