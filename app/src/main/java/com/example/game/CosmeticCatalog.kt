package com.example.game

import androidx.compose.ui.graphics.Color
import com.example.data.model.BoosterType
import com.example.data.model.ShopCategory
import com.example.data.model.ShopItem

enum class BlockSkinRenderStyle {
    DEFAULT_GLOSSY,
    NEON,
    DIAMOND,
    GOLD,
    RAINBOW,
    CRYSTAL,
    FIRE,
    ICE,
    HOLOGRAM_3D,
    ROYAL_EMERALD_EX,
    COSMIC_ECLIPSE_EX
}

data class BlockSkinSpec(
    val key: String,
    val name: String,
    val renderStyle: BlockSkinRenderStyle,
    val primaryAccent: Color,
    val secondaryAccent: Color,
    val exclusive: Boolean = false
)

enum class BoardBackgroundAnimation {
    NONE,
    STARFIELD,
    PRISM_WAVE,
    AURORA_PULSE,
    COSMIC_NEBULA
}

data class BoardBackgroundSpec(
    val key: String,
    val name: String,
    val styleLabel: String,
    val topColor: Color,
    val midColor: Color,
    val bottomColor: Color,
    val accentGlow: Color,
    val animation: BoardBackgroundAnimation = BoardBackgroundAnimation.NONE,
    val exclusive: Boolean = false
)

enum class SpecialEffectStyle {
    DEFAULT,
    GLOW_AURA,
    FIRE_BURST,
    LIGHTNING_ARC,
    RAINBOW_WAVE,
    STARDUST_PARTICLES,
    LINE_SHOCKWAVE,
    CRYSTAL_PLACEMENT,
    SUPERNOVA_EX
}

data class SpecialEffectSpec(
    val key: String,
    val name: String,
    val style: SpecialEffectStyle,
    val primaryColor: Color,
    val secondaryColor: Color,
    val exclusive: Boolean = false
)

data class AvatarSpec(
    val key: String,
    val name: String,
    val badgeSymbol: String,
    val bgStart: Color,
    val bgEnd: Color,
    val accent: Color,
    val exclusive: Boolean = false
)

data class ProfileFrameSpec(
    val key: String,
    val name: String,
    val ringStart: Color,
    val ringEnd: Color,
    val glowColor: Color,
    val borderWidthDp: Float = 3.5f,
    val exclusive: Boolean = false
)

data class PlayerBadgeSpec(
    val key: String,
    val name: String,
    val shortTitle: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val exclusive: Boolean = false
)

object CosmeticCatalog {

    val defaultSkin = BlockSkinSpec("skin_default", "Classic Glossy 3D", BlockSkinRenderStyle.DEFAULT_GLOSSY, Color(0xFF38E8FF), Color(0xFF8B5CF6))

    val blockSkins: List<BlockSkinSpec> = listOf(
        BlockSkinSpec("skin_neon", "Neon Laser Blocks", BlockSkinRenderStyle.NEON, Color(0xFF00F5FF), Color(0xFFFF00A8)),
        BlockSkinSpec("skin_diamond", "Diamond Facet 3D", BlockSkinRenderStyle.DIAMOND, Color(0xFFB9F2FF), Color(0xFF38BDF8)),
        BlockSkinSpec("skin_gold", "24K Bullion Gold", BlockSkinRenderStyle.GOLD, Color(0xFFFFDF73), Color(0xFFB45309)),
        BlockSkinSpec("skin_rainbow", "Rainbow Spectrum", BlockSkinRenderStyle.RAINBOW, Color(0xFFFF5D8F), Color(0xFF38E8FF)),
        BlockSkinSpec("skin_crystal", "Quartz Crystal 3D", BlockSkinRenderStyle.CRYSTAL, Color(0xFFE9D5FF), Color(0xFF7C3AED)),
        BlockSkinSpec("skin_fire", "Magma Fire Blocks", BlockSkinRenderStyle.FIRE, Color(0xFFFF7A18), Color(0xFFDC2626)),
        BlockSkinSpec("skin_ice", "Glacier Ice Blocks", BlockSkinRenderStyle.ICE, Color(0xFFBAE6FD), Color(0xFF0284C7)),
        BlockSkinSpec("skin_hologram_3d", "Hologram Prism 3D", BlockSkinRenderStyle.HOLOGRAM_3D, Color(0xFF5EEAD4), Color(0xFFC084FC)),
        BlockSkinSpec("skin_royal_emerald_ex", "Royal Emerald Vault", BlockSkinRenderStyle.ROYAL_EMERALD_EX, Color(0xFF34D399), Color(0xFFF6C644), exclusive = true),
        BlockSkinSpec("skin_cosmic_eclipse_ex", "Cosmic Eclipse 3D", BlockSkinRenderStyle.COSMIC_ECLIPSE_EX, Color(0xFFE879F9), Color(0xFF38E8FF), exclusive = true)
    )

