package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.model.DailyRewardConfig
import com.example.data.model.DEFAULT_THEME_ID
import com.example.data.model.ShopItem

data class BlockColorStyle(
    val base: Color,
    val highlight: Color,
    val shadow: Color,
    val glow: Color
)

data class BoardThemeSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val premiumPastel: Boolean,
    val defaultPriceCoins: Long,
    val boardBgTop: Color,
    val boardBgBottom: Color,
    val emptyCellFill: Color,
    val emptyCellBorder: Color,
    val boardFrameBorder: Color,
    val previewAccent: Color,
    val palette: List<BlockColorStyle>
)

object BlockThemes {
    val simpleClassic = BoardThemeSpec(
        id = DEFAULT_THEME_ID,
        name = "Simple Prism",
        subtitle = "Free glossy 3D jewel blocks",
        premiumPastel = false,
        defaultPriceCoins = 0L,
        boardBgTop = Color(0xFF15122B),
        boardBgBottom = Color(0xFF0E0B1E),
        emptyCellFill = Color(0xFF1D183A),
        emptyCellBorder = Color(0xFF2D2656),
        boardFrameBorder = Color(0xFF3E3675),
        previewAccent = Color(0xFF38E8FF),
        palette = listOf(
            BlockColorStyle(Color(0xFF22D3EE), Color(0xFFA5F3FC), Color(0xFF0891B2), Color(0xFF38E8FF)), // Cyan
            BlockColorStyle(Color(0xFFFF5D8F), Color(0xFFFFB3CA), Color(0xFFBE185D), Color(0xFFFF5D8F)), // Coral Rose
            BlockColorStyle(Color(0xFFF59E0B), Color(0xFFFDE68A), Color(0xFFB45309), Color(0xFFFBBF24)), // Amber Gold
            BlockColorStyle(Color(0xFF34D399), Color(0xFFA7F3D0), Color(0xFF047857), Color(0xFF34D399)), // Emerald
            BlockColorStyle(Color(0xFF8B5CF6), Color(0xFFDDD6FE), Color(0xFF5B21B6), Color(0xFFA78BFA)), // Royal Violet
            BlockColorStyle(Color(0xFFFB7185), Color(0xFFFECDD3), Color(0xFFBE123C), Color(0xFFFB7185)), // Ruby
            BlockColorStyle(Color(0xFF3B82F6), Color(0xFFBFDBFE), Color(0xFF1D4ED8), Color(0xFF60A5FA))  // Sapphire
        )
    )

    val pastelMacaron = BoardThemeSpec(
        id = "pastel_macaron",
        name = "Macaron Cloud 3D",
        subtitle = "Premium pastel porcelain blocks",
        premiumPastel = true,
        defaultPriceCoins = 250L,
        boardBgTop = Color(0xFF271F3D),
        boardBgBottom = Color(0xFF1B152C),
        emptyCellFill = Color(0xFF31284C),
        emptyCellBorder = Color(0xFF473B6B),
        boardFrameBorder = Color(0xFFF9A8D4),
        previewAccent = Color(0xFFF9A8D4),
        palette = listOf(
            BlockColorStyle(Color(0xFFF9A8D4), Color(0xFFFDF2F8), Color(0xFFDB2777), Color(0xFFF472B6)), // Blush Rose
            BlockColorStyle(Color(0xFF99F6E4), Color(0xFFF0FDFA), Color(0xFF0D9488), Color(0xFF5EEAD4)), // Mint Chiffon
            BlockColorStyle(Color(0xFFFDE68A), Color(0xFFFFFBEB), Color(0xFFD97706), Color(0xFFFCD34D)), // Buttercream
            BlockColorStyle(Color(0xFFC4B5FD), Color(0xFFF5F3FF), Color(0xFF7C3AED), Color(0xFFA78BFA)), // Lavender Milk
            BlockColorStyle(Color(0xFFBAE6FD), Color(0xFFF0F9FF), Color(0xFF0284C7), Color(0xFF7DD3FC)), // Sky Sorbet
            BlockColorStyle(Color(0xFFFED7AA), Color(0xFFFFF7ED), Color(0xFFEA580C), Color(0xFFFDBA74)), // Peach Pearl
            BlockColorStyle(Color(0xFFBBF7D0), Color(0xFFF0FDF4), Color(0xFF16A34A), Color(0xFF86EFAC))  // Pistachio Cream
        )
    )

