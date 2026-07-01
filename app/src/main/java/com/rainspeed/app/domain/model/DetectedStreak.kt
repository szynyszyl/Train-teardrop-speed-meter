package com.rainspeed.app.domain.model

import kotlin.math.atan2

/**
 * A single rain-streak line segment found in one camera frame, in that frame's pixel
 * coordinates.
 */
data class DetectedStreak(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float
) {
    /** Angle from vertical (0° = straight down the glass), in degrees, range (-180, 180]. */
    val angleFromVerticalDegrees: Double
        get() = Math.toDegrees(atan2((x2 - x1).toDouble(), (y2 - y1).toDouble()))

    /** Maps this streak into the coordinate space of the frame rotated clockwise by [rotationDegrees]. */
    fun rotated(rotationDegrees: Int, sourceWidth: Int, sourceHeight: Int): DetectedStreak =
        when (rotationDegrees) {
            90 -> copy(
                x1 = sourceHeight - y1, y1 = x1,
                x2 = sourceHeight - y2, y2 = x2
            )
            180 -> copy(
                x1 = sourceWidth - x1, y1 = sourceHeight - y1,
                x2 = sourceWidth - x2, y2 = sourceHeight - y2
            )
            270 -> copy(
                x1 = y1, y1 = sourceWidth - x1,
                x2 = y2, y2 = sourceWidth - x2
            )
            else -> this
        }
}
