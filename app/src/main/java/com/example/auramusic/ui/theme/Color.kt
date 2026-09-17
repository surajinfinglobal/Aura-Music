package com.example.auramusic.ui.theme

import androidx.compose.ui.graphics.Color

val AuraDarkBackground = Color(0xFF090714)
val AuraDarkSurface = Color(0xFF130E26)
val AuraCardBackground = Color(0xFF1C1636)
val AuraCardHover = Color(0xFF261F45)
val AuraBorder = Color(0x33A78BFA)

val AuraPrimary = Color(0xFF7C3AED)
val AuraPrimaryVariant = Color(0xFF8B5CF6)
val AuraSecondary = Color(0xFF06B6D4)
val AuraTertiary = Color(0xFFEC4899)

val AuraTextPrimary = Color(0xFFF9FAFB)
val AuraTextSecondary = Color(0xFF9CA3AF)
val AuraTextMuted = Color(0xFF6B7280)
val AuraError = Color(0xFFEF4444)

data class BrandColor(
    val id: String,
    val name: String,
    val color: Color
)

val BRAND_PALETTES = listOf(
    BrandColor("violet", "Electric Violet", Color(0xFF7C3AED)),
    BrandColor("cyan", "Cyber Cyan", Color(0xFF06B6D4)),
    BrandColor("pink", "Neon Pink", Color(0xFFEC4899)),
    BrandColor("amber", "Solar Amber", Color(0xFFF59E0B)),
    BrandColor("emerald", "Emerald Mint", Color(0xFF10B981)),
    BrandColor("indigo", "Indigo Pulse", Color(0xFF6366F1)),
    BrandColor("orange", "Sunset Orange", Color(0xFFF97316)),
    BrandColor("rose", "Crimson Beat", Color(0xFFF43F5E))
)
