package com.rainspeed.app.data.vision

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

/**
 * Turns one camera frame into a contrast-enhanced grayscale [Bitmap] plus the rain streaks
 * [DropDetector] finds in it, with everything rotated into display orientation. The Y plane of
 * YUV_420_888 is already luma (grayscale), so no color conversion is needed — only cropping away
 * row-stride padding and equalizing contrast before detection.
 */
class FrameProcessor(private val dropDetector: DropDetector = DropDetector()) {

    fun process(imageProxy: ImageProxy): FrameAnalysisResult {
        val width = imageProxy.width
        val height = imageProxy.height
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val gray = imageProxy.toCroppedGrayMat()
        Imgproc.equalizeHist(gray, gray)

        val streaks = dropDetector.detectStreaks(gray)
            .map { it.rotated(rotationDegrees, width, height) }

        val displayMat = gray.rotated(rotationDegrees)
        if (displayMat !== gray) gray.release()

        val bitmap = Bitmap.createBitmap(displayMat.cols(), displayMat.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(displayMat, bitmap)
        displayMat.release()

        return FrameAnalysisResult(
            previewBitmap = bitmap,
            streaks = streaks,
            displayWidth = bitmap.width,
            displayHeight = bitmap.height
        )
    }

    private fun ImageProxy.toCroppedGrayMat(): Mat {
        val yPlane = planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride

        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val rawMat = Mat(height, rowStride, CvType.CV_8UC1)
        rawMat.put(0, 0, bytes)

        return if (rowStride == width) {
            rawMat
        } else {
            val cropped = Mat(rawMat, Rect(0, 0, width, height)).clone()
            rawMat.release()
            cropped
        }
    }

    private fun Mat.rotated(rotationDegrees: Int): Mat {
        val rotateCode = when (rotationDegrees) {
            90 -> Core.ROTATE_90_CLOCKWISE
            180 -> Core.ROTATE_180
            270 -> Core.ROTATE_90_COUNTERCLOCKWISE
            else -> return this
        }
        val rotated = Mat()
        Core.rotate(this, rotated, rotateCode)
        return rotated
    }
}
