package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NeoBadge
import com.example.ui.components.NeoIconButton
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.DedicatedVideoPlayerScreen
import com.example.ui.screens.FontsRepoScreen
import com.example.ui.screens.HubScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.TimelinePicturesScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.GalleryProTheme
import com.example.ui.theme.NeoBg
import com.example.ui.theme.NeoBorder
import com.example.ui.theme.NeoCyan
import com.example.ui.theme.NeoDark
import com.example.ui.theme.NeoMint
import com.example.ui.theme.NeoPink
import com.example.ui.theme.NeoWhite
import com.example.ui.theme.NeoYellow
import com.example.viewmodel.ActiveScreen
import com.example.viewmodel.GalleryTab
import com.example.viewmodel.GalleryViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GalleryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            GalleryProTheme {
                GalleryApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun GalleryApp(viewModel: GalleryViewModel) {
    val context = LocalContext.current
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

    val allMedia by viewModel.allMedia.collectAsStateWithLifecycle()
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val videos by viewModel.videos.collectAsStateWithLifecycle()
    val trashMedia by viewModel.trashMedia.collectAsStateWithLifecycle()
    val fontsList by viewModel.fonts.collectAsStateWithLifecycle()

    val gridColumnCount by viewModel.gridColumnCount.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()

    val viewerMediaList by viewModel.viewerMediaList.collectAsStateWithLifecycle()
    val activeMediaIndex by viewModel.activeMediaIndex.collectAsStateWithLifecycle()
    val currentViewerMedia by viewModel.currentViewerMedia.collectAsStateWithLifecycle()

    val currentVideoItem by viewModel.currentVideoItem.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isVideoLooping by viewModel.isVideoLooping.collectAsStateWithLifecycle()
    val isVideoFitAspect by viewModel.isVideoFitAspect.collectAsStateWithLifecycle()
    val isVideoMuted by viewModel.isVideoMuted.collectAsStateWithLifecycle()

    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val selectedAlbumName by viewModel.selectedAlbumName.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()

    // Zero-permission Android Photo Picker
    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importMedia(uris, isVideo = false)
        }
    }

    // Feedback Toast
    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearFeedback()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = NeoBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ActiveScreen.MAIN -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Neo Top Bar
                        GalleryTopBar(
                            currentTab = currentTab,
                            isSelectionMode = isSelectionMode,
                            onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                            onImportMedia = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            }
                        )

                        // Main Content depending on Tab
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AnimatedContent(
                                targetState = currentTab,
                                transitionSpec = { fadeIn() togetherWith fadeOut() },
                                label = "TabContent"
                            ) { targetTab ->
                                when (targetTab) {
                                    GalleryTab.PICTURES -> {
                                        TimelinePicturesScreen(
                                            mediaList = allMedia,
                                            albums = albums,
                                            gridColumns = gridColumnCount,
                                            isSelectionMode = isSelectionMode,
                                            selectedIds = selectedMediaIds,
                                            onColumnsChange = { viewModel.setGridColumnCount(it) },
                                            onMediaClick = { media ->
                                                viewModel.openViewer(media, allMedia)
                                            },
                                            onMediaLongClick = { media ->
                                                if (!isSelectionMode) {
                                                    viewModel.toggleSelectionMode()
                                                }
                                                viewModel.toggleMediaSelection(media.id)
                                            },
                                            onSelectToggle = { id ->
                                                viewModel.toggleMediaSelection(id)
                                            },
                                            onSelectAll = {
                                                viewModel.selectAll(allMedia)
                                            },
                                            onClearSelection = {
                                                viewModel.clearSelection()
                                            },
                                            onDeleteSelected = {
                                                viewModel.deleteSelectedToTrash()
                                            },
                                            onMoveSelectedToAlbum = { albumName ->
                                                viewModel.moveSelectedToAlbum(albumName)
                                            }
                                        )
                                    }

                                    GalleryTab.ALBUMS -> {
                                        AlbumsScreen(
                                            albums = albums,
                                            mediaList = allMedia,
                                            onAlbumClick = { albumName ->
                                                viewModel.openAlbum(albumName)
                                            },
                                            onCreateAlbum = { name ->
                                                viewModel.createNewAlbum(name)
                                            }
                                        )
                                    }

                                    GalleryTab.VIDEOS -> {
                                        VideosScreen(
                                            videoList = videos,
                                            onVideoClick = { video ->
                                                viewModel.openVideoPlayer(video)
                                            }
                                        )
                                    }

                                    GalleryTab.HUB -> {
                                        HubScreen(
                                            trashItems = trashMedia,
                                            favoriteItems = favorites,
                                            fontsList = fontsList,
                                            totalMediaCount = allMedia.size,
                                            onOpenTrash = { viewModel.openTrash() },
                                            onOpenFavorites = {
                                                // Switch to pictures tab filtered or show favorites
                                                viewModel.openAlbum("Favorites")
                                            },
                                            onOpenFontsRepo = { viewModel.openFontsRepo() }
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom Navigation Bar
                        GalleryBottomNavBar(
                            currentTab = currentTab,
                            onTabSelected = { tab ->
                                viewModel.selectTab(tab)
                            }
                        )
                    }
                }

                ActiveScreen.FULLSCREEN_VIEWER -> {
                    currentViewerMedia?.let { media ->
                        MediaViewerScreen(
                            mediaItem = media,
                            currentIndex = activeMediaIndex,
                            totalCount = viewerMediaList.size,
                            onClose = { viewModel.closeViewer() },
                            onNext = { viewModel.nextMedia() },
                            onPrevious = { viewModel.previousMedia() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                            onDeleteToTrash = { viewModel.deleteMediaToTrash(it) },
                            onEdit = { viewModel.openEditor(it) }
                        )
                    }
                }

                ActiveScreen.VIDEO_PLAYER -> {
                    currentVideoItem?.let { video ->
                        DedicatedVideoPlayerScreen(
                            videoItem = video,
                            playbackSpeed = playbackSpeed,
                            isLooping = isVideoLooping,
                            isFitAspect = isVideoFitAspect,
                            isMuted = isVideoMuted,
                            onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                            onToggleLoop = { viewModel.toggleVideoLoop() },
                            onToggleAspect = { viewModel.toggleVideoAspect() },
                            onToggleMute = { viewModel.toggleVideoMute() },
                            onGrabFrame = { ms -> viewModel.grabVideoFrame(ms) },
                            onClose = { viewModel.closeVideoPlayer() }
                        )
                    }
                }

                ActiveScreen.PHOTO_EDITOR -> {
                    PhotoEditorScreen(
                        editorState = editorState,
                        fontsList = fontsList,
                        onToolChange = { viewModel.setEditorTool(it) },
                        onFilterChange = { viewModel.setEditorFilter(it) },
                        onRotate = { viewModel.rotateEditor90() },
                        onFlipH = { viewModel.flipEditorH() },
                        onFlipV = { viewModel.flipEditorV() },
                        onCropRatioChange = { viewModel.setEditorCropRatio(it) },
                        onAdjustmentsChange = { b, c, s, w ->
                            viewModel.updateEditorAdjustment(b, c, s, w)
                        },
                        onAddDrawingPath = { path ->
                            viewModel.addDrawingPath(path)
                        },
                        onUndoDrawing = { viewModel.undoDrawingPath() },
                        onClearDrawing = { viewModel.clearDrawingPaths() },
                        onTextOverlayChange = { viewModel.setEditorTextOverlay(it) },
                        onInstallCustomFont = { name, cat ->
                            viewModel.installCustomFont(name, cat)
                        },
                        onSave = { asNew -> viewModel.saveEditedPhoto(asNew) },
                        onClose = { viewModel.closeEditor() }
                    )
                }

                ActiveScreen.TRASH_BIN -> {
                    TrashScreen(
                        trashItems = trashMedia,
                        onRestore = { viewModel.restoreFromTrash(it) },
                        onDeletePermanently = { viewModel.deletePermanently(it) },
                        onEmptyTrash = { viewModel.emptyTrash() },
                        onClose = { viewModel.closeTrash() }
                    )
                }

                ActiveScreen.ALBUM_DETAIL -> {
                    selectedAlbumName?.let { albumName ->
                        AlbumDetailScreen(
                            albumName = albumName,
                            mediaList = allMedia,
                            gridColumns = gridColumnCount,
                            onColumnsChange = { viewModel.setGridColumnCount(it) },
                            onMediaClick = { media ->
                                val albumItems = allMedia.filter { it.albumName.equals(albumName, ignoreCase = true) }
                                viewModel.openViewer(media, albumItems)
                            },
                            onAddMediaClick = {
                                mediaPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                )
                            },
                            onClose = { viewModel.closeAlbumDetail() }
                        )
                    }
                }

                ActiveScreen.FONTS_REPO -> {
                    FontsRepoScreen(
                        fontsList = fontsList,
                        onInstallFont = { name, cat ->
                            viewModel.installCustomFont(name, cat)
                        },
                        onRemoveFont = { fontId ->
                            viewModel.removeCustomFont(fontId)
                        },
                        onClose = { viewModel.closeFontsRepo() }
                    )
                }
            }
        }
    }
}

