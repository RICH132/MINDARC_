package com.example.mindarc.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.RestrictedApp
import com.example.mindarc.ui.viewmodel.MindArcViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionScreen(
    navController: NavController,
    viewModel: MindArcViewModel = viewModel()
) {
    val restrictedApps by viewModel.restrictedApps.collectAsState()
    val installedApps = remember { mutableStateListOf<RestrictedApp>() }
    val isLoading = remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        isLoading.value = true
        installedApps.clear()
        installedApps.addAll(viewModel.loadInstalledApps())
        isLoading.value = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Select Apps to Block") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isLoading.value) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "Select apps you want to restrict. You'll need to complete activities to unlock them.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                items(installedApps) { app ->
                    val isBlocked = restrictedApps.any { 
                        it.packageName == app.packageName && it.isBlocked 
                    }

                    AppListItem(
                        app = app,
                        isBlocked = isBlocked,
                        onToggle = { isChecked ->
                            if (isChecked) {
                                viewModel.addRestrictedApp(app)
                            } else {
                                viewModel.removeRestrictedApp(app.packageName)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AppListItem(
    app: RestrictedApp,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                    text = app.appName,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isBlocked,
                onCheckedChange = onToggle
            )
        }
    }
}

