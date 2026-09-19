package com.example.ui.screens

import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun DedicatedVideoPlayerScreen(
    videoItem: MediaItem,
    playbackSpeed: Float,
    isLooping: Boolean,
    isFitAspect: Boolean,
    isMuted: Boolean,
    onSpeedChange: (Float) -> Unit,
    onToggleLoop: () -> Unit,
    onToggleAspect: () -> Unit,
    onToggleMute: () -> Unit,
    onGrabFrame: (Long) -> Unit,
    onClose: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(true) }
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(videoItem.durationMs.coerceAtLeast(1000L)) }
    var showControls by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }

    // Double-tap seek feedback overlay
    var doubleTapFeedback by remember { mutableStateOf<String?>(null) }

    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    // Coroutine to poll video playback position
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoViewRef?.let { vv ->
                if (vv.isPlaying) {
                    currentPositionMs = vv.currentPosition.toLong()
                    if (vv.duration > 0) {
                        durationMs = vv.duration.toLong()
                    }
                }
            }
            delay(250)
        }
    }

    // Auto-hide controls after 4 seconds of inactivity
    LaunchedEffect(showControls, isPlaying, isScreenLocked) {
        if (showControls && isPlaying && !isScreenLocked) {
            delay(4000)
            showControls = false
        }
    }

    // Double tap feedback clear
    LaunchedEffect(doubleTapFeedback) {
        if (doubleTapFeedback != null) {
            delay(900)
            doubleTapFeedback = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("dedicated_video_player")
    ) {
        // Video Surface
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isScreenLocked) {
                    detectTapGestures(
                        onTap = {
                            showControls = !showControls
                        },
                        onDoubleTap = { offset ->
                            if (!isScreenLocked) {
                                val screenWidth = size.width
                                if (offset.x < screenWidth / 2) {
                                    // Left side -> rewind 10s
                                    val newPos = (currentPositionMs - 10000L).coerceAtLeast(0L)
                                    videoViewRef?.seekTo(newPos.toInt())
                                    currentPositionMs = newPos
                                    doubleTapFeedback = "-10s"
                                } else {
                                    // Right side -> forward 10s
                                    val newPos = (currentPositionMs + 10000L).coerceAtMost(durationMs)
                                    videoViewRef?.seekTo(newPos.toInt())
                                    currentPositionMs = newPos
                                    doubleTapFeedback = "+10s"
                                }
                            }
                        }
                    )
                },
            factory = { context ->
                VideoView(context).apply {
                    setVideoURI(Uri.parse(videoItem.uri))
                    setOnPreparedListener { mp ->
                        mp.isLooping = isLooping
                        if (isMuted) {
                            mp.setVolume(0f, 0f)
                        } else {
                            mp.setVolume(1f, 1f)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                mp.playbackParams = mp.playbackParams.setSpeed(playbackSpeed)
                            } catch (_: Exception) {}
                        }
                        if (mp.duration > 0) {
                            durationMs = mp.duration.toLong()
                        }
                        start()
                        isPlaying = true
                    }
                    setOnCompletionListener {
                        if (!isLooping) {
                            isPlaying = false
                        }
                    }
                    videoViewRef = this
                }
            },
            update = { vv ->
                videoViewRef = vv
            }
        )

        // Double tap seek visual feedback pill
        if (doubleTapFeedback != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(NeoYellow, RoundedCornerShape(12.dp))
                    .border(2.5.dp, NeoBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (doubleTapFeedback == "-10s") Icons.Default.Replay10 else Icons.Default.Forward10,
                        contentDescription = null,
                        tint = NeoDark,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = doubleTapFeedback ?: "",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = NeoDark
                    )
                }
            }
        }

        // Screen Lock Floating Toggle (always accessible or on tap)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(16.dp)
        ) {
            NeoIconButton(
                icon = if (isScreenLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock Controls",
                onClick = {
                    isScreenLocked = !isScreenLocked
                    if (!isScreenLocked) showControls = true
                },
                backgroundColor = if (isScreenLocked) NeoYellow else NeoWhite,
                size = 40.dp,
                shadowOffset = 2.dp,
                testTag = "btn_lock_video_screen"
            )
        }

        if (!isScreenLocked) {
            // Top Controls Bar
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 68.dp, top = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NeoIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onClose,
                        backgroundColor = NeoWhite,
                        testTag = "video_btn_back"
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = videoItem.title.uppercase(),
                            color = NeoWhite,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        NeoBadge(
                            text = "SAMSUNG PLAYER STYLE",
                            backgroundColor = NeoCyan,
                            textColor = NeoDark
                        )
                    }
                }
            }

            // Center Play / Pause button
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Rewind 10s
                    NeoIconButton(
                        icon = Icons.Default.Replay10,
                        contentDescription = "Rewind 10s",
                        onClick = {
                            val newPos = (currentPositionMs - 10000L).coerceAtLeast(0L)
                            videoViewRef?.seekTo(newPos.toInt())
                            currentPositionMs = newPos
                        },
                        backgroundColor = NeoWhite,
                        size = 48.dp,
                        shadowOffset = 3.dp
                    )

                    // Big Play/Pause
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(NeoYellow, CircleShape)
                            .border(3.dp, NeoBorder, CircleShape)
                            .clickable {
                                val vv = videoViewRef
                                if (vv != null) {
                                    if (vv.isPlaying) {
                                        vv.pause()
                                        isPlaying = false
                                    } else {
                                        vv.start()
                                        isPlaying = true
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = NeoDark,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    // Forward 10s
                    NeoIconButton(
                        icon = Icons.Default.Forward10,
                        contentDescription = "Forward 10s",
                        onClick = {
                            val newPos = (currentPositionMs + 10000L).coerceAtMost(durationMs)
                            videoViewRef?.seekTo(newPos.toInt())
                            currentPositionMs = newPos
                        },
                        backgroundColor = NeoWhite,
                        size = 48.dp,
                        shadowOffset = 3.dp
                    )
                }
            }

            // Bottom Controller Pane
            AnimatedVisibility(
                visible = showControls,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .background(NeoDark, RoundedCornerShape(14.dp))
                        .border(2.5.dp, NeoBorder, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    // Time Scrubber Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val currentSec = (currentPositionMs / 1000) % 60
                        val currentMin = currentPositionMs / 60000
                        Text(
                            text = String.format(Locale.getDefault(), "%d:%02d", currentMin, currentSec),
                            color = NeoYellow,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )

                        Slider(
                            value = (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f),
                            onValueChange = { norm ->
                                val target = (norm * durationMs).toLong()
                                currentPositionMs = target
                                videoViewRef?.seekTo(target.toInt())
                            },
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = NeoYellow,
                                activeTrackColor = NeoYellow,
                                inactiveTrackColor = Color.DarkGray
                            )
                        )

                        val totalSec = (durationMs / 1000) % 60
                        val totalMin = durationMs / 60000
                        Text(
                            text = String.format(Locale.getDefault(), "%d:%02d", totalMin, totalSec),
                            color = NeoWhite,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Samsung Video Player Toolbar: Frame Grab, Speed, Aspect, Mute, Loop
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Frame Grab / Screenshot
                        NeoButton(
                            text = "CAPTURE",
                            onClick = { onGrabFrame(currentPositionMs) },
                            containerColor = NeoPink,
                            contentColor = NeoWhite,
                            leadingIcon = Icons.Default.CameraAlt,
                            modifier = Modifier.height(36.dp),
                            testTag = "btn_video_capture_frame"
                        )

                        // Playback Speed Selector (0.5x, 1x, 1.25x, 1.5x, 2x)
                        Box(
                            modifier = Modifier
                                .background(NeoWhite, RoundedCornerShape(8.dp))
                                .border(1.5.dp, NeoBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextSpeed = when (playbackSpeed) {
                                        0.5f -> 1.0f
                                        1.0f -> 1.25f
                                        1.25f -> 1.5f
                                        1.5f -> 2.0f
                                        else -> 0.5f
                                    }
                                    onSpeedChange(nextSpeed)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            videoViewRef?.let { vv ->
                                                // Adjust speed if player accessible
                                            }
                                        } catch (_: Exception) {}
                                    }
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "${playbackSpeed}X",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = NeoDark
                            )
                        }

                        // Loop toggle
                        NeoIconButton(
                            icon = Icons.Default.Replay,
                            contentDescription = "Loop",
                            onClick = onToggleLoop,
                            backgroundColor = if (isLooping) NeoMint else NeoWhite,
                            size = 36.dp,
                            shadowOffset = 2.dp
                        )

                        // Mute toggle
                        NeoIconButton(
                            icon = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Mute",
                            onClick = onToggleMute,
                            backgroundColor = if (isMuted) NeoPink else NeoWhite,
                            size = 36.dp,
                            shadowOffset = 2.dp
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoViewRef?.stopPlayback()
        }
    }
}
