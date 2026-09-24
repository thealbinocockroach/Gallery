package com.example.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
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
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
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
import com.example.ui.theme.NeoRed
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

enum class SwipeDirection { NEXT, PREV }

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
    // 3. Swipe left/right to change photo (bounce back at the ends)
    // 4. Swipe down to dismiss (pull down)
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var switchDirection by remember { mutableStateOf(SwipeDirection.NEXT) }
    val maxScale = 6f
    val scope = rememberCoroutineScope()
    // Favorite toggle routes to the DAO; the heart button animates red-fill <-> outline
    // with a bouncy pop, so no floating burst is needed over the media.
    val onLikeClicked: () -> Unit = {
        onToggleFavorite(mediaItem)
    }

    // Reset zoom whenever the displayed photo changes.
    LaunchedEffect(mediaItem.id) {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    // Back first un-zooms, then exits.
    BackHandler {
        if (scale > 1.2f) {
            scale = 1f
            offsetX = 0f
            offsetY = 0f
        } else {
            onClose()
        }
    }

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
                .onSizeChanged { containerSize = it }
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    rotationZ = mediaItem.rotationDegrees
                )
                .pointerInput(Unit) {
                    // Samsung Gallery gestures in ONE handler so they cooperate:
                    //  - single tap        -> toggle controls
                    //  - double tap        -> smooth zoom 1x <-> 2.5x
                    //  - pinch (2 fingers) -> zoom & pan (anchored at the pinch centroid)
                    //  - swipe down        -> close the viewer (zoom out only)
                    //  - swipe left/right  -> next / previous photo (zoom out only)
                    var lastTapTime = 0L
                    var lastTapPos = Offset.Zero

                    fun clampOffsets() {
                        if (scale > 1f) {
                            val maxX = containerSize.width * (scale - 1f) / 2f
                            val maxY = containerSize.height * (scale - 1f) / 2f
                            offsetX = offsetX.coerceIn(-maxX, maxX)
                            offsetY = offsetY.coerceIn(-maxY, maxY)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }

                    // Smoothly animate scale + pan to a target (eased, frame-by-frame).
                    fun animateTransformTo(targetScale: Float, targetOffsetX: Float, targetOffsetY: Float) {
                        scope.launch {
                            val s0 = scale
                            val ox0 = offsetX
                            val oy0 = offsetY
                            val ds = targetScale - s0
                            val dox = targetOffsetX - ox0
                            val doy = targetOffsetY - oy0
                            val durationMs = 220L
                            val startNs = System.nanoTime()
                            while (true) {
                                withFrameMillis { }
                                val elapsed = (System.nanoTime() - startNs) / 1_000_000L
                                if (elapsed >= durationMs) break
                                val t = elapsed.toFloat() / durationMs
                                val eased = 1f - (1f - t).pow(3)
                                scale = s0 + ds * eased
                                offsetX = ox0 + dox * eased
                                offsetY = oy0 + doy * eased
                                clampOffsets()
                            }
                            scale = targetScale
                            offsetX = targetOffsetX
                            offsetY = targetOffsetY
                            clampOffsets()
                        }
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        down.consume()

                        var multiTouch = false
                        var moved = false
                        var dragDownAccum = 0f
                        var swipeDragX = 0f
                        val startPos = down.position
                        val gestureStartTime = down.uptimeMillis

                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }

                            if (pressed.isEmpty()) {
                                val upChange = event.changes.firstOrNull()
                                val upPos = upChange?.position ?: startPos
                                if (!moved && !multiTouch) {
                                    val now = down.uptimeMillis
                                    val withinTime = now - lastTapTime < 300L
                                    val withinDist = (upPos - lastTapPos).getDistance() < 80f
                                    if (withinTime && withinDist) {
                                        // Double tap: like (the Favorite button toggles red <-> outline)
                                        if (!mediaItem.isFavorite) {
                                            onToggleFavorite(mediaItem)
                                        }
                                        if (scale > 1.2f) {
                                            animateTransformTo(1f, 0f, 0f)
                                        } else {
                                            val targetScale = 2.5f
                                            val ratio = if (scale == 0f) 1f else targetScale / scale
                                            val cx = containerSize.width / 2f
                                            val cy = containerSize.height / 2f
                                            var tox = (upPos.x - cx) * (1f - ratio) + offsetX * ratio
                                            var toy = (upPos.y - cy) * (1f - ratio) + offsetY * ratio
                                            val maxX = containerSize.width * (targetScale - 1f) / 2f
                                            val maxY = containerSize.height * (targetScale - 1f) / 2f
                                            tox = tox.coerceIn(-maxX, maxX)
                                            toy = toy.coerceIn(-maxY, maxY)
                                            animateTransformTo(targetScale, tox, toy)
                                        }
                                        lastTapTime = 0L
                                    } else {
                                        // Treat as single tap -> toggle chrome.
                                        showControls = !showControls
                                        lastTapTime = now
                                        lastTapPos = upPos
                                    }
                                    break
                                }

                                // A zoomed-out drag ended: dismiss / next / prev.
                                // Thresholds intentionally high to prevent accidental viewer close
                                // when user is just panning/adjusting. Need deliberate long downward drag.
                                if (!multiTouch && scale <= 1f) {
                                    val h = abs(swipeDragX)
                                    val v = dragDownAccum
                                    val elapsedMs = (upChange?.uptimeMillis ?: gestureStartTime) - gestureStartTime
                                    val fling = elapsedMs > 0L && h > 60f && (h / elapsedMs) > 1.4f
                                    when {
                                        v > 280f && v > h * 1.6f -> onClose()
                                        swipeDragX < 0f && (h > 220f || fling) -> {
                                            switchDirection = SwipeDirection.NEXT
                                            onNext()
                                        }
                                        swipeDragX > 0f && (h > 220f || fling) -> {
                                            switchDirection = SwipeDirection.PREV
                                            onPrevious()
                                        }
                                        else -> {
                                            // Photo never moved, so nothing to bounce back.
                                        }
                                    }
                                }
                                // Snap back with a soft bounce if the pinch left it slightly below 1x.
                                if (scale in 0.01f..0.999f) {
                                    animateTransformTo(1f, 0f, 0f)
                                }
                                break
                            }

                            if (pressed.size >= 2) {
                                if (!multiTouch) {
                                    multiTouch = true
                                    moved = true
                                }
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()
                                val centroid = event.calculateCentroid(useCurrent = false)
                                val oldScale = scale
                                // Slight rubber-band below 1x so pinch-in feels forgiving.
                                val newScale = (scale * zoom).coerceIn(0.85f, maxScale)
                                val ratio = if (oldScale == 0f) 1f else newScale / oldScale
                                val cx = containerSize.width / 2f
                                val cy = containerSize.height / 2f
                                // Keep the pixel under the fingers fixed while zooming.
                                offsetX = (centroid.x - cx) * (1f - ratio) + offsetX * ratio + pan.x
                                offsetY = (centroid.y - cy) * (1f - ratio) + offsetY * ratio + pan.y
                                scale = newScale
                                clampOffsets()
                                event.changes.forEach { it.consume() }
                            } else if (pressed.size == 1 && !multiTouch) {
                                val change = pressed.first()
                                val dx = change.position.x - startPos.x
                                val dy = change.position.y - startPos.y
                                if (abs(dx) > viewConfiguration.touchSlop || abs(dy) > viewConfiguration.touchSlop) {
                                    moved = true
                                }
                                if (moved) {
                                    val delta = change.positionChange()
                                    if (scale > 1f) {
                                        // Zoomed in: single finger pans the image only.
                                        offsetX += delta.x
                                        offsetY += delta.y
                                        clampOffsets()
                                    } else {
                                        // Zoomed out: just measure the drag; the photo never follows.
                                        // Require a long, mostly-vertical drag to dismiss — prevents accidental
                                        // close when user slightly scrolls/pans in the viewer.
                                        swipeDragX += delta.x
                                        if (delta.y > 0f) {
                                            dragDownAccum += delta.y
                                            if (dragDownAccum > 380f && dragDownAccum > abs(swipeDragX) * 1.8f) {
                                                onClose()
                                                break
                                            }
                                        } else if (delta.y < 0f) {
                                            // User reversed direction upward — forgive accumulated downward drag
                                            dragDownAccum = (dragDownAccum + delta.y).coerceAtLeast(0f)
                                        }
                                    }
                                    change.consume()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = mediaItem,
                transitionSpec = {
                    val dir = switchDirection
                    val enter = slideInHorizontally(
                        initialOffsetX = { width -> if (dir == SwipeDirection.NEXT) width else -width }
                    ) + fadeIn()
                    val exit = slideOutHorizontally(
                        targetOffsetX = { width -> if (dir == SwipeDirection.NEXT) -width else width }
                    ) + fadeOut()
                    enter togetherWith exit
                },
                label = "viewer_switch"
            ) { currentItem ->
                val request = remember(currentItem.uri, currentItem.filterName) {
                    ImageRequest.Builder(context)
                        .data(currentItem.uri)
                        .crossfade(false)
                        .build()
                }
                // Missing-file placeholder: stale rows (file deleted outside the app)
                // render an explicit notice instead of a black void. Next sync prunes them.
                var loadFailed by remember(currentItem.id, currentItem.uri) { mutableStateOf(false) }
                // System-thumbnail fallback: if Coil can't decode the full file, fall back
                // to the OS thumbnail path (same decoder the grids use) at screen size.
                var fallbackBitmap by remember(currentItem.id, currentItem.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
                var fallbackDone by remember(currentItem.id, currentItem.uri) { mutableStateOf(false) }
                LaunchedEffect(loadFailed, currentItem.id, currentItem.uri) {
                    if (loadFailed && !fallbackDone) {
                        fallbackBitmap = com.example.data.ThumbnailCache.get(
                            uri = currentItem.uri,
                            isVideo = currentItem.isVideo,
                            size = 1280
                        )
                        fallbackDone = true
                    }
                }
                val fallbackImage = remember(fallbackBitmap) { fallbackBitmap?.asImageBitmap() }
                if (fallbackImage != null) {
                    Image(
                        bitmap = fallbackImage,
                        contentDescription = currentItem.title,
                        contentScale = ContentScale.Fit,
                        colorFilter = FilterHelper.getColorFilter(currentItem.filterName),
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = if (currentItem.flipHorizontal) -1f else 1f,
                                scaleY = if (currentItem.flipVertical) -1f else 1f
                            )
                    )
                }
                AsyncImage(
                    model = request,
                    contentDescription = currentItem.title,
                    contentScale = ContentScale.Fit,
                    colorFilter = FilterHelper.getColorFilter(currentItem.filterName),
                    onError = { loadFailed = true },
                    onSuccess = { loadFailed = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = if (currentItem.flipHorizontal) -1f else 1f,
                            scaleY = if (currentItem.flipVertical) -1f else 1f,
                            alpha = if (fallbackImage != null && loadFailed) 0f else 1f
                        ))
                if (loadFailed && fallbackDone && fallbackImage == null) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "FILE MISSING",
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This photo was deleted outside the gallery",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Liking Heart animation lives on the Favorite button (not over the media).
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
                        text = "${currentIndex + 1} of $totalCount â€¢ ${mediaItem.albumName}",
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

        // Swipe left/right to change photos (bounce back at the ends)
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
                    .background(NeoDark, RectangleShape)
                    .border(2.5.dp, NeoBorder, RectangleShape)
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
                        onClick = { shareMedia(context, mediaItem) },
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

                    // Favorite (heart turns red with bounce pop animation)
                    val favHeartScale = remember { Animatable(1f) }
                    LaunchedEffect(mediaItem.isFavorite) {
                        if (mediaItem.isFavorite) {
                            favHeartScale.snapTo(0.5f)
                            favHeartScale.animateTo(
                                targetValue = 1.35f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                            favHeartScale.animateTo(
                                targetValue = 1.0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                            )
                        } else {
                            favHeartScale.snapTo(1.0f)
                        }
                    }

                    val favColor by animateColorAsState(
                        targetValue = if (mediaItem.isFavorite) NeoRed else NeoDark,
                        animationSpec = tween(250),
                        label = "fav_color"
                    )

                    Box(
                        modifier = Modifier.graphicsLayer(
                            scaleX = favHeartScale.value,
                            scaleY = favHeartScale.value
                        )
                    ) {
                        NeoIconButton(
                            icon = if (mediaItem.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            onClick = onLikeClicked,
                            backgroundColor = NeoWhite,
                            tint = favColor,
                            testTag = "viewer_btn_fav"
                        )
                    }

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

                    val dateStr = SimpleDateFormat("EEEE, MMMM d, yyyy â€¢ HH:mm", Locale.getDefault()).format(Date(mediaItem.dateTaken))
                    val sizeMb = String.format(Locale.getDefault(), "%.2f MB", mediaItem.sizeBytes / (1024f * 1024f))

                    InfoRow(label = "FILENAME", value = mediaItem.title)
                    InfoRow(label = "ALBUM / FOLDER", value = mediaItem.albumName)
                    InfoRow(label = "RESOLUTION", value = "${mediaItem.width} Ã— ${mediaItem.height} px")
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

private fun shareMedia(context: Context, mediaItem: MediaItem) {
    val parsed = Uri.parse(mediaItem.uri)
    val shareUri = if (parsed.scheme == "file") {
        try {
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(parsed.path!!))
        } catch (_: Exception) {
            null
        }
    } else {
        parsed
    }
    if (shareUri == null) {
        Toast.makeText(context, "Could not share this file", Toast.LENGTH_SHORT).show()
        return
    }
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = if (mediaItem.isVideo) "video/*" else "image/*"
        putExtra(Intent.EXTRA_STREAM, shareUri)
        putExtra(Intent.EXTRA_SUBJECT, mediaItem.title)
        putExtra(Intent.EXTRA_TEXT, "Shared from Gallery: ${mediaItem.title}")
        clipData = ClipData.newRawUri(mediaItem.title, shareUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
}
