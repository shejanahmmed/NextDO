package com.shejan.nextdo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Color Palette
val MatteTerracotta = Color(0xFFdb7f67) // Primary Accent
val WarmOffWhite = Color(0xFFfffcf7)    // Card Background
val MutedClay = Color(0xFFf4e9dc)       // Icon Background
val CharcoalBrown = Color(0xFF4a443f)   // Text/Content

@Composable
fun WarmReminderCard(
    timeText: String,
    AmPmText: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(WarmOffWhite)
            .drawBehind {
                val stroke = Stroke(
                    width = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                )
                drawRoundRect(
                    color = MatteTerracotta, // Using Primary Accent for the border
                    style = stroke,
                    cornerRadius = CornerRadius(24.dp.toPx())
                )
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Large Circular Leading Icon Container
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MutedClay),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_alarm), // Assuming ic_alarm exists
                contentDescription = null,
                tint = MatteTerracotta,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Text Content
        Column {
            Text(
                text = timeText,
                color = CharcoalBrown,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = AmPmText,
                color = CharcoalBrown.copy(alpha = 0.6f), // Subtle subtitle
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
