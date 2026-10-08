package com.example.buddygotchi

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.theme.NothingBorder
import com.example.buddygotchi.ui.theme.NothingCardSurface
import com.example.buddygotchi.ui.theme.NothingDarkBackground
import com.example.buddygotchi.ui.theme.NothingRed
import com.example.buddygotchi.ui.theme.NothingSurfaceVariant
import com.example.buddygotchi.ui.theme.NothingTextSecondary
import com.example.buddygotchi.ui.theme.NothingTextTertiary
import com.example.buddygotchi.ui.theme.NothingWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Collapsed Notes Widget card rendered on the Left Page.
 * Tapping it triggers onExpand() to smoothly expand across the whole page.
 */
@Composable
fun NotesCollapsedWidget(
    notes: List<NoteItem>,
    onExpand: () -> Unit,
    onQuickCreate: () -> Unit,
    size: WidgetSize = WidgetSize.WIDE,
    modifier: Modifier = Modifier
) {
    val latestNote = notes.firstOrNull()
    val dateFormat = remember { SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()) }

    val isCube = size.cols == 1 && size.rows == 1
    val isTallStrip = size.cols == 1 && size.rows > 1
    val isHalfRow = size.cols == 2 && size.rows == 1
    val isHalfBlock = size.cols == 2 && size.rows > 1
    val isSlim = size.cols == 4 && size.rows == 1

    val cornerRadius = if (isCube) 20.dp else 24.dp
    val internalPadding = when {
        size.cols == 1 -> 8.dp
        size.rows == 1 -> 10.dp
        else -> 14.dp
    }

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = NothingCardSurface),
        border = BorderStroke(1.dp, NothingBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(DashboardGridDefaults.getWidgetHeight(size))
            .clickable { onExpand() }
    ) {
        when {
            // === 1. COMPACT 1x1 CUBE ===
            isCube -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${notes.size}",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed,
                        lineHeight = 24.sp
                    )
                    Text(
                        text = if (notes.isEmpty()) "EMPTY" else "NOTES",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "EXPAND ↗",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.5.sp,
                        color = NothingTextTertiary
                    )
                }
            }

            // === 2. VERTICAL STRIP (1x2, 1x3, 1x4) ===
            isTallStrip -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${notes.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                        Text(
                            text = "NOTES",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingTextSecondary
                        )
                    }

                    if (latestNote != null) {
                        Text(
                            text = if (latestNote.title.isNotBlank()) latestNote.title else "(UNTITLED)",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            maxLines = size.rows,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "EXPAND ↗",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.sp,
                        color = NothingTextTertiary
                    )
                }
            }

            // === 3. HALF ROW (2x1, 88dp height) ===
            isHalfRow -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "NOTES [${notes.size}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            if (latestNote != null) {
                                Text(
                                    text = if (latestNote.title.isNotBlank()) latestNote.title else "(UNTITLED)",
                                    fontSize = 9.sp,
                                    color = NothingTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Text(
                        text = "EXPAND ↗",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary
                    )
                }
            }

            // === 4. HALF BLOCK (2x2, 2x3, 2x4) ===
            isHalfBlock -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NOTES [${notes.size}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }

                        Text(
                            text = "EXPAND ↗",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            color = NothingTextSecondary
                        )
                    }

                    if (latestNote != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingDarkBackground)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = if (latestNote.title.isNotBlank()) latestNote.title else "(UNTITLED)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (latestNote.content.isNotBlank()) latestNote.content else "No text...",
                                fontSize = 9.sp,
                                color = NothingTextSecondary,
                                maxLines = if (size.rows == 2) 2 else 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text(
                            text = "NO NOTES YET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NothingTextTertiary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingRed.copy(alpha = 0.15f))
                                .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                                .clickable { onQuickCreate() }
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "+ NEW",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }

            // === 5. SLIM BANNER (4x1) ===
            isSlim -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "NOTES [${notes.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        if (latestNote != null) {
                            Text(
                                text = if (latestNote.title.isNotBlank()) latestNote.title else "(UNTITLED NOTE)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = NothingTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "EXPAND ↗",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NothingRed.copy(alpha = 0.15f))
                                .border(1.dp, NothingRed, RoundedCornerShape(6.dp))
                                .clickable { onQuickCreate() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ NEW",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }

            // === 6. FULL RICH DISPLAY (4x2, 4x3, 4x4) ===
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "NOTES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ ${String.format("%02d", notes.size)} ]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingSurfaceVariant)
                                .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "EXPAND ↗",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingTextSecondary
                            )
                        }
                    }

                    if (latestNote != null) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NothingDarkBackground)
                                .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (latestNote.title.isNotBlank()) latestNote.title else "(UNTITLED NOTE)",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = dateFormat.format(Date(latestNote.updatedAt)),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = NothingTextTertiary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = if (latestNote.content.isNotBlank()) latestNote.content else "No additional text...",
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                color = NothingTextSecondary,
                                maxLines = if (size.rows == 2) 2 else 5,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(55.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NothingDarkBackground)
                                .border(1.dp, NothingBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "NO NOTES YET // TAP TO WRITE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TAP CARD TO EXPAND FULLSCREEN",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.8.sp,
                            color = NothingTextTertiary
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingRed.copy(alpha = 0.15f))
                                .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                                .clickable { onQuickCreate() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ NEW NOTE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fullscreen / Full-page expanded Notes experience occupying the whole Left Page.
 * Features a notebook list, full title & body note editor, and auto-save.
 */
@Composable
fun NotesExpandedView(
    notes: List<NoteItem>,
    initialEditingNoteId: String? = null,
    onNotesUpdated: (List<NoteItem>) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedNoteId by remember { mutableStateOf(initialEditingNoteId) }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, yyyy HH:mm", Locale.getDefault()) }

    // If an editing note is selected, find it
    val activeNote = notes.firstOrNull { it.id == selectedNoteId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingDarkBackground)
    ) {
        if (activeNote == null) {
            // === LIST VIEW: All Notes ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                // Top Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Minimize / Collapse Button
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingCardSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                            .clickable { onCollapse() }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "← MINIMIZE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }

                    // Page Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "NOTES // ${notes.size} ENTRIES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }

                    // New Note Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingRed)
                            .clickable {
                                val newNote = NoteItem(
                                    title = "",
                                    content = "",
                                    updatedAt = System.currentTimeMillis()
                                )
                                val updated = NotesManager.upsertNote(context, newNote)
                                onNotesUpdated(updated)
                                selectedNoteId = newNote.id
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+ NEW",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = NothingBorder, thickness = 1.dp)
                Spacer(modifier = Modifier.height(14.dp))

                if (notes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO NOTES YET",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = NothingTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap '+ NEW' to write your first entry",
                                fontSize = 11.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                dateFormat = dateFormat,
                                onClick = { selectedNoteId = note.id },
                                onDelete = {
                                    val updated = NotesManager.deleteNote(context, note.id)
                                    onNotesUpdated(updated)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // === EDITOR VIEW: Single Note Detailed Writing ===
            NoteEditorScreen(
                note = activeNote,
                onBack = { selectedNoteId = null },
                onCollapse = onCollapse,
                onDelete = {
                    val updated = NotesManager.deleteNote(context, activeNote.id)
                    onNotesUpdated(updated)
                    selectedNoteId = null
                },
                onUpdate = { updatedNote ->
                    val updated = NotesManager.upsertNote(context, updatedNote)
                    onNotesUpdated(updated)
                }
            )
        }
    }
}

@Composable
private fun NoteCardItem(
    note: NoteItem,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NothingCardSurface),
        border = BorderStroke(1.dp, NothingBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(NothingRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (note.title.isNotBlank()) note.title else "(UNTITLED NOTE)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Delete Button
                Text(
                    text = "✕",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingTextTertiary,
                    modifier = Modifier
                        .clickable { onDelete() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (note.content.isNotBlank()) note.content else "Empty note...",
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = NothingTextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateFormat.format(Date(note.updatedAt)),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                color = NothingTextTertiary
            )
        }
    }
}

/**
 * Clean Nothing OS distraction-free note editor with title and body fields.
 */
@Composable
private fun NoteEditorScreen(
    note: NoteItem,
    onBack: () -> Unit,
    onCollapse: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (NoteItem) -> Unit
) {
    var titleText by remember(note.id) { mutableStateOf(note.title) }
    var contentText by remember(note.id) { mutableStateOf(note.content) }

    // Auto-save changes on text input
    fun save() {
        onUpdate(note.copy(title = titleText, content = contentText))
    }

    val wordCount = remember(contentText) {
        if (contentText.isBlank()) 0 else contentText.trim().split("\\s+".toRegex()).size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 12.dp)
    ) {
        // Navigation Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Back to list
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .clickable { onBack() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "← ALL NOTES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }

                // Direct Collapse
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .clickable { onCollapse() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "MINIMIZE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary
                    )
                }
            }

            // Delete Note Button
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingSurfaceVariant)
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .clickable { onDelete() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "DELETE",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        HorizontalDivider(color = NothingBorder, thickness = 1.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Title Input
        Box(modifier = Modifier.fillMaxWidth()) {
            if (titleText.isEmpty()) {
                Text(
                    text = "NOTE TITLE...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NothingTextTertiary
                )
            }
            BasicTextField(
                value = titleText,
                onValueChange = {
                    titleText = it
                    save()
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NothingWhite
                ),
                cursorBrush = SolidColor(NothingRed),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = NothingBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(10.dp))

        // Note Body Input
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (contentText.isEmpty()) {
                Text(
                    text = "Write your thoughts, daily tasks, ideas, or meeting notes here...",
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = NothingTextTertiary
                )
            }
            BasicTextField(
                value = contentText,
                onValueChange = {
                    contentText = it
                    save()
                },
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = NothingWhite.copy(alpha = 0.9f)
                ),
                cursorBrush = SolidColor(NothingRed),
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Footer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$wordCount WORDS // ${contentText.length} CHARS",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = NothingTextTertiary
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00E5FF))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "AUTO-SAVED",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF80D8FF)
                )
            }
        }
    }
}
