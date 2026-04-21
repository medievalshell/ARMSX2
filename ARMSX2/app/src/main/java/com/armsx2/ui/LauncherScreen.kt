package com.armsx2.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.armsx2.Launcher
import com.armsx2.Main

@Composable
fun LauncherScreen() {
    Row(
        Modifier
            .fillMaxSize()
            .background(Colors.surface.value)
            .padding(12.dp)
    ) {
        // Left column: controls
        Column(
            Modifier
                .width(360.dp)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                "ARMSX2",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Colors.pasx2_blue
            )
            Spacer(Modifier.height(12.dp))

            Text("BIOS", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            val bios = Launcher.biosName.value
            val biosOk = Launcher.biosReadable.value
            when {
                bios == null -> Text("No BIOS detected", color = Color.Red, fontSize = 13.sp)
                !biosOk -> Text(
                    "Found '$bios'\nbut not readable — re-import it",
                    color = Color(0xFFFFA500), fontSize = 13.sp
                )
                else -> Text("OK: $bios", color = Colors.green, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { Launcher.openBiosPicker() },
                enabled = !Launcher.busy.value
            ) { Text("Import BIOS") }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(Modifier.height(8.dp))

            Text(
                "Games folder",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(2.dp))
            val folder = Launcher.gamesFolderName.value
            Text(
                if (folder != null) "Sel: $folder" else "No folder selected",
                color = if (folder != null) Colors.green else Color.Gray,
                fontSize = 13.sp
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { Launcher.openFolderPicker() },
                enabled = !Launcher.busy.value
            ) { Text("Choose folder") }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = Color.DarkGray)
            Spacer(Modifier.height(8.dp))

            val msg = Launcher.message.value
            if (msg != null) {
                Text(msg, color = Color.Yellow, fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
            }
            Button(
                onClick = {
                    val sel = Launcher.selectedGame.value ?: return@Button
                    Main.startWithGamePath(sel.uri.toString())
                },
                enabled = Launcher.selectedGame.value != null &&
                        Launcher.biosReadable.value &&
                        !Launcher.busy.value
            ) { Text("START GAME") }
        }

        Spacer(Modifier.width(12.dp))

        // Right column: game list
        Column(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth()
        ) {
            val games = Launcher.games.value
            val gameCount = games.size
            val folder = Launcher.gamesFolderName.value
            Text(
                "Games ($gameCount)",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            if (Launcher.busy.value) {
                Text("Scanning…", color = Color.LightGray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))

            val selected = Launcher.selectedGame.value
            val scrollState = rememberScrollState()
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Colors.surfaceDark.value)
                    .verticalScroll(scrollState)
            ) {
                if (gameCount == 0 && !Launcher.busy.value) {
                    Text(
                        if (folder == null) "Choose a folder"
                        else "No PS2 games found",
                        color = Color.LightGray,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        fontSize = 13.sp
                    )
                } else {
                    games.forEach { entry ->
                        val isSel = selected?.uri == entry.uri
                        val bg = if (isSel) Colors.pasx2_blue.copy(alpha = 0.35f) else Color.Transparent
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .background(bg)
                                .clickable { Launcher.selectedGame.value = entry }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(entry.displayName, color = Color.Yellow, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}
