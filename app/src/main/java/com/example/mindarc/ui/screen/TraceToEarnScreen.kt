package com.example.mindarc.ui.screen

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.mindarc.ui.navigation.Screen
import com.example.mindarc.ui.viewmodel.MindArcViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TRACE_DURATION_SEC = 30
private const val THRESHOLD_EXCELLENT_PX = 10f   // avg < 10px -> 5 min
private const val THRESHOLD_AVERAGE_PX = 30f    // avg < 30px -> 1 min
private const val STRAY_COLOR_THRESHOLD_PX = 25f // above this draw red

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TraceToEarnScreen(
    navController: NavController,
    viewModel: MindArcViewModel = hiltViewModel()
) {
    val scope = rememberCoroutineScope()
    val outline = remember { TraceOutlines.random() }
    val outlinePoints = outline.points

    var timeLeft by remember { mutableIntStateOf(TRACE_DURATION_SEC) }
    var isRunning by remember { mutableStateOf(true) }
    var userPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var userPointsNormalized by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasScale by remember { mutableStateOf(1f) }
    var showResult by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var minutesEarned by remember { mutableIntStateOf(0) }
    var avgDistance by remember { mutableStateOf(Float.MAX_VALUE) }

    LaunchedEffect(isRunning) {
        if (!isRunning) return@LaunchedEffect
        while (timeLeft > 0 && isRunning) {
            delay(1000)
            timeLeft -= 1
        }
        if (timeLeft <= 0) {
            isRunning = false
            // Accuracy: user points normalized 0..1; distance in norm space * scale = pixels
            val normPoints = userPointsNormalized
            val avgNorm = if (normPoints.isEmpty()) Float.MAX_VALUE
            else normPoints.map { distanceToOutline(it, outlinePoints) }.average().toFloat()
            avgDistance = avgNorm * canvasScale
            minutesEarned = when {
                avgDistance < THRESHOLD_EXCELLENT_PX -> 5
                avgDistance < THRESHOLD_AVERAGE_PX -> 1
                else -> 0
            }
            resultMessage = when (minutesEarned) {
                5 -> "Excellent! You earned 5 minutes."
                1 -> "Average. You earned 1 minute."
                else -> "Needs improvement. No time earned this time."
            }
            scope.launch {
                viewModel.completeTraceToEarnActivity(minutesEarned)
            }
            showResult = true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Trace to Earn", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (timeLeft <= 10) MaterialTheme.colorScheme.errorContainer
                        else MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "${timeLeft}s",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (showResult) {
                TraceResultOverlay(
                    resultMessage = resultMessage,
                    minutesEarned = minutesEarned,
                    avgDistancePx = avgDistance,
                    onDismiss = { navController.popBackStack() }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Trace the ${outline.name}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay close to the line. <10px = 5 min, <30px = 1 min.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    TraceCanvas(
                        outline = outline,
                        userPoints = userPoints,
                        userPointsNormalized = userPointsNormalized,
                        onPointsChange = { pts, normPts -> userPoints = pts; userPointsNormalized = normPts },
                        onCanvasScale = { canvasScale = it },
                        enabled = isRunning
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                userPoints = emptyList()
                                userPointsNormalized = emptyList()
                                timeLeft = TRACE_DURATION_SEC
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset")
                        }
                        Button(
                            onClick = {
                                isRunning = false
                                val normPoints = userPointsNormalized
                                val avgNorm = if (normPoints.isEmpty()) Float.MAX_VALUE
                                else normPoints.map { distanceToOutline(it, outlinePoints) }.average().toFloat()
                                avgDistance = avgNorm * canvasScale
                                minutesEarned = when {
                                    avgDistance < THRESHOLD_EXCELLENT_PX -> 5
                                    avgDistance < THRESHOLD_AVERAGE_PX -> 1
                                    else -> 0
                                }
                                resultMessage = when (minutesEarned) {
                                    5 -> "Excellent! You earned 5 minutes."
                                    1 -> "Average. You earned 1 minute."
                                    else -> "Needs improvement. No time earned this time."
                                }
                                scope.launch {
                                    viewModel.completeTraceToEarnActivity(minutesEarned)
                                }
                                showResult = true
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Submit")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TraceCanvas(
    outline: TraceOutline,
    userPoints: List<Offset>,
    userPointsNormalized: List<Offset>,
    onPointsChange: (List<Offset>, List<Offset>) -> Unit,
    onCanvasScale: (Float) -> Unit,
    enabled: Boolean
) {
    var currentPath by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var currentPathNorm by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    fun toNormalized(p: Offset) = Offset((p.x - offsetX) / scale, (p.y - offsetY) / scale)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .onSizeChanged { size ->
                val w = size.width.toFloat()
                val h = size.height.toFloat()
                scale = minOf(w, h) * 0.85f
                offsetX = (w - scale) / 2
                offsetY = (h - scale) / 2
                onCanvasScale(scale)
            }
            .pointerInput(enabled, scale, offsetX, offsetY) {
                if (!enabled || scale <= 0) return@pointerInput
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = listOf(offset)
                        currentPathNorm = listOf(toNormalized(offset))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        currentPath = currentPath + pos
                        currentPathNorm = currentPathNorm + toNormalized(pos)
                    },
                    onDragEnd = {
                        onPointsChange(userPoints + currentPath, userPointsNormalized + currentPathNorm)
                        currentPath = emptyList()
                        currentPathNorm = emptyList()
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val s = minOf(w, h) * 0.85f
        val ox = (w - s) / 2
        val oy = (h - s) / 2

        fun toCanvas(p: Offset) = Offset(ox + p.x * s, oy + p.y * s)

        // Draw outline (gray)
        val outlinePath = Path().apply {
            val pts = outline.points
            if (pts.isNotEmpty()) {
                moveTo(toCanvas(pts.first()).x, toCanvas(pts.first()).y)
                pts.drop(1).forEach { lineTo(toCanvas(it).x, toCanvas(it).y) }
            }
        }
        drawPath(
            outlinePath,
            color = Color.Gray.copy(alpha = 0.6f),
            style = Stroke(width = 4f, cap = StrokeCap.Round)
        )

        // Draw user path (green when close, red when far)
        val allDrawn = userPoints + currentPath
        val allDrawnNorm = userPointsNormalized + currentPathNorm
        val refPts = outline.points
        for (i in 0 until allDrawn.size - 1) {
            if (i >= allDrawnNorm.size - 1) break
            val np0 = allDrawnNorm[i]
            val np1 = allDrawnNorm[i + 1]
            val d0 = distanceToOutline(np0, refPts)
            val d1 = distanceToOutline(np1, refPts)
            val avgD = (d0 + d1) / 2
            val normalizedD = avgD * s
            val color = if (normalizedD <= STRAY_COLOR_THRESHOLD_PX) Color(0xFF2E7D32)
            else Color(0xFFC62828)
            drawLine(
                color = color,
                start = allDrawn[i],
                end = allDrawn[i + 1],
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun TraceResultOverlay(
    resultMessage: String,
    minutesEarned: Int,
    avgDistancePx: Float,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = when (minutesEarned) {
                        5 -> "🏆 Excellent!"
                        1 -> "👍 Average"
                        else -> "📉 Needs improvement"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (avgDistancePx < Float.MAX_VALUE) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Average distance: ${(avgDistancePx * 10).roundToInt() / 10f}px",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
            }
        }
    }
}
