package com.example.buddygotchi

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.buddygotchi.ui.theme.*

/**
 * PETS WIDGET (Deployed by default on the INVENTORY page).
 * Tap anywhere or tap [INSPECT ↗] to open the full Pets Collection Inspector Dialog.
 */
@Composable
fun PetsWidget(
    size: WidgetSize = WidgetSize.WIDE,
    buddyLevel: Int = 1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AccountAndInventoryManager.ensureInitialized(context)
    }

    var showInspector by remember { mutableStateOf(false) }
    var initialInspectedPetId by remember { mutableStateOf<String?>(null) }

    val equippedPetId by AccountAndInventoryManager.equippedPetIdState
    val unlockedPetIds = AccountAndInventoryManager.unlockedPetIdsState
    val catalog = AccountAndInventoryManager.petCatalog
    val equippedPet = catalog.find { it.id == equippedPetId } ?: catalog.first()

    val widgetHeight = DashboardGridDefaults.getWidgetHeight(size)
    val isCompact = size.cols == 1 || size.rows == 1

    Surface(
        shape = RoundedCornerShape(if (size == WidgetSize.CUBE) 20.dp else 24.dp),
        color = NothingCardSurface,
        border = BorderStroke(1.dp, NothingBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(widgetHeight)
            .clip(RoundedCornerShape(if (size == WidgetSize.CUBE) 20.dp else 24.dp))
            .clickable {
                initialInspectedPetId = equippedPet.id
                showInspector = true
            }
            .semantics {
                contentDescription = "Pets Inventory Widget. Active pet: ${equippedPet.name}. Tap to inspect pets."
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
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
                        text = "PETS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                    if (size.cols >= 2) {
                        Text(
                            text = "[0${unlockedPetIds.size}/0${catalog.size}]",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }
                }

                if (size.cols >= 2) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder)
                    ) {
                        Text(
                            text = "INSPECT ↗",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (size.rows == 1) {
                // Slim 1-row summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = equippedPet.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "ACTIVE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00E676)
                    )
                }
            } else {
                // Active Pet Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NothingDarkBackground,
                    border = BorderStroke(1.dp, NothingRed.copy(alpha = 0.7f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00E676))
                                )
                                Text(
                                    text = "EQUIPPED: ${equippedPet.name}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = equippedPet.subtitle,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = NothingRed.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, NothingRed)
                        ) {
                            Text(
                                text = if (equippedPet.displayMode == BuddyDisplayMode.THREE_D) "3D GLTF" else "2D EYES",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Horizontal scrollable chips of owned pets (tap chip to inspect that pet directly)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    catalog.forEach { pet ->
                        val isUnlocked = pet.id in unlockedPetIds || buddyLevel >= pet.requiredLevel
                        val isEquipped = pet.id == equippedPetId
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isEquipped) NothingRed else NothingSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isEquipped) NothingRed else if (isUnlocked) NothingBorder else NothingBorder.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    initialInspectedPetId = pet.id
                                    showInspector = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                isEquipped -> NothingWhite
                                                isUnlocked -> Color(0xFF00E676)
                                                else -> NothingTextTertiary
                                            }
                                        )
                                )
                                Text(
                                    text = pet.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) NothingWhite else NothingTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInspector) {
        PetsInspectorDialog(
            initialPetId = initialInspectedPetId ?: equippedPetId,
            buddyLevel = buddyLevel,
            onDismiss = { showInspector = false }
        )
    }
}

/**
 * ITEMS WIDGET (Deployed by default on the INVENTORY page).
 * Tap anywhere or tap [INSPECT ↗] to open the full Items & Gear Inspector Dialog.
 */
