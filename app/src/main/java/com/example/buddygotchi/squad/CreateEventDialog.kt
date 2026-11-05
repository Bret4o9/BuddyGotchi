package com.example.buddygotchi.squad

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.example.buddygotchi.AccountAndInventoryManager
import com.example.buddygotchi.Buddy3DManager
import com.example.buddygotchi.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEventDialog(
    currentUser: UserProfile,
    friends: List<SquadFriend>,
    initialInvitedCodes: Set<String> = emptySet(),
    onDismiss: () -> Unit,
    onEventCreated: (GroupEvent) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

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
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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

    fun triggerSnap() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(VibrationEffect.createOneShot(25, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) {}
    }

    // --- Page 1 State: Title & Location ---
    var titleInput by remember { mutableStateOf("") }
    var locationInput by remember { mutableStateOf("") }
    var selectedLatitude by remember { mutableStateOf<Double?>(null) }
    var selectedLongitude by remember { mutableStateOf<Double?>(null) }
    var showMapPicker by remember { mutableStateOf(false) }
    var isResolvingLocation by remember { mutableStateOf(false) }

    // --- Page 2 State: Schedule (Date, Drum Time & Offset) ---
    var baseDate by remember { mutableStateOf(Date()) }
    var selectedDayOffset by remember { mutableIntStateOf(1) } // default Tomorrow
    var isCustomDateSelected by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }

    // Live/Initial Time
    val initialCal = remember {
        Calendar.getInstance().apply {
            add(Calendar.HOUR_OF_DAY, 2)
            set(Calendar.MINUTE, 0)
        }
    }
    var hourFloat by remember { mutableFloatStateOf(initialCal.get(Calendar.HOUR_OF_DAY).toFloat()) }
    var minuteFloat by remember { mutableFloatStateOf(initialCal.get(Calendar.MINUTE).toFloat()) }
    var lastTickedHour by remember { mutableIntStateOf(initialCal.get(Calendar.HOUR_OF_DAY)) }
    var lastTickedMinute by remember { mutableIntStateOf(initialCal.get(Calendar.MINUTE)) }

    val hourVelocityTracker = remember { VelocityTracker() }
    val minuteVelocityTracker = remember { VelocityTracker() }
    var accumulatedFastMinuteDragY by remember { mutableFloatStateOf(0f) }
    var wasFastMinuteDrag by remember { mutableStateOf(false) }

    // Alarm Reminder Offset
    val offsetOptions = remember { listOf(0, 5, 10, 15, 30, 45, 60) }
    var selectedOffsetMinutes by remember { mutableIntStateOf(15) }

    // Current selected time wrapped cleanly
    val currentSelectedHour = ((hourFloat.roundToInt() % 24) + 24) % 24
    val currentSelectedMinute = ((minuteFloat.roundToInt() % 60) + 60) % 60

    // Calendar target calculation
    val finalCal = remember(baseDate, selectedDayOffset, currentSelectedHour, currentSelectedMinute) {
        Calendar.getInstance().apply {
            time = baseDate
            add(Calendar.DAY_OF_YEAR, selectedDayOffset)
            set(Calendar.HOUR_OF_DAY, currentSelectedHour)
            set(Calendar.MINUTE, currentSelectedMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // --- Page 3 State: Invite Squad Members ---
    // If friends list is empty, provide default squad mates so user can test and enjoy the 3x3 grid
    val effectiveFriends = remember(friends) {
        if (friends.isNotEmpty()) friends else listOf(
            SquadFriend("BG-4182", "POLI", 12, isOnline = true),
            SquadFriend("BG-7731", "SANI", 16, isOnline = true),
            SquadFriend("BG-9055", "GERI", 8, isOnline = false),
            SquadFriend("BG-1204", "ALEX", 10, isOnline = true),
            SquadFriend("BG-3391", "VIKI", 14, isOnline = true),
            SquadFriend("BG-8842", "NICK", 11, isOnline = false),
            SquadFriend("BG-5521", "LILI", 9, isOnline = true),
            SquadFriend("BG-6710", "DANI", 15, isOnline = true),
            SquadFriend("BG-2209", "MAYA", 13, isOnline = false)
        )
    }

    var selectedFriendCodes by remember(initialInvitedCodes) {
        mutableStateOf(initialInvitedCodes)
    }

    // Drag-and-drop state for Page 3
    var draggedFriend by remember { mutableStateOf<SquadFriend?>(null) }
    var dragGhostOffset by remember { mutableStateOf(Offset.Zero) }
    var containerBoundsInRoot by remember { mutableStateOf<Rect?>(null) }
    var isHoveringContainer by remember { mutableStateOf(false) }

    // Multi-Page Pager (3 Steps)
    val pagerState = rememberPagerState(pageCount = { 3 })

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
        val imm = remember { context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager }

        val hideKeyboard: () -> Unit = {
            focusManager.clearFocus(force = true)
            keyboardController?.hide()
            view.clearFocus()
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
            imm?.hideSoftInputFromWindow(view.applicationWindowToken, 0)
            dialogWindow?.let { imm?.hideSoftInputFromWindow(it.decorView.windowToken, 0) }
            (context as? Activity)?.let { imm?.hideSoftInputFromWindow(it.window.decorView.windowToken, 0) }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f)
                .imePadding()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        hideKeyboard()
                    })
                },
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Header
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Text(
                            text = "CREATE SQUAD EVENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = NothingWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", fontSize = 11.sp, color = NothingTextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Multi-Page 3D Drum Roll Horizontal Pager
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false, // Clean button/spin navigation without gesture conflicts
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val pageOffset = ((pageIndex - pagerState.currentPage) - pagerState.currentPageOffsetFraction)
                                    val absOffset = abs(pageOffset).coerceIn(0f, 1f)
                                    val clampedOffset = pageOffset.coerceIn(-1f, 1f)

                                    // 3D Drum roll cylinder rotation
                                    rotationY = clampedOffset * 55f
                                    cameraDistance = 14f * density.density

                                    // Cylinder scale: curves away into depth
                                    val scale = (1f - absOffset * 0.15f).coerceIn(0.82f, 1f)
                                    scaleX = scale
                                    scaleY = scale
                                    alpha = (1f - absOffset * 0.6f).coerceIn(0f, 1f)
                                }
                        ) {
                            when (pageIndex) {
                                // ============================================================
                                // PAGE 1: EVENT TITLE & LOCATION (No suggested names)
                                // ============================================================
                                0 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        // Title Section
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "EVENT TITLE",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingTextSecondary,
                                                letterSpacing = 1.sp
                                            )
                                            BasicTextField(
                                                value = titleInput,
                                                onValueChange = { input ->
                                                    if (input.contains("\n") || input.contains("\r")) {
                                                        titleInput = input.replace("\r", "").replace("\n", "").uppercase()
                                                        hideKeyboard()
                                                    } else {
                                                        titleInput = input.uppercase()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF141414))
                                                    .border(
                                                        1.dp,
                                                        if (titleInput.isNotBlank()) NothingRed else Color(0xFF2B2B2B),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.key == Key.Enter || keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                                                            hideKeyboard()
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                textStyle = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingWhite,
                                                    letterSpacing = 1.sp
                                                ),
                                                cursorBrush = SolidColor(NothingRed),
                                                keyboardOptions = KeyboardOptions(
                                                    capitalization = KeyboardCapitalization.Characters,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onNext = { hideKeyboard() },
                                                    onDone = { hideKeyboard() },
                                                    onGo = { hideKeyboard() }
                                                ),
                                                singleLine = true,
                                                decorationBox = { innerTextField ->
                                                    if (titleInput.isEmpty()) {
                                                        Text(
                                                            text = "ENTER SQUAD EVENT TITLE...",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 12.sp,
                                                            color = NothingTextTertiary.copy(alpha = 0.5f),
                                                            letterSpacing = 1.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )
                                        }

                                        // Location Section
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "LOCATION // PIN",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingTextSecondary,
                                                    letterSpacing = 1.sp
                                                )

                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Quick Paste Link Button
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(Color(0xFF202020))
                                                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                val clip = clipboardManager.getText()?.text
                                                                if (!clip.isNullOrBlank()) {
                                                                    if (GoogleMapsLinkResolver.isResolvable(clip)) {
                                                                        isResolvingLocation = true
                                                                        locationInput = "RESOLVING LINK..."
                                                                        coroutineScope.launch {
                                                                            val res = GoogleMapsLinkResolver.resolve(clip)
                                                                            if (res != null) {
                                                                                locationInput = res.title
                                                                                selectedLatitude = res.latitude
                                                                                selectedLongitude = res.longitude
                                                                            } else {
                                                                                locationInput = clip.uppercase()
                                                                            }
                                                                            isResolvingLocation = false
                                                                        }
                                                                    } else {
                                                                        locationInput = clip.uppercase()
                                                                    }
                                                                }
                                                            }
                                                            .padding(horizontal = 7.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isResolvingLocation) "● RESOLVING..." else "📋 PASTE LINK",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isResolvingLocation) NothingRed else NothingTextSecondary
                                                        )
                                                    }

                                                    // Map Picker Button
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (selectedLatitude != null) NothingRed.copy(alpha = 0.25f) else Color(0xFF202020))
                                                            .border(1.dp, if (selectedLatitude != null) NothingRed else Color(0xFF333333), RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                focusManager.clearFocus()
                                                                showMapPicker = true
                                                            }
                                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                                    ) {
                                                        Text(
                                                            text = if (selectedLatitude != null) "📍 PIN SET [ ⌖ ]" else "📍 PICK ON MAP",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 8.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (selectedLatitude != null) NothingWhite else NothingTextSecondary
                                                        )
                                                    }
                                                }
                                            }

                                            BasicTextField(
                                                value = locationInput,
                                                onValueChange = { input ->
                                                    if (input.contains("\n") || input.contains("\r")) {
                                                        val clean = input.replace("\r", "").replace("\n", "")
                                                        locationInput = clean
                                                        hideKeyboard()
                                                    } else {
                                                        val trimmed = input.trim()
                                                        if (GoogleMapsLinkResolver.isResolvable(trimmed)) {
                                                            locationInput = "● RESOLVING LINK..."
                                                            isResolvingLocation = true
                                                            coroutineScope.launch {
                                                                val res = GoogleMapsLinkResolver.resolve(trimmed)
                                                                if (res != null) {
                                                                    locationInput = res.title
                                                                    selectedLatitude = res.latitude
                                                                    selectedLongitude = res.longitude
                                                                } else {
                                                                    locationInput = input.uppercase()
                                                                }
                                                                isResolvingLocation = false
                                                            }
                                                        } else {
                                                            locationInput = input
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(Color(0xFF141414))
                                                    .border(
                                                        1.dp,
                                                        if (locationInput.isNotBlank()) Color(0xFF444444) else Color(0xFF2B2B2B),
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                                    .onKeyEvent { keyEvent ->
                                                        if (keyEvent.key == Key.Enter || keyEvent.nativeKeyEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                                                            hideKeyboard()
                                                            true
                                                        } else {
                                                            false
                                                        }
                                                    },
                                                textStyle = TextStyle(
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingWhite,
                                                    letterSpacing = 0.8.sp
                                                ),
                                                cursorBrush = SolidColor(NothingRed),
                                                keyboardOptions = KeyboardOptions(
                                                    capitalization = KeyboardCapitalization.None,
                                                    imeAction = ImeAction.Done
                                                ),
                                                keyboardActions = KeyboardActions(
                                                    onDone = { hideKeyboard() },
                                                    onNext = { hideKeyboard() },
                                                    onGo = { hideKeyboard() },
                                                    onSearch = { hideKeyboard() },
                                                    onSend = { hideKeyboard() }
                                                ),
                                                singleLine = true,
                                                decorationBox = { innerTextField ->
                                                    if (locationInput.isEmpty()) {
                                                        Text(
                                                            text = "ENTER LOCATION OR PASTE LINK...",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 12.sp,
                                                            color = NothingTextTertiary.copy(alpha = 0.5f),
                                                            letterSpacing = 0.8.sp
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            )

                                            // Pin coordinates attached badge
                                            if (selectedLatitude != null && selectedLongitude != null) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF181818))
                                                        .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = String.format(Locale.US, "📍 PIN: %.4f, %.4f", selectedLatitude, selectedLongitude),
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = NothingRed
                                                    )
                                                    Text(
                                                        text = "✕ REMOVE PIN",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.sp,
                                                        color = NothingTextTertiary,
                                                        modifier = Modifier.clickable {
                                                            selectedLatitude = null
                                                            selectedLongitude = null
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // ============================================================
                                // PAGE 2: SCHEDULE (Date Tab with More, Drum Clock & Offset)
                                // ============================================================
                                1 -> {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState()),
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        // 1. DATE SELECTOR (Today, Tomorrow, More -> Calendar)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = "EVENT DATE",
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
                                                val isTodaySelected = selectedDayOffset == 0 && !isCustomDateSelected
                                                val isTomorrowSelected = selectedDayOffset == 1 && !isCustomDateSelected
                                                val isMoreSelected = isCustomDateSelected || (selectedDayOffset > 1)

                                                // Today
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isTodaySelected) NothingRed else Color(0xFF181818))
                                                        .border(1.dp, if (isTodaySelected) NothingRed else Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            selectedDayOffset = 0
                                                            isCustomDateSelected = false
                                                            triggerTick()
                                                        }
                                                        .padding(vertical = 9.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "TODAY",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isTodaySelected) Color.White else NothingTextSecondary
                                                    )
                                                }

                                                // Tomorrow
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isTomorrowSelected) NothingRed else Color(0xFF181818))
                                                        .border(1.dp, if (isTomorrowSelected) NothingRed else Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            selectedDayOffset = 1
                                                            isCustomDateSelected = false
                                                            triggerTick()
                                                        }
                                                        .padding(vertical = 9.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "TOMORROW",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isTomorrowSelected) Color.White else NothingTextSecondary
                                                    )
                                                }

                                                // More (Calendar Picker)
                                                val moreLabel = if (isMoreSelected) {
                                                    SimpleDateFormat("MMM d", Locale.getDefault()).format(finalCal.time).uppercase()
                                                } else {
                                                    "MORE 📅"
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(if (isMoreSelected) NothingRed else Color(0xFF181818))
                                                        .border(1.dp, if (isMoreSelected) NothingRed else Color(0xFF2A2A2A), RoundedCornerShape(10.dp))
                                                        .clickable {
                                                            showDatePickerDialog = true
                                                            triggerTick()
                                                        }
                                                        .padding(vertical = 9.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = moreLabel,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isMoreSelected) Color.White else NothingTextSecondary
                                                    )
                                                }
                                            }
                                        }

                                        // 2. DRUM ROLL CLOCK TIME SELECTOR (Exact ClockWidget Drum Wheel)
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "EVENT TIME (DRUM ROLL)",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingTextSecondary,
                                                    letterSpacing = 1.sp
                                                )
                                                Text(
                                                    text = String.format(Locale.getDefault(), "%02d:%02d", currentSelectedHour, currentSelectedMinute),
                                                    fontFamily = Dseg7FontFamily,
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingRed
                                                )
                                            }

                                            val drumHeightDp = 130.dp
                                            val itemHeightDp = 40.dp
                                            val itemHeightPx = with(density) { itemHeightDp.toPx() }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(drumHeightDp)
                                                    .clip(RoundedCornerShape(14.dp))
                                                    .background(Color(0xFF121212))
                                                    .border(1.dp, Color(0xFF242424), RoundedCornerShape(14.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                // Center Guide brackets
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = "›", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NothingRed.copy(alpha = 0.5f))
                                                    Text(text = "‹", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = NothingRed.copy(alpha = 0.5f))
                                                }

                                                // Columns Row
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 28.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    // Left Column: HOURS (00..23)
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .pointerInput(Unit) {
                                                                detectDragGestures(
                                                                    onDragStart = { hourVelocityTracker.resetTracking() },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        hourVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                                                        val deltaY = dragAmount.y
                                                                        val speed = abs(deltaY)
                                                                        val excessSpeed = (speed - 5f).coerceAtLeast(0f)
                                                                        val acceleration = (1.0f + (excessSpeed / 14f)).coerceIn(1.0f, 2.8f)
                                                                        val basePxPerHour = with(density) { 44.dp.toPx() }
                                                                        val hourDelta = (-deltaY / basePxPerHour) * acceleration
                                                                        hourFloat += hourDelta

                                                                        val h = ((hourFloat.roundToInt() % 24) + 24) % 24
                                                                        if (h != lastTickedHour) {
                                                                            triggerTick()
                                                                            lastTickedHour = h
                                                                        }
                                                                    },
                                                                    onDragEnd = {
                                                                        val velocity = hourVelocityTracker.calculateVelocity().y
                                                                        val flingHours = if (abs(velocity) > 600f) {
                                                                            val sign = if (velocity < 0) 1f else -1f
                                                                            val excessVelocity = abs(velocity) - 600f
                                                                            sign * (excessVelocity / 900f).coerceIn(0f, 6f)
                                                                        } else 0f

                                                                        val targetHour = (hourFloat + flingHours).roundToInt()
                                                                        coroutineScope.launch {
                                                                            Animatable(hourFloat).animateTo(
                                                                                targetValue = targetHour.toFloat(),
                                                                                animationSpec = spring(
                                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                                    stiffness = Spring.StiffnessMediumLow
                                                                                )
                                                                            ) {
                                                                                hourFloat = this.value
                                                                                val h = ((hourFloat.roundToInt() % 24) + 24) % 24
                                                                                if (h != lastTickedHour) {
                                                                                    triggerTick()
                                                                                    lastTickedHour = h
                                                                                }
                                                                            }
                                                                            hourFloat = targetHour.toFloat()
                                                                            triggerConfirmPulse()
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val centerIntHour = hourFloat.roundToInt()
                                                        val subHourFraction = hourFloat - centerIntHour

                                                        for (k in -2..2) {
                                                            val relPos = k - subHourFraction
                                                            val yOffset = relPos * itemHeightPx
                                                            val slotHour = (((centerIntHour + k) % 24) + 24) % 24
                                                            val slotHourString = String.format(Locale.getDefault(), "%02d", slotHour)

                                                            val absDist = abs(relPos)
                                                            val scale = (1f - absDist * 0.15f).coerceIn(0.70f, 1f)
                                                            val alpha = (1f - absDist * 0.42f).coerceIn(0.08f, 1f)
                                                            val rotX = (-relPos * 25f).coerceIn(-65f, 65f)

                                                            Box(
                                                                modifier = Modifier
                                                                    .offset { IntOffset(0, yOffset.roundToInt()) }
                                                                    .graphicsLayer {
                                                                        rotationX = rotX
                                                                        scaleX = scale
                                                                        scaleY = scale
                                                                        this.alpha = alpha
                                                                        cameraDistance = 16f * density.density
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = slotHourString,
                                                                    fontFamily = Dseg7FontFamily,
                                                                    fontSize = if (k == 0) 42.sp else 24.sp,
                                                                    fontWeight = if (k == 0) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (k == 0) NothingWhite else NothingTextTertiary
                                                                )
                                                            }
                                                        }
                                                    }

                                                    // Center Colon Divider
                                                    Box(
                                                        modifier = Modifier
                                                            .width(20.dp)
                                                            .fillMaxHeight(),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = ":",
                                                            fontFamily = Dseg7FontFamily,
                                                            fontSize = 36.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = NothingRed
                                                        )
                                                    }

                                                    // Right Column: MINUTES (00..59)
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .fillMaxHeight()
                                                            .pointerInput(Unit) {
                                                                detectDragGestures(
                                                                    onDragStart = {
                                                                        minuteVelocityTracker.resetTracking()
                                                                        accumulatedFastMinuteDragY = 0f
                                                                        wasFastMinuteDrag = false
                                                                    },
                                                                    onDrag = { change, dragAmount ->
                                                                        change.consume()
                                                                        minuteVelocityTracker.addPosition(change.uptimeMillis, change.position)
                                                                        val deltaY = dragAmount.y
                                                                        val speed = abs(deltaY)
                                                                        if (speed > 25f) wasFastMinuteDrag = true

                                                                        val basePxPerMinute = with(density) { 34.dp.toPx() }
                                                                        val excessSpeed = (speed - 5f).coerceAtLeast(0f)
                                                                        val acceleration = (1.0f + (excessSpeed / 10f)).coerceIn(1.0f, 3.2f)
                                                                        val minuteDelta = (-deltaY / basePxPerMinute) * acceleration
                                                                        minuteFloat += minuteDelta

                                                                        val m = ((minuteFloat.roundToInt() % 60) + 60) % 60
                                                                        if (m != lastTickedMinute) {
                                                                            if (m % 5 == 0) triggerSnap() else triggerTick()
                                                                            lastTickedMinute = m
                                                                        }
                                                                    },
                                                                    onDragEnd = {
                                                                        val velocity = minuteVelocityTracker.calculateVelocity().y
                                                                        val isFastFling = abs(velocity) > 600f || wasFastMinuteDrag

                                                                        val targetMinute = if (isFastFling) {
                                                                            val flingSteps = if (abs(velocity) > 600f) {
                                                                                val sign = if (velocity < 0) 1f else -1f
                                                                                val excessVelocity = abs(velocity) - 600f
                                                                                sign * (excessVelocity / 300f).coerceIn(0f, 12f)
                                                                            } else 0f
                                                                            val rawTarget = minuteFloat + flingSteps * 5f
                                                                            ((rawTarget / 5f).roundToInt() * 5)
                                                                        } else {
                                                                            minuteFloat.roundToInt()
                                                                        }

                                                                        accumulatedFastMinuteDragY = 0f
                                                                        wasFastMinuteDrag = false

                                                                        coroutineScope.launch {
                                                                            Animatable(minuteFloat).animateTo(
                                                                                targetValue = targetMinute.toFloat(),
                                                                                animationSpec = spring(
                                                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                                                    stiffness = if (isFastFling) Spring.StiffnessMediumLow else Spring.StiffnessMedium
                                                                                )
                                                                            ) {
                                                                                minuteFloat = this.value
                                                                                val m = ((minuteFloat.roundToInt() % 60) + 60) % 60
                                                                                if (m != lastTickedMinute) {
                                                                                    if (m % 5 == 0) triggerSnap() else triggerTick()
                                                                                    lastTickedMinute = m
                                                                                }
                                                                            }
                                                                            minuteFloat = targetMinute.toFloat()
                                                                            if (targetMinute % 5 == 0) triggerSnap() else triggerConfirmPulse()
                                                                        }
                                                                    }
                                                                )
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        val centerIntMin = minuteFloat.roundToInt()
                                                        val subMinFraction = minuteFloat - centerIntMin

                                                        for (k in -2..2) {
                                                            val relPos = k - subMinFraction
                                                            val yOffset = relPos * itemHeightPx
                                                            val slotMin = (((centerIntMin + k) % 60) + 60) % 60
                                                            val slotMinString = String.format(Locale.getDefault(), "%02d", slotMin)

                                                            val absDist = abs(relPos)
                                                            val scale = (1f - absDist * 0.15f).coerceIn(0.70f, 1f)
                                                            val alpha = (1f - absDist * 0.42f).coerceIn(0.08f, 1f)
                                                            val rotX = (-relPos * 25f).coerceIn(-65f, 65f)

                                                            Box(
                                                                modifier = Modifier
                                                                    .offset { IntOffset(0, yOffset.roundToInt()) }
                                                                    .graphicsLayer {
                                                                        rotationX = rotX
                                                                        scaleX = scale
                                                                        scaleY = scale
                                                                        this.alpha = alpha
                                                                        cameraDistance = 16f * density.density
                                                                    },
                                                                contentAlignment = Alignment.Center
                                                            ) {
                                                                Text(
                                                                    text = slotMinString,
                                                                    fontFamily = Dseg7FontFamily,
                                                                    fontSize = if (k == 0) 42.sp else 24.sp,
                                                                    fontWeight = if (k == 0) FontWeight.Bold else FontWeight.Normal,
                                                                    color = if (k == 0) NothingWhite else NothingTextTertiary
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 3. ALARM REMINDER OFFSET (From Clock Widget)
                                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "ALARM REMINDER OFFSET",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingTextSecondary,
                                                    letterSpacing = 1.sp
                                                )
                                                Text(
                                                    text = if (selectedOffsetMinutes == 0) "AT EVENT TIME" else "-${selectedOffsetMinutes}M BEFORE",
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NothingRed
                                                )
                                            }

                                            // Offset Pills
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                offsetOptions.forEach { offset ->
                                                    val isSelected = selectedOffsetMinutes == offset
                                                    val label = when (offset) {
                                                        0 -> "0M"
                                                        60 -> "1H"
                                                        else -> "${offset}M"
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) NothingRed else Color(0xFF181818))
                                                            .border(1.dp, if (isSelected) NothingRed else Color(0xFF282828), RoundedCornerShape(8.dp))
                                                            .clickable {
                                                                selectedOffsetMinutes = offset
                                                                triggerTick()
                                                            }
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
                                    }
                                }

                                // ============================================================
                                // PAGE 3: INVITE SQUAD MEMBERS (3x3 Grid & Drag-to-Container)
                                // ============================================================
                                2 -> {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        // Grid Title
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "SQUAD MEMBERS (DRAG OR TAP TO INVITE)",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingTextSecondary,
                                                letterSpacing = 1.sp
                                            )
                                            Text(
                                                text = "${selectedFriendCodes.size} INVITED",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NothingRed
                                            )
                                        }

                                        // 3x3 Visible Grid of Squad Members
                                        val petCatalog = remember { AccountAndInventoryManager.petCatalog }
                                        LazyVerticalGrid(
                                            columns = GridCells.Fixed(3),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(effectiveFriends) { friend ->
                                                val isInvited = friend.buddyCode in selectedFriendCodes
                                                val deterministicPetId = remember(friend.buddyCode) {
                                                    val idx = abs(friend.buddyCode.hashCode()) % petCatalog.size
                                                    petCatalog[idx].id
                                                }
                                                val petScreenshot = remember(deterministicPetId) {
                                                    Buddy3DManager.getBuddyScreenshotBitmap(context, deterministicPetId)
                                                }

                                                var localDragOffset by remember { mutableStateOf(Offset.Zero) }
                                                var isBeingDragged by remember { mutableStateOf(false) }

                                                Box(
                                                    modifier = Modifier
                                                        .offset { IntOffset(localDragOffset.x.roundToInt(), localDragOffset.y.roundToInt()) }
                                                        .zIndex(if (isBeingDragged) 100f else 1f)
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (isInvited) NothingRed.copy(alpha = 0.16f) else Color(0xFF181818))
                                                        .border(
                                                            width = if (isInvited) 1.5.dp else 1.dp,
                                                            color = if (isInvited) NothingRed else Color(0xFF2B2B2B),
                                                            shape = RoundedCornerShape(12.dp)
                                                        )
                                                        .clickable {
                                                            selectedFriendCodes = if (isInvited) {
                                                                selectedFriendCodes - friend.buddyCode
                                                            } else {
                                                                triggerTick()
                                                                selectedFriendCodes + friend.buddyCode
                                                            }
                                                        }
                                                        .pointerInput(friend.buddyCode) {
                                                            detectDragGestures(
                                                                onDragStart = {
                                                                    isBeingDragged = true
                                                                    draggedFriend = friend
                                                                    triggerTick()
                                                                },
                                                                onDrag = { change, dragAmount ->
                                                                    change.consume()
                                                                    localDragOffset += dragAmount
                                                                    dragGhostOffset += dragAmount
                                                                    // Check if dragged down near container
                                                                    if (localDragOffset.y > 60f) {
                                                                        isHoveringContainer = true
                                                                    }
                                                                },
                                                                onDragEnd = {
                                                                    if (localDragOffset.y > 50f || isHoveringContainer) {
                                                                        if (friend.buddyCode !in selectedFriendCodes) {
                                                                            selectedFriendCodes = selectedFriendCodes + friend.buddyCode
                                                                            triggerConfirmPulse()
                                                                        }
                                                                    }
                                                                    localDragOffset = Offset.Zero
                                                                    isBeingDragged = false
                                                                    draggedFriend = null
                                                                    isHoveringContainer = false
                                                                },
                                                                onDragCancel = {
                                                                    localDragOffset = Offset.Zero
                                                                    isBeingDragged = false
                                                                    draggedFriend = null
                                                                    isHoveringContainer = false
                                                                }
                                                            )
                                                        }
                                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                                    ) {
                                                        // Buddy Pet Thumbnail
                                                        Box(
                                                            modifier = Modifier
                                                                .size(46.dp)
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
                                                                Text(text = friend.avatarEmoji, fontSize = 22.sp)
                                                            }

                                                            if (isInvited) {
                                                                Box(
                                                                    modifier = Modifier
                                                                        .fillMaxSize()
                                                                        .background(NothingRed.copy(alpha = 0.4f)),
                                                                    contentAlignment = Alignment.Center
                                                                ) {
                                                                    Text(text = "✓", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }

                                                        // Name & Level
                                                        Text(
                                                            text = friend.displayName.uppercase(),
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isInvited) NothingWhite else Color(0xFFDDDDDD),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )

                                                        Text(
                                                            text = if (isInvited) "INVITED" else "LVL ${friend.level}",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 7.5.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isInvited) NothingRed else NothingTextTertiary
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Container Below: INVITED SQUAD DROP ZONE
                                        val invitedFriends = remember(selectedFriendCodes, effectiveFriends) {
                                            effectiveFriends.filter { it.buddyCode in selectedFriendCodes }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(min = 90.dp, max = 110.dp)
                                                .onGloballyPositioned { coords ->
                                                    containerBoundsInRoot = coords.boundsInRoot()
                                                },
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isHoveringContainer) NothingRed.copy(alpha = 0.12f) else Color(0xFF141414),
                                            border = BorderStroke(
                                                width = if (isHoveringContainer) 2.dp else 1.dp,
                                                color = if (isHoveringContainer) NothingRed else Color(0xFF2C2C2C)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(10.dp),
                                                verticalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = if (isHoveringContainer) "⬇ DROP MEMBER HERE TO INVITE" else "INVITED SQUAD CONTAINER [${invitedFriends.size}]",
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 8.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (isHoveringContainer) NothingRed else NothingTextSecondary,
                                                        letterSpacing = 0.8.sp
                                                    )
                                                    if (invitedFriends.isNotEmpty()) {
                                                        Text(
                                                            text = "CLEAR ALL",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 8.sp,
                                                            color = NothingTextTertiary,
                                                            modifier = Modifier.clickable {
                                                                selectedFriendCodes = emptySet()
                                                                triggerTick()
                                                            }
                                                        )
                                                    }
                                                }

                                                if (invitedFriends.isEmpty()) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 12.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "DRAG SQUAD MEMBERS HERE OR TAP TO INVITE",
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 9.sp,
                                                            color = NothingTextTertiary,
                                                            letterSpacing = 0.5.sp,
                                                            textAlign = TextAlign.Center
                                                        )
                                                    }
                                                } else {
                                                    LazyRow(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        items(invitedFriends) { friend ->
                                                            val deterministicPetId = remember(friend.buddyCode) {
                                                                val idx = abs(friend.buddyCode.hashCode()) % petCatalog.size
                                                                petCatalog[idx].id
                                                            }
                                                            val petScreenshot = remember(deterministicPetId) {
                                                                Buddy3DManager.getBuddyScreenshotBitmap(context, deterministicPetId)
                                                            }

                                                            Row(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(8.dp))
                                                                    .background(Color(0xFF222222))
                                                                    .border(1.dp, NothingRed.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                            ) {
                                                                if (petScreenshot != null) {
                                                                    Image(
                                                                        bitmap = petScreenshot.asImageBitmap(),
                                                                        contentDescription = friend.displayName,
                                                                        modifier = Modifier
                                                                            .size(20.dp)
                                                                            .clip(RoundedCornerShape(4.dp))
                                                                    )
                                                                } else {
                                                                    Text(text = friend.avatarEmoji, fontSize = 12.sp)
                                                                }

                                                                Text(
                                                                    text = friend.displayName,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    fontSize = 9.5.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = NothingWhite
                                                                )

                                                                Text(
                                                                    text = "✕",
                                                                    fontSize = 10.sp,
                                                                    color = NothingRed,
                                                                    modifier = Modifier
                                                                        .clickable {
                                                                            selectedFriendCodes = selectedFriendCodes - friend.buddyCode
                                                                            triggerTick()
                                                                        }
                                                                        .padding(start = 2.dp)
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
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Step Navigation Bar (With 3D Drum Roll Transition Trigger)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (pagerState.currentPage) {
                        0 -> {
                            val canProceed = titleInput.trim().isNotBlank()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (canProceed) NothingRed else Color(0xFF222222))
                                    .clickable(enabled = canProceed) {
                                        hideKeyboard()
                                        triggerConfirmPulse()
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                1,
                                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NEXT: SCHEDULE →",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = if (canProceed) Color.White else NothingTextTertiary
                                )
                            }
                        }
                        1 -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF222222))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                                    .clickable {
                                        triggerTick()
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                0,
                                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "← BACK",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingTextSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.5f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingRed)
                                    .clickable {
                                        triggerConfirmPulse()
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                2,
                                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "NEXT: INVITE SQUAD →",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = Color.White
                                )
                            }
                        }
                        2 -> {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF222222))
                                    .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                                    .clickable {
                                        triggerTick()
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(
                                                1,
                                                animationSpec = tween(450, easing = FastOutSlowInEasing)
                                            )
                                        }
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "← BACK",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingTextSecondary
                                )
                            }

                            val canCreate = titleInput.trim().isNotBlank()
                            Box(
                                modifier = Modifier
                                    .weight(1.8f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (canCreate) NothingRed else Color(0xFF222222))
                                    .clickable(enabled = canCreate) {
                                        triggerConfirmPulse()

                                        val participants = mutableListOf<EventParticipant>()
                                        // Host participant
                                        participants.add(
                                            EventParticipant(
                                                buddyCode = currentUser.buddyCode,
                                                displayName = currentUser.displayName,
                                                status = ParticipantStatus.ACCEPTED,
                                                isHost = true
                                            )
                                        )
                                        // Invited Friends
                                        effectiveFriends.filter { it.buddyCode in selectedFriendCodes }.forEach { f ->
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
                                            latitude = selectedLatitude,
                                            longitude = selectedLongitude,
                                            eventTime = finalCal.timeInMillis,
                                            reminderOffsetMinutes = selectedOffsetMinutes,
                                            hostBuddyCode = currentUser.buddyCode,
                                            hostDisplayName = currentUser.displayName,
                                            participants = participants
                                        )
                                        onEventCreated(newEvent)
                                        onDismiss()
                                    }
                                    .padding(vertical = 13.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CREATE SQUAD EVENT",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                    color = if (canCreate) Color.White else NothingTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Material 3 Date Picker Dialog (When 'MORE' is clicked)
    if (showDatePickerDialog) {
        val userZone = remember { ZoneId.systemDefault() }
        val todayLocalDate = remember(baseDate) {
            Instant.ofEpochMilli(baseDate.time).atZone(userZone).toLocalDate()
        }
        val currentTargetDate = remember(todayLocalDate, selectedDayOffset) {
            todayLocalDate.plusDays(selectedDayOffset.toLong())
        }
        val initialUtcMillis = remember(currentTargetDate) {
            currentTargetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }

        key(initialUtcMillis) {
            val datePickerState = rememberDatePickerState(
                initialSelectedDateMillis = initialUtcMillis
            )
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val selectedLocalDate = Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                                val diffDays = ChronoUnit.DAYS.between(todayLocalDate, selectedLocalDate).toInt()
                                selectedDayOffset = diffDays.coerceAtLeast(0)
                                isCustomDateSelected = diffDays > 0
                                triggerTick()
                            }
                            showDatePickerDialog = false
                        }
                    ) {
                        Text("OK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }
    }

    // Map Picker Dialog
    if (showMapPicker) {
        NothingMapPickerDialog(
            initialLat = selectedLatitude,
            initialLng = selectedLongitude,
            initialLocationName = locationInput,
            onDismiss = { showMapPicker = false },
            onLocationSelected = { name, lat, lng ->
                locationInput = name
                selectedLatitude = lat
                selectedLongitude = lng
                showMapPicker = false
            }
        )
    }
}
