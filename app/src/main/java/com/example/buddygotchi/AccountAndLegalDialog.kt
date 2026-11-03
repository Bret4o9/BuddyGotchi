package com.example.buddygotchi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.buddygotchi.ui.theme.*

enum class AccountHubTab(val label: String) {
    ACCOUNT("ACCOUNT"),
    INVENTORY("PETS & ITEMS"),
    LEGAL("LEGAL & PRIVACY")
}

enum class LegalDocType(val title: String) {
    PRIVACY("PRIVACY POLICY"),
    TERMS("TERMS OF SERVICE"),
    REFUND("REFUND & FEES"),
    COOKIES("COOKIE / STORAGE"),
    SDK_AUDIT("SDK & LICENSES")
}

/**
 * Non-intrusive, Dark-Pattern-Free Cookie / Local Storage & Cloud Sync Consent Banner.
 * Both "LOCAL ONLY" and "ALLOW CLOUD SYNC" buttons have equal visual weight and size.
 */
@Composable
fun StorageAndPrivacyConsentBanner(
    onOpenLegalDetails: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AccountAndInventoryManager.ensureInitialized(context)
    }
    val hasAnswered by AccountAndInventoryManager.hasAnsweredConsentBannerState
    if (hasAnswered) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .semantics {
                contentDescription = "Privacy, local storage, and cloud sync consent banner"
            },
        shape = RoundedCornerShape(18.dp),
        color = NothingCardSurface,
        border = BorderStroke(1.dp, NothingBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                    text = "STORAGE, COOKIES & DATA PRIVACY CONSENT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite,
                    letterSpacing = 0.8.sp
                )
            }

            Text(
                text = "BuddyGotchi uses local device storage for your tasks, notes, and companions. Cloud sync (Firebase) for cross-device accounts & Squad events is 100% optional. No advertising trackers or hidden fees are ever used.",
                fontSize = 11.sp,
                color = NothingTextSecondary,
                lineHeight = 15.sp
            )

            Text(
                text = "READ PRIVACY, COOKIE, REFUND & TERMS POLICIES →",
                fontFamily = FontFamily.Monospace,
                fontSize = 9.5.sp,
                fontWeight = FontWeight.Bold,
                color = NothingRed,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onOpenLegalDetails() }
                    .padding(vertical = 2.dp)
            )

            // Equal-prominence buttons (Zero Dark Patterns)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingTextTertiary, RoundedCornerShape(10.dp))
                        .clickable {
                            AccountAndInventoryManager.setStorageAndCloudConsent(context, allowCloudSync = false)
                        }
                        .padding(vertical = 10.dp)
                        .semantics { contentDescription = "Essential local storage only, decline cloud sync" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LOCAL ONLY",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingRed)
                        .border(1.dp, NothingRed, RoundedCornerShape(10.dp))
                        .clickable {
                            AccountAndInventoryManager.setStorageAndCloudConsent(context, allowCloudSync = true)
                        }
                        .padding(vertical = 10.dp)
                        .semantics { contentDescription = "Accept local storage and optional cloud sync" },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ALLOW CLOUD SYNC",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun AccountAndLegalHubDialog(
    initialTab: AccountHubTab = AccountHubTab.ACCOUNT,
    onDismiss: () -> Unit,
    onDataChanged: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AccountAndInventoryManager.ensureInitialized(context)
        AccountAndInventoryManager.autoUnlockForCurrentLevel(context)
    }

    var activeTab by remember { mutableStateOf(initialTab) }
    var activeLegalDoc by remember { mutableStateOf(LegalDocType.PRIVACY) }

    val isLoggedIn by AccountAndInventoryManager.isLoggedInState
    val accountEmail by AccountAndInventoryManager.accountEmailState
    val accountUsername by AccountAndInventoryManager.accountUsernameState
    val accountBuddyCode by AccountAndInventoryManager.accountBuddyCodeState
    val equippedPetId by AccountAndInventoryManager.equippedPetIdState
    val unlockedPets = AccountAndInventoryManager.unlockedPetIdsState
    val unlockedItems = AccountAndInventoryManager.unlockedItemIdsState
    val equippedItems = AccountAndInventoryManager.equippedItemIdsState
    val cloudSyncConsent by AccountAndInventoryManager.cloudSyncConsentState
    val updatesSubscribed by AccountAndInventoryManager.updatesSubscribedState
    val statusMsg by AccountAndInventoryManager.syncStatusMessageState

    val xp = remember(statusMsg, equippedPetId) { TaskManager.getBuddyXp(context) }
    val level = (xp / 50) + 1

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.88f),
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
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
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Column {
                            Text(
                                text = "ACCOUNT, INVENTORY & LEGAL HUB",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingWhite
                            )
                            Text(
                                text = if (isLoggedIn) "SIGNED IN: $accountUsername ($accountEmail)" else "GUEST MODE • ACCOUNT OPTIONAL",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                color = NothingTextSecondary
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { onDismiss() }
                            .semantics { contentDescription = "Close Account and Legal Hub" },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✕", fontSize = 12.sp, color = NothingWhite)
                    }
                }

                // Top Tab Switcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    AccountHubTab.entries.forEach { tab ->
                        val selected = activeTab == tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) NothingRed else Color.Transparent)
                                .clickable { activeTab = tab }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tab.label,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.White else NothingTextSecondary
                            )
                        }
                    }
                }

                if (!statusMsg.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF1E1415),
                        border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMsg!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }

                // Tab Body
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (activeTab) {
                        AccountHubTab.ACCOUNT -> {
                            AccountTabContent(
                                isLoggedIn = isLoggedIn,
                                accountEmail = accountEmail,
                                accountUsername = accountUsername,
                                accountBuddyCode = accountBuddyCode,
                                unlockedPetsCount = unlockedPets.size,
                                unlockedItemsCount = unlockedItems.size,
                                onOpenLegalDoc = { docType ->
                                    activeLegalDoc = docType
                                    activeTab = AccountHubTab.LEGAL
                                },
                                onDataChanged = onDataChanged
                            )
                        }
                        AccountHubTab.INVENTORY -> {
                            InventoryTabContent(
                                level = level,
                                xp = xp,
                                equippedPetId = equippedPetId,
                                unlockedPets = unlockedPets,
                                unlockedItems = unlockedItems,
                                equippedItems = equippedItems,
                                onDataChanged = onDataChanged
                            )
                        }
                        AccountHubTab.LEGAL -> {
                            LegalAndPrivacyTabContent(
                                activeDoc = activeLegalDoc,
                                onSelectDoc = { activeLegalDoc = it },
                                cloudSyncConsent = cloudSyncConsent,
                                updatesSubscribed = updatesSubscribed,
                                onDataChanged = onDataChanged
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountTabContent(
    isLoggedIn: Boolean,
    accountEmail: String,
    accountUsername: String,
    accountBuddyCode: String,
    unlockedPetsCount: Int,
    unlockedItemsCount: Int,
    onOpenLegalDoc: (LegalDocType) -> Unit,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var isSignInMode by remember { mutableStateOf(false) }
    var usernameInput by remember { mutableStateOf(if (accountUsername != "GUEST") accountUsername else "") }
    var emailInput by remember { mutableStateOf(accountEmail) }
    var passwordInput by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    // Unchecked-by-default explicit consent checkboxes (No pre-checked dark patterns)
    var ageConfirmed13Plus by remember { mutableStateOf(false) }
    var termsAccepted by remember { mutableStateOf(false) }
    var subscribeUpdates by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Optional Account Notice
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = NothingSurfaceVariant,
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "OPTIONAL CROSS-DEVICE CLOUD ACCOUNT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )
                Text(
                    text = "Creating an account is completely optional. It backs up your Companion Level, XP, Pets, and Gear Inventory across devices. Passwords stored in your Passwords Widget remain 100% local on your phone and are never uploaded.",
                    fontSize = 11.sp,
                    color = NothingTextSecondary,
                    lineHeight = 15.sp
                )
            }
        }

        if (isLoggedIn) {
            // Signed-In Account Card
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF181818),
                border = BorderStroke(1.dp, NothingBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ACTIVE CLOUD OPERATOR",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed
                    )
                    Text(
                        text = "$accountUsername • $accountBuddyCode",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Text(
                        text = "EMAIL: $accountEmail",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.5.sp,
                        color = NothingTextSecondary
                    )
                    Text(
                        text = "SYNCED INVENTORY: $unlockedPetsCount PETS • $unlockedItemsCount ITEMS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = NothingTextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingRed)
                                .clickable {
                                    AccountAndInventoryManager.syncAccountToCloud(context) {
                                        onDataChanged()
                                    }
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SYNC TO CLOUD NOW",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingSurfaceVariant)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .clickable {
                                    AccountAndInventoryManager.signOutToGuestMode(context)
                                    onDataChanged()
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SWITCH TO GUEST",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }
        } else {
            // Create Account vs Sign In Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF181818))
                    .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (!isSignInMode) NothingRed else Color.Transparent)
                        .clickable { isSignInMode = false }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CREATE ACCOUNT",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (!isSignInMode) Color.White else NothingTextSecondary
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSignInMode) NothingRed else Color.Transparent)
                        .clickable { isSignInMode = true }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "SIGN IN & RESTORE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSignInMode) Color.White else NothingTextSecondary
                    )
                }
            }

            if (!isSignInMode) {
                LabeledInputBox(
                    label = "DISPLAY NAME / CALLSIGN",
                    value = usernameInput,
                    placeholder = "E.G. DENIS",
                    onValueChange = { usernameInput = it }
                )
            }

            LabeledInputBox(
                label = "EMAIL ADDRESS (USED FOR CLOUD RESTORE ONLY)",
                value = emailInput,
                placeholder = "you@example.com",
                onValueChange = { emailInput = it }
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "PASSWORD (MIN 6 CHARS)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary
                    )
                    Text(
                        text = if (showPassword) "HIDE" else "SHOW",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed,
                        modifier = Modifier.clickable { showPassword = !showPassword }
                    )
                }
                BasicTextField(
                    value = passwordInput,
                    onValueChange = { passwordInput = it },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = NothingWhite
                    ),
                    cursorBrush = SolidColor(NothingRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF181818))
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                )
            }

            if (!isSignInMode) {
                // Explicit Form Consents & Age of Consent Gate
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF171717))
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "REQUIRED CONSENT & AGE VERIFICATION",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary
                    )

                    ConsentCheckboxRow(
                        checked = ageConfirmed13Plus,
                        onToggle = { ageConfirmed13Plus = !ageConfirmed13Plus },
                        text = "I confirm I am at least 13 years old (or 16+ in the EEA/UK, or have verifiable parental consent under COPPA/GDPR-K)."
                    )

                    ConsentCheckboxRow(
                        checked = termsAccepted,
                        onToggle = { termsAccepted = !termsAccepted },
                        text = "I have read and agree to the Privacy Policy, Terms of Service, Cookie/Storage Policy, and Refund Policy."
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(start = 26.dp)
                    ) {
                        Text(
                            text = "[PRIVACY POLICY]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            modifier = Modifier.clickable { onOpenLegalDoc(LegalDocType.PRIVACY) }
                        )
                        Text(
                            text = "[TERMS OF SERVICE]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            modifier = Modifier.clickable { onOpenLegalDoc(LegalDocType.TERMS) }
                        )
                    }

                    ConsentCheckboxRow(
                        checked = subscribeUpdates,
                        onToggle = { subscribeUpdates = !subscribeUpdates },
                        text = "(Optional) Subscribe to in-app companion & item release notices. You can unsubscribe with 1 tap anytime."
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingRed)
                    .clickable {
                        if (isSignInMode) {
                            AccountAndInventoryManager.signInAndRestoreFromCloud(
                                context = context,
                                email = emailInput,
                                password = passwordInput
                            ) {
                                onDataChanged()
                            }
                        } else {
                            AccountAndInventoryManager.registerOrLinkAccount(
                                context = context,
                                email = emailInput,
                                password = passwordInput,
                                displayName = usernameInput,
                                ageConfirmed13Plus = ageConfirmed13Plus,
                                termsAndPrivacyAccepted = termsAccepted,
                                subscribeToUpdates = subscribeUpdates
                            ) {
                                onDataChanged()
                            }
                        }
                    }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isSignInMode) "SIGN IN & RESTORE INVENTORY" else "CREATE OPTIONAL CLOUD ACCOUNT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun InventoryTabContent(
    level: Int,
    xp: Int,
    equippedPetId: String,
    unlockedPets: List<String>,
    unlockedItems: List<String>,
    equippedItems: List<String>,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = NothingSurfaceVariant,
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OPERATOR LEVEL $level • $xp XP",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                    Text(
                        text = "EARN XP BY COMPLETING TASKS & PETTING BUDDY • $0 HIDDEN FEES",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        color = NothingTextSecondary
                    )
                }
            }
        }

        // 1. Companion Pets Section
        Text(
            text = "COMPANION PETS (${unlockedPets.size}/${AccountAndInventoryManager.petCatalog.size} UNLOCKED)",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NothingTextSecondary
        )

        AccountAndInventoryManager.petCatalog.forEach { pet ->
            val isUnlocked = pet.id in unlockedPets || level >= pet.requiredLevel
            val isEquipped = equippedPetId == pet.id

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF181818),
                border = BorderStroke(1.dp, if (isEquipped) NothingRed else NothingBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = pet.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF252525))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = pet.licenseBadge,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 7.5.sp,
                                    color = NothingTextSecondary
                                )
                            }
                        }
                        Text(
                            text = pet.subtitle,
                            fontSize = 10.sp,
                            color = NothingTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isEquipped -> NothingRed
                                    isUnlocked -> NothingSurfaceVariant
                                    else -> Color(0xFF1F1F1F)
                                }
                            )
                            .border(
                                1.dp,
                                if (isEquipped) NothingRed else NothingBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = isUnlocked) {
                                AccountAndInventoryManager.equipPet(context, pet.id)
                                onDataChanged()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isEquipped -> "EQUIPPED"
                                isUnlocked -> "EQUIP"
                                else -> "LVL ${pet.requiredLevel}"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else NothingTextTertiary
                        )
                    }
                }
            }
        }

        // 2. Companion Items & Gear Section
        Text(
            text = "COMPANION GEAR & ITEMS (${unlockedItems.size}/${AccountAndInventoryManager.itemCatalog.size} UNLOCKED)",
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = NothingTextSecondary
        )

        AccountAndInventoryManager.itemCatalog.forEach { item ->
            val isUnlocked = item.id in unlockedItems || level >= item.requiredLevel
            val isEquipped = item.id in equippedItems

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF181818),
                border = BorderStroke(1.dp, if (isEquipped) NothingRed else NothingBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = item.name,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Text(
                                text = "[${item.slot}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingRed
                            )
                        }
                        Text(
                            text = item.description,
                            fontSize = 10.sp,
                            color = NothingTextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isEquipped -> NothingRed
                                    isUnlocked -> NothingSurfaceVariant
                                    else -> Color(0xFF1F1F1F)
                                }
                            )
                            .border(
                                1.dp,
                                if (isEquipped) NothingRed else NothingBorder,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(enabled = isUnlocked) {
                                AccountAndInventoryManager.toggleEquipGearItem(context, item.id)
                                onDataChanged()
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                isEquipped -> "EQUIPPED"
                                isUnlocked -> "EQUIP"
                                else -> "LVL ${item.requiredLevel}"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isUnlocked) Color.White else NothingTextTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegalAndPrivacyTabContent(
    activeDoc: LegalDocType,
    onSelectDoc: (LegalDocType) -> Unit,
    cloudSyncConsent: Boolean,
    updatesSubscribed: Boolean,
    onDataChanged: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var confirmDeleteStep by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Live Consent, Unsubscribe & Data Deletion Controls
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF181818),
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "YOUR PRIVACY, UNSUBSCRIBE & DATA RIGHTS",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingWhite
                )

                // Cloud Sync Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "CLOUD SYNC & STORAGE CONSENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = if (cloudSyncConsent) "ENABLED (Firebase Cloud Sync Active)" else "LOCAL ONLY (All Cloud Sync Disabled)",
                            fontSize = 10.sp,
                            color = NothingTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (cloudSyncConsent) NothingRed else NothingSurfaceVariant)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                AccountAndInventoryManager.setStorageAndCloudConsent(context, !cloudSyncConsent)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (cloudSyncConsent) "OPT OUT (LOCAL)" else "ENABLE CLOUD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)

                // One-Tap Unsubscribe Link/Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RELEASE NOTES & COMMUNICATIONS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Text(
                            text = if (updatesSubscribed) "SUBSCRIBED TO OPTIONAL UPDATES" else "UNSUBSCRIBED (NO MARKETING MESSAGES)",
                            fontSize = 10.sp,
                            color = NothingTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NothingSurfaceVariant)
                            .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                            .clickable {
                                AccountAndInventoryManager.setUpdatesSubscription(context, !updatesSubscribed)
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (updatesSubscribed) "UNSUBSCRIBE" else "SUBSCRIBE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                    }
                }

                HorizontalDivider(color = NothingBorder, thickness = 0.5.dp)

                // Mandatory Account & Data Deletion Option (GDPR Art. 17 / Google Play Policy)
                if (!confirmDeleteStep) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF281214))
                            .border(1.dp, NothingRed, RoundedCornerShape(10.dp))
                            .clickable { confirmDeleteStep = true }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "DELETE MY ACCOUNT & ALL CLOUD/LOCAL DATA",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "CONFIRM PERMANENT DELETION? This immediately erases your Firebase Cloud profile, inventory, and local device data.",
                            fontSize = 10.sp,
                            color = NothingWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingSurfaceVariant)
                                    .clickable { confirmDeleteStep = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CANCEL",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NothingRed)
                                    .clickable {
                                        AccountAndInventoryManager.deleteAccountAndAllData(context) {
                                            confirmDeleteStep = false
                                            onDataChanged()
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "PERMANENTLY DELETE",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Document Selector Pills
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(LegalDocType.PRIVACY, LegalDocType.TERMS, LegalDocType.REFUND).forEach { doc ->
                    val sel = activeDoc == doc
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) NothingRed else NothingSurfaceVariant)
                            .clickable { onSelectDoc(doc) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = doc.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(LegalDocType.COOKIES, LegalDocType.SDK_AUDIT).forEach { doc ->
                    val sel = activeDoc == doc
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (sel) NothingRed else NothingSurfaceVariant)
                            .clickable { onSelectDoc(doc) }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = doc.title,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // 3. Full Legal Document Text Box
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0xFF151515),
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = activeDoc.title,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingRed
                )
                Text(
                    text = getLegalDocumentBody(activeDoc),
                    fontSize = 11.sp,
                    color = NothingTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

private fun getLegalDocumentBody(doc: LegalDocType): String {
    return when (doc) {
        LegalDocType.PRIVACY -> """
            EFFECTIVE DATE: NOVEMBER 2026
            
            1. DATA MINIMIZATION (COLLECT ONLY NEEDED DATA)
            BuddyGotchi collects only the minimum data required to operate the app:
            • Local-Only Data: Tasks, Notes, Grocery Lists, Water intake, and Passwords Widget entries are stored locally on your device in Android SharedPreferences. Passwords Widget entries NEVER leave your device.
            • Optional Cloud Account Data: If you explicitly opt in to Cloud Sync or create an account, we store your Display Name, Buddy Code, Email, Companion XP/Level, Unlocked Pets & Items Inventory, and Squad Event RSVPs in Google Cloud Firestore.
            • What We DO NOT Collect: We never collect background GPS location, contacts, microphone, camera, biometric data, or advertising IDs. Map coordinates are only sent to OpenStreetMap when you manually use the Squad Event Location Picker.
            
            2. AGE OF CONSENT FOR MINORS (COPPA & GDPR-K)
            Cloud accounts require users to be at least 13 years old (or 16+ in the EEA/UK, or have verifiable parental consent). Users of any age may use Local-Only Guest Mode offline without submitting personal data.
            
            3. YOUR RIGHTS & DATA DELETION (GDPR ART. 17 / CCPA)
            You may inspect, opt out of cloud sync, unsubscribe from updates, or permanently erase your cloud and local data at any time using the "DELETE MY ACCOUNT & ALL CLOUD/LOCAL DATA" button above.
        """.trimIndent()

        LegalDocType.TERMS -> """
            EFFECTIVE DATE: NOVEMBER 2026
            
            1. ACCEPTANCE & OPTIONAL ACCOUNTS
            BuddyGotchi provides productivity widgets (Clock, Reminders, Notes, Groceries, Hydration) and interactive 2D/3D virtual companions. Creating an account is optional and used solely to back up and sync your companion progression and inventory across devices.
            
            2. NO UNSUPPORTED CLAIMS OR MEDICAL ADVICE
            The Water Tracker, Reminder Alarms, and Companion Mood features are for general personal organization and entertainment only. BuddyGotchi does not provide medical, health, or financial advice.
            
            3. VIRTUAL PETS & ITEMS
            Unlocked virtual pets and companion items represent non-transferable in-app digital features with no real-world monetary value.
            
            4. LIMITATION OF LIABILITY
            BuddyGotchi is provided "AS IS" without warranties of uninterrupted alarm delivery or cloud availability. To the maximum extent permitted by law, the developer shall not be liable for indirect or consequential damages.
        """.trimIndent()

        LegalDocType.REFUND -> """
            EFFECTIVE DATE: NOVEMBER 2026
            
            1. NO HIDDEN FEES & TRANSPARENT PRICING
            BuddyGotchi is free to download and use. There are no hidden subscriptions, auto-renewing trial traps, dark patterns, or fake reviews. All starter and level-up companions and gear items are earned through free XP progression.
            
            2. REFUND POLICY FOR FUTURE DIGITAL ITEMS
            If optional paid cosmetic companions or supporter packs are offered in the future via Google Play Billing:
            • All prices will be displayed upfront inclusive of applicable taxes before checkout.
            • Refund requests are processed through Google Play's official refund policy (within 48 hours of purchase or as required by local consumer law, including EU/EEA/UK statutory consumer rules).
        """.trimIndent()

        LegalDocType.COOKIES -> """
            EFFECTIVE DATE: NOVEMBER 2026
            
            1. NO ADVERTISING OR TRACKING COOKIES
            BuddyGotchi does NOT use third-party advertising cookies, cross-app trackers, or analytics pixels.
            
            2. ESSENTIAL LOCAL STORAGE & WEBVIEW ASSETS
            • Android SharedPreferences: Used to save your dashboard widget layout, tasks, notes, and companion state locally.
            • Local WebView Rendering: The 3D Companion viewport (Three.js) and Map Picker (Leaflet) run inside an embedded Android WebView using local assets without setting tracking cookies.
            • Firebase Auth Tokens: When you opt into Cloud Sync, Firebase stores an authentication token locally to keep your session active. You can revoke this anytime by switching to "LOCAL ONLY".
        """.trimIndent()

        LegalDocType.SDK_AUDIT -> """
            THIRD-PARTY SDK & ASSET LICENSE AUDIT
            Every library and asset in BuddyGotchi has been audited for open-source and commercial license compliance:
            
            1. AndroidX & Jetpack Compose UI (Google)
               • License: Apache License 2.0 (Free / Commercial Safe)
               • Purpose: User interface, accessibility semantics, and local lifecycle.
            
            2. Google Firebase Authentication & Cloud Firestore
               • License: Apache License 2.0 (Google Cloud Platform Terms)
               • Purpose: Optional cross-device account backup and Squad event sync.
            
            3. Three.js (r147) & fflate
               • License: MIT License (Copyright © Three.js Authors / Arjun Mehrotra)
               • Purpose: Offline 3D companion rendering (.fbx, .gltf, .glb, .obj).
            
            4. Leaflet.js (v1.9.4) & OpenStreetMap / Nominatim
               • License: BSD 2-Clause License (Leaflet) & ODbL 1.0 (© OpenStreetMap contributors)
               • Purpose: Interactive Squad event map picker with visible ODbL attribution.
            
            5. DSEG7 Classic Font (by Keshikan)
               • License: SIL Open Font License 1.1 (OFL-1.1 — Free / Commercial Safe)
               • Purpose: Retro 7-segment digital clock typography.
            
            6. Companion 3D & 2D Assets
               • 2D Classic Eyes & BuddyBot MK-I (buddybot.gltf): 100% Original CC0 Public Domain assets created for BuddyGotchi.
               • SSRB Hololive Test Model: Non-commercial fan-work test asset (Cover Corp. Derivative Works Guidelines); replace or disable before commercial monetization.
        """.trimIndent()
    }
}

@Composable
private fun LabeledInputBox(
    label: String,
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = NothingTextSecondary
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                color = NothingWhite
            ),
            cursorBrush = SolidColor(NothingRed),
            decorationBox = { innerTextField ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = NothingTextTertiary
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF181818))
                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun ConsentCheckboxRow(
    checked: Boolean,
    onToggle: () -> Unit,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (checked) NothingRed else Color(0xFF222222))
                .border(1.dp, if (checked) NothingRed else NothingTextSecondary, RoundedCornerShape(4.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Text(
                    text = "✓",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
        Text(
            text = text,
            fontSize = 10.sp,
            color = NothingTextSecondary,
            lineHeight = 14.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
