package com.example.ui.game

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.GameSessionProgressMetrics
import com.example.game.LevelProgressMetrics
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismGoldWarm
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder

/**
 * Dual compact premium 3D animated progress bar panel placed directly below the game mode title
 * and above the Score, Lines and Coins HUD row.
 *
 * 1. Level Progress Bar: Permanent progress toward the 1,000-line requirement for the current level.
 * 2. Game Progress Bar: Current session's line-clearing progress (resets when a new game starts).
 */
@Composable
fun DualGameProgressPanel(
    levelMetrics: LevelProgressMetrics,
    gameMetrics: GameSessionProgressMetrics,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dual_progress_bars_panel"),
        shape = RoundedCornerShape(16.dp),
        color = DeepSurface.copy(alpha = 0.94f),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                listOf(
                    PrismGold.copy(alpha = 0.45f),
                    SurfaceBorder.copy(alpha = 0.75f),
                    PrismCyan.copy(alpha = 0.45f)
                )
            )
        ),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // 1. PERMANENT LEVEL PROGRESS BAR (1,000 lines per level)
            Labeled3DProgressRow(
                icon = Icons.Filled.EmojiEvents,
                title = "LEVEL ${levelMetrics.levelNumber}",
                countText = "${levelMetrics.countText} lines",
                percentageText = levelMetrics.percentageText,
                progress = levelMetrics.fraction,
                triggerValue = levelMetrics.clearedLines + levelMetrics.levelNumber * 10000L,
                gradientColors = listOf(PrismGold, PrismGoldWarm, PrismCoral),
                accentColor = PrismGold,
                glowColor = PrismGoldWarm,
                sectionTestTag = "level_progress_bar_section",
                trackTestTag = "level_progress_bar_track",
                countTestTag = "level_progress_count",
                percentTestTag = "level_progress_percent",
                accessibilityLabel = "Level ${levelMetrics.levelNumber} progress: ${levelMetrics.countText} lines, ${levelMetrics.percentageText}"
            )

            // 2. CURRENT GAME SESSION PROGRESS BAR (Resets on new game)
            Labeled3DProgressRow(
                icon = Icons.Filled.Bolt,
                title = "GAME",
                countText = gameMetrics.countText,
                percentageText = gameMetrics.percentageText,
                progress = gameMetrics.fraction,
                triggerValue = gameMetrics.linesCleared,
                gradientColors = listOf(PrismCyan, PrismMint, PrismViolet),
                accentColor = PrismCyan,
                glowColor = PrismMint,
                sectionTestTag = "game_progress_bar_section",
                trackTestTag = "game_progress_bar_track",
                countTestTag = "game_progress_count",
                percentTestTag = "game_progress_percent",
                accessibilityLabel = "Current game progress: ${gameMetrics.countText}, ${gameMetrics.percentageText}"
            )
        }
    }
}

@Composable
private fun Labeled3DProgressRow(
    icon: ImageVector,
    title: String,
    countText: String,
    percentageText: String,
    progress: Float,
    triggerValue: Long,
    gradientColors: List<Color>,
    accentColor: Color,
    glowColor: Color,
    sectionTestTag: String,
    trackTestTag: String,
    countTestTag: String,
    percentTestTag: String,
    accessibilityLabel: String
) {
    // Pulse animation whenever cleared line count increases
    var lastObserved by remember { mutableLongStateOf(triggerValue) }
    val surgePulse = remember { Animatable(0f) }

    LaunchedEffect(triggerValue) {
        if (triggerValue > lastObserved) {
            surgePulse.snapTo(1f)
            surgePulse.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
        }
        lastObserved = triggerValue
    }

    val badgeScale = 1f + surgePulse.value * 0.14f

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(sectionTestTag)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            color = accentColor.copy(alpha = 0.18f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            text = title,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 0.5.sp
                            ),
                            color = accentColor,
                            maxLines = 1
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp
                    ),
                    color = Color(0xFFE6E3F8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag(countTestTag)
                )
            }

            Spacer(Modifier.width(6.dp))

            Text(
                text = percentageText,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp
                ),
                color = accentColor,
                maxLines = 1,
                modifier = Modifier
                    .scale(badgeScale)
                    .testTag(percentTestTag)
            )
        }

        Spacer(Modifier.height(4.dp))

        Prism3DProgressBar(
            progress = progress,
            gradientColors = gradientColors,
            glowColor = glowColor,
            surgeIntensity = surgePulse.value,
            barHeight = 11.dp,
            contentDesc = accessibilityLabel,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(trackTestTag)
        )
    }
}

/**
 * Reusable glossy 3D beveled progress bar featuring:
 * - Recessed obsidian 3D groove track with subtle milestone ticks
 * - Smooth spring-animated fill expansion
 * - Multi-stop vibrant gradient body with top glass specular highlight & bottom 3D bevel shadow
 * - Continuous diagonal crystal shimmer sweep + bright leading-edge glint bead
 * - Surge glow intensity burst on line clears
 */
