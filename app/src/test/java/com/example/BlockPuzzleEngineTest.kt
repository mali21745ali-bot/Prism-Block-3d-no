package com.example

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.example.game.BOARD_SIZE
import com.example.game.BlockDragCalculator
import com.example.game.BlockPuzzleEngine
import com.example.game.BoardGeometry
import com.example.game.GridCell
import com.example.game.PolyominoShape
import com.example.game.TrayPiece
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class BlockPuzzleEngineTest {

    private val dot = PolyominoShape("dot", "Single", listOf(GridCell(0, 0)), 1)
    private val square2 = PolyominoShape(
        "sq2",
        "2x2 Square",
        listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 0), GridCell(1, 1)),
        2
    )

    @Test
    fun collisionAndBoundsValidationWorks() {
        val empty = BlockPuzzleEngine.emptyGrid()
        assertTrue(BlockPuzzleEngine.canPlace(empty, square2, 0, 0))
        assertTrue(BlockPuzzleEngine.canPlace(empty, square2, 8, 8))
        assertFalse(BlockPuzzleEngine.canPlace(empty, square2, 9, 8))
        assertFalse(BlockPuzzleEngine.canPlace(empty, square2, -1, 0))

        val placed = BlockPuzzleEngine.placeBlock(empty, square2, 0, 0)
        assertNotNull(placed)
        assertFalse(BlockPuzzleEngine.canPlace(placed!!.newGrid, dot, 0, 0))
        assertNull(BlockPuzzleEngine.placeBlock(placed.newGrid, square2, 1, 1))
    }

    @Test
    fun simultaneousRowAndColumnClearAwardsExactScoreAndCoins() {
        val grid = BlockPuzzleEngine.emptyGrid().toMutableList()
        for (c in 1 until BOARD_SIZE) grid[BlockPuzzleEngine.index(0, c)] = 3
        for (r in 1 until BOARD_SIZE) grid[BlockPuzzleEngine.index(r, 0)] = 4

        val outcome = BlockPuzzleEngine.placeBlock(grid, dot, 0, 0)
        assertNotNull(outcome)
        assertEquals(2, outcome!!.linesCleared)
        assertEquals(20L, outcome.scoreGained)
        assertEquals(2L, outcome.coinsGained)
        for (c in 0 until BOARD_SIZE) assertEquals(0, outcome.newGrid[BlockPuzzleEngine.index(0, c)])
        for (r in 0 until BOARD_SIZE) assertEquals(0, outcome.newGrid[BlockPuzzleEngine.index(r, 0)])
    }

    @Test
    fun gameOverDetectedWhenNoTrayPieceFitsAndContinueRescuesBoard() {
        val fullGrid = List(BOARD_SIZE * BOARD_SIZE) { 2 }
        val tray = listOf(TrayPiece(0, square2, false))
        assertFalse(BlockPuzzleEngine.hasAnyValidMove(fullGrid, tray))

        val (rescued, freshTray) = BlockPuzzleEngine.applyContinueRescue(fullGrid, Random(42))
        assertEquals(3, freshTray.size)
        assertTrue(BlockPuzzleEngine.hasAnyValidMove(rescued, freshTray))
    }

    @Test
    fun all27ShapesSnapAndPlaceAccuratelyForMobileTouchAndDesktopMouse() {
        val board = BoardGeometry.fromBounds(Rect(left = 40f, top = 200f, right = 1040f, bottom = 1200f))
        assertEquals(100f, board.cellPx, 0.01f)
        val allShapes = BlockPuzzleEngine.allShapeTemplates()
        assertEquals(27, allShapes.size)

        val touchLiftPx = 192f
        val mouseLiftPx = 0f

        for (shape in allShapes) {
            val emptyGrid = BlockPuzzleEngine.emptyGrid()
            val targetRow = (BOARD_SIZE - shape.rows) / 2
            val targetCol = (BOARD_SIZE - shape.cols) / 2
            val desiredCenter = board.pieceCenter(shape, targetRow, targetCol)

            // Mobile touch (pointer is below piece center by touchLiftPx)
            val touchPointer = Offset(desiredCenter.x, desiredCenter.y + touchLiftPx)
            val touchSnap = BlockDragCalculator.computeSnapTarget(emptyGrid, shape, touchPointer, touchLiftPx, board)
            assertNotNull("Touch snap should exist for ${shape.id}", touchSnap)
            assertTrue("Touch snap should be valid for ${shape.id}", touchSnap!!.isValid)
            assertEquals(targetRow, touchSnap.anchorRow)
            assertEquals(targetCol, touchSnap.anchorCol)

            // Desktop mouse (pointer is directly at piece center, liftPx == 0)
            val mouseSnap = BlockDragCalculator.computeSnapTarget(emptyGrid, shape, desiredCenter, mouseLiftPx, board)
            assertNotNull("Mouse snap should exist for ${shape.id}", mouseSnap)
            assertTrue("Mouse snap should be valid for ${shape.id}", mouseSnap!!.isValid)
            assertEquals(targetRow, mouseSnap.anchorRow)
            assertEquals(targetCol, mouseSnap.anchorCol)

            // Verify actual grid placement places exact cells
            val outcome = BlockPuzzleEngine.placeBlock(emptyGrid, shape, targetRow, targetCol)
            assertNotNull("Placement must succeed for ${shape.id}", outcome)
            for (cell in shape.cells) {
                val idx = BlockPuzzleEngine.index(targetRow + cell.row, targetCol + cell.col)
                assertEquals(shape.colorIndex, outcome!!.newGrid[idx])
            }

            // Verify overlapping placement on the same cells is strictly rejected
            val overlapSnap = BlockDragCalculator.computeSnapTarget(outcome!!.newGrid, shape, desiredCenter, mouseLiftPx, board)
            assertNotNull(overlapSnap)
            assertFalse("Overlap must be rejected for ${shape.id}", overlapSnap!!.isValid)

            // Verify floating overlay top-left aligns with board grid cells
            val overlayTopLeft = BlockDragCalculator.computeFloatingOverlayTopLeft(
                pointerInRoot = touchPointer,
                liftPx = touchLiftPx,
                shape = shape,
                board = board,
                overlayOriginInRoot = Offset.Zero
            )
            val expectedTopLeftX = board.left + targetCol * board.cellPx
            val expectedTopLeftY = board.top + targetRow * board.cellPx
            assertEquals(expectedTopLeftX, overlayTopLeft.x, 0.01f)
            assertEquals(expectedTopLeftY, overlayTopLeft.y, 0.01f)
        }
    }

    @Test
    fun outOfBoundsAndTrayDropsAreRejected() {
        val board = BoardGeometry.fromBounds(Rect(left = 40f, top = 200f, right = 1040f, bottom = 1200f))
        val emptyGrid = BlockPuzzleEngine.emptyGrid()

        // Pointer staying down in bottom tray -> returns null (no board snap)
        val inTray = BlockDragCalculator.computeSnapTarget(
            grid = emptyGrid,
            shape = square2,
            pointerInRoot = Offset(540f, 1550f),
            liftPx = 120f,
            board = board
        )
        assertNull(inTray)

        // Pointer dragging 2x2 square 1.5 cells past right edge -> strictly invalid
        val pastRightEdge = BlockDragCalculator.computeSnapTarget(
            grid = emptyGrid,
            shape = square2,
            pointerInRoot = Offset(board.right + board.cellPx * 0.6f, board.top + board.cellPx * 3f),
            liftPx = 0f,
            board = board
        )
        if (pastRightEdge != null) {
            assertFalse(pastRightEdge.isValid)
        }
    }
}
