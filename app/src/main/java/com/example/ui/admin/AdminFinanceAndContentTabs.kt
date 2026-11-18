package com.example.ui.admin

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DepositRequest
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.data.model.TimeChallenge
import com.example.data.model.WithdrawalRequest
import com.example.data.model.formatPkr
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCrimson
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AdminViewModel

@Composable
fun AdminDepositsTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val deposits by adminViewModel.allDeposits.collectAsStateWithLifecycle()
    var filterStatus by remember { mutableStateOf("ALL") }

    val filtered = remember(deposits, filterStatus) {
        if (filterStatus == "ALL") deposits else deposits.filter { it.status == filterStatus }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("ALL", "PENDING", "PROCESSING", "APPROVED", "REJECTED").forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = st },
                        label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No Deposit Requests",
                    description = "Player JazzCash, Easypaisa, and Bank Transfer deposits will appear here for manual verification."
                )
            }
        } else {
            items(filtered, key = { it.id }) { dep ->
                AdminDepositCard(dep, busy) { newStatus, ref, note ->
                    adminViewModel.reviewDeposit(dep, newStatus, ref, note)
                }
            }
        }
    }
}

@Composable
private fun AdminDepositCard(
    dep: DepositRequest,
    busy: Boolean,
    onReview: (String, String, String) -> Unit
) {
    var adminRef by remember(dep.id) { mutableStateOf(dep.adminPaymentRef) }
    var note by remember(dep.id) { mutableStateOf(dep.adminNote) }
    val isTerminal = dep.status == "APPROVED" || dep.status == "REJECTED"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, if (dep.status == "PENDING") PrismGold else SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(formatPkr(dep.amountPaisa), style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismMint)
                    Text("${dep.userName} (${dep.userEmail})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                StatusPill(dep.status, when (dep.status) {
                    "APPROVED" -> PrismMint
                    "REJECTED" -> PrismCrimson
                    "PROCESSING" -> PrismCyan
                    else -> PrismGold
                })
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "${dep.paymentMethod} • Ref: ${dep.transactionRef} • Sender: ${dep.senderAccount} • ${if (dep.sandboxMode) "SANDBOX" else "LIVE"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (!isTerminal) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = adminRef,
                    onValueChange = { adminRef = it.take(40) },
                    label = { Text("Admin Verification Ref") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("Admin Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onReview("PROCESSING", adminRef, note) },
                        enabled = !busy && dep.status == "PENDING",
                        modifier = Modifier.weight(1f)
                    ) { Text("Process", style = MaterialTheme.typography.labelSmall) }
                    Button(
                        onClick = { onReview("APPROVED", adminRef, note) },
                        enabled = !busy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_deposit_${dep.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismMint, contentColor = Color(0xFF042218))
                    ) { Text("Approve", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { onReview("REJECTED", adminRef, note) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCrimson, contentColor = Color.White)
                    ) { Text("Reject", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AdminWithdrawalsTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val withdrawals by adminViewModel.allWithdrawals.collectAsStateWithLifecycle()
    var filterStatus by remember { mutableStateOf("ALL") }

    val filtered = remember(withdrawals, filterStatus) {
        if (filterStatus == "ALL") withdrawals else withdrawals.filter { it.status == filterStatus }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("ALL", "PENDING", "PROCESSING", "APPROVED", "PAID", "REJECTED").forEach { st ->
                    FilterChip(
                        selected = filterStatus == st,
                        onClick = { filterStatus = st },
                        label = { Text(st, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Filled.ReceiptLong,
                    title = "No Withdrawal Requests",
                    description = "Password-verified player withdrawal requests (min PKR 500) will appear here for manual payout processing."
                )
            }
        } else {
            items(filtered, key = { it.id }) { wd ->
                AdminWithdrawalCard(wd, busy) { newStatus, payoutRef, note ->
                    adminViewModel.reviewWithdrawal(wd, newStatus, payoutRef, note)
                }
            }
        }
    }
}

@Composable
private fun AdminWithdrawalCard(
    wd: WithdrawalRequest,
    busy: Boolean,
    onReview: (String, String, String) -> Unit
) {
    var payoutRef by remember(wd.id) { mutableStateOf(wd.payoutReference) }
    var note by remember(wd.id) { mutableStateOf(wd.adminNote) }
    val isTerminal = wd.status == "PAID" || wd.status == "REJECTED"

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, if (wd.status == "PENDING") PrismCoral else SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(formatPkr(wd.amountPaisa), style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismCoral)
                    Text("${wd.userName} (${wd.userEmail})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                }
                StatusPill(wd.status, when (wd.status) {
                    "PAID" -> PrismMint
                    "APPROVED" -> PrismCyan
                    "REJECTED" -> PrismCrimson
                    else -> PrismGold
                })
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Pay via ${wd.paymentMethod} → ${wd.accountTitle} (${wd.accountNumber})",
                style = MaterialTheme.typography.labelSmall,
                color = PrismCyan
            )

            if (!isTerminal) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = payoutRef,
                    onValueChange = { payoutRef = it.take(40) },
                    label = { Text("Manual Payout Reference / Receipt ID") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(120) },
                    label = { Text("Admin Note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { onReview("PROCESSING", payoutRef, note) },
                        enabled = !busy && wd.status == "PENDING",
                        modifier = Modifier.weight(1f)
                    ) { Text("Process", style = MaterialTheme.typography.labelSmall) }
                    Button(
                        onClick = { onReview("PAID", payoutRef, note) },
                        enabled = !busy,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("mark_paid_withdrawal_${wd.id}"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismMint, contentColor = Color(0xFF042218))
                    ) { Text("Mark Paid", fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = { onReview("REJECTED", payoutRef, note) },
                        enabled = !busy,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCrimson, contentColor = Color.White)
                    ) { Text("Reject", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Composable
fun AdminLevelsTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val levels by adminViewModel.allLevels.collectAsStateWithLifecycle()
    val levelMap = remember(levels) { levels.associateBy { it.levelNumber } }
    var selectedLevel by remember { mutableStateOf(1L) }
    val curConfig = levelMap[selectedLevel]
    var rewardCoins by remember(selectedLevel, curConfig) {
        mutableStateOf((curConfig?.rewardCoins ?: (100L + selectedLevel * 10L)).toString())
    }
    var requiredLines by remember(selectedLevel, curConfig) {
        mutableStateOf((curConfig?.requiredLines ?: DEFAULT_LINES_PER_LEVEL).toString())
    }
    var jumpInput by remember { mutableStateOf("1") }

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
                    Text("Edit Level $selectedLevel of $MAX_LEVEL_NUMBER", style = MaterialTheme.typography.titleMedium, color = PrismGold)
                    Text(
                        text = "Completions recorded: ${curConfig?.completionsCount ?: 0L}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = jumpInput,
                            onValueChange = { s ->
                                jumpInput = s.filter { it.isDigit() }.take(3)
                                val parsed = jumpInput.toLongOrNull()
                                if (parsed != null && parsed in 1L..100L) selectedLevel = parsed
                            },
                            label = { Text("Jump Level (1–100)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(onClick = {
                            selectedLevel = (selectedLevel - 1L).coerceAtLeast(1L)
                            jumpInput = selectedLevel.toString()
                        }) { Text("-1") }
                        OutlinedButton(onClick = {
                            selectedLevel = (selectedLevel + 1L).coerceAtMost(100L)
                            jumpInput = selectedLevel.toString()
                        }) { Text("+1") }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = requiredLines,
                        onValueChange = { requiredLines = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("Required Lines (Default: 1000)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rewardCoins,
                        onValueChange = { rewardCoins = it.filter { c -> c.isDigit() }.take(6) },
                        label = { Text("Completion Bonus Coins") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = {
                            adminViewModel.updateLevel(
                                levelNumber = selectedLevel,
                                rewardCoins = rewardCoins.toLongOrNull() ?: 100L,
                                requiredLines = requiredLines.toLongOrNull() ?: DEFAULT_LINES_PER_LEVEL
                            )
                        },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_save_level_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00))
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Save Level $selectedLevel Config", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        items((1..20).toList(), key = { it }) { lvl ->
            val cfg = levelMap[lvl.toLong()]
            Surface(
                onClick = {
                    selectedLevel = lvl.toLong()
                    jumpInput = lvl.toString()
                },
                shape = RoundedCornerShape(14.dp),
                color = DeepSurface,
                border = BorderStroke(1.dp, if (selectedLevel == lvl.toLong()) PrismGold else SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Level $lvl", style = MaterialTheme.typography.titleSmall, color = Color.White)
                    Text(
                        "${cfg?.requiredLines ?: DEFAULT_LINES_PER_LEVEL} lines • +${cfg?.rewardCoins ?: (100L + lvl * 10L)} Coins • ${cfg?.completionsCount ?: 0} wins",
                        style = MaterialTheme.typography.labelSmall,
                        color = PrismCyan
                    )
                }
            }
        }
    }
}

@Composable
fun AdminTimeChallengesTab(adminViewModel: AdminViewModel, busy: Boolean) {
    val challenges by adminViewModel.allTimeChallenges.collectAsStateWithLifecycle()

    var editingId by remember { mutableStateOf<String?>(null) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationSeconds by remember { mutableStateOf("180") }
    var windowHours by remember { mutableStateOf("24") }
    var rewardType by remember { mutableStateOf("LEADERBOARD") }
    var targetScore by remember { mutableStateOf("250") }
    var rewardCoins by remember { mutableStateOf("150") }
    var active by remember { mutableStateOf(true) }

    fun resetForm() {
        editingId = null
        title = ""
        description = ""
        durationSeconds = "180"
        windowHours = "24"
        rewardType = "LEADERBOARD"
        targetScore = "250"
        rewardCoins = "150"
        active = true
    }

    fun loadForEdit(tc: TimeChallenge) {
        editingId = tc.id
        title = tc.title
        description = tc.description
        durationSeconds = tc.durationSeconds.toString()
        val hrs = ((tc.endTimeMillis - tc.startTimeMillis) / 3_600_000L).coerceAtLeast(1L)
        windowHours = hrs.toString()
        rewardType = tc.rewardType
        targetScore = tc.targetScore.toString()
        rewardCoins = tc.rewardCoins.toString()
        active = tc.active
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismCoral.copy(alpha = 0.6f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (editingId == null) "Create Free Time Challenge" else "Edit Time Challenge",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        if (editingId != null) {
                            OutlinedButton(onClick = { resetForm() }) { Text("New") }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it.take(60) },
                        label = { Text("Challenge Title") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_tc_title_input")
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it.take(180) },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = durationSeconds,
                            onValueChange = { durationSeconds = it.filter { c -> c.isDigit() }.take(5) },
                            label = { Text("Play Secs") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = windowHours,
                            onValueChange = { windowHours = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Open Hours") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Reward Method", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = rewardType == "LEADERBOARD",
                            onClick = { rewardType = "LEADERBOARD" },
                            label = { Text("Leaderboard Top Players") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = rewardType == "TARGET",
                            onClick = { rewardType = "TARGET" },
                            label = { Text("Target Score") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (rewardType == "TARGET") {
                            OutlinedTextField(
                                value = targetScore,
                                onValueChange = { targetScore = it.filter { c -> c.isDigit() }.take(7) },
                                label = { Text("Target Score") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = rewardCoins,
                            onValueChange = { rewardCoins = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("Reward Coins") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Active & Visible to Players (Free Entry)", style = MaterialTheme.typography.bodySmall, color = Color.White)
                        Switch(checked = active, onCheckedChange = { active = it })
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            val now = System.currentTimeMillis()
                            val hrs = (windowHours.toLongOrNull() ?: 24L).coerceAtLeast(1L)
                            adminViewModel.saveTimeChallenge(
                                existingId = editingId,
                                title = title,
                                description = description,
                                durationSeconds = (durationSeconds.toLongOrNull() ?: 180L).coerceAtLeast(30L),
                                startTimeMillis = now,
                                endTimeMillis = now + hrs * 3_600_000L,
                                rewardType = rewardType,
                                targetScore = (targetScore.toLongOrNull() ?: 200L).coerceAtLeast(10L),
                                rewardCoins = (rewardCoins.toLongOrNull() ?: 100L).coerceAtLeast(1L),
                                active = active
                            )
                            resetForm()
                        },
                        enabled = !busy && title.trim().length >= 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_save_tc_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismCoral, contentColor = Color.White)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text(if (editingId == null) "Publish Time Challenge" else "Update Time Challenge", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        item {
            Text("ALL TIME CHALLENGES (${challenges.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (challenges.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Filled.Timer,
                    title = "No Time Challenges Created",
                    description = "Publish unlimited free Time Challenges above with Leaderboard or Target Score rewards."
                )
            }
        } else {
            items(challenges, key = { it.id }) { tc ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, if (tc.active) PrismCoral.copy(alpha = 0.55f) else SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(tc.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                StatusPill(if (tc.active) "ACTIVE" else "PAUSED", if (tc.active) PrismMint else PrismGold)
                            }
                            Text(
                                "${tc.durationSeconds}s • ${tc.rewardType} • +${tc.rewardCoins} Coins" +
                                    if (tc.rewardType == "TARGET") " (Goal: ${tc.targetScore})" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { loadForEdit(tc) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit challenge", tint = PrismCyan)
                        }
                        IconButton(onClick = { adminViewModel.deleteTimeChallenge(tc.id) }, enabled = !busy) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete challenge", tint = PrismCrimson)
                        }
                    }
                }
            }
        }
    }
}
