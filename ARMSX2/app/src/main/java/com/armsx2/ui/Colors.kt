package com.armsx2.ui

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color

object Colors {
    val pasx2_blue = Color(0xFF0033CC)

    val bg900 = Color(0x263238)
    val surfaceColor = Color(0xFF272525)
    val surfaceDarkColor = Color(0xFF1B1A1A)
    val surfaceDarkerColor = Color(0xFF111010)
    val secondaryColor = pasx2_blue
    val secondarySurfaceColor = Color(0xff292828)

    val surface = mutableStateOf(surfaceColor)
    val surfaceDark = mutableStateOf(surfaceDarkColor)
    val surfaceDarker = mutableStateOf(surfaceDarkerColor)
    val secondary = mutableStateOf(secondaryColor)
    val secondarySurface = mutableStateOf(secondarySurfaceColor)


    val green = Color(0xFF00ff1a)
    val purple = Color(0xff7e00e1)
    val red = Color(0x0ffff0000)
    val orange = Color(0x0ffff9900)
    val yellow = Color(0x0fffff500)
    val cyan = Color(0x0ff00ffe0)
    val blue = Color(0x0ff0038ff)
    val pink = Color(0x0fffa00ff)

    // Launcher navy background
    val navyTop = Color(0xFF0C2843)
    val navyMid = Color(0xFF081E36)
    val navyBottom = Color(0xFF051326)

    // Accent / active indicator
    val accentCyan = Color(0xFF2FE8DA)

    // Divider over dock
    val dockDivider = Color(0x33FFFFFF)

    // Cover placeholder
    val coverBorder = Color(0xFF0B2A4A)
    val coverPs2Top = Color(0xFF1A6FD9)
    val coverPs2Bottom = Color(0xFF0A2F6B)
    val coverInnerTop = Color(0xFF5B6A85)
    val coverInnerBottom = Color(0xFF2A3A55)
    val coverSelectedGlow = Color(0xFF2FE8DA)
}