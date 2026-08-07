package com.example.securekeep.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.securekeep.data.local.Note
import com.example.securekeep.ui.components.NoteColors
import com.example.securekeep.ui.components.SingleClickBackButton
import com.example.securekeep.ui.components.getContrastingTextColor
import com.example.securekeep.viewmodel.NotesViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun AddEditNoteScreen(
    viewModel: NotesViewModel,
    noteId: Int?,
    searchQuery: String? = null,
    onBack: () -> Unit
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    var currentNoteId by remember { mutableStateOf(noteId ?: -1) }
    
    var title by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var selectedColor by remember { mutableStateOf(NoteColors[0]) }
    var isPinned by remember { mutableStateOf(false) }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLocked by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }
    
    var initialNote by remember { mutableStateOf<Note?>(null) }
    var lastEditedTimestamp by remember { mutableStateOf<Long?>(null) }
    var isInitialLoad by remember { mutableStateOf(true) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    var textLayoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }
    var hasScrolledToSearch by remember { mutableStateOf(false) }
    var textFieldTopY by remember { mutableStateOf(0f) }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    LaunchedEffect(noteId) {
        if (noteId != null && noteId != -1) {
            val note = viewModel.getNoteById(noteId)
            note?.let {
                initialNote = it
                lastEditedTimestamp = it.timestamp
                title = it.title
                contentValue = TextFieldValue(it.content)
                selectedColor = Color(it.color.toInt())
                isPinned = it.isPinned
                isLocked = it.isLocked
                imageUri = it.imageUri?.let { uri -> Uri.parse(uri) }
                
                if (!searchQuery.isNullOrBlank()) {
                    val idx = it.content.indexOf(searchQuery, ignoreCase = true)
                    if (idx >= 0) {
                        contentValue = TextFieldValue(
                            text = it.content,
                            selection = TextRange(idx, idx + searchQuery.length)
                        )
                        // Selection change natively triggers active view bounds adjustment inside LaunchedEffect below
                    }
                }
            }
        }
        isInitialLoad = false
    }

    // AUTO-SAVE LOGIC
    LaunchedEffect(title, contentValue.text, selectedColor, isPinned, imageUri, isLocked) {
        if (!isInitialLoad) {
            val isEdited = initialNote == null || 
                title != initialNote!!.title || 
                contentValue.text != initialNote!!.content ||
                selectedColor.toArgb().toLong() != initialNote!!.color ||
                isPinned != initialNote!!.isPinned ||
                isLocked != initialNote!!.isLocked ||
                imageUri?.toString() != initialNote!!.imageUri

            val isEmpty = title.trim().isEmpty() && contentValue.text.trim().isEmpty() && imageUri == null

            if (isEmpty && currentNoteId == -1) {
                // Do not create DB entry for empty note
            } else if (isEdited) {
                kotlinx.coroutines.delay(500) // Debounce auto-save to prevent race conditions
                val newTimestamp = System.currentTimeMillis()
                lastEditedTimestamp = newTimestamp
                
                val note = Note(
                    id = if (currentNoteId != -1) currentNoteId else 0,
                    title = title,
                    content = contentValue.text,
                    color = selectedColor.toArgb().toLong(),
                    isPinned = isPinned,
                    isLocked = isLocked,
                    lockedName = initialNote?.lockedName,
                    isDeleted = false,
                    imageUri = imageUri?.toString(),
                    timestamp = newTimestamp
                )
                
                coroutineScope.launch {
                    val newId = viewModel.insert(note)
                    if (currentNoteId == -1) currentNoteId = newId
                }
            }
        }
    }

    // Keyboard collision avoidance that actively dodges upward animations
    LaunchedEffect(contentValue.selection, textLayoutResult, imeBottom) {
        if (!isInitialLoad && textLayoutResult != null) {
            delay(100) // Buffer for asynchronous keyboard measurements
            try {
                val cursorRect = textLayoutResult!!.getCursorRect(contentValue.selection.max)
                // Let the native matrix dynamically scale the view instead of crashing on artificial boundary overlaps
                bringIntoViewRequester.bringIntoView(cursorRect.copy(bottom = cursorRect.bottom + 60f))
            } catch (e: Exception) {
                // Ignore transient layout shifts
            }
        }
    }

    // Mathematically perfect scroll behavior for loading specific searched queries using absolute hierarchy alignment
    LaunchedEffect(textLayoutResult, searchQuery, textFieldTopY) {
        if (textLayoutResult != null && textFieldTopY > 0f && !searchQuery.isNullOrBlank() && !hasScrolledToSearch) {
            try {
                val idx = contentValue.text.indexOf(searchQuery, ignoreCase = true)
                if (idx >= 0) {
                    val line = textLayoutResult!!.getLineForOffset(idx)
                    val lineTop = textLayoutResult!!.getLineTop(line)
                    val absoluteY = textFieldTopY + lineTop
                    scrollState.animateScrollTo(absoluteY.toInt() - 50) // Tiny top cushion for perfect centering
                    hasScrolledToSearch = true
                }
            } catch (e: Exception) {}
        }
    }

    val backgroundColor = selectedColor
    val textColor = getContrastingTextColor(selectedColor, isDarkTheme)
    val themeBasedIconColor = if (isDarkTheme) Color.White else Color.Black

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(), // Move whole scaffold up
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    SingleClickBackButton(
                        onBack = onBack,
                        tint = textColor
                    )
                },
                actions = {
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            imageVector = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin",
                            tint = textColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .windowInsetsPadding(BottomAppBarDefaults.windowInsets)
                    .padding(vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.padding(start = 4.dp).size(40.dp).border(1.dp, themeBasedIconColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Outlined.Image, contentDescription = "Add Image", tint = themeBasedIconColor)
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    LazyRow(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(NoteColors) { color ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (color == selectedColor) 2.dp else 1.dp,
                                        color = themeBasedIconColor,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = color }
                            )
                        }
                        item {
                            Box(
                                modifier = Modifier.size(32.dp).border(1.dp, themeBasedIconColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { showColorPicker = true }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Palette, contentDescription = "Color Picker", tint = themeBasedIconColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                lastEditedTimestamp?.let { 
                    Text(
                        text = "Edited ${java.text.SimpleDateFormat("dd-MM-yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date(it))}",
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(vertical = 16.dp) // Removed horizontal padding so TextField internal padding aligns flush
        ) {
            imageUri?.let { uri ->
                Box(modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(horizontal = 16.dp)) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium),
                        contentScale = ContentScale.Fit
                    )
                    IconButton(
                        onClick = { imageUri = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Remove Image", tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            TextField(
                value = title,
                onValueChange = { title = it },
                placeholder = { 
                    Text(
                        "Title", 
                        style = MaterialTheme.typography.headlineMedium, 
                        color = textColor.copy(alpha = 0.5f)
                    ) 
                },
                modifier = Modifier.fillMaxWidth(),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Medium, 
                    color = textColor
                ),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = textColor
                )
            )
            
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = textColor.copy(alpha = 0.2f)
            )

            BasicTextField(
                value = contentValue,
                onValueChange = { contentValue = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .onGloballyPositioned { coordinates ->
                        if (textFieldTopY == 0f) {
                            textFieldTopY = coordinates.positionInParent().y
                        }
                    }
                    .bringIntoViewRequester(bringIntoViewRequester),
                onTextLayout = { textLayoutResult = it },
                visualTransformation = if (!searchQuery.isNullOrBlank()) {
                    val highlightColor = if (isDarkTheme) Color.White.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.2f)
                    object : VisualTransformation {
                        override fun filter(text: AnnotatedString): TransformedText {
                            val builder = AnnotatedString.Builder(text)
                            var startIndex = text.indexOf(searchQuery, ignoreCase = true)
                            while (startIndex >= 0) {
                                builder.addStyle(
                                    style = SpanStyle(background = highlightColor),
                                    start = startIndex,
                                    end = startIndex + searchQuery.length
                                )
                                startIndex = text.indexOf(searchQuery, startIndex + searchQuery.length, ignoreCase = true)
                            }
                            return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
                        }
                    }
                } else VisualTransformation.None,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = textColor,
                    lineHeight = 26.sp,
                    letterSpacing = 0.3.sp
                ),
                cursorBrush = SolidColor(textColor),
                decorationBox = { innerTextField ->
                    if (contentValue.text.isEmpty()) {
                        Text(
                            "Note", 
                            style = MaterialTheme.typography.bodyLarge, 
                            color = textColor.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            )
            
            // Ensures we can always scroll past the keyboard
            Spacer(modifier = Modifier.height(150.dp))
        }
    }

    if (showColorPicker) {
        GoogleColorPickerDialog(
            initialColor = selectedColor,
            onDismiss = { showColorPicker = false },
            onColorSelected = { 
                selectedColor = it
                showColorPicker = false
            }
        )
    }
}

