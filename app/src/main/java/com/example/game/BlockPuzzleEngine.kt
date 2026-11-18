package com.example.game

import kotlin.random.Random

const val BOARD_SIZE = 10

data class GridCell(val row: Int, val col: Int)

data class PolyominoShape(
    val id: String,
    val name: String,
    val cells: List<GridCell>,
    val colorIndex: Int
) {
    val rows: Int = (cells.maxOfOrNull { it.row } ?: 0) + 1
    val cols: Int = (cells.maxOfOrNull { it.col } ?: 0) + 1
}

data class TrayPiece(
    val slotIndex: Int,
    val shape: PolyominoShape,
    val used: Boolean = false
)

data class PlacementOutcome(
    val newGrid: List<Int>,
    val clearedRows: List<Int>,
    val clearedCols: List<Int>,
    val clearedCellIndices: Set<Int>,
    val linesCleared: Int,
    val scoreGained: Long,
    val coinsGained: Long
)

object BlockPuzzleEngine {

    private val shapeTemplates: List<Pair<String, List<GridCell>>> = listOf(
        // Single block
        "dot_1" to listOf(GridCell(0, 0)),
        // 2-cell lines
        "line_h2" to listOf(GridCell(0, 0), GridCell(0, 1)),
        "line_v2" to listOf(GridCell(0, 0), GridCell(1, 0)),
        // 3-cell lines
        "line_h3" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2)),
        "line_v3" to listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0)),
        // 4-cell lines
        "line_h4" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2), GridCell(0, 3)),
        "line_v4" to listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(3, 0)),
        // 5-cell lines
        "line_h5" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2), GridCell(0, 3), GridCell(0, 4)),
        "line_v5" to listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(3, 0), GridCell(4, 0)),
        // Square blocks
        "sq_2x2" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 0), GridCell(1, 1)),
        "sq_3x3" to listOf(
            GridCell(0, 0), GridCell(0, 1), GridCell(0, 2),
            GridCell(1, 0), GridCell(1, 1), GridCell(1, 2),
            GridCell(2, 0), GridCell(2, 1), GridCell(2, 2)
        ),
        // Small L-shapes (3 cells)
        "l_sm_br" to listOf(GridCell(0, 0), GridCell(1, 0), GridCell(1, 1)),
        "l_sm_bl" to listOf(GridCell(0, 1), GridCell(1, 0), GridCell(1, 1)),
        "l_sm_tr" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 0)),
        "l_sm_tl" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 1)),
        // Large 3x3 L-shapes (5 cells)
        "l_lg_br" to listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(2, 1), GridCell(2, 2)),
        "l_lg_bl" to listOf(GridCell(0, 2), GridCell(1, 2), GridCell(2, 0), GridCell(2, 1), GridCell(2, 2)),
        "l_lg_tr" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2), GridCell(1, 0), GridCell(2, 0)),
        "l_lg_tl" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2), GridCell(1, 2), GridCell(2, 2)),
        // T-shapes (4 cells)
        "t_down" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2), GridCell(1, 1)),
        "t_up" to listOf(GridCell(0, 1), GridCell(1, 0), GridCell(1, 1), GridCell(1, 2)),
        "t_right" to listOf(GridCell(0, 0), GridCell(1, 0), GridCell(2, 0), GridCell(1, 1)),
        "t_left" to listOf(GridCell(0, 1), GridCell(1, 0), GridCell(1, 1), GridCell(2, 1)),
        // Rectangles (2x3 / 3x2)
        "rect_2x3" to listOf(
            GridCell(0, 0), GridCell(0, 1), GridCell(0, 2),
            GridCell(1, 0), GridCell(1, 1), GridCell(1, 2)
        ),
        "rect_3x2" to listOf(
            GridCell(0, 0), GridCell(0, 1),
            GridCell(1, 0), GridCell(1, 1),
            GridCell(2, 0), GridCell(2, 1)
        ),
        // Standard Z / S tetrominoes
        "z_h" to listOf(GridCell(0, 0), GridCell(0, 1), GridCell(1, 1), GridCell(1, 2)),
        "s_h" to listOf(GridCell(0, 1), GridCell(0, 2), GridCell(1, 0), GridCell(1, 1))
    )

    fun emptyGrid(): List<Int> = List(BOARD_SIZE * BOARD_SIZE) { 0 }

    fun index(row: Int, col: Int): Int = row * BOARD_SIZE + col

    fun allShapeTemplates(): List<PolyominoShape> = shapeTemplates.mapIndexed { i, (templateId, cells) ->
        PolyominoShape(
            id = templateId,
            name = templateId,
            cells = cells,
            colorIndex = (i % 7) + 1
        )
    }

    fun generateTray(random: Random = Random.Default, colorCount: Int = 7): List<TrayPiece> {
        return List(3) { slot ->
            val (templateId, cells) = shapeTemplates[random.nextInt(shapeTemplates.size)]
            val colorIndex = random.nextInt(1, colorCount + 1)
            TrayPiece(
                slotIndex = slot,
                shape = PolyominoShape(
                    id = "${templateId}_${slot}_${random.nextInt(100_000)}",
                    name = templateId,
                    cells = cells,
                    colorIndex = colorIndex
                ),
                used = false
            )
        }
    }

    /**
     * Strict placement validation:
     * - Every cell must remain strictly inside 0 until BOARD_SIZE
     * - No cell may overlap an already-occupied grid cell
     * - Rotation is never allowed
     */
    fun canPlace(grid: List<Int>, shape: PolyominoShape, anchorRow: Int, anchorCol: Int): Boolean {
        for (cell in shape.cells) {
            val r = anchorRow + cell.row
            val c = anchorCol + cell.col
            if (r !in 0 until BOARD_SIZE || c !in 0 until BOARD_SIZE) return false
            if (grid[index(r, c)] != 0) return false
        }
        return true
    }

    /**
     * Places [shape] at ([anchorRow], [anchorCol]), detects ALL completed horizontal rows and
     * vertical columns simultaneously, clears them, and computes exact score & coin rewards:
     * 1 cleared line = +10 Score and +1 Coin.
     */
    fun placeBlock(
        grid: List<Int>,
        shape: PolyominoShape,
        anchorRow: Int,
        anchorCol: Int
    ): PlacementOutcome? {
        if (!canPlace(grid, shape, anchorRow, anchorCol)) return null

        val mutable = grid.toMutableList()
        for (cell in shape.cells) {
            mutable[index(anchorRow + cell.row, anchorCol + cell.col)] = shape.colorIndex
        }

        val completedRows = mutableListOf<Int>()
        for (r in 0 until BOARD_SIZE) {
            var full = true
            for (c in 0 until BOARD_SIZE) {
                if (mutable[index(r, c)] == 0) {
                    full = false
                    break
                }
            }
            if (full) completedRows.add(r)
        }

        val completedCols = mutableListOf<Int>()
        for (c in 0 until BOARD_SIZE) {
            var full = true
            for (r in 0 until BOARD_SIZE) {
                if (mutable[index(r, c)] == 0) {
                    full = false
                    break
                }
            }
            if (full) completedCols.add(c)
        }

        val clearedIndices = mutableSetOf<Int>()
        for (r in completedRows) {
            for (c in 0 until BOARD_SIZE) {
                val idx = index(r, c)
                clearedIndices.add(idx)
                mutable[idx] = 0
            }
        }
        for (c in completedCols) {
            for (r in 0 until BOARD_SIZE) {
                val idx = index(r, c)
                clearedIndices.add(idx)
                mutable[idx] = 0
            }
        }

        val totalLinesCleared = completedRows.size + completedCols.size
        val scoreGained = totalLinesCleared * 10L
        val coinsGained = totalLinesCleared.toLong()

        return PlacementOutcome(
            newGrid = mutable.toList(),
            clearedRows = completedRows,
            clearedCols = completedCols,
            clearedCellIndices = clearedIndices,
            linesCleared = totalLinesCleared,
            scoreGained = scoreGained,
            coinsGained = coinsGained
        )
    }

    /**
     * Returns true if at least one unused piece in [tray] can fit somewhere on [grid].
     * When false, Game Over is triggered.
     */
    fun hasAnyValidMove(grid: List<Int>, tray: List<TrayPiece>): Boolean {
        val activePieces = tray.filter { !it.used }
        if (activePieces.isEmpty()) return true
        for (piece in activePieces) {
            for (r in 0 until BOARD_SIZE) {
                for (c in 0 until BOARD_SIZE) {
                    if (canPlace(grid, piece.shape, r, c)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    /**
     * Computes which rows and columns WOULD complete if [shape] were placed at ([anchorRow], [anchorCol]),
     * used for live highlight previews during drag-and-drop!
     */
    fun previewPotentialClears(
        grid: List<Int>,
        shape: PolyominoShape,
        anchorRow: Int,
        anchorCol: Int
    ): Set<Int> {
        if (!canPlace(grid, shape, anchorRow, anchorCol)) return emptySet()
        val temp = grid.toMutableList()
        for (cell in shape.cells) {
            temp[index(anchorRow + cell.row, anchorCol + cell.col)] = shape.colorIndex
        }
        val highlighted = mutableSetOf<Int>()
        for (r in 0 until BOARD_SIZE) {
            if ((0 until BOARD_SIZE).all { c -> temp[index(r, c)] != 0 }) {
                for (c in 0 until BOARD_SIZE) highlighted.add(index(r, c))
            }
        }
        for (c in 0 until BOARD_SIZE) {
            if ((0 until BOARD_SIZE).all { r -> temp[index(r, c)] != 0 }) {
                for (r in 0 until BOARD_SIZE) highlighted.add(index(r, c))
            }
        }
        return highlighted
    }

    /**
     * Rescue board clear after player pays Coins (or completes a verified Rewarded Ad) to continue:
     * Clears the central 4x4 region and the two fullest rows so every tray piece can resume smoothly.
     */
    fun applyContinueRescue(grid: List<Int>, random: Random = Random.Default): Pair<List<Int>, List<TrayPiece>> {
        val mutable = grid.toMutableList()
        for (r in 3..6) {
            for (c in 3..6) {
                mutable[index(r, c)] = 0
            }
        }
        // Also clear 2 most crowded rows
        val rowsByFill = (0 until BOARD_SIZE)
            .sortedByDescending { r -> (0 until BOARD_SIZE).count { c -> mutable[index(r, c)] != 0 } }
            .take(2)
        for (r in rowsByFill) {
            for (c in 0 until BOARD_SIZE) {
                mutable[index(r, c)] = 0
            }
        }
        val freshTray = generateTray(random)
        return mutable.toList() to freshTray
    }
}
