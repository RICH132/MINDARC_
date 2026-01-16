package com.example.mindarc.ui.screen

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.components.CameraPreview
import com.example.mindarc.ui.components.PoseOverlay
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import com.example.mindarc.ui.viewmodel.PushUpCounterViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun PushupsActivityScreen(
    navController: NavController,
    mindArcViewModel: MindArcViewModel = viewModel(),
    pushUpCounterViewModel: PushUpCounterViewModel = viewModel()
) {
    val cameraPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(android.Manifest.permission.CAMERA)
    )
    
    val pushUpState by pushUpCounterViewModel.state.collectAsState()
    var isCompleted by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val unlockDuration = remember(pushUpState.count) {
        if (pushUpState.count > 0) (pushUpState.count * 15) / 10 else 0
    }
    val points = remember(pushUpState.count) { pushUpState.count }
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("AI Pushup Counter", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.3f),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview (Full Screen)
            if (cameraPermissionState.allPermissionsGranted) {
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onPoseDetected = { metrics, pose, size ->
                        pushUpCounterViewModel.updateMetrics(metrics, pose, size)
                    }
                )
                
                // Skeleton Overlay
                if (pushUpState.imageSize.width > 0) {
                    PoseOverlay(
                        modifier = Modifier.fillMaxSize(),
                        pose = pushUpState.currentPose,
                        imageSize = pushUpState.imageSize,
                        metrics = null // We don't need text metrics inside PoseOverlay for now
                    )
                }
                
                // Placeholder Guide Lines for Pushup Position
                PushupGuideOverlay()
            } else {
                PermissionRequestUI(onGrant = { cameraPermissionState.launchMultiplePermissionRequest() })
            }
            
            // UI Overlay
            if (cameraPermissionState.allPermissionsGranted && !isCompleted) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Feedback Card
                    AnimatedVisibility(
                        visible = pushUpState.formFeedback.isNotEmpty(),
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                pushUpState.formFeedback.contains("Complete", true) -> Color(0xFF4CAF50).copy(alpha = 0.9f)
                                !pushUpState.isGoodForm && pushUpState.isDetecting -> Color(0xFFF44336).copy(alpha = 0.9f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
                            },
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = pushUpState.formFeedback.uppercase(),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }

                    // Main Counter
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
                    
                    // Stats and Controls Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+$points", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("POINTS", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                                VerticalDivider(modifier = Modifier.height(32.dp), color = Color.White.copy(alpha = 0.2f))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("$unlockDuration min", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("UNLOCK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.6f))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                IconButton(
                                    onClick = { pushUpCounterViewModel.resetCount() },
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                                }
                                
                                Button(
                                    onClick = {
                                        if (pushUpState.count > 0) {
                                            scope.launch {
                                                mindArcViewModel.completePushupsActivity(pushUpState.count)
                                                isCompleted = true
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(56.dp),
                                    shape = RoundedCornerShape(16.dp),
                                    enabled = pushUpState.count > 0,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Finish Workout", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
            
            // Completion Screen Overlay
            if (isCompleted) {
                CompletionOverlay(pushUpState.count, points, unlockDuration) {
                    navController.navigate(Screen.Home.route) { 
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            }
        }
    }
}

@Composable
fun PushupGuideOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Horizontal guide lines for torso/floor placement
        val strokeWidth = 2.dp.toPx()
        val pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
        
        // Floor line
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height * 0.8f),
            end = Offset(width, height * 0.8f),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
        
        // Shoulder line
        drawLine(
            color = Color.White.copy(alpha = 0.3f),
            start = Offset(0f, height * 0.5f),
            end = Offset(width, height * 0.5f),
            strokeWidth = strokeWidth,
            pathEffect = pathEffect
        )
    }
}

@Composable
fun CompletionOverlay(count: Int, points: Int, duration: Int, onHome: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(32.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(80.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Incredible Work!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    text = "You smashed $count pushups, earned $points points and unlocked your apps for $duration minutes.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = onHome,
                    modifier = Modifier.fillMaxWidth().height(60.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Return Home", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun PermissionRequestUI(onGrant: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.size(100.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.VideocamOff, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Camera Access Needed",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "To count your pushups accurately, MindArc uses AI to detect your posture through the camera.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Button(
            onClick = onGrant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Enable Camera Access", fontWeight = FontWeight.Bold)
        }
    }
}
