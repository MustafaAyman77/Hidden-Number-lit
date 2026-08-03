package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ResultsScreen(
    viewModel: MainViewModel,
    isWinner: Boolean,
    winnerName: String,
    attemptsCount: Int,
    mySecret: String,
    opponentSecret: String,
    languageAr: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "trophy_bounce")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF1B0B3A), DarkBackground)
                )
            )
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Trophy / Result Icon
        Box(
            modifier = Modifier
                .scale(scale)
                .size(110.dp)
                .clip(CircleShape)
                .background(if (isWinner) NeonYellow.copy(0.2f) else NeonRed.copy(0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = "Result",
                tint = if (isWinner) NeonYellow else NeonRed,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = if (isWinner) (if (languageAr) "فوز أسطوري! 🏆🎯" else "Victory! 🏆🎯") else (if (languageAr) "مباراة حماسية! 💥" else "Defeat! 💥"),
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = if (isWinner) NeonYellow else NeonRed
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isWinner) (if (languageAr) "الفائز بالمباراة: $winnerName" else "Winner: $winnerName") else (if (languageAr) "تم استنتاج رقمك السري بواسطة $winnerName" else "Your secret was cracked by $winnerName"),
            fontSize = 15.sp,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Match Secrets & Stats Card
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowEffect = isWinner,
            borderColor = if (isWinner) NeonEmerald else NeonMagenta
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (languageAr) "الأرقام السرية والإحصائيات 🔓" else "Secret Numbers & Stats 🔓",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )

                // Revealed Secrets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkBackground.copy(0.5f))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (languageAr) "رقمك السري:" else "Your Secret:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = mySecret.ifEmpty { "----" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonEmerald
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(1.dp, 30.dp)
                            .background(GlassBorder)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (languageAr) "رقم الخصم السري:" else "Opponent Secret:",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Text(
                            text = opponentSecret.ifEmpty { "----" },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonMagenta
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatBox(
                        title = if (languageAr) "المحاولات" else "Attempts",
                        value = "$attemptsCount",
                        color = NeonCyan
                    )

                    StatBox(
                        title = if (languageAr) "نقاط XP" else "XP Earned",
                        value = if (isWinner) "+100 XP" else "+20 XP",
                        color = NeonYellow
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Action Buttons
        CyberButton(
            text = if (languageAr) "إعادة اللعب (Rematch) 🔄" else "Rematch 🔄",
            onClick = { viewModel.requestRematch() },
            primaryColor = NeonCyan
        )

        Spacer(modifier = Modifier.height(12.dp))

        CyberButton(
            text = if (languageAr) "الصفحة الرئيسية 🏠" else "Main Menu 🏠",
            onClick = { viewModel.navigateTo(AppScreen.HOME) },
            primaryColor = NeonMagenta
        )
    }
}

@Composable
fun StatBox(
    title: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Text(
            text = title,
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}
