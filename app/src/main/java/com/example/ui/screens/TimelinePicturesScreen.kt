package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
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
import com.example.data.Album
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
import androidx.compose.foundation.ExperimentalFoundationApi
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    onMoveSelectedToAlbum: (String) -> Unit
) {
    var moveMenuExpanded by remember { mutableStateOf(false) }

    // Pinch-to-zoom gesture state (Samsung Gallery finger gesture)
    var cumulativeZoom by remember { mutableFloatStateOf(1f) }

    // Group media by date
    val groupedMedia = remember(mediaList) {
        val now = System.currentTimeMillis()
        val dayMs = 86400000L
        val groups = linkedMapOf<String, MutableList<MediaItem>>()

        mediaList.forEach { item ->
            val diff = now - item.dateTaken
            val key = when {
                diff < dayMs -> "TODAY"
                diff < dayMs * 2 -> "YESTERDAY"
                diff < dayMs * 7 -> "THIS WEEK"
                else -> {
                    val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    sdf.format(Date(item.dateTaken)).uppercase()
                }
            }
            groups.getOrPut(key) { mutableListOf() }.add(item)
        }
        groups
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .pointerInput(gridColumns) {
                detectTransformGestures { _, _, zoom, _ ->
                    cumulativeZoom *= zoom
                    if (cumulativeZoom > 1.35f) {
                        // Pinch out -> decrease columns (bigger photos)
                        if (gridColumns > 2) {
                            onColumnsChange(gridColumns - 1)
                        }
                        cumulativeZoom = 1f
                    } else if (cumulativeZoom < 0.75f) {
                        // Pinch in -> increase columns (smaller photos)
                        if (gridColumns < 5) {
                            onColumnsChange(gridColumns + 1)
                        }
                        cumulativeZoom = 1f
                    }
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Controls header: items count & grid zoom buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${mediaList.size} ITEMS",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = NeoDark
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    NeoBadge(
                        text = "PINCH TO ZOOM",
                        backgroundColor = NeoMint,
                        textColor = NeoDark
                    )
                }

                // Quick column count selector pills
                Row(
                    modifier = Modifier
                        .background(NeoWhite, RoundedCornerShape(6.dp))
                        .border(1.5.dp, NeoBorder, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(2, 3, 4, 5).forEach { cols ->
                        val isSelected = cols == gridColumns
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) NeoYellow else Color.Transparent)
                                .combinedClickable(
                                    onClick = { onColumnsChange(cols) }
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${cols}X",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoDark
                            )
                        }
                    }
                }
            }

            if (mediaList.isEmpty()) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .background(NeoYellow, RoundedCornerShape(12.dp))
                                .border(2.5.dp, NeoBorder, RoundedCornerShape(12.dp)),
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
                            text = "NO PHOTOS YET",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap '+' to import photos or take shots locally",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("timeline_grid"),
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
                                        .background(NeoDark, RoundedCornerShape(4.dp))
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
                        items(itemsInGroup, key = { it.id }) { item ->
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
                                }
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
                shape = RoundedCornerShape(12.dp),
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

                        // Move to album
                        Box {
                            NeoIconButton(
                                icon = Icons.Default.DriveFileMove,
                                contentDescription = "Move to Album",
                                onClick = { moveMenuExpanded = true },
                                backgroundColor = NeoYellow,
                                size = 38.dp,
                                shadowOffset = 0.dp
                            )
                            DropdownMenu(
                                expanded = moveMenuExpanded,
                                onDismissRequest = { moveMenuExpanded = false }
                            ) {
                                Text(
                                    text = "MOVE TO ALBUM",
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
    onLongClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(NeoDark)
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) NeoYellow else NeoBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Thumbnail Image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(item.uri)
                .crossfade(true)
                .build(),
            contentDescription = item.title,
            contentScale = ContentScale.Crop,
            colorFilter = FilterHelper.getColorFilter(item.filterName),
            modifier = Modifier.fillMaxSize()
        )

        // Video Badge
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(NeoDark.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .border(1.dp, NeoWhite, RoundedCornerShape(4.dp))
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

        // Favorite Heart
        if (item.isFavorite && !isSelectionMode) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(NeoDark.copy(alpha = 0.75f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Favorite",
                    tint = NeoPink,
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
                    .background(if (isSelected) NeoYellow else NeoWhite, CircleShape)
                    .border(2.dp, NeoBorder, CircleShape),
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
