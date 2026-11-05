package com.example.buddygotchi.squad

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.buddygotchi.AccountAndInventoryManager
import com.example.buddygotchi.Buddy3DManager
import com.example.buddygotchi.ui.theme.*
import kotlin.math.abs

@Composable
fun SquadFriendProfileDialog(
    friend: SquadFriend,
    isStaged: Boolean,
    onDismiss: () -> Unit,
    onToggleStage: () -> Unit
) {
    val context = LocalContext.current
    val petCatalog = remember { AccountAndInventoryManager.petCatalog }
    val catalogEntry = remember(friend.buddyCode) {
        val idx = abs(friend.buddyCode.hashCode()) % petCatalog.size
        petCatalog[idx]
    }
    val petScreenshot = remember(catalogEntry.id) {
        Buddy3DManager.getBuddyScreenshotBitmap(context, catalogEntry.id)
    }

    val friendMood = remember(friend.buddyCode) {
        val moods = listOf(
            "HAPPY \uD83D\uDE0A",
            "CHILL \uD83D\uDE0E",
            "HYPED ⚡",
            "PLAYFUL \uD83C\uDF89",
            "FOCUSED \uD83C\uDFAF",
            "COZY ☕",
            "CURIOUS \uD83D\uDD0D"
        )
        val idx = abs(friend.buddyCode.hashCode()) % moods.size
        moods[idx]
    }

    fun copyFriendCode() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Buddy Code", friend.buddyCode)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(context, "${friend.displayName}'S CODE ${friend.buddyCode} COPIED", Toast.LENGTH_SHORT).show()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
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
                                .background(if (friend.isOnline) Color(0xFF00E676) else NothingRed)
                        )
                        Text(
                            text = "BUDDY // ${friend.displayName.uppercase()}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
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
                        Text(
                            text = "✕",
                            fontSize = 12.sp,
                            color = NothingTextSecondary
                        )
                    }
                }

                // Equipped Companion Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF282828), RoundedCornerShape(16.dp))
                        .padding(vertical = 16.dp, horizontal = 14.dp)
                ) {
                    // 3D Companion Preview
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF101010)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (petScreenshot != null) {
                            Image(
                                bitmap = petScreenshot.asImageBitmap(),
                                contentDescription = "Equipped Companion",
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = friend.displayName.take(1).uppercase(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = NothingWhite
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = catalogEntry.name,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            letterSpacing = 0.8.sp,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "EQUIPPED COMPANION",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 7.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            letterSpacing = 1.sp
                        )
                    }
                }

                // 3-Column Stats Row: Level, Character, Mood
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 1. Level Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF282828), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "LEVEL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = "LVL ${friend.level}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite
                        )
                    }

                    // 2. Character Variant Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF282828), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "CHARACTER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = catalogEntry.name.split(" ").firstOrNull() ?: "BUDDY",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingRed,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // 3. Mood Card
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, Color(0xFF282828), RoundedCornerShape(12.dp))
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "MOOD",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = friendMood,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            maxLines = 1
                        )
                    }
                }

                // Buddy Code Row with Tap-To-Copy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF141414))
                        .border(1.dp, Color(0xFF282828), RoundedCornerShape(12.dp))
                        .clickable { copyFriendCode() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "SQUAD BUDDY CODE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextTertiary,
                            letterSpacing = 0.8.sp
                        )
                        Text(
                            text = friend.buddyCode,
                            fontFamily = Dseg7FontFamily,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = NothingWhite
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (friend.isOnline) Color(0xFF00E676) else Color.Gray)
                        )
                        Text(
                            text = if (friend.isOnline) "ONLINE" else "OFFLINE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (friend.isOnline) Color(0xFF00E676) else NothingTextTertiary
                        )
                    }
                }

                // Bottom Action: Stage toggle / Done
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF202020))
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(12.dp))
                            .clickable { onDismiss() }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "CLOSE",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingTextSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.3f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isStaged) Color(0xFF2A1515) else NothingRed)
                            .border(1.dp, if (isStaged) NothingRed else Color.Transparent, RoundedCornerShape(12.dp))
                            .clickable {
                                onToggleStage()
                                onDismiss()
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isStaged) "UNSTAGE BUDDY" else "+ STAGE FOR EVENT",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }
        }
    }
}
