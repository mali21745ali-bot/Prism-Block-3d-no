package com.example.ui.wallet

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.GlobalSettings
import com.example.data.model.PaymentMethod
import com.example.data.model.UserAccount
import com.example.data.model.formatPkr
import com.example.integrations.PaymentGatewayRegistry
import com.example.ui.components.EmptyStatePanel
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
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Locale

enum class WalletTab(val label: String) {
    OVERVIEW("Overview"),
    EXCHANGE("Exchange"),
    DEPOSIT("Deposit"),
    WITHDRAW("Withdraw"),
    VERIFY("Verify")
}

@Composable
fun WalletHubScreen(
    appViewModel: AppViewModel,
    initialTab: WalletTab = WalletTab.OVERVIEW
) {
    var activeTab by remember(initialTab) { mutableStateOf(initialTab) }
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val settings by appViewModel.globalSettings.collectAsStateWithLifecycle()
    val isBusy by appViewModel.isBusy.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
            Text(
                text = "Dual Wallet Center",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )
            Text(
                text = "Main Wallet (PKR) & Coin Wallet (Purchased + Earned Coins tracked separately)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(WalletTab.entries) { tab ->
                    FilterChip(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        label = { Text(tab.label) },
                        modifier = Modifier.testTag("wallet_tab_${tab.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrismCyan,
                            selectedLabelColor = Color(0xFF041A24)
                        )
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when (activeTab) {
                WalletTab.OVERVIEW -> WalletOverviewTab(
                    user = user,
                    appViewModel = appViewModel,
                    onSelectTab = { activeTab = it }
                )
                WalletTab.EXCHANGE -> WalletExchangeTab(
                    user = user,
                    settings = settings,
                    isBusy = isBusy,
                    appViewModel = appViewModel
                )
                WalletTab.DEPOSIT -> WalletDepositTab(
                    settings = settings,
                    isBusy = isBusy,
                    appViewModel = appViewModel
                )
                WalletTab.WITHDRAW -> WalletWithdrawTab(
                    user = user,
                    settings = settings,
                    isBusy = isBusy,
                    appViewModel = appViewModel
                )
                WalletTab.VERIFY -> WalletVerificationTab(
                    user = user,
                    isBusy = isBusy,
                    appViewModel = appViewModel
                )
            }
        }
    }
}

