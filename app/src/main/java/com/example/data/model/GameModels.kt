package com.example.data.model

import androidx.annotation.DrawableRes

enum class GameMode(val titleAr: String, val titleEn: String, val descAr: String, val descEn: String) {
    SINGLE_PLAYER("لعب فردي ضد الذكاء الاصطناعي", "Single Player vs AI", "تحدى البوت بدرجات صعوبة مختلفة بدون إنترنت", "Challenge smart AI bot with various difficulties offline"),
    ONLINE_ROOM("غرفة أونلاين (Online PIN)", "Remote Online Room", "أنشئ غرفة برمز سداسي (PIN) للعب الثنائي السريع", "Create or join a multiplayer room with a 6-digit PIN"),
    LOCAL_WIFI("شبكة محلية (Local Wi-Fi)", "Local Wi-Fi / Hotspot", "العب مع أصدقائك عبر الشبكة المحلية بدون إنترنت", "Play with friends nearby on local Wi-Fi or Hotspot")
}

enum class GameType(val titleAr: String, val titleEn: String, val descAr: String, val descEn: String) {
    CODE_SECRET("الرقم المخفي (3 - 6 أرقام)", "Hidden Number (3-6 Digits)", "تخمين الرقم السري المكون من 3 إلى 6 أرقام وحساب المطابقة الدقيقة", "Guess secret code with exact position match counts"),
    RANGE_1_100("تخمين النطاق (1 - 100)", "Range Mode (1 - 100)", "تخمين رقم سري بين 1 و 100 مع تلميحات أكبر/أصغر", "Guess a number from 1 to 100 with Higher/Lower clues")
}

enum class AiDifficulty(val titleAr: String, val titleEn: String, val colorHex: Long) {
    EASY("سهل", "Easy", 0xFF00FF87),
    MEDIUM("متوسط", "Medium", 0xFF00F0FF),
    HARD("صعب", "Hard", 0xFFFF007A),
    IMPOSSIBLE("خوارزمي خارق 🔥", "Super Algo 🔥", 0xFFD500F9)
}

data class PlayerProfile(
    val id: String = "player_${System.currentTimeMillis() % 10000}",
    val username: String = "اللاعب الأسطوري",
    val avatarId: Int = 1,
    val level: Int = 1,
    val xp: Int = 150,
    val wins: Int = 0,
    val losses: Int = 0,
    val totalGames: Int = 0
) {
    val winRate: Int
        get() = if (totalGames > 0) (wins * 100) / totalGames else 0
}

data class GuessAttempt(
    val id: Long = System.currentTimeMillis(),
    val attemptNumber: Int,
    val guessedNumber: String,
    val exactMatches: Int = 0,
    val totalLength: Int = 4,
    val clueTextAr: String = "",
    val clueTextEn: String = "",
    val isWin: Boolean = false,
    val proximityPercent: Int = 0,
    val isHigher: Boolean? = null,
    val playerName: String = "أنت"
)

data class RoomPlayer(
    val id: String,
    val name: String,
    val avatarId: Int,
    val isHost: Boolean,
    val isReady: Boolean = false,
    val secretSet: Boolean = false,
    val isSpeaking: Boolean = false
)

data class NetworkMessage(
    val type: String,
    val senderId: String,
    val senderName: String,
    val payload: String,
    val timestamp: Long = System.currentTimeMillis()
)

sealed class AppError(
    open val messageAr: String,
    open val messageEn: String
) {
    data class NetworkConnectionFailed(
        override val messageAr: String = "فشل الاتصال بالسيرفر. يرجى التحقق من اتصال الإنترنت والإعادة.",
        override val messageEn: String = "Failed to connect to server. Please check your internet connection."
    ) : AppError(messageAr, messageEn)

    data class RoomNotFoundOrClosed(
        override val messageAr: String = "الغرفة غير موجودة أو تم إغلاقها من قِبل المضيف.",
        override val messageEn: String = "Room not found or closed by host."
    ) : AppError(messageAr, messageEn)

    data class LocalWifiConnectionFailed(
        val ipAddress: String,
        override val messageAr: String = "فشل الاتصال عبر الشبكة المحلية بالمرسل ($ipAddress). تأكد من الاتصال بنفس شبكة الواي فاي.",
        override val messageEn: String = "Failed to connect via local Wi-Fi to $ipAddress. Ensure both devices are on the same Wi-Fi."
    ) : AppError(messageAr, messageEn)

    data class InvalidSecretInput(
        override val messageAr: String = "الرقم السري غير صالح، يجب أن يطابق الشروط المحددة.",
        override val messageEn: String = "Invalid secret number input."
    ) : AppError(messageAr, messageEn)

    data class CustomError(
        override val messageAr: String,
        override val messageEn: String = messageAr
    ) : AppError(messageAr, messageEn)
}

