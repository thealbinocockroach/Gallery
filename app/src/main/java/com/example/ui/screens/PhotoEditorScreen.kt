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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.InsertEmoticon
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Tune
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.FontItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoIconButton
import com.example.ui.components.NeoSlider
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
    hasPixelWork: Boolean = false,
    previewRenderer: (suspend (Int) -> android.graphics.Bitmap?)? = null,
    onAddDrawingPath: (DrawingPath) -> Unit,
    onUndoDrawing: () -> Unit,
    onClearDrawing: () -> Unit,
    onTextOverlayChange: (TextOverlay?) -> Unit,
    onInstallCustomFont: (String, String) -> Unit,
    onSaveFull: (asNew: Boolean, composed: android.graphics.Bitmap?) -> Unit,
    fullWorkingRenderer: (suspend () -> android.graphics.Bitmap?)? = null,
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

    // Doodle tool state (strokes live in the embedded library canvas)
    var selectedBrushColor by remember { mutableStateOf(NeoYellow) }
    var brushStrokeWidth by remember { mutableFloatStateOf(8f) }
    var brushAlpha by remember { mutableFloatStateOf(1f) }
    var eraserOn by remember { mutableStateOf(false) }
    // Retouch tap handler is configured by the RETOUCH panel below (mode/radius/strength).
    var retouchTapHandler by remember { mutableStateOf<((Float, Float) -> Unit)>({ _, _ -> }) }

    // Shape tool state
    var selectedShapeColor by remember { mutableStateOf(NeoCyan) }
    var shapeStrokeWidth by remember { mutableFloatStateOf(4f) }
    var shapeFillAlpha by remember { mutableFloatStateOf(0.3f) }
    var shapePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }
    var currentShapeType by remember { mutableStateOf(ShapeType.RECT) }

    // Text tool state (committed to the embedded canvas via ADD/UPDATE TEXT)
    var editingTextString by remember { mutableStateOf("") }
    var selectedFontId by remember { mutableStateOf("font_neo_black") }
    var fontSizeSp by remember { mutableFloatStateOf(26f) }
    var selectedTextColor by remember { mutableStateOf(NeoDark) }
    var selectedBadgeColor by remember { mutableStateOf(NeoYellow) }
    var hasBackgroundBadge by remember { mutableStateOf(true) }
    var selectedTextAlign by remember { mutableStateOf("CENTER") }
    var badgeAlpha by remember { mutableFloatStateOf(1f) }

    // Compare / Filter Strength
    var showCompare by remember { mutableStateOf(editorState.showCompare) }
    // Hold-to-compare: press-and-hold the preview to peek at the original base image.
    var pressComparing by remember { mutableStateOf(false) }
    val isComparing = showCompare || pressComparing
    // ---- Embedded burhanrashid52 PhotoEditor: transparent markup canvas (brush,
    // text, emoji, shapes with native move/scale) layered over our own preview.
    // Our tone/spatial pipeline feeds its source via workingBitmap below.
    var libEditor by remember { mutableStateOf<ja.burhanrashid52.photoeditor.PhotoEditor?>(null) }
    var libView by remember { mutableStateOf<ja.burhanrashid52.photoeditor.PhotoEditorView?>(null) }
    var libCanUndo by remember { mutableStateOf(false) }
    var libCanRedo by remember { mutableStateOf(false) }
    fun syncLibUndo() {
        libCanUndo = libEditor?.isUndoAvailable == true
        libCanRedo = libEditor?.isRedoAvailable == true
    }
    var pendingTextEditView by remember { mutableStateOf<android.view.View?>(null) }
    val scope = rememberCoroutineScope()
    var workingRefreshTick by remember { mutableStateOf(0) }
    // Working bitmap (tone + spatial baked, no geometry/markup) shown through the
    // lib source. Null when pristine — then the lib stays transparent over base.
    var workingBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    androidx.compose.runtime.LaunchedEffect(editorState, hasPixelWork, isComparing, media.uri, workingRefreshTick) {
        if (isComparing || previewRenderer == null || !hasPixelWork) {
            if (!hasPixelWork) {
                workingBitmap = null
                libView?.source?.setImageDrawable(null)
            }
            return@LaunchedEffect
        }
        kotlinx.coroutines.delay(250)
        workingBitmap = previewRenderer(1080)
        val view = libView
        if (view != null) {
            if (workingBitmap != null) {
                view.source.setImageBitmap(workingBitmap)
            } else {
                view.source.setImageDrawable(null)
            }
        }
    }
    // Brush drawing only intercepts touches on drawing tools; elsewhere the canvas
    // must stay transparent to taps (crop/text/retouch gestures live around it).
    // Keep the library brush config in sync with our panel controls.
    fun applyLibBrushConfig() {
        val lib = libEditor ?: return
        if (editorState.activeTool == "DOODLE") {
            if (eraserOn) {
                lib.brushEraser()
            } else {
                lib.setShape(
                    ja.burhanrashid52.photoeditor.shape.ShapeBuilder()
                        .withShapeType(ja.burhanrashid52.photoeditor.shape.ShapeType.Brush)
                        .withShapeSize(brushStrokeWidth)
                        .withShapeOpacity((brushAlpha * 255).toInt().coerceIn(0, 255))
                        .withShapeColor(selectedBrushColor.toArgb())
                )
            }
            lib.setBrushDrawingMode(true)
        } else if (editorState.activeTool == "SHAPES" && currentShapeType != ShapeType.HIGHLIGHT) {
            val libType = when (currentShapeType) {
                ShapeType.CIRCLE -> ja.burhanrashid52.photoeditor.shape.ShapeType.Oval
                ShapeType.ARROW -> ja.burhanrashid52.photoeditor.shape.ShapeType.Arrow()
                else -> ja.burhanrashid52.photoeditor.shape.ShapeType.Rectangle
            }
            lib.setShape(
                ja.burhanrashid52.photoeditor.shape.ShapeBuilder()
                    .withShapeType(libType)
                    .withShapeSize(shapeStrokeWidth)
                    .withShapeOpacity(((0.4f + shapeFillAlpha * 0.6f).coerceIn(0f, 1f) * 255).toInt())
                    .withShapeColor(selectedShapeColor.toArgb())
            )
            lib.setBrushDrawingMode(true)
        } else {
            lib.setBrushDrawingMode(false)
        }
        syncLibUndo()
    }
    androidx.compose.runtime.LaunchedEffect(
        editorState.activeTool, currentShapeType, selectedBrushColor,
        brushStrokeWidth, brushAlpha, eraserOn, selectedShapeColor,
        shapeStrokeWidth, shapeFillAlpha, libEditor
    ) {
        applyLibBrushConfig()
    }
    // Fresh media (open, crop result): wipe lib markup, it belongs to the old pixels.
    androidx.compose.runtime.LaunchedEffect(media.uri) {
        libEditor?.clearAllViews()
        pendingTextEditView = null
        syncLibUndo()
    }

    /** Best-effort mapping from editor font ids to native typefaces for canvas export. */
    fun nativeTypefaceFor(fontId: String): android.graphics.Typeface {
        val family = when (fontId) {
            "font_editorial_serif" -> android.graphics.Typeface.SERIF
            "font_retro_mono" -> android.graphics.Typeface.MONOSPACE
            "font_script_flow", "font_marker_brush" -> android.graphics.Typeface.SANS_SERIF
            else -> android.graphics.Typeface.SANS_SERIF
        }
        val style = when (fontId) {
            "font_neo_black", "font_bebas_impact" -> android.graphics.Typeface.BOLD
            "font_editorial_serif", "font_retro_mono" -> android.graphics.Typeface.BOLD
            "font_script_flow" -> android.graphics.Typeface.ITALIC
            else -> android.graphics.Typeface.BOLD
        }
        return android.graphics.Typeface.create(family, style)
    }

    // Commit the TEXT panel styling to the embedded canvas (add new or update tapped).
    fun commitLibText() {
        val lib = libEditor ?: return
        val text = editingTextString.trim()
        if (text.isEmpty()) return
        val outlineInt = if (selectedTextColor.luminance() > 0.5f) {
            NeoDark.toArgb()
        } else {
            NeoWhite.toArgb()
        }
        val badgeInt = selectedBadgeColor.copy(alpha = badgeAlpha).toArgb()
        val style = ja.burhanrashid52.photoeditor.TextStyleBuilder()
            .apply {
                withTextSize(fontSizeSp)
                withTextColor(selectedTextColor.toArgb())
                withTextFont(nativeTypefaceFor(selectedFontId))
                withGravity(
                    when (selectedTextAlign) {
                        "LEFT" -> android.view.Gravity.START
                        "RIGHT" -> android.view.Gravity.END
                        else -> android.view.Gravity.CENTER
                    }
                )
                withTextShadow(8f, 4f, 4f, 0x99000000.toInt())
                withTextBorder(
                    ja.burhanrashid52.photoeditor.TextBorder(
                        corner = 0f,
                        backGroundColor = android.graphics.Color.TRANSPARENT,
                        strokeWidth = 4,
                        strokeColor = outlineInt
                    )
                )
                if (hasBackgroundBadge) withBackgroundColor(badgeInt)
            }
        val pending = pendingTextEditView
        if (pending != null) {
            lib.editText(pending, text, style)
            pendingTextEditView = null
        } else {
            lib.addText(text, style)
        }
        editingTextString = ""
        syncLibUndo()
    }

    // Capture the lib canvas (working pixels + markup) for export. Runs on Main.
    suspend fun captureComposed(): android.graphics.Bitmap? {
        val lib = libEditor ?: return null
        return try {
            if (lib.isCacheEmpty) null else lib.saveAsBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun doSave(asNew: Boolean, fullWorking: (suspend () -> android.graphics.Bitmap?)?) {
        scope.launch {
            showSaveDialog = false
            var composed: android.graphics.Bitmap? = null
            try {
                val lib = libEditor
                if (lib != null && !lib.isCacheEmpty && fullWorking != null) {
                    // Full-res working pixels under the markup for full-quality export.
                    val full = withContext(kotlinx.coroutines.Dispatchers.IO) { fullWorking() }
                    if (full != null) {
                        libView?.source?.setImageBitmap(full)
                        composed = try {
                            lib.saveAsBitmap()
                        } catch (_: Exception) {
                            null
                        }
                        try { full.recycle() } catch (_: Exception) {}
                        // Restore the downscaled preview working bitmap.
                        workingRefreshTick++
                    }
                }
            } catch (_: Exception) {
                composed = null
            }
            onSaveFull(asNew, composed)
        }
    }
    var filterStrength by remember { mutableFloatStateOf(editorState.filterStrength) }
    var previewBoxSize by remember { mutableStateOf(IntSize.Zero) }

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

        // Top Action Bar, two rows so nothing can overlap on narrow screens:
        // row 1 = navigation + title + undo/redo, row 2 = reset + save actions.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoIconButton(
                icon = Icons.Default.Close,
                contentDescription = "Cancel",
                onClick = onClose,
                backgroundColor = NeoWhite,
                size = 34.dp,
                testTag = "editor_btn_close"
            )

            Text(
                text = "EDIT PHOTO",
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = NeoDark,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )

            NeoIconButton(
                icon = Icons.AutoMirrored.Filled.Undo,
                contentDescription = "Undo",
                onClick = {
                    if (libCanUndo) {
                        libEditor?.undo()
                        syncLibUndo()
                    } else {
                        onUndo()
                    }
                },
                backgroundColor = if (canUndo || libCanUndo) NeoWhite else NeoBg,
                tint = if (canUndo || libCanUndo) NeoDark else Color.LightGray,
                size = 34.dp,
                shadowOffset = 2.dp,
                testTag = "editor_btn_undo"
            )
            NeoIconButton(
                icon = Icons.AutoMirrored.Filled.Redo,
                contentDescription = "Redo",
                onClick = {
                    if (libCanRedo) {
                        libEditor?.redo()
                        syncLibUndo()
                    } else {
                        onRedo()
                    }
                },
                backgroundColor = if (canRedo || libCanRedo) NeoWhite else NeoBg,
                tint = if (canRedo || libCanRedo) NeoDark else Color.LightGray,
                size = 34.dp,
                shadowOffset = 2.dp,
                testTag = "editor_btn_redo"
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            NeoButton(
                text = "RESET ALL",
                onClick = onResetAllEditor,
                containerColor = NeoPink,
                contentColor = NeoWhite,
                modifier = Modifier.weight(1f),
                testTag = "editor_btn_reset_all"
            )
            NeoButton(
                text = "SAVE",
                onClick = { showSaveDialog = true },
                containerColor = NeoMint,
                leadingIcon = Icons.Default.Save,
                modifier = Modifier.weight(1f),
                testTag = "editor_btn_save"
            )
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
                    .pointerInput(editorState.activeTool) {
                        // Hold-to-compare only on non-drawing tools — on DOODLE/SHAPES/
                        // TEXT/RETOUCH/CROP every touch is a gesture, never a "hold".
                        if (editorState.activeTool in listOf("FILTERS", "ADJUST", "DETAIL")) {
                            detectTapGestures(
                                onPress = {
                                    pressComparing = true
                                    tryAwaitRelease()
                                    pressComparing = false
                                }
                            )
                        }
                    }
                    .graphicsLayer(
                        rotationZ = editorState.rotation + editorState.levelAngle,
                        scaleX = if (editorState.flipH) -1f else 1f,
                        scaleY = if (editorState.flipV) -1f else 1f
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Base photo (plain, always underneath). Tone + spatial come through
                // the working bitmap fed into the library canvas below. If Coil fails,
                // fall back to the OS thumbnail path instead of black.
                var baseLoadFailed by remember(media.uri) { mutableStateOf(false) }
                var baseFallback by remember(media.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                androidx.compose.runtime.LaunchedEffect(baseLoadFailed, media.uri) {
                    if (baseLoadFailed && baseFallback == null) {
                        baseFallback = com.example.data.ThumbnailCache.get(
                            uri = media.uri,
                            isVideo = media.isVideo,
                            size = 1280
                        )
                    }
                }
                val baseFallbackImage = remember(baseFallback) { baseFallback?.asImageBitmap() }
                AsyncImage(
                    model = editorImageRequest,
                    contentDescription = media.title,
                    contentScale = ContentScale.Fit,
                    onError = { baseLoadFailed = true },
                    onSuccess = { baseLoadFailed = false },
                    modifier = Modifier.fillMaxSize(),
                    alpha = if (baseFallbackImage != null && baseLoadFailed) 0f else 1f
                )
                if (baseFallbackImage != null && baseLoadFailed) {
                    Image(
                        bitmap = baseFallbackImage,
                        contentDescription = media.title,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Embedded library markup canvas (brush / text / emoji / shapes with
                // native move + pinch-scale). Transparent: our photo shows through.
                // Hidden while comparing so the raw original shows.
                if (!isComparing) {
                    androidx.compose.ui.viewinterop.AndroidView(
                        factory = { ctx ->
                            ja.burhanrashid52.photoeditor.PhotoEditorView(ctx).apply {
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                // Match our Fit mapping so overlays align with the photo.
                                source.scaleType =
                                    android.widget.ImageView.ScaleType.FIT_CENTER
                            }
                        },
                        update = { view ->
                            libView = view
                            if (libEditor == null) {
                                libEditor = ja.burhanrashid52.photoeditor.PhotoEditor
                                    .Builder(context, view)
                                    .setPinchTextScalable(true)
                                    .build()
                                libEditor?.setOnPhotoEditorListener(
                                    object : ja.burhanrashid52.photoeditor.OnPhotoEditorListener {
                                        override fun onEditTextChangeListener(
                                            rootView: android.view.View,
                                            text: String,
                                            colorCode: Int
                                        ) {
                                            editingTextString = text
                                            pendingTextEditView = rootView
                                        }

                                        override fun onAddViewListener(
                                            viewType: ja.burhanrashid52.photoeditor.ViewType,
                                            numberOfAddedViews: Int
                                        ) {
                                            syncLibUndo()
                                        }

                                        override fun onRemoveViewListener(
                                            viewType: ja.burhanrashid52.photoeditor.ViewType,
                                            numberOfAddedViews: Int
                                        ) {
                                            syncLibUndo()
                                        }

                                        override fun onStartViewChangeListener(
                                            viewType: ja.burhanrashid52.photoeditor.ViewType
                                        ) {
                                        }

                                        override fun onStopViewChangeListener(
                                            viewType: ja.burhanrashid52.photoeditor.ViewType
                                        ) {
                                        }

                                        override fun onTouchSourceImage(event: android.view.MotionEvent) {
                                        }
                                    }
                                )
                                syncLibUndo()
                            }
                        },
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

                // Highlight-only overlay Canvas (brush / rect / oval / line / arrow live
                // in the embedded library canvas). Hidden while comparing.
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(
                            editorState.activeTool, currentShapeType, retouchTapHandler
                        ) {
                            if (editorState.activeTool == "SHAPES" && currentShapeType == ShapeType.HIGHLIGHT) {
                                // Drag defines the highlight bounds; DRAW SHAPE commits it.
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
                    // Committed HIGHLIGHT shapes only — rect / oval / line / arrow live
                    // in the embedded library canvas with native move + scale.
                    editorState.shapes.forEach { shape ->
                        if (shape.type != ShapeType.HIGHLIGHT) return@forEach
                        if (shape.points.size < 2) return@forEach
                        val a = shape.points.first()
                        val b = shape.points.last()
                        val left = minOf(a.first, b.first)
                        val top = minOf(a.second, b.second)
                        val right = maxOf(a.first, b.first)
                        val bottom = maxOf(a.second, b.second)
                        val base = Color(shape.color.toULong())
                        drawRect(
                            color = base.copy(alpha = (0.35f + shape.fillAlpha * 0.4f).coerceIn(0f, 0.75f)),
                            topLeft = Offset(left, top),
                            size = Size(right - left, bottom - top)
                        )
                    }

                    // In-progress highlight drag preview
                    if (editorState.activeTool == "SHAPES" && currentShapeType == ShapeType.HIGHLIGHT && shapePoints.size >= 2) {
                        val a = shapePoints.first()
                        val b = shapePoints.last()
                        drawRect(
                            color = selectedShapeColor.copy(alpha = 0.5f),
                            topLeft = Offset(minOf(a.first, b.first), minOf(a.second, b.second)),
                            size = Size(abs(b.first - a.first), abs(b.second - a.second)),
                            style = Stroke(width = 3f)
                        )
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
                        .height(if (editorState.activeTool in listOf("TEXT", "SHAPES", "DETAIL", "RETOUCH", "CROP", "DOODLE")) 208.dp else 115.dp)
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
                                NeoSlider(
                                    value = filterStrength,
                                    onValueChange = { filterStrength = it; onFilterStrengthChange(it) },
                                    valueRange = 0f..1f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                        NeoSlider(
                                            value = editorState.levelAngle,
                                            onValueChange = onLevelAngleChange,
                                            valueRange = -45f..45f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        )
                                        Text("${editorState.levelAngle.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                    }

                                    // Keystone correction sliders (baked on export)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("PERSP-H", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        NeoSlider(
                                            value = editorState.perspectiveHorizontal,
                                            onValueChange = onPerspectiveHChange,
                                            valueRange = -45f..45f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        )
                                        Text("${editorState.perspectiveHorizontal.toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(40.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("PERSP-V", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        NeoSlider(
                                            value = editorState.perspectiveVertical,
                                            onValueChange = onPerspectiveVChange,
                                            valueRange = -45f..45f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                        NeoSlider(
                                            value = editorState.hsl[hslBase],
                                            onValueChange = { onHslChange(hslChannel, it, editorState.hsl[hslBase + 1], editorState.hsl[hslBase + 2]) },
                                            valueRange = -180f..180f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        )
                                        Text("${editorState.hsl[hslBase].toInt()}°", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("SAT", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        NeoSlider(
                                            value = editorState.hsl[hslBase + 1],
                                            onValueChange = { onHslChange(hslChannel, editorState.hsl[hslBase], it, editorState.hsl[hslBase + 2]) },
                                            valueRange = -100f..100f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        )
                                        Text("${editorState.hsl[hslBase + 1].toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(44.dp))
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("LUM", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                        NeoSlider(
                                            value = editorState.hsl[hslBase + 2],
                                            onValueChange = { onHslChange(hslChannel, editorState.hsl[hslBase], editorState.hsl[hslBase + 1], it) },
                                            valueRange = -100f..100f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                        NeoSlider(
                                            value = filterStrength,
                                            onValueChange = { filterStrength = it; onFilterStrengthChange(it) },
                                            valueRange = 0f..1f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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

                                    // Eraser toggle (library eraser paints transparency)
                                    NeoButton(
                                        text = if (eraserOn) "ERASER ON" else "ERASER",
                                        onClick = { eraserOn = !eraserOn },
                                        containerColor = if (eraserOn) NeoYellow else NeoWhite,
                                        modifier = Modifier.height(40.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("STROKE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    NeoSlider(
                                        value = brushStrokeWidth,
                                        onValueChange = { brushStrokeWidth = it },
                                        valueRange = 3f..24f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Brush opacity
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("OPACITY", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    NeoSlider(
                                        value = brushAlpha,
                                        onValueChange = { brushAlpha = it },
                                        valueRange = 0.1f..1f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                    NeoSlider(
                                        value = brushHue,
                                        onValueChange = {
                                            brushHue = it
                                            selectedBrushColor = hsvToPickerColor(it)
                                            brushHex = ""
                                            brushHexError = false
                                        },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                        NeoSlider(
                                            value = retouchRadius,
                                            onValueChange = { retouchRadius = it },
                                            valueRange = 12f..120f,
                                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                        )
                                        Text("${retouchRadius.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black, modifier = Modifier.width(32.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("STRENGTH", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    NeoSlider(
                                        value = retouchStrength,
                                        onValueChange = { retouchStrength = it },
                                        valueRange = 0f..1f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                    NeoSlider(
                                        value = fontSizeSp,
                                        onValueChange = { fontSizeSp = it },
                                        valueRange = 14f..72f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                    NeoSlider(
                                        value = hueDeg,
                                        onValueChange = {
                                            hueDeg = it
                                            selectedTextColor = hsvToPickerColor(it)
                                            hexInput = ""
                                            hexError = false
                                        },
                                        valueRange = 0f..360f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
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
                                    NeoSlider(
                                        value = badgeAlpha,
                                        onValueChange = { badgeAlpha = it },
                                        valueRange = 0f..1f,
                                        enabled = hasBackgroundBadge,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    )
                                    Text(
                                        text = "${(badgeAlpha * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        modifier = Modifier.width(36.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                // Commit to the embedded canvas (add new, or update tapped text).
                                // The caption then moves + scales natively with pinch gestures.
                                NeoButton(
                                    text = if (pendingTextEditView != null) "UPDATE TEXT" else "ADD TEXT",
                                    onClick = { commitLibText() },
                                    containerColor = NeoYellow,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        "EMOJI" -> {
                            val emojis = remember {
                                listOf(
                                    "😀", "😎", "😍", "🤣", "😮", "😭", "😡", "🥳",
                                    "❤️", "🔥", "⭐", "🎉", "👍", "👏", "🙏", "💯",
                                    "🐱", "🐶", "🌹", "⚽", "🚀", "💡", "🎵", "✨"
                                )
                            }
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = "TAP TO STAMP — DRAG TO MOVE, PINCH TO SCALE",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeoDark.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(6),
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(emojis) { emoji ->
                                        Box(
                                            modifier = Modifier
                                                .aspectRatio(1f)
                                                .background(NeoWhite, RectangleShape)
                                                .border(1.5.dp, NeoBorder, RectangleShape)
                                                .clickable {
                                                    libEditor?.addEmoji(emoji)
                                                    syncLibUndo()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = emoji, fontSize = 22.sp)
                                        }
                                    }
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
                                    NeoSlider(
                                        value = shapeStrokeWidth,
                                        onValueChange = { shapeStrokeWidth = it },
                                        valueRange = 2f..12f,
                                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                                    )
                                    Text("${shapeStrokeWidth.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                // Rect / oval / arrow draw directly on the canvas with native
                                // move + scale. Highlight commits through our own overlay.
                                if (currentShapeType == ShapeType.HIGHLIGHT) {
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
                                } else {
                                    Text(
                                        text = "DRAW DIRECTLY ON THE PHOTO — PINCH TO MOVE / SCALE",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeoDark.copy(alpha = 0.7f)
                                    )
                                }
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
                        "FILTERS" to Icons.Default.PhotoFilter,
                        "CROP" to Icons.Default.Crop,
                        "ADJUST" to Icons.Default.Tune,
                        "DOODLE" to Icons.Default.Brush,
                        "TEXT" to Icons.Default.FontDownload,
                        "EMOJI" to Icons.Default.InsertEmoticon,
                        "SHAPES" to Icons.Default.CropSquare,
                        "RETOUCH" to Icons.Default.Healing,
                        "DETAIL" to Icons.Default.AutoFixHigh
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
                        onClick = { doSave(true, fullWorkingRenderer) },
                        containerColor = NeoMint
                    )
                },
                dismissButton = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeoButton(
                            text = "OVERWRITE",
                            onClick = { doSave(false, fullWorkingRenderer) },
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
        NeoSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = -100f..100f,
            modifier = Modifier.weight(1f)
        )
    }
}
