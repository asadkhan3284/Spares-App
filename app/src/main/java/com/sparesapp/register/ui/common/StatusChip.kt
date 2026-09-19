package com.sparesapp.register.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparesapp.register.data.model.StockStatus
import com.sparesapp.register.ui.theme.Danger
import com.sparesapp.register.ui.theme.DangerBg
import com.sparesapp.register.ui.theme.Ok
import com.sparesapp.register.ui.theme.OkBg
import com.sparesapp.register.ui.theme.Warn
import com.sparesapp.register.ui.theme.WarnBg

fun statusColors(status: StockStatus): Pair<Color, Color> = when (status) {
    StockStatus.ZERO -> Danger to DangerBg
    StockStatus.BELOW_SAFETY_STOCK -> Warn to WarnBg
    StockStatus.BELOW_ROP -> Warn to WarnBg
    StockStatus.OK -> Ok to OkBg
}

@Composable
fun StatusChip(status: StockStatus, modifier: Modifier = Modifier) {
    val (fg, bg) = statusColors(status)
    Text(
        text = status.label,
        color = fg,
        fontSize = 11.5.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
        modifier = modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
