package com.example.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AdminRepository
import com.example.data.model.AdminRecord
import com.example.data.model.AuditLog
import com.example.data.model.DailyRewardConfig
import com.example.data.model.DepositRequest
import com.example.data.model.ExchangeRecord
import com.example.data.model.GlobalSettings
import com.example.data.model.LevelConfig
import com.example.data.model.PromotionalOffer
import com.example.data.model.PurchasedItem
import com.example.data.model.ShopCategory
import com.example.data.model.ShopItem
import com.example.data.model.ShopTransaction
import com.example.data.model.TimeChallenge
import com.example.data.model.UserAccount
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.unwrapFirestoreThrowable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val adminRepository: AdminRepository,
    adminUid: String
) : ViewModel() {

    val allUsers: StateFlow<List<UserAccount>> = adminRepository.observeAllUsers(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allDeposits: StateFlow<List<DepositRequest>> = adminRepository.observeAllDeposits(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allWithdrawals: StateFlow<List<WithdrawalRequest>> = adminRepository.observeAllWithdrawals(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allExchanges: StateFlow<List<ExchangeRecord>> = adminRepository.observeAllExchanges(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTransactions: StateFlow<List<WalletTransaction>> = adminRepository.observeAllTransactions(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allTimeChallenges: StateFlow<List<TimeChallenge>> = adminRepository.observeAllTimeChallenges(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allShopItems: StateFlow<List<ShopItem>> = adminRepository.observeAllShopItems(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allShopCategories: StateFlow<List<ShopCategory>> = adminRepository.observeAllShopCategories(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allPromotionalOffers: StateFlow<List<PromotionalOffer>> = adminRepository.observeAllPromotionalOffers(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allShopTransactions: StateFlow<List<ShopTransaction>> = adminRepository.observeAllShopTransactions(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allPurchasedItems: StateFlow<List<PurchasedItem>> = adminRepository.observeAllPurchasedItems(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allDailyRewards: StateFlow<List<DailyRewardConfig>> = adminRepository.observeAllDailyRewards(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allLevels: StateFlow<List<LevelConfig>> = adminRepository.observeAllLevels(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val allAdmins: StateFlow<List<AdminRecord>> = adminRepository.observeAllAdmins(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    val auditLogs: StateFlow<List<AuditLog>> = adminRepository.observeAuditLogs(adminUid)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    private val _feedback = MutableStateFlow<String?>(null)
    val feedback: StateFlow<String?> = _feedback.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun clearFeedback() {
        _feedback.value = null
    }

    fun seedDefaults() = runAdmin("Full 9-Category Coin Shop, 7 Daily Rewards, Settings & 100 Levels synced!") {
        adminRepository.seedInitialCatalogAndSettings()
    }

    fun seedCoinShopCatalog() = runAdmin("Full 9-Category Coin Shop catalog synced (custom prices preserved)!") {
        adminRepository.seedCoinShopCatalogOnly()
    }

    fun togglePlayerBlocked(targetUid: String, blocked: Boolean) =
        runAdmin(if (blocked) "Player blocked." else "Player unblocked.") {
            adminRepository.togglePlayerBlocked(targetUid, blocked)
        }

    fun adjustCoins(targetUid: String, coinSource: String, delta: Long, reason: String) =
        runAdmin("Player $coinSource Coins adjusted by $delta.") {
            adminRepository.adjustPlayerCoins(targetUid, coinSource, delta, reason)
        }

    fun reviewDeposit(deposit: DepositRequest, newStatus: String, ref: String, note: String) =
        runAdmin("Deposit marked $newStatus.") {
            adminRepository.reviewDeposit(deposit, newStatus, ref, note)
        }

    fun reviewWithdrawal(withdrawal: WithdrawalRequest, newStatus: String, payoutRef: String, note: String) =
        runAdmin("Withdrawal marked $newStatus.") {
            adminRepository.reviewWithdrawal(withdrawal, newStatus, payoutRef, note)
        }

    fun saveTimeChallenge(
        existingId: String?,
        title: String,
        description: String,
        durationSeconds: Long,
        startTimeMillis: Long,
        endTimeMillis: Long,
        rewardType: String,
        targetScore: Long,
        rewardCoins: Long,
        active: Boolean
    ) = runAdmin("Time Challenge saved!") {
        adminRepository.saveTimeChallenge(
            existingId, title, description, durationSeconds,
            startTimeMillis, endTimeMillis, rewardType, targetScore, rewardCoins, active
        )
    }

    fun deleteTimeChallenge(id: String) = runAdmin("Time Challenge deleted.") {
        adminRepository.deleteTimeChallenge(id)
    }

    fun saveDailyReward(config: DailyRewardConfig) = runAdmin("Day ${config.dayNumber} reward saved!") {
        adminRepository.saveDailyRewardConfig(config)
    }

    fun saveShopItem(item: ShopItem) = runAdmin("Shop item '${item.name}' saved live!") {
        adminRepository.saveShopItem(item)
    }

    fun updateShopItemPriceAndPromo(
        itemId: String,
        itemName: String,
        newPriceCoins: Long,
        promoActive: Boolean,
        promoPriceCoins: Long,
        promoStartMillis: Long,
        promoEndMillis: Long,
        promoLabel: String
    ) = runAdmin("Updated price for '$itemName' to %,d Coins!".format(newPriceCoins)) {
        adminRepository.updateShopItemPriceAndPromo(
            itemId, itemName, newPriceCoins, promoActive, promoPriceCoins, promoStartMillis, promoEndMillis, promoLabel
        )
    }

    fun toggleShopItemAvailability(item: ShopItem, available: Boolean) =
        runAdmin(if (available) "'${item.name}' enabled in Coin Shop." else "'${item.name}' disabled (existing owners keep it permanently).") {
            adminRepository.toggleShopItemAvailability(item, available)
        }

    fun deleteShopItem(id: String) = runAdmin("Shop item removed from store.") {
        adminRepository.deleteShopItem(id)
    }

    fun saveShopCategory(category: ShopCategory) = runAdmin("Category '${category.displayName}' saved!") {
        adminRepository.saveShopCategory(category)
    }

    fun savePromotionalOffer(offer: PromotionalOffer) = runAdmin("Promotional offer '${offer.title}' saved!") {
        adminRepository.savePromotionalOffer(offer)
    }

    fun deletePromotionalOffer(id: String) = runAdmin("Promotional offer deleted.") {
        adminRepository.deletePromotionalOffer(id)
    }

    fun updateLevel(levelNumber: Long, rewardCoins: Long, requiredLines: Long) =
        runAdmin("Level $levelNumber updated!") {
            adminRepository.updateLevelConfig(levelNumber, rewardCoins, requiredLines)
        }

    fun saveSettings(settings: GlobalSettings) = runAdmin("Global game settings saved!") {
        adminRepository.saveGlobalSettings(settings)
    }

    fun grantAdmin(user: UserAccount, permissions: List<String>) =
        runAdmin("Admin privileges granted to ${user.email}!") {
            adminRepository.grantAdminAccess(user, permissions)
        }

    fun revokeAdmin(uid: String, email: String) = runAdmin("Revoked Admin privileges from $email.") {
        adminRepository.revokeAdminAccess(uid, email)
    }

    private fun runAdmin(successMsg: String, block: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            _busy.value = true
            val res = block()
            _busy.value = false
            res.onSuccess {
                _feedback.value = successMsg
            }.onFailure { err ->
                _feedback.value = "Error: ${unwrapFirestoreThrowable(err).message ?: "Action failed"}"
            }
        }
    }
}
