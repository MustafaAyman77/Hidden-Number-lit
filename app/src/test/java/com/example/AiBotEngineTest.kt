package com.example

import com.example.data.model.AiDifficulty
import com.example.data.model.GameType
import com.example.data.model.GuessAttempt
import com.example.domain.AiBotEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AiBotEngineTest {

    private lateinit var aiEngine: AiBotEngine

    @Before
    fun setUp() {
        aiEngine = AiBotEngine()
    }

    @Test
    fun testGenerateSecretNumber_CodeSecretLength() {
        val secret4NoRep = aiEngine.generateSecretNumber(
            gameType = GameType.CODE_SECRET,
            codeLength = 4,
            allowRepetition = false
        )
        assertEquals(4, secret4NoRep.length)
        val uniqueChars = secret4NoRep.toSet()
        assertEquals(4, uniqueChars.size)

        val secret5Rep = aiEngine.generateSecretNumber(
            gameType = GameType.CODE_SECRET,
            codeLength = 5,
            allowRepetition = true
        )
        assertEquals(5, secret5Rep.length)
    }

    @Test
    fun testGenerateSecretNumber_Range1To100() {
        val secretRange = aiEngine.generateSecretNumber(
            gameType = GameType.RANGE_1_100
        )
        val num = secretRange.toIntOrNull()
        assertNotNull(num)
        assertTrue(num!! in 1..100)
    }

    @Test
    fun testCalculateExactMatches() {
        val exact3 = aiEngine.calculateExactMatches("1234", "1238")
        assertEquals(3, exact3)

        val exact4 = aiEngine.calculateExactMatches("9876", "9876")
        assertEquals(4, exact4)

        val exact0 = aiEngine.calculateExactMatches("1111", "2222")
        assertEquals(0, exact0)
    }

    @Test
    fun testEvaluateGuess_CodeSecretWin() {
        val result = aiEngine.evaluateGuess(
            guessedStr = "5678",
            targetSecretStr = "5678",
            gameType = GameType.CODE_SECRET,
            attemptNum = 1,
            playerName = "Tester"
        )
        assertTrue(result.isWin)
        assertEquals(4, result.exactMatches)
    }

    @Test
    fun testEvaluateGuess_Range1To100HigherLower() {
        val resultHigher = aiEngine.evaluateGuess(
            guessedStr = "30",
            targetSecretStr = "70",
            gameType = GameType.RANGE_1_100,
            attemptNum = 1,
            playerName = "Tester"
        )
        assertFalse(resultHigher.isWin)
        assertEquals(true, resultHigher.isHigher)

        val resultLower = aiEngine.evaluateGuess(
            guessedStr = "80",
            targetSecretStr = "70",
            gameType = GameType.RANGE_1_100,
            attemptNum = 2,
            playerName = "Tester"
        )
        assertFalse(resultLower.isWin)
        assertEquals(false, resultLower.isHigher)
    }

    @Test
    fun testMakeAiTurnGuess_Range1To100Narrowing() {
        val attempts = listOf(
            GuessAttempt(
                attemptNumber = 1,
                guessedNumber = "50",
                isHigher = true,
                playerName = "User"
            ),
            GuessAttempt(
                attemptNumber = 2,
                guessedNumber = "75",
                isHigher = false,
                playerName = "User"
            )
        )

        val aiGuess = aiEngine.makeAiTurnGuess(
            difficulty = AiDifficulty.IMPOSSIBLE,
            gameType = GameType.RANGE_1_100,
            previousAttempts = attempts
        )

        val guessNum = aiGuess.toInt()
        assertTrue(guessNum in 51..74)
    }

    @Test
    fun testMakeAiTurnGuess_CodeSecretFiltering() {
        val attempts = listOf(
            GuessAttempt(
                attemptNumber = 1,
                guessedNumber = "1234",
                exactMatches = 3,
                totalLength = 4,
                playerName = "User"
            )
        )

        val aiGuess = aiEngine.makeAiTurnGuess(
            difficulty = AiDifficulty.HARD,
            gameType = GameType.CODE_SECRET,
            codeLength = 4,
            allowRepetition = false,
            previousAttempts = attempts
        )

        assertEquals(4, aiGuess.length)
        assertEquals(3, aiEngine.calculateExactMatches("1234", aiGuess))
    }
}
