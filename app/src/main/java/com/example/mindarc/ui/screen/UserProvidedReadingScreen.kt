package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.example.mindarc.data.repository.MindArcRepository
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.viewmodel.ReadingViewModel
import com.example.mindarc.viewmodel.ReadingViewModelFactory
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProvidedReadingScreen(navController: NavController) {
    var reflection by remember { mutableStateOf("") }
    val context = LocalContext.current
    val repository = MindArcRepository(context)
    val viewModel: ReadingViewModel = viewModel(factory = ReadingViewModelFactory(repository))

    // Timer state
    val totalTime = 60 // 1 minute in seconds
    var timeLeft by remember { mutableStateOf(totalTime) }
    var isTimerRunning by remember { mutableStateOf(false) }
    val isTimerFinished by remember { derivedStateOf { timeLeft <= 0 } }

    LaunchedEffect(key1 = isTimerRunning, key2 = timeLeft) {
        if (isTimerRunning && timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Read Your Own Material") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Start the timer and read your own material. When you're done, write a short reflection on what you've learned.")
            Spacer(modifier = Modifier.height(24.dp))

            // Timer display
            Text(
                text = "${timeLeft / 60}:${(timeLeft % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { isTimerRunning = true },
                    enabled = !isTimerRunning && !isTimerFinished
                ) {
                    Text("Start")
                }
                Button(
                    onClick = {
                        isTimerRunning = false
                        timeLeft = totalTime
                    },
                    enabled = isTimerFinished || isTimerRunning
                ) {
                    Text("Reset")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = reflection,
                onValueChange = { reflection = it },
                label = { Text("Reflection") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled = isTimerFinished // Only enable when timer is done
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    val durationMinutes = totalTime / 60
                    viewModel.saveUserProvidedReading(durationMinutes, reflection)
                    navController.navigate(Screen.ActivitySelection.route) {
                        popUpTo(Screen.ActivitySelection.route) {
                            inclusive = true
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isTimerFinished && reflection.isNotBlank()
            ) {
                Text("Finish")
            }
        }
    }
}
