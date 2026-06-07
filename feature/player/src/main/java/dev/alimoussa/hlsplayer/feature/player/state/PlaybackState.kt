package dev.alimoussa.hlsplayer.feature.player.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.listen
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

/** Creates and remembers a [PlaybackState] that observes [player] for the lifetime of the call. */
@UnstableApi
@Composable
internal fun rememberPlaybackState(player: Player): PlaybackState {
    val state = remember(player) { PlaybackState(player) }
    LaunchedEffect(player) { state.observe() }
    return state
}

/**
 * Observes a [Player] and exposes only the slices of playback the custom controls render:
 * whether content is playing, buffering or ended, whether an ad is on screen, the content
 * position/duration, and the real Google IMA ad-break markers (0..1 fractions of the content).
 *
 * Keeping this here — rather than as loose `mutableStateOf`s inside the screen — mirrors
 * NextPlayer's state-holder pattern and keeps `PlayerScreen` purely about layout.
 */
@Stable
@androidx.annotation.OptIn(UnstableApi::class)
internal class PlaybackState(private val player: Player) {

    var isPlaying: Boolean by mutableStateOf(false)
        private set
    var playbackState: Int by mutableIntStateOf(Player.STATE_IDLE)
        private set
    var isAd: Boolean by mutableStateOf(false)
        private set
    var positionMs: Long by mutableLongStateOf(0L)
        private set
    var durationMs: Long by mutableLongStateOf(0L)
        private set
    var adMarkers: List<Float> by mutableStateOf(emptyList())
        private set

    /** True only while content (not an ad) is buffering, so we don't spin over the ad UI. */
    val isBuffering: Boolean get() = playbackState == Player.STATE_BUFFERING && !isAd
    val isEnded: Boolean get() = playbackState == Player.STATE_ENDED

    suspend fun observe() {
        // Seed the initial snapshot, then update each field only on the event that changes it
        // (NextPlayer's approach) — cheaper and clearer than re-reading everything every time.
        isPlaying = player.isPlaying
        playbackState = player.playbackState
        updateAdState()
        updateDuration()
        updatePosition()

        coroutineScope {
            launch {
                player.listen { events ->
                    // Qualified with this@PlaybackState because `listen`'s lambda receiver is the
                    // Player, whose own isPlaying/playbackState are read-only and would shadow ours.
                    if (events.contains(Player.EVENT_IS_PLAYING_CHANGED)) {
                        this@PlaybackState.isPlaying = player.isPlaying
                    }
                    if (events.contains(Player.EVENT_PLAYBACK_STATE_CHANGED)) {
                        this@PlaybackState.playbackState = player.playbackState
                    }
                    if (events.containsAny(
                            Player.EVENT_TIMELINE_CHANGED,
                            Player.EVENT_MEDIA_ITEM_TRANSITION,
                            Player.EVENT_PLAYBACK_STATE_CHANGED,
                        )
                    ) {
                        updateDuration()
                    }
                    if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) updatePosition()
                    updateAdState()
                }
            }
            // Poll the smoothly-advancing content position while playing.
            while (true) {
                delay(POLL_INTERVAL_MS.milliseconds)
                if (player.isPlaying) updatePosition()
            }
        }
    }

    private fun updatePosition() {
        positionMs = player.contentPosition.coerceAtLeast(0L)
    }

    private fun updateDuration() {
        durationMs = player.contentDuration.let { if (it == C.TIME_UNSET) 0L else it }
        adMarkers = player.adGroupMarkers(durationMs)
    }

    private fun updateAdState() {
        isAd = player.isPlayingAd
    }

    private companion object {
        const val POLL_INTERVAL_MS = 500L
    }
}

/**
 * Reads the real IMA ad-break cue points from the current timeline as fractions (0f..1f) of the
 * content duration — used to paint the yellow scrubber markers. A post-roll reports
 * [C.TIME_END_OF_SOURCE], which maps to the very end (1f).
 */
@UnstableApi
private fun Player.adGroupMarkers(contentDurationMs: Long): List<Float> {
    if (contentDurationMs <= 0L) return emptyList()
    val timeline = currentTimeline
    if (timeline.isEmpty) return emptyList()
    val period = timeline.getPeriod(currentPeriodIndex, Timeline.Period())
    val markers = ArrayList<Float>(period.adGroupCount)
    for (i in 0 until period.adGroupCount) {
        val timeUs = period.getAdGroupTimeUs(i)
        val fraction = if (timeUs == C.TIME_END_OF_SOURCE) {
            1f
        } else {
            (timeUs / 1000f / contentDurationMs).coerceIn(0f, 1f)
        }
        markers.add(fraction)
    }
    return markers
}
