package com.example.buddygotchi.squad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import kotlinx.coroutines.delay
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
    var viewingMapEvent by remember { mutableStateOf<GroupEvent?>(null) }
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
                        val buddyColor = getSquadBuddyColor(friend)
                        var tapBlink by remember { mutableStateOf(false) }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NothingCardSurface,
                            border = BorderStroke(1.dp, buddyColor.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .width(146.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    tapBlink = true
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (friend.isOnline) Color(0xFF00E676) else Color.Gray)
                                        )
                                        Text(
                                            text = if (friend.isOnline) "LIVE" else "AWAY",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (friend.isOnline) Color(0xFF00E676) else Color.Gray,
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                    Text(
                                        text = "LVL ${friend.level}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = NothingTextTertiary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = friend.displayName,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingWhite
                                        )
                                        Text(
                                            text = friend.buddyCode,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 8.5.sp,
                                            color = NothingTextSecondary,
                                            letterSpacing = 0.5.sp
                                        )
                                    }

                                    SquadMemberBuddy(
                                        buddyColor = buddyColor,
                                        manualBlinkTrigger = tapBlink,
                                        onBlinkFinished = { tapBlink = false }
                                    )
                                }
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

                                // Location Row & Map Navigation
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF181818))
                                        .border(1.dp, if (event.latitude != null) Color(0xFF333333) else Color(0xFF252525), RoundedCornerShape(10.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "📍",
                                            fontSize = 11.sp
                                        )
                                        Column {
                                            Text(
                                                text = event.location,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingWhite,
                                                maxLines = 1
                                            )
                                            if (event.latitude != null && event.longitude != null) {
                                                Text(
                                                    text = String.format(Locale.US, "GPS: %.4f, %.4f", event.latitude, event.longitude),
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.sp,
                                                    color = NothingRed,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                        if (event.latitude != null && event.longitude != null) {
                                            // Preview in Nothing OS map
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF222222))
                                                    .border(1.dp, Color(0xFF383838), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        viewingMapEvent = event
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "MAP ⌖",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingWhite
                                                )
                                            }

                                            // External Navigation (Google Maps on Android, Apple Maps on iOS)
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(NothingRed)
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        SquadMapNavigationHelper.openNavigation(context, event.latitude, event.longitude, event.title)
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "NAV ↗",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        } else {
                                            // Fallback text search in maps
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF222222))
                                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                        SquadMapNavigationHelper.openSearch(context, event.location)
                                                    }
                                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = "MAP ↗",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingTextSecondary
                                                )
                                            }
                                        }
                                    }
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
                SquadManager.addFriend(context, code, name)
                friends = SquadManager.getFriends(context)
                Toast.makeText(context, "SQUAD BUDDY ADDED", Toast.LENGTH_SHORT).show()
                FirebaseSquadSync.findUserByBuddyCode(code) { cloudBuddy ->
                    if (cloudBuddy != null) {
                        val current = SquadManager.getFriends(context).toMutableList()
                        val idx = current.indexOfFirst { it.buddyCode == cloudBuddy.buddyCode }
                        if (idx >= 0) {
                            current[idx] = cloudBuddy
                            SquadManager.saveFriends(context, current)
                            friends = current
                        }
                    }
                }
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

    if (viewingMapEvent != null) {
        val ev = viewingMapEvent!!
        NothingMapPickerDialog(
            initialLat = ev.latitude,
            initialLng = ev.longitude,
            initialLocationName = ev.location,
            onDismiss = { viewingMapEvent = null },
            onLocationSelected = { _, _, _ ->
                viewingMapEvent = null
            }
        )
    }
}

/**
 * Returns member-specific buddy color:
 * - Poli: Orange (#FF7A00)
 * - Sani: Green (#00E676)
 * - Geri: Pink (#FF4081)
 */
