package com.example.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.UserAccount
import com.example.data.model.formatPkr
import com.example.ui.components.ScreenHeader
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.MidnightBg
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCrimson
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AdminAccessState
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AppViewModel

enum class AdminSection(val label: String) {
    OVERVIEW("Overview"),
    PLAYERS("Players"),
    WALLETS("Wallets"),
    DEPOSITS("Deposits"),
    WITHDRAWALS("Withdrawals"),
    LEVELS("Levels"),
    CHALLENGES("Challenges"),
    REWARDS("Daily Rewards"),
    SHOP("Coin Shop"),
    SETTINGS("Settings"),
    REPORTS("Reports"),
    ADMINS("Admins & Logs")
}

@Composable
fun AdminDashboardScreen(
    appViewModel: AppViewModel,
    adminViewModel: AdminViewModel,
    adminAccess: AdminAccessState,
    onExitAdmin: () -> Unit
) {
    BackHandler { onExitAdmin() }

    var activeSection by remember { mutableStateOf(AdminSection.OVERVIEW) }
    val feedback by adminViewModel.feedback.collectAsStateWithLifecycle()
    val busy by adminViewModel.busy.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF14102B), MidnightBg)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        ScreenHeader(
            title = "Admin Control Panel",
            subtitle = if (adminAccess.isSuperAdmin) "Super Admin • Full Governance" else "Authorized Admin",
            onBack = onExitAdmin,
            trailing = {
                StatusPill(
                    label = if (adminAccess.isSuperAdmin) "SUPER ADMIN" else "ADMIN",
                    color = PrismGold
                )
            }
        )

        AnimatedVisibility(visible = feedback != null) {
            val msg = feedback.orEmpty()
            val isError = msg.startsWith("Error:")
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isError) PrismCrimson.copy(alpha = 0.2f) else PrismMint.copy(alpha = 0.2f),
                border = BorderStroke(1.dp, if (isError) PrismCrimson else PrismMint),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { adminViewModel.clearFeedback() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // 12-Section Navigation Strip
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(AdminSection.entries) { section ->
                FilterChip(
                    selected = activeSection == section,
                    onClick = { activeSection = section },
                    label = { Text(section.label) },
                    modifier = Modifier.testTag("admin_tab_${section.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrismGold,
                        selectedLabelColor = Color(0xFF241A00)
                    )
                )
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeSection) {
                AdminSection.OVERVIEW -> AdminOverviewTab(adminViewModel, busy)
                AdminSection.PLAYERS -> AdminPlayersTab(adminViewModel, busy)
                AdminSection.WALLETS -> AdminWalletsTab(adminViewModel, busy)
                AdminSection.DEPOSITS -> AdminDepositsTab(adminViewModel, busy)
                AdminSection.WITHDRAWALS -> AdminWithdrawalsTab(adminViewModel, busy)
                AdminSection.LEVELS -> AdminLevelsTab(adminViewModel, busy)
                AdminSection.CHALLENGES -> AdminTimeChallengesTab(adminViewModel, busy)
                AdminSection.REWARDS -> AdminDailyRewardsTab(adminViewModel, busy)
                AdminSection.SHOP -> AdminCoinShopTab(adminViewModel, adminAccess, busy)
                AdminSection.SETTINGS -> AdminSettingsTab(appViewModel, adminViewModel, busy)
                AdminSection.REPORTS -> AdminReportsTab(adminViewModel)
                AdminSection.ADMINS -> AdminGovernanceTab(adminViewModel, adminAccess, busy)
            }
        }
    }
}