    val defaultBackground = BoardBackgroundSpec(
        key = "bg_default",
        name = "Midnight Prism",
        styleLabel = "Default",
        topColor = Color(0xFF15122B),
        midColor = Color(0xFF0E0B1E),
        bottomColor = Color(0xFF090714),
        accentGlow = Color(0xFF38E8FF)
    )

    val boardBackgrounds: List<BoardBackgroundSpec> = listOf(
        BoardBackgroundSpec("bg_dark_obsidian", "Obsidian Shadow", "Dark", Color(0xFF09090D), Color(0xFF12101A), Color(0xFF050508), Color(0xFFF6C644)),
        BoardBackgroundSpec("bg_gradient_aurora", "Aurora Sky Gradient", "Gradient", Color(0xFF0F2942), Color(0xFF1E1B4B), Color(0xFF3B0764), Color(0xFF38E8FF)),
        BoardBackgroundSpec("bg_colorful_candy", "Sunset Prism Pop", "Colorful", Color(0xFF4C1D95), Color(0xFF9D174D), Color(0xFF9A3412), Color(0xFFF472B6)),
        BoardBackgroundSpec("bg_premium_velvet", "Imperial Velvet Gold", "Premium", Color(0xFF1E1233), Color(0xFF2E1065), Color(0xFF170B2B), Color(0xFFF6C644)),
        BoardBackgroundSpec("bg_anim_starfield", "Animated Starfield", "Animated", Color(0xFF070A1F), Color(0xFF11193F), Color(0xFF050817), Color(0xFF7DD3FC), BoardBackgroundAnimation.STARFIELD),
        BoardBackgroundSpec("bg_anim_prism_wave", "Animated Prism Wave", "Animated", Color(0xFF120A33), Color(0xFF1D104F), Color(0xFF0B1F3A), Color(0xFFE879F9), BoardBackgroundAnimation.PRISM_WAVE),
        BoardBackgroundSpec("bg_exclusive_nebula", "Seasonal Aurora Nebula", "Exclusive Animated", Color(0xFF0D0526), Color(0xFF280B4A), Color(0xFF07243B), Color(0xFF2DD4BF), BoardBackgroundAnimation.COSMIC_NEBULA, exclusive = true)
    )

    val defaultEffect = SpecialEffectSpec("fx_default", "Classic Spark", SpecialEffectStyle.DEFAULT, Color(0xFF38E8FF), Color(0xFFF6C644))

    val specialEffects: List<SpecialEffectSpec> = listOf(
        SpecialEffectSpec("fx_glow", "Radiant Glow Aura", SpecialEffectStyle.GLOW_AURA, Color(0xFF38E8FF), Color(0xFFA5F3FC)),
        SpecialEffectSpec("fx_fire", "Inferno Blaze Clear", SpecialEffectStyle.FIRE_BURST, Color(0xFFFF6B00), Color(0xFFFACC15)),
        SpecialEffectSpec("fx_lightning", "Volt Lightning Arc", SpecialEffectStyle.LIGHTNING_ARC, Color(0xFF00F5FF), Color(0xFFFFFFFF)),
        SpecialEffectSpec("fx_rainbow", "Rainbow Prism Wave", SpecialEffectStyle.RAINBOW_WAVE, Color(0xFFFF5D8F), Color(0xFF34D399)),
        SpecialEffectSpec("fx_particles", "Stardust Confetti", SpecialEffectStyle.STARDUST_PARTICLES, Color(0xFFF6C644), Color(0xFFE879F9)),
        SpecialEffectSpec("fx_line_shockwave", "Sonic Line Shockwave", SpecialEffectStyle.LINE_SHOCKWAVE, Color(0xFF818CF8), Color(0xFF38E8FF)),
        SpecialEffectSpec("fx_crystal_place", "Crystal Placement Ripple", SpecialEffectStyle.CRYSTAL_PLACEMENT, Color(0xFF5EEAD4), Color(0xFFF9A8D4)),
        SpecialEffectSpec("fx_exclusive_supernova", "Supernova Finale FX", SpecialEffectStyle.SUPERNOVA_EX, Color(0xFFF6C644), Color(0xFFFF00A8), exclusive = true)
    )

