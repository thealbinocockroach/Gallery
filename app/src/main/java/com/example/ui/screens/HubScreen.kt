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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FontItem
import com.example.data.MediaItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoCard
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBlue
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoOrange
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow

@Composable
fun HubScreen(
    trashItems: List<MediaItem>,
    favoriteItems: List<MediaItem>,
    fontsList: List<FontItem>,
    totalMediaCount: Int,
    onOpenTrash: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenFontsRepo: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
            .testTag("hub_screen"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "UTILITIES & SYSTEM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = NeoDark
                )
                NeoBadge(
                    text = "LOCAL ONLY",
                    backgroundColor = NeoMint,
                    textColor = NeoDark
                )
            }
        }

        // Trash Bin (Recycle bin)
        item {
            HubActionCard(
                title = "TRASH / RECYCLE BIN",
                subtitle = "${trashItems.size} items • Auto-deleted after 30 days",
                badgeText = if (trashItems.isEmpty()) "EMPTY" else "${trashItems.size} IN BIN",
                badgeColor = if (trashItems.isEmpty()) NeoMint else NeoPink,
                icon = Icons.Default.Delete,
                iconBgColor = NeoPink,
                onClick = onOpenTrash,
                testTag = "hub_card_trash"
            )
        }

        // Favorites
        item {
            HubActionCard(
                title = "FAVORITES",
                subtitle = "${favoriteItems.size} starred photos and clips",
                badgeText = "${favoriteItems.size} SAVED",
                badgeColor = NeoYellow,
                icon = Icons.Default.Favorite,
                iconBgColor = NeoYellow,
                onClick = onOpenFavorites,
                testTag = "hub_card_favorites"
            )
        }

        // Free Fonts Repo & Custom Font Installer
        item {
            HubActionCard(
                title = "FREE FONTS REPO & STUDIO",
                subtitle = "${fontsList.size} fonts available • Install custom .TTF/.OTF",
                badgeText = "FREE REPO",
                badgeColor = NeoCyan,
                icon = Icons.Default.FontDownload,
                iconBgColor = NeoCyan,
                onClick = onOpenFontsRepo,
                testTag = "hub_card_fonts"
            )
        }

        // System & Performance Card (Speed & Local Storage guarantee)
        item {
            NeoCard(
                backgroundColor = NeoWhite,
                shadowOffset = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(NeoMint, RoundedCornerShape(8.dp))
                                .border(1.5.dp, NeoBorder, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = NeoDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "GALLERY PRO ENGINE",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = NeoDark
                            )
                            Text(
                                text = "High speed • Zero AI latency • Pure local control",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(NeoBorder)
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TOTAL MEDIA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                            Text(
                                text = "$totalMediaCount Items",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoDark
                            )
                        }
                        Column {
                            Text(
                                text = "STORAGE MODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                            Text(
                                text = "100% Offline Room DB",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoDark
                            )
                        }
                        Column {
                            Text(
                                text = "PRIVACY",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Gray
                            )
                            Text(
                                text = "No Cloud Upload",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = NeoDark
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HubActionCard(
    title: String,
    subtitle: String,
    badgeText: String,
    badgeColor: Color,
    icon: ImageVector,
    iconBgColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    NeoCard(
        onClick = onClick,
        backgroundColor = NeoWhite,
        shadowOffset = 4.dp,
        modifier = Modifier.fillMaxWidth(),
        testTag = testTag
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(iconBgColor, RoundedCornerShape(8.dp))
                        .border(2.dp, NeoBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = NeoDark,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        color = NeoDark
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            NeoBadge(
                text = badgeText,
                backgroundColor = badgeColor,
                textColor = NeoDark
            )
        }
    }
}
