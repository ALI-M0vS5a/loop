package dev.alimoussa.hlsplayer.feature.player

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.alimoussa.hlsplayer.feature.player.util.formatTime

@Composable
internal fun PlayerControls(
    subscribed: Boolean,
    isPlaying: Boolean,
    isEnded: Boolean,
    positionMs: Long,
    durationMs: Long,
    adMarkers: List<Float>,
    fullscreen: Boolean,
    onBack: () -> Unit,
    onTogglePlay: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    onSeekTo: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // ── Scrims: darken the top & bottom so the controls stay legible over any frame ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent
                        )
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.7f)
                        )
                    ),
                ),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout)),
        ) {
            TopBar(
                subscribed = subscribed,
                onBack = onBack,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 12.dp, end = 12.dp, top = 16.dp),
            )

            CenterTransport(
                isPlaying = isPlaying,
                isEnded = isEnded,
                onTogglePlay = onTogglePlay,
                onRewind = onRewind,
                onForward = onForward,
                modifier = Modifier.align(Alignment.Center),
            )

            BottomBar(
                subscribed = subscribed,
                positionMs = positionMs,
                durationMs = durationMs,
                adMarkers = adMarkers,
                fullscreen = fullscreen,
                onSeekTo = onSeekTo,
                onToggleFullscreen = onToggleFullscreen,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
            )
        }
    }
}

@Composable
private fun TopBar(
    subscribed: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        IconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            size = 42.dp,
            onClick = onBack,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tears of Steel",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = if (subscribed) "HLS · Ad-free" else "HLS · Ad-supported",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // "HD" quality pill
        Text(
            text = "HD",
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(PlayerTokens.HdPillBg)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        )
    }
}

@Composable
private fun CenterTransport(
    isPlaying: Boolean,
    isEnded: Boolean,
    onTogglePlay: () -> Unit,
    onRewind: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(26.dp),
    ) {
        IconButton(
            icon = Icons.Filled.Replay10,
            contentDescription = "Rewind 10 seconds",
            size = 50.dp,
            onClick = onRewind,
        )
        PlayPauseButton(isPlaying = isPlaying, isEnded = isEnded, onClick = onTogglePlay)
        IconButton(
            icon = Icons.Filled.Forward10,
            contentDescription = "Forward 10 seconds",
            size = 50.dp,
            onClick = onForward,
        )
    }
}

@Composable
private fun PlayPauseButton(
    isPlaying: Boolean,
    isEnded: Boolean,
    onClick: () -> Unit,
) {
    val size = 76.dp
    val colorScheme = MaterialTheme.colorScheme
    Box(contentAlignment = Alignment.Center) {
        // Expressive pulsing ring while content is playing.
        if (isPlaying && !isEnded) {
            val transition = rememberInfiniteTransition(label = "pulse")
            val scale by transition.animateFloat(
                initialValue = 0.85f,
                targetValue = 1.5f,
                animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart),
                label = "pulseScale",
            )
            val alpha by transition.animateFloat(
                initialValue = 0.6f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Restart),
                label = "pulseAlpha",
            )
            Box(
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .border(2.dp, colorScheme.primary, CircleShape),
            )
        }

        val interaction = remember { MutableInteractionSource() }
        val pressed by interaction.collectIsPressedAsState()
        val pressScale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "pressScale")
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
                .clip(CircleShape)
                .background(colorScheme.primary)
                .clickable(interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            val icon = when {
                isEnded -> Icons.Filled.Replay
                isPlaying -> Icons.Filled.Pause
                else -> Icons.Filled.PlayArrow
            }
            Icon(
                imageVector = icon,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = colorScheme.onPrimary,
                modifier = Modifier.size(34.dp),
            )
        }
    }
}

@Composable
private fun IconButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = PlayerTokens.IconButtonBg,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.88f else 1f, label = "iconPress")
    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(background)
            .clickable(interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

@Composable
private fun BottomBar(
    subscribed: Boolean,
    positionMs: Long,
    durationMs: Long,
    adMarkers: List<Float>,
    fullscreen: Boolean,
    onSeekTo: (Float) -> Unit,
    onToggleFullscreen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = formatTime(positionMs),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.widthIn(min = 38.dp),
        )
        Scrubber(
            progress = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(
                0f,
                1f
            ) else 0f,
            adMarkers = if (subscribed) emptyList() else adMarkers,
            onSeekTo = onSeekTo,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatTime(durationMs),
            color = Color.White,
            fontSize = 12.sp,
            modifier = Modifier.widthIn(min = 38.dp),
        )
        IconButton(
            icon = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
            contentDescription = "Fullscreen",
            size = 38.dp,
            background = Color.Transparent,
            onClick = onToggleFullscreen,
        )
    }
}

@Composable
private fun Scrubber(
    progress: Float,
    adMarkers: List<Float>,
    onSeekTo: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier
            .height(22.dp)
            .pointerInput(Unit) {
                val widthPx = size.width.toFloat()
                detectTapGestures { offset ->
                    if (widthPx > 0f) onSeekTo((offset.x / widthPx).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                val widthPx = size.width.toFloat()
                detectHorizontalDragGestures { change, _ ->
                    if (widthPx > 0f) onSeekTo((change.position.x / widthPx).coerceIn(0f, 1f))
                }
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        val barWidth = maxWidth
        val thumbSize = 15.dp
        val markerSize = 9.dp
        val primary = MaterialTheme.colorScheme.primary

        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(PlayerTokens.ScrubberTrack),
        )
        // Played portion
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .height(6.dp)
                .clip(CircleShape)
                .background(primary),
        )
        // IMA ad-break markers
        adMarkers.forEach { fraction ->
            Box(
                modifier = Modifier
                    .offset(x = barWidth * fraction - markerSize / 2)
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(PlayerTokens.AdYellow)
                    .border(1.5.dp, Color.Black.copy(alpha = 0.35f), CircleShape),
            )
        }
        // Thumb halo
        Box(
            modifier = Modifier
                .offset(x = barWidth * progress - 13.dp)
                .size(26.dp)
                .clip(CircleShape)
                .background(primary.copy(alpha = 0.25f)),
        )
        // Thumb
        Box(
            modifier = Modifier
                .offset(x = barWidth * progress - thumbSize / 2)
                .size(thumbSize)
                .clip(CircleShape)
                .background(primary),
        )
    }
}

@Preview(name = "Controls · Ad-supported (playing)", widthDp = 360, heightDp = 780)
@Composable
private fun PlayerControlsAdSupportedPreview() {
    PlayerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PlayerControls(
                subscribed = false,
                isPlaying = true,
                isEnded = false,
                positionMs = 7_000L,
                durationMs = 734_000L,
                adMarkers = listOf(0f, 0.12f, 1f),
                fullscreen = false,
                onBack = {},
                onTogglePlay = {},
                onRewind = {},
                onForward = {},
                onSeekTo = {},
                onToggleFullscreen = {},
            )
        }
    }
}

@Preview(name = "Controls · Ad-free (paused)", widthDp = 360, heightDp = 780)
@Composable
private fun PlayerControlsAdFreePreview() {
    PlayerTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            PlayerControls(
                subscribed = true,
                isPlaying = false,
                isEnded = false,
                positionMs = 312_000L,
                durationMs = 734_000L,
                adMarkers = emptyList(),
                fullscreen = false,
                onBack = {},
                onTogglePlay = {},
                onRewind = {},
                onForward = {},
                onSeekTo = {},
                onToggleFullscreen = {},
            )
        }
    }
}