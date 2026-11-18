package com.example.ui.rewards

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.BoosterType
import com.example.data.model.ShopItem
import com.example.game.BlockThemes
import com.example.game.BoardThemeSpec
import com.example.game.CosmeticCatalog
import com.example.game.SpecialEffectStyle
import com.example.ui.game.EquippedBoardBackground
import com.example.ui.game.EquippedProfileAvatar
import com.example.ui.game.drawGlossy3DBlock
import com.example.ui.theme.CardSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import java.io.ByteArrayOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun parseHexColor(hex: String, fallback: Color = PrismCyan): Color {
    val clean = hex.trim().removePrefix("#")
    return runCatching {
        when (clean.length) {
            6 -> Color(android.graphics.Color.parseColor("#$clean"))
            8 -> Color(android.graphics.Color.parseColor("#$clean"))
            else -> fallback
        }
    }.getOrDefault(fallback)
}

/**
 * Zero-permission image compression helper for Admin Photo Picker (`PickVisualMedia`).
 * Resizes to a crisp 220x220 square thumbnail and compresses to JPEG Base64 data URI
 * guaranteed to stay safely under Firestore's 120KB field limit.
 */
fun encodePickedImageToDataUri(context: Context, uri: Uri): String? {
    return runCatching {
        val resolver = context.contentResolver
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOpts) }
        val srcW = boundsOpts.outWidth
        val srcH = boundsOpts.outHeight
        if (srcW <= 0 || srcH <= 0) return null

        var sample = 1
        while ((srcW / sample) > 480 || (srcH / sample) > 480) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: return null

        val side = minOf(decoded.width, decoded.height)
        val xOff = (decoded.width - side) / 2
        val yOff = (decoded.height - side) / 2
        val square = Bitmap.createBitmap(decoded, xOff, yOff, side, side)
        val scaled = Bitmap.createScaledBitmap(square, 220, 220, true)

        val baos = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 72, baos)
        val bytes = baos.toByteArray()
        if (scaled != square) scaled.recycle()
        if (square != decoded) square.recycle()
        decoded.recycle()

        val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
        val dataUri = "data:image/jpeg;base64,$encoded"
        if (dataUri.length <= 118_000) dataUri else null
    }.getOrNull()
}

