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

    data class SquatMetrics(
        val kneeAngle: Float?,
        val repCount: Int,
        val depthPercentage: Int,
        val feedback: String,
        val isUpright: Boolean,
        val confidence: Float
    )

    private var pushupRepCount = 0
    private var isInPushupRep = false
    private var squatRepCount = 0
    private var isInSquatRep = false

    private val PUSHUP_DOWN_THRESHOLD = 95f
    private val PUSHUP_UP_THRESHOLD = 160f
    private val SQUAT_DOWN_THRESHOLD = 100f
    private val SQUAT_UP_THRESHOLD = 160f

    fun analyzePushUpPose(pose: Pose, imageWidth: Int, imageHeight: Int): PushUpMetrics {
        val leftAngle = calculateAngle(
            pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        )
        val rightAngle = calculateAngle(
            pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        )

        val leftConf = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)?.inFrameLikelihood ?: 0f
        val rightConf = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)?.inFrameLikelihood ?: 0f

        val currentAngle = when {
            leftAngle != null && rightAngle != null -> (leftAngle + rightAngle) / 2f
            leftAngle != null && leftConf > 0.5f -> leftAngle
            rightAngle != null && rightConf > 0.5f -> rightAngle
            else -> null
        }

        if (currentAngle != null) {
            if (!isInPushupRep && currentAngle < PUSHUP_DOWN_THRESHOLD) {
                isInPushupRep = true
            } else if (isInPushupRep && currentAngle > PUSHUP_UP_THRESHOLD) {
                isInPushupRep = false
                pushupRepCount++
            }
        }

        val depthPercentage = if (currentAngle != null) {
            val progress = (PUSHUP_UP_THRESHOLD - currentAngle) / (PUSHUP_UP_THRESHOLD - PUSHUP_DOWN_THRESHOLD)
            (progress * 100).toInt().coerceIn(0, 100)
        } else 0

        val feedback = when {
            currentAngle == null -> "Position your arms"
            currentAngle < PUSHUP_DOWN_THRESHOLD -> "Good depth! Now push up"
            isInPushupRep -> "Pushing up..."
            else -> "Go lower"
        }

        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val hip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
        val isHorizontal = shoulder != null && hip != null &&
                          abs(shoulder.position.x - hip.position.x) > (imageWidth * 0.2f)

        return PushUpMetrics(
            elbowAngle = currentAngle,
            repCount = pushupRepCount,
            depthPercentage = depthPercentage,
            feedback = feedback,
            isHorizontal = isHorizontal,
            confidence = shoulder?.inFrameLikelihood ?: 0f
        )
    }

    fun analyzeSquatPose(pose: Pose, imageWidth: Int, imageHeight: Int): SquatMetrics {
        val leftKneeAngle = calculateAngle(
            pose.getPoseLandmark(PoseLandmark.LEFT_HIP),
            pose.getPoseLandmark(PoseLandmark.LEFT_KNEE),
            pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
        )
        val rightKneeAngle = calculateAngle(
            pose.getPoseLandmark(PoseLandmark.RIGHT_HIP),
            pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        )

        val leftConf = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)?.inFrameLikelihood ?: 0f
        val rightConf = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)?.inFrameLikelihood ?: 0f

        val currentAngle = when {
            leftKneeAngle != null && rightKneeAngle != null -> (leftKneeAngle + rightKneeAngle) / 2f
            leftKneeAngle != null && leftConf > 0.5f -> leftKneeAngle
            rightKneeAngle != null && rightConf > 0.5f -> rightKneeAngle
            else -> null
        }

        if (currentAngle != null) {
            if (!isInSquatRep && currentAngle < SQUAT_DOWN_THRESHOLD) {
                isInSquatRep = true
            } else if (isInSquatRep && currentAngle > SQUAT_UP_THRESHOLD) {
                isInSquatRep = false
                squatRepCount++
            }
        }

        val depthPercentage = if (currentAngle != null) {
            val progress = (SQUAT_UP_THRESHOLD - currentAngle) / (SQUAT_UP_THRESHOLD - SQUAT_DOWN_THRESHOLD)
            (progress * 100).toInt().coerceIn(0, 100)
        } else 0

        val feedback = when {
            currentAngle == null -> "Position your legs"
            currentAngle < SQUAT_DOWN_THRESHOLD -> "Good depth! Now stand up"
            isInSquatRep -> "Going up..."
            else -> "Go lower"
        }

        val shoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
        val ankle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE) ?: pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
        val isUpright = shoulder != null && ankle != null &&
                          abs(shoulder.position.y - ankle.position.y) > (imageHeight * 0.6f)


        return SquatMetrics(
            kneeAngle = currentAngle,
            repCount = squatRepCount,
            depthPercentage = depthPercentage,
            feedback = feedback,
            isUpright = isUpright,
            confidence = shoulder?.inFrameLikelihood ?: 0f
        )
    }

    private fun calculateAngle(p1: PoseLandmark?, p2: PoseLandmark?, p3: PoseLandmark?): Float? {
        if (p1 == null || p2 == null || p3 == null) return null
        val p1_3D = p1.position3D; val p2_3D = p2.position3D; val p3_3D = p3.position3D
        val v1 = floatArrayOf(p1_3D.x - p2_3D.x, p1_3D.y - p2_3D.y, p1_3D.z - p2_3D.z)
        val v2 = floatArrayOf(p3_3D.x - p2_3D.x, p3_3D.y - p2_3D.y, p3_3D.z - p2_3D.z)
        val dot = v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]
        val m1 = sqrt(v1[0]*v1[0] + v1[1]*v1[1] + v1[2]*v2[2])
        val m2 = sqrt(v2[0]*v2[0] + v2[1]*v2[1] + v2[2]*v2[2])
        return if (m1 == 0f || m2 == 0f) null else (acos((dot/(m1*m2)).coerceIn(-1f, 1f)) * 180f / PI).toFloat()
    }

    fun resetReps() {
        pushupRepCount = 0
        isInPushupRep = false
        squatRepCount = 0
        isInSquatRep = false
    }
}
