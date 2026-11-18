package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PlayerDataRepository
import com.example.data.model.DEFAULT_LINES_PER_LEVEL
import com.example.data.model.LeaderboardBoardType
import com.example.data.model.LeaderboardEntry
import com.example.data.model.LeaderboardScore
import com.example.data.model.LevelProgress
import com.example.data.model.MAX_LEVEL_NUMBER
import com.example.data.model.PlayerProfileSummary
import com.example.data.model.UserAccount
import com.example.data.model.UserProfile
import com.example.data.model.toLeaderboardScore
import com.example.data.model.toUserProfile
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PlayerDataModelAndRepositoryTest {

    @Test
    fun firestoreDataModels_haveNoArgConstructorsAndRuntimeAnnotations() {
        val modelClasses = listOf(
            UserProfile::class.java,
            LevelProgress::class.java,
            LeaderboardScore::class.java
        )

        for (clazz in modelClasses) {
            // Firestore toObject() requires a public no-arg constructor
            val ctor = clazz.getDeclaredConstructor()
            val instance = ctor.newInstance()
            assertNotNull("Expected no-arg instance for ${clazz.simpleName}", instance)

            // Schema evolution safety (@IgnoreExtraProperties has RUNTIME retention)
            assertNotNull(
                "${clazz.simpleName} must have @IgnoreExtraProperties",
                clazz.getAnnotation(IgnoreExtraProperties::class.java)
            )

            // @DocumentId on `id`
            val idField = clazz.getDeclaredField("id")
            assertNotNull("${clazz.simpleName}.id must have @DocumentId", idField.getAnnotation(DocumentId::class.java))

            // @ServerTimestamp on createdAt & updatedAt
            assertNotNull(
                "${clazz.simpleName}.createdAt must have @ServerTimestamp",
                clazz.getDeclaredField("createdAt").getAnnotation(ServerTimestamp::class.java)
            )
            assertNotNull(
                "${clazz.simpleName}.updatedAt must have @ServerTimestamp",
                clazz.getDeclaredField("updatedAt").getAnnotation(ServerTimestamp::class.java)
            )

            // Guard against Kotlin `is`-prefixed Boolean properties (Firestore reflection naming bug)
            val isPrefixedBooleans = clazz.declaredFields.filter { field ->
                (field.type == Boolean::class.javaPrimitiveType || field.type == Boolean::class.javaObjectType) &&
                    field.name.startsWith("is") &&
                    field.name.length > 2 &&
                    field.name[2].isUpperCase()
            }
            assertTrue(
                "${clazz.simpleName} must not declare is-prefixed Booleans: $isPrefixedBooleans",
                isPrefixedBooleans.isEmpty()
            )
        }

        // Verify computed properties are excluded from Firestore serialization
        assertNotNull(
            UserProfile::class.java.getDeclaredMethod("getLevelProgressFraction").getAnnotation(Exclude::class.java)
        )
        assertNotNull(
            LevelProgress::class.java.getDeclaredMethod("getProgressFraction").getAnnotation(Exclude::class.java)
        )
        assertNotNull(
            LevelProgress::class.java.getDeclaredMethod("getRemainingLines").getAnnotation(Exclude::class.java)
        )
        assertNotNull(
            LeaderboardScore::class.java.getDeclaredMethod("getResolvedBoardType").getAnnotation(Exclude::class.java)
        )
        assertNotNull(
            LevelProgress::class.java.getDeclaredField("completedAt").getAnnotation(ServerTimestamp::class.java)
        )
    }

    @Test
    fun levelProgress_documentIdBoundsAndStarCalculations() {
        assertEquals("level_1", LevelProgress.documentId(1L))
        assertEquals("level_100", LevelProgress.documentId(MAX_LEVEL_NUMBER.toLong()))

        assertThrowsIllegalArgument { LevelProgress.documentId(0L) }
        assertThrowsIllegalArgument { LevelProgress.documentId(101L) }

        assertEquals(0L, LevelProgress.computeStars(linesCleared = 0L, requiredLines = 1000L, completed = false))
        assertEquals(0L, LevelProgress.computeStars(linesCleared = 249L, requiredLines = 1000L, completed = false))
        assertEquals(1L, LevelProgress.computeStars(linesCleared = 250L, requiredLines = 1000L, completed = false))
        assertEquals(2L, LevelProgress.computeStars(linesCleared = 650L, requiredLines = 1000L, completed = false))
        assertEquals(3L, LevelProgress.computeStars(linesCleared = 1000L, requiredLines = 1000L, completed = false))
        assertEquals(3L, LevelProgress.computeStars(linesCleared = 100L, requiredLines = 1000L, completed = true))

        val lp = LevelProgress(
            id = "level_4",
            userId = "uid_1",
            levelNumber = 4L,
            linesCleared = 250L,
            requiredLines = DEFAULT_LINES_PER_LEVEL
        )
        assertEquals(0.25f, lp.progressFraction, 0.0001f)
        assertEquals(750L, lp.remainingLines)
    }

    @Test
    fun leaderboardScore_documentIdsBoardTypesAndMappers() {
        assertEquals("daily_2026-11-18_u123", LeaderboardScore.dailyDocumentId("2026-11-18", "u123"))
        assertEquals("alltime_u123", LeaderboardScore.allTimeDocumentId("u123"))
        assertThrowsIllegalArgument { LeaderboardScore.dailyDocumentId("bad", "u123") }
        assertThrowsIllegalArgument { LeaderboardScore.allTimeDocumentId("  ") }

        assertEquals(LeaderboardBoardType.DAILY, LeaderboardBoardType.fromWire("DAILY"))
        assertEquals(LeaderboardBoardType.DAILY, LeaderboardBoardType.fromWire("daily"))
        assertEquals(LeaderboardBoardType.ALL_TIME, LeaderboardBoardType.fromWire("ALL_TIME"))
        assertEquals(LeaderboardBoardType.ALL_TIME, LeaderboardBoardType.fromWire("UNKNOWN"))

        val account = UserAccount(
            id = "u_99",
            userId = "u_99",
            displayName = "Aria",
            email = "aria@example.com",
            classicHighScore = 4800L,
            totalLinesCleared = 480L,
            totalGamesPlayed = 12L,
            currentLevel = 3L,
            currentLevelLines = 500L,
            completedLevelsCount = 2L
        )
        val profile = account.toUserProfile()
        assertEquals("u_99", profile.id)
        assertEquals("Aria", profile.displayName)
        assertEquals(0.5f, profile.levelProgressFraction, 0.0001f)

        val entry = LeaderboardEntry(
            id = "alltime_u_99",
            userId = "u_99",
            displayName = "Aria",
            boardType = "ALL_TIME",
            dateKey = "ALL_TIME",
            bestScore = 4800L,
            totalLines = 480L,
            highestLevel = 3L,
            gamesPlayed = 12L
        )
        val score = entry.toLeaderboardScore()
        assertEquals(LeaderboardBoardType.ALL_TIME, score.resolvedBoardType)
        assertEquals(4800L, score.bestScore)

        val summary = PlayerProfileSummary(
            profile = profile,
            levels = listOf(LevelProgress(id = "level_1", userId = "u_99", levelNumber = 1L, completed = true)),
            allTimeScore = score
        )
        assertEquals(1, summary.levels.size)
        assertEquals(4800L, summary.allTimeScore?.bestScore)
    }

    @Test
    fun repositoryConstantsAndNamedDatabaseResource_areConfigured() {
        assertEquals("users", PlayerDataRepository.COLLECTION_USERS)
        assertEquals("levelProgress", PlayerDataRepository.SUBCOLLECTION_LEVEL_PROGRESS)
        assertEquals("leaderboards", PlayerDataRepository.COLLECTION_LEADERBOARDS)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbId = context.getString(R.string.firestore_database_id)
        assertTrue("Expected named Firestore database ID resource", dbId.isNotBlank())
        assertNotEquals("(default)", dbId)
    }

    @Test
    fun levelProgress_completedAtNullByDefaultAndProgressClampsSafely() {
        val defaultLp = LevelProgress()
        assertFalse(defaultLp.completed)
        assertNull(defaultLp.completedAt)
        assertEquals(0f, defaultLp.progressFraction, 0.0001f)

        val overflowLp = LevelProgress(linesCleared = 1500L, requiredLines = 1000L)
        assertEquals(1f, overflowLp.progressFraction, 0.0001f)
        assertEquals(0L, overflowLp.remainingLines)
    }

    @Test
    fun defaultModelValues_areSafeForEmptyProfilesAndLeaderboards() = runTest {
        val emptyProfile = UserProfile(userId = "")
        assertEquals(0f, emptyProfile.levelProgressFraction, 0.0001f)
        assertEquals("ALL_TIME", LeaderboardScore().boardType)
    }

    private fun assertThrowsIllegalArgument(block: () -> Unit) {
        var threw = false
        try {
            block()
        } catch (_: IllegalArgumentException) {
            threw = true
        }
        assertTrue("Expected IllegalArgumentException to be thrown", threw)
    }
}
