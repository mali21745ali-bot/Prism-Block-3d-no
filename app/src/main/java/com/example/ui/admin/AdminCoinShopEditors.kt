package com.example.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.BoosterType
import com.example.data.model.PromotionalOffer
import com.example.data.model.ShopCategoryKey
import com.example.data.model.ShopItem
import com.example.data.model.ShopTransaction
import com.example.game.BlockThemes
import com.example.game.CosmeticCatalog
import com.example.ui.components.StatusPill
import com.example.ui.rewards.ShopItemVisualPreview
import com.example.ui.rewards.encodePickedImageToDataUri
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

private fun presetAssetKeysForCategory(category: String): List<Pair<String, String>> = when (category) {
    "THEME" -> BlockThemes.allThemes.map { it.id to it.name }
    "BLOCK_SKIN" -> CosmeticCatalog.blockSkins.map { it.key to it.name }
    "BOARD_BACKGROUND" -> CosmeticCatalog.boardBackgrounds.map { it.key to it.name }
    "SPECIAL_EFFECT" -> CosmeticCatalog.specialEffects.map { it.key to it.name }
    "GAME_BOOSTER" -> BoosterType.entries.map { it.wireName to it.label }
    "AVATAR" -> CosmeticCatalog.avatars.map { it.key to it.name }
    "PROFILE_FRAME" -> CosmeticCatalog.profileFrames.map { it.key to it.name }
    "BADGE" -> CosmeticCatalog.playerBadges.map { it.key to it.name }
    "EXCLUSIVE" -> listOf(
        "skin_royal_emerald_ex" to "Royal Emerald Skin",
        "skin_cosmic_eclipse_ex" to "Cosmic Eclipse Skin",
        "bg_exclusive_nebula" to "Aurora Nebula BG",
        "fx_exclusive_supernova" to "Supernova FX",
        "frame_exclusive_celestial" to "Celestial Frame"
    )
    else -> emptyList()
}

