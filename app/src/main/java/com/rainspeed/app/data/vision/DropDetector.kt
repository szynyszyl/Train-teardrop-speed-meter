package com.rainspeed.app.data.vision

import com.rainspeed.app.domain.model.DetectedStreak
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.imgproc.Imgproc
import org.opencv.video.Video

/**
 * Detects rain-streak line segments in a single grayscale frame via Canny edge detection
 * followed by a probabilistic Hough transform, and tracks feature points between consecutive
 * frames via Lucas-Kanade optical flow to recover the streaks' slide direction. Both signals
 * yield line-segment angles; aggregating them happens downstream in AngleAggregator.
 */
class DropDetector(
    private val cannyThreshold1: Double = 50.0,
    private val cannyThreshold2: Double = 150.0,
    private val houghThreshold: Int = 40,
    private val minLineLength: Double = 15.0,
    private val maxLineGap: Double = 8.0,
    private val maxTrackedCorners: Int = 60,
    private val cornerQualityLevel: Double = 0.01,
    private val minCornerDistance: Double = 10.0,
    private val minTrackedDisplacementPx: Double = 2.0
) {
    private var previousGray: Mat? = null
    private var previousPoints: MatOfPoint2f? = null

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

    /**
     * Tracks feature points from the previous call's frame into [gray] via calcOpticalFlowPyrLK
     * and returns each successfully tracked point's motion as a [DetectedStreak] vector (from its
     * previous position to its new one). Returns an empty list on the first call, since there is
     * no previous frame yet to track from.
     */
    fun trackMotion(gray: Mat): List<DetectedStreak> {
        val prevGray = previousGray
        val prevPoints = previousPoints

        val motionVectors = if (prevGray != null && prevPoints != null && prevPoints.rows() > 0) {
            opticalFlowVectors(prevGray, gray, prevPoints)
        } else {
            emptyList()
        }

        previousGray?.release()
        previousPoints?.release()
        previousGray = gray.clone()
        previousPoints = detectCorners(gray)

        return motionVectors
    }

    /** Releases internal state held for tracking; call when detection stops (e.g. lifecycle end). */
    fun reset() {
        previousGray?.release()
        previousPoints?.release()
        previousGray = null
        previousPoints = null
    }

    private fun detectCorners(gray: Mat): MatOfPoint2f {
        val corners = MatOfPoint()
        Imgproc.goodFeaturesToTrack(gray, corners, maxTrackedCorners, cornerQualityLevel, minCornerDistance)
        return MatOfPoint2f(*corners.toArray()).also { corners.release() }
    }

    private fun opticalFlowVectors(
        prevGray: Mat,
        currGray: Mat,
        prevPoints: MatOfPoint2f
    ): List<DetectedStreak> {
        val nextPoints = MatOfPoint2f()
        val status = MatOfByte()
        val err = MatOfFloat()
        try {
            Video.calcOpticalFlowPyrLK(prevGray, currGray, prevPoints, nextPoints, status, err)

            val prevArr = prevPoints.toArray()
            val nextArr = nextPoints.toArray()
            val statusArr = status.toArray()

            val vectors = ArrayList<DetectedStreak>(prevArr.size)
            for (i in prevArr.indices) {
                if (i >= statusArr.size || statusArr[i].toInt() != 1) continue
                val prev = prevArr[i]
                val next = nextArr[i]
                val dx = next.x - prev.x
                val dy = next.y - prev.y
                if (Math.hypot(dx, dy) < minTrackedDisplacementPx) continue
                vectors += DetectedStreak(
                    x1 = prev.x.toFloat(), y1 = prev.y.toFloat(),
                    x2 = next.x.toFloat(), y2 = next.y.toFloat()
                )
            }
            return vectors
        } finally {
            nextPoints.release()
            status.release()
            err.release()
        }
    }
}
