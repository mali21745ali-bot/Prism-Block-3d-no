package com.example

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.MouseButton
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import com.example.game.BOARD_SIZE
import com.example.game.BlockPuzzleEngine
import com.example.game.BlockThemes
import com.example.game.BoardGeometry
import com.example.game.GridCell
import com.example.game.PolyominoShape
import com.example.game.TrayPiece
import com.example.ui.game.GameplayBoardStage
import com.example.ui.theme.PrismBlockTheme
import com.example.viewmodel.GamePlayUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.random.Random

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class BlockDragAndDropComposeTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dot = PolyominoShape("dot_1", "dot_1", listOf(GridCell(0, 0)), 1)
    private val square2 = PolyominoShape(
        "sq_2x2",
        "sq_2x2",
        listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 0), GridCell(1, 1)),
        2
    )
    private val square3 = PolyominoShape(
        "sq_3x3",
        "sq_3x3",
        listOf(
            GridCell(0, 0), GridCell(0, 1), GridCell(0, 2),
            GridCell(1, 0), GridCell(1, 1), GridCell(1, 2),
            GridCell(2, 0), GridCell(2, 1), GridCell(2, 2)
        ),
        3
    )

    private fun applyRealMove(state: GamePlayUiState, slotIndex: Int, anchorRow: Int, anchorCol: Int): Pair<Boolean, GamePlayUiState> {
        val piece = state.tray.firstOrNull { it.slotIndex == slotIndex && !it.used } ?: return false to state
        val outcome = BlockPuzzleEngine.placeBlock(state.grid, piece.shape, anchorRow, anchorCol) ?: return false to state
        val markedTray = state.tray.map { if (it.slotIndex == slotIndex) it.copy(used = true) else it }
        val nextTray = if (markedTray.all { it.used }) BlockPuzzleEngine.generateTray(Random(1234)) else markedTray
        val hasMoves = BlockPuzzleEngine.hasAnyValidMove(outcome.newGrid, nextTray)
        return true to state.copy(
            grid = outcome.newGrid,
            tray = nextTray,
            score = state.score + outcome.scoreGained,
            linesCleared = state.linesCleared + outcome.linesCleared,
            coinsEarned = state.coinsEarned + outcome.coinsGained,
            isGameOver = !hasMoves
        )
    }

    private fun touchLiftPx(): Float = 64f * composeTestRule.density.density

    private fun dragPieceByTouch(slotIndex: Int, shape: PolyominoShape, targetRow: Int, targetCol: Int, cancelInsteadOfUp: Boolean = false) {
        val boardBounds = composeTestRule.onNodeWithTag("puzzle_board_canvas").fetchSemanticsNode().boundsInRoot
        val trayBounds = composeTestRule.onNodeWithTag("tray_piece_$slotIndex").fetchSemanticsNode().boundsInRoot
        val boardGeom = BoardGeometry.fromBounds(boardBounds)

        val desiredPieceCenterInRoot = boardGeom.pieceCenter(shape, targetRow, targetCol)
        // Mobile touch lifts the piece upward by 64dp so pointer sits 64dp below desired piece center
        val pointerTargetInRoot = Offset(desiredPieceCenterInRoot.x, desiredPieceCenterInRoot.y + touchLiftPx())
        val pointerTargetLocal = pointerTargetInRoot - trayBounds.topLeft

        composeTestRule.onNodeWithTag("tray_piece_$slotIndex").performTouchInput {
            down(center)
            // Drag smoothly in intermediate steps across the screen
            val mid = Offset((center.x + pointerTargetLocal.x) / 2f, (center.y + pointerTargetLocal.y) / 2f)
            moveTo(mid)
            moveTo(pointerTargetLocal)
            if (cancelInsteadOfUp) {
                cancel()
            } else {
                up()
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun dragPieceByMouse(slotIndex: Int, shape: PolyominoShape, targetRow: Int, targetCol: Int) {
        val boardBounds = composeTestRule.onNodeWithTag("puzzle_board_canvas").fetchSemanticsNode().boundsInRoot
        val trayBounds = composeTestRule.onNodeWithTag("tray_piece_$slotIndex").fetchSemanticsNode().boundsInRoot
        val boardGeom = BoardGeometry.fromBounds(boardBounds)

        // Desktop mouse uses zero finger lift offset -> cursor sits directly at piece center
        val pointerTargetInRoot = boardGeom.pieceCenter(shape, targetRow, targetCol)
        val pointerTargetLocal = pointerTargetInRoot - trayBounds.topLeft

        composeTestRule.onNodeWithTag("tray_piece_$slotIndex").performMouseInput {
            moveTo(center)
            press(MouseButton.Primary)
            val mid = Offset((center.x + pointerTargetLocal.x) / 2f, (center.y + pointerTargetLocal.y) / 2f)
            moveTo(mid)
            moveTo(pointerTargetLocal)
            release(MouseButton.Primary)
        }
        composeTestRule.waitForIdle()
    }

    @Test
    fun mobileTouchDragAndDropPlacesBlockAndShowsLiveFloatingPreview() {
        var uiState by mutableStateOf(
            GamePlayUiState(
                grid = BlockPuzzleEngine.emptyGrid(),
                tray = listOf(
                    TrayPiece(0, square2, false),
                    TrayPiece(1, dot, false),
                    TrayPiece(2, dot, false)
                )
            )
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = uiState,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { slot, r, c ->
                        val (ok, next) = applyRealMove(uiState, slot, r, c)
                        if (ok) uiState = next
                        ok
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        val boardBounds = composeTestRule.onNodeWithTag("puzzle_board_canvas").fetchSemanticsNode().boundsInRoot
        val trayBounds = composeTestRule.onNodeWithTag("tray_piece_0").fetchSemanticsNode().boundsInRoot
        val boardGeom = BoardGeometry.fromBounds(boardBounds)
        val desiredCenter = boardGeom.pieceCenter(square2, 4, 4)
        val targetLocal = Offset(desiredCenter.x, desiredCenter.y + touchLiftPx()) - trayBounds.topLeft

        // Hold pointer down and move over board -> live floating 3D piece must be visible
        composeTestRule.onNodeWithTag("tray_piece_0").performTouchInput {
            down(center)
            moveTo(targetLocal)
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("floating_drag_piece").assertIsDisplayed()

        // Release pointer -> block snaps into (4,4)..(5,5) and is removed from tray
        composeTestRule.onNodeWithTag("tray_piece_0").performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        assertTrue(uiState.tray[0].used)
        assertFalse(uiState.tray[1].used)
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(4, 4)])
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(4, 5)])
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(5, 4)])
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(5, 5)])
    }

    @Test
    fun desktopMouseDragAndDropPlacesBlockAccurately() {
        var uiState by mutableStateOf(
            GamePlayUiState(
                grid = BlockPuzzleEngine.emptyGrid(),
                tray = listOf(
                    TrayPiece(0, square2, false),
                    TrayPiece(1, dot, false),
                    TrayPiece(2, dot, false)
                )
            )
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = uiState,
                    activeTheme = BlockThemes.pastelMacaron,
                    onTryPlacePiece = { slot, r, c ->
                        val (ok, next) = applyRealMove(uiState, slot, r, c)
                        if (ok) uiState = next
                        ok
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        dragPieceByMouse(slotIndex = 0, shape = square2, targetRow = 2, targetCol = 6)

        assertTrue(uiState.tray[0].used)
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(2, 6)])
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(2, 7)])
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(3, 6)])
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(3, 7)])
    }

    @Test
    fun invalidOverlapOutOfBoundsAndPointerCancelReturnBlockToTray() {
        val occupiedGrid = BlockPuzzleEngine.emptyGrid().toMutableList().apply {
            this[BlockPuzzleEngine.index(4, 4)] = 5
        }
        var uiState by mutableStateOf(
            GamePlayUiState(
                grid = occupiedGrid,
                tray = listOf(
                    TrayPiece(0, square2, false),
                    TrayPiece(1, dot, false),
                    TrayPiece(2, dot, false)
                )
            )
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = uiState,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { slot, r, c ->
                        val (ok, next) = applyRealMove(uiState, slot, r, c)
                        if (ok) uiState = next
                        ok
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Overlapping occupied cell (4,4) -> rejected, piece stays in tray
        dragPieceByTouch(slotIndex = 0, shape = square2, targetRow = 4, targetCol = 4)
        assertFalse(uiState.tray[0].used)
        assertEquals(0, uiState.grid[BlockPuzzleEngine.index(5, 5)])

        // 2. Pointer cancel over a VALID empty spot (0,0) -> rejected, piece stays in tray
        dragPieceByTouch(slotIndex = 0, shape = square2, targetRow = 0, targetCol = 0, cancelInsteadOfUp = true)
        assertFalse(uiState.tray[0].used)
        assertEquals(0, uiState.grid[BlockPuzzleEngine.index(0, 0)])

        // 3. Releasing back inside tray / off-grid -> rejected, piece stays in tray
        composeTestRule.onNodeWithTag("tray_piece_0").performTouchInput {
            down(center)
            moveTo(Offset(center.x + 80f, center.y))
            up()
        }
        composeTestRule.waitForIdle()
        assertFalse(uiState.tray[0].used)

        // 4. Finally drop onto open cells (0,0) -> succeeds!
        dragPieceByTouch(slotIndex = 0, shape = square2, targetRow = 0, targetCol = 0)
        assertTrue(uiState.tray[0].used)
        assertEquals(square2.colorIndex, uiState.grid[BlockPuzzleEngine.index(0, 0)])
    }

    @Test
    fun placingAll3BlocksClearsLineAwardsScoreAndCoinsAndRefillsTray() {
        val grid = BlockPuzzleEngine.emptyGrid().toMutableList()
        for (c in 0 until 7) grid[BlockPuzzleEngine.index(0, c)] = 4

        var uiState by mutableStateOf(
            GamePlayUiState(
                grid = grid,
                tray = listOf(
                    TrayPiece(0, dot.copy(id = "d0"), false),
                    TrayPiece(1, dot.copy(id = "d1"), false),
                    TrayPiece(2, dot.copy(id = "d2"), false)
                )
            )
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = uiState,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { slot, r, c ->
                        val (ok, next) = applyRealMove(uiState, slot, r, c)
                        if (ok) uiState = next
                        ok
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        dragPieceByTouch(0, dot, 0, 7)
        assertTrue(uiState.tray[0].used)
        assertEquals(0L, uiState.linesCleared)

        dragPieceByTouch(1, dot, 0, 8)
        assertTrue(uiState.tray[1].used)
        assertEquals(0L, uiState.linesCleared)

        // Third drop completes row 0 AND empties tray -> triggers line clear + 3 fresh blocks
        dragPieceByTouch(2, dot, 0, 9)

        assertEquals(1L, uiState.linesCleared)
        assertEquals(10L, uiState.score)
        assertEquals(1L, uiState.coinsEarned)
        for (c in 0 until BOARD_SIZE) {
            assertEquals("Row 0 cell $c must be cleared", 0, uiState.grid[BlockPuzzleEngine.index(0, c)])
        }
        assertEquals(3, uiState.tray.size)
        assertTrue("All 3 tray blocks must regenerate unused", uiState.tray.all { !it.used })
    }

    @Test
    fun dragAndDropTriggersGameOverWhenRemainingBlockCannotFit() {
        // Checkerboard grid: (0,0) is empty for dot, no line clears on drop, and no 3x3 can fit anywhere
        val grid = MutableList(BOARD_SIZE * BOARD_SIZE) { idx ->
            val r = idx / BOARD_SIZE
            val c = idx % BOARD_SIZE
            if ((r + c) % 2 == 0) 0 else 3
        }

        var uiState by mutableStateOf(
            GamePlayUiState(
                grid = grid,
                tray = listOf(
                    TrayPiece(0, dot, false),
                    TrayPiece(1, square3, false),
                    TrayPiece(2, square3, false)
                )
            )
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = uiState,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { slot, r, c ->
                        val (ok, next) = applyRealMove(uiState, slot, r, c)
                        if (ok) uiState = next
                        ok
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        dragPieceByTouch(0, dot, 0, 0)

        assertTrue(uiState.isGameOver)
        composeTestRule.onNodeWithText("GAME OVER").assertIsDisplayed()
    }

    @Test
    fun allDistinctPolyominoShapeFamiliesDragAndSnapCleanlyInUi() {
        val sampleShapes = BlockPuzzleEngine.allShapeTemplates().filter {
            it.id in setOf("dot_1", "line_h5", "line_v4", "sq_3x3", "l_lg_br", "t_up", "rect_2x3", "z_h")
        }
        assertEquals(8, sampleShapes.size)

        var uiState by mutableStateOf(
            GamePlayUiState(
                grid = BlockPuzzleEngine.emptyGrid(),
                tray = listOf(TrayPiece(0, sampleShapes.first(), false))
            )
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = uiState,
                    activeTheme = BlockThemes.simpleClassic,
                    onTryPlacePiece = { slot, r, c ->
                        val (ok, next) = applyRealMove(uiState, slot, r, c)
                        if (ok) uiState = next
                        ok
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        for (shape in sampleShapes) {
            uiState = GamePlayUiState(
                grid = BlockPuzzleEngine.emptyGrid(),
                tray = listOf(
                    TrayPiece(0, shape, false),
                    TrayPiece(1, dot, false),
                    TrayPiece(2, dot, false)
                )
            )
            composeTestRule.waitForIdle()

            val targetRow = (BOARD_SIZE - shape.rows) / 2
            val targetCol = (BOARD_SIZE - shape.cols) / 2
            dragPieceByTouch(slotIndex = 0, shape = shape, targetRow = targetRow, targetCol = targetCol)

            assertTrue("Shape ${shape.id} must be marked used after drop", uiState.tray[0].used)
            for (cell in shape.cells) {
                val idx = BlockPuzzleEngine.index(targetRow + cell.row, targetCol + cell.col)
                assertEquals(
                    "Shape ${shape.id} cell (${targetRow + cell.row}, ${targetCol + cell.col}) must be occupied",
                    shape.colorIndex,
                    uiState.grid[idx]
                )
            }
        }
    }
}
