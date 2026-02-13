package com.example.mindarc.ui.screen

import android.content.res.Configuration
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.data.processor.PoseDetectionProcessor
import com.example.mindarc.domain.PoseAnalyzer
import com.example.mindarc.ui.components.CameraPreview
import com.example.mindarc.ui.components.CompletionOverlay
import com.example.mindarc.ui.components.PermissionRequestUI
import com.example.mindarc.ui.components.PoseOverlay
import com.example.mindarc.ui.components.RepCountTts
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.ui.viewmodel.PushUpCounterViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PushupsActivityScreen(
    navController: NavController,
    mindArcViewModel: MindArcViewModel = hiltViewModel(),
    pushUpCounterViewModel: PushUpCounterViewModel = hiltViewModel()
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val cameraPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.CAMERA)
    )

    val pushUpState by pushUpCounterViewModel.state.collectAsState()
    var isCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // --- Start / Countdown state ---
    var hasStarted by remember { mutableStateOf(false) }
    var isCountingDown by remember { mutableStateOf(false) }
    var countdownValue by remember { mutableStateOf(5) }

    val processor = remember {
        PoseDetectionProcessor(ActivityType.PUSHUPS) { metrics, pose, size ->
            if (metrics is PoseAnalyzer.PushUpMetrics) {
                pushUpCounterViewModel.updateMetrics(metrics, pose, size)
            }
        }
    }

    // Countdown timer
    LaunchedEffect(isCountingDown) {
        if (isCountingDown) {
            countdownValue = 5
            for (i in 5 downTo 1) {
                countdownValue = i
                delay(1000L)
            }
            countdownValue = 0 // 0 = "GO!"
            delay(600L)
            // Reset reps so any pre-start movement doesn't count
            processor.poseAnalyzer.resetReps()
            pushUpCounterViewModel.resetCount()
            isCountingDown = false
            hasStarted = true
        }
    }

    val unlockDuration = remember(pushUpState.count) {
        if (pushUpState.count > 0) (pushUpState.count * 15) / 10 else 0
    }
    val points = remember(pushUpState.count) { pushUpState.count }

    // Full-screen Box — no Scaffold in landscape to maximise camera area
    Box(modifier = Modifier.fillMaxSize()) {
        if (cameraPermissionState.allPermissionsGranted) {
            // ---- Camera background (always running so user can position) ----
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                processor = processor
            )

            // Pose skeleton only after activity has started
            if (hasStarted && pushUpState.imageSize.width > 0) {
                PoseOverlay(
                    modifier = Modifier.fillMaxSize(),
                    pose = pushUpState.currentPose,
                    imageSize = pushUpState.imageSize,
                    repCount = pushUpState.count,
                    depthPercentage = pushUpState.depthPercentage,
                    feedback = pushUpState.formFeedback
                )
            }

            PushupGuideOverlay()

            // TTS: announce each rep ("One", "Two", ...) when count increases
            RepCountTts(currentCount = pushUpState.count, hasStarted = hasStarted)

            // ---- Floating back button (always visible) ----
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            // ============================================================
            // Phase 1 — NOT STARTED: show Start button
            // ============================================================
            if (!hasStarted && !isCountingDown && !isCompleted) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "AI PUSHUP COUNTER",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Portrait or landscape — position yourself and tap Start",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { isCountingDown = true },
                            modifier = Modifier
                                .height(64.dp)
                                .width(220.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ============================================================
            // Phase 2 — COUNTDOWN: 5 → 4 → 3 → 2 → 1 → GO!
            // ============================================================
            if (isCountingDown) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "GET READY",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (countdownValue > 0) "$countdownValue" else "GO!",
                            style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                            fontWeight = FontWeight.Black,
                            color = if (countdownValue > 0) Color.White else Color(0xFF4CAF50)
                        )
                    }
                }
            }

            // ============================================================
            // Phase 3 — ACTIVE: Row layout in landscape, Column in portrait
            // ============================================================
            if (hasStarted && !isCompleted) {
                if (isLandscape) {
                    // Landscape: side-by-side
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PushupActiveFeedbackChip(pushUpState = pushUpState)
                            PushupRepCounterCircle(pushUpState = pushUpState)
                        }
                        Column(
                            modifier = Modifier.width(220.dp).fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            PushupStatsCard(
                                points = points,
                                unlockDuration = unlockDuration,
                                onReset = {
                                    processor.poseAnalyzer.resetReps()
                                    pushUpCounterViewModel.resetCount()
                                },
                                onFinish = {
                                    if (pushUpState.count > 0) {
                                        scope.launch {
                                            mindArcViewModel.completePushupsActivity(pushUpState.count)
                                            isCompleted = true
                                        }
                                    }
                                },
                                canFinish = pushUpState.count > 0
                            )
                        }
                    }
                } else {
                    // Portrait: stacked like squats
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        PushupActiveFeedbackChip(pushUpState = pushUpState)
                        Surface(
                            modifier = Modifier.size(160.dp),
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                4.dp,
                                if (pushUpState.isGoodForm) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "${pushUpState.count}",
                                        style = MaterialTheme.typography.displayLarge,
                                        fontWeight = FontWeight.Black,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "REPS",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        PushupStatsCard(
                            points = points,
                            unlockDuration = unlockDuration,
                            onReset = {
                                processor.poseAnalyzer.resetReps()
                                pushUpCounterViewModel.resetCount()
                            },
                            onFinish = {
                                if (pushUpState.count > 0) {
                                    scope.launch {
                                        mindArcViewModel.completePushupsActivity(pushUpState.count)
                                        isCompleted = true
                                    }
                                }
                            },
                            canFinish = pushUpState.count > 0,
                            compact = false
                        )
                    }
                }
            }
        } else {
            // Camera permission not granted
            PermissionRequestUI(onGrant = { cameraPermissionState.launchMultiplePermissionRequest() })
            // Still show a back button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .statusBarsPadding()
                    .padding(8.dp)
                    .align(Alignment.TopStart)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
        }

        // ---- Completion overlay ----
        if (isCompleted) {
            CompletionOverlay(pushUpState.count, points, unlockDuration) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Home.route) { inclusive = true }
                }
            }
        }
    }
}

