package com.example.securekeep.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun StatusBarAppearance(
    backgroundColor: Color
) {
    val view = LocalView.current

    if (!view.isInEditMode) {

        SideEffect {

            val window = (view.context as Activity).window

            // Match system bars to the current screen
            window.statusBarColor = backgroundColor.toArgb()
            window.navigationBarColor = backgroundColor.toArgb()

            val controller = WindowCompat.getInsetsController(window, view)

            val darkIcons = backgroundColor.luminance() > 0.5f

            controller?.isAppearanceLightStatusBars = darkIcons
            controller?.isAppearanceLightNavigationBars = darkIcons
        }
    }
}