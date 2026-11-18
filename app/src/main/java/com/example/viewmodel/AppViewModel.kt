package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.AdminRepository
import com.example.data.AudioPreferences
import com.example.data.GameRepository
import com.example.data.LiveProgressSyncResult
import com.example.data.PlayerDataRepository
import com.example.data.PreferencesRepository
import com.example.data.SessionCommitResult
import com.example.data.ShopRepository
import com.example.data.WalletRepository
import com.example.data.model.AdminPermissions
import com.example.data.model.AdminRecord
import com.example.data.model.BoosterType
import com.example.data.model.ChallengeParticipant
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DailyChallenge
import com.example.data.model.DailyChallengeClaim
import com.example.data.model.DailyRewardConfig
import com.example.data.model.DepositRequest
import com.example.data.model.ExchangeRecord
import com.example.data.model.GameMode
import com.example.data.model.GlobalSettings
import com.example.data.model.LeaderboardBoardType
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardScore
import com.example.data.model.LevelConfig
import com.example.data.model.LevelProgress
import com.example.data.model.PaymentMethod
import com.example.data.model.PlayerInventory
import com.example.data.model.PromotionalOffer
import com.example.data.model.PurchasedItem
import com.example.data.model.SUPER_ADMIN_EMAIL
import com.example.data.model.ShopCategory
import com.example.data.model.ShopItem
import com.example.data.model.ShopTransaction
import com.example.data.model.TimeChallenge
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.unwrapFirestoreThrowable
import com.example.game.BlockThemes
import com.example.game.CosmeticCatalog
import com.example.security.WithdrawalPinHasher
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class AdminAccessState(
    val isSuperAdmin: Boolean = false,
    val isAdmin: Boolean = false,
    val permissions: Set<String> = emptySet()
)