@Composable
fun Prism3DProgressBar(
    progress: Float,
    gradientColors: List<Color>,
    glowColor: Color,
    modifier: Modifier = Modifier,
    surgeIntensity: Float = 0f,
    barHeight: Dp = 12.dp,
    contentDesc: String = "Progress bar"
) {
    val clampedTarget = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedTarget,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "prism3dBarProgress"
    )

    val shimmerTransition = rememberInfiniteTransition(label = "prism3dBarShimmer")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "prism3dShimmerPhase"
    )

    Canvas(
        modifier = modifier
            .height(barHeight)
            .semantics {
                contentDescription = contentDesc
                progressBarRangeInfo = ProgressBarRangeInfo(clampedTarget, 0f..1f)
            }
    ) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val radius = h / 2f
        val corner = CornerRadius(radius, radius)

        // 1. Recessed 3D obsidian trough (dark inner top shadow -> soft bottom inner rim)
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF070512),
                    Color(0xFF130F28),
                    Color(0xFF211B40)
                )
            ),
            topLeft = Offset.Zero,
            size = Size(w, h),
            cornerRadius = corner
        )

        // Subtle inner top groove shadow for realistic depth
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.45f),
            topLeft = Offset(1f, 0.5f),
            size = Size((w - 2f).coerceAtLeast(0f), (h * 0.42f)),
            cornerRadius = corner
        )

        // Subtle 25%, 50%, 75% milestone ticks inside groove
        val tickColor = Color.White.copy(alpha = 0.12f)
        for (i in 1..3) {
            val tickX = w * (i * 0.25f)
            drawLine(
                color = tickColor,
                start = Offset(tickX, h * 0.24f),
                end = Offset(tickX, h * 0.76f),
                strokeWidth = 1.2f
            )
        }

        // 2. Animated 3D glossy beveled fill bar
        val displayFraction = animatedProgress.coerceIn(0f, 1f)
        if (displayFraction > 0.0001f) {
            // Ensure even 0.1% (1/1,000 lines) renders a crisp glowing jewel bead tip
            val fillWidth = maxOf(h * 0.92f, w * displayFraction).coerceAtMost(w)
            val inset = 1.2f
            val innerH = (h - inset * 2f).coerceAtLeast(2f)
            val innerW = (fillWidth - inset * 2f).coerceAtLeast(innerH * 0.75f).coerceAtMost(w - inset * 2f)
            val innerR = innerH / 2f
            val innerCorner = CornerRadius(innerR, innerR)

            // Soft luminous aura glow under & around active bar
            val glowAlpha = (0.26f + surgeIntensity * 0.34f).coerceIn(0f, 0.75f)
            drawRoundRect(
                color = glowColor.copy(alpha = glowAlpha),
                topLeft = Offset(0f, -1f),
                size = Size(fillWidth.coerceAtMost(w), h + 2f),
                cornerRadius = CornerRadius(radius + 1f, radius + 1f)
            )

            val fillPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        rect = Rect(inset, inset, inset + innerW, inset + innerH),
                        cornerRadius = innerCorner
                    )
                )
            }

            clipPath(fillPath) {
                // Layer A: Vibrant multi-stop horizontal crystal gradient
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = gradientColors,
                        startX = inset,
                        endX = w
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(innerW, innerH)
                )

                // Layer B: Bottom 3D cylindrical shadow bevel
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.34f)
                        ),
                        startY = inset + innerH * 0.55f,
                        endY = inset + innerH
                    ),
                    topLeft = Offset(inset, inset + innerH * 0.55f),
                    size = Size(innerW, innerH * 0.45f)
                )

                // Layer C: Upper glossy glass specular highlight (matches 3D crystal blocks)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.56f),
                            Color.White.copy(alpha = 0.08f)
                        ),
                        startY = inset,
                        endY = inset + innerH * 0.46f
                    ),
                    topLeft = Offset(inset + 1f, inset + 0.6f),
                    size = Size((innerW - 2f).coerceAtLeast(0f), innerH * 0.44f),
                    cornerRadius = CornerRadius(innerR, innerR)
                )

                // Layer D: Animated diagonal crystal shimmer sweep
                val shimmerCenterX = inset + w * shimmerOffset
                val shimmerBandW = (w * 0.22f).coerceAtLeast(28f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.36f + surgeIntensity * 0.24f),
                            Color.Transparent
                        ),
                        start = Offset(shimmerCenterX - shimmerBandW / 2f, inset),
                        end = Offset(shimmerCenterX + shimmerBandW / 2f, inset + innerH)
                    ),
                    topLeft = Offset(inset, inset),
                    size = Size(innerW, innerH)
                )

                // Layer E: Surge flash overlay when a line is just cleared
                if (surgeIntensity > 0.01f) {
                    drawRect(
                        color = Color.White.copy(alpha = surgeIntensity * 0.35f),
                        topLeft = Offset(inset, inset),
                        size = Size(innerW, innerH)
                    )
                }

                // Layer F: Bright specular pinpoint glint at the active leading edge
                val tipX = (inset + innerW - innerR * 0.85f).coerceAtLeast(inset + innerR * 0.5f)
                drawCircle(
                    color = Color.White.copy(alpha = (0.72f + surgeIntensity * 0.28f).coerceAtMost(1f)),
                    radius = (innerH * 0.22f).coerceAtLeast(1.8f),
                    center = Offset(tipX, inset + innerH * 0.34f)
                )
            }

            // Crisp specular rim on the filled 3D bar
            drawRoundRect(
                color = Color.White.copy(alpha = 0.28f),
                topLeft = Offset(inset, inset),
                size = Size(innerW, innerH),
                cornerRadius = innerCorner,
                style = Stroke(width = 0.9f)
            )
        }

        // 3. Outer subtle obsidian rim stroke
        drawRoundRect(
            color = SurfaceBorder.copy(alpha = 0.72f),
            topLeft = Offset(0.5f, 0.5f),
            size = Size((w - 1f).coerceAtLeast(0f), (h - 1f).coerceAtLeast(0f)),
            cornerRadius = corner,
            style = Stroke(width = 1f)
        )
    }
}
