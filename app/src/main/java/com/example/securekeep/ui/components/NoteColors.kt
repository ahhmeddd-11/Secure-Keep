package com.example.securekeep.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

// Dark aesthetic palette
val NoteColors = listOf(
    Color(0xFF202124), // Charcoal (Default)
    Color(0xFF3C4043), // Steel
    Color(0xFF17474F), // Deep Teal
    Color(0xFF3E2723), // Deep Brown
    Color(0xFF1A237E), // Navy Blue
    Color(0xFF4A148C), // Deep Purple
    Color(0xFF004D40)  // Dark Jungle Green
)

fun getContrastingTextColor(backgroundColor: Color, isDarkTheme: Boolean): Color {
    val argb = backgroundColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(argb)
    // For dark aesthetic colors, we usually want white text. 
    // If luminance is high (unlikely in this palette), use black.
    return if (luminance > 0.4) Color.Black else Color.White
}
