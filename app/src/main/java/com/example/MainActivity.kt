package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Recycling
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.NeoIconButton
import com.example.ui.screens.AlbumDetailScreen
import com.example.ui.screens.AlbumsScreen
import com.example.ui.screens.DedicatedVideoPlayerScreen
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.FontsRepoScreen
import com.example.ui.screens.MediaViewerScreen
import com.example.ui.screens.PhotoEditorScreen
import com.example.ui.screens.TimelinePicturesScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.GalleryTheme
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
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        // Fullscreen: hide the status bar so the app draws edge-to-edge.
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.statusBars())
        }

        setContent {
            GalleryTheme {
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
    val trashMedia by viewModel.trashMedia.collectAsStateWithLifecycle()
    val fontsList by viewModel.fonts.collectAsStateWithLifecycle()

    val mediaGridColumnCount by viewModel.mediaGridColumnCount.collectAsStateWithLifecycle()
    val albumsGridColumnCount by viewModel.albumsGridColumnCount.collectAsStateWithLifecycle()
    val albumGridColumnCounts by viewModel.albumGridColumnCounts.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val selectedMediaIds by viewModel.selectedMediaIds.collectAsStateWithLifecycle()
    val canUndoEditor by viewModel.canUndoEditor.collectAsStateWithLifecycle()
    val canRedoEditor by viewModel.canRedoEditor.collectAsStateWithLifecycle()

    val viewerMediaList by viewModel.viewerMediaList.collectAsStateWithLifecycle()
    val activeMediaIndex by viewModel.activeMediaIndex.collectAsStateWithLifecycle()
    val currentViewerMedia by viewModel.currentViewerMedia.collectAsStateWithLifecycle()

    val currentVideoItem by viewModel.currentVideoItem.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val isVideoLooping by viewModel.isVideoLooping.collectAsStateWithLifecycle()
    val isVideoFitAspect by viewModel.isVideoFitAspect.collectAsStateWithLifecycle()
    val isVideoMuted by viewModel.isVideoMuted.collectAsStateWithLifecycle()

    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val exportFormat by viewModel.exportFormat.collectAsStateWithLifecycle()
    val exportQuality by viewModel.exportQuality.collectAsStateWithLifecycle()
    val stripExif by viewModel.stripExif.collectAsStateWithLifecycle()
    val selectedAlbumName by viewModel.selectedAlbumName.collectAsStateWithLifecycle()
    val feedbackMessage by viewModel.feedbackMessage.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()

    // Hoisted timeline grid state — survives viewer navigation so scroll position is restored
    val timelineGridState = rememberLazyGridState()

    // Back in selection mode clears the selection first.
    // Sub-screen BackHandlers (composed later) take priority when active.
    BackHandler(enabled = isSelectionMode) {
        viewModel.clearSelection()
    }

    // Runtime permission for reading device media (Android 13+ uses READ_MEDIA_*)
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val granted = if (Build.VERSION.SDK_INT >= 33) {
            result[Manifest.permission.READ_MEDIA_IMAGES] == true ||
                result[Manifest.permission.READ_MEDIA_VIDEO] == true
        } else {
            result[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (granted) {
            viewModel.refreshDeviceMedia()
        }
    }

    // Request device media permission on first launch, then sync device contents.
    LaunchedEffect(Unit) {
        val permissions = if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        val missing = permissions.filter {
            context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isEmpty()) {
            viewModel.refreshDeviceMedia()
        } else {
            mediaPermissionLauncher.launch(missing.toTypedArray())
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
                        // Top bar: GALLERY title + Favorites / Recycle Bin shortcuts
                        GalleryTopBar(
                            onOpenFavorites = { viewModel.openFavorites() },
                            onOpenTrash = { viewModel.openTrash() }
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
                                    GalleryTab.MEDIA -> {
                                        TimelinePicturesScreen(
                                            mediaList = allMedia,
                                            albums = albums,
                                            gridColumns = mediaGridColumnCount,
                                            isSelectionMode = isSelectionMode,
                                            selectedIds = selectedMediaIds,
                                            onColumnsChange = { viewModel.setMediaGridColumnCount(it) },
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
                                            },
                                            onCopySelectedToAlbum = { albumName ->
                                                viewModel.copySelectedToAlbum(albumName)
                                            },
                                            onToggleFavorite = { viewModel.toggleFavorite(it) },
                                            isLoading = isSyncing && allMedia.isEmpty(),
                                            gridState = timelineGridState
                                        )
                                    }

                                    GalleryTab.ALBUMS -> {
                                        AlbumsScreen(
                                            albums = albums,
                                            mediaList = allMedia,
                                            gridColumns = albumsGridColumnCount,
                                            onColumnsChange = { viewModel.setAlbumsGridColumnCount(it) },
                                            onAlbumClick = { albumName ->
                                                viewModel.openAlbum(albumName)
                                            },
                                            onCreateAlbum = { name ->
                                                viewModel.createNewAlbum(name)
                                            }
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
                        canUndo = canUndoEditor,
                        canRedo = canRedoEditor,
                        onUndo = { viewModel.undoEditor() },
                        onRedo = { viewModel.redoEditor() },
                        onToolChange = { viewModel.setEditorTool(it) },
                        onFilterChange = { viewModel.setEditorFilter(it) },
                        onRotate = { viewModel.rotateEditor90() },
                        onRotateCcw = { viewModel.rotateEditorCcw() },
                        onResetAllEditor = { viewModel.resetAllEditor() },
                        onFlipH = { viewModel.flipEditorH() },
                        onFlipV = { viewModel.flipEditorV() },
                        onCropRatioChange = { viewModel.setEditorCropRatio(it) },
                        onCropChange = { viewModel.updateEditorCropRect(it) },
                        onApplyCrop = { viewModel.applyCrop() },
                        onResetCrop = { viewModel.resetEditorCropRect() },
                        onAdjustmentsChange = { b, c, s, w ->
                            viewModel.updateEditorAdjustment(b, c, s, w)
                        },
                        onFullAdjustmentsChange = { b, c, s, w, h, sh, wh, bl, t, v, sharp, cl, dn, vig ->
                            viewModel.updateFullAdjustments(b, c, s, w, h, sh, wh, bl, t, v, sharp, cl, dn, vig)
                        },
                        onFilterStrengthChange = { viewModel.setFilterStrength(it) },
                        onCompareToggle = { viewModel.toggleCompare() },
                        onAddShape = { shape -> viewModel.addShape(shape) },
                        onClearShapes = { viewModel.clearShapes() },
                        onLevelAngleChange = { viewModel.setLevelAngle(it) },
                        onPerspectiveHChange = { viewModel.setPerspectiveHorizontal(it) },
                        onPerspectiveVChange = { viewModel.setPerspectiveVertical(it) },
                        onHslChange = { ch, h, s, l -> viewModel.setHslChannel(ch, h, s, l) },
                        onBokehChange = { type, strength, cx, cy -> viewModel.setBokeh(type, strength, cx, cy) },
                        onRetouchTap = { op -> viewModel.addRetouchOp(op) },
                        onClearRetouch = { viewModel.clearRetouchOps() },
                        exportFormat = exportFormat,
                        exportQuality = exportQuality,
                        stripExif = stripExif,
                        onSetExportFormat = { viewModel.setExportFormat(it) },
                        onSetExportQuality = { viewModel.setExportQuality(it) },
                        onSetStripExif = { viewModel.setStripExif(it) },
                        needsSpatialPreview = viewModel.editorNeedsSpatialPreview(),
                        previewRenderer = { maxDim -> viewModel.renderPreviewBitmap(maxDim) },
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
                            albums = albums,
                            gridColumns = albumGridColumnCounts[albumName] ?: albumsGridColumnCount,
                            isSelectionMode = isSelectionMode,
                            selectedIds = selectedMediaIds,
                            onColumnsChange = { viewModel.setAlbumGridColumnCount(albumName, it) },
                            onMediaClick = { media ->
                                val albumItems = allMedia.filter { it.albumName.equals(albumName, ignoreCase = true) }
                                viewModel.openViewer(media, albumItems)
                            },
                            onMediaLongClick = { media ->
                                if (!isSelectionMode) {
                                    viewModel.toggleSelectionMode()
                                }
                                viewModel.toggleMediaSelection(media.id)
                            },
                            onSelectToggle = { id -> viewModel.toggleMediaSelection(id) },
                            onSelectAll = {
                                viewModel.selectAll(allMedia.filter {
                                    it.albumName.equals(albumName, ignoreCase = true)
                                })
                            },
                            onClearSelection = { viewModel.clearSelection() },
                            onDeleteSelected = { viewModel.deleteSelectedToTrash() },
                            onMoveSelectedToAlbum = { target ->
                                viewModel.moveSelectedToAlbum(target)
                            },
                            onCopySelectedToAlbum = { target ->
                                viewModel.copySelectedToAlbum(target)
                            },
                            onClose = { viewModel.closeAlbumDetail() },
                            onToggleFavorite = { viewModel.toggleFavorite(it) }
                        )
                    }
                }

                ActiveScreen.FAVORITES -> {
                    FavoritesScreen(
                        favoriteItems = favorites,
                        isSelectionMode = isSelectionMode,
                        selectedIds = selectedMediaIds,
                        onMediaClick = { media ->
                            viewModel.openViewer(media, favorites)
                        },
                        onMediaLongClick = { media ->
                            if (!isSelectionMode) {
                                viewModel.toggleSelectionMode()
                            }
                            viewModel.toggleMediaSelection(media.id)
                        },
                        onSelectToggle = { id -> viewModel.toggleMediaSelection(id) },
                        onClearSelection = { viewModel.clearSelection() },
                        onClose = { viewModel.closeFavorites() },
                        onToggleFavorite = { viewModel.toggleFavorite(it) }
                    )
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
    onOpenFavorites: () -> Unit,
    onOpenTrash: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        color = NeoDark,
        border = androidx.compose.foundation.BorderStroke(2.dp, NeoBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "GALLERY",
                color = NeoWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NeoIconButton(
                    icon = Icons.Default.Favorite,
                    contentDescription = "Favorites",
                    onClick = onOpenFavorites,
                    backgroundColor = NeoPink,
                    size = 34.dp,
                    shadowOffset = 2.dp,
                    testTag = "top_btn_favorites"
                )

                NeoIconButton(
                    icon = Icons.Default.Recycling,
                    contentDescription = "Recycle Bin",
                    onClick = onOpenTrash,
                    backgroundColor = NeoMint,
                    size = 34.dp,
                    shadowOffset = 2.dp,
                    testTag = "top_btn_trash"
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
                title = "MEDIA",
                icon = Icons.Default.PhotoLibrary,
                isSelected = currentTab == GalleryTab.MEDIA,
                selectedColor = NeoYellow,
                onClick = { onTabSelected(GalleryTab.MEDIA) },
                testTag = "tab_media"
            )

            NavBarItem(
                title = "ALBUMS",
                icon = Icons.Filled.Folder,
                isSelected = currentTab == GalleryTab.ALBUMS,
                selectedColor = NeoMint,
                onClick = { onTabSelected(GalleryTab.ALBUMS) },
                testTag = "tab_albums"
            )
        }
    }
}

@Composable
fun NavBarItem(
    title: String,
    icon: ImageVector?,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Column(
        modifier = Modifier
            .clip(RectangleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .clip(RectangleShape)
                    .background(if (isSelected) selectedColor else Color.Transparent)
                    .then(
                        if (isSelected) Modifier.border(2.dp, NeoBorder, RectangleShape)
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
        }

        Text(
            text = title,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
            color = NeoDark
        )
    }
}
