package com.kizitonwose.remindr.compose.multiplatform.sample

import androidx.compose.ui.graphics.Color

// === COLOR PALETTES ===
// Switch active palette by changing `activePalette` below

data class ColorPalette(
    val name: String,
    val noteColors: List<Color>,
    val background: Color = Color(0xFF0E0E0E),
    val surface: Color = Color(0xFF1B1B1B),
    val toolbar: Color = Color(0xFF282828),
    val accent: Color,
    val accentSecondary: Color,
)

object Palettes {
    // Your custom palette - Rosy Copper
    val rosyCopper = ColorPalette(
        name = "Rosy Copper",
        noteColors = listOf(
            Color(0xFFDB504A), // Rosy Copper
            Color(0xFFFF6F59), // Vibrant Coral
            Color(0xFF43AA8B), // Seagrass
            Color(0xFFB2B09B), // Dry Sage
            Color(0xFF254441), // Dark Slate Grey
        ),
        background = Color(0xFF0E0E0E),
        surface = Color(0xFF1A1A1A),
        accent = Color(0xFFDB504A),
        accentSecondary = Color(0xFF43AA8B),
    )

    // Ocean palette
    val ocean = ColorPalette(
        name = "Ocean",
        noteColors = listOf(
            Color(0xFF0077B6),
            Color(0xFF00B4D8),
            Color(0xFF90E0EF),
            Color(0xFF023E8A),
            Color(0xFFCAF0F8),
        ),
        accent = Color(0xFF0077B6),
        accentSecondary = Color(0xFF00B4D8),
    )

    // Sunset palette
    val sunset = ColorPalette(
        name = "Sunset",
        noteColors = listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFA06B),
            Color(0xFFFFD93D),
            Color(0xFF6BCB77),
            Color(0xFF4D96FF),
        ),
        accent = Color(0xFFFF6B6B),
        accentSecondary = Color(0xFFFFD93D),
    )

    // Monochrome palette
    val mono = ColorPalette(
        name = "Mono",
        noteColors = listOf(
            Color(0xFFFFFFFF),
            Color(0xFFB0B0B0),
            Color(0xFF707070),
            Color(0xFF404040),
            Color(0xFF909090),
        ),
        accent = Color(0xFFB0B0B0),
        accentSecondary = Color(0xFF707070),
    )

    // Original palette
    val original = ColorPalette(
        name = "Original",
        noteColors = listOf(
            Color(0xFF1565C0),
            Color(0xFFC62828),
            Color(0xFF5D4037),
            Color(0xFF00796B),
            Color(0xFFEF6C00),
        ),
        accent = Color(0xFF1565C0),
        accentSecondary = Color(0xFF00796B),
    )

    val all = listOf(rosyCopper, ocean, sunset, mono, original)
}

// === CHANGE THIS TO SWITCH PALETTES ===
val activePalette = Palettes.rosyCopper

object Colors {
    val example1Selection = Color(0xFFFCCA3E)
    val example1Bg = Color(0xFF3A284C)
    val example1BgLight = Color(0xFF433254)
    val example1BgSecondary = Color(0xFF51356E)
    val example1WhiteLight = Color(0x4DFFFFFF)
    val example4GrayPast = Color(0xFFBEBEBE)
    val example4Gray = Color(0xFF474747)
    val example5PageBgColor = activePalette.background
    val example5ItemViewBgColor = activePalette.surface
    val example5ToolbarColor = activePalette.toolbar
    val example5TextGrey = Color(0xFFDCDCDC)
    val example5TextGreyLight = Color(0xFF616161)
    val example6MonthBgColor = Color(0xFFB2EBF2)
    val example6MonthBgColor2 = Color(0xFFF2C4B2)
    val example6MonthBgColor3 = Color(0xFFB2B8F2)
    val example7Yellow = Color(0xFFFFEB3B)
    val primary = activePalette.accent
    val accent = activePalette.accent
    val accentSecondary = activePalette.accentSecondary

    val noteColors = activePalette.noteColors
}
