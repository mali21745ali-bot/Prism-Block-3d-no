package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

const val SUPER_ADMIN_EMAIL = "mali21745ali@gmail.com"
const val DEFAULT_THEME_ID = "simple_classic"
const val DEFAULT_LINES_PER_LEVEL = 1000L
const val MAX_LEVEL_NUMBER = 100
const val DEFAULT_EXCHANGE_RATE_PAISA = 5L // 1 Coin = 0.05 PKR = 5 paisa
const val MIN_WITHDRAWAL_PAISA = 50_000L // PKR 500.00 = 50,000 paisa

@Keep
@IgnoreExtraProperties
data class UserAccount(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val email: String = "",
    val photoUrl: String = "",
    val blocked: Boolean = false,
    val selectedThemeId: String = DEFAULT_THEME_ID,
    val unlockedThemeIds: List<String> = listOf(DEFAULT_THEME_ID),
    val classicHighScore: Long = 0L,
    val totalLinesCleared: Long = 0L,
    val totalGamesPlayed: Long = 0L,
    val currentLevel: Long = 1L,
    val currentLevelLines: Long = 0L,
    val completedLevelsCount: Long = 0L,
    val dailyRewardNextDay: Long = 1L,
    val dailyRewardLastClaimDate: String = "",
    val dailyChallengeLastDate: String = "",
    val dailyChallengeBestScore: Long = 0L,
    val mainWalletPkrPaisa: Long = 0L,
    val purchasedCoins: Long = 0L,
    val earnedCoins: Long = 0L,
    val walletVerificationName: String = "",
    val walletVerificationProvider: String = "",
    val walletVerificationAccount: String = "",
    val walletVerificationCompleted: Boolean = false,
    val withdrawalPinHash: String = "",
    val withdrawalPinSalt: String = "",
    val musicEnabled: Boolean = true,
    val sfxEnabled: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val totalCoins: Long get() = purchasedCoins + earnedCoins
    val hasWithdrawalPin: Boolean get() = withdrawalPinHash.isNotBlank() && withdrawalPinSalt.isNotBlank()
}

