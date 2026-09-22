package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Album
import com.example.data.AppDatabase
import com.example.data.FontItem
import com.example.data.GalleryRepository
import com.example.data.MediaItem
import com.example.data.ThumbnailCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Matrix
import android.graphics.Rect
import com.example.ui.components.NormalizedCropRect
import com.example.ui.components.toPixelRect
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.min

enum class GalleryTab {
    MEDIA,
    ALBUMS
}

enum class ActiveScreen {
    MAIN,
    FULLSCREEN_VIEWER,
    VIDEO_PLAYER,
    PHOTO_EDITOR,
    TRASH_BIN,
    ALBUM_DETAIL,
    FONTS_REPO,
    FAVORITES
}

data class DrawingPath(
    val points: List<Pair<Float, Float>>,
    val color: Long,
    val strokeWidth: Float
)

data class TextOverlay(
    val text: String = "",
    val fontId: String = "font_neo_black",
    val fontSizeSp: Float = 28f,
    val textColor: Long = 0xFF141416,
    val backgroundColor: Long = 0xFFFFE500,
    val hasBackgroundBadge: Boolean = true,
    val badgeAlpha: Float = 1f,
    val textAlign: String = "CENTER", // LEFT, CENTER, RIGHT
    val xOffsetNorm: Float = 0.5f,
    val yOffsetNorm: Float = 0.5f,
    // Preview-box pixels at edit time — lets export map box-relative position
    // onto the fitted image exactly (compensates Fit letterboxing).
    val anchorW: Int = 0,
    val anchorH: Int = 0
)

