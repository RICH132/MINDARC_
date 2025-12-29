package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PushupsActivityScreen(
    navController: NavController,
    viewModel: MindArcViewModel = viewModel()
) {
    var pushupCount by remember { mutableStateOf(0) }
    var isCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val unlockDuration = remember(pushupCount) {
        if (pushupCount > 0) (pushupCount * 15) / 10 else 0
    }
    val points = remember(pushupCount) { pushupCount }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pushups Activity") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            if (isCompleted) {
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
                            text = "Your apps are now unlocked",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { navController.navigate(Screen.Home.route) { 
                                popUpTo(Screen.Home.route) { inclusive = true }
                            } },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Go to Home")
                        }
                    }
                }
            } else {
                Text(
                    text = "Complete pushups to unlock your apps",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Counter Display
                Card(
                    modifier = Modifier
                        .size(200.dp),
                    shape = RoundedCornerShape(100.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$pushupCount",
                            style = MaterialTheme.typography.displayLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = "Pushups",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Counter Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (pushupCount > 0) pushupCount-- },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("-1", fontSize = 20.sp)
                    }
                    OutlinedButton(
                        onClick = { pushupCount++ },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+1", fontSize = 20.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (pushupCount >= 5) pushupCount -= 5 },
                        modifier = Modifier.weight(1f),
                        enabled = pushupCount >= 5
                    ) {
                        Text("-5", fontSize = 20.sp)
                    }
                    OutlinedButton(
                        onClick = { pushupCount += 5 },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("+5", fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Rewards Preview
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "You'll earn:",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$points",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Points",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "$unlockDuration",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Minutes",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Complete Button
                Button(
                    onClick = {
                        if (pushupCount > 0) {
                            scope.launch {
                                viewModel.completePushupsActivity(pushupCount)
                                isCompleted = true
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = pushupCount > 0,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Complete Activity", fontSize = 18.sp)
                }
            }
        }
    }
}

