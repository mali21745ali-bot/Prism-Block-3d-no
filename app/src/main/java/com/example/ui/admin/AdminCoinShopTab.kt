package com.example.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AdminPermissions
import com.example.data.model.PromotionalOffer
import com.example.data.model.ShopCategoryKey
import com.example.data.model.ShopItem
import com.example.game.CosmeticCatalog
import com.example.game.ShopPricingEngine
import com.example.ui.components.StatMetricCard
import com.example.ui.components.StatusPill
import com.example.ui.rewards.ShopItemVisualPreview
import com.example.ui.theme.CardSurface
import com.example.ui.theme.DeepSurface
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.PrismCoral
import com.example.ui.theme.PrismCrimson
import com.example.ui.theme.PrismCyan
import com.example.ui.theme.PrismGold
import com.example.ui.theme.PrismMint
import com.example.ui.theme.PrismViolet
import com.example.ui.theme.SurfaceBorder
import com.example.viewmodel.AdminAccessState
import com.example.viewmodel.AdminViewModel

private enum class AdminShopViewMode(val label: String) {
    ITEMS("Items"),
    CATEGORIES("Categories"),
    PROMOTIONS("Promotions"),
    ANALYTICS("Sales & History")
}

@Composable
fun AdminCoinShopTab(
    adminViewModel: AdminViewModel,
    adminAccess: AdminAccessState,
    busy: Boolean
) {
    val shopItems by adminViewModel.allShopItems.collectAsStateWithLifecycle()
    val shopCategories by adminViewModel.allShopCategories.collectAsStateWithLifecycle()
    val promos by adminViewModel.allPromotionalOffers.collectAsStateWithLifecycle()
    val shopTxs by adminViewModel.allShopTransactions.collectAsStateWithLifecycle()

    val canEditItems = adminAccess.isSuperAdmin ||
        AdminPermissions.MANAGE_SHOP in adminAccess.permissions ||
        AdminPermissions.MANAGE_SHOP_ITEMS in adminAccess.permissions
    val canEditPrices = canEditItems || AdminPermissions.MANAGE_SHOP_PRICES in adminAccess.permissions
    val canEditPromos = adminAccess.isSuperAdmin ||
        AdminPermissions.MANAGE_SHOP in adminAccess.permissions ||
        AdminPermissions.MANAGE_SHOP_PROMOS in adminAccess.permissions
    val canEditCategories = adminAccess.isSuperAdmin ||
        AdminPermissions.MANAGE_SHOP in adminAccess.permissions ||
        AdminPermissions.MANAGE_SHOP_CATEGORIES in adminAccess.permissions
    val canViewAnalytics = adminAccess.isSuperAdmin ||
        AdminPermissions.MANAGE_SHOP in adminAccess.permissions ||
        AdminPermissions.VIEW_SHOP_ANALYTICS in adminAccess.permissions ||
        AdminPermissions.VIEW_REPORTS in adminAccess.permissions

    var viewMode by remember { mutableStateOf(AdminShopViewMode.ITEMS) }
    var selectedCategory by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    var editingItem by remember { mutableStateOf<ShopItem?>(null) }
    var showCreateItemDialog by remember { mutableStateOf(false) }
    var quickPriceItem by remember { mutableStateOf<ShopItem?>(null) }
    var analyticsItem by remember { mutableStateOf<ShopItem?>(null) }
    var pendingDeleteItem by remember { mutableStateOf<ShopItem?>(null) }
    var editingPromo by remember { mutableStateOf<PromotionalOffer?>(null) }
    var showCreatePromoDialog by remember { mutableStateOf(false) }

    val totalRevenueCoins = remember(shopTxs) { shopTxs.sumOf { it.pricePaidCoins } }
    val activeItemsCount = remember(shopItems) { shopItems.count { it.available } }

    val filteredItems = remember(shopItems, selectedCategory, searchQuery) {
        val q = searchQuery.trim().lowercase()
        shopItems.filter { item ->
            val catOk = selectedCategory == "ALL" ||
                item.category == selectedCategory ||
                (selectedCategory == "BADGE" && item.category == "ITEM")
            val searchOk = q.isEmpty() ||
                item.name.lowercase().contains(q) ||
                item.id.lowercase().contains(q) ||
                item.category.lowercase().contains(q)
            catOk && searchOk
        }.sortedBy { it.sortOrder }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_coin_shop_tab"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Store KPI Header + Sync Catalog Action
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                StatMetricCard("STORE ITEMS", "${shopItems.size} ($activeItemsCount ON)", PrismCyan, Icons.Filled.ShoppingBag, Modifier.weight(1f))
                StatMetricCard("COIN SALES", "${shopTxs.size} • %,d".format(totalRevenueCoins), PrismGold, Icons.Filled.MonetizationOn, Modifier.weight(1f))
            }
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canEditItems) {
                    Button(
                        onClick = { showCreateItemDialog = true },
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_shop_add_item_btn")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Item", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { adminViewModel.seedCoinShopCatalog() },
                        enabled = !busy,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("admin_seed_shop_catalog_btn")
                    ) {
                        Icon(Icons.Filled.CloudSync, contentDescription = null, tint = PrismCyan, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sync Catalog", color = PrismCyan)
                    }
                } else {
                    Surface(shape = RoundedCornerShape(12.dp), color = CardSurface, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (canEditPrices) "Price Manager Mode: You can adjust item Coin prices & promotional pricing."
                            else "View-Only Store Permissions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PrismGold,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        }

        // Sub-Section Switcher: Items | Categories | Promotions | Sales & History
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(AdminShopViewMode.entries) { mode ->
                    if (mode == AdminShopViewMode.ANALYTICS && !canViewAnalytics) return@items
                    FilterChip(
                        selected = viewMode == mode,
                        onClick = { viewMode = mode },
                        label = { Text(mode.label) },
                        modifier = Modifier.testTag("admin_shop_mode_${mode.name.lowercase()}"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrismViolet,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        when (viewMode) {
            AdminShopViewMode.ITEMS -> {
                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search Coin Shop items...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Separate Category Tabs inside Admin Coin Shop
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedCategory == "ALL",
                                onClick = { selectedCategory = "ALL" },
                                label = { Text("All (${shopItems.size})") }
                            )
                        }
                        items(ShopCategoryKey.entries) { cat ->
                            val cnt = shopItems.count { it.category == cat.wireName || (cat == ShopCategoryKey.BADGE && it.category == "ITEM") }
                            FilterChip(
                                selected = selectedCategory == cat.wireName,
                                onClick = { selectedCategory = cat.wireName },
                                label = { Text("${cat.shortLabel} ($cnt)") },
                                modifier = Modifier.testTag("admin_shop_cat_${cat.wireName.lowercase()}")
                            )
                        }
                    }
                }

                items(filteredItems, key = { it.id }) { item ->
                    val resolved = ShopPricingEngine.resolvePrice(item, promos)
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepSurface),
                        border = BorderStroke(1.dp, if (item.available) SurfaceBorder else PrismCrimson.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_shop_card_${item.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                ShopItemVisualPreview(item = item, size = 56.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = item.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        StatusPill(if (item.available) "ACTIVE" else "DISABLED", if (item.available) PrismMint else PrismCrimson)
                                    }
                                    Text(
                                        text = buildString {
                                            append(ShopCategoryKey.fromWire(item.category).shortLabel)
                                            append(" • ")
                                            append("%,d Coins".format(item.priceCoins))
                                            if (resolved.promoApplied) append(" → %,d PROMO".format(resolved.effectivePriceCoins))
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = JetBrainsMonoFontFamily),
                                        color = PrismGold
                                    )
                                    Text(
                                        text = "Sales: ${item.totalSalesCount} • Owners: ${item.uniqueOwnersCount} • Key: ${item.resolvedAssetKey}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                if (canEditItems) {
                                    Switch(
                                        checked = item.available,
                                        onCheckedChange = { adminViewModel.toggleShopItemAvailability(item, it) },
                                        enabled = !busy
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (canEditPrices) {
                                    OutlinedButton(
                                        onClick = { quickPriceItem = item },
                                        enabled = !busy,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.65f)),
                                        modifier = Modifier.testTag("admin_price_btn_${item.id}")
                                    ) {
                                        Icon(Icons.Filled.LocalOffer, contentDescription = null, tint = PrismGold, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Price", color = PrismGold, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (canEditItems) {
                                    OutlinedButton(
                                        onClick = { editingItem = item },
                                        enabled = !busy,
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, PrismCyan.copy(alpha = 0.65f)),
                                        modifier = Modifier.testTag("admin_edit_btn_${item.id}")
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, tint = PrismCyan, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Edit", color = PrismCyan, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                if (canViewAnalytics) {
                                    OutlinedButton(
                                        onClick = { analyticsItem = item },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        border = BorderStroke(1.dp, PrismViolet.copy(alpha = 0.65f))
                                    ) {
                                        Icon(Icons.Filled.Analytics, contentDescription = null, tint = PrismViolet, modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Sales", color = PrismViolet, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Spacer(Modifier.weight(1f))
                                if (canEditItems) {
                                    IconButton(
                                        onClick = { pendingDeleteItem = item },
                                        enabled = !busy,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete item", tint = PrismCrimson, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminShopViewMode.CATEGORIES -> {
                val displayCats = (if (shopCategories.isNotEmpty()) shopCategories else CosmeticCatalog.defaultShopCategories())
                    .sortedBy { it.sortOrder }
                items(displayCats, key = { it.categoryKey }) { cat ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DeepSurface,
                        border = BorderStroke(1.dp, if (cat.enabled) SurfaceBorder else PrismCoral.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(cat.displayName, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                    StatusPill(cat.categoryKey, PrismCyan)
                                }
                                Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(
                                checked = cat.enabled,
                                enabled = canEditCategories && !busy,
                                onCheckedChange = { enabled ->
                                    adminViewModel.saveShopCategory(cat.copy(enabled = enabled))
                                }
                            )
                        }
                    }
                }
            }

            AdminShopViewMode.PROMOTIONS -> {
                if (canEditPromos) {
                    item {
                        Button(
                            onClick = { showCreatePromoDialog = true },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_create_promo_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrismViolet, contentColor = Color.White)
                        ) {
                            Icon(Icons.Filled.LocalOffer, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Store / Category Promotion", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                if (promos.isEmpty()) {
                    item {
                        Surface(shape = RoundedCornerShape(16.dp), color = DeepSurface, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "No active promotional campaigns. Tap above to launch a time-limited category or storewide Coin sale!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                } else {
                    items(promos, key = { it.id }) { offer ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = DeepSurface,
                            border = BorderStroke(1.dp, if (offer.active) PrismViolet else SurfaceBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(offer.title, style = MaterialTheme.typography.titleSmall, color = Color.White)
                                        StatusPill("-${offer.discountPercent}%", PrismCoral)
                                    }
                                    Text(
                                        "Target: ${offer.categoryKey.ifBlank { offer.itemId }} • ${offer.description}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (canEditPromos) {
                                    Switch(
                                        checked = offer.active,
                                        enabled = !busy,
                                        onCheckedChange = { adminViewModel.savePromotionalOffer(offer.copy(active = it)) }
                                    )
                                    IconButton(onClick = { adminViewModel.deletePromotionalOffer(offer.id) }, enabled = !busy) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete promo", tint = PrismCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            AdminShopViewMode.ANALYTICS -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = DeepSurface),
                        border = BorderStroke(1.dp, PrismGold.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("REVENUE BY CATEGORY", style = MaterialTheme.typography.labelLarge, color = PrismGold)
                            val grouped = shopTxs.groupBy { it.category }
                            if (grouped.isEmpty()) {
                                Text("No Coin Shop sales recorded yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                grouped.entries.sortedByDescending { it.value.sumOf { tx -> tx.pricePaidCoins } }.forEach { (cat, list) ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("${ShopCategoryKey.fromWire(cat).displayName} (${list.size})", style = MaterialTheme.typography.bodySmall, color = Color.White)
                                        Text("%,d Coins".format(list.sumOf { it.pricePaidCoins }), style = MaterialTheme.typography.labelMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismGold)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    Text("ALL STORE PURCHASE HISTORY (${shopTxs.size})", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(shopTxs.take(40), key = { it.id }) { tx ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DeepSurface,
                        border = BorderStroke(1.dp, SurfaceBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = PrismCyan, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${tx.itemName} (×${tx.quantity})", style = MaterialTheme.typography.titleSmall, color = Color.White)
                                Text("${tx.userDisplayName} • ${ShopCategoryKey.fromWire(tx.category).shortLabel} • ${tx.coinSource}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("%,d Coins".format(tx.pricePaidCoins), style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismGold)
                        }
                    }
                }
            }
        }
    }

    // Dialog Hosts
    if (showCreateItemDialog || editingItem != null) {
        AdminShopItemEditorDialog(
            initialItem = editingItem,
            nextSortOrder = (shopItems.maxOfOrNull { it.sortOrder } ?: 0L) + 1L,
            busy = busy,
            onDismiss = {
                showCreateItemDialog = false
                editingItem = null
            },
            onSave = { item ->
                adminViewModel.saveShopItem(item)
                showCreateItemDialog = false
                editingItem = null
            }
        )
    }

    quickPriceItem?.let { item ->
        AdminQuickPriceDialog(
            item = item,
            busy = busy,
            onDismiss = { quickPriceItem = null },
            onApply = { newPrice, pActive, pPrice, pStart, pEnd, pLabel ->
                adminViewModel.updateShopItemPriceAndPromo(
                    itemId = item.id,
                    itemName = item.name,
                    newPriceCoins = newPrice,
                    promoActive = pActive,
                    promoPriceCoins = pPrice,
                    promoStartMillis = pStart,
                    promoEndMillis = pEnd,
                    promoLabel = pLabel
                )
                quickPriceItem = null
            }
        )
    }

    analyticsItem?.let { item ->
        AdminItemAnalyticsDialog(
            item = item,
            transactions = shopTxs,
            onDismiss = { analyticsItem = null }
        )
    }

    pendingDeleteItem?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDeleteItem = null },
            containerColor = DeepSurface,
            title = { Text("Delete '${item.name}'?", color = PrismCrimson) },
            text = {
                Text(
                    "Removing this item hides it from future purchases in the Coin Shop. Players who already bought it keep permanent ownership in My Collection.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        adminViewModel.deleteShopItem(item.id)
                        pendingDeleteItem = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrismCrimson, contentColor = Color.White)
                ) {
                    Text("Delete from Store", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteItem = null }) { Text("Cancel") } }
        )
    }

    if (showCreatePromoDialog || editingPromo != null) {
        AdminPromoOfferEditorDialog(
            initialOffer = editingPromo,
            busy = busy,
            onDismiss = {
                showCreatePromoDialog = false
                editingPromo = null
            },
            onSave = { offer ->
                adminViewModel.savePromotionalOffer(offer)
                showCreatePromoDialog = false
                editingPromo = null
            }
        )
    }
}
