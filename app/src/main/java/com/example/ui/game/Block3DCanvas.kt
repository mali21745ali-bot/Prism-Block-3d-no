package com.example.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.BOARD_SIZE
import com.example.game.BlockColorStyle
import com.example.game.BlockPuzzleEngine
import com.example.game.BlockSkinRenderStyle
import com.example.game.BoardThemeSpec
import com.example.game.PolyominoShape

/**
 * Draws an individual tactile 3D glossy beveled puzzle cube at [topLeft] with side length [cellSize],
 * styled dynamically according to the player's equipped [blockSkinStyle] (Default Glossy, Neon, Diamond,
 * Gold, Rainbow, Crystal, Fire, Ice, Hologram 3D, Royal Emerald Exclusive, Cosmic Eclipse Exclusive).
 */
fun DrawScope.drawGlossy3DBlock(
    topLeft: Offset,
    cellSize: Float,
    style: BlockColorStyle,
    alpha: Float = 1f,
    highlightPulse: Boolean = false,
    blockSkinStyle: BlockSkinRenderStyle = BlockSkinRenderStyle.DEFAULT_GLOSSY
) {
    val pad = (cellSize * 0.055f).coerceAtLeast(1.2f)
    val x = topLeft.x + pad
    val y = topLeft.y + pad
    val w = (cellSize - pad * 2f).coerceAtLeast(4f)
    val corner = (w * 0.24f).coerceAtLeast(3f)

    // 1. Soft 3D drop shadow
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.36f * alpha),
        topLeft = Offset(x + w * 0.06f, y + w * 0.10f),
        size = Size(w, w),
        cornerRadius = CornerRadius(corner, corner)
    )

    val bevelDepth = (w * 0.11f).coerceAtLeast(2f)
    val faceW = w - bevelDepth * 0.55f
    val faceH = w - bevelDepth

    when (blockSkinStyle) {
        BlockSkinRenderStyle.NEON -> {
            // Dark obsidian neon core + glowing laser double border
            drawRoundRect(
                color = Color(0xFF0A061A).copy(alpha = 0.96f * alpha),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(style.glow.copy(alpha = 0.42f * alpha), Color.Transparent),
                    center = Offset(x + w * 0.5f, y + w * 0.5f),
                    radius = w * 0.6f
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = style.glow.copy(alpha = 0.95f * alpha),
                topLeft = Offset(x + 1.5f, y + 1.5f),
                size = Size(w - 3f, w - 3f),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = (w * 0.11f).coerceAtLeast(2f))
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.85f * alpha),
                topLeft = Offset(x + w * 0.15f, y + w * 0.15f),
                size = Size(w * 0.70f, w * 0.70f),
                cornerRadius = CornerRadius(corner * 0.6f, corner * 0.6f),
                style = Stroke(width = 1.2f)
            )
        }

        BlockSkinRenderStyle.DIAMOND -> {
            // Precision faceted diamond crystal
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE0F7FF).copy(alpha = alpha), style.base.copy(alpha = alpha), style.shadow.copy(alpha = alpha)),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner * 0.7f, corner * 0.7f)
            )
            val cx = x + w * 0.5f
            val cy = y + w * 0.5f
            val topFacet = Path().apply {
                moveTo(x + w * 0.12f, y + w * 0.12f)
                lineTo(x + w * 0.88f, y + w * 0.12f)
                lineTo(cx, cy)
                close()
            }
            val leftFacet = Path().apply {
                moveTo(x + w * 0.12f, y + w * 0.12f)
                lineTo(cx, cy)
                lineTo(x + w * 0.12f, y + w * 0.88f)
                close()
            }
            val bottomFacet = Path().apply {
                moveTo(x + w * 0.12f, y + w * 0.88f)
                lineTo(cx, cy)
                lineTo(x + w * 0.88f, y + w * 0.88f)
                close()
            }
            drawPath(topFacet, Color.White.copy(alpha = 0.55f * alpha))
            drawPath(leftFacet, style.highlight.copy(alpha = 0.35f * alpha))
            drawPath(bottomFacet, style.shadow.copy(alpha = 0.45f * alpha))
            drawCircle(Color.White.copy(alpha = 0.9f * alpha), radius = (w * 0.08f).coerceAtLeast(1.5f), center = Offset(x + w * 0.24f, y + w * 0.24f))
        }

        BlockSkinRenderStyle.GOLD -> {
            // 24K Bullion forged gold with metallic diagonal sheen + color tint accent rim
            drawRoundRect(
                color = Color(0xFF78350F).copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF8D6).copy(alpha = alpha),
                        Color(0xFFF6C644).copy(alpha = alpha),
                        Color(0xFFD97706).copy(alpha = alpha),
                        Color(0xFFFFDF73).copy(alpha = alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + faceW, y + faceH)
                ),
                topLeft = Offset(x, y),
                size = Size(faceW, faceH),
                cornerRadius = CornerRadius(corner * 0.9f, corner * 0.9f)
            )
            // Subtle inner recessed bullion frame tinted by block piece color
            drawRoundRect(
                color = style.base.copy(alpha = 0.22f * alpha),
                topLeft = Offset(x + w * 0.18f, y + w * 0.18f),
                size = Size(w * 0.60f, w * 0.60f),
                cornerRadius = CornerRadius(corner * 0.45f, corner * 0.45f)
            )
            drawRoundRect(
                color = Color(0xFFFFFBEB).copy(alpha = 0.65f * alpha),
                topLeft = Offset(x + w * 0.14f, y + w * 0.12f),
                size = Size(w * 0.56f, w * 0.14f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
        }

        BlockSkinRenderStyle.RAINBOW -> {
            // Full-spectrum diagonal prism cube
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF3B6B).copy(alpha = alpha),
                        Color(0xFFFFB703).copy(alpha = alpha),
                        Color(0xFF34D399).copy(alpha = alpha),
                        Color(0xFF38E8FF).copy(alpha = alpha),
                        Color(0xFFA855F7).copy(alpha = alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.42f * alpha),
                topLeft = Offset(x + w * 0.14f, y + w * 0.11f),
                size = Size(w * 0.55f, w * 0.16f),
                cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
            )
        }

        BlockSkinRenderStyle.CRYSTAL -> {
            // Translucent quartz crystal cube with inner rotated diamond refraction
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        style.highlight.copy(alpha = 0.85f * alpha),
                        style.base.copy(alpha = 0.72f * alpha),
                        style.shadow.copy(alpha = 0.88f * alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            val innerCrystal = Path().apply {
                moveTo(x + w * 0.5f, y + w * 0.14f)
                lineTo(x + w * 0.86f, y + w * 0.5f)
                lineTo(x + w * 0.5f, y + w * 0.86f)
                lineTo(x + w * 0.14f, y + w * 0.5f)
                close()
            }
            drawPath(innerCrystal, Color.White.copy(alpha = 0.28f * alpha))
            drawPath(innerCrystal, Color.White.copy(alpha = 0.65f * alpha), style = Stroke(width = 1.2f))
        }

        BlockSkinRenderStyle.FIRE -> {
            // Molten magma cube with blazing ember core
            drawRoundRect(
                color = Color(0xFF450A0A).copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFF59D).copy(alpha = alpha),
                        Color(0xFFFF7A18).copy(alpha = alpha),
                        Color(0xFFDC2626).copy(alpha = alpha),
                        Color(0xFF7F1D1D).copy(alpha = alpha)
                    ),
                    center = Offset(x + w * 0.42f, y + w * 0.42f),
                    radius = w * 0.72f
                ),
                topLeft = Offset(x + 1f, y + 1f),
                size = Size(w - 2f, w - 2f),
                cornerRadius = CornerRadius(corner * 0.9f, corner * 0.9f)
            )
            drawRoundRect(
                color = Color(0xFFFDE047).copy(alpha = 0.65f * alpha),
                topLeft = Offset(x + 1.5f, y + 1.5f),
                size = Size(w - 3f, w - 3f),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.5f)
            )
        }

        BlockSkinRenderStyle.ICE -> {
            // Frosted arctic glacier cube
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF0F9FF).copy(alpha = alpha),
                        Color(0xFF7DD3FC).copy(alpha = 0.92f * alpha),
                        Color(0xFF0284C7).copy(alpha = 0.92f * alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.78f * alpha),
                start = Offset(x + w * 0.18f, y + w * 0.72f),
                end = Offset(x + w * 0.74f, y + w * 0.18f),
                strokeWidth = (w * 0.08f).coerceAtLeast(1.5f)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.48f * alpha),
                start = Offset(x + w * 0.44f, y + w * 0.80f),
                end = Offset(x + w * 0.82f, y + w * 0.42f),
                strokeWidth = (w * 0.05f).coerceAtLeast(1f)
            )
        }

        BlockSkinRenderStyle.HOLOGRAM_3D -> {
            // Iridescent holographic 3D foil with crisp scanlines
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF5EEAD4).copy(alpha = 0.92f * alpha),
                        style.base.copy(alpha = 0.88f * alpha),
                        Color(0xFFC084FC).copy(alpha = 0.92f * alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            for (line in 1..3) {
                val ly = y + w * (line / 4f)
                drawLine(
                    color = Color.White.copy(alpha = 0.32f * alpha),
                    start = Offset(x + w * 0.12f, ly),
                    end = Offset(x + w * 0.88f, ly),
                    strokeWidth = 1.2f
                )
            }
            drawRoundRect(
                color = Color(0xFF5EEAD4).copy(alpha = 0.85f * alpha),
                topLeft = Offset(x + 1f, y + 1f),
                size = Size(w - 2f, w - 2f),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.8f)
            )
        }

        BlockSkinRenderStyle.ROYAL_EMERALD_EX -> {
            // Exclusive Royal Emerald Vault gem inlaid with 24K gold inner filigree
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF6EE7B7).copy(alpha = alpha),
                        Color(0xFF059669).copy(alpha = alpha),
                        Color(0xFF064E3B).copy(alpha = alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawRoundRect(
                color = Color(0xFFF6C644).copy(alpha = 0.92f * alpha),
                topLeft = Offset(x + w * 0.15f, y + w * 0.15f),
                size = Size(w * 0.70f, w * 0.70f),
                cornerRadius = CornerRadius(corner * 0.55f, corner * 0.55f),
                style = Stroke(width = (w * 0.08f).coerceAtLeast(1.6f))
            )
            drawCircle(
                color = Color(0xFFFFF3C4).copy(alpha = 0.85f * alpha),
                radius = (w * 0.11f).coerceAtLeast(2f),
                center = Offset(x + w * 0.5f, y + w * 0.5f)
            )
        }

        BlockSkinRenderStyle.COSMIC_ECLIPSE_EX -> {
            // Exclusive Cosmic Eclipse dark-matter cube with violet-cyan corona & starlight
            drawRoundRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF2E1065).copy(alpha = alpha),
                        Color(0xFF090414).copy(alpha = alpha)
                    ),
                    center = Offset(x + w * 0.5f, y + w * 0.5f),
                    radius = w * 0.7f
                ),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(Color(0xFFE879F9), Color(0xFF38E8FF), Color(0xFFF6C644), Color(0xFFE879F9)),
                    center = Offset(x + w * 0.5f, y + w * 0.5f)
                ),
                radius = w * 0.28f,
                center = Offset(x + w * 0.5f, y + w * 0.5f),
                alpha = 0.9f * alpha,
                style = Stroke(width = (w * 0.09f).coerceAtLeast(1.8f))
            )
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFFE879F9).copy(alpha = 0.95f * alpha), Color(0xFF38E8FF).copy(alpha = 0.95f * alpha)),
                    start = Offset(x, y),
                    end = Offset(x + w, y + w)
                ),
                topLeft = Offset(x + 1f, y + 1f),
                size = Size(w - 2f, w - 2f),
                cornerRadius = CornerRadius(corner, corner),
                style = Stroke(width = 1.8f)
            )
        }

        BlockSkinRenderStyle.DEFAULT_GLOSSY -> {
            // 2. Extruded deep bevel base (bottom-right rim)
            drawRoundRect(
                color = style.shadow.copy(alpha = alpha),
                topLeft = Offset(x, y),
                size = Size(w, w),
                cornerRadius = CornerRadius(corner, corner)
            )

            // 3. Main glossy 3D face slightly inset on bottom/right for depth
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        style.highlight.copy(alpha = 0.95f * alpha),
                        style.base.copy(alpha = alpha),
                        style.shadow.copy(alpha = 0.95f * alpha)
                    ),
                    start = Offset(x, y),
                    end = Offset(x + faceW, y + faceH)
                ),
                topLeft = Offset(x, y),
                size = Size(faceW, faceH),
                cornerRadius = CornerRadius(corner * 0.9f, corner * 0.9f)
            )

            // 4. Inner raised gem cushion
            val innerInset = w * 0.16f
            val innerW = (faceW - innerInset * 1.6f).coerceAtLeast(2f)
            val innerH = (faceH - innerInset * 1.6f).coerceAtLeast(2f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        style.base.copy(alpha = 0.85f * alpha),
                        style.shadow.copy(alpha = 0.35f * alpha)
                    ),
                    startY = y + innerInset * 0.8f,
                    endY = y + innerInset * 0.8f + innerH
                ),
                topLeft = Offset(x + innerInset * 0.8f, y + innerInset * 0.8f),
                size = Size(innerW, innerH),
                cornerRadius = CornerRadius(corner * 0.55f, corner * 0.55f)
            )

            // 5. Glossy upper-left glass specular highlight
            drawRoundRect(
                color = Color.White.copy(alpha = 0.42f * alpha),
                topLeft = Offset(x + w * 0.15f, y + w * 0.12f),
                size = Size(w * 0.50f, w * 0.16f),
                cornerRadius = CornerRadius(w * 0.1f, w * 0.1f)
            )

            // 6. Specular pinpoint glint
            drawCircle(
                color = Color.White.copy(alpha = 0.65f * alpha),
                radius = (w * 0.065f).coerceAtLeast(1.2f),
                center = Offset(x + w * 0.22f, y + w * 0.22f)
            )
        }
    }

    // 7. Crisp rim highlight or active line-clear pulse glow
    if (highlightPulse) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.65f * alpha),
            topLeft = Offset(x, y),
            size = Size(w, w),
            cornerRadius = CornerRadius(corner, corner)
        )
        drawRoundRect(
            color = style.glow.copy(alpha = 0.95f * alpha),
            topLeft = Offset(x, y),
            size = Size(w, w),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = (w * 0.09f).coerceAtLeast(2f))
        )
    } else if (blockSkinStyle == BlockSkinRenderStyle.DEFAULT_GLOSSY) {
        drawRoundRect(
            color = Color.White.copy(alpha = 0.22f * alpha),
            topLeft = Offset(x + 0.5f, y + 0.5f),
            size = Size(w - 1f, w - 1f),
            cornerRadius = CornerRadius(corner, corner),
            style = Stroke(width = 1f)
        )
    }
}