@Composable
fun AdminShopItemEditorDialog(
    initialItem: ShopItem?,
    nextSortOrder: Long,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (ShopItem) -> Unit
) {
    val context = LocalContext.current
    val isEditing = initialItem != null

    var name by remember(initialItem) { mutableStateOf(initialItem?.name ?: "") }
    var desc by remember(initialItem) { mutableStateOf(initialItem?.description ?: "") }
    var category by remember(initialItem) { mutableStateOf(initialItem?.category ?: "THEME") }
    var subType by remember(initialItem) { mutableStateOf(initialItem?.subType ?: "") }
    var assetKey by remember(initialItem) { mutableStateOf(initialItem?.resolvedAssetKey ?: "pastel_macaron") }
    var imageUrl by remember(initialItem) { mutableStateOf(initialItem?.imageUrl ?: "") }
    var imageStatus by remember { mutableStateOf<String?>(null) }
    var priceCoins by remember(initialItem) { mutableStateOf((initialItem?.priceCoins ?: 250L).toString()) }
    var promoActive by remember(initialItem) { mutableStateOf(initialItem?.promoActive ?: false) }
    var promoPrice by remember(initialItem) { mutableStateOf((initialItem?.promoPriceCoins ?: 0L).toString()) }
    var promoHours by remember { mutableStateOf("48") }
    var promoLabel by remember(initialItem) { mutableStateOf(initialItem?.promoLabel ?: "FLASH SALE") }
    var featured by remember(initialItem) { mutableStateOf(initialItem?.featured ?: false) }
    var newArrival by remember(initialItem) { mutableStateOf(initialItem?.newArrival ?: true) }
    var limitedEdition by remember(initialItem) { mutableStateOf(initialItem?.limitedEdition ?: false) }
    var available by remember(initialItem) { mutableStateOf(initialItem?.available ?: true) }
    var packQty by remember(initialItem) { mutableStateOf((initialItem?.boosterPackQty ?: 3L).toString()) }
    var maxPerGame by remember(initialItem) { mutableStateOf((initialItem?.boosterMaxPerGame ?: 3L).toString()) }
    var powerRadius by remember(initialItem) { mutableStateOf((initialItem?.boosterPowerRadius ?: 1L).toString()) }
    var accentHex by remember(initialItem) { mutableStateOf(initialItem?.accentHex ?: "#38E8FF") }

    // Zero-permission Android Photo Picker (strictly Google Play compliant!)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = encodePickedImageToDataUri(context, uri)
            if (encoded != null) {
                imageUrl = encoded
                imageStatus = "Image uploaded & compressed (${encoded.length / 1024} KB)"
            } else {
                imageStatus = "Could not compress image under 120KB — try another photo."
            }
        }
    }

    val previewItem = remember(name, category, subType, assetKey, imageUrl, accentHex, packQty) {
        ShopItem(
            id = initialItem?.id ?: "preview",
            name = name.ifBlank { "Preview" },
            category = category,
            subType = subType,
            themeKey = if (category == "THEME") assetKey else "",
            assetKey = assetKey,
            imageUrl = imageUrl,
            accentHex = accentHex,
            boosterPackQty = packQty.toLongOrNull() ?: 1L
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ShopItemVisualPreview(item = previewItem, size = 50.dp)
                Column {
                    Text(
                        text = if (isEditing) "Edit Shop Item" else "Add New Coin Shop Item",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text("Live Firestore sync • All 9 categories", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(60) },
                    label = { Text("Item Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_shop_edit_name")
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it.take(260) },
                    label = { Text("Description") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Category", style = MaterialTheme.typography.labelLarge, color = PrismGold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(ShopCategoryKey.entries) { cat ->
                        FilterChip(
                            selected = category == cat.wireName,
                            onClick = {
                                category = cat.wireName
                                val firstPreset = presetAssetKeysForCategory(cat.wireName).firstOrNull()?.first
                                if (firstPreset != null) assetKey = firstPreset
                                if (cat == ShopCategoryKey.GAME_BOOSTER) {
                                    subType = firstPreset ?: BoosterType.HAMMER.wireName
                                } else if (cat == ShopCategoryKey.EXCLUSIVE) {
                                    subType = "BLOCK_SKIN"
                                    limitedEdition = true
                                }
                            },
                            label = { Text(cat.shortLabel) }
                        )
                    }
                }

                // Preset visual / booster asset picker
                val presets = remember(category) { presetAssetKeysForCategory(category) }
                if (presets.isNotEmpty()) {
                    Text("Built-In 3D Visual / Power Key", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presets) { (key, label) ->
                            FilterChip(
                                selected = assetKey == key,
                                onClick = {
                                    assetKey = key
                                    if (category == "GAME_BOOSTER") subType = key
                                    if (category == "EXCLUSIVE") {
                                        subType = when {
                                            key.startsWith("skin_") -> "BLOCK_SKIN"
                                            key.startsWith("bg_") -> "BOARD_BACKGROUND"
                                            key.startsWith("fx_") -> "SPECIAL_EFFECT"
                                            key.startsWith("frame_") -> "PROFILE_FRAME"
                                            else -> "BLOCK_SKIN"
                                        }
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = assetKey,
                        onValueChange = { assetKey = it.take(64) },
                        label = { Text("Asset / Theme Key") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceCoins,
                        onValueChange = { priceCoins = it.filter { c -> c.isDigit() }.take(7) },
                        label = { Text("Coin Price") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .weight(0.85f)
                            .testTag("admin_shop_edit_price")
                    )
                }

                // Zero-permission Photo Picker Image Upload + HTTPS URL override
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = CardSurface,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Custom Item Image (Optional)", style = MaterialTheme.typography.labelMedium, color = PrismCyan)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_shop_pick_image_btn")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Upload Photo")
                            }
                            if (imageUrl.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        imageUrl = ""
                                        imageStatus = "Reset to live 3D preview"
                                    }
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Clear image", tint = PrismCrimson, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                        if (!imageUrl.startsWith("data:image/")) {
                            OutlinedTextField(
                                value = imageUrl,
                                onValueChange = { imageUrl = it.take(500) },
                                label = { Text("Or paste HTTPS image URL") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        imageStatus?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = PrismMint)
                        }
                    }
                }

                // Booster-specific Gameplay Configuration
                if (category == "GAME_BOOSTER") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = packQty,
                            onValueChange = { packQty = it.filter { c -> c.isDigit() }.take(3) },
                            label = { Text("Pack Qty") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxPerGame,
                            onValueChange = { maxPerGame = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Max/Game") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = powerRadius,
                            onValueChange = { powerRadius = it.filter { c -> c.isDigit() }.take(2) },
                            label = { Text("Radius") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                HorizontalDivider(color = SurfaceBorder)

                // Inline Promotional Discount Controls
                AdminSwitchLine("Enable Promotional Discount", promoActive) { promoActive = it }
                if (promoActive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = promoPrice,
                            onValueChange = { promoPrice = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("Promo Coins") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = promoHours,
                            onValueChange = { promoHours = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Duration (hrs)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = promoLabel,
                        onValueChange = { promoLabel = it.take(36) },
                        label = { Text("Promo Tag (e.g. FLASH SALE)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                HorizontalDivider(color = SurfaceBorder)
                AdminSwitchLine("Available for Purchase", available) { available = it }
                AdminSwitchLine("Show in Featured Showcase", featured) { featured = it }
                AdminSwitchLine("Show in New Arrivals", newArrival) { newArrival = it }
                AdminSwitchLine("Limited Edition Badge", limitedEdition) { limitedEdition = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val basePrice = (priceCoins.toLongOrNull() ?: 0L).coerceAtLeast(0L)
                    val now = System.currentTimeMillis()
                    val hrs = (promoHours.toLongOrNull() ?: 48L).coerceIn(1L, 720L)
                    val pPrice = (promoPrice.toLongOrNull() ?: 0L).coerceIn(0L, basePrice)
                    val docId = initialItem?.id?.ifBlank { null } ?: "${category.lowercase()}_${System.currentTimeMillis()}"
                    val isConsumable = category == "GAME_BOOSTER"
                    onSave(
                        ShopItem(
                            id = docId,
                            name = name.trim(),
                            description = desc.trim(),
                            category = category,
                            subType = subType.trim(),
                            themeKey = if (category == "THEME") assetKey.trim() else "",
                            assetKey = assetKey.trim().ifBlank { docId },
                            imageUrl = imageUrl.trim(),
                            priceCoins = basePrice,
                            promoActive = promoActive && pPrice < basePrice,
                            promoPriceCoins = if (promoActive) pPrice else 0L,
                            promoStartMillis = if (promoActive) now else 0L,
                            promoEndMillis = if (promoActive) now + hrs * 3_600_000L else 0L,
                            promoLabel = if (promoActive) promoLabel.trim().ifBlank { "SALE" } else "",
                            premiumPastel = category == "THEME" && subType.equals("PASTEL", ignoreCase = true),
                            featured = featured,
                            newArrival = newArrival,
                            limitedEdition = limitedEdition,
                            available = available,
                            consumable = isConsumable,
                            boosterPackQty = if (isConsumable) (packQty.toLongOrNull() ?: 1L).coerceIn(1L, 50L) else 1L,
                            boosterMaxPerGame = if (isConsumable) (maxPerGame.toLongOrNull() ?: 3L).coerceIn(1L, 20L) else 0L,
                            boosterPowerRadius = if (isConsumable) (powerRadius.toLongOrNull() ?: 0L).coerceIn(0L, 10L) else 0L,
                            sortOrder = initialItem?.sortOrder ?: nextSortOrder,
                            accentHex = accentHex.ifBlank { "#38E8FF" },
                            totalSalesCount = initialItem?.totalSalesCount ?: 0L,
                            uniqueOwnersCount = initialItem?.uniqueOwnersCount ?: 0L
                        )
                    )
                },
                enabled = !busy && name.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00)),
                modifier = Modifier.testTag("admin_shop_save_item_btn")
            ) {
                Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save Live", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AdminQuickPriceDialog(
    item: ShopItem,
    busy: Boolean,
    onDismiss: () -> Unit,
    onApply: (
        newPrice: Long,
        promoActive: Boolean,
        promoPrice: Long,
        promoStart: Long,
        promoEnd: Long,
        promoLabel: String
    ) -> Unit
) {
    var priceInput by remember(item) { mutableStateOf(item.priceCoins.toString()) }
    var promoOn by remember(item) { mutableStateOf(item.promoActive) }
    var promoPriceInput by remember(item) {
        mutableStateOf(
            if (item.promoPriceCoins > 0L) item.promoPriceCoins.toString()
            else ((item.priceCoins * 80L) / 100L).toString()
        )
    }
    var durationHours by remember { mutableStateOf("24") }
    var promoTag by remember(item) { mutableStateOf(item.promoLabel.ifBlank { "LIMITED DEAL" }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text("Dynamic Pricing: ${item.name}", color = Color.White, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Updates immediately for all players. Existing owners keep permanent ownership without paying extra.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text("Base Price (Coins)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_quick_price_input")
                )
                AdminSwitchLine("Activate Promo Discount", promoOn) { promoOn = it }
                if (promoOn) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = promoPriceInput,
                            onValueChange = { promoPriceInput = it.filter { c -> c.isDigit() }.take(7) },
                            label = { Text("Promo Coins") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = durationHours,
                            onValueChange = { durationHours = it.filter { c -> c.isDigit() }.take(4) },
                            label = { Text("Hours") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(0.75f)
                        )
                    }
                    OutlinedTextField(
                        value = promoTag,
                        onValueChange = { promoTag = it.take(32) },
                        label = { Text("Promo Label") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val base = (priceInput.toLongOrNull() ?: item.priceCoins).coerceAtLeast(0L)
                    val pPrice = (promoPriceInput.toLongOrNull() ?: 0L).coerceIn(0L, base)
                    val now = System.currentTimeMillis()
                    val hrs = (durationHours.toLongOrNull() ?: 24L).coerceIn(1L, 720L)
                    onApply(
                        base,
                        promoOn && pPrice < base,
                        if (promoOn) pPrice else 0L,
                        if (promoOn) now else 0L,
                        if (promoOn) now + hrs * 3_600_000L else 0L,
                        if (promoOn) promoTag.trim().ifBlank { "SALE" } else ""
                    )
                },
                enabled = !busy && priceInput.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrismGold, contentColor = Color(0xFF241A00)),
                modifier = Modifier.testTag("admin_quick_price_save_btn")
            ) {
                Text("Update Live Price", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun AdminItemAnalyticsDialog(
    item: ShopItem,
    transactions: List<ShopTransaction>,
    onDismiss: () -> Unit
) {
    val itemTxs = remember(item.id, transactions) { transactions.filter { it.itemId == item.id } }
    val revenueCoins = remember(itemTxs) { itemTxs.sumOf { it.pricePaidCoins } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text("Item Analytics: ${item.name}", color = Color.White, style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(shape = RoundedCornerShape(12.dp), color = CardSurface, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("SALES", style = MaterialTheme.typography.labelSmall, color = PrismCyan)
                            Text("${maxOf(item.totalSalesCount, itemTxs.size.toLong())}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = Color.White)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = CardSurface, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("OWNERS", style = MaterialTheme.typography.labelSmall, color = PrismMint)
                            Text("${maxOf(item.uniqueOwnersCount, itemTxs.map { it.userId }.toSet().size.toLong())}", style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = Color.White)
                        }
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = CardSurface, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(10.dp)) {
                            Text("REVENUE", style = MaterialTheme.typography.labelSmall, color = PrismGold)
                            Text("%,d".format(revenueCoins), style = MaterialTheme.typography.titleMedium.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismGold)
                        }
                    }
                }

                Text("PURCHASE HISTORY (${itemTxs.size})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (itemTxs.isEmpty()) {
                    Text("No purchases recorded for this item yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(itemTxs.take(25), key = { it.id }) { tx ->
                            Surface(shape = RoundedCornerShape(10.dp), color = CardSurface, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(tx.userDisplayName, style = MaterialTheme.typography.labelLarge, color = Color.White)
                                        Text("${tx.coinSource} • ×${tx.quantity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text("%,d Coins".format(tx.pricePaidCoins), style = MaterialTheme.typography.labelLarge.copy(fontFamily = JetBrainsMonoFontFamily), color = PrismGold)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun AdminPromoOfferEditorDialog(
    initialOffer: PromotionalOffer?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (PromotionalOffer) -> Unit
) {
    var title by remember(initialOffer) { mutableStateOf(initialOffer?.title ?: "Weekend 3D Sale") }
    var description by remember(initialOffer) { mutableStateOf(initialOffer?.description ?: "Limited-time Coin Shop discount!") }
    var categoryKey by remember(initialOffer) { mutableStateOf(initialOffer?.categoryKey?.ifBlank { "ALL" } ?: "ALL") }
    var discountPercent by remember(initialOffer) { mutableStateOf((initialOffer?.discountPercent ?: 25L).toString()) }
    var durationHours by remember { mutableStateOf("48") }
    var active by remember(initialOffer) { mutableStateOf(initialOffer?.active ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepSurface,
        title = { Text(if (initialOffer == null) "Create Promo Campaign" else "Edit Promo Campaign", color = Color.White) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it.take(50) }, label = { Text("Campaign Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it.take(140) }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Target Category", style = MaterialTheme.typography.labelSmall, color = PrismGold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(selected = categoryKey == "ALL", onClick = { categoryKey = "ALL" }, label = { Text("ALL STORE") })
                    }
                    items(ShopCategoryKey.entries) { cat ->
                        FilterChip(selected = categoryKey == cat.wireName, onClick = { categoryKey = cat.wireName }, label = { Text(cat.shortLabel) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = discountPercent,
                        onValueChange = { discountPercent = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Discount % (1-90)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = durationHours,
                        onValueChange = { durationHours = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text("Duration (hrs)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
                AdminSwitchLine("Campaign Active Now", active) { active = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val now = System.currentTimeMillis()
                    val hrs = (durationHours.toLongOrNull() ?: 48L).coerceIn(1L, 720L)
                    val pct = (discountPercent.toLongOrNull() ?: 20L).coerceIn(1L, 90L)
                    onSave(
                        PromotionalOffer(
                            id = initialOffer?.id?.ifBlank { null } ?: "promo_${System.currentTimeMillis()}",
                            title = title.trim(),
                            description = description.trim(),
                            itemId = "",
                            categoryKey = categoryKey,
                            discountPercent = pct,
                            promoPriceCoins = 0L,
                            startTimeMillis = now,
                            endTimeMillis = now + hrs * 3_600_000L,
                            active = active
                        )
                    )
                },
                enabled = !busy && title.trim().length >= 2,
                colors = ButtonDefaults.buttonColors(containerColor = PrismViolet, contentColor = Color.White)
            ) {
                Text("Save Offer", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
internal fun AdminSwitchLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
