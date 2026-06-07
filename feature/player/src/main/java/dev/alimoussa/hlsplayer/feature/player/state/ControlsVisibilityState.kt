package dev.alimoussa.hlsplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.media3.common.Player
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Default time the controls linger after content starts playing. */
private val DEFAULT_HIDE_AFTER = 3200.milliseconds

/** Creates and remembers a [ControlsVisibilityState] that observes [player]. */
@UnstableApi
@Composable
internal fun rememberControlsVisibilityState(
    player: Player,
    hideAfter: Duration = DEFAULT_HIDE_AFTER,
): ControlsVisibilityState {
    val scope = rememberCoroutineScope()
    val state = remember(player) { ControlsVisibilityState(player, hideAfter, scope) }
    LaunchedEffect(player) { state.observe() }
    return state
}

/**
 * Owns whether the custom controls overlay is visible, and the auto-hide timer — mirroring
 * NextPlayer's `ControlsVisibilityState`. The holder watches the [player] itself: each time
 * playback starts it schedules an auto-hide, and the hide only fires if the player is still
 * playing (so paused content keeps its controls on screen without the screen having to coordinate).
 */
@Stable
@androidx.annotation.OptIn(UnstableApi::class)
internal class ControlsVisibilityState(
    private val player: Player,
    private val hideAfter: Duration,
    private val scope: CoroutineScope,
) {
    private var autoHideJob: Job? = null

    var controlsVisible: Boolean by mutableStateOf(true)
        private set

    fun showControls(duration: Duration = hideAfter) {
        controlsVisible = true
        autoHideControls(duration)
    }

    fun hideControls() {
        autoHideJob?.cancel()
        controlsVisible = false
    }

    fun toggleControlsVisibility() {
        if (controlsVisible) hideControls() else showControls()
    }

    suspend fun observe() {
        player.listen { events ->
            if (events.contains(Player.EVENT_IS_PLAYING_CHANGED) && player.isPlaying) {
                autoHideControls()
            }
        }
    }

    private fun autoHideControls(duration: Duration = hideAfter) {
        autoHideJob?.cancel()
        autoHideJob = scope.launch {
            delay(duration)
            if (player.isPlaying) controlsVisible = false
        }
    }
}
