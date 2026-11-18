package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.GameMode
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.game.CosmeticCatalog
import com.example.game.GameProgressCalculator
import com.example.ui.components.DualWalletBalanceStrip
import com.example.ui.components.StatusPill
import com.example.ui.game.EquippedBadgePill
import com.example.ui.game.EquippedProfileAvatar
import com.example.ui.game.Prism3DProgressBar
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismGoldWarm
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AppViewModel

@Composable
fun HomeDashboardScreen(
    appViewModel: AppViewModel,
    onOpenDrawer: () -> Unit,
    onStartClassic: () -> Unit,
    onOpenLevelSelector: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenRewards: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenAdminPanel: () -> Unit
) {
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val inventory by appViewModel.playerInventory.collectAsStateWithLifecycle()
    val adminAccess by appViewModel.adminAccess.collectAsStateWithLifecycle()
    val activeChallenges by appViewModel.activeTimeChallenges.collectAsStateWithLifecycle()
    val dailyClaim by appViewModel.dailyChallengeClaim.collectAsStateWithLifecycle()

    val equippedAvatar = CosmeticCatalog.avatarByKey(inventory.equippedAvatarId)
    val equippedFrame = CosmeticCatalog.frameByKey(inventory.equippedFrameId)
    val equippedBadge = CosmeticCatalog.badgeByKey(inventory.equippedBadgeId)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Top App Bar with Side Drawer trigger + Admin Dashboard badge if authorized
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onOpenDrawer,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("open_drawer_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Menu,
                        contentDescription = "Open navigation menu",
                        tint = Color.White
                    )
                }
                EquippedProfileAvatar(
                    photoUrl = user?.photoUrl,
                    displayName = user?.displayName ?: "Player",
                    avatarSpec = equippedAvatar,
                    frameSpec = equippedFrame,
                    size = 42.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Prism Block 3D",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = user?.displayName ?: "Player",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        equippedBadge?.let { badge ->
                            EquippedBadgePill(badgeSpec = badge)
                        }
                    }
                }
                if (adminAccess.isAdmin) {
                    Surface(
                        onClick = onOpenAdminPanel,
                        shape = RoundedCornerShape(14.dp),
                        color = PrismGold.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, PrismGold),
                        modifier = Modifier.testTag("home_admin_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AdminPanelSettings,
                                contentDescription = "Open Admin Panel",
                                tint = PrismGold,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (adminAccess.isSuperAdmin) "Super Admin" else "Admin",
                                style = MaterialTheme.typography.labelLarge,
                                color = PrismGold
                            )
                        }
                    }
                }
            }
        }

        // Dual Wallet Strip (Main PKR Wallet + Coin Wallet)
        item {
            DualWalletBalanceStrip(
                mainWalletPaisa = user?.mainWalletPkrPaisa ?: 0L,
                purchasedCoins = user?.purchasedCoins ?: 0L,
                earnedCoins = user?.earnedCoins ?: 0L,
                onWalletClick = onOpenWallet
            )
        }

        // Hero Banner Card with generated 3D crystal artwork
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .clickable(onClick = onStartClassic)
                    .testTag("home_hero_play_card")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_home_banner),
                    contentDescription = "3D crystal blocks hero banner",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xEE0D0A1E), Color(0xBB161130), Color(0x33161130))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusPill(
                        label = "BEST CLASSIC: %,d".format(user?.classicHighScore ?: 0L),
                        color = PrismCyan
                    )
                    Column {
                        Text(
                            text = "Drop 3D Blocks.\nClear Lines. Earn Coins.",
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = onStartClassic,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrismCyan,
                                contentColor = Color(0xFF041A24)
                            ),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("hero_play_classic_button")
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Play Classic", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // 4 Game Modes Section
        item {
            Text(
                text = "GAME MODES",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeShortcutCard(
                    title = "Classic",
                    subtitle = "Endless high score",
                    badge = "Best: ${user?.classicHighScore ?: 0L}",
                    icon = Icons.Filled.GridView,
                    accent = PrismCyan,
                    onClick = onStartClassic,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_card_classic")
                )
                ModeShortcutCard(
                    title = "100 Levels",
                    subtitle = "1,000 lines / level",
                    badge = "Level ${user?.currentLevel ?: 1L}/100",
                    icon = Icons.Filled.Stairs,
                    accent = PrismGold,
                    onClick = onOpenLevelSelector,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_card_levels")
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeShortcutCard(
                    title = "Daily Puzzle",
                    subtitle = "Free daily seed",
                    badge = if (dailyClaim != null) "Claimed" else "Free Bonus",
                    icon = Icons.Filled.CalendarToday,
                    accent = PrismMint,
                    onClick = onOpenChallenges,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_card_daily")
                )
                ModeShortcutCard(
                    title = "Time Arena",
                    subtitle = "Admin-hosted events",
                    badge = "${activeChallenges.size} Live",
                    icon = Icons.Filled.Timer,
                    accent = PrismCoral,
                    onClick = onOpenChallenges,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("mode_card_time")
                )
            }
        }

        // Level Progress Snapshot Card
        item {
            val curLvl = user?.currentLevel ?: 1L
            val curLines = user?.currentLevelLines ?: 0L
            val lvlMetrics = GameProgressCalculator.computeLevelProgress(
                levelNumber = curLvl,
                clearedLines = curLines,
                requiredLines = DEFAULT_LINES_PER_LEVEL
            )
            Card(
                onClick = onOpenLevelSelector,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Active Level Journey", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "Level $curLvl of $MAX_LEVEL_NUMBER • ${user?.completedLevelsCount ?: 0} completed",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = lvlMetrics.percentageText,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.ExtraBold
                                ),
                                color = PrismGold
                            )
                            Text(
                                text = "${lvlMetrics.countText} lines",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = JetBrainsMonoFontFamily
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Prism3DProgressBar(
                        progress = lvlMetrics.fraction,
                        gradientColors = listOf(PrismGold, PrismGoldWarm, PrismCoral),
                        glowColor = PrismGoldWarm,
                        barHeight = 12.dp,
                        contentDesc = "Level $curLvl progress: ${lvlMetrics.countText} lines, ${lvlMetrics.percentageText}",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Quick Action Tiles: Daily Rewards, Coin Shop & Pastel Themes, Leaderboards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickSquareAction(
                    icon = Icons.Filled.Redeem,
                    label = "Day ${user?.dailyRewardNextDay ?: 1L} Gift",
                    accent = PrismGold,
                    onClick = onOpenRewards,
                    modifier = Modifier.weight(1f)
                )
                QuickSquareAction(
                    icon = Icons.Filled.Palette,
                    label = "Coin Shop",
                    accent = PrismViolet,
                    onClick = onOpenShop,
                    modifier = Modifier.weight(1f)
                )
                QuickSquareAction(
                    icon = Icons.Filled.Leaderboard,
                    label = "Rankings",
                    accent = PrismCyan,
                    onClick = onOpenLeaderboard,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PlayModesAndLevelsScreen(
    appViewModel: AppViewModel,
    onLaunchClassic: () -> Unit,
    onLaunchLevel: (Long) -> Unit,
    onOpenChallenges: () -> Unit
) {
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val levelConfigs by appViewModel.levelConfigs.collectAsStateWithLifecycle()
    val currentLevel = (user?.currentLevel ?: 1L).toInt().coerceIn(1, MAX_LEVEL_NUMBER)
    val currentLevelLines = user?.currentLevelLines ?: 0L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = "Play Modes & 100 Levels",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White
        )
        Text(
            text = "1 cleared line = +10 Score & +1 Coin • Each level requires 1,000 cleared lines.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        // Quick launch buttons for Classic & Challenges
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onLaunchClassic,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("play_classic_mode_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24))
            ) {
                Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Classic Endless", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onOpenChallenges,
                modifier = Modifier
                    .weight(1f)
                    .height(50.dp)
                    .testTag("play_challenges_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = CardSurface, contentColor = PrismCoral),
                border = BorderStroke(1.dp, PrismCoral.copy(alpha = 0.6f))
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Daily & Time", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Active Level Spotlight
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = DeepSurface,
            border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current Level $currentLevel",
                        style = MaterialTheme.typography.titleMedium,
                        color = PrismGold
                    )
                    Text(
                        text = "$currentLevelLines / $DEFAULT_LINES_PER_LEVEL lines • Reward: +${levelConfigs[currentLevel.toLong()]?.rewardCoins ?: (100L + currentLevel * 10L)} Coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onLaunchLevel(currentLevel.toLong()) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00)),
                    modifier = Modifier.testTag("play_current_level_button")
                ) {
                    Text("Play", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 100-Level Interactive Map Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 76.dp),
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items((1..MAX_LEVEL_NUMBER).toList(), key = { it }) { lvl ->
                val isCompleted = lvl < currentLevel || (user?.completedLevelsCount ?: 0L) >= lvl
                val isCurrent = lvl == currentLevel
                val isUnlocked = lvl <= currentLevel

                Surface(
                    onClick = { if (isUnlocked) onLaunchLevel(lvl.toLong()) },
                    enabled = isUnlocked,
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        isCurrent -> PrismGold.copy(alpha = 0.22f)
                        isCompleted -> PrismMint.copy(alpha = 0.14f)
                        else -> DeepSurface
                    },
                    border = BorderStroke(
                        width = if (isCurrent) 2.dp else 1.dp,
                        color = when {
                            isCurrent -> PrismGold
                            isCompleted -> PrismMint.copy(alpha = 0.5f)
                            else -> SurfaceBorder.copy(alpha = 0.5f)
                        }
                    ),
                    modifier = Modifier
                        .height(74.dp)
                        .testTag("level_tile_$lvl")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        when {
                            isCompleted -> Icon(Icons.Filled.CheckCircle, contentDescription = "Completed", tint = PrismMint, modifier = Modifier.size(16.dp))
                            !isUnlocked -> Icon(Icons.Filled.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f), modifier = Modifier.size(16.dp))
                            else -> Icon(Icons.Filled.PlayArrow, contentDescription = "Current level", tint = PrismGold, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "LVL $lvl",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isUnlocked) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                        )
                        if (isCurrent) {
                            Text(
                                text = "${(currentLevelLines * 100 / DEFAULT_LINES_PER_LEVEL)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = PrismGold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeShortcutCard(
    title: String,
    subtitle: String,
    badge: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = CardSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = accent, modifier = Modifier.size(20.dp))
                }
                StatusPill(label = badge, color = accent)
            }
            Spacer(Modifier.height(12.dp))
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = Color.White)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun QuickSquareAction(
    icon: ImageVector,
    label: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(78.dp),
        shape = RoundedCornerShape(18.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
        }
    }
}
