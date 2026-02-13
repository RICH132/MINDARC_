package com.example.mindarc.ui.components

import android.util.Size
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark

// Premium palette: soft white skeleton, teal accent, amber for success
private val PremiumSkeleton = Color(0xFFE8F4FC)
private val PremiumSkeletonGlow = Color(0xFF00B4A0)
private val PremiumJointHigh = Color(0xFF00D4AA)
private val PremiumJointLow = Color(0xFF4A5568)
private val PremiumTextBg = Color(0xE6121419)
private val PremiumAccent = Color(0xFF00D4AA)
private val PremiumSuccess = Color(0xFF34D399)
private val PremiumMuted = Color(0xFF94A3B8)

@Composable
fun PoseOverlay(
    modifier: Modifier = Modifier,
    pose: Pose?,
    imageSize: Size,
    repCount: Int,
    depthPercentage: Int,
    feedback: String
) {
    Canvas(modifier = modifier) {
        if (pose == null) return@Canvas

        val canvasWidth = size.width
        val canvasHeight = size.height
        val scaleX = canvasWidth / imageSize.width
        val scaleY = canvasHeight / imageSize.height
        val scaleFactor = maxOf(scaleX, scaleY)
        val dx = (canvasWidth - imageSize.width * scaleFactor) / 2
        val dy = (canvasHeight - imageSize.height * scaleFactor) / 2

        fun mapPoint(x: Float, y: Float): Offset {
            val flippedX = imageSize.width - x
            return Offset(flippedX * scaleFactor + dx, y * scaleFactor + dy)
        }

        val connections = listOf(
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.RIGHT_SHOULDER),
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_ELBOW),
            Pair(PoseLandmark.LEFT_ELBOW, PoseLandmark.LEFT_WRIST),
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_ELBOW),
            Pair(PoseLandmark.RIGHT_ELBOW, PoseLandmark.RIGHT_WRIST),
            Pair(PoseLandmark.LEFT_SHOULDER, PoseLandmark.LEFT_HIP),
            Pair(PoseLandmark.RIGHT_SHOULDER, PoseLandmark.RIGHT_HIP),
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.RIGHT_HIP),
            Pair(PoseLandmark.LEFT_HIP, PoseLandmark.LEFT_KNEE),
            Pair(PoseLandmark.RIGHT_HIP, PoseLandmark.RIGHT_KNEE),
            Pair(PoseLandmark.LEFT_KNEE, PoseLandmark.LEFT_ANKLE),
            Pair(PoseLandmark.RIGHT_KNEE, PoseLandmark.RIGHT_ANKLE)
        )

        val strokeWidth = 5f
        val glowWidth = 14f
        val jointRadius = 7f
        val jointGlowRadius = 14f

        // ---- Bones: glow layer then main stroke (round cap/join) ----
        connections.forEach { (startType, endType) ->
            val start = pose.getPoseLandmark(startType)
            val end = pose.getPoseLandmark(endType)
            if (start != null && end != null) {
                val p1 = mapPoint(start.position.x, start.position.y)
                val p2 = mapPoint(end.position.x, end.position.y)
                val path = Path().apply {
                    moveTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                }
                drawPath(
                    path,
                    PremiumSkeletonGlow.copy(alpha = 0.25f),
                    style = Stroke(width = glowWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawPath(
                    path,
                    PremiumSkeleton.copy(alpha = 0.95f),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }

        // ---- Joints: outer glow + inner fill (premium look) ----
        pose.allPoseLandmarks.forEach { landmark ->
            val jointCenter = mapPoint(landmark.position.x, landmark.position.y)
            val confident = landmark.inFrameLikelihood > 0.5f
            val jointColor = if (confident) PremiumJointHigh else PremiumJointLow
            drawCircle(
                color = jointColor.copy(alpha = 0.35f),
                radius = jointGlowRadius,
                center = jointCenter
            )
            drawCircle(
                color = jointColor.copy(alpha = 0.9f),
                radius = jointRadius,
                center = jointCenter
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.9f),
                radius = jointRadius * 0.5f,
                center = jointCenter
            )
        }

        // ---- Premium HUD: rounded card + typography ----
        val nose = pose.getPoseLandmark(PoseLandmark.NOSE)
        if (nose != null) {
            val pt = mapPoint(nose.position.x, nose.position.y)
            val hudLeft = (pt.x - 120f).coerceIn(20f, canvasWidth - 260f)
            val hudTop = (pt.y - 140f).coerceIn(20f, canvasHeight - 120f)
            val hudWidth = 240f
            val hudHeight = 100f
            val cornerRadius = CornerRadius(20f)
            drawRoundRect(
                color = PremiumTextBg,
                topLeft = Offset(hudLeft, hudTop),
                size = androidx.compose.ui.geometry.Size(hudWidth, hudHeight),
                cornerRadius = cornerRadius
            )
            drawRoundRect(
                color = PremiumSkeletonGlow.copy(alpha = 0.2f),
                topLeft = Offset(hudLeft, hudTop),
                size = androidx.compose.ui.geometry.Size(hudWidth, hudHeight),
                cornerRadius = cornerRadius,
                style = Stroke(width = 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawContext.canvas.nativeCanvas.apply {
                val repPaint = Paint().asFrameworkPaint().apply {
                    color = Color.White.toArgb()
                    textSize = 36f
                    isFakeBoldText = true
                    setShadowLayer(6f, 0f, 2f, android.graphics.Color.BLACK)
                }
                val labelPaint = Paint().asFrameworkPaint().apply {
                    color = PremiumMuted.toArgb()
                    textSize = 22f
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                }
                val accentPaint = Paint().asFrameworkPaint().apply {
                    color = PremiumAccent.toArgb()
                    textSize = 20f
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                }
                val feedbackPaint = Paint().asFrameworkPaint().apply {
                    color = if (feedback.contains("Good", true)) PremiumSuccess.toArgb() else Color.White.toArgb()
                    textSize = 24f
                    isFakeBoldText = true
                    setShadowLayer(4f, 0f, 1f, android.graphics.Color.BLACK)
                }

                drawText("$repCount", hudLeft + 24f, hudTop + 42f, repPaint)
                drawText("reps", hudLeft + 24f, hudTop + 68f, labelPaint)
                drawText("$depthPercentage%", hudLeft + 130f, hudTop + 42f, accentPaint)
                drawText("depth", hudLeft + 130f, hudTop + 68f, labelPaint)
                drawText(feedback, hudLeft + 24f, hudTop + 92f, feedbackPaint)
            }
        }
    }
}
