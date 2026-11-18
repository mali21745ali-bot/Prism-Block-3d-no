package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Exact square pixel geometry of the rendered 10x10 board in root window coordinates.
 * Mirrors the centered square logic inside Board10x10Canvas so pointer-to-grid math
 * matches rendered cell boundaries pixel-for-pixel.
 */
data class BoardGeometry(
    val left: Float,
    val top: Float,
    val sizePx: Float
) {
    val right: Float get() = left + sizePx
    val bottom: Float get() = top + sizePx
    val cellPx: Float get() = if (sizePx > 0f) sizePx / BOARD_SIZE.toFloat() else 0f
    val isValid: Boolean get() = sizePx > 10f

    fun cellCenter(row: Int, col: Int): Offset = Offset(
        x = left + (col + 0.5f) * cellPx,
        y = top + (row + 0.5f) * cellPx
    )

    fun pieceCenter(shape: PolyominoShape, anchorRow: Int, anchorCol: Int): Offset = Offset(
        x = left + (anchorCol + shape.cols / 2f) * cellPx,
        y = top + (anchorRow + shape.rows / 2f) * cellPx
    )

    companion object {
        fun fromBounds(bounds: Rect): BoardGeometry {
            val boardPx = minOf(bounds.width, bounds.height)
            if (boardPx <= 0f) return BoardGeometry(0f, 0f, 0f)
            val startX = bounds.left + (bounds.width - boardPx) / 2f
            val startY = bounds.top + (bounds.height - boardPx) / 2f
            return BoardGeometry(left = startX, top = startY, sizePx = boardPx)
        }
    }
}

data class SnapPreview(
    val anchorRow: Int,
    val anchorCol: Int,
    val isValid: Boolean
)

/**
 * Pure coordinate-to-grid snapping engine used by both live drag previews and pointer-up placement.
 * Works identically for mobile touch (with thumb lift offset) and desktop mouse (zero lift offset).
 */
object BlockDragCalculator {

    private const val EDGE_FORGIVENESS_CELLS = 0.45f
    private const val MAGNETIC_RADIUS_CELLS = 0.62f

    fun computeSnapTarget(
        grid: List<Int>,
        shape: PolyominoShape,
        pointerInRoot: Offset,
        liftPx: Float,
        board: BoardGeometry
    ): SnapPreview? {
        if (!board.isValid) return null
        val cellPx = board.cellPx
        val pieceW = shape.cols * cellPx
        val pieceH = shape.rows * cellPx

        val liftedCenterX = pointerInRoot.x
        val liftedCenterY = pointerInRoot.y - liftPx
        val pieceLeft = liftedCenterX - pieceW / 2f
        val pieceTop = liftedCenterY - pieceH / 2f
        val pieceRight = pieceLeft + pieceW
        val pieceBottom = pieceTop + pieceH

        // If the dragged piece does not overlap the 10x10 board area at all, show no ghost preview
        val margin = cellPx * 0.35f
        if (pieceRight < board.left - margin ||
            pieceLeft > board.right + margin ||
            pieceBottom < board.top - margin ||
            pieceTop > board.bottom + margin
        ) {
            return null
        }

        val exactCol = (pieceLeft - board.left) / cellPx
        val exactRow = (pieceTop - board.top) / cellPx
        val roundedRow = exactRow.roundToInt()
        val roundedCol = exactCol.roundToInt()

        // 1. Exact rounded cell check first
        if (BlockPuzzleEngine.canPlace(grid, shape, roundedRow, roundedCol)) {
            return SnapPreview(anchorRow = roundedRow, anchorCol = roundedCol, isValid = true)
        }

        // 2. If the piece is clearly outside the 10x10 board bounds, reject strictly (show red preview)
        val outOfBoundsTooFar = exactRow < -EDGE_FORGIVENESS_CELLS ||
            exactCol < -EDGE_FORGIVENESS_CELLS ||
            (exactRow + shape.rows) > (BOARD_SIZE + EDGE_FORGIVENESS_CELLS) ||
            (exactCol + shape.cols) > (BOARD_SIZE + EDGE_FORGIVENESS_CELLS)

        if (!outOfBoundsTooFar) {
            // Check immediate straddled cell candidates within magnetic radius so edge/seam drops feel crisp
            val minR = floor(exactRow).toInt().coerceIn(0, BOARD_SIZE - shape.rows)
            val maxR = ceil(exactRow).toInt().coerceIn(0, BOARD_SIZE - shape.rows)
            val minC = floor(exactCol).toInt().coerceIn(0, BOARD_SIZE - shape.cols)
            val maxC = ceil(exactCol).toInt().coerceIn(0, BOARD_SIZE - shape.cols)

            var bestRow = -1
            var bestCol = -1
            var bestDist = Float.MAX_VALUE
            for (r in minR..maxR) {
                for (c in minC..maxC) {
                    if (BlockPuzzleEngine.canPlace(grid, shape, r, c)) {
                        val dist = hypot((r - exactRow).toDouble(), (c - exactCol).toDouble()).toFloat()
                        if (dist <= MAGNETIC_RADIUS_CELLS && dist < bestDist) {
                            bestDist = dist
                            bestRow = r
                            bestCol = c
                        }
                    }
                }
            }
            if (bestRow >= 0 && bestCol >= 0) {
                return SnapPreview(anchorRow = bestRow, anchorCol = bestCol, isValid = true)
            }
        }

        // 3. Otherwise show invalid (red) preview at rounded coordinates so user sees why placement is blocked
        return SnapPreview(anchorRow = roundedRow, anchorCol = roundedCol, isValid = false)
    }

    /**
     * Computes top-left pixel offset of the floating 3D piece inside the unpadded overlay container.
     */
    fun computeFloatingOverlayTopLeft(
        pointerInRoot: Offset,
        liftPx: Float,
        shape: PolyominoShape,
        board: BoardGeometry,
        overlayOriginInRoot: Offset
    ): Offset {
        val widthPx = shape.cols * board.cellPx
        val heightPx = shape.rows * board.cellPx
        val localX = (pointerInRoot.x - overlayOriginInRoot.x) - widthPx / 2f
        val localY = (pointerInRoot.y - overlayOriginInRoot.y - liftPx) - heightPx / 2f
        return Offset(localX, localY)
    }

    /**
     * Computes anchor row/col when using tap-to-place fallback on the 10x10 board.
     */
    fun computeTapAnchor(
        tapOffsetInCanvas: Offset,
        canvasWidth: Float,
        canvasHeight: Float,
        shape: PolyominoShape
    ): GridCell? {
        val boardPx = minOf(canvasWidth, canvasHeight)
        if (boardPx <= 10f) return null
        val startX = (canvasWidth - boardPx) / 2f
        val startY = (canvasHeight - boardPx) / 2f
        val cellPx = boardPx / BOARD_SIZE.toFloat()

        val relX = tapOffsetInCanvas.x - startX
        val relY = tapOffsetInCanvas.y - startY
        if (relX < 0f || relY < 0f || relX > boardPx || relY > boardPx) return null

        val col = ((relX / cellPx) - shape.cols / 2f).roundToInt()
        val row = ((relY / cellPx) - shape.rows / 2f).roundToInt()
        if (abs(row) > BOARD_SIZE || abs(col) > BOARD_SIZE) return null
        return GridCell(row = row, col = col)
    }
}
