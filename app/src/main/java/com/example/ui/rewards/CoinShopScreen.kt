package com.example.ui.rewards

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.BoosterType
import com.example.data.model.PlayerInventory
import com.example.data.model.PromotionalOffer
import com.example.data.model.PurchasedItem
import com.example.data.model.ShopCategory
import com.example.data.model.ShopCategoryKey
import com.example.data.model.ShopItem
import com.example.data.model.ShopTransaction
import com.example.game.BlockThemes
import com.example.game.CosmeticCatalog
import com.example.game.ResolvedShopPrice
import com.example.game.ShopPricingEngine
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.ScreenHeader
import com.example.ui.components.StatusPill
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AppViewModel

enum class CoinShopTab(val label: String) {
    STORE("Store"),
    COLLECTION("My Collection"),
    HISTORY("History")
}

@Composable
fun CoinShopScreen(
    appViewModel: AppViewModel,
    onBack: () -> Unit
) {
    val user by appViewModel.userAccount.collectAsStateWithLifecycle()
    val categories by appViewModel.shopCategories.collectAsStateWithLifecycle()
    val items by appViewModel.shopItems.collectAsStateWithLifecycle()
    val promotions by appViewModel.activePromotions.collectAsStateWithLifecycle()
    val inventory by appViewModel.playerInventory.collectAsStateWithLifecycle()
    val purchasedItems by appViewModel.purchasedItems.collectAsStateWithLifecycle()
    val shopTransactions by appViewModel.shopTransactions.collectAsStateWithLifecycle()
    val isBusy by appViewModel.isBusy.collectAsStateWithLifecycle()

    CoinShopContent(
        totalCoins = user?.totalCoins ?: 0L,
        earnedCoins = user?.earnedCoins ?: 0L,
        purchasedCoins = user?.purchasedCoins ?: 0L,
        unlockedThemeIds = user?.unlockedThemeIds.orEmpty().toSet(),
        legacySelectedThemeId = user?.selectedThemeId ?: BlockThemes.simpleClassic.id,
        categories = categories,
        shopItems = items,
        activePromotions = promotions,
        inventory = inventory,
        purchasedItems = purchasedItems,
        shopTransactions = shopTransactions,
        isBusy = isBusy,
        onBack = onBack,
        onPurchaseItem = { appViewModel.purchaseShopItem(it) },
        onEquipShopItem = { appViewModel.equipShopItem(it) },
        onEquipPurchasedItem = { p, sub -> appViewModel.equipPurchasedItem(p, sub) },
        onUnequipSlot = { cat, sub -> appViewModel.unequipCosmeticSlot(cat, sub) }
    )
}

/**
 * State-hoisted, pure testable Premium Coin Shop UI.
 * All item prices are read live from [shopItems] & [activePromotions] via [ShopPricingEngine.resolvePrice]
 * (zero hardcoded UI prices!).
 */
