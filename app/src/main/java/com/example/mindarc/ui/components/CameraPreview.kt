package com.example.mindarc.ui.components

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.mindarc.data.processor.PoseDetectionProcessor
import java.util.concurrent.Executors

/**
 * CameraX preview composable that integrates with ML Kit Pose Detection.
 * Displays camera preview and processes frames for pose detection.
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onPoseDetected: (Float?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var poseProcessor: PoseDetectionProcessor? by remember { mutableStateOf(null) }
    
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
            
            val executor = ContextCompat.getMainExecutor(ctx)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                // Create preview use case
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Create image analysis use case for pose detection
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .build()
                
                val processor = PoseDetectionProcessor(onPoseDetected)
                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), processor)
                poseProcessor = processor
                
                // Select camera (prefer front camera for selfie mode)
                val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
                
                try {
                    // Unbind all use cases before rebinding
                    cameraProvider.unbindAll()
                    
                    // Bind use cases to lifecycle
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            
            previewView
        },
        modifier = modifier.fillMaxSize(),
        update = { }
    )
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            poseProcessor?.cleanup()
            cameraProviderFuture.get().unbindAll()
        }
    }
}
