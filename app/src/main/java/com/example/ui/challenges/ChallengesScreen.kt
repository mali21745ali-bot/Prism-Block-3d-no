package com.example.ui.challenges

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Leaderboard
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DailyChallenge
import com.example.data.model.TimeChallenge
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AppViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChallengesScreen(
    appViewModel: AppViewModel,
    onPlayDailyChallenge: (DailyChallenge) -> Unit,
    onPlayTimeChallenge: (TimeChallenge) -> Unit
) {
    val dailyChallenge by appViewModel.todayDailyChallenge.collectAsStateWithLifecycle()
    val dailyClaim by appViewModel.dailyChallengeClaim.collectAsStateWithLifecycle()
    val timeChallenges by appViewModel.activeTimeChallenges.collectAsStateWithLifecycle()
    val nowMillis = System.currentTimeMillis()
    val dateFmt = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.US) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Challenges Arena",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
                Text(
                    text = "All Daily & Time Challenges are 100% free to enter. Scores are verified securely in Cloud Firestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // SECTION A: DAILY CHALLENGE CARD
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_challenge_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.5.dp, PrismMint.copy(alpha = 0.6f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrismMint.copy(alpha = 0.14f), Color.Transparent)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(PrismMint.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CalendarToday,
                                        contentDescription = "Daily Challenge",
                                        tint = PrismMint,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = dailyChallenge.title,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Date: ${dailyChallenge.dateKey} • FREE ENTRY",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PrismMint
                                    )
                                }
                            }
                            StatusPill(
                                label = if (dailyClaim != null) "CLAIMED" else "+${dailyChallenge.bonusCoins} COINS",
                                color = if (dailyClaim != null) PrismMint else PrismGold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        Text(
                            text = dailyChallenge.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ChallengeStatChip(
                                label = "GOAL",
                                value = "${dailyChallenge.targetLines} Lines",
                                accent = PrismCyan,
                                modifier = Modifier.weight(1f)
                            )
                            ChallengeStatChip(
                                label = "BONUS",
                                value = "+${dailyChallenge.bonusCoins} Coins",
                                accent = PrismGold,
                                modifier = Modifier.weight(1f)
                            )
                            ChallengeStatChip(
                                label = "ENTRY FEE",
                                value = "FREE",
                                accent = PrismMint,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (dailyClaim != null) {
                            Spacer(Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = PrismMint.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, PrismMint.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = "Daily reward claimed",
                                        tint = PrismMint,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "Daily bonus claimed (+${dailyClaim!!.bonusCoinsClaimed} Coins). Best Score: %,d (%d lines). Duplicate claims are blocked, but you can replay for practice!"
                                            .format(dailyClaim!!.score, dailyClaim!!.linesCleared),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(14.dp))

                        Button(
                            onClick = { onPlayDailyChallenge(dailyChallenge) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("play_daily_challenge_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrismMint,
                                contentColor = Color(0xFF042218)
                            )
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (dailyClaim != null) "Replay Today's Puzzle" else "Play Daily Challenge",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // SECTION B: ADMIN-CONTROLLED TIME CHALLENGES
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TIME CHALLENGES (ADMIN-HOSTED)",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                StatusPill(label = "${timeChallenges.size} Active", color = PrismCoral)
            }
        }

        if (timeChallenges.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.Filled.Timer,
                    title = "No Live Time Challenges Yet",
                    description = "Time Challenges are created and scheduled exclusively through the Admin Panel. Check back soon or create one from the Admin Dashboard."
                )
            }
        } else {
            items(timeChallenges, key = { it.id }) { challenge ->
                TimeChallengeCard(
                    challenge = challenge,
                    nowMillis = nowMillis,
                    dateFmt = dateFmt,
                    appViewModel = appViewModel,
                    onPlay = { onPlayTimeChallenge(challenge) }
                )
            }
        }
    }
}

@Composable
private fun TimeChallengeCard(
    challenge: TimeChallenge,
    nowMillis: Long,
    dateFmt: SimpleDateFormat,
    appViewModel: AppViewModel,
    onPlay: () -> Unit
) {
    var showStandings by remember { mutableStateOf(false) }
    val participants by remember(challenge.id) {
        appViewModel.observeChallengeParticipants(challenge.id)
    }.collectAsStateWithLifecycle(initialValue = emptyList())

    val isLive = challenge.active &&
        (challenge.startTimeMillis <= 0L || nowMillis >= challenge.startTimeMillis) &&
        (challenge.endTimeMillis <= 0L || nowMillis <= challenge.endTimeMillis)
    val isUpcoming = challenge.active && challenge.startTimeMillis > nowMillis
    val statusLabel = when {
        isLive -> "LIVE NOW"
        isUpcoming -> "UPCOMING"
        else -> "ENDED"
    }
    val statusColor = when {
        isLive -> PrismMint
        isUpcoming -> PrismGold
        else -> PrismCoral
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("time_challenge_card_${challenge.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(1.dp, if (isLive) PrismCoral.copy(alpha = 0.65f) else SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = "Time Challenge",
                        tint = PrismCoral,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = challenge.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
                Spacer(Modifier.width(8.dp))
                StatusPill(label = statusLabel, color = statusColor)
            }

            if (challenge.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = challenge.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChallengeStatChip(
                    label = "DURATION",
                    value = "${challenge.durationSeconds}s",
                    accent = PrismCyan,
                    modifier = Modifier.weight(1f)
                )
                ChallengeStatChip(
                    label = if (challenge.rewardType == "TARGET") "TARGET SCORE" else "REWARD MODE",
                    value = if (challenge.rewardType == "TARGET") "%,d pts".format(challenge.targetScore) else "Leaderboard",
                    accent = PrismCoral,
                    modifier = Modifier.weight(1f)
                )
                ChallengeStatChip(
                    label = "PRIZE",
                    value = "+${challenge.rewardCoins} Coins",
                    accent = PrismGold,
                    modifier = Modifier.weight(1f)
                )
            }

            if (challenge.startTimeMillis > 0L && challenge.endTimeMillis > 0L) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Window: ${dateFmt.format(Date(challenge.startTimeMillis))} — ${dateFmt.format(Date(challenge.endTimeMillis))} • FREE ENTRY",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlay,
                    enabled = isLive,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("play_time_challenge_${challenge.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrismCoral,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isLive) "Compete Free" else statusLabel, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { showStandings = !showStandings },
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("toggle_standings_${challenge.id}"),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Icon(
                        imageVector = if (challenge.rewardType == "TARGET") Icons.Filled.Flag else Icons.Filled.Leaderboard,
                        contentDescription = "Challenge standings",
                        tint = PrismCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("${participants.size}", color = PrismCyan)
                    Icon(
                        imageVector = if (showStandings) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = PrismCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showStandings) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = SurfaceBorder)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Challenge Standings (Top ${participants.size})",
                        style = MaterialTheme.typography.labelLarge,
                        color = PrismCyan
                    )
                    Spacer(Modifier.height(6.dp))
                    if (participants.isEmpty()) {
                        Text(
                            text = "No scores submitted yet. Be the first to compete!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        participants.take(10).forEachIndexed { idx, p ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (idx < 3) PrismGold else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.width(34.dp)
                                )
                                Text(
                                    text = p.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    modifier = Modifier.weight(1f)
                                )
                                if (p.targetRewardClaimed) {
                                    Icon(
                                        Icons.Filled.EmojiEvents,
                                        contentDescription = "Target reached",
                                        tint = PrismGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                }
                                Text(
                                    text = "%,d pts".format(p.bestScore),
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = PrismCyan
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeStatChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                color = accent,
                maxLines = 1
            )
        }
    }
}