    val pastelSakura = BoardThemeSpec(
        id = "pastel_sakura",
        name = "Sakura Mist 3D",
        subtitle = "Premium pastel blossom & matcha blocks",
        premiumPastel = true,
        defaultPriceCoins = 400L,
        boardBgTop = Color(0xFF2B1C2E),
        boardBgBottom = Color(0xFF1A111D),
        emptyCellFill = Color(0xFF36253A),
        emptyCellBorder = Color(0xFF4F3854),
        boardFrameBorder = Color(0xFFFBCFE8),
        previewAccent = Color(0xFFF472B6),
        palette = listOf(
            BlockColorStyle(Color(0xFFFBCFE8), Color(0xFFFFFFFF), Color(0xFFEC4899), Color(0xFFF9A8D4)), // Sakura Petal
            BlockColorStyle(Color(0xFFA7F3D0), Color(0xFFECFDF5), Color(0xFF059669), Color(0xFF6EE7B7)), // Matcha Silk
            BlockColorStyle(Color(0xFFDDD6FE), Color(0xFFFAF5FF), Color(0xFF8B5CF6), Color(0xFFC4B5FD)), // Wisteria
            BlockColorStyle(Color(0xFFFECACA), Color(0xFFFEF2F2), Color(0xFFEF4444), Color(0xFFFCA5A5)), // Camellia
            BlockColorStyle(Color(0xFFFEF08A), Color(0xFFFEFCE8), Color(0xFFCA8A04), Color(0xFFFDE047)), // Yuzu Cream
            BlockColorStyle(Color(0xFFA5F3FC), Color(0xFFECFEFF), Color(0xFF0891B2), Color(0xFF67E8F9)), // Mist Aqua
            BlockColorStyle(Color(0xFFFED7AA), Color(0xFFFFF7ED), Color(0xFFC2410C), Color(0xFFFDBA74))  // Persimmon Soft
        )
    )

    val pastelAurora = BoardThemeSpec(
        id = "pastel_aurora",
        name = "Aurora Pearl 3D",
        subtitle = "Premium pastel opal iridescence",
        premiumPastel = true,
        defaultPriceCoins = 600L,
        boardBgTop = Color(0xFF162436),
        boardBgBottom = Color(0xFF0D1724),
        emptyCellFill = Color(0xFF1E3047),
        emptyCellBorder = Color(0xFF324D70),
        boardFrameBorder = Color(0xFF7DD3FC),
        previewAccent = Color(0xFF7DD3FC),
        palette = listOf(
            BlockColorStyle(Color(0xFF7DD3FC), Color(0xFFE0F2FE), Color(0xFF0284C7), Color(0xFF38BDF8)), // Opal Ice
            BlockColorStyle(Color(0xFFD8B4FE), Color(0xFFFAF5FF), Color(0xFF9333EA), Color(0xFFC084FC)), // Lilac Glow
            BlockColorStyle(Color(0xFF6EE7B7), Color(0xFFECFDF5), Color(0xFF047857), Color(0xFF34D399)), // Seafoam Jade
            BlockColorStyle(Color(0xFFFDA4AF), Color(0xFFFFF1F2), Color(0xFFE11D48), Color(0xFFFB7185)), // Coral Sherbet
            BlockColorStyle(Color(0xFFFDE68A), Color(0xFFFFFBEB), Color(0xFFD97706), Color(0xFFFCD34D)), // Moonbeam Gold
            BlockColorStyle(Color(0xFFA5B4FC), Color(0xFFEEF2FF), Color(0xFF4F46E5), Color(0xFF818CF8)), // Periwinkle
            BlockColorStyle(Color(0xFF99F6E4), Color(0xFFF0FDFA), Color(0xFF0F766E), Color(0xFF5EEAD4))  // Glacier Mint
        )
    )

