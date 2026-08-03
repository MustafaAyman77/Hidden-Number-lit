package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.GameType
import com.example.data.model.GuessAttempt
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.CyberButton
import com.example.ui.components.CyberKeypad
import com.example.ui.components.GlassCard
import com.example.ui.components.VoiceChatBar
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceGlass
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonRed
import com.example.ui.theme.NeonYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GameplayScreen(
    viewModel: MainViewModel,
    isMyTurn: Boolean,
    turnTimerSeconds: Int,
    currentInput: String,
    myAttempts: List<GuessAttempt>,
    opponentAttempts: List<GuessAttempt>,
    mySecret: String,
    gameType: GameType,
    languageAr: Boolean,
    isMuted: Boolean,
    isSpeakerMuted: Boolean,
    audioLevel: Float,
    lastReactionEmoji: String?
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var selectedBoardTab by remember { mutableStateOf(0) } // 0 = My Board, 1 = Opponent Board

    var hasMicPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.voiceChatManager.startVoiceChat()
        }
    }

    val activeList = if (selectedBoardTab == 0) myAttempts else opponentAttempts

    LaunchedEffect(activeList.size) {
        if (activeList.isNotEmpty()) {
            listState.animateScrollToItem(activeList.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(DarkBackground, Color(0xFF0F0A2B), DarkBackground)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.HOME) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                // Turn Indicator Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isMyTurn) NeonEmerald.copy(0.2f) else NeonYellow.copy(0.2f))
                        .border(1.5.dp, if (isMyTurn) NeonEmerald else NeonYellow, RoundedCornerShape(20.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isMyTurn) (if (languageAr) "دورك الآن! 🎯" else "Your Turn! 🎯") else (if (languageAr) "انتظر المنافس... ⏳" else "Waiting... ⏳"),
                        color = if (isMyTurn) NeonEmerald else NeonYellow,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Countdown Timer
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Timer",
                        tint = if (turnTimerSeconds <= 5) NeonRed else NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${turnTimerSeconds}s",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = if (turnTimerSeconds <= 5) NeonRed else NeonCyan
                    )
                }
            }

            // Voice Chat Bar
            VoiceChatBar(
                isMuted = isMuted,
                isSpeakerMuted = isSpeakerMuted,
                audioLevel = audioLevel,
                hasMicPermission = hasMicPermission,
                onToggleMute = { viewModel.voiceChatManager.toggleMute() },
                onToggleSpeaker = { viewModel.voiceChatManager.toggleSpeaker() },
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                onSendEmoji = { emoji -> viewModel.sendReactionEmoji(emoji) },
                lastReactionEmoji = lastReactionEmoji,
                languageAr = languageAr
            )

            // Secret Number Display Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = "Secret", tint = NeonMagenta, modifier = Modifier.size(16.dp))
                    Text(
                        text = if (languageAr) "رقمك السري المخفي: $mySecret" else "Your Secret: $mySecret",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                }
            }

            // Dual Boards Tab Switcher (لوحة تخميناتي / لوحة الخصم)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceGlass)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedBoardTab == 0) NeonCyan.copy(0.25f) else Color.Transparent)
                        .clickable { selectedBoardTab = 0 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (languageAr) "لوحة تخميناتي (${myAttempts.size}) 🎯" else "My Board (${myAttempts.size}) 🎯",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBoardTab == 0) NeonCyan else TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedBoardTab == 1) NeonMagenta.copy(0.25f) else Color.Transparent)
                        .clickable { selectedBoardTab = 1 }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (languageAr) "لوحة الخصم (${opponentAttempts.size}) 👁️" else "Opponent Board (${opponentAttempts.size}) 👁️",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBoardTab == 1) NeonMagenta else TextPrimary
                    )
                }
            }

            // Attempts Log List for Selected Board
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (activeList.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (selectedBoardTab == 0) {
                                if (languageAr) "أدخل تخمينك الأول ضد رقم الخصم! 🚀" else "Enter your first guess! 🚀"
                            } else {
                                if (languageAr) "لم يقم الخصم بأي تخمين بعد ⏳" else "No guesses from opponent yet ⏳"
                            },
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(activeList) { attempt ->
                            AttemptLogCard(attempt = attempt, languageAr = languageAr)
                        }
                    }
                }
            }

            // Input Display Box & Submit Button
            GlassCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = if (isMyTurn) NeonCyan else GlassBorder
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = if (currentInput.isEmpty()) (if (languageAr) "اكتب تخمينك هنا..." else "Type guess...") else currentInput,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = if (currentInput.isNotEmpty()) NeonCyan else TextSecondary,
                                letterSpacing = 2.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        CyberButton(
                            text = if (languageAr) "تخمين 🚀" else "Guess 🚀",
                            onClick = { viewModel.submitGuess() },
                            enabled = isMyTurn && currentInput.isNotEmpty(),
                            modifier = Modifier.width(105.dp),
                            primaryColor = NeonCyan
                        )
                    }

                    // Numeric Keypad
                    CyberKeypad(
                        onDigitClick = { viewModel.onKeypadDigit(it) },
                        onBackspaceClick = { viewModel.onKeypadBackspace() },
                        onSubmitClick = { viewModel.submitGuess() },
                        enabled = isMyTurn
                    )
                }
            }
        }

        // Reaction Overlay
        AnimatedVisibility(
            visible = lastReactionEmoji != null,
            enter = fadeIn() + slideInVertically { -100 },
            exit = fadeOut() + slideOutVertically { -100 },
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceGlass)
                    .border(2.dp, NeonMagenta, CircleShape)
                    .padding(16.dp)
            ) {
                Text(text = lastReactionEmoji ?: "", fontSize = 48.sp)
            }
        }
    }
}

@Composable
fun AttemptLogCard(
    attempt: GuessAttempt,
    languageAr: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (attempt.isWin) NeonEmerald else GlassBorder
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(NeonCyan.copy(0.2f))
                        .border(1.dp, NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${attempt.attemptNumber}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = NeonCyan
                    )
                }

                Column {
                    Text(
                        text = attempt.guessedNumber,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = attempt.playerName,
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (attempt.isWin) NeonEmerald.copy(0.2f) else DarkSurfaceGlass)
                    .border(1.dp, if (attempt.isWin) NeonEmerald else NeonMagenta.copy(0.5f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (languageAr) attempt.clueTextAr else attempt.clueTextEn,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (attempt.isWin) NeonEmerald else TextPrimary
                )
            }
        }
    }
}
