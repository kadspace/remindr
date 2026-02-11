package com.remindr.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.remindr.core.CalendarDay
import com.kizitonwose.remindr.core.DayPosition
import com.remindr.app.data.model.Item
import com.remindr.app.ui.theme.Colors

private val itemBackgroundColor: Color = Colors.example5ItemViewBgColor
private val selectedItemColor: Color = Colors.accent
private val inActiveTextColor: Color = Colors.example5TextGreyLight

@Composable
fun DayCell(
    day: CalendarDay,
    isSelected: Boolean = false,
    isHighlighted: Boolean = false,
    items: List<Item> = emptyList(),
    onClick: (CalendarDay) -> Unit = {},
) {
    val pulseAlpha = remember { Animatable(0f) }
    LaunchedEffect(isHighlighted) {
        if (isHighlighted) {
            pulseAlpha.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulseAlpha.snapTo(0f)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .border(
                width = if (isHighlighted) 3.dp else if (isSelected) 1.dp else 0.dp,
                color = if (isHighlighted) Color(0xFFFFD700).copy(alpha = pulseAlpha.value) else if (isSelected) selectedItemColor else Color.Transparent,
                shape = RoundedCornerShape(4.dp),
            )
            .padding(1.dp)
            .background(color = itemBackgroundColor)
            .clickable(
                enabled = day.position == DayPosition.MonthDate,
                onClick = { onClick(day) },
            ),
    ) {
        val maxLines = 4

        // All items shown as lines
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 4.dp, start = 3.dp, end = 3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items.take(maxLines).forEach { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(
                            color = item.color,
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
            }
            if (items.size > maxLines) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(
                            color = Color.Gray.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(1.dp),
                        ),
                )
            }
        }

        val textColor = when (day.position) {
            DayPosition.MonthDate -> Color.Unspecified
            DayPosition.InDate, DayPosition.OutDate -> inActiveTextColor
        }
        Text(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 3.dp, end = 4.dp),
            text = day.date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 12.sp,
        )
    }
}
