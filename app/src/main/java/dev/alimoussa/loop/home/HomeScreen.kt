package dev.alimoussa.loop.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import dev.alimoussa.loop.ui.theme.LoopTheme

/** Number of tiles in each carousel, per the spec. */
private const val ITEMS_PER_CAROUSEL = 10

/**
 * Home page (recreates the `home.jsx` design): a persisted subscription switch and two
 * horizontally-scrolling carousels — 10 vertical posters and 10 horizontal thumbnails. Tapping
 * any tile opens the player.
 *
 * @param subscribed       current persisted subscription state.
 * @param onSubscribedChange called when the user toggles the switch (the caller persists it).
 * @param onOpenPlayer     called when any tile is tapped, to launch the player.
 */
@Composable
fun HomeScreen(
    subscribed: Boolean,
    onSubscribedChange: (Boolean) -> Unit,
    onOpenPlayer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Header()
        SubscriptionCard(
            subscribed = subscribed,
            onSubscribedChange = onSubscribedChange,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        )
        Carousel(
            title = "Featured posters",
            orientation = TileOrientation.Vertical,
            onOpen = onOpenPlayer,
        )
        Carousel(
            title = "Continue watching",
            orientation = TileOrientation.Horizontal,
            onOpen = onOpenPlayer,
        )
    }
}

// ── Header (expressive large title + floating decorative shapes) ─────────────────
@Composable
private fun Header() {
    val colors = MaterialTheme.colorScheme
    // Two organic "blobs" that gently float — pure decoration, the M3 Expressive flourish.
    val transition = rememberInfiniteTransition(label = "blobs")
    val floatA by transition.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(3500), RepeatMode.Reverse),
        label = "floatA",
    )
    val floatB by transition.animateFloat(
        initialValue = 0f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(4500), RepeatMode.Reverse),
        label = "floatB",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clip(RoundedCornerShape(0.dp)),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-26).dp, y = (-26).dp + floatA.dp)
                .size(96.dp)
                .clip(RoundedCornerShape(48.dp))
                .background(colors.tertiaryContainer),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = (-96).dp, y = 30.dp + floatB.dp)
                .size(44.dp)
                .clip(CircleShape)
                .background(colors.secondaryContainer),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 20.dp, bottom = 6.dp),
        ) {
            Text(
                text = "STREAM",
                color = colors.tertiary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp,
            )
            Text(
                text = "Watch",
                color = colors.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ── Subscription card (with the persisted switch) ────────────────────────────────
@Composable
private fun SubscriptionCard(
    subscribed: Boolean,
    onSubscribedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val cardBg by animateColorAsState(
        if (subscribed) colors.primaryContainer else colors.surfaceContainerHigh,
        animationSpec = tween(320),
        label = "cardBg",
    )
    // The star badge morphs from a rounded square (not subscribed) to a circle (subscribed).
    val badgeRadius by animateDpAsState(
        if (subscribed) 22.dp else 16.dp,
        tween(360),
        label = "badge"
    )
    val badgeBg by animateColorAsState(
        if (subscribed) colors.primary else colors.outline,
        animationSpec = tween(320),
        label = "badgeBg",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cardBg)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(badgeRadius))
                .background(badgeBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(24.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (subscribed) "Subscribed" else "Not subscribed",
                color = if (subscribed) colors.onPrimaryContainer else colors.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (subscribed) "Ad-free playback" else "Ads play before, during & after",
                color = if (subscribed) colors.onPrimaryContainer else colors.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        M3Switch(checked = subscribed, onCheckedChange = onSubscribedChange)
    }
}

/** Custom Material 3 Expressive switch with a springy, growing thumb and a check when on. */
@Composable
private fun M3Switch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val trackBg by animateColorAsState(
        if (checked) colors.primary else colors.surfaceContainerHighest,
        animationSpec = tween(220),
        label = "track",
    )
    val borderColor by animateColorAsState(
        if (checked) colors.primary else colors.outline,
        animationSpec = tween(220),
        label = "border",
    )
    val thumbOffset by animateDpAsState(if (checked) 22.dp else 4.dp, tween(280), label = "thumbX")
    val thumbSize by animateDpAsState(
        if (checked) 24.dp else 16.dp,
        tween(280),
        label = "thumbSize"
    )

    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 32.dp)
            .clip(CircleShape)
            .background(trackBg)
            .border(2.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onCheckedChange(!checked) },
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .clip(CircleShape)
                .background(if (checked) colors.onPrimary else colors.outline),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

// ── Carousels ────────────────────────────────────────────────────────────────────
private enum class TileOrientation { Vertical, Horizontal }

@Composable
private fun Carousel(
    title: String,
    orientation: TileOrientation,
    onOpen: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(top = 24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Colored accent bar distinguishes the two sections.
                Box(
                    modifier = Modifier
                        .size(width = 5.dp, height = 20.dp)
                        .clip(CircleShape)
                        .background(if (orientation == TileOrientation.Vertical) colors.primary else colors.tertiary),
                )
                Spacer(Modifier.width(9.dp))
                Text(
                    text = title,
                    color = colors.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                )
            }
            Text(
                text = "$ITEMS_PER_CAROUSEL items",
                color = colors.onSurfaceVariant,
                fontSize = 13.sp,
            )
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(ITEMS_PER_CAROUSEL) { index ->
                MediaTile(index = index, orientation = orientation, onOpen = onOpen)
            }
        }
    }
}

/** A single media tile: a real poster/thumbnail with springy press feedback (scale + radius morph). */
@Composable
private fun MediaTile(
    index: Int,
    orientation: TileOrientation,
    onOpen: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val vertical = orientation == TileOrientation.Vertical
    val width = if (vertical) 132.dp else 256.dp
    val height = if (vertical) 198.dp else 144.dp

    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(240), label = "tileScale")
    val radius by animateDpAsState(
        targetValue = when {
            pressed && vertical -> 28.dp
            pressed -> 20.dp
            vertical -> 20.dp
            else -> 16.dp
        },
        animationSpec = tween(320),
        label = "tileRadius",
    )

    Box(
        modifier = Modifier
            .size(width = width, height = height)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(radius))
            // Gradient sits under the image as a graceful placeholder while it loads (or if
            // offline); the hue is offset so the two carousels don't repeat the same colors.
            .background(tileBrush(if (vertical) index else index + 3))
            .clickable(interaction, indication = null, onClick = onOpen),
    ) {
        // The real poster / thumbnail, cropped to fill the tile.
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(tileImageUrl(vertical = vertical, index = index))
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )

        // Aspect-ratio + number tag. The scrim under it is always dark (over arbitrary images),
        // so the text is a fixed light color rather than a theme on-color that would flip in dark.
        Text(
            text = "${if (vertical) "2:3" else "16:9"} · ${
                (index + 1).toString().padStart(2, '0')
            }",
            color = Color.White,
            fontSize = 10.sp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(10.dp)
                .clip(CircleShape)
                .background(colors.scrim.copy(alpha = 0.4f))
                .padding(horizontal = 7.dp, vertical = 3.dp),
        )
        // Play affordance — brand-colored so the carousels carry the M3 Expressive identity.
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .size(34.dp)
                .clip(CircleShape)
                .background(colors.primary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = "Play",
                tint = colors.onPrimary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Preview(name = "Home", widthDp = 360, heightDp = 780)
@Composable
private fun HomeScreenPreview() {
    LoopTheme {
        HomeScreen(subscribed = false, onSubscribedChange = {}, onOpenPlayer = {})
    }
}
