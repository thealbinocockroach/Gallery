package com.example.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight

object FontHelper {
    fun getFontFamilyForId(fontId: String): FontFamily {
        return when (fontId) {
            "font_neo_black" -> FontFamily.SansSerif
            "font_editorial_serif" -> FontFamily.Serif
            "font_retro_mono" -> FontFamily.Monospace
            "font_bebas_impact" -> FontFamily.SansSerif
            "font_marker_brush" -> FontFamily.Cursive
            "font_grotesk_clean" -> FontFamily.SansSerif
            "font_script_flow" -> FontFamily.Cursive
            else -> FontFamily.Default
        }
    }

    fun getFontWeightForId(fontId: String): FontWeight {
        return when (fontId) {
            "font_neo_black" -> FontWeight.Black
            "font_editorial_serif" -> FontWeight.Bold
            "font_bebas_impact" -> FontWeight.ExtraBold
            "font_retro_mono" -> FontWeight.SemiBold
            "font_grotesk_clean" -> FontWeight.Medium
            else -> FontWeight.Bold
        }
    }

    fun getFontStyleForId(fontId: String): FontStyle {
        return when (fontId) {
            "font_editorial_serif" -> FontStyle.Italic
            "font_script_flow" -> FontStyle.Italic
            else -> FontStyle.Normal
        }
    }
}
