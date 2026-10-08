package com.example.buddygotchi.squad

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.buddygotchi.ui.theme.*

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAddFriend: (buddyCode: String, name: String) -> Unit
) {
    var codeInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            color = NothingCardSurface,
            border = BorderStroke(1.dp, NothingBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
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
                            text = "CONNECT SQUAD MEMBER",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
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

                Text(
                    text = "ENTER 6-CHARACTER BUDDY CODE (E.G. BG-4821)",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = NothingTextTertiary,
                    letterSpacing = 0.5.sp
                )

                // Code Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "BUDDY CODE",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    BasicTextField(
                        value = codeInput,
                        onValueChange = { if (it.length <= 10) codeInput = it.uppercase() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, if (codeInput.isNotBlank()) NothingRed else Color(0xFF2B2B2B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        textStyle = TextStyle(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = NothingWhite,
                            letterSpacing = 2.sp
                        ),
                        cursorBrush = SolidColor(NothingRed),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        singleLine = true,
                        decorationBox = { innerTextField ->
                            if (codeInput.isEmpty()) {
                                Text(
                                    text = "BG-XXXX",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 15.sp,
                                    color = NothingTextTertiary.copy(alpha = 0.4f),
                                    letterSpacing = 2.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                // Name Input
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "NAME // NICKNAME (OPTIONAL)",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = NothingTextSecondary,
                        letterSpacing = 1.sp
                    )
                    BasicTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it.uppercase() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, if (nameInput.isNotBlank()) Color(0xFF444444) else Color(0xFF2B2B2B), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
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
                            if (nameInput.isEmpty()) {
                                Text(
                                    text = "E.G. ALEX",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp,
                                    color = NothingTextTertiary.copy(alpha = 0.4f),
                                    letterSpacing = 1.sp
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Action Button
                val isValid = codeInput.trim().length >= 4
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isValid) NothingRed else Color(0xFF2A2A2A))
                        .clickable(enabled = isValid) {
                            onAddFriend(codeInput.trim(), nameInput.trim())
                            onDismiss()
                        }
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "LINK BUDDY +",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = if (isValid) Color.White else NothingTextTertiary
                    )
                }
            }
        }
    }
}
