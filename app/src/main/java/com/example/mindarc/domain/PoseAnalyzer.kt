package com.example.mindarc.domain

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

class PoseAnalyzer {
    
    data class PushUpMetrics(
        val elbowAngle: Float?,
        val repCount: Int,
        val depthPercentage: Int,
        val feedback: String,
        val isHorizontal: Boolean,
        val confidence: Float
    )

    private var repCount = 0
    private var isInRep = false // Tracks if we are currently in the "Down" phase
    
    private val DOWN_THRESHOLD = 95f
    private val UP_THRESHOLD = 160f

    fun analyzePushUpPose(pose: Pose, imageWidth: Int, imageHeight: Int): PushUpMetrics {
        val leftAngle = calculateElbowAngle(
            pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        )
        val rightAngle = calculateElbowAngle(
            pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        )

        val leftConf = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.inFrameLikelihood ?: 0f
        val rightConf = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.inFrameLikelihood ?: 0f

        // Robust Elbow Angle: Average of both if visible, otherwise the best visible one
        val currentAngle = when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle != null && leftConf > 0.5f -> leftAngle
            rightAngle != null && rightConf > 0.5f -> rightAngle
            else -> null
        }

        // Rep Counting Logic
        if (currentAngle != null) {
            if (!isInRep && currentAngle < DOWN_THRESHOLD) {
                isInRep = true
            } else if (isInRep && currentAngle > UP_THRESHOLD) {
                isInRep = false
                repCount++
            }
        }

        // Depth Percentage Calculation (160° is 0%, 95° is 100%)
        val depthPercentage = if (currentAngle != null) {
            val progress = (UP_THRESHOLD - currentAngle) / (UP_THRESHOLD - DOWN_THRESHOLD)
            (progress * 100).toInt().coerceIn(0, 100)
        } else 0

        val feedback = when {
            currentAngle == null -> "Position your arms"
            currentAngle < DOWN_THRESHOLD -> "Good depth! Now push up"
            isInRep -> "Pushing up..."
            else -> "Go lower"
        }

        // Simple plank check
        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val isHorizontal = shoulder != null && hip != null && 
                          abs(shoulder.position.x - hip.position.x) > (imageWidth * 0.2f)

        return PushUpMetrics(
            elbowAngle = currentAngle,
            repCount = repCount,
            depthPercentage = depthPercentage,
            feedback = feedback,
            isHorizontal = isHorizontal,
            confidence = shoulder?.inFrameLikelihood ?: 0f
        )
    }

    private fun calculateElbowAngle(s: PoseLandmark?, e: PoseLandmark?, w: PoseLandmark?): Float? {
        if (s == null || e == null || w == null) return null
        val sP = s.position3D; val eP = e.position3D; val wP = w.position3D
        val v1 = floatArrayOf(sP.x - eP.x, sP.y - eP.y, sP.z - eP.z)
        val v2 = floatArrayOf(wP.x - eP.x, wP.y - eP.y, wP.z - eP.z)
        val dot = v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]
        val m1 = sqrt(v1[0]*v1[0] + v1[1]*v1[1] + v1[2]*v1[2])
        val m2 = sqrt(v2[0]*v2[0] + v2[1]*v2[1] + v2[2]*v2[2])
        return if (m1 == 0f || m2 == 0f) null else (acos((dot/(m1*m2)).coerceIn(-1f, 1f)) * 180f / PI).toFloat()
    }
    
    fun resetReps() {
        repCount = 0
        isInRep = false
    }
}
