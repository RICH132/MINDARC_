package com.example.mindarc.domain

import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*

/**
 * Domain layer component for analyzing pose data and calculating angles.
 * Follows Clean Architecture principles by keeping business logic separate.
 */
class PoseAnalyzer {
    
    /**
     * Calculates the angle at the elbow joint using three points:
     * Shoulder -> Elbow -> Wrist
     * 
     * @param shoulder The shoulder landmark
     * @param elbow The elbow landmark
     * @param wrist The wrist landmark
     * @return The angle in degrees, or null if any landmark is missing
     */
    fun calculateElbowAngle(
        shoulder: PoseLandmark?,
        elbow: PoseLandmark?,
        wrist: PoseLandmark?
    ): Float? {
        if (shoulder == null || elbow == null || wrist == null) {
            return null
        }
        
        val shoulderPoint = shoulder.position3D
        val elbowPoint = elbow.position3D
        val wristPoint = wrist.position3D
        
        // Calculate vectors
        val vector1 = Point3D(
            shoulderPoint.x - elbowPoint.x,
            shoulderPoint.y - elbowPoint.y,
            shoulderPoint.z - elbowPoint.z
        )
        
        val vector2 = Point3D(
            wristPoint.x - elbowPoint.x,
            wristPoint.y - elbowPoint.y,
            wristPoint.z - elbowPoint.z
        )
        
        // Calculate angle using dot product
        val dotProduct = vector1.x * vector2.x + vector1.y * vector2.y + vector1.z * vector2.z
        val magnitude1 = sqrt(vector1.x * vector1.x + vector1.y * vector1.y + vector1.z * vector1.z)
        val magnitude2 = sqrt(vector2.x * vector2.x + vector2.y * vector2.y + vector2.z * vector2.z)
        
        if (magnitude1 == 0f || magnitude2 == 0f) {
            return null
        }
        
        val cosAngle = dotProduct / (magnitude1 * magnitude2)
        // Clamp to avoid NaN from acos
        val clampedCos = cosAngle.coerceIn(-1f, 1f)
        val angleRadians = acos(clampedCos)
        // Convert radians to degrees: degrees = radians * 180 / PI
        val angleDegrees = (angleRadians * 180f / PI).toFloat()
        
        return angleDegrees
    }
    
    /**
     * Gets the left arm landmarks from a pose
     */
    fun getLeftArmLandmarks(pose: Pose): Triple<PoseLandmark?, PoseLandmark?, PoseLandmark?> {
        return Triple(
            pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
        )
    }
    
    /**
     * Gets the right arm landmarks from a pose
     */
    fun getRightArmLandmarks(pose: Pose): Triple<PoseLandmark?, PoseLandmark?, PoseLandmark?> {
        return Triple(
            pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER),
            pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW),
            pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
        )
    }
    
    /**
     * Determines which arm is more visible/accurate based on confidence scores
     */
    fun getBestArmAngle(pose: Pose): Float? {
        val leftArm = getLeftArmLandmarks(pose)
        val rightArm = getRightArmLandmarks(pose)
        
        val leftAngle = calculateElbowAngle(leftArm.first, leftArm.second, leftArm.third)
        val rightAngle = calculateElbowAngle(rightArm.first, rightArm.second, rightArm.third)
        
        // Prefer the arm with better visibility (higher confidence)
        val leftConfidence = leftArm.second?.inFrameLikelihood ?: 0f
        val rightConfidence = rightArm.second?.inFrameLikelihood ?: 0f
        
        return when {
            leftConfidence > rightConfidence && leftConfidence > 0.5f -> leftAngle
            rightConfidence > 0.5f -> rightAngle
            leftAngle != null -> leftAngle
            else -> rightAngle
        }
    }
}

/**
 * Simple 3D point data class
 */
private data class Point3D(
    val x: Float,
    val y: Float,
    val z: Float
)

