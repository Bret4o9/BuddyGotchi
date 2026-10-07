package com.example.buddygotchi

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WidgetCard(
    title: String,
    height: Dp,
    modifier: Modifier = Modifier,
    size: WidgetSize = WidgetSize.WIDE,
    headerAction: @Composable (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isCompact = size == WidgetSize.CUBE || size == WidgetSize.TALL
    val isCube = isCompact
    val cornerRadius = if (isCompact) 20.dp else 24.dp
    val internalPadding = when (size) {
        WidgetSize.CUBE, WidgetSize.TALL -> 8.dp
        WidgetSize.SLIM -> 10.dp
        else -> 16.dp
    }

    val animatedHeight by animateDpAsState(
        targetValue = height,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardHeight_$title"
    )

    Card(
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(animatedHeight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (title.isNotBlank() || headerAction != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (title.isNotBlank()) {
                        Text(
                            text = title,
                            fontSize = if (isCube) 8.5.sp else 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = if (isCube) 1.sp else 1.5.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    if (headerAction != null) {
                        headerAction()
                    }
                }
                Spacer(modifier = Modifier.height(if (isCube) 2.dp else 4.dp))
            }
            content()
        }
    }
}