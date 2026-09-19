package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    mediaItem: MediaItem,
    currentIndex: Int,
    totalCount: Int,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onDeleteToTrash: (Long) -> Unit,
    onEdit: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Samsung Gallery Finger Gestures:
    // 1. Double tap to zoom 1x <-> 2.5x
    // 2. Pinch to zoom & pan
    // 3. Swipe down to dismiss (pull down)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var dragDownDismissY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("media_viewer_screen")
    ) {
        // Image Canvas with Samsung-like gestures
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset { IntOffset(offsetX.roundToInt(), (offsetY + dragDownDismissY).roundToInt()) }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = mediaItem.rotationDegrees
                )
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = {
                            if (scale > 1.2f) {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(scale) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (scale == 1f && pan.y > 15f && pan.x in -20f..20f) {
                            // Swipe down to dismiss gesture!
                            dragDownDismissY += pan.y
                            if (dragDownDismissY > 220f) {
                                onClose()
                            }
                        } else {
                            scale = (scale * zoom).coerceIn(1f, 4.5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                                dragDownDismissY = 0f
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(mediaItem.uri)
                    .crossfade(true)
                    .build(),
                contentDescription = mediaItem.title,
                contentScale = ContentScale.Fit,
                colorFilter = FilterHelper.getColorFilter(mediaItem.filterName),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = if (mediaItem.flipHorizontal) -1f else 1f,
                        scaleY = if (mediaItem.flipVertical) -1f else 1f
                    )
            )
        }

        // Top Controls Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onClose,
                    backgroundColor = NeoWhite,
                    testTag = "viewer_btn_back"
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = mediaItem.title.uppercase(),
                        color = NeoWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                    Text(
                        text = "${currentIndex + 1} of $totalCount • ${mediaItem.albumName}",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                NeoIconButton(
                    icon = Icons.Default.Info,
                    contentDescription = "Info",
                    onClick = { showInfoSheet = true },
                    backgroundColor = NeoYellow,
                    testTag = "viewer_btn_info"
                )
            }
        }

        // Horizontal Nav Arrows (for quick next/prev navigation)
        AnimatedVisibility(
            visible = showControls && totalCount > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            if (currentIndex > 0) {
                Box(modifier = Modifier.padding(start = 12.dp)) {
                    NeoBadge(
                        text = "◀ PREV",
                        backgroundColor = NeoWhite,
                        modifier = Modifier.clickable {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            onPrevious()
                        }
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showControls && totalCount > 1,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            if (currentIndex < totalCount - 1) {
                Box(modifier = Modifier.padding(end = 12.dp)) {
                    NeoBadge(
                        text = "NEXT ▶",
                        backgroundColor = NeoWhite,
                        modifier = Modifier.clickable {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                            onNext()
                        }
                    )
                }
            }
        }

        // Bottom Action Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(NeoDark, RoundedCornerShape(12.dp))
                    .border(2.5.dp, NeoBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Share
                    NeoIconButton(
                        icon = Icons.Default.Share,
                        contentDescription = "Share",
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/*"
                                putExtra(Intent.EXTRA_SUBJECT, mediaItem.title)
                                putExtra(Intent.EXTRA_TEXT, "Shared from Gallery Pro: ${mediaItem.title}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                        },
                        backgroundColor = NeoCyan,
                        testTag = "viewer_btn_share"
                    )

                    // Edit
                    NeoIconButton(
                        icon = Icons.Default.Edit,
                        contentDescription = "Edit Photo",
                        onClick = { onEdit(mediaItem) },
                        backgroundColor = NeoYellow,
                        testTag = "viewer_btn_edit"
                    )

                    // Favorite
                    NeoIconButton(
                        icon = if (mediaItem.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        onClick = { onToggleFavorite(mediaItem) },
                        backgroundColor = if (mediaItem.isFavorite) NeoPink else NeoWhite,
                        tint = if (mediaItem.isFavorite) NeoWhite else NeoDark,
                        testTag = "viewer_btn_fav"
                    )

                    // Delete to Trash
                    NeoIconButton(
                        icon = Icons.Default.Delete,
                        contentDescription = "Delete to Trash",
                        onClick = { onDeleteToTrash(mediaItem.id) },
                        backgroundColor = NeoPink,
                        tint = NeoWhite,
                        testTag = "viewer_btn_delete"
                    )
                }
            }
        }

        // Details / Info Sheet
        if (showInfoSheet) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                containerColor = NeoBg,
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "IMAGE DETAILS",
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = NeoDark
                        )
                        NeoBadge(
                            text = "OFFLINE LOCAL",
                            backgroundColor = NeoMint,
                            textColor = NeoDark
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy • HH:mm", Locale.getDefault()).format(Date(mediaItem.dateTaken))
                    val sizeMb = String.format(Locale.getDefault(), "%.2f MB", mediaItem.sizeBytes / (1024f * 1024f))

                    InfoRow(label = "FILENAME", value = mediaItem.title)
                    InfoRow(label = "ALBUM / FOLDER", value = mediaItem.albumName)
                    InfoRow(label = "RESOLUTION", value = "${mediaItem.width} × ${mediaItem.height} px")
                    InfoRow(label = "FILE SIZE", value = sizeMb)
                    InfoRow(label = "DATE TAKEN", value = dateStr)
                    InfoRow(label = "ACTIVE FILTER", value = mediaItem.filterName)

                    Spacer(modifier = Modifier.height(24.dp))

                    NeoButton(
                        text = "CLOSE DETAILS",
                        onClick = { showInfoSheet = false },
                        containerColor = NeoYellow,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = NeoDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color.LightGray.copy(alpha = 0.5f))
        )
    }
}
