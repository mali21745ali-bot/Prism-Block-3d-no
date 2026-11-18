package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.SessionCommitResult
import com.example.data.model.BoosterType
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DailyChallenge
import com.example.data.model.GameMode
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.data.model.TimeChallenge
import com.example.game.BOARD_SIZE
import com.example.game.BlockPuzzleEngine
import com.example.game.GameProgressCalculator
import com.example.game.TrayPiece
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

data class FloatingRewardPopup(
    val id: Long,
    val text: String,
    val comboLabel: String?
)

data class GamePlayUiState(
    val sessionId: Long = 1L,
    val mode: GameMode = GameMode.CLASSIC,
    val grid: List<Int> = BlockPuzzleEngine.emptyGrid(),
    val tray: List<TrayPiece> = BlockPuzzleEngine.generateTray(),
    val score: Long = 0L,
    val linesCleared: Long = 0L,
    val coinsEarned: Long = 0L,
    val activeLevelNumber: Long = 1L,
    val levelStartingLines: Long = 0L,
    val permanentLevelNumber: Long = 1L,
    val permanentLevelLines: Long? = null,
    val permanentCompletedLevels: Long = 0L,
    val pendingNextLevelNumber: Long? = null,
    val pendingNextLevelLines: Long = 0L,
    val activeDailyChallenge: DailyChallenge? = null,
    val activeTimeChallenge: TimeChallenge? = null,
    val remainingSeconds: Long = 0L,
    val clearingCells: Set<Int> = emptySet(),
    val lastPlacedCells: List<Int> = emptyList(),
    val clearEffectToken: Int = 0,
    val placementEffectToken: Int = 0,
    val floatingPopup: FloatingRewardPopup? = null,
    val activeBoosterMode: BoosterType? = null,
    val boostersUsedInGame: Map<BoosterType, Int> = emptyMap(),
    val canUndo: Boolean = false,
    val continuesUsed: Long = 0L,
    val isGameOver: Boolean = false,
    val isLevelCompleted: Boolean = false,
    val isTimeUp: Boolean = false,
    val isCommittingSession: Boolean = false,
    val lastCommitResult: SessionCommitResult? = null
) {
    /**
     * Live permanent level lines (0..1000) used by the Level Progress Bar.
     * Falls back seamlessly to (levelStartingLines + linesCleared) when state is constructed in isolation.
     */
    val effectivePermanentLevelLines: Long
        get() = (permanentLevelLines ?: (levelStartingLines + linesCleared))
            .coerceIn(0L, DEFAULT_LINES_PER_LEVEL)
}

private data class SessionSnapshot(
    val sessionId: Long,
    val mode: GameMode,
    val totalLinesCleared: Long,
    val activeLevelNumber: Long,
    val activeTimeChallenge: TimeChallenge?,
    val continuesUsed: Long
)

private data class UndoMoveSnapshot(
    val grid: List<Int>,
    val tray: List<TrayPiece>,
    val score: Long,
    val linesCleared: Long,
    val coinsEarned: Long,
    val permanentLevelNumber: Long,
    val permanentLevelLines: Long?,
    val permanentCompletedLevels: Long
)

