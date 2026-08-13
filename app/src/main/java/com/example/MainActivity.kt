package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.RegisterScreen
import com.example.ui.screens.CreateJoinRoomScreen
import com.example.ui.screens.GameplayScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LobbyScreen
import com.example.ui.screens.ProfileRegistrationScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SecretSetupScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.theme.MyApplicationTheme

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            var showSplash by remember { mutableStateOf(true) }
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

            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        (updateState as? com.example.update.UpdateUIState.ReadyToInstall)?.let { readyState ->
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                if (packageManager.canRequestPackageInstalls()) {
                                    viewModel.installApk(readyState.apkFilePath)
                                }
                            } else {
                                viewModel.installApk(readyState.apkFilePath)
                            }
                        }
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose {
                    lifecycleOwner.lifecycle.removeObserver(observer)
                }
            }

            val context = LocalContext.current
            var lastBackPressTime by remember { mutableStateOf(0L) }

            BackHandler(enabled = !showSplash) {
                val handled = viewModel.handleBackPress()
                if (!handled) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastBackPressTime < 2000) {
                        (context as? android.app.Activity)?.finish()
                    } else {
                        lastBackPressTime = currentTime
                        val msg = if (languageAr) "اضغط رجوع مرة أخرى للخروج" else "Press back again to exit"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                MyApplicationTheme {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            if (showSplash) {
                                SplashScreen(
                                    languageAr = languageAr,
                                    onSplashFinished = { showSplash = false }
                                )
                            } else {
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
                                        ProfileRegistrationScreen(
                                            viewModel = viewModel,
                                            profile = profile,
                                            languageAr = languageAr
                                        )
                                    }
                                    AppScreen.LOGIN -> {
                                        LoginScreen(
                                            viewModel = viewModel,
                                            languageAr = languageAr
                                        )
                                    }
                                    AppScreen.REGISTER -> {
                                        RegisterScreen(
                                            viewModel = viewModel,
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
}
