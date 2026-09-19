package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import com.example.viewmodel.DrawingPath
import com.example.viewmodel.EditorState
import com.example.viewmodel.TextOverlay

@Composable
fun PhotoEditorScreen(
    editorState: EditorState,
    fontsList: List<FontItem>,
    onToolChange: (String) -> Unit,
    onFilterChange: (String) -> Unit,
    onRotate: () -> Unit,
    onFlipH: () -> Unit,
    onFlipV: () -> Unit,
    onCropRatioChange: (String) -> Unit,
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

    var showInstallFontDialog by remember { mutableStateOf(false) }
    var newFontName by remember { mutableStateOf("") }
    var newFontCategory by remember { mutableStateOf("Custom Installed") }

    // Doodle tool state
    var selectedBrushColor by remember { mutableStateOf(NeoYellow) }
    var brushStrokeWidth by remember { mutableFloatStateOf(8f) }
    var currentDoodlePoints by remember { mutableStateOf<List<Pair<Float, Float>>>(emptyList()) }

    // Text tool state
    var editingTextString by remember { mutableStateOf(editorState.textOverlay?.text ?: "") }
    var selectedFontId by remember { mutableStateOf(editorState.textOverlay?.fontId ?: "font_neo_black") }
    var fontSizeSp by remember { mutableFloatStateOf(editorState.textOverlay?.fontSizeSp ?: 26f) }
    var selectedTextColor by remember { mutableStateOf(NeoDark) }
    var selectedBadgeColor by remember { mutableStateOf(NeoYellow) }
    var hasBackgroundBadge by remember { mutableStateOf(true) }

    val cropAspectRatio = when (editorState.cropRatio) {
        "1:1" -> 1f
        "4:3" -> 4f / 3f
        "16:9" -> 16f / 9f
        else -> null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("photo_editor_screen")
    ) {
        // Top Action Bar: Close, Title, Undo, Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    text = "STUDIO PRO",
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
                .clip(RoundedCornerShape(12.dp))
                .background(NeoDark)
                .border(2.5.dp, NeoBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .then(
                        if (cropAspectRatio != null) Modifier.aspectRatio(cropAspectRatio)
                        else Modifier.fillMaxSize()
                    )
                    .graphicsLayer(
                        rotationZ = editorState.rotation,
                        scaleX = if (editorState.flipH) -1f else 1f,
                        scaleY = if (editorState.flipV) -1f else 1f
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Main Photo with Color Filter
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(media.uri)
                        .crossfade(true)
                        .build(),
                    contentDescription = media.title,
                    contentScale = if (cropAspectRatio != null) ContentScale.Crop else ContentScale.Fit,
                    colorFilter = FilterHelper.getColorFilter(editorState.selectedFilter),
                    modifier = Modifier.fillMaxSize()
                )

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

                // Text Overlay Preview
                if (editingTextString.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .then(
                                if (hasBackgroundBadge) {
                                    Modifier
                                        .background(selectedBadgeColor, RoundedCornerShape(6.dp))
                                        .border(2.dp, NeoBorder, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                } else {
                                    Modifier.padding(8.dp)
                                }
                            )
                    ) {
                        Text(
                            text = editingTextString,
                            color = selectedTextColor,
                            fontSize = fontSizeSp.sp,
                            fontFamily = FontHelper.getFontFamilyForId(selectedFontId),
                            fontWeight = FontHelper.getFontWeightForId(selectedFontId),
                            fontStyle = FontHelper.getFontStyleForId(selectedFontId)
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
                // Secondary Controls Row based on active tool
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
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
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) NeoYellow else NeoBg)
                                            .border(2.dp, NeoBorder, RoundedCornerShape(8.dp))
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Rotate 90
                                NeoButton(
                                    text = "ROTATE 90°",
                                    onClick = onRotate,
                                    containerColor = NeoYellow,
                                    leadingIcon = Icons.Default.RotateRight
                                )

                                // Flip H
                                NeoIconButton(
                                    icon = Icons.Default.Flip,
                                    contentDescription = "Flip Horizontal",
                                    onClick = onFlipH,
                                    backgroundColor = NeoWhite
                                )

                                // Aspect Ratios
                                listOf("Original", "1:1", "4:3", "16:9").forEach { ratio ->
                                    val isSelected = editorState.cropRatio == ratio
                                    Box(
                                        modifier = Modifier
                                            .background(if (isSelected) NeoMint else NeoBg, RoundedCornerShape(6.dp))
                                            .border(1.5.dp, NeoBorder, RoundedCornerShape(6.dp))
                                            .clickable { onCropRatioChange(ratio) }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = ratio,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 12.sp,
                                            color = NeoDark
                                        )
                                    }
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
                                        text = "BRIGHTNESS: ${editorState.brightness.toInt()}",
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
                                Slider(
                                    value = editorState.brightness,
                                    onValueChange = {
                                        onAdjustmentsChange(it, editorState.contrast, editorState.saturation, editorState.warmth)
                                    },
                                    valueRange = -100f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = NeoYellow,
                                        activeTrackColor = NeoDark
                                    )
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
                                                    .background(color, CircleShape)
                                                    .border(
                                                        width = if (selectedBrushColor == color) 3.dp else 1.5.dp,
                                                        color = if (selectedBrushColor == color) NeoDark else NeoBorder,
                                                        shape = CircleShape
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
                            Column(modifier = Modifier.fillMaxWidth()) {
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
                                                .background(if (isSelected) NeoYellow else NeoBg, RoundedCornerShape(6.dp))
                                                .border(1.5.dp, NeoBorder, RoundedCornerShape(6.dp))
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
                                    .background(if (isSelected) NeoYellow else NeoBg, RoundedCornerShape(8.dp))
                                    .border(1.5.dp, NeoBorder, RoundedCornerShape(8.dp)),
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
                            text = "Add any custom .TTF/.OTF font into your local Gallery Pro studio library:",
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
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