    val avatars: List<AvatarSpec> = listOf(
        AvatarSpec("avatar_cyber_fox", "Cyber Fox", "FOX", Color(0xFF00F5FF), Color(0xFF6366F1), Color(0xFF38E8FF)),
        AvatarSpec("avatar_prism_knight", "Prism Knight", "KNT", Color(0xFF8B5CF6), Color(0xFFEC4899), Color(0xFFF9A8D4)),
        AvatarSpec("avatar_crystal_mage", "Crystal Mage", "MGE", Color(0xFF06B6D4), Color(0xFF10B981), Color(0xFF5EEAD4)),
        AvatarSpec("avatar_golden_sovereign", "Golden Sovereign", "KNG", Color(0xFFF6C644), Color(0xFFD97706), Color(0xFFFEF08A)),
        AvatarSpec("avatar_neon_dragon", "Neon Dragon", "DRG", Color(0xFFFF3366), Color(0xFF7C3AED), Color(0xFFFF5D8F)),
        AvatarSpec("avatar_exclusive_phoenix", "Solar Phoenix", "PHX", Color(0xFFFF6B00), Color(0xFFF6C644), Color(0xFFFFF3C4), exclusive = true)
    )

    val profileFrames: List<ProfileFrameSpec> = listOf(
        ProfileFrameSpec("frame_neon_cyan", "Cyber Neon Ring", Color(0xFF00F5FF), Color(0xFF8B5CF6), Color(0xFF38E8FF)),
        ProfileFrameSpec("frame_royal_gold", "Royal 24K Gold Frame", Color(0xFFFFDF73), Color(0xFFB45309), Color(0xFFF6C644), 4.0f),
        ProfileFrameSpec("frame_sakura_bloom", "Sakura Silk Frame", Color(0xFFFBCFE8), Color(0xFFEC4899), Color(0xFFF472B6)),
        ProfileFrameSpec("frame_diamond_prism", "Diamond Prism Halo", Color(0xFFE0F2FE), Color(0xFF38BDF8), Color(0xFF7DD3FC), 4.0f),
        ProfileFrameSpec("frame_exclusive_celestial", "Celestial Sovereign Ring", Color(0xFFE879F9), Color(0xFFF6C644), Color(0xFFF0ABFC), 4.5f, exclusive = true)
    )

    val playerBadges: List<PlayerBadgeSpec> = listOf(
        PlayerBadgeSpec("badge_prism_crown", "Prism Profile Crown", "PRISM CROWN", Color(0xFFF6C644), Color(0xFFD97706)),
        PlayerBadgeSpec("badge_grandmaster", "Block Grandmaster", "GRANDMASTER", Color(0xFF38E8FF), Color(0xFF6366F1)),
        PlayerBadgeSpec("badge_combo_king", "Multi-Line Legend", "COMBO PRO", Color(0xFFFF5D8F), Color(0xFF9333EA)),
        PlayerBadgeSpec("badge_speed_blitz", "Time Blitz Champion", "BLITZ ACE", Color(0xFF34D399), Color(0xFF0891B2)),
        PlayerBadgeSpec("badge_exclusive_founder", "Season 1 Collector", "LIMITED S1", Color(0xFFE879F9), Color(0xFFF6C644), exclusive = true)
    )

    fun blockSkinByKey(key: String): BlockSkinSpec =
        blockSkins.firstOrNull { it.key == key } ?: defaultSkin

    fun backgroundByKey(key: String): BoardBackgroundSpec =
        boardBackgrounds.firstOrNull { it.key == key } ?: defaultBackground

    fun effectByKey(key: String): SpecialEffectSpec =
        specialEffects.firstOrNull { it.key == key } ?: defaultEffect

    fun avatarByKey(key: String): AvatarSpec? =
        avatars.firstOrNull { it.key == key }

    fun frameByKey(key: String): ProfileFrameSpec? =
        profileFrames.firstOrNull { it.key == key }

    fun badgeByKey(key: String): PlayerBadgeSpec? =
        playerBadges.firstOrNull { it.key == key }

