package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Album
import com.example.data.MediaItem
import com.example.ui.components.NeoIconButton
import com.example.ui.components.pinchGridZoom
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite

@Composable
fun AlbumDetailScreen(
    albumName: String,
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
    onClose: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit
) {
    val itemsInAlbum = mediaList.filter { it.albumName.equals(albumName, ignoreCase = true) }

    // Back clears selection first, otherwise leaves the album.
    BackHandler {
        if (isSelectionMode) {
            onClearSelection()
        } else {
            onClose()
        }
    }

    var moveMenuExpanded by remember { mutableStateOf(false) }
    var copyMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("album_detail_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NeoIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = onClose,
                    backgroundColor = NeoWhite,
                    testTag = "album_detail_btn_back"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = albumName.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                    Text(
                        text = "${itemsInAlbum.size} items in folder",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (itemsInAlbum.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeoCyan, RectangleShape)
                            .border(2.5.dp, NeoBorder, RectangleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = NeoDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "FOLDER IS EMPTY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Copy or move media into this folder from the MEDIA tab",
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
                    .padding(horizontal = 12.dp)
                    .pinchGridZoom(gridColumns, onColumnsChange),
                contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(itemsInAlbum, key = { it.id }) { item ->
                    MediaGridThumbnail(
                        item = item,
                        isSelectionMode = isSelectionMode,
                        isSelected = selectedIds.contains(item.id),
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
                        onToggleFavorite = { onToggleFavorite(it) }
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
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