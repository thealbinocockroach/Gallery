package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.min

/**
 * Normalized crop rectangle coordinates relative to the underlying image:
 * left, top, right, bottom are all in [0.0f, 1.0f].
 */
data class NormalizedCropRect(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 1f,
    val bottom: Float = 1f
) {
    val width: Float get() = (right - left).coerceAtLeast(0.001f)
    val height: Float get() = (bottom - top).coerceAtLeast(0.001f)
    val aspect: Float get() = width / height

    fun clamped(): NormalizedCropRect {
        val l = left.coerceIn(0f, 0.95f)
        val t = top.coerceIn(0f, 0.95f)
        val r = right.coerceIn(l + 0.05f, 1f)
        val b = bottom.coerceIn(t + 0.05f, 1f)
        return NormalizedCropRect(l, t, r, b)
    }

    companion object {
        val FULL = NormalizedCropRect(0f, 0f, 1f, 1f)
    }
}

private enum class CropDragHandle {
    NONE,
    INSIDE,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT,
    TOP,
    BOTTOM,
    LEFT,
    RIGHT
}

/**
 * Interactive Image Cropper overlay:
 * - Dark translucent mask outside crop area.
 * - Bold boundary box with rule-of-thirds grid.
 * - Draggable corner handles and edge handles.
 * - Strict aspect ratio locking when a preset ratio is selected.
 * - Pan inside box without resizing.
 * - Strict boundary containment inside rendered image.
 */
