package com.example.ui.screens

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

object FilterHelper {
    val AVAILABLE_FILTERS = listOf(
        "Normal",
        "Vivid",
        "Noir B&W",
        "Vintage Amber",
        "Cyber Cyan",
        "Neo-Pop",
        "Sunset Warm",
        "Cool Fade"
    )

    fun getColorFilter(filterName: String): ColorFilter? {
        return when (filterName) {
            "Vivid" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.3f, 0f, 0f, 0f, -10f,
                        0f, 1.3f, 0f, 0f, -10f,
                        0f, 0f, 1.3f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Noir B&W" -> ColorFilter.colorMatrix(
                ColorMatrix().apply { setToSaturation(0f) }
            )
            "Vintage Amber" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.2f, 0.1f, 0f, 0f, 20f,
                        0.1f, 1.0f, 0f, 0f, 10f,
                        0f, 0f, 0.7f, 0f, -20f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Cyber Cyan" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.7f, 0f, 0.2f, 0f, -15f,
                        0f, 1.2f, 0.1f, 0f, 15f,
                        0.1f, 0.3f, 1.4f, 0f, 30f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Neo-Pop" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.5f, 0f, 0f, 0f, -20f,
                        0f, 1.4f, 0f, 0f, -15f,
                        0f, 0f, 1.1f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Sunset Warm" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        1.3f, 0.1f, 0f, 0f, 25f,
                        0f, 1.0f, 0f, 0f, 5f,
                        0f, 0f, 0.8f, 0f, -10f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            "Cool Fade" -> ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, 10f,
                        0f, 1.0f, 0f, 0f, 15f,
                        0f, 0.1f, 1.2f, 0f, 25f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
            )
            else -> null
        }
    }
}
