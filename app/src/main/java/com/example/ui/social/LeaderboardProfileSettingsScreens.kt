package com.example.ui.social

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stairs
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.game.BlockThemes
import com.example.game.CosmeticCatalog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ScreenHeader
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.game.EquippedBadgePill
import com.example.ui.game.EquippedProfileAvatar
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCrimson
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AppViewModel

@Composable
fun LeaderboardScreen(
    appViewModel: AppViewModel,
    onBack: () -> Unit
) {
    var showDaily by remember { mutableStateOf(true) }
    val dailyEntries by appViewModel.dailyLeaderboard.collectAsStateWithLifecycle()
    val allTimeEntries by appViewModel.allTimeLeaderboard.collectAsStateWithLifecycle()
    val activeList = if (showDaily) dailyEntries else allTimeEntries

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Leaderboards",
            subtitle = "Verified Cloud Firestore scores — zero private data exposed",
            onBack = onBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = showDaily,
                onClick = { showDaily = true },
                label = { Text("Daily Leaderboard") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("leaderboard_tab_daily"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrismCyan,
                    selectedLabelColor = Color(0xFF041A24)
                )
            )
            FilterChip(
                selected = !showDaily,
                onClick = { showDaily = false },
                label = { Text("All-Time Best") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("leaderboard_tab_alltime"),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = PrismGold,
                    selectedLabelColor = Color(0xFF241A00)
                )
            )
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (activeList.isEmpty()) {
                item {
                    EmptyStatePanel(
                        icon = Icons.Filled.Leaderboard,
                        title = if (showDaily) "No Daily Scores Yet" else "No All-Time Scores Yet",
                        description = "Play Classic, Level, or Daily Challenge modes to climb the global leaderboard!"
                    )
                }
            } else {
                itemsIndexed(activeList, key = { _, item -> item.id }) { idx, entry ->
                    val rank = idx + 1
                    val isMe = entry.userId == appViewModel.currentUserId
                    val rankColor = when (rank) {
                        1 -> PrismGold
                        2 -> Color(0xFFE2E8F0)
                        3 -> Color(0xFFFB923C)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isMe) CardSurface else DeepSurface,
                        border = BorderStroke(
                            width = if (isMe || rank <= 3) 1.5.dp else 1.dp,
                            color = if (isMe) PrismCyan else if (rank <= 3) rankColor.copy(alpha = 0.6f) else SurfaceBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(rankColor.copy(alpha = 0.16f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "#$rank",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = rankColor
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(entry.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    if (isMe) StatusPill("YOU", PrismCyan)
                                }
                                Text(
                                    text = "Level ${entry.highestLevel} • %,d lines • %d games".format(entry.totalLines, entry.gamesPlayed),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "%,d".format(entry.bestScore),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (rank == 1) PrismGold else PrismCyan
                                )
                                Text("SCORE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    appViewModel: AppViewModel,
    onBack: () -> Unit,
    onSignOut: () -> Unit
) {
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val inventory by appViewModel.playerInventory.collectAsStateWithLifecycle()
    val adminAccess by appViewModel.adminAccess.collectAsStateWithLifecycle()
    val isBusy by appViewModel.isBusy.collectAsStateWithLifecycle()
    var editName by remember(user?.displayName) { mutableStateOf(user?.displayName ?: "") }

    val equippedAvatar = remember(inventory.equippedAvatarId) { CosmeticCatalog.avatarByKey(inventory.equippedAvatarId) }
    val equippedFrame = remember(inventory.equippedFrameId) { CosmeticCatalog.frameByKey(inventory.equippedFrameId) }
    val equippedBadge = remember(inventory.equippedBadgeId) { CosmeticCatalog.badgeByKey(inventory.equippedBadgeId) }
    val totalBoosters = inventory.hammerCount + inventory.undoCount + inventory.shuffleCount + inventory.rowBlasterCount

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Player Profile",
            subtitle = "Synced with Google & Cloud Firestore",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    border = BorderStroke(1.dp, equippedFrame?.ringStart?.copy(alpha = 0.65f) ?: SurfaceBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EquippedProfileAvatar(
                            photoUrl = user?.photoUrl,
                            displayName = user?.displayName ?: "Player",
                            avatarSpec = equippedAvatar,
                            frameSpec = equippedFrame,
                            size = 86.dp
                        )

                        Spacer(Modifier.height(10.dp))
                        Text(user?.displayName ?: "Player", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (equippedBadge != null) {
                            Spacer(Modifier.height(6.dp))
                            EquippedBadgePill(badgeSpec = equippedBadge)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatusPill(
                                label = when {
                                    adminAccess.isSuperAdmin -> "SUPER ADMIN"
                                    adminAccess.isAdmin -> "ADMIN"
                                    else -> "VERIFIED PLAYER"
                                },
                                color = if (adminAccess.isAdmin) PrismGold else PrismCyan
                            )
                            StatusPill(
                                label = if (user?.walletVerificationCompleted == true) "WALLET FORM SAVED" else "WALLET UNVERIFIED",
                                color = if (user?.walletVerificationCompleted == true) PrismMint else PrismCoral
                            )
                        }

                        Spacer(Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it.take(40) },
                                label = { Text("Display Name") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("profile_name_input")
                            )
                            Button(
                                onClick = { appViewModel.updateDisplayName(editName) },
                                enabled = !isBusy && editName.trim().length >= 2 && editName.trim() != user?.displayName,
                                modifier = Modifier
                                    .height(54.dp)
                                    .testTag("profile_save_name_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24))
                            ) {
                                Icon(Icons.Filled.Save, contentDescription = "Save name")
                            }
                        }
                    }
                }
            }

            // Lifetime Stats 2x2 Grid
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatMetricCard(
                        label = "CLASSIC BEST",
                        value = "%,d".format(user?.classicHighScore ?: 0L),
                        accent = PrismCyan,
                        icon = Icons.Filled.EmojiEvents,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "TOTAL LINES",
                        value = "%,d".format(user?.totalLinesCleared ?: 0L),
                        accent = PrismMint,
                        icon = Icons.Filled.GridView,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatMetricCard(
                        label = "CURRENT LEVEL",
                        value = "LVL ${user?.currentLevel ?: 1L}",
                        accent = PrismGold,
                        icon = Icons.Filled.Stairs,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        label = "COLLECTION",
                        value = "${maxOf(inventory.ownedItemIds.size, user?.unlockedThemeIds?.size ?: 1)} Items • ${totalBoosters}x",
                        accent = PrismViolet,
                        icon = Icons.Filled.Palette,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("profile_sign_out_button"),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, PrismCrimson.copy(alpha = 0.65f))
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = PrismCrimson)
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out", color = PrismCrimson, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    appViewModel: AppViewModel,
    onBack: () -> Unit
) {
    val audioPrefs by appViewModel.audioPreferences.collectAsStateWithLifecycle()
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val isBusy by appViewModel.isBusy.collectAsStateWithLifecycle()
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Settings & Audio",
            subtitle = "Independent Music, SFX & Withdrawal Password controls",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Sound & Music Card
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Audio Preferences", style = MaterialTheme.typography.titleMedium, color = Color.White)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.MusicNote, contentDescription = null, tint = PrismCyan)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Background Music", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("Ambient crystalline synth loop", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = audioPrefs.musicEnabled,
                                onCheckedChange = { appViewModel.setMusicEnabled(it) },
                                modifier = Modifier.testTag("settings_music_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = PrismGold)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Sound Effects (SFX)", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    Text("Placement, line clear, coin chime, level up & game over", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Switch(
                                checked = audioPrefs.sfxEnabled,
                                onCheckedChange = { appViewModel.setSfxEnabled(it) },
                                modifier = Modifier.testTag("settings_sfx_switch")
                            )
                        }

                        OutlinedButton(
                            onClick = { appViewModel.soundEngine.playCoinReward() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_test_audio_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Test 3D Coin Reward Chime")
                        }
                    }
                }
            }

            // Quick Theme Selector (for unlocked themes)
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Active 3D Block Theme", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        val owned = user?.unlockedThemeIds.orEmpty().toSet()
                        BlockThemes.allThemes.filter { it.defaultPriceCoins == 0L || it.id in owned }.forEach { spec ->
                            val selected = user?.selectedThemeId == spec.id
                            Surface(
                                onClick = { appViewModel.selectTheme(spec.id) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) CardSurface else Color.Transparent,
                                border = BorderStroke(1.dp, if (selected) PrismCyan else SurfaceBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(spec.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    if (selected) StatusPill("ACTIVE", PrismCyan)
                                }
                            }
                        }
                    }
                }
            }

            // Withdrawal Security Password Update
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = PrismGold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (user?.hasWithdrawalPin == true) "Update Withdrawal Password" else "Create Withdrawal Password",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it.take(32) },
                            label = { Text("New Password (min 6 characters)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it.take(32) },
                            label = { Text("Confirm Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newPin == confirmPin) {
                                    appViewModel.setWithdrawalSecurityPassword(newPin)
                                    newPin = ""
                                    confirmPin = ""
                                } else {
                                    appViewModel.showError("Passwords do not match.")
                                }
                            },
                            enabled = !isBusy && newPin.length >= 6 && newPin == confirmPin,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00))
                        ) {
                            Text("Save Security Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HelpSupportScreen(
    onBack: () -> Unit
) {
    val faqs = remember {
        listOf(
            "How do scoring & Coin rewards work?" to
                "Every cleared horizontal or vertical line on the 10×10 board awards exactly +10 Score and +1 Coin. Clearing 2 lines simultaneously awards +20 Score and +2 Coins, 3 lines = +30 Score and +3 Coins, verified securely in Firestore.",
            "How do the 100 Levels work?" to
                "Each of the 100 levels requires 1,000 cleared lines. Progress saves automatically after sessions so you can play across multiple sittings. Completing a level unlocks the next level and grants a Coin completion bonus.",
            "What happens if I miss a day of Daily Login Rewards?" to
                "Your 7-day cycle NEVER resets! If you claimed Day 3 and return days later, you resume right at Day 4. After Day 7, the cycle loops smoothly back to Day 1.",
            "What is the difference between Purchased Coins and Earned Coins?" to
                "Purchased Coins (bought from your Main PKR Wallet at 1 Coin = PKR 0.05) can convert back to PKR. Earned/Promotional Coins (won from gameplay, daily login rewards, or challenges) are used for Premium Pastel 3D Themes, shop items, and Game Over continues unless the Super Admin enables earned-coin cashout.",
            "Why are Deposits & Withdrawals reviewed by an Admin?" to
                "Payment gateways run in Sandbox / Manual Verification mode until live JazzCash, Easypaisa, or Bank API merchant keys are configured. Deposits never auto-credit, and Withdrawals (min PKR 500) require your Withdrawal Security Password and Admin confirmation before being marked Paid.",
            "Why does 'Watch Rewarded Ad' show Configuration Required?" to
                "For fairness, rewarded ad continues only unlock after a verified ad SDK completion callback. Until AdMob App ID, Rewarded Ad Unit ID, and Server-Side Verification credentials are configured, free continues are disabled."
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Help & Support",
            subtitle = "Rules, Wallet Compliance & Fair Play Guide",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            itemsIndexed(faqs) { _, (q, a) ->
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.HelpOutline, contentDescription = null, tint = PrismCyan, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(q, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(a, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