@Composable
fun ItemsWidget(
    size: WidgetSize = WidgetSize.WIDE,
    buddyLevel: Int = 1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AccountAndInventoryManager.ensureInitialized(context)
    }

    var showInspector by remember { mutableStateOf(false) }
    var initialInspectedItemId by remember { mutableStateOf<String?>(null) }

    val unlockedItemIds = AccountAndInventoryManager.unlockedItemIdsState
    val equippedItemIds = AccountAndInventoryManager.equippedItemIdsState
    val catalog = AccountAndInventoryManager.itemCatalog

    val widgetHeight = DashboardGridDefaults.getWidgetHeight(size)
    val isCompact = size.cols == 1 || size.rows == 1

    Surface(
        shape = RoundedCornerShape(if (size == WidgetSize.CUBE) 20.dp else 24.dp),
        color = NothingCardSurface,
        border = BorderStroke(1.dp, NothingBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(widgetHeight)
            .clip(RoundedCornerShape(if (size == WidgetSize.CUBE) 20.dp else 24.dp))
            .clickable {
                initialInspectedItemId = equippedItemIds.firstOrNull() ?: catalog.first().id
                showInspector = true
            }
            .semantics {
                contentDescription = "Items Inventory Widget. ${unlockedItemIds.size} items unlocked. Tap to inspect items."
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row
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
                        text = "ITEMS",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                    if (size.cols >= 2) {
                        Text(
                            text = "[0${unlockedItemIds.size}/0${catalog.size}]",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }
                }

                if (size.cols >= 2) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder)
                    ) {
                        Text(
                            text = "INSPECT ↗",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (size.rows == 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${equippedItemIds.size} EQUIPPED • ${unlockedItemIds.size} OWNED",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        maxLines = 1
                    )
                }
            } else {
                // Equipped Gear Summary Strip
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = NothingDarkBackground,
                    border = BorderStroke(1.dp, NothingBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val equippedNames = catalog.filter { it.id in equippedItemIds }.joinToString(" • ") { "[${it.slot}] ${it.name}" }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "EQUIPPED GEAR SLOTS (${equippedItemIds.size})",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary
                            )
                            Text(
                                text = equippedNames.ifBlank { "NO GEAR EQUIPPED — TAP TO EQUIP" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Horizontal scrollable chips of items (tap to inspect that item)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    catalog.forEach { item ->
                        val isUnlocked = item.id in unlockedItemIds
                        val isEquipped = item.id in equippedItemIds
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isEquipped) NothingRed else NothingSurfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isEquipped) NothingRed else if (isUnlocked) NothingBorder else NothingBorder.copy(alpha = 0.4f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    initialInspectedItemId = item.id
                                    showInspector = true
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "[${item.slot}]",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEquipped) NothingWhite else if (isUnlocked) NothingRed else NothingTextTertiary
                                )
                                Text(
                                    text = item.name,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isUnlocked) NothingWhite else NothingTextTertiary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInspector) {
        ItemsInspectorDialog(
            initialItemId = initialInspectedItemId ?: catalog.first().id,
            buddyLevel = buddyLevel,
            onDismiss = { showInspector = false }
        )
    }
}

/**
 * STORE WIDGET (Deployed by default on the INVENTORY page).
 * Features purchasable/unlockable Buddies, Items, and Goodies.
 */
@Composable
fun StoreWidget(
    size: WidgetSize = WidgetSize.MAX,
    buddyXp: Int = 0,
    buddyLevel: Int = 1,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        AccountAndInventoryManager.ensureInitialized(context)
    }

    var showStoreDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<StoreCategory?>(null) }
    val storeEntries = AccountAndInventoryManager.storeCatalog
    val widgetHeight = DashboardGridDefaults.getWidgetHeight(size)
    val isCompact = size.cols == 1 || size.rows == 1

    Surface(
        shape = RoundedCornerShape(if (size == WidgetSize.CUBE) 20.dp else 24.dp),
        color = NothingCardSurface,
        border = BorderStroke(1.dp, NothingBorder),
        modifier = modifier
            .fillMaxWidth()
            .height(widgetHeight)
            .clip(RoundedCornerShape(if (size == WidgetSize.CUBE) 20.dp else 24.dp))
            .clickable {
                selectedCategory = null
                showStoreDialog = true
            }
            .semantics {
                contentDescription = "BuddyGotchi Store Widget. Browse Buddies, Items, and Goodies."
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isCompact) 12.dp else 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Store Header Row
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
                        text = "STORE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = NothingWhite
                    )
                    if (size.cols >= 2) {
                        Text(
                            text = "// BUDDIES • ITEMS • GOODIES",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary
                        )
                    }
                }

                if (size.cols >= 2) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NothingRed,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp))
                    ) {
                        Text(
                            text = "OPEN STORE ↗",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (size.rows >= 2) {
                // Category Pills Row (BUDDIES | ITEMS | GOODIES)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoreCategory.entries.forEach { cat ->
                        val count = storeEntries.count { it.category == cat }
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = NothingDarkBackground,
                            border = BorderStroke(1.dp, NothingBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    selectedCategory = cat
                                    showStoreDialog = true
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = cat.label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                                Text(
                                    text = "0$count DROPS",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 7.5.sp,
                                    color = NothingRed
                                )
                            }
                        }
                    }
                }

                // Featured Store Cards Preview
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val previewItems = if (size.rows >= 3) storeEntries.take(2) else storeEntries.take(1)
                    previewItems.forEach { entry ->
                        val isUnlocked = AccountAndInventoryManager.isStoreEntryUnlocked(entry)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = NothingDarkBackground,
                            border = BorderStroke(
                                1.dp,
                                if (isUnlocked) Color(0xFF00E676).copy(alpha = 0.6f) else NothingBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    selectedCategory = entry.category
                                    showStoreDialog = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = entry.title,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingWhite,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = entry.subtitle,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        color = NothingTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isUnlocked -> Color(0xFF00E676).copy(alpha = 0.16f)
                                        entry.isAvailableNow -> NothingRed.copy(alpha = 0.2f)
                                        else -> NothingSurfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isUnlocked -> Color(0xFF00E676)
                                            entry.isAvailableNow -> NothingRed
                                            else -> NothingBorder
                                        }
                                    )
                                ) {
                                    Text(
                                        text = if (isUnlocked) "OWNED" else entry.priceLabel,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) Color(0xFF00E676) else NothingWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showStoreDialog) {
        BuddyStoreDialog(
            initialCategory = selectedCategory,
            buddyXp = buddyXp,
            buddyLevel = buddyLevel,
            onDismiss = { showStoreDialog = false }
        )
    }
}