fun getSquadBuddyColor(friend: SquadFriend): Color {
    if (!friend.buddyColorHex.isNullOrBlank()) {
        try {
            return Color(android.graphics.Color.parseColor(friend.buddyColorHex))
        } catch (_: Exception) {}
    }
    return when (friend.displayName.trim().uppercase()) {
        "POLI" -> Color(0xFFFF7A00) // Orange
        "SANI" -> Color(0xFF00E676) // Green
        "GERI" -> Color(0xFFFF4081) // Pink
        else -> Color(0xFF00E676)
    }
}

/**
 * Mini animated BuddyGotchi companion eyes for squad members.
 * Custom tinted per member with natural blinking and idle looking around.
 */
@Composable
fun SquadMemberBuddy(
    buddyColor: Color,
    manualBlinkTrigger: Boolean = false,
    onBlinkFinished: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isBlinking by remember { mutableStateOf(false) }

    // Manual blink when card is tapped
    LaunchedEffect(manualBlinkTrigger) {
        if (manualBlinkTrigger) {
            isBlinking = true
            delay(120)
            isBlinking = false
            onBlinkFinished()
        }
    }

    // Natural random blinking loop with staggered delays
    LaunchedEffect(Unit) {
        while (true) {
            delay(kotlin.random.Random.nextLong(2800, 6500))
            isBlinking = true
            delay(115)
            isBlinking = false
            if (kotlin.random.Random.nextFloat() < 0.22f) {
                delay(90)
                isBlinking = true
                delay(100)
                isBlinking = false
            }
        }
    }

    val blinkScaleY by animateFloatAsState(
        targetValue = if (isBlinking) 0.08f else 1f,
        animationSpec = tween(durationMillis = 60, easing = FastOutLinearInEasing),
        label = "squadBuddyBlink"
    )

    // Gentle idle glance shift
    var gazeOffset by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(kotlin.random.Random.nextLong(3200, 7000))
            val choices = listOf(
                Offset.Zero,
                Offset.Zero,
                Offset(-1.8f, 0f),
                Offset(1.8f, 0f),
                Offset(0f, -1.2f)
            )
            gazeOffset = choices.random()
        }
    }

    val animatedGaze by animateOffsetAsState(
        targetValue = gazeOffset,
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "squadBuddyGaze"
    )

    Canvas(
        modifier = modifier.size(width = 38.dp, height = 20.dp)
    ) {
        val eyeRadius = 7.dp.toPx()
        val eyeSpacing = 17.5.dp.toPx()
        val strokeWidth = 2.dp.toPx()
        val pupilRadius = 3.dp.toPx()

        val centerCanvas = Offset(size.width / 2f, size.height / 2f)
        val leftCenter = Offset(centerCanvas.x - eyeSpacing / 2f, centerCanvas.y)
        val rightCenter = Offset(centerCanvas.x + eyeSpacing / 2f, centerCanvas.y)

        val gazePx = Offset(animatedGaze.x * (density / 2.75f), animatedGaze.y * (density / 2.75f))

        // Left Eye
        scale(scaleX = 1f, scaleY = blinkScaleY, pivot = leftCenter) {
            drawCircle(
                color = Color(0xFF101010),
                radius = eyeRadius,
                center = leftCenter
            )
            drawCircle(
                color = buddyColor,
                radius = eyeRadius,
                center = leftCenter,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = buddyColor,
                radius = pupilRadius,
                center = leftCenter + gazePx
            )
        }

        // Right Eye
        scale(scaleX = 1f, scaleY = blinkScaleY, pivot = rightCenter) {
            drawCircle(
                color = Color(0xFF101010),
                radius = eyeRadius,
                center = rightCenter
            )
            drawCircle(
                color = buddyColor,
                radius = eyeRadius,
                center = rightCenter,
                style = Stroke(width = strokeWidth)
            )
            drawCircle(
                color = buddyColor,
                radius = pupilRadius,
                center = rightCenter + gazePx
            )
        }
    }
}

