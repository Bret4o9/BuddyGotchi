package com.example.buddygotchi

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.ui.theme.Dseg7FontFamily
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
 * Reusable Nothing OS circle checkbox for grocery items.
 */
@Composable
fun GroceryCheckCircle(
    isChecked: Boolean,
    onClick: () -> Unit,
    size: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                if (isChecked) NothingTextTertiary.copy(alpha = 0.35f)
                else Color.Transparent
            )
            .border(
                width = 1.5.dp,
                color = if (isChecked) NothingTextTertiary.copy(alpha = 0.7f) else NothingRed,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isChecked) {
            Text(
                text = "✓",
                fontSize = (size.value * 0.58f).sp,
                fontWeight = FontWeight.Bold,
                color = NothingTextSecondary,
                lineHeight = (size.value * 0.6f).sp
            )
        }
    }
}

/**
 * Collapsed Grocery List Widget card rendered on any Dashboard page or Shelf.
 * Supports interactive item checking, LIST / GRID mode switching, completion prompt,
 * and expanding to full screen.
 */
@Composable
fun GroceryCollapsedWidget(
    groceryLists: List<GroceryList>,
    onListsUpdated: (List<GroceryList>) -> Unit = {},
    onExpand: () -> Unit,
    onQuickCreate: () -> Unit,
    onFinishTaskReward: () -> Unit = {},
    size: WidgetSize = WidgetSize.WIDE,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val latestList = groceryLists.firstOrNull()

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

    val totalItems = latestList?.entries?.size ?: 0
    val checkedItems = latestList?.entries?.count { it.isChecked } ?: 0
    val allChecked = totalItems > 0 && checkedItems == totalItems

    // Track whether the user chose "CHECK LIST" for the current completed state
    var hasReviewedCompletedList by remember(latestList?.id, allChecked) {
        mutableStateOf(false)
    }

    var fallbackMode by remember { mutableStateOf(GroceryViewMode.LIST) }
    val currentViewMode = latestList?.viewMode ?: fallbackMode

    fun toggleEntryOnWidget(entryId: String) {
        val current = latestList ?: return
        val updatedEntries = GroceryManager.sortEntries(
            current.entries.map { entry ->
                if (entry.id == entryId) entry.copy(isChecked = !entry.isChecked) else entry
            }
        )
        val updatedList = current.copy(entries = updatedEntries)
        onListsUpdated(GroceryManager.upsertList(context, updatedList))
    }

    fun toggleModeOnWidget(newMode: GroceryViewMode) {
        fallbackMode = newMode
        val current = latestList ?: return
        if (current.viewMode == newMode) return
        val updatedList = current.copy(viewMode = newMode)
        onListsUpdated(GroceryManager.upsertList(context, updatedList))
    }

    fun finishAndDeleteLatestList() {
        val current = latestList ?: return
        val updated = GroceryManager.deleteList(context, current.id)
        onListsUpdated(updated)
        TaskManager.addBuddyXp(context, 15)
        onFinishTaskReward()
    }

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = NothingCardSurface),
        border = BorderStroke(1.dp, if (allChecked && !hasReviewedCompletedList) NothingRed else NothingBorder),
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
                        text = if (latestList != null) "$checkedItems/$totalItems" else "0",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "GROCERY",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = NothingTextSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentViewMode.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.sp,
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
                            text = if (latestList != null) "$checkedItems/$totalItems" else "0",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                        Text(
                            text = "GROCERY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingTextSecondary
                        )
                    }

                    if (latestList != null) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            latestList.entries.take(size.rows + 1).forEach { entry ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.alpha(if (entry.isChecked) 0.45f else 1f)
                                ) {
                                    GroceryCheckCircle(
                                        isChecked = entry.isChecked,
                                        onClick = { toggleEntryOnWidget(entry.id) },
                                        size = 12.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = entry.text.ifBlank { "Item" },
                                        fontSize = 8.5.sp,
                                        color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                                        textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = currentViewMode.name,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 7.5.sp,
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                text = "GROCERIES [$checkedItems/$totalItems]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Text(
                                text = if (latestList != null) {
                                    if (latestList.title.isNotBlank()) latestList.title else "UNTITLED LIST"
                                } else "NO LISTS",
                                fontSize = 8.5.sp,
                                color = NothingTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
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
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                                text = "GROCERY [$checkedItems/$totalItems]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }

                        Text(
                            text = currentViewMode.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed
                        )
                    }

                    if (latestList != null && allChecked && !hasReviewedCompletedList) {
                        // Compact Completion Prompt inside 2x2+ block
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingDarkBackground)
                                .border(1.dp, NothingRed.copy(alpha = 0.7f), RoundedCornerShape(10.dp))
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ALL ITEMS GOT!",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NothingRed)
                                        .clickable { finishAndDeleteLatestList() }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "FINISH",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingWhite
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(NothingSurfaceVariant)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(6.dp))
                                        .clickable { hasReviewedCompletedList = true }
                                        .padding(vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CHECK",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingWhite
                                    )
                                }
                            }
                        }
                    } else if (latestList != null && latestList.entries.isNotEmpty()) {
                        if (latestList.viewMode == GroceryViewMode.LIST) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NothingDarkBackground)
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val maxItems = if (size.rows == 2) 3 else 6
                                latestList.entries.take(maxItems).forEach { entry ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (entry.isChecked) 0.45f else 1f)
                                    ) {
                                        GroceryCheckCircle(
                                            isChecked = entry.isChecked,
                                            onClick = { toggleEntryOnWidget(entry.id) },
                                            size = 14.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = entry.text.ifBlank { "Unnamed item" },
                                            fontSize = 9.5.sp,
                                            color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                                            textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        } else {
                            // Mini 1x1 Grid Mode preview
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                latestList.entries.take(2).forEach { entry ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(NothingDarkBackground)
                                            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                            .alpha(if (entry.isChecked) 0.45f else 1f)
                                            .clickable { toggleEntryOnWidget(entry.id) }
                                            .padding(6.dp)
                                    ) {
                                        GroceryCheckCircle(
                                            isChecked = entry.isChecked,
                                            onClick = { toggleEntryOnWidget(entry.id) },
                                            size = 13.dp,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )
                                        Text(
                                            text = entry.text.ifBlank { "Item" },
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                                            textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "NO GROCERIES YET",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = NothingTextTertiary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
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
                            text = "GROCERIES [$checkedItems/$totalItems]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        if (latestList != null) {
                            Text(
                                text = latestList.title.ifBlank { "(UNTITLED LIST)" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.5.sp,
                                color = NothingTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
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

            // === 6. FULL RICH DISPLAY (4x2, 4x3, 4x4, 3x2+) ===
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(internalPadding),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Header Row: Title on the left, grouped [ ≡ LIST | ⊞ GRID ] switcher on the right (like expanded view)
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
                                text = "GROCERIES",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "[ $checkedItems/$totalItems ]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (allChecked) NothingRed else NothingTextSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // Grouped Mode Switcher Pill (≡ LIST | ⊞ GRID) matching the expanded view
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(NothingDarkBackground)
                                .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                                .padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isListMode = currentViewMode == GroceryViewMode.LIST
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (isListMode) NothingRed else Color.Transparent)
                                    .clickable { toggleModeOnWidget(GroceryViewMode.LIST) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "≡ LIST",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isListMode) NothingWhite else NothingTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(if (!isListMode) NothingRed else Color.Transparent)
                                    .clickable { toggleModeOnWidget(GroceryViewMode.GRID) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "⊞ GRID",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isListMode) NothingWhite else NothingTextSecondary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Body Area: Either Completion Prompt, Active List/Grid Items, or Empty State
                    if (latestList != null && allChecked && !hasReviewedCompletedList) {
                        // All items checked prompt right on the widget!
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(NothingDarkBackground)
                                .border(1.dp, NothingRed.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ALL GROCERIES CHECKED // ${latestList.title.ifBlank { "GROCERY LIST" }}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NothingRed)
                                        .clickable { finishAndDeleteLatestList() }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "✓ FINISH TASK",
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
                                        .background(NothingSurfaceVariant)
                                        .border(1.dp, NothingBorder, RoundedCornerShape(8.dp))
                                        .clickable {
                                            hasReviewedCompletedList = true
                                            onExpand()
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "CHECK LIST",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NothingWhite
                                    )
                                }
                            }
                        }
                    } else if (latestList != null && latestList.entries.isNotEmpty()) {
                        if (latestList.viewMode == GroceryViewMode.LIST) {
                            // LIST MODE preview: lines with circle checks, checked items at bottom grayed out
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(NothingDarkBackground)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val maxVisible = if (size.rows <= 2) 2 else 6
                                latestList.entries.take(maxVisible).forEach { entry ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .alpha(if (entry.isChecked) 0.45f else 1f)
                                            .clickable { toggleEntryOnWidget(entry.id) },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        GroceryCheckCircle(
                                            isChecked = entry.isChecked,
                                            onClick = { toggleEntryOnWidget(entry.id) },
                                            size = 15.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = entry.text.ifBlank { "Unnamed grocery" },
                                            fontSize = 11.sp,
                                            color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                                            textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        } else {
                            // GRID MODE preview: 1x1 square cells with circle checks
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val cellsToShow = latestList.entries.take(4)
                                cellsToShow.forEach { entry ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(if (size.rows <= 2) 84.dp else 96.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(NothingDarkBackground)
                                            .border(
                                                width = 1.dp,
                                                color = if (entry.isChecked) NothingBorder.copy(alpha = 0.4f) else NothingBorder,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .alpha(if (entry.isChecked) 0.42f else 1f)
                                            .clickable { toggleEntryOnWidget(entry.id) }
                                            .padding(8.dp)
                                    ) {
                                        GroceryCheckCircle(
                                            isChecked = entry.isChecked,
                                            onClick = { toggleEntryOnWidget(entry.id) },
                                            size = 15.dp,
                                            modifier = Modifier.align(Alignment.TopStart)
                                        )

                                        Text(
                                            text = entry.text.ifBlank { "Item" },
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            lineHeight = 12.sp,
                                            color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                                            textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.align(Alignment.BottomStart)
                                        )
                                    }
                                }
                                // Fill remaining slots up to 4 so each cell stays a 1x1 square
                                repeat((4 - cellsToShow.size).coerceAtLeast(0)) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
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
                                text = "NO GROCERY LISTS // TAP TO ADD",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }

                    // Footer Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = latestList?.title?.ifBlank { "TAP TO EDIT GROCERIES" } ?: "TAP CARD TO EXPAND",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            letterSpacing = 0.8.sp,
                            color = NothingTextTertiary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingRed.copy(alpha = 0.15f))
                                .border(1.dp, NothingRed, RoundedCornerShape(8.dp))
                                .clickable { onQuickCreate() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "+ NEW LIST",
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
 * Fullscreen / Full-page expanded Grocery List experience.
 * Supports multiple grocery lists, List Mode & 1x1 Grid Mode, moving checked items
 * to the bottom and graying them out, and the "FINISH TASK" / "CHECK LIST" completion prompt.
 */
@Composable
fun GroceryExpandedView(
    groceryLists: List<GroceryList>,
    initialEditingListId: String? = null,
    onListsUpdated: (List<GroceryList>) -> Unit,
    onFinishTaskReward: () -> Unit = {},
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedListId by remember {
        mutableStateOf(initialEditingListId ?: groceryLists.firstOrNull()?.id)
    }
    val dateFormat = remember { SimpleDateFormat("EEE, MMM d, HH:mm", Locale.getDefault()) }

    val activeList = groceryLists.firstOrNull { it.id == selectedListId }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NothingDarkBackground)
    ) {
        if (activeList == null) {
            // === OVERVIEW: All Grocery Lists ===
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(NothingRed)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "GROCERIES // ${groceryLists.size} LISTS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingRed)
                            .clickable {
                                val newList = GroceryList(
                                    title = "GROCERY LIST",
                                    entries = emptyList(),
                                    viewMode = GroceryViewMode.LIST,
                                    updatedAt = System.currentTimeMillis()
                                )
                                val updated = GroceryManager.upsertList(context, newList)
                                onListsUpdated(updated)
                                selectedListId = newList.id
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

                if (groceryLists.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO GROCERY LISTS YET",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = NothingTextSecondary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tap '+ NEW' to start a new grocery list",
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
                        items(groceryLists, key = { it.id }) { listItem ->
                            GroceryListOverviewCard(
                                groceryList = listItem,
                                dateFormat = dateFormat,
                                onClick = { selectedListId = listItem.id },
                                onDelete = {
                                    val updated = GroceryManager.deleteList(context, listItem.id)
                                    onListsUpdated(updated)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // === SINGLE GROCERY LIST EDITOR (LIST MODE & 1x1 GRID MODE) ===
            GroceryListEditorScreen(
                groceryList = activeList,
                onBackToAllLists = { selectedListId = null },
                onCollapse = onCollapse,
                onDeleteList = {
                    val updated = GroceryManager.deleteList(context, activeList.id)
                    onListsUpdated(updated)
                    selectedListId = updated.firstOrNull()?.id
                },
                onFinishTaskAndDelete = {
                    val updated = GroceryManager.deleteList(context, activeList.id)
                    onListsUpdated(updated)
                    TaskManager.addBuddyXp(context, 15)
                    onFinishTaskReward()
                    selectedListId = updated.firstOrNull()?.id
                },
                onUpdateList = { updatedList ->
                    val updated = GroceryManager.upsertList(context, updatedList)
                    onListsUpdated(updated)
                }
            )
        }
    }
}

@Composable
private fun GroceryListOverviewCard(
    groceryList: GroceryList,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val total = groceryList.entries.size
    val checked = groceryList.entries.count { it.isChecked }

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
                        text = groceryList.title.ifBlank { "(UNTITLED GROCERIES)" },
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${groceryList.viewMode.name} · $checked/$total",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingRed
                    )
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
            }

            Spacer(modifier = Modifier.height(6.dp))

            val previewText = if (groceryList.entries.isEmpty()) {
                "Empty grocery list..."
            } else {
                groceryList.entries.joinToString(", ") {
                    if (it.isChecked) "✓ ${it.text}" else "○ ${it.text}"
                }
            }
            Text(
                text = previewText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = NothingTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = dateFormat.format(Date(groceryList.updatedAt)),
                fontFamily = FontFamily.Monospace,
                fontSize = 8.5.sp,
                color = NothingTextTertiary
            )
        }
    }
}

@Composable
private fun GroceryListEditorScreen(
    groceryList: GroceryList,
    onBackToAllLists: () -> Unit,
    onCollapse: () -> Unit,
    onDeleteList: () -> Unit,
    onFinishTaskAndDelete: () -> Unit,
    onUpdateList: (GroceryList) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var titleText by remember(groceryList.id) { mutableStateOf(groceryList.title) }
    var newItemText by remember(groceryList.id) { mutableStateOf("") }

    val sortedEntries = remember(groceryList.entries) {
        GroceryManager.sortEntries(groceryList.entries)
    }

    val totalCount = sortedEntries.size
    val checkedCount = sortedEntries.count { it.isChecked }
    val allItemsChecked = totalCount > 0 && checkedCount == totalCount

    // Tracks whether the user clicked "CHECK LIST" on the completion prompt so they can review items
    var isReviewingCompletedList by remember(groceryList.id, allItemsChecked) {
        mutableStateOf(false)
    }

    LaunchedEffect(allItemsChecked) {
        if (allItemsChecked) {
            focusManager.clearFocus()
        }
    }

    fun saveTitle(newTitle: String) {
        titleText = newTitle
        onUpdateList(groceryList.copy(title = newTitle))
    }

    fun setViewMode(mode: GroceryViewMode) {
        if (groceryList.viewMode != mode) {
            onUpdateList(groceryList.copy(viewMode = mode))
        }
    }

    fun addGroceryItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val newEntry = GroceryItemEntry(text = trimmed, isChecked = false)
        // Insert new unchecked item at the end of the unchecked section (before checked items)
        val unchecked = sortedEntries.filter { !it.isChecked }
        val checked = sortedEntries.filter { it.isChecked }
        val updatedEntries = unchecked + newEntry + checked
        onUpdateList(groceryList.copy(entries = updatedEntries))
        newItemText = ""
    }

    fun toggleGroceryItem(entryId: String) {
        focusManager.clearFocus()
        val updatedEntries = GroceryManager.sortEntries(
            sortedEntries.map { entry ->
                if (entry.id == entryId) entry.copy(isChecked = !entry.isChecked) else entry
            }
        )
        onUpdateList(groceryList.copy(entries = updatedEntries))
    }

    fun updateGroceryItemText(entryId: String, newText: String) {
        // If the user presses Enter (newline) inside an item in List Mode, split/add a new item!
        if (newText.contains("\n")) {
            val parts = newText.split("\n")
            val firstLine = parts.first().trim()
            val restLine = parts.drop(1).joinToString(" ").trim()
            val mutable = sortedEntries.toMutableList()
            val idx = mutable.indexOfFirst { it.id == entryId }
            if (idx != -1) {
                mutable[idx] = mutable[idx].copy(text = firstLine)
                val inserted = GroceryItemEntry(text = restLine, isChecked = false)
                mutable.add(idx + 1, inserted)
                onUpdateList(groceryList.copy(entries = GroceryManager.sortEntries(mutable)))
            }
            return
        }

        val updatedEntries = sortedEntries.map { entry ->
            if (entry.id == entryId) entry.copy(text = newText) else entry
        }
        onUpdateList(groceryList.copy(entries = updatedEntries))
    }

    fun removeGroceryItem(entryId: String) {
        val updatedEntries = sortedEntries.filter { it.id != entryId }
        onUpdateList(groceryList.copy(entries = updatedEntries))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 12.dp)
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(NothingCardSurface)
                            .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                            .clickable { onBackToAllLists() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "← ALL LISTS",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                    }

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

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingSurfaceVariant)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .clickable { onDeleteList() }
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

            Spacer(modifier = Modifier.height(12.dp))

            // Title & Mode Switcher Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (titleText.isEmpty()) {
                        Text(
                            text = "GROCERY LIST...",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = NothingTextTertiary
                        )
                    }
                    BasicTextField(
                        value = titleText,
                        onValueChange = { saveTitle(it) },
                        singleLine = true,
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

                Spacer(modifier = Modifier.width(10.dp))

                // Mode Picker: LIST MODE vs GRID MODE
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(NothingCardSurface)
                        .border(1.dp, NothingBorder, RoundedCornerShape(10.dp))
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isListMode = groceryList.viewMode == GroceryViewMode.LIST
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isListMode) NothingRed else Color.Transparent)
                            .clickable { setViewMode(GroceryViewMode.LIST) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "≡ LIST",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isListMode) NothingWhite else NothingTextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (!isListMode) NothingRed else Color.Transparent)
                            .clickable { setViewMode(GroceryViewMode.GRID) }
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "⊞ GRID",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!isListMode) NothingWhite else NothingTextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Add New Grocery Item Input Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(NothingCardSurface)
                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, NothingRed.copy(alpha = 0.7f), CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (newItemText.isEmpty()) {
                        Text(
                            text = "Write a grocery item & tap + ADD...",
                            fontSize = 12.5.sp,
                            color = NothingTextTertiary
                        )
                    }
                    BasicTextField(
                        value = newItemText,
                        onValueChange = { input ->
                            if (input.contains("\n")) {
                                addGroceryItem(input.replace("\n", ""))
                            } else {
                                newItemText = input
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = { addGroceryItem(newItemText) }
                        ),
                        textStyle = TextStyle(
                            fontSize = 12.5.sp,
                            color = NothingWhite
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(NothingRed)
                        .clickable { addGroceryItem(newItemText) }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "+ ADD",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Persistent "REVIEWING COMPLETED LIST" banner when the user chose "CHECK LIST"
            AnimatedVisibility(visible = allItemsChecked && isReviewingCompletedList) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = NothingCardSurface,
                    border = BorderStroke(1.dp, NothingRed),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "CHECKING COMPLETED LIST",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                            Text(
                                text = "Uncheck forgotten items or finish task to delete list",
                                fontSize = 8.5.sp,
                                color = NothingTextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(NothingRed)
                                .clickable { onFinishTaskAndDelete() }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "✓ FINISH TASK",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }
                }
            }

            // Main Content Area: LIST MODE vs 1x1 GRID MODE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (sortedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "NO GROCERIES IN LIST",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingTextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Type an item above to add it to your list",
                                fontSize = 11.sp,
                                color = NothingTextTertiary
                            )
                        }
                    }
                } else if (groceryList.viewMode == GroceryViewMode.LIST) {
                    // === LIST MODE: Regular notes style with each grocery on its own line + circle check ===
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedEntries, key = { it.id }) { entry ->
                            GroceryListModeRow(
                                entry = entry,
                                onToggleCheck = { toggleGroceryItem(entry.id) },
                                onTextChange = { updateGroceryItemText(entry.id, it) },
                                onDelete = { removeGroceryItem(entry.id) }
                            )
                        }
                    }
                } else {
                    // === GRID MODE: Each item within its own 1x1 square cell + circle check ===
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(sortedEntries, key = { it.id }) { entry ->
                            GroceryGridModeCell(
                                entry = entry,
                                onToggleCheck = { toggleGroceryItem(entry.id) },
                                onTextChange = { updateGroceryItemText(entry.id, it) },
                                onDelete = { removeGroceryItem(entry.id) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Footer Status Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$checkedCount / $totalCount ITEMS CHECKED // ${groceryList.viewMode.name} MODE",
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

        // === COMPLETION PROMPT OVERLAY ("FINISH TASK" or "CHECK LIST") ===
        AnimatedVisibility(
            visible = allItemsChecked && !isReviewingCompletedList,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.78f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = NothingCardSurface,
                    border = BorderStroke(1.5.dp, NothingRed),
                    shadowElevation = 20.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(NothingRed)
                            )
                            Text(
                                text = "GROCERY LIST COMPLETE",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.5.sp,
                                color = NothingRed
                            )
                        }

                        Text(
                            text = "$totalCount / $totalCount",
                            fontFamily = Dseg7FontFamily,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )

                        Text(
                            text = "You have checked all items in \"${titleText.ifBlank { "GROCERY LIST" }}\". Would you like to finish the task and delete this list, or check the list in case you forgot something?",
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = NothingTextSecondary,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Option 1: CHECK LIST (review items in case you forgot something)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingSurfaceVariant)
                                    .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        isReviewingCompletedList = true
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "CHECK LIST",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp,
                                    color = NothingWhite
                                )
                            }

                            // Option 2: FINISH TASK (finish task and delete the list)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(NothingRed)
                                    .clickable {
                                        onFinishTaskAndDelete()
                                    }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "FINISH TASK",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.5.sp,
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
}

