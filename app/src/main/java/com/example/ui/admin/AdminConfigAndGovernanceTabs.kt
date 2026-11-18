package com.example.ui.admin

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminPermissions
import com.example.data.model.DailyRewardConfig
import com.example.data.model.SUPER_ADMIN_EMAIL
import com.example.data.model.ShopItem
import com.example.data.model.UserAccount
import com.example.data.model.formatPkr
import com.example.game.BlockThemes
import com.example.ui.components.StatusPill
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
import com.example.viewmodel.AdminAccessState
import com.example.viewmodel.AdminViewModel
import com.example.viewmodel.AppViewModel

@Composable
fun AdminDailyRewardsTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val rewards by adminViewModel.allDailyRewards.collectAsStateWithLifecycle()
    val list = remember(rewards) {
        (if (rewards.size >= 7) rewards else BlockThemes.defaultDailyRewards()).sortedBy { it.dayNumber }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(list, key = { it.dayNumber }) { cfg ->
            AdminDailyRewardEditorCard(cfg, busy) { updated ->
                adminViewModel.saveDailyReward(updated)
            }
        }
    }
}

@Composable
private fun AdminDailyRewardEditorCard(
    cfg: DailyRewardConfig,
    busy: Boolean,
    onSave: (DailyRewardConfig) -> Unit
) {
    var title by remember(cfg) { mutableStateOf(cfg.title) }
    var desc by remember(cfg) { mutableStateOf(cfg.description) }
    var coins by remember(cfg) { mutableStateOf(cfg.rewardCoins.toString()) }
    var bonusItem by remember(cfg) { mutableStateOf(cfg.bonusItemName) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Day ${cfg.dayNumber} Reward", style = MaterialTheme.typography.titleMedium, color = PrismGold)
                Button(
                    onClick = {
                        onSave(
                            cfg.copy(
                                id = "day_${cfg.dayNumber}",
                                title = title.trim().ifBlank { "Day ${cfg.dayNumber} Gift" },
                                description = desc.trim(),
                                rewardCoins = (coins.toLongOrNull() ?: 10L).coerceAtLeast(1L),
                                bonusItemName = bonusItem.trim()
                            )
                        )
                    },
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00))
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it.take(40) }, label = { Text("Title") }, singleLine = true, modifier = Modifier.weight(1.3f))
                OutlinedTextField(
                    value = coins,
                    onValueChange = { coins = it.filter { c -> c.isDigit() }.take(6) },
                    label = { Text("Coins") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(0.7f)
                )
            }
            OutlinedTextField(value = desc, onValueChange = { desc = it.take(100) }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = bonusItem, onValueChange = { bonusItem = it.take(40) }, label = { Text("Bonus Cosmetic Label (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AdminSettingsTab(
    appViewModel: AppViewModel,
    adminViewModel: AdminViewModel,
    busy: Boolean
) {
    val current by appViewModel.globalSettings.collectAsStateWithLifecycle()

    var continueCost by remember(current) { mutableStateOf(current.continueCostCoins.toString()) }
    var exchangePaisa by remember(current) { mutableStateOf(current.exchangeRatePaisaPerCoin.toString()) }
    var minWithdrawalRupees by remember(current) { mutableStateOf((current.minWithdrawalPaisa / 100L).toString()) }
    var maxDailyWd by remember(current) { mutableStateOf(current.maxDailyWithdrawals.toString()) }
    var allowEarnedCashout by remember(current) { mutableStateOf(current.allowEarnedCoinCashout) }
    var jazzEnabled by remember(current) { mutableStateOf(current.jazzCashEnabled) }
    var easyEnabled by remember(current) { mutableStateOf(current.easypaisaEnabled) }
    var bankEnabled by remember(current) { mutableStateOf(current.bankTransferEnabled) }
    var jazzInstr by remember(current) { mutableStateOf(current.jazzCashInstructions) }
    var easyInstr by remember(current) { mutableStateOf(current.easypaisaInstructions) }
    var bankInstr by remember(current) { mutableStateOf(current.bankTransferInstructions) }
    var sandboxMode by remember(current) { mutableStateOf(current.paymentSandboxMode) }
    var rewardedAdReady by remember(current) { mutableStateOf(current.rewardedAdConfigured) }
    var gameEnabled by remember(current) { mutableStateOf(current.gameEnabled) }
    var maintenanceMsg by remember(current) { mutableStateOf(current.maintenanceMessage) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Core Game & Economy Rules", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = continueCost,
                            onValueChange = { continueCost = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Continue Cost (Coins)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = exchangePaisa,
                            onValueChange = { exchangePaisa = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("1 Coin = Paisa (5=0.05)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minWithdrawalRupees,
                            onValueChange = { minWithdrawalRupees = it.filter { c -> c.isDigit() }.take(6) },
                            label = { Text("Min Withdrawal (PKR)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxDailyWd,
                            onValueChange = { maxDailyWd = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Max Withdrawals/Day") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    SettingToggleRow("Allow Earned/Promo Coin Cashout (Compliance)", allowEarnedCashout) { allowEarnedCashout = it }
                    SettingToggleRow("Payment Sandbox / Manual Verification Mode", sandboxMode) { sandboxMode = it }
                    SettingToggleRow("Rewarded Ad SDK Credentials Configured", rewardedAdReady) { rewardedAdReady = it }
                    SettingToggleRow("Game Service Enabled", gameEnabled) { gameEnabled = it }
                    SettingToggleRow("Enable JazzCash", jazzEnabled) { jazzEnabled = it }
                    SettingToggleRow("Enable Easypaisa", easyEnabled) { easyEnabled = it }
                    SettingToggleRow("Enable Bank Transfer", bankEnabled) { bankEnabled = it }

                    OutlinedTextField(value = jazzInstr, onValueChange = { jazzInstr = it.take(300) }, label = { Text("JazzCash Deposit Instructions") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = easyInstr, onValueChange = { easyInstr = it.take(300) }, label = { Text("Easypaisa Deposit Instructions") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = bankInstr, onValueChange = { bankInstr = it.take(300) }, label = { Text("Bank Transfer Instructions") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = maintenanceMsg, onValueChange = { maintenanceMsg = it.take(180) }, label = { Text("Maintenance Banner Message (optional)") }, modifier = Modifier.fillMaxWidth())

                    Button(
                        onClick = {
                            adminViewModel.saveSettings(
                                current.copy(
                                    continueCostCoins = (continueCost.toLongOrNull() ?: 50L).coerceAtLeast(1L),
                                    exchangeRatePaisaPerCoin = (exchangePaisa.toLongOrNull() ?: 5L).coerceAtLeast(1L),
                                    minWithdrawalPaisa = ((minWithdrawalRupees.toLongOrNull() ?: 500L).coerceAtLeast(100L)) * 100L,
                                    maxDailyWithdrawals = (maxDailyWd.toLongOrNull() ?: 5L).coerceIn(1L, 50L),
                                    allowEarnedCoinCashout = allowEarnedCashout,
                                    jazzCashEnabled = jazzEnabled,
                                    easypaisaEnabled = easyEnabled,
                                    bankTransferEnabled = bankEnabled,
                                    jazzCashInstructions = jazzInstr.trim(),
                                    easypaisaInstructions = easyInstr.trim(),
                                    bankTransferInstructions = bankInstr.trim(),
                                    paymentSandboxMode = sandboxMode,
                                    rewardedAdConfigured = rewardedAdReady,
                                    gameEnabled = gameEnabled,
                                    maintenanceMessage = maintenanceMsg.trim()
                                )
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_save_settings_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24))
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save Global Settings", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
fun AdminReportsTab(adminViewModel: AdminViewModel) {
    val context = LocalContext.current
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val deposits by adminViewModel.allDeposits.collectAsStateWithLifecycle()
    val withdrawals by adminViewModel.allWithdrawals.collectAsStateWithLifecycle()
    val exchanges by adminViewModel.allExchanges.collectAsStateWithLifecycle()
    val transactions by adminViewModel.allTransactions.collectAsStateWithLifecycle()

    fun shareCsv(title: String, csv: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Prism Block 3D — $title")
            putExtra(Intent.EXTRA_TEXT, csv)
        }
        context.startActivity(Intent.createChooser(intent, "Export $title"))
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ReportExportCard(
                title = "Deposit Report",
                summary = "${deposits.size} total • Approved: ${formatPkr(deposits.filter { it.status == "APPROVED" }.sumOf { it.amountPaisa })}",
                onExport = {
                    val header = "id,userEmail,method,amountPkr,ref,status,sandbox\n"
                    val rows = deposits.joinToString("\n") {
                        "${it.id},${it.userEmail},${it.paymentMethod},${formatPkr(it.amountPaisa)},${it.transactionRef},${it.status},${it.sandboxMode}"
                    }
                    shareCsv("Deposit Report", header + rows)
                }
            )
        }
        item {
            ReportExportCard(
                title = "Withdrawal Report",
                summary = "${withdrawals.size} total • Paid: ${formatPkr(withdrawals.filter { it.status == "PAID" }.sumOf { it.amountPaisa })}",
                onExport = {
                    val header = "id,userEmail,method,amountPkr,account,status,payoutRef\n"
                    val rows = withdrawals.joinToString("\n") {
                        "${it.id},${it.userEmail},${it.paymentMethod},${formatPkr(it.amountPaisa)},${it.accountNumber},${it.status},${it.payoutReference}"
                    }
                    shareCsv("Withdrawal Report", header + rows)
                }
            )
        }
        item {
            ReportExportCard(
                title = "Exchange Report",
                summary = "${exchanges.size} conversions • Total Volume: ${formatPkr(exchanges.sumOf { it.pkrPaisaAmount })}",
                onExport = {
                    val header = "id,userId,direction,coins,pkr,source\n"
                    val rows = exchanges.joinToString("\n") {
                        "${it.id},${it.userId},${it.direction},${it.coinsAmount},${formatPkr(it.pkrPaisaAmount)},${it.coinSource}"
                    }
                    shareCsv("Exchange Report", header + rows)
                }
            )
        }
        item {
            ReportExportCard(
                title = "Coin & Wallet Transactions Report",
                summary = "${transactions.size} immutable ledger rows recorded",
                onExport = {
                    val header = "id,userId,walletType,coinSource,txType,delta,balanceAfter\n"
                    val rows = transactions.joinToString("\n") {
                        "${it.id},${it.userId},${it.walletType},${it.coinSource},${it.txType},${it.amountDelta},${it.balanceAfter}"
                    }
                    shareCsv("Transactions Report", header + rows)
                }
            )
        }
        item {
            ReportExportCard(
                title = "Player Activity Report",
                summary = "${users.size} players • Total Lines Cleared: %,d".format(users.sumOf { it.totalLinesCleared }),
                onExport = {
                    val header = "userId,displayName,email,classicBest,level,lines,mainWalletPkr,purchasedCoins,earnedCoins,blocked\n"
                    val rows = users.joinToString("\n") {
                        "${it.userId},${it.displayName},${it.email},${it.classicHighScore},${it.currentLevel},${it.totalLinesCleared},${formatPkr(it.mainWalletPkrPaisa)},${it.purchasedCoins},${it.earnedCoins},${it.blocked}"
                    }
                    shareCsv("Player Activity Report", header + rows)
                }
            )
        }
    }
}

@Composable
private fun ReportExportCard(title: String, summary: String, onExport: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, SurfaceBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onExport, shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Filled.Share, contentDescription = "Export CSV", tint = PrismCyan, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("CSV", color = PrismCyan)
            }
        }
    }
}

@Composable
fun AdminGovernanceTab(
    adminViewModel: AdminViewModel,
    adminAccess: AdminAccessState,
    busy: Boolean
) {
    val users by adminViewModel.allUsers.collectAsStateWithLifecycle()
    val admins by adminViewModel.allAdmins.collectAsStateWithLifecycle()
    val logs by adminViewModel.auditLogs.collectAsStateWithLifecycle()

    var selectedCandidate by remember(users) {
        mutableStateOf<UserAccount?>(users.firstOrNull { !it.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) })
    }
    var selectedPerms by remember { mutableStateOf(AdminPermissions.ALL.toSet()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.5.dp, PrismGold.copy(alpha = 0.65f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = PrismGold)
                        Spacer(Modifier.width(8.dp))
                        Text("Super Admin Authorization Control", style = MaterialTheme.typography.titleMedium, color = PrismGold)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Primary Super Admin: $SUPER_ADMIN_EMAIL (enforced server-side in Firestore Security Rules). Only the Super Admin can grant or revoke Admin access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (adminAccess.isSuperAdmin) {
                        Spacer(Modifier.height(12.dp))
                        Text("Select Registered User to Authorize:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Spacer(Modifier.height(6.dp))
                        val eligible = users.filter { !it.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) }
                        if (eligible.isEmpty()) {
                            Text("No other registered players yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(eligible, key = { it.id }) { cand ->
                                    FilterChip(
                                        selected = selectedCandidate?.userId == cand.userId,
                                        onClick = { selectedCandidate = cand },
                                        label = { Text(cand.email.ifBlank { cand.displayName }) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text("Granular Permissions:", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            AdminPermissions.ALL.chunked(2).forEach { pair ->
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    pair.forEach { perm ->
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = perm in selectedPerms,
                                                onCheckedChange = { checked ->
                                                    selectedPerms = if (checked) selectedPerms + perm else selectedPerms - perm
                                                }
                                            )
                                            Text(
                                                text = perm.removePrefix("MANAGE_").removePrefix("VIEW_"),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    selectedCandidate?.let { cand ->
                                        adminViewModel.grantAdmin(cand, selectedPerms.toList())
                                    }
                                },
                                enabled = !busy && selectedCandidate != null && selectedPerms.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("super_admin_grant_button"),
                                colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00))
                            ) {
                                Icon(Icons.Filled.AdminPanelSettings, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Authorize Admin", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = RoundedCornerShape(12.dp), color = CardSurface) {
                            Text(
                                text = "Read-Only View: Your account has delegated Admin privileges. Only the Super Admin ($SUPER_ADMIN_EMAIL) may promote or revoke Admins.",
                                style = MaterialTheme.typography.bodySmall,
                                color = PrismCoral,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("AUTHORIZED ADMINS (${admins.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(admins, key = { it.id }) { admin ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${admin.displayName} (${admin.email})", style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Text(
                            text = "${admin.role} • ${admin.permissions.size} permissions • ${if (admin.active) "ACTIVE" else "REVOKED"}",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = PrismCyan
                        )
                    }
                    if (adminAccess.isSuperAdmin && !admin.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true)) {
                        OutlinedButton(
                            onClick = { adminViewModel.revokeAdmin(admin.userId, admin.email) },
                            enabled = !busy,
                            border = BorderStroke(1.dp, PrismCrimson)
                        ) {
                            Text("Revoke", color = PrismCrimson)
                        }
                    }
                }
            }
        }

        item {
            Text("SECURITY AUDIT LOG (${logs.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        items(logs.take(30), key = { it.id }) { log ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(log.action, style = MaterialTheme.typography.labelLarge, color = PrismGold)
                        Text(log.targetType, style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                    }
                    Text(log.details, style = MaterialTheme.typography.bodySmall, color = Color.White)
                    Text("Actor: ${log.adminEmail}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
