package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: MindArcViewModel = viewModel()
) {
    val userProgress by viewModel.userProgress.collectAsState()
    val activeSession by viewModel.activeSession.collectAsState()
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val isInitialized by viewModel.isInitialized.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.checkActiveSession()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MindArc") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (!isInitialized) {
                CircularProgressIndicator()
                return@Column
            }

            // Tagline
            Text(
                text = "Screen time through effort",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Active Session Card
            activeSession?.let { session ->
                val currentTime = System.currentTimeMillis()
                val remainingTime = (session.endTime - currentTime) / 1000 / 60 // minutes
                
                if (remainingTime > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Apps Unlocked",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "$remainingTime minutes remaining",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Points",
                    value = "${userProgress?.totalPoints ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Streak",
                    value = "${userProgress?.currentStreak ?: 0} days",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    title = "Activities",
                    value = "${userProgress?.totalActivities ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    title = "Blocked Apps",
                    value = "${restrictedApps.count { it.isBlocked }}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action Buttons
            Button(
                onClick = { navController.navigate(Screen.ActivitySelection.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Start Activity", fontSize = 18.sp)
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.AppSelection.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Manage Apps", fontSize = 18.sp)
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.Progress.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Progress", fontSize = 18.sp)
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

