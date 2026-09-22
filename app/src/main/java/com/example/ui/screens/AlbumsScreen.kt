package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Album
import com.example.data.MediaItem
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.pinchGridZoom
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow

@Composable
fun AlbumsScreen(
    albums: List<Album>,
    mediaList: List<MediaItem>,
    gridColumns: Int,
    onColumnsChange: (Int) -> Unit,
    onAlbumClick: (String) -> Unit,
    onCreateAlbum: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${albums.size} ${if (albums.size == 1) "ALBUM" else "ALBUMS"}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = NeoDark
                )

                NeoButton(
                    text = "NEW ALBUM",
                    onClick = { showCreateDialog = true },
                    containerColor = NeoMint,
                    leadingIcon = Icons.Default.Add,
                    testTag = "btn_new_folder"
                )
            }

            // Group once for all album tiles — avoids re-filtering mediaList per album on every recomposition.
            val albumMediaMap = remember(mediaList) {
                mediaList.groupBy { it.albumName }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("albums_grid")
                    .pinchGridZoom(gridColumns, onColumnsChange),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    val albumItems = albumMediaMap[album.name] ?: emptyList()
                    // Latest image in album is the cover — not the stored coverUri which may be stale.
                    val latestItem = albumItems.maxByOrNull { it.dateTaken }
                    val coverUri = latestItem?.uri ?: album.coverUri.takeIf { it.isNotBlank() }
                    val isVideoCover = latestItem?.isVideo ?: false
                    AlbumCard(
                        album = album,
                        coverUri = coverUri,
                        isVideoCover = isVideoCover,
                        photoCount = albumItems.size,
                        onClick = { onAlbumClick(album.name) }
                    )
                }
            }
        }

        if (showCreateDialog) {
            AlertDialog(
                onDismissRequest = { showCreateDialog = false },
                title = {
                    Text(
                        text = "CREATE NEW ALBUM",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter a name for your local organization folder:",
                            fontSize = 14.sp,
                            color = NeoDark
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = newAlbumName,
                            onValueChange = { newAlbumName = it },
                            placeholder = { Text("e.g. Travel, Family, Artwork") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_new_album_name"),
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
                        text = "CREATE",
                        onClick = {
                            if (newAlbumName.isNotBlank()) {
                                onCreateAlbum(newAlbumName)
                                newAlbumName = ""
                                showCreateDialog = false
                            }
                        },
                        containerColor = NeoYellow,
                        testTag = "btn_confirm_album_create"
                    )
                },
                dismissButton = {
                    NeoButton(
                        text = "CANCEL",
                        onClick = { showCreateDialog = false },
                        containerColor = NeoWhite
                    )
                },
                containerColor = NeoBg,
                shape = RectangleShape
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    coverUri: String? = null,
    isVideoCover: Boolean = false,
    photoCount: Int = 0,
    onClick: () -> Unit
) {
    NeoCard(
        onClick = onClick,
        backgroundColor = NeoWhite,
        shadowOffset = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Thumbnail cover
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(NeoDark)
                    .clip(RectangleShape)
            ) {
                if (coverUri != null) {
                    MediaThumbnail(
                        uri = coverUri,
                        isVideo = isVideoCover,
                        contentDescription = album.name,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = NeoWhite.copy(alpha = 0.6f),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(48.dp)
                    )
                }
                // Video badge on cover
                if (isVideoCover && coverUri != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                            .background(NeoDark.copy(alpha = 0.85f), RectangleShape)
                            .border(1.dp, NeoWhite, RectangleShape)
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = NeoYellow,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "VIDEO",
                                color = NeoWhite,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            // Footer with album icon + name + count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NeoWhite)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(NeoYellow, RectangleShape)
                        .border(1.5.dp, NeoBorder, RectangleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = null,
                        tint = NeoDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = album.name.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = NeoDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (photoCount > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$photoCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeoDark.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
