package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.FontItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.FontHelper
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBlue
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoOrange
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoRed
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.ImageCropperView
import com.example.ui.components.NormalizedCropRect
import com.example.viewmodel.DrawingPath
import com.example.viewmodel.EditorState
import com.example.viewmodel.TextOverlay

@Composable
fun PhotoEditorScreen(
    editorState: EditorState,
    fontsList: List<FontItem>,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onToolChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onRotate: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onCropRatioChange: (String) -> Unit,
    onCropChange: (NormalizedCropRect) -> Unit = {},
    onApplyCrop: () -> Unit = {},
    onResetCrop: () -> Unit = {},
    onAdjustmentsChange: (Float, Float, Float, Float) -> Unit,
    onAddDrawingPath: (DrawingPath) -> Unit,
    onUndoDrawing: () -> Unit,
    onClearDrawing: () -> Unit,
    onTextOverlayChange: (TextOverlay?) -> Unit,
    onInstallCustomFont: (String, String) -> Unit,
    onSave: (asNew: Boolean) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val media = editorState.mediaItem ?: return

    // Back discards the editor (same as the X button).
    BackHandler(onBack = onClose)

    val editorImageRequest = remember(media.uri) {
        ImageRequest.Builder(context)
            .data(media.uri)
            .crossfade(false)
            .build()
    }

    var showInstallFontDialog by remember { mutableStateOf(false) }
    var newFontName by remember { mutableStateOf("") }
    var newFontCategory by remember { mutableStateOf("Custom Installed") }

    // Doodle tool state
    var selectedBrushColor by remember { mutableStateOf(NeoYellow) }
    var brushStrokeWidth by remember { mutableFloatStateOf(8f) }
    var currentDoodlePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    // Text tool state — now fully draggable anywhere on the image
    var editingTextString by remember { mutableStateOf(editorState.textOverlay?.text ?: "") }
    var selectedFontId by remember { mutableStateOf(editorState.textOverlay?.fontId ?: "font_neo_black") }
    var fontSizeSp by remember { mutableFloatStateOf(editorState.textOverlay?.fontSizeSp ?: 26f) }
    var selectedTextColor by remember { mutableStateOf(NeoDark) }
    var selectedBadgeColor by remember { mutableStateOf(NeoYellow) }
    var hasBackgroundBadge by remember { mutableStateOf(true) }
    var selectedTextAlign by remember { mutableStateOf(editorState.textOverlay?.textAlign ?: "CENTER") }
    var badgeAlpha by remember { mutableFloatStateOf(editorState.textOverlay?.badgeAlpha ?: 1f) }
    var textDragOffset by remember { mutableStateOf(Offset.Zero) }
    var previewBoxSize by remember { mutableStateOf(IntSize.Zero) }
    // Sync initial drag offset from stored normalized position (once)
    androidx.compose.runtime.LaunchedEffect(editorState.textOverlay) {
        editorState.textOverlay?.let {
            if (previewBoxSize.width > 0 && previewBoxSize.height > 0) {
                textDragOffset = Offset(
                    x = (it.xOffsetNorm - 0.5f) * previewBoxSize.width,
                    y = (it.yOffsetNorm - 0.5f) * previewBoxSize.height
                )
            }
        }
    }
    // Push every local text edit (including drag) to ViewModel so Save persists it
    androidx.compose.runtime.LaunchedEffect(editingTextString, selectedFontId, fontSizeSp, selectedTextColor, selectedBadgeColor, hasBackgroundBadge, selectedTextAlign, badgeAlpha, textDragOffset, previewBoxSize) {
        val anchorW = previewBoxSize.width
        val anchorH = previewBoxSize.height
        if (editingTextString.isNotBlank() && previewBoxSize.width > 0) {
            val normX = (0.5f + textDragOffset.x / previewBoxSize.width).coerceIn(0.05f, 0.95f)
            val normY = (0.5f + textDragOffset.y / previewBoxSize.height).coerceIn(0.05f, 0.95f)
            onTextOverlayChange(
                TextOverlay(
                    text = editingTextString,
                    fontId = selectedFontId,
                    fontSizeSp = fontSizeSp,
                    textColor = selectedTextColor.value.toLong(),
                    backgroundColor = selectedBadgeColor.value.toLong(),
                    hasBackgroundBadge = hasBackgroundBadge,
                    badgeAlpha = badgeAlpha,
                    textAlign = selectedTextAlign,
                    xOffsetNorm = normX,
                    yOffsetNorm = normY,
                    anchorW = anchorW,
                    anchorH = anchorH
                )
            )
        } else if (editingTextString.isBlank()) {
            onTextOverlayChange(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("photo_editor_screen")
    ) {
        // Notch Clearance & Safety Line: prevents contents from interfering with camera cutout/notch
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(3.5.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(NeoDark.copy(alpha = 0.35f))
            )
        }

        // Top Action Bar: Close, Title, Undo, Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Cancel",
                onClick = onClose,
                backgroundColor = NeoWhite,
                testTag = "editor_btn_close"
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoBadge(
                    text = "STUDIO",
                    backgroundColor = NeoYellow,
                    textColor = NeoDark
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "EDIT PHOTO",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = NeoDark
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    onClick = onUndo,
                    backgroundColor = if (canUndo) NeoWhite else NeoBg,
                    tint = if (canUndo) NeoDark else Color.LightGray,
                    size = 38.dp,
                    shadowOffset = 2.dp,
                    testTag = "editor_btn_undo"
                )
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.Redo,
                    contentDescription = "Redo",
                    onClick = onRedo,
                    backgroundColor = if (canRedo) NeoWhite else NeoBg,
                    tint = if (canRedo) NeoDark else Color.LightGray,
                    size = 38.dp,
                    shadowOffset = 2.dp,
                    testTag = "editor_btn_redo"
                )
                NeoButton(
                    text = "SAVE",
                    onClick = { onSave(true) },
                    containerColor = NeoMint,
                    leadingIcon = Icons.Default.Save,
                    testTag = "editor_btn_save"
                )
            }
        }

        // Live Photo Preview Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(12.dp)
                .clip(RectangleShape)
                .background(NeoDark)
                .border(2.5.dp, NeoBorder, RectangleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { previewBoxSize = it }
                    .graphicsLayer(
                        rotationZ = editorState.rotation,
                        scaleX = if (editorState.flipH) -1f else 1f,
                        scaleY = if (editorState.flipV) -1f else 1f
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Main Photo with Color Filter
                AsyncImage(
                    model = editorImageRequest,
                    contentDescription = media.title,
                    contentScale = ContentScale.Fit,
                    colorFilter = FilterHelper.getColorFilter(
                        editorState.selectedFilter,
                        editorState.brightness,
                        editorState.contrast,
                        editorState.saturation,
                        editorState.warmth
                    ),
                    modifier = Modifier.fillMaxSize()
                )

                // Interactive Crop Overlay when CROP tool is active
                if (editorState.activeTool == "CROP") {
                    ImageCropperView(
                        imageWidth = media.width,
                        imageHeight = media.height,
                        cropRect = editorState.cropRectNorm,
                        selectedAspectRatio = editorState.cropRatio,
                        onCropChange = onCropChange,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Doodles Canvas
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(editorState.activeTool, selectedBrushColor, brushStrokeWidth) {
                            if (editorState.activeTool == "DOODLE") {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        currentDoodlePoints = listOf(offset.x to offset.y)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        currentDoodlePoints = currentDoodlePoints + (change.position.x to change.position.y)
                                    },
                                    onDragEnd = {
                                        if (currentDoodlePoints.size > 1) {
                                            onAddDrawingPath(
                                                DrawingPath(
                                                    points = currentDoodlePoints,
                                                    color = selectedBrushColor.value.toLong(),
                                                    strokeWidth = brushStrokeWidth
                                                )
                                            )
                                        }
                                        currentDoodlePoints = emptyList()
                                    }
                                )
                            }
                        }
                ) {
                    // Draw committed paths
                    editorState.drawingPaths.forEach { dp ->
                        if (dp.points.size > 1) {
                            val path = Path().apply {
                                moveTo(dp.points.first().first, dp.points.first().second)
                                for (i in 1 until dp.points.size) {
                                    lineTo(dp.points[i].first, dp.points[i].second)
                                }
                            }
                            drawPath(
                                path = path,
                                color = Color(dp.color.toULong()),
                                style = Stroke(
                                    width = dp.strokeWidth,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw currently dragging path
                    if (currentDoodlePoints.size > 1) {
                        val activePath = Path().apply {
                            moveTo(currentDoodlePoints.first().first, currentDoodlePoints.first().second)
                            for (i in 1 until currentDoodlePoints.size) {
                                lineTo(currentDoodlePoints[i].first, currentDoodlePoints[i].second)
                            }
                        }
                        drawPath(
                            path = activePath,
                            color = selectedBrushColor,
                            style = Stroke(
                                width = brushStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Text Overlay Preview — fully draggable anywhere on the image
                if (editingTextString.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(textDragOffset.x.roundToInt(), textDragOffset.y.roundToInt()) }
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val next = textDragOffset + dragAmount
                                            // Keep text inside visible bounds
                                            val maxX = if (previewBoxSize.width > 0) previewBoxSize.width / 2.5f else 500f
                                            val maxY = if (previewBoxSize.height > 0) previewBoxSize.height / 2.5f else 500f
                                            textDragOffset = Offset(
                                                x = next.x.coerceIn(-maxX, maxX),
                                                y = next.y.coerceIn(-maxY, maxY)
                                            )
                                        }
                                    )
                                }
                                .then(
                                    if (hasBackgroundBadge) {
                                        Modifier
                                            .background(
                                                selectedBadgeColor.copy(alpha = badgeAlpha),
                                                RectangleShape
                                            )
                                            .border(2.dp, NeoBorder, RectangleShape)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    } else {
                                        Modifier.padding(8.dp)
                                    }
                                )
                        ) {
                            // Contrast outline picked from text luminance, so the caption
                            // stays readable over dark AND bright photos. Outline pass
                            // draws first (underneath), fill pass with drop shadow on top.
                            val outlineColor =
                                if (selectedTextColor.luminance() > 0.5f) NeoDark else NeoWhite
                            val align = when (selectedTextAlign) {
                                "LEFT" -> TextAlign.Start
                                "RIGHT" -> TextAlign.End
                                else -> TextAlign.Center
                            }
                            val captionStyle = TextStyle(
                                fontSize = fontSizeSp.sp,
                                fontFamily = FontHelper.getFontFamilyForId(selectedFontId),
                                fontWeight = FontHelper.getFontWeightForId(selectedFontId),
                                fontStyle = FontHelper.getFontStyleForId(selectedFontId),
                                textAlign = align
                            )
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = editingTextString,
                                    style = captionStyle.copy(
                                        color = outlineColor,
                                        drawStyle = Stroke(width = 8f)
                                    )
                                )
                                Text(
                                    text = editingTextString,
                                    style = captionStyle.copy(
                                        color = selectedTextColor,
                                        shadow = Shadow(
                                            color = Color.Black.copy(alpha = 0.6f),
                                            offset = Offset(3f, 3f),
                                            blurRadius = 6f
                                        )
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Tool Bar Panels
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = NeoWhite,
            border = androidx.compose.foundation.BorderStroke(2.5.dp, NeoBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Secondary Controls Row based on active tool (TEXT needs room for
                // size / align / color / opacity controls, and scrolls internally)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (editorState.activeTool == "TEXT") 208.dp else 115.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (editorState.activeTool) {
                        "FILTERS" -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 4.dp)
                            ) {
                                items(FilterHelper.AVAILABLE_FILTERS) { filterName ->
                                    val isSelected = editorState.selectedFilter == filterName
                                    Box(
                                        modifier = Modifier
                                            .clip(RectangleShape)
                                            .background(if (isSelected) NeoYellow else NeoBg)
                                            .border(2.dp, NeoBorder, RectangleShape)
                                            .clickable { onFilterChange(filterName) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = filterName.uppercase(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeoDark
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        "CROP" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Preset Aspect Ratios Toolbar
                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    val ratios = listOf("Freeform", "1:1", "9:16", "16:9", "4:3", "3:4")
                                    items(ratios) { ratio ->
                                        val isSelected = editorState.cropRatio == ratio
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                                .border(
                                                    width = if (isSelected) 2.dp else 1.5.dp,
                                                    color = if (isSelected) NeoDark else NeoBorder,
                                                    shape = RectangleShape
                                                )
                                                .clickable { onCropRatioChange(ratio) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = ratio.uppercase(),
                                                fontWeight = FontWeight.Black,
                                                fontSize = 11.sp,
                                                color = NeoDark
                                            )
                                        }
                                    }
                                }

                                // Secondary Actions: Rotate, Flip, Reset & Apply Crop
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NeoIconButton(
                                            icon = Icons.Default.RotateRight,
                                            contentDescription = "Rotate 90°",
                                            onClick = onRotate,
                                            backgroundColor = NeoWhite,
                                            size = 36.dp
                                        )
                                        NeoIconButton(
                                            icon = Icons.Default.Flip,
                                            contentDescription = "Flip Horizontal",
                                            onClick = onFlipH,
                                            backgroundColor = NeoWhite,
                                            size = 36.dp
                                        )
                                        NeoIconButton(
                                            icon = Icons.Default.RestartAlt,
                                            contentDescription = "Reset Crop",
                                            onClick = onResetCrop,
                                            backgroundColor = NeoWhite,
                                            size = 36.dp
                                        )
                                    }

                                    NeoButton(
                                        text = "APPLY CROP",
                                        onClick = onApplyCrop,
                                        containerColor = NeoMint,
                                        leadingIcon = Icons.Default.Check,
                                        testTag = "editor_btn_apply_crop"
                                    )
                                }
                            }
                        }

                        "ADJUST" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "ADJUST COLOR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    NeoBadge(
                                        text = "RESET",
                                        backgroundColor = NeoPink,
                                        textColor = NeoWhite,
                                        modifier = Modifier.clickable {
                                            onAdjustmentsChange(0f, 0f, 0f, 0f)
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))

                                AdjustSliderRow(
                                    label = "BRIGHTNESS ${editorState.brightness.toInt()}",
                                    value = editorState.brightness,
                                    onValueChange = {
                                        onAdjustmentsChange(it, editorState.contrast, editorState.saturation, editorState.warmth)
                                    }
                                )
                                AdjustSliderRow(
                                    label = "CONTRAST ${editorState.contrast.toInt()}",
                                    value = editorState.contrast,
                                    onValueChange = {
                                        onAdjustmentsChange(editorState.brightness, it, editorState.saturation, editorState.warmth)
                                    }
                                )
                                AdjustSliderRow(
                                    label = "SATURATION ${editorState.saturation.toInt()}",
                                    value = editorState.saturation,
                                    onValueChange = {
                                        onAdjustmentsChange(editorState.brightness, editorState.contrast, it, editorState.warmth)
                                    }
                                )
                                AdjustSliderRow(
                                    label = "WARMTH ${editorState.warmth.toInt()}",
                                    value = editorState.warmth,
                                    onValueChange = {
                                        onAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, it)
                                    }
                                )
                            }
                        }

                        "DOODLE" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Color palette
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf(NeoYellow, NeoMint, NeoCyan, NeoPink, NeoDark, NeoWhite).forEach { color ->
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .background(color, RectangleShape)
                                                    .border(
                                                        width = if (selectedBrushColor == color) 3.dp else 1.5.dp,
                                                        color = if (selectedBrushColor == color) NeoDark else NeoBorder,
                                                        shape = RectangleShape
                                                    )
                                                    .clickable { selectedBrushColor = color }
                                            )
                                        }
                                    }

                                    // Undo & Clear
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        NeoIconButton(
                                            icon = Icons.AutoMirrored.Filled.Undo,
                                            contentDescription = "Undo",
                                            onClick = onUndoDrawing,
                                            backgroundColor = NeoWhite,
                                            size = 32.dp,
                                            shadowOffset = 1.dp
                                        )
                                        NeoBadge(
                                            text = "CLEAR",
                                            backgroundColor = NeoPink,
                                            textColor = NeoWhite,
                                            modifier = Modifier.clickable(onClick = onClearDrawing)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("STROKE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Slider(
                                        value = brushStrokeWidth,
                                        onValueChange = { brushStrokeWidth = it },
                                        valueRange = 3f..24f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                }
                            }
                        }

                        "TEXT" -> {
                            var hueDeg by remember { mutableFloatStateOf(200f) }
                            var hexInput by remember { mutableStateOf("") }
                            var hexError by remember { mutableStateOf(false) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                // Text Input Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    OutlinedTextField(
                                        value = editingTextString,
                                        onValueChange = { editingTextString = it },
                                        placeholder = { Text("Enter caption...") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f).height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeoDark,
                                            unfocusedBorderColor = NeoBorder,
                                            focusedContainerColor = NeoBg,
                                            unfocusedContainerColor = NeoBg
                                        )
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Install custom font button
                                    NeoButton(
                                        text = "+ FONT",
                                        onClick = { showInstallFontDialog = true },
                                        containerColor = NeoCyan,
                                        modifier = Modifier.height(44.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Fonts Selector from Free Fonts Repo
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp)
                                ) {
                                    items(fontsList) { font ->
                                        val isSelected = selectedFontId == font.id
                                        Box(
                                            modifier = Modifier
                                                .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                                .border(1.5.dp, NeoBorder, RectangleShape)
                                                .clickable { selectedFontId = font.id }
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = font.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontHelper.getFontFamilyForId(font.id)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Font size
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("SIZE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Slider(
                                        value = fontSizeSp,
                                        onValueChange = { fontSizeSp = it },
                                        valueRange = 14f..72f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    Text(
                                        text = "${fontSizeSp.toInt()}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.width(28.dp)
                                    )
                                }

                                // Alignment segmented control
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("ALIGN", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    listOf("LEFT", "CENTER", "RIGHT").forEach { option ->
                                        val isSelected = selectedTextAlign == option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                                .border(1.5.dp, NeoBorder, RectangleShape)
                                                .clickable { selectedTextAlign = option }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = option,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = NeoDark
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Preset palette bar for text color
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("COLOR", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    listOf(
                                        NeoYellow, NeoMint, NeoCyan, NeoPink,
                                        NeoOrange, NeoRed, NeoBlue, NeoDark, NeoWhite
                                    ).forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .background(color, RectangleShape)
                                                .border(
                                                    width = if (selectedTextColor == color) 3.dp else 1.5.dp,
                                                    color = if (selectedTextColor == color) NeoDark else NeoBorder,
                                                    shape = RectangleShape
                                                )
                                                .clickable {
                                                    selectedTextColor = color
                                                    hexInput = ""
                                                    hexError = false
                                                }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // HSV hue slider + hex input with live swatch
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(selectedTextColor, RectangleShape)
                                            .border(1.5.dp, NeoBorder, RectangleShape)
                                    )
                                    Slider(
                                        value = hueDeg,
                                        onValueChange = {
                                            hueDeg = it
                                            selectedTextColor = hsvToPickerColor(it)
                                            hexInput = ""
                                            hexError = false
                                        },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    OutlinedTextField(
                                        value = hexInput,
                                        onValueChange = { raw ->
                                            hexInput = raw
                                            val parsed = hexToPickerColorOrNull(raw)
                                            if (parsed != null) {
                                                selectedTextColor = parsed
                                                hueDeg = pickerColorToHue(parsed)
                                                hexError = false
                                            } else {
                                                hexError = raw.isNotBlank()
                                            }
                                        },
                                        placeholder = { Text("#RRGGBB", fontSize = 10.sp) },
                                        singleLine = true,
                                        isError = hexError,
                                        modifier = Modifier.width(104.dp).height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeoDark,
                                            unfocusedBorderColor = NeoBorder,
                                            focusedContainerColor = NeoBg,
                                            unfocusedContainerColor = NeoBg,
                                            errorBorderColor = NeoRed
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Badge toggle + opacity
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("BADGE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = hasBackgroundBadge,
                                        onCheckedChange = { hasBackgroundBadge = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = NeoDark,
                                            checkedTrackColor = NeoYellow,
                                            uncheckedThumbColor = NeoDark,
                                            uncheckedTrackColor = NeoBorder
                                        )
                                    )
                                    Slider(
                                        value = badgeAlpha,
                                        onValueChange = { badgeAlpha = it },
                                        valueRange = 0f..1f,
                                        enabled = hasBackgroundBadge,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    Text(
                                        text = "${(badgeAlpha * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(NeoBorder)
                )

                // Bottom Primary Tool Selector Tabs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tools = listOf(
                        "FILTERS" to Icons.Default.Tune,
                        "CROP" to Icons.Default.Crop,
                        "ADJUST" to Icons.Default.RestartAlt,
                        "DOODLE" to Icons.Default.Brush,
                        "TEXT" to Icons.Default.FontDownload
                    )

                    tools.forEach { (toolName, icon) ->
                        val isSelected = editorState.activeTool == toolName
                        Column(
                            modifier = Modifier
                                .clickable { onToolChange(toolName) }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                    .border(1.5.dp, NeoBorder, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = toolName,
                                    tint = NeoDark,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = toolName,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                color = NeoDark
                            )
                        }
                    }
                }
            }
        }

        // Install Font Modal Dialog
        if (showInstallFontDialog) {
            AlertDialog(
                onDismissRequest = { showInstallFontDialog = false },
                title = {
                    Text(
                        text = "INSTALL CUSTOM FONT",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Add any custom .TTF/.OTF font into your local Gallery studio library:",
                            fontSize = 13.sp,
                            color = NeoDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newFontName,
                            onValueChange = { newFontName = it },
                            placeholder = { Text("Font name e.g. Cyberpunk Bold") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeoDark,
                                unfocusedBorderColor = NeoBorder,
                                focusedContainerColor = NeoWhite,
                                unfocusedContainerColor = NeoWhite
                            )
                        )
                    }
                },
                confirmButton = {
                    NeoButton(
                        text = "INSTALL",
                        onClick = {
                            if (newFontName.isNotBlank()) {
                                onInstallCustomFont(newFontName, newFontCategory)
                                newFontName = ""
                                showInstallFontDialog = false
                            }
                        },
                        containerColor = NeoCyan
                    )
                },
                dismissButton = {
                    NeoButton(
                        text = "CANCEL",
                        onClick = { showInstallFontDialog = false },
                        containerColor = NeoWhite
                    )
                },
                containerColor = NeoBg,
                shape = RectangleShape
            )
        }
    }
}

/**
 * Custom color-picker helpers: preset palette + HSV hue slider + hex input.
 * [hsvToPickerColor] maps a 0..360 hue to a vivid color; [hexToPickerColorOrNull]
 * accepts #RRGGBB or #AARRGGBB; [pickerColorToHue] syncs the hue slider to a pick.
 */
private fun hsvToPickerColor(hue: Float, sat: Float = 0.85f, value: Float = 1f): Color {
    val rgb = android.graphics.Color.HSVToColor(
        floatArrayOf(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f))
    )
    return Color(rgb)
}

private fun hexToPickerColorOrNull(raw: String): Color? {
    return try {
        val hex = raw.trim().removePrefix("#")
        val argb = when (hex.length) {
            6 -> "FF$hex"
            8 -> hex
            else -> return null
        }
        Color(argb.toULong(16))
    } catch (_: Exception) {
        null
    }
}

private fun pickerColorToHue(color: Color): Float {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), hsv)
    return hsv[0]
}

@Composable
private fun AdjustSliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            modifier = Modifier.width(110.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = NeoYellow,
                activeTrackColor = NeoDark
            )
        )
    }
}
