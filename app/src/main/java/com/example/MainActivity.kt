package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.CreateJoinRoomScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SecretSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val currentScreen by viewModel.currentScreen.collectAsState()
            val languageAr by viewModel.languageAr.collectAsState()
            val profile by viewModel.playerProfile.collectAsState()
            val selectedMode by viewModel.selectedMode.collectAsState()
            val selectedType by viewModel.selectedType.collectAsState()
            val selectedDifficulty by viewModel.selectedDifficulty.collectAsState()
            val roomCode by viewModel.roomCode.collectAsState()
            val roomPlayers by viewModel.roomPlayers.collectAsState()
            val isHost by viewModel.isHost.collectAsState()
            val mySecret by viewModel.mySecretNumber.collectAsState()
            val opponentSecret by viewModel.opponentSecretNumber.collectAsState()
            val isMyTurn by viewModel.isMyTurn.collectAsState()
            val timerSeconds by viewModel.turnTimerSeconds.collectAsState()
            val currentInput by viewModel.currentInput.collectAsState()
            val myAttempts by viewModel.myAttemptsLog.collectAsState()
            val opponentAttempts by viewModel.opponentAttemptsLog.collectAsState()
            val isWinner by viewModel.isWinner.collectAsState()
            val winnerName by viewModel.winnerName.collectAsState()
            val isMuted by viewModel.voiceChatManager.isMuted.collectAsState()
            val isSpeakerMuted by viewModel.voiceChatManager.isSpeakerMuted.collectAsState()
            val audioLevel by viewModel.voiceChatManager.audioLevel.collectAsState()
            val lastEmoji by viewModel.lastReactionEmoji.collectAsState()
            val appError by viewModel.appError.collectAsState()
            val updateState by viewModel.updateState.collectAsState()

            val layoutDirection = if (languageAr) LayoutDirection.Rtl else LayoutDirection.Ltr

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (appError != null) {
                                com.example.ui.components.ErrorDialog(
                                    error = appError!!,
                                    languageAr = languageAr,
                                    onDismiss = { viewModel.clearAppError() }
                                )
                            }

                            com.example.ui.components.UpdateDialog(
                                updateState = updateState,
                                languageAr = languageAr,
                                onUpdateClick = {
                                    (updateState as? com.example.update.UpdateUIState.Available)?.let {
                                        viewModel.downloadAndInstallUpdate(it.manifest)
                                    }
                                },
                                onInstallClick = { filePath ->
                                    viewModel.installApk(filePath)
                                },
                                onDismissClick = {
                                    (updateState as? com.example.update.UpdateUIState.Available)?.let {
                                        viewModel.skipUpdate(it.manifest.versionCode.toLong())
                                    } ?: viewModel.dismissUpdateUi()
                                }
                            )
                            when (currentScreen) {
                                AppScreen.HOME -> {
                                    HomeScreen(
                                        viewModel = viewModel,
                                        profile = profile,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.CREATE_JOIN -> {
                                    CreateJoinRoomScreen(
                                        viewModel = viewModel,
                                        mode = selectedMode,
                                        roomCode = roomCode,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.LOBBY -> {
                                    LobbyScreen(
                                        viewModel = viewModel,
                                        mode = selectedMode,
                                        selectedType = selectedType,
                                        selectedDifficulty = selectedDifficulty,
                                        roomCode = roomCode,
                                        players = roomPlayers,
                                        isHost = isHost,
                                        mySecret = mySecret,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.SECRET_SETUP -> {
                                    val codeLength by viewModel.codeLength.collectAsState()
                                    val allowRepetition by viewModel.allowRepetition.collectAsState()
                                    SecretSetupScreen(
                                        viewModel = viewModel,
                                        codeLength = codeLength,
                                        allowRepetition = allowRepetition,
                                        mySecret = mySecret,
                                        mode = selectedMode,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.GAMEPLAY -> {
                                    GameplayScreen(
                                        viewModel = viewModel,
                                        isMyTurn = isMyTurn,
                                        turnTimerSeconds = timerSeconds,
                                        currentInput = currentInput,
                                        myAttempts = myAttempts,
                                        opponentAttempts = opponentAttempts,
                                        mySecret = mySecret,
                                        gameType = selectedType,
                                        languageAr = languageAr,
                                        isMuted = isMuted,
                                        isSpeakerMuted = isSpeakerMuted,
                                        audioLevel = audioLevel,
                                        lastReactionEmoji = lastEmoji
                                    )
                                }
                                AppScreen.RESULTS -> {
                                    ResultsScreen(
                                        viewModel = viewModel,
                                        isWinner = isWinner,
                                        winnerName = winnerName,
                                        attemptsCount = myAttempts.size,
                                        mySecret = mySecret,
                                        opponentSecret = opponentSecret,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.SETTINGS -> {
                                    SettingsScreen(
                                        viewModel = viewModel,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.HISTORY -> {
                                    HistoryScreen(
                                        viewModel = viewModel,
                                        languageAr = languageAr
                                    )
                                }
                                AppScreen.PROFILE -> {
                                    com.example.ui.screens.ProfileRegistrationScreen(
                                        viewModel = viewModel,
                                        profile = profile,
                                        languageAr = languageAr
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
