package com.example.ui.game

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.integrations.RewardedAdStatus
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.viewmodel.GamePlayUiState

@Composable
internal fun LevelCompleteOverlay(
    visible: Boolean,
    gameState: GamePlayUiState,
    onPlayNextLevel: (Long) -> Unit,
    onExitGame: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD080614)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .widthIn(max = 440.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(2.dp, PrismGold)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = "Level Completed",
                        tint = PrismGold,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "LEVEL ${gameState.activeLevelNumber} COMPLETE!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PrismGold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "1,000 lines conquered! Next level unlocked and saved to Cloud Firestore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HudCounterBox("SCORE", "%,d".format(gameState.score), PrismCyan, Modifier.weight(1f))
                        HudCounterBox(
                            "TOTAL COINS",
                            "+%,d".format(gameState.lastCommitResult?.totalCoinsAwarded ?: gameState.coinsEarned),
                            PrismGold,
                            Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(20.dp))
                    val nextLvl = (gameState.activeLevelNumber + 1L).coerceAtMost(100L)
                    Button(
                        onClick = { onPlayNextLevel(nextLvl) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("next_level_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF221800))
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Play Level $nextLvl", fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = onExitGame,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text("Back to Dashboard")
                    }
                }
            }
        }
    }
}

@Composable
internal fun GameOverOverlay(
    visible: Boolean,
    gameState: GamePlayUiState,
    continueCost: Long,
    playerCoins: Long,
    isContinuingWithCoins: Boolean,
    rewardedAdStatus: RewardedAdStatus,
    showAdConfigDetails: Boolean,
    onContinueWithCoins: () -> Unit,
    onToggleAdDetails: () -> Unit,
    onExitGame: () -> Unit,
    onPlayAgain: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xDD080614)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .widthIn(max = 460.dp),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.5.dp, PrismCoral.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (gameState.isTimeUp) "TIME'S UP!" else "GAME OVER",
                        style = MaterialTheme.typography.displaySmall,
                        color = PrismCoral
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (gameState.isTimeUp) {
                            "Your timed challenge score has been saved securely."
                        } else {
                            "No available block fits on the 10×10 board."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HudCounterBox("SCORE", "%,d".format(gameState.score), PrismCyan, Modifier.weight(1f))
                        HudCounterBox("LINES", "%,d".format(gameState.linesCleared), PrismMint, Modifier.weight(1f))
                        HudCounterBox("COINS", "+%,d".format(gameState.coinsEarned), PrismGold, Modifier.weight(1f))
                    }

                    if (gameState.isCommittingSession) {
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = PrismCyan)
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing score & Coins to Firestore...", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // OPTION 1: CONTINUE WITH COINS
                    Button(
                        onClick = onContinueWithCoins,
                        enabled = !isContinuingWithCoins && playerCoins >= continueCost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("continue_with_coins_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrismGold,
                            contentColor = Color(0xFF241A00)
                        )
                    ) {
                        if (isContinuingWithCoins) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF241A00))
                        } else {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Continue with $continueCost Coins (Bal: $playerCoins)",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // OPTION 2: WATCH REWARDED AD (Never grants fake free rewards!)
                    OutlinedButton(
                        onClick = onToggleAdDetails,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("watch_rewarded_ad_button"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.65f))
                    ) {
                        Icon(Icons.Filled.VideoLibrary, contentDescription = null, tint = PrismCyan)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (rewardedAdStatus is RewardedAdStatus.Ready) {
                                "Watch Rewarded Ad to Continue"
                            } else {
                                "Watch Rewarded Ad (Config Required)"
                            },
                            color = PrismCyan,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }

                    AnimatedVisibility(visible = showAdConfigDetails && rewardedAdStatus is RewardedAdStatus.ConfigurationRequired) {
                        val cfg = rewardedAdStatus as? RewardedAdStatus.ConfigurationRequired
                        if (cfg != null) {
                            Surface(
                                color = CardSurface,
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.Warning, contentDescription = null, tint = PrismGold, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            "Rewarded Ad Credentials Not Configured",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = PrismGold
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = cfg.explanation,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    cfg.requiredCredentials.forEach { cred ->
                                        Text(
                                            text = "• $cred",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PrismCyan
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onExitGame,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("game_over_exit_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Exit")
                        }
                        Button(
                            onClick = onPlayAgain,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("game_over_retry_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24))
                        ) {
                            Text("Play Again", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun HudCounterBox(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = DeepSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = accent,
                maxLines = 1
            )
        }
    }
}
