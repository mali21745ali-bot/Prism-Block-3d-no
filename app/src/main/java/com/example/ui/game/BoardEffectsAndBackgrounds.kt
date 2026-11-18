package com.example.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.game.AvatarSpec
import com.example.game.BOARD_SIZE
import com.example.game.BoardBackgroundAnimation
import com.example.game.BoardBackgroundSpec
import com.example.game.PlayerBadgeSpec
import com.example.game.ProfileFrameSpec
import com.example.game.SpecialEffectSpec
import com.example.game.SpecialEffectStyle
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the player's equipped Board Background (Dark, Gradient, Colorful, Premium,
 * or live animated Starfield / Prism Wave / Cosmic Nebula).
 */
@Composable
fun EquippedBoardBackground(
    spec: BoardBackgroundSpec,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "boardBgAnim")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bgPhase"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgPulse"
    )

    Box(
        modifier = modifier.background(
            Brush.verticalGradient(listOf(spec.topColor, spec.midColor, spec.bottomColor))
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Subtle ambient radial halo at top
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        spec.accentGlow.copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.18f),
                    radius = size.minDimension * 0.75f
                ),
                radius = size.minDimension * 0.75f,
                center = Offset(size.width * 0.5f, size.height * 0.18f)
            )

            when (spec.animation) {
                BoardBackgroundAnimation.NONE -> {
                    // Static luxury secondary highlight glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(spec.accentGlow.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.82f),
                            radius = size.minDimension * 0.5f
                        ),
                        radius = size.minDimension * 0.5f,
                        center = Offset(size.width * 0.85f, size.height * 0.82f)
                    )
                }

                BoardBackgroundAnimation.STARFIELD -> {
                    for (i in 0 until 44) {
                        val seedX = ((i * 73 + 17) % 100) / 100f
                        val seedY = ((i * 41 + 29) % 100) / 100f
                        val speed = 0.4f + (i % 4) * 0.25f
                        val yFrac = (seedY - phase * speed + 2f) % 1f
                        val twinkle = 0.35f + 0.65f * ((sin((phase * 2 * PI + i).toDouble()).toFloat() + 1f) / 2f)
                        val r = (1.2f + (i % 3) * 1.1f).dp.toPx()
                        drawCircle(
                            color = if (i % 5 == 0) spec.accentGlow.copy(alpha = twinkle * 0.85f)
                            else Color.White.copy(alpha = twinkle * 0.7f),
                            radius = r,
                            center = Offset(seedX * size.width, yFrac * size.height)
                        )
                    }
                }

                BoardBackgroundAnimation.PRISM_WAVE -> {
                    for (band in 0 until 3) {
                        val path = Path()
                        val baseY = size.height * (0.24f + band * 0.26f)
                        val amp = size.height * 0.055f
                        path.moveTo(0f, baseY)
                        val steps = 28
                        for (s in 0..steps) {
                            val x = size.width * (s / steps.toFloat())
                            val angle = (s / steps.toFloat()) * 2.0 * PI + phase * 2.0 * PI + band * 1.4
                            val y = baseY + sin(angle).toFloat() * amp
                            path.lineTo(x, y)
                        }
                        val waveColor = when (band) {
                            0 -> spec.accentGlow
                            1 -> Color(0xFF38E8FF)
                            else -> Color(0xFFF6C644)
                        }
                        drawPath(
                            path = path,
                            color = waveColor.copy(alpha = 0.22f + pulse * 0.14f),
                            style = Stroke(width = (18f - band * 3f).dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }

                BoardBackgroundAnimation.AURORA_PULSE -> {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(spec.accentGlow.copy(alpha = pulse * 0.32f), Color.Transparent),
                            center = Offset(size.width * 0.3f, size.height * 0.35f),
                            radius = size.minDimension * 0.65f
                        ),
                        radius = size.minDimension * 0.65f,
                        center = Offset(size.width * 0.3f, size.height * 0.35f)
                    )
                }

                BoardBackgroundAnimation.COSMIC_NEBULA -> {
                    // Swirling dual nebula orbs + sparkling starfield
                    val orbX1 = size.width * (0.3f + 0.18f * cos(phase * 2.0 * PI).toFloat())
                    val orbY1 = size.height * (0.3f + 0.12f * sin(phase * 2.0 * PI).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(Color(0xFFE879F9).copy(alpha = 0.26f), Color.Transparent),
                            center = Offset(orbX1, orbY1),
                            radius = size.minDimension * 0.62f
                        ),
                        radius = size.minDimension * 0.62f,
                        center = Offset(orbX1, orbY1)
                    )
                    val orbX2 = size.width * (0.72f - 0.16f * cos(phase * 2.0 * PI).toFloat())
                    val orbY2 = size.height * (0.68f - 0.10f * sin(phase * 2.0 * PI).toFloat())
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(spec.accentGlow.copy(alpha = 0.28f), Color.Transparent),
                            center = Offset(orbX2, orbY2),
                            radius = size.minDimension * 0.58f
                        ),
                        radius = size.minDimension * 0.58f,
                        center = Offset(orbX2, orbY2)
                    )
                    for (i in 0 until 32) {
                        val sx = ((i * 61 + 13) % 100) / 100f
                        val sy = ((i * 37 + 19) % 100) / 100f
                        val tw = 0.3f + 0.7f * ((sin((phase * 4.0 * PI + i).toDouble()).toFloat() + 1f) / 2f)
                        drawCircle(
                            color = Color.White.copy(alpha = tw * 0.8f),
                            radius = (1.3f + (i % 2) * 0.9f).dp.toPx(),
                            center = Offset(sx * size.width, sy * size.height)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Renders live animated Special Effects (Glow, Fire, Lightning, Rainbow, Stardust Particles,
 * Line Shockwave, Crystal Placement Ripple, Exclusive Supernova) on top of the 10x10 grid.
 */
@Composable
fun SpecialEffectOverlay(
    effectSpec: SpecialEffectSpec,
    clearingCells: Set<Int>,
    lastPlacedCells: List<Int>,
    clearEffectToken: Int,
    placementEffectToken: Int,
    modifier: Modifier = Modifier
) {
    val clearAnim = remember { Animatable(1f) }
    val placeAnim = remember { Animatable(1f) }

    LaunchedEffect(clearEffectToken) {
        if (clearEffectToken > 0) {
            clearAnim.snapTo(0f)
            clearAnim.animateTo(1f, animationSpec = tween(durationMillis = 520, easing = LinearEasing))
        }
    }

    LaunchedEffect(placementEffectToken) {
        if (placementEffectToken > 0) {
            placeAnim.snapTo(0f)
            placeAnim.animateTo(1f, animationSpec = tween(durationMillis = 320, easing = LinearEasing))
        }
    }

    val clearT = clearAnim.value
    val placeT = placeAnim.value
    val activeClear = clearEffectToken > 0 && clearT < 0.99f && clearingCells.isNotEmpty()
    val activePlace = placementEffectToken > 0 && placeT < 0.99f && lastPlacedCells.isNotEmpty()

    if (!activeClear && !activePlace) return

    Canvas(modifier = modifier) {
        val boardPx = minOf(size.width, size.height)
        val startX = (size.width - boardPx) / 2f
        val startY = (size.height - boardPx) / 2f
        val cellSize = boardPx / BOARD_SIZE.toFloat()

        // 1. Placement Ripple / Glow Effect
        if (activePlace) {
            val fade = (1f - placeT).coerceIn(0f, 1f)
            val isPlacementSpecialty = effectSpec.style == SpecialEffectStyle.CRYSTAL_PLACEMENT ||
                effectSpec.style == SpecialEffectStyle.GLOW_AURA ||
                effectSpec.style == SpecialEffectStyle.SUPERNOVA_EX

            val radiusScale = if (isPlacementSpecialty) 0.55f + placeT * 0.95f else 0.45f + placeT * 0.45f
            val alphaMult = if (isPlacementSpecialty) 0.72f else 0.35f

            for (idx in lastPlacedCells) {
                val r = idx / BOARD_SIZE
                val c = idx % BOARD_SIZE
                val center = Offset(startX + (c + 0.5f) * cellSize, startY + (r + 0.5f) * cellSize)
                if (effectSpec.style == SpecialEffectStyle.CRYSTAL_PLACEMENT) {
                    val half = cellSize * radiusScale * 0.65f
                    val diamond = Path().apply {
                        moveTo(center.x, center.y - half)
                        lineTo(center.x + half, center.y)
                        lineTo(center.x, center.y + half)
                        lineTo(center.x - half, center.y)
                        close()
                    }
                    drawPath(
                        path = diamond,
                        color = effectSpec.primaryColor.copy(alpha = fade * alphaMult),
                        style = Stroke(width = 2.5f.dp.toPx())
                    )
                } else {
                    drawCircle(
                        color = effectSpec.primaryColor.copy(alpha = fade * alphaMult),
                        radius = cellSize * radiusScale * 0.55f,
                        center = center,
                        style = Stroke(width = (if (isPlacementSpecialty) 3f else 1.8f).dp.toPx())
                    )
                }
            }
        }

        // 2. Line-Clear / Booster Explosion Effect
        if (activeClear) {
            val fade = (1f - clearT).coerceIn(0f, 1f)
            val rows = clearingCells.map { it / BOARD_SIZE }.toSet()
            val cols = clearingCells.map { it % BOARD_SIZE }.toSet()

            when (effectSpec.style) {
                SpecialEffectStyle.DEFAULT, SpecialEffectStyle.GLOW_AURA -> {
                    for (idx in clearingCells) {
                        val r = idx / BOARD_SIZE
                        val c = idx % BOARD_SIZE
                        val center = Offset(startX + (c + 0.5f) * cellSize, startY + (r + 0.5f) * cellSize)
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    effectSpec.primaryColor.copy(alpha = fade * 0.85f),
                                    effectSpec.secondaryColor.copy(alpha = fade * 0.35f),
                                    Color.Transparent
                                ),
                                center = center,
                                radius = cellSize * (0.7f + clearT * 0.7f)
                            ),
                            radius = cellSize * (0.7f + clearT * 0.7f),
                            center = center
                        )
                    }
                }

                SpecialEffectStyle.FIRE_BURST -> {
                    for (idx in clearingCells) {
                        val r = idx / BOARD_SIZE
                        val c = idx % BOARD_SIZE
                        val cx = startX + (c + 0.5f) * cellSize
                        val cy = startY + (r + 0.5f) * cellSize - clearT * cellSize * 0.6f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFFF59D).copy(alpha = fade),
                                    Color(0xFFFF6B00).copy(alpha = fade * 0.85f),
                                    Color(0xFFDC2626).copy(alpha = fade * 0.4f),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy),
                                radius = cellSize * (0.65f + clearT * 0.55f)
                            ),
                            radius = cellSize * (0.65f + clearT * 0.55f),
                            center = Offset(cx, cy)
                        )
                    }
                }

                SpecialEffectStyle.LIGHTNING_ARC -> {
                    val boltRows = rows.filter { r -> (0 until BOARD_SIZE).count { c -> r * BOARD_SIZE + c in clearingCells } >= 5 }
                    val boltCols = cols.filter { c -> (0 until BOARD_SIZE).count { r -> r * BOARD_SIZE + c in clearingCells } >= 5 }

                    for (r in boltRows) {
                        val yMid = startY + (r + 0.5f) * cellSize
                        val path = Path().apply {
                            moveTo(startX, yMid)
                            for (step in 1..12) {
                                val x = startX + boardPx * (step / 12f)
                                val jitter = if (step % 2 == 0) cellSize * 0.28f else -cellSize * 0.28f
                                lineTo(x, yMid + jitter * (1f - clearT * 0.4f))
                            }
                        }
                        drawPath(path, Color(0xFF00F5FF).copy(alpha = fade * 0.9f), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(path, Color.White.copy(alpha = fade), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    for (c in boltCols) {
                        val xMid = startX + (c + 0.5f) * cellSize
                        val path = Path().apply {
                            moveTo(xMid, startY)
                            for (step in 1..12) {
                                val y = startY + boardPx * (step / 12f)
                                val jitter = if (step % 2 == 0) cellSize * 0.28f else -cellSize * 0.28f
                                lineTo(xMid + jitter * (1f - clearT * 0.4f), y)
                            }
                        }
                        drawPath(path, Color(0xFF00F5FF).copy(alpha = fade * 0.9f), style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round))
                        drawPath(path, Color.White.copy(alpha = fade), style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                    }
                    if (boltRows.isEmpty() && boltCols.isEmpty()) {
                        for (idx in clearingCells) {
                            val r = idx / BOARD_SIZE
                            val c = idx % BOARD_SIZE
                            val center = Offset(startX + (c + 0.5f) * cellSize, startY + (r + 0.5f) * cellSize)
                            drawCircle(Color(0xFF00F5FF).copy(alpha = fade), cellSize * (0.45f + clearT * 0.5f), center, style = Stroke(3.dp.toPx()))
                        }
                    }
                }

                SpecialEffectStyle.RAINBOW_WAVE -> {
                    val rainbow = listOf(
                        Color(0xFFFF3B6B), Color(0xFFFF9F1C), Color(0xFFFEE440),
                        Color(0xFF00F5D4), Color(0xFF00BBF9), Color(0xFF9B5DE5)
                    )
                    for (idx in clearingCells) {
                        val r = idx / BOARD_SIZE
                        val c = idx % BOARD_SIZE
                        val tl = Offset(startX + c * cellSize, startY + r * cellSize)
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = rainbow.map { it.copy(alpha = fade * 0.85f) },
                                start = Offset(tl.x + clearT * cellSize, tl.y),
                                end = Offset(tl.x + cellSize * (1f + clearT), tl.y + cellSize)
                            ),
                            topLeft = tl,
                            size = Size(cellSize, cellSize)
                        )
                    }
                }

                SpecialEffectStyle.STARDUST_PARTICLES -> {
                    val colors = listOf(effectSpec.primaryColor, effectSpec.secondaryColor, Color.White, Color(0xFF38E8FF))
                    for (idx in clearingCells) {
                        val r = idx / BOARD_SIZE
                        val c = idx % BOARD_SIZE
                        val cx = startX + (c + 0.5f) * cellSize
                        val cy = startY + (r + 0.5f) * cellSize
                        for (p in 0 until 4) {
                            val angle = (p * 90 + (idx * 23) % 60) * PI / 180.0
                            val dist = clearT * cellSize * 1.15f
                            val px = cx + cos(angle).toFloat() * dist
                            val py = cy + sin(angle).toFloat() * dist
                            drawCircle(
                                color = colors[(idx + p) % colors.size].copy(alpha = fade),
                                radius = (3.5f * (1f - clearT * 0.5f)).dp.toPx(),
                                center = Offset(px, py)
                            )
                        }
                    }
                }

                SpecialEffectStyle.LINE_SHOCKWAVE, SpecialEffectStyle.CRYSTAL_PLACEMENT -> {
                    val avgRow = clearingCells.map { it / BOARD_SIZE }.average().toFloat()
                    val avgCol = clearingCells.map { it % BOARD_SIZE }.average().toFloat()
                    val center = Offset(startX + (avgCol + 0.5f) * cellSize, startY + (avgRow + 0.5f) * cellSize)
                    val radius = boardPx * (0.12f + clearT * 0.65f)
                    drawCircle(
                        color = effectSpec.primaryColor.copy(alpha = fade * 0.9f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = (5f * fade + 1f).dp.toPx())
                    )
                    drawCircle(
                        color = effectSpec.secondaryColor.copy(alpha = fade * 0.65f),
                        radius = radius * 0.68f,
                        center = center,
                        style = Stroke(width = (3f * fade + 1f).dp.toPx())
                    )
                }

                SpecialEffectStyle.SUPERNOVA_EX -> {
                    val avgRow = clearingCells.map { it / BOARD_SIZE }.average().toFloat()
                    val avgCol = clearingCells.map { it % BOARD_SIZE }.average().toFloat()
                    val center = Offset(startX + (avgCol + 0.5f) * cellSize, startY + (avgRow + 0.5f) * cellSize)
                    val outerRadius = boardPx * (0.15f + clearT * 0.72f)
                    // Golden shockwave + magenta halo
                    drawCircle(
                        color = Color(0xFFF6C644).copy(alpha = fade * 0.95f),
                        radius = outerRadius,
                        center = center,
                        style = Stroke(width = (6f * fade + 1.5f).dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFF00A8).copy(alpha = fade * 0.75f),
                        radius = outerRadius * 0.72f,
                        center = center,
                        style = Stroke(width = (3.5f * fade + 1f).dp.toPx())
                    )
                    // 12 radial starburst rays
                    for (ray in 0 until 12) {
                        val a = ray * (2.0 * PI / 12.0) + clearT * 0.4
                        val inner = outerRadius * 0.25f
                        val outer = outerRadius * 1.05f
                        drawLine(
                            color = if (ray % 2 == 0) Color(0xFFFFF3C4).copy(alpha = fade) else Color(0xFFE879F9).copy(alpha = fade * 0.85f),
                            start = Offset(center.x + cos(a).toFloat() * inner, center.y + sin(a).toFloat() * inner),
                            end = Offset(center.x + cos(a).toFloat() * outer, center.y + sin(a).toFloat() * outer),
                            strokeWidth = 2.5f.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
    }
}

/**
 * Shared Avatar + Profile Frame renderer showing the player's equipped Avatar & Frame cosmetics
 * across Profile, Home, and Shop previews.
 */
@Composable
fun EquippedProfileAvatar(
    photoUrl: String?,
    displayName: String,
    avatarSpec: AvatarSpec?,
    frameSpec: ProfileFrameSpec?,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val ringWidth = (frameSpec?.borderWidthDp ?: 2f).dp
    val ringBrush = if (frameSpec != null) {
        Brush.sweepGradient(listOf(frameSpec.ringStart, frameSpec.ringEnd, frameSpec.glowColor, frameSpec.ringStart))
    } else {
        Brush.linearGradient(listOf(Color(0xFF38E8FF).copy(alpha = 0.65f), Color(0xFF8B5CF6).copy(alpha = 0.65f)))
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .border(width = ringWidth, brush = ringBrush, shape = CircleShape)
            .padding(ringWidth + 2.dp)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        when {
            avatarSpec != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(avatarSpec.bgStart, avatarSpec.bgEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(size * 0.62f)
                    )
                    Text(
                        text = avatarSpec.badgeSymbol,
                        style = if (size >= 60.dp) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            !photoUrl.isNullOrBlank() -> {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "$displayName avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1F1A3D)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "$displayName avatar",
                        tint = Color(0xFF38E8FF),
                        modifier = Modifier.size(size * 0.52f)
                    )
                }
            }
        }
    }
}

@Composable
fun EquippedBadgePill(
    badgeSpec: PlayerBadgeSpec,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = badgeSpec.primaryColor.copy(alpha = 0.20f),
        border = BorderStroke(1.dp, badgeSpec.primaryColor),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.WorkspacePremium,
                contentDescription = badgeSpec.name,
                tint = badgeSpec.primaryColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = badgeSpec.shortTitle,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = badgeSpec.primaryColor
            )
        }
    }
}
