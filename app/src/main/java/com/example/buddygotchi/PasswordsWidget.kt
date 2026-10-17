package com.example.buddygotchi

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Reusable Nothing-styled Copy button with animated feedback and haptic vibration.
 */
@Composable
fun CopyToClipboardButton(
    textToCopy: String,
    modifier: Modifier = Modifier,
    label: String = "COPY",
    compact: Boolean = false
) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCopied) Color(0xFF00E676).copy(alpha = 0.20f) else NothingSurfaceVariant)
            .border(
                1.dp,
                if (isCopied) Color(0xFF00E676) else NothingBorder,
                RoundedCornerShape(8.dp)
            )
            .clickable {
                if (textToCopy.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(textToCopy))
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isCopied = true
                    scope.launch {
                        delay(1800)
                        isCopied = false
                    }
                }
            }
            .padding(
                horizontal = if (compact) 6.dp else 10.dp,
                vertical = if (compact) 3.dp else 5.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isCopied) "✓" else "⎘",
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 9.sp else 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isCopied) Color(0xFF00E676) else NothingWhite
            )
            Text(
                text = if (isCopied) "COPIED" else label,
                fontFamily = FontFamily.Monospace,
                fontSize = if (compact) 8.sp else 9.5.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = if (isCopied) Color(0xFF00E676) else NothingWhite
            )
        }
    }
}

/**
 * Collapsed Passwords Widget card rendered on the Left / Main Page.
 * Displays no credential previews on the home screen for privacy & security.
 * Features a prominent centered expand button to open the full Password Vault.
 */
@Composable
fun PasswordsCollapsedWidget(
    passwords: List<PasswordItem>,
    onExpand: () -> Unit,
    onQuickCreate: () -> Unit,
    size: WidgetSize = WidgetSize.WIDE,
    modifier: Modifier = Modifier
) {
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
                        text = "${passwords.size}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "KEYS",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
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
                            text = "${passwords.size}",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                        Text(
                            text = "PASSWORDS",
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp,
                            color = NothingTextSecondary
                        )
                    }

                    Text(
                        text = "⚿",
                        fontSize = 18.sp,
                        color = NothingRed
                    )

                    Text(
                        text = "SECURED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.5.sp,
                        color = NothingTextTertiary,
                        letterSpacing = 0.8.sp
                    )
                }
            }

            // === 3. HALF ROW (2x1, 88dp height) ===
            isHalfRow -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
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
                            text = "PASSWORDS [${passwords.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = "SECURED // ENCRYPTED",
                            fontSize = 8.5.sp,
                            color = NothingTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // === 4. HALF BLOCK (2x2, 2x3, 2x4) ===
            isHalfBlock -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding)
                ) {
                    // Header at top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart),
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
                                text = "VAULT [${passwords.size}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                        Text(
                            text = "SECURED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            color = NothingTextTertiary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    // Centered Vault Status Box (no expand button)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingDarkBackground)
                            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "⚿",
                                fontSize = 12.sp,
                                color = NothingRed
                            )
                            Text(
                                text = "${passwords.size} KEYS SECURED",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingTextSecondary
                            )
                        }
                    }

                    // Bottom bar with quick create
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NO PREVIEW",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.5.sp,
                            color = NothingTextTertiary,
                            letterSpacing = 0.6.sp
                        )

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
                                fontSize = 8.sp,
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
                            text = "PASSWORDS [${passwords.size}]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                    }

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

            // === 6. FULL RICH DISPLAY (4x2 WIDE, 4x3, 4x4 MAX) ===
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding)
                ) {
                    // Header Row at top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart),
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
                                text = "PASSWORDS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ ${String.format("%02d", passwords.size)} ]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary
                            )
                        }

                        Text(
                            text = "SECURED // ENCRYPTED",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            letterSpacing = 1.sp,
                            color = NothingTextTertiary
                        )
                    }

                    // Centered Vault Status Box (no EXPAND button)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(55.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NothingDarkBackground)
                            .border(1.dp, NothingBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "⚿",
                                fontSize = 14.sp,
                                color = NothingRed
                            )
                            Text(
                                text = "VAULT SECURED // TAP CARD TO OPEN",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }

                    // Bottom Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (passwords.isEmpty()) "VAULT EMPTY // NO PREVIEW" else "${passwords.size} CREDENTIALS SECURED",
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
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ NEW PASS",
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
 * Fullscreen / Full-page expanded Passwords experience occupying the whole Left Page.
 * Features a password list with one-tap copy, reveal toggle, and a dedicated password editor.
 */
