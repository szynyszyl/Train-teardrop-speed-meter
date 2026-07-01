package com.rainspeed.app.data.vision

import com.rainspeed.app.domain.model.DetectedStreak
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc

/**
 * Detects rain-streak line segments in a single grayscale frame via Canny edge detection
 * followed by a probabilistic Hough transform. Tracking streaks across frames (optical flow)
 * and aggregating their angles happens downstream.
 */
class DropDetector(
    private val cannyThreshold1: Double = 50.0,
    private val cannyThreshold2: Double = 150.0,
    private val houghThreshold: Int = 40,
    private val minLineLength: Double = 15.0,
    private val maxLineGap: Double = 8.0
) {
    fun detectStreaks(gray: Mat): List<DetectedStreak> {
        val edges = Mat()
        val lines = Mat()
        try {
            Imgproc.Canny(gray, edges, cannyThreshold1, cannyThreshold2)
            Imgproc.HoughLinesP(
                edges, lines,
                1.0, Math.PI / 180.0,
                houghThreshold, minLineLength, maxLineGap
            )

            val streaks = ArrayList<DetectedStreak>(lines.rows())
            for (i in 0 until lines.rows()) {
                val segment = lines.get(i, 0)
                streaks += DetectedStreak(
                    x1 = segment[0].toFloat(),
                    y1 = segment[1].toFloat(),
                    x2 = segment[2].toFloat(),
                    y2 = segment[3].toFloat()
                )
            }
            return streaks
        } finally {
            edges.release()
            lines.release()
        }
    }
}