@Composable
fun GalleryTopBar(
    currentTab: GalleryTab,
    isSelectionMode: Boolean,
    onToggleSelectionMode: () -> Unit,
    onImportMedia: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = NeoWhite,
        border = androidx.compose.foundation.BorderStroke(2.dp, NeoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Title Logo with neobrutalist badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(NeoYellow, RoundedCornerShape(8.dp))
                        .border(2.dp, NeoBorder, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "GP",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = NeoDark
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "GALLERY",
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = NeoDark
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        NeoBadge(
                            text = "PRO",
                            backgroundColor = NeoPink,
                            textColor = NeoWhite
                        )
                    }
                    Text(
                        text = "LIGHTWEIGHT • LOCAL • SAMSUNG GESTURES",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.Gray
                    )
                }
            }

            // Top Actions: Import (+) & Select Checkmark
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Select Mode Toggle
                NeoIconButton(
                    icon = Icons.Default.Checklist,
                    contentDescription = "Selection Mode",
                    onClick = onToggleSelectionMode,
                    backgroundColor = if (isSelectionMode) NeoYellow else NeoWhite,
                    size = 38.dp,
                    shadowOffset = 2.dp,
                    testTag = "top_btn_selection"
                )

                // Import Media (+)
                NeoIconButton(
                    icon = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Import Photos/Videos",
                    onClick = onImportMedia,
                    backgroundColor = NeoMint,
                    size = 38.dp,
                    shadowOffset = 2.dp,
                    testTag = "top_btn_import"
                )
            }
        }
    }
}

