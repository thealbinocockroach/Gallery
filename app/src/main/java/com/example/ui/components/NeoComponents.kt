package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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

    Box(
        modifier = modifier
            .testTag(testTag)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Shadow
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
