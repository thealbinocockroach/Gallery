package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MediaItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow

@Composable
fun AlbumDetailScreen(
    albumName: String,
    mediaList: List<MediaItem>,
    gridColumns: Int,
    onColumnsChange: (Int) -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onAddMediaClick: () -> Unit,
    onClose: () -> Unit
) {
    val itemsInAlbum = mediaList.filter { it.albumName.equals(albumName, ignoreCase = true) }

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

            NeoButton(
                text = "+ ADD",
                onClick = onAddMediaClick,
                containerColor = NeoYellow,
                leadingIcon = Icons.Default.AddPhotoAlternate
            )
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
                            .background(NeoCyan, RoundedCornerShape(12.dp))
                            .border(2.5.dp, NeoBorder, RoundedCornerShape(12.dp)),
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
                        text = "Move items here or tap '+ ADD' to import media",
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
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(itemsInAlbum, key = { it.id }) { item ->
                    MediaGridThumbnail(
                        item = item,
                        isSelectionMode = false,
                        isSelected = false,
                        onClick = { onMediaClick(item) },
                        onLongClick = {}
                    )
                }
            }
        }
    }
}