/**
 * Single grocery line in LIST MODE.
 * Features the clickable circle checkbox on the left, editable grocery text in the middle,
 * and grayed-out styling when checked.
 */
@Composable
private fun GroceryListModeRow(
    entry: GroceryItemEntry,
    onToggleCheck: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (entry.isChecked) NothingDarkBackground
                else NothingCardSurface
            )
            .border(
                width = 1.dp,
                color = if (entry.isChecked) NothingBorder.copy(alpha = 0.4f) else NothingBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .alpha(if (entry.isChecked) 0.48f else 1f)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroceryCheckCircle(
            isChecked = entry.isChecked,
            onClick = onToggleCheck,
            size = 20.dp
        )

        Spacer(modifier = Modifier.width(12.dp))

        BasicTextField(
            value = entry.text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontSize = 13.5.sp,
                fontWeight = if (entry.isChecked) FontWeight.Normal else FontWeight.Medium,
                color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush = SolidColor(NothingRed),
            modifier = Modifier.weight(1f)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = "✕",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = NothingTextTertiary,
            modifier = Modifier
                .clickable { onDelete() }
                .padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

/**
 * Single 1x1 square cell in GRID MODE.
 * Displays the grocery item inside its own 1x1 cell with the circle check in the top-left,
 * delete button in the top-right, and grocery name below. Moves down & grays out when checked.
 */
@Composable
private fun GroceryGridModeCell(
    entry: GroceryItemEntry,
    onToggleCheck: () -> Unit,
    onTextChange: (String) -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (entry.isChecked) NothingDarkBackground
                else NothingCardSurface
            )
            .border(
                width = 1.dp,
                color = if (entry.isChecked) NothingBorder.copy(alpha = 0.4f) else NothingBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .alpha(if (entry.isChecked) 0.45f else 1f)
            .clickable { onToggleCheck() }
            .padding(10.dp)
    ) {
        // Top row: Circle Check on left, Delete '✕' on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GroceryCheckCircle(
                isChecked = entry.isChecked,
                onClick = onToggleCheck,
                size = 20.dp
            )

            Text(
                text = "✕",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = NothingTextTertiary,
                modifier = Modifier
                    .clickable { onDelete() }
                    .padding(2.dp)
            )
        }

        // Bottom/Center: Editable Grocery Item Name inside the 1x1 cell
        BasicTextField(
            value = entry.text,
            onValueChange = onTextChange,
            textStyle = TextStyle(
                fontSize = 12.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (entry.isChecked) NothingTextTertiary else NothingWhite,
                textDecoration = if (entry.isChecked) TextDecoration.LineThrough else TextDecoration.None
            ),
            cursorBrush = SolidColor(NothingRed),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
        )
    }
}
