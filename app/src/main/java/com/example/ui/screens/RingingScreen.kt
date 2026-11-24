package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.alarm.AlarmTimeCalculator
import com.example.ui.theme.IceBlueFixedDim
import com.example.ui.theme.NavyAccent
import com.example.ui.theme.NavyPrimary
import com.example.ui.theme.NavyPrimaryContainer
import com.example.ui.theme.OnNavyContainer
import com.example.ui.theme.tabularNums

@Composable
fun RingingScreen(
    hour: Int,
    minute: Int,
    label: String,
    snoozeMinutes: Int,
    is24Hour: Boolean,
    isPreview: Boolean,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit
) {
    val (timeText, period) = AlarmTimeCalculator.formatTimeParts(hour, minute, is24Hour)
    val transition = rememberInfiniteTransition(label = "ringing_pulse")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(NavyPrimary, NavyPrimaryContainer, NavyAccent)
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension * 0.58f
            for (i in 0..2) {
                val phase = (pulse + i / 3f) % 1f
                val radius = size.minDimension * 0.16f + (maxRadius * phase)
                val alpha = (1f - phase) * 0.22f
                drawCircle(
                    color = IceBlueFixedDim.copy(alpha = alpha),
                    radius = radius,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 520.dp)
                .padding(horizontal = 28.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.14f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = IceBlueFixedDim,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (isPreview) "PREVIEW ALERT" else "ALARM RINGING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color.White
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.displayLarge.tabularNums().copy(
                            fontSize = 68.sp,
                            lineHeight = 72.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                    if (period != null) {
                        Text(
                            text = period,
                            style = MaterialTheme.typography.headlineMedium,
                            color = IceBlueFixedDim,
                            modifier = Modifier.padding(start = 10.dp, bottom = 10.dp)
                        )
                    }
                }
                Text(
                    text = label.ifBlank { stringResource(R.string.alarm_ringing) },
                    style = MaterialTheme.typography.headlineSmall,
                    color = OnNavyContainer,
                    textAlign = TextAlign.Center
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (snoozeMinutes > 0) {
                    Button(
                        onClick = onSnooze,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.16f),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("snooze_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Snooze,
                            contentDescription = null,
                            tint = IceBlueFixedDim
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${stringResource(R.string.snooze)} • $snoozeMinutes min",
                            style = MaterialTheme.typography.titleMedium.tabularNums()
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = NavyPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .testTag("dismiss_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AlarmOff,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.dismiss),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}
