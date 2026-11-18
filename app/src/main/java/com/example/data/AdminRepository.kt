package com.example.data

import com.example.data.model.AdminRecord
import com.example.data.model.AuditLog
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
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
import com.example.data.model.formatPkr
import com.example.game.BlockThemes
import com.example.game.CosmeticCatalog
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class AdminRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireAdminIdentity(): Pair<String, String> {
        val u = auth.currentUser ?: throw IllegalStateException("Admin must be signed in.")
        return u.uid to (u.email ?: "")
    }

    fun observeAllUsers(adminUid: String): Flow<List<UserAccount>> = safeAdminList("users", adminUid) {
        it.toObjects(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { u -> u.updatedAt ?: Timestamp(0, 0) }
    }

    fun observeAllDeposits(adminUid: String): Flow<List<DepositRequest>> = safeAdminList("deposits", adminUid) {
        it.toObjects(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { d -> d.createdAt ?: Timestamp(0, 0) }
    }

    fun observeAllWithdrawals(adminUid: String): Flow<List<WithdrawalRequest>> = safeAdminList("withdrawals", adminUid) {
        it.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { w -> w.createdAt ?: Timestamp(0, 0) }
    }

    fun observeAllExchanges(adminUid: String): Flow<List<ExchangeRecord>> = safeAdminList("exchanges", adminUid) {
        it.toObjects(ExchangeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { e -> e.createdAt ?: Timestamp(0, 0) }
    }

    fun observeAllTransactions(adminUid: String): Flow<List<WalletTransaction>> = safeAdminList("walletTransactions", adminUid) {
        it.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { t -> t.createdAt ?: Timestamp(0, 0) }
    }

    fun observeAllTimeChallenges(adminUid: String): Flow<List<TimeChallenge>> = safeAdminList("timeChallenges", adminUid) {
        it.toObjects(TimeChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { c -> c.startTimeMillis }
    }

    fun observeAllShopItems(adminUid: String): Flow<List<ShopItem>> = safeAdminList("shopItems", adminUid) {
        it.toObjects(ShopItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { s -> s.sortOrder }
    }

    fun observeAllShopCategories(adminUid: String): Flow<List<ShopCategory>> = safeAdminList("shopCategories", adminUid) {
        it.toObjects(ShopCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { c -> c.sortOrder }
    }

    fun observeAllPromotionalOffers(adminUid: String): Flow<List<PromotionalOffer>> = safeAdminList("promotionalOffers", adminUid) {
        it.toObjects(PromotionalOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { p -> p.updatedAt ?: Timestamp(0, 0) }
    }

    fun observeAllShopTransactions(adminUid: String): Flow<List<ShopTransaction>> = safeAdminList("shopTransactions", adminUid) {
        it.toObjects(ShopTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { t -> t.createdAt ?: Timestamp(0, 0) }
    }

    fun observeAllPurchasedItems(adminUid: String): Flow<List<PurchasedItem>> = safeAdminList("purchasedItems", adminUid) {
        it.toObjects(PurchasedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { p -> p.purchasedAt ?: Timestamp(0, 0) }
    }

    fun observeAllDailyRewards(adminUid: String): Flow<List<DailyRewardConfig>> = safeAdminList("dailyRewards", adminUid) {
        it.toObjects(DailyRewardConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { r -> r.dayNumber }
    }

    fun observeAllLevels(adminUid: String): Flow<List<LevelConfig>> = safeAdminList("levels", adminUid) {
        it.toObjects(LevelConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedBy { l -> l.levelNumber }
    }

    fun observeAllAdmins(adminUid: String): Flow<List<AdminRecord>> = safeAdminList("admins", adminUid) {
        it.toObjects(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { a -> a.updatedAt ?: Timestamp(0, 0) }
    }

    fun observeAuditLogs(adminUid: String): Flow<List<AuditLog>> = safeAdminList("auditLogs", adminUid) {
        it.toObjects(AuditLog::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            .sortedByDescending { l -> l.createdAt ?: Timestamp(0, 0) }
    }

    private fun <T> safeAdminList(
        collectionPath: String,
        adminUid: String,
        mapper: (com.google.firebase.firestore.QuerySnapshot) -> List<T>
    ): Flow<List<T>> = flow {
        if (adminUid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection(collectionPath)
                .snapshots()
                .map(mapper)
                .catch { err ->
                    val fsErr = generateSequence(err) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && err is CancellationException) throw err
                    if (auth.currentUser?.uid == adminUid) {
                        handleFirestoreError(fsErr ?: err, OperationType.LIST, collectionPath)
                    }
                    emit(emptyList())
                }
        )
    }

    private suspend fun writeAuditLog(action: String, targetType: String, targetId: String, details: String) {
        val (uid, email) = requireAdminIdentity()
        val logId = "aud_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        db.collection("auditLogs").document(logId).set(
            mapOf(
                "adminUid" to uid,
                "adminEmail" to email.take(120),
                "action" to action.take(80),
                "targetType" to targetType.take(50),
                "targetId" to targetId.take(128),
                "details" to details.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    /**
     * Initializes default global settings, Coin Shop themes, 7 Daily Login Rewards, and Levels 1..100.
     */
    suspend fun seedInitialCatalogAndSettings(): Result<Unit> {
        return try {
            requireAdminIdentity()
            val batch = db.batch()

            val settingsRef = db.collection("settings").document("global")
            if (!settingsRef.get().await().exists()) {
                val defaults = GlobalSettings()
                batch.set(
                    settingsRef,
                    mapOf(
                        "continueCostCoins" to defaults.continueCostCoins,
                        "exchangeRatePaisaPerCoin" to defaults.exchangeRatePaisaPerCoin,
                        "minWithdrawalPaisa" to defaults.minWithdrawalPaisa,
                        "maxDailyWithdrawals" to defaults.maxDailyWithdrawals,
                        "allowEarnedCoinCashout" to defaults.allowEarnedCoinCashout,
                        "jazzCashEnabled" to defaults.jazzCashEnabled,
                        "easypaisaEnabled" to defaults.easypaisaEnabled,
                        "bankTransferEnabled" to defaults.bankTransferEnabled,
                        "jazzCashInstructions" to defaults.jazzCashInstructions,
                        "easypaisaInstructions" to defaults.easypaisaInstructions,
                        "bankTransferInstructions" to defaults.bankTransferInstructions,
                        "paymentSandboxMode" to defaults.paymentSandboxMode,
                        "rewardedAdConfigured" to defaults.rewardedAdConfigured,
                        "gameEnabled" to defaults.gameEnabled,
                        "maintenanceMessage" to defaults.maintenanceMessage,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            val existingItemsSnap = db.collection("shopItems").get().await()
            val existingById = existingItemsSnap.documents.associateBy { it.id }

            for (cat in CosmeticCatalog.defaultShopCategories()) {
                val catRef = db.collection("shopCategories").document(cat.id)
                batch.set(catRef, shopCategoryPayload(cat, isUpdate = false))
            }

            for (item in CosmeticCatalog.defaultFullShopCatalog()) {
                val ref = db.collection("shopItems").document(item.id)
                val existingDoc = existingById[item.id]
                // Preserve Admin-configured custom price & availability on existing items!
                val preservedPrice = existingDoc?.getLong("priceCoins") ?: item.priceCoins
                val preservedAvailable = existingDoc?.getBoolean("available") ?: item.available
                val preservedSales = existingDoc?.getLong("totalSalesCount") ?: item.totalSalesCount
                val preservedOwners = existingDoc?.getLong("uniqueOwnersCount") ?: item.uniqueOwnersCount
                val preservedCreatedAt = existingDoc?.getTimestamp("createdAt")
                val merged = item.copy(
                    priceCoins = preservedPrice,
                    available = preservedAvailable,
                    totalSalesCount = preservedSales,
                    uniqueOwnersCount = preservedOwners
                )
                val payload = shopItemPayload(merged, preservedCreatedAt = preservedCreatedAt)
                batch.set(ref, payload)
            }

            for (reward in BlockThemes.defaultDailyRewards()) {
                val ref = db.collection("dailyRewards").document(reward.id)
                batch.set(
                    ref,
                    mapOf(
                        "dayNumber" to reward.dayNumber,
                        "title" to reward.title,
                        "description" to reward.description,
                        "rewardCoins" to reward.rewardCoins,
                        "bonusItemName" to reward.bonusItemName,
                        "active" to reward.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            for (lvl in 1..100) {
                val ref = db.collection("levels").document("level_$lvl")
                batch.set(
                    ref,
                    mapOf(
                        "levelNumber" to lvl.toLong(),
                        "requiredLines" to DEFAULT_LINES_PER_LEVEL,
                        "rewardCoins" to (100L + (lvl * 10L)),
                        "completionsCount" to 0L,
                        "active" to true,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            batch.commit().await()
            writeAuditLog(
                "SEED_DEFAULTS",
                "SYSTEM",
                "global",
                "Synced global settings, 9 shop categories, ${CosmeticCatalog.defaultFullShopCatalog().size} Coin Shop items (preserving custom prices), 7 daily rewards & 100 levels."
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/global")
            Result.failure(e)
        }
    }

    suspend fun seedCoinShopCatalogOnly(): Result<Unit> {
        return try {
            requireAdminIdentity()
            val batch = db.batch()
            val existingById = db.collection("shopItems").get().await().documents.associateBy { it.id }

            for (cat in CosmeticCatalog.defaultShopCategories()) {
                batch.set(db.collection("shopCategories").document(cat.id), shopCategoryPayload(cat, isUpdate = false))
            }
            for (item in CosmeticCatalog.defaultFullShopCatalog()) {
                val existingDoc = existingById[item.id]
                val merged = item.copy(
                    priceCoins = existingDoc?.getLong("priceCoins") ?: item.priceCoins,
                    available = existingDoc?.getBoolean("available") ?: item.available,
                    totalSalesCount = existingDoc?.getLong("totalSalesCount") ?: item.totalSalesCount,
                    uniqueOwnersCount = existingDoc?.getLong("uniqueOwnersCount") ?: item.uniqueOwnersCount
                )
                batch.set(
                    db.collection("shopItems").document(item.id),
                    shopItemPayload(merged, preservedCreatedAt = existingDoc?.getTimestamp("createdAt"))
                )
            }
            batch.commit().await()
            writeAuditLog("SEED_COIN_SHOP", "SHOP_CATALOG", "all", "Synced full 9-category Coin Shop catalog (${CosmeticCatalog.defaultFullShopCatalog().size} items).")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "shopItems")
            Result.failure(e)
        }
    }

    suspend fun togglePlayerBlocked(targetUid: String, blocked: Boolean): Result<Unit> {
        return try {
            requireAdminIdentity()
            db.collection("users").document(targetUid).update(
                mapOf(
                    "blocked" to blocked,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                if (blocked) "BLOCK_PLAYER" else "UNBLOCK_PLAYER",
                "USER",
                targetUid,
                "Set blocked=$blocked for player $targetUid"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun adjustPlayerCoins(
        targetUid: String,
        coinSource: String,
        deltaCoins: Long,
        reason: String
    ): Result<Unit> {
        val cleanReason = reason.trim().take(180)
        if (cleanReason.length < 3) return Result.failure(IllegalArgumentException("Provide a clear audit reason (min 3 chars)."))
        if (deltaCoins == 0L) return Result.failure(IllegalArgumentException("Coin delta cannot be zero."))
        return try {
            requireAdminIdentity()
            val userRef = db.collection("users").document(targetUid)
            db.runTransaction { tx ->
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player not found.")
                val newPurchased = if (coinSource == "PURCHASED") user.purchasedCoins + deltaCoins else user.purchasedCoins
                val newEarned = if (coinSource == "EARNED") user.earnedCoins + deltaCoins else user.earnedCoins
                if (newPurchased < 0L || newEarned < 0L) {
                    throw IllegalStateException("Adjustment would create a negative Coin balance.")
                }

                tx.update(
                    userRef,
                    mapOf(
                        "purchasedCoins" to newPurchased,
                        "earnedCoins" to newEarned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val txId = "adm_adj_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
                tx.set(
                    db.collection("walletTransactions").document(txId),
                    mapOf(
                        "userId" to targetUid,
                        "walletType" to "COIN",
                        "coinSource" to coinSource,
                        "txType" to "ADMIN_ADJUST",
                        "amountDelta" to deltaCoins,
                        "balanceAfter" to (newPurchased + newEarned),
                        "description" to "Admin adjustment ($coinSource): $cleanReason",
                        "referenceId" to txId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()

            writeAuditLog("ADJUST_COINS", "USER", targetUid, "Adjusted $coinSource coins by $deltaCoins. Reason: $cleanReason")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "users/$targetUid")
            Result.failure(e)
        }
    }

    suspend fun reviewDeposit(
        deposit: DepositRequest,
        newStatus: String,
        adminPaymentRef: String,
        adminNote: String
    ): Result<Unit> {
        return try {
            val (adminUid, _) = requireAdminIdentity()
            val depRef = db.collection("deposits").document(deposit.id)
            val userRef = db.collection("users").document(deposit.userId)
            db.runTransaction { tx ->
                val freshDep = tx.get(depRef).toObject(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Deposit not found.")
                if (freshDep.status in listOf("APPROVED", "REJECTED")) {
                    throw IllegalStateException("Deposit already finalized as ${freshDep.status}.")
                }
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User not found.")

                tx.update(
                    depRef,
                    mapOf(
                        "status" to newStatus,
                        "adminPaymentRef" to adminPaymentRef.trim().take(120),
                        "adminNote" to adminNote.trim().take(300),
                        "reviewedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (newStatus == "APPROVED") {
                    val newMainPaisa = user.mainWalletPkrPaisa + freshDep.amountPaisa
                    tx.update(
                        userRef,
                        mapOf(
                            "mainWalletPkrPaisa" to newMainPaisa,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val txId = "dep_crd_${freshDep.id.takeLast(14)}"
                    tx.set(
                        db.collection("walletTransactions").document(txId),
                        mapOf(
                            "userId" to freshDep.userId,
                            "walletType" to "MAIN_PKR",
                            "coinSource" to "NONE",
                            "txType" to "DEPOSIT_CREDIT",
                            "amountDelta" to freshDep.amountPaisa,
                            "balanceAfter" to newMainPaisa,
                            "description" to "Deposit Approved (${freshDep.paymentMethod} • Ref: ${freshDep.transactionRef})",
                            "referenceId" to freshDep.id,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            writeAuditLog(
                "DEPOSIT_$newStatus",
                "DEPOSIT",
                deposit.id,
                "Deposit ${formatPkr(deposit.amountPaisa)} for ${deposit.userEmail} -> $newStatus (Ref: $adminPaymentRef)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "deposits/${deposit.id}")
            Result.failure(e)
        }
    }

    suspend fun reviewWithdrawal(
        withdrawal: WithdrawalRequest,
        newStatus: String,
        payoutReference: String,
        adminNote: String
    ): Result<Unit> {
        return try {
            val (adminUid, _) = requireAdminIdentity()
            val wdRef = db.collection("withdrawals").document(withdrawal.id)
            val userRef = db.collection("users").document(withdrawal.userId)
            db.runTransaction { tx ->
                val freshWd = tx.get(wdRef).toObject(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Withdrawal not found.")
                if (freshWd.status in listOf("PAID", "REJECTED")) {
                    throw IllegalStateException("Withdrawal already finalized as ${freshWd.status}.")
                }
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User not found.")

                tx.update(
                    wdRef,
                    mapOf(
                        "status" to newStatus,
                        "payoutReference" to payoutReference.trim().take(120),
                        "adminNote" to adminNote.trim().take(300),
                        "reviewedBy" to adminUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (newStatus == "REJECTED") {
                    val refundedPaisa = user.mainWalletPkrPaisa + freshWd.amountPaisa
                    tx.update(
                        userRef,
                        mapOf(
                            "mainWalletPkrPaisa" to refundedPaisa,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                    val txId = "wd_ref_${freshWd.id.takeLast(14)}"
                    tx.set(
                        db.collection("walletTransactions").document(txId),
                        mapOf(
                            "userId" to freshWd.userId,
                            "walletType" to "MAIN_PKR",
                            "coinSource" to "NONE",
                            "txType" to "WITHDRAWAL_REFUND",
                            "amountDelta" to freshWd.amountPaisa,
                            "balanceAfter" to refundedPaisa,
                            "description" to "Withdrawal Rejected — Refunded ${formatPkr(freshWd.amountPaisa)}",
                            "referenceId" to freshWd.id,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else if (newStatus == "PAID") {
                    val txId = "wd_paid_${freshWd.id.takeLast(14)}"
                    tx.set(
                        db.collection("walletTransactions").document(txId),
                        mapOf(
                            "userId" to freshWd.userId,
                            "walletType" to "MAIN_PKR",
                            "coinSource" to "NONE",
                            "txType" to "WITHDRAWAL_PAID",
                            "amountDelta" to 0L,
                            "balanceAfter" to user.mainWalletPkrPaisa,
                            "description" to "Withdrawal Marked Paid (${freshWd.paymentMethod} • Ref: $payoutReference)",
                            "referenceId" to freshWd.id,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()

            writeAuditLog(
                "WITHDRAWAL_$newStatus",
                "WITHDRAWAL",
                withdrawal.id,
                "Withdrawal ${formatPkr(withdrawal.amountPaisa)} for ${withdrawal.userEmail} -> $newStatus (Payout Ref: $payoutReference)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "withdrawals/${withdrawal.id}")
            Result.failure(e)
        }
    }

    suspend fun saveTimeChallenge(
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
    ): Result<Unit> {
        return try {
            val (adminUid, _) = requireAdminIdentity()
            val docId = existingId?.takeIf { it.isNotBlank() } ?: "tc_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val ref = db.collection("timeChallenges").document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "title" to title.trim().take(100),
                        "description" to description.trim().take(500),
                        "durationSeconds" to durationSeconds.coerceIn(30L, 86400L),
                        "startTimeMillis" to startTimeMillis,
                        "endTimeMillis" to maxOf(endTimeMillis, startTimeMillis + 60_000L),
                        "rewardType" to rewardType,
                        "targetScore" to targetScore.coerceAtLeast(0L),
                        "rewardCoins" to rewardCoins.coerceIn(0L, 100_000L),
                        "active" to active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "title" to title.trim().take(100),
                        "description" to description.trim().take(500),
                        "durationSeconds" to durationSeconds.coerceIn(30L, 86400L),
                        "startTimeMillis" to startTimeMillis,
                        "endTimeMillis" to maxOf(endTimeMillis, startTimeMillis + 60_000L),
                        "rewardType" to rewardType,
                        "targetScore" to targetScore.coerceAtLeast(0L),
                        "rewardCoins" to rewardCoins.coerceIn(0L, 100_000L),
                        "active" to active,
                        "createdBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("SAVE_TIME_CHALLENGE", "TIME_CHALLENGE", docId, "Saved Time Challenge '$title' ($rewardType, active=$active)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "timeChallenges")
            Result.failure(e)
        }
    }

    suspend fun deleteTimeChallenge(challengeId: String): Result<Unit> {
        return try {
            requireAdminIdentity()
            db.collection("timeChallenges").document(challengeId).delete().await()
            writeAuditLog("DELETE_TIME_CHALLENGE", "TIME_CHALLENGE", challengeId, "Deleted Time Challenge $challengeId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "timeChallenges/$challengeId")
            Result.failure(e)
        }
    }

    suspend fun saveDailyRewardConfig(config: DailyRewardConfig): Result<Unit> {
        return try {
            requireAdminIdentity()
            val docId = "day_${config.dayNumber.coerceIn(1L, 7L)}"
            db.collection("dailyRewards").document(docId).set(
                mapOf(
                    "dayNumber" to config.dayNumber.coerceIn(1L, 7L),
                    "title" to config.title.trim().take(60),
                    "description" to config.description.trim().take(200),
                    "rewardCoins" to config.rewardCoins.coerceIn(0L, 5000L),
                    "bonusItemName" to config.bonusItemName.trim().take(80),
                    "active" to config.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("UPDATE_DAILY_REWARD", "DAILY_REWARD", docId, "Updated Day ${config.dayNumber}: ${config.rewardCoins} Coins")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "dailyRewards")
            Result.failure(e)
        }
    }

    private fun shopItemPayload(
        item: ShopItem,
        preservedCreatedAt: Timestamp? = null
    ): Map<String, Any> = mapOf(
        "name" to item.name.trim().take(80),
        "description" to item.description.trim().take(300),
        "category" to item.category,
        "subType" to item.subType.trim().take(40),
        "themeKey" to (if (item.category == "THEME") item.resolvedAssetKey else item.themeKey).trim().take(60),
        "assetKey" to item.resolvedAssetKey.trim().take(60),
        "imageUrl" to item.imageUrl.trim().take(120_000),
        "priceCoins" to item.priceCoins.coerceIn(0L, 1_000_000L),
        "promoActive" to item.promoActive,
        "promoPriceCoins" to item.promoPriceCoins.coerceIn(0L, 1_000_000L),
        "promoStartMillis" to item.promoStartMillis.coerceAtLeast(0L),
        "promoEndMillis" to item.promoEndMillis.coerceAtLeast(0L),
        "promoLabel" to item.promoLabel.trim().take(40),
        "premiumPastel" to item.premiumPastel,
        "featured" to item.featured,
        "newArrival" to item.newArrival,
        "limitedEdition" to item.limitedEdition,
        "available" to item.available,
        "consumable" to (item.consumable || item.category == "GAME_BOOSTER"),
        "boosterPackQty" to item.boosterPackQty.coerceIn(1L, 99L),
        "boosterMaxPerGame" to item.boosterMaxPerGame.coerceIn(0L, 50L),
        "boosterPowerRadius" to item.boosterPowerRadius.coerceIn(0L, 10L),
        "sortOrder" to item.sortOrder.coerceIn(0L, 10_000L),
        "accentHex" to item.accentHex.trim().take(12).ifBlank { "#38E8FF" },
        "totalSalesCount" to item.totalSalesCount.coerceAtLeast(0L),
        "uniqueOwnersCount" to item.uniqueOwnersCount.coerceAtLeast(0L),
        "createdAt" to (preservedCreatedAt ?: FieldValue.serverTimestamp()),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    private fun shopCategoryPayload(
        cat: ShopCategory,
        isUpdate: Boolean,
        preservedCreatedAt: Timestamp? = null
    ): Map<String, Any> = if (isUpdate) {
        mapOf(
            "categoryKey" to cat.categoryKey.trim().take(40),
            "displayName" to cat.displayName.trim().take(60),
            "description" to cat.description.trim().take(200),
            "iconKey" to cat.iconKey.trim().take(40),
            "accentHex" to cat.accentHex.trim().take(12).ifBlank { "#38E8FF" },
            "enabled" to cat.enabled,
            "sortOrder" to cat.sortOrder.coerceIn(0L, 1000L),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    } else {
        mapOf(
            "categoryKey" to cat.categoryKey.trim().take(40),
            "displayName" to cat.displayName.trim().take(60),
            "description" to cat.description.trim().take(200),
            "iconKey" to cat.iconKey.trim().take(40),
            "accentHex" to cat.accentHex.trim().take(12).ifBlank { "#38E8FF" },
            "enabled" to cat.enabled,
            "sortOrder" to cat.sortOrder.coerceIn(0L, 1000L),
            "createdAt" to (preservedCreatedAt ?: FieldValue.serverTimestamp()),
            "updatedAt" to FieldValue.serverTimestamp()
        )
    }

    suspend fun saveShopItem(item: ShopItem): Result<Unit> {
        return try {
            requireAdminIdentity()
            val docId = item.id.takeIf { it.isNotBlank() }
                ?: "shop_${item.category.lowercase().take(8)}_${System.currentTimeMillis()}"
            val ref = db.collection("shopItems").document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                val preservedCreatedAt = snap.getTimestamp("createdAt")
                val preservedSales = snap.getLong("totalSalesCount") ?: item.totalSalesCount
                val preservedOwners = snap.getLong("uniqueOwnersCount") ?: item.uniqueOwnersCount
                val merged = item.copy(
                    id = docId,
                    totalSalesCount = preservedSales,
                    uniqueOwnersCount = preservedOwners
                )
                ref.set(shopItemPayload(merged, preservedCreatedAt = preservedCreatedAt)).await()
            } else {
                ref.set(shopItemPayload(item.copy(id = docId))).await()
            }
            writeAuditLog(
                "SAVE_SHOP_ITEM",
                "SHOP_ITEM",
                docId,
                "Saved '${item.name}' [${item.category}] — ${item.priceCoins} Coins (available=${item.available}, promo=${item.promoActive})"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "shopItems")
            Result.failure(e)
        }
    }

    /**
     * Price & inline promotion update compatible with both full Shop Admins and
     * scoped MANAGE_SHOP_PRICES sub-admins.
     */
    suspend fun updateShopItemPriceAndPromo(
        itemId: String,
        itemName: String,
        newPriceCoins: Long,
        promoActive: Boolean,
        promoPriceCoins: Long,
        promoStartMillis: Long,
        promoEndMillis: Long,
        promoLabel: String
    ): Result<Unit> {
        return try {
            requireAdminIdentity()
            val safePrice = newPriceCoins.coerceIn(0L, 1_000_000L)
            val safePromoPrice = promoPriceCoins.coerceIn(0L, safePrice)
            db.collection("shopItems").document(itemId).update(
                mapOf(
                    "priceCoins" to safePrice,
                    "promoActive" to promoActive,
                    "promoPriceCoins" to safePromoPrice,
                    "promoStartMillis" to promoStartMillis.coerceAtLeast(0L),
                    "promoEndMillis" to promoEndMillis.coerceAtLeast(0L),
                    "promoLabel" to promoLabel.trim().take(40),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog(
                "UPDATE_SHOP_PRICE",
                "SHOP_ITEM",
                itemId,
                "Updated price for '$itemName': $safePrice Coins" +
                    if (promoActive) " (Promo: $safePromoPrice Coins '$promoLabel')" else " (No Promo)"
            )
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "shopItems/$itemId")
            Result.failure(e)
        }
    }

    suspend fun toggleShopItemAvailability(item: ShopItem, available: Boolean): Result<Unit> {
        return saveShopItem(item.copy(available = available))
    }

    suspend fun deleteShopItem(itemId: String): Result<Unit> {
        return try {
            requireAdminIdentity()
            db.collection("shopItems").document(itemId).delete().await()
            writeAuditLog("DELETE_SHOP_ITEM", "SHOP_ITEM", itemId, "Deleted shop item $itemId (existing owners retain permanent ownership receipts)")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "shopItems/$itemId")
            Result.failure(e)
        }
    }

    suspend fun saveShopCategory(category: ShopCategory): Result<Unit> {
        return try {
            requireAdminIdentity()
            val docId = category.id.takeIf { it.isNotBlank() }
                ?: "cat_${category.categoryKey.lowercase().replace(Regex("[^a-z0-9_]"), "")}"
            val ref = db.collection("shopCategories").document(docId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(shopCategoryPayload(category, isUpdate = true)).await()
            } else {
                ref.set(shopCategoryPayload(category, isUpdate = false)).await()
            }
            writeAuditLog("SAVE_SHOP_CATEGORY", "SHOP_CATEGORY", docId, "Saved category '${category.displayName}' (enabled=${category.enabled})")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "shopCategories")
            Result.failure(e)
        }
    }

    suspend fun savePromotionalOffer(offer: PromotionalOffer): Result<Unit> {
        return try {
            val (adminUid, _) = requireAdminIdentity()
            val docId = offer.id.takeIf { it.isNotBlank() }
                ?: "promo_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
            val ref = db.collection("promotionalOffers").document(docId)
            val snap = ref.get().await()
            val startMs = offer.startTimeMillis.coerceAtLeast(0L)
            val endMs = maxOf(offer.endTimeMillis, startMs + 3_600_000L)
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "title" to offer.title.trim().take(80),
                        "description" to offer.description.trim().take(300),
                        "itemId" to offer.itemId.trim().take(128),
                        "categoryKey" to offer.categoryKey.trim().take(40),
                        "discountPercent" to offer.discountPercent.coerceIn(0L, 95L),
                        "promoPriceCoins" to offer.promoPriceCoins.coerceIn(0L, 1_000_000L),
                        "startTimeMillis" to startMs,
                        "endTimeMillis" to endMs,
                        "active" to offer.active,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "title" to offer.title.trim().take(80),
                        "description" to offer.description.trim().take(300),
                        "itemId" to offer.itemId.trim().take(128),
                        "categoryKey" to offer.categoryKey.trim().take(40),
                        "discountPercent" to offer.discountPercent.coerceIn(0L, 95L),
                        "promoPriceCoins" to offer.promoPriceCoins.coerceIn(0L, 1_000_000L),
                        "startTimeMillis" to startMs,
                        "endTimeMillis" to endMs,
                        "active" to offer.active,
                        "createdBy" to adminUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("SAVE_PROMO_OFFER", "PROMO_OFFER", docId, "Saved promo '${offer.title}' (${offer.discountPercent}% / ${offer.promoPriceCoins} Coins, active=${offer.active})")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "promotionalOffers")
            Result.failure(e)
        }
    }

    suspend fun deletePromotionalOffer(offerId: String): Result<Unit> {
        return try {
            requireAdminIdentity()
            db.collection("promotionalOffers").document(offerId).delete().await()
            writeAuditLog("DELETE_PROMO_OFFER", "PROMO_OFFER", offerId, "Deleted promotional offer $offerId")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "promotionalOffers/$offerId")
            Result.failure(e)
        }
    }

    suspend fun updateLevelConfig(levelNumber: Long, rewardCoins: Long, requiredLines: Long): Result<Unit> {
        return try {
            requireAdminIdentity()
            val lvl = levelNumber.coerceIn(1L, 100L)
            val docId = "level_$lvl"
            val ref = db.collection("levels").document(docId)
            val snap = ref.get().await()
            val completions = snap.getLong("completionsCount") ?: 0L
            ref.set(
                mapOf(
                    "levelNumber" to lvl,
                    "requiredLines" to requiredLines.coerceIn(1L, 10_000L),
                    "rewardCoins" to rewardCoins.coerceIn(0L, 50_000L),
                    "completionsCount" to completions,
                    "active" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("UPDATE_LEVEL", "LEVEL", docId, "Level $lvl: requiredLines=$requiredLines, rewardCoins=$rewardCoins")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "levels/level_$levelNumber")
            Result.failure(e)
        }
    }

    suspend fun saveGlobalSettings(settings: GlobalSettings): Result<Unit> {
        return try {
            requireAdminIdentity()
            db.collection("settings").document("global").set(
                mapOf(
                    "continueCostCoins" to settings.continueCostCoins.coerceIn(0L, 10_000L),
                    "exchangeRatePaisaPerCoin" to settings.exchangeRatePaisaPerCoin.coerceIn(1L, 1000L),
                    "minWithdrawalPaisa" to settings.minWithdrawalPaisa.coerceIn(10_000L, 10_000_000L),
                    "maxDailyWithdrawals" to settings.maxDailyWithdrawals.coerceIn(1L, 50L),
                    "allowEarnedCoinCashout" to settings.allowEarnedCoinCashout,
                    "jazzCashEnabled" to settings.jazzCashEnabled,
                    "easypaisaEnabled" to settings.easypaisaEnabled,
                    "bankTransferEnabled" to settings.bankTransferEnabled,
                    "jazzCashInstructions" to settings.jazzCashInstructions.trim().take(400),
                    "easypaisaInstructions" to settings.easypaisaInstructions.trim().take(400),
                    "bankTransferInstructions" to settings.bankTransferInstructions.trim().take(400),
                    "paymentSandboxMode" to settings.paymentSandboxMode,
                    "rewardedAdConfigured" to settings.rewardedAdConfigured,
                    "gameEnabled" to settings.gameEnabled,
                    "maintenanceMessage" to settings.maintenanceMessage.trim().take(300),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("UPDATE_SETTINGS", "SETTINGS", "global", "Updated global game, wallet, and compliance settings")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "settings/global")
            Result.failure(e)
        }
    }

    /**
     * SUPER ADMIN ONLY (enforced server-side by firestore.rules isSuperAdmin()):
     * Authorizes or updates another user's Admin account with scoped permissions.
     */
    suspend fun grantAdminAccess(
        targetUser: UserAccount,
        permissions: List<String>
    ): Result<Unit> {
        return try {
            val (superUid, _) = requireAdminIdentity()
            val ref = db.collection("admins").document(targetUser.userId)
            val snap = ref.get().await()
            if (snap.exists()) {
                ref.update(
                    mapOf(
                        "email" to targetUser.email.take(120),
                        "displayName" to targetUser.displayName.take(60),
                        "role" to "ADMIN",
                        "active" to true,
                        "permissions" to permissions.take(25),
                        "grantedBy" to superUid,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.set(
                    mapOf(
                        "userId" to targetUser.userId,
                        "email" to targetUser.email.take(120),
                        "displayName" to targetUser.displayName.take(60),
                        "role" to "ADMIN",
                        "active" to true,
                        "permissions" to permissions.take(25),
                        "grantedBy" to superUid,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("GRANT_ADMIN", "ADMIN", targetUser.userId, "Granted Admin to ${targetUser.email} with ${permissions.size} permissions")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "admins/${targetUser.userId}")
            Result.failure(e)
        }
    }

    /**
     * SUPER ADMIN ONLY (enforced server-side by firestore.rules isSuperAdmin()):
     * Immediately revokes an Admin's privileges.
     */
    suspend fun revokeAdminAccess(targetAdminUid: String, targetEmail: String): Result<Unit> {
        return try {
            requireAdminIdentity()
            db.collection("admins").document(targetAdminUid).delete().await()
            writeAuditLog("REVOKE_ADMIN", "ADMIN", targetAdminUid, "Revoked Admin access from $targetEmail")
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "admins/$targetAdminUid")
            Result.failure(e)
        }
    }
}
