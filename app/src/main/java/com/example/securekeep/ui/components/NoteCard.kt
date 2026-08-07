package com.example.securekeep.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.securekeep.data.local.Note
import com.example.securekeep.ui.utils.getContrastingTextColor

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val isDarkTheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backgroundColor = Color(note.color.toInt())
    val contentColor = getContrastingTextColor(backgroundColor, isDarkTheme)
    
    val effectiveBgColor = if (isDarkTheme && note.color.toInt() == 0xFF000000.toInt()) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        backgroundColor
    }

    val effectiveContentColor = if (isDarkTheme && note.color.toInt() == 0xFF000000.toInt()) {
        MaterialTheme.colorScheme.onSurfaceVariant
    } else {
        contentColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = effectiveBgColor,
            contentColor = effectiveContentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isDarkTheme) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = onClick,
                        onLongClick = onLongClick
                    )
            ) {
                if (!note.isLocked) {
                    // Edge-to-edge image
                    note.imageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Content padded area
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (note.title.isNotEmpty()) {
                            Text(
                                text = note.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.1.sp, // Smooth letter tracking
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                color = effectiveContentColor
                            )
                        }

                        if (note.content.isNotEmpty()) {
                            if (note.title.isNotEmpty()) Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = note.content,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 20.sp,    // Better readability
                                letterSpacing = 0.2.sp, 
                                maxLines = 10,
                                overflow = TextOverflow.Ellipsis,
                                color = effectiveContentColor.copy(alpha = 0.85f)
                            )
                        }
                        
                        if (note.title.isEmpty() && note.content.isEmpty() && note.imageUri == null) {
                            Text(
                                text = "Empty note",
                                style = MaterialTheme.typography.bodyLarge,
                                color = effectiveContentColor.copy(alpha = 0.5f)
                            )
                        }
                    }
                } else {
                    // Locked State - Premium look
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = effectiveContentColor.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked Note",
                                modifier = Modifier.size(24.dp),
                                tint = effectiveContentColor.copy(alpha = 0.8f)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Locked Note",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = effectiveContentColor.copy(alpha = 0.8f)
                        )
                        
                        if (!note.lockedName.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = note.lockedName,
                                style = MaterialTheme.typography.bodySmall,
                                color = effectiveContentColor.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            
            // Pin Indicator positioned relative to 16dp margins
            if (note.isPinned) {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = "Pinned",
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(20.dp),
                    tint = effectiveContentColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}
