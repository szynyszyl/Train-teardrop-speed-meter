package com.rainspeed.app.data.vision

import android.graphics.Bitmap
import com.rainspeed.app.domain.model.DetectedStreak

/**
 * Output of processing one camera frame, already rotated into display orientation so the UI
 * layer can draw [streaks] directly over the preview without knowing about sensor rotation.
 */
data class FrameAnalysisResult(
    val previewBitmap: Bitmap,
    val streaks: List<DetectedStreak>,
    val trackedAngles: List<Double>,
    val displayWidth: Int,
    val displayHeight: Int
)
