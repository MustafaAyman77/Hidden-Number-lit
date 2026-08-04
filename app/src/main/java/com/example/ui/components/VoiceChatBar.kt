package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed

@Composable
fun VoiceChatBar(
    isMuted: Boolean,
    isSpeakerMuted: Boolean,
    audioLevel: Float,
    hasMicPermission: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onRequestPermission: () -> Unit,
    onSendEmoji: (String) -> Unit,
    lastReactionEmoji: String?,
    languageAr: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Voice Chat controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!hasMicPermission) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonRed.copy(alpha = 0.2f))
                            .border(1.dp, NeonRed, RoundedCornerShape(12.dp))
                            .clickable { onRequestPermission() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (languageAr) "إذن المايك🎙️" else "Mic Access🎙️",
                            color = NeonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    // Mic button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isMuted) NeonRed.copy(0.2f) else NeonEmerald.copy(0.2f))
                            .border(1.dp, if (isMuted) NeonRed else NeonEmerald, CircleShape)
                            .clickable { onToggleMute() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mic",
                            tint = if (isMuted) NeonRed else NeonEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Speaker button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (isSpeakerMuted) Color.Gray.copy(0.2f) else NeonCyan.copy(0.2f))
                            .border(1.dp, if (isSpeakerMuted) Color.Gray else NeonCyan, CircleShape)
                            .clickable { onToggleSpeaker() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isSpeakerMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Speaker",
                            tint = if (isSpeakerMuted) Color.Gray else NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Audio level wave animation
                    Row(
                        modifier = Modifier
                            .height(24.dp)
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val barsCount = 5
                        for (i in 0 until barsCount) {
                            val factor = (i + 1) * 0.2f
                            val targetHeight = if (isMuted) 4f else (audioLevel * 20f * factor).coerceIn(4f, 24f)
                            val animatedH by animateFloatAsState(
                                targetValue = targetHeight,
                                animationSpec = tween(100),
                                label = "wave"
                            )

                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(animatedH.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(if (isMuted) Color.Gray else NeonEmerald)
                            )
                        }
                    }
                }
            }

            // Quick Emoji Reaction launcher
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val emojis = listOf("🔥", "🎯", "⚡", "🤣", "🤔")
                for (e in emojis) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceGlass)
                            .clickable { onSendEmoji(e) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = e, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
