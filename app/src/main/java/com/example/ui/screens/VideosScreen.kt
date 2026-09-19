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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.MediaItem
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoCard
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun VideosScreen(
    videoList: List<MediaItem>,
    onVideoClick: (MediaItem) -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NeoBg)
    ) {
        if (videoList.isEmpty()) {
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
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = null,
                            tint = NeoDark,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "NO VIDEOS FOUND",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Import or record videos to play them here",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("videos_list"),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${videoList.size} VIDEOS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            color = NeoDark
                        )
                        NeoBadge(
                            text = "SAMSUNG PLAYER ENGINE",
                            backgroundColor = NeoCyan,
                            textColor = NeoDark
                        )
                    }
                }

                items(videoList, key = { it.id }) { video ->
                    NeoCard(
                        onClick = { onVideoClick(video) },
                        backgroundColor = NeoWhite,
                        shadowOffset = 4.dp,
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "video_card_${video.id}"
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Video Thumbnail Box
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1.77f)
                                    .background(NeoDark)
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(video.uri)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = video.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Dark overlay tint
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f))
                                )

                                // Center Play Button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(56.dp)
                                        .background(NeoYellow, CircleShape)
                                        .border(2.5.dp, NeoBorder, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Play Video",
                                        tint = NeoDark,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                // Duration Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp)
                                ) {
                                    val seconds = (video.durationMs / 1000) % 60
                                    val minutes = (video.durationMs / 60000)
                                    NeoBadge(
                                        text = String.format(Locale.getDefault(), "%d:%02d", minutes, seconds),
                                        backgroundColor = NeoDark,
                                        textColor = NeoWhite,
                                        borderColor = NeoWhite
                                    )
                                }

                                // Quality badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                ) {
                                    NeoBadge(
                                        text = "${video.width}P HD",
                                        backgroundColor = NeoMint,
                                        textColor = NeoDark
                                    )
                                }
                            }

                            // Footer info
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = video.title,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = NeoDark,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(video.dateTaken))
                                    val sizeMb = String.format(Locale.getDefault(), "%.1f MB", video.sizeBytes / (1024f * 1024f))
                                    Text(
                                        text = "$dateStr • $sizeMb • ${video.albumName}",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                NeoBadge(
                                    text = "PLAY",
                                    backgroundColor = NeoYellow,
                                    textColor = NeoDark
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
