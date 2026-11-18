package com.example.game

import com.example.data.model.PromotionalOffer
import com.example.data.model.ShopItem
import kotlin.math.roundToInt

data class ResolvedShopPrice(
    val basePriceCoins: Long,
    val effectivePriceCoins: Long,
    val promoApplied: Boolean,
    val discountPercent: Int,
    val promoLabel: String,
    val promoOfferId: String
)

sealed interface PurchaseValidationResult {
    data class Allowed(val price: ResolvedShopPrice) : PurchaseValidationResult
    data class Denied(val reason: String) : PurchaseValidationResult
}

data class CoinDeductionSplit(
    val newEarnedCoins: Long,
    val newPurchasedCoins: Long,
    val earnedSpent: Long,
    val purchasedSpent: Long,
    val coinSourceLabel: String // EARNED, PURCHASED, MIXED, FREE
)

/**
 * Pure, deterministic Coin Shop pricing & purchase validation engine.
 * Guarantees live database-driven prices (never hardcoded), promo window checks,
 * duplicate non-consumable purchase protection, disabled-item protection, and
 * non-negative Coin wallet deductions.
 */
object ShopPricingEngine {

    fun resolvePrice(
        item: ShopItem,
        activePromotions: List<PromotionalOffer>,
        nowMillis: Long = System.currentTimeMillis()
    ): ResolvedShopPrice {
        val base = item.priceCoins.coerceAtLeast(0L)
        if (base == 0L) {
            return ResolvedShopPrice(
                basePriceCoins = 0L,
                effectivePriceCoins = 0L,
                promoApplied = false,
                discountPercent = 0,
                promoLabel = "FREE",
                promoOfferId = ""
            )
        }

        var bestPrice = base
        var bestLabel = ""
        var bestOfferId = ""

        // 1. Check inline item promotional pricing window
        val inlineWindowValid = item.promoActive &&
            item.promoPriceCoins in 0L until base &&
            (item.promoStartMillis == 0L || nowMillis >= item.promoStartMillis) &&
            (item.promoEndMillis == 0L || nowMillis <= item.promoEndMillis)
        if (inlineWindowValid) {
            bestPrice = item.promoPriceCoins
            bestLabel = item.promoLabel.ifBlank { "SALE" }
            bestOfferId = "inline_${item.id}"
        }

        // 2. Check active PromotionalOffer campaigns targeting this item or category
        for (offer in activePromotions) {
            if (!offer.active) continue
            if (nowMillis < offer.startTimeMillis || nowMillis > offer.endTimeMillis) continue
            val matchesItem = offer.itemId.isNotBlank() && offer.itemId == item.id
            val matchesCategory = offer.itemId.isBlank() &&
                offer.categoryKey.isNotBlank() &&
                (offer.categoryKey == "ALL" || offer.categoryKey == item.category)
            if (!matchesItem && !matchesCategory) continue

            val candidatePrice = when {
                matchesItem && offer.promoPriceCoins > 0L && offer.promoPriceCoins < base -> offer.promoPriceCoins
                offer.discountPercent in 1L..95L -> {
                    ((base * (100L - offer.discountPercent)) / 100L).coerceAtLeast(1L)
                }
                else -> base
            }
            if (candidatePrice < bestPrice) {
                bestPrice = candidatePrice
                bestLabel = offer.title.take(24).ifBlank { "PROMO" }
                bestOfferId = offer.id
            }
        }

        val applied = bestPrice < base
        val pct = if (applied && base > 0L) {
            (((base - bestPrice).toDouble() / base.toDouble()) * 100.0).roundToInt().coerceIn(1, 99)
        } else 0

        return ResolvedShopPrice(
            basePriceCoins = base,
            effectivePriceCoins = bestPrice,
            promoApplied = applied,
            discountPercent = pct,
            promoLabel = if (applied) bestLabel else "",
            promoOfferId = if (applied) bestOfferId else ""
        )
    }

    fun validatePurchase(
        item: ShopItem,
        resolvedPrice: ResolvedShopPrice,
        totalUserCoins: Long,
        ownedItemIds: Set<String>,
        unlockedThemeIds: Set<String>
    ): PurchaseValidationResult {
        if (!item.available) {
            return PurchaseValidationResult.Denied("This item is currently unavailable in the Coin Shop.")
        }
        val alreadyOwned = !item.consumable && (
            item.id in ownedItemIds ||
                (item.category == "THEME" && item.themeKey.isNotBlank() && item.themeKey in unlockedThemeIds) ||
                resolvedPrice.effectivePriceCoins == 0L && item.id == "theme_simple_classic"
            )
        if (alreadyOwned) {
            return PurchaseValidationResult.Denied("You already permanently own '${item.name}'. Equip it from My Collection!")
        }
        if (totalUserCoins < resolvedPrice.effectivePriceCoins) {
            val missing = resolvedPrice.effectivePriceCoins - totalUserCoins
            return PurchaseValidationResult.Denied(
                "Not enough Coins. Need %,d more Coins to buy '${item.name}'.".format(missing)
            )
        }
        return PurchaseValidationResult.Allowed(resolvedPrice)
    }

    /**
     * Spends Earned Coins first (non-cashable), then Purchased Coins if needed.
     * Guarantees balances never drop below 0.
     */
    fun computeCoinDeduction(
        priceCoins: Long,
        earnedCoins: Long,
        purchasedCoins: Long
    ): Result<CoinDeductionSplit> {
        val safePrice = priceCoins.coerceAtLeast(0L)
        if (safePrice == 0L) {
            return Result.success(
                CoinDeductionSplit(
                    newEarnedCoins = earnedCoins,
                    newPurchasedCoins = purchasedCoins,
                    earnedSpent = 0L,
                    purchasedSpent = 0L,
                    coinSourceLabel = "FREE"
                )
            )
        }
        if (earnedCoins + purchasedCoins < safePrice) {
            return Result.failure(IllegalStateException("Insufficient Coins for purchase."))
        }
        val earnedSpend = minOf(earnedCoins.coerceAtLeast(0L), safePrice)
        val remaining = safePrice - earnedSpend
        val purchasedSpend = minOf(purchasedCoins.coerceAtLeast(0L), remaining)
        if (earnedSpend + purchasedSpend != safePrice) {
            return Result.failure(IllegalStateException("Insufficient Coins for purchase."))
        }
        val sourceLabel = when {
            earnedSpend > 0L && purchasedSpend > 0L -> "MIXED"
            purchasedSpend > 0L -> "PURCHASED"
            else -> "EARNED"
        }
        return Result.success(
            CoinDeductionSplit(
                newEarnedCoins = earnedCoins - earnedSpend,
                newPurchasedCoins = purchasedCoins - purchasedSpend,
                earnedSpent = earnedSpend,
                purchasedSpent = purchasedSpend,
                coinSourceLabel = sourceLabel
            )
        )
    }
}