@Keep
@IgnoreExtraProperties
data class AdminRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val role: String = "ADMIN",
    val active: Boolean = true,
    val permissions: List<String> = emptyList(),
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GameSession(
    @DocumentId val id: String = "",
    val userId: String = "",
    val mode: String = "CLASSIC",
    val score: Long = 0L,
    val linesCleared: Long = 0L,
    val coinsEarned: Long = 0L,
    val levelNumber: Long = 0L,
    val challengeId: String = "",
    val continuesUsed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LeaderboardEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val photoUrl: String = "",
    val boardType: String = "ALL_TIME",
    val dateKey: String = "ALL_TIME",
    val bestScore: Long = 0L,
    val totalLines: Long = 0L,
    val highestLevel: Long = 1L,
    val gamesPlayed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyChallenge(
    @DocumentId val id: String = "",
    val dateKey: String = "",
    val title: String = "Daily Prism Sprint",
    val description: String = "Clear target lines in today's curated puzzle board to earn bonus Coins.",
    val seed: Long = 20261117L,
    val targetLines: Long = 15L,
    val bonusCoins: Long = 40L,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyChallengeClaim(
    @DocumentId val id: String = "",
    val userId: String = "",
    val dateKey: String = "",
    val score: Long = 0L,
    val linesCleared: Long = 0L,
    val bonusCoinsClaimed: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class TimeChallenge(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val durationSeconds: Long = 180L,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val rewardType: String = "LEADERBOARD", // LEADERBOARD or TARGET
    val targetScore: Long = 200L,
    val rewardCoins: Long = 100L,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ChallengeParticipant(
    @DocumentId val id: String = "",
    val challengeId: String = "",
    val userId: String = "",
    val displayName: String = "Player",
    val bestScore: Long = 0L,
    val linesCleared: Long = 0L,
    val targetRewardClaimed: Boolean = false,
    val rewardCoinsAwarded: Long = 0L,
    val publicEntry: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val walletType: String = "COIN", // MAIN_PKR or COIN
    val coinSource: String = "EARNED", // PURCHASED, EARNED, NONE
    val txType: String = "GAME_REWARD",
    val amountDelta: Long = 0L,
    val balanceAfter: Long = 0L,
    val description: String = "",
    val referenceId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DepositRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val amountPaisa: Long = 0L,
    val paymentMethod: String = "JAZZCASH",
    val transactionRef: String = "",
    val senderAccount: String = "",
    val status: String = "PENDING", // PENDING, PROCESSING, APPROVED, REJECTED
    val adminNote: String = "",
    val adminPaymentRef: String = "",
    val reviewedBy: String = "",
    val idempotencyKey: String = "",
    val sandboxMode: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class WithdrawalRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userName: String = "",
    val amountPaisa: Long = 0L,
    val paymentMethod: String = "JAZZCASH",
    val accountTitle: String = "",
    val accountNumber: String = "",
    val status: String = "PENDING", // PENDING, PROCESSING, APPROVED, REJECTED, PAID
    val adminNote: String = "",
    val payoutReference: String = "",
    val reviewedBy: String = "",
    val idempotencyKey: String = "",
    val sandboxMode: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ExchangeRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val direction: String = "PKR_TO_COIN", // COIN_TO_PKR or PKR_TO_COIN
    val coinsAmount: Long = 0L,
    val pkrPaisaAmount: Long = 0L,
    val ratePaisaPerCoin: Long = DEFAULT_EXCHANGE_RATE_PAISA,
    val coinSource: String = "PURCHASED", // PURCHASED or EARNED
    val idempotencyKey: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShopItem(
    @DocumentId val id: String = "",
    val name: String = "",
    val description: String = "",
    val category: String = "THEME", // THEME, BLOCK_SKIN, BOARD_BACKGROUND, SPECIAL_EFFECT, GAME_BOOSTER, AVATAR, PROFILE_FRAME, BADGE, EXCLUSIVE, ITEM
    val subType: String = "", // e.g., PASTEL, DARK, NEON, SPECIAL, HAMMER, UNDO, SHUFFLE, ROW_BLASTER, SEASONAL
    val themeKey: String = "",
    val assetKey: String = "",
    val imageUrl: String = "",
    val priceCoins: Long = 0L,
    val promoActive: Boolean = false,
    val promoPriceCoins: Long = 0L,
    val promoStartMillis: Long = 0L,
    val promoEndMillis: Long = 0L,
    val promoLabel: String = "",
    val premiumPastel: Boolean = false,
    val featured: Boolean = false,
    val newArrival: Boolean = false,
    val limitedEdition: Boolean = false,
    val available: Boolean = true,
    val consumable: Boolean = false,
    val boosterPackQty: Long = 1L,
    val boosterMaxPerGame: Long = 0L,
    val boosterPowerRadius: Long = 0L,
    val sortOrder: Long = 0L,
    val accentHex: String = "#38E8FF",
    val totalSalesCount: Long = 0L,
    val uniqueOwnersCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedAssetKey: String
        get() = assetKey.ifBlank { themeKey.ifBlank { id } }
}

@Keep
@IgnoreExtraProperties
data class ShopCategory(
    @DocumentId val id: String = "",
    val categoryKey: String = "THEME",
    val displayName: String = "Themes",
    val description: String = "",
    val iconKey: String = "palette",
    val accentHex: String = "#38E8FF",
    val enabled: Boolean = true,
    val sortOrder: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PromotionalOffer(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val itemId: String = "",
    val categoryKey: String = "",
    val discountPercent: Long = 0L,
    val promoPriceCoins: Long = 0L,
    val startTimeMillis: Long = 0L,
    val endTimeMillis: Long = 0L,
    val active: Boolean = true,
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayerInventory(
    @DocumentId val id: String = "",
    val userId: String = "",
    val ownedItemIds: List<String> = listOf("theme_simple_classic"),
    val equippedThemeId: String = DEFAULT_THEME_ID,
    val equippedBlockSkinId: String = "",
    val equippedBackgroundId: String = "",
    val equippedEffectId: String = "",
    val equippedAvatarId: String = "",
    val equippedFrameId: String = "",
    val equippedBadgeId: String = "",
    val equippedExclusiveId: String = "",
    val hammerCount: Long = 0L,
    val undoCount: Long = 0L,
    val shuffleCount: Long = 0L,
    val rowBlasterCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun boosterCount(type: BoosterType): Long = when (type) {
        BoosterType.HAMMER -> hammerCount
        BoosterType.UNDO -> undoCount
        BoosterType.SHUFFLE -> shuffleCount
        BoosterType.ROW_BLASTER -> rowBlasterCount
    }
}

@Keep
@IgnoreExtraProperties
data class PurchasedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val itemId: String = "",
    val itemName: String = "",
    val category: String = "THEME",
    val assetKey: String = "",
    val pricePaidCoins: Long = 0L,
    val originalPriceCoins: Long = 0L,
    val promoApplied: Boolean = false,
    val quantityPurchased: Long = 1L,
    val consumable: Boolean = false,
    val idempotencyKey: String = "",
    @ServerTimestamp val purchasedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ShopTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userDisplayName: String = "Player",
    val itemId: String = "",
    val itemName: String = "",
    val category: String = "THEME",
    val pricePaidCoins: Long = 0L,
    val originalPriceCoins: Long = 0L,
    val coinSource: String = "EARNED", // EARNED, PURCHASED, MIXED, FREE
    val quantity: Long = 1L,
    val promoOfferId: String = "",
    val idempotencyKey: String = "",
    val status: String = "COMPLETED",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class DailyRewardConfig(
    @DocumentId val id: String = "",
    val dayNumber: Long = 1L,
    val title: String = "Day 1 Gift",
    val description: String = "Welcome Coin boost",
    val rewardCoins: Long = 20L,
    val bonusItemName: String = "",
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LevelConfig(
    @DocumentId val id: String = "",
    val levelNumber: Long = 1L,
    val requiredLines: Long = DEFAULT_LINES_PER_LEVEL,
    val rewardCoins: Long = 100L,
    val completionsCount: Long = 0L,
    val active: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class GlobalSettings(
    @DocumentId val id: String = "global",
    val continueCostCoins: Long = 50L,
    val exchangeRatePaisaPerCoin: Long = DEFAULT_EXCHANGE_RATE_PAISA,
    val minWithdrawalPaisa: Long = MIN_WITHDRAWAL_PAISA,
    val maxDailyWithdrawals: Long = 5L,
    val allowEarnedCoinCashout: Boolean = false,
    val jazzCashEnabled: Boolean = true,
    val easypaisaEnabled: Boolean = true,
    val bankTransferEnabled: Boolean = true,
    val jazzCashInstructions: String = "SANDBOX MODE: Configure your verified JazzCash Merchant Til/MPOS credentials before accepting live PKR deposits. Submit a reference ID for manual Admin verification.",
    val easypaisaInstructions: String = "SANDBOX MODE: Configure your verified Easypaisa Business Account credentials before accepting live PKR deposits. Submit a reference ID for manual Admin verification.",
    val bankTransferInstructions: String = "SANDBOX MODE: Configure your official corporate IBAN & Bank details in Admin Settings before accepting live transfers. Admin verifies before crediting.",
    val paymentSandboxMode: Boolean = true,
    val rewardedAdConfigured: Boolean = false,
    val gameEnabled: Boolean = true,
    val maintenanceMessage: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val adminUid: String = "",
    val adminEmail: String = "",
    val action: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class GameMode(val wireName: String, val label: String) {
    CLASSIC("CLASSIC", "Classic Mode"),
    LEVEL("LEVEL", "Level Mode"),
    DAILY("DAILY", "Daily Challenge"),
    TIME("TIME", "Time Challenge");

    companion object {
        fun fromWire(raw: String): GameMode = entries.firstOrNull { it.wireName == raw } ?: CLASSIC
    }
}

enum class PaymentMethod(val wireName: String, val displayName: String) {
    JAZZCASH("JAZZCASH", "JazzCash"),
    EASYPAISA("EASYPAISA", "Easypaisa"),
    BANK_TRANSFER("BANK_TRANSFER", "Bank Transfer");

    companion object {
        fun fromWire(raw: String): PaymentMethod = entries.firstOrNull { it.wireName == raw } ?: JAZZCASH
    }
}

enum class ShopCategoryKey(val wireName: String, val displayName: String, val shortLabel: String) {
    THEME("THEME", "Themes", "Themes"),
    BLOCK_SKIN("BLOCK_SKIN", "Block Skins", "Block Skins"),
    BOARD_BACKGROUND("BOARD_BACKGROUND", "Board Backgrounds", "Backgrounds"),
    SPECIAL_EFFECT("SPECIAL_EFFECT", "Special Effects", "Effects"),
    GAME_BOOSTER("GAME_BOOSTER", "Game Boosters", "Boosters"),
    AVATAR("AVATAR", "Avatars", "Avatars"),
    PROFILE_FRAME("PROFILE_FRAME", "Profile Frames", "Frames"),
    BADGE("BADGE", "Player Badges", "Badges"),
    EXCLUSIVE("EXCLUSIVE", "Exclusive Items", "Exclusive");

    companion object {
        fun fromWire(raw: String): ShopCategoryKey = when (raw) {
            "ITEM" -> BADGE
            else -> entries.firstOrNull { it.wireName == raw } ?: THEME
        }
    }
}

enum class BoosterType(
    val wireName: String,
    val label: String,
    val description: String,
    val defaultMaxPerGame: Int
) {
    HAMMER("HAMMER", "Hammer", "Smash any occupied cell (and adjacent cells if upgraded) on the 10×10 board.", 3),
    UNDO("UNDO", "Undo", "Reverse your last valid placement, restoring board cells, tray & score.", 3),
    SHUFFLE("SHUFFLE", "Shuffle", "Replace remaining tray pieces with a fresh guaranteed-playable set.", 3),
    ROW_BLASTER("ROW_BLASTER", "Line Blaster", "Tap any board row to instantly blast & clear the entire line.", 2);

    companion object {
        fun fromWire(raw: String): BoosterType? = entries.firstOrNull { it.wireName.equals(raw, ignoreCase = true) }
    }
}

object AdminPermissions {
    const val MANAGE_PLAYERS = "MANAGE_PLAYERS"
    const val MANAGE_WALLETS = "MANAGE_WALLETS"
    const val MANAGE_DEPOSITS = "MANAGE_DEPOSITS"
    const val MANAGE_WITHDRAWALS = "MANAGE_WITHDRAWALS"
    const val MANAGE_LEVELS = "MANAGE_LEVELS"
    const val MANAGE_CHALLENGES = "MANAGE_CHALLENGES"
    const val MANAGE_REWARDS = "MANAGE_REWARDS"
    const val MANAGE_SHOP = "MANAGE_SHOP"
    const val MANAGE_SHOP_ITEMS = "MANAGE_SHOP_ITEMS"
    const val MANAGE_SHOP_PRICES = "MANAGE_SHOP_PRICES"
    const val MANAGE_SHOP_PROMOS = "MANAGE_SHOP_PROMOS"
    const val MANAGE_SHOP_CATEGORIES = "MANAGE_SHOP_CATEGORIES"
    const val VIEW_SHOP_ANALYTICS = "VIEW_SHOP_ANALYTICS"
    const val MANAGE_SETTINGS = "MANAGE_SETTINGS"
    const val VIEW_REPORTS = "VIEW_REPORTS"

    val ALL = listOf(
        MANAGE_PLAYERS, MANAGE_WALLETS, MANAGE_DEPOSITS, MANAGE_WITHDRAWALS,
        MANAGE_LEVELS, MANAGE_CHALLENGES, MANAGE_REWARDS,
        MANAGE_SHOP, MANAGE_SHOP_ITEMS, MANAGE_SHOP_PRICES, MANAGE_SHOP_PROMOS,
        MANAGE_SHOP_CATEGORIES, VIEW_SHOP_ANALYTICS,
        MANAGE_SETTINGS, VIEW_REPORTS
    )
}

fun formatPkr(paisa: Long): String {
    val negative = paisa < 0
    val absVal = kotlin.math.abs(paisa)
    val rupees = absVal / 100
    val remPaisa = absVal % 100
    val prefix = if (negative) "-PKR " else "PKR "
    return "$prefix%,d.%02d".format(rupees, remPaisa)
}
