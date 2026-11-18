package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val MAX_PROFILE_DISPLAY_NAME_LENGTH = 50
const val MAX_PROFILE_PHOTO_URL_LENGTH = 500
const val MAX_LEADERBOARD_SCORE = 10_000_000L
const val MAX_LEVEL_REQUIRED_LINES = 10_000L
const val MAX_LEVEL_STARS = 3L

/**
 * Type-safe Firestore projection of a player's profile stored at `/users/{userId}`.
 *
 * Uses `@IgnoreExtraProperties` so reading `/users/{userId}` cleanly deserializes profile,
 * preferences, and gameplay summary fields without coupling to wallet or PIN fields.
 * Every property provides a default value so Firestore reflection can construct instances safely.
 */
@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val email: String = "",
    val photoUrl: String = "",
    val blocked: Boolean = false,
    val selectedThemeId: String = DEFAULT_THEME_ID,
    val classicHighScore: Long = 0L,
    val totalLinesCleared: Long = 0L,
    val totalGamesPlayed: Long = 0L,
    val currentLevel: Long = 1L,
    val currentLevelLines: Long = 0L,
    val completedLevelsCount: Long = 0L,
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val levelProgressFraction: Float
        get() = (currentLevelLines.toFloat() / DEFAULT_LINES_PER_LEVEL.toFloat()).coerceIn(0f, 1f)
}

/**
 * Per-player, per-level progress record stored at `/users/{userId}/levelProgress/{levelId}`
 * with deterministic document ID `level_<levelNumber>` (e.g. `level_1`..`level_100`).
 */
@Keep
@IgnoreExtraProperties
data class LevelProgress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val levelNumber: Long = 1L,
    val linesCleared: Long = 0L,
    val requiredLines: Long = DEFAULT_LINES_PER_LEVEL,
    val bestScore: Long = 0L,
    val attemptsCount: Long = 0L,
    val starsEarned: Long = 0L,
    val completed: Boolean = false,
    @ServerTimestamp val completedAt: Timestamp? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val progressFraction: Float
        get() {
            val req = requiredLines.coerceAtLeast(1L)
            return (linesCleared.toFloat() / req.toFloat()).coerceIn(0f, 1f)
        }

    @get:Exclude
    val remainingLines: Long
        get() = (requiredLines - linesCleared).coerceAtLeast(0L)

    companion object {
        fun documentId(levelNumber: Long): String {
            require(levelNumber in 1L..MAX_LEVEL_NUMBER.toLong()) {
                "levelNumber must be between 1 and $MAX_LEVEL_NUMBER (was $levelNumber)"
            }
            return "level_$levelNumber"
        }

        /**
         * Derives a 0..3 star rating from level completion progress.
         */
        fun computeStars(linesCleared: Long, requiredLines: Long, completed: Boolean): Long {
            val safeReq = requiredLines.coerceAtLeast(1L)
            if (completed || linesCleared >= safeReq) return MAX_LEVEL_STARS
            val ratio = linesCleared.toDouble() / safeReq.toDouble()
            return when {
                ratio >= 0.65 -> 2L
                ratio >= 0.25 -> 1L
                else -> 0L
            }
        }
    }
}

/**
 * Supported leaderboard scopes enforced by `firestore.rules` (`boardType in ['DAILY', 'ALL_TIME']`).
 */
enum class LeaderboardBoardType(val wireName: String) {
    DAILY("DAILY"),
    ALL_TIME("ALL_TIME");

    companion object {
        fun fromWire(raw: String): LeaderboardBoardType =
            entries.firstOrNull { it.wireName.equals(raw, ignoreCase = true) } ?: ALL_TIME
    }
}

/**
 * Public, zero-PII leaderboard score entry stored at `/leaderboards/{entryId}`.
 * Matches the 11-field `LeaderboardEntry` Firestore schema 1:1.
 */
@Keep
@IgnoreExtraProperties
data class LeaderboardScore(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val photoUrl: String = "",
    val boardType: String = LeaderboardBoardType.ALL_TIME.wireName,
    val dateKey: String = LeaderboardBoardType.ALL_TIME.wireName,
    val bestScore: Long = 0L,
    val totalLines: Long = 0L,
    val highestLevel: Long = 1L,
    val gamesPlayed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    @get:Exclude
    val resolvedBoardType: LeaderboardBoardType
        get() = LeaderboardBoardType.fromWire(boardType)

    companion object {
        fun dailyDocumentId(dateKey: String, userId: String): String {
            require(dateKey.length in 4..20) { "Invalid dateKey: $dateKey" }
            require(userId.isNotBlank()) { "userId must not be blank" }
            return "daily_${dateKey}_$userId"
        }

        fun allTimeDocumentId(userId: String): String {
            require(userId.isNotBlank()) { "userId must not be blank" }
            return "alltime_$userId"
        }
    }
}

/**
 * Combined read-model snapshot for UI screens displaying profile info, level progress,
 * and leaderboard standing together.
 */
data class PlayerProfileSummary(
    val profile: UserProfile,
    val levels: List<LevelProgress> = emptyList(),
    val allTimeScore: LeaderboardScore? = null,
    val dailyScore: LeaderboardScore? = null
)

fun UserAccount.toUserProfile(): UserProfile = UserProfile(
    id = id.ifBlank { userId },
    userId = userId,
    displayName = displayName,
    email = email,
    photoUrl = photoUrl,
    blocked = blocked,
    selectedThemeId = selectedThemeId,
    classicHighScore = classicHighScore,
    totalLinesCleared = totalLinesCleared,
    totalGamesPlayed = totalGamesPlayed,
    currentLevel = currentLevel,
    currentLevelLines = currentLevelLines,
    completedLevelsCount = completedLevelsCount,
    musicEnabled = musicEnabled,
    sfxEnabled = sfxEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun LeaderboardEntry.toLeaderboardScore(): LeaderboardScore = LeaderboardScore(
    id = id,
    userId = userId,
    displayName = displayName,
    photoUrl = photoUrl,
    boardType = boardType,
    dateKey = dateKey,
    bestScore = bestScore,
    totalLines = totalLines,
    highestLevel = highestLevel,
    gamesPlayed = gamesPlayed,
    createdAt = createdAt,
    updatedAt = updatedAt
)