class AppViewModel(
    private val gameRepository: GameRepository,
    private val playerDataRepository: PlayerDataRepository,
    private val walletRepository: WalletRepository,
    private val shopRepository: ShopRepository,
    private val adminRepository: AdminRepository,
    private val preferencesRepository: PreferencesRepository,
    val soundEngine: SoundEngine,
    val currentUserId: String
) : ViewModel() {

    private val todayKey: String = gameRepository.todayUtcDateKey()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val userAccount: StateFlow<UserAccount?> = gameRepository.observeUser(currentUserId)
        .safeCatch(null, "observeUser")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val userProfile: StateFlow<UserProfile?> = playerDataRepository.observeUserProfile(currentUserId)
        .safeCatch(null, "observeUserProfile")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val levelProgressMap: StateFlow<Map<Long, LevelProgress>> =
        playerDataRepository.observeAllLevelProgress(currentUserId)
            .map { records -> records.associateBy { it.levelNumber } }
            .safeCatch(emptyMap(), "observeAllLevelProgress")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val dailyLeaderboardScores: StateFlow<List<LeaderboardScore>> =
        playerDataRepository.observeLeaderboard(LeaderboardBoardType.DAILY, todayKey)
            .safeCatch(emptyList(), "observeDailyLeaderboardScores")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTimeLeaderboardScores: StateFlow<List<LeaderboardScore>> =
        playerDataRepository.observeLeaderboard(LeaderboardBoardType.ALL_TIME)
            .safeCatch(emptyList(), "observeAllTimeLeaderboardScores")
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val globalSettings: StateFlow<GlobalSettings> = gameRepository.observeGlobalSettings(currentUserId)
        .safeCatch(GlobalSettings(), "observeGlobalSettings")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), GlobalSettings())

    private val adminRecord: StateFlow<AdminRecord?> = gameRepository.observeAdminRecord(currentUserId)
        .safeCatch(null, "observeAdminRecord")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val adminAccess: StateFlow<AdminAccessState> = combine(userAccount, adminRecord) { _, record ->
        val authUser = Firebase.auth.currentUser
        val verifiedSuper = authUser != null &&
            authUser.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) &&
            authUser.isEmailVerified
        val activeAdminRecord = record?.takeIf { it.active }
        when {
            verifiedSuper -> AdminAccessState(
                isSuperAdmin = true,
                isAdmin = true,
                permissions = AdminPermissions.ALL.toSet()
            )
            activeAdminRecord != null -> AdminAccessState(
                isSuperAdmin = false,
                isAdmin = true,
                permissions = activeAdminRecord.permissions.toSet()
            )
            else -> AdminAccessState()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AdminAccessState())

    val shopCategories: StateFlow<List<ShopCategory>> = shopRepository.observeCategories(currentUserId)
        .safeCatch(CosmeticCatalog.defaultShopCategories(), "observeShopCategories")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CosmeticCatalog.defaultShopCategories())

    val shopItems: StateFlow<List<ShopItem>> = shopRepository.observeAvailableItems(currentUserId)
        .safeCatch(CosmeticCatalog.defaultFullShopCatalog(), "observeShopItems")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), CosmeticCatalog.defaultFullShopCatalog())

    val activePromotions: StateFlow<List<PromotionalOffer>> = shopRepository.observeActivePromotions(currentUserId)
        .safeCatch(emptyList(), "observeActivePromotions")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val playerInventory: StateFlow<PlayerInventory> = shopRepository.observePlayerInventory(currentUserId)
        .safeCatch(PlayerInventory(userId = currentUserId), "observePlayerInventory")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), PlayerInventory(userId = currentUserId))

    val purchasedItems: StateFlow<List<PurchasedItem>> = shopRepository.observePurchasedItems(currentUserId)
        .safeCatch(emptyList(), "observePurchasedItems")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val shopTransactions: StateFlow<List<ShopTransaction>> = shopRepository.observeShopTransactions(currentUserId)
        .safeCatch(emptyList(), "observeShopTransactions")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dailyRewards: StateFlow<List<DailyRewardConfig>> = gameRepository.observeDailyRewards(currentUserId)
        .safeCatch(BlockThemes.defaultDailyRewards(), "observeDailyRewards")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), BlockThemes.defaultDailyRewards())

    val levelConfigs: StateFlow<Map<Long, LevelConfig>> = gameRepository.observeLevelConfigs(currentUserId)
        .safeCatch(emptyMap(), "observeLevelConfigs")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyMap())

    val todayDailyChallenge: StateFlow<DailyChallenge> = gameRepository.observeTodayDailyChallenge(currentUserId)
        .safeCatch(DailyChallenge(id = todayKey, dateKey = todayKey), "observeDailyChallenge")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), DailyChallenge(id = todayKey, dateKey = todayKey))

    val dailyChallengeClaim: StateFlow<DailyChallengeClaim?> = gameRepository.observeDailyChallengeClaim(currentUserId, todayKey)
        .safeCatch(null, "observeDailyChallengeClaim")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val activeTimeChallenges: StateFlow<List<TimeChallenge>> = gameRepository.observeActiveTimeChallenges(currentUserId)
        .safeCatch(emptyList(), "observeActiveTimeChallenges")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val dailyLeaderboard: StateFlow<List<LeaderboardEntry>> = gameRepository.observeDailyLeaderboard(currentUserId, todayKey)
        .safeCatch(emptyList(), "observeDailyLeaderboard")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTimeLeaderboard: StateFlow<List<LeaderboardEntry>> = gameRepository.observeAllTimeLeaderboard(currentUserId)
        .safeCatch(emptyList(), "observeAllTimeLeaderboard")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val walletTransactions: StateFlow<List<WalletTransaction>> = walletRepository.observeTransactions(currentUserId)
        .safeCatch(emptyList(), "observeTransactions")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val depositHistory: StateFlow<List<DepositRequest>> = walletRepository.observeDeposits(currentUserId)
        .safeCatch(emptyList(), "observeDeposits")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val withdrawalHistory: StateFlow<List<WithdrawalRequest>> = walletRepository.observeWithdrawals(currentUserId)
        .safeCatch(emptyList(), "observeWithdrawals")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val exchangeHistory: StateFlow<List<ExchangeRecord>> = walletRepository.observeExchanges(currentUserId)
        .safeCatch(emptyList(), "observeExchanges")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val audioPreferences: StateFlow<AudioPreferences> = preferencesRepository.audioPreferences
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), AudioPreferences())

    init {
        val currentUser: FirebaseUser? = Firebase.auth.currentUser
        if (currentUser != null && currentUser.uid == currentUserId) {
            viewModelScope.launch {
                val res = gameRepository.ensureUserInitialized(currentUser)
                res.onFailure { err ->
                    _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Failed to initialize profile."
                }
                shopRepository.ensureInventoryInitialized(currentUser.uid)
                // If verified Super Admin signs in and the upgraded Coin Shop catalog isn't seeded yet, sync cleanly without overwriting custom prices
                if (currentUser.email.equals(SUPER_ADMIN_EMAIL, ignoreCase = true) && currentUser.isEmailVerified) {
                    runCatching {
                        val existing = shopItems.value
                        if (existing.size < 10) {
                            adminRepository.seedCoinShopCatalogOnly()
                        }
                    }
                }
            }
        }
        viewModelScope.launch {
            audioPreferences.collect { prefs ->
                soundEngine.setPreferences(prefs.musicEnabled, prefs.sfxEnabled)
            }
        }
    }

    fun observeChallengeParticipants(challengeId: String): Flow<List<ChallengeParticipant>> {
        return gameRepository.observeChallengeParticipants(currentUserId, challengeId)
            .safeCatch(emptyList(), "observeChallengeParticipants")
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun showMessage(msg: String) {
        _statusMessage.value = msg
    }

    fun showError(msg: String) {
        _errorMessage.value = msg
    }

    fun todayDateKey(): String = todayKey

    fun setMusicEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setMusicEnabled(enabled)
            soundEngine.setPreferences(enabled, audioPreferences.value.sfxEnabled)
            gameRepository.updateAudioSettings(enabled, audioPreferences.value.sfxEnabled)
        }
    }

    fun setSfxEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setSfxEnabled(enabled)
            soundEngine.setPreferences(audioPreferences.value.musicEnabled, enabled)
            gameRepository.updateAudioSettings(audioPreferences.value.musicEnabled, enabled)
        }
    }

    fun updateDisplayName(newName: String) = runAction("Profile name updated!") {
        playerDataRepository.updateUserProfile(displayName = newName)
    }

    fun selectTheme(themeId: String) = runAction("Theme applied!") {
        val invRes = shopRepository.equipCosmetic(
            category = "THEME",
            subType = "",
            assetKey = themeId,
            itemId = "theme_$themeId",
            isFreeDefault = themeId == "simple_classic"
        )
        if (invRes.isSuccess) invRes else gameRepository.selectTheme(themeId)
    }

    fun purchaseShopItem(item: ShopItem) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = shopRepository.purchaseItem(
                itemId = item.id,
                activePromotions = activePromotions.value
            )
            _isBusy.value = false
            res.onSuccess { receipt ->
                soundEngine.playCoinReward()
                _statusMessage.value = if (item.consumable) {
                    "Purchased ${item.name} (+${receipt.quantityGranted} added to Boosters)!"
                } else {
                    "Unlocked ${item.name} permanently! Equip it anytime in My Collection."
                }
            }.onFailure { err ->
                _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Purchase failed."
            }
        }
    }

    fun equipShopItem(item: ShopItem) = runAction("Equipped ${item.name}!") {
        shopRepository.equipCosmetic(
            category = item.category,
            subType = item.subType,
            assetKey = item.resolvedAssetKey,
            itemId = item.id,
            isFreeDefault = item.priceCoins == 0L && item.id == "theme_simple_classic"
        )
    }

    fun equipPurchasedItem(purchased: PurchasedItem, subType: String = "") =
        runAction("Equipped ${purchased.itemName}!") {
            shopRepository.equipCosmetic(
                category = purchased.category,
                subType = subType,
                assetKey = purchased.assetKey,
                itemId = purchased.itemId
            )
        }

    fun unequipCosmeticSlot(category: String, subType: String = "") =
        runAction("Reset to default style.") {
            shopRepository.equipCosmetic(
                category = category,
                subType = subType,
                assetKey = "",
                itemId = "",
                isFreeDefault = true
            )
        }

    suspend fun consumeBooster(type: BoosterType): Result<Long> {
        val res = shopRepository.consumeBooster(type)
        res.onFailure { err ->
            _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Could not use ${type.label}."
        }
        return res
    }

    fun claimDailyLoginReward() {
        val user = userAccount.value ?: return
        val nextDay = user.dailyRewardNextDay.coerceIn(1L, 7L)
        val config = dailyRewards.value.firstOrNull { it.dayNumber == nextDay }
            ?: BlockThemes.defaultDailyRewards().first { it.dayNumber == nextDay }
        viewModelScope.launch {
            _isBusy.value = true
            val res = gameRepository.claimDailyLoginReward(config)
            _isBusy.value = false
            res.onSuccess { coins ->
                soundEngine.playCoinReward()
                _statusMessage.value = "Claimed Day $nextDay Reward: +$coins Coins!"
            }.onFailure { err ->
                _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Could not claim daily reward."
            }
        }
    }

    suspend fun spendCoinsForContinue(): Result<Unit> {
        val cost = globalSettings.value.continueCostCoins
        val res = gameRepository.spendCoinsForContinue(cost)
        res.onFailure { err ->
            _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Not enough Coins to continue."
        }
        return res
    }

    suspend fun saveLiveProgressDelta(
        mode: GameMode,
        deltaLines: Long,
        sessionTotalLines: Long,
        levelNumber: Long = 1L
    ): Result<LiveProgressSyncResult> {
        val levelReward = levelConfigs.value[levelNumber]?.rewardCoins ?: (100L + levelNumber * 10L)
        val res = gameRepository.saveLiveProgressDelta(
            mode = mode,
            deltaLines = deltaLines,
            sessionTotalLines = sessionTotalLines,
            levelRewardCoins = levelReward
        )
        if (res.isSuccess && deltaLines > 0L) {
            val safeLevel = levelNumber.coerceIn(1L, 100L)
            val requiredLines = levelConfigs.value[safeLevel]?.requiredLines ?: DEFAULT_LINES_PER_LEVEL
            playerDataRepository.recordLevelProgress(
                levelNumber = safeLevel,
                linesAdded = deltaLines,
                sessionScore = (sessionTotalLines.coerceAtLeast(0L) * 10L),
                requiredLines = requiredLines,
                incrementAttempt = false
            )
        }
        return res
    }

    suspend fun commitSession(
        mode: GameMode,
        linesCleared: Long,
        levelNumber: Long = 0L,
        activeTimeChallenge: TimeChallenge? = null,
        continuesUsed: Long = 0L,
        sessionTotalLines: Long = linesCleared
    ): Result<SessionCommitResult> {
        val levelReward = levelConfigs.value[levelNumber]?.rewardCoins ?: (100L + levelNumber * 10L)
        val res = gameRepository.commitGameSession(
            mode = mode,
            linesCleared = linesCleared,
            levelNumber = levelNumber,
            activeTimeChallenge = activeTimeChallenge,
            dailyChallenge = if (mode == GameMode.DAILY) todayDailyChallenge.value else null,
            levelRewardCoins = levelReward,
            continuesUsed = continuesUsed,
            sessionTotalLines = sessionTotalLines
        )
        res.onSuccess {
            if (linesCleared > 0L || sessionTotalLines > 0L) {
                val safeLevel = (if (levelNumber > 0L) levelNumber else (userAccount.value?.currentLevel ?: 1L))
                    .coerceIn(1L, 100L)
                val requiredLines = levelConfigs.value[safeLevel]?.requiredLines ?: DEFAULT_LINES_PER_LEVEL
                playerDataRepository.recordLevelProgress(
                    levelNumber = safeLevel,
                    linesAdded = linesCleared.coerceAtLeast(0L),
                    sessionScore = (sessionTotalLines.coerceAtLeast(0L) * 10L),
                    requiredLines = requiredLines,
                    incrementAttempt = true
                )
            }
        }.onFailure { err ->
            _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Could not save game session."
        }
        return res
    }

    fun saveWalletVerificationForm(fullName: String, provider: String, accountNumber: String) =
        runAction("Wallet details saved and form marked complete.") {
            gameRepository.saveWalletVerificationForm(fullName, provider, accountNumber)
        }

    fun setWithdrawalSecurityPassword(newPassword: String) {
        if (newPassword.length < 6) {
            _errorMessage.value = "Withdrawal password must be at least 6 characters."
            return
        }
        val salt = WithdrawalPinHasher.generateSalt()
        val hash = WithdrawalPinHasher.hashPassword(newPassword, salt)
        runAction("Withdrawal security password saved.") {
            gameRepository.saveWithdrawalSecurityPassword(hash, salt)
        }
    }

    fun exchangePkrToCoins(coinsToBuy: Long) = runAction("Converted PKR to $coinsToBuy Purchased Coins!") {
        val res = walletRepository.exchangePkrToCoins(coinsToBuy)
        if (res.isSuccess) soundEngine.playCoinReward()
        res
    }

    fun exchangeCoinsToPkr(coinsToConvert: Long) = runAction("Converted $coinsToConvert Coins to Main PKR Wallet!") {
        val res = walletRepository.exchangeCoinsToPkr(coinsToConvert)
        if (res.isSuccess) soundEngine.playCoinReward()
        res
    }

    fun submitDepositRequest(
        amountPkrRupees: Long,
        paymentMethod: PaymentMethod,
        transactionRef: String,
        senderAccount: String
    ) {
        val user = userAccount.value ?: run {
            _errorMessage.value = "Account still loading."
            return
        }
        val sandbox = globalSettings.value.paymentSandboxMode
        viewModelScope.launch {
            _isBusy.value = true
            val res = walletRepository.submitDepositRequest(
                user = user,
                amountPkrRupees = amountPkrRupees,
                paymentMethod = paymentMethod,
                transactionRef = transactionRef,
                senderAccount = senderAccount,
                sandboxMode = sandbox
            )
            _isBusy.value = false
            res.onSuccess { refId ->
                _statusMessage.value = "Deposit request submitted (ID: ${refId.takeLast(8)}). Status: Pending Admin Verification."
            }.onFailure { err ->
                _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Deposit request failed."
            }
        }
    }

    fun submitWithdrawalRequest(
        amountPkrRupees: Long,
        paymentMethod: PaymentMethod,
        accountTitle: String,
        accountNumber: String,
        enteredPassword: String
    ) {
        val user = userAccount.value ?: run {
            _errorMessage.value = "Account still loading."
            return
        }
        viewModelScope.launch {
            _isBusy.value = true
            val res = walletRepository.submitWithdrawalRequest(
                user = user,
                amountPkrRupees = amountPkrRupees,
                paymentMethod = paymentMethod,
                accountTitle = accountTitle,
                accountNumber = accountNumber,
                enteredPassword = enteredPassword,
                settings = globalSettings.value
            )
            _isBusy.value = false
            res.onSuccess { refId ->
                _statusMessage.value = "Withdrawal request submitted (ID: ${refId.takeLast(8)}). Status: Pending Admin Review."
            }.onFailure { err ->
                _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Withdrawal request failed."
            }
        }
    }

    private fun runAction(successMessage: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = block()
            _isBusy.value = false
            result.onSuccess {
                _statusMessage.value = successMessage
            }.onFailure { err ->
                _errorMessage.value = unwrapFirestoreThrowable(err).message ?: "Operation failed."
            }
        }
    }

    private fun <T> Flow<T>.safeCatch(fallback: T, label: String): Flow<T> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        Log.w("AppViewModel", "Stream error in $label: ${(firestoreError ?: error).message}")
        emit(fallback)
    }

    override fun onCleared() {
        super.onCleared()
        soundEngine.stopMusic()
    }
}
