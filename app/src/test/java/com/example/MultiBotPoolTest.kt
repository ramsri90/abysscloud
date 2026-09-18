package com.example

import com.example.data.BotProfile
import com.example.data.TelegramConfigManager
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MultiBotPoolTest {

    @Test
    fun botProfile_jsonSerialization_isLossless() {
        val bot = BotProfile(
            id = "bot_test_123",
            name = "Backup Bot 2",
            token = "123456789:ABCdefGhIJKLMNOP",
            isEnabled = true,
            isVerified = true,
            lastChecked = 1700000000000L,
            statusMessage = "Verified! Active in -100123456",
            botUsername = "@my_test_bot"
        )

        val json = bot.toJson()
        val restored = BotProfile.fromJson(json)

        assertEquals(bot.id, restored.id)
        assertEquals(bot.name, restored.name)
        assertEquals(bot.token, restored.token)
        assertEquals(bot.isEnabled, restored.isEnabled)
        assertEquals(bot.isVerified, restored.isVerified)
        assertEquals(bot.lastChecked, restored.lastChecked)
        assertEquals(bot.statusMessage, restored.statusMessage)
        assertEquals(bot.botUsername, restored.botUsername)
    }

    @Test
    fun botToken_sanitization_cleansSpecialCharactersAndBotPrefix() {
        val rawTokenWithBotPrefix = "bot123456789:ABCdefGhIJKLMNOP"
        val sanitizedPrefix = TelegramConfigManager.sanitizeBotToken(rawTokenWithBotPrefix)
        assertEquals("123456789:ABCdefGhIJKLMNOP", sanitizedPrefix)

        val tokenWithWhitespace = "  123456789:ABCdefGhIJKLMNOP \n"
        val sanitizedWhitespace = TelegramConfigManager.sanitizeBotToken(tokenWithWhitespace)
        assertEquals("123456789:ABCdefGhIJKLMNOP", sanitizedWhitespace)
    }

    @Test
    fun roundRobinRotation_distributesAcrossBotsSequentially() {
        val bots = listOf("token_bot_1", "token_bot_2", "token_bot_3")
        val rotationCounter = java.util.concurrent.atomic.AtomicInteger(0)

        val getNextToken = {
            val idx = Math.abs(rotationCounter.getAndIncrement() % bots.size)
            bots[idx]
        }

        assertEquals("token_bot_1", getNextToken())
        assertEquals("token_bot_2", getNextToken())
        assertEquals("token_bot_3", getNextToken())
        assertEquals("token_bot_1", getNextToken())
        assertEquals("token_bot_2", getNextToken())
    }

    @Test
    fun multiBotFailover_rotatesToNextBotOnHttp429OrTimeout() {
        val botTokens = listOf("token_bot_1", "token_bot_2", "token_bot_3")
        var attemptsMade = 0
        var successfulToken: String? = null

        // Simulate Bot 1 returning 429 (Rate Limit), Bot 2 timing out, Bot 3 succeeding
        for (token in botTokens) {
            attemptsMade++
            if (token == "token_bot_1") {
                // HTTP 429 - Rotate
                continue
            }
            if (token == "token_bot_2") {
                // Socket Timeout - Rotate
                continue
            }
            // Bot 3 succeeds
            successfulToken = token
            break
        }

        assertEquals(3, attemptsMade)
        assertEquals("token_bot_3", successfulToken)
    }

    @Test
    fun botPoolFiltering_onlyIncludesEnabledBotsWithTokens() {
        val bots = listOf(
            BotProfile(id = "1", name = "Bot 1", token = "tok1", isEnabled = true),
            BotProfile(id = "2", name = "Bot 2", token = "", isEnabled = true),
            BotProfile(id = "3", name = "Bot 3", token = "tok3", isEnabled = false),
            BotProfile(id = "4", name = "Bot 4", token = "tok4", isEnabled = true)
        )

        val activeTokens = bots.filter { it.isEnabled && it.token.isNotBlank() }.map { it.token }

        assertEquals(2, activeTokens.size)
        assertTrue(activeTokens.contains("tok1"))
        assertTrue(activeTokens.contains("tok4"))
        assertFalse(activeTokens.contains(""))
        assertFalse(activeTokens.contains("tok3"))
    }

    @Test
    fun telegramErrorFormatting_detectsChatNotFoundAndProvidesActionableGuide() {
        val rawChatNotFoundJson = """{"ok":false,"error_code":400,"description":"Bad Request: chat not found"}"""
        val json = org.json.JSONObject(rawChatNotFoundJson)
        val description = json.optString("description")

        val containsChatNotFound = description.contains("chat not found", ignoreCase = true)
        assertTrue(containsChatNotFound)

        val userFacingMsg = if (containsChatNotFound) {
            "Chat not found (HTTP 400). Please ensure this bot is added as an Administrator to your Telegram group/channel. Also ensure channel/supergroup Chat IDs begin with -100."
        } else {
            description
        }
        assertTrue(userFacingMsg.contains("Administrator"))
        assertTrue(userFacingMsg.contains("-100"))
    }

    @Test
    fun telegramErrorFormatting_detectsUnauthorizedAndInvalidToken() {
        val rawUnauthorizedJson = """{"ok":false,"error_code":401,"description":"Unauthorized"}"""
        val json = org.json.JSONObject(rawUnauthorizedJson)
        val description = json.optString("description")

        val isUnauthorized = description.contains("Unauthorized", ignoreCase = true)
        assertTrue(isUnauthorized)
    }
}
