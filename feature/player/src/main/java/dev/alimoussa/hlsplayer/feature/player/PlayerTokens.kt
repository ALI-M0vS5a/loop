package dev.alimoussa.hlsplayer.feature.player

import androidx.compose.ui.graphics.Color

internal object PlayerTokens {
    // Google IMA ad accent (used for the scrubber ad-break markers) — kept fixed on purpose.
    val AdYellow = Color(0xFFFFCB2D)

    // Translucent whites for chrome that floats over the video frame.
    val IconButtonBg = Color(0x1FFFFFFF) // rgba(255,255,255,0.12)
    val ScrubberTrack = Color(0x47FFFFFF) // rgba(255,255,255,0.28)
    val HdPillBg = Color(0x29FFFFFF) // rgba(255,255,255,0.16)
}