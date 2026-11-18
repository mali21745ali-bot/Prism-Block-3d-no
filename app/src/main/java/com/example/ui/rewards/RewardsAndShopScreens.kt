package com.example.ui.rewards

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.game.BlockThemes
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AppViewModel

@Composable
fun DailyRewardsScreen(
    appViewModel: AppViewModel,
    onOpenCoinShop: () -> Unit
) {
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val rewards by appViewModel.dailyRewards.collectAsStateWithLifecycle()
    val isBusy by appViewModel.isBusy.collectAsStateWithLifecycle()

    val todayKey = appViewModel.todayDateKey()
    val alreadyClaimedToday = user?.dailyRewardLastClaimDate == todayKey
    val nextDay = (user?.dailyRewardNextDay ?: 1L).coerceIn(1L, 7L)
    val sortedRewards = remember(rewards) {
        (if (rewards.size >= 7) rewards else BlockThemes.defaultDailyRewards())
            .sortedBy { it.dayNumber }
    }
    val nextRewardConfig = sortedRewards.firstOrNull { it.dayNumber == nextDay } ?: sortedRewards.first()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Daily Login Rewards",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Text(
                        text = "7-Day Progressive Cycle • Loops cleanly back to Day 1 after Day 7.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = onOpenCoinShop,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, PrismViolet.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("rewards_open_shop_button")
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = null, tint = PrismViolet, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Coin Shop", color = PrismViolet)
                }
            }
        }

        // Non-Reset Progress Guarantee Callout
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = DeepSurface,
                border = BorderStroke(1.5.dp, PrismGold.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(PrismGold.copy(alpha = 0.16f), Color.Transparent)
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Verified, contentDescription = "No reset protection", tint = PrismGold)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Missed Days Never Reset Your Progress",
                                style = MaterialTheme.typography.titleSmall,
                                color = PrismGold
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = if (alreadyClaimedToday) {
                                "Today's gift is claimed! Next login continues smoothly at Day $nextDay."
                            } else {
                                "Ready to claim Day $nextDay (+${nextRewardConfig.rewardCoins} Coins). Missing a day will never push you back to Day 1."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { appViewModel.claimDailyLoginReward() },
                            enabled = !alreadyClaimedToday && !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("claim_daily_reward_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrismGold,
                                contentColor = Color(0xFF241A00)
                            )
                        ) {
                            Icon(
                                imageVector = if (alreadyClaimedToday) Icons.Filled.CheckCircle else Icons.Filled.Redeem,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (alreadyClaimedToday) {
                                    "Claimed Today — Come Back Tomorrow for Day $nextDay"
                                } else {
                                    "Claim Day $nextDay Reward (+${nextRewardConfig.rewardCoins} Coins)"
                                },
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 7-Day Reward Schedule
        items(sortedRewards, key = { it.dayNumber }) { cfg ->
            val isCompletedInCycle = cfg.dayNumber < nextDay
            val isNextUp = cfg.dayNumber == nextDay
            val borderColor = when {
                isNextUp && !alreadyClaimedToday -> PrismGold
                isCompletedInCycle -> PrismMint.copy(alpha = 0.55f)
                else -> SurfaceBorder
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("daily_reward_day_${cfg.dayNumber}"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isNextUp) CardSurface else DeepSurface
                ),
                border = BorderStroke(if (isNextUp) 1.5.dp else 1.dp, borderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                when {
                                    isCompletedInCycle -> PrismMint.copy(alpha = 0.2f)
                                    isNextUp -> PrismGold.copy(alpha = 0.22f)
                                    else -> SurfaceBorder.copy(alpha = 0.45f)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompletedInCycle) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = "Claimed", tint = PrismMint)
                        } else {
                            Text(
                                text = "D${cfg.dayNumber}",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (isNextUp) PrismGold else Color.White
                            )
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(cfg.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                            if (cfg.bonusItemName.isNotBlank()) {
                                StatusPill(label = cfg.bonusItemName, color = PrismViolet)
                            }
                        }
                        Text(
                            text = cfg.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "+${cfg.rewardCoins}",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = PrismGold
                        )
                        Text(
                            text = when {
                                isCompletedInCycle -> "Done"
                                isNextUp && !alreadyClaimedToday -> "Ready"
                                isNextUp -> "Tomorrow"
                                else -> "Coins"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCompletedInCycle) PrismMint else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
