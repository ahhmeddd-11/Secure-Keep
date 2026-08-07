package com.example.securekeep.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

@Composable
fun SingleClickBackButton(
    tint: Color = Color.Unspecified,
    onBack: () -> Unit
) {
    var clicked by remember {
        mutableStateOf(false)
    }

    IconButton(
        onClick = {

            if (clicked) return@IconButton

            clicked = true

            onBack()
        }
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = tint
        )
    }
}