@Composable
fun GalleryBottomNavBar(
    currentTab: GalleryTab,
    onTabSelected: (GalleryTab) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(),
        color = NeoWhite,
        border = androidx.compose.foundation.BorderStroke(2.dp, NeoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavBarItem(
                title = "PICTURES",
                icon = Icons.Default.PhotoLibrary,
                isSelected = currentTab == GalleryTab.PICTURES,
                selectedColor = NeoYellow,
                onClick = { onTabSelected(GalleryTab.PICTURES) },
                testTag = "tab_pictures"
            )

            NavBarItem(
                title = "ALBUMS",
                icon = Icons.Default.Folder,
                isSelected = currentTab == GalleryTab.ALBUMS,
                selectedColor = NeoMint,
                onClick = { onTabSelected(GalleryTab.ALBUMS) },
                testTag = "tab_albums"
            )

            NavBarItem(
                title = "VIDEOS",
                icon = Icons.Default.Videocam,
                isSelected = currentTab == GalleryTab.VIDEOS,
                selectedColor = NeoCyan,
                onClick = { onTabSelected(GalleryTab.VIDEOS) },
                testTag = "tab_videos"
            )

            NavBarItem(
                title = "HUB",
                icon = Icons.Default.Dashboard,
                isSelected = currentTab == GalleryTab.HUB,
                selectedColor = NeoPink,
                onClick = { onTabSelected(GalleryTab.HUB) },
                testTag = "tab_hub"
            )
        }
    }
}

@Composable
fun NavBarItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) selectedColor else Color.Transparent)
                .then(
                    if (isSelected) Modifier.border(2.dp, NeoBorder, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .padding(horizontal = 14.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = NeoDark,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = NeoDark
        )
    }
}
