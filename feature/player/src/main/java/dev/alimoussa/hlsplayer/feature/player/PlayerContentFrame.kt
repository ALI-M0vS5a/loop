package dev.alimoussa.hlsplayer.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState

/**
 * The video frame: the Compose-native [PlayerSurface] (NextPlayer-style) letterboxed to the
 * video's aspect ratio, with a black "shutter" held over it until the first frame is ready so we
 * never flash a stale or uninitialized buffer.
 */
@UnstableApi
@Composable
internal fun PlayerContentFrame(
    player: Player,
    modifier: Modifier = Modifier,
) {
    val presentationState = rememberPresentationState(player)

    PlayerSurface(
        player = player,
        surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        modifier = modifier
            .fillMaxSize()
            .resizeWithContentScale(ContentScale.Fit, presentationState.videoSizeDp),
    )

    if (presentationState.coverSurface) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        )
    }
}