    val pastelVelvet = BoardThemeSpec(
        id = "pastel_velvet",
        name = "Velvet Sorbet 3D",
        subtitle = "Premium pastel satin luxury edition",
        premiumPastel = true,
        defaultPriceCoins = 850L,
        boardBgTop = Color(0xFF261E35),
        boardBgBottom = Color(0xFF181224),
        emptyCellFill = Color(0xFF322746),
        emptyCellBorder = Color(0xFF4C3C6B),
        boardFrameBorder = Color(0xFFE9D5FF),
        previewAccent = Color(0xFFC4B5FD),
        palette = listOf(
            BlockColorStyle(Color(0xFFF5D0FE), Color(0xFFFDF4FF), Color(0xFFC026D3), Color(0xFFE879F9)), // Orchid Silk
            BlockColorStyle(Color(0xFFFECDD3), Color(0xFFFFF1F2), Color(0xFFE11D48), Color(0xFFFDA4AF)), // Rosewater
            BlockColorStyle(Color(0xFFC7D2FE), Color(0xFFEEF2FF), Color(0xFF4338CA), Color(0xFFA5B4FC)), // Soft Indigo
            BlockColorStyle(Color(0xFFBBF7D0), Color(0xFFF0FDF4), Color(0xFF15803D), Color(0xFF86EFAC)), // Mint Cream
            BlockColorStyle(Color(0xFFFED7AA), Color(0xFFFFF7ED), Color(0xFFEA580C), Color(0xFFFDBA74)), // Apricot
            BlockColorStyle(Color(0xFFFEF08A), Color(0xFFFEFCE8), Color(0xFFCA8A04), Color(0xFFFDE047)), // Vanilla Gold
            BlockColorStyle(Color(0xFFBAE6FD), Color(0xFFF0F9FF), Color(0xFF0369A1), Color(0xFF7DD3FC))  // Ice Mist
        )
    )

    val darkObsidianGold = BoardThemeSpec(
        id = "dark_obsidian_gold",
        name = "Obsidian Gold 3D",
        subtitle = "Dark luxury onyx board with molten gold bevels",
        premiumPastel = false,
        defaultPriceCoins = 450L,
        boardBgTop = Color(0xFF121015),
        boardBgBottom = Color(0xFF070609),
        emptyCellFill = Color(0xFF1B1821),
        emptyCellBorder = Color(0xFF362E1F),
        boardFrameBorder = Color(0xFFF6C644),
        previewAccent = Color(0xFFF6C644),
        palette = listOf(
            BlockColorStyle(Color(0xFFF6C644), Color(0xFFFFF3C4), Color(0xFF9A6700), Color(0xFFFFD866)), // 24K Gold
            BlockColorStyle(Color(0xFF38BDF8), Color(0xFFBAE6FD), Color(0xFF0369A1), Color(0xFF38E8FF)), // Obsidian Cyan
            BlockColorStyle(Color(0xFFE11D48), Color(0xFFFDA4AF), Color(0xFF881337), Color(0xFFFB7185)), // Blood Ruby
            BlockColorStyle(Color(0xFF10B981), Color(0xFFA7F3D0), Color(0xFF065F46), Color(0xFF34D399)), // Deep Jade
            BlockColorStyle(Color(0xFFA855F7), Color(0xFFE9D5FF), Color(0xFF581C87), Color(0xFFC084FC)), // Imperial Amethyst
            BlockColorStyle(Color(0xFFF97316), Color(0xFFFED7AA), Color(0xFF9A3412), Color(0xFFFB923C)), // Bronze Flame
            BlockColorStyle(Color(0xFF94A3B8), Color(0xFFF1F5F9), Color(0xFF334155), Color(0xFFCBD5E1))  // Platinum Steel
        )
    )

    val neonCyberPulse = BoardThemeSpec(
        id = "neon_cyber_pulse",
        name = "Cyber Neon 3D",
        subtitle = "High-voltage synthwave laser grid & neon blocks",
        premiumPastel = false,
        defaultPriceCoins = 550L,
        boardBgTop = Color(0xFF0D0624),
        boardBgBottom = Color(0xFF060212),
        emptyCellFill = Color(0xFF150B38),
        emptyCellBorder = Color(0xFF2D196B),
        boardFrameBorder = Color(0xFF00F5FF),
        previewAccent = Color(0xFF00F5FF),
        palette = listOf(
            BlockColorStyle(Color(0xFF00F5FF), Color(0xFFC2FCFF), Color(0xFF008B99), Color(0xFF00F5FF)), // Electric Cyan
            BlockColorStyle(Color(0xFFFF00A8), Color(0xFFFFB8E6), Color(0xFF9E0068), Color(0xFFFF33BA)), // Laser Magenta
            BlockColorStyle(Color(0xFF39FF14), Color(0xFFD0FFC7), Color(0xFF1A8F00), Color(0xFF5CFF3D)), // Acid Lime
            BlockColorStyle(Color(0xFFFFE600), Color(0xFFFFF8B3), Color(0xFFA69600), Color(0xFFFFEC3D)), // Volt Yellow
            BlockColorStyle(Color(0xFF9D4EDD), Color(0xFFE5CCFF), Color(0xFF4A1880), Color(0xFFB57BFF)), // Hyper Violet
            BlockColorStyle(Color(0xFFFF4D00), Color(0xFFFFC7AD), Color(0xFF992E00), Color(0xFFFF6E2E)), // Plasma Orange
            BlockColorStyle(Color(0xFF2979FF), Color(0xFFBDD6FF), Color(0xFF0039A6), Color(0xFF5C9CFF))  // Arc Blue
        )
    )

