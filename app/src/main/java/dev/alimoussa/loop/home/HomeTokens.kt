package dev.alimoussa.loop.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Vibrant expressive hue pairs used to generate the placeholder media tiles (straight from the
 * design's `TILE_HUES`). Real artwork would replace these, but the colorful gradients keep the
 * carousels lively and honest about being placeholders.
 */
private val TileHues = listOf(
    Color(0xFF6948C6) to Color(0xFFB36AF5), // violet
    Color(0xFFB3266A) to Color(0xFFFF77A9), // magenta
    Color(0xFF0E7C7B) to Color(0xFF3FD9C9), // teal
    Color(0xFFC4631A) to Color(0xFFFFB14E), // amber
    Color(0xFF2A4DB8) to Color(0xFF6FA0FF), // blue
    Color(0xFF1C7A3D) to Color(0xFF5FD988), // green
    Color(0xFF8A2BE2) to Color(0xFFC77DFF), // purple
    Color(0xFFB81E45) to Color(0xFFFF6E85), // red-pink
    Color(0xFF0F6FB8) to Color(0xFF56C2FF), // sky
    Color(0xFF7A4E0E) to Color(0xFFE0A95A), // bronze
)

/** Diagonal two-tone gradient placeholder fill for tile [index] (shown under/while images load). */
fun tileBrush(index: Int): Brush {
    val (start, end) = TileHues[index % TileHues.size]
    return Brush.linearGradient(
        colors = listOf(start, end),
        start = Offset.Zero,
        end = Offset.Infinite,
    )
}

/**
 * A real image URL for a carousel tile, sized to the tile's orientation. Uses the deterministic
 * Lorem Picsum service (seeded per tile, so each one is distinct and stable across launches).
 *  - vertical   -> 2:3 poster
 *  - horizontal -> 16:9 thumbnail
 */
fun tileImageUrl(vertical: Boolean, index: Int): String {
    val seed = if (vertical) "hls-v-$index" else "hls-h-$index"
    return if (vertical) {
        "https://picsum.photos/seed/$seed/300/450"
    } else {
        "https://picsum.photos/seed/$seed/480/270"
    }
}
