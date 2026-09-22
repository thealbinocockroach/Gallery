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

    private val presetCache = HashMap<String, ColorFilter>()

    fun getColorFilter(filterName: String): ColorFilter? {
        val cached = presetCache[filterName]
        if (cached != null) return cached
        val created = getColorFilter(filterName, 0f, 0f, 0f, 0f)
        if (created != null) presetCache[filterName] = created
        return created
    }

    /**
     * Combines the preset filter matrix with manual ADJUST easing
     * (brightness / contrast / saturation / warmth, each -100..100).
     */
    fun getColorFilter(
        filterName: String,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float
    ): ColorFilter? {
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

        val adjusted = applyAdjustments(base, brightness, contrast, saturation, warmth)
        return ColorFilter.colorMatrix(ColorMatrix(adjusted))
    }

    private fun colorMatrixToArray(cm: ColorMatrix): FloatArray = cm.values.copyOf()

    private fun applyAdjustments(
        base: FloatArray,
        brightness: Float,
        contrast: Float,
        saturation: Float,
        warmth: Float
    ): FloatArray {
        var b = base.clone()

        if (saturation != 0f) {
            val sat = (100f + saturation) / 100f
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

        if (contrast != 0f) {
            val c = ((255f + contrast) / 255f)
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
            val shift = brightness * 1.28f
            val brightMatrix = floatArrayOf(
                1f, 0f, 0f, 0f, shift,
                0f, 1f, 0f, 0f, shift,
                0f, 0f, 1f, 0f, shift,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(brightMatrix, b)
        }

        if (warmth != 0f) {
            val w = warmth / 100f
            val warmMatrix = floatArrayOf(
                1f + w * 0.3f, 0f, 0f, 0f, w * 12f,
                0f, 1f + w * 0.1f, 0f, 0f, w * 6f,
                0f, 0f, 1f - w * 0.2f, 0f, -w * 10f,
                0f, 0f, 0f, 1f, 0f
            )
            b = multiplyMatrices(warmMatrix, b)
        }

        return b
    }

    private fun multiplyMatrices(a: FloatArray, b: FloatArray): FloatArray {
        // a and b are 4x5 row-major augmented matrices.
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

    /** Serialize adjust values to a compact "b,c,s,w" string (or null when all zero). */
    fun adjustmentsToJson(brightness: Float, contrast: Float, saturation: Float, warmth: Float): String? {
        if (brightness == 0f && contrast == 0f && saturation == 0f && warmth == 0f) return null
        return "${brightness.toInt()},${contrast.toInt()},${saturation.toInt()},${warmth.toInt()}"
    }

    /** Parse adjustments from a saved MediaItem editJson "b,c,s,w". */
    fun adjustmentsFromJson(json: String?): FloatArray {
        val zero = floatArrayOf(0f, 0f, 0f, 0f)
        if (json.isNullOrBlank()) return zero
        val parts = json.split(",")
        if (parts.size < 4) return zero
        return try {
            floatArrayOf(
                parts[0].trim().toFloat(),
                parts[1].trim().toFloat(),
                parts[2].trim().toFloat(),
                parts[3].trim().toFloat()
            )
        } catch (_: NumberFormatException) {
            zero
        }
    }
}