package com.example.securekeep.ui.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

fun getContrastingTextColor(
    backgroundColor: Color,
    isDarkTheme: Boolean
): Color {
    return if (backgroundColor.luminance() > 0.5f) {
        Color.Black
    } else {
        Color.White
    }
}