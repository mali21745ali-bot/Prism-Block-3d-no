package com.example.data

import android.content.Context
import com.example.R
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.DEFAULT_THEME_ID
import com.example.data.model.LeaderboardBoardType
import com.example.data.model.LeaderboardScore
import com.example.data.model.LevelProgress
import com.example.data.model.MAX_LEADERBOARD_SCORE
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.data.model.MAX_LEVEL_REQUIRED_LINES
import com.example.data.model.MAX_PROFILE_DISPLAY_NAME_LENGTH
import com.example.data.model.MAX_PROFILE_PHOTO_URL_LENGTH
import com.example.data.model.UserProfile
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

/**
 * Dedicated Firestore repository managing:
 * 1. User Profiles (`/users/{userId}`)
 * 2. Per-Level Progress (`/users/{userId}/levelProgress/{levelId}`)
 * 3. Leaderboard Scores (`/leaderboards/{entryId}`)
 *
 * Strictly binds to the custom named database (`R.string.firestore_database_id`),
 * enforces authenticated Google Sign-In state via [requireUserId], uses server timestamps,
 * and surfaces all failures cleanly via [Result] and [handleFirestoreError].
 */
class PlayerDataRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    /**
     * Convenience constructor resolving the provisioned custom Firestore database ID
     * using `applicationContext` to prevent Activity leaks.
     */
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    private fun requireMatchingUser(targetUserId: String): String {
        val signedInUid = requireUserId()
        check(signedInUid == targetUserId) {
            "Signed-in user does not match requested userId; refusing cross-user access."
        }
        return signedInUid
    }

    fun todayUtcDateKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }

    // =========================================================================
    // 1. USER PROFILES (`/users/{userId}`)
    // =========================================================================

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(null)
            return@flow
        }
        val path = "$COLLECTION_USERS/$userId"
        emitAll(
            db.collection(COLLECTION_USERS).document(userId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, userId, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getUserProfile(userId: String): Result<UserProfile?> {
        val path = "$COLLECTION_USERS/$userId"
        return try {
            requireMatchingUser(userId)
            val snap = db.collection(COLLECTION_USERS).document(userId).get().await()
            val profile = if (snap.exists()) {
                snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(profile)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Initializes a brand-new player document at `/users/{uid}` matching the exact
     * 30-field `isValidUserAccount` rules contract if it does not yet exist, then returns [UserProfile].
     */
    suspend fun createOrEnsureUserProfile(firebaseUser: FirebaseUser): Result<UserProfile> {
        val uid = firebaseUser.uid
        val path = "$COLLECTION_USERS/$uid"
        return try {
            requireMatchingUser(uid)
            val docRef = db.collection(COLLECTION_USERS).document(uid)
            val existing = docRef.get().await()
            if (existing.exists()) {
                val profile = existing.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    ?: UserProfile(id = uid, userId = uid)
                return Result.success(profile)
            }

            val safeName = sanitizeDisplayName(
                firebaseUser.displayName
                    ?: firebaseUser.email?.substringBefore("@")
                    ?: "Prism Player"
            )
            val safePhoto = (firebaseUser.photoUrl?.toString() ?: "").take(MAX_PROFILE_PHOTO_URL_LENGTH)
            val safeEmail = (firebaseUser.email ?: "").take(120)

            val initialPayload = mapOf(
                "userId" to uid,
                "displayName" to safeName,
                "email" to safeEmail,
                "photoUrl" to safePhoto,
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
            docRef.set(initialPayload).await()

            Result.success(
                UserProfile(
                    id = uid,
                    userId = uid,
                    displayName = safeName,
                    email = safeEmail,
                    photoUrl = safePhoto
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    /**
     * Performs a validated partial update of editable user profile fields at `/users/{uid}`,
     * preserving immutable `userId`, `createdAt`, and wallet/gameplay balances.
     */
    suspend fun updateUserProfile(
        displayName: String? = null,
        photoUrl: String? = null,
        musicEnabled: Boolean? = null,
        sfxEnabled: Boolean? = null
    ): Result<Unit> {
        var path = "$COLLECTION_USERS/${auth.currentUser?.uid ?: "unauthenticated"}"
        return try {
            val uid = requireUserId()
            path = "$COLLECTION_USERS/$uid"

            val updates = mutableMapOf<String, Any>()
            if (displayName != null) {
                val trimmed = displayName.trim()
                require(trimmed.isNotEmpty() && trimmed.length <= MAX_PROFILE_DISPLAY_NAME_LENGTH) {
                    "Display name must be between 1 and $MAX_PROFILE_DISPLAY_NAME_LENGTH characters."
                }
                updates["displayName"] = trimmed
            }
            if (photoUrl != null) {
                val trimmedUrl = photoUrl.trim()
                require(trimmedUrl.length <= MAX_PROFILE_PHOTO_URL_LENGTH) {
                    "Photo URL must be at most $MAX_PROFILE_PHOTO_URL_LENGTH characters."
                }
                updates["photoUrl"] = trimmedUrl
            }
            if (musicEnabled != null) updates["musicEnabled"] = musicEnabled
            if (sfxEnabled != null) updates["sfxEnabled"] = sfxEnabled

            if (updates.isEmpty()) return Result.success(Unit)
            updates["updatedAt"] = FieldValue.serverTimestamp()

            db.collection(COLLECTION_USERS).document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 2. LEVEL PROGRESS (`/users/{userId}/levelProgress/{levelId}`)
    // =========================================================================

    fun observeAllLevelProgress(userId: String): Flow<List<LevelProgress>> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(emptyList())
            return@flow
        }
        val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_LEVEL_PROGRESS"
        emitAll(
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_LEVEL_PROGRESS)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LevelProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedBy { it.levelNumber }
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, userId, OperationType.LIST, path)
                    throw error
                }
        )
    }

    fun observeLevelProgress(userId: String, levelNumber: Long): Flow<LevelProgress?> = flow {
        if (userId.isBlank() || auth.currentUser == null || levelNumber !in 1L..MAX_LEVEL_NUMBER.toLong()) {
            emit(null)
            return@flow
        }
        val docId = LevelProgress.documentId(levelNumber)
        val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_LEVEL_PROGRESS/$docId"
        emitAll(
            db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_LEVEL_PROGRESS)
                .document(docId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(LevelProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, userId, OperationType.GET, path)
                    throw error
                }
        )
    }

    suspend fun getLevelProgress(userId: String, levelNumber: Long): Result<LevelProgress?> {
        val docId = if (levelNumber in 1L..MAX_LEVEL_NUMBER.toLong()) LevelProgress.documentId(levelNumber) else "invalid"
        val path = "$COLLECTION_USERS/$userId/$SUBCOLLECTION_LEVEL_PROGRESS/$docId"
        return try {
            requireMatchingUser(userId)
            require(levelNumber in 1L..MAX_LEVEL_NUMBER.toLong()) {
                "Level number must be in 1..$MAX_LEVEL_NUMBER."
            }
            val snap = db.collection(COLLECTION_USERS)
                .document(userId)
                .collection(SUBCOLLECTION_LEVEL_PROGRESS)
                .document(docId)
                .get()
                .await()
            val record = if (snap.exists()) {
                snap.toObject(LevelProgress::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
            } else null
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    /**
     * Atomically creates or updates a player's per-level progress document at
     * `/users/{uid}/levelProgress/level_<levelNumber>`.
     *
     * Guarantees monotonic progression (`linesCleared`, `bestScore`, `attemptsCount`, `starsEarned`),
     * omits optional `completedAt` while incomplete, and sets `completedAt` once upon completion.
     */
    suspend fun recordLevelProgress(
        levelNumber: Long,
        linesAdded: Long,
        sessionScore: Long,
        requiredLines: Long = DEFAULT_LINES_PER_LEVEL,
        incrementAttempt: Boolean = true
    ): Result<LevelProgress> {
        var path = "$COLLECTION_USERS/${auth.currentUser?.uid ?: "unauthenticated"}/$SUBCOLLECTION_LEVEL_PROGRESS"
        return try {
            val uid = requireUserId()
            require(levelNumber in 1L..MAX_LEVEL_NUMBER.toLong()) {
                "Level number must be in 1..$MAX_LEVEL_NUMBER."
            }
            require(linesAdded >= 0L) { "linesAdded cannot be negative." }
            require(sessionScore in 0L..MAX_LEADERBOARD_SCORE) {
                "sessionScore must be between 0 and $MAX_LEADERBOARD_SCORE."
            }
            val safeRequired = requiredLines.coerceIn(1L, MAX_LEVEL_REQUIRED_LINES)
            val docId = LevelProgress.documentId(levelNumber)
            val docRef = db.collection(COLLECTION_USERS)
                .document(uid)
                .collection(SUBCOLLECTION_LEVEL_PROGRESS)
                .document(docId)
            path = docRef.path

            val saved = db.runTransaction { tx ->
                val snap = tx.get(docRef)
                val attemptDelta = if (incrementAttempt) 1L else 0L

                if (snap.exists()) {
                    val prevReq = (snap.getLong("requiredLines") ?: safeRequired).coerceIn(1L, MAX_LEVEL_REQUIRED_LINES)
                    val prevLines = snap.getLong("linesCleared") ?: 0L
                    val prevBest = snap.getLong("bestScore") ?: 0L
                    val prevAttempts = snap.getLong("attemptsCount") ?: 0L
                    val prevStars = snap.getLong("starsEarned") ?: 0L
                    val prevCompleted = snap.getBoolean("completed") ?: false

                    val nextLines = (prevLines + linesAdded).coerceAtMost(prevReq)
                    val nextCompleted = prevCompleted || nextLines >= prevReq
                    val nextBest = maxOf(prevBest, sessionScore)
                    val nextAttempts = (prevAttempts + attemptDelta).coerceAtMost(1_000_000L)
                    val nextStars = maxOf(prevStars, LevelProgress.computeStars(nextLines, prevReq, nextCompleted))

                    val updateMap = mutableMapOf<String, Any>(
                        "linesCleared" to nextLines,
                        "bestScore" to nextBest,
                        "attemptsCount" to nextAttempts,
                        "starsEarned" to nextStars,
                        "completed" to nextCompleted,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (!prevCompleted && nextCompleted) {
                        updateMap["completedAt"] = FieldValue.serverTimestamp()
                    }
                    tx.update(docRef, updateMap)

                    LevelProgress(
                        id = docId,
                        userId = uid,
                        levelNumber = levelNumber,
                        linesCleared = nextLines,
                        requiredLines = prevReq,
                        bestScore = nextBest,
                        attemptsCount = nextAttempts,
                        starsEarned = nextStars,
                        completed = nextCompleted
                    )
                } else {
                    val nextLines = linesAdded.coerceAtMost(safeRequired)
                    val nowCompleted = nextLines >= safeRequired
                    val stars = LevelProgress.computeStars(nextLines, safeRequired, nowCompleted)

                    val createMap = mutableMapOf<String, Any>(
                        "userId" to uid,
                        "levelNumber" to levelNumber,
                        "linesCleared" to nextLines,
                        "requiredLines" to safeRequired,
                        "bestScore" to sessionScore,
                        "attemptsCount" to attemptDelta,
                        "starsEarned" to stars,
                        "completed" to nowCompleted,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    if (nowCompleted) {
                        createMap["completedAt"] = FieldValue.serverTimestamp()
                    }
                    tx.set(docRef, createMap)

                    LevelProgress(
                        id = docId,
                        userId = uid,
                        levelNumber = levelNumber,
                        linesCleared = nextLines,
                        requiredLines = safeRequired,
                        bestScore = sessionScore,
                        attemptsCount = attemptDelta,
                        starsEarned = stars,
                        completed = nowCompleted
                    )
                }
            }.await()

            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // =========================================================================
    // 3. LEADERBOARD SCORES (`/leaderboards/{entryId}`)
    // =========================================================================

    /**
     * Real-time leaderboard stream aligned with `firestore.rules`
     * (`resource.data.boardType in ['DAILY', 'ALL_TIME']`).
     */
    fun observeLeaderboard(
        boardType: LeaderboardBoardType,
        dateKey: String = todayUtcDateKey(),
        limit: Int = 50
    ): Flow<List<LeaderboardScore>> = flow {
        val currentUid = auth.currentUser?.uid
        if (currentUid.isNullOrBlank()) {
            emit(emptyList())
            return@flow
        }
        val safeLimit = limit.coerceIn(1, 200)
        val baseQuery = db.collection(COLLECTION_LEADERBOARDS)
            .whereEqualTo("boardType", boardType.wireName)
        val query = if (boardType == LeaderboardBoardType.DAILY) {
            baseQuery.whereEqualTo("dateKey", dateKey)
        } else {
            baseQuery
        }

        emitAll(
            query.snapshots()
                .map { snapshot ->
                    snapshot.toObjects(LeaderboardScore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareByDescending<LeaderboardScore> { it.bestScore }
                                .thenByDescending { it.totalLines }
                        )
                        .take(safeLimit)
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, currentUid, OperationType.LIST, COLLECTION_LEADERBOARDS)
                    throw error
                }
        )
    }

    suspend fun getTopLeaderboardScores(
        boardType: LeaderboardBoardType,
        dateKey: String = todayUtcDateKey(),
        limit: Int = 50
    ): Result<List<LeaderboardScore>> {
        return try {
            requireUserId()
            val safeLimit = limit.coerceIn(1, 200)
            val baseQuery = db.collection(COLLECTION_LEADERBOARDS)
                .whereEqualTo("boardType", boardType.wireName)
            val query = if (boardType == LeaderboardBoardType.DAILY) {
                baseQuery.whereEqualTo("dateKey", dateKey)
            } else {
                baseQuery
            }
            val snap = query.get().await()
            val ranked = snap.toObjects(LeaderboardScore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                .sortedWith(
                    compareByDescending<LeaderboardScore> { it.bestScore }
                        .thenByDescending { it.totalLines }
                )
                .take(safeLimit)
            Result.success(ranked)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, COLLECTION_LEADERBOARDS)
            Result.failure(e)
        }
    }

    fun observePlayerLeaderboardScore(
        userId: String,
        boardType: LeaderboardBoardType,
        dateKey: String = todayUtcDateKey()
    ): Flow<LeaderboardScore?> = flow {
        if (userId.isBlank() || auth.currentUser == null) {
            emit(null)
            return@flow
        }
        val docId = when (boardType) {
            LeaderboardBoardType.DAILY -> LeaderboardScore.dailyDocumentId(dateKey, userId)
            LeaderboardBoardType.ALL_TIME -> LeaderboardScore.allTimeDocumentId(userId)
        }
        val path = "$COLLECTION_LEADERBOARDS/$docId"
        emitAll(
            db.collection(COLLECTION_LEADERBOARDS).document(docId)
                .snapshots()
                .map { snapshot ->
                    if (snapshot.exists()) {
                        snapshot.toObject(LeaderboardScore::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    logStreamErrorIfAuthenticated(error, userId, OperationType.GET, path)
                    throw error
                }
        )
    }

    /**
     * Atomically upserts a single leaderboard score entry (DAILY or ALL_TIME),
     * preserving zero PII, monotonic `bestScore`, immutable `userId`/`boardType`/`dateKey`/`createdAt`,
     * and server timestamps.
     */
    suspend fun submitLeaderboardScore(
        displayName: String,
        photoUrl: String,
        score: Long,
        linesCleared: Long,
        highestLevel: Long,
        boardType: LeaderboardBoardType = LeaderboardBoardType.ALL_TIME,
        dateKey: String = todayUtcDateKey()
    ): Result<LeaderboardScore> {
        var path = COLLECTION_LEADERBOARDS
        return try {
            val uid = requireUserId()
            val safeName = sanitizeDisplayName(displayName)
            val safePhoto = photoUrl.trim().take(MAX_PROFILE_PHOTO_URL_LENGTH)
            require(score in 0L..MAX_LEADERBOARD_SCORE) { "Score must be between 0 and $MAX_LEADERBOARD_SCORE." }
            require(linesCleared >= 0L) { "linesCleared cannot be negative." }
            val safeLevel = highestLevel.coerceIn(1L, MAX_LEVEL_NUMBER.toLong())
            val effectiveDateKey = if (boardType == LeaderboardBoardType.DAILY) {
                require(dateKey.length in 4..20) { "Invalid dateKey: $dateKey" }
                dateKey
            } else {
                LeaderboardBoardType.ALL_TIME.wireName
            }
            val docId = when (boardType) {
                LeaderboardBoardType.DAILY -> LeaderboardScore.dailyDocumentId(effectiveDateKey, uid)
                LeaderboardBoardType.ALL_TIME -> LeaderboardScore.allTimeDocumentId(uid)
            }
            val docRef = db.collection(COLLECTION_LEADERBOARDS).document(docId)
            path = docRef.path

            val saved = db.runTransaction { tx ->
                upsertLeaderboardInTransaction(
                    tx = tx,
                    docRef = docRef,
                    docId = docId,
                    uid = uid,
                    displayName = safeName,
                    photoUrl = safePhoto,
                    boardType = boardType,
                    dateKey = effectiveDateKey,
                    score = score,
                    linesCleared = linesCleared,
                    highestLevel = safeLevel
                )
            }.await()

            Result.success(saved)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    /**
     * Atomically updates both the player's Daily and All-Time leaderboard entries in a single transaction.
     */
    suspend fun submitDailyAndAllTimeScores(
        displayName: String,
        photoUrl: String,
        score: Long,
        linesCleared: Long,
        highestLevel: Long,
        dateKey: String = todayUtcDateKey()
    ): Result<Pair<LeaderboardScore, LeaderboardScore>> {
        return try {
            val uid = requireUserId()
            val safeName = sanitizeDisplayName(displayName)
            val safePhoto = photoUrl.trim().take(MAX_PROFILE_PHOTO_URL_LENGTH)
            require(score in 0L..MAX_LEADERBOARD_SCORE) { "Score must be between 0 and $MAX_LEADERBOARD_SCORE." }
            require(linesCleared >= 0L) { "linesCleared cannot be negative." }
            require(dateKey.length in 4..20) { "Invalid dateKey: $dateKey" }
            val safeLevel = highestLevel.coerceIn(1L, MAX_LEVEL_NUMBER.toLong())

            val dailyId = LeaderboardScore.dailyDocumentId(dateKey, uid)
            val allTimeId = LeaderboardScore.allTimeDocumentId(uid)
            val dailyRef = db.collection(COLLECTION_LEADERBOARDS).document(dailyId)
            val allTimeRef = db.collection(COLLECTION_LEADERBOARDS).document(allTimeId)

            val result = db.runTransaction { tx ->
                val dailySnap = tx.get(dailyRef)
                val allTimeSnap = tx.get(allTimeRef)

                val dailySaved = applyLeaderboardUpsert(
                    tx = tx,
                    docRef = dailyRef,
                    snap = dailySnap,
                    docId = dailyId,
                    uid = uid,
                    displayName = safeName,
                    photoUrl = safePhoto,
                    boardType = LeaderboardBoardType.DAILY,
                    dateKey = dateKey,
                    score = score,
                    linesCleared = linesCleared,
                    highestLevel = safeLevel
                )
                val allTimeSaved = applyLeaderboardUpsert(
                    tx = tx,
                    docRef = allTimeRef,
                    snap = allTimeSnap,
                    docId = allTimeId,
                    uid = uid,
                    displayName = safeName,
                    photoUrl = safePhoto,
                    boardType = LeaderboardBoardType.ALL_TIME,
                    dateKey = LeaderboardBoardType.ALL_TIME.wireName,
                    score = score,
                    linesCleared = linesCleared,
                    highestLevel = safeLevel
                )
                dailySaved to allTimeSaved
            }.await()

            Result.success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, COLLECTION_LEADERBOARDS)
            Result.failure(e)
        }
    }

    private fun upsertLeaderboardInTransaction(
        tx: com.google.firebase.firestore.Transaction,
        docRef: com.google.firebase.firestore.DocumentReference,
        docId: String,
        uid: String,
        displayName: String,
        photoUrl: String,
        boardType: LeaderboardBoardType,
        dateKey: String,
        score: Long,
        linesCleared: Long,
        highestLevel: Long
    ): LeaderboardScore {
        val snap = tx.get(docRef)
        return applyLeaderboardUpsert(
            tx = tx,
            docRef = docRef,
            snap = snap,
            docId = docId,
            uid = uid,
            displayName = displayName,
            photoUrl = photoUrl,
            boardType = boardType,
            dateKey = dateKey,
            score = score,
            linesCleared = linesCleared,
            highestLevel = highestLevel
        )
    }

    private fun applyLeaderboardUpsert(
        tx: com.google.firebase.firestore.Transaction,
        docRef: com.google.firebase.firestore.DocumentReference,
        snap: DocumentSnapshot,
        docId: String,
        uid: String,
        displayName: String,
        photoUrl: String,
        boardType: LeaderboardBoardType,
        dateKey: String,
        score: Long,
        linesCleared: Long,
        highestLevel: Long
    ): LeaderboardScore {
        return if (snap.exists()) {
            val prevBest = snap.getLong("bestScore") ?: 0L
            val prevLines = snap.getLong("totalLines") ?: 0L
            val prevLevel = snap.getLong("highestLevel") ?: 1L
            val prevGames = snap.getLong("gamesPlayed") ?: 0L

            val nextBest = maxOf(prevBest, score)
            val nextLines = prevLines + linesCleared
            val nextLevel = maxOf(prevLevel, highestLevel)
            val nextGames = prevGames + 1L

            tx.update(
                docRef,
                mapOf(
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "bestScore" to nextBest,
                    "totalLines" to nextLines,
                    "highestLevel" to nextLevel,
                    "gamesPlayed" to nextGames,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            LeaderboardScore(
                id = docId,
                userId = uid,
                displayName = displayName,
                photoUrl = photoUrl,
                boardType = boardType.wireName,
                dateKey = dateKey,
                bestScore = nextBest,
                totalLines = nextLines,
                highestLevel = nextLevel,
                gamesPlayed = nextGames
            )
        } else {
            tx.set(
                docRef,
                mapOf(
                    "userId" to uid,
                    "displayName" to displayName,
                    "photoUrl" to photoUrl,
                    "boardType" to boardType.wireName,
                    "dateKey" to dateKey,
                    "bestScore" to score,
                    "totalLines" to linesCleared,
                    "highestLevel" to highestLevel,
                    "gamesPlayed" to 1L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            LeaderboardScore(
                id = docId,
                userId = uid,
                displayName = displayName,
                photoUrl = photoUrl,
                boardType = boardType.wireName,
                dateKey = dateKey,
                bestScore = score,
                totalLines = linesCleared,
                highestLevel = highestLevel,
                gamesPlayed = 1L
            )
        }
    }

    private fun sanitizeDisplayName(raw: String): String {
        return raw.trim().take(MAX_PROFILE_DISPLAY_NAME_LENGTH).ifBlank { "Prism Player" }
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

    companion object {
        const val COLLECTION_USERS = "users"
        const val SUBCOLLECTION_LEVEL_PROGRESS = "levelProgress"
        const val COLLECTION_LEADERBOARDS = "leaderboards"
    }
}
