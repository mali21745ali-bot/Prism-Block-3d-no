package com.example.data

import com.example.data.model.AdminRecord
import com.example.data.model.ChallengeParticipant
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DEFAULT_THEME_ID
import com.example.data.model.DailyChallenge
import com.example.data.model.DailyChallengeClaim
import com.example.data.model.DailyRewardConfig
import com.example.data.model.GameMode
import com.example.data.model.GlobalSettings
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LevelConfig
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.data.model.ShopItem
import com.example.data.model.TimeChallenge
import com.example.data.model.UserAccount
import com.example.game.BlockThemes
import com.example.game.GameProgressCalculator
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.UUID

data class SessionCommitResult(
    val score: Long,
    val linesCleared: Long,
    val totalCoinsAwarded: Long,
    val levelCompleted: Boolean,
    val newLevelNumber: Long,
    val newLevelLines: Long = 0L,
    val dailyBonusAwarded: Long,
    val challengeBonusAwarded: Long
)

data class LiveProgressSyncResult(
    val deltaLinesSaved: Long,
    val levelCompleted: Boolean,
    val newLevelNumber: Long,
    val newLevelLines: Long,
    val completedLevelsCount: Long,
    val coinsAdded: Long
)

class GameRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun todayUtcDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    suspend fun ensureUserInitialized(firebaseUser: FirebaseUser): Result<Unit> {
        val uid = firebaseUser.uid
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = (firebaseUser.displayName?.trim()?.take(50))
                    ?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.email?.substringBefore("@")?.take(30)
                    ?: "Prism Player"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to (firebaseUser.email ?: ""),
                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "blocked" to false,
                    "selectedThemeId" to DEFAULT_THEME_ID,
                    "unlockedThemeIds" to listOf(DEFAULT_THEME_ID),
                    "classicHighScore" to 0L,
                    "totalLinesCleared" to 0L,
                    "totalGamesPlayed" to 0L,
                    "currentLevel" to 1L,
                    "currentLevelLines" to 0L,
                    "completedLevelsCount" to 0L,
                    "dailyRewardNextDay" to 1L,
                    "dailyRewardLastClaimDate" to "",
                    "dailyChallengeLastDate" to "",
                    "dailyChallengeBestScore" to 0L,
                    "mainWalletPkrPaisa" to 0L,
                    "purchasedCoins" to 0L,
                    "earnedCoins" to 0L,
                    "walletVerificationName" to "",
                    "walletVerificationProvider" to "",
                    "walletVerificationAccount" to "",
                    "walletVerificationCompleted" to false,
                    "withdrawalPinHash" to "",
                    "withdrawalPinSalt" to "",
                    "musicEnabled" to true,
                    "sfxEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    fun observeUser(uid: String): Flow<UserAccount?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.GET, path)
                    throw err
                }
        )
    }

    fun observeAdminRecord(uid: String): Flow<AdminRecord?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val path = "admins/$uid"
        emitAll(
            db.collection("admins").document(uid)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(AdminRecord::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.GET, path)
                    emit(null)
                }
        )
    }

    fun observeGlobalSettings(uid: String): Flow<GlobalSettings> = flow {
        if (uid.isBlank()) {
            emit(GlobalSettings())
            return@flow
        }
        val path = "settings/global"
        emitAll(
            db.collection("settings").document("global")
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(GlobalSettings::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: GlobalSettings()
                    } else {
                        GlobalSettings()
                    }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.GET, path)
                    emit(GlobalSettings())
                }
        )
    }

    fun observeShopItems(uid: String): Flow<List<ShopItem>> = flow {
        if (uid.isBlank()) {
            emit(BlockThemes.defaultShopCatalog())
            return@flow
        }
        val path = "shopItems"
        emitAll(
            db.collection(path)
                .whereEqualTo("available", true)
                .snapshots()
                .map { snap ->
                    val items = snap.toObjects(ShopItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.sortOrder }
                    items.ifEmpty { BlockThemes.defaultShopCatalog() }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    emit(BlockThemes.defaultShopCatalog())
                }
        )
    }

    fun observeDailyRewards(uid: String): Flow<List<DailyRewardConfig>> = flow {
        if (uid.isBlank()) {
            emit(BlockThemes.defaultDailyRewards())
            return@flow
        }
        val path = "dailyRewards"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    val remote = snap.toObjects(DailyRewardConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val byDay = remote.associateBy { it.dayNumber }
                    BlockThemes.defaultDailyRewards().map { fallback -> byDay[fallback.dayNumber] ?: fallback }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    emit(BlockThemes.defaultDailyRewards())
                }
        )
    }

    fun observeLevelConfigs(uid: String): Flow<Map<Long, LevelConfig>> = flow {
        if (uid.isBlank()) {
            emit(emptyMap())
            return@flow
        }
        val path = "levels"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LevelConfig::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .associateBy { it.levelNumber }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    emit(emptyMap())
                }
        )
    }

    fun observeActiveTimeChallenges(uid: String): Flow<List<TimeChallenge>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "timeChallenges"
        emitAll(
            db.collection(path)
                .whereEqualTo("active", true)
                .snapshots()
                .map { snap ->
                    snap.toObjects(TimeChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.startTimeMillis }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    fun observeTodayDailyChallenge(uid: String): Flow<DailyChallenge> = flow {
        val dateKey = todayUtcDateKey()
        val defaultDaily = DailyChallenge(
            id = dateKey,
            dateKey = dateKey,
            title = "Daily Prism Sprint ($dateKey)",
            description = "Clear 15 lines in today's shared puzzle sequence to earn a one-time bonus of 40 Coins.",
            seed = dateKey.replace("-", "").toLongOrNull() ?: 20261117L,
            targetLines = 15L,
            bonusCoins = 40L,
            active = true
        )
        if (uid.isBlank()) {
            emit(defaultDaily)
            return@flow
        }
        val path = "dailyChallenges/$dateKey"
        emitAll(
            db.collection("dailyChallenges").document(dateKey)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(DailyChallenge::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) ?: defaultDaily
                    } else defaultDaily
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.GET, path)
                    emit(defaultDaily)
                }
        )
    }

    fun observeDailyChallengeClaim(uid: String, dateKey: String): Flow<DailyChallengeClaim?> = flow {
        if (uid.isBlank()) {
            emit(null)
            return@flow
        }
        val docId = "${dateKey}_$uid"
        val path = "dailyChallengeClaims/$docId"
        emitAll(
            db.collection("dailyChallengeClaims").document(docId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) snap.toObject(DailyChallengeClaim::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    else null
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.GET, path)
                    emit(null)
                }
        )
    }

    fun observeDailyLeaderboard(uid: String, dateKey: String): Flow<List<LeaderboardEntry>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "leaderboards"
        emitAll(
            db.collection(path)
                .whereEqualTo("boardType", "DAILY")
                .whereEqualTo("dateKey", dateKey)
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.bestScore }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    fun observeAllTimeLeaderboard(uid: String): Flow<List<LeaderboardEntry>> = flow {
        if (uid.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "leaderboards"
        emitAll(
            db.collection(path)
                .whereEqualTo("boardType", "ALL_TIME")
                .snapshots()
                .map { snap ->
                    snap.toObjects(LeaderboardEntry::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.bestScore }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    throw err
                }
        )
    }

    fun observeChallengeParticipants(uid: String, challengeId: String): Flow<List<ChallengeParticipant>> = flow {
        if (uid.isBlank() || challengeId.isBlank()) {
            emit(emptyList())
            return@flow
        }
        val path = "challengeParticipants"
        emitAll(
            db.collection(path)
                .whereEqualTo("publicEntry", true)
                .whereEqualTo("challengeId", challengeId)
                .snapshots()
                .map { snap ->
                    snap.toObjects(ChallengeParticipant::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.bestScore }
                }
                .catch { err ->
                    logStreamErrorIfAuthenticated(err, uid, OperationType.LIST, path)
                    emit(emptyList())
                }
        )
    }

    /**
     * Atomically saves incremental line-clearing progress to Firestore mid-session:
     * - Advances permanent Level Progress (1,000 lines per level across 100 levels)
     * - Unlocks next level & awards level-completion bonus coins when 1,000 lines are reached
     * - Credits exact 1 Coin per cleared line and updates Classic high score if applicable
     * - Does NOT increment totalGamesPlayed or create premature gameSessions records
     */
    suspend fun saveLiveProgressDelta(
        mode: GameMode,
        deltaLines: Long,
        sessionTotalLines: Long,
        levelRewardCoins: Long = 100L
    ): Result<LiveProgressSyncResult> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val safeDelta = deltaLines.coerceIn(0L, 1000L)
        if (safeDelta == 0L) {
            return Result.success(
                LiveProgressSyncResult(
                    deltaLinesSaved = 0L,
                    levelCompleted = false,
                    newLevelNumber = 1L,
                    newLevelLines = 0L,
                    completedLevelsCount = 0L,
                    coinsAdded = 0L
                )
            )
        }
        val sessionTotalScore = maxOf(sessionTotalLines, safeDelta).coerceIn(0L, 10000L) * 10L
        val path = "users/$uid"

        return try {
            val result = db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User profile not initialized.")
                if (user.blocked) throw IllegalStateException("Your account is restricted by Admin.")

                val advancement = GameProgressCalculator.advancePermanentLevel(
                    currentLevel = user.currentLevel,
                    currentLevelLines = user.currentLevelLines,
                    completedLevelsCount = user.completedLevelsCount,
                    addedLines = safeDelta,
                    requiredLinesPerLevel = DEFAULT_LINES_PER_LEVEL,
                    maxLevel = MAX_LEVEL_NUMBER.toLong()
                )
                val levelBonus = if (advancement.levelsCompletedNow > 0L) {
                    (levelRewardCoins.coerceIn(0L, 500L) * advancement.levelsCompletedNow).coerceAtMost(500L)
                } else 0L

                val totalCoinsToAdd = (safeDelta + levelBonus).coerceAtMost(1500L)
                val newEarnedCoins = user.earnedCoins + totalCoinsToAdd
                val newTotalLines = user.totalLinesCleared + safeDelta
                val newClassicHigh = if (mode == GameMode.CLASSIC) {
                    maxOf(user.classicHighScore, sessionTotalScore)
                } else user.classicHighScore

                tx.update(
                    userRef,
                    mapOf(
                        "classicHighScore" to newClassicHigh,
                        "totalLinesCleared" to newTotalLines,
                        "currentLevel" to advancement.newLevel,
                        "currentLevelLines" to advancement.newLevelLines,
                        "completedLevelsCount" to advancement.newCompletedLevelsCount,
                        "earnedCoins" to newEarnedCoins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                if (advancement.levelsCompletedNow > 0L && levelBonus > 0L) {
                    val completedLvl = advancement.completedLevelNumber ?: user.currentLevel
                    val txId = "tx_lvl_${completedLvl}_${uid.take(6)}_${System.currentTimeMillis().toString().takeLast(6)}"
                    tx.set(
                        db.collection("walletTransactions").document(txId),
                        mapOf(
                            "userId" to uid,
                            "walletType" to "COIN",
                            "coinSource" to "EARNED",
                            "txType" to "LEVEL_REWARD",
                            "amountDelta" to levelBonus,
                            "balanceAfter" to (newEarnedCoins + user.purchasedCoins),
                            "description" to "Level $completedLvl Complete! (+$levelBonus Bonus Coins)",
                            "referenceId" to "level_$completedLvl",
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                LiveProgressSyncResult(
                    deltaLinesSaved = safeDelta,
                    levelCompleted = advancement.levelsCompletedNow > 0L,
                    newLevelNumber = advancement.newLevel,
                    newLevelLines = advancement.newLevelLines,
                    completedLevelsCount = advancement.newCompletedLevelsCount,
                    coinsAdded = totalCoinsToAdd
                )
            }.await()
            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    /**
     * Atomically commits a finished or mid-session gameplay batch to Firestore:
     * - Enforces exact 1 line = 10 Score + 1 Coin
     * - Updates permanent Level progress (1,000 lines per level across 100 levels) for uncommitted lines only
     * - Prevents duplicate Daily Challenge bonus claims via deterministic doc ID
     * - Updates Daily & All-Time Leaderboards (zero PII)
     * - Records Time Challenge participation & prevents duplicate target reward claims
     */
    suspend fun commitGameSession(
        mode: GameMode,
        linesCleared: Long,
        levelNumber: Long = 0L,
        activeTimeChallenge: TimeChallenge? = null,
        dailyChallenge: DailyChallenge? = null,
        levelRewardCoins: Long = 100L,
        continuesUsed: Long = 0L,
        sessionTotalLines: Long = linesCleared
    ): Result<SessionCommitResult> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val dateKey = todayUtcDateKey()
        val sessionId = "ses_${uid.take(8)}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(4)}"
        val safeDeltaLines = linesCleared.coerceIn(0L, 1000L)
        val safeSessionLines = maxOf(sessionTotalLines, safeDeltaLines).coerceIn(0L, 10000L)
        val exactSessionScore = safeSessionLines * 10L
        val baseDeltaCoins = safeDeltaLines

        return try {
            val result = db.runTransaction { tx ->
                val userRef = db.collection("users").document(uid)
                val userSnap = tx.get(userRef)
                val user = userSnap.toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("User profile not initialized.")
                if (user.blocked) throw IllegalStateException("Your account is restricted by Admin.")

                val dailyLbRef = db.collection("leaderboards").document("daily_${dateKey}_$uid")
                val allTimeLbRef = db.collection("leaderboards").document("alltime_$uid")
                val dailyLbSnap = tx.get(dailyLbRef)
                val allTimeLbSnap = tx.get(allTimeLbRef)

                val dailyClaimRef = if (mode == GameMode.DAILY && dailyChallenge != null) {
                    db.collection("dailyChallengeClaims").document("${dailyChallenge.dateKey}_$uid")
                } else null
                val dailyClaimSnap = dailyClaimRef?.let { tx.get(it) }

                val participantRef = if (mode == GameMode.TIME && activeTimeChallenge != null) {
                    db.collection("challengeParticipants").document("${activeTimeChallenge.id}_$uid")
                } else null
                val participantSnap = participantRef?.let { tx.get(it) }

                var extraCoins = 0L
                val advancement = GameProgressCalculator.advancePermanentLevel(
                    currentLevel = user.currentLevel,
                    currentLevelLines = user.currentLevelLines,
                    completedLevelsCount = user.completedLevelsCount,
                    addedLines = safeDeltaLines,
                    requiredLinesPerLevel = DEFAULT_LINES_PER_LEVEL,
                    maxLevel = MAX_LEVEL_NUMBER.toLong()
                )
                val levelCompleted = advancement.levelsCompletedNow > 0L
                val nextLevel = advancement.newLevel
                val nextLevelLines = advancement.newLevelLines
                val nextCompletedLevels = advancement.newCompletedLevelsCount
                if (levelCompleted) {
                    extraCoins += (levelRewardCoins.coerceIn(0L, 500L) * advancement.levelsCompletedNow).coerceAtMost(500L)
                }

                var dailyBonusAwarded = 0L
                if (mode == GameMode.DAILY && dailyChallenge != null && dailyClaimRef != null) {
                    val alreadyClaimed = dailyClaimSnap?.exists() == true
                    if (!alreadyClaimed && safeSessionLines >= dailyChallenge.targetLines) {
                        dailyBonusAwarded = dailyChallenge.bonusCoins.coerceAtMost(500L)
                        extraCoins += dailyBonusAwarded
                    }
                }

                var challengeBonusAwarded = 0L
                var targetNowClaimed = participantSnap?.getBoolean("targetRewardClaimed") ?: false
                if (mode == GameMode.TIME && activeTimeChallenge != null && participantRef != null) {
                    if (activeTimeChallenge.rewardType == "TARGET" && !targetNowClaimed && exactSessionScore >= activeTimeChallenge.targetScore) {
                        challengeBonusAwarded = activeTimeChallenge.rewardCoins.coerceAtMost(500L)
                        extraCoins += challengeBonusAwarded
                        targetNowClaimed = true
                    }
                }

                val totalCoinsToAdd = (baseDeltaCoins + extraCoins).coerceAtMost(1500L)
                val newEarnedCoins = user.earnedCoins + totalCoinsToAdd
                val newClassicHigh = if (mode == GameMode.CLASSIC) maxOf(user.classicHighScore, exactSessionScore) else user.classicHighScore
                val newDailyBest = if (mode == GameMode.DAILY) {
                    if (user.dailyChallengeLastDate == dateKey) maxOf(user.dailyChallengeBestScore, exactSessionScore) else exactSessionScore
                } else user.dailyChallengeBestScore
                val newDailyDate = if (mode == GameMode.DAILY) dateKey else user.dailyChallengeLastDate
                val newTotalLines = user.totalLinesCleared + safeDeltaLines
                val newTotalGames = user.totalGamesPlayed + 1L

                // 1. Update user document
                tx.update(
                    userRef,
                    mapOf(
                        "classicHighScore" to newClassicHigh,
                        "totalLinesCleared" to newTotalLines,
                        "totalGamesPlayed" to newTotalGames,
                        "currentLevel" to nextLevel,
                        "currentLevelLines" to nextLevelLines,
                        "completedLevelsCount" to nextCompletedLevels,
                        "dailyChallengeLastDate" to newDailyDate,
                        "dailyChallengeBestScore" to newDailyBest,
                        "earnedCoins" to newEarnedCoins,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                // 2. Write immutable GameSession (reflects full session score, lines & earned coins)
                tx.set(
                    db.collection("gameSessions").document(sessionId),
                    mapOf(
                        "userId" to uid,
                        "mode" to mode.wireName,
                        "score" to exactSessionScore,
                        "linesCleared" to safeSessionLines,
                        "coinsEarned" to safeSessionLines,
                        "levelNumber" to (if (mode == GameMode.LEVEL) levelNumber.coerceIn(1L, 100L) else nextLevel.coerceIn(1L, 100L)),
                        "challengeId" to (activeTimeChallenge?.id ?: if (mode == GameMode.DAILY) dateKey else ""),
                        "continuesUsed" to continuesUsed.coerceIn(0L, 50L),
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )

                // 3. Write consolidated WalletTransaction for session rewards
                val txDisplayCoins = (safeSessionLines + extraCoins).coerceAtMost(1500L)
                if (txDisplayCoins > 0L) {
                    val txId = "tx_game_${sessionId.takeLast(14)}"
                    tx.set(
                        db.collection("walletTransactions").document(txId),
                        mapOf(
                            "userId" to uid,
                            "walletType" to "COIN",
                            "coinSource" to "EARNED",
                            "txType" to when {
                                levelCompleted -> "LEVEL_REWARD"
                                challengeBonusAwarded > 0L -> "CHALLENGE_REWARD"
                                else -> "GAME_REWARD"
                            },
                            "amountDelta" to txDisplayCoins,
                            "balanceAfter" to (newEarnedCoins + user.purchasedCoins),
                            "description" to "${mode.label}: $safeSessionLines lines cleared (+$txDisplayCoins Coins)",
                            "referenceId" to sessionId,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 4. Write DailyChallengeClaim receipt if bonus awarded
                if (dailyBonusAwarded > 0L && dailyClaimRef != null && dailyChallenge != null) {
                    tx.set(
                        dailyClaimRef,
                        mapOf(
                            "userId" to uid,
                            "dateKey" to dailyChallenge.dateKey,
                            "score" to exactSessionScore,
                            "linesCleared" to safeSessionLines,
                            "bonusCoinsClaimed" to dailyBonusAwarded,
                            "createdAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                // 5. Upsert Time Challenge participant score
                if (mode == GameMode.TIME && activeTimeChallenge != null && participantRef != null) {
                    if (participantSnap?.exists() == true) {
                        val prevBest = participantSnap.getLong("bestScore") ?: 0L
                        val prevAwarded = participantSnap.getLong("rewardCoinsAwarded") ?: 0L
                        tx.update(
                            participantRef,
                            mapOf(
                                "displayName" to user.displayName,
                                "bestScore" to maxOf(prevBest, exactSessionScore),
                                "linesCleared" to maxOf(participantSnap.getLong("linesCleared") ?: 0L, safeSessionLines),
                                "targetRewardClaimed" to targetNowClaimed,
                                "rewardCoinsAwarded" to (prevAwarded + challengeBonusAwarded),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    } else {
                        tx.set(
                            participantRef,
                            mapOf(
                                "challengeId" to activeTimeChallenge.id,
                                "userId" to uid,
                                "displayName" to user.displayName,
                                "bestScore" to exactSessionScore,
                                "linesCleared" to safeSessionLines,
                                "targetRewardClaimed" to targetNowClaimed,
                                "rewardCoinsAwarded" to challengeBonusAwarded,
                                "publicEntry" to true,
                                "createdAt" to FieldValue.serverTimestamp(),
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                    }
                }

                // 6. Upsert Daily & All-Time Leaderboards (zero PII)
                if (dailyLbSnap.exists()) {
                    val prevBest = dailyLbSnap.getLong("bestScore") ?: 0L
                    tx.update(
                        dailyLbRef,
                        mapOf(
                            "displayName" to user.displayName,
                            "photoUrl" to user.photoUrl,
                            "bestScore" to maxOf(prevBest, exactSessionScore),
                            "totalLines" to ((dailyLbSnap.getLong("totalLines") ?: 0L) + safeSessionLines),
                            "highestLevel" to nextLevel,
                            "gamesPlayed" to ((dailyLbSnap.getLong("gamesPlayed") ?: 0L) + 1L),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        dailyLbRef,
                        mapOf(
                            "userId" to uid,
                            "displayName" to user.displayName,
                            "photoUrl" to user.photoUrl,
                            "boardType" to "DAILY",
                            "dateKey" to dateKey,
                            "bestScore" to exactSessionScore,
                            "totalLines" to safeSessionLines,
                            "highestLevel" to nextLevel,
                            "gamesPlayed" to 1L,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                if (allTimeLbSnap.exists()) {
                    val prevBest = allTimeLbSnap.getLong("bestScore") ?: 0L
                    tx.update(
                        allTimeLbRef,
                        mapOf(
                            "displayName" to user.displayName,
                            "photoUrl" to user.photoUrl,
                            "bestScore" to maxOf(prevBest, exactSessionScore),
                            "totalLines" to newTotalLines,
                            "highestLevel" to nextLevel,
                            "gamesPlayed" to newTotalGames,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                } else {
                    tx.set(
                        allTimeLbRef,
                        mapOf(
                            "userId" to uid,
                            "displayName" to user.displayName,
                            "photoUrl" to user.photoUrl,
                            "boardType" to "ALL_TIME",
                            "dateKey" to "ALL_TIME",
                            "bestScore" to exactSessionScore,
                            "totalLines" to newTotalLines,
                            "highestLevel" to nextLevel,
                            "gamesPlayed" to newTotalGames,
                            "createdAt" to FieldValue.serverTimestamp(),
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }

                SessionCommitResult(
                    score = exactSessionScore,
                    linesCleared = safeSessionLines,
                    totalCoinsAwarded = (safeSessionLines + extraCoins).coerceAtMost(1500L),
                    levelCompleted = levelCompleted,
                    newLevelNumber = nextLevel,
                    newLevelLines = nextLevelLines,
                    dailyBonusAwarded = dailyBonusAwarded,
                    challengeBonusAwarded = challengeBonusAwarded
                )
            }.await()

            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "gameSessions/$sessionId")
            Result.failure(e)
        }
    }

    /**
     * Claims today's Daily Login Reward.
     * NEVER resets progress if days were missed — continues cleanly from dailyRewardNextDay (1..7 -> 1).
     */
    suspend fun claimDailyLoginReward(rewardConfig: DailyRewardConfig): Result<Long> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val dateKey = todayUtcDateKey()
        val userRef = db.collection("users").document(uid)
        return try {
            val coinsAwarded = db.runTransaction { tx ->
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Profile missing")
                if (user.blocked) throw IllegalStateException("Account restricted")
                if (user.dailyRewardLastClaimDate == dateKey) {
                    throw IllegalStateException("Today's daily reward has already been claimed.")
                }
                val currentDay = user.dailyRewardNextDay.coerceIn(1L, 7L)
                val nextDay = (currentDay % 7L) + 1L
                val rewardCoins = rewardConfig.rewardCoins.coerceIn(0L, 5000L)
                val newEarned = user.earnedCoins + rewardCoins

                tx.update(
                    userRef,
                    mapOf(
                        "dailyRewardNextDay" to nextDay,
                        "dailyRewardLastClaimDate" to dateKey,
                        "earnedCoins" to newEarned,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val txId = "daily_rew_${dateKey}_${uid.take(8)}"
                tx.set(
                    db.collection("walletTransactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "COIN",
                        "coinSource" to "EARNED",
                        "txType" to "DAILY_REWARD",
                        "amountDelta" to rewardCoins,
                        "balanceAfter" to (newEarned + user.purchasedCoins),
                        "description" to "Daily Login Reward — Day $currentDay (${rewardConfig.title})",
                        "referenceId" to "day_${currentDay}_$dateKey",
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                rewardCoins
            }.await()
            Result.success(coinsAwarded)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun spendCoinsForContinue(costCoins: Long): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        if (costCoins <= 0L) return Result.success(Unit)
        val userRef = db.collection("users").document(uid)
        return try {
            db.runTransaction { tx ->
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Profile missing")
                if (user.blocked) throw IllegalStateException("Account restricted")
                if (user.totalCoins < costCoins) {
                    throw IllegalStateException("Insufficient Coins (need $costCoins, have ${user.totalCoins}).")
                }
                val spendFromEarned = minOf(user.earnedCoins, costCoins)
                val spendFromPurchased = costCoins - spendFromEarned
                val newEarned = user.earnedCoins - spendFromEarned
                val newPurchased = user.purchasedCoins - spendFromPurchased

                tx.update(
                    userRef,
                    mapOf(
                        "earnedCoins" to newEarned,
                        "purchasedCoins" to newPurchased,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                val txId = "cont_${uid.take(8)}_${System.currentTimeMillis()}"
                tx.set(
                    db.collection("walletTransactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "COIN",
                        "coinSource" to if (spendFromPurchased > 0L) "PURCHASED" else "EARNED",
                        "txType" to "CONTINUE_SPEND",
                        "amountDelta" to -costCoins,
                        "balanceAfter" to (newEarned + newPurchased),
                        "description" to "Continue Game with Coins (-$costCoins Coins)",
                        "referenceId" to txId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun purchaseShopItem(item: ShopItem): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val userRef = db.collection("users").document(uid)
        return try {
            db.runTransaction { tx ->
                val user = tx.get(userRef).toObject(UserAccount::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: throw IllegalStateException("Profile missing")
                if (user.blocked) throw IllegalStateException("Account restricted")
                if (item.category == "THEME" && item.themeKey in user.unlockedThemeIds) {
                    throw IllegalStateException("Theme already unlocked.")
                }
                val price = item.priceCoins.coerceAtLeast(0L)
                if (user.totalCoins < price) {
                    throw IllegalStateException("Not enough Coins. Need $price Coins.")
                }
                val spendFromEarned = minOf(user.earnedCoins, price)
                val spendFromPurchased = price - spendFromEarned
                val newEarned = user.earnedCoins - spendFromEarned
                val newPurchased = user.purchasedCoins - spendFromPurchased
                val updatedThemes = if (item.category == "THEME" && item.themeKey.isNotBlank()) {
                    (user.unlockedThemeIds + item.themeKey).distinct().take(30)
                } else user.unlockedThemeIds
                val nextSelectedTheme = if (item.category == "THEME" && item.themeKey.isNotBlank()) {
                    item.themeKey
                } else user.selectedThemeId

                tx.update(
                    userRef,
                    mapOf(
                        "earnedCoins" to newEarned,
                        "purchasedCoins" to newPurchased,
                        "unlockedThemeIds" to updatedThemes,
                        "selectedThemeId" to nextSelectedTheme,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )

                val txId = "shop_${uid.take(8)}_${System.currentTimeMillis()}"
                tx.set(
                    db.collection("walletTransactions").document(txId),
                    mapOf(
                        "userId" to uid,
                        "walletType" to "COIN",
                        "coinSource" to if (spendFromPurchased > 0L) "PURCHASED" else "EARNED",
                        "txType" to if (item.category == "THEME") "THEME_PURCHASE" else "SHOP_PURCHASE",
                        "amountDelta" to -price,
                        "balanceAfter" to (newEarned + newPurchased),
                        "description" to "Purchased ${item.name} (-$price Coins)",
                        "referenceId" to item.id.ifBlank { txId },
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
            }.await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
            Result.failure(e)
        }
    }

    suspend fun selectTheme(themeId: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "selectedThemeId" to themeId,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateDisplayName(newName: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val trimmed = newName.trim().take(50)
        if (trimmed.isBlank()) return Result.failure(IllegalArgumentException("Display name cannot be empty."))
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to trimmed,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateAudioSettings(musicEnabled: Boolean, sfxEnabled: Boolean): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "musicEnabled" to musicEnabled,
                    "sfxEnabled" to sfxEnabled,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveWalletVerificationForm(
        fullName: String,
        provider: String,
        accountNumber: String
    ): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val cleanName = fullName.trim().take(100)
        val cleanAcc = accountNumber.trim().take(40)
        if (cleanName.length < 2) return Result.failure(IllegalArgumentException("Please enter your full name."))
        if (provider !in listOf("JAZZCASH", "EASYPAISA")) return Result.failure(IllegalArgumentException("Select JazzCash or Easypaisa."))
        if (cleanAcc.length < 10) return Result.failure(IllegalArgumentException("Please enter a valid account number."))

        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "walletVerificationName" to cleanName,
                    "walletVerificationProvider" to provider,
                    "walletVerificationAccount" to cleanAcc,
                    "walletVerificationCompleted" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun saveWithdrawalSecurityPassword(hash: String, salt: String): Result<Unit> {
        val uid = try { requireUserId() } catch (e: Exception) { return Result.failure(e) }
        val path = "users/$uid"
        return try {
            db.collection("users").document(uid).update(
                mapOf(
                    "withdrawalPinHash" to hash,
                    "withdrawalPinSalt" to salt,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    private fun logStreamErrorIfAuthenticated(
        error: Throwable,
        expectedUid: String,
        op: OperationType,
        path: String
    ) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) return
        val exceptionToLog = firestoreError ?: (error as? Exception)
        if (exceptionToLog != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(exceptionToLog, op, path)
        }
    }
}