@Composable
private fun PushupActiveFeedbackChip(pushUpState: com.example.mindarc.ui.viewmodel.PushUpCounterState) {
    AnimatedVisibility(
        visible = pushUpState.formFeedback.isNotEmpty(),
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when {
                pushUpState.formFeedback.contains("Good", true) ->
                    Color(0xFF4CAF50).copy(alpha = 0.9f)
                !pushUpState.isGoodForm && pushUpState.isDetecting ->
                    Color(0xFFF44336).copy(alpha = 0.9f)
                else ->
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = pushUpState.formFeedback.uppercase(),
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun PushupRepCounterCircle(pushUpState: com.example.mindarc.ui.viewmodel.PushUpCounterState) {
    Surface(
        modifier = Modifier.size(120.dp),
        shape = CircleShape,
        color = Color.Black.copy(alpha = 0.5f),
        border = androidx.compose.foundation.BorderStroke(
            3.dp,
            if (pushUpState.isGoodForm) MaterialTheme.colorScheme.primary else Color.Gray
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${pushUpState.count}",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PushupStatsCard(
    points: Int,
    unlockDuration: Int,
    onReset: () -> Unit,
    onFinish: () -> Unit,
    canFinish: Boolean,
    compact: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(if (compact) 20.dp else 28.dp),
        color = Color.Black.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(if (compact) 16.dp else 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "+$points",
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "POINTS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
                VerticalDivider(
                    modifier = Modifier.height(if (compact) 28.dp else 32.dp),
                    color = Color.White.copy(alpha = 0.2f)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$unlockDuration min",
                        style = if (compact) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "UNLOCK",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(if (compact) 12.dp else 20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(if (compact) 48.dp else 56.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .weight(1f)
                        .height(if (compact) 48.dp else 56.dp),
                    shape = RoundedCornerShape(if (compact) 14.dp else 16.dp),
                    enabled = canFinish,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(if (compact) 18.dp else 24.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Finish Workout",
                        fontWeight = FontWeight.Bold,
                        style = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RotateToLandscapePrompt(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Pushup Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(horizontal = 32.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(96.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ScreenRotation,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    text = "Rotate to Landscape",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "Place your phone on the ground in landscape mode to start the pushup exercise. You can face the camera or position sideways.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PushupGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val strokeWidth = 2.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)

        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height * 0.8f),
            end = Offset(width, height * 0.8f),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )

        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height * 0.5f),
            end = Offset(width, height * 0.5f),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
    }
}