    fun defaultShopCategories(): List<ShopCategory> = listOf(
        ShopCategory("cat_theme", "THEME", "Themes", "Simple, Pastel, Dark, Neon & Special Edition board themes", "palette", "#F9A8D4", true, 1L),
        ShopCategory("cat_block_skin", "BLOCK_SKIN", "Block Skins", "Neon, Diamond, Gold, Rainbow, Crystal, Fire & Ice 3D blocks", "cube", "#38E8FF", true, 2L),
        ShopCategory("cat_board_bg", "BOARD_BACKGROUND", "Board Backgrounds", "Dark, Gradient, Colorful, Premium & Animated boards", "wallpaper", "#8B5CF6", true, 3L),
        ShopCategory("cat_special_fx", "SPECIAL_EFFECT", "Special Effects", "Glow, Fire, Lightning, Rainbow & Particle clear animations", "sparkles", "#FF5D8F", true, 4L),
        ShopCategory("cat_booster", "GAME_BOOSTER", "Game Boosters", "Hammer, Undo, Shuffle & Line Blaster gameplay power-ups", "bolt", "#F6C644", true, 5L),
        ShopCategory("cat_avatar", "AVATAR", "Avatars", "Premium stylized player avatars", "person", "#34D399", true, 6L),
        ShopCategory("cat_frame", "PROFILE_FRAME", "Profile Frames", "Glowing luxury borders around your player avatar", "frame", "#7DD3FC", true, 7L),
        ShopCategory("cat_badge", "BADGE", "Player Badges", "Prestige titles & collector emblems for profile & leaderboards", "badge", "#FBBF24", true, 8L),
        ShopCategory("cat_exclusive", "EXCLUSIVE", "Exclusive Items", "Limited Edition 3D designs & Seasonal cosmetic collections", "diamond", "#E879F9", true, 9L)
    )

