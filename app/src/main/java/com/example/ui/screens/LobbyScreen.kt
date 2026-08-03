package com.example.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiDifficulty
import com.example.data.model.GameMode
import com.example.data.model.GameType
import com.example.data.model.RoomPlayer
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberButton
import com.example.ui.components.GlassCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LobbyScreen(
    viewModel: MainViewModel,
    mode: GameMode,
    selectedType: GameType,
    selectedDifficulty: AiDifficulty,
    roomCode: String,
    players: List<RoomPlayer>,
    isHost: Boolean,
    mySecret: String,
    languageAr: Boolean
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val codeLength by viewModel.codeLength.collectAsStateWithLifecycle()
    val allowRepetition by viewModel.allowRepetition.collectAsStateWithLifecycle()
    var showHostSettingsPanel by remember { mutableStateOf(false) }

    val opponentPresent = players.size >= 2 || mode == GameMode.SINGLE_PLAYER

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF140D33), DarkBackground)
                )
            )
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.leaveRoom() }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Leave",
                    tint = TextPrimary
                )
            }
            Text(
                text = if (languageAr) mode.titleAr else mode.titleEn,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.weight(1f))
            
            // Leave Room Quick Button at top right
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(NeonRed.copy(0.2f))
                    .border(1.dp, NeonRed, RoundedCornerShape(8.dp))
                    .clickable { viewModel.leaveRoom() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Leave",
                        tint = NeonRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (languageAr) "مغادرة" else "Leave",
                        fontSize = 11.sp,
                        color = NeonRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 1. Room PIN & Host Settings Header Card
        if (mode != GameMode.SINGLE_PLAYER) {
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                glowEffect = true,
                borderColor = NeonCyan
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (languageAr) "رمز الغرفة السداسي (Room PIN):" else "6-Digit Room PIN:",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = roomCode,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            letterSpacing = 4.sp
                        )

                        // Copy PIN Button
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceGlass)
                                .border(1.dp, NeonCyan, CircleShape)
                                .clickable {
                                    clipboardManager.setText(AnnotatedString(roomCode))
                                    Toast.makeText(
                                        context,
                                        if (languageAr) "تم نسخ رقم الغرفة! 📋" else "Room code copied! 📋",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy PIN",
                                tint = NeonCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Settings Button (Visible ONLY to the room Host!)
                        if (isHost) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(if (showHostSettingsPanel) NeonEmerald.copy(0.3f) else DarkSurfaceGlass)
                                    .border(1.dp, if (showHostSettingsPanel) NeonEmerald else NeonMagenta, CircleShape)
                                    .clickable { showHostSettingsPanel = !showHostSettingsPanel },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Room Settings",
                                    tint = if (showHostSettingsPanel) NeonEmerald else NeonMagenta,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    if (isHost) {
                        Text(
                            text = if (languageAr) "💡 بصفتك صانع الغرفة، زر الإعدادات ⚙️ يتيح لك تحديد طول وقوانين الرقم" else "💡 As Room Host, the Settings button ⚙️ allows configuring rules",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Expandable Host Settings Panel (Opened by Settings Icon)
            AnimatedVisibility(
                visible = showHostSettingsPanel && isHost,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = NeonEmerald
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = NeonEmerald
                            )
                            Text(
                                text = if (languageAr) "إعدادات صانع الغرفة (Host Rules):" else "Host Room Settings:",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonEmerald
                            )
                        }

                        // Length Picker
                        Text(
                            text = if (languageAr) "طول الرقم السري ($codeLength أرقام):" else "Code Length ($codeLength Digits):",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (len in 3..6) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (codeLength == len) NeonCyan.copy(0.25f) else DarkSurfaceGlass)
                                        .border(1.5.dp, if (codeLength == len) NeonCyan else GlassBorder, RoundedCornerShape(10.dp))
                                        .clickable { viewModel.setCodeLength(len) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$len",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (codeLength == len) NeonCyan else TextPrimary
                                    )
                                }
                            }
                        }

                        // Repetition Option
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (languageAr) "تكرار الأرقام:" else "Allow Digit Repetition:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (allowRepetition) NeonEmerald.copy(0.25f) else NeonRed.copy(0.2f))
                                    .border(1.dp, if (allowRepetition) NeonEmerald else NeonRed, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.toggleAllowRepetition() }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (allowRepetition) {
                                        if (languageAr) "مسموح 🔄" else "Allowed 🔄"
                                    } else {
                                        if (languageAr) "ممنوع (فريد) 🚫" else "Unique Only 🚫"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allowRepetition) NeonEmerald else NeonRed
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Present Players & Characters Area
        Text(
            text = if (languageAr) "الشخصيات المتواجدة في الغرفة:" else "Room Players & Characters:",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            val hostPlayer = players.find { it.isHost }
            val guestPlayer = players.find { !it.isHost }

            PlayerCard(
                player = hostPlayer,
                roleTitle = if (languageAr) "صانع الغرفة (Host)" else "Room Host",
                defaultName = if (languageAr) "في الانتظار..." else "Waiting...",
                languageAr = languageAr,
                modifier = Modifier.weight(1f)
            )

            PlayerCard(
                player = guestPlayer,
                roleTitle = if (languageAr) "الخصم (Guest)" else "Opponent Guest",
                defaultName = if (languageAr) "في انتظار الخصم..." else "Waiting Guest...",
                languageAr = languageAr,
                modifier = Modifier.weight(1f)
            )
        }

        // 3. Status Action Button: "في انتظار الخصم..." or "الدخول إلى اللعب"
        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            glowEffect = opponentPresent,
            borderColor = if (opponentPresent) NeonEmerald else NeonMagenta
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!opponentPresent) {
                    // Waiting state for guest
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        CircularProgressIndicator(
                            color = NeonMagenta,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = if (languageAr) "في انتظار دخول الخصم إلى الغرفة..." else "Waiting for opponent to join...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonMagenta
                        )
                    }

                    Text(
                        text = if (languageAr) "شارك الرمز السداسي السريع ($roomCode) مع الخصم للانضمام الفوري!" else "Share code ($roomCode) with opponent to join fast!",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                } else {
                    // Guest joined! Show "Enter Game / Secret Setup"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Connected",
                            tint = NeonEmerald
                        )
                        Text(
                            text = if (languageAr) "الخصم متصل واكتملت الغرفة! 🎉" else "Opponent connected & room complete! 🎉",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonEmerald
                        )
                    }

                    CyberButton(
                        text = if (mySecret.isNotEmpty()) {
                            if (languageAr) "الدخول إلى اللعب مباشرة 🎮" else "Enter Gameplay Directly 🎮"
                        } else {
                            if (languageAr) "الدخول إلى اللعب (إعداد الرقم السري) 🔐" else "Enter Game (Set Secret Code) 🔐"
                        },
                        onClick = {
                            viewModel.navigateTo(AppScreen.SECRET_SETUP)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        primaryColor = NeonEmerald
                    )
                }
            }
        }

        // 4. Leave Room Button at the bottom
        CyberButton(
            text = if (languageAr) "مغادرة الغرفة 🚪" else "Leave Room 🚪",
            onClick = { viewModel.leaveRoom() },
            modifier = Modifier.fillMaxWidth(),
            primaryColor = NeonRed
        )
    }
}

