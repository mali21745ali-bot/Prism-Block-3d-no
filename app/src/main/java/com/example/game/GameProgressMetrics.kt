package com.example.game

import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DailyChallenge
import com.example.data.model.GameMode
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.data.model.TimeChallenge
import kotlin.math.floor

/**
 * Computed presentation metrics for the permanent Level Progress Bar (1,000 lines per level).
 */
data class LevelProgressMetrics(
    val levelNumber: Long,
    val clearedLines: Long,
    val requiredLines: Long = DEFAULT_LINES_PER_LEVEL,
    val fraction: Float,
    val percentageText: String,
    val countText: String,
    val isLevelMaxed: Boolean
)

/**
 * Computed presentation metrics for the per-session Game Progress Bar (resets on every new game).
 */
data class GameSessionProgressMetrics(
    val linesCleared: Long,
    val targetLines: Long,
    val label: String,
    val fraction: Float,
    val percentageText: String,
    val countText: String,
    val milestoneReached: Boolean
)

/**
 * Result of adding cleared lines to permanent level progression across levels 1..100.
 */
data class LevelAdvancementResult(
    val newLevel: Long,
    val newLevelLines: Long,
    val newCompletedLevelsCount: Long,
    val levelsCompletedNow: Long,
    val completedLevelNumber: Long?
)

object GameProgressCalculator {

    private val sessionMilestones = longArrayOf(20L, 50L, 100L, 250L, 500L, 1000L)

    /**
     * Formats a progress ratio as a clean percentage string:
     * - Exact whole percentages display cleanly without trailing ".0" (e.g. 0%, 25%, 100%).
     * - Fractional values display 1 decimal place (e.g. 0.1%, 25.1%, 99.9%) so every cleared line
     *   shows immediate percentage movement and 999 / 1,000 never prematurely displays 100%.
     */
    fun formatProgressPercent(current: Long, target: Long): String {
        if (target <= 0L || current <= 0L) return "0%"
        if (current >= target) return "100%"
        val rawTenths = floor((current.toDouble() * 1000.0) / target.toDouble()).toLong()
        val clampedTenths = rawTenths.coerceIn(1L, 999L)
        val whole = clampedTenths / 10L
        val decimal = clampedTenths % 10L
        return if (decimal == 0L) "$whole%" else "$whole.$decimal%"
    }

    /**
     * Advances a player's permanent level progress by [addedLines] cleared lines.
     * Every 1,000 cleared lines completes the current level and unlocks the next level (up to 100).
     */
    fun advancePermanentLevel(
        currentLevel: Long,
        currentLevelLines: Long,
        completedLevelsCount: Long,
        addedLines: Long,
        requiredLinesPerLevel: Long = DEFAULT_LINES_PER_LEVEL,
        maxLevel: Long = MAX_LEVEL_NUMBER.toLong()
    ): LevelAdvancementResult {
        val safeRequired = requiredLinesPerLevel.coerceAtLeast(1L)
        var level = currentLevel.coerceIn(1L, maxLevel)
        var lines = currentLevelLines.coerceIn(0L, safeRequired)
        var completedCount = completedLevelsCount.coerceIn(0L, maxLevel)
        val safeAdded = addedLines.coerceAtLeast(0L)

        if (safeAdded == 0L) {
            return LevelAdvancementResult(
                newLevel = level,
                newLevelLines = lines,
                newCompletedLevelsCount = completedCount,
                levelsCompletedNow = 0L,
                completedLevelNumber = null
            )
        }

        // Already mastered max level at 100%
        if (level >= maxLevel && lines >= safeRequired && completedCount >= maxLevel) {
            return LevelAdvancementResult(
                newLevel = maxLevel,
                newLevelLines = safeRequired,
                newCompletedLevelsCount = maxLevel,
                levelsCompletedNow = 0L,
                completedLevelNumber = null
            )
        }

        var remaining = lines + safeAdded
        var levelsCompletedNow = 0L
        var lastCompletedLevel: Long? = null

        while (remaining >= safeRequired) {
            if (level < maxLevel) {
                levelsCompletedNow += 1L
                lastCompletedLevel = level
                completedCount = maxOf(completedCount, level).coerceAtMost(maxLevel)
                level += 1L
                remaining -= safeRequired
            } else {
                // Completing the final level (Level 100)
                if (completedCount < maxLevel) {
                    levelsCompletedNow += 1L
                    lastCompletedLevel = maxLevel
                    completedCount = maxLevel
                }
                remaining = safeRequired
                break
            }
        }

        return LevelAdvancementResult(
            newLevel = level,
            newLevelLines = remaining.coerceIn(0L, safeRequired),
            newCompletedLevelsCount = completedCount,
            levelsCompletedNow = levelsCompletedNow,
            completedLevelNumber = lastCompletedLevel
        )
    }

    fun computeLevelProgress(
        levelNumber: Long,
        clearedLines: Long,
        requiredLines: Long = DEFAULT_LINES_PER_LEVEL
    ): LevelProgressMetrics {
        val safeLevel = levelNumber.coerceIn(1L, MAX_LEVEL_NUMBER.toLong())
        val safeReq = requiredLines.coerceAtLeast(1L)
        val safeLines = clearedLines.coerceIn(0L, safeReq)
        val fraction = (safeLines.toFloat() / safeReq.toFloat()).coerceIn(0f, 1f)
        return LevelProgressMetrics(
            levelNumber = safeLevel,
            clearedLines = safeLines,
            requiredLines = safeReq,
            fraction = fraction,
            percentageText = formatProgressPercent(safeLines, safeReq),
            countText = "%,d / %,d".format(safeLines, safeReq),
            isLevelMaxed = safeLevel >= MAX_LEVEL_NUMBER.toLong() && safeLines >= safeReq
        )
    }

    fun computeGameSessionProgress(
        mode: GameMode,
        sessionLinesCleared: Long,
        dailyChallenge: DailyChallenge? = null,
        timeChallenge: TimeChallenge? = null
    ): GameSessionProgressMetrics {
        val safeLines = sessionLinesCleared.coerceAtLeast(0L)

        // Mode-specific session goal when available; otherwise progressive session milestones
        val (targetLines, label) = when {
            mode == GameMode.DAILY && dailyChallenge != null && dailyChallenge.targetLines > 0L -> {
                dailyChallenge.targetLines to "Game Progress"
            }
            mode == GameMode.TIME &&
                timeChallenge != null &&
                timeChallenge.rewardType == "TARGET" &&
                timeChallenge.targetScore > 0L -> {
                val targetFromScore = ((timeChallenge.targetScore + 9L) / 10L).coerceAtLeast(1L)
                targetFromScore to "Game Progress"
            }
            else -> {
                val nextMilestone = sessionMilestones.firstOrNull { safeLines <= it } ?: DEFAULT_LINES_PER_LEVEL
                nextMilestone to "Game Progress"
            }
        }

        val cappedForPct = safeLines.coerceAtMost(targetLines)
        val fraction = (safeLines.toFloat() / targetLines.toFloat()).coerceIn(0f, 1f)
        val unit = if (safeLines == 1L && safeLines > targetLines) "line" else "lines"
        val countText = if (safeLines > targetLines) {
            "%,d $unit".format(safeLines)
        } else {
            "%,d / %,d lines".format(safeLines, targetLines)
        }

        return GameSessionProgressMetrics(
            linesCleared = safeLines,
            targetLines = targetLines,
            label = label,
            fraction = fraction,
            percentageText = formatProgressPercent(cappedForPct, targetLines),
            countText = countText,
            milestoneReached = safeLines >= targetLines
        )
    }
}