    fun defaultFullShopCatalog(): List<ShopItem> = listOf(
        // ==================== A. THEMES ====================
        ShopItem(
            id = "theme_simple_classic",
            name = BlockThemes.simpleClassic.name,
            description = BlockThemes.simpleClassic.subtitle,
            category = "THEME",
            subType = "SIMPLE",
            themeKey = BlockThemes.simpleClassic.id,
            assetKey = BlockThemes.simpleClassic.id,
            priceCoins = 0L,
            premiumPastel = false,
            featured = false,
            available = true,
            sortOrder = 1L,
            accentHex = "#38E8FF"
        ),
        ShopItem(
            id = "theme_pastel_macaron",
            name = BlockThemes.pastelMacaron.name,
            description = BlockThemes.pastelMacaron.subtitle,
            category = "THEME",
            subType = "PASTEL",
            themeKey = BlockThemes.pastelMacaron.id,
            assetKey = BlockThemes.pastelMacaron.id,
            priceCoins = 250L,
            premiumPastel = true,
            featured = true,
            available = true,
            sortOrder = 2L,
            accentHex = "#F9A8D4"
        ),
        ShopItem(
            id = "theme_pastel_sakura",
            name = BlockThemes.pastelSakura.name,
            description = BlockThemes.pastelSakura.subtitle,
            category = "THEME",
            subType = "PASTEL",
            themeKey = BlockThemes.pastelSakura.id,
            assetKey = BlockThemes.pastelSakura.id,
            priceCoins = 400L,
            premiumPastel = true,
            available = true,
            sortOrder = 3L,
            accentHex = "#F472B6"
        ),
        ShopItem(
            id = "theme_pastel_aurora",
            name = BlockThemes.pastelAurora.name,
            description = BlockThemes.pastelAurora.subtitle,
            category = "THEME",
            subType = "PASTEL",
            themeKey = BlockThemes.pastelAurora.id,
            assetKey = BlockThemes.pastelAurora.id,
            priceCoins = 600L,
            premiumPastel = true,
            available = true,
            sortOrder = 4L,
            accentHex = "#7DD3FC"
        ),
        ShopItem(
            id = "theme_pastel_velvet",
            name = BlockThemes.pastelVelvet.name,
            description = BlockThemes.pastelVelvet.subtitle,
            category = "THEME",
            subType = "PASTEL",
            themeKey = BlockThemes.pastelVelvet.id,
            assetKey = BlockThemes.pastelVelvet.id,
            priceCoins = 850L,
            premiumPastel = true,
            available = true,
            sortOrder = 5L,
            accentHex = "#C4B5FD"
        ),
        ShopItem(
            id = "theme_dark_obsidian_gold",
            name = BlockThemes.darkObsidianGold.name,
            description = BlockThemes.darkObsidianGold.subtitle,
            category = "THEME",
            subType = "DARK",
            themeKey = BlockThemes.darkObsidianGold.id,
            assetKey = BlockThemes.darkObsidianGold.id,
            priceCoins = 450L,
            featured = true,
            newArrival = true,
            available = true,
            sortOrder = 6L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "theme_neon_cyber_pulse",
            name = BlockThemes.neonCyberPulse.name,
            description = BlockThemes.neonCyberPulse.subtitle,
            category = "THEME",
            subType = "NEON",
            themeKey = BlockThemes.neonCyberPulse.id,
            assetKey = BlockThemes.neonCyberPulse.id,
            priceCoins = 550L,
            newArrival = true,
            available = true,
            sortOrder = 7L,
            accentHex = "#00F5FF"
        ),
        ShopItem(
            id = "theme_special_galactic_prism",
            name = BlockThemes.specialGalacticPrism.name,
            description = BlockThemes.specialGalacticPrism.subtitle,
            category = "THEME",
            subType = "SPECIAL",
            themeKey = BlockThemes.specialGalacticPrism.id,
            assetKey = BlockThemes.specialGalacticPrism.id,
            priceCoins = 950L,
            featured = true,
            limitedEdition = true,
            available = true,
            sortOrder = 8L,
            accentHex = "#E879F9"
        ),

        // ==================== B. BLOCK SKINS ====================
        ShopItem(
            id = "skin_neon_blocks",
            name = "Neon Blocks",
            description = "Electric glowing tube borders and intense neon core reflections on every 3D block.",
            category = "BLOCK_SKIN",
            subType = "NEON",
            assetKey = "skin_neon",
            priceCoins = 320L,
            featured = true,
            newArrival = true,
            available = true,
            sortOrder = 10L,
            accentHex = "#00F5FF"
        ),
        ShopItem(
            id = "skin_diamond_blocks",
            name = "Diamond Blocks",
            description = "Precision-cut diamond facets with razor specular gleams across all 10×10 board pieces.",
            category = "BLOCK_SKIN",
            subType = "DIAMOND",
            assetKey = "skin_diamond",
            priceCoins = 520L,
            featured = true,
            available = true,
            sortOrder = 11L,
            accentHex = "#38BDF8"
        ),
        ShopItem(
            id = "skin_gold_blocks",
            name = "Gold Blocks",
            description = "24K metallic brushed gold blocks with warm forged sheen and gleaming bevel edges.",
            category = "BLOCK_SKIN",
            subType = "GOLD",
            assetKey = "skin_gold",
            priceCoins = 600L,
            available = true,
            sortOrder = 12L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "skin_rainbow_blocks",
            name = "Rainbow Blocks",
            description = "Full-spectrum iridescent gradient blocks that shimmer diagonally across the grid.",
            category = "BLOCK_SKIN",
            subType = "RAINBOW",
            assetKey = "skin_rainbow",
            priceCoins = 480L,
            newArrival = true,
            available = true,
            sortOrder = 13L,
            accentHex = "#FF5D8F"
        ),
        ShopItem(
            id = "skin_crystal_blocks",
            name = "Crystal Blocks",
            description = "Translucent amethyst & quartz prism cubes with inner light refraction planes.",
            category = "BLOCK_SKIN",
            subType = "CRYSTAL",
            assetKey = "skin_crystal",
            priceCoins = 420L,
            available = true,
            sortOrder = 14L,
            accentHex = "#C084FC"
        ),
        ShopItem(
            id = "skin_fire_blocks",
            name = "Fire Blocks",
            description = "Molten volcanic magma cubes glowing with blazing ember cores and hot rims.",
            category = "BLOCK_SKIN",
            subType = "FIRE",
            assetKey = "skin_fire",
            priceCoins = 380L,
            available = true,
            sortOrder = 15L,
            accentHex = "#FF7A18"
        ),
        ShopItem(
            id = "skin_ice_blocks",
            name = "Ice Blocks",
            description = "Crystalline glacier blocks with frosted edges and crisp arctic specular shine.",
            category = "BLOCK_SKIN",
            subType = "ICE",
            assetKey = "skin_ice",
            priceCoins = 360L,
            available = true,
            sortOrder = 16L,
            accentHex = "#7DD3FC"
        ),
        ShopItem(
            id = "skin_hologram_3d",
            name = "Hologram Prism 3D",
            description = "Special 3D holographic foil block design with dual-tone prism scanlines.",
            category = "BLOCK_SKIN",
            subType = "SPECIAL_3D",
            assetKey = "skin_hologram_3d",
            priceCoins = 750L,
            featured = true,
            newArrival = true,
            available = true,
            sortOrder = 17L,
            accentHex = "#5EEAD4"
        ),

        // ==================== C. BOARD BACKGROUNDS ====================
        ShopItem(
            id = "bg_dark_obsidian",
            name = "Obsidian Shadow Board",
            description = "Ultra-clean deep dark backdrop engineered for maximum block contrast.",
            category = "BOARD_BACKGROUND",
            subType = "DARK",
            assetKey = "bg_dark_obsidian",
            priceCoins = 180L,
            available = true,
            sortOrder = 20L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "bg_gradient_aurora",
            name = "Aurora Sky Gradient",
            description = "Luminous arctic teal-to-violet atmospheric gradient behind your puzzle board.",
            category = "BOARD_BACKGROUND",
            subType = "GRADIENT",
            assetKey = "bg_gradient_aurora",
            priceCoins = 240L,
            available = true,
            sortOrder = 21L,
            accentHex = "#38E8FF"
        ),
        ShopItem(
            id = "bg_colorful_candy",
            name = "Sunset Prism Pop",
            description = "Vibrant multicolor magenta-amber backdrop bursting with warm arcade energy.",
            category = "BOARD_BACKGROUND",
            subType = "COLORFUL",
            assetKey = "bg_colorful_candy",
            priceCoins = 280L,
            newArrival = true,
            available = true,
            sortOrder = 22L,
            accentHex = "#F472B6"
        ),
        ShopItem(
            id = "bg_premium_velvet",
            name = "Imperial Velvet Gold",
            description = "Premium royal purple studio backdrop framed by subtle golden aura lighting.",
            category = "BOARD_BACKGROUND",
            subType = "PREMIUM",
            assetKey = "bg_premium_velvet",
            priceCoins = 380L,
            available = true,
            sortOrder = 23L,
            accentHex = "#C4B5FD"
        ),
        ShopItem(
            id = "bg_anim_starfield",
            name = "Animated Starfield",
            description = "Live animated twinkling deep-space stars drifting smoothly behind the 10×10 grid.",
            category = "BOARD_BACKGROUND",
            subType = "ANIMATED",
            assetKey = "bg_anim_starfield",
            priceCoins = 520L,
            featured = true,
            newArrival = true,
            available = true,
            sortOrder = 24L,
            accentHex = "#7DD3FC"
        ),
        ShopItem(
            id = "bg_anim_prism_wave",
            name = "Animated Prism Wave",
            description = "Flowing live neon aurora waves pulsing rhythmically across the gameplay arena.",
            category = "BOARD_BACKGROUND",
            subType = "ANIMATED",
            assetKey = "bg_anim_prism_wave",
            priceCoins = 640L,
            featured = true,
            available = true,
            sortOrder = 25L,
            accentHex = "#E879F9"
        ),

        // ==================== D. SPECIAL EFFECTS ====================
        ShopItem(
            id = "fx_glow_aura",
            name = "Glow Effect",
            description = "Soft radiant cyan aura halos radiating from every block placement & cleared row.",
            category = "SPECIAL_EFFECT",
            subType = "GLOW",
            assetKey = "fx_glow",
            priceCoins = 220L,
            available = true,
            sortOrder = 30L,
            accentHex = "#38E8FF"
        ),
        ShopItem(
            id = "fx_fire_burst",
            name = "Fire Effect",
            description = "Blazing amber & crimson flame waves erupting across every cleared line.",
            category = "SPECIAL_EFFECT",
            subType = "FIRE",
            assetKey = "fx_fire",
            priceCoins = 340L,
            featured = true,
            available = true,
            sortOrder = 31L,
            accentHex = "#FF6B00"
        ),
        ShopItem(
            id = "fx_lightning_arc",
            name = "Lightning Effect",
            description = "High-voltage electric lightning bolts crackling along cleared rows & columns.",
            category = "SPECIAL_EFFECT",
            subType = "LIGHTNING",
            assetKey = "fx_lightning",
            priceCoins = 420L,
            newArrival = true,
            available = true,
            sortOrder = 32L,
            accentHex = "#00F5FF"
        ),
        ShopItem(
            id = "fx_rainbow_wave",
            name = "Rainbow Effect",
            description = "Prismatic full-spectrum wave sweeping across completed lines & combos.",
            category = "SPECIAL_EFFECT",
            subType = "RAINBOW",
            assetKey = "fx_rainbow",
            priceCoins = 390L,
            available = true,
            sortOrder = 33L,
            accentHex = "#FF5D8F"
        ),
        ShopItem(
            id = "fx_particle_stardust",
            name = "Particle Stardust FX",
            description = "Sparkling multi-colored 3D stardust bursts exploding from cleared cells.",
            category = "SPECIAL_EFFECT",
            subType = "PARTICLE",
            assetKey = "fx_particles",
            priceCoins = 300L,
            available = true,
            sortOrder = 34L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "fx_line_shockwave",
            name = "Sonic Line-Clear FX",
            description = "Special expanding shockwave ring animation amplifying multi-line clears.",
            category = "SPECIAL_EFFECT",
            subType = "LINE_CLEAR",
            assetKey = "fx_line_shockwave",
            priceCoins = 460L,
            newArrival = true,
            available = true,
            sortOrder = 35L,
            accentHex = "#818CF8"
        ),
        ShopItem(
            id = "fx_crystal_placement",
            name = "Crystal Placement FX",
            description = "Premium expanding crystalline ripple effect triggered whenever a block snaps in.",
            category = "SPECIAL_EFFECT",
            subType = "PLACEMENT",
            assetKey = "fx_crystal_place",
            priceCoins = 360L,
            available = true,
            sortOrder = 36L,
            accentHex = "#5EEAD4"
        ),

        // ==================== E. GAME BOOSTERS ====================
        ShopItem(
            id = "booster_hammer_pack",
            name = "Hammer Booster (×3 Pack)",
            description = "Real gameplay power-up: tap any occupied block on the 10×10 board to smash it (plus adjacent blocks in configured radius) to escape tight spots.",
            category = "GAME_BOOSTER",
            subType = BoosterType.HAMMER.wireName,
            assetKey = BoosterType.HAMMER.wireName,
            priceCoins = 120L,
            featured = true,
            available = true,
            consumable = true,
            boosterPackQty = 3L,
            boosterMaxPerGame = 3L,
            boosterPowerRadius = 1L,
            sortOrder = 40L,
            accentHex = "#FF6B6B"
        ),
        ShopItem(
            id = "booster_undo_pack",
            name = "Undo Move (×3 Pack)",
            description = "Real gameplay power-up: reverse your last valid block placement, restoring the exact board, tray piece, and score.",
            category = "GAME_BOOSTER",
            subType = BoosterType.UNDO.wireName,
            assetKey = BoosterType.UNDO.wireName,
            priceCoins = 90L,
            featured = true,
            available = true,
            consumable = true,
            boosterPackQty = 3L,
            boosterMaxPerGame = 3L,
            boosterPowerRadius = 0L,
            sortOrder = 41L,
            accentHex = "#38E8FF"
        ),
        ShopItem(
            id = "booster_shuffle_pack",
            name = "Tray Shuffle (×3 Pack)",
            description = "Real gameplay power-up: replace all remaining tray blocks with a fresh playable set while keeping your board intact.",
            category = "GAME_BOOSTER",
            subType = BoosterType.SHUFFLE.wireName,
            assetKey = BoosterType.SHUFFLE.wireName,
            priceCoins = 100L,
            newArrival = true,
            available = true,
            consumable = true,
            boosterPackQty = 3L,
            boosterMaxPerGame = 3L,
            boosterPowerRadius = 0L,
            sortOrder = 42L,
            accentHex = "#34D399"
        ),
        ShopItem(
            id = "booster_row_blaster_pack",
            name = "Line Blaster (×2 Pack)",
            description = "Real gameplay power-up: tap any row on the 10×10 grid to instantly vaporize the entire horizontal line and score +10 pts & +1 Coin!",
            category = "GAME_BOOSTER",
            subType = BoosterType.ROW_BLASTER.wireName,
            assetKey = BoosterType.ROW_BLASTER.wireName,
            priceCoins = 180L,
            featured = true,
            newArrival = true,
            available = true,
            consumable = true,
            boosterPackQty = 2L,
            boosterMaxPerGame = 2L,
            boosterPowerRadius = 10L,
            sortOrder = 43L,
            accentHex = "#F6C644"
        ),

        // ==================== F. PROFILE ITEMS (AVATARS, FRAMES, BADGES) ====================
        ShopItem(
            id = "avatar_cyber_fox",
            name = "Cyber Fox Avatar",
            description = "Neon holographic fox portrait displayed on your Profile & Home card.",
            category = "AVATAR",
            subType = "PREMIUM_AVATAR",
            assetKey = "avatar_cyber_fox",
            priceCoins = 200L,
            newArrival = true,
            available = true,
            sortOrder = 50L,
            accentHex = "#38E8FF"
        ),
        ShopItem(
            id = "avatar_prism_knight",
            name = "Prism Knight Avatar",
            description = "Armored amethyst puzzle guardian crest for your player profile.",
            category = "AVATAR",
            subType = "PREMIUM_AVATAR",
            assetKey = "avatar_prism_knight",
            priceCoins = 260L,
            available = true,
            sortOrder = 51L,
            accentHex = "#F472B6"
        ),
        ShopItem(
            id = "avatar_golden_sovereign",
            name = "Golden Sovereign Avatar",
            description = "Prestigious 24K golden crown avatar marking elite Prism Block players.",
            category = "AVATAR",
            subType = "PREMIUM_AVATAR",
            assetKey = "avatar_golden_sovereign",
            priceCoins = 420L,
            featured = true,
            available = true,
            sortOrder = 52L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "frame_neon_cyan",
            name = "Cyber Neon Frame",
            description = "Electric glowing dual-gradient ring framing your profile portrait.",
            category = "PROFILE_FRAME",
            subType = "FRAME",
            assetKey = "frame_neon_cyan",
            priceCoins = 220L,
            available = true,
            sortOrder = 55L,
            accentHex = "#00F5FF"
        ),
        ShopItem(
            id = "frame_royal_gold",
            name = "Royal 24K Gold Frame",
            description = "Heavy gilded luxury ring showcasing top-tier profile prestige.",
            category = "PROFILE_FRAME",
            subType = "FRAME",
            assetKey = "frame_royal_gold",
            priceCoins = 380L,
            featured = true,
            available = true,
            sortOrder = 56L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "frame_diamond_prism",
            name = "Diamond Prism Halo",
            description = "Crystalline diamond border radiating cool arctic brilliance.",
            category = "PROFILE_FRAME",
            subType = "FRAME",
            assetKey = "frame_diamond_prism",
            priceCoins = 460L,
            newArrival = true,
            available = true,
            sortOrder = 57L,
            accentHex = "#7DD3FC"
        ),
        ShopItem(
            id = "item_continue_pack",
            name = "Prism Profile Crown",
            description = "Prestige collector badge displayed prominently on your player profile.",
            category = "BADGE",
            subType = "BADGE",
            assetKey = "badge_prism_crown",
            priceCoins = 300L,
            available = true,
            sortOrder = 60L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "badge_grandmaster",
            name = "Grandmaster Emblem",
            description = "Sapphire Grandmaster title badge signaling 10×10 puzzle mastery.",
            category = "BADGE",
            subType = "BADGE",
            assetKey = "badge_grandmaster",
            priceCoins = 350L,
            newArrival = true,
            available = true,
            sortOrder = 61L,
            accentHex = "#38E8FF"
        ),
        ShopItem(
            id = "badge_combo_king",
            name = "Multi-Line Legend Badge",
            description = "Rose-violet badge celebrating explosive simultaneous multi-line clears.",
            category = "BADGE",
            subType = "BADGE",
            assetKey = "badge_combo_king",
            priceCoins = 280L,
            available = true,
            sortOrder = 62L,
            accentHex = "#FF5D8F"
        ),

        // ==================== G. EXCLUSIVE ITEMS ====================
        ShopItem(
            id = "exclusive_royal_emerald_skin",
            name = "Royal Emerald Vault Skin",
            description = "Limited Edition 3D block skin forged from emerald gems & gold inlay bevels.",
            category = "EXCLUSIVE",
            subType = "BLOCK_SKIN",
            assetKey = "skin_royal_emerald_ex",
            priceCoins = 1200L,
            featured = true,
            limitedEdition = true,
            available = true,
            sortOrder = 70L,
            accentHex = "#34D399"
        ),
        ShopItem(
            id = "exclusive_cosmic_eclipse_skin",
            name = "Cosmic Eclipse 3D Skin",
            description = "Seasonal Collector 3D block skin with dark-matter cores and supernova edges.",
            category = "EXCLUSIVE",
            subType = "BLOCK_SKIN",
            assetKey = "skin_cosmic_eclipse_ex",
            priceCoins = 1500L,
            featured = true,
            newArrival = true,
            limitedEdition = true,
            available = true,
            sortOrder = 71L,
            accentHex = "#E879F9"
        ),
        ShopItem(
            id = "exclusive_nebula_bg",
            name = "Seasonal Aurora Nebula",
            description = "Limited Edition animated cosmic nebula backdrop with drifting stardust waves.",
            category = "EXCLUSIVE",
            subType = "BOARD_BACKGROUND",
            assetKey = "bg_exclusive_nebula",
            priceCoins = 1100L,
            limitedEdition = true,
            available = true,
            sortOrder = 72L,
            accentHex = "#2DD4BF"
        ),
        ShopItem(
            id = "exclusive_supernova_fx",
            name = "Supernova Finale FX",
            description = "Exclusive grand-finale multi-burst shockwave & golden flare clear effect.",
            category = "EXCLUSIVE",
            subType = "SPECIAL_EFFECT",
            assetKey = "fx_exclusive_supernova",
            priceCoins = 1350L,
            featured = true,
            limitedEdition = true,
            available = true,
            sortOrder = 73L,
            accentHex = "#F6C644"
        ),
        ShopItem(
            id = "exclusive_celestial_frame",
            name = "Celestial Sovereign Frame",
            description = "Limited Season 1 collector profile ring radiating gold & amethyst stardust.",
            category = "EXCLUSIVE",
            subType = "PROFILE_FRAME",
            assetKey = "frame_exclusive_celestial",
            priceCoins = 980L,
            limitedEdition = true,
            available = true,
            sortOrder = 74L,
            accentHex = "#F0ABFC"
        )
    )
}