class GamePlayViewModel(
    private val soundEngine: SoundEngine
) : ViewModel() {

    private val _state = MutableStateFlow(GamePlayUiState())
    val state: StateFlow<GamePlayUiState> = _state.asStateFlow()

    private var timerJob: Job? = null
    private var rng: Random = Random.Default
    private var nextSessionCounter: Long = 1L
    private val undoStack = ArrayDeque<UndoMoveSnapshot>()

    private val syncMutex = Mutex()
    private val committedLinesBySession = mutableMapOf<Long, Long>()
    private val finalizedSessions = mutableSetOf<Long>()

    fun startNewGame(
        mode: GameMode,
        levelNumber: Long = 1L,
        levelExistingLines: Long = 0L,
        permanentLevelNumber: Long = levelNumber,
        permanentLevelLines: Long = levelExistingLines,
        permanentCompletedLevels: Long = (permanentLevelNumber - 1L).coerceAtLeast(0L),
        dailyChallenge: DailyChallenge? = null,
        timeChallenge: TimeChallenge? = null,
        customGrid: List<Int>? = null,
        customTray: List<TrayPiece>? = null
    ) {
        timerJob?.cancel()
        undoStack.clear()
        val newSessionId = ++nextSessionCounter
        rng = when (mode) {
            GameMode.DAILY -> Random(dailyChallenge?.seed ?: 20261117L)
            else -> Random(System.currentTimeMillis() + newSessionId)
        }
        val initialTray = customTray ?: BlockPuzzleEngine.generateTray(rng)
        val initialGrid = customGrid ?: BlockPuzzleEngine.emptyGrid()
        val duration = if (mode == GameMode.TIME) (timeChallenge?.durationSeconds ?: 180L) else 0L
        val safePermLevel = permanentLevelNumber.coerceIn(1L, MAX_LEVEL_NUMBER.toLong())
        val safePermLines = permanentLevelLines.coerceIn(0L, DEFAULT_LINES_PER_LEVEL)
        val safeCompleted = permanentCompletedLevels.coerceIn(0L, MAX_LEVEL_NUMBER.toLong())

        _state.value = GamePlayUiState(
            sessionId = newSessionId,
            mode = mode,
            grid = initialGrid,
            tray = initialTray,
            score = 0L,
            linesCleared = 0L,
            coinsEarned = 0L,
            activeLevelNumber = levelNumber.coerceIn(1L, MAX_LEVEL_NUMBER.toLong()),
            levelStartingLines = safePermLines,
            permanentLevelNumber = safePermLevel,
            permanentLevelLines = safePermLines,
            permanentCompletedLevels = safeCompleted,
            pendingNextLevelNumber = null,
            pendingNextLevelLines = 0L,
            activeDailyChallenge = dailyChallenge,
            activeTimeChallenge = timeChallenge,
            remainingSeconds = duration,
            activeBoosterMode = null,
            boostersUsedInGame = emptyMap(),
            canUndo = false
        )

        if (mode == GameMode.TIME && duration > 0L) {
            startCountdownTimer()
        }
    }

    /**
     * Starts a fresh game session (resetting ONLY the Game Progress Bar, score, lines, and board)
     * while permanently preserving the player's Level Progress Bar and committing any unsaved lines
     * from the previous game to Firestore.
     */
    fun restartPreservingPermanentLevel(
        appViewModel: AppViewModel? = null,
        targetMode: GameMode = _state.value.mode,
        overrideLevelNumber: Long? = null
    ) {
        val prev = _state.value
        if (appViewModel != null && prev.linesCleared > 0L) {
            val snap = SessionSnapshot(
                sessionId = prev.sessionId,
                mode = prev.mode,
                totalLinesCleared = prev.linesCleared,
                activeLevelNumber = prev.permanentLevelNumber,
                activeTimeChallenge = prev.activeTimeChallenge,
                continuesUsed = prev.continuesUsed
            )
            viewModelScope.launch {
                finalizeSnapshot(appViewModel, snap)
            }
        }

        val carriedLevel = prev.pendingNextLevelNumber ?: prev.permanentLevelNumber
        val carriedLines = if (prev.pendingNextLevelNumber != null) {
            prev.pendingNextLevelLines
        } else {
            prev.effectivePermanentLevelLines
        }
        val nextActiveLevel = overrideLevelNumber ?: if (targetMode == GameMode.LEVEL) {
            if (prev.isLevelCompleted) carriedLevel else prev.activeLevelNumber
        } else {
            carriedLevel
        }

        startNewGame(
            mode = targetMode,
            levelNumber = nextActiveLevel,
            levelExistingLines = carriedLines,
            permanentLevelNumber = carriedLevel,
            permanentLevelLines = carriedLines,
            permanentCompletedLevels = prev.permanentCompletedLevels,
            dailyChallenge = prev.activeDailyChallenge,
            timeChallenge = prev.activeTimeChallenge
        )
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val cur = _state.value
                if (cur.isGameOver || cur.isLevelCompleted || cur.isTimeUp) break
                val nextSec = (cur.remainingSeconds - 1L).coerceAtLeast(0L)
                if (nextSec == 0L) {
                    soundEngine.playGameOver()
                    _state.update { it.copy(remainingSeconds = 0L, isTimeUp = true, isGameOver = true, activeBoosterMode = null) }
                    break
                } else {
                    _state.update { it.copy(remainingSeconds = nextSec) }
                }
            }
        }
    }

    fun tryPlacePiece(slotIndex: Int, anchorRow: Int, anchorCol: Int): Boolean {
        val current = _state.value
        if (current.isGameOver || current.isLevelCompleted) return false
        val piece = current.tray.firstOrNull { it.slotIndex == slotIndex && !it.used } ?: return false

        val outcome = BlockPuzzleEngine.placeBlock(current.grid, piece.shape, anchorRow, anchorCol)
            ?: return false

        // Record undo snapshot before applying move
        undoStack.addLast(
            UndoMoveSnapshot(
                grid = current.grid,
                tray = current.tray,
                score = current.score,
                linesCleared = current.linesCleared,
                coinsEarned = current.coinsEarned,
                permanentLevelNumber = current.permanentLevelNumber,
                permanentLevelLines = current.permanentLevelLines,
                permanentCompletedLevels = current.permanentCompletedLevels
            )
        )
        while (undoStack.size > 10) undoStack.removeFirst()

        val placedCellIndices = piece.shape.cells.map {
            BlockPuzzleEngine.index(anchorRow + it.row, anchorCol + it.col)
        }

        soundEngine.playBlockPlace()

        val markedTray = current.tray.map {
            if (it.slotIndex == slotIndex) it.copy(used = true) else it
        }
        val nextTray = if (markedTray.all { it.used }) {
            BlockPuzzleEngine.generateTray(rng)
        } else {
            markedTray
        }

        val newScore = current.score + outcome.scoreGained
        val newLines = current.linesCleared + outcome.linesCleared
        val newCoins = current.coinsEarned + outcome.coinsGained

        val advancement = GameProgressCalculator.advancePermanentLevel(
            currentLevel = current.permanentLevelNumber,
            currentLevelLines = current.effectivePermanentLevelLines,
            completedLevelsCount = current.permanentCompletedLevels,
            addedLines = outcome.linesCleared.toLong(),
            requiredLinesPerLevel = DEFAULT_LINES_PER_LEVEL,
            maxLevel = MAX_LEVEL_NUMBER.toLong()
        )
        val completedLevelNow = advancement.levelsCompletedNow > 0L

        val showLevelCompleteOverlay = completedLevelNow &&
            current.mode == GameMode.LEVEL &&
            current.activeLevelNumber == current.permanentLevelNumber

        val displayPermanentLevel = if (showLevelCompleteOverlay) current.permanentLevelNumber else advancement.newLevel
        val displayPermanentLines = if (showLevelCompleteOverlay) DEFAULT_LINES_PER_LEVEL else advancement.newLevelLines
        val pendingNextLvl = if (showLevelCompleteOverlay) advancement.newLevel else null
        val pendingNextLines = if (showLevelCompleteOverlay) advancement.newLevelLines else 0L

        val hasMoves = BlockPuzzleEngine.hasAnyValidMove(outcome.newGrid, nextTray)
        val gameOverNow = !showLevelCompleteOverlay && !hasMoves

        if (outcome.linesCleared > 0) {
            soundEngine.playLineClear(outcome.linesCleared)
            soundEngine.playCoinReward()
        }
        if (completedLevelNow) {
            if (showLevelCompleteOverlay) timerJob?.cancel()
            soundEngine.playLevelComplete()
        } else if (gameOverNow) {
            timerJob?.cancel()
            soundEngine.playGameOver()
        }

        val comboLabel = when {
            completedLevelNow && !showLevelCompleteOverlay -> {
                val doneLvl = advancement.completedLevelNumber ?: current.permanentLevelNumber
                if (advancement.newLevel > doneLvl) {
                    "LEVEL $doneLvl COMPLETE! LEVEL ${advancement.newLevel} UNLOCKED!"
                } else {
                    "LEVEL $doneLvl MASTERED!"
                }
            }
            outcome.linesCleared >= 4 -> "PRISM MEGA x${outcome.linesCleared}!"
            outcome.linesCleared == 3 -> "TRIPLE CLEAR!"
            outcome.linesCleared == 2 -> "DOUBLE CLEAR!"
            else -> null
        }

        val popup = if (outcome.linesCleared > 0) {
            FloatingRewardPopup(
                id = System.currentTimeMillis(),
                text = "+${outcome.scoreGained} Score  •  +${outcome.coinsGained} Coin${if (outcome.coinsGained > 1) "s" else ""}",
                comboLabel = comboLabel
            )
        } else null

        _state.update {
            it.copy(
                grid = outcome.newGrid,
                tray = nextTray,
                score = newScore,
                linesCleared = newLines,
                coinsEarned = newCoins,
                permanentLevelNumber = displayPermanentLevel,
                permanentLevelLines = displayPermanentLines,
                permanentCompletedLevels = advancement.newCompletedLevelsCount,
                pendingNextLevelNumber = pendingNextLvl,
                pendingNextLevelLines = pendingNextLines,
                clearingCells = outcome.clearedCellIndices,
                lastPlacedCells = placedCellIndices,
                clearEffectToken = if (outcome.clearedCellIndices.isNotEmpty()) it.clearEffectToken + 1 else it.clearEffectToken,
                placementEffectToken = it.placementEffectToken + 1,
                floatingPopup = popup,
                activeBoosterMode = null,
                canUndo = undoStack.isNotEmpty(),
                isLevelCompleted = showLevelCompleteOverlay,
                isGameOver = gameOverNow
            )
        }

        if (outcome.clearedCellIndices.isNotEmpty()) {
            scheduleClearAndPopupCleanup(popup?.id)
        }

        return true
    }

    // ==================== REAL GAMEPLAY BOOSTERS ====================

    fun validateBoosterUsable(
        type: BoosterType,
        ownedCount: Long,
        maxPerGame: Int = type.defaultMaxPerGame
    ): Result<Unit> {
        val cur = _state.value
        if (cur.isLevelCompleted || cur.isTimeUp) {
            return Result.failure(IllegalStateException("Game round has ended."))
        }
        if (cur.isGameOver && type != BoosterType.UNDO) {
            return Result.failure(IllegalStateException("Start a new game or continue first."))
        }
        if (ownedCount <= 0L) {
            return Result.failure(IllegalStateException("You have 0 ${type.label} boosters. Buy a pack in the Coin Shop!"))
        }
        val used = cur.boostersUsedInGame[type] ?: 0
        if (maxPerGame > 0 && used >= maxPerGame) {
            return Result.failure(IllegalStateException("${type.label} limit ($maxPerGame per game) reached for this round."))
        }
        if (type == BoosterType.UNDO && undoStack.isEmpty()) {
            return Result.failure(IllegalStateException("No move available to undo yet."))
        }
        return Result.success(Unit)
    }

    fun toggleBoosterTargeting(type: BoosterType, ownedCount: Long, maxPerGame: Int = type.defaultMaxPerGame): Result<Boolean> {
        val cur = _state.value
        if (cur.activeBoosterMode == type) {
            _state.update { it.copy(activeBoosterMode = null) }
            return Result.success(false)
        }
        validateBoosterUsable(type, ownedCount, maxPerGame).onFailure { return Result.failure(it) }
        _state.update { it.copy(activeBoosterMode = type) }
        return Result.success(true)
    }

    fun cancelBoosterTargeting() {
        _state.update { it.copy(activeBoosterMode = null) }
    }

    /**
     * Checks whether tapping `(row, col)` is a valid Hammer target BEFORE consuming a Hammer from inventory.
     */
    fun canHammerAt(row: Int, col: Int): Boolean {
        if (row !in 0 until BOARD_SIZE || col !in 0 until BOARD_SIZE) return false
        return _state.value.grid.getOrElse(BlockPuzzleEngine.index(row, col)) { 0 } > 0
    }

    /**
     * Real Hammer Booster: removes the tapped occupied cell (plus occupied orthogonal adjacent cells
     * when [powerRadius] >= 1) from the 10x10 board and updates Game Over detection immediately.
     */
    fun executeHammerAt(
        row: Int,
        col: Int,
        ownedCount: Long,
        maxPerGame: Int = BoosterType.HAMMER.defaultMaxPerGame,
        powerRadius: Int = 1
    ): Result<Int> {
        validateBoosterUsable(BoosterType.HAMMER, ownedCount, maxPerGame).onFailure { return Result.failure(it) }
        if (!canHammerAt(row, col)) {
            return Result.failure(IllegalStateException("Tap an occupied 3D block to smash it with the Hammer!"))
        }

        val cur = _state.value
        val mutable = cur.grid.toMutableList()
        val removed = mutableSetOf<Int>()

        fun smashIfOccupied(r: Int, c: Int) {
            if (r in 0 until BOARD_SIZE && c in 0 until BOARD_SIZE) {
                val idx = BlockPuzzleEngine.index(r, c)
                if (mutable[idx] > 0) {
                    mutable[idx] = 0
                    removed.add(idx)
                }
            }
        }

        smashIfOccupied(row, col)
        if (powerRadius >= 1) {
            smashIfOccupied(row - 1, col)
            smashIfOccupied(row + 1, col)
            smashIfOccupied(row, col - 1)
            smashIfOccupied(row, col + 1)
        }

        soundEngine.playLineClear(1)
        val popup = FloatingRewardPopup(
            id = System.currentTimeMillis(),
            text = "Smashed ${removed.size} block${if (removed.size > 1) "s" else ""}!",
            comboLabel = "HAMMER BOOSTER"
        )
        val updatedUsed = cur.boostersUsedInGame.toMutableMap().apply {
            put(BoosterType.HAMMER, (get(BoosterType.HAMMER) ?: 0) + 1)
        }
        val hasMoves = BlockPuzzleEngine.hasAnyValidMove(mutable, cur.tray)

        _state.update {
            it.copy(
                grid = mutable,
                clearingCells = removed,
                clearEffectToken = it.clearEffectToken + 1,
                floatingPopup = popup,
                activeBoosterMode = null,
                boostersUsedInGame = updatedUsed,
                isGameOver = !hasMoves
            )
        }
        scheduleClearAndPopupCleanup(popup.id)
        return Result.success(removed.size)
    }

    /**
     * Checks whether [row] contains at least 1 occupied cell BEFORE consuming a Line Blaster from inventory.
     */
    fun canBlastRow(row: Int): Boolean {
        if (row !in 0 until BOARD_SIZE) return false
        val grid = _state.value.grid
        return (0 until BOARD_SIZE).any { c -> grid[BlockPuzzleEngine.index(row, c)] > 0 }
    }

    /**
     * Real Line Blaster Booster: instantly vaporizes an entire horizontal row on the 10x10 board,
     * awarding +10 Score, +1 Line, +1 Coin and advancing the permanent Level Progress Bar!
     */
    fun executeRowBlasterAt(
        row: Int,
        ownedCount: Long,
        maxPerGame: Int = BoosterType.ROW_BLASTER.defaultMaxPerGame
    ): Result<Int> {
        validateBoosterUsable(BoosterType.ROW_BLASTER, ownedCount, maxPerGame).onFailure { return Result.failure(it) }
        if (!canBlastRow(row)) {
            return Result.failure(IllegalStateException("Tap a row with at least one block to blast it!"))
        }

        val cur = _state.value
        val mutable = cur.grid.toMutableList()
        val clearedIndices = mutableSetOf<Int>()
        for (c in 0 until BOARD_SIZE) {
            val idx = BlockPuzzleEngine.index(row, c)
            mutable[idx] = 0
            clearedIndices.add(idx)
        }

        val advancement = GameProgressCalculator.advancePermanentLevel(
            currentLevel = cur.permanentLevelNumber,
            currentLevelLines = cur.effectivePermanentLevelLines,
            completedLevelsCount = cur.permanentCompletedLevels,
            addedLines = 1L,
            requiredLinesPerLevel = DEFAULT_LINES_PER_LEVEL,
            maxLevel = MAX_LEVEL_NUMBER.toLong()
        )
        val completedLevelNow = advancement.levelsCompletedNow > 0L
        val showLevelCompleteOverlay = completedLevelNow &&
            cur.mode == GameMode.LEVEL &&
            cur.activeLevelNumber == cur.permanentLevelNumber

        val displayPermanentLevel = if (showLevelCompleteOverlay) cur.permanentLevelNumber else advancement.newLevel
        val displayPermanentLines = if (showLevelCompleteOverlay) DEFAULT_LINES_PER_LEVEL else advancement.newLevelLines
        val pendingNextLvl = if (showLevelCompleteOverlay) advancement.newLevel else null
        val pendingNextLines = if (showLevelCompleteOverlay) advancement.newLevelLines else 0L

        soundEngine.playLineClear(2)
        soundEngine.playCoinReward()

        val popup = FloatingRewardPopup(
            id = System.currentTimeMillis(),
            text = "+10 Score  •  +1 Coin",
            comboLabel = "LINE BLASTER!"
        )
        val updatedUsed = cur.boostersUsedInGame.toMutableMap().apply {
            put(BoosterType.ROW_BLASTER, (get(BoosterType.ROW_BLASTER) ?: 0) + 1)
        }
        val hasMoves = BlockPuzzleEngine.hasAnyValidMove(mutable, cur.tray)

        _state.update {
            it.copy(
                grid = mutable,
                score = it.score + 10L,
                linesCleared = it.linesCleared + 1L,
                coinsEarned = it.coinsEarned + 1L,
                permanentLevelNumber = displayPermanentLevel,
                permanentLevelLines = displayPermanentLines,
                permanentCompletedLevels = advancement.newCompletedLevelsCount,
                pendingNextLevelNumber = pendingNextLvl,
                pendingNextLevelLines = pendingNextLines,
                clearingCells = clearedIndices,
                clearEffectToken = it.clearEffectToken + 1,
                floatingPopup = popup,
                activeBoosterMode = null,
                boostersUsedInGame = updatedUsed,
                isLevelCompleted = showLevelCompleteOverlay,
                isGameOver = !showLevelCompleteOverlay && !hasMoves
            )
        }
        scheduleClearAndPopupCleanup(popup.id)
        return Result.success(BOARD_SIZE)
    }

    /**
     * Real Undo Booster: reverses the last valid block placement, restoring exact board, tray & score.
     */
    fun executeUndo(
        ownedCount: Long,
        maxPerGame: Int = BoosterType.UNDO.defaultMaxPerGame
    ): Result<Unit> {
        validateBoosterUsable(BoosterType.UNDO, ownedCount, maxPerGame).onFailure { return Result.failure(it) }
        val prev = undoStack.removeLastOrNull()
            ?: return Result.failure(IllegalStateException("No move available to undo."))

        val cur = _state.value
        soundEngine.playBlockPlace()
        val popup = FloatingRewardPopup(
            id = System.currentTimeMillis(),
            text = "Previous move & tray restored",
            comboLabel = "UNDO BOOSTER"
        )
        val updatedUsed = cur.boostersUsedInGame.toMutableMap().apply {
            put(BoosterType.UNDO, (get(BoosterType.UNDO) ?: 0) + 1)
        }

        _state.update {
            it.copy(
                grid = prev.grid,
                tray = prev.tray,
                score = prev.score,
                linesCleared = prev.linesCleared,
                coinsEarned = prev.coinsEarned,
                permanentLevelNumber = prev.permanentLevelNumber,
                permanentLevelLines = prev.permanentLevelLines,
                permanentCompletedLevels = prev.permanentCompletedLevels,
                clearingCells = emptySet(),
                floatingPopup = popup,
                activeBoosterMode = null,
                boostersUsedInGame = updatedUsed,
                canUndo = undoStack.isNotEmpty(),
                isGameOver = false
            )
        }
        scheduleClearAndPopupCleanup(popup.id)
        return Result.success(Unit)
    }

    /**
     * Real Shuffle Booster: replaces all unused tray blocks with a fresh playable set while preserving the board.
     */
    fun executeShuffle(
        ownedCount: Long,
        maxPerGame: Int = BoosterType.SHUFFLE.defaultMaxPerGame
    ): Result<Unit> {
        validateBoosterUsable(BoosterType.SHUFFLE, ownedCount, maxPerGame).onFailure { return Result.failure(it) }
        val cur = _state.value

        // Build fresh tray pieces for all unused slots, guaranteeing at least one piece fits if any space exists
        val generated = BlockPuzzleEngine.generateTray(rng)
        val freshTray = cur.tray.mapIndexed { idx, existing ->
            if (existing.used) existing
            else existing.copy(shape = generated.getOrElse(idx) { generated.first() }.shape)
        }.toMutableList()

        if (!BlockPuzzleEngine.hasAnyValidMove(cur.grid, freshTray)) {
            val firstUnusedIdx = freshTray.indexOfFirst { !it.used }
            if (firstUnusedIdx >= 0) {
                val fitting = BlockPuzzleEngine.allShapeTemplates().firstOrNull { shape ->
                    BlockPuzzleEngine.hasAnyValidMove(cur.grid, listOf(TrayPiece(0, shape, false)))
                }
                if (fitting != null) {
                    freshTray[firstUnusedIdx] = freshTray[firstUnusedIdx].copy(shape = fitting)
                }
            }
        }

        soundEngine.playBlockPlace()
        val popup = FloatingRewardPopup(
            id = System.currentTimeMillis(),
            text = "Fresh 3D block set dealt!",
            comboLabel = "SHUFFLE BOOSTER"
        )
        val updatedUsed = cur.boostersUsedInGame.toMutableMap().apply {
            put(BoosterType.SHUFFLE, (get(BoosterType.SHUFFLE) ?: 0) + 1)
        }
        val hasMoves = BlockPuzzleEngine.hasAnyValidMove(cur.grid, freshTray)

        _state.update {
            it.copy(
                tray = freshTray,
                floatingPopup = popup,
                activeBoosterMode = null,
                boostersUsedInGame = updatedUsed,
                isGameOver = !hasMoves
            )
        }
        scheduleClearAndPopupCleanup(popup.id)
        return Result.success(Unit)
    }

    private fun scheduleClearAndPopupCleanup(popupId: Long?) {
        viewModelScope.launch {
            delay(460L)
            _state.update { it.copy(clearingCells = emptySet()) }
            if (popupId != null) {
                delay(900L)
                _state.update { s -> if (s.floatingPopup?.id == popupId) s.copy(floatingPopup = null) else s }
            }
        }
    }

    fun applyContinueAfterPayment() {
        val current = _state.value
        val (rescuedGrid, freshTray) = BlockPuzzleEngine.applyContinueRescue(current.grid, rng)
        val extraTime = if (current.mode == GameMode.TIME && current.isTimeUp) 30L else current.remainingSeconds
        _state.update {
            it.copy(
                grid = rescuedGrid,
                tray = freshTray,
                continuesUsed = it.continuesUsed + 1L,
                activeBoosterMode = null,
                isGameOver = false,
                isTimeUp = false,
                remainingSeconds = extraTime
            )
        }
        if (current.mode == GameMode.TIME && extraTime > 0L) {
            startCountdownTimer()
        }
    }

    /**
     * Automatically saves incremental permanent Level Progress & Coins to Firestore whenever lines are cleared.
     * Serialized with [syncMutex] so lines are never double-counted against [syncSessionToBackend].
     */
    fun saveLiveProgressToDatabase(appViewModel: AppViewModel) {
        val cur = _state.value
        if (cur.linesCleared <= 0L) return
        val sessionId = cur.sessionId
        val mode = cur.mode
        val targetLines = cur.linesCleared
        val lvlNum = cur.permanentLevelNumber

        viewModelScope.launch {
            syncMutex.withLock {
                if (sessionId in finalizedSessions) return@withLock
                val alreadySaved = committedLinesBySession[sessionId] ?: 0L
                val delta = (targetLines - alreadySaved).coerceAtLeast(0L)
                if (delta == 0L) return@withLock

                val result = appViewModel.saveLiveProgressDelta(
                    mode = mode,
                    deltaLines = delta,
                    sessionTotalLines = targetLines,
                    levelNumber = lvlNum
                )
                if (result.isSuccess) {
                    committedLinesBySession[sessionId] = alreadySaved + delta
                }
            }
        }
    }

    fun syncSessionToBackend(appViewModel: AppViewModel, onDone: (SessionCommitResult?) -> Unit = {}) {
        val cur = _state.value
        val snap = SessionSnapshot(
            sessionId = cur.sessionId,
            mode = cur.mode,
            totalLinesCleared = cur.linesCleared,
            activeLevelNumber = cur.permanentLevelNumber,
            activeTimeChallenge = cur.activeTimeChallenge,
            continuesUsed = cur.continuesUsed
        )

        viewModelScope.launch {
            val commit = finalizeSnapshot(appViewModel, snap)
            onDone(commit)
        }
    }

    private suspend fun finalizeSnapshot(
        appViewModel: AppViewModel,
        snap: SessionSnapshot
    ): SessionCommitResult? {
        return syncMutex.withLock {
            val alreadySaved = committedLinesBySession[snap.sessionId] ?: 0L
            val uncommittedDelta = (snap.totalLinesCleared - alreadySaved).coerceAtLeast(0L)
            val alreadyFinalized = snap.sessionId in finalizedSessions

            if (alreadyFinalized && uncommittedDelta == 0L) {
                return@withLock if (_state.value.sessionId == snap.sessionId) _state.value.lastCommitResult else null
            }

            if (_state.value.sessionId == snap.sessionId) {
                _state.update { it.copy(isCommittingSession = true) }
            }

            val result = appViewModel.commitSession(
                mode = snap.mode,
                linesCleared = uncommittedDelta,
                levelNumber = snap.activeLevelNumber,
                activeTimeChallenge = snap.activeTimeChallenge,
                continuesUsed = snap.continuesUsed,
                sessionTotalLines = snap.totalLinesCleared
            )

            var output: SessionCommitResult? = null
            result.onSuccess { commit ->
                committedLinesBySession[snap.sessionId] = alreadySaved + uncommittedDelta
                finalizedSessions.add(snap.sessionId)
                output = commit
                if (_state.value.sessionId == snap.sessionId) {
                    _state.update { it.copy(isCommittingSession = false, lastCommitResult = commit) }
                }
            }.onFailure {
                if (_state.value.sessionId == snap.sessionId) {
                    _state.update { it.copy(isCommittingSession = false) }
                }
            }
            output
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
