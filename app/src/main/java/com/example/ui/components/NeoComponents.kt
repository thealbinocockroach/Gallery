package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow

@Composable
fun NeoCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoWhite,
    borderColor: Color = NeoBorder,
    borderWidth: Dp = 2.5.dp,
    shadowOffset: Dp = 4.dp,
    shadowColor: Color = NeoDark,
    shape: Shape = RectangleShape,
    onClick: (() -> Unit)? = null,
    testTag: String = "neo_card",
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.testTag(testTag)) {
        // Drop shadow behind
        if (shadowOffset > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(shadowColor, shape)
                    .border(borderWidth, borderColor, shape)
            )
        }
        // Main surface
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .then(
                    if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
                ),
            color = backgroundColor,
            shape = shape,
            border = BorderStroke(borderWidth, borderColor)
        ) {
            content()
        }
    }
}

@Composable
fun NeoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = NeoYellow,
    contentColor: Color = NeoDark,
    borderColor: Color = NeoBorder,
    leadingIcon: ImageVector? = null,
    testTag: String = "neo_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shadowOffset by animateDpAsState(
        targetValue = if (isPressed) 1.dp else 4.dp,
        label = "btn_shadow"
    )
    val pressedOffset by animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        label = "btn_press"
    )

    // The pill (foreground + shadow) sizes to its content and centers in the tap
    // area, so stretched buttons (weight/fillMaxWidth) keep a tight theme shadow
    // instead of a full-bleed black slab.
    Box(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Shadow sized to the pill (matchParentSize of this wrap-content box).
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = shadowOffset, y = shadowOffset)
                    .background(NeoDark, RectangleShape)
                    .border(2.5.dp, borderColor, RectangleShape)
            )
            // Foreground button
            Box(
                modifier = Modifier
                    .offset(x = pressedOffset, y = pressedOffset)
                    .background(containerColor, RectangleShape)
                    .border(2.5.dp, borderColor, RectangleShape)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (leadingIcon != null) {
                        Icon(
                            imageVector = leadingIcon,
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 6.dp)
                        )
                    }
                    Text(
                        text = text.uppercase(),
                        color = contentColor,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun NeoIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoWhite,
    tint: Color = NeoDark,
    borderColor: Color = NeoBorder,
    size: Dp = 44.dp,
    shadowOffset: Dp = 3.dp,
    testTag: String = "neo_icon_button"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val actualShadow by animateDpAsState(
        targetValue = if (isPressed) 0.dp else shadowOffset,
        label = "icon_btn_shadow"
    )
    val pressOffset by animateDpAsState(
        targetValue = if (isPressed) shadowOffset else 0.dp,
        label = "icon_btn_press"
    )

    Box(
        modifier = modifier
            .size(size)
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // Shadow
        if (actualShadow > 0.dp) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .offset(x = actualShadow, y = actualShadow)
                    .background(NeoDark, RectangleShape)
                    .border(2.dp, borderColor, RectangleShape)
            )
        }
        // Button surface
        Box(
            modifier = Modifier
                .offset(x = pressOffset, y = pressOffset)
                .matchParentSize()
                .background(backgroundColor, RectangleShape)
                .border(2.dp, borderColor, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Deterministic brutalist slider: thin black active track, gray inactive track,
 * square yellow thumb with dark border. Fully self-drawn (Canvas) so it looks
 * identical on every device/theme — no Material3 theme dependence.
 */
@Composable
fun NeoSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
    testTag: String = ""
) {
    var widthPx by remember { mutableStateOf(0f) }
    fun fractionToValue(f: Float): Float {
        val span = valueRange.endInclusive - valueRange.start
        if (span <= 0f) return valueRange.start
        return valueRange.start + f.coerceIn(0f, 1f) * span
    }
    fun setFromX(x: Float) {
        if (widthPx > 0f) onValueChange(fractionToValue(x / widthPx))
    }
    val frac = if (valueRange.endInclusive > valueRange.start) {
        ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
    } else {
        0f
    }
    Canvas(
        modifier = modifier
            .height(30.dp)
            .onSizeChanged { widthPx = it.width.toFloat() }
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
            .pointerInput(valueRange, enabled) {
                if (!enabled) return@pointerInput
                // Single gesture loop for tap + drag: two competing detectors on one
                // element starve each other, which left sliders dead on device.
                // Value applies on down (tap jumps) and tracks the finger on drag.
                awaitEachGesture {
                    val down = awaitFirstDown()
                    setFromX(down.position.x)
                    while (true) {
                        val event = awaitPointerEvent()
                        val changes = event.changes
                        if (changes.all { !it.pressed }) break
                        val change = changes.firstOrNull() ?: break
                        if (change.positionChange() != Offset.Zero) {
                            change.consume()
                            setFromX(change.position.x)
                        }
                    }
                }
            }
    ) {
        val trackH = 10.dp.toPx()
        val cy = size.height / 2f
        val thumb = 22.dp.toPx()
        val thumbX = (frac * size.width).coerceIn(thumb / 2f, size.width - thumb / 2f)
        val alpha = if (enabled) 1f else 0.4f
        // Inactive track (full width, theme-independent gray).
        drawRoundRect(
            color = NeoBorder.copy(alpha = alpha),
            topLeft = Offset(0f, cy - trackH / 2f),
            size = Size(size.width, trackH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Active track (start -> thumb).
        drawRoundRect(
            color = NeoDark.copy(alpha = alpha),
            topLeft = Offset(0f, cy - trackH / 2f),
            size = Size((thumbX).coerceAtLeast(0f), trackH),
            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
        )
        // Square thumb with dark border (brutalist, always a square — never theme-shaped).
        val half = thumb / 2f
        drawRect(
            color = NeoDark.copy(alpha = alpha),
            topLeft = Offset(thumbX - half - 2.dp.toPx(), cy - half - 2.dp.toPx()),
            size = Size(thumb + 4.dp.toPx(), thumb + 4.dp.toPx())
        )
        drawRect(
            color = NeoYellow.copy(alpha = alpha),
            topLeft = Offset(thumbX - half, cy - half),
            size = Size(thumb, thumb)
        )
    }
}

@Composable
fun NeoBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = NeoYellow,
    textColor: Color = NeoDark,
    borderColor: Color = NeoBorder
) {
    Box(
        modifier = modifier
            .background(backgroundColor, RectangleShape)
            .border(1.5.dp, borderColor, RectangleShape)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
    }
}