@Composable
fun PasswordsExpandedView(
    passwords: List<PasswordItem>,
    initialEditingPasswordId: String? = null,
    onPasswordsUpdated: (List<PasswordItem>) -> Unit,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedPasswordId by remember { mutableStateOf(initialEditingPasswordId) }
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()) }

    val activeItem = passwords.firstOrNull { it.id == selectedPasswordId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingDarkBackground)
    ) {
        if (activeItem == null) {
            // === LIST VIEW: All Passwords ===
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

                    // Title
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VAULT // ${passwords.size} KEYS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }

                    // New Password Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingRed)
                            .clickable {
                                val newItem = PasswordItem(
                                    title = "",
                                    password = "",
                                    username = "",
                                    updatedAt = System.currentTimeMillis()
                                )
                                val updated = PasswordsManager.upsertPassword(context, newItem)
                                onPasswordsUpdated(updated)
                                selectedPasswordId = newItem.id
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

                if (passwords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "VAULT EMPTY",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = NothingTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap '+ NEW' to store your first password",
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
                        items(passwords, key = { it.id }) { item ->
                            PasswordCardItem(
                                item = item,
                                dateFormat = dateFormat,
                                onClick = { selectedPasswordId = item.id },
                                onDelete = {
                                    val updated = PasswordsManager.deletePassword(context, item.id)
                                    onPasswordsUpdated(updated)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // === EDITOR VIEW: Single Password Screen ===
            PasswordEditorScreen(
                item = activeItem,
                onBack = { selectedPasswordId = null },
                onCollapse = onCollapse,
                onDelete = {
                    val updated = PasswordsManager.deletePassword(context, activeItem.id)
                    onPasswordsUpdated(updated)
                    selectedPasswordId = null
                },
                onUpdate = { updatedItem ->
                    val updated = PasswordsManager.upsertPassword(context, updatedItem)
                    onPasswordsUpdated(updated)
                }
            )
        }
    }
}

@Composable
private fun PasswordCardItem(
    item: PasswordItem,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var isRevealed by remember { mutableStateOf(false) }

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
            // Top Row: Title + Delete Button
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
                        text = if (item.title.isNotBlank()) item.title.uppercase() else "(UNTITLED SERVICE)",
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

            if (item.username.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.username,
                    fontSize = 10.5.sp,
                    color = NothingTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Password Content Row: Masked/Plaintext + Show Toggle + Copy Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(NothingDarkBackground)
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Password Text
                Text(
                    text = if (item.password.isEmpty()) {
                        "No password set"
                    } else if (isRevealed) {
                        item.password
                    } else {
                        "•".repeat(item.password.length.coerceIn(8, 16))
                    },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.password.isEmpty()) NothingTextTertiary else NothingWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Reveal / Hide Toggle
                if (item.password.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NothingSurfaceVariant)
                            .clickable { isRevealed = !isRevealed }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isRevealed) "HIDE" else "SHOW",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Dedicated Copy Button
                    CopyToClipboardButton(
                        textToCopy = item.password,
                        compact = true
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Timestamp
            Text(
                text = dateFormat.format(Date(item.updatedAt)),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                color = NothingTextTertiary
            )
        }
    }
}

/**
 * Nothing OS password editor with Title, Password, Quick Copy, and Generator.
 */
@Composable
private fun PasswordEditorScreen(
    item: PasswordItem,
    onBack: () -> Unit,
    onCollapse: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (PasswordItem) -> Unit
) {
    var titleText by remember(item.id) { mutableStateOf(item.title) }
    var passwordText by remember(item.id) { mutableStateOf(item.password) }
    var usernameText by remember(item.id) { mutableStateOf(item.username) }
    var isPasswordVisible by remember { mutableStateOf(true) }

    fun save() {
        onUpdate(
            item.copy(
                title = titleText,
                password = passwordText,
                username = usernameText
            )
        )
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
                        text = "← ALL KEYS",
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

            // Delete Button
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

        // Label: Title / Service
        Text(
            text = "SERVICE // TITLE",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = NothingTextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (titleText.isEmpty()) {
                Text(
                    text = "E.G. GITHUB, WI-FI, EMAIL...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp,
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = NothingWhite
                ),
                cursorBrush = SolidColor(NothingRed),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Username / Account (Optional)
        Text(
            text = "ACCOUNT // USERNAME (OPTIONAL)",
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = NothingTextTertiary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(modifier = Modifier.fillMaxWidth()) {
            if (usernameText.isEmpty()) {
                Text(
                    text = "Username, email, or login ID...",
                    fontSize = 13.sp,
                    color = NothingTextTertiary
                )
            }
            BasicTextField(
                value = usernameText,
                onValueChange = {
                    usernameText = it
                    save()
                },
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = NothingWhite.copy(alpha = 0.9f)
                ),
                cursorBrush = SolidColor(NothingRed),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        HorizontalDivider(color = NothingBorder.copy(alpha = 0.5f), thickness = 0.5.dp)
        Spacer(modifier = Modifier.height(14.dp))

        // Password Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PASSWORD",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NothingRed
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Show/Hide Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                        .clickable { isPasswordVisible = !isPasswordVisible }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPasswordVisible) "HIDE" else "SHOW",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary
                    )
                }

                // Copy Button
                CopyToClipboardButton(
                    textToCopy = passwordText,
                    compact = true
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Password Input Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(NothingCardSurface)
                .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                .padding(14.dp)
        ) {
            if (passwordText.isEmpty()) {
                Text(
                    text = "Type or generate your password here...",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    color = NothingTextTertiary
                )
            }
            BasicTextField(
                value = passwordText,
                onValueChange = {
                    passwordText = it
                    save()
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = NothingWhite
                ),
                cursorBrush = SolidColor(NothingRed),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Generator Action: Generate Strong Password
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(NothingSurfaceVariant)
                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                .clickable {
                    passwordText = PasswordsManager.generatePassword(16)
                    save()
                }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "⚡ GENERATE 16-CHAR STRONG PASSWORD",
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = NothingWhite
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Footer Auto-saved status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${passwordText.length} CHARS",
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
