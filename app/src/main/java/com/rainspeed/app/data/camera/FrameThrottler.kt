package com.rainspeed.app.data.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

/**
 * Forwards only every [frameInterval]th frame to [onFrame]; all other frames are closed
 * immediately so CameraX can keep producing without backing up. [onFrame] owns closing
 * the frames it receives.
 */
class ThrottlingAnalyzer(
    private val frameInterval: Int = 4,
    private val onFrame: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {

    private var frameCounter = 0

    override fun analyze(image: ImageProxy) {
        frameCounter++
        if (frameCounter % frameInterval == 0) {
            onFrame(image)
        } else {
            image.close()
        }
    }
}
