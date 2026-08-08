package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    val myListState = rememberLazyListState()
    val opponentListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var selectedBoardTab by remember { mutableStateOf(0) } // 0 = Dual Side-By-Side, 1 = My Board, 2 = Opponent Board

    val shakeOffsetX = remember { Animatable(0f) }
    var activeFeedbackAttempt by remember { mutableStateOf<GuessAttempt?>(null) }
    var previousMyAttemptsSize by remember { mutableStateOf(myAttempts.size) }
    var previousOpponentAttemptsSize by remember { mutableStateOf(opponentAttempts.size) }

    // Trigger visual/audio feedback and shake animation on new guess
    LaunchedEffect(myAttempts.size) {
        if (myAttempts.size > previousMyAttemptsSize && myAttempts.isNotEmpty()) {
            val latest = myAttempts.last()
            activeFeedbackAttempt = latest

            if (latest.isWin) {
                viewModel.soundManager.playWin()
            } else {
                viewModel.soundManager.playLoss()
                coroutineScope.launch {
                    repeat(3) {
                        shakeOffsetX.animateTo(18f, animationSpec = tween(40))
                        shakeOffsetX.animateTo(-18f, animationSpec = tween(40))
                    }
                    shakeOffsetX.animateTo(0f, animationSpec = tween(40))
                }
            }

            coroutineScope.launch {
                delay(2500)
                if (activeFeedbackAttempt == latest) {
                    activeFeedbackAttempt = null
                }
            }
        }
        previousMyAttemptsSize = myAttempts.size
    }

    LaunchedEffect(opponentAttempts.size) {
        if (opponentAttempts.size > previousOpponentAttemptsSize && opponentAttempts.isNotEmpty()) {
            val latest = opponentAttempts.last()
            activeFeedbackAttempt = latest
            coroutineScope.launch {
                delay(2500)
                if (activeFeedbackAttempt == latest) {
                    activeFeedbackAttempt = null
                }
            }
        }
        previousOpponentAttemptsSize = opponentAttempts.size
    }

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

    LaunchedEffect(myAttempts.size) {
        if (myAttempts.isNotEmpty()) {
            myListState.animateScrollToItem(myAttempts.size - 1)
        }
    }

    LaunchedEffect(opponentAttempts.size) {
        if (opponentAttempts.isNotEmpty()) {
            opponentListState.animateScrollToItem(opponentAttempts.size - 1)
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
                .offset(x = shakeOffsetX.value.dp)
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
                    val minutes = turnTimerSeconds / 60
                    val seconds = turnTimerSeconds % 60
                    val timerDisplay = if (turnTimerSeconds >= 60) "${minutes}:${seconds.toString().padStart(2, '0')}" else "${turnTimerSeconds}s"

                    Text(
                        text = timerDisplay,
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

            // Dual Boards Tab Switcher (عرض مزدوج / تخميناتي / الخصم)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceGlass)
                    .border(1.dp, GlassBorder, RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedBoardTab == 0) NeonEmerald.copy(0.25f) else Color.Transparent)
                        .clickable { selectedBoardTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (languageAr) "👥 جنباً إلى جنب" else "👥 Side-by-Side",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBoardTab == 0) NeonEmerald else TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedBoardTab == 1) NeonCyan.copy(0.25f) else Color.Transparent)
                        .clickable { selectedBoardTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (languageAr) "🎯 تخميناتي (${myAttempts.size})" else "My Board (${myAttempts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBoardTab == 1) NeonCyan else TextPrimary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedBoardTab == 2) NeonMagenta.copy(0.25f) else Color.Transparent)
                        .clickable { selectedBoardTab = 2 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (languageAr) "👁️ الخصم (${opponentAttempts.size})" else "Opponent (${opponentAttempts.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedBoardTab == 2) NeonMagenta else TextPrimary
                    )
                }
            }

            // Attempts Log List / Dual Side-By-Side View
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (selectedBoardTab == 0) {
                    // Side-By-Side View
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Left Column: My Board
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, NeonCyan.copy(0.4f), RoundedCornerShape(12.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = if (languageAr) "تخميناتي (${myAttempts.size}) 🎯" else "My Board (${myAttempts.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonCyan,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (myAttempts.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (languageAr) "اكتب تخمينك 🚀" else "Type guess 🚀",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = myListState,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(myAttempts) { attempt ->
                                        AttemptLogCard(attempt = attempt, languageAr = languageAr, compact = true)
                                    }
                                }
                            }
                        }

                        // Right Column: Opponent Board
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceGlass)
                                .border(1.dp, NeonMagenta.copy(0.4f), RoundedCornerShape(12.dp))
                                .padding(6.dp)
                        ) {
                            Text(
                                text = if (languageAr) "تخمينات الخصم (${opponentAttempts.size}) 👁️" else "Opponent (${opponentAttempts.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeonMagenta,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            if (opponentAttempts.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (languageAr) "في انتظار الخصم ⏳" else "Waiting... ⏳",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                LazyColumn(
                                    state = opponentListState,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    items(opponentAttempts) { attempt ->
                                        AttemptLogCard(attempt = attempt, languageAr = languageAr, compact = true)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    val activeList = if (selectedBoardTab == 1) myAttempts else opponentAttempts
                    val activeState = if (selectedBoardTab == 1) myListState else opponentListState

                    if (activeList.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (selectedBoardTab == 1) {
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
                            state = activeState,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(activeList) { attempt ->
                                AttemptLogCard(attempt = attempt, languageAr = languageAr, compact = false)
                            }
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

        // Animated Banner Overlay on Guess Result (Correct or Wrong)
        AnimatedVisibility(
            visible = activeFeedbackAttempt != null,
            enter = fadeIn() + scaleIn(animationSpec = spring(dampingRatio = 0.6f)) + slideInVertically { -100 },
            exit = fadeOut() + scaleOut() + slideOutVertically { -100 },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 70.dp, start = 16.dp, end = 16.dp)
        ) {
            activeFeedbackAttempt?.let { attempt ->
                val isWin = attempt.isWin
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isWin) {
                                    listOf(Color(0xFF003820), Color(0xFF006B38), Color(0xFF003820))
                                } else {
                                    listOf(Color(0xFF4A0018), Color(0xFF8B0028), Color(0xFF4A0018))
                                }
                            )
                        )
                        .border(
                            width = 2.dp,
                            color = if (isWin) NeonEmerald else NeonRed,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(if (isWin) NeonEmerald.copy(0.25f) else NeonRed.copy(0.25f))
                                .border(2.dp, if (isWin) NeonEmerald else NeonRed, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isWin) "🎉" else "❌",
                                fontSize = 28.sp
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isWin) {
                                    if (languageAr) "إجابة صحيحة أسطورية! 🏆" else "LEGENDARY CORRECT GUESS! 🏆"
                                } else {
                                    if (languageAr) "تخمين خاطئ! ⚡" else "INCORRECT GUESS! ⚡"
                                },
                                color = if (isWin) NeonEmerald else NeonRed,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (languageAr) {
                                    "الرقم [${attempt.guessedNumber}]: ${attempt.clueTextAr}"
                                } else {
                                    "Code [${attempt.guessedNumber}]: ${attempt.clueTextEn}"
                                },
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
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
    languageAr: Boolean,
    compact: Boolean = false
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
                horizontalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(if (compact) 26.dp else 36.dp)
                        .clip(CircleShape)
                        .background(if (attempt.isWin) NeonEmerald.copy(0.2f) else NeonCyan.copy(0.2f))
                        .border(1.dp, if (attempt.isWin) NeonEmerald else NeonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${attempt.attemptNumber}",
                        fontSize = if (compact) 10.sp else 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (attempt.isWin) NeonEmerald else NeonCyan
                    )
                }

                Column {
                    Text(
                        text = attempt.guessedNumber,
                        fontSize = if (compact) 15.sp else 20.sp,
                        fontWeight = FontWeight.Black,
                        color = if (attempt.isWin) NeonEmerald else TextPrimary,
                        letterSpacing = if (compact) 1.sp else 2.sp
                    )
                    if (!compact) {
                        Text(
                            text = attempt.playerName,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (attempt.isWin) NeonEmerald.copy(0.2f) else DarkSurfaceGlass)
                    .border(1.dp, if (attempt.isWin) NeonEmerald else NeonMagenta.copy(0.5f), RoundedCornerShape(8.dp))
                    .padding(horizontal = if (compact) 6.dp else 10.dp, vertical = if (compact) 4.dp else 6.dp)
            ) {
                Text(
                    text = if (languageAr) attempt.clueTextAr else attempt.clueTextEn,
                    fontSize = if (compact) 10.sp else 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (attempt.isWin) NeonEmerald else TextPrimary
                )
            }
        }
    }
}

