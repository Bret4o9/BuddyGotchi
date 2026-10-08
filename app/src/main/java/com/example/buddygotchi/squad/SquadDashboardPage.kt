package com.example.buddygotchi.squad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.DashboardGridDefaults
import com.example.buddygotchi.ui.DashboardGridBackground
import com.example.buddygotchi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SquadDashboardPage(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    var profile by remember { mutableStateOf(SquadManager.getProfile(context)) }
    var friends by remember { mutableStateOf(SquadManager.getFriends(context)) }
    var events by remember { mutableStateOf(SquadManager.getEvents(context)) }

    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddFriendDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        FirebaseSquadSync.initialize(context) { updatedEvents ->
            events = updatedEvents
        }
    }

    fun copyBuddyCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("BuddyGotchi Code", profile.buddyCode)
        clipboard.setPrimaryClip(clip)
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        Toast.makeText(context, "BUDDY CODE ${profile.buddyCode} COPIED", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Aligned Dot Grid Background
        DashboardGridBackground(
            isEditMode = false,
            packedRows = emptyList(),
            activeResizeWidgetId = null,
            activeResizePreviewSize = null,
            scrollY = scrollState.value.toFloat(),
            modifier = Modifier.fillMaxSize()
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(
                    start = DashboardGridDefaults.HORIZONTAL_PADDING,
                    end = DashboardGridDefaults.HORIZONTAL_PADDING,
                    top = 6.dp,
                    bottom = 40.dp
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. User Identity & Buddy Code Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = NothingCardSurface,
                border = BorderStroke(1.dp, NothingBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Text(
                                text = "OPERATOR // ${profile.displayName}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp,
                                color = NothingWhite
                            )
                        }

                        // Status pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1E281E))
                                .border(1.dp, Color(0xFF00E676).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ONLINE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E676),
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    // Buddy Code Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "YOUR SQUAD BUDDY CODE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingTextTertiary,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = profile.buddyCode,
                                fontFamily = Dseg7FontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = NothingWhite
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Copy button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF202020))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(10.dp))
                                    .clickable { copyBuddyCode() }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "COPY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite,
                                    letterSpacing = 0.5.sp
                                )
                            }

                            // Add friend button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingRed)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        showAddFriendDialog = true
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = "+ BUDDY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // 2. Connected Squad Strip
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CONNECTED SQUAD [ ${String.format(Locale.getDefault(), "%02d", friends.size)} ]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = NothingTextSecondary
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(friends) { friend ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NothingCardSurface,
                            border = BorderStroke(1.dp, NothingBorder),
                            modifier = Modifier.width(130.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (friend.isOnline) Color(0xFF00E676) else Color.Gray)
                                    )
                                    Text(
                                        text = "LVL ${friend.level}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = NothingTextTertiary
                                    )
                                }
                                Text(
                                    text = friend.displayName,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                                Text(
                                    text = friend.buddyCode,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    color = NothingTextSecondary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }

            // 3. Group Events Section
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Text(
                            text = "GROUP EVENTS [ ${String.format(Locale.getDefault(), "%02d", events.size)} ]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = NothingTextSecondary
                        )
                    }

                    // Create event action
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingRed)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCreateDialog = true
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "+ NEW EVENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                if (events.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = NothingCardSurface,
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "NO ACTIVE SQUAD EVENTS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "TAP '+ NEW EVENT' TO SCHEDULE A SESSION",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }
                } else {
                    events.forEach { event ->
                        val isUserHost = event.hostBuddyCode == profile.buddyCode
                        val userParticipant = event.participants.find { it.buddyCode == profile.buddyCode }
                        val isUserPending = userParticipant?.status == ParticipantStatus.PENDING

                        val timeFormatter = SimpleDateFormat("EEE, dd MMM • HH:mm", Locale.getDefault())
                        val formattedTime = timeFormatter.format(Date(event.eventTime)).uppercase()

                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = NothingCardSurface,
                            border = BorderStroke(1.dp, if (isUserPending) NothingRed.copy(alpha = 0.5f) else NothingBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Top row: Title + Time
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(NothingRed)
                                        )
                                        Text(
                                            text = event.title,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingWhite
                                        )
                                    }

                                    // Delete/cancel button
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.05f))
                                            .clickable {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                events = SquadManager.deleteEvent(context, event.id)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "✕",
                                            fontSize = 9.sp,
                                            color = NothingTextTertiary
                                        )
                                    }
                                }

                                // Date & Time Banner
                                Text(
                                    text = formattedTime,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingRed,
                                    letterSpacing = 1.sp
                                )

                                // Location Row
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "📍 ${event.location}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = NothingWhite
                                    )
                                }

                                // Alert & Host Pills
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1E1E1E))
                                            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (event.reminderOffsetMinutes > 0)
                                                "🔔 -${event.reminderOffsetMinutes}M ALARM"
                                            else "🔔 EXACT ALARM",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingTextSecondary
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFF1A1A1A))
                                            .border(1.dp, Color(0xFF262626), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = if (isUserHost) "HOST: YOU" else "HOST: ${event.hostDisplayName}",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingTextTertiary
                                        )
                                    }
                                }

                                // Participants Row
                                if (event.participants.isNotEmpty()) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "ATTENDEES:",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.sp,
                                            color = NothingTextTertiary
                                        )
                                        event.participants.forEach { p ->
                                            val statusSymbol = when (p.status) {
                                                ParticipantStatus.ACCEPTED -> "✓"
                                                ParticipantStatus.PENDING -> "?"
                                                ParticipantStatus.DECLINED -> "✕"
                                            }
                                            val statusColor = when (p.status) {
                                                ParticipantStatus.ACCEPTED -> Color(0xFF00E676)
                                                ParticipantStatus.PENDING -> Color(0xFFFFB300)
                                                ParticipantStatus.DECLINED -> Color.Gray
                                            }
                                            Text(
                                                text = "${p.displayName} ($statusSymbol)",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = statusColor
                                            )
                                        }
                                    }
                                }

                                // RSVP Banner if pending
                                if (isUserPending) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(NothingRed)
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    events = SquadManager.respondToEvent(
                                                        context,
                                                        event.id,
                                                        ParticipantStatus.ACCEPTED
                                                    )
                                                    Toast.makeText(context, "EVENT ACCEPTED • ALARM SET", Toast.LENGTH_SHORT).show()
                                                }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✓ ACCEPT & SET ALARM",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(0xFF222222))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    events = SquadManager.respondToEvent(
                                                        context,
                                                        event.id,
                                                        ParticipantStatus.DECLINED
                                                    )
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "DECLINE",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    if (showAddFriendDialog) {
        AddFriendDialog(
            onDismiss = { showAddFriendDialog = false },
            onAddFriend = { code, name ->
                friends = SquadManager.getFriends(context).toMutableList().also {
                    val added = SquadManager.addFriend(context, code, name)
                }
                friends = SquadManager.getFriends(context)
                Toast.makeText(context, "SQUAD BUDDY ADDED", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showCreateDialog) {
        CreateEventDialog(
            currentUser = profile,
            friends = friends,
            onDismiss = { showCreateDialog = false },
            onEventCreated = { newEvent ->
                events = SquadManager.createEvent(context, newEvent)
                Toast.makeText(context, "EVENT DEPLOYED & ALARM SCHEDULED", Toast.LENGTH_SHORT).show()
            }
        )
    }
}
