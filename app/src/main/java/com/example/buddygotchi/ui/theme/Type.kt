package com.example.buddygotchi.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.buddygotchi.R

val Dseg7FontFamily = FontFamily(
    Font(R.font.dseg7_classic_regular, FontWeight.Normal),
    Font(R.font.dseg7_classic_bold, FontWeight.Bold),
    Font(R.font.dseg7_classic_bold, FontWeight.SemiBold)
)

val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)