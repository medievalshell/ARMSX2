package com.armsx2.ui.launcher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsx2.Launcher
import com.armsx2.ui.Colors

val COVER_WIDTH = 88.dp
val COVER_HEIGHT = 122.dp
private val SPINE_WIDTH = 8.dp
private val BANNER_HEIGHT = 14.dp

@Composable
fun GameCover(
    entry: Launcher.GameEntry,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) Colors.coverSelectedGlow else Color(0xFF05101E)
    Box(Modifier.size(COVER_WIDTH, COVER_HEIGHT)) {
        Row(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    rotationY = -14f
                    cameraDistance = 12f * density
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.5f)
                }
                .shadow(elevation = 6.dp, shape = RoundedCornerShape(3.dp))
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF05101E))
                .border(
                    width = if (selected) 2.dp else 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(3.dp)
                )
                .clickable { onClick() }
        ) {
            Spine()
            FrontFace(entry, Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun Spine() {
    Column(
        Modifier
            .fillMaxHeight()
            .width(SPINE_WIDTH)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF14243D), Color(0xFF05101E), Color(0xFF0A1A30))
                )
            )
    ) {
        // Blue cap continuing the front banner
        Box(
            Modifier
                .fillMaxWidth()
                .height(BANNER_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        listOf(Colors.coverPs2Top, Colors.coverPs2Bottom)
                    )
                )
        )
        // Rotated PS2 along the spine
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "PS2",
                color = Color(0xCCFFFFFF),
                fontSize = 5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.rotate(-90f)
            )
        }
    }
}

@Composable
private fun FrontFace(entry: Launcher.GameEntry, modifier: Modifier) {
    Column(modifier.background(Color(0xFF05101E))) {
        // Top PS2 banner
        Box(
            Modifier
                .fillMaxWidth()
                .height(BANNER_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        listOf(Colors.coverPs2Top, Colors.coverPs2Bottom)
                    )
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                "PlayStation.2",
                color = Color.White,
                fontSize = 7.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
        // Cover artwork area (placeholder)
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Colors.coverInnerTop, Colors.coverInnerBottom)
                    )
                )
        ) {
            // Subtle gloss
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            0.0f to Color(0x22FFFFFF),
                            0.5f to Color(0x00FFFFFF),
                            1.0f to Color(0x14000000)
                        )
                    )
            )
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            ) {
                Text(
                    entry.displayName.substringBeforeLast('.', entry.displayName),
                    color = Color(0xFFE8EEF7),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.weight(1f))
                EsrbBox()
            }
        }
    }
}

@Composable
private fun EsrbBox() {
    Box(
        Modifier
            .size(width = 14.dp, height = 16.dp)
            .clip(RoundedCornerShape(1.dp))
            .background(Color.White)
            .border(0.5.dp, Color(0xFF333333), RoundedCornerShape(1.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "T",
                color = Color.Black,
                fontSize = 8.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                "TEEN",
                color = Color.Black,
                fontSize = 3.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
