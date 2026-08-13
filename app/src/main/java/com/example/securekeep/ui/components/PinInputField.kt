package com.example.securekeep.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntOffset
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.offset

enum class PinVerificationState {
    NORMAL,
    SUCCESS,
    ERROR
}

@Composable
fun PinInputField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    verificationState: PinVerificationState = PinVerificationState.NORMAL,
    enabled: Boolean = true,
    onComplete: () -> Unit = {},
    onSuccessAnimationFinished: () -> Unit = {},
    onErrorAnimationFinished: () -> Unit = {}
) {
    val pinLength = 6

    val focusRequesters = remember {
        List(pinLength) { FocusRequester() }
    }

    val shakeOffset = remember {
        Animatable(0f)
    }

    var focusedIndex by remember {
        mutableStateOf(0)
    }

    val isDarkMode = isSystemInDarkTheme()

    // Success animation
    LaunchedEffect(verificationState) {
        when (verificationState) {

            PinVerificationState.SUCCESS -> {
                delay(500)
                onSuccessAnimationFinished()
            }

            PinVerificationState.ERROR -> {

                shakeOffset.snapTo(0f)

                shakeOffset.animateTo(
                    targetValue = 10f,
                    animationSpec = tween(70)
                )

                shakeOffset.animateTo(
                    targetValue = -10f,
                    animationSpec = tween(70)
                )

                shakeOffset.animateTo(
                    targetValue = 0f,
                    animationSpec = tween(70)
                )

                onErrorAnimationFinished()
            }

            PinVerificationState.NORMAL -> Unit
        }
    }

    // Automatically verify as soon as the sixth digit is entered.
    LaunchedEffect(value) {
        if (
            value.length == pinLength &&
            verificationState == PinVerificationState.NORMAL
        ) {
            onComplete()
        }
    }

    Row(
        modifier = modifier.offset {
            IntOffset(
                x = shakeOffset.value.roundToInt(),
                y = 0
            )
        },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        repeat(pinLength) { index ->

            val digit = value.getOrNull(index)?.toString() ?: ""

            val borderColor = when {
                verificationState == PinVerificationState.SUCCESS ->
                    Color(0xFF4CAF50)

                verificationState == PinVerificationState.ERROR ->
                    MaterialTheme.colorScheme.error

                focusedIndex == index ->
                    if (isDarkMode) Color.White else Color.Black

                else ->
                    MaterialTheme.colorScheme.outline
            }

            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (focusedIndex == index) 2.dp else 1.dp,
                        color = borderColor,
                        shape = RoundedCornerShape(8.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BasicTextField(
                    value = digit,
                    onValueChange = { input ->

                        if (
                            !enabled ||
                            verificationState != PinVerificationState.NORMAL ||
                            value.length >= pinLength
                        ) {
                            return@BasicTextField
                        }

                        val digitInput = input
                            .filter { it.isDigit() }
                            .takeLast(1)

                        if (digitInput.isNotEmpty()) {

                            val newValue = buildString {
                                append(value.take(index))
                                append(digitInput)
                                append(value.drop(index + 1))
                            }.take(pinLength)

                            onValueChange(newValue)

                            if (index < pinLength - 1) {
                                focusRequesters[index + 1].requestFocus()
                            }

                        } else if (input.isEmpty()) {

                            if (value.isNotEmpty()) {
                                val lastIndex = value.lastIndex

                                val newValue = value.removeRange(
                                    lastIndex,
                                    lastIndex + 1
                                )

                                onValueChange(newValue)

                                focusRequesters[lastIndex].requestFocus()
                                focusedIndex = lastIndex
                            }
                        }
                    },
                    enabled = enabled &&
                            verificationState != PinVerificationState.SUCCESS,
                    singleLine = true,
                    textStyle = TextStyle(
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 20.sp
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged {
                            if (it.isFocused) {
                                focusedIndex = index
                            }
                        }
                        .onPreviewKeyEvent { event ->

                            if (
                                event.key == Key.Backspace &&
                                event.type == KeyEventType.KeyDown &&
                                verificationState == PinVerificationState.NORMAL &&
                                value.isNotEmpty() &&
                                value.length < pinLength
                            ) {
                                val lastIndex = value.lastIndex

                                val newValue = value.removeRange(
                                    lastIndex,
                                    lastIndex + 1
                                )

                                onValueChange(newValue)

                                focusRequesters[lastIndex].requestFocus()
                                focusedIndex = lastIndex

                                true
                            } else {
                                false
                            }
                        }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters.first().requestFocus()
        focusedIndex = 0
    }

    LaunchedEffect(verificationState, value) {
        if (
            verificationState == PinVerificationState.NORMAL &&
            value.isEmpty()
        ) {
            focusRequesters.first().requestFocus()
            focusedIndex = 0
        }
    }
}