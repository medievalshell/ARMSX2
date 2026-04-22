package com.armsx2.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsx2.Launcher
import com.armsx2.ui.launcher.Dock
import com.armsx2.ui.launcher.DockState
import com.armsx2.ui.launcher.GameShelfRow
import compose.icons.LineAwesomeIcons
import compose.icons.lineawesomeicons.AngleDownSolid

private const val RECENT_MAX = 5

private val LETTER_CHUNKS = listOf(
    "A–D" to ('A'..'D'),
    "E–H" to ('E'..'H'),
    "I–L" to ('I'..'L'),
    "M–P" to ('M'..'P'),
    "Q–T" to ('Q'..'T'),
    "U–X" to ('U'..'X'),
    "Y–Z" to ('Y'..'Z')
)

@Composable
fun LauncherScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Colors.navyTop, Colors.navyMid, Colors.navyBottom)
                )
            )
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (DockState.selected.intValue) {
                    DockState.LIBRARY -> LibraryContent()
                    DockState.SEARCH -> PlaceholderContent("Search")
                    DockState.SETTINGS -> SettingsContent()
                }
            }
            Dock()
        }
    }
}

@Composable
private fun LibraryContent() {
    val games = Launcher.games.value
    val folder = Launcher.gamesFolderName.value
    val busy = Launcher.busy.value
    // Read lastPlayed so recomposition triggers when a game is marked played
    Launcher.lastPlayed.value
    val recent = Launcher.recentGames(RECENT_MAX)

    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(top = 16.dp, bottom = 8.dp)
    ) {
        SectionHeader("Currently playing")
        Spacer(Modifier.height(8.dp))
        if (recent.isNotEmpty()) {
            GameShelfRow(entries = recent)
        } else {
            EmptyShelf(folder = folder, busy = busy)
        }

        Spacer(Modifier.height(18.dp))

        SectionHeader("Library")
        Spacer(Modifier.height(8.dp))
        if (games.isEmpty()) {
            EmptyShelf(folder = folder, busy = busy)
        } else {
            val sorted = games.sortedBy { it.displayName.lowercase() }
            val nonAlpha = sorted.filter {
                val c = it.displayName.firstOrNull()?.uppercaseChar()
                c == null || !c.isLetter()
            }
            if (nonAlpha.isNotEmpty()) {
                GameShelfRow(entries = nonAlpha, label = "#")
                Spacer(Modifier.height(14.dp))
            }
            LETTER_CHUNKS.forEach { (label, range) ->
                val inRange = sorted.filter {
                    val c = it.displayName.firstOrNull()?.uppercaseChar()
                    c != null && c in range
                }
                if (inRange.isNotEmpty()) {
                    GameShelfRow(entries = inRange, label = label)
                    Spacer(Modifier.height(14.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(6.dp))
        Image(
            LineAwesomeIcons.AngleDownSolid,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            colorFilter = ColorFilter.tint(Color.White)
        )
    }
}

@Composable
private fun EmptyShelf(folder: String?, busy: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            when {
                busy -> "Scanning…"
                folder == null -> "Open Settings to choose your games folder"
                else -> "No PS2 games found"
            },
            color = Color(0xCCFFFFFF),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun PlaceholderContent(title: String) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$title — coming soon",
            color = Color(0xCCFFFFFF),
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SettingsContent() {
    val scroll = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(20.dp)
    ) {
        Text(
            "Settings",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(16.dp))

        SettingsRow(
            label = "BIOS",
            value = when {
                Launcher.biosName.value == null -> "not imported"
                !Launcher.biosReadable.value -> "unreadable: ${Launcher.biosName.value}"
                else -> Launcher.biosName.value!!
            },
            ok = Launcher.biosReadable.value,
            buttonText = "Import BIOS",
            enabled = !Launcher.busy.value,
            onClick = { Launcher.openBiosPicker() }
        )
        Spacer(Modifier.height(14.dp))
        SettingsRow(
            label = "Games folder",
            value = Launcher.gamesFolderName.value ?: "not selected",
            ok = Launcher.gamesFolderName.value != null,
            buttonText = "Choose folder",
            enabled = !Launcher.busy.value,
            onClick = { Launcher.openFolderPicker() }
        )

        val msg = Launcher.message.value
        if (msg != null) {
            Spacer(Modifier.height(16.dp))
            Text(msg, color = Color(0xFFFFE082), fontSize = 13.sp)
        }
    }
}

@Composable
private fun SettingsRow(
    label: String,
    value: String,
    ok: Boolean,
    buttonText: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x22FFFFFF))
            .padding(14.dp)
    ) {
        Text(
            label,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            value,
            color = if (ok) Colors.accentCyan else Color(0xFFFFB4B4),
            fontSize = 12.sp
        )
        Spacer(Modifier.height(10.dp))
        Button(onClick = onClick, enabled = enabled) {
            Text(buttonText)
        }
    }
}
