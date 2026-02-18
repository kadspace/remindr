package com.remindr.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AccentRed = Color(0xFFDB504A)
private val DoneGray = Color(0xFF5A5A5A)

@Composable
fun RemindrLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    Canvas(modifier = modifier.size(width = size, height = size * 0.5f)) {
        val w = this.size.width
        val h = this.size.height
        val strokeWidth = h * 0.18f
        val padX = w * 0.1f

        // Top line — full width
        val topY = h * 0.35f
        drawLine(
            color = AccentRed,
            start = Offset(padX, topY),
            end = Offset(w - padX, topY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )

        // Bottom line — 70% width (stagger)
        val botY = h * 0.7f
        val botEnd = padX + (w - 2 * padX) * 0.7f
        drawLine(
            color = DoneGray,
            start = Offset(padX, botY),
            end = Offset(botEnd, botY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
fun RemindrWordmark(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    fontSize: TextUnit = 22.sp,
    textColor: Color = Color.White,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        RemindrLogo(size = iconSize)
        Spacer(Modifier.width(iconSize * 0.3f))
        Text(
            text = "remindr",
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.5).sp,
        )
    }
}
