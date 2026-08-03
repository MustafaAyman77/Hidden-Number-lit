package com.example.domain

import com.example.data.model.AiDifficulty
import com.example.data.model.GameType
import com.example.data.model.GuessAttempt
import kotlin.math.abs
import kotlin.random.Random

class AiBotEngine {

    private var lowBound = 1
    private var highBound = 100
    private var possibleCodes = mutableListOf<String>()

    fun resetAiState(gameType: GameType, codeLength: Int = 4, allowRepetition: Boolean = false) {
        lowBound = 1
        highBound = 100
        possibleCodes = generatePossibleCodes(codeLength, allowRepetition)
    }

    fun generateSecretNumber(gameType: GameType, codeLength: Int = 4, allowRepetition: Boolean = false): String {
        return when (gameType) {
            GameType.RANGE_1_100 -> Random.nextInt(1, 101).toString()
            GameType.CODE_SECRET -> {
                if (allowRepetition) {
                    (1..codeLength).map { Random.nextInt(0, 10) }.joinToString("")
                } else {
                    val digits = (0..9).shuffled().take(codeLength)
                    digits.joinToString("")
                }
            }
        }
    }

    fun evaluateGuess(
        guessedStr: String,
        targetSecretStr: String,
        gameType: GameType,
        attemptNum: Int,
        playerName: String
    ): GuessAttempt {
        return when (gameType) {
            GameType.RANGE_1_100 -> {
                val guess = guessedStr.toIntOrNull() ?: 50
                val target = targetSecretStr.toIntOrNull() ?: 50
                val isWin = guess == target
                val diff = abs(guess - target)
                val proximityPercent = ((100 - diff).coerceAtLeast(0))

                val isHigher = if (target > guess) true else if (target < guess) false else null

                val clueAr = when {
                    isWin -> "مبروك! إجابة صحيحة 🎯"
                    diff <= 3 -> if (target > guess) "الرقم السري أكبر ⬆️ (قريب جداً! 🔥)" else "الرقم السري أصغر ⬇️ (قريب جداً! 🔥)"
                    diff <= 12 -> if (target > guess) "الرقم السري أكبر ⬆️ (قريب ⚡)" else "الرقم السري أصغر ⬇️ (قريب ⚡)"
                    else -> if (target > guess) "الرقم السري أكبر ⬆️ (بعيد ❄️)" else "الرقم السري أصغر ⬇️ (بعيد ❄️)"
                }

                val clueEn = when {
                    isWin -> "Bingo! Correct Answer 🎯"
                    diff <= 3 -> if (target > guess) "Secret is HIGHER ⬆️ (Super Close! 🔥)" else "Secret is LOWER ⬇️ (Super Close! 🔥)"
                    diff <= 12 -> if (target > guess) "Secret is HIGHER ⬆️ (Warm ⚡)" else "Secret is LOWER ⬇️ (Warm ⚡)"
                    else -> if (target > guess) "Secret is HIGHER ⬆️ (Cold ❄️)" else "Secret is LOWER ⬇️ (Cold ❄️)"
                }

                GuessAttempt(
                    attemptNumber = attemptNum,
                    guessedNumber = guessedStr,
                    clueTextAr = clueAr,
                    clueTextEn = clueEn,
                    isWin = isWin,
                    proximityPercent = proximityPercent,
                    isHigher = isHigher,
                    playerName = playerName
                )
            }

            GameType.CODE_SECRET -> {
                val len = targetSecretStr.length
                val exactMatches = calculateExactMatches(guessedStr, targetSecretStr)
                val isWin = exactMatches == len

                val clueAr = if (isWin) {
                    "تم كسر الرقم السري بنجاح! 🔓🎯"
                } else {
                    "$exactMatches / $len أرقام بمكانها الصحيح 🎯"
                }

                val clueEn = if (isWin) {
                    "Secret Cracked Successfully! 🔓🎯"
                } else {
                    "$exactMatches / $len exact position matches 🎯"
                }

                GuessAttempt(
                    attemptNumber = attemptNum,
                    guessedNumber = guessedStr,
                    exactMatches = exactMatches,
                    totalLength = len,
                    clueTextAr = clueAr,
                    clueTextEn = clueEn,
                    isWin = isWin,
                    playerName = playerName
                )
            }
        }
    }

    fun calculateExactMatches(guess: String, secret: String): Int {
        var count = 0
        val len = minOf(guess.length, secret.length)
        for (i in 0 until len) {
            if (guess[i] == secret[i]) {
                count++
            }
        }
        return count
    }

    fun makeAiTurnGuess(
        difficulty: AiDifficulty,
        gameType: GameType,
        codeLength: Int = 4,
        allowRepetition: Boolean = false,
        previousAttempts: List<GuessAttempt>
    ): String {
        when (gameType) {
            GameType.RANGE_1_100 -> {
                for (att in previousAttempts) {
                    val g = att.guessedNumber.toIntOrNull() ?: continue
                    if (att.isHigher == true && g >= lowBound) {
                        lowBound = g + 1
                    } else if (att.isHigher == false && g <= highBound) {
                        highBound = g - 1
                    }
                }

                if (lowBound > highBound) {
                    lowBound = 1
                    highBound = 100
                }

                return when (difficulty) {
                    AiDifficulty.EASY -> Random.nextInt(lowBound, highBound + 1).toString()
                    AiDifficulty.MEDIUM -> {
                        val mid = (lowBound + highBound) / 2
                        val jitter = Random.nextInt(-3, 4)
                        (mid + jitter).coerceIn(lowBound, highBound).toString()
                    }
                    AiDifficulty.HARD, AiDifficulty.IMPOSSIBLE -> {
                        ((lowBound + highBound) / 2).toString()
                    }
                }
            }

            GameType.CODE_SECRET -> {
                if (possibleCodes.isEmpty()) {
                    possibleCodes = generatePossibleCodes(codeLength, allowRepetition)
                }

                // Filter possible codes based on exact match feedback
                for (att in previousAttempts) {
                    possibleCodes.retainAll { code ->
                        calculateExactMatches(att.guessedNumber, code) == att.exactMatches
                    }
                }

                if (possibleCodes.isEmpty()) {
                    possibleCodes = generatePossibleCodes(codeLength, allowRepetition)
                }

                return when (difficulty) {
                    AiDifficulty.EASY -> generateSecretNumber(GameType.CODE_SECRET, codeLength, allowRepetition)
                    AiDifficulty.MEDIUM -> possibleCodes.random()
                    AiDifficulty.HARD, AiDifficulty.IMPOSSIBLE -> possibleCodes.first()
                }
            }
        }
    }

    private fun generatePossibleCodes(length: Int, allowRepetition: Boolean): MutableList<String> {
        val list = mutableListOf<String>()
        val maxPossibilities = 1000 // Limit for performance if len is high
        
        fun search(current: String) {
            if (list.size >= maxPossibilities) return
            if (current.length == length) {
                list.add(current)
                return
            }
            for (digit in 0..9) {
                val charDigit = digit.toString()[0]
                if (!allowRepetition && current.contains(charDigit)) continue
                search(current + charDigit)
            }
        }

        search("")
        return list
    }
}