@Composable
fun ShopItemVisualPreview(
    item: ShopItem,
    size: Dp = 76.dp,
    modifier: Modifier = Modifier
) {
    val accent = remember(item.accentHex) { parseHexColor(item.accentHex, PrismCyan) }
    val shape = RoundedCornerShape(16.dp)

    // 1. Custom Admin-uploaded image (Base64 data URI or HTTPS URL)
    if (item.imageUrl.startsWith("data:image/")) {
        val decodedBitmap = remember(item.imageUrl) {
            runCatching {
                val base64Part = item.imageUrl.substringAfter("base64,", "")
                val bytes = Base64.decode(base64Part, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }.getOrNull()
        }
        if (decodedBitmap != null) {
            Surface(
                shape = shape,
                border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
                modifier = modifier.size(size)
            ) {
                Image(
                    bitmap = decodedBitmap,
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            return
        }
    } else if (item.imageUrl.startsWith("https://") || item.imageUrl.startsWith("http://")) {
        Surface(
            shape = shape,
            border = BorderStroke(1.5.dp, accent.copy(alpha = 0.7f)),
            modifier = modifier.size(size)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        return
    }

    // 2. Live Procedural 3D Visual Preview tailored to the exact Shop Category & Asset Key
    val effectiveCat = if (item.category == "EXCLUSIVE" && item.subType.isNotBlank()) item.subType else item.category
    val assetKey = item.resolvedAssetKey

    Surface(
        shape = shape,
        color = Color(0xFF14102A),
        border = BorderStroke(1.2.dp, accent.copy(alpha = 0.6f)),
        modifier = modifier.size(size)
    ) {
        when (effectiveCat) {
            "THEME" -> {
                val theme = remember(assetKey) { BlockThemes.byId(assetKey) }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(theme.boardBgTop, theme.boardBgBottom)))
                        .padding(6.dp)
                ) {
                    val gap = 3.dp.toPx()
                    val cell = (minOf(this.size.width, this.size.height) - gap) / 2f
                    val ox = (this.size.width - (cell * 2 + gap)) / 2f
                    val oy = (this.size.height - (cell * 2 + gap)) / 2f
                    val indices = listOf(0, 1, 2, 3)
                    indices.forEachIndexed { idx, pIdx ->
                        val r = idx / 2
                        val c = idx % 2
                        drawGlossy3DBlock(
                            topLeft = Offset(ox + c * (cell + gap), oy + r * (cell + gap)),
                            cellSize = cell,
                            style = theme.palette[pIdx % theme.palette.size]
                        )
                    }
                }
            }

            "BLOCK_SKIN" -> {
                val skin = remember(assetKey) { CosmeticCatalog.blockSkinByKey(assetKey) }
                val palette = BlockThemes.simpleClassic.palette
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color(0xFF181232), Color(0xFF0A0717))))
                        .padding(6.dp)
                ) {
                    val gap = 3.dp.toPx()
                    val cell = (minOf(this.size.width, this.size.height) - gap) / 2f
                    val ox = (this.size.width - (cell * 2 + gap)) / 2f
                    val oy = (this.size.height - (cell * 2 + gap)) / 2f
                    for (i in 0 until 4) {
                        drawGlossy3DBlock(
                            topLeft = Offset(ox + (i % 2) * (cell + gap), oy + (i / 2) * (cell + gap)),
                            cellSize = cell,
                            style = palette[i % palette.size],
                            blockSkinStyle = skin.renderStyle
                        )
                    }
                }
            }

            "BOARD_BACKGROUND" -> {
                val bg = remember(assetKey) { CosmeticCatalog.backgroundByKey(assetKey) }
                Box(modifier = Modifier.fillMaxSize()) {
                    EquippedBoardBackground(spec = bg, modifier = Modifier.fillMaxSize())
                    // Mini 3x3 translucent grid emblem showing how board looks over background
                    Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        val s = minOf(this.size.width, this.size.height)
                        val step = s / 3f
                        val ox = (this.size.width - s) / 2f
                        val oy = (this.size.height - s) / 2f
                        for (r in 0 until 3) {
                            for (c in 0 until 3) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.22f),
                                    topLeft = Offset(ox + c * step + 1.5f, oy + r * step + 1.5f),
                                    size = androidx.compose.ui.geometry.Size(step - 3f, step - 3f),
                                    style = Stroke(width = 1.2f)
                                )
                            }
                        }
                    }
                }
            }

            "SPECIAL_EFFECT" -> {
                val fx = remember(assetKey) { CosmeticCatalog.effectByKey(assetKey) }
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(fx.primaryColor.copy(alpha = 0.22f), Color(0xFF0B081A))))
                ) {
                    val center = Offset(this.size.width / 2f, this.size.height / 2f)
                    val radius = this.size.minDimension * 0.36f
                    when (fx.style) {
                        SpecialEffectStyle.LIGHTNING_ARC -> {
                            val bolt = Path().apply {
                                moveTo(this@Canvas.size.width * 0.56f, this@Canvas.size.height * 0.14f)
                                lineTo(this@Canvas.size.width * 0.30f, this@Canvas.size.height * 0.52f)
                                lineTo(this@Canvas.size.width * 0.52f, this@Canvas.size.height * 0.52f)
                                lineTo(this@Canvas.size.width * 0.42f, this@Canvas.size.height * 0.86f)
                                lineTo(this@Canvas.size.width * 0.72f, this@Canvas.size.height * 0.46f)
                                lineTo(this@Canvas.size.width * 0.50f, this@Canvas.size.height * 0.46f)
                                close()
                            }
                            drawPath(bolt, fx.primaryColor)
                            drawPath(bolt, Color.White.copy(alpha = 0.85f), style = Stroke(width = 1.6f))
                        }

                        SpecialEffectStyle.RAINBOW_WAVE -> {
                            val colors = listOf(Color(0xFFFF3B6B), Color(0xFFFFB703), Color(0xFF34D399), Color(0xFF38E8FF), Color(0xFFA855F7))
                            colors.forEachIndexed { idx, col ->
                                val r = radius * (1.15f - idx * 0.16f)
                                drawCircle(col, radius = r.coerceAtLeast(4f), center = center, style = Stroke(width = 3.2f.dp.toPx()))
                            }
                        }

                        SpecialEffectStyle.FIRE_BURST -> {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(Color(0xFFFFF59D), Color(0xFFFF6B00), Color(0xFFDC2626), Color.Transparent),
                                    center = center,
                                    radius = radius * 1.2f
                                ),
                                radius = radius * 1.2f,
                                center = center
                            )
                        }

                        SpecialEffectStyle.SUPERNOVA_EX -> {
                            drawCircle(fx.primaryColor, radius = radius * 0.82f, center = center, style = Stroke(width = 3.dp.toPx()))
                            for (i in 0 until 8) {
                                val a = i * (PI / 4.0)
                                drawLine(
                                    color = if (i % 2 == 0) fx.primaryColor else fx.secondaryColor,
                                    start = Offset(center.x + cos(a).toFloat() * radius * 0.35f, center.y + sin(a).toFloat() * radius * 0.35f),
                                    end = Offset(center.x + cos(a).toFloat() * radius * 1.15f, center.y + sin(a).toFloat() * radius * 1.15f),
                                    strokeWidth = 2.5f.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }

                        else -> {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    listOf(fx.primaryColor.copy(alpha = 0.85f), fx.secondaryColor.copy(alpha = 0.35f), Color.Transparent),
                                    center = center,
                                    radius = radius * 1.1f
                                ),
                                radius = radius * 1.1f,
                                center = center
                            )
                            drawCircle(Color.White.copy(alpha = 0.8f), radius = radius * 0.62f, center = center, style = Stroke(width = 2.dp.toPx()))
                        }
                    }
                }
            }

            "GAME_BOOSTER" -> {
                val bType = BoosterType.fromWire(item.subType) ?: BoosterType.fromWire(assetKey) ?: BoosterType.HAMMER
                val (icon, tint) = when (bType) {
                    BoosterType.HAMMER -> Icons.Filled.Construction to PrismCoral
                    BoosterType.UNDO -> Icons.AutoMirrored.Filled.Undo to PrismCyan
                    BoosterType.SHUFFLE -> Icons.Filled.Shuffle to PrismMint
                    BoosterType.ROW_BLASTER -> Icons.Filled.Bolt to PrismGold
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.radialGradient(listOf(tint.copy(alpha = 0.32f), Color(0xFF120E28)))),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(icon, contentDescription = item.name, tint = tint, modifier = Modifier.size(size * 0.44f))
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = tint.copy(alpha = 0.24f),
                            border = BorderStroke(1.dp, tint)
                        ) {
                            Text(
                                text = "×${item.boosterPackQty.coerceAtLeast(1L)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            "AVATAR" -> {
                val avatar = CosmeticCatalog.avatarByKey(assetKey)
                Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                    EquippedProfileAvatar(
                        photoUrl = null,
                        displayName = item.name,
                        avatarSpec = avatar,
                        frameSpec = null,
                        size = size - 12.dp
                    )
                }
            }

            "PROFILE_FRAME" -> {
                val frame = CosmeticCatalog.frameByKey(assetKey)
                Box(modifier = Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
                    EquippedProfileAvatar(
                        photoUrl = null,
                        displayName = "Frame",
                        avatarSpec = null,
                        frameSpec = frame,
                        size = size - 12.dp
                    )
                }
            }

            else -> {
                val badge = CosmeticCatalog.badgeByKey(assetKey)
                val c1 = badge?.primaryColor ?: accent
                val c2 = badge?.secondaryColor ?: CardSurface
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.linearGradient(listOf(c1.copy(alpha = 0.32f), c2.copy(alpha = 0.25f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(size * 0.56f)
                            .clip(CircleShape)
                            .background(c1.copy(alpha = 0.22f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = item.name,
                            tint = c1,
                            modifier = Modifier.size(size * 0.38f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Theme3DSwatchStrip(theme: BoardThemeSpec, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = theme.boardBgBottom,
        border = BorderStroke(1.dp, theme.boardFrameBorder.copy(alpha = 0.55f)),
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            val count = minOf(7, theme.palette.size)
            val gap = 6.dp.toPx()
            val cellSize = minOf(size.height, (size.width - gap * (count - 1)) / count.toFloat())
            val totalWidth = cellSize * count + gap * (count - 1)
            val startX = (size.width - totalWidth) / 2f
            val startY = (size.height - cellSize) / 2f
            for (i in 0 until count) {
                drawGlossy3DBlock(
                    topLeft = Offset(startX + i * (cellSize + gap), startY),
                    cellSize = cellSize,
                    style = theme.palette[i]
                )
            }
        }
    }
}
