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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.MediaItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoButton
import com.example.ui.components.NeoCard
import com.example.ui.components.NeoIconButton
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow

@Composable
fun TrashScreen(
    trashItems: List<MediaItem>,
    onRestore: (Long) -> Unit,
    onDeletePermanently: (Long) -> Unit,
    onEmptyTrash: () -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    var selectedItemForAction by remember { mutableStateOf<MediaItem?>(null) }
    var showEmptyConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("trash_screen")
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
                    testTag = "trash_btn_back"
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "TRASH BIN",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                    Text(
                        text = "${trashItems.size} items in recycle bin",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (trashItems.isNotEmpty()) {
                NeoButton(
                    text = "EMPTY",
                    onClick = { showEmptyConfirmDialog = true },
                    containerColor = NeoPink,
                    contentColor = NeoWhite,
                    testTag = "btn_empty_trash"
                )
            }
        }

        // 30-Day Retention Notice Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .background(NeoYellow, RoundedCornerShape(8.dp))
                .border(2.dp, NeoBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = NeoDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Items in the Recycle Bin will be permanently deleted after 30 days. Tap any item to restore it.",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeoDark,
                    lineHeight = 16.sp
                )
            }
        }

        if (trashItems.isEmpty()) {
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
                            .background(NeoMint, RoundedCornerShape(12.dp))
                            .border(2.5.dp, NeoBorder, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = null,
                            tint = NeoDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "TRASH IS EMPTY",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Deleted photos and videos will appear here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(trashItems, key = { it.id }) { item ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeoDark)
                            .border(2.dp, NeoBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedItemForAction = item }
                    ) {
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

                        // 30d badge
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                        ) {
                            NeoBadge(
                                text = "30D",
                                backgroundColor = NeoPink,
                                textColor = NeoWhite,
                                borderColor = NeoDark
                            )
                        }
                    }
                }
            }
        }

        // Action Dialog for Single Trashed Item
        selectedItemForAction?.let { item ->
            AlertDialog(
                onDismissRequest = { selectedItemForAction = null },
                title = {
                    Text(
                        text = item.title.uppercase(),
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeoDark
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Original folder: ${item.albumName}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeoDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Choose an action for this file:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                },
                confirmButton = {
                    NeoButton(
                        text = "RESTORE",
                        onClick = {
                            onRestore(item.id)
                            selectedItemForAction = null
                        },
                        containerColor = NeoMint,
                        leadingIcon = Icons.Default.Restore
                    )
                },
                dismissButton = {
                    NeoButton(
                        text = "DELETE FOREVER",
                        onClick = {
                            onDeletePermanently(item.id)
                            selectedItemForAction = null
                        },
                        containerColor = NeoPink,
                        contentColor = NeoWhite,
                        leadingIcon = Icons.Default.DeleteForever
                    )
                },
                containerColor = NeoBg,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Confirm Empty Trash Dialog
        if (showEmptyConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyConfirmDialog = false },
                title = {
                    Text(
                        text = "EMPTY RECYCLE BIN?",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = NeoDark
                    )
                },
                text = {
                    Text(
                        text = "All items in the Trash will be permanently deleted. This cannot be undone.",
                        fontSize = 14.sp,
                        color = NeoDark
                    )
                },
                confirmButton = {
                    NeoButton(
                        text = "EMPTY NOW",
                        onClick = {
                            onEmptyTrash()
                            showEmptyConfirmDialog = false
                        },
                        containerColor = NeoPink,
                        contentColor = NeoWhite,
                        testTag = "btn_confirm_empty_trash"
                    )
                },
                dismissButton = {
                    NeoButton(
                        text = "CANCEL",
                        onClick = { showEmptyConfirmDialog = false },
                        containerColor = NeoWhite
                    )
                },
                containerColor = NeoBg,
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}
