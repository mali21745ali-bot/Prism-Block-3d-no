package com.example

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.audio.SoundEngine
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DailyChallenge
import com.example.data.model.GameMode
import com.example.data.model.TimeChallenge
import com.example.game.BOARD_SIZE
import com.example.game.BlockPuzzleEngine
import com.example.game.BlockThemes
import com.example.game.BoardGeometry
import com.example.game.GameProgressCalculator
import com.example.game.GridCell
import com.example.game.PolyominoShape
import com.example.game.TrayPiece
import com.example.ui.game.GameplayBoardStage
import com.example.ui.theme.PrismBlockTheme
import com.example.viewmodel.GamePlayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class ProgressBarsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dot = PolyominoShape("dot_1", "dot_1", listOf(GridCell(0, 0)), 1)

    private fun createMutedViewModel(): GamePlayViewModel {
        val sound = SoundEngine().apply { setPreferences(musicOn = false, sfxOn = false) }
        return GamePlayViewModel(sound)
    }

    private fun gridAlmostFullRow0(): List<Int> {
        val g = BlockPuzzleEngine.emptyGrid().toMutableList()
        for (c in 0 until 9) g[BlockPuzzleEngine.index(0, c)] = 2
        return g
    }

    private fun gridAlmostFullRow0AndCol9(): List<Int> {
        val g = BlockPuzzleEngine.emptyGrid().toMutableList()
        for (c in 0 until 9) g[BlockPuzzleEngine.index(0, c)] = 2
        for (r in 1 until BOARD_SIZE) g[BlockPuzzleEngine.index(r, 9)] = 4
        return g
    }

    private fun dragSlotByTouch(slotIndex: Int, shape: PolyominoShape, targetRow: Int, targetCol: Int) {
        val boardBounds = composeTestRule.onNodeWithTag("puzzle_board_canvas").fetchSemanticsNode().boundsInRoot
        val trayBounds = composeTestRule.onNodeWithTag("tray_piece_$slotIndex").fetchSemanticsNode().boundsInRoot
        val boardGeom = BoardGeometry.fromBounds(boardBounds)
        val touchLiftPx = 64f * composeTestRule.density.density

        val desiredCenter = boardGeom.pieceCenter(shape, targetRow, targetCol)
        val pointerTargetInRoot = Offset(desiredCenter.x, desiredCenter.y + touchLiftPx)
        val pointerTargetLocal = pointerTargetInRoot - trayBounds.topLeft

        composeTestRule.onNodeWithTag("tray_piece_$slotIndex").performTouchInput {
            down(center)
            moveTo(Offset((center.x + pointerTargetLocal.x) / 2f, (center.y + pointerTargetLocal.y) / 2f))
            moveTo(pointerTargetLocal)
            up()
        }
        composeTestRule.waitForIdle()
    }

    // -------------------------------------------------------------------------
    // 1. PURE CALCULATOR UNIT TESTS
    // -------------------------------------------------------------------------

    @Test
    fun levelProgressFormatsPercentageAndCountAccurately() {
        val m0 = GameProgressCalculator.computeLevelProgress(1L, 0L)
        assertEquals("0%", m0.percentageText)
        assertEquals("0 / 1,000", m0.countText)
        assertEquals(0f, m0.fraction, 0.0001f)

        val m1 = GameProgressCalculator.computeLevelProgress(1L, 1L)
        assertEquals("0.1%", m1.percentageText)
        assertEquals("1 / 1,000", m1.countText)

        val m250 = GameProgressCalculator.computeLevelProgress(1L, 250L)
        assertEquals("25%", m250.percentageText)
        assertEquals("250 / 1,000", m250.countText)
        assertEquals(0.25f, m250.fraction, 0.0001f)

        val m999 = GameProgressCalculator.computeLevelProgress(4L, 999L)
        assertEquals("99.9%", m999.percentageText)
        assertEquals("999 / 1,000", m999.countText)

        val m1000 = GameProgressCalculator.computeLevelProgress(4L, 1000L)
        assertEquals("100%", m1000.percentageText)
        assertEquals("1,000 / 1,000", m1000.countText)
        assertEquals(1f, m1000.fraction, 0.0001f)
    }

    @Test
    fun permanentLevelCompletesAt1000UnlocksNextLevelAndCarriesOverflow() {
        val exact = GameProgressCalculator.advancePermanentLevel(
            currentLevel = 1L,
            currentLevelLines = 999L,
            completedLevelsCount = 0L,
            addedLines = 1L
        )
        assertEquals(2L, exact.newLevel)
        assertEquals(0L, exact.newLevelLines)
        assertEquals(1L, exact.newCompletedLevelsCount)
        assertEquals(1L, exact.levelsCompletedNow)
        assertEquals(1L, exact.completedLevelNumber)

        // Clearing 4 lines at 998 completes Level 1, unlocks Level 2, and carries 2 overflow lines
        val overflow = GameProgressCalculator.advancePermanentLevel(
            currentLevel = 1L,
            currentLevelLines = 998L,
            completedLevelsCount = 0L,
            addedLines = 4L
        )
        assertEquals(2L, overflow.newLevel)
        assertEquals(2L, overflow.newLevelLines)
        assertEquals(1L, overflow.newCompletedLevelsCount)
        assertEquals(1L, overflow.levelsCompletedNow)

        // Level 100 caps cleanly at 1000/1000 mastered without duplicating completions
        val maxCap = GameProgressCalculator.advancePermanentLevel(
            currentLevel = 100L,
            currentLevelLines = 999L,
            completedLevelsCount = 99L,
            addedLines = 3L
        )
        assertEquals(100L, maxCap.newLevel)
        assertEquals(DEFAULT_LINES_PER_LEVEL, maxCap.newLevelLines)
        assertEquals(100L, maxCap.newCompletedLevelsCount)
        assertEquals(1L, maxCap.levelsCompletedNow)

        val afterMax = GameProgressCalculator.advancePermanentLevel(
            currentLevel = 100L,
            currentLevelLines = 1000L,
            completedLevelsCount = 100L,
            addedLines = 5L
        )
        assertEquals(100L, afterMax.newLevel)
        assertEquals(1000L, afterMax.newLevelLines)
        assertEquals(0L, afterMax.levelsCompletedNow)
        assertNull(afterMax.completedLevelNumber)
    }

    @Test
    fun gameSessionProgressHandlesClassicDailyAndTargetTimeModes() {
        val classic0 = GameProgressCalculator.computeGameSessionProgress(GameMode.CLASSIC, 0L)
        assertEquals("0%", classic0.percentageText)
        assertEquals("0 / 20 lines", classic0.countText)

        val classic1 = GameProgressCalculator.computeGameSessionProgress(GameMode.CLASSIC, 1L)
        assertEquals("5%", classic1.percentageText)
        assertEquals("1 / 20 lines", classic1.countText)

        val classic25 = GameProgressCalculator.computeGameSessionProgress(GameMode.CLASSIC, 25L)
        assertEquals("50%", classic25.percentageText)
        assertEquals("25 / 50 lines", classic25.countText)

        val daily = DailyChallenge(dateKey = "2026-11-17", targetLines = 15L)
        val daily3 = GameProgressCalculator.computeGameSessionProgress(GameMode.DAILY, 3L, dailyChallenge = daily)
        assertEquals("20%", daily3.percentageText)
        assertEquals("3 / 15 lines", daily3.countText)

        val timeTarget = TimeChallenge(id = "tc1", rewardType = "TARGET", targetScore = 200L)
        val time10 = GameProgressCalculator.computeGameSessionProgress(GameMode.TIME, 10L, timeChallenge = timeTarget)
        assertEquals("50%", time10.percentageText)
        assertEquals("10 / 20 lines", time10.countText)
    }

    // -------------------------------------------------------------------------
    // 2. VIEWMODEL STATE LIFECYCLE TESTS
    // -------------------------------------------------------------------------

    @Test
    fun viewModelUpdatesBothBarsAndRestartResetsOnlyGameProgress() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.CLASSIC,
            levelNumber = 1L,
            permanentLevelNumber = 1L,
            permanentLevelLines = 250L,
            permanentCompletedLevels = 0L,
            customGrid = gridAlmostFullRow0(),
            customTray = listOf(
                TrayPiece(0, dot, false),
                TrayPiece(1, dot, false),
                TrayPiece(2, dot, false)
            )
        )

        assertEquals(0L, vm.state.value.linesCleared)
        assertEquals(250L, vm.state.value.effectivePermanentLevelLines)

        // Place dot at (0, 9) -> clears row 0 (+1 line, +10 score, +1 coin)
        assertTrue(vm.tryPlacePiece(0, 0, 9))
        assertEquals(1L, vm.state.value.linesCleared)
        assertEquals(10L, vm.state.value.score)
        assertEquals(1L, vm.state.value.coinsEarned)
        assertEquals(251L, vm.state.value.effectivePermanentLevelLines)
        assertEquals(1L, vm.state.value.permanentLevelNumber)

        // Restart game -> Game Progress resets to 0, Level Progress stays at 251!
        vm.restartPreservingPermanentLevel()
        assertEquals(0L, vm.state.value.linesCleared)
        assertEquals(0L, vm.state.value.score)
        assertEquals(0L, vm.state.value.coinsEarned)
        assertEquals(251L, vm.state.value.effectivePermanentLevelLines)
        assertEquals(1L, vm.state.value.permanentLevelNumber)
    }

    @Test
    fun viewModelCompletesLevelAt1000InLevelModeAndCarriesOverflowToNextLevel() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.LEVEL,
            levelNumber = 1L,
            permanentLevelNumber = 1L,
            permanentLevelLines = 999L,
            permanentCompletedLevels = 0L,
            customGrid = gridAlmostFullRow0AndCol9(),
            customTray = listOf(
                TrayPiece(0, dot, false),
                TrayPiece(1, dot, false),
                TrayPiece(2, dot, false)
            )
        )

        // Placing at (0, 9) clears BOTH row 0 and col 9 -> +2 lines (999 + 2 = 1001)
        assertTrue(vm.tryPlacePiece(0, 0, 9))
        val completedState = vm.state.value
        assertTrue(completedState.isLevelCompleted)
        assertEquals(2L, completedState.linesCleared)
        assertEquals(1000L, completedState.effectivePermanentLevelLines)
        assertEquals(1L, completedState.permanentLevelNumber)
        assertEquals(2L, completedState.pendingNextLevelNumber)
        assertEquals(1L, completedState.pendingNextLevelLines)
        assertEquals(1L, completedState.permanentCompletedLevels)

        // Launch unlocked Level 2 -> Game Progress resets to 0, Level 2 starts with 1 overflow line
        vm.restartPreservingPermanentLevel(targetMode = GameMode.LEVEL, overrideLevelNumber = 2L)
        val lvl2State = vm.state.value
        assertFalse(lvl2State.isLevelCompleted)
        assertEquals(0L, lvl2State.linesCleared)
        assertEquals(2L, lvl2State.activeLevelNumber)
        assertEquals(2L, lvl2State.permanentLevelNumber)
        assertEquals(1L, lvl2State.effectivePermanentLevelLines)
    }

    @Test
    fun viewModelUnlocksNextLevelLiveDuringClassicModeWithoutStoppingGame() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.CLASSIC,
            levelNumber = 3L,
            permanentLevelNumber = 3L,
            permanentLevelLines = 999L,
            permanentCompletedLevels = 2L,
            customGrid = gridAlmostFullRow0(),
            customTray = listOf(
                TrayPiece(0, dot, false),
                TrayPiece(1, dot, false),
                TrayPiece(2, dot, false)
            )
        )

        assertTrue(vm.tryPlacePiece(0, 0, 9))
        val st = vm.state.value
        assertFalse(st.isLevelCompleted)
        assertFalse(st.isGameOver)
        assertEquals(1L, st.linesCleared)
        assertEquals(4L, st.permanentLevelNumber)
        assertEquals(0L, st.effectivePermanentLevelLines)
        assertEquals(3L, st.permanentCompletedLevels)
    }

    // -------------------------------------------------------------------------
    // 3. ROBOLECTRIC COMPOSE UI END-TO-END TESTS
    // -------------------------------------------------------------------------

    @Test
    fun bothProgressBarsRenderBetweenTitleAndCountersWithoutShrinkingBoard() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.CLASSIC,
            levelNumber = 2L,
            permanentLevelNumber = 2L,
            permanentLevelLines = 250L,
            permanentCompletedLevels = 1L
        )

        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = state,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { s, r, c -> vm.tryPlacePiece(s, r, c) }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dual_progress_bars_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("level_progress_bar_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("level_progress_bar_track").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_progress_bar_section").assertIsDisplayed()
        composeTestRule.onNodeWithTag("game_progress_bar_track").assertIsDisplayed()

        val titleBounds = composeTestRule.onNodeWithTag("game_mode_title").fetchSemanticsNode().boundsInRoot
        val panelBounds = composeTestRule.onNodeWithTag("dual_progress_bars_panel").fetchSemanticsNode().boundsInRoot
        val scoreBounds = composeTestRule.onNodeWithTag("hud_score").fetchSemanticsNode().boundsInRoot
        val boardBounds = composeTestRule.onNodeWithTag("puzzle_board_10x10").fetchSemanticsNode().boundsInRoot

        // Strict placement order: Title -> Dual Progress Bars -> Score/Lines/Coins -> 10x10 Board
        assertTrue(
            "Progress panel (${panelBounds.top}) must be below mode title (${titleBounds.bottom})",
            panelBounds.top >= titleBounds.bottom - 1f
        )
        assertTrue(
            "Progress panel (${panelBounds.bottom}) must be above Score/Lines/Coins (${scoreBounds.top})",
            panelBounds.bottom <= scoreBounds.top + 1f
        )
        assertTrue(
            "10x10 board (${boardBounds.top}) must sit cleanly below HUD counters (${scoreBounds.bottom})",
            boardBounds.top >= scoreBounds.bottom - 1f
        )

        // 10x10 board remains square and uses full available mobile width (not shrunk by progress bars)
        val density = composeTestRule.density.density
        val boardWidthDp = boardBounds.width / density
        val boardHeightDp = boardBounds.height / density
        assertEquals(boardWidthDp, boardHeightDp, 1.5f)
        assertTrue("10x10 board width ($boardWidthDp dp) should stay full-size on 411dp phone", boardWidthDp >= 350f)
    }

    @Test
    fun dragAndDropUpdatesBothBarsImmediatelyAndRestartResetsOnlyGameBar() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.CLASSIC,
            levelNumber = 1L,
            permanentLevelNumber = 1L,
            permanentLevelLines = 250L,
            permanentCompletedLevels = 0L,
            customGrid = gridAlmostFullRow0(),
            customTray = listOf(
                TrayPiece(0, dot, false),
                TrayPiece(1, dot, false),
                TrayPiece(2, dot, false)
            )
        )

        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = state,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { s, r, c -> vm.tryPlacePiece(s, r, c) },
                    onRestartClick = { vm.restartPreservingPermanentLevel() }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Initial bar labels & percentages
        composeTestRule.onNodeWithTag("level_progress_count").assertTextEquals("250 / 1,000 lines")
        composeTestRule.onNodeWithTag("level_progress_percent").assertTextEquals("25%")
        composeTestRule.onNodeWithTag("game_progress_count").assertTextEquals("0 / 20 lines")
        composeTestRule.onNodeWithTag("game_progress_percent").assertTextEquals("0%")

        // Drag & drop dot onto (0, 9) to clear row 0
        dragSlotByTouch(slotIndex = 0, shape = dot, targetRow = 0, targetCol = 9)

        // BOTH bars update immediately!
        composeTestRule.onNodeWithTag("level_progress_count").assertTextEquals("251 / 1,000 lines")
        composeTestRule.onNodeWithTag("level_progress_percent").assertTextEquals("25.1%")
        composeTestRule.onNodeWithTag("game_progress_count").assertTextEquals("1 / 20 lines")
        composeTestRule.onNodeWithTag("game_progress_percent").assertTextEquals("5%")

        // Tap Restart button -> Game Progress resets to 0%, Level Progress STAYS at 251 / 1,000 (25.1%)!
        composeTestRule.onNodeWithTag("game_restart_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("game_progress_count").assertTextEquals("0 / 20 lines")
        composeTestRule.onNodeWithTag("game_progress_percent").assertTextEquals("0%")
        composeTestRule.onNodeWithTag("level_progress_count").assertTextEquals("251 / 1,000 lines")
        composeTestRule.onNodeWithTag("level_progress_percent").assertTextEquals("25.1%")
    }

    @Test
    fun clearing1000thLineCompletesLevelAt100PercentAndUnlocksNextLevelInUi() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.LEVEL,
            levelNumber = 1L,
            permanentLevelNumber = 1L,
            permanentLevelLines = 999L,
            permanentCompletedLevels = 0L,
            customGrid = gridAlmostFullRow0(),
            customTray = listOf(
                TrayPiece(0, dot, false),
                TrayPiece(1, dot, false),
                TrayPiece(2, dot, false)
            )
        )

        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = state,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { s, r, c -> vm.tryPlacePiece(s, r, c) },
                    onPlayNextLevel = { nextLvl ->
                        vm.restartPreservingPermanentLevel(
                            targetMode = GameMode.LEVEL,
                            overrideLevelNumber = nextLvl
                        )
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("level_progress_count").assertTextEquals("999 / 1,000 lines")
        composeTestRule.onNodeWithTag("level_progress_percent").assertTextEquals("99.9%")

        // Clear the 1,000th line via real touch drag & drop!
        dragSlotByTouch(slotIndex = 0, shape = dot, targetRow = 0, targetCol = 9)

        composeTestRule.onNodeWithTag("level_progress_count").assertTextEquals("1,000 / 1,000 lines")
        composeTestRule.onNodeWithTag("level_progress_percent").assertTextEquals("100%")
        composeTestRule.onNodeWithText("LEVEL 1 COMPLETE!").assertIsDisplayed()

        // Tap "Play Level 2" -> unlocks Level 2 and resets Game Progress bar cleanly
        composeTestRule.onNodeWithTag("next_level_button").performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("game_mode_title").assertTextEquals("Level 2 / 100")
        composeTestRule.onNodeWithTag("level_progress_count").assertTextEquals("0 / 1,000 lines")
        composeTestRule.onNodeWithTag("level_progress_percent").assertTextEquals("0%")
        composeTestRule.onNodeWithTag("game_progress_count").assertTextEquals("0 / 20 lines")
        composeTestRule.onNodeWithTag("game_progress_percent").assertTextEquals("0%")
    }
}
