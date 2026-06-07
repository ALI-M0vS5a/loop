package dev.alimoussa.hlsplayer.feature.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import dev.alimoussa.hlsplayer.feature.player.state.rememberControlsVisibilityState
import dev.alimoussa.hlsplayer.feature.player.state.rememberPlaybackState

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    player: Player?,
    adOverlay: View,
    subscribed: Boolean,
    onBack: () -> Unit,
) {
    // While the MediaController is still connecting, show a spinner over black.
    if (player == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(44.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f),
                strokeWidth = 3.dp,
            )
        }
        return
    }

    val playback = rememberPlaybackState(player)
    val controls = rememberControlsVisibilityState(player)
    val activity = LocalContext.current.findActivity()

    var fullscreen by remember { mutableStateOf(false) }

    // ── Transport actions ──
    val togglePlay: () -> Unit = {
        with(player) {
            when {
                playbackState == Player.STATE_ENDED -> {
                    seekTo(0); play()
                }

                isPlaying -> pause()
                else -> play()
            }
        }
        controls.showControls()
    }
    val seekBy: (Long) -> Unit = { deltaMs ->
        val duration = playback.durationMs
        val target = (player.contentPosition + deltaMs)
            .coerceIn(0L, if (duration > 0L) duration else player.contentPosition)
        player.seekTo(target)
        controls.showControls()
    }
    val seekTo: (Float) -> Unit = { fraction ->
        if (playback.durationMs > 0L) player.seekTo((fraction * playback.durationMs).toLong())
    }
    val toggleFullscreen: () -> Unit = {
        activity?.let {
            it.requestedOrientation = if (fullscreen) {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            } else {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            }
            fullscreen = !fullscreen
        }
    }

    val controlsShouldShow = !playback.isAd &&
            playback.playbackState != Player.STATE_IDLE &&
            !playback.isBuffering &&
            (controls.controlsVisible || !playback.isPlaying)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PlayerContentFrame(player = player)

        if (!playback.isAd) {
            // Tap anywhere to toggle the controls (no ripple, full-bleed).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { controls.toggleControlsVisibility() },
            )

            if (playback.isBuffering) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(44.dp)
                        .align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.2f),
                    strokeWidth = 3.dp,
                )
            }

            AnimatedVisibility(visible = controlsShouldShow, enter = fadeIn(), exit = fadeOut()) {
                PlayerControls(
                    subscribed = subscribed,
                    isPlaying = playback.isPlaying,
                    isEnded = playback.isEnded,
                    positionMs = playback.positionMs,
                    durationMs = playback.durationMs,
                    adMarkers = playback.adMarkers,
                    fullscreen = fullscreen,
                    onBack = onBack,
                    onTogglePlay = togglePlay,
                    onRewind = { seekBy(-10_000L) },
                    onForward = { seekBy(10_000L) },
                    onSeekTo = seekTo,
                    onToggleFullscreen = toggleFullscreen,
                )
            }
        }

        // IMA ad-UI overlay, kept on top of the z-order. INVISIBLE during content (so it never
        // steals touches from the Compose controls), VISIBLE while an ad plays.
        AndroidView(
            factory = { adOverlay },
            modifier = Modifier.fillMaxSize(),
            update = { it.visibility = if (playback.isAd) View.VISIBLE else View.INVISIBLE },
        )
    }
}

/** Walks the context wrapper chain to find the hosting [Activity], if any. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