@Composable
fun GoogleColorPickerDialog(
    initialColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val hsv = remember {
        val hsvOut = floatArrayOf(0f, 0f, 0f)
        AndroidColor.colorToHSV(initialColor.toArgb(), hsvOut)
        mutableStateOf(Triple(hsvOut[0], hsvOut[1], hsvOut[2]))
    }

    val (hue, saturation, value) = hsv.value
    val currentColor = remember(hue, saturation, value) {
        Color.hsv(hue, saturation, value)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF202124), 
            contentColor = Color.White
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Colour picker", style = MaterialTheme.typography.titleLarge)
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(4.dp))) {
                    Box(modifier = Modifier.weight(0.35f).fillMaxHeight().background(currentColor))
                    Box(modifier = Modifier.weight(0.65f).fillMaxHeight()) {
                        SaturationValuePicker(
                            hue = hue,
                            saturation = saturation,
                            value = value,
                            onSaturationValueChange = { s, v -> hsv.value = Triple(hue, s, v) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                HueSlider(hue = hue, onHueChange = { h -> hsv.value = Triple(h, saturation, value) })

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color(0xFF8AB4F8)) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onColorSelected(currentColor) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8AB4F8), contentColor = Color.Black)
                    ) { Text("Select") }
                }
            }
        }
    }
}

