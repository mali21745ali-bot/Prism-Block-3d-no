package com.example.data

import com.example.data.model.BoosterType
import com.example.data.model.DEFAULT_THEME_ID
import com.example.data.model.PlayerInventory
import com.example.data.model.PromotionalOffer
import com.example.data.model.PurchasedItem
import com.example.data.model.ShopCategory
import com.example.data.model.ShopCategoryKey
import com.example.data.model.ShopItem
import com.example.data.model.ShopTransaction
import com.example.data.model.UserAccount
import com.example.game.CosmeticCatalog
import com.example.game.PurchaseValidationResult
import com.example.game.ShopPricingEngine
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

data class ShopPurchaseReceipt(
    val item: ShopItem,
    val pricePaidCoins: Long,
    val promoApplied: Boolean,
    val quantityGranted: Long,
    val transactionId: String
)

class ShopRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUid(): String =
        auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Please sign in with Google to use the Coin Shop.")

    suspend fun ensureInventoryInitialized(uid: String): Result<Unit> {
        if (uid.isBlank()) return Result.success(Unit)
        val invRef = db.collection("playerInventory").document(uid)
        return try {
            val snap = invRef.get().await()
            if (!snap.exists()) {
                val userSnap = db.collection("users").document(uid).get().await()
                val user = userSnap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                val initialTheme = user?.selectedThemeId?.ifBlank { DEFAULT_THEME_ID } ?: DEFAULT_THEME_ID
                invRef.set(
                    mapOf(
                        "userId" to uid,
                        "ownedItemIds" to listOf("theme_simple_classic"),
                        "equippedThemeId" to initialTheme,
                        "equippedBlockSkinId" to "",
                        "equippedBackgroundId" to "",
                        "equippedEffectId" to "",
                        "equippedAvatarId" to "",
                        "equippedFrameId" to "",
                        "equippedBadgeId" to "",
                        "equippedExclusiveId" to "",
                        "hammerCount" to 0L,
                        "undoCount" to 0L,
                        "shuffleCount" to 0L,
                        "rowBlasterCount" to 0L,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "playerInventory/$uid")
            Result.failure(e)
        }
    }

    fun observeCategories(uid: String): Flow<List<ShopCategory>> = flow {
        if (uid.isBlank()) {
            emit(CosmeticCatalog.defaultShopCategories())
            return@flow
        }
        emitAll(
            db.collection("shopCategories")
                .whereEqualTo("enabled", true)
                .snapshots()
                .map { snap ->
                    val list = snap.toObjects(ShopCategory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                    list.ifEmpty { CosmeticCatalog.defaultShopCategories() }
                }
                .catch { err ->
                    logStreamError(err, uid, "shopCategories")
                    emit(CosmeticCatalog.defaultShopCategories())
                }
        )
    }

    fun observeAvailableItems(uid: String): Flow<List<ShopItem>> = flow {
        if (uid.isBlank()) {
            emit(CosmeticCatalog.defaultFullShopCatalog())
            return@flow
        }
        emitAll(
            db.collection("shopItems")
                .whereEqualTo("available", true)
                .snapshots()
                .map { snap ->
                    val items = snap.toObjects(ShopItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                    items.ifEmpty { CosmeticCatalog.defaultFullShopCatalog() }
                }
                .catch { err ->
                    logStreamError(err, uid, "shopItems")
                    emit(CosmeticCatalog.defaultFullShopCatalog())
                }
        )
    }

    fun observeActivePromotions(uid: String): Flow<List<PromotionalOffer>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("promotionalOffers")
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PromotionalOffer::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .catch { err ->
                    logStreamError(err, uid, "promotionalOffers")
                    emit(emptyList())
                }
        )
    }

    fun observePlayerInventory(uid: String): Flow<PlayerInventory> = flow {
        if (uid.isBlank()) {
            emit(PlayerInventory(userId = uid))
            return@flow
        }
        emitAll(
            db.collection("playerInventory").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(PlayerInventory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                            ?: PlayerInventory(userId = uid)
                    } else {
                        PlayerInventory(userId = uid)
                    }
                }
                .catch { err ->
                    logStreamError(err, uid, "playerInventory/$uid")
                    emit(PlayerInventory(userId = uid))
                }
        )
    }

    fun observePurchasedItems(uid: String): Flow<List<PurchasedItem>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("purchasedItems")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(PurchasedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.purchasedAt ?: Timestamp(0, 0) }
                }
                .catch { err ->
                    logStreamError(err, uid, "purchasedItems")
                    emit(emptyList())
                }
        )
    }

    fun observeShopTransactions(uid: String): Flow<List<ShopTransaction>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        emitAll(
            db.collection("shopTransactions")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ShopTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { err ->
                    logStreamError(err, uid, "shopTransactions")
                    emit(emptyList())
                }
        )
    }

    /**
     * Atomic transactional Coin Shop purchase:
     * - Reads LIVE ShopItem from Firestore (never trusts stale frontend price or disabled state)
     * - Evaluates active promotional pricing dynamically
     * - Blocks duplicate non-consumable purchases via deterministic purchasedItems doc ID
     * - Deducts Coins atomically from users/{uid} with non-negative enforcement
     * - Records permanent ownership in playerInventory/{uid}, purchasedItems, shopTransactions, walletTransactions
     * - Increments live sales & unique owner stats on shopItems/{itemId}
     */
    suspend fun purchaseItem(
        itemId: String,
        activePromotions: List<PromotionalOffer>,
        idempotencyKey: String = "buy_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
    ): Result<ShopPurchaseReceipt> {
        val uid = try { requireUid() } catch (e: Exception) { return Result.failure(e) }
        val cleanIdemp = idempotencyKey.replace(Regex("[^a-zA-Z0-9_\\-]"), "").take(64).ifBlank {
            "buy_${System.currentTimeMillis()}"
        }
        ensureInventoryInitialized(uid)

        val itemRef = db.collection("shopItems").document(itemId)
        val userRef = db.collection("users").document(uid)
        val invRef = db.collection("playerInventory").document(uid)
        val nonConsumableReceiptRef = db.collection("purchasedItems").document("${uid}_$itemId")
        val shopTxId = "stx_${uid}_$cleanIdemp"
        val shopTxRef = db.collection("shopTransactions").document(shopTxId)

        return try {
            val receipt = db.runTransaction { tx ->
                // 1. ALL READS FIRST
                val itemSnap = tx.get(itemRef)
                val userSnap = tx.get(userRef)
                val invSnap = tx.get(invRef)
                val existingReceiptSnap = tx.get(nonConsumableReceiptRef)

                if (!itemSnap.exists()) {
                    throw IllegalStateException("This shop item is not yet published in the live store. Ask Admin to sync the Coin Shop catalog.")
                }
                val liveItem = itemSnap.toObject(ShopItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Could not load shop item details.")
                val user = userSnap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player account not found.")
                if (user.blocked) throw IllegalStateException("Your account is restricted by Admin.")
                val inv = invSnap.toObject(PlayerInventory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Player inventory not initialized.")

                // 2. RESOLVE LIVE PRICE & VALIDATE
                val resolvedPrice = ShopPricingEngine.resolvePrice(
                    item = liveItem,
                    activePromotions = activePromotions,
                    nowMillis = System.currentTimeMillis()
                )
                val ownedSet = inv.ownedItemIds.toMutableSet()
                if (existingReceiptSnap.exists()) ownedSet.add(liveItem.id)

                when (val validation = ShopPricingEngine.validatePurchase(
                    item = liveItem,
                    resolvedPrice = resolvedPrice,
                    totalUserCoins = user.totalCoins,
                    ownedItemIds = ownedSet,
                    unlockedThemeIds = user.unlockedThemeIds.toSet()
                )) {
                    is PurchaseValidationResult.Denied -> throw IllegalStateException(validation.reason)
                    is PurchaseValidationResult.Allowed -> Unit
                }

                val split = ShopPricingEngine.computeCoinDeduction(
                    priceCoins = resolvedPrice.effectivePriceCoins,
                    earnedCoins = user.earnedCoins,
                    purchasedCoins = user.purchasedCoins
                ).getOrThrow()

                val isFirstOwnerPurchase = liveItem.id !in inv.ownedItemIds && !existingReceiptSnap.exists()
                val packQty = if (liveItem.consumable) liveItem.boosterPackQty.coerceIn(1L, 99L) else 1L

                // 3. ALL WRITES SECOND
                // 3a. Deduct Coins from users/{uid} (and sync theme unlocks for backward compatibility)
                val updatedThemes = user.unlockedThemeIds.toMutableList()
                var newSelectedTheme = user.selectedThemeId
                if (liveItem.category == "THEME" && liveItem.themeKey.isNotBlank()) {
                    if (liveItem.themeKey !in updatedThemes && updatedThemes.size < 60) {
                        updatedThemes.add(liveItem.themeKey)
                    }
                    newSelectedTheme = liveItem.themeKey
                }
                tx.update(
                    userRef,
                    mapOf(
                        "earnedCoins" to split.newEarnedCoins,
                        "purchasedCoins" to split.newPurchasedCoins,
                        "unlockedThemeIds" to updatedThemes,
                        "selectedThemeId" to newSelectedTheme,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 3b. Update playerInventory/{uid}
                val newOwned = inv.ownedItemIds.toMutableList()
                if (!liveItem.consumable && liveItem.id !in newOwned && newOwned.size < 250) {
                    newOwned.add(liveItem.id)
                }
                val boosterType = if (liveItem.category == "GAME_BOOSTER") {
                    BoosterType.fromWire(liveItem.subType) ?: BoosterType.fromWire(liveItem.resolvedAssetKey)
                } else null

                val invUpdate = mutableMapOf<String, Any>(
                    "ownedItemIds" to newOwned,
                    "hammerCount" to (inv.hammerCount + if (boosterType == BoosterType.HAMMER) packQty else 0L).coerceAtMost(9999L),
                    "undoCount" to (inv.undoCount + if (boosterType == BoosterType.UNDO) packQty else 0L).coerceAtMost(9999L),
                    "shuffleCount" to (inv.shuffleCount + if (boosterType == BoosterType.SHUFFLE) packQty else 0L).coerceAtMost(9999L),
                    "rowBlasterCount" to (inv.rowBlasterCount + if (boosterType == BoosterType.ROW_BLASTER) packQty else 0L).coerceAtMost(9999L),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (liveItem.category == "THEME" && liveItem.resolvedAssetKey.isNotBlank()) {
                    invUpdate["equippedThemeId"] = liveItem.resolvedAssetKey
                }
                tx.update(invRef, invUpdate)

                // 3c. Write immutable permanent ownership receipt
                val purchaseDocRef = if (liveItem.consumable) {
                    db.collection("purchasedItems").document("bst_${uid}_$cleanIdemp")
                } else {
                    nonConsumableReceiptRef
                }
                tx.set(
                    purchaseDocRef,
                    mapOf(
                        "userId" to uid,
                        "itemId" to liveItem.id,
                        "itemName" to liveItem.name.take(80),
                        "category" to liveItem.category,
                        "assetKey" to liveItem.resolvedAssetKey.take(60),
                        "pricePaidCoins" to resolvedPrice.effectivePriceCoins,
                        "originalPriceCoins" to resolvedPrice.basePriceCoins,
                        "promoApplied" to resolvedPrice.promoApplied,
                        "quantityPurchased" to packQty,
                        "consumable" to liveItem.consumable,
                        "idempotencyKey" to cleanIdemp,
                        "purchasedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 3d. Write immutable Shop Transaction ledger
                tx.set(
                    shopTxRef,
                    mapOf(
                        "userId" to uid,
                        "userDisplayName" to user.displayName.take(60).ifBlank { "Player" },
                        "itemId" to liveItem.id,
                        "itemName" to liveItem.name.take(80),
                        "category" to liveItem.category,
                        "pricePaidCoins" to resolvedPrice.effectivePriceCoins,
                        "originalPriceCoins" to resolvedPrice.basePriceCoins,
                        "coinSource" to split.coinSourceLabel,
                        "quantity" to packQty,
                        "promoOfferId" to resolvedPrice.promoOfferId.take(128),
                        "idempotencyKey" to cleanIdemp,
                        "status" to "COMPLETED",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                // 3e. Write immutable WalletTransaction ledger when Coins spent > 0
                if (resolvedPrice.effectivePriceCoins > 0L) {
                    val walletTxId = "shop_${uid.take(6)}_${cleanIdemp.takeLast(14)}"
                    val walletCoinSource = if (split.purchasedSpent > 0L && split.earnedSpent == 0L) "PURCHASED" else "EARNED"
                    tx.set(
                        db.collection("walletTransactions").document(walletTxId),
                        mapOf(
                            "userId" to uid,
                            "walletType" to "COIN",
                            "coinSource" to walletCoinSource,
                            "txType" to if (liveItem.category == "THEME") "THEME_PURCHASE" else "SHOP_PURCHASE",
                            "amountDelta" to -resolvedPrice.effectivePriceCoins,
                            "balanceAfter" to (split.newEarnedCoins + split.newPurchasedCoins),
                            "description" to "Coin Shop: ${liveItem.name}" + if (resolvedPrice.promoApplied) " (Promo)" else "",
                            "referenceId" to shopTxId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 3f. Atomically increment item sales & owner analytics counters
                val prevSales = itemSnap.getLong("totalSalesCount") ?: 0L
                val prevOwners = itemSnap.getLong("uniqueOwnersCount") ?: 0L
                tx.update(
                    itemRef,
                    mapOf(
                        "totalSalesCount" to (prevSales + 1L),
                        "uniqueOwnersCount" to (prevOwners + if (isFirstOwnerPurchase) 1L else 0L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                ShopPurchaseReceipt(
                    item = liveItem,
                    pricePaidCoins = resolvedPrice.effectivePriceCoins,
                    promoApplied = resolvedPrice.promoApplied,
                    quantityGranted = packQty,
                    transactionId = shopTxId
                )
            }.await()

            Result.success(receipt)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "shopItems/$itemId")
            Result.failure(e)
        }
    }

    /**
     * Equips or unequips a cosmetic item into the player's active slot in playerInventory/{uid}.
     * Also keeps users/{uid}.selectedThemeId synced when equipping a THEME.
     */
    suspend fun equipCosmetic(
        category: String,
        subType: String,
        assetKey: String,
        itemId: String,
        isFreeDefault: Boolean = false
    ): Result<Unit> {
        val uid = try { requireUid() } catch (e: Exception) { return Result.failure(e) }
        ensureInventoryInitialized(uid)

        val effectiveSlotCategory = if (category == "EXCLUSIVE" && subType.isNotBlank()) subType else category
        val fieldName = when (ShopCategoryKey.fromWire(effectiveSlotCategory)) {
            ShopCategoryKey.THEME -> "equippedThemeId"
            ShopCategoryKey.BLOCK_SKIN -> "equippedBlockSkinId"
            ShopCategoryKey.BOARD_BACKGROUND -> "equippedBackgroundId"
            ShopCategoryKey.SPECIAL_EFFECT -> "equippedEffectId"
            ShopCategoryKey.AVATAR -> "equippedAvatarId"
            ShopCategoryKey.PROFILE_FRAME -> "equippedFrameId"
            ShopCategoryKey.BADGE -> "equippedBadgeId"
            ShopCategoryKey.EXCLUSIVE -> "equippedExclusiveId"
            ShopCategoryKey.GAME_BOOSTER -> return Result.failure(IllegalArgumentException("Boosters are used directly during gameplay."))
        }

        val invRef = db.collection("playerInventory").document(uid)
        val userRef = db.collection("users").document(uid)
        val receiptRef = db.collection("purchasedItems").document("${uid}_$itemId")

        return try {
            db.runTransaction { tx ->
                val invSnap = tx.get(invRef)
                val userSnap = tx.get(userRef)
                val receiptSnap = if (itemId.isNotBlank()) tx.get(receiptRef) else null

                val inv = invSnap.toObject(PlayerInventory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Inventory not found.")
                val user = userSnap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User not found.")

                val isUnequip = assetKey.isBlank()
                if (!isUnequip && !isFreeDefault) {
                    val owns = itemId in inv.ownedItemIds ||
                        (receiptSnap?.exists() == true) ||
                        (effectiveSlotCategory == "THEME" && assetKey in user.unlockedThemeIds) ||
                        assetKey == DEFAULT_THEME_ID
                    if (!owns) throw IllegalStateException("Unlock this item in the Coin Shop before equipping it.")
                }

                val valueToWrite = if (fieldName == "equippedThemeId" && assetKey.isBlank()) DEFAULT_THEME_ID else assetKey
                val invUpdates = mutableMapOf<String, Any>(
                    fieldName to valueToWrite,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (category == "EXCLUSIVE") {
                    invUpdates["equippedExclusiveId"] = if (isUnequip) "" else itemId
                }
                tx.update(invRef, invUpdates)

                if (fieldName == "equippedThemeId" && valueToWrite in user.unlockedThemeIds) {
                    tx.update(
                        userRef,
                        mapOf(
                            "selectedThemeId" to valueToWrite,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "playerInventory/$uid")
            Result.failure(e)
        }
    }

    /**
     * Atomically decrements a player's consumable Game Booster count by 1 when used in gameplay.
     */
    suspend fun consumeBooster(type: BoosterType): Result<Long> {
        val uid = try { requireUid() } catch (e: Exception) { return Result.failure(e) }
        val invRef = db.collection("playerInventory").document(uid)
        val fieldName = when (type) {
            BoosterType.HAMMER -> "hammerCount"
            BoosterType.UNDO -> "undoCount"
            BoosterType.SHUFFLE -> "shuffleCount"
            BoosterType.ROW_BLASTER -> "rowBlasterCount"
        }
        return try {
            val remaining = db.runTransaction { tx ->
                val snap = tx.get(invRef)
                val inv = snap.toObject(PlayerInventory::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("No ${type.label} boosters owned. Buy them in the Coin Shop!")
                val current = inv.boosterCount(type)
                if (current <= 0L) {
                    throw IllegalStateException("Out of ${type.label} boosters! Buy a pack in the Coin Shop.")
                }
                val nextCount = current - 1L
                tx.update(
                    invRef,
                    mapOf(
                        fieldName to nextCount,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                nextCount
            }.await()
            Result.success(remaining)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "playerInventory/$uid")
            Result.failure(e)
        }
    }

    private fun logStreamError(err: Throwable, uid: String, path: String) {
        val fsErr = generateSequence(err) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (fsErr == null && err is CancellationException) throw err
        if (auth.currentUser?.uid == uid) {
            handleFirestoreError(fsErr ?: err, OperationType.LIST, path)
        }
    }
}