/**
 * FULL PETS INSPECTOR DIALOG:
 * Inspect any companion pet in your collection, see its 3D/2D model specs, license, unlock level, and equip it.
 */
@Composable
fun PetsInspectorDialog(
    initialPetId: String,
    buddyLevel: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val catalog = AccountAndInventoryManager.petCatalog
    val unlockedPetIds = AccountAndInventoryManager.unlockedPetIdsState
    val equippedPetId by AccountAndInventoryManager.equippedPetIdState

    var inspectedPetId by remember { mutableStateOf(initialPetId) }
    val inspectedPet = catalog.find { it.id == inspectedPetId } ?: catalog.first()
    val isInspectedUnlocked = inspectedPet.id in unlockedPetIds || buddyLevel >= inspectedPet.requiredLevel
    val isInspectedEquipped = inspectedPet.id == equippedPetId

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = NothingDarkBackground,
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Bar
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
                        Text(
                            text = "PETS INSPECTOR",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = NothingWhite
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                    ) {
                        Text(
                            text = "✕ CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Detailed Inspector Card for Selected Pet
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NothingCardSurface,
                    border = BorderStroke(1.dp, if (isInspectedEquipped) NothingRed else NothingBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "INSPECTING COMPANION",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingRed
                            )
                            Text(
                                text = inspectedPet.licenseBadge,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingTextSecondary
                            )
                        }

                        Text(
                            text = inspectedPet.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )

                        Text(
                            text = inspectedPet.subtitle,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NothingTextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NothingDarkBackground,
                                border = BorderStroke(1.dp, NothingBorder)
                            ) {
                                Text(
                                    text = if (inspectedPet.displayMode == BuddyDisplayMode.THREE_D) "ENGINE: 3D GLTF" else "ENGINE: 2D VECTOR",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = NothingWhite,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = NothingDarkBackground,
                                border = BorderStroke(1.dp, NothingBorder)
                            ) {
                                Text(
                                    text = "UNLOCK: LVL ${inspectedPet.requiredLevel}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 8.5.sp,
                                    color = if (isInspectedUnlocked) Color(0xFF00E676) else NothingTextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isInspectedEquipped -> Color(0xFF00E676).copy(alpha = 0.2f)
                                isInspectedUnlocked -> NothingRed
                                else -> NothingSurfaceVariant
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isInspectedEquipped -> Color(0xFF00E676)
                                    isInspectedUnlocked -> NothingRed
                                    else -> NothingBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = isInspectedUnlocked && !isInspectedEquipped) {
                                    AccountAndInventoryManager.equipPet(context, inspectedPet.id)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isInspectedEquipped -> "✓ CURRENTLY EQUIPPED COMPANION"
                                        isInspectedUnlocked -> "EQUIP ${inspectedPet.name}"
                                        else -> "LOCKED • REACH LEVEL ${inspectedPet.requiredLevel}"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isInspectedEquipped) Color(0xFF00E676) else NothingWhite
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "ALL COMPANIONS IN INVENTORY (${unlockedPetIds.size}/${catalog.size}) • TAP TO INSPECT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingTextSecondary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    catalog.forEach { pet ->
                        val isUnlocked = pet.id in unlockedPetIds || buddyLevel >= pet.requiredLevel
                        val isEquipped = pet.id == equippedPetId
                        val isSelected = pet.id == inspectedPet.id

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NothingCardSurface else NothingDarkBackground,
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> NothingRed
                                    isEquipped -> Color(0xFF00E676)
                                    else -> NothingBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    inspectedPetId = pet.id
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = pet.name,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) NothingWhite else NothingTextSecondary
                                    )
                                    Text(
                                        text = pet.subtitle,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        color = NothingTextSecondary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isEquipped -> Color(0xFF00E676).copy(alpha = 0.18f)
                                        isUnlocked -> NothingSurfaceVariant
                                        else -> NothingDarkBackground
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isEquipped) Color(0xFF00E676) else NothingBorder
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            isEquipped -> "EQUIPPED"
                                            isUnlocked -> "OWNED"
                                            else -> "LVL ${pet.requiredLevel}"
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEquipped) Color(0xFF00E676) else NothingWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

/**
 * FULL ITEMS INSPECTOR DIALOG:
 * Inspect any item/gear in your inventory, view its slot & description, and equip/unequip it.
 */
@Composable
fun ItemsInspectorDialog(
    initialItemId: String,
    buddyLevel: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val catalog = AccountAndInventoryManager.itemCatalog
    val unlockedItemIds = AccountAndInventoryManager.unlockedItemIdsState
    val equippedItemIds = AccountAndInventoryManager.equippedItemIdsState

    var inspectedItemId by remember { mutableStateOf(initialItemId) }
    val inspectedItem = catalog.find { it.id == inspectedItemId } ?: catalog.first()
    val isInspectedUnlocked = inspectedItem.id in unlockedItemIds
    val isInspectedEquipped = inspectedItem.id in equippedItemIds

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = NothingDarkBackground,
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Bar
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
                        Text(
                            text = "ITEMS INSPECTOR",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = NothingWhite
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                    ) {
                        Text(
                            text = "✕ CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Detailed Inspector Card for Selected Item
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = NothingCardSurface,
                    border = BorderStroke(1.dp, if (isInspectedEquipped) NothingRed else NothingBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "GEAR SLOT: [${inspectedItem.slot}]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingRed
                            )
                            Text(
                                text = if (isInspectedUnlocked) "IN INVENTORY" else "REQ. LVL ${inspectedItem.requiredLevel}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isInspectedUnlocked) Color(0xFF00E676) else NothingTextSecondary
                            )
                        }

                        Text(
                            text = inspectedItem.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )

                        Text(
                            text = inspectedItem.description,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = NothingTextSecondary,
                            lineHeight = 14.sp
                        )

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = when {
                                isInspectedEquipped -> NothingSurfaceVariant
                                isInspectedUnlocked -> NothingRed
                                else -> NothingDarkBackground
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isInspectedEquipped -> Color(0xFF00E676)
                                    isInspectedUnlocked -> NothingRed
                                    else -> NothingBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = isInspectedUnlocked) {
                                    AccountAndInventoryManager.toggleEquipGearItem(context, inspectedItem.id)
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when {
                                        isInspectedEquipped -> "UNEQUIP FROM [${inspectedItem.slot}]"
                                        isInspectedUnlocked -> "EQUIP TO [${inspectedItem.slot}]"
                                        else -> "LOCKED • UNLOCKS AT LEVEL ${inspectedItem.requiredLevel} OR STORE"
                                    },
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "ALL ITEMS IN INVENTORY (${unlockedItemIds.size}/${catalog.size}) • TAP TO INSPECT",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = NothingTextSecondary
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    catalog.forEach { item ->
                        val isUnlocked = item.id in unlockedItemIds
                        val isEquipped = item.id in equippedItemIds
                        val isSelected = item.id == inspectedItem.id

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) NothingCardSurface else NothingDarkBackground,
                            border = BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> NothingRed
                                    isEquipped -> Color(0xFF00E676)
                                    else -> NothingBorder
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    inspectedItemId = item.id
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "[${item.slot}] ${item.name}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) NothingWhite else NothingTextSecondary
                                    )
                                    Text(
                                        text = item.description,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        color = NothingTextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when {
                                        isEquipped -> Color(0xFF00E676).copy(alpha = 0.18f)
                                        isUnlocked -> NothingSurfaceVariant
                                        else -> NothingDarkBackground
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isEquipped) Color(0xFF00E676) else NothingBorder
                                    )
                                ) {
                                    Text(
                                        text = when {
                                            isEquipped -> "EQUIPPED"
                                            isUnlocked -> "OWNED"
                                            else -> "LVL ${item.requiredLevel}"
                                        },
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isEquipped) Color(0xFF00E676) else NothingWhite,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

/**
 * FULL BUDDYGOTCHI STORE DIALOG:
 * Browse Buddies, Items, and Goodies. Claim free starter drops & XP rewards or inspect upcoming Wave 2 drops.
 */
@Composable
fun BuddyStoreDialog(
    initialCategory: StoreCategory?,
    buddyXp: Int,
    buddyLevel: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeFilter by remember { mutableStateOf(initialCategory) }
    var feedbackBanner by remember { mutableStateOf<String?>(null) }

    val allEntries = AccountAndInventoryManager.storeCatalog
    val filteredEntries = remember(activeFilter) {
        if (activeFilter == null) allEntries else allEntries.filter { it.category == activeFilter }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = NothingDarkBackground,
            border = BorderStroke(1.dp, NothingBorder),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.86f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
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
                                text = "BUDDYGOTCHI STORE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingWhite
                            )
                            Text(
                                text = "BALANCE: $buddyXp XP (LVL $buddyLevel) • NO HIDDEN FEES",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 8.5.sp,
                                color = NothingTextSecondary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NothingSurfaceVariant,
                        border = BorderStroke(1.dp, NothingBorder),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                    ) {
                        Text(
                            text = "✕ CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Filter Tabs: ALL | BUDDIES | ITEMS | GOODIES
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabs: List<Pair<String, StoreCategory?>> = listOf(
                        "ALL" to null,
                        "BUDDIES" to StoreCategory.BUDDIES,
                        "ITEMS" to StoreCategory.ITEMS,
                        "GOODIES" to StoreCategory.GOODIES
                    )
                    tabs.forEach { (label, cat) ->
                        val isSelected = activeFilter == cat
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) NothingRed else NothingCardSurface,
                            border = BorderStroke(1.dp, if (isSelected) NothingRed else NothingBorder),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { activeFilter = cat }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )
                            }
                        }
                    }
                }

                if (feedbackBanner != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = NothingCardSurface,
                        border = BorderStroke(1.dp, NothingRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = feedbackBanner!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    filteredEntries.forEach { entry ->
                        val isUnlocked = AccountAndInventoryManager.isStoreEntryUnlocked(entry)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = NothingCardSurface,
                            border = BorderStroke(
                                1.dp,
                                if (isUnlocked) Color(0xFF00E676) else NothingBorder
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "[${entry.category.label}] ${entry.subtitle}",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingRed
                                    )
                                    Text(
                                        text = if (isUnlocked) "OWNED" else entry.priceLabel,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUnlocked) Color(0xFF00E676) else NothingWhite
                                    )
                                }

                                Text(
                                    text = entry.title,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NothingWhite
                                )

                                Text(
                                    text = entry.description,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.5.sp,
                                    color = NothingTextSecondary,
                                    lineHeight = 13.5.sp
                                )

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = when {
                                        isUnlocked -> Color(0xFF00E676).copy(alpha = 0.16f)
                                        entry.isAvailableNow -> NothingRed
                                        else -> NothingSurfaceVariant
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        when {
                                            isUnlocked -> Color(0xFF00E676)
                                            entry.isAvailableNow -> NothingRed
                                            else -> NothingBorder
                                        }
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = !isUnlocked && entry.isAvailableNow) {
                                            val res = AccountAndInventoryManager.claimStoreEntry(context, entry)
                                            feedbackBanner = res.getOrElse { it.message ?: "Unable to unlock" }
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 9.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when {
                                                isUnlocked -> "✓ IN YOUR INVENTORY (AUTO-SYNCED)"
                                                entry.isAvailableNow -> "CLAIM / UNLOCK (${entry.priceLabel})"
                                                else -> "COMING SOON IN FUTURE STORE WAVE"
                                            },
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isUnlocked) Color(0xFF00E676) else NothingWhite
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