    val specialGalacticPrism = BoardThemeSpec(
        id = "special_galactic_prism",
        name = "Galactic Prism Edition",
        subtitle = "Special Edition cosmic stardust & nebula crystal theme",
        premiumPastel = false,
        defaultPriceCoins = 950L,
        boardBgTop = Color(0xFF120C36),
        boardBgBottom = Color(0xFF06041A),
        emptyCellFill = Color(0xFF1C144D),
        emptyCellBorder = Color(0xFF392B80),
        boardFrameBorder = Color(0xFFE879F9),
        previewAccent = Color(0xFFE879F9),
        palette = listOf(
            BlockColorStyle(Color(0xFFE879F9), Color(0xFFFDF4FF), Color(0xFFA21CAF), Color(0xFFF0ABFC)), // Supernova Pink
            BlockColorStyle(Color(0xFF38BDF8), Color(0xFFE0F2FE), Color(0xFF0369A1), Color(0xFF7DD3FC)), // Starlight Blue
            BlockColorStyle(Color(0xFFFBBF24), Color(0xFFFEF3C7), Color(0xFFB45309), Color(0xFFFDE68A)), // Solar Corona
            BlockColorStyle(Color(0xFF2DD4BF), Color(0xFFCCFBF1), Color(0xFF0F766E), Color(0xFF5EEAD4)), // Nebula Teal
            BlockColorStyle(Color(0xFF818CF8), Color(0xFFE0E7FF), Color(0xFF3730A3), Color(0xFFA5B4FC)), // Pulsar Indigo
            BlockColorStyle(Color(0xFFFB7185), Color(0xFFFFE4E6), Color(0xFFBE123C), Color(0xFFFDA4AF)), // Red Giant
            BlockColorStyle(Color(0xFF4ADE80), Color(0xFFDCFCE7), Color(0xFF15803D), Color(0xFF86EFAC))  // Comet Emerald
        )
    )

    val allThemes: List<BoardThemeSpec> = listOf(
        simpleClassic,
        pastelMacaron,
        pastelSakura,
        pastelAurora,
        pastelVelvet,
        darkObsidianGold,
        neonCyberPulse,
        specialGalacticPrism
    )

    fun byId(themeId: String): BoardThemeSpec =
        allThemes.firstOrNull { it.id == themeId } ?: simpleClassic

    fun defaultShopCatalog(): List<ShopItem> = CosmeticCatalog.defaultFullShopCatalog()

    fun defaultDailyRewards(): List<DailyRewardConfig> = listOf(
        DailyRewardConfig("day_1", 1L, "Day 1 Spark", "Start your cycle with bonus Coins", 15L, "", true),
        DailyRewardConfig("day_2", 2L, "Day 2 Gleam", "Keep your rhythm going — progress never resets", 25L, "Prism Emblem", true),
        DailyRewardConfig("day_3", 3L, "Day 3 Crystal", "Mid-cycle Coin boost", 40L, "", true),
        DailyRewardConfig("day_4", 4L, "Day 4 Radiance", "Shiny mid-week reward", 55L, "Glow Trail", true),
        DailyRewardConfig("day_5", 5L, "Day 5 Quartz", "Generous Coin stash", 75L, "", true),
        DailyRewardConfig("day_6", 6L, "Day 6 Aurora", "High-value pre-finale gift", 100L, "Aurora Frame", true),
        DailyRewardConfig("day_7", 7L, "Day 7 Prism Jackpot", "Cycle finale reward before cycling back to Day 1", 200L, "Prism Crown", true)
    )
}
