package com.example.buddygotchi

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.buddygotchi.ui.theme.BuddyGotchiTheme

@Preview(showBackground =true)
@Composable
fun DashboardPreview() {
    BuddyGotchiTheme {
        DashboardScreen()
    }
}