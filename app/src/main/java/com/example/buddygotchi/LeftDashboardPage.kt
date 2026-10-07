package com.example.buddygotchi

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.theme.NothingDarkBackground
import com.example.buddygotchi.ui.theme.NothingRed
import com.example.buddygotchi.ui.theme.NothingTextSecondary
import com.example.buddygotchi.ui.theme.NothingTextTertiary
import com.example.buddygotchi.ui.theme.NothingWhite

@Composable
fun LeftDashboardPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var notes by remember { mutableStateOf(NotesManager.getNotes(context)) }
    var isNotesExpanded by remember { mutableStateOf(false) }
    var initialEditingNoteId by remember { mutableStateOf<String?>(null) }
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingDarkBackground)
    ) {
        AnimatedContent(
            targetState = isNotesExpanded,
            transitionSpec = {
                if (targetState) {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                        .togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    scaleOut(targetScale = 1.04f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                } else {
                    (fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                            scaleIn(initialScale = 1.04f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)))
                        .togetherWith(
                            fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                                    scaleOut(targetScale = 0.94f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
                        )
                }
            },
            label = "NotesExpandTransition"
        ) { expanded ->
            if (expanded) {
                // Occupies the entire page
                NotesExpandedView(
                    notes = notes,
                    initialEditingNoteId = initialEditingNoteId,
                    onNotesUpdated = { updated ->
                        notes = updated
                    },
                    onCollapse = {
                        isNotesExpanded = false
                        initialEditingNoteId = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Normal Left Page Dashboard layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(
                            start = DashboardGridDefaults.HORIZONTAL_PADDING,
                            end = DashboardGridDefaults.HORIZONTAL_PADDING,
                            top = 10.dp,
                            bottom = 36.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Page Subtitle / Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PAGE 01 // WORKSPACE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingTextSecondary
                            )
                        }

                        Text(
                            text = "HYDRATION & NOTES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingTextTertiary
                        )
                    }

                    // 1. Water Tracker Widget (with Tilt Physics & Pouring Animation)
                    WaterTrackerWidget()

                    // 2. Notes Widget (Collapsed Card - Tap to Expand to Whole Page)
                    NotesCollapsedWidget(
                        notes = notes,
                        onExpand = {
                            initialEditingNoteId = null
                            isNotesExpanded = true
                        },
                        onQuickCreate = {
                            val newNote = NoteItem(
                                title = "",
                                content = "",
                                updatedAt = System.currentTimeMillis()
                            )
                            notes = NotesManager.upsertNote(context, newNote)
                            initialEditingNoteId = newNote.id
                            isNotesExpanded = true
                        }
                    )
                }
            }
        }
    }
}