@Composable
private fun WalletOverviewTab(
    user: UserAccount?,
    appViewModel: AppViewModel,
    onSelectTab: (WalletTab) -> Unit
) {
    val transactions by appViewModel.walletTransactions.collectAsStateWithLifecycle()
    val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Main Wallet (PKR)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.5.dp, PrismMint.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(PrismMint.copy(alpha = 0.14f), Color.Transparent)))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = PrismMint)
                                Spacer(Modifier.width(8.dp))
                                Text("Main Wallet (PKR)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            }
                            StatusPill(
                                label = if (user?.walletVerificationCompleted == true) "FORM SAVED" else "UNFILLED",
                                color = if (user?.walletVerificationCompleted == true) PrismMint else PrismGold
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = formatPkr(user?.mainWalletPkrPaisa ?: 0L),
                            style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = PrismMint
                        )
                        Text(
                            text = "Used for Deposits, Coin Exchanges, and Withdrawals (min PKR 500).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(14.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = { onSelectTab(WalletTab.DEPOSIT) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("overview_deposit_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrismMint, contentColor = Color(0xFF042218))
                            ) {
                                Icon(Icons.Filled.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Deposit", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onSelectTab(WalletTab.WITHDRAW) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                                    .testTag("overview_withdraw_button"),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, PrismMint.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Filled.ArrowUpward, contentDescription = null, tint = PrismMint, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Withdraw", color = PrismMint, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // 2. Coin Wallet (Purchased vs Earned/Promotional breakdown)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.5.dp, PrismGold.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = PrismGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Coin Wallet", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        }
                        TextButton(onClick = { onSelectTab(WalletTab.EXCHANGE) }) {
                            Icon(Icons.Filled.CurrencyExchange, contentDescription = null, tint = PrismCyan, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Exchange", color = PrismCyan)
                        }
                    }
                    Text(
                        text = "%,d Coins".format(user?.totalCoins ?: 0L),
                        style = MaterialTheme.typography.displaySmall.copy(fontFamily = JetBrainsMonoFontFamily),
                        color = PrismGold
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("PURCHASED COINS", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "%,d".format(user?.purchasedCoins ?: 0L),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = PrismCyan
                                )
                                Text("Convertible to PKR", style = MaterialTheme.typography.labelSmall, color = PrismCyan.copy(alpha = 0.8f))
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = CardSurface,
                            border = BorderStroke(1.dp, PrismViolet.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("EARNED / PROMO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = "%,d".format(user?.earnedCoins ?: 0L),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = PrismViolet
                                )
                                Text("Gameplay & Rewards", style = MaterialTheme.typography.labelSmall, color = PrismViolet.copy(alpha = 0.8f))
                            }
                        }
                    }
                }
            }
        }

        // 3. Transaction History Ledger
        item {
            Text(
                text = "WALLET LEDGER (${transactions.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (transactions.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No Wallet Activity Yet",
                    description = "Play blocks, claim daily rewards, exchange Coins, or submit a deposit to see your immutable transaction ledger here."
                )
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                val isPositive = tx.amountDelta >= 0
                val deltaText = if (tx.walletType == "MAIN_PKR") {
                    (if (isPositive) "+" else "") + formatPkr(tx.amountDelta)
                } else {
                    (if (isPositive) "+" else "") + "%,d Coins".format(tx.amountDelta)
                }
                val balText = if (tx.walletType == "MAIN_PKR") formatPkr(tx.balanceAfter) else "%,d Coins".format(tx.balanceAfter)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.description.ifBlank { tx.txType }, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            val ts = tx.createdAt?.toDate()?.let { dateFmt.format(it) } ?: "Syncing..."
                            Text(
                                text = "${tx.walletType} • ${tx.coinSource} • $ts",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = deltaText,
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (isPositive) PrismMint else PrismCoral
                            )
                            Text(
                                text = "Bal: $balText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletExchangeTab(
    user: UserAccount?,
    settings: GlobalSettings,
    isBusy: Boolean,
    appViewModel: AppViewModel
) {
    val exchanges by appViewModel.exchangeHistory.collectAsStateWithLifecycle()
    var pkrToCoin by remember { mutableStateOf(true) }
    var coinsInput by remember { mutableStateOf("200") }
    var showConfirm by remember { mutableStateOf(false) }

    val ratePaisa = settings.exchangeRatePaisaPerCoin.coerceAtLeast(1L)
    val parsedCoins = coinsInput.toLongOrNull()?.coerceAtLeast(0L) ?: 0L
    val exactPaisa = parsedCoins * ratePaisa

    val eligibleCoinPool = if (settings.allowEarnedCoinCashout) {
        (user?.totalCoins ?: 0L)
    } else {
        (user?.purchasedCoins ?: 0L)
    }
    val canSubmit = parsedCoins > 0L && if (pkrToCoin) {
        (user?.mainWalletPkrPaisa ?: 0L) >= exactPaisa
    } else {
        eligibleCoinPool >= parsedCoins
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Coin ↔ PKR Exchange", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        StatusPill("1 Coin = ${formatPkr(ratePaisa)}", PrismGold)
                    }

                    Spacer(Modifier.height(10.dp))

                    // Compliance Notice for Purchased vs Earned Coins
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, PrismViolet.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = PrismViolet, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (settings.allowEarnedCoinCashout) {
                                    "Super Admin enabled promotional/earned Coin conversion. Both Purchased (${user?.purchasedCoins ?: 0L}) and Earned (${user?.earnedCoins ?: 0L}) Coins are eligible."
                                } else {
                                    "Compliance Rule Active: Only Purchased Coins (${user?.purchasedCoins ?: 0L}) can convert to PKR. Earned/Promotional Coins (${user?.earnedCoins ?: 0L}) are for in-game 3D Themes, Shop items & Continues."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = pkrToCoin,
                            onClick = { pkrToCoin = true },
                            label = { Text("PKR → Coins") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exchange_dir_pkr_to_coin")
                        )
                        FilterChip(
                            selected = !pkrToCoin,
                            onClick = { pkrToCoin = false },
                            label = { Text("Coins → PKR") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("exchange_dir_coin_to_pkr")
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = coinsInput,
                        onValueChange = { s -> coinsInput = s.filter { it.isDigit() }.take(9) },
                        label = { Text(if (pkrToCoin) "Coins to Purchase" else "Coins to Convert to PKR") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("exchange_coins_input")
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(100L, 500L, 1000L, 5000L).forEach { preset ->
                            OutlinedButton(
                                onClick = { coinsInput = preset.toString() },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("$preset", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Live Exchange Preview
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text("EXCHANGE PREVIEW", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (pkrToCoin) "You Pay (Main Wallet)" else "You Convert", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (pkrToCoin) formatPkr(exactPaisa) else "%,d Coins".format(parsedCoins),
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = PrismCoral
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(if (pkrToCoin) "You Receive (Purchased Coins)" else "You Receive (Main Wallet)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = if (pkrToCoin) "+%,d Coins".format(parsedCoins) else "+${formatPkr(exactPaisa)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = PrismMint
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = { showConfirm = true },
                        enabled = canSubmit && !isBusy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("exchange_submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24))
                    ) {
                        Icon(Icons.Filled.CurrencyExchange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (canSubmit) "Review & Confirm Exchange" else "Insufficient Eligible Balance",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "EXCHANGE HISTORY (${exchanges.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(exchanges, key = { it.id }) { ex ->
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
                    Column {
                        Text(
                            text = if (ex.direction == "PKR_TO_COIN") "PKR → Purchased Coins" else "Coins (${ex.coinSource}) → PKR",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        Text(
                            text = "Rate: 1 Coin = ${formatPkr(ex.ratePaisaPerCoin)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("%,d Coins".format(ex.coinsAmount), style = MaterialTheme.typography.labelLarge, color = PrismGold)
                        Text(formatPkr(ex.pkrPaisaAmount), style = MaterialTheme.typography.labelSmall, color = PrismMint)
                    }
                }
            }
        }
    }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = DeepSurface,
            title = { Text("Confirm Exchange", color = Color.White) },
            text = {
                Text(
                    text = if (pkrToCoin) {
                        "Spend ${formatPkr(exactPaisa)} from Main Wallet to receive %,d Purchased Coins?".format(parsedCoins)
                    } else {
                        "Convert %,d Coins to receive ${formatPkr(exactPaisa)} in your Main PKR Wallet?".format(parsedCoins)
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pkrToCoin) appViewModel.exchangePkrToCoins(parsedCoins)
                        else appViewModel.exchangeCoinsToPkr(parsedCoins)
                        showConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24))
                ) {
                    Text("Execute Exchange", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun WalletDepositTab(
    settings: GlobalSettings,
    isBusy: Boolean,
    appViewModel: AppViewModel
) {
    val deposits by appViewModel.depositHistory.collectAsStateWithLifecycle()
    val descriptors = remember(settings) { PaymentGatewayRegistry.describeAll(settings) }
    var selectedMethod by remember { mutableStateOf(PaymentMethod.JAZZCASH) }
    var amountRupees by remember { mutableStateOf("500") }
    var senderAccount by remember { mutableStateOf("") }
    var txRef by remember { mutableStateOf("") }

    val selectedDesc = descriptors.first { it.method == selectedMethod }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Explicit Sandbox / Manual Admin Verification Banner
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = PrismGold.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = PrismGold, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (settings.paymentSandboxMode) {
                            "SANDBOX / MANUAL VERIFICATION MODE: Deposits never auto-credit. Submit your JazzCash, Easypaisa, or Bank Transfer reference below — an Admin will verify and approve before your Main PKR Wallet is credited. No password required."
                        } else {
                            "LIVE MANUAL RECONCILIATION: Every deposit requires verified Admin review before crediting your Main PKR Wallet. No password required for deposits."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Submit Deposit Request", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        descriptors.forEach { desc ->
                            FilterChip(
                                selected = selectedMethod == desc.method,
                                enabled = desc.enabled,
                                onClick = { selectedMethod = desc.method },
                                label = { Text(desc.method.displayName, maxLines = 1) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("deposit_method_${desc.method.wireName.lowercase()}")
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${selectedDesc.method.displayName} Instructions", style = MaterialTheme.typography.labelLarge, color = PrismCyan)
                            Spacer(Modifier.height(4.dp))
                            Text(selectedDesc.instructions, style = MaterialTheme.typography.bodySmall, color = Color.White)
                            Spacer(Modifier.height(4.dp))
                            Text(selectedDesc.complianceNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountRupees,
                        onValueChange = { amountRupees = it.filter { ch -> ch.isDigit() }.take(7) },
                        label = { Text("Deposit Amount (PKR)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_amount_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = senderAccount,
                        onValueChange = { senderAccount = it.take(36) },
                        label = { Text("Your Sender Account / Phone Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_sender_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = txRef,
                        onValueChange = { txRef = it.take(40) },
                        label = { Text("Transaction Reference ID (TID / UTR)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("deposit_txref_input")
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            val amt = amountRupees.toLongOrNull() ?: 0L
                            appViewModel.submitDepositRequest(amt, selectedMethod, txRef, senderAccount)
                            txRef = ""
                        },
                        enabled = !isBusy && selectedDesc.enabled && (amountRupees.toLongOrNull() ?: 0L) >= 50L && txRef.trim().length >= 4 && senderAccount.trim().length >= 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("deposit_submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismMint, contentColor = Color(0xFF042218))
                    ) {
                        Icon(Icons.Filled.AccountBalance, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Submit for Admin Verification", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "MY DEPOSIT REQUESTS (${deposits.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(deposits, key = { it.id }) { dep ->
            val color = statusBadgeColor(dep.status)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatPkr(dep.amountPaisa), style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = Color.White)
                        StatusPill(dep.status, color)
                    }
                    Text(
                        text = "${dep.paymentMethod} • Ref: ${dep.transactionRef} • From: ${dep.senderAccount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (dep.adminNote.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Admin Note: ${dep.adminNote}", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletWithdrawTab(
    user: UserAccount?,
    settings: GlobalSettings,
    isBusy: Boolean,
    appViewModel: AppViewModel
) {
    val withdrawals by appViewModel.withdrawalHistory.collectAsStateWithLifecycle()
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    var selectedMethod by remember { mutableStateOf(PaymentMethod.JAZZCASH) }
    var amountRupees by remember { mutableStateOf((settings.minWithdrawalPaisa / 100L).toString()) }
    var accountTitle by remember(user?.walletVerificationName) { mutableStateOf(user?.walletVerificationName ?: "") }
    var accountNumber by remember(user?.walletVerificationAccount) { mutableStateOf(user?.walletVerificationAccount ?: "") }
    var withdrawalPassword by remember { mutableStateOf("") }

    val minRupees = settings.minWithdrawalPaisa / 100L
    val enteredRupees = amountRupees.toLongOrNull() ?: 0L

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Security Password Setup Card (Required before first withdrawal)
        if (user != null && !user.hasWithdrawalPin) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepSurface),
                    border = BorderStroke(1.5.dp, PrismGold)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = PrismGold)
                            Spacer(Modifier.width(8.dp))
                            Text("Set Withdrawal Security Password", style = MaterialTheme.typography.titleMedium, color = PrismGold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Every withdrawal request requires your personal Withdrawal Security Password (stored as a salted SHA-256 hash). Create yours now.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { newPin = it.take(32) },
                            label = { Text("New Withdrawal Password (min 6 chars)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("setup_withdrawal_pin_input")
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { confirmPin = it.take(32) },
                            label = { Text("Confirm Withdrawal Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("confirm_withdrawal_pin_input")
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (newPin == confirmPin) {
                                    appViewModel.setWithdrawalSecurityPassword(newPin)
                                    newPin = ""
                                    confirmPin = ""
                                } else {
                                    appViewModel.showError("Withdrawal passwords do not match.")
                                }
                            },
                            enabled = !isBusy && newPin.length >= 6 && newPin == confirmPin,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_withdrawal_pin_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00))
                        ) {
                            Text("Save Withdrawal Security Password", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Withdrawal Form
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Request Withdrawal", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        StatusPill("Min ${formatPkr(settings.minWithdrawalPaisa)}", PrismCyan)
                    }
                    Text(
                        text = "Available Main Wallet: ${formatPkr(user?.mainWalletPkrPaisa ?: 0L)} • Up to ${settings.maxDailyWithdrawals} requests/day • Admin manually verifies & pays.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        PaymentMethod.entries.forEach { method ->
                            FilterChip(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method },
                                label = { Text(method.displayName, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountRupees,
                        onValueChange = { amountRupees = it.filter { ch -> ch.isDigit() }.take(7) },
                        label = { Text("Withdrawal Amount (PKR, min $minRupees)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_amount_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountTitle,
                        onValueChange = { accountTitle = it.take(60) },
                        label = { Text("Recipient Account Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_name_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.take(36) },
                        label = { Text("JazzCash / Easypaisa / Bank IBAN Number") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_account_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = withdrawalPassword,
                        onValueChange = { withdrawalPassword = it.take(32) },
                        label = { Text("Withdrawal Security Password (Required)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("withdraw_password_input")
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            appViewModel.submitWithdrawalRequest(
                                amountPkrRupees = enteredRupees,
                                paymentMethod = selectedMethod,
                                accountTitle = accountTitle,
                                accountNumber = accountNumber,
                                enteredPassword = withdrawalPassword
                            )
                            withdrawalPassword = ""
                        },
                        enabled = !isBusy &&
                            (user?.hasWithdrawalPin == true) &&
                            enteredRupees >= minRupees &&
                            (user?.mainWalletPkrPaisa ?: 0L) >= enteredRupees * 100L &&
                            accountTitle.trim().length >= 3 &&
                            accountNumber.trim().length >= 8 &&
                            withdrawalPassword.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("withdraw_submit_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCoral, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Confirm & Submit Withdrawal", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text(
                text = "MY WITHDRAWAL REQUESTS (${withdrawals.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(withdrawals, key = { it.id }) { wd ->
            val color = statusBadgeColor(wd.status)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatPkr(wd.amountPaisa), style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = Color.White)
                        StatusPill(wd.status, color)
                    }
                    Text(
                        text = "${wd.paymentMethod} • ${wd.accountTitle} (${wd.accountNumber})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (wd.payoutReference.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text("Payout Ref: ${wd.payoutReference}", style = MaterialTheme.typography.labelSmall, color = PrismMint)
                    }
                    if (wd.adminNote.isNotBlank()) {
                        Text("Admin Note: ${wd.adminNote}", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun WalletVerificationTab(
    user: UserAccount?,
    isBusy: Boolean,
    appViewModel: AppViewModel
) {
    var fullName by remember(user?.walletVerificationName) { mutableStateOf(user?.walletVerificationName ?: "") }
    var provider by remember(user?.walletVerificationProvider) {
        mutableStateOf(user?.walletVerificationProvider?.ifBlank { "JAZZCASH" } ?: "JAZZCASH")
    }
    var accountNumber by remember(user?.walletVerificationAccount) { mutableStateOf(user?.walletVerificationAccount ?: "") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(
                    1.5.dp,
                    if (user?.walletVerificationCompleted == true) PrismMint else PrismCyan.copy(alpha = 0.55f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.VerifiedUser, contentDescription = null, tint = PrismMint)
                            Spacer(Modifier.width(8.dp))
                            Text("Wallet Verification Form", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        }
                        if (user?.walletVerificationCompleted == true) {
                            StatusPill("COMPLETE", PrismMint)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardSurface,
                        border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Honest Verification Disclosure",
                                style = MaterialTheme.typography.labelLarge,
                                color = PrismGold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Submitting this form saves your payout Full Name and JazzCash/Easypaisa Account Number to your account and marks the verification form complete automatically. Independent real-world account ownership verification requires live JazzCash/Easypaisa OTP or KYC API integration.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(
                            selected = provider == "JAZZCASH",
                            onClick = { provider = "JAZZCASH" },
                            label = { Text("JazzCash") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = provider == "EASYPAISA",
                            onClick = { provider = "EASYPAISA" },
                            label = { Text("Easypaisa") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it.take(60) },
                        label = { Text("Account Holder Full Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_fullname_input")
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = accountNumber,
                        onValueChange = { accountNumber = it.take(32) },
                        label = { Text("JazzCash / Easypaisa Account Number") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("verify_account_input")
                    )

                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            appViewModel.saveWalletVerificationForm(fullName, provider, accountNumber)
                        },
                        enabled = !isBusy && fullName.trim().length >= 3 && accountNumber.trim().length >= 8,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("verify_save_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismMint, contentColor = Color(0xFF042218))
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save & Mark Verification Form Complete", fontWeight = FontWeight.Bold)
                    }

                    if (user?.walletVerificationCompleted == true) {
                        Spacer(Modifier.height(12.dp))
                        HorizontalDivider(color = SurfaceBorder)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Saved Details: ${user.walletVerificationName} • ${user.walletVerificationProvider} (${user.walletVerificationAccount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrismMint
                        )
                    }
                }
            }
        }
    }
}

private fun statusBadgeColor(status: String): Color = when (status) {
    "APPROVED", "PAID" -> PrismMint
    "PROCESSING" -> PrismCyan
    "REJECTED" -> PrismCrimson
    else -> PrismGold
}
