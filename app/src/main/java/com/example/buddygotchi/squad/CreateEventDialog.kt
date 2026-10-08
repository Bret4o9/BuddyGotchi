package com.example.buddygotchi.squad

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.buddygotchi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun CreateEventDialog(
    currentUser: UserProfile,
    friends: List<SquadFriend>,
    onDismiss: () -> Unit,
    onEventCreated: (GroupEvent) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    // Date offset: 0 = Today, 1 = Tomorrow, 2 = Day After
    var dateDaysOffset by remember { mutableIntStateOf(1) }

    // Time: default to next whole hour
    val initialCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }
    }
    var selectedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var selectedMinute by remember { mutableIntStateOf(0) }

    // Reminder offset minutes
    var selectedOffsetMinutes by remember { mutableIntStateOf(30) }

    // Selected friends to invite
    var selectedFriendCodes by remember {
        mutableStateOf(friends.map { it.buddyCode }.toSet())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f),
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
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
                            text = "CREATE SQUAD EVENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = NothingWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 11.sp,
                            color = NothingTextSecondary
                        )
                    }
                }

                // Title Input & Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "EVENT TITLE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    BasicTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it.uppercase() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, if (titleInput.isNotBlank()) NothingRed else Color(0xFF2B2B2B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            letterSpacing = 1.sp
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (titleInput.isEmpty()) {
                                Text(
                                    text = "E.G. SQUAD WORKOUT // STUDY",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = NothingTextTertiary.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Quick Title Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("WORKOUT", "STUDY SPRINT", "GAMING", "MEETUP").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (titleInput == preset) NothingRed.copy(alpha = 0.3f) else Color(0xFF1A1A1A))
                                    .border(1.dp, if (titleInput == preset) NothingRed else Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    .clickable { titleInput = preset }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (titleInput == preset) Color.White else NothingTextSecondary
                                )
                            }
                        }
                    }
                }

                // Location Input & Presets
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "LOCATION // PIN",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    BasicTextField(
                        value = locationInput,
                        onValueChange = { locationInput = it.uppercase() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, if (locationInput.isNotBlank()) Color(0xFF444444) else Color(0xFF2B2B2B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            letterSpacing = 1.sp
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (locationInput.isEmpty()) {
                                Text(
                                    text = "E.G. CENTRAL GYM / DISCORD",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = NothingTextTertiary.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    // Quick Location Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("CENTRAL GYM", "CAMPUS LIBRARY", "DISCORD", "ROOM 404").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (locationInput == preset) Color(0xFF2C2C2C) else Color(0xFF1A1A1A))
                                    .border(1.dp, if (locationInput == preset) Color(0xFF666666) else Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                    .clickable { locationInput = preset }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = preset,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (locationInput == preset) Color.White else NothingTextSecondary
                                )
                            }
                        }
                    }
                }

                // Date Picker Pills
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DATE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "TODAY",
                            1 to "TOMORROW",
                            2 to "+2 DAYS"
                        ).forEach { (offset, label) ->
                            val isSelected = dateDaysOffset == offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NothingRed else Color(0xFF1C1C1C))
                                    .border(
                                        1.dp,
                                        if (isSelected) NothingRed else Color(0xFF2E2E2E),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { dateDaysOffset = offset }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else NothingTextSecondary,
                                    letterSpacing = 0.8.sp
                                )
                            }
                        }
                    }
                }

                // Time Selector (Hour & Minute increment buttons)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "TIME (24-HOUR)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Hour Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .clickable { selectedHour = (selectedHour - 1 + 24) % 24 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = NothingWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedHour),
                                fontFamily = Dseg7FontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .clickable { selectedHour = (selectedHour + 1) % 24 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = NothingWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Text(
                            text = ":",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )

                        // Minute Selector
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .clickable { selectedMinute = (selectedMinute - 5 + 60) % 60 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = NothingWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = String.format(Locale.getDefault(), "%02d", selectedMinute),
                                fontFamily = Dseg7FontFamily,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF222222))
                                    .clickable { selectedMinute = (selectedMinute + 5) % 60 },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = NothingWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Reminder Offset Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ALARM REMINDER OFFSET",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            0 to "AT TIME",
                            15 to "15M",
                            30 to "30M",
                            60 to "1 HOUR"
                        ).forEach { (offset, label) ->
                            val isSelected = selectedOffsetMinutes == offset
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) NothingRed else Color(0xFF1E1E1E))
                                    .border(
                                        1.dp,
                                        if (isSelected) NothingRed else Color(0xFF2E2E2E),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { selectedOffsetMinutes = offset }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else NothingTextSecondary
                                )
                            }
                        }
                    }
                }

                // Invite Squad Friends (Checkboxes)
                if (friends.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "INVITE SQUAD MEMBERS [${selectedFriendCodes.size}/${friends.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            letterSpacing = 1.sp
                        )
                        friends.forEach { friend ->
                            val isInvited = friend.buddyCode in selectedFriendCodes
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isInvited) Color(0xFF222222) else Color(0xFF161616))
                                    .border(
                                        1.dp,
                                        if (isInvited) Color(0xFF444444) else Color(0xFF252525),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedFriendCodes = if (isInvited) {
                                            selectedFriendCodes - friend.buddyCode
                                        } else {
                                            selectedFriendCodes + friend.buddyCode
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (friend.isOnline) Color(0xFF00E676) else Color.Gray)
                                    )
                                    Text(
                                        text = "${friend.displayName} // ${friend.buddyCode}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isInvited) NothingWhite else NothingTextSecondary
                                    )
                                }
                                Text(
                                    text = if (isInvited) "[✓]" else "[ ]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInvited) NothingRed else NothingTextTertiary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Submit Button
                val canSubmit = titleInput.trim().isNotBlank()
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (canSubmit) NothingRed else Color(0xFF2A2A2A))
                        .clickable(enabled = canSubmit) {
                            val targetCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, dateDaysOffset)
                                set(Calendar.HOUR_OF_DAY, selectedHour)
                                set(Calendar.MINUTE, selectedMinute)
                                set(Calendar.SECOND, 0)
                            }
                            val participants = mutableListOf<EventParticipant>()
                            // Host
                            participants.add(
                                EventParticipant(
                                    buddyCode = currentUser.buddyCode,
                                    displayName = currentUser.displayName,
                                    status = ParticipantStatus.ACCEPTED,
                                    isHost = true
                                )
                            )
                            // Friends
                            friends.filter { it.buddyCode in selectedFriendCodes }.forEach { f ->
                                participants.add(
                                    EventParticipant(
                                        buddyCode = f.buddyCode,
                                        displayName = f.displayName,
                                        status = ParticipantStatus.PENDING,
                                        isHost = false
                                    )
                                )
                            }

                            val newEvent = GroupEvent(
                                title = titleInput.trim(),
                                location = if (locationInput.isNotBlank()) locationInput.trim() else "LOCATION TBD",
                                eventTime = targetCal.timeInMillis,
                                reminderOffsetMinutes = selectedOffsetMinutes,
                                hostBuddyCode = currentUser.buddyCode,
                                hostDisplayName = currentUser.displayName,
                                participants = participants
                            )
                            onEventCreated(newEvent)
                            onDismiss()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "DEPLOY SQUAD EVENT →",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (canSubmit) Color.White else NothingTextTertiary
                    )
                }
            }
        }
    }
}
