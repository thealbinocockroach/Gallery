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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.Album
import com.example.data.MediaItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
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
    onAlbumClick: (String) -> Unit,
    onCreateAlbum: (String) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var newAlbumName by remember { mutableStateOf("") }

    // Count items per album
    val albumCounts = remember(mediaList) {
        mediaList.groupBy { it.albumName }.mapValues { it.value.size }
    }

    // Cover image per album
    val albumCovers = remember(mediaList, albums) {
        albums.associate { album ->
            val firstItem = mediaList.firstOrNull { it.albumName == album.name }
            album.name to (firstItem?.uri ?: album.coverUri)
        }
    }

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
                    text = "${albums.size} FOLDERS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = NeoDark
                )

                NeoButton(
                    text = "NEW FOLDER",
                    onClick = { showCreateDialog = true },
                    containerColor = NeoMint,
                    leadingIcon = Icons.Default.CreateNewFolder,
                    testTag = "btn_new_folder"
                )
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("albums_grid"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(albums, key = { it.id }) { album ->
                    val count = albumCounts[album.name] ?: 0
                    val cover = albumCovers[album.name] ?: ""

                    AlbumCard(
                        album = album,
                        itemCount = count,
                        coverUri = cover,
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
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

@Composable
fun AlbumCard(
    album: Album,
    itemCount: Int,
    coverUri: String,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    NeoCard(
        onClick = onClick,
        backgroundColor = NeoWhite,
        shadowOffset = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cover Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(NeoDark)
            ) {
                if (coverUri.isNotEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(coverUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = album.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = NeoYellow,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }

                // Item count badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    NeoBadge(
                        text = "$itemCount ITEMS",
                        backgroundColor = NeoYellow,
                        textColor = NeoDark
                    )
                }
            }

            // Album Info Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(NeoMint, RoundedCornerShape(6.dp))
                        .border(1.5.dp, NeoBorder, RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = null,
                        tint = NeoDark,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = album.name.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = NeoDark,
                        maxLines = 1
                    )
                    Text(
                        text = if (album.isSystem) "System Album" else "User Folder",
                        fontSize = 11.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
