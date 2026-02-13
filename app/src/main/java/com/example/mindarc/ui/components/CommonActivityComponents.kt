package com.example.mindarc.ui.components

import android.speech.tts.TextToSpeech
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

/**
 * Announces each rep count via TTS when the count increases (e.g. "One", "Two", "Three").
 * Call this from pushup or squat screens when the activity has started.
 */
@Composable
fun RepCountTts(
    currentCount: Int,
    hasStarted: Boolean
) {
    val context = LocalContext.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var prevCount by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        var engineRef: TextToSpeech? = null
        val engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engineRef?.language = Locale.getDefault()
                tts = engineRef
            }
        }
        engineRef = engine
        onDispose {
            engine.shutdown()
            tts = null
        }
    }

    LaunchedEffect(currentCount, hasStarted) {
        if (!hasStarted) return@LaunchedEffect
        if (currentCount == 0) {
            prevCount = 0
            return@LaunchedEffect
        }
        if (currentCount > prevCount) {
            prevCount = currentCount
            val word = numberToWord(currentCount)
            tts?.speak(word, TextToSpeech.QUEUE_FLUSH, null, "rep_$currentCount")
        }
    }
}

private fun numberToWord(n: Int): String = when (n) {
    1 -> "One"
    2 -> "Two"
    3 -> "Three"
    4 -> "Four"
    5 -> "Five"
    6 -> "Six"
    7 -> "Seven"
    8 -> "Eight"
    9 -> "Nine"
    10 -> "Ten"
    11 -> "Eleven"
    12 -> "Twelve"
    13 -> "Thirteen"
    14 -> "Fourteen"
    15 -> "Fifteen"
    16 -> "Sixteen"
    17 -> "Seventeen"
    18 -> "Eighteen"
    19 -> "Nineteen"
    20 -> "Twenty"
    else -> n.toString()
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
                    text = "You smashed $count reps, earned $points points and unlocked your apps for $duration minutes.",
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
            text = "To count your reps accurately, MindArc uses AI to detect your posture through the camera.",
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