@Composable
fun ImageCropperView(
    imageWidth: Int,
    imageHeight: Int,
    cropRect: NormalizedCropRect,
    selectedAspectRatio: String, // "Freeform", "1:1", "9:16", "16:9", "4:3", "3:4"
    onCropChange: (NormalizedCropRect) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val handleTouchRadiusPx = with(density) { 40.dp.toPx() }
    val cornerBracketLengthPx = with(density) { 22.dp.toPx() }
    val cornerBracketStrokePx = with(density) { 4.dp.toPx() }
    val edgeHandleLengthPx = with(density) { 28.dp.toPx() }
    val edgeHandleStrokePx = with(density) { 4.dp.toPx() }

    // Target aspect ratio (width / height) in normalized image space:
    // Because normalized coords represent fractions of imageWidth and imageHeight,
    // a real pixel aspect ratio of R corresponds to a normalized aspect ratio of:
    // (normW * imgW) / (normH * imgH) = R  =>  normW / normH = R * (imgH / imgW)
    val targetNormAspect: Float? = remember(selectedAspectRatio, imageWidth, imageHeight) {
        val realAspect = when (selectedAspectRatio) {
            "1:1" -> 1.0f
            "9:16" -> 9f / 16f
            "16:9" -> 16f / 9f
            "4:3" -> 4f / 3f
            "3:4" -> 3f / 4f
            else -> null // "Freeform"
        }
        if (realAspect != null && imageWidth > 0 && imageHeight > 0) {
            realAspect * (imageHeight.toFloat() / imageWidth.toFloat())
        } else {
            null
        }
    }

    var activeHandle by remember { mutableStateOf(CropDragHandle.NONE) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(cropRect, targetNormAspect, imageWidth, imageHeight) {
                    val safeImgW = if (imageWidth > 0) imageWidth.toFloat() else 1000f
                    val safeImgH = if (imageHeight > 0) imageHeight.toFloat() else 1000f
                    val imgAspect = safeImgW / safeImgH

                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val containerW = size.width.toFloat()
                            val containerH = size.height.toFloat()
                            val imgBounds = calculateFittedImageBounds(containerW, containerH, imgAspect)

                            val boxRect = Rect(
                                left = imgBounds.left + cropRect.left * imgBounds.width,
                                top = imgBounds.top + cropRect.top * imgBounds.height,
                                right = imgBounds.left + cropRect.right * imgBounds.width,
                                bottom = imgBounds.top + cropRect.bottom * imgBounds.height
                            )

                            activeHandle = hitTestHandle(startOffset, boxRect, handleTouchRadiusPx)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (activeHandle == CropDragHandle.NONE) return@detectDragGestures

                            val containerW = size.width.toFloat()
                            val containerH = size.height.toFloat()
                            val imgBounds = calculateFittedImageBounds(containerW, containerH, imgAspect)
                            if (imgBounds.width <= 0 || imgBounds.height <= 0) return@detectDragGestures

                            val dNormX = dragAmount.x / imgBounds.width
                            val dNormY = dragAmount.y / imgBounds.height

                            val updated = computeUpdatedCropRect(
                                current = cropRect,
                                handle = activeHandle,
                                deltaX = dNormX,
                                deltaY = dNormY,
                                targetAspect = targetNormAspect
                            )
                            onCropChange(updated)
                        },
                        onDragEnd = {
                            activeHandle = CropDragHandle.NONE
                        },
                        onDragCancel = {
                            activeHandle = CropDragHandle.NONE
                        }
                    )
                }
        ) {
            val safeImgW = if (imageWidth > 0) imageWidth.toFloat() else 1000f
            val safeImgH = if (imageHeight > 0) imageHeight.toFloat() else 1000f
            val imgAspect = safeImgW / safeImgH

            val imgBounds = calculateFittedImageBounds(size.width, size.height, imgAspect)

            val boxLeft = (imgBounds.left + cropRect.left * imgBounds.width).coerceIn(imgBounds.left, imgBounds.right)
            val boxTop = (imgBounds.top + cropRect.top * imgBounds.height).coerceIn(imgBounds.top, imgBounds.bottom)
            val boxRight = (imgBounds.left + cropRect.right * imgBounds.width).coerceIn(boxLeft + 10f, imgBounds.right)
            val boxBottom = (imgBounds.top + cropRect.bottom * imgBounds.height).coerceIn(boxTop + 10f, imgBounds.bottom)

            val boxWidth = boxRight - boxLeft
            val boxHeight = boxBottom - boxTop

            // 1. Draw Translucent Scrim Outside the Crop Box
            val scrimPath = Path().apply {
                fillType = PathFillType.EvenOdd
                // Outer image bounds
                addRect(imgBounds)
                // Inner crop box cutout
                addRect(Rect(boxLeft, boxTop, boxRight, boxBottom))
            }
            drawPath(path = scrimPath, color = Color(0xB3000000))

            // 2. Draw Crop Boundary Box
            drawRect(
                color = Color.White,
                topLeft = Offset(boxLeft, boxTop),
                size = Size(boxWidth, boxHeight),
                style = Stroke(width = 2.dp.toPx())
            )
            // Outer subtle contrast border
            drawRect(
                color = Color(0x66000000),
                topLeft = Offset(boxLeft - 1.dp.toPx(), boxTop - 1.dp.toPx()),
                size = Size(boxWidth + 2.dp.toPx(), boxHeight + 2.dp.toPx()),
                style = Stroke(width = 1.dp.toPx())
            )

            // 3. Rule of Thirds Grid
            val oneThirdX = boxLeft + boxWidth / 3f
            val twoThirdX = boxLeft + (2f * boxWidth) / 3f
            val oneThirdY = boxTop + boxHeight / 3f
            val twoThirdY = boxTop + (2f * boxHeight) / 3f

            val gridColor = Color.White.copy(alpha = 0.45f)
            val gridStroke = Stroke(width = 1.dp.toPx())

            drawLine(gridColor, Offset(oneThirdX, boxTop), Offset(oneThirdX, boxBottom), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(twoThirdX, boxTop), Offset(twoThirdX, boxBottom), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(boxLeft, oneThirdY), Offset(boxRight, oneThirdY), strokeWidth = gridStroke.width)
            drawLine(gridColor, Offset(boxLeft, twoThirdY), Offset(boxRight, twoThirdY), strokeWidth = gridStroke.width)

            // 4. Draggable Corner Bracket Handles
            val cornerColor = Color(0xFFFFE500) // NeoYellow for high visibility & brand aesthetic
            val cornerShadowColor = Color(0xFF141416) // NeoDark
            val bracketLen = min(cornerBracketLengthPx, min(boxWidth / 3f, boxHeight / 3f))

            // The grabbed handle renders larger so the finger never loses its target.
            fun drawCorner(x: Float, y: Float, dirX: Float, dirY: Float, grabbed: Boolean) {
                val len = bracketLen * (if (grabbed) 1.45f else 1f)
                val stroke = cornerBracketStrokePx + (if (grabbed) 2f else 0f)
                // Background shadow stroke
                drawLine(
                    color = cornerShadowColor,
                    start = Offset(x, y),
                    end = Offset(x + dirX * len, y),
                    strokeWidth = stroke + 2f,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = cornerShadowColor,
                    start = Offset(x, y),
                    end = Offset(x, y + dirY * len),
                    strokeWidth = stroke + 2f,
                    cap = StrokeCap.Square
                )
                // Foreground colored stroke
                drawLine(
                    color = cornerColor,
                    start = Offset(x, y),
                    end = Offset(x + dirX * len, y),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
                drawLine(
                    color = cornerColor,
                    start = Offset(x, y),
                    end = Offset(x, y + dirY * len),
                    strokeWidth = stroke,
                    cap = StrokeCap.Square
                )
            }

            // TL, TR, BL, BR
            drawCorner(boxLeft, boxTop, 1f, 1f, activeHandle == CropDragHandle.TOP_LEFT)
            drawCorner(boxRight, boxTop, -1f, 1f, activeHandle == CropDragHandle.TOP_RIGHT)
            drawCorner(boxLeft, boxBottom, 1f, -1f, activeHandle == CropDragHandle.BOTTOM_LEFT)
            drawCorner(boxRight, boxBottom, -1f, -1f, activeHandle == CropDragHandle.BOTTOM_RIGHT)

            // 5. Draggable Edge Center Handles (grabbed bar grows so it stays under the finger)
            val edgeColor = Color.White
            val edgeBarLen = min(edgeHandleLengthPx, min(boxWidth / 3f, boxHeight / 3f))
            fun edgeLen(handle: CropDragHandle) =
                edgeBarLen * (if (activeHandle == handle) 1.4f else 1f)
            fun edgeStroke(handle: CropDragHandle) =
                edgeHandleStrokePx + (if (activeHandle == handle) 2f else 0f)

            // Top edge center
            drawLine(
                color = cornerShadowColor,
                start = Offset(boxLeft + boxWidth / 2f - edgeLen(CropDragHandle.TOP) / 2f, boxTop),
                end = Offset(boxLeft + boxWidth / 2f + edgeLen(CropDragHandle.TOP) / 2f, boxTop),
                strokeWidth = edgeStroke(CropDragHandle.TOP) + 2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = edgeColor,
                start = Offset(boxLeft + boxWidth / 2f - edgeLen(CropDragHandle.TOP) / 2f, boxTop),
                end = Offset(boxLeft + boxWidth / 2f + edgeLen(CropDragHandle.TOP) / 2f, boxTop),
                strokeWidth = edgeStroke(CropDragHandle.TOP),
                cap = StrokeCap.Round
            )

            // Bottom edge center
            drawLine(
                color = cornerShadowColor,
                start = Offset(boxLeft + boxWidth / 2f - edgeLen(CropDragHandle.BOTTOM) / 2f, boxBottom),
                end = Offset(boxLeft + boxWidth / 2f + edgeLen(CropDragHandle.BOTTOM) / 2f, boxBottom),
                strokeWidth = edgeStroke(CropDragHandle.BOTTOM) + 2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = edgeColor,
                start = Offset(boxLeft + boxWidth / 2f - edgeLen(CropDragHandle.BOTTOM) / 2f, boxBottom),
                end = Offset(boxLeft + boxWidth / 2f + edgeLen(CropDragHandle.BOTTOM) / 2f, boxBottom),
                strokeWidth = edgeStroke(CropDragHandle.BOTTOM),
                cap = StrokeCap.Round
            )

            // Left edge center
            drawLine(
                color = cornerShadowColor,
                start = Offset(boxLeft, boxTop + boxHeight / 2f - edgeLen(CropDragHandle.LEFT) / 2f),
                end = Offset(boxLeft, boxTop + boxHeight / 2f + edgeLen(CropDragHandle.LEFT) / 2f),
                strokeWidth = edgeStroke(CropDragHandle.LEFT) + 2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = edgeColor,
                start = Offset(boxLeft, boxTop + boxHeight / 2f - edgeLen(CropDragHandle.LEFT) / 2f),
                end = Offset(boxLeft, boxTop + boxHeight / 2f + edgeLen(CropDragHandle.LEFT) / 2f),
                strokeWidth = edgeStroke(CropDragHandle.LEFT),
                cap = StrokeCap.Round
            )

            // Right edge center
            drawLine(
                color = cornerShadowColor,
                start = Offset(boxRight, boxTop + boxHeight / 2f - edgeLen(CropDragHandle.RIGHT) / 2f),
                end = Offset(boxRight, boxTop + boxHeight / 2f + edgeLen(CropDragHandle.RIGHT) / 2f),
                strokeWidth = edgeStroke(CropDragHandle.RIGHT) + 2f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = edgeColor,
                start = Offset(boxRight, boxTop + boxHeight / 2f - edgeLen(CropDragHandle.RIGHT) / 2f),
                end = Offset(boxRight, boxTop + boxHeight / 2f + edgeLen(CropDragHandle.RIGHT) / 2f),
                strokeWidth = edgeStroke(CropDragHandle.RIGHT),
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Calculates the bounding rectangle of the image rendered with ContentScale.Fit
 * inside the given container dimensions.
 */
fun calculateFittedImageBounds(containerW: Float, containerH: Float, imageAspect: Float): Rect {
    if (containerW <= 0f || containerH <= 0f || imageAspect <= 0f) {
        return Rect(0f, 0f, containerW, containerH)
    }
    val containerAspect = containerW / containerH
    return if (containerAspect > imageAspect) {
        // Pillarboxed (letterboxed on left and right)
        val fittedW = containerH * imageAspect
        val left = (containerW - fittedW) / 2f
        Rect(left = left, top = 0f, right = left + fittedW, bottom = containerH)
    } else {
        // Letterboxed on top and bottom
        val fittedH = containerW / imageAspect
        val top = (containerH - fittedH) / 2f
        Rect(left = 0f, top = top, right = containerW, bottom = top + fittedH)
    }
}

/**
 * Single normalization contract shared by preview and export: maps a normalized
 * crop box in [0,1] space to integer source pixels in [0,imageWidth] x [0,imageHeight].
 * Output is always at least 1x1 px — the box can never collapse to zero dimensions.
 */
fun NormalizedCropRect.toPixelRect(imageWidth: Int, imageHeight: Int): Rect {
    val w = imageWidth.coerceAtLeast(1)
    val h = imageHeight.coerceAtLeast(1)
    val left = (left * w).toInt().coerceIn(0, w - 1)
    val top = (top * h).toInt().coerceIn(0, h - 1)
    val right = (right * w).toInt().coerceIn(left + 1, w)
    val bottom = (bottom * h).toInt().coerceIn(top + 1, h)
    return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}

/**
 * Determines which handle is touched based on proximity.
 */
private fun hitTestHandle(point: Offset, box: Rect, touchRadius: Float): CropDragHandle {
    fun near(x: Float, y: Float): Boolean {
        val dx = point.x - x
        val dy = point.y - y
        return (dx * dx + dy * dy) <= (touchRadius * touchRadius)
    }

    // 1. Check Corners first
    if (near(box.left, box.top)) return CropDragHandle.TOP_LEFT
    if (near(box.right, box.top)) return CropDragHandle.TOP_RIGHT
    if (near(box.left, box.bottom)) return CropDragHandle.BOTTOM_LEFT
    if (near(box.right, box.bottom)) return CropDragHandle.BOTTOM_RIGHT

    // 2. Check Edges
    val midX = box.left + box.width / 2f
    val midY = box.top + box.height / 2f
    if (near(midX, box.top)) return CropDragHandle.TOP
    if (near(midX, box.bottom)) return CropDragHandle.BOTTOM
    if (near(box.left, midY)) return CropDragHandle.LEFT
    if (near(box.right, midY)) return CropDragHandle.RIGHT

    // 3. Check Inside (for panning)
    if (point.x in box.left..box.right && point.y in box.top..box.bottom) {
        return CropDragHandle.INSIDE
    }

    return CropDragHandle.NONE
}

/**
 * Computes updated normalized crop rectangle while enforcing:
 * 1. Aspect ratio locking (if targetAspect != null)
 * 2. Minimum dimension bounds (min 0.05)
 * 3. Boundary constraints strictly inside [0.0, 1.0]
 */
private fun computeUpdatedCropRect(
    current: NormalizedCropRect,
    handle: CropDragHandle,
    deltaX: Float,
    deltaY: Float,
    targetAspect: Float?
): NormalizedCropRect {
    val minSize = 0.03f

    when (handle) {
        CropDragHandle.INSIDE -> {
            // Panning: translate crop box, keeping size constant
            val boxW = current.width
            val boxH = current.height
            val newL = (current.left + deltaX).coerceIn(0f, 1f - boxW)
            val newT = (current.top + deltaY).coerceIn(0f, 1f - boxH)
            return NormalizedCropRect(newL, newT, newL + boxW, newT + boxH)
        }

        CropDragHandle.BOTTOM_RIGHT -> {
            var newR = (current.right + deltaX).coerceIn(current.left + minSize, 1f)
            var newB = (current.bottom + deltaY).coerceIn(current.top + minSize, 1f)

            if (targetAspect != null && targetAspect > 0f) {
                val candidateW = newR - current.left
                val adjustedH = candidateW / targetAspect
                if (current.top + adjustedH <= 1f && adjustedH >= minSize) {
                    newB = current.top + adjustedH
                } else {
                    val maxH = 1f - current.top
                    // No room left to honor both bounds and minSize: hold position.
                    if (maxH < minSize) return current
                    newB = current.top + maxH
                    newR = (current.left + maxH * targetAspect).coerceIn(current.left + minSize, 1f)
                }
            }
            return NormalizedCropRect(current.left, current.top, newR, newB)
        }

        CropDragHandle.BOTTOM_LEFT -> {
            var newL = (current.left + deltaX).coerceIn(0f, current.right - minSize)
            var newB = (current.bottom + deltaY).coerceIn(current.top + minSize, 1f)

            if (targetAspect != null && targetAspect > 0f) {
                val candidateW = current.right - newL
                val adjustedH = candidateW / targetAspect
                if (current.top + adjustedH <= 1f && adjustedH >= minSize) {
                    newB = current.top + adjustedH
                } else {
                    val maxH = 1f - current.top
                    // No room left to honor both bounds and minSize: hold position.
                    if (maxH < minSize) return current
                    newB = current.top + maxH
                    newL = (current.right - maxH * targetAspect).coerceIn(0f, current.right - minSize)
                }
            }
            return NormalizedCropRect(newL, current.top, current.right, newB)
        }

        CropDragHandle.TOP_RIGHT -> {
            var newR = (current.right + deltaX).coerceIn(current.left + minSize, 1f)
            var newT = (current.top + deltaY).coerceIn(0f, current.bottom - minSize)

            if (targetAspect != null && targetAspect > 0f) {
                val candidateW = newR - current.left
                val adjustedH = candidateW / targetAspect
                if (current.bottom - adjustedH >= 0f && adjustedH >= minSize) {
                    newT = current.bottom - adjustedH
                } else {
                    val maxH = current.bottom
                    // No room left to honor both bounds and minSize: hold position.
                    if (maxH < minSize) return current
                    newT = 0f
                    newR = (current.left + maxH * targetAspect).coerceIn(current.left + minSize, 1f)
                }
            }
            return NormalizedCropRect(current.left, newT, newR, current.bottom)
        }

        CropDragHandle.TOP_LEFT -> {
            var newL = (current.left + deltaX).coerceIn(0f, current.right - minSize)
            var newT = (current.top + deltaY).coerceIn(0f, current.bottom - minSize)

            if (targetAspect != null && targetAspect > 0f) {
                val candidateW = current.right - newL
                val adjustedH = candidateW / targetAspect
                if (current.bottom - adjustedH >= 0f && adjustedH >= minSize) {
                    newT = current.bottom - adjustedH
                } else {
                    val maxH = current.bottom
                    // No room left to honor both bounds and minSize: hold position.
                    if (maxH < minSize) return current
                    newT = 0f
                    newL = (current.right - maxH * targetAspect).coerceIn(0f, current.right - minSize)
                }
            }
            return NormalizedCropRect(newL, newT, current.right, current.bottom)
        }

        CropDragHandle.TOP -> {
            val newT = (current.top + deltaY).coerceIn(0f, current.bottom - minSize)
            if (targetAspect != null && targetAspect > 0f) {
                val newH = current.bottom - newT
                val newW = (newH * targetAspect).coerceIn(minSize, 1f)
                val midX = (current.left + current.right) / 2f
                // Span-clamp: anchor start so [start, start + newW] stays ordered and in bounds.
                val startL = (midX - newW / 2f).coerceIn(0f, 1f - newW)
                return NormalizedCropRect(startL, newT, startL + newW, current.bottom)
            }
            return current.copy(top = newT)
        }

        CropDragHandle.BOTTOM -> {
            val newB = (current.bottom + deltaY).coerceIn(current.top + minSize, 1f)
            if (targetAspect != null && targetAspect > 0f) {
                val newH = newB - current.top
                val newW = (newH * targetAspect).coerceIn(minSize, 1f)
                val midX = (current.left + current.right) / 2f
                // Span-clamp: anchor start so [start, start + newW] stays ordered and in bounds.
                val startL = (midX - newW / 2f).coerceIn(0f, 1f - newW)
                return NormalizedCropRect(startL, current.top, startL + newW, newB)
            }
            return current.copy(bottom = newB)
        }

        CropDragHandle.LEFT -> {
            val newL = (current.left + deltaX).coerceIn(0f, current.right - minSize)
            if (targetAspect != null && targetAspect > 0f) {
                val newW = current.right - newL
                val newH = (newW / targetAspect).coerceIn(minSize, 1f)
                val midY = (current.top + current.bottom) / 2f
                // Span-clamp: anchor start so [start, start + newH] stays ordered and in bounds.
                val startT = (midY - newH / 2f).coerceIn(0f, 1f - newH)
                return NormalizedCropRect(newL, startT, current.right, startT + newH)
            }
            return current.copy(left = newL)
        }

        CropDragHandle.RIGHT -> {
            val newR = (current.right + deltaX).coerceIn(current.left + minSize, 1f)
            if (targetAspect != null && targetAspect > 0f) {
                val newW = newR - current.left
                val newH = (newW / targetAspect).coerceIn(minSize, 1f)
                val midY = (current.top + current.bottom) / 2f
                // Span-clamp: anchor start so [start, start + newH] stays ordered and in bounds.
                val startT = (midY - newH / 2f).coerceIn(0f, 1f - newH)
                return NormalizedCropRect(current.left, startT, newR, startT + newH)
            }
            return current.copy(right = newR)
        }

        CropDragHandle.NONE -> return current
    }
    return current
}
