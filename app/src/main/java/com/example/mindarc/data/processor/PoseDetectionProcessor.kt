package com.example.mindarc.data.processor

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.mindarc.domain.PoseAnalyzer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions

/**
 * Processes camera frames for pose detection using ML Kit.
 * This is part of the data layer as it handles external ML Kit API interactions.
 */
class PoseDetectionProcessor(
    private val onPoseDetected: (Float?) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val poseAnalyzer = PoseAnalyzer()
    
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
            
            poseDetector.process(image)
                .addOnSuccessListener { pose ->
                    val angle = poseAnalyzer.getBestArmAngle(pose)
                    onPoseDetected(angle)
                }
                .addOnFailureListener {
                    onPoseDetected(null)
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
