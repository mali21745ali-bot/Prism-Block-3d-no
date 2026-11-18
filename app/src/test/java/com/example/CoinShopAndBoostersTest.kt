package com.example

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.audio.SoundEngine
import com.example.data.model.BoosterType
import com.example.data.model.GameMode
import com.example.data.model.PlayerInventory
import com.example.data.model.PromotionalOffer
import com.example.data.model.PurchasedItem
import com.example.data.model.ShopItem
import com.example.data.model.ShopTransaction
import com.example.game.BOARD_SIZE
import com.example.game.BlockPuzzleEngine
import com.example.game.BlockThemes
import com.example.game.BoardBackgroundAnimation
import com.example.game.CosmeticCatalog
import com.example.game.GridCell
import com.example.game.PolyominoShape
import com.example.game.PurchaseValidationResult
import com.example.game.ShopPricingEngine
import com.example.game.TrayPiece
import com.example.ui.game.GameplayBoardStage
import com.example.ui.rewards.CoinShopContent
import com.example.ui.theme.PrismBlockTheme
import com.example.viewmodel.GamePlayViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class CoinShopAndBoostersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dot = PolyominoShape("dot_1", "dot_1", listOf(GridCell(0, 0)), 1)
    private val line3H = PolyominoShape(
        "line_3h",
        "line_3h",
        listOf(GridCell(0, 0), GridCell(0, 1), GridCell(0, 2)),
        3
    )

    private fun createMutedViewModel(): GamePlayViewModel {
        val sound = SoundEngine().apply { setPreferences(musicOn = false, sfxOn = false) }
        return GamePlayViewModel(sound)
    }

    // -------------------------------------------------------------------------
    // 1. CATALOG & THEME PRESERVATION TESTS
    // -------------------------------------------------------------------------

    @Test
    fun catalogPreservesAllOriginalThemesAndCoversAllNineCoinShopCategories() {
        val categories = CosmeticCatalog.defaultShopCategories()
        val expectedKeys = setOf(
            "THEME", "BLOCK_SKIN", "BOARD_BACKGROUND", "SPECIAL_EFFECT",
            "GAME_BOOSTER", "AVATAR", "PROFILE_FRAME", "BADGE", "EXCLUSIVE"
        )
        assertEquals(expectedKeys, categories.map { it.categoryKey }.toSet())

        val catalog = CosmeticCatalog.defaultFullShopCatalog()
        val itemIds = catalog.map { it.id }
        assertEquals("Every ShopItem must have a unique ID", itemIds.size, itemIds.toSet().size)

        // All 5 original themes preserved + Dark, Neon, Special Edition themes
        val themeIds = BlockThemes.allThemes.map { it.id }.toSet()
        assertTrue(themeIds.containsAll(setOf(
            "simple_classic", "pastel_macaron", "pastel_sakura", "pastel_aurora", "pastel_velvet",
            "dark_obsidian_gold", "neon_cyber_pulse", "special_galactic_prism"
        )))
        assertEquals(0L, catalog.first { it.id == "theme_simple_classic" }.priceCoins)

        // All required Block Skins (Neon, Diamond, Gold, Rainbow, Crystal, Fire, Ice, Special 3D)
        val skinKeys = CosmeticCatalog.blockSkins.map { it.key }.toSet()
        assertTrue(skinKeys.containsAll(setOf(
            "skin_neon", "skin_diamond", "skin_gold", "skin_rainbow",
            "skin_crystal", "skin_fire", "skin_ice", "skin_hologram_3d",
            "skin_royal_emerald_ex", "skin_cosmic_eclipse_ex"
        )))

        // Board Backgrounds including Animated backgrounds
        val animatedBgs = CosmeticCatalog.boardBackgrounds.filter { it.animation != BoardBackgroundAnimation.NONE }
        assertTrue("Expected at least 3 animated board backgrounds", animatedBgs.size >= 3)

        // Special Effects (Glow, Fire, Lightning, Rainbow, Particles, Line-Clear, Placement)
        assertTrue(CosmeticCatalog.specialEffects.size >= 8)

        // All 4 real Game Boosters are marked consumable with pack quantities
        val boosters = catalog.filter { it.category == "GAME_BOOSTER" }
        assertEquals(
            setOf("HAMMER", "UNDO", "SHUFFLE", "ROW_BLASTER"),
            boosters.map { it.subType }.toSet()
        )
        assertTrue(boosters.all { it.consumable && it.boosterPackQty >= 2L })

        // Profile items (Avatars, Frames, Badges) & Exclusive Limited Edition items
        assertTrue(catalog.any { it.category == "AVATAR" })
        assertTrue(catalog.any { it.category == "PROFILE_FRAME" })
        assertTrue(catalog.any { it.category == "BADGE" })
        assertTrue(catalog.count { it.category == "EXCLUSIVE" && it.limitedEdition } >= 4)
    }

    // -------------------------------------------------------------------------
    // 2. DYNAMIC PRICING, PROMOTIONS & WALLET DEDUCTION ENGINE TESTS
    // -------------------------------------------------------------------------

    @Test
    fun pricingEngineReadsLivePricesAppliesActivePromosAndRevertsAfterExpiry() {
        val now = 1_700_000_000_000L
        val baseItem = ShopItem(
            id = "skin_neon_blocks",
            name = "Neon Blocks",
            category = "BLOCK_SKIN",
            priceCoins = 400L,
            promoActive = true,
            promoPriceCoins = 300L,
            promoStartMillis = now - 60_000L,
            promoEndMillis = now + 60_000L,
            promoLabel = "FLASH SALE",
            available = true
        )

        // Active inline promo reduces 400 -> 300 (-25%)
        val duringInline = ShopPricingEngine.resolvePrice(baseItem, emptyList(), nowMillis = now)
        assertEquals(400L, duringInline.basePriceCoins)
        assertEquals(300L, duringInline.effectivePriceCoins)
        assertTrue(duringInline.promoApplied)
        assertEquals(25, duringInline.discountPercent)

        // Campaign category offer (50% off BLOCK_SKIN -> 200) beats inline 300
        val categoryCampaign = PromotionalOffer(
            id = "promo_skins_50",
            title = "Weekend Skin Fest",
            categoryKey = "BLOCK_SKIN",
            discountPercent = 50L,
            startTimeMillis = now - 10_000L,
            endTimeMillis = now + 10_000L,
            active = true
        )
        val withCampaign = ShopPricingEngine.resolvePrice(baseItem, listOf(categoryCampaign), nowMillis = now)
        assertEquals(200L, withCampaign.effectivePriceCoins)
        assertEquals(50, withCampaign.discountPercent)
        assertEquals("promo_skins_50", withCampaign.promoOfferId)

        // After promoEndMillis passes, price automatically reverts to live base price (400)
        val afterExpiry = ShopPricingEngine.resolvePrice(baseItem, listOf(categoryCampaign), nowMillis = now + 120_000L)
        assertEquals(400L, afterExpiry.effectivePriceCoins)
        assertFalse(afterExpiry.promoApplied)

        // Dynamic Admin price change (400 -> 650) immediately reflects without code edits
        val repriced = baseItem.copy(priceCoins = 650L, promoActive = false)
        val liveResolved = ShopPricingEngine.resolvePrice(repriced, emptyList(), nowMillis = now)
        assertEquals(650L, liveResolved.effectivePriceCoins)
    }

    @Test
    fun purchaseValidationBlocksDisabledDuplicateAndInsufficientBalanceWhileAllowingConsumables() {
        val skin = CosmeticCatalog.defaultFullShopCatalog().first { it.id == "skin_diamond_blocks" }
        val skinPrice = ShopPricingEngine.resolvePrice(skin, emptyList())

        // Disabled item strictly blocked
        val disabledRes = ShopPricingEngine.validatePurchase(
            item = skin.copy(available = false),
            resolvedPrice = skinPrice,
            totalUserCoins = 5_000L,
            ownedItemIds = emptySet(),
            unlockedThemeIds = emptySet()
        )
        assertTrue(disabledRes is PurchaseValidationResult.Denied)

        // Duplicate non-consumable strictly blocked
        val dupRes = ShopPricingEngine.validatePurchase(
            item = skin,
            resolvedPrice = skinPrice,
            totalUserCoins = 5_000L,
            ownedItemIds = setOf(skin.id),
            unlockedThemeIds = emptySet()
        )
        assertTrue(dupRes is PurchaseValidationResult.Denied)

        // Insufficient Coins strictly blocked with shortfall
        val poorRes = ShopPricingEngine.validatePurchase(
            item = skin,
            resolvedPrice = skinPrice,
            totalUserCoins = 100L,
            ownedItemIds = emptySet(),
            unlockedThemeIds = emptySet()
        )
        assertTrue(poorRes is PurchaseValidationResult.Denied)

        // Valid purchase allowed
        val okRes = ShopPricingEngine.validatePurchase(
            item = skin,
            resolvedPrice = skinPrice,
            totalUserCoins = skinPrice.effectivePriceCoins,
            ownedItemIds = emptySet(),
            unlockedThemeIds = emptySet()
        )
        assertTrue(okRes is PurchaseValidationResult.Allowed)

        // Consumable booster pack CAN be re-purchased even if already bought before
        val hammer = CosmeticCatalog.defaultFullShopCatalog().first { it.id == "booster_hammer_pack" }
        val hammerPrice = ShopPricingEngine.resolvePrice(hammer, emptyList())
        val repeatBooster = ShopPricingEngine.validatePurchase(
            item = hammer,
            resolvedPrice = hammerPrice,
            totalUserCoins = 500L,
            ownedItemIds = setOf(hammer.id),
            unlockedThemeIds = emptySet()
        )
        assertTrue(repeatBooster is PurchaseValidationResult.Allowed)
    }

    @Test
    fun coinDeductionSpendsEarnedCoinsFirstAndPreventsNegativeBalances() {
        // 1. Fully covered by Earned Coins
        val earnedOnly = ShopPricingEngine.computeCoinDeduction(
            priceCoins = 200L, earnedCoins = 350L, purchasedCoins = 500L
        ).getOrThrow()
        assertEquals(150L, earnedOnly.newEarnedCoins)
        assertEquals(500L, earnedOnly.newPurchasedCoins)
        assertEquals("EARNED", earnedOnly.coinSourceLabel)

        // 2. Mixed spend (Earned depleted to 0, remainder taken from Purchased)
        val mixed = ShopPricingEngine.computeCoinDeduction(
            priceCoins = 400L, earnedCoins = 150L, purchasedCoins = 500L
        ).getOrThrow()
        assertEquals(0L, mixed.newEarnedCoins)
        assertEquals(250L, mixed.newPurchasedCoins)
        assertEquals("MIXED", mixed.coinSourceLabel)

        // 3. Insufficient total fails cleanly (never negative!)
        val insufficient = ShopPricingEngine.computeCoinDeduction(
            priceCoins = 800L, earnedCoins = 150L, purchasedCoins = 500L
        )
        assertTrue(insufficient.isFailure)
    }

    // -------------------------------------------------------------------------
    // 3. REAL GAMEPLAY BOOSTER MECHANICS TESTS (HAMMER, UNDO, SHUFFLE, BLASTER)
    // -------------------------------------------------------------------------

    @Test
    fun hammerBoosterRejectsEmptyCellSmashesRadiusAndEnforcesPerGameLimit() {
        val vm = createMutedViewModel()
        val grid = BlockPuzzleEngine.emptyGrid().toMutableList()
        // Cross pattern centered at (4, 4)
        grid[BlockPuzzleEngine.index(4, 4)] = 2
        grid[BlockPuzzleEngine.index(3, 4)] = 3
        grid[BlockPuzzleEngine.index(5, 4)] = 4
        grid[BlockPuzzleEngine.index(4, 3)] = 5
        grid[BlockPuzzleEngine.index(4, 5)] = 6
        grid[BlockPuzzleEngine.index(0, 0)] = 1

        vm.startNewGame(mode = GameMode.CLASSIC, customGrid = grid)

        // Empty cell rejected without burning a charge
        assertFalse(vm.canHammerAt(9, 9))
        assertTrue(vm.executeHammerAt(9, 9, ownedCount = 3L, maxPerGame = 1, powerRadius = 1).isFailure)
        assertEquals(0, vm.state.value.boostersUsedInGame[BoosterType.HAMMER] ?: 0)

        // Valid occupied cell (4,4) with radius=1 smashes center + 4 orthogonal neighbors (5 total)
        val smash = vm.executeHammerAt(4, 4, ownedCount = 3L, maxPerGame = 1, powerRadius = 1)
        assertTrue(smash.isSuccess)
        assertEquals(5, smash.getOrNull())
        assertEquals(0, vm.state.value.grid[BlockPuzzleEngine.index(4, 4)])
        assertEquals(0, vm.state.value.grid[BlockPuzzleEngine.index(3, 4)])
        assertEquals(0, vm.state.value.grid[BlockPuzzleEngine.index(4, 5)])
        assertEquals(1, vm.state.value.grid[BlockPuzzleEngine.index(0, 0)]) // distant block untouched
        assertEquals(1, vm.state.value.boostersUsedInGame[BoosterType.HAMMER])

        // Per-game limit (maxPerGame = 1) blocks second Hammer use in same round
        val blocked = vm.executeHammerAt(0, 0, ownedCount = 2L, maxPerGame = 1, powerRadius = 1)
        assertTrue(blocked.isFailure)
    }

    @Test
    fun undoBoosterRestoresExactBoardTrayScoreAndCanRescueGameOver() {
        val vm = createMutedViewModel()
        // Build a board where every row and every column has exactly 2 empty cells spaced 5 apart:
        // zero pre-filled lines, zero horizontal 3-cell gaps -> placing dot at (0,0) clears nothing
        // and line3H cannot fit anywhere -> immediate Game Over!
        val tightGrid = MutableList(BOARD_SIZE * BOARD_SIZE) { 1 }
        for (r in 0 until BOARD_SIZE) {
            tightGrid[BlockPuzzleEngine.index(r, r)] = 0
            tightGrid[BlockPuzzleEngine.index(r, (r + 5) % BOARD_SIZE)] = 0
        }

        vm.startNewGame(
            mode = GameMode.CLASSIC,
            permanentLevelNumber = 1L,
            permanentLevelLines = 50L,
            customGrid = tightGrid,
            customTray = listOf(
                TrayPiece(0, dot, false),
                TrayPiece(1, line3H, false),
                TrayPiece(2, line3H, false)
            )
        )
        val gridBefore = vm.state.value.grid
        assertFalse(vm.state.value.canUndo)

        // Place dot at (0, 0) -> triggers Game Over because line3H needs 3 horizontal cells
        assertTrue(vm.tryPlacePiece(0, 0, 0))
        assertTrue(vm.state.value.isGameOver)
        assertTrue(vm.state.value.canUndo)
        assertEquals(1, vm.state.value.grid[BlockPuzzleEngine.index(0, 0)])

        // Execute Undo Booster -> rescues Game Over and restores exact board & tray!
        val undoRes = vm.executeUndo(ownedCount = 2L)
        assertTrue(undoRes.isSuccess)
        assertFalse(vm.state.value.isGameOver)
        assertEquals(gridBefore, vm.state.value.grid)
        assertFalse(vm.state.value.tray[0].used)
        assertEquals(50L, vm.state.value.effectivePermanentLevelLines)
        assertEquals(1, vm.state.value.boostersUsedInGame[BoosterType.UNDO])
    }

    @Test
    fun shuffleAndRowBlasterBoostersOperateOnLiveGameplayState() {
        val vm = createMutedViewModel()
        val grid = BlockPuzzleEngine.emptyGrid().toMutableList()
        // Populate partial row 2 (cells 0..5 filled)
        for (c in 0 until 6) grid[BlockPuzzleEngine.index(2, c)] = 4

        vm.startNewGame(
            mode = GameMode.CLASSIC,
            permanentLevelNumber = 2L,
            permanentLevelLines = 100L,
            customGrid = grid,
            customTray = listOf(
                TrayPiece(0, line3H, false),
                TrayPiece(1, line3H, true), // already used slot must stay used
                TrayPiece(2, line3H, false)
            )
        )

        // 1. Shuffle replaces unused tray slots while keeping grid and used flags intact
        val gridBeforeShuffle = vm.state.value.grid
        assertTrue(vm.executeShuffle(ownedCount = 2L).isSuccess)
        assertEquals(gridBeforeShuffle, vm.state.value.grid)
        assertTrue(vm.state.value.tray[1].used)
        assertEquals(1, vm.state.value.boostersUsedInGame[BoosterType.SHUFFLE])

        // 2. Line Blaster rejects completely empty row 9
        assertFalse(vm.canBlastRow(9))
        assertTrue(vm.executeRowBlasterAt(9, ownedCount = 2L).isFailure)

        // 3. Line Blaster blasts occupied row 2 -> clears all 10 cells, +10 score, +1 line, +1 coin, +1 permanent level line
        assertTrue(vm.canBlastRow(2))
        val blastRes = vm.executeRowBlasterAt(2, ownedCount = 2L)
        assertTrue(blastRes.isSuccess)
        for (c in 0 until BOARD_SIZE) {
            assertEquals(0, vm.state.value.grid[BlockPuzzleEngine.index(2, c)])
        }
        assertEquals(10L, vm.state.value.score)
        assertEquals(1L, vm.state.value.linesCleared)
        assertEquals(1L, vm.state.value.coinsEarned)
        assertEquals(101L, vm.state.value.effectivePermanentLevelLines)
        assertEquals(1, vm.state.value.boostersUsedInGame[BoosterType.ROW_BLASTER])
    }

    // -------------------------------------------------------------------------
    // 4. ROBOLECTRIC COMPOSE UI TESTS — COIN SHOP & GAMEPLAY BOOSTER BAR
    // -------------------------------------------------------------------------

    @Test
    fun coinShopUiReflectsLiveDynamicPricesFiltersCategoriesAndPreservesVaultedOwnedItems() {
        val defaultCatalog = CosmeticCatalog.defaultFullShopCatalog()
        var liveCatalog by mutableStateOf(defaultCatalog)
        var purchasedByUser by mutableStateOf<ShopItem?>(null)

        // Vaulted item: player permanently purchased an exclusive item that Admin later disabled/removed
        val vaultedReceipt = PurchasedItem(
            id = "uid1__retired_obsidian_crown",
            userId = "uid1",
            itemId = "retired_obsidian_crown",
            itemName = "Retired Obsidian Crown",
            category = "BADGE",
            assetKey = "badge_prism_crown",
            pricePaidCoins = 500L,
            consumable = false
        )

        composeTestRule.setContent {
            PrismBlockTheme {
                CoinShopContent(
                    totalCoins = 2_500L,
                    earnedCoins = 1_500L,
                    purchasedCoins = 1_000L,
                    unlockedThemeIds = setOf("simple_classic"),
                    legacySelectedThemeId = "simple_classic",
                    categories = CosmeticCatalog.defaultShopCategories(),
                    shopItems = liveCatalog,
                    activePromotions = emptyList(),
                    inventory = PlayerInventory(
                        userId = "uid1",
                        ownedItemIds = listOf("theme_simple_classic", "retired_obsidian_crown"),
                        hammerCount = 4L,
                        undoCount = 2L,
                        shuffleCount = 3L,
                        rowBlasterCount = 1L
                    ),
                    purchasedItems = listOf(vaultedReceipt),
                    shopTransactions = listOf(
                        ShopTransaction(
                            id = "tx_1",
                            userId = "uid1",
                            itemId = "retired_obsidian_crown",
                            itemName = "Retired Obsidian Crown",
                            category = "BADGE",
                            pricePaidCoins = 500L,
                            originalPriceCoins = 500L,
                            coinSource = "EARNED",
                            status = "COMPLETED"
                        )
                    ),
                    isBusy = false,
                    onBack = {},
                    onPurchaseItem = { purchasedByUser = it },
                    onEquipShopItem = {},
                    onEquipPurchasedItem = { _, _ -> },
                    onUnequipSlot = { _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        // 1. Coin Balance Pill & Featured section render cleanly
        composeTestRule.onNodeWithTag("shop_coin_balance_pill").assertIsDisplayed()
        composeTestRule.onNodeWithTag("shop_section_featured").assertIsDisplayed()

        // 2. Filter to Block Skins -> check initial live price (320 Coins) for Neon Blocks
        composeTestRule.onNodeWithTag("shop_cat_block_skin").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shop_price_skin_neon_blocks", useUnmergedTree = true).assertTextEquals("320 Coins")

        // 3. Simulate live Firestore Admin price update (320 -> 777 Coins) -> UI updates immediately!
        liveCatalog = liveCatalog.map {
            if (it.id == "skin_neon_blocks") it.copy(priceCoins = 777L) else it
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shop_price_skin_neon_blocks", useUnmergedTree = true).assertTextEquals("777 Coins")

        // 4. Open Preview modal and tap Confirm Purchase -> triggers real onPurchaseItem callback
        composeTestRule.onNodeWithTag("buy_item_skin_neon_blocks").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("confirm_purchase_button").assertIsDisplayed().performClick()
        composeTestRule.waitForIdle()
        assertEquals("skin_neon_blocks", purchasedByUser?.id)
        assertEquals(777L, purchasedByUser?.priceCoins)

        // 5. Switch to My Collection tab -> Vaulted/removed item is STILL permanently shown & equippable!
        composeTestRule.onNodeWithTag("shop_tab_collection").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shop_my_collection_list")
            .performScrollToNode(hasTestTag("collection_item_retired_obsidian_crown"))
        composeTestRule.onNodeWithTag("collection_item_retired_obsidian_crown").assertIsDisplayed()
        composeTestRule.onNodeWithText("VAULTED • YOURS FOREVER").assertIsDisplayed()
        composeTestRule.onNodeWithTag("collection_equip_retired_obsidian_crown").assertIsDisplayed()

        // 6. Switch to Purchase History tab -> recorded receipt is displayed
        composeTestRule.onNodeWithTag("shop_tab_history").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("shop_purchase_history_list").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retired Obsidian Crown").assertIsDisplayed()
    }

    @Test
    fun gameplayBoardStageDisplaysInteractiveBoosterBarAndTargetingBanner() {
        val vm = createMutedViewModel()
        vm.startNewGame(
            mode = GameMode.CLASSIC,
            permanentLevelNumber = 1L,
            permanentLevelLines = 125L
        )

        composeTestRule.setContent {
            val state by vm.state.collectAsState()
            PrismBlockTheme {
                GameplayBoardStage(
                    gameState = state,
                    activeTheme = BlockThemes.neonCyberPulse,
                    blockSkinStyle = CosmeticCatalog.blockSkinByKey("skin_diamond").renderStyle,
                    boardBackground = CosmeticCatalog.backgroundByKey("bg_anim_starfield"),
                    specialEffect = CosmeticCatalog.effectByKey("fx_lightning"),
                    hammerCount = 3L,
                    undoCount = 2L,
                    shuffleCount = 4L,
                    rowBlasterCount = 1L,
                    onTryPlacePiece = { s, r, c -> vm.tryPlacePiece(s, r, c) },
                    onBoosterButtonClick = { type ->
                        if (type == BoosterType.HAMMER || type == BoosterType.ROW_BLASTER) {
                            vm.toggleBoosterTargeting(type, ownedCount = 3L)
                        }
                    },
                    onCancelBoosterMode = { vm.cancelBoosterTargeting() }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Both progress bars, 10x10 board, and all 4 real Booster buttons are displayed simultaneously
        composeTestRule.onNodeWithTag("dual_progress_bars_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("puzzle_board_10x10").assertIsDisplayed()
        composeTestRule.onNodeWithTag("gameplay_booster_bar").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booster_btn_hammer").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booster_btn_undo").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booster_btn_shuffle").assertIsDisplayed()
        composeTestRule.onNodeWithTag("booster_btn_row_blaster").assertIsDisplayed()

        // Tapping Hammer enters interactive board-targeting mode with Cancel button
        composeTestRule.onNodeWithTag("booster_btn_hammer").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("booster_targeting_banner").assertIsDisplayed()
        assertEquals(BoosterType.HAMMER, vm.state.value.activeBoosterMode)

        // Tapping Cancel cleanly exits targeting mode
        composeTestRule.onNodeWithTag("booster_cancel_button").performClick()
        composeTestRule.waitForIdle()
        assertNotEquals(BoosterType.HAMMER, vm.state.value.activeBoosterMode)
    }
}
