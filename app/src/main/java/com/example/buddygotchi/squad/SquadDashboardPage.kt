package com.example.buddygotchi.squad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.delay
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.buddygotchi.AccountAndInventoryManager
import com.example.buddygotchi.Buddy3DManager
import com.example.buddygotchi.DashboardGridDefaults
import com.example.buddygotchi.AccountAndLegalHubDialog
import com.example.buddygotchi.AccountHubTab
import com.example.buddygotchi.ui.DashboardGridBackground
import com.example.buddygotchi.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

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
    var showAccountHubDialog by remember { mutableStateOf(false) }
    var accountHubTab by remember { mutableStateOf(AccountHubTab.ACCOUNT) }

    if (showAccountHubDialog) {
        AccountAndLegalHubDialog(
            initialTab = accountHubTab,
            onDismiss = { showAccountHubDialog = false },
            onDataChanged = {
                profile = SquadManager.getProfile(context)
                friends = SquadManager.getFriends(context)
                events = SquadManager.getEvents(context)
            }
        )
    }
    var viewingMapEvent by remember { mutableStateOf<GroupEvent?>(null) }
    val scrollState = rememberScrollState()

    // Easter Egg Drag & Drop State
    var stagedInviteCodes by remember { mutableStateOf(setOf<String>()) }
    var draggedFriend by remember { mutableStateOf<SquadFriend?>(null) }
    var dragTouchInRoot by remember { mutableStateOf(Offset.Zero) }
    var isHoveringGroupEvents by remember { mutableStateOf(false) }
    var groupEventsBoundsInRoot by remember { mutableStateOf<Rect?>(null) }

    // Haptics & Vibration helper
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun triggerTick() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(15, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    fun triggerConfirmPulse() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

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
            scrollYProvider = { scrollState.value.toFloat() },
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

                    // Cross-Device Account & Legal Action Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "CLOUD ACCOUNT" to AccountHubTab.ACCOUNT,
                            "PRIVACY & LEGAL" to AccountHubTab.LEGAL
                        ).forEach { (btnLabel, tabTarget) ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF1E1E1E))
                                    .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                    .clickable {
                                        accountHubTab = tabTarget
                                        showAccountHubDialog = true
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = btnLabel,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingTextSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. Connected Squad 3x3 Grid
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

                val petCatalog = remember { AccountAndInventoryManager.petCatalog }
                val gridHeight = when {
                    friends.size <= 3 -> 110.dp
                    friends.size <= 6 -> 224.dp
                    else -> 338.dp
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight)
                        .clipToBounds(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(friends) { friend ->
                        val isStaged = friend.buddyCode in stagedInviteCodes
                        val deterministicPetId = remember(friend.buddyCode) {
                            val idx = abs(friend.buddyCode.hashCode()) % petCatalog.size
                            petCatalog[idx].id
                        }
                        val petScreenshot = remember(deterministicPetId) {
                            Buddy3DManager.getBuddyScreenshotBitmap(context, deterministicPetId)
                        }
                        var cardCenterInRoot by remember { mutableStateOf(Offset.Zero) }
                        val isBeingDragged = draggedFriend?.buddyCode == friend.buddyCode

                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .graphicsLayer {
                                    alpha = if (isBeingDragged) 0.3f else 1f
                                    scaleX = if (isBeingDragged) 0.95f else 1f
                                    scaleY = if (isBeingDragged) 0.95f else 1f
                                }
                                .onGloballyPositioned { coords ->
                                    cardCenterInRoot = coords.boundsInRoot().center
                                }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isStaged) NothingRed.copy(alpha = 0.16f) else Color(0xFF181818))
                                .border(
                                    width = if (isStaged || isBeingDragged) 1.5.dp else 1.dp,
                                    color = if (isBeingDragged) NothingRed else if (isStaged) NothingRed else Color(0xFF2B2B2B),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    triggerTick()
                                    stagedInviteCodes = if (isStaged) {
                                        stagedInviteCodes - friend.buddyCode
                                    } else {
                                        stagedInviteCodes + friend.buddyCode
                                    }
                                }
                                .pointerInput(friend.buddyCode) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedFriend = friend
                                            dragTouchInRoot = cardCenterInRoot
                                            triggerTick()
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragTouchInRoot += dragAmount
                                            val bounds = groupEventsBoundsInRoot
                                            isHoveringGroupEvents = if (bounds != null) {
                                                dragTouchInRoot.y >= bounds.top - 50f &&
                                                dragTouchInRoot.y <= bounds.bottom + 50f &&
                                                dragTouchInRoot.x >= bounds.left - 50f &&
                                                dragTouchInRoot.x <= bounds.right + 50f
                                            } else {
                                                false
                                            }
                                        },
                                        onDragEnd = {
                                            if (isHoveringGroupEvents && draggedFriend != null) {
                                                triggerConfirmPulse()
                                                val wasAlreadyStaged = friend.buddyCode in stagedInviteCodes
                                                stagedInviteCodes = stagedInviteCodes + friend.buddyCode
                                                val count = stagedInviteCodes.size
                                                Toast.makeText(
                                                    context,
                                                    if (!wasAlreadyStaged)
                                                        "${friend.displayName} ADDED TO EVENT ($count STAGED)"
                                                    else
                                                        "${friend.displayName} ALREADY STAGED ($count STAGED)",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                            draggedFriend = null
                                            isHoveringGroupEvents = false
                                        },
                                        onDragCancel = {
                                            draggedFriend = null
                                            isHoveringGroupEvents = false
                                        }
                                    )
                                }
                                .padding(horizontal = 4.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Buddy Pet Thumbnail
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF101010)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (petScreenshot != null) {
                                        Image(
                                            bitmap = petScreenshot.asImageBitmap(),
                                            contentDescription = friend.displayName,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else {
                                        Text(text = friend.avatarEmoji, fontSize = 20.sp)
                                    }

                                    if (isStaged) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(NothingRed.copy(alpha = 0.45f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = "✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        // Online indicator dot
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(3.dp)
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(if (friend.isOnline) Color(0xFF00E676) else Color.Gray)
                                        )
                                    }
                                }

                                // Name & Level
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = friend.displayName.uppercase(),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStaged) NothingWhite else Color(0xFFDDDDDD),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = if (isStaged) "STAGED" else "LVL ${friend.level}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isStaged) NothingRed else NothingTextTertiary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Group Events Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        groupEventsBoundsInRoot = coords.boundsInRoot()
                    },
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val stagedFriends = remember(stagedInviteCodes, friends) {
                    friends.filter { it.buddyCode in stagedInviteCodes }
                }

                // Top Header Row with spacious breathing room
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

                    // Top button stays strictly "+ NEW EVENT" (no transforming, spacious breathing room)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingRed)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCreateDialog = true
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
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

                // Drop indicator banner when hovering (NO emojis, spacious breathing room)
                AnimatedVisibility(
                    visible = isHoveringGroupEvents,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = NothingRed.copy(alpha = 0.18f),
                        border = BorderStroke(1.5.dp, NothingRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RELEASE TO DROP ${draggedFriend?.displayName ?: "FRIEND"} INTO SQUAD EVENT",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.8.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Dedicated Staged Friends Container (Multi-drop confirmation zone with generous breathing room)
                if (stagedFriends.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF141414),
                        border = BorderStroke(1.5.dp, NothingRed.copy(alpha = 0.85f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(NothingRed)
                                    )
                                    Text(
                                        text = "STAGED SQUAD MEMBERS [ ${stagedFriends.size} ]",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingWhite,
                                        letterSpacing = 1.sp
                                    )
                                }

                                Text(
                                    text = "CLEAR ALL",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingTextTertiary,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            stagedInviteCodes = emptySet()
                                            triggerTick()
                                            Toast.makeText(context, "STAGED FRIENDS CLEARED", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }

                            // Horizontal list of staged friends with comfortable chip spacing
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(stagedFriends) { friend ->
                                    val petCatalog = remember { AccountAndInventoryManager.petCatalog }
                                    val deterministicPetId = remember(friend.buddyCode) {
                                        val idx = abs(friend.buddyCode.hashCode()) % petCatalog.size
                                        petCatalog[idx].id
                                    }
                                    val petScreenshot = remember(deterministicPetId) {
                                        Buddy3DManager.getBuddyScreenshotBitmap(context, deterministicPetId)
                                    }

                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF202020))
                                            .border(1.dp, NothingRed.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (petScreenshot != null) {
                                            Image(
                                                bitmap = petScreenshot.asImageBitmap(),
                                                contentDescription = friend.displayName,
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(RoundedCornerShape(5.dp))
                                            )
                                        } else {
                                            Text(text = friend.avatarEmoji, fontSize = 14.sp)
                                        }

                                        Text(
                                            text = friend.displayName,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NothingWhite
                                        )

                                        Box(
                                            modifier = Modifier
                                                .clip(CircleShape)
                                                .clickable {
                                                    stagedInviteCodes = stagedInviteCodes - friend.buddyCode
                                                    triggerTick()
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✕",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingRed
                                            )
                                        }
                                    }
                                }
                            }

                            // Confirm & Create Event Button with generous breathing room
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingRed)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        triggerConfirmPulse()
                                        showCreateDialog = true
                                    }
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CONFIRM & CREATE EVENT (${stagedFriends.size} INVITED) →",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }

                if (events.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isHoveringGroupEvents) NothingRed.copy(alpha = 0.10f) else NothingCardSurface,
                        border = BorderStroke(
                            if (isHoveringGroupEvents) 2.dp else 1.dp,
                            if (isHoveringGroupEvents) NothingRed else NothingBorder
                        ),
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
                                text = if (isHoveringGroupEvents) "DROP FRIEND HERE TO STAGE FOR EVENT" else "NO ACTIVE SQUAD EVENTS",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isHoveringGroupEvents) Color.White else NothingTextSecondary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = if (isHoveringGroupEvents) "RELEASE TO ADD TO STAGED SQUAD MEMBERS" else "TAP '+ NEW EVENT' OR DRAG FRIENDS HERE TO SCHEDULE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = if (isHoveringGroupEvents) NothingRed else NothingTextTertiary
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

        // Floating Drag Ghost Overlay (Easter Egg Drag-to-Events)
        if (draggedFriend != null) {
            val activeFriend = draggedFriend!!
            val petCatalog = remember { AccountAndInventoryManager.petCatalog }
            val deterministicPetId = remember(activeFriend.buddyCode) {
                val idx = abs(activeFriend.buddyCode.hashCode()) % petCatalog.size
                petCatalog[idx].id
            }
            val petScreenshot = remember(deterministicPetId) {
                Buddy3DManager.getBuddyScreenshotBitmap(context, deterministicPetId)
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9999f)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF181818).copy(alpha = 0.95f),
                    border = BorderStroke(
                        width = 2.dp,
                        color = if (isHoveringGroupEvents) NothingRed else NothingRed.copy(alpha = 0.7f)
                    ),
                    shadowElevation = 18.dp,
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragTouchInRoot.x - 70.dp.toPx()).roundToInt(),
                                (dragTouchInRoot.y - 70.dp.toPx()).roundToInt()
                            )
                        }
                        .width(140.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF101010)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (petScreenshot != null) {
                                Image(
                                    bitmap = petScreenshot.asImageBitmap(),
                                    contentDescription = activeFriend.displayName,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                )
                            } else {
                                Text(text = activeFriend.avatarEmoji, fontSize = 22.sp)
                            }
                        }

                        Text(
                            text = activeFriend.displayName.uppercase(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = if (isHoveringGroupEvents) "RELEASE TO DROP" else "DRAGGING TO EVENTS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isHoveringGroupEvents) NothingRed else NothingTextSecondary,
                            letterSpacing = 0.5.sp
                        )
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
            initialInvitedCodes = stagedInviteCodes,
            onDismiss = { showCreateDialog = false },
            onEventCreated = { newEvent ->
                stagedInviteCodes = emptySet()
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

