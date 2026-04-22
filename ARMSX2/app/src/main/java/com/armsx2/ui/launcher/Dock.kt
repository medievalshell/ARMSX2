package com.armsx2.ui.launcher

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import com.armsx2.ui.Colors
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.CogSolid
import compose.icons.lineawesomeicons.SearchSolid
import compose.icons.lineawesomeicons.ThSolid

object DockState {
    val selected = mutableIntStateOf(0)
    const val LIBRARY = 0
    const val SEARCH = 1
    const val SETTINGS = 2
}

@Composable
fun Dock() {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Colors.dockDivider)
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DockTab(
                icon = LineAwesomeIcons.ThSolid,
                label = "LIBRARY",
                active = DockState.selected.intValue == DockState.LIBRARY,
                showLabel = true,
                onClick = { DockState.selected.intValue = DockState.LIBRARY }
            )
            DockTab(
                icon = LineAwesomeIcons.SearchSolid,
                label = "",
                active = DockState.selected.intValue == DockState.SEARCH,
                onClick = { DockState.selected.intValue = DockState.SEARCH }
            )
            DockTab(
                icon = LineAwesomeIcons.CogSolid,
                label = "",
                active = DockState.selected.intValue == DockState.SETTINGS,
                onClick = { DockState.selected.intValue = DockState.SETTINGS }
            )
        }
    }
}

@Composable
private fun DockTab(
    icon: ImageVector,
    label: String,
    active: Boolean,
    showLabel: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Image(
            icon,
            contentDescription = label.ifEmpty { null },
            modifier = Modifier.size(26.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
        if (showLabel && label.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .width(if (active) 18.dp else 0.dp)
                .height(2.dp)
                .background(if (active) Colors.accentCyan else Color.Transparent)
        )
    }
}