@Composable
fun CoinShopContent(
    totalCoins: Long,
    earnedCoins: Long,
    purchasedCoins: Long,
    unlockedThemeIds: Set<String>,
    legacySelectedThemeId: String,
    categories: List<ShopCategory>,
    shopItems: List<ShopItem>,
    activePromotions: List<PromotionalOffer>,
    inventory: PlayerInventory,
    purchasedItems: List<PurchasedItem>,
    shopTransactions: List<ShopTransaction>,
    isBusy: Boolean,
    onBack: () -> Unit,
    onPurchaseItem: (ShopItem) -> Unit,
    onEquipShopItem: (ShopItem) -> Unit,
    onEquipPurchasedItem: (PurchasedItem, String) -> Unit,
    onUnequipSlot: (String, String) -> Unit,
    nowMillis: Long = System.currentTimeMillis()
) {
    var activeTab by remember { mutableStateOf(CoinShopTab.STORE) }
    var selectedCategoryKey by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }
    var inspectedItem by remember { mutableStateOf<ShopItem?>(null) }

    val enabledCategories = remember(categories) {
        (if (categories.isNotEmpty()) categories else CosmeticCatalog.defaultShopCategories())
            .filter { it.enabled }
            .sortedBy { it.sortOrder }
    }
    val enabledCategoryKeys = remember(enabledCategories) {
        enabledCategories.map { it.categoryKey }.toSet()
    }

    val liveStoreItems = remember(shopItems, enabledCategoryKeys) {
        (if (shopItems.isNotEmpty()) shopItems else CosmeticCatalog.defaultFullShopCatalog())
            .filter { it.available && (it.category in enabledCategoryKeys || it.category == "ITEM") }
            .sortedBy { it.sortOrder }
    }

    val ownedItemIds = remember(inventory.ownedItemIds, purchasedItems) {
        (inventory.ownedItemIds + purchasedItems.map { it.itemId } + "theme_simple_classic").toSet()
    }

    fun isOwned(item: ShopItem): Boolean {
        if (item.consumable) return false
        return item.id in ownedItemIds ||
            (item.category == "THEME" && (item.priceCoins == 0L || item.themeKey in unlockedThemeIds))
    }

    fun isEquipped(item: ShopItem): Boolean {
        if (item.consumable) return false
        val key = item.resolvedAssetKey
        val activeTheme = inventory.equippedThemeId.ifBlank { legacySelectedThemeId }
        return when (item.category) {
            "THEME" -> activeTheme == key
            "BLOCK_SKIN" -> inventory.equippedBlockSkinId == key
            "BOARD_BACKGROUND" -> inventory.equippedBackgroundId == key
            "SPECIAL_EFFECT" -> inventory.equippedEffectId == key
            "AVATAR" -> inventory.equippedAvatarId == key
            "PROFILE_FRAME" -> inventory.equippedFrameId == key
            "BADGE", "ITEM" -> inventory.equippedBadgeId == key
            "EXCLUSIVE" -> when (item.subType) {
                "BLOCK_SKIN" -> inventory.equippedBlockSkinId == key
                "BOARD_BACKGROUND" -> inventory.equippedBackgroundId == key
                "SPECIAL_EFFECT" -> inventory.equippedEffectId == key
                "AVATAR" -> inventory.equippedAvatarId == key
                "PROFILE_FRAME" -> inventory.equippedFrameId == key
                "BADGE" -> inventory.equippedBadgeId == key
                "THEME" -> activeTheme == key
                else -> inventory.equippedExclusiveId == key
            }
            else -> false
        }
    }

    val filteredStoreItems = remember(liveStoreItems, selectedCategoryKey, searchQuery) {
        val q = searchQuery.trim().lowercase()
        liveStoreItems.filter { item ->
            val catMatch = selectedCategoryKey == "ALL" ||
                item.category == selectedCategoryKey ||
                (selectedCategoryKey == "BADGE" && item.category == "ITEM")
            val searchMatch = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.description.lowercase().contains(q) ||
                item.category.lowercase().contains(q) ||
                item.subType.lowercase().contains(q)
            catMatch && searchMatch
        }
    }

    val featuredItems = remember(liveStoreItems) { liveStoreItems.filter { it.featured }.take(10) }
    val newArrivalItems = remember(liveStoreItems) { liveStoreItems.filter { it.newArrival }.take(10) }
    val limitedItems = remember(liveStoreItems) { liveStoreItems.filter { it.limitedEdition || it.category == "EXCLUSIVE" }.take(10) }

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenHeader(
            title = "Premium Coin Shop",
            subtitle = "Themes, 3D Skins, Animated Boards, FX, Boosters & Profile Gear",
            onBack = onBack,
            trailing = {
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = PrismGold.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.6f)),
                    modifier = Modifier.testTag("shop_coin_balance_pill")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = "Coin balance", tint = PrismGold, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "%,d".format(totalCoins),
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily),
                            color = PrismGold
                        )
                    }
                }
            }
        )

        // Top View Switcher: Store | My Collection | Purchase History
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CoinShopTab.entries.forEach { tab ->
                val selected = activeTab == tab
                FilterChip(
                    selected = selected,
                    onClick = { activeTab = tab },
                    leadingIcon = {
                        Icon(
                            imageVector = when (tab) {
                                CoinShopTab.STORE -> Icons.Filled.Storefront
                                CoinShopTab.COLLECTION -> Icons.Filled.CollectionsBookmark
                                CoinShopTab.HISTORY -> Icons.AutoMirrored.Filled.ReceiptLong
                            },
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(tab.label, maxLines = 1) },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("shop_tab_${tab.name.lowercase()}"),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrismCyan,
                        selectedLabelColor = Color(0xFF041A24),
                        selectedLeadingIconColor = Color(0xFF041A24)
                    )
                )
            }
        }

        when (activeTab) {
            CoinShopTab.STORE -> {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it.take(40) },
                            placeholder = { Text("Search skins, themes, boosters, effects...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search shop") },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("shop_search_input")
                        )
                    }

                    // Dynamic Category Filter Strip
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = selectedCategoryKey == "ALL",
                                    onClick = { selectedCategoryKey = "ALL" },
                                    label = { Text("All (${liveStoreItems.size})") },
                                    modifier = Modifier.testTag("shop_cat_all"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrismGold,
                                        selectedLabelColor = Color(0xFF241A00)
                                    )
                                )
                            }
                            items(enabledCategories, key = { it.categoryKey }) { cat ->
                                val count = liveStoreItems.count {
                                    it.category == cat.categoryKey || (cat.categoryKey == "BADGE" && it.category == "ITEM")
                                }
                                FilterChip(
                                    selected = selectedCategoryKey == cat.categoryKey,
                                    onClick = { selectedCategoryKey = cat.categoryKey },
                                    label = { Text("${cat.displayName} ($count)") },
                                    modifier = Modifier.testTag("shop_cat_${cat.categoryKey.lowercase()}"),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = parseHexColor(cat.accentHex, PrismViolet),
                                        selectedLabelColor = Color(0xFF0A0716)
                                    )
                                )
                            }
                        }
                    }

                    // Curated Storefronts (Featured, New Arrivals, Limited Edition) when browsing All
                    val showCurated = selectedCategoryKey == "ALL" && searchQuery.isBlank()
                    if (showCurated && featuredItems.isNotEmpty()) {
                        item {
                            ShopSectionHeader("FEATURED SHOWCASE", "Top 3D designs & power-ups", PrismGold, "shop_section_featured")
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(featuredItems, key = { "feat_${it.id}" }) { item ->
                                    val price = ShopPricingEngine.resolvePrice(item, activePromotions, nowMillis)
                                    CompactShowcaseCard(
                                        item = item,
                                        resolvedPrice = price,
                                        owned = isOwned(item),
                                        equipped = isEquipped(item),
                                        onClick = { inspectedItem = item }
                                    )
                                }
                            }
                        }
                    }

                    if (showCurated && newArrivalItems.isNotEmpty()) {
                        item {
                            ShopSectionHeader("NEW ARRIVALS", "Fresh additions to Prism Block 3D", PrismCyan, "shop_section_new_arrivals")
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(newArrivalItems, key = { "new_${it.id}" }) { item ->
                                    val price = ShopPricingEngine.resolvePrice(item, activePromotions, nowMillis)
                                    CompactShowcaseCard(
                                        item = item,
                                        resolvedPrice = price,
                                        owned = isOwned(item),
                                        equipped = isEquipped(item),
                                        onClick = { inspectedItem = item }
                                    )
                                }
                            }
                        }
                    }

                    if (showCurated && limitedItems.isNotEmpty()) {
                        item {
                            ShopSectionHeader("LIMITED EDITION & EXCLUSIVE", "Collector 3D designs & seasonal drops", PrismViolet, "shop_section_limited")
                            Spacer(Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(limitedItems, key = { "lim_${it.id}" }) { item ->
                                    val price = ShopPricingEngine.resolvePrice(item, activePromotions, nowMillis)
                                    CompactShowcaseCard(
                                        item = item,
                                        resolvedPrice = price,
                                        owned = isOwned(item),
                                        equipped = isEquipped(item),
                                        onClick = { inspectedItem = item }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        val headerTitle = if (selectedCategoryKey == "ALL") {
                            if (searchQuery.isBlank()) "ALL STORE ITEMS (${filteredStoreItems.size})" else "SEARCH RESULTS (${filteredStoreItems.size})"
                        } else {
                            val label = enabledCategories.firstOrNull { it.categoryKey == selectedCategoryKey }?.displayName ?: selectedCategoryKey
                            "${label.uppercase()} (${filteredStoreItems.size})"
                        }
                        Text(
                            text = headerTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (filteredStoreItems.isEmpty()) {
                        item {
                            EmptyStatePanel(
                                icon = Icons.Filled.ShoppingBag,
                                title = "No Matching Items",
                                description = "Try clearing your search or switching to another Coin Shop category."
                            )
                        }
                    } else {
                        items(filteredStoreItems, key = { it.id }) { item ->
                            val resolved = ShopPricingEngine.resolvePrice(item, activePromotions, nowMillis)
                            val boosterOwned = if (item.consumable) {
                                BoosterType.fromWire(item.subType)?.let { inventory.boosterCount(it) } ?: 0L
                            } else 0L
                            StoreItemRowCard(
                                item = item,
                                resolvedPrice = resolved,
                                owned = isOwned(item),
                                equipped = isEquipped(item),
                                boosterOwnedCount = boosterOwned,
                                totalCoins = totalCoins,
                                isBusy = isBusy,
                                onInspect = { inspectedItem = item },
                                onEquip = { onEquipShopItem(item) }
                            )
                        }
                    }
                }
            }

            CoinShopTab.COLLECTION -> {
                MyCollectionTabContent(
                    shopItems = shopItems.ifEmpty { CosmeticCatalog.defaultFullShopCatalog() },
                    purchasedItems = purchasedItems,
                    inventory = inventory,
                    unlockedThemeIds = unlockedThemeIds,
                    legacySelectedThemeId = legacySelectedThemeId,
                    isBusy = isBusy,
                    onEquipShopItem = onEquipShopItem,
                    onEquipPurchasedItem = onEquipPurchasedItem,
                    onUnequipSlot = onUnequipSlot,
                    onBuyMoreBoosters = {
                        selectedCategoryKey = "GAME_BOOSTER"
                        activeTab = CoinShopTab.STORE
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            CoinShopTab.HISTORY -> {
                PurchaseHistoryTabContent(
                    transactions = shopTransactions,
                    earnedCoins = earnedCoins,
                    purchasedCoins = purchasedCoins,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Item Preview & Purchase Confirmation Modal
    inspectedItem?.let { item ->
        val resolved = ShopPricingEngine.resolvePrice(item, activePromotions, nowMillis)
        val owned = isOwned(item)
        val equipped = isEquipped(item)
        val canAfford = totalCoins >= resolved.effectivePriceCoins
        val boosterType = if (item.consumable) BoosterType.fromWire(item.subType) else null

        AlertDialog(
            onDismissRequest = { inspectedItem = null },
            containerColor = DeepSurface,
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShopItemVisualPreview(item = item, size = 56.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            text = ShopCategoryKey.fromWire(item.category).displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = parseHexColor(item.accentHex, PrismCyan)
                        )
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(item.description, style = MaterialTheme.typography.bodySmall, color = Color.White)

                    if (item.category == "THEME" && item.themeKey.isNotBlank()) {
                        Theme3DSwatchStrip(theme = BlockThemes.byId(item.themeKey))
                    }

                    if (boosterType != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CardSurface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "Pack Grants: +${item.boosterPackQty.coerceAtLeast(1L)} ${boosterType.label} charges",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = PrismGold
                                )
                                Text(
                                    "You currently own: ×${inventory.boosterCount(boosterType)} • Max ${item.boosterMaxPerGame.coerceAtLeast(1L)} uses/game",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = SurfaceBorder)

                    // Live Dynamic Price & Balance Summary
                    if (!owned || item.consumable) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live Store Price", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (resolved.promoApplied) {
                                    Text(
                                        text = "%,d".format(resolved.basePriceCoins),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = JetBrainsMonoFontFamily,
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = if (resolved.effectivePriceCoins == 0L) "FREE" else "%,d Coins".format(resolved.effectivePriceCoins),
                                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                                    color = if (resolved.effectivePriceCoins == 0L) PrismMint else PrismGold
                                )
                            }
                        }
                        if (resolved.promoApplied) {
                            StatusPill("${resolved.promoLabel} • -${resolved.discountPercent}% OFF", PrismCoral)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Your Coin Balance", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = "%,d → %,d Coins".format(totalCoins, (totalCoins - resolved.effectivePriceCoins).coerceAtLeast(0L)),
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = if (canAfford) PrismMint else PrismCoral
                            )
                        }
                        if (!canAfford) {
                            Text(
                                text = "You need %,d more Coins. Play 10×10 puzzles, claim Daily Rewards, or convert PKR in Wallet!"
                                    .format(resolved.effectivePriceCoins - totalCoins),
                                style = MaterialTheme.typography.labelSmall,
                                color = PrismCoral
                            )
                        }
                    } else {
                        StatusPill("PERMANENTLY OWNED IN YOUR COLLECTION", PrismMint)
                    }
                }
            },
            confirmButton = {
                when {
                    owned && !item.consumable && equipped -> {
                        if (item.category != "THEME") {
                            OutlinedButton(
                                onClick = {
                                    onUnequipSlot(item.category, item.subType)
                                    inspectedItem = null
                                },
                                enabled = !isBusy
                            ) {
                                Text("Unequip")
                            }
                        } else {
                            TextButton(onClick = { inspectedItem = null }) { Text("Equipped") }
                        }
                    }
                    owned && !item.consumable -> {
                        Button(
                            onClick = {
                                onEquipShopItem(item)
                                inspectedItem = null
                            },
                            enabled = !isBusy,
                            colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24)),
                            modifier = Modifier.testTag("confirm_equip_button")
                        ) {
                            Text("Equip Now", fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Button(
                            onClick = {
                                onPurchaseItem(item)
                                inspectedItem = null
                            },
                            enabled = !isBusy && canAfford,
                            colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00)),
                            modifier = Modifier.testTag("confirm_purchase_button")
                        ) {
                            Text(
                                text = if (canAfford) "Confirm Purchase" else "Not Enough Coins",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { inspectedItem = null }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun ShopSectionHeader(title: String, subtitle: String, accent: Color, tag: String) {
    Column(modifier = Modifier.testTag(tag)) {
        Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = accent)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CompactShowcaseCard(
    item: ShopItem,
    resolvedPrice: ResolvedShopPrice,
    owned: Boolean,
    equipped: Boolean,
    onClick: () -> Unit
) {
    val accent = parseHexColor(item.accentHex, PrismCyan)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.2.dp, if (equipped) PrismCyan else accent.copy(alpha = 0.55f)),
        modifier = Modifier
            .width(156.dp)
            .testTag("showcase_item_${item.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.fillMaxWidth()) {
                ShopItemVisualPreview(
                    item = item,
                    size = 72.dp,
                    modifier = Modifier.align(Alignment.Center)
                )
                if (resolvedPrice.promoApplied) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = PrismCoral,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "-${resolvedPrice.discountPercent}%",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                } else if (item.limitedEdition) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Limited edition",
                        tint = PrismGold,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = item.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = ShopCategoryKey.fromWire(item.category).shortLabel,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                maxLines = 1
            )
            Spacer(Modifier.height(6.dp))
            when {
                equipped -> StatusPill("EQUIPPED", PrismCyan)
                owned -> StatusPill("OWNED", PrismMint)
                else -> Text(
                    text = if (resolvedPrice.effectivePriceCoins == 0L) "FREE" else "%,d Coins".format(resolvedPrice.effectivePriceCoins),
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily),
                    fontWeight = FontWeight.Bold,
                    color = if (resolvedPrice.effectivePriceCoins == 0L) PrismMint else PrismGold
                )
            }
        }
    }
}

@Composable
private fun StoreItemRowCard(
    item: ShopItem,
    resolvedPrice: ResolvedShopPrice,
    owned: Boolean,
    equipped: Boolean,
    boosterOwnedCount: Long,
    totalCoins: Long,
    isBusy: Boolean,
    onInspect: () -> Unit,
    onEquip: () -> Unit
) {
    val accent = parseHexColor(item.accentHex, PrismCyan)
    Card(
        onClick = onInspect,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shop_item_${item.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepSurface),
        border = BorderStroke(
            width = if (equipped) 2.dp else 1.dp,
            color = when {
                equipped -> PrismCyan
                resolvedPrice.promoApplied -> PrismCoral.copy(alpha = 0.75f)
                item.limitedEdition -> PrismViolet.copy(alpha = 0.65f)
                else -> SurfaceBorder
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShopItemVisualPreview(item = item, size = 74.dp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (resolvedPrice.promoApplied) {
                        StatusPill("-${resolvedPrice.discountPercent}%", PrismCoral)
                    } else if (item.limitedEdition) {
                        StatusPill("LIMITED", PrismViolet)
                    } else if (item.newArrival) {
                        StatusPill("NEW", PrismCyan)
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        if (item.consumable) {
                            Text(
                                text = "Owned: ×$boosterOwnedCount • +${item.boosterPackQty.coerceAtLeast(1L)}/pack",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (resolvedPrice.promoApplied) {
                                Text(
                                    text = "%,d".format(resolvedPrice.basePriceCoins),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = JetBrainsMonoFontFamily,
                                        textDecoration = TextDecoration.LineThrough
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (resolvedPrice.effectivePriceCoins == 0L) "Free" else "%,d Coins".format(resolvedPrice.effectivePriceCoins),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (resolvedPrice.effectivePriceCoins == 0L) PrismMint else PrismGold,
                                modifier = Modifier.testTag("shop_price_${item.id}")
                            )
                        }
                    }

                    when {
                        equipped -> {
                            StatusPill("ACTIVE", PrismCyan)
                        }
                        owned && !item.consumable -> {
                            Button(
                                onClick = onEquip,
                                enabled = !isBusy,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24)),
                                modifier = Modifier.testTag("equip_item_${item.id}")
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Equip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                        else -> {
                            val canAfford = totalCoins >= resolvedPrice.effectivePriceCoins
                            Button(
                                onClick = onInspect,
                                enabled = !isBusy,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAfford) PrismGold else CardSurface,
                                    contentColor = if (canAfford) Color(0xFF241A00) else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("buy_item_${item.id}")
                            ) {
                                Icon(
                                    imageVector = if (resolvedPrice.promoApplied) Icons.Filled.LocalOffer else Icons.Filled.Lock,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = if (item.consumable) "Buy Pack" else "Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MyCollectionTabContent(
    shopItems: List<ShopItem>,
    purchasedItems: List<PurchasedItem>,
    inventory: PlayerInventory,
    unlockedThemeIds: Set<String>,
    legacySelectedThemeId: String,
    isBusy: Boolean,
    onEquipShopItem: (ShopItem) -> Unit,
    onEquipPurchasedItem: (PurchasedItem, String) -> Unit,
    onUnequipSlot: (String, String) -> Unit,
    onBuyMoreBoosters: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemMap = remember(shopItems) { shopItems.associateBy { it.id } }
    val activeThemeId = inventory.equippedThemeId.ifBlank { legacySelectedThemeId }

    // Merge catalog items + immutable PurchasedItem records so players ALWAYS keep disabled/removed items permanently!
    data class OwnedEntry(
        val id: String,
        val name: String,
        val category: String,
        val subType: String,
        val assetKey: String,
        val shopItem: ShopItem?,
        val purchased: PurchasedItem?
    )

    val allOwnedCosmetics = remember(shopItems, purchasedItems, inventory, unlockedThemeIds) {
        val entries = linkedMapOf<String, OwnedEntry>()
        // Always include Free Simple Prism theme
        val simpleItem = itemMap["theme_simple_classic"] ?: CosmeticCatalog.defaultFullShopCatalog().first()
        entries[simpleItem.id] = OwnedEntry(simpleItem.id, simpleItem.name, "THEME", "SIMPLE", simpleItem.resolvedAssetKey, simpleItem, null)

        // Include legacy unlocked themes
        unlockedThemeIds.forEach { tKey ->
            val match = shopItems.firstOrNull { it.category == "THEME" && it.themeKey == tKey }
            val id = match?.id ?: "theme_$tKey"
            entries[id] = OwnedEntry(id, match?.name ?: BlockThemes.byId(tKey).name, "THEME", match?.subType ?: "PASTEL", tKey, match, null)
        }

        // Include inventory.ownedItemIds matched in catalog
        inventory.ownedItemIds.forEach { oid ->
            val match = itemMap[oid]
            if (match != null && !match.consumable) {
                entries[oid] = OwnedEntry(oid, match.name, match.category, match.subType, match.resolvedAssetKey, match, null)
            }
        }

        // Include ALL permanent PurchasedItem receipts (survives Admin disabling or deleting the ShopItem!)
        purchasedItems.filter { !it.consumable }.forEach { receipt ->
            val match = itemMap[receipt.itemId]
            entries[receipt.itemId] = OwnedEntry(
                id = receipt.itemId,
                name = match?.name ?: receipt.itemName,
                category = match?.category ?: receipt.category,
                subType = match?.subType ?: "",
                assetKey = receipt.assetKey.ifBlank { match?.resolvedAssetKey ?: receipt.itemId },
                shopItem = match,
                purchased = receipt
            )
        }
        entries.values.toList()
    }

    fun isEntryEquipped(entry: OwnedEntry): Boolean {
        val effectiveCat = if (entry.category == "EXCLUSIVE" && entry.subType.isNotBlank()) entry.subType else entry.category
        return when (effectiveCat) {
            "THEME" -> activeThemeId == entry.assetKey
            "BLOCK_SKIN" -> inventory.equippedBlockSkinId == entry.assetKey
            "BOARD_BACKGROUND" -> inventory.equippedBackgroundId == entry.assetKey
            "SPECIAL_EFFECT" -> inventory.equippedEffectId == entry.assetKey
            "AVATAR" -> inventory.equippedAvatarId == entry.assetKey
            "PROFILE_FRAME" -> inventory.equippedFrameId == entry.assetKey
            "BADGE", "ITEM" -> inventory.equippedBadgeId == entry.assetKey
            else -> inventory.equippedExclusiveId == entry.assetKey
        }
    }

    LazyColumn(
        modifier = modifier.testTag("shop_my_collection_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Active Boosters Stockpile
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DeepSurface),
                border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.55f))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("GAME BOOSTERS STOCKPILE", style = MaterialTheme.typography.labelLarge, color = PrismGold)
                        TextButton(onClick = onBuyMoreBoosters, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)) {
                            Text("Buy More", color = PrismCyan)
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BoosterCountPill("Hammer", inventory.hammerCount, PrismCoral, Modifier.weight(1f))
                        BoosterCountPill("Undo", inventory.undoCount, PrismCyan, Modifier.weight(1f))
                        BoosterCountPill("Shuffle", inventory.shuffleCount, PrismMint, Modifier.weight(1f))
                        BoosterCountPill("Blaster", inventory.rowBlasterCount, PrismGold, Modifier.weight(1f))
                    }
                }
            }
        }

        // Reset optional equipped slots back to Classic Defaults
        item {
            val hasCustomExtras = inventory.equippedBlockSkinId.isNotBlank() ||
                inventory.equippedBackgroundId.isNotBlank() ||
                inventory.equippedEffectId.isNotBlank() ||
                inventory.equippedAvatarId.isNotBlank() ||
                inventory.equippedFrameId.isNotBlank() ||
                inventory.equippedBadgeId.isNotBlank()
            if (hasCustomExtras) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("EQUIPPED LOADOUT", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                        Text(
                            text = buildList {
                                add("Theme: ${BlockThemes.byId(activeThemeId).name}")
                                if (inventory.equippedBlockSkinId.isNotBlank()) add("Skin: ${CosmeticCatalog.blockSkinByKey(inventory.equippedBlockSkinId).name}")
                                if (inventory.equippedBackgroundId.isNotBlank()) add("Board: ${CosmeticCatalog.backgroundByKey(inventory.equippedBackgroundId).name}")
                                if (inventory.equippedEffectId.isNotBlank()) add("FX: ${CosmeticCatalog.effectByKey(inventory.equippedEffectId).name}")
                                CosmeticCatalog.avatarByKey(inventory.equippedAvatarId)?.let { add("Avatar: ${it.name}") }
                                CosmeticCatalog.frameByKey(inventory.equippedFrameId)?.let { add("Frame: ${it.name}") }
                                CosmeticCatalog.badgeByKey(inventory.equippedBadgeId)?.let { add("Badge: ${it.shortTitle}") }
                            }.joinToString("  •  "),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "MY PERMANENT COLLECTION (${allOwnedCosmetics.size})",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(allOwnedCosmetics, key = { it.id }) { entry ->
            val equipped = isEntryEquipped(entry)
            val previewModel = entry.shopItem ?: ShopItem(
                id = entry.id,
                name = entry.name,
                category = entry.category,
                subType = entry.subType,
                assetKey = entry.assetKey
            )

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = DeepSurface,
                border = BorderStroke(if (equipped) 1.8.dp else 1.dp, if (equipped) PrismCyan else SurfaceBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("collection_item_${entry.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShopItemVisualPreview(item = previewModel, size = 58.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.name, style = MaterialTheme.typography.titleSmall, color = Color.White)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = ShopCategoryKey.fromWire(entry.category).displayName,
                                style = MaterialTheme.typography.labelSmall,
                                color = PrismCyan
                            )
                            if (entry.shopItem == null || !entry.shopItem.available) {
                                StatusPill("VAULTED • YOURS FOREVER", PrismGold)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    if (equipped) {
                        if (entry.category != "THEME") {
                            OutlinedButton(
                                onClick = { onUnequipSlot(entry.category, entry.subType) },
                                enabled = !isBusy,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("collection_unequip_${entry.id}")
                            ) {
                                Text("Unequip", style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = "Active", tint = PrismCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Active", style = MaterialTheme.typography.labelMedium, color = PrismCyan)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                if (entry.shopItem != null) {
                                    onEquipShopItem(entry.shopItem)
                                } else if (entry.purchased != null) {
                                    onEquipPurchasedItem(entry.purchased, entry.subType)
                                }
                            },
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismCyan, contentColor = Color(0xFF041A24)),
                            modifier = Modifier.testTag("collection_equip_${entry.id}")
                        ) {
                            Text("Equip", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BoosterCountPill(label: String, count: Long, accent: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("×$count", style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily), color = accent, fontWeight = FontWeight.ExtraBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1)
        }
    }
}

@Composable
private fun PurchaseHistoryTabContent(
    transactions: List<ShopTransaction>,
    earnedCoins: Long,
    purchasedCoins: Long,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.testTag("shop_purchase_history_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = CardSurface,
                border = BorderStroke(1.dp, SurfaceBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Coin Spend Order", style = MaterialTheme.typography.labelLarge, color = Color.White)
                        Text(
                            "Earned Coins spend first, then Purchased Coins.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("E: %,d".format(earnedCoins), style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismMint)
                        Text("P: %,d".format(purchasedCoins), style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismGold)
                    }
                }
            }
        }

        if (transactions.isEmpty()) {
            item {
                EmptyStatePanel(
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    title = "No Coin Shop Purchases Yet",
                    description = "Every Theme, 3D Block Skin, Board Background, Special Effect, Booster, and Profile Item you buy is recorded permanently here."
                )
            }
        } else {
            items(transactions, key = { it.id }) { tx ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = DeepSurface,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(tx.itemName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                if (tx.quantity > 1L) StatusPill("×${tx.quantity}", PrismCyan)
                            }
                            Text(
                                text = "${ShopCategoryKey.fromWire(tx.category).displayName} • Source: ${tx.coinSource} • ${tx.status}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "-%,d Coins".format(tx.pricePaidCoins),
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                color = PrismGold
                            )
                            if (tx.originalPriceCoins > tx.pricePaidCoins) {
                                Text(
                                    text = "Was %,d".format(tx.originalPriceCoins),
                                    style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                                    color = PrismCoral
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
