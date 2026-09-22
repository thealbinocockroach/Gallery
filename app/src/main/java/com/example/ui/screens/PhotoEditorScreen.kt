package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.RotateLeft
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.viewmodel.EditorShape
import com.example.viewmodel.RetouchOp
import com.example.viewmodel.RetouchType
import com.example.viewmodel.ShapeType
import com.example.viewmodel.EditorState
import com.example.viewmodel.TextOverlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

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
    onRotateCcw: () -> Unit = {},
    onResetAllEditor: () -> Unit = {},
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onCropRatioChange: (String) -> Unit,
    onCropChange: (NormalizedCropRect) -> Unit = {},
    onApplyCrop: () -> Unit = {},
    onResetCrop: () -> Unit = {},
    onAdjustmentsChange: (Float, Float, Float, Float) -> Unit,
    onFullAdjustmentsChange: (Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float, Float) -> Unit = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
    onFilterStrengthChange: (Float) -> Unit = {},
    onCompareToggle: () -> Unit = {},
    onAddShape: (EditorShape) -> Unit = {},
    onClearShapes: () -> Unit = {},
    onLevelAngleChange: (Float) -> Unit = {},
    onPerspectiveHChange: (Float) -> Unit = {},
    onPerspectiveVChange: (Float) -> Unit = {},
    onHslChange: (Int, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onBokehChange: (String, Float, Float, Float) -> Unit = { _, _, _, _ -> },
    onRetouchTap: (RetouchOp) -> Unit = {},
    onClearRetouch: () -> Unit = {},
    exportFormat: String = "JPEG",
    exportQuality: Int = 90,
    stripExif: Boolean = false,
    onSetExportFormat: (String) -> Unit = {},
    onSetExportQuality: (Int) -> Unit = {},
    onSetStripExif: (Boolean) -> Unit = {},
    needsSpatialPreview: Boolean = false,
    previewRenderer: (suspend (Int) -> android.graphics.Bitmap?)? = null,
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
    var showSaveDialog by remember { mutableStateOf(false) }
    var newFontName by remember { mutableStateOf("") }
    var newFontCategory by remember { mutableStateOf("Custom Installed") }

    // Doodle tool state
    var selectedBrushColor by remember { mutableStateOf(NeoYellow) }
    var brushStrokeWidth by remember { mutableFloatStateOf(8f) }
    var brushAlpha by remember { mutableFloatStateOf(1f) }
    var currentDoodlePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    // Retouch tap handler is configured by the RETOUCH panel below (mode/radius/strength).
    var retouchTapHandler by remember { mutableStateOf<((Float, Float) -> Unit)>({ _, _ -> }) }

    // Shape tool state
    var selectedShapeColor by remember { mutableStateOf(NeoCyan) }
    var shapeStrokeWidth by remember { mutableFloatStateOf(4f) }
    var shapeFillAlpha by remember { mutableFloatStateOf(0.3f) }
    var shapePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var currentShapeType by remember { mutableStateOf(ShapeType.RECT) }

    // Compare / Filter Strength
    var showCompare by remember { mutableStateOf(editorState.showCompare) }
    // Hold-to-compare: press-and-hold the preview to peek at the original base image.
    var pressComparing by remember { mutableStateOf(false) }
    val isComparing = showCompare || pressComparing
    // Downscaled CPU render for spatial effects (sharp/clarity/denoise/vignette/HSL/
    // bokeh/retouch/level/perspective) so the preview stays WYSIWYG with export.
    var spatialBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(editorState, needsSpatialPreview, isComparing) {
        if (!needsSpatialPreview || previewRenderer == null || isComparing) {
            spatialBitmap = null
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(150)
        spatialBitmap = previewRenderer(1080)
    }
    val spatialImageBitmap = remember(spatialBitmap) { spatialBitmap?.asImageBitmap() }
    var filterStrength by remember { mutableFloatStateOf(editorState.filterStrength) }

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
                NeoBadge(
                    text = "RESET ALL",
                    backgroundColor = NeoPink,
                    textColor = NeoWhite,
                    modifier = Modifier.clickable(onClick = onResetAllEditor)
                )
                NeoButton(
                    text = "SAVE",
                    onClick = { showSaveDialog = true },
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
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                pressComparing = true
                                tryAwaitRelease()
                                pressComparing = false
                            }
                        )
                    }
                    .graphicsLayer(
                        rotationZ = editorState.rotation + editorState.levelAngle,
                        scaleX = if (editorState.flipH) -1f else 1f,
                        scaleY = if (editorState.flipV) -1f else 1f
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Main Photo: base original while comparing, CPU-rendered spatial preview
                // when available, otherwise GPU ColorMatrix path.
                if (isComparing) {
                    AsyncImage(
                        model = editorImageRequest,
                        contentDescription = media.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (spatialImageBitmap != null) {
                    Image(
                        bitmap = spatialImageBitmap,
                        contentDescription = media.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    AsyncImage(
                        model = editorImageRequest,
                        contentDescription = media.title,
                        contentScale = ContentScale.Fit,
                        colorFilter = FilterHelper.getColorFilter(
                            editorState.selectedFilter,
                            editorState.brightness,
                            editorState.contrast,
                            editorState.saturation,
                            editorState.warmth,
                            editorState.tint,
                            editorState.highlights,
                            editorState.shadows,
                            editorState.whites,
                            editorState.blacks,
                            editorState.vibrance,
                            editorState.filterStrength
                        ),
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Interactive Crop Overlay when CROP tool is active (hidden while comparing)
                if (editorState.activeTool == "CROP" && !isComparing) {
                    ImageCropperView(
                        imageWidth = media.width,
                        imageHeight = media.height,
                        cropRect = editorState.cropRectNorm,
                        selectedAspectRatio = editorState.cropRatio,
                        levelAngle = editorState.levelAngle,
                        perspectiveHorizontal = editorState.perspectiveHorizontal,
                        perspectiveVertical = editorState.perspectiveVertical,
                        onCropChange = onCropChange,
                        onLevelAngleChange = onLevelAngleChange,
                        onPerspectiveHChange = onPerspectiveHChange,
                        onPerspectiveVChange = onPerspectiveVChange,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Doodles + Shapes Canvas (hidden while comparing with the original)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            editorState.activeTool, selectedBrushColor, brushStrokeWidth,
                            brushAlpha, currentShapeType, retouchTapHandler
                        ) {
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
                                                    color = selectedBrushColor.copy(alpha = brushAlpha).value.toLong(),
                                                    strokeWidth = brushStrokeWidth,
                                                    anchorW = previewBoxSize.width,
                                                    anchorH = previewBoxSize.height
                                                )
                                            )
                                        }
                                        currentDoodlePoints = emptyList()
                                    }
                                )
                            } else if (editorState.activeTool == "SHAPES") {
                                // Drag defines the shape bounds; DRAW SHAPE commits it.
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        shapePoints = listOf(offset.x to offset.y)
                                    },
                                    onDrag = { change, _ ->
                                        change.consume()
                                        val start = shapePoints.firstOrNull()
                                            ?: (change.position.x to change.position.y)
                                        shapePoints = listOf(start, change.position.x to change.position.y)
                                    }
                                )
                            } else if (editorState.activeTool == "RETOUCH") {
                                detectTapGestures(
                                    onTap = { offset ->
                                        val w = size.width.toFloat()
                                        val h = size.height.toFloat()
                                        if (w > 0 && h > 0) {
                                            retouchTapHandler(offset.x / w, offset.y / h)
                                        }
                                    }
                                )
                            }
                        }
                ) {
                    if (isComparing) return@Canvas
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

                    // Draw committed shapes (vector overlays in preview-box pixels)
                    editorState.shapes.forEach { shape ->
                        if (shape.points.size < 2) return@forEach
                        val a = shape.points.first()
                        val b = shape.points.last()
                        val left = minOf(a.first, b.first)
                        val top = minOf(a.second, b.second)
                        val right = maxOf(a.first, b.first)
                        val bottom = maxOf(a.second, b.second)
                        val base = Color(shape.color.toULong())
                        val stroke = Stroke(
                            width = shape.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                        when (shape.type) {
                            ShapeType.RECT -> {
                                if (shape.fillAlpha > 0f) {
                                    drawRect(
                                        color = base.copy(alpha = shape.fillAlpha),
                                        topLeft = Offset(left, top),
                                        size = Size(right - left, bottom - top)
                                    )
                                }
                                drawRect(
                                    color = base,
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top),
                                    style = stroke
                                )
                            }
                            ShapeType.CIRCLE -> {
                                val cx = (left + right) / 2f
                                val cy = (top + bottom) / 2f
                                val rad = minOf(right - left, bottom - top) / 2f
                                if (shape.fillAlpha > 0f) {
                                    drawCircle(color = base.copy(alpha = shape.fillAlpha), radius = rad, center = Offset(cx, cy))
                                }
                                drawCircle(color = base, radius = rad, center = Offset(cx, cy), style = stroke)
                            }
                            ShapeType.HIGHLIGHT -> {
                                drawRect(
                                    color = base.copy(alpha = (0.35f + shape.fillAlpha * 0.4f).coerceIn(0f, 0.75f)),
                                    topLeft = Offset(left, top),
                                    size = Size(right - left, bottom - top)
                                )
                            }
                            ShapeType.ARROW -> {
                                drawLine(start = Offset(a.first, a.second), end = Offset(b.first, b.second), color = base, strokeWidth = shape.strokeWidth, cap = StrokeCap.Round)
                                val angle = atan2(b.second - a.second, b.first - a.first)
                                val headLen = 24f
                                val headAng = 0.5f
                                listOf(angle + PI.toFloat() - headAng, angle + PI.toFloat() + headAng).forEach { ha ->
                                    drawLine(
                                        start = Offset(b.first, b.second),
                                        end = Offset(b.first + cos(ha) * headLen, b.second + sin(ha) * headLen),
                                        color = base,
                                        strokeWidth = shape.strokeWidth,
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                        }
                    }

                    // In-progress shape drag preview
                    if (editorState.activeTool == "SHAPES" && shapePoints.size >= 2) {
                        val a = shapePoints.first()
                        val b = shapePoints.last()
                        drawRect(
                            color = selectedShapeColor.copy(alpha = 0.5f),
                            topLeft = Offset(minOf(a.first, b.first), minOf(a.second, b.second)),
                            size = Size(abs(b.first - a.first), abs(b.second - a.second)),
                            style = Stroke(width = 3f)
                        )
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
                            color = selectedBrushColor.copy(alpha = brushAlpha),
                            style = Stroke(
                                width = brushStrokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Text Overlay Preview — fully draggable anywhere on the image
                // (hidden while comparing with the original)
                if (editingTextString.isNotBlank() && !isComparing) {
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
                        .height(if (editorState.activeTool in listOf("TEXT", "SHAPES", "DETAIL", "RETOUCH", "CROP")) 208.dp else 115.dp)
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when (editorState.activeTool) {
                        "FILTERS" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                            Spacer(modifier = Modifier.height(4.dp))
                            // Filter Strength 0..100%
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("STRENGTH", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Slider(
                                    value = filterStrength,
                                    onValueChange = { filterStrength = it; onFilterStrengthChange(it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    colors = SliderDefaults.colors(thumbColor = NeoYellow, activeTrackColor = NeoDark)
                                )
                                Text("${(filterStrength * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                            }
                            }
                        }

                        "CROP" -> {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .verticalScroll(rememberScrollState())
                                        .padding(vertical = 2.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Preset Aspect Ratios Toolbar
                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        val ratios = listOf("Freeform", "1:1", "9:16", "16:9", "4:3", "3:2", "5:4", "4:5", "3:4")
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

                                    // Level/Angle straightening slider
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("LEVEL", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = editorState.levelAngle,
                                            onValueChange = onLevelAngleChange,
                                            valueRange = -45f..45f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoYellow, activeTrackColor = NeoDark)
                                        )
                                        Text("${editorState.levelAngle.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                    }

                                    // Keystone correction sliders (baked on export)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("PERSP-H", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = editorState.perspectiveHorizontal,
                                            onValueChange = onPerspectiveHChange,
                                            valueRange = -45f..45f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoYellow, activeTrackColor = NeoDark)
                                        )
                                        Text("${editorState.perspectiveHorizontal.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("PERSP-V", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = editorState.perspectiveVertical,
                                            onValueChange = onPerspectiveVChange,
                                            valueRange = -45f..45f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoYellow, activeTrackColor = NeoDark)
                                        )
                                        Text("${editorState.perspectiveVertical.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
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
                                            contentDescription = "Rotate 90° clockwise",
                                            onClick = onRotate,
                                            backgroundColor = NeoWhite,
                                            size = 36.dp
                                        )
                                        NeoIconButton(
                                            icon = Icons.Default.RotateLeft,
                                            contentDescription = "Rotate 90° counter-clockwise",
                                            onClick = onRotateCcw,
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
                                            text = "EXPOSURE & COLOR",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        NeoBadge(
                                            text = "RESET",
                                            backgroundColor = NeoPink,
                                            textColor = NeoWhite,
                                            modifier = Modifier.clickable {
                                                onFullAdjustmentsChange(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))

                                    // Exposure
                                    AdjustSliderRow(
                                        label = "BRIGHTNESS ${editorState.brightness.toInt()}",
                                        value = editorState.brightness,
                                        onValueChange = { onFullAdjustmentsChange(it, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "CONTRAST ${editorState.contrast.toInt()}",
                                        value = editorState.contrast,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, it, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "HIGHLIGHTS ${editorState.highlights.toInt()}",
                                        value = editorState.highlights,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, it, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "SHADOWS ${editorState.shadows.toInt()}",
                                        value = editorState.shadows,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, it, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "WHITES ${editorState.whites.toInt()}",
                                        value = editorState.whites,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, it, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "BLACKS ${editorState.blacks.toInt()}",
                                        value = editorState.blacks,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, it, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )

                                    // Color
                                    AdjustSliderRow(
                                        label = "SATURATION ${editorState.saturation.toInt()}",
                                        value = editorState.saturation,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, it, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "VIBRANCE ${editorState.vibrance.toInt()}",
                                        value = editorState.vibrance,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, it, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "WARMTH ${editorState.warmth.toInt()}",
                                        value = editorState.warmth,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, it, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "TINT ${editorState.tint.toInt()}",
                                        value = editorState.tint,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, it, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )

                                    // HSL Color Mixer: per-channel hue/sat/lum for 8 channels
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("HSL MIXER", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    var hslChannel by remember { mutableStateOf(0) }
                                    val hslNames = listOf("RED", "ORANGE", "YELLOW", "GREEN", "CYAN", "BLUE", "PURPLE", "MAGENTA")
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        contentPadding = PaddingValues(horizontal = 2.dp)
                                    ) {
                                        items(8) { ch ->
                                            val isSelected = hslChannel == ch
                                            Box(
                                                modifier = Modifier
                                                    .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                                    .border(1.5.dp, NeoBorder, RectangleShape)
                                                    .clickable { hslChannel = ch }
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(hslNames[ch], fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoDark)
                                            }
                                        }
                                    }
                                    val hslBase = hslChannel * 3
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("HUE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = editorState.hsl[hslBase],
                                            onValueChange = { onHslChange(hslChannel, it, editorState.hsl[hslBase + 1], editorState.hsl[hslBase + 2]) },
                                            valueRange = -180f..180f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                        )
                                        Text("${editorState.hsl[hslBase].toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("SAT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = editorState.hsl[hslBase + 1],
                                            onValueChange = { onHslChange(hslChannel, editorState.hsl[hslBase], it, editorState.hsl[hslBase + 2]) },
                                            valueRange = -100f..100f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                        )
                                        Text("${editorState.hsl[hslBase + 1].toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("LUM", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = editorState.hsl[hslBase + 2],
                                            onValueChange = { onHslChange(hslChannel, editorState.hsl[hslBase], editorState.hsl[hslBase + 1], it) },
                                            valueRange = -100f..100f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                        )
                                        Text("${editorState.hsl[hslBase + 2].toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
                                    }

                                    // Detail
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("DETAIL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    AdjustSliderRow(
                                        label = "SHARPNESS ${editorState.sharpness.toInt()}",
                                        value = editorState.sharpness,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, it, editorState.clarity, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "CLARITY ${editorState.clarity.toInt()}",
                                        value = editorState.clarity,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, it, editorState.denoise, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "DENOISE ${editorState.denoise.toInt()}",
                                        value = editorState.denoise,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, it, editorState.vignette) }
                                    )
                                    AdjustSliderRow(
                                        label = "VIGNETTE ${editorState.vignette.toInt()}",
                                        value = editorState.vignette,
                                        onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, it) }
                                    )

                                    // Filter Strength
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("FILTER INTENSITY", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Slider(
                                            value = filterStrength,
                                            onValueChange = { filterStrength = it; onFilterStrengthChange(it) },
                                            valueRange = 0f..1f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoYellow, activeTrackColor = NeoDark)
                                        )
                                        Text("${(filterStrength * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                    }

                                    // Hold to Compare toggle
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("HOLD TO COMPARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Switch(
                                            checked = showCompare,
                                            onCheckedChange = { showCompare = it; onCompareToggle() },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = NeoDark,
                                                checkedTrackColor = NeoYellow,
                                                uncheckedThumbColor = NeoDark,
                                                uncheckedTrackColor = NeoBorder
                                            )
                                        )
                                    }
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

                                Spacer(modifier = Modifier.height(4.dp))

                                // Brush opacity
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("OPACITY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Slider(
                                        value = brushAlpha,
                                        onValueChange = { brushAlpha = it },
                                        valueRange = 0.1f..1f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    Text("${(brushAlpha * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Brush custom color: hue slider + hex input with live swatch
                                var brushHue by remember { mutableFloatStateOf(55f) }
                                var brushHex by remember { mutableStateOf("") }
                                var brushHexError by remember { mutableStateOf(false) }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .background(selectedBrushColor, RectangleShape)
                                            .border(1.5.dp, NeoBorder, RectangleShape)
                                    )
                                    Slider(
                                        value = brushHue,
                                        onValueChange = {
                                            brushHue = it
                                            selectedBrushColor = hsvToPickerColor(it)
                                            brushHex = ""
                                            brushHexError = false
                                        },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    OutlinedTextField(
                                        value = brushHex,
                                        onValueChange = { raw ->
                                            brushHex = raw
                                            val parsed = hexToPickerColorOrNull(raw)
                                            if (parsed != null) {
                                                selectedBrushColor = parsed
                                                brushHue = pickerColorToHue(parsed)
                                                brushHexError = false
                                            } else {
                                                brushHexError = raw.isNotBlank()
                                            }
                                        },
                                        placeholder = { Text("#HEX", fontSize = 10.sp) },
                                        singleLine = true,
                                        isError = brushHexError,
                                        modifier = Modifier.width(96.dp).height(48.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = NeoDark,
                                            unfocusedBorderColor = NeoBorder,
                                            focusedContainerColor = NeoBg,
                                            unfocusedContainerColor = NeoBg,
                                            errorBorderColor = NeoRed
                                        )
                                    )
                                }
                            }
                        }

                        "RETOUCH" -> {
                            var retouchMode by remember { mutableStateOf("HEAL") }
                            var retouchRadius by remember { mutableFloatStateOf(36f) }
                            var retouchStrength by remember { mutableFloatStateOf(0.8f) }
                            var bokehShape by remember { mutableStateOf("RADIAL") }
                            // Wire taps from current panel state every composition.
                            retouchTapHandler = { xNorm, yNorm ->
                                val bw = previewBoxSize.width
                                val bh = previewBoxSize.height
                                if (bw > 0 && bh > 0) {
                                    if (retouchMode == "BOKEH") {
                                        onBokehChange(bokehShape, retouchStrength, xNorm, yNorm)
                                    } else {
                                        onRetouchTap(
                                            RetouchOp(
                                                type = if (retouchMode == "REDEYE") RetouchType.REDEYE else RetouchType.HEAL,
                                                xNorm = xNorm,
                                                yNorm = yNorm,
                                                radiusNorm = (retouchRadius / bw).coerceIn(0.01f, 0.5f),
                                                strength = retouchStrength,
                                                anchorW = bw,
                                                anchorH = bh
                                            )
                                        )
                                    }
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState())
                            ) {
                                Text(
                                    text = "TAP PHOTO TO APPLY" + if (retouchMode == "BOKEH") " FOCUS" else "",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("HEAL", "REDEYE", "BOKEH").forEach { option ->
                                        val isSelected = retouchMode == option
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                                .border(1.5.dp, NeoBorder, RectangleShape)
                                                .clickable { retouchMode = option }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(option, fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoDark)
                                        }
                                    }
                                }
                                if (retouchMode == "BOKEH") {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("RADIAL", "LINEAR").forEach { option ->
                                            val isSelected = bokehShape == option
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .background(if (isSelected) NeoCyan else NeoBg, RectangleShape)
                                                    .border(1.5.dp, NeoBorder, RectangleShape)
                                                    .clickable { bokehShape = option }
                                                    .padding(vertical = 5.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(option, fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoDark)
                                            }
                                        }
                                    }
                                    if (editorState.bokehType != "OFF") {
                                        Text(
                                            text = "FOCUS LOCKED — TAP TO MOVE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeoDark.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("RADIUS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        Slider(
                                            value = retouchRadius,
                                            onValueChange = { retouchRadius = it },
                                            valueRange = 12f..120f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                            colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                        )
                                        Text("${retouchRadius.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("STRENGTH", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Slider(
                                        value = retouchStrength,
                                        onValueChange = { retouchStrength = it },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    Text("${(retouchStrength * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                NeoButton(
                                    text = "CLEAR RETOUCH",
                                    onClick = {
                                        onClearRetouch()
                                        onBokehChange("OFF", 0f, 0.5f, 0.5f)
                                    },
                                    containerColor = NeoPink,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (editorState.retouchOps.isNotEmpty() || editorState.bokehType != "OFF") {
                                    Text(
                                        text = "${editorState.retouchOps.size} SPOTS" +
                                            if (editorState.bokehType != "OFF") " + BOKEH ${editorState.bokehType}" else "",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoDark.copy(alpha = 0.7f)
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
// SHAPES panel (branch of the tool when above)
                        "SHAPES" -> {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("SHAPE TOOL", fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    NeoBadge(
                                        text = "CLEAR",
                                        backgroundColor = NeoPink,
                                        textColor = NeoWhite,
                                        modifier = Modifier.clickable { onClearShapes() }
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Shape type selector
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("TYPE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    listOf("RECT", "CIRCLE", "ARROW", "HIGHLIGHT").forEach { type ->
                                        val isSelected = currentShapeType == ShapeType.valueOf(type)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                                .border(1.5.dp, NeoBorder, RectangleShape)
                                                .clickable { currentShapeType = ShapeType.valueOf(type) }
                                                .padding(vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(type, fontSize = 10.sp, fontWeight = FontWeight.Black, color = NeoDark)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Color + Size
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("COLOR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    listOf(NeoYellow, NeoMint, NeoCyan, NeoPink, NeoOrange, NeoRed, NeoDark, NeoWhite).forEach { color ->
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .background(color, RectangleShape)
                                                .border(
                                                    width = if (selectedShapeColor == color) 3.dp else 1.5.dp,
                                                    color = if (selectedShapeColor == color) NeoDark else NeoBorder,
                                                    shape = RectangleShape
                                                )
                                                .clickable { selectedShapeColor = color }
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("SIZE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Slider(
                                        value = shapeStrokeWidth,
                                        onValueChange = { shapeStrokeWidth = it },
                                        valueRange = 2f..12f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        colors = SliderDefaults.colors(thumbColor = NeoDark, activeTrackColor = NeoDark)
                                    )
                                    Text("${shapeStrokeWidth.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                NeoButton(
                                    text = "DRAW SHAPE",
                                    onClick = {
                                        if (shapePoints.size >= 2) {
                                            onAddShape(
                                                EditorShape(
                                                    type = currentShapeType,
                                                    points = shapePoints,
                                                    color = selectedShapeColor.value.toLong(),
                                                    strokeWidth = shapeStrokeWidth,
                                                    fillAlpha = shapeFillAlpha,
                                                    anchorW = previewBoxSize.width,
                                                    anchorH = previewBoxSize.height
                                                )
                                            )
                                            shapePoints = emptyList()
                                        } else {
                                            onClearShapes()
                                        }
                                    },
                                    containerColor = NeoCyan,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

// DETAIL panel
                        "DETAIL" -> {
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
                                    Text("DETAIL ENHANCE", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    NeoBadge(
                                        text = "RESET",
                                        backgroundColor = NeoPink,
                                        textColor = NeoWhite,
                                        modifier = Modifier.clickable {
                                            onFullAdjustmentsChange(
                                                editorState.brightness, editorState.contrast,
                                                editorState.saturation, editorState.warmth,
                                                editorState.highlights, editorState.shadows,
                                                editorState.whites, editorState.blacks,
                                                editorState.tint, editorState.vibrance,
                                                0f, 0f, 0f, 0f
                                            )
                                        }
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                AdjustSliderRow(
                                    label = "SHARPNESS ${editorState.sharpness.toInt()}",
                                    value = editorState.sharpness,
                                    onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, it, editorState.clarity, editorState.denoise, editorState.vignette) }
                                )
                                AdjustSliderRow(
                                    label = "CLARITY ${editorState.clarity.toInt()}",
                                    value = editorState.clarity,
                                    onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, it, editorState.denoise, editorState.vignette) }
                                )
                                AdjustSliderRow(
                                    label = "DENOISE ${editorState.denoise.toInt()}",
                                    value = editorState.denoise,
                                    onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, it, editorState.vignette) }
                                )
                                AdjustSliderRow(
                                    label = "VIGNETTE ${editorState.vignette.toInt()}",
                                    value = editorState.vignette,
                                    onValueChange = { onFullAdjustmentsChange(editorState.brightness, editorState.contrast, editorState.saturation, editorState.warmth, editorState.highlights, editorState.shadows, editorState.whites, editorState.blacks, editorState.tint, editorState.vibrance, editorState.sharpness, editorState.clarity, editorState.denoise, it) }
                                )
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
                        "TEXT" to Icons.Default.FontDownload,
                        "SHAPES" to Icons.Default.CropSquare,
                        "RETOUCH" to Icons.Default.Healing,
                        "DETAIL" to Icons.Default.TouchApp
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
                                 fontSize = 9.sp,
                                 fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                 color = NeoDark
                             )
                         }
                     }
                 }
            }
        }

        // Save / Export dialog: copy vs overwrite, format, quality, EXIF privacy
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = {
                    Text(
                        text = "SAVE / EXPORT",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                },
                text = {
                    Column {
                        Text("FORMAT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("JPEG", "PNG", "WEBP").forEach { fmt ->
                                val isSelected = exportFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                        .border(1.5.dp, NeoBorder, RectangleShape)
                                        .clickable { onSetExportFormat(fmt) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(fmt, fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeoDark)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("QUALITY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(80, 90, 100).forEach { q ->
                                val isSelected = exportQuality == q
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(if (isSelected) NeoYellow else NeoBg, RectangleShape)
                                        .border(1.5.dp, NeoBorder, RectangleShape)
                                        .clickable { onSetExportQuality(q) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("$q%", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeoDark)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("REMOVE EXIF & GPS", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                Text(
                                    text = "Strip location + camera metadata",
                                    fontSize = 10.sp,
                                    color = NeoDark.copy(alpha = 0.7f)
                                )
                            }
                            Switch(
                                checked = stripExif,
                                onCheckedChange = onSetStripExif,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = NeoDark,
                                    checkedTrackColor = NeoYellow,
                                    uncheckedThumbColor = NeoDark,
                                    uncheckedTrackColor = NeoBorder
                                )
                            )
                        }
                    }
                },
                confirmButton = {
                    NeoButton(
                        text = "SAVE COPY",
                        onClick = {
                            showSaveDialog = false
                            onSave(true)
                        },
                        containerColor = NeoMint
                    )
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeoButton(
                            text = "OVERWRITE",
                            onClick = {
                                showSaveDialog = false
                                onSave(false)
                            },
                            containerColor = NeoYellow
                        )
                        NeoButton(
                            text = "CANCEL",
                            onClick = { showSaveDialog = false },
                            containerColor = NeoWhite
                        )
                    }
                },
                containerColor = NeoBg,
                shape = RectangleShape
            )
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
