package com.armsx2.ui.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsx2.Launcher
import com.armsx2.Main
import com.armsx2.R
import com.armsx2.ui.Colors

private val SHELF_HEIGHT = 22.dp
private val COVER_SPACING = 18.dp
private val SHELF_SIDE_PADDING = 18.dp

@Composable
fun GameShelfRow(
    entries: List<Launcher.GameEntry>,
    label: String? = null
) {
    if (entries.size <= 3) {
        ShortShelf(entries, label)
    } else {
        LongShelf(entries, label)
    }
}

@Composable
private fun ShortShelf(entries: List<Launcher.GameEntry>, label: String?) {
    Box(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Box {
            // Covers + per-cover reflections
            Row(
                horizontalArrangement = Arrangement.spacedBy(COVER_SPACING),
                verticalAlignment = Alignment.Top
            ) {
                entries.forEach { entry ->
                    CoverWithReflection(entry, interactive = true)
                }
            }
            // Shelf glass overlay (semi-transparent) on top of reflections.
            // Width = inner Box width = Row width.
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(SHELF_HEIGHT)
            ) {
                ShelfGlass()
                ShelfLabel(label)
            }
        }
    }
}

@Composable
private fun LongShelf(entries: List<Launcher.GameEntry>, label: String?) {
    val scroll = rememberScrollState()
    Box(Modifier.fillMaxWidth()) {
        // Scrollable covers + reflections
        Row(
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = SHELF_SIDE_PADDING),
            horizontalArrangement = Arrangement.spacedBy(COVER_SPACING),
            verticalAlignment = Alignment.Top
        ) {
            entries.forEach { entry ->
                CoverWithReflection(entry, interactive = true)
            }
        }
        // Static shelf glass overlay — screen-wide, never scrolls → truly infinite
        Row(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(SHELF_HEIGHT)
        ) {
            // Left cap: proper shelf ramp, same visual as the short shelf's left edge
            Image(
                painter = painterResource(id = R.drawable.shelf_glass_cap_left),
                contentDescription = null,
                modifier = Modifier
                    .width(50.dp)
                    .height(SHELF_HEIGHT),
                contentScale = ContentScale.FillBounds
            )
            // Body: straight strip extending to the right edge — infinite
            Box(
                Modifier
                    .weight(1f)
                    .height(SHELF_HEIGHT)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.shelf_glass_long),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )
                ShelfLabel(label)
            }
        }
    }
}

@Composable
private fun CoverWithReflection(entry: Launcher.GameEntry, interactive: Boolean) {
    val selected = Launcher.selectedGame.value
    val isSelected = interactive && selected?.uri == entry.uri

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        GameCover(
            entry = entry,
            selected = isSelected,
            onClick = {
                if (!interactive) return@GameCover
                if (selected?.uri == entry.uri) {
                    Main.startWithGamePath(entry.uri.toString())
                } else {
                    Launcher.selectedGame.value = entry
                }
            }
        )
        // Per-cover reflection region — always directly below its cover
        Box(
            Modifier
                .width(COVER_WIDTH)
                .height(SHELF_HEIGHT)
                .clipToBounds()
        ) {
            Box(
                Modifier
                    .requiredSize(COVER_WIDTH, COVER_HEIGHT)
                    .graphicsLayer {
                        scaleY = -1f
                        transformOrigin = TransformOrigin(0.5f, 0f)
                        translationY = COVER_HEIGHT.toPx()
                        alpha = 0.38f
                    }
            ) {
                GameCover(entry = entry, selected = false, onClick = {})
            }
            // Bottom fade
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            1.0f to Colors.navyBottom.copy(alpha = 0.85f)
                        )
                    )
            )
        }
    }
}

@Composable
private fun ShelfGlass() {
    Image(
        painter = painterResource(id = R.drawable.shelf_glass),
        contentDescription = null,
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.FillBounds
    )
}

@Composable
private fun BoxScope.ShelfLabel(label: String?) {
    if (!label.isNullOrEmpty()) {
        Text(
            label,
            color = Color(0xE6FFFFFF),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}
