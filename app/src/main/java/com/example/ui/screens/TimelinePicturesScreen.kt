package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Bitmap
import com.example.data.Album
import com.example.data.MediaItem
import com.example.data.ThumbnailCache
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoIconButton
import com.example.ui.components.pinchGridZoom
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoRed
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow
import androidx.compose.foundation.ExperimentalFoundationApi
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TimelinePicturesScreen(
    mediaList: List<MediaItem>,
    albums: List<Album>,
    gridColumns: Int,
    isSelectionMode: Boolean,
    selectedIds: Set<Long>,
    onColumnsChange: (Int) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    onSelectToggle: (Long) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMoveSelectedToAlbum: (String) -> Unit,
    onCopySelectedToAlbum: (String) -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    isLoading: Boolean = false,
    gridState: LazyGridState = rememberLazyGridState()
) {
    var moveMenuExpanded by remember { mutableStateOf(false) }
    var copyMenuExpanded by remember { mutableStateOf(false) }

    // Group media by date
    val groupedMedia = remember(mediaList) {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val groups = linkedMapOf<String, MutableList<MediaItem>>()
        // Hoist these out of the loop: allocating a SimpleDateFormat/Calendar per item
        // is pure overhead when grouping thousands of photos.
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()
        val monthCache = HashMap<Int, String>()

        mediaList.forEach { item ->
            val diff = now - item.dateTaken
            val key = when {
                diff < dayMs -> "TODAY"
                diff < dayMs * 2 -> "YESTERDAY"
                diff < dayMs * 7 -> "THIS WEEK"
                else -> {
                    calendar.timeInMillis = item.dateTaken
                    val monthKey = calendar.get(Calendar.YEAR) * 12 + calendar.get(Calendar.MONTH)
                    monthCache.getOrPut(monthKey) {
                        monthFormat.format(Date(item.dateTaken)).uppercase()
                    }
                }
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }
        groups
    }

    // Prefetch ~1 screen of thumbnails ahead/behind the viewport so fast flings
    // don't outrun the decoder. Skips anything already in memory; decodes stay
    // bounded by ThumbnailCache's own semaphore.
    val indexById = remember(mediaList) {
        mediaList.withIndex().associate { it.value.id to it.index }
    }
    LaunchedEffect(gridState, mediaList) {
        snapshotFlow { gridState.layoutInfo.visibleItemsInfo.map { it.key } }
            .debounce(150)
            .collect { keys ->
                val positions = keys.filterIsInstance<Long>().mapNotNull(indexById::get)
                if (positions.isEmpty()) return@collect
                val lo = (positions.min() - PREFETCH_AHEAD).coerceAtLeast(0)
                val hi = (positions.max() + PREFETCH_AHEAD).coerceAtMost(mediaList.size - 1)
                for (i in lo..hi) {
                    val item = mediaList[i]
                    if (ThumbnailCache.peek(item.uri) == null) {
                        launch { ThumbnailCache.get(item.uri, item.isVideo, MEDIA_THUMBNAIL_SIZE) }
                    }
                }
            }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (mediaList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = NeoDark,
                                strokeWidth = 4.dp,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "LOADING MEDIA...",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = NeoDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Scanning your gallery",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(NeoYellow, RectangleShape)
                                    .border(2.5.dp, NeoBorder, RectangleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = NeoDark
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "NO MEDIA YET",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Allow media access below to load photos and videos from this device",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("timeline_grid")
                        .pinchGridZoom(gridColumns, onColumnsChange),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 96.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedMedia.forEach { (dateHeader, itemsInGroup) ->
                        // Header Span
                        item(span = { GridItemSpan(gridColumns) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(NeoDark, RectangleShape)
                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = dateHeader,
                                        color = NeoWhite,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(2.dp)
                                        .background(NeoBorder)
                                )
                            }
                        }

                        // Media Items
                        items(itemsInGroup, key = { it.id }, contentType = { if (it.isVideo) "video" else "image" }) { item ->
                            val isSelected = selectedIds.contains(item.id)
                            MediaGridThumbnail(
                                item = item,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    if (isSelectionMode) {
                                        onSelectToggle(item.id)
                                    } else {
                                        onMediaClick(item)
                                    }
                                },
                                onLongClick = {
                                    onMediaLongClick(item)
                                },
                                onToggleFavorite = { onToggleFavorite(item) }
                            )
                        }
                    }
                }
            }
        }

        // Selection Action Bar (Floating bottom banner when items are selected)
        AnimatedVisibility(
            visible = isSelectionMode,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = NeoDark,
                shape = RectangleShape,
                border = BorderStroke(2.5.dp, NeoBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        NeoIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Cancel",
                            onClick = onClearSelection,
                            backgroundColor = NeoWhite,
                            size = 38.dp,
                            shadowOffset = 0.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${selectedIds.size} SELECTED",
                            color = NeoWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NeoIconButton(
                            icon = Icons.Default.SelectAll,
                            contentDescription = "Select All",
                            onClick = onSelectAll,
                            backgroundColor = NeoCyan,
                            size = 38.dp,
                            shadowOffset = 0.dp
                        )

                        // Copy selected items into a chosen album folder
                        Box {
                            NeoIconButton(
                                icon = Icons.Default.ContentCopy,
                                contentDescription = "Copy to Folder",
                                onClick = { copyMenuExpanded = true },
                                backgroundColor = NeoWhite,
                                size = 38.dp,
                                shadowOffset = 0.dp
                            )
                            DropdownMenu(
                                expanded = copyMenuExpanded,
                                onDismissRequest = { copyMenuExpanded = false }
                            ) {
                                Text(
                                    text = "COPY TO FOLDER",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                albums.forEach { album ->
                                    DropdownMenuItem(
                                        text = { Text(album.name, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            copyMenuExpanded = false
                                            onCopySelectedToAlbum(album.name)
                                        }
                                    )
                                }
                            }
                        }

                        // Move selected items into a chosen album folder
                        Box {
                            NeoIconButton(
                                icon = Icons.Default.DriveFileMove,
                                contentDescription = "Move to Folder",
                                onClick = { moveMenuExpanded = true },
                                backgroundColor = NeoMint,
                                size = 38.dp,
                                shadowOffset = 0.dp
                            )
                            DropdownMenu(
                                expanded = moveMenuExpanded,
                                onDismissRequest = { moveMenuExpanded = false }
                            ) {
                                Text(
                                    text = "MOVE TO FOLDER",
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 12.sp
                                )
                                albums.forEach { album ->
                                    DropdownMenuItem(
                                        text = { Text(album.name, fontWeight = FontWeight.Bold) },
                                        onClick = {
                                            moveMenuExpanded = false
                                            onMoveSelectedToAlbum(album.name)
                                        }
                                    )
                                }
                            }
                        }

                        // Trash
                        NeoIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Trash Selected",
                            onClick = onDeleteSelected,
                            backgroundColor = NeoPink,
                            size = 38.dp,
                            shadowOffset = 0.dp
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MediaGridThumbnail(
    item: MediaItem,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit
) {
    var heartBurst by remember { mutableStateOf(false) }
    var isFav by remember { mutableStateOf(item.isFavorite) }
    val heartScale = remember { Animatable(1f) }
    val heartColor by animateColorAsState(
        targetValue = if (isFav) NeoRed else NeoDark,
        animationSpec = tween(250),
        label = "heart_color"
    )

    LaunchedEffect(item.isFavorite) {
        isFav = item.isFavorite
        if (item.isFavorite) {
            heartBurst = true
            heartScale.snapTo(0.5f)
            heartScale.animateTo(
                targetValue = 1.35f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
            heartScale.animateTo(
                targetValue = 1.0f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
            )
            delay(400)
            heartBurst = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RectangleShape)
            .background(NeoDark)
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) NeoYellow else NeoBorder,
                shape = RectangleShape
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Thumbnail Image
        MediaThumbnail(
            uri = item.uri,
            isVideo = item.isVideo,
            filterName = item.filterName,
            contentDescription = item.title,
            modifier = Modifier.fillMaxSize()
        )

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(NeoDark.copy(alpha = 0.85f), RectangleShape)
                    .border(1.dp, NeoWhite, RectangleShape)
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = NeoYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    val seconds = (item.durationMs / 1000) % 60
                    val minutes = (item.durationMs / 60000)
                    Text(
                        text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds),
                        color = NeoWhite,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Favorite Heart with animation
        if (!isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(NeoDark.copy(alpha = 0.75f), RectangleShape)
                    .clickable { onToggleFavorite(item) }
                    .scale(if (heartBurst) heartScale.value else 1f),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFav) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isFav) "Favorite" else "Favorite Border",
                    tint = heartColor,
                    modifier = Modifier.size(13.dp)
                )
            }
        }

        // Selection Checkmark
        if (isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .background(if (isSelected) NeoYellow else NeoWhite, RectangleShape)
                    .border(2.dp, NeoBorder, RectangleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = NeoDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

const val MEDIA_THUMBNAIL_SIZE = 384

/** Tiles prefetched on each side of the viewport (~1 screen at 3 columns). */
private const val PREFETCH_AHEAD = 12

@Composable
fun MediaThumbnail(
    uri: String,
    isVideo: Boolean,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    filterName: String? = null
) {
    val bitmap by produceState<Bitmap?>(initialValue = ThumbnailCache.peek(uri), key1 = uri) {
        value = ThumbnailCache.get(
            uri = uri,
            isVideo = isVideo,
            size = MEDIA_THUMBNAIL_SIZE
        )
    }

    val colorFilter = remember(filterName) {
        filterName?.let { FilterHelper.getColorFilter(it) }
    }
    val imageBitmap = remember(bitmap) { bitmap?.asImageBitmap() }

    Box(modifier = modifier.background(NeoDark)) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Pulsing placeholder so fast flings shimmer instead of flashing flat tiles.
            val pulse by rememberInfiniteTransition(label = "thumb_placeholder").animateFloat(
                initialValue = 0.45f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "thumb_pulse"
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isVideo) Icons.Outlined.Videocam else Icons.Outlined.PhotoLibrary,
                    contentDescription = null,
                    tint = NeoWhite.copy(alpha = 0.3f + 0.45f * pulse),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}
