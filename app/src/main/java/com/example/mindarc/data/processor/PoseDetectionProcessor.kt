package com.example.mindarc.data.processor

import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.mindarc.data.model.ActivityType
import com.example.mindarc.domain.PoseAnalyzer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

class PoseDetectionProcessor(
    private val activityType: ActivityType = ActivityType.PUSHUPS,
    private val onPoseDetected: (Any, Pose?, Size) -> Unit
) : ImageAnalysis.Analyzer {
    
    val poseAnalyzer = PoseAnalyzer()
    
    private val poseDetector: PoseDetector = PoseDetection.getClient(
        AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.STREAM_MODE)
            .build()
    )
    
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
            
            // Normalize size based on rotation
            val width = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.width else imageProxy.height
            val height = if (imageProxy.imageInfo.rotationDegrees % 180 == 0) imageProxy.height else imageProxy.width
            val imageSize = Size(width, height)
            
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    when (activityType) {
                        ActivityType.PUSHUPS -> {
                            val metrics = poseAnalyzer.analyzePushUpPose(pose, width, height)
                            onPoseDetected(metrics, pose, imageSize)
                        }
                        ActivityType.SQUATS -> {
                            val metrics = poseAnalyzer.analyzeSquatPose(pose, width, height)
                            onPoseDetected(metrics, pose, imageSize)
                        }
                        else -> {
                            // Fallback or other activities
                        }
                    }
                }
                .addOnFailureListener {
                    // Send empty/error metrics based on activity type
                    val errorMetrics = when (activityType) {
                        ActivityType.PUSHUPS -> PoseAnalyzer.PushUpMetrics(null, 0, 0, "Detection failed", false, 0f)
                        ActivityType.SQUATS -> PoseAnalyzer.SquatMetrics(null, 0, 0, "Detection failed", false, 0f)
                        else -> Any()
                    }
                    onPoseDetected(errorMetrics, null, imageSize)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }
    
    fun cleanup() {
        poseDetector.close()
    }
}
