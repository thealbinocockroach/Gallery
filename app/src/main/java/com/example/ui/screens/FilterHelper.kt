package com.example.ui.screens

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.toArgb
import kotlin.math.pow

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

    private val presetCache = HashMap<String, ColorFilter>()

    fun getColorFilter(
        filterName: String,
        brightness: Float = 0f,
        contrast: Float = 0f,
        saturation: Float = 0f,
        warmth: Float = 0f,
        tint: Float = 0f,
        highlights: Float = 0f,
        shadows: Float = 0f,
        whites: Float = 0f,
        blacks: Float = 0f,
        vibrance: Float = 0f,
        filterStrength: Float = 1f
    ): ColorFilter? {
        val key = "$filterName|$brightness|$contrast|$saturation|$warmth|$tint|$highlights|$shadows|$whites|$blacks|$vibrance|$filterStrength"
        val cached = presetCache[key]
        if (cached != null) return cached
        val created = buildColorFilter(
            filterName, brightness, contrast, saturation, warmth,
            tint, highlights, shadows, whites, blacks, vibrance, filterStrength
        )
        if (created != null) presetCache[key] = created
        return created
    }

    /**
     * Public matrix builder for CPU export: same math as the GPU preview filter,
     * returned as a raw 4x5 array so callers can feed android.graphics.ColorMatrix
     * when burning adjustments into saved pixels.
     */
    fun buildAdjustmentMatrix(
        filterName: String,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float,
        tint: Float = 0f,
        highlights: Float = 0f,
        shadows: Float = 0f,
        whites: Float = 0f,
        blacks: Float = 0f,
        vibrance: Float = 0f,
        filterStrength: Float = 1f
    ): FloatArray {
        val base = when (filterName) {
            "Vivid" -> floatArrayOf(
                1.3f, 0f, 0f, 0f, -10f,
                0f, 1.3f, 0f, 0f, -10f,
                0f, 0f, 1.3f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            "Noir B&W" -> colorMatrixToArray(ColorMatrix().apply { setToSaturation(0f) })
            "Vintage Amber" -> floatArrayOf(
                1.2f, 0.1f, 0f, 0f, 20f,
                0.1f, 1.0f, 0f, 0f, 10f,
                0f, 0f, 0.7f, 0f, -20f,
                0f, 0f, 0f, 1f, 0f
            )
            "Cyber Cyan" -> floatArrayOf(
                0.7f, 0f, 0.2f, 0f, -15f,
                0f, 1.2f, 0.1f, 0f, 15f,
                0.1f, 0.3f, 1.4f, 0f, 30f,
                0f, 0f, 0f, 1f, 0f
            )
            "Neo-Pop" -> floatArrayOf(
                1.5f, 0f, 0f, 0f, -20f,
                0f, 1.4f, 0f, 0f, -15f,
                0f, 0f, 1.1f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            "Sunset Warm" -> floatArrayOf(
                1.3f, 0.1f, 0f, 0f, 25f,
                0f, 1.0f, 0f, 0f, 5f,
                0f, 0f, 0.8f, 0f, -10f,
                0f, 0f, 0f, 1f, 0f
            )
            "Cool Fade" -> floatArrayOf(
                0.9f, 0f, 0f, 0f, 10f,
                0f, 1.0f, 0f, 0f, 15f,
                0f, 0.1f, 1.2f, 0f, 25f,
                0f, 0f, 0f, 1f, 0f
            )
            else -> floatArrayOf(
                1f, 0f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        }
        return applyAllAdjustments(
            base, brightness, contrast, saturation, warmth,
            tint, highlights, shadows, whites, blacks, vibrance, filterStrength
        )
    }

    private fun buildColorFilter(
        filterName: String,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float,
        tint: Float,
        highlights: Float,
        shadows: Float,
        whites: Float,
        blacks: Float,
        vibrance: Float,
        filterStrength: Float
    ): ColorFilter? {
        val adjusted = buildAdjustmentMatrix(
            filterName, brightness, contrast, saturation, warmth,
            tint, highlights, shadows, whites, blacks, vibrance, filterStrength
        )
        return ColorFilter.colorMatrix(ColorMatrix(adjusted))
    }

    private fun applyAllAdjustments(
        base: FloatArray,
        brightness: Float, contrast: Float, saturation: Float, warmth: Float,
        tint: Float, highlights: Float, shadows: Float, whites: Float, blacks: Float,
        vibrance: Float, filterStrength: Float
    ): FloatArray {
        var b = base.clone()
        val strength = filterStrength.coerceIn(0f, 1f)

        if (saturation != 0f) {
            val sat = 1f + (saturation / 100f) * strength
            val lumR = 0.2126f
            val lumG = 0.7152f
            val lumB = 0.0722f
            val sr = (1f - sat) * lumR
            val sg = (1f - sat) * lumG
            val sb = (1f - sat) * lumB
            val satMatrix = floatArrayOf(
                sr + sat, sg, sb, 0f, 0f,
                sr, sg + sat, sb, 0f, 0f,
                sr, sg, sb + sat, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(satMatrix, b)
        }

        if (vibrance != 0f) {
            val v = (vibrance / 100f) * strength
            val sat = 1f + v
            val lumR = 0.2126f
            val lumG = 0.7152f
            val lumB = 0.0722f
            val sr = (1f - sat) * lumR
            val sg = (1f - sat) * lumG
            val sb = (1f - sat) * lumB
            val vibMatrix = floatArrayOf(
                sr + sat * 0.85f, sg * 0.85f, sb * 0.85f, 0f, 0f,
                sr * 0.85f, sg + sat * 0.85f, sb * 0.85f, 0f, 0f,
                sr * 0.85f, sg * 0.85f, sb + sat * 0.85f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(vibMatrix, b)
        }

        if (contrast != 0f) {
            val c = 1f + (contrast / 100f) * strength
            val t = 128f * (1f - c)
            val contrastMatrix = floatArrayOf(
                c, 0f, 0f, 0f, t,
                0f, c, 0f, 0f, t,
                0f, 0f, c, 0f, t,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(contrastMatrix, b)
        }

        if (brightness != 0f) {
            val shift = (brightness / 100f) * 128f * strength
            val brightMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, shift,
                0f, 1f, 0f, 0f, shift,
                0f, 0f, 1f, 0f, shift,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(brightMatrix, b)
        }

        if (warmth != 0f) {
            val w = (warmth / 100f) * strength
            val warmMatrix = floatArrayOf(
                1f + w * 0.3f, 0f, 0f, 0f, w * 12f,
                0f, 1f + w * 0.1f, 0f, 0f, w * 6f,
                0f, 0f, 1f - w * 0.2f, 0f, -w * 10f,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(warmMatrix, b)
        }

        if (tint != 0f) {
            val t = (tint / 100f) * strength
            val tR = t * 0.3f
            val tG = t * 0.1f
            val tB = t * 0.6f
            val tintMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, tR,
                0f, 1f, 0f, 0f, tG,
                0f, 0f, 1f, 0f, tB,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(tintMatrix, b)
        }

        if (highlights != 0f) {
            val h = (highlights / 100f) * strength
            val hShift = h * 0.5f
            val hMatrix = floatArrayOf(
                1f + h * 0.2f, 0f, 0f, 0f, hShift,
                0f, 1f + h * 0.2f, 0f, 0f, hShift,
                0f, 0f, 1f + h * 0.2f, 0f, hShift,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(hMatrix, b)
        }

        if (shadows != 0f) {
            val s = (shadows / 100f) * strength
            val sShift = s * 0.3f
            val sMatrix = floatArrayOf(
                1f + s * 0.1f, 0f, 0f, 0f, sShift,
                0f, 1f + s * 0.1f, 0f, 0f, sShift,
                0f, 0f, 1f + s * 0.1f, 0f, sShift,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(sMatrix, b)
        }

        if (whites != 0f || blacks != 0f) {
            val w = (whites / 100f) * strength
            val bl = (blacks / 100f) * strength
            val clipMatrix = floatArrayOf(
                1f + w * 0.15f + bl * 0.1f, 0f, 0f, 0f, w * 0.1f + bl * 0.05f,
                0f, 1f + w * 0.15f + bl * 0.1f, 0f, 0f, w * 0.1f + bl * 0.05f,
                0f, 0f, 1f + w * 0.15f + bl * 0.1f, 0f, w * 0.1f + bl * 0.05f,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(clipMatrix, b)
        }

        return b
    }

    /** Apply tone curve for highlights/shadows as a separate overlay effect.
     * Returns a function that maps normalized luminance [0..1] to adjusted [0..1]. */
    fun toneCurve(highlights: Float, shadows: Float, whites: Float, blacks: Float): (Float) -> Float {
        return { lum ->
            var v = lum
            // Blacks adjustment (dark tones)
            if (blacks != 0f) {
                val b = blacks / 100f
                v = v * (1f + b) - b * 0.5f
            }
            // Shadows adjustment
            if (shadows != 0f) {
                val s = shadows / 100f
                v += s * 0.15f * (1f - v)
            }
            // Highlights adjustment
            if (highlights != 0f) {
                val h = highlights / 100f
                v += h * 0.15f * v
            }
            // Whites (clipping)
            if (whites != 0f) {
                val w = whites / 100f
                v = v + w * 0.2f * (v - 0.5f).coerceAtLeast(0f)
            }
            v.coerceIn(0f, 1f)
        }
    }

    /** Hue rotation for tint effect. */
    fun hueRotationMatrix(degrees: Float): FloatArray {
        val rad = Math.toRadians(degrees.toDouble())
        val cos = Math.cos(rad).toFloat()
        val sin = Math.sin(rad).toFloat()
        return floatArrayOf(
            0.213f + cos * 0.787f - sin * 0.213f, 0.715f - cos * 0.715f - sin * 0.715f, 0.072f - cos * 0.072f + sin * 0.928f, 0f, 0f,
            0.213f - cos * 0.213f + sin * 0.143f, 0.715f + cos * 0.285f + sin * 0.140f, 0.072f - cos * 0.072f - sin * 0.283f, 0f, 0f,
            0.213f - cos * 0.213f - sin * 0.787f, 0.715f - cos * 0.715f + sin * 0.715f, 0.072f + cos * 0.928f + sin * 0.072f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    /** Build a saturation adjustment matrix with vibrance (protects skin tones). */
    fun vibranceMatrix(vibrance: Float): FloatArray {
        val v = vibrance / 100f
        val sat = 1f + v * 0.5f
        val lumR = 0.2126f
        val lumG = 0.7152f
        val lumB = 0.0722f
        val sr = (1f - sat) * lumR
        val sg = (1f - sat) * lumG
        val sb = (1f - sat) * lumB
        return floatArrayOf(
            sr + sat * 0.85f, sg * 0.85f, sb * 0.85f, 0f, 0f,
            sr * 0.85f, sg + sat * 0.85f, sb * 0.85f, 0f, 0f,
            sr * 0.85f, sg * 0.85f, sb + sat * 0.85f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f
        )
    }

    private fun colorMatrixToArray(cm: ColorMatrix): FloatArray = cm.values.copyOf()

    private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        val result = FloatArray(20)
        for (row in 0 until 4) {
            for (col in 0 until 5) {
                var sum = 0f
                for (k in 0 until 4) {
                    sum += a[row * 5 + k] * b[k * 5 + col]
                }
                if (col == 4) sum += a[row * 5 + 4]
                result[row * 5 + col] = sum
            }
        }
        return result
    }

    /** Serialize all adjustment values to a JSON-like string. */
    fun adjustmentsToJson(
        brightness: Float, contrast: Float, saturation: Float, warmth: Float,
        highlights: Float, shadows: Float, whites: Float, blacks: Float,
        tint: Float, vibrance: Float, sharpness: Float, clarity: Float,
        denoise: Float, vignette: Float
    ): String? {
        val all = listOf(
            brightness, contrast, saturation, warmth,
            highlights, shadows, whites, blacks,
            tint, vibrance, sharpness, clarity, denoise, vignette
        )
        if (all.all { it == 0f }) return null
        return all.joinToString(",") { it.toInt().toString() }
    }

    /** Parse adjustments from saved MediaItem editJson. Supports both old (4-value) and new (14-value) formats. */
    fun adjustmentsFromJson(json: String?): FloatArray {
        val zero = FloatArray(14)
        if (json.isNullOrBlank()) return zero
        val parts = json.split(",")
        if (parts.isEmpty()) return zero
        val result = FloatArray(14) { 0f }
        val count = minOf(parts.size, 14)
        for (i in 0 until count) {
            try {
                result[i] = parts[i].trim().toFloat()
            } catch (_: NumberFormatException) {
                result[i] = 0f
            }
        }
        return result
    }

    /** Extract HSL channel adjustments from a saved string. */
    fun hslFromJson(json: String?): FloatArray {
        if (json.isNullOrBlank()) return FloatArray(24) { 0f }
        val parts = json.split(";")
        if (parts.size < 2) return FloatArray(24) { 0f }
        val hsl = FloatArray(24)
        val values = parts[0].split(",").map { it.trim().toFloatOrNull() ?: 0f }
        val counts = parts[1].split(",").map { it.trim().toIntOrNull() ?: 0 }
        for (i in values.indices) {
            if (i < 24) hsl[i] = values[i]
        }
        return hsl
    }
}