private fun validateSecretInput(secret: String, length: Int, allowRep: Boolean, languageAr: Boolean): String? {
    if (secret.length != length) {
        return if (languageAr) "يجب إدخال $length أرقام بالضبط!" else "Must enter exactly $length digits!"
    }
    if (!allowRep && secret.toSet().size != length) {
        return if (languageAr) "غير مسموح بتكرار الأرقام حسب قواعد الغرفة!" else "Duplicate digits not allowed!"
    }
    return null
}

@Composable
fun PlayerCard(
    player: RoomPlayer?,
    roleTitle: String,
    defaultName: String,
    languageAr: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = roleTitle,
                fontSize = 11.sp,
                color = TextSecondary,
                fontWeight = FontWeight.SemiBold
            )

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (player != null) NeonCyan.copy(0.3f) else GlassBorder),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (player != null) "👤" else "❓",
                    fontSize = 20.sp
                )
            }

            Text(
                text = player?.name ?: defaultName,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )

            val statusText = when {
                player == null -> if (languageAr) "في الانتظار..." else "Waiting..."
                player.secretSet -> if (languageAr) "تم قفل الرقم 🔒" else "Secret Locked 🔒"
                player.isReady -> if (languageAr) "جاهز 🟢" else "Ready 🟢"
                else -> if (languageAr) "متصل ⚡" else "Connected ⚡"
            }

            Text(
                text = statusText,
                fontSize = 11.sp,
                color = if (player?.secretSet == true) NeonEmerald else if (player != null) NeonCyan else TextSecondary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TypeChip(
    type: GameType,
    isSelected: Boolean,
    languageAr: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonCyan.copy(0.25f) else DarkSurfaceGlass)
            .border(1.5.dp, if (isSelected) NeonCyan else GlassBorder, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (languageAr) type.titleAr else type.titleEn,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) NeonCyan else TextPrimary
        )
    }
}

@Composable
fun DifficultyChip(
    difficulty: AiDifficulty,
    isSelected: Boolean,
    languageAr: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color(difficulty.colorHex).copy(0.25f) else DarkSurfaceGlass)
            .border(1.dp, if (isSelected) Color(difficulty.colorHex) else GlassBorder, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = if (languageAr) difficulty.titleAr else difficulty.titleEn,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) Color(difficulty.colorHex) else TextSecondary
        )
    }
}

