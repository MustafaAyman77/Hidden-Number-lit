package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.audio.SoundManager
import com.example.audio.VoiceChatManager
import com.example.data.local.AppDatabase
import com.example.data.local.MatchRecord
import com.example.data.model.AiDifficulty
import com.example.data.model.AppError
import com.example.data.model.GameMode
import com.example.data.model.GameType
import com.example.data.model.GuessAttempt
import com.example.data.model.NetworkMessage
import com.example.data.model.PlayerProfile
import com.example.data.model.RoomPlayer
import com.example.domain.AiBotEngine
import com.example.network.LocalWifiNetworkManager
import com.example.network.OnlineNetworkManager
import com.example.update.AppUpdateManager
import com.example.update.UpdateManifest
import com.example.update.UpdateUIState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.random.Random

enum class AppScreen {
    HOME, CREATE_JOIN, LOBBY, SECRET_SETUP, GAMEPLAY, RESULTS, SETTINGS, HISTORY, PROFILE, LOGIN, REGISTER
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java,
        "hidden_number_db"
    ).build()

    val matchHistory = db.matchRecordDao().getAllMatches()

    val soundManager = SoundManager(application)
    val voiceChatManager = VoiceChatManager(application, viewModelScope)
    val onlineNetworkManager = OnlineNetworkManager(viewModelScope)
    val localWifiNetworkManager = LocalWifiNetworkManager(application, viewModelScope)
    val appUpdateManager = AppUpdateManager(application, viewModelScope)
    val updateState = appUpdateManager.updateState
    private val aiBotEngine = AiBotEngine()

    // Supabase Auth & Profile Services
    private val supabaseAuthService = com.example.data.supabase.SupabaseAuthService()
    private val supabaseProfileService = com.example.data.supabase.SupabaseProfileService()
    private val supabaseStorageService = com.example.data.supabase.SupabaseStorageService()
    private val secureTokenManager = com.example.data.supabase.SecureTokenManager(application)
    private val localAuthManager = com.example.data.supabase.LocalAuthManager(application)

    // Auth State Flows
    private val _authLoading = MutableStateFlow(false)
    val authLoading: StateFlow<Boolean> = _authLoading.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _currentSession = MutableStateFlow<com.example.data.supabase.AuthSession?>(null)
    val currentSession: StateFlow<com.example.data.supabase.AuthSession?> = _currentSession.asStateFlow()

    fun clearAuthError() {
        _authError.value = null
    }

    // Player Profile Persistence
    private val prefs = application.getSharedPreferences("user_profile_prefs", android.content.Context.MODE_PRIVATE)

    // App Error State
    private val _appError = MutableStateFlow<AppError?>(null)
    val appError: StateFlow<AppError?> = _appError.asStateFlow()

    fun clearAppError() {
        _appError.value = null
    }

    fun setError(error: AppError) {
        _appError.value = error
    }

    // App Navigation State
    private val _currentScreen = MutableStateFlow(AppScreen.LOGIN)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Settings
    private val _languageAr = MutableStateFlow(true)
    val languageAr: StateFlow<Boolean> = _languageAr.asStateFlow()

    // Player Profile
    private val _playerProfile = MutableStateFlow(loadSavedProfile())
    val playerProfile: StateFlow<PlayerProfile> = _playerProfile.asStateFlow()

    init {
        checkSavedSession()
    }

    fun isSupabaseConfigured(): Boolean {
        return com.example.data.supabase.SupabaseConfig.isConfigured()
    }

    private fun checkSavedSession() {
        val savedSession = secureTokenManager.getSession()
        val isGuestChoice = prefs.getBoolean("is_guest_mode", false)
        val isRegistered = prefs.getBoolean("isProfileRegistered", false)
        val localProfile = loadSavedProfile()

        if (savedSession != null) {
            viewModelScope.launch {
                _authLoading.value = true
                try {
                    val profile = supabaseProfileService.getProfile(savedSession.userId, savedSession.accessToken)
                    if (profile != null) {
                        val finalProfile = profile.copy(
                            avatarCustomUri = profile.avatarCustomUri ?: localProfile.avatarCustomUri
                        )
                        _playerProfile.value = finalProfile
                        saveProfileToPrefs(finalProfile)
                        _currentSession.value = savedSession
                        _currentScreen.value = AppScreen.HOME
                    } else if (savedSession.refreshToken.isNotEmpty()) {
                        val refreshResult = supabaseAuthService.refreshToken(savedSession.refreshToken)
                        if (refreshResult is com.example.data.supabase.AuthResult.Success) {
                            val newSession = refreshResult.data
                            saveSessionToPrefs(newSession)
                            _currentSession.value = newSession
                            val fetched = supabaseProfileService.getProfile(newSession.userId, newSession.accessToken)
                            val finalProfile = (fetched ?: localProfile).copy(
                                avatarCustomUri = fetched?.avatarCustomUri ?: localProfile.avatarCustomUri
                            )
                            _playerProfile.value = finalProfile
                            saveProfileToPrefs(finalProfile)
                            _currentScreen.value = AppScreen.HOME
                        } else if (refreshResult is com.example.data.supabase.AuthResult.Error && (refreshResult.messageAr.contains("401") || refreshResult.messageEn.contains("401"))) {
                            clearSessionFromPrefs()
                            _currentScreen.value = AppScreen.LOGIN
                        } else {
                            // Offline fallback: keep saved session and go to HOME
                            _currentSession.value = savedSession
                            _playerProfile.value = localProfile
                            _currentScreen.value = AppScreen.HOME
                        }
                    } else {
                        // Offline fallback: keep saved session and go to HOME
                        _currentSession.value = savedSession
                        _playerProfile.value = localProfile
                        _currentScreen.value = AppScreen.HOME
                    }
                } catch (e: Exception) {
                    // Offline fallback on network exception
                    _currentSession.value = savedSession
                    _playerProfile.value = localProfile
                    _currentScreen.value = AppScreen.HOME
                } finally {
                    _authLoading.value = false
                }
            }
        } else if (isGuestChoice || isRegistered) {
            _playerProfile.value = localProfile
            _currentScreen.value = AppScreen.HOME
        } else {
            _currentScreen.value = AppScreen.LOGIN
        }
    }

    private fun saveSessionToPrefs(session: com.example.data.supabase.AuthSession) {
        secureTokenManager.saveSession(session)
        prefs.edit().putBoolean("is_guest_mode", false).apply()
    }

    private fun clearSessionFromPrefs() {
        secureTokenManager.clearSession()
        prefs.edit().remove("is_guest_mode").apply()
    }

    private fun loadSavedProfile(): PlayerProfile {
        val savedId = prefs.getString("playerId", "player_${System.currentTimeMillis() % 10000}") ?: "player_123"
        val savedUsername = prefs.getString("username", "اللاعب الأسطوري") ?: "اللاعب الأسطوري"
        val savedDisplayName = prefs.getString("displayName", savedUsername) ?: savedUsername
        val savedUserAvatarId = if (savedId.isNotEmpty()) prefs.getInt("user_avatar_id_$savedId", -1) else -1
        val savedAvatarId = if (savedUserAvatarId != -1) savedUserAvatarId else prefs.getInt("avatarId", 1)

        val savedUserCustomUri = if (savedId.isNotEmpty()) prefs.getString("user_avatar_uri_$savedId", null) else null
        val savedCustomUri = savedUserCustomUri ?: prefs.getString("avatarCustomUri", null)

        val savedLevel = prefs.getInt("level", 1)
        val savedXp = prefs.getInt("xp", 0)
        val savedCoins = prefs.getInt("coins", 0)
        val savedWins = prefs.getInt("wins", 0)
        val savedLosses = prefs.getInt("losses", 0)
        val savedDraws = prefs.getInt("draws", 0)
        val savedTotal = prefs.getInt("totalGames", 0)
        val savedEmail = prefs.getString("email", null)
        val isGuest = prefs.getBoolean("is_guest_mode", true)

        return PlayerProfile(
            id = savedId,
            username = savedUsername,
            displayName = savedDisplayName,
            avatarId = savedAvatarId,
            avatarCustomUri = savedCustomUri,
            level = savedLevel,
            xp = savedXp,
            coins = savedCoins,
            wins = savedWins,
            losses = savedLosses,
            draws = savedDraws,
            totalGames = savedTotal,
            email = savedEmail,
            isGuest = isGuest
        )
    }

    private fun saveProfileToPrefs(profile: PlayerProfile) {
        val editor = prefs.edit()
            .putString("playerId", profile.id)
            .putString("username", profile.username)
            .putString("displayName", profile.displayName)
            .putInt("avatarId", profile.avatarId)
            .putString("avatarCustomUri", profile.avatarCustomUri)
            .putInt("level", profile.level)
            .putInt("xp", profile.xp)
            .putInt("coins", profile.coins)
            .putInt("wins", profile.wins)
            .putInt("losses", profile.losses)
            .putInt("draws", profile.draws)
            .putInt("totalGames", profile.totalGames)
            .putString("email", profile.email)
            .putBoolean("is_guest_mode", profile.isGuest)
            .putBoolean("isProfileRegistered", true)

        if (!profile.isGuest && profile.id.isNotEmpty()) {
            editor.putString("user_avatar_uri_${profile.id}", profile.avatarCustomUri)
            editor.putInt("user_avatar_id_${profile.id}", profile.avatarId)
            editor.putString("user_display_name_${profile.id}", profile.displayName)
            localAuthManager.updateProfile(
                userId = profile.id,
                displayName = profile.displayName,
                avatarId = profile.avatarId,
                avatarCustomUri = profile.avatarCustomUri
            )
        }

        editor.apply()
    }

    // Selected Game Config
    private val _selectedMode = MutableStateFlow(GameMode.SINGLE_PLAYER)
    val selectedMode: StateFlow<GameMode> = _selectedMode.asStateFlow()

    private val _selectedType = MutableStateFlow(GameType.CODE_SECRET)
    val selectedType: StateFlow<GameType> = _selectedType.asStateFlow()

    private val _codeLength = MutableStateFlow(4)
    val codeLength: StateFlow<Int> = _codeLength.asStateFlow()

    private val _allowRepetition = MutableStateFlow(false)
    val allowRepetition: StateFlow<Boolean> = _allowRepetition.asStateFlow()

    private val _selectedDifficulty = MutableStateFlow(AiDifficulty.MEDIUM)
    val selectedDifficulty: StateFlow<AiDifficulty> = _selectedDifficulty.asStateFlow()

    // Room State (6-Digit Numeric PIN)
    private val _roomCode = MutableStateFlow("849201")
    val roomCode: StateFlow<String> = _roomCode.asStateFlow()

    private val _roomPlayers = MutableStateFlow<List<RoomPlayer>>(emptyList())
    val roomPlayers: StateFlow<List<RoomPlayer>> = _roomPlayers.asStateFlow()

    private val _isHost = MutableStateFlow(true)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    // Gameplay State
    private val _mySecretNumber = MutableStateFlow("")
    val mySecretNumber: StateFlow<String> = _mySecretNumber.asStateFlow()

    private val _opponentSecretNumber = MutableStateFlow("")
    val opponentSecretNumber: StateFlow<String> = _opponentSecretNumber.asStateFlow()

    // Dual Board Attempt Logs
    private val _myAttemptsLog = MutableStateFlow<List<GuessAttempt>>(emptyList())
    val myAttemptsLog: StateFlow<List<GuessAttempt>> = _myAttemptsLog.asStateFlow()

    private val _opponentAttemptsLog = MutableStateFlow<List<GuessAttempt>>(emptyList())
    val opponentAttemptsLog: StateFlow<List<GuessAttempt>> = _opponentAttemptsLog.asStateFlow()

    private val _isMyTurn = MutableStateFlow(true)
    val isMyTurn: StateFlow<Boolean> = _isMyTurn.asStateFlow()

    private val _turnTimerSeconds = MutableStateFlow(60)
    val turnTimerSeconds: StateFlow<Int> = _turnTimerSeconds.asStateFlow()

    private val _currentInput = MutableStateFlow("")
    val currentInput: StateFlow<String> = _currentInput.asStateFlow()

    private val _isWinner = MutableStateFlow(false)
    val isWinner: StateFlow<Boolean> = _isWinner.asStateFlow()

    private val _winnerName = MutableStateFlow("")
    val winnerName: StateFlow<String> = _winnerName.asStateFlow()

    private val _lastReactionEmoji = MutableStateFlow<String?>(null)
    val lastReactionEmoji: StateFlow<String?> = _lastReactionEmoji.asStateFlow()

    private var timerJob: Job? = null

    init {
        setupVoiceChatCallback()
        observeNetworkMessages()
        startLobbySyncHeartbeat()
        appUpdateManager.checkForUpdates(manualTrigger = false)
    }

    fun checkUpdatesManually() {
        appUpdateManager.checkForUpdates(manualTrigger = true)
    }

    fun downloadAndInstallUpdate(manifest: UpdateManifest) {
        appUpdateManager.downloadAndInstallApk(manifest)
    }

    fun installApk(filePath: String) {
        appUpdateManager.installApk(filePath)
    }

    fun skipUpdate(versionCode: Long) {
        appUpdateManager.skipVersion(versionCode)
    }

    fun dismissUpdateUi() {
        appUpdateManager.dismissUpdateUi()
    }

    private fun startLobbySyncHeartbeat() {
        viewModelScope.launch {
            while (true) {
                delay(3000)
                if (_selectedMode.value != GameMode.SINGLE_PLAYER && _roomCode.value.isNotEmpty()) {
                    val currentScr = _currentScreen.value
                    if (currentScr == AppScreen.LOBBY || currentScr == AppScreen.SECRET_SETUP) {
                        val me = _playerProfile.value
                        if (_isHost.value) {
                            broadcastRoomState()
                        } else {
                            val payload = createJoinPayload()
                            if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                                onlineNetworkManager.sendMessage("JOIN", me.id, me.username, payload)
                            } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                                localWifiNetworkManager.sendMessage("JOIN", me.id, me.username, payload)
                            }
                        }

                        // Re-send SECRET_SET if secret was set
                        if (_mySecretNumber.value.isNotEmpty()) {
                            if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                                onlineNetworkManager.sendMessage("SECRET_SET", me.id, me.username, "true")
                            } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                                localWifiNetworkManager.sendMessage("SECRET_SET", me.id, me.username, "true")
                            }
                        }

                        checkIfBothSecretsSetAndStart()
                    } else if (currentScr == AppScreen.RESULTS) {
                        val me = _playerProfile.value
                        if (_mySecretNumber.value.isNotEmpty()) {
                            if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                                onlineNetworkManager.sendMessage("REVEAL_SECRET", me.id, me.username, _mySecretNumber.value)
                            } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                                localWifiNetworkManager.sendMessage("REVEAL_SECRET", me.id, me.username, _mySecretNumber.value)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupVoiceChatCallback() {
        voiceChatManager.onAudioPacketReady = { base64Audio ->
            val profile = _playerProfile.value
            when (_selectedMode.value) {
                GameMode.ONLINE_ROOM -> {
                    onlineNetworkManager.sendMessage("VOICE", profile.id, profile.username, base64Audio)
                }
                GameMode.LOCAL_WIFI -> {
                    localWifiNetworkManager.sendMessage("VOICE", profile.id, profile.username, base64Audio)
                }
                GameMode.SINGLE_PLAYER -> {}
            }
        }
    }

    private fun observeNetworkMessages() {
        viewModelScope.launch {
            onlineNetworkManager.incomingMessages.collectLatest { handleNetworkMessage(it) }
        }
        viewModelScope.launch {
            localWifiNetworkManager.incomingMessages.collectLatest { handleNetworkMessage(it) }
        }
        viewModelScope.launch {
            onlineNetworkManager.errorEvents.collect { err ->
                if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                    _appError.value = err
                }
            }
        }
        viewModelScope.launch {
            localWifiNetworkManager.errorEvents.collect { err ->
                if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                    _appError.value = err
                }
            }
        }
    }

    private fun createJoinPayload(): String {
        val p = _playerProfile.value
        return JSONObject().apply {
            put("avatarId", p.avatarId)
            put("customUri", p.avatarCustomUri ?: "")
            put("level", p.level)
        }.toString()
    }

    private fun handleNetworkMessage(msg: NetworkMessage) {
        if (msg.senderId == _playerProfile.value.id) return // ignore self echo

        when (msg.type) {
            "JOIN" -> {
                var avId = 2
                var custUri: String? = null
                var lvl = 1
                try {
                    val raw = msg.payload.trim()
                    if (raw.startsWith("{")) {
                        val json = JSONObject(raw)
                        avId = json.optInt("avatarId", 2)
                        custUri = json.optString("customUri", "").ifEmpty { null }
                        lvl = json.optInt("level", 1)
                    } else {
                        avId = raw.toIntOrNull() ?: 2
                    }
                } catch (e: Exception) {
                    avId = msg.payload.toIntOrNull() ?: 2
                }

                val guestPlayer = RoomPlayer(
                    id = msg.senderId,
                    name = msg.senderName,
                    avatarId = avId,
                    avatarCustomUri = custUri,
                    level = lvl,
                    isHost = false
                )
                _roomPlayers.value = _roomPlayers.value.filter { it.id != guestPlayer.id } + guestPlayer
                soundManager.playClick()
                // Host acknowledges join by replying ROOM_STATE
                if (_isHost.value) {
                    broadcastRoomState()
                }
            }

            "ROOM_STATE" -> {
                try {
                    val json = JSONObject(msg.payload)
                    val modeStr = json.optString("mode", GameMode.ONLINE_ROOM.name)
                    val typeStr = json.optString("type", GameType.CODE_SECRET.name)
                    _selectedType.value = GameType.valueOf(typeStr)
                    _codeLength.value = json.optInt("codeLength", 4)
                    _allowRepetition.value = json.optBoolean("allowRepetition", false)

                    val playersArr = json.getJSONArray("players")
                    val list = mutableListOf<RoomPlayer>()
                    for (i in 0 until playersArr.length()) {
                        val pObj = playersArr.getJSONObject(i)
                        list.add(
                            RoomPlayer(
                                id = pObj.getString("id"),
                                name = pObj.getString("name"),
                                avatarId = pObj.optInt("avatarId", 1),
                                avatarCustomUri = pObj.optString("customUri", "").ifEmpty { null },
                                level = pObj.optInt("level", 1),
                                isHost = pObj.getBoolean("isHost"),
                                isReady = pObj.getBoolean("isReady"),
                                secretSet = pObj.getBoolean("secretSet")
                            )
                        )
                    }
                    _roomPlayers.value = list
                    checkIfBothSecretsSetAndStart()
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error parsing ROOM_STATE: ${e.message}")
                }
            }

            "READY" -> {
                _roomPlayers.value = _roomPlayers.value.map {
                    if (it.id == msg.senderId) it.copy(isReady = msg.payload.toBoolean()) else it
                }
                soundManager.playClick()
            }

            "SECRET_SET" -> {
                _roomPlayers.value = _roomPlayers.value.map {
                    if (it.id == msg.senderId) it.copy(secretSet = true) else it
                }
                soundManager.playClick()
                if (_isHost.value) {
                    broadcastRoomState()
                }
                checkIfBothSecretsSetAndStart()
            }

            "REVEAL_SECRET" -> {
                if (msg.payload.isNotEmpty()) {
                    _opponentSecretNumber.value = msg.payload
                }
            }

            "GUESS" -> {
                val guessedVal = msg.payload
                val attempt = aiBotEngine.evaluateGuess(
                    guessedStr = guessedVal,
                    targetSecretStr = _mySecretNumber.value,
                    gameType = _selectedType.value,
                    attemptNum = _opponentAttemptsLog.value.size + 1,
                    playerName = msg.senderName
                )

                // Opponent guessed against MY secret -> add to opponentAttemptsLog
                _opponentAttemptsLog.value = _opponentAttemptsLog.value + attempt

                val replyPayload = JSONObject().apply {
                    put("guessed", guessedVal)
                    put("exact", attempt.exactMatches)
                    put("isWin", attempt.isWin)
                }.toString()

                val me = _playerProfile.value
                if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                    onlineNetworkManager.sendMessage("GUESS_REPLY", me.id, me.username, replyPayload)
                } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                    localWifiNetworkManager.sendMessage("GUESS_REPLY", me.id, me.username, replyPayload)
                }

                if (attempt.isWin) {
                    soundManager.playLoss()
                    onGameEnded(winner = msg.senderName, isMeWin = false)
                } else {
                    soundManager.playGuess()
                    _isMyTurn.value = true
                    startTurnTimer()
                }
            }

            "GUESS_REPLY" -> {
                try {
                    val json = JSONObject(msg.payload)
                    val guessedVal = json.getString("guessed")
                    val exact = json.getInt("exact")
                    val isWin = json.getBoolean("isWin")

                    val len = _codeLength.value
                    val attempt = GuessAttempt(
                        attemptNumber = _myAttemptsLog.value.size + 1,
                        guessedNumber = guessedVal,
                        exactMatches = exact,
                        totalLength = len,
                        clueTextAr = if (isWin) "تم كسر الرقم السري بنجاح! 🔓🎯" else "$exact / $len أرقام بمكانها الصحيح 🎯",
                        clueTextEn = if (isWin) "Secret Cracked! 🔓🎯" else "$exact / $len exact matches 🎯",
                        isWin = isWin,
                        playerName = _playerProfile.value.username
                    )

                    _myAttemptsLog.value = _myAttemptsLog.value + attempt

                    if (isWin) {
                        _opponentSecretNumber.value = guessedVal
                        soundManager.playWin()
                        onGameEnded(winner = _playerProfile.value.username, isMeWin = true)
                    } else {
                        _isMyTurn.value = false
                        startTurnTimer()
                    }
                } catch (e: Exception) {
                    Log.e("MainViewModel", "Error handling GUESS_REPLY: ${e.message}")
                }
            }

            "VOICE" -> {
                voiceChatManager.playAudioChunk(msg.payload)
            }

            "CHAT" -> {
                _lastReactionEmoji.value = msg.payload
                viewModelScope.launch {
                    delay(3000)
                    _lastReactionEmoji.value = null
                }
            }

            "REMATCH" -> {
                _currentScreen.value = AppScreen.LOBBY
                resetLobbyState()
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        soundManager.playClick()
        _currentScreen.value = screen
    }

    fun toggleLanguage() {
        _languageAr.value = !_languageAr.value
        soundManager.playClick()
    }

    fun loginWithSupabase(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authError.value = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
            return
        }
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null

            var session: com.example.data.supabase.AuthSession? = null

            if (isSupabaseConfigured()) {
                val res = supabaseAuthService.login(email, pass)
                if (res is com.example.data.supabase.AuthResult.Success) {
                    session = res.data
                } else if (res is com.example.data.supabase.AuthResult.Error) {
                    _authError.value = res.messageAr
                    _authLoading.value = false
                    return@launch
                }
            } else {
                // Local authentication fallback
                val localSession = localAuthManager.login(email, pass)
                if (localSession != null) {
                    session = localSession
                } else {
                    _authError.value = "البريد الإلكتروني أو كلمة المرور غير صحيحة، أو الحساب غير مسجل."
                    _authLoading.value = false
                    return@launch
                }
            }

            if (session == null) {
                _authError.value = "تعذر تسجيل الدخول. يرجى التأكد من بيانات الحساب."
                _authLoading.value = false
                return@launch
            }

            _currentSession.value = session
            saveSessionToPrefs(session)

            val profile = if (isSupabaseConfigured()) {
                supabaseProfileService.getProfile(session.userId, session.accessToken)
            } else null

            val userSavedAvatarUri = prefs.getString("user_avatar_uri_${session.userId}", null)
            val userSavedAvatarId = prefs.getInt("user_avatar_id_${session.userId}", -1)

            val baseProfile = profile ?: localAuthManager.getProfile(session.userId) ?: PlayerProfile(
                id = session.userId,
                username = session.username.ifEmpty { email.substringBefore("@") },
                displayName = session.displayName.ifEmpty { session.username.ifEmpty { email.substringBefore("@") } },
                email = email,
                isGuest = false
            )

            val finalProfile = baseProfile.copy(
                avatarId = if (userSavedAvatarId != -1) userSavedAvatarId else baseProfile.avatarId,
                avatarCustomUri = baseProfile.avatarCustomUri ?: userSavedAvatarUri
            )

            _playerProfile.value = finalProfile
            saveProfileToPrefs(finalProfile)
            soundManager.playWin()
            _currentScreen.value = AppScreen.HOME
            _authLoading.value = false
        }
    }

    fun registerWithSupabase(email: String, password: String, username: String, displayName: String) {
        viewModelScope.launch {
            _authLoading.value = true
            _authError.value = null

            var session: com.example.data.supabase.AuthSession? = null

            if (isSupabaseConfigured()) {
                val res = supabaseAuthService.signUp(email, password, username, displayName)
                if (res is com.example.data.supabase.AuthResult.Success && res.data.accessToken.isNotEmpty()) {
                    session = res.data
                } else if (res is com.example.data.supabase.AuthResult.Error) {
                    _authError.value = res.messageAr
                    _authLoading.value = false
                    return@launch
                }
            } else {
                // Local account creation fallback
                if (localAuthManager.isEmailRegistered(email)) {
                    _authError.value = "هذا البريد الإلكتروني مسجل بالفعل محلياً."
                    _authLoading.value = false
                    return@launch
                }
                session = localAuthManager.registerAccount(email, password, username, displayName)
            }

            if (session == null) {
                _authError.value = "تعذر إنشاء الحساب."
                _authLoading.value = false
                return@launch
            }

            _currentSession.value = session
            saveSessionToPrefs(session)

            val finalProfile = PlayerProfile(
                id = session.userId,
                username = username.ifEmpty { email.substringBefore("@") },
                displayName = displayName.ifEmpty { username.ifEmpty { email.substringBefore("@") } },
                email = email,
                isGuest = false
            )

            _playerProfile.value = finalProfile
            saveProfileToPrefs(finalProfile)
            soundManager.playWin()
            _currentScreen.value = AppScreen.HOME
            _authLoading.value = false
        }
    }

    suspend fun isUsernameTaken(username: String): Boolean {
        if (!isSupabaseConfigured()) return false
        return try {
            supabaseProfileService.isUsernameTaken(username)
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendPasswordReset(email: String): com.example.data.supabase.AuthResult<Unit> {
        if (!isSupabaseConfigured()) {
            return com.example.data.supabase.AuthResult.Success(Unit)
        }
        return supabaseAuthService.sendPasswordReset(email)
    }

    fun continueAsGuest() {
        prefs.edit().putBoolean("is_guest_mode", true).apply()
        val guestProfile = _playerProfile.value.copy(isGuest = true)
        _playerProfile.value = guestProfile
        saveProfileToPrefs(guestProfile)
        _currentScreen.value = AppScreen.HOME
    }

    fun logoutSupabase() {
        val session = _currentSession.value
        if (session != null) {
            viewModelScope.launch {
                supabaseAuthService.logout(session.accessToken)
            }
        }
        clearSessionFromPrefs()
        _currentSession.value = null
        val guest = PlayerProfile(isGuest = true)
        _playerProfile.value = guest
        
        prefs.edit()
            .putString("playerId", guest.id)
            .putString("username", guest.username)
            .putString("displayName", guest.displayName)
            .putInt("avatarId", guest.avatarId)
            .remove("avatarCustomUri")
            .putBoolean("is_guest_mode", true)
            .apply()

        _currentScreen.value = AppScreen.LOGIN
    }

    fun updateProfile(newUsername: String, newAvatarId: Int) {
        val updated = _playerProfile.value.copy(
            username = newUsername.ifEmpty { "اللاعب الأسطوري" },
            displayName = newUsername.ifEmpty { "اللاعب الأسطوري" },
            avatarId = newAvatarId
        )
        _playerProfile.value = updated
        saveProfileToPrefs(updated)
        syncProfileToRoomPlayers(updated)

        val session = _currentSession.value
        if (session != null && !updated.isGuest) {
            viewModelScope.launch {
                supabaseProfileService.updateDisplayMetadata(
                    userId = session.userId,
                    accessToken = session.accessToken,
                    displayName = updated.displayName,
                    avatar = newAvatarId.toString()
                )
            }
        }
        soundManager.playClick()
    }

    fun updateProfileFull(newUsername: String, newAvatarId: Int, newCustomUri: String?) {
        val updated = _playerProfile.value.copy(
            displayName = newUsername.ifEmpty { _playerProfile.value.displayName },
            avatarId = newAvatarId,
            avatarCustomUri = newCustomUri
        )
        _playerProfile.value = updated
        saveProfileToPrefs(updated)
        syncProfileToRoomPlayers(updated)

        val session = _currentSession.value
        if (session != null && !updated.isGuest) {
            viewModelScope.launch {
                var finalAvatarStr = newCustomUri ?: newAvatarId.toString()

                if (!newCustomUri.isNullOrEmpty() && (newCustomUri.startsWith("content://") || newCustomUri.startsWith("file://"))) {
                    try {
                        val uploadResult = supabaseStorageService.uploadAvatar(
                            context = getApplication(),
                            uri = android.net.Uri.parse(newCustomUri),
                            userId = session.userId,
                            accessToken = session.accessToken
                        )
                        if (uploadResult.isSuccess) {
                            finalAvatarStr = uploadResult.getOrNull() ?: finalAvatarStr
                            // Update local avatar URL to public Supabase URL
                            val syncedProfile = _playerProfile.value.copy(avatarCustomUri = finalAvatarStr)
                            _playerProfile.value = syncedProfile
                            saveProfileToPrefs(syncedProfile)
                        }
                    } catch (e: Exception) {
                        Log.w("MainViewModel", "Avatar upload exception: ${e.message}")
                    }
                }

                supabaseProfileService.updateDisplayMetadata(
                    userId = session.userId,
                    accessToken = session.accessToken,
                    displayName = updated.displayName,
                    avatar = finalAvatarStr
                )
            }
        }
        soundManager.playClick()
    }

    private fun syncProfileToRoomPlayers(updated: PlayerProfile) {
        if (_roomPlayers.value.isNotEmpty()) {
            _roomPlayers.value = _roomPlayers.value.map {
                if (it.id == updated.id) {
                    it.copy(
                        name = updated.username,
                        avatarId = updated.avatarId,
                        avatarCustomUri = updated.avatarCustomUri,
                        level = updated.level
                    )
                } else it
            }
            if (_isHost.value) {
                broadcastRoomState()
            } else {
                val payload = createJoinPayload()
                if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                    onlineNetworkManager.sendMessage("JOIN", updated.id, updated.username, payload)
                } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                    localWifiNetworkManager.sendMessage("JOIN", updated.id, updated.username, payload)
                }
            }
        }
    }

    fun selectGameMode(mode: GameMode) {
        _selectedMode.value = mode
        soundManager.playClick()
    }

    fun selectGameType(type: GameType) {
        _selectedType.value = type
        soundManager.playClick()
    }

    fun selectDifficulty(difficulty: AiDifficulty) {
        _selectedDifficulty.value = difficulty
        soundManager.playClick()
    }

    fun leaveRoom() {
        soundManager.playClick()
        onlineNetworkManager.disconnect()
        localWifiNetworkManager.disconnect()
        _roomPlayers.value = emptyList()
        _roomCode.value = ""
        _mySecretNumber.value = ""
        _opponentSecretNumber.value = ""
        _currentScreen.value = AppScreen.HOME
    }

    // --- LOBBY & ROOM ACTIONS ---

    fun getDetectedHostIp(): String {
        return localWifiNetworkManager.getGatewayOrHostIpAddress()
    }

    fun getMyDeviceIp(): String {
        return localWifiNetworkManager.getLocalIpAddress()
    }

    fun createRoom() {
        _isHost.value = true
        if (_selectedMode.value == GameMode.LOCAL_WIFI) {
            val devIp = localWifiNetworkManager.getLocalIpAddress()
            _roomCode.value = if (devIp.isNotEmpty()) devIp else "192.168.43.1"
        } else {
            _roomCode.value = generateRoomCode()
        }
        val me = RoomPlayer(
            id = _playerProfile.value.id,
            name = _playerProfile.value.username,
            avatarId = _playerProfile.value.avatarId,
            avatarCustomUri = _playerProfile.value.avatarCustomUri,
            level = _playerProfile.value.level,
            isHost = true,
            isReady = true
        )
        _roomPlayers.value = listOf(me)
        resetLobbyState()

        when (_selectedMode.value) {
            GameMode.ONLINE_ROOM -> {
                onlineNetworkManager.connectToRoom(_roomCode.value, me.id)
            }
            GameMode.LOCAL_WIFI -> {
                localWifiNetworkManager.startHosting()
            }
            GameMode.SINGLE_PLAYER -> {
                val aiBot = RoomPlayer(
                    id = "ai_bot",
                    name = "البوت الذكي (${_selectedDifficulty.value.titleAr})",
                    avatarId = 1,
                    avatarCustomUri = null,
                    level = when (_selectedDifficulty.value) {
                        AiDifficulty.EASY -> 10
                        AiDifficulty.MEDIUM -> 30
                        AiDifficulty.HARD -> 60
                        AiDifficulty.IMPOSSIBLE -> 99
                    },
                    isHost = false,
                    isReady = true,
                    secretSet = true
                )
                _roomPlayers.value = listOf(me, aiBot)
            }
        }

        _currentScreen.value = AppScreen.LOBBY
        soundManager.playClick()
    }

    fun joinRoom(codeToJoin: String) {
        val trimmedCode = codeToJoin.trim()
        if (trimmedCode.isEmpty()) {
            setError(AppError.CustomError("يرجى إدخال رمز الغرفة أو عنوان IP الخاص بالمضيف.", "Please enter a valid room PIN or host IP address."))
            return
        }
        _isHost.value = false
        _roomCode.value = trimmedCode.uppercase()
        val me = RoomPlayer(
            id = _playerProfile.value.id,
            name = _playerProfile.value.username,
            avatarId = _playerProfile.value.avatarId,
            avatarCustomUri = _playerProfile.value.avatarCustomUri,
            level = _playerProfile.value.level,
            isHost = false,
            isReady = false
        )
        _roomPlayers.value = listOf(me)
        resetLobbyState()

        val joinPayloadStr = createJoinPayload()

        when (_selectedMode.value) {
            GameMode.ONLINE_ROOM -> {
                onlineNetworkManager.connectToRoom(_roomCode.value, me.id)
                viewModelScope.launch {
                    // Send rapid burst of JOIN packets to guarantee fast delivery
                    for (del in listOf(0L, 200L, 500L, 1000L)) {
                        delay(del)
                        onlineNetworkManager.sendMessage("JOIN", me.id, me.name, joinPayloadStr)
                    }
                }
            }
            GameMode.LOCAL_WIFI -> {
                localWifiNetworkManager.connectToHost(_roomCode.value)
                viewModelScope.launch {
                    for (del in listOf(0L, 200L, 500L)) {
                        delay(del)
                        localWifiNetworkManager.sendMessage("JOIN", me.id, me.name, joinPayloadStr)
                    }
                }
            }
            GameMode.SINGLE_PLAYER -> {}
        }

        _currentScreen.value = AppScreen.LOBBY
        soundManager.playClick()
    }

    fun togglePlayerReady() {
        val meId = _playerProfile.value.id
        _roomPlayers.value = _roomPlayers.value.map {
            if (it.id == meId) it.copy(isReady = !it.isReady) else it
        }
        val isReadyNow = _roomPlayers.value.find { it.id == meId }?.isReady ?: false

        if (_selectedMode.value == GameMode.ONLINE_ROOM) {
            onlineNetworkManager.sendMessage("READY", meId, _playerProfile.value.username, isReadyNow.toString())
        } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
            localWifiNetworkManager.sendMessage("READY", meId, _playerProfile.value.username, isReadyNow.toString())
        }

        if (_isHost.value) broadcastRoomState()
        soundManager.playClick()
    }

    fun setCodeLength(len: Int) {
        _codeLength.value = len.coerceIn(3, 6)
        soundManager.playClick()
        if (_isHost.value) broadcastRoomState()
    }

    fun toggleAllowRepetition() {
        _allowRepetition.value = !_allowRepetition.value
        soundManager.playClick()
        if (_isHost.value) broadcastRoomState()
    }

    fun setMySecretNumberAndStart(secret: String) {
        if (secret.isEmpty()) {
            setError(AppError.InvalidSecretInput())
            return
        }
        if (_selectedType.value == GameType.CODE_SECRET) {
            if (secret.length != _codeLength.value) {
                setError(AppError.InvalidSecretInput(
                    messageAr = "الرقم السري يجب أن يكون مكوناً من ${_codeLength.value} أرقام.",
                    messageEn = "Secret code must be exactly ${_codeLength.value} digits."
                ))
                return
            }
            if (!_allowRepetition.value && secret.toSet().size != secret.length) {
                setError(AppError.InvalidSecretInput(
                    messageAr = "تكرار الأرقام غير مسموح به في إعدادات الجولة الحالية.",
                    messageEn = "Repeated digits are disabled for this round."
                ))
                return
            }
        } else if (_selectedType.value == GameType.RANGE_1_100) {
            val num = secret.toIntOrNull()
            if (num == null || num !in 1..100) {
                setError(AppError.InvalidSecretInput(
                    messageAr = "يرجى اختيار رقم صحيح بين 1 و 100.",
                    messageEn = "Please choose a valid number between 1 and 100."
                ))
                return
            }
        }

        _mySecretNumber.value = secret
        aiBotEngine.resetAiState(_selectedType.value, _codeLength.value, _allowRepetition.value)

        val meId = _playerProfile.value.id
        _roomPlayers.value = _roomPlayers.value.map {
            if (it.id == meId) it.copy(secretSet = true) else it
        }

        soundManager.playClick()

        when (_selectedMode.value) {
            GameMode.SINGLE_PLAYER -> {
                // Generate AI Bot secret number based on current code length & repetition settings
                _opponentSecretNumber.value = aiBotEngine.generateSecretNumber(_selectedType.value, _codeLength.value, _allowRepetition.value)
                startGameplaySession()
            }
            GameMode.ONLINE_ROOM -> {
                viewModelScope.launch {
                    for (del in listOf(0L, 200L, 500L)) {
                        delay(del)
                        onlineNetworkManager.sendMessage("SECRET_SET", meId, _playerProfile.value.username, "true")
                    }
                }
                broadcastRoomState()
                checkIfBothSecretsSetAndStart()
            }
            GameMode.LOCAL_WIFI -> {
                viewModelScope.launch {
                    for (del in listOf(0L, 200L, 500L)) {
                        delay(del)
                        localWifiNetworkManager.sendMessage("SECRET_SET", meId, _playerProfile.value.username, "true")
                    }
                }
                broadcastRoomState()
                checkIfBothSecretsSetAndStart()
            }
        }
    }

    fun resendSecretSetState() {
        if (_mySecretNumber.value.isEmpty()) return
        val meId = _playerProfile.value.id
        when (_selectedMode.value) {
            GameMode.ONLINE_ROOM -> {
                onlineNetworkManager.sendMessage("SECRET_SET", meId, _playerProfile.value.username, "true")
                if (_isHost.value) broadcastRoomState()
            }
            GameMode.LOCAL_WIFI -> {
                localWifiNetworkManager.sendMessage("SECRET_SET", meId, _playerProfile.value.username, "true")
                if (_isHost.value) broadcastRoomState()
            }
            else -> {}
        }
        checkIfBothSecretsSetAndStart()
    }

    fun autoDiscoverAndJoinLocalRoom(onResult: (Boolean, String) -> Unit) {
        localWifiNetworkManager.discoverAndConnect { success, ip ->
            if (success && ip.isNotEmpty()) {
                joinRoom(ip)
            }
            onResult(success, ip)
        }
    }

    private fun checkIfBothSecretsSetAndStart() {
        if (_selectedMode.value != GameMode.SINGLE_PLAYER && _roomPlayers.value.size >= 2 && _roomPlayers.value.all { it.secretSet }) {
            if (_currentScreen.value != AppScreen.GAMEPLAY && _currentScreen.value != AppScreen.RESULTS) {
                startGameplaySession()
            }
        }
    }

    private fun broadcastRoomState() {
        val payloadObj = JSONObject().apply {
            put("mode", _selectedMode.value.name)
            put("type", _selectedType.value.name)
            put("codeLength", _codeLength.value)
            put("allowRepetition", _allowRepetition.value)
            val arr = org.json.JSONArray()
            for (p in _roomPlayers.value) {
                val pObj = JSONObject().apply {
                    put("id", p.id)
                    put("name", p.name)
                    put("avatarId", p.avatarId)
                    put("customUri", p.avatarCustomUri ?: "")
                    put("level", p.level)
                    put("isHost", p.isHost)
                    put("isReady", p.isReady)
                    put("secretSet", p.secretSet)
                }
                arr.put(pObj)
            }
            put("players", arr)
        }

        if (_selectedMode.value == GameMode.ONLINE_ROOM) {
            onlineNetworkManager.sendMessage("ROOM_STATE", _playerProfile.value.id, _playerProfile.value.username, payloadObj.toString())
        } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
            localWifiNetworkManager.sendMessage("ROOM_STATE", _playerProfile.value.id, _playerProfile.value.username, payloadObj.toString())
        }
    }

    private fun startGameplaySession() {
        _myAttemptsLog.value = emptyList()
        _opponentAttemptsLog.value = emptyList()
        _isMyTurn.value = true
        _currentInput.value = ""
        _currentScreen.value = AppScreen.GAMEPLAY
        startTurnTimer()
    }

    private fun resetLobbyState() {
        _mySecretNumber.value = ""
        _opponentSecretNumber.value = ""
        _myAttemptsLog.value = emptyList()
        _opponentAttemptsLog.value = emptyList()
        _currentInput.value = ""
    }

    // --- GAMEPLAY INPUT ACTIONS ---

    fun onKeypadDigit(digit: String) {
        val maxLen = if (_selectedType.value == GameType.CODE_SECRET) _codeLength.value else 3
        if (_currentInput.value.length < maxLen) {
            _currentInput.value += digit
            soundManager.playClick()
        }
    }

    fun onKeypadBackspace() {
        if (_currentInput.value.isNotEmpty()) {
            _currentInput.value = _currentInput.value.dropLast(1)
            soundManager.playClick()
        }
    }

    fun onKeypadClear() {
        _currentInput.value = ""
        soundManager.playClick()
    }

    fun submitGuess() {
        val guessVal = _currentInput.value.trim()
        if (guessVal.isEmpty() || !_isMyTurn.value) return

        soundManager.playGuess()
        _currentInput.value = ""

        when (_selectedMode.value) {
            GameMode.SINGLE_PLAYER -> {
                val attempt = aiBotEngine.evaluateGuess(
                    guessedStr = guessVal,
                    targetSecretStr = _opponentSecretNumber.value,
                    gameType = _selectedType.value,
                    attemptNum = _myAttemptsLog.value.size + 1,
                    playerName = _playerProfile.value.username
                )

                _myAttemptsLog.value = _myAttemptsLog.value + attempt

                if (attempt.isWin) {
                    onGameEnded(winner = _playerProfile.value.username, isMeWin = true)
                } else {
                    // AI Bot turn
                    _isMyTurn.value = false
                    viewModelScope.launch {
                        delay(1200)
                        val aiGuess = aiBotEngine.makeAiTurnGuess(
                            difficulty = _selectedDifficulty.value,
                            gameType = _selectedType.value,
                            codeLength = _codeLength.value,
                            allowRepetition = _allowRepetition.value,
                            previousAttempts = _opponentAttemptsLog.value
                        )

                        val aiAttempt = aiBotEngine.evaluateGuess(
                            guessedStr = aiGuess,
                            targetSecretStr = _mySecretNumber.value,
                            gameType = _selectedType.value,
                            attemptNum = _opponentAttemptsLog.value.size + 1,
                            playerName = "البوت الذكي🤖"
                        )

                        _opponentAttemptsLog.value = _opponentAttemptsLog.value + aiAttempt

                        if (aiAttempt.isWin) {
                            onGameEnded(winner = "البوت الذكي🤖", isMeWin = false)
                        } else {
                            _isMyTurn.value = true
                            startTurnTimer()
                        }
                    }
                }
            }

            GameMode.ONLINE_ROOM -> {
                _isMyTurn.value = false
                val me = _playerProfile.value
                onlineNetworkManager.sendMessage("GUESS", me.id, me.username, guessVal)
                startTurnTimer()
            }

            GameMode.LOCAL_WIFI -> {
                _isMyTurn.value = false
                val me = _playerProfile.value
                localWifiNetworkManager.sendMessage("GUESS", me.id, me.username, guessVal)
                startTurnTimer()
            }
        }
    }

    fun sendReactionEmoji(emoji: String) {
        val me = _playerProfile.value
        _lastReactionEmoji.value = emoji
        soundManager.playClick()

        when (_selectedMode.value) {
            GameMode.ONLINE_ROOM -> onlineNetworkManager.sendMessage("CHAT", me.id, me.username, emoji)
            GameMode.LOCAL_WIFI -> localWifiNetworkManager.sendMessage("CHAT", me.id, me.username, emoji)
            GameMode.SINGLE_PLAYER -> {}
        }

        viewModelScope.launch {
            delay(3000)
            _lastReactionEmoji.value = null
        }
    }

    private fun startTurnTimer() {
        timerJob?.cancel()
        _turnTimerSeconds.value = 60
        timerJob = viewModelScope.launch {
            while (_turnTimerSeconds.value > 0) {
                delay(1000)
                _turnTimerSeconds.value -= 1
                if (_turnTimerSeconds.value <= 5 && _turnTimerSeconds.value > 0) {
                    soundManager.playTimerTick()
                }
            }
            // Time out - pass turn or default guess
            if (_isMyTurn.value) {
                val autoVal = if (_selectedType.value == GameType.CODE_SECRET) "1".repeat(_codeLength.value) else "50"
                _currentInput.value = autoVal
                submitGuess()
            } else if (_selectedMode.value != GameMode.SINGLE_PLAYER) {
                // Opponent turn timed out -> pass turn back to me
                _isMyTurn.value = true
                startTurnTimer()
            }
        }
    }

    private fun onGameEnded(winner: String, isMeWin: Boolean) {
        timerJob?.cancel()
        _isWinner.value = isMeWin
        _winnerName.value = winner

        val me = _playerProfile.value
        if (_mySecretNumber.value.isNotEmpty()) {
            if (_selectedMode.value == GameMode.ONLINE_ROOM) {
                onlineNetworkManager.sendMessage("REVEAL_SECRET", me.id, me.username, _mySecretNumber.value)
            } else if (_selectedMode.value == GameMode.LOCAL_WIFI) {
                localWifiNetworkManager.sendMessage("REVEAL_SECRET", me.id, me.username, _mySecretNumber.value)
            }
        }

        if (isMeWin) {
            soundManager.playWin()
            val newWins = _playerProfile.value.wins + 1
            val newXp = _playerProfile.value.xp + 100
            val newLevel = (newXp / 200) + 1
            _playerProfile.value = _playerProfile.value.copy(
                wins = newWins,
                totalGames = _playerProfile.value.totalGames + 1,
                xp = newXp,
                level = newLevel
            )
        } else {
            soundManager.playLoss()
            _playerProfile.value = _playerProfile.value.copy(
                losses = _playerProfile.value.losses + 1,
                totalGames = _playerProfile.value.totalGames + 1
            )
        }
        saveProfileToPrefs(_playerProfile.value)

        // Sync stats to Supabase if logged in
        val session = _currentSession.value
        val updatedProfile = _playerProfile.value
        if (session != null && !updatedProfile.isGuest) {
            viewModelScope.launch {
                // Attempt secure database RPC first to calculate & increment stats server-side
                val rpcSuccess = supabaseProfileService.recordMatchResultRpc(
                    accessToken = session.accessToken,
                    isWin = isMeWin,
                    isDraw = false,
                    xpEarned = if (isMeWin) 50 else 0,
                    coinsEarned = if (isMeWin) 20 else 0
                )
                // Fallback to profile patch if RPC is not deployed
                if (!rpcSuccess) {
                    supabaseProfileService.updateProfile(
                        userId = session.userId,
                        accessToken = session.accessToken,
                        level = updatedProfile.level,
                        xp = updatedProfile.xp,
                        coins = updatedProfile.coins,
                        wins = updatedProfile.wins,
                        losses = updatedProfile.losses,
                        draws = updatedProfile.draws,
                        gamesPlayed = updatedProfile.totalGames
                    )
                }
            }
        }

        // Save match record to database
        viewModelScope.launch {
            db.matchRecordDao().insertMatch(
                MatchRecord(
                    opponentName = if (_selectedMode.value == GameMode.SINGLE_PLAYER) "البوت (${_selectedDifficulty.value.titleAr})" else "خصم شبكي",
                    gameMode = _selectedMode.value.titleAr,
                    gameType = _selectedType.value.titleAr,
                    secretNumber = _mySecretNumber.value,
                    attemptsCount = _myAttemptsLog.value.size,
                    isWin = isMeWin,
                    durationSeconds = 60,
                    scoreEarned = if (isMeWin) 100 else 20
                )
            )
        }

        _currentScreen.value = AppScreen.RESULTS
    }

    fun requestRematch() {
        soundManager.playClick()
        val me = _playerProfile.value
        when (_selectedMode.value) {
            GameMode.SINGLE_PLAYER -> {
                _currentScreen.value = AppScreen.LOBBY
                resetLobbyState()
            }
            GameMode.ONLINE_ROOM -> {
                onlineNetworkManager.sendMessage("REMATCH", me.id, me.username, "true")
                _currentScreen.value = AppScreen.LOBBY
                resetLobbyState()
            }
            GameMode.LOCAL_WIFI -> {
                localWifiNetworkManager.sendMessage("REMATCH", me.id, me.username, "true")
                _currentScreen.value = AppScreen.LOBBY
                resetLobbyState()
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            db.matchRecordDao().clearHistory()
            soundManager.playClick()
        }
    }

    private fun generateRoomCode(): String {
        return Random.nextInt(100000, 999999).toString()
    }

    override fun onCleared() {
        super.onCleared()
        soundManager.release()
        voiceChatManager.stopVoiceChat()
        onlineNetworkManager.disconnect()
        localWifiNetworkManager.disconnect()
    }
}
