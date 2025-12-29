package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.ui.viewmodel.ReadingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingActivityScreen(
    navController: NavController,
    viewModel: MindArcViewModel = viewModel(),
    readingViewModel: ReadingViewModel = viewModel()
) {
    val readingMode by readingViewModel.readingMode.collectAsState()
    val currentContent by readingViewModel.currentContent.collectAsState()
    val quizQuestions by readingViewModel.quizQuestions.collectAsState()
    val currentQuestionIndex by readingViewModel.currentQuestionIndex.collectAsState()
    val userAnswers by readingViewModel.userAnswers.collectAsState()
    val quizCompleted by readingViewModel.quizCompleted.collectAsState()
    val userReadingTitle by readingViewModel.userReadingTitle.collectAsState()
    val userReadingReflection by readingViewModel.userReadingReflection.collectAsState()

    var readingTimeMinutes by remember { mutableStateOf(5) }
    var isReadingStarted by remember { mutableStateOf(false) }
    var readingStartTime by remember { mutableStateOf(0L) }
    var isActivityCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reading Activity") },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (readingMode == ReadingViewModel.ReadingMode.SELECTION) {
                            navController.popBackStack()
                        } else {
                            readingViewModel.reset()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (readingMode) {
            ReadingViewModel.ReadingMode.SELECTION -> {
                ReadingModeSelectionScreen(
                    onSelectAppProvided = {
                        readingViewModel.setReadingMode(ReadingViewModel.ReadingMode.APP_PROVIDED)
                    },
                    onSelectUserProvided = {
                        readingViewModel.setReadingMode(ReadingViewModel.ReadingMode.USER_PROVIDED)
                    },
                    modifier = Modifier.padding(padding)
                )
            }
            ReadingViewModel.ReadingMode.APP_PROVIDED -> {
                if (isActivityCompleted) {
                    CompletionScreen(
                        message = "Reading activity completed! Your apps are now unlocked.",
                        onNavigateHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                } else if (quizCompleted) {
                    QuizScreen(
                        questions = quizQuestions,
                        currentQuestionIndex = currentQuestionIndex,
                        userAnswers = userAnswers,
                        onAnswerSelected = { index, answer ->
                            readingViewModel.selectAnswer(index, answer)
                        },
                        onNext = {
                            if (currentQuestionIndex < quizQuestions.size - 1) {
                                readingViewModel.nextQuestion()
                            } else {
                                val allCorrect = readingViewModel.checkQuizAnswers()
                                if (allCorrect) {
                                    scope.launch {
                                        currentContent?.let { content ->
                                            val minutes = content.estimatedReadingTimeMinutes
                                            viewModel.completeReadingActivity(
                                                ActivityType.READING_APP_PROVIDED,
                                                minutes,
                                                content.id
                                            )
                                            isActivityCompleted = true
                                        }
                                    }
                                } else {
                                    // Show error and reset
                                    readingViewModel.reset()
                                }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                } else if (isReadingStarted) {
                    AppProvidedReadingScreen(
                        content = currentContent,
                        onComplete = {
                            readingViewModel.nextQuestion()
                        },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    currentContent?.let { content ->
                        AppProvidedReadingStartScreen(
                            content = content,
                            onStart = {
                                isReadingStarted = true
                                readingStartTime = System.currentTimeMillis()
                            },
                            modifier = Modifier.padding(padding)
                        )
                    } ?: run {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(padding),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
            }
            ReadingViewModel.ReadingMode.USER_PROVIDED -> {
                if (isActivityCompleted) {
                    CompletionScreen(
                        message = "Reading activity completed! Your apps are now unlocked.",
                        onNavigateHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                } else {
                    UserProvidedReadingScreen(
                        title = userReadingTitle,
                        onTitleChange = { readingViewModel.setUserReadingTitle(it) },
                        reflection = userReadingReflection,
                        onReflectionChange = { readingViewModel.setUserReadingReflection(it) },
                        readingTime = readingTimeMinutes,
                        onReadingTimeChange = { readingTimeMinutes = it },
                        onSubmit = {
                            if (readingViewModel.validateUserReading()) {
                                scope.launch {
                                    viewModel.completeReadingActivity(
                                        ActivityType.READING_USER_PROVIDED,
                                        readingTimeMinutes,
                                        userReadingTitle = userReadingTitle
                                    )
                                    isActivityCompleted = true
                                }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }
}

@Composable
fun ReadingModeSelectionScreen(
    onSelectAppProvided: () -> Unit,
    onSelectUserProvided: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Choose Reading Option",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            onClick = onSelectAppProvided,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "App-Provided Reading",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Read a short story or article provided by the app",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            onClick = onSelectUserProvided,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Your Own Reading",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Read your own book, PDF, or document",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun AppProvidedReadingStartScreen(
    content: com.example.mindarc.data.model.ReadingContent,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Estimated reading time: ${content.estimatedReadingTimeMinutes} minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = content.content,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("I've Finished Reading", fontSize = 18.sp)
        }
    }
}

@Composable
fun AppProvidedReadingScreen(
    content: com.example.mindarc.data.model.ReadingContent?,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // This screen would show the reading content
    // For now, we'll just show a completion button
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Continue to Quiz")
        }
    }
}

@Composable
fun QuizScreen(
    questions: List<com.example.mindarc.data.model.QuizQuestion>,
    currentQuestionIndex: Int,
    userAnswers: Map<Int, String>,
    onAnswerSelected: (Int, String) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (questions.isEmpty() || currentQuestionIndex >= questions.size) {
        return
    }

    val question = questions[currentQuestionIndex]
    val selectedAnswer = userAnswers[currentQuestionIndex]

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Question ${currentQuestionIndex + 1} of ${questions.size}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Text(
                text = question.question,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val options = listOf(
            question.option1,
            question.option2,
            question.option3,
            question.option4
        )

        options.forEach { option ->
            val isSelected = selectedAnswer == option
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onAnswerSelected(currentQuestionIndex, option) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
                Text(
                    text = option,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedAnswer != null,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                if (currentQuestionIndex < questions.size - 1) "Next" else "Complete",
                fontSize = 18.sp
            )
        }
    }
}

@Composable
fun UserProvidedReadingScreen(
    title: String,
    onTitleChange: (String) -> Unit,
    reflection: String,
    onReflectionChange: (String) -> Unit,
    readingTime: Int,
    onReadingTimeChange: (Int) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Your Reading Activity",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            label = { Text("What are you reading? (Book/Article Title)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = readingTime.toString(),
            onValueChange = { 
                it.toIntOrNull()?.let { time -> 
                    if (time > 0) onReadingTimeChange(time) 
                }
            },
            label = { Text("Reading Time (minutes)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        OutlinedTextField(
            value = reflection,
            onValueChange = onReflectionChange,
            label = { Text("Reflection (What did you learn? At least 20 characters)") },
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            minLines = 5,
            maxLines = 10
        )

        Text(
            text = "Points: ${readingTime * 2} | Unlock Time: $readingTime minutes",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = title.isNotBlank() && reflection.length >= 20 && readingTime > 0,
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Complete Activity", fontSize = 18.sp)
        }
    }
}

@Composable
fun CompletionScreen(
    message: String,
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Activity Completed!",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onNavigateHome,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Go to Home")
                }
            }
        }
    }
}

