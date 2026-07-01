package com.rainspeed.app.data.vision

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Rect
import org.opencv.imgproc.Imgproc

/**
 * Converts a camera frame to a contrast-enhanced grayscale [Bitmap] using OpenCV, proving the
 * OpenCV pipeline works end to end. The Y plane of YUV_420_888 is already luma (grayscale), so no
 * color conversion is needed — only cropping away row-stride padding and equalizing contrast.
 */
object GrayscaleFrameProcessor {

    fun process(imageProxy: ImageProxy): Bitmap {
        val yPlane = imageProxy.planes[0]
        val buffer = yPlane.buffer
        val rowStride = yPlane.rowStride
        val width = imageProxy.width
        val height = imageProxy.height

        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val rawMat = Mat(height, rowStride, CvType.CV_8UC1)
        rawMat.put(0, 0, bytes)

        val gray = if (rowStride == width) {
            rawMat
        } else {
            val cropped = Mat(rawMat, Rect(0, 0, width, height)).clone()
            rawMat.release()
            cropped
        }

        Imgproc.equalizeHist(gray, gray)

        val bitmap = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(gray, bitmap)
        gray.release()

        return bitmap
    }
}