@Composable
fun Board10x10Canvas(
    grid: List<Int>,
    theme: BoardThemeSpec,
    clearingCells: Set<Int>,
    previewShape: PolyominoShape?,
    previewAnchorRow: Int?,
    previewAnchorCol: Int?,
    previewValid: Boolean,
    potentialClearCells: Set<Int>,
    clearPulseAlpha: Float,
    modifier: Modifier = Modifier,
    blockSkinStyle: BlockSkinRenderStyle = BlockSkinRenderStyle.DEFAULT_GLOSSY,
    boosterTargetingActive: Boolean = false
) {
    Canvas(modifier = modifier) {
        val boardPx = minOf(size.width, size.height)
        val startX = (size.width - boardPx) / 2f
        val startY = (size.height - boardPx) / 2f
        val cellSize = boardPx / BOARD_SIZE.toFloat()
        val cellCorner = (cellSize * 0.20f).coerceAtLeast(3f)

        // Outer ambient frame background
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(theme.boardBgTop, theme.boardBgBottom)),
            topLeft = Offset(startX, startY),
            size = Size(boardPx, boardPx),
            cornerRadius = CornerRadius(cellSize * 0.4f, cellSize * 0.4f)
        )

        val previewCellsMap = mutableMapOf<Int, Int>()
        if (previewShape != null && previewAnchorRow != null && previewAnchorCol != null) {
            for (c in previewShape.cells) {
                val r = previewAnchorRow + c.row
                val col = previewAnchorCol + c.col
                if (r in 0 until BOARD_SIZE && col in 0 until BOARD_SIZE) {
                    previewCellsMap[BlockPuzzleEngine.index(r, col)] = previewShape.colorIndex
                }
            }
        }

        // Draw 10x10 recessed cells & filled glossy 3D blocks
        for (r in 0 until BOARD_SIZE) {
            for (c in 0 until BOARD_SIZE) {
                val idx = BlockPuzzleEngine.index(r, c)
                val cellTopLeft = Offset(startX + c * cellSize, startY + r * cellSize)
                val pad = (cellSize * 0.06f).coerceAtLeast(1.2f)
                val innerSize = (cellSize - pad * 2f).coerceAtLeast(4f)

                // Empty recessed cell socket
                drawRoundRect(
                    color = theme.emptyCellFill,
                    topLeft = Offset(cellTopLeft.x + pad, cellTopLeft.y + pad),
                    size = Size(innerSize, innerSize),
                    cornerRadius = CornerRadius(cellCorner, cellCorner)
                )
                drawRoundRect(
                    color = theme.emptyCellBorder.copy(alpha = 0.65f),
                    topLeft = Offset(cellTopLeft.x + pad, cellTopLeft.y + pad),
                    size = Size(innerSize, innerSize),
                    cornerRadius = CornerRadius(cellCorner, cellCorner),
                    style = Stroke(width = 1f)
                )

                val colorIdx = grid.getOrElse(idx) { 0 }
                val isClearing = idx in clearingCells
                val isPotentialClear = idx in potentialClearCells

                if (colorIdx > 0) {
                    val style = theme.palette[(colorIdx - 1) % theme.palette.size]
                    drawGlossy3DBlock(
                        topLeft = cellTopLeft,
                        cellSize = cellSize,
                        style = style,
                        alpha = if (isClearing) (1f - clearPulseAlpha * 0.45f) else 1f,
                        highlightPulse = isClearing || isPotentialClear,
                        blockSkinStyle = blockSkinStyle
                    )
                    if (boosterTargetingActive) {
                        drawRoundRect(
                            color = Color(0xFFF6C644).copy(alpha = 0.35f + clearPulseAlpha * 0.45f),
                            topLeft = Offset(cellTopLeft.x + pad, cellTopLeft.y + pad),
                            size = Size(innerSize, innerSize),
                            cornerRadius = CornerRadius(cellCorner, cellCorner),
                            style = Stroke(width = 2f)
                        )
                    }
                } else if (isClearing) {
                    val glowStyle = theme.palette[(r + c) % theme.palette.size]
                    drawRoundRect(
                        color = glowStyle.glow.copy(alpha = (0.75f * (1f - clearPulseAlpha)).coerceIn(0f, 1f)),
                        topLeft = Offset(cellTopLeft.x + pad, cellTopLeft.y + pad),
                        size = Size(innerSize, innerSize),
                        cornerRadius = CornerRadius(cellCorner, cellCorner)
                    )
                }

                // Ghost preview overlay when dragging over board
                val previewColorIdx = previewCellsMap[idx]
                if (previewColorIdx != null) {
                    if (previewValid) {
                        val style = theme.palette[(previewColorIdx - 1) % theme.palette.size]
                        drawGlossy3DBlock(
                            topLeft = cellTopLeft,
                            cellSize = cellSize,
                            style = style,
                            alpha = 0.58f,
                            highlightPulse = isPotentialClear,
                            blockSkinStyle = blockSkinStyle
                        )
                    } else {
                        drawRoundRect(
                            color = Color(0xFFFF4D6D).copy(alpha = 0.40f),
                            topLeft = Offset(cellTopLeft.x + pad, cellTopLeft.y + pad),
                            size = Size(innerSize, innerSize),
                            cornerRadius = CornerRadius(cellCorner, cellCorner)
                        )
                        drawRoundRect(
                            color = Color(0xFFFF4D6D),
                            topLeft = Offset(cellTopLeft.x + pad, cellTopLeft.y + pad),
                            size = Size(innerSize, innerSize),
                            cornerRadius = CornerRadius(cellCorner, cellCorner),
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Polyomino3DCanvas(
    shape: PolyominoShape,
    theme: BoardThemeSpec,
    maxCellSizePx: Float? = null,
    alpha: Float = 1f,
    modifier: Modifier = Modifier,
    blockSkinStyle: BlockSkinRenderStyle = BlockSkinRenderStyle.DEFAULT_GLOSSY
) {
    Canvas(modifier = modifier) {
        val style = theme.palette[(shape.colorIndex - 1) % theme.palette.size]
        val computedCell = minOf(
            size.width / shape.cols.toFloat(),
            size.height / shape.rows.toFloat()
        )
        val cellSize = if (maxCellSizePx != null) minOf(computedCell, maxCellSizePx) else computedCell
        val totalW = cellSize * shape.cols
        val totalH = cellSize * shape.rows
        val offsetX = (size.width - totalW) / 2f
        val offsetY = (size.height - totalH) / 2f

        for (cell in shape.cells) {
            drawGlossy3DBlock(
                topLeft = Offset(offsetX + cell.col * cellSize, offsetY + cell.row * cellSize),
                cellSize = cellSize,
                style = style,
                alpha = alpha,
                blockSkinStyle = blockSkinStyle
            )
        }
    }
}