data class EditorState(
    val mediaItem: MediaItem? = null,
    val selectedFilter: String = "Normal",
    val rotation: Float = 0f,
    val flipH: Boolean = false,
    val flipV: Boolean = false,
    val brightness: Float = 0f, // -100 to +100
    val contrast: Float = 0f,   // -100 to +100
    val saturation: Float = 0f, // -100 to +100
    val warmth: Float = 0f,     // -100 to +100
    val cropRatio: String = "Freeform", // Freeform, 1:1, 9:16, 16:9, 4:3, 3:4
    val cropRectNorm: NormalizedCropRect = NormalizedCropRect.FULL,
    val drawingPaths: List<DrawingPath> = emptyList(),
    val textOverlay: TextOverlay? = null,
    val activeTool: String = "FILTERS" // CROP, ADJUST, FILTERS, DOODLE, TEXT
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    // Lazy: flow properties below read repository during construction, before init runs.
    private val repository: GalleryRepository by lazy {
        GalleryRepository(AppDatabase.getDatabase(application, viewModelScope).mediaDao())
    }

    // Loading state — used to avoid flashing "NO MEDIA YET" while DB/media sync is still warming up.
    // Declared before init: init launches coroutines on Main.immediate that may read these
    // synchronously before the constructor finishes, so they must already be initialized.
    private val _isSyncing = MutableStateFlow(true)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Flows from DB — collapse burst shots so each burst appears as one tile everywhere.
    val allMedia: StateFlow<List<MediaItem>> = repository.allMedia
        .map { com.example.data.DeviceMediaScanner.collapseBursts(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val albums: StateFlow<List<Album>> = repository.albums
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favorites: StateFlow<List<MediaItem>> = repository.favorites
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val videos: StateFlow<List<MediaItem>> = repository.videos
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashMedia: StateFlow<List<MediaItem>> = repository.trashMedia
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val fonts: StateFlow<List<FontItem>> = repository.fonts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        ThumbnailCache.init(application)
        // Stop spinning as soon as the DB yields media; the safety timeout below
        // covers the truly-empty library case.
        viewModelScope.launch {
            try {
                allMedia.first { it.isNotEmpty() }
                _isSyncing.value = false
            } catch (_: Exception) {
            }
        }
        viewModelScope.launch {
            // Safety timeout: if sync never starts (no permission etc.), stop spinning after 3s.
            kotlinx.coroutines.delay(3000)
            if (_isSyncing.value) _isSyncing.value = false
        }
    }

    // Navigation State
    private val _currentTab = MutableStateFlow(GalleryTab.MEDIA)
    val currentTab: StateFlow<GalleryTab> = _currentTab.asStateFlow()

    private val _currentScreen = MutableStateFlow(ActiveScreen.MAIN)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    private val _selectedAlbumName = MutableStateFlow<String?>(null)
    val selectedAlbumName: StateFlow<String?> = _selectedAlbumName.asStateFlow()

    // Grid Scaling (Finger gestures: 2, 3, 4, 5 columns)
    // Samsung-style: the MEDIA timeline keeps its own zoom,
    // the ALBUMS tab keeps its own, and each album folder remembers its own.
    private val _mediaGridColumnCount = MutableStateFlow(3)
    val mediaGridColumnCount: StateFlow<Int> = _mediaGridColumnCount.asStateFlow()

    private val _albumsGridColumnCount = MutableStateFlow(3)
    val albumsGridColumnCount: StateFlow<Int> = _albumsGridColumnCount.asStateFlow()

    private val _albumGridColumnCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val albumGridColumnCounts: StateFlow<Map<String, Int>> = _albumGridColumnCounts.asStateFlow()

    // Selection Mode
    private val _isSelectionMode = MutableStateFlow(false)
    val isSelectionMode: StateFlow<Boolean> = _isSelectionMode.asStateFlow()

    private val _selectedMediaIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedMediaIds: StateFlow<Set<Long>> = _selectedMediaIds.asStateFlow()

    // Search Query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Fullscreen Viewer
    private val _activeMediaIndex = MutableStateFlow(0)
    val activeMediaIndex: StateFlow<Int> = _activeMediaIndex.asStateFlow()

    private val _viewerMediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val viewerMediaList: StateFlow<List<MediaItem>> = _viewerMediaList.asStateFlow()

    // Where the viewer/video-player/editor was opened from, so Back returns there.
    private val _viewerOrigin = MutableStateFlow(ActiveScreen.MAIN)

    val currentViewerMedia: StateFlow<MediaItem?> = combine(
        _viewerMediaList,
        _activeMediaIndex
    ) { list, index ->
        if (list.isNotEmpty() && index in list.indices) list[index] else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Dedicated Video Player State
    private val _currentVideoItem = MutableStateFlow<MediaItem?>(null)
    val currentVideoItem: StateFlow<MediaItem?> = _currentVideoItem.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isVideoLooping = MutableStateFlow(false)
    val isVideoLooping: StateFlow<Boolean> = _isVideoLooping.asStateFlow()

    private val _isVideoFitAspect = MutableStateFlow(true)
    val isVideoFitAspect: StateFlow<Boolean> = _isVideoFitAspect.asStateFlow()

    private val _isVideoMuted = MutableStateFlow(false)
    val isVideoMuted: StateFlow<Boolean> = _isVideoMuted.asStateFlow()

    // Editor State
    private val _editorState = MutableStateFlow(EditorState())
    val editorState: StateFlow<EditorState> = _editorState.asStateFlow()

    private val _editorUndoStack = MutableStateFlow<List<EditorState>>(emptyList())
    private val _editorRedoStack = MutableStateFlow<List<EditorState>>(emptyList())

    private val _canUndoEditor = MutableStateFlow(false)
    val canUndoEditor: StateFlow<Boolean> = _canUndoEditor.asStateFlow()

    private val _canRedoEditor = MutableStateFlow(false)
    val canRedoEditor: StateFlow<Boolean> = _canRedoEditor.asStateFlow()

    // Toast / Feedback message
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    private fun itemsWord(count: Int): String = if (count == 1) "item" else "items"

    // Read media from the device via MediaStore
    fun refreshDeviceMedia() {
        viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                val context = getApplication<Application>()
                val added = repository.syncDeviceMedia(context)
                if (added > 0) {
                    showFeedback("Imported $added items from device")
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    // Tab Navigation
    fun selectTab(tab: GalleryTab) {
        _currentTab.value = tab
        if (_isSelectionMode.value) {
            clearSelection()
        }
    }

    fun setMediaGridColumnCount(count: Int) {
        _mediaGridColumnCount.value = count.coerceIn(2, 5)
    }

    fun zoomMediaGridIn() {
        if (_mediaGridColumnCount.value > 2) {
            _mediaGridColumnCount.value -= 1
        }
    }

    fun zoomMediaGridOut() {
        if (_mediaGridColumnCount.value < 5) {
            _mediaGridColumnCount.value += 1
        }
    }

    fun setAlbumsGridColumnCount(count: Int) {
        _albumsGridColumnCount.value = count.coerceIn(2, 5)
    }

    fun zoomAlbumsGridIn() {
        if (_albumsGridColumnCount.value > 2) {
            _albumsGridColumnCount.value -= 1
        }
    }

    fun zoomAlbumsGridOut() {
        if (_albumsGridColumnCount.value < 5) {
            _albumsGridColumnCount.value += 1
        }
    }

    fun albumGridColumnsFor(albumName: String): Int =
        _albumGridColumnCounts.value[albumName] ?: _albumsGridColumnCount.value

    fun setAlbumGridColumnCount(albumName: String, count: Int) {
        _albumGridColumnCounts.value = _albumGridColumnCounts.value + (albumName to count.coerceIn(2, 5))
    }

    fun zoomAlbumGridIn(albumName: String) {
        val current = albumGridColumnsFor(albumName)
        if (current > 2) {
            _albumGridColumnCounts.value = _albumGridColumnCounts.value + (albumName to current - 1)
        }
    }

    fun zoomAlbumGridOut(albumName: String) {
        val current = albumGridColumnsFor(albumName)
        if (current < 5) {
            _albumGridColumnCounts.value = _albumGridColumnCounts.value + (albumName to current + 1)
        }
    }

    // Selection
    fun toggleSelectionMode() {
        _isSelectionMode.value = !_isSelectionMode.value
        if (!_isSelectionMode.value) {
            _selectedMediaIds.value = emptySet()
        }
    }

    fun toggleMediaSelection(id: Long) {
        val current = _selectedMediaIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _selectedMediaIds.value = current
        if (current.isEmpty() && _isSelectionMode.value) {
            // Deselecting the last item exits selection mode.
            _isSelectionMode.value = false
        }
    }

    fun selectAll(list: List<MediaItem>) {
        _selectedMediaIds.value = list.map { it.id }.toSet()
        _isSelectionMode.value = true
    }

    fun clearSelection() {
        _selectedMediaIds.value = emptySet()
        _isSelectionMode.value = false
    }

    // Favorites Screen
    fun openFavorites() {
        _currentScreen.value = ActiveScreen.FAVORITES
    }

    fun closeFavorites() {
        _currentScreen.value = ActiveScreen.MAIN
    }

    // Copy selected items directly into a chosen album folder.
    fun copySelectedToAlbum(albumName: String) {
        val ids = _selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        val context = getApplication<Application>()
        viewModelScope.launch(Dispatchers.IO) {
            val sourceItems = repository.getMediaByIds(ids)
            val copied = repository.copyMediaFiles(context, sourceItems, albumName)
            if (copied.isNotEmpty()) {
                repository.insertMultipleMedia(copied)
            }
            clearSelection()
            showFeedback("Copied ${copied.size} ${itemsWord(copied.size)} to $albumName")
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Open Fullscreen Viewer
    fun openViewer(media: MediaItem, contextList: List<MediaItem>) {
        _viewerMediaList.value = contextList
        val index = contextList.indexOfFirst { it.id == media.id }
        _activeMediaIndex.value = if (index >= 0) index else 0

        _viewerOrigin.value = when (_currentScreen.value) {
            ActiveScreen.ALBUM_DETAIL -> ActiveScreen.ALBUM_DETAIL
            ActiveScreen.FAVORITES -> ActiveScreen.FAVORITES
            else -> ActiveScreen.MAIN
        }

        if (media.isVideo) {
            openVideoPlayer(media)
        } else {
            _currentScreen.value = ActiveScreen.FULLSCREEN_VIEWER
        }
    }

    fun nextMedia() {
        val list = _viewerMediaList.value
        if (list.isNotEmpty()) {
            _activeMediaIndex.value = (_activeMediaIndex.value + 1).coerceAtMost(list.size - 1)
        }
    }

    fun previousMedia() {
        if (_viewerMediaList.value.isNotEmpty()) {
            _activeMediaIndex.value = (_activeMediaIndex.value - 1).coerceAtLeast(0)
        }
    }

    fun closeViewer() {
        _currentScreen.value = _viewerOrigin.value
    }

    // Video Player
    fun openVideoPlayer(media: MediaItem) {
        _currentVideoItem.value = media
        _currentScreen.value = ActiveScreen.VIDEO_PLAYER
    }

    fun closeVideoPlayer() {
        _currentVideoItem.value = null
        _currentScreen.value = _viewerOrigin.value
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
    }

    fun toggleVideoLoop() {
        _isVideoLooping.value = !_isVideoLooping.value
    }

    fun toggleVideoAspect() {
        _isVideoFitAspect.value = !_isVideoFitAspect.value
    }

    fun toggleVideoMute() {
        _isVideoMuted.value = !_isVideoMuted.value
    }

    fun grabVideoFrame(currentMs: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val video = _currentVideoItem.value ?: return@launch
            val newPhoto = MediaItem(
                title = "Frame Capture ${video.title} (${currentMs / 1000}s)",
                uri = video.uri,
                isVideo = false,
                durationMs = 0,
                sizeBytes = 2500000L,
                dateTaken = System.currentTimeMillis(),
                width = video.width,
                height = video.height,
                albumName = "Screenshots",
                isFavorite = false
            )
            repository.insertMedia(newPhoto)
            showFeedback("Frame captured and saved to Screenshots!")
        }
    }

    // Album Navigation
    fun openAlbum(albumName: String) {
        _selectedAlbumName.value = albumName
        _currentScreen.value = ActiveScreen.ALBUM_DETAIL
    }

    fun closeAlbumDetail() {
        _selectedAlbumName.value = null
        _currentScreen.value = ActiveScreen.MAIN
    }

    fun createNewAlbum(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.createAlbum(name.trim())
            showFeedback("Album '$name' created!")
        }
    }

    fun moveSelectedToAlbum(albumName: String) {
        val ids = _selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { id ->
                repository.moveMediaToAlbum(id, albumName)
            }
            clearSelection()
            showFeedback("Moved ${ids.size} ${itemsWord(ids.size)} to $albumName")
        }
    }

    // Trash System (Recycle bin)
    fun openTrash() {
        _currentScreen.value = ActiveScreen.TRASH_BIN
    }

    fun closeTrash() {
        _currentScreen.value = ActiveScreen.MAIN
    }

    fun deleteMediaToTrash(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.moveToTrash(id)
            showFeedback("Moved to Trash")
            if (_currentScreen.value == ActiveScreen.FULLSCREEN_VIEWER) {
                closeViewer()
            }
        }
    }

    fun deleteSelectedToTrash() {
        val ids = _selectedMediaIds.value.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            ids.forEach { repository.moveToTrash(it) }
            clearSelection()
            showFeedback("Moved ${ids.size} items to Trash")
        }
    }

    fun restoreFromTrash(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.restoreFromTrash(id)
            showFeedback("Restored item from Trash")
        }
    }

    fun deletePermanently(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePermanently(getApplication(), id)
            showFeedback("Permanently deleted")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyTrash(getApplication())
            showFeedback("Trash bin emptied")
        }
    }

    // Favorites
    fun toggleFavorite(media: MediaItem) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(media.id, !media.isFavorite)
        }
    }

    // Import Media from Device (zero-permission or picker)
    fun importMedia(uris: List<Uri>, isVideo: Boolean = false, albumName: String = "Camera") {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            uris.forEachIndexed { idx, uri ->
                val title = if (isVideo) "Video ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(now + idx))}"
                            else "Photo ${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(now + idx))}"
                val item = MediaItem(
                    title = title,
                    uri = uri.toString(),
                    isVideo = isVideo,
                    durationMs = if (isVideo) 12000L else 0L,
                    sizeBytes = if (isVideo) 6500000L else 3200000L,
                    dateTaken = now + idx,
                    albumName = albumName
                )
                repository.insertMedia(item)
            }
            showFeedback("Imported ${uris.size} items into $albumName")
        }
    }

    // Fonts Repository
    fun openFontsRepo() {
        _currentScreen.value = ActiveScreen.FONTS_REPO
    }

    fun closeFontsRepo() {
        _currentScreen.value = ActiveScreen.MAIN
    }

    fun installCustomFont(name: String, category: String = "Custom Installed", fontPath: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val fontId = "custom_font_${System.currentTimeMillis()}"
            val newFont = FontItem(
                id = fontId,
                name = name.trim(),
                category = category,
                isCustomInstalled = true,
                fontPath = fontPath,
                previewText = "CUSTOM FONT: ${name.uppercase()} READY"
            )
            repository.installFont(newFont)
            showFeedback("Font '$name' added to font library!")
        }
    }

    fun removeCustomFont(fontId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeFont(fontId)
            showFeedback("Font removed")
        }
    }

    // Photo Editor
    fun openEditor(media: MediaItem) {
        _editorState.value = EditorState(
            mediaItem = media,
            selectedFilter = media.filterName,
            rotation = media.rotationDegrees,
            flipH = media.flipHorizontal,
            flipV = media.flipVertical,
            activeTool = "FILTERS"
        )
        _editorUndoStack.value = emptyList()
        _editorRedoStack.value = emptyList()
        _canUndoEditor.value = false
        _canRedoEditor.value = false
        _currentScreen.value = ActiveScreen.PHOTO_EDITOR
    }

    fun closeEditor() {
        _currentScreen.value = if (currentViewerMedia.value != null) {
            ActiveScreen.FULLSCREEN_VIEWER
        } else {
            ActiveScreen.MAIN
        }
    }

    private fun pushEditorUndo() {
        _editorUndoStack.value = (_editorUndoStack.value + listOf(_editorState.value)).takeLast(50)
        _editorRedoStack.value = emptyList()
        _canUndoEditor.value = true
        _canRedoEditor.value = false
    }

    fun undoEditor() {
        val undoStack = _editorUndoStack.value
        if (undoStack.isEmpty()) return
        val previous = undoStack.last()
        _editorRedoStack.value = _editorRedoStack.value + listOf(_editorState.value)
        _editorUndoStack.value = undoStack.dropLast(1)
        _editorState.value = previous
        _canUndoEditor.value = _editorUndoStack.value.isNotEmpty()
        _canRedoEditor.value = true
    }

    fun redoEditor() {
        val redoStack = _editorRedoStack.value
        if (redoStack.isEmpty()) return
        val next = redoStack.last()
        _editorUndoStack.value = _editorUndoStack.value + listOf(_editorState.value)
        _editorRedoStack.value = redoStack.dropLast(1)
        _editorState.value = next
        _canRedoEditor.value = _editorRedoStack.value.isNotEmpty()
        _canUndoEditor.value = true
    }

    fun setEditorTool(tool: String) {
        if (_editorState.value.activeTool != tool) pushEditorUndo()
        _editorState.value = _editorState.value.copy(activeTool = tool)
    }

    fun setEditorFilter(filter: String) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(selectedFilter = filter)
    }

    fun rotateEditor90() {
        pushEditorUndo()
        val current = _editorState.value.rotation
        _editorState.value = _editorState.value.copy(rotation = (current + 90f) % 360f)
    }

    fun flipEditorH() {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(flipH = !_editorState.value.flipH)
    }

    fun flipEditorV() {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(flipV = !_editorState.value.flipV)
    }

    fun setEditorCropRatio(ratio: String) {
        val prev = _editorState.value
        if (prev.cropRatio == ratio) return
        pushEditorUndo()
        val media = prev.mediaItem
        val imgW = if (media != null && media.width > 0) media.width.toFloat() else 1000f
        val imgH = if (media != null && media.height > 0) media.height.toFloat() else 1000f

        val targetPixelAspect = when (ratio) {
            "1:1" -> 1.0f
            "9:16" -> 9f / 16f
            "16:9" -> 16f / 9f
            "4:3" -> 4f / 3f
            "3:4" -> 3f / 4f
            else -> null // "Freeform"
        }

        val newRect = if (targetPixelAspect != null) {
            val targetNormAspect = targetPixelAspect * (imgH / imgW)
            if (targetNormAspect >= 1f) {
                val normW = min(1f, targetNormAspect)
                val normH = (normW / targetNormAspect).coerceAtMost(1f)
                val left = (1f - normW) / 2f
                val top = (1f - normH) / 2f
                NormalizedCropRect(left, top, left + normW, top + normH)
            } else {
                val normH = 1f
                val normW = (normH * targetNormAspect).coerceAtMost(1f)
                val left = (1f - normW) / 2f
                val top = 0f
                NormalizedCropRect(left, top, left + normW, top + normH)
            }
        } else {
            prev.cropRectNorm
        }

        _editorState.value = prev.copy(
            cropRatio = ratio,
            cropRectNorm = newRect
        )
    }

    fun updateEditorCropRect(rect: NormalizedCropRect) {
        _editorState.value = _editorState.value.copy(cropRectNorm = rect)
    }

    fun resetEditorCropRect() {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(
            cropRatio = "Freeform",
            cropRectNorm = NormalizedCropRect.FULL
        )
    }

    fun applyCrop() {
        val state = _editorState.value
        val original = state.mediaItem ?: return
        val cropRect = state.cropRectNorm

        val rotation = ((state.rotation % 360f) + 360f) % 360f
        if (cropRect.left <= 0.005f && cropRect.top <= 0.005f &&
            cropRect.right >= 0.995f && cropRect.bottom >= 0.995f &&
            rotation == 0f && !state.flipH && !state.flipV
        ) {
            showFeedback("Image already at full frame")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                // Region decode: only the cropped window is ever decoded, then the
                // preview transform (rotation/flip) is baked into that small result.
                val cropped = cropRegionEfficient(
                    context = context,
                    uriString = original.uri,
                    rect = cropRect,
                    rotationDegrees = rotation,
                    flipH = state.flipH,
                    flipV = state.flipV
                )
                if (cropped == null) {
                    showFeedback("Could not load image for cropping")
                    return@launch
                }

                val outW = cropped.width
                val outH = cropped.height
                val editsDir = File(context.filesDir, "edits").apply { mkdirs() }
                val cropFile = File(editsDir, "crop_${System.currentTimeMillis()}.jpg")
                FileOutputStream(cropFile).use { out ->
                    cropped.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                try {
                    cropped.recycle()
                } catch (_: Exception) {
                }

                pushEditorUndo()
                _editorState.value = _editorState.value.copy(
                    mediaItem = original.copy(
                        uri = Uri.fromFile(cropFile).toString(),
                        width = outW,
                        height = outH,
                        // Transform is baked into the saved pixels — clear it so the
                        // viewer/editor don't apply it a second time.
                        rotationDegrees = 0f,
                        flipHorizontal = false,
                        flipVertical = false
                    ),
                    rotation = 0f,
                    flipH = false,
                    flipV = false,
                    cropRectNorm = NormalizedCropRect.FULL,
                    cropRatio = "Freeform"
                )
                showFeedback("Cropped to ${outW}×${outH}!")
            } catch (e: Exception) {
                showFeedback("Failed to crop: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Crops [rect] (normalized coords in the *displayed*, possibly rotated/flipped frame)
     * without ever decoding the full image: maps the box back to stored pixel space,
     * decodes only that window via BitmapRegionDecoder, then bakes rotation/flip into
     * the small result. Falls back to full decode when region decoding is unavailable.
     */
    private suspend fun cropRegionEfficient(
        context: android.content.Context,
        uriString: String,
        rect: NormalizedCropRect,
        rotationDegrees: Float,
        flipH: Boolean,
        flipV: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val parsed = Uri.parse(uriString)
            if (parsed.scheme != "file" && parsed.scheme != "content") {
                return@withContext cropViaFullDecode(context, uriString, rect, rotationDegrees, flipH, flipV)
            }
            // 1. Stored dimensions from bounds only (no pixel allocation).
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openImageStream(context, parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val storedW = bounds.outWidth
            val storedH = bounds.outHeight
            if (storedW <= 0 || storedH <= 0) {
                return@withContext cropViaFullDecode(context, uriString, rect, rotationDegrees, flipH, flipV)
            }
            // 2. Display-frame box -> stored-space box (un-mirror, then inverse-rotate),
            // then to source pixels via the single shared normalization contract.
            val stored = mapDisplayRectToStored(rect, rotationDegrees, flipH, flipV)
            val px = stored.toPixelRect(storedW, storedH)
            // 3. Decode only the window.
            val window = openImageStream(context, parsed)?.use { stream ->
                val decoder = BitmapRegionDecoder.newInstance(stream, false)
                try {
                    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    decoder?.decodeRegion(
                        Rect(px.left.toInt(), px.top.toInt(), px.right.toInt(), px.bottom.toInt()),
                        opts
                    )
                } finally {
                    decoder?.recycle()
                }
            } ?: return@withContext cropViaFullDecode(context, uriString, rect, rotationDegrees, flipH, flipV)
            // 4. Bake the preview transform into the small result.
            bakeTransform(window, rotationDegrees, flipH, flipV)
        } catch (_: Exception) {
            cropViaFullDecode(context, uriString, rect, rotationDegrees, flipH, flipV)
        }
    }

    private fun openImageStream(context: android.content.Context, parsed: Uri): java.io.InputStream? {
        return try {
            when (parsed.scheme) {
                "file" -> FileInputStream(parsed.path ?: return null)
                "content" -> context.contentResolver.openInputStream(parsed)
                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Maps a normalized crop box from the displayed (rotated/flipped) frame back to
     * stored pixel space. Display pipeline is rotation * mirror, so invert in reverse:
     * un-mirror first, then inverse-rotate. 90°/270° swaps the axes.
     */
    private fun mapDisplayRectToStored(
        rect: NormalizedCropRect,
        rotationDegrees: Float,
        flipH: Boolean,
        flipV: Boolean
    ): NormalizedCropRect {
        var l = rect.left
        var t = rect.top
        var r = rect.right
        var b = rect.bottom
        if (flipH) {
            val nl = 1f - r
            r = 1f - l
            l = nl
        }
        if (flipV) {
            val nt = 1f - b
            b = 1f - t
            t = nt
        }
        return when (((rotationDegrees % 360f) + 360f) % 360f) {
            90f -> NormalizedCropRect(t, 1f - r, b, 1f - l)
            180f -> NormalizedCropRect(1f - r, 1f - b, 1f - l, 1f - t)
            270f -> NormalizedCropRect(1f - b, l, 1f - t, r)
            else -> NormalizedCropRect(l, t, r, b)
        }
    }

    /** Applies the preview transform (rotation, then mirror) to an already-cropped bitmap. */
    private fun bakeTransform(src: Bitmap, rotationDegrees: Float, flipH: Boolean, flipV: Boolean): Bitmap {
        val rot = ((rotationDegrees % 360f) + 360f) % 360f
        if (rot == 0f && !flipH && !flipV) return src
        val m = Matrix()
        if (rot != 0f) m.postRotate(rot)
        if (flipH || flipV) m.postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
        val out = Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        if (out != src) {
            try {
                src.recycle()
            } catch (_: Exception) {
            }
        }
        return out
    }

    /** Full-decode fallback for non-seekable sources: same mapping, whole bitmap in RAM. */
    private suspend fun cropViaFullDecode(
        context: android.content.Context,
        uriString: String,
        rect: NormalizedCropRect,
        rotationDegrees: Float,
        flipH: Boolean,
        flipV: Boolean
    ): Bitmap? {
        val full = decodeFullBitmap(context, uriString) ?: return null
        return try {
            val stored = mapDisplayRectToStored(rect, rotationDegrees, flipH, flipV)
            val cropX = (stored.left * full.width).toInt().coerceIn(0, full.width - 1)
            val cropY = (stored.top * full.height).toInt().coerceIn(0, full.height - 1)
            val cropW = (stored.width * full.width).toInt().coerceIn(1, full.width - cropX)
            val cropH = (stored.height * full.height).toInt().coerceIn(1, full.height - cropY)
            val window = Bitmap.createBitmap(full, cropX, cropY, cropW, cropH)
            if (window != full) {
                try {
                    full.recycle()
                } catch (_: Exception) {
                }
            }
            bakeTransform(window, rotationDegrees, flipH, flipV)
        } catch (_: Exception) {
            try {
                full.recycle()
            } catch (_: Exception) {
            }
            null
        }
    }

    /**
     * Burns the styled text layer onto a high-resolution bitmap (WYSIWYG with the
     * editor preview): badge rect with opacity, contrast outline, drop shadow, fill.
     * Position maps box-relative norms onto the fitted image, compensating the
     * ContentScale.Fit letterbox via the anchor (preview-box) size recorded at edit time.
     */
    private suspend fun renderTextComposition(
        context: android.content.Context,
        base: Bitmap,
        overlay: TextOverlay
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val out = base.copy(Bitmap.Config.ARGB_8888, true) ?: return@withContext null
            val canvas = android.graphics.Canvas(out)
            val density = context.resources.displayMetrics.density
            val anchorW = overlay.anchorW.takeIf { it > 0 } ?: out.width
            val anchorH = overlay.anchorH.takeIf { it > 0 } ?: out.height
            val fitted = fitRect(
                anchorW.toFloat(),
                anchorH.toFloat(),
                out.width.toFloat() / out.height.toFloat()
            )
            val ix = (0.5f + (overlay.xOffsetNorm - 0.5f) * (anchorW / fitted.width())).coerceIn(0f, 1f)
            val iy = (0.5f + (overlay.yOffsetNorm - 0.5f) * (anchorH / fitted.height())).coerceIn(0f, 1f)
            val cx = ix * out.width
            val cy = iy * out.height
            // Preserve on-screen proportion: sp -> px scaled by image-vs-preview width.
            val textPx = (overlay.fontSizeSp * density * (out.width.toFloat() / anchorW.toFloat()))
                .coerceIn(12f, out.height.toFloat())
            val scaleUnit = out.width / 1080f

            val align = when (overlay.textAlign) {
                "LEFT" -> android.graphics.Paint.Align.LEFT
                "RIGHT" -> android.graphics.Paint.Align.RIGHT
                else -> android.graphics.Paint.Align.CENTER
            }
            val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                textSize = textPx
                typeface = nativeTypefaceFor(overlay.fontId)
                textAlign = align
            }
            val text = overlay.text
            val textW = paint.measureText(text)
            val fm = paint.fontMetrics
            // Anchor box by alignment, mirroring the preview's centered container.
            val boxLeft = when (align) {
                android.graphics.Paint.Align.LEFT -> cx
                android.graphics.Paint.Align.RIGHT -> cx - textW
                else -> cx - textW / 2f
            }
            val textTop = cy + fm.ascent
            val textBottom = cy + fm.descent

            if (overlay.hasBackgroundBadge) {
                val padH = textPx * 0.45f
                val padV = textPx * 0.28f
                val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.FILL
                    // N.B. color first: setColor() resets alpha, so alpha goes last.
                    color = overlay.backgroundColor.toInt()
                    alpha = (overlay.badgeAlpha.coerceIn(0f, 1f) * 255).toInt()
                }
                val badgeRect = android.graphics.RectF(
                    boxLeft - padH,
                    textTop - padV,
                    boxLeft + textW + padH,
                    textBottom + padV
                )
                canvas.drawRect(badgeRect, badgePaint)
                val borderPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = (3f * scaleUnit).coerceAtLeast(2f)
                    color = 0xFF141416.toInt()
                }
                canvas.drawRect(badgeRect, borderPaint)
            }

            // Outline pass (contrast color from text luminance) then fill pass with shadow.
            val textInt = overlay.textColor.toInt()
            val luminance =
                (0.299f * android.graphics.Color.red(textInt) +
                    0.587f * android.graphics.Color.green(textInt) +
                    0.114f * android.graphics.Color.blue(textInt)) / 255f
            val outlineInt = if (luminance > 0.5f) 0xFF141416.toInt() else 0xFFFFFFFF.toInt()
            val baseline = cy - (fm.ascent + fm.descent) / 2f
            val outlinePaint = android.graphics.Paint(paint).apply {
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = (textPx / 14f).coerceAtLeast(2f)
                strokeJoin = android.graphics.Paint.Join.ROUND
                color = outlineInt
            }
            canvas.drawText(text, cx, baseline, outlinePaint)
            val fillPaint = android.graphics.Paint(paint).apply {
                style = android.graphics.Paint.Style.FILL
                color = textInt
                setShadowLayer(6f * scaleUnit, 3f * scaleUnit, 3f * scaleUnit, 0x99000000.toInt())
            }
            canvas.drawText(text, cx, baseline, fillPaint)
            out
        } catch (_: Exception) {
            null
        }
    }

    /** Fitted image rect for a Fit-scaled image inside a container (mirrors the cropper math). */
    private fun fitRect(containerW: Float, containerH: Float, imageAspect: Float): android.graphics.RectF {
        return if (containerW / containerH > imageAspect) {
            val w = containerH * imageAspect
            val l = (containerW - w) / 2f
            android.graphics.RectF(l, 0f, l + w, containerH)
        } else {
            val h = containerW / imageAspect
            val t = (containerH - h) / 2f
            android.graphics.RectF(0f, t, containerW, t + h)
        }
    }

    /** Best-effort mapping from editor font ids to native typefaces for canvas export. */
    private fun nativeTypefaceFor(fontId: String): android.graphics.Typeface {
        val family = when (fontId) {
            "font_editorial_serif" -> android.graphics.Typeface.SERIF
            "font_retro_mono" -> android.graphics.Typeface.MONOSPACE
            "font_script_flow", "font_marker_brush" -> android.graphics.Typeface.SANS_SERIF
            else -> android.graphics.Typeface.SANS_SERIF
        }
        val style = when (fontId) {
            "font_neo_black", "font_bebas_impact" -> android.graphics.Typeface.BOLD
            "font_editorial_serif", "font_retro_mono" -> android.graphics.Typeface.BOLD
            "font_script_flow" -> android.graphics.Typeface.ITALIC
            else -> android.graphics.Typeface.BOLD
        }
        return android.graphics.Typeface.create(family, style)
    }

    private suspend fun decodeFullBitmap(context: android.content.Context, uriString: String): Bitmap? =
        withContext(Dispatchers.IO) {
            try {
                val parsed = Uri.parse(uriString)
                if (parsed.scheme == "file") {
                    val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                    BitmapFactory.decodeFile(parsed.path, opts)
                } else if (parsed.scheme == "content") {
                    context.contentResolver.openInputStream(parsed)?.use { stream ->
                        val opts = BitmapFactory.Options().apply { inPreferredConfig = Bitmap.Config.ARGB_8888 }
                        BitmapFactory.decodeStream(stream, null, opts)
                    }
                } else {
                    val loader = coil.Coil.imageLoader(context)
                    val req = coil.request.ImageRequest.Builder(context)
                        .data(uriString)
                        .allowHardware(false)
                        .build()
                    (loader.execute(req).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                }
            } catch (_: Exception) {
                try {
                    val loader = coil.Coil.imageLoader(context)
                    val req = coil.request.ImageRequest.Builder(context)
                        .data(uriString)
                        .allowHardware(false)
                        .build()
                    (loader.execute(req).drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                } catch (_: Exception) {
                    null
                }
            }
        }

    fun updateEditorAdjustment(brightness: Float, contrast: Float, saturation: Float, warmth: Float) {
        val prev = _editorState.value
        if (prev.brightness == brightness && prev.contrast == contrast &&
            prev.saturation == saturation && prev.warmth == warmth
        ) return
        pushEditorUndo()
        _editorState.value = prev.copy(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth
        )
    }

    fun addDrawingPath(path: DrawingPath) {
        pushEditorUndo()
        val current = _editorState.value.drawingPaths
        _editorState.value = _editorState.value.copy(drawingPaths = current + path)
    }

    fun undoDrawingPath() {
        val current = _editorState.value.drawingPaths
        if (current.isNotEmpty()) {
            pushEditorUndo()
            _editorState.value = _editorState.value.copy(drawingPaths = current.dropLast(1))
        }
    }

    fun clearDrawingPaths() {
        if (_editorState.value.drawingPaths.isEmpty()) return
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(drawingPaths = emptyList())
    }

    fun setEditorTextOverlay(textOverlay: TextOverlay?) {
        if (_editorState.value.textOverlay == textOverlay) return
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(textOverlay = textOverlay)
    }

    fun saveEditedPhoto(asNew: Boolean = true) {
        val state = _editorState.value
        val original = state.mediaItem ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val adjustmentsJson = buildString {
                append(state.brightness.toInt())
                append(",")
                append(state.contrast.toInt())
                append(",")
                append(state.saturation.toInt())
                append(",")
                append(state.warmth.toInt())
            }
            if (asNew) {
                // Burn the styled text layer into real pixels when present; the saved
                // file then looks exactly like the preview on any viewer.
                val overlay = state.textOverlay?.takeIf { it.text.isNotBlank() }
                var finalUri = original.uri
                var finalW = original.width
                var finalH = original.height
                var finalRot = state.rotation
                var finalFlipH = state.flipH
                var finalFlipV = state.flipV
                if (overlay != null) {
                    val context = getApplication<Application>()
                    val base = decodeFullBitmap(context, original.uri)
                    if (base != null) {
                        val rotation = ((state.rotation % 360f) + 360f) % 360f
                        val baked = bakeTransform(base, rotation, state.flipH, state.flipV)
                        val composed = renderTextComposition(context, baked, overlay)
                        if (baked != composed) {
                            try {
                                baked.recycle()
                            } catch (_: Exception) {
                            }
                        }
                        if (composed != null) {
                            val editsDir = File(context.filesDir, "edits").apply { mkdirs() }
                            val outFile = File(editsDir, "edit_${System.currentTimeMillis()}.jpg")
                            var ok = false
                            try {
                                FileOutputStream(outFile).use { out ->
                                    composed.compress(Bitmap.CompressFormat.JPEG, 95, out)
                                }
                                ok = true
                            } catch (_: Exception) {
                            }
                            try {
                                composed.recycle()
                            } catch (_: Exception) {
                            }
                            if (ok) {
                                finalUri = Uri.fromFile(outFile).toString()
                                finalW = composed.width
                                finalH = composed.height
                                finalRot = 0f
                                finalFlipH = false
                                finalFlipV = false
                            }
                        }
                    }
                }
                val newMedia = MediaItem(
                    title = "Edit_${original.title}",
                    uri = finalUri,
                    isVideo = false,
                    durationMs = 0,
                    sizeBytes = original.sizeBytes,
                    dateTaken = System.currentTimeMillis(),
                    width = finalW,
                    height = finalH,
                    albumName = "Edits",
                    isFavorite = false,
                    filterName = state.selectedFilter,
                    rotationDegrees = finalRot,
                    flipHorizontal = finalFlipH,
                    flipVertical = finalFlipV,
                    editJson = adjustmentsJson
                )
                repository.insertMedia(newMedia)
                showFeedback("Saved as new photo in 'Edits' album!")
            } else {
                val updated = original.copy(
                    filterName = state.selectedFilter,
                    rotationDegrees = state.rotation,
                    flipHorizontal = state.flipH,
                    flipVertical = state.flipV,
                    editJson = adjustmentsJson
                )
                repository.updateMedia(updated)
                showFeedback("Photo changes saved!")
            }
            closeEditor()
        }
    }
}