@Composable
private fun AdminOverviewTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val deposits by adminViewModel.allDeposits.collectAsStateWithLifecycle()
    val withdrawals by adminViewModel.allWithdrawals.collectAsStateWithLifecycle()
    val transactions by adminViewModel.allTransactions.collectAsStateWithLifecycle()
    val logs by adminViewModel.auditLogs.collectAsStateWithLifecycle()

    val activePlayers = users.count { !it.blocked }
    val approvedDepositsPaisa = deposits.filter { it.status == "APPROVED" }.sumOf { it.amountPaisa }
    val paidWithdrawalsPaisa = withdrawals.filter { it.status == "PAID" || it.status == "APPROVED" }.sumOf { it.amountPaisa }
    val totalCoinsInCirculation = users.sumOf { it.totalCoins }
    val pendingDeposits = deposits.count { it.status == "PENDING" || it.status == "PROCESSING" }
    val pendingWithdrawals = withdrawals.count { it.status == "PENDING" || it.status == "PROCESSING" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Seed Catalog / 100 Levels Action
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.55f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Bootstrap Firestore Game Data", style = MaterialTheme.typography.titleSmall, color = PrismGold)
                        Text(
                            "Seeds global settings, full 9-Category Coin Shop (38 items), 7 Daily Rewards & all 100 Level configs safely without overwriting custom prices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { adminViewModel.seedDefaults() },
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00)),
                        modifier = Modifier.testTag("admin_seed_defaults_button")
                    ) {
                        Icon(Icons.Filled.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Seed All", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // KPI Metrics 2-Column Grid
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("TOTAL PLAYERS", "${users.size}", PrismCyan, Icons.Filled.Group, Modifier.weight(1f))
                StatMetricCard("ACTIVE PLAYERS", "$activePlayers", PrismMint, Icons.Filled.CheckCircle, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("APPROVED DEPOSITS", formatPkr(approvedDepositsPaisa), PrismMint, Icons.Filled.ArrowDownward, Modifier.weight(1f))
                StatMetricCard("PAID WITHDRAWALS", formatPkr(paidWithdrawalsPaisa), PrismCoral, Icons.Filled.ArrowUpward, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("COINS ISSUED", "%,d".format(totalCoinsInCirculation), PrismGold, Icons.Filled.MonetizationOn, Modifier.weight(1f))
                StatMetricCard("TRANSACTIONS", "${transactions.size}", PrismViolet, Icons.Filled.ReceiptLong, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("PENDING DEPOSITS", "$pendingDeposits", PrismGold, Icons.Filled.AccountBalanceWallet, Modifier.weight(1f))
                StatMetricCard("PENDING PAYOUTS", "$pendingWithdrawals", PrismCoral, Icons.Filled.AdminPanelSettings, Modifier.weight(1f))
            }
        }

        item {
            Text("RECENT ADMIN ACTIVITY (${logs.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(logs.take(10), key = { it.id }) { log ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, style = MaterialTheme.typography.labelLarge, color = PrismCyan)
                        Text(log.targetType, style = MaterialTheme.typography.labelSmall, color = PrismGold)
                    }
                    Text(log.details, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("By: ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AdminPlayersTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }

    val filtered = remember(users, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) users else users.filter {
            it.displayName.lowercase().contains(q) ||
                it.email.lowercase().contains(q) ||
                it.userId.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search players by name, email, or UID") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_player_search")
            )
        }

        items(filtered, key = { it.id }) { player ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, if (player.blocked) PrismCrimson else SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(player.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            Text(player.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusPill(
                            label = if (player.blocked) "BLOCKED" else "ACTIVE",
                            color = if (player.blocked) PrismCrimson else PrismMint
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Main: ${formatPkr(player.mainWalletPkrPaisa)} • Coins: ${player.totalCoins} (P:${player.purchasedCoins} / E:${player.earnedCoins})",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = PrismGold
                    )
                    Text(
                        text = "Best: ${player.classicHighScore} • Lvl ${player.currentLevel} (${player.currentLevelLines}/1000) • Lines: ${player.totalLinesCleared}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (player.walletVerificationCompleted) {
                        Text(
                            text = "Payout Info: ${player.walletVerificationName} • ${player.walletVerificationProvider} (${player.walletVerificationAccount})",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrismMint
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { adminViewModel.togglePlayerBlocked(player.userId, !player.blocked) },
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (player.blocked) PrismMint else PrismCrimson),
                        modifier = Modifier.testTag("admin_block_player_${player.userId}")
                    ) {
                        Icon(
                            imageVector = if (player.blocked) Icons.Filled.CheckCircle else Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (player.blocked) PrismMint else PrismCrimson,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (player.blocked) "Unblock Player" else "Block Player",
                            color = if (player.blocked) PrismMint else PrismCrimson
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminWalletsTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val transactions by adminViewModel.allTransactions.collectAsStateWithLifecycle()
    var selectedPlayer by remember(users) { mutableStateOf<UserAccount?>(users.firstOrNull()) }
    var coinSource by remember { mutableStateOf("EARNED") }
    var amountInput by remember { mutableStateOf("100") }
    var reason by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Adjust Player Coin Wallet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(Modifier.height(8.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(users, key = { it.id }) { u ->
                            FilterChip(
                                selected = selectedPlayer?.userId == u.userId,
                                onClick = { selectedPlayer = u },
                                label = { Text(u.displayName) }
                            )
                        }
                    }

                    selectedPlayer?.let { p ->
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("${p.displayName} (${p.email})", style = MaterialTheme.typography.labelLarge, color = Color.White)
                                Text(
                                    "Main: ${formatPkr(p.mainWalletPkrPaisa)} • Purchased: ${p.purchasedCoins} • Earned: ${p.earnedCoins}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = PrismCyan
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = coinSource == "EARNED",
                            onClick = { coinSource = "EARNED" },
                            label = { Text("Earned Coins") }
                        )
                        FilterChip(
                            selected = coinSource == "PURCHASED",
                            onClick = { coinSource = "PURCHASED" },
                            label = { Text("Purchased Coins") }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { c -> c.isDigit() }.take(8) },
                        label = { Text("Coin Amount") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it.take(120) },
                        label = { Text("Mandatory Audit Reason") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        val amt = amountInput.toLongOrNull() ?: 0L
                        Button(
                            onClick = {
                                selectedPlayer?.let {
                                    adminViewModel.adjustCoins(it.userId, coinSource, amt, reason)
                                    reason = ""
                                }
                            },
                            enabled = !busy && selectedPlayer != null && amt > 0L && reason.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismMint, contentColor = Color(0xFF042218))
                        ) {
                            Text("+ Add Coins", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                selectedPlayer?.let {
                                    adminViewModel.adjustCoins(it.userId, coinSource, -amt, reason)
                                    reason = ""
                                }
                            },
                            enabled = !busy && selectedPlayer != null && amt > 0L && reason.isNotBlank(),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismCrimson, contentColor = Color.White)
                        ) {
                            Text("- Deduct Coins", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Text("ALL WALLET TRANSACTIONS (${transactions.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(transactions.take(40), key = { it.id }) { tx ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tx.description.ifBlank { tx.txType }, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text("UID: ${tx.userId.take(10)} • ${tx.walletType} (${tx.coinSource})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = if (tx.walletType == "MAIN_PKR") formatPkr(tx.amountDelta) else "%+,d Coins".format(tx.amountDelta),
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = if (tx.amountDelta >= 0) PrismMint else PrismCoral
                    )
                }
            }
        }
    }
}
