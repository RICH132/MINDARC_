package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(
    navController: NavController,
    viewModel: MindArcViewModel = viewModel()
) {
    val userProgress by viewModel.userProgress.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your Progress") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Your Statistics",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Total Points",
                        value = "${userProgress?.totalPoints ?: 0}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Current Streak",
                        value = "${userProgress?.currentStreak ?: 0} days",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        title = "Longest Streak",
                        value = "${userProgress?.longestStreak ?: 0} days",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Total Activities",
                        value = "${userProgress?.totalActivities ?: 0}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                StatCard(
                    title = "Unlock Sessions",
                    value = "${userProgress?.totalUnlockSessions ?: 0}",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Achievements",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            userProgress?.let { progress ->
                item {
                    AchievementCard(
                        title = "First Steps",
                        description = "Complete your first activity",
                        achieved = progress.totalActivities > 0
                    )
                }

                item {
                    AchievementCard(
                        title = "Week Warrior",
                        description = "Maintain a 7-day streak",
                        achieved = progress.longestStreak >= 7
                    )
                }

                item {
                    AchievementCard(
                        title = "Month Master",
                        description = "Maintain a 30-day streak",
                        achieved = progress.longestStreak >= 30
                    )
                }

                item {
                    AchievementCard(
                        title = "Point Collector",
                        description = "Earn 100 points",
                        achieved = progress.totalPoints >= 100
                    )
                }

                item {
                    AchievementCard(
                        title = "Century Club",
                        description = "Earn 1000 points",
                        achieved = progress.totalPoints >= 1000
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementCard(
    title: String,
    description: String,
    achieved: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (achieved) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (achieved) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

