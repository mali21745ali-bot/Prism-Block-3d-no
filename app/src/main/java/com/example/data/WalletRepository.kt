package com.example.data

import com.example.data.model.DepositRequest
import com.example.data.model.ExchangeRecord
import com.example.data.model.GlobalSettings
import com.example.data.model.MIN_WITHDRAWAL_PAISA
import com.example.data.model.PaymentMethod
import com.example.data.model.UserAccount
import com.example.data.model.WalletTransaction
import com.example.data.model.WithdrawalRequest
import com.example.data.model.formatPkr
import com.example.security.WithdrawalPinHasher
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

class WalletRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google.")
    }

    fun observeTransactions(uid: String): Flow<List<WalletTransaction>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "walletTransactions"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { err ->
                    logStreamError(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    fun observeDeposits(uid: String): Flow<List<DepositRequest>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "deposits"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(DepositRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { err ->
                    logStreamError(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    fun observeWithdrawals(uid: String): Flow<List<WithdrawalRequest>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "withdrawals"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(WithdrawalRequest::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { err ->
                    logStreamError(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    fun observeExchanges(uid: String): Flow<List<ExchangeRecord>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "exchanges"
        emitAll(
            db.collection(path)
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ExchangeRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                }
                .catch { err ->
                    logStreamError(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    /**
     * Exact integer-paisa exchange: PKR -> Purchased Coins (1 Coin = 5 paisa = 0.05 PKR by default).
     */
    suspend fun exchangePkrToCoins(coinsToBuy: Long): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (coinsToBuy <= 0L) return Result.failure(IllegalArgumentException("Enter a positive Coin amount."))
        val idempotencyKey = "ex_p2c_${uid.take(8)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val userRef = db.collection("users").document(uid)
        val settingsRef = db.collection("settings").document("global")

        return try {
            db.runTransaction { tx ->
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Profile not found.")
                if (user.blocked) throw IllegalStateException("Account restricted by Admin.")
                val settings = tx.get(settingsRef).toObject(GlobalSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: GlobalSettings()
                val rate = settings.exchangeRatePaisaPerCoin.coerceAtLeast(1L)
                val pkrCostPaisa = coinsToBuy * rate
                if (user.mainWalletPkrPaisa < pkrCostPaisa) {
                    throw IllegalStateException("Insufficient Main Wallet balance (need ${formatPkr(pkrCostPaisa)}).")
                }

                val newMainPaisa = user.mainWalletPkrPaisa - pkrCostPaisa
                val newPurchasedCoins = user.purchasedCoins + coinsToBuy

                tx.update(
                    userRef,
                    mapOf(
                        "mainWalletPkrPaisa" to newMainPaisa,
                        "purchasedCoins" to newPurchasedCoins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("exchanges").document(idempotencyKey),
                    mapOf(
                        "userId" to uid,
                        "direction" to "PKR_TO_COIN",
                        "coinsAmount" to coinsToBuy,
                        "pkrPaisaAmount" to pkrCostPaisa,
                        "ratePaisaPerCoin" to rate,
                        "coinSource" to "PURCHASED",
                        "idempotencyKey" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("walletTransactions").document("${idempotencyKey}_pkr"),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "MAIN_PKR",
                        "coinSource" to "NONE",
                        "txType" to "EXCHANGE_TO_COIN",
                        "amountDelta" to -pkrCostPaisa,
                        "balanceAfter" to newMainPaisa,
                        "description" to "Exchanged ${formatPkr(pkrCostPaisa)} for $coinsToBuy Purchased Coins",
                        "referenceId" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("walletTransactions").document("${idempotencyKey}_coin"),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "COIN",
                        "coinSource" to "PURCHASED",
                        "txType" to "EXCHANGE_TO_COIN",
                        "amountDelta" to coinsToBuy,
                        "balanceAfter" to (newPurchasedCoins + user.earnedCoins),
                        "description" to "Received $coinsToBuy Purchased Coins from Main Wallet",
                        "referenceId" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "exchanges/$idempotencyKey")
            Result.failure(e)
        }
    }

    /**
     * Exact integer-paisa exchange: Eligible Coins -> PKR.
     * Enforces Section 8 Financial Rule: Tracks Purchased Coins vs Promotional/Earned Coins separately;
     * Earned Coins cannot convert to PKR unless Super Admin enables allowEarnedCoinCashout.
     */
    suspend fun exchangeCoinsToPkr(coinsToConvert: Long): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (coinsToConvert <= 0L) return Result.failure(IllegalArgumentException("Enter a positive Coin amount."))
        val idempotencyKey = "ex_c2p_${uid.take(8)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val userRef = db.collection("users").document(uid)
        val settingsRef = db.collection("settings").document("global")

        return try {
            db.runTransaction { tx ->
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Profile not found.")
                if (user.blocked) throw IllegalStateException("Account restricted by Admin.")
                val settings = tx.get(settingsRef).toObject(GlobalSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: GlobalSettings()
                val rate = settings.exchangeRatePaisaPerCoin.coerceAtLeast(1L)
                val allowEarned = settings.allowEarnedCoinCashout

                val eligibleCoins = if (allowEarned) user.purchasedCoins + user.earnedCoins else user.purchasedCoins
                if (eligibleCoins < coinsToConvert) {
                    val reason = if (!allowEarned) {
                        "Only Purchased Coins (${user.purchasedCoins}) are cash-convertible under current compliance settings. Promotional/Earned Coins (${user.earnedCoins}) can be spent in the Coin Shop, Themes, and Continues."
                    } else {
                        "Insufficient Coin balance (have $eligibleCoins Coins)."
                    }
                    throw IllegalStateException(reason)
                }

                val deductPurchased = minOf(user.purchasedCoins, coinsToConvert)
                val deductEarned = coinsToConvert - deductPurchased
                val pkrGainPaisa = coinsToConvert * rate
                val newPurchased = user.purchasedCoins - deductPurchased
                val newEarned = user.earnedCoins - deductEarned
                val newMainPaisa = user.mainWalletPkrPaisa + pkrGainPaisa

                tx.update(
                    userRef,
                    mapOf(
                        "mainWalletPkrPaisa" to newMainPaisa,
                        "purchasedCoins" to newPurchased,
                        "earnedCoins" to newEarned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val sourceTag = if (deductEarned > 0L) "EARNED" else "PURCHASED"
                tx.set(
                    db.collection("exchanges").document(idempotencyKey),
                    mapOf(
                        "userId" to uid,
                        "direction" to "COIN_TO_PKR",
                        "coinsAmount" to coinsToConvert,
                        "pkrPaisaAmount" to pkrGainPaisa,
                        "ratePaisaPerCoin" to rate,
                        "coinSource" to sourceTag,
                        "idempotencyKey" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("walletTransactions").document("${idempotencyKey}_coin"),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "COIN",
                        "coinSource" to sourceTag,
                        "txType" to "EXCHANGE_TO_PKR",
                        "amountDelta" to -coinsToConvert,
                        "balanceAfter" to (newPurchased + newEarned),
                        "description" to "Converted $coinsToConvert Coins to ${formatPkr(pkrGainPaisa)}",
                        "referenceId" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("walletTransactions").document("${idempotencyKey}_pkr"),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "MAIN_PKR",
                        "coinSource" to "NONE",
                        "txType" to "EXCHANGE_TO_PKR",
                        "amountDelta" to pkrGainPaisa,
                        "balanceAfter" to newMainPaisa,
                        "description" to "Received ${formatPkr(pkrGainPaisa)} from Coin Exchange",
                        "referenceId" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "exchanges/$idempotencyKey")
            Result.failure(e)
        }
    }

    /**
     * Submits a PENDING Deposit Request (no password required per Section 10).
     * Never credits Main Wallet automatically — requires Admin verification & approval.
     */
    suspend fun submitDepositRequest(
        user: UserAccount,
        amountPkrRupees: Long,
        paymentMethod: PaymentMethod,
        transactionRef: String,
        senderAccount: String,
        sandboxMode: Boolean
    ): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (user.blocked) return Result.failure(IllegalStateException("Account restricted by Admin."))
        val amountPaisa = amountPkrRupees * 100L
        if (amountPaisa < 10_000L) return Result.failure(IllegalArgumentException("Minimum deposit request is PKR 100."))
        val cleanRef = transactionRef.trim().take(100)
        val cleanSender = senderAccount.trim().take(60)
        if (cleanRef.length < 4) return Result.failure(IllegalArgumentException("Enter a valid payment reference / Transaction ID (min 4 characters)."))
        if (cleanSender.length < 4) return Result.failure(IllegalArgumentException("Enter your sender account number / IBAN."))

        val idempotencyKey = "dep_${uid.take(8)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val path = "deposits/$idempotencyKey"
        return try {
            db.collection("deposits").document(idempotencyKey).set(
                mapOf(
                    "userId" to uid,
                    "userEmail" to user.email.take(120),
                    "userName" to user.displayName.take(60),
                    "amountPaisa" to amountPaisa,
                    "paymentMethod" to paymentMethod.wireName,
                    "transactionRef" to cleanRef,
                    "senderAccount" to cleanSender,
                    "status" to "PENDING",
                    "adminNote" to "",
                    "adminPaymentRef" to "",
                    "reviewedBy" to "",
                    "idempotencyKey" to idempotencyKey,
                    "sandboxMode" to sandboxMode,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(idempotencyKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Submits a password-verified Withdrawal Request (minimum PKR 500).
     * Verifies the user's withdrawal password every time and atomically places funds in escrow hold.
     */
    suspend fun submitWithdrawalRequest(
        user: UserAccount,
        amountPkrRupees: Long,
        paymentMethod: PaymentMethod,
        accountTitle: String,
        accountNumber: String,
        enteredPassword: String,
        settings: GlobalSettings
    ): Result<String> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (user.blocked) return Result.failure(IllegalStateException("Account restricted by Admin."))
        if (!user.hasWithdrawalPin) {
            return Result.failure(IllegalStateException("Set your Withdrawal Security Password first before requesting a withdrawal."))
        }
        if (!WithdrawalPinHasher.verifyPassword(enteredPassword, user.withdrawalPinSalt, user.withdrawalPinHash)) {
            return Result.failure(IllegalArgumentException("Incorrect withdrawal password. Every withdrawal requires password verification."))
        }

        val amountPaisa = amountPkrRupees * 100L
        val minPaisa = maxOf(MIN_WITHDRAWAL_PAISA, settings.minWithdrawalPaisa)
        if (amountPaisa < minPaisa) {
            return Result.failure(IllegalArgumentException("Minimum withdrawal is ${formatPkr(minPaisa)}."))
        }
        val cleanTitle = accountTitle.trim().take(100)
        val cleanAccount = accountNumber.trim().take(60)
        if (cleanTitle.length < 2) return Result.failure(IllegalArgumentException("Enter recipient account title."))
        if (cleanAccount.length < 4) return Result.failure(IllegalArgumentException("Enter recipient account number / IBAN."))

        val idempotencyKey = "wd_${uid.take(8)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val userRef = db.collection("users").document(uid)

        return try {
            db.runTransaction { tx ->
                val freshUser = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Profile not found.")
                if (freshUser.blocked) throw IllegalStateException("Account restricted by Admin.")
                if (freshUser.mainWalletPkrPaisa < amountPaisa) {
                    throw IllegalStateException("Insufficient Main Wallet balance (available: ${formatPkr(freshUser.mainWalletPkrPaisa)}).")
                }

                val newMainPaisa = freshUser.mainWalletPkrPaisa - amountPaisa
                tx.update(
                    userRef,
                    mapOf(
                        "mainWalletPkrPaisa" to newMainPaisa,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("withdrawals").document(idempotencyKey),
                    mapOf(
                        "userId" to uid,
                        "userEmail" to freshUser.email.take(120),
                        "userName" to freshUser.displayName.take(60),
                        "amountPaisa" to amountPaisa,
                        "paymentMethod" to paymentMethod.wireName,
                        "accountTitle" to cleanTitle,
                        "accountNumber" to cleanAccount,
                        "status" to "PENDING",
                        "adminNote" to "",
                        "payoutReference" to "",
                        "reviewedBy" to "",
                        "idempotencyKey" to idempotencyKey,
                        "sandboxMode" to settings.paymentSandboxMode,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                tx.set(
                    db.collection("walletTransactions").document("${idempotencyKey}_hold"),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "MAIN_PKR",
                        "coinSource" to "NONE",
                        "txType" to "WITHDRAWAL_HOLD",
                        "amountDelta" to -amountPaisa,
                        "balanceAfter" to newMainPaisa,
                        "description" to "Withdrawal request hold (${paymentMethod.displayName} • $cleanAccount)",
                        "referenceId" to idempotencyKey,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(idempotencyKey)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "withdrawals/$idempotencyKey")
            Result.failure(e)
        }
    }

    private fun logStreamError(
        error: Throwable,
        expectedUid: String,
        op: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val ex = firestoreError ?: (error as? Exception)
        if (ex != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(ex, op, path)
        }
    }
}