@Composable
fun SaturationValuePicker(hue: Float, saturation: Float, value: Float, onSaturationValueChange: (Float, Float) -> Unit) {
    Canvas(
        modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectDragGestures { change, _ ->
                val s = (change.position.x / size.width).coerceIn(0f, 1f)
                val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                onSaturationValueChange(s, v)
            }
        }
    ) {
        val saturationBrush = Brush.linearGradient(
            colors = listOf(Color.White, Color.hsv(hue, 1f, 1f)),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f)
        )
        drawRect(brush = saturationBrush)
        val valueBrush = Brush.linearGradient(colors = listOf(Color.Transparent, Color.Black), start = Offset(0f, 0f), end = Offset(0f, size.height))
        drawRect(brush = valueBrush)
        val cursorX = saturation * size.width
        val cursorY = (1f - value) * size.height
        drawCircle(color = Color.White, radius = 8.dp.toPx(), center = Offset(cursorX, cursorY), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun HueSlider(hue: Float, onHueChange: (Float) -> Unit) {
    val hueColors = remember { (0..360).map { Color.hsv(it.toFloat(), 1f, 1f) } }
    val hueBrush = remember { Brush.linearGradient(colors = hueColors) }
    Box(modifier = Modifier.fillMaxWidth().height(16.dp), contentAlignment = Alignment.Center) {
        Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)).background(hueBrush))
        Slider(
            value = hue,
            onValueChange = onHueChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

