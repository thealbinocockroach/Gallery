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
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sqrt

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
    val strokeWidth: Float,
    // Preview-box pixels at draw time — maps box-space points onto fitted image on export.
    val anchorW: Int = 0,
    val anchorH: Int = 0
)

data class EditorShape(
    val type: ShapeType, // RECT, CIRCLE, ARROW, HIGHLIGHT
    val points: List<Pair<Float, Float>>,
    val color: Long,
    val strokeWidth: Float,
    val fillAlpha: Float,
    val id: Long = System.currentTimeMillis(),
    // Preview-box pixels at draw time — maps box-space points onto fitted image on export.
    val anchorW: Int = 0,
    val anchorH: Int = 0
)

enum class ShapeType { RECT, CIRCLE, ARROW, HIGHLIGHT }

enum class RetouchType { HEAL, BOKEH_SPOT, REDEYE }

data class RetouchOp(
    val type: RetouchType,
    val xNorm: Float, // box-relative [0..1]
    val yNorm: Float,
    val radiusNorm: Float, // fraction of box width
    val strength: Float, // 0..1
    val anchorW: Int = 0,
    val anchorH: Int = 0,
    val id: Long = System.currentTimeMillis()
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
    val warmth: Float = 0f,     // -100 to +100 (Kelvin: Cool Blue ↔ Warm Yellow)
    val highlights: Float = 0f, // -100 to +100
    val shadows: Float = 0f,    // -100 to +100
    val whites: Float = 0f,     // -100 to +100
    val blacks: Float = 0f,     // -100 to +100
    val tint: Float = 0f,       // -100 to +100 (Green ↔ Magenta)
    val vibrance: Float = 0f,   // -100 to +100
    val sharpness: Float = 0f,  // -100 to +100
    val clarity: Float = 0f,    // -100 to +100
    val denoise: Float = 0f,    // -100 to +100
    val vignette: Float = 0f,   // -100 to +100
    val levelAngle: Float = 0f, // -45 to +45 (horizon straightening)
    val perspectiveHorizontal: Float = 0f, // -45 to +45 (keystone horizontal)
    val perspectiveVertical: Float = 0f,   // -45 to +45 (keystone vertical)
    val cropRatio: String = "Freeform",
    val cropRectNorm: NormalizedCropRect = NormalizedCropRect.FULL,
    val drawingPaths: List<DrawingPath> = emptyList(),
    val textOverlay: TextOverlay? = null,
    val activeTool: String = "FILTERS",
    val filterStrength: Float = 1f, // 0f to 1f
    val showCompare: Boolean = false,
    val shapes: List<EditorShape> = emptyList(),
    val selectedAspectRatios: List<String> = listOf("Freeform", "1:1", "4:3", "16:9", "9:16", "3:2", "5:4", "4:5"),
    // HSL mixer: 8 channels (R,O,Y,G,C,B,P,M) x (hueShift°, satMul-1, lumShift), in-memory, burned on export.
    val hsl: List<Float> = List(24) { 0f },
    // Bokeh / depth-of-field.
    val bokehType: String = "OFF", // OFF, RADIAL, LINEAR
    val bokehStrength: Float = 0f, // 0..1
    val bokehCenterX: Float = 0.5f,
    val bokehCenterY: Float = 0.5f,
    // Tap-placed retouch ops (heal / spot-blur / red-eye), burned on export.
    val retouchOps: List<RetouchOp> = emptyList()
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

    // Export preferences (kept out of the undo stack — they are choices, not edits).
    private val _exportFormat = MutableStateFlow("JPEG") // JPEG, PNG, WEBP
    val exportFormat: StateFlow<String> = _exportFormat.asStateFlow()
    private val _exportQuality = MutableStateFlow(90) // 80, 90, 100
    val exportQuality: StateFlow<Int> = _exportQuality.asStateFlow()
    private val _stripExif = MutableStateFlow(false)
    val stripExif: StateFlow<Boolean> = _stripExif.asStateFlow()

    fun setExportFormat(format: String) {
        if (format in listOf("JPEG", "PNG", "WEBP")) _exportFormat.value = format
    }

    fun setExportQuality(quality: Int) {
        if (quality in listOf(80, 90, 100)) _exportQuality.value = quality
    }

    fun setStripExif(strip: Boolean) {
        _stripExif.value = strip
    }

    private fun pushEditorUndo() {
        _editorUndoStack.value = (_editorUndoStack.value + listOf(_editorState.value)).takeLast(200)
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

    fun rotateEditorCcw() {
        pushEditorUndo()
        val current = _editorState.value.rotation
        _editorState.value = _editorState.value.copy(rotation = ((current - 90f) % 360f + 360f) % 360f)
    }

    /** One-click revert of every adjustment/tool state, keeping the loaded photo. */
    fun resetAllEditor() {
        pushEditorUndo()
        val media = _editorState.value.mediaItem
        val tool = _editorState.value.activeTool
        _editorState.value = EditorState(mediaItem = media, activeTool = tool)
    }

    fun setHslChannel(channel: Int, hueShift: Float, satShift: Float, lumShift: Float) {
        if (channel !in 0..7) return
        pushEditorUndo()
        val next = _editorState.value.hsl.toMutableList()
        next[channel * 3] = hueShift.coerceIn(-180f, 180f)
        next[channel * 3 + 1] = satShift.coerceIn(-100f, 100f)
        next[channel * 3 + 2] = lumShift.coerceIn(-100f, 100f)
        _editorState.value = _editorState.value.copy(hsl = next)
    }

    fun setBokeh(type: String, strength: Float, centerX: Float = 0.5f, centerY: Float = 0.5f) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(
            bokehType = if (type in listOf("RADIAL", "LINEAR")) type else "OFF",
            bokehStrength = strength.coerceIn(0f, 1f),
            bokehCenterX = centerX.coerceIn(0f, 1f),
            bokehCenterY = centerY.coerceIn(0f, 1f)
        )
    }

    fun addRetouchOp(op: RetouchOp) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(retouchOps = _editorState.value.retouchOps + op)
    }

    fun clearRetouchOps() {
        if (_editorState.value.retouchOps.isNotEmpty()) {
            pushEditorUndo()
            _editorState.value = _editorState.value.copy(retouchOps = emptyList())
        }
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
            "3:2" -> 3f / 2f
            "5:4" -> 5f / 4f
            "4:5" -> 4f / 5f
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
        val straighten = state.levelAngle.coerceIn(-45f, 45f)
        if (cropRect.left <= 0.005f && cropRect.top <= 0.005f &&
            cropRect.right >= 0.995f && cropRect.bottom >= 0.995f &&
            rotation == 0f && !state.flipH && !state.flipV && straighten == 0f
        ) {
            showFeedback("Image already at full frame")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val context = getApplication<Application>()
                // The overlay box lives in the straightened frame, which region decode
                // can't represent — so with straightening active, full-decode, bake the
                // effective rotation, then cut the axis-aligned box from baked pixels.
                // Otherwise use the cheap region-decode path.
                val cropped = if (straighten != 0f) {
                    cropStraightened(
                        context = context,
                        uriString = original.uri,
                        rect = cropRect,
                        effectiveRotation = ((rotation + straighten) % 360f + 360f) % 360f,
                        flipH = state.flipH,
                        flipV = state.flipV
                    )
                } else {
                    // Region decode: only the cropped window is ever decoded, then the
                    // preview transform (rotation/flip) is baked into that small result.
                    cropRegionEfficient(
                        context = context,
                        uriString = original.uri,
                        rect = cropRect,
                        rotationDegrees = rotation,
                        flipH = state.flipH,
                        flipV = state.flipV
                    )
                }
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
                    levelAngle = 0f,
                    perspectiveHorizontal = 0f,
                    perspectiveVertical = 0f,
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

    /**
     * Straightened crop: the overlay box lives in the arbitrarily-rotated frame,
     * which region decode cannot represent — so full-decode, bake the effective
     * rotation (+ mirrors), then cut the axis-aligned box straight from baked pixels.
     */
    private suspend fun cropStraightened(
        context: android.content.Context,
        uriString: String,
        rect: NormalizedCropRect,
        effectiveRotation: Float,
        flipH: Boolean,
        flipV: Boolean
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val full = decodeFullBitmap(context, uriString) ?: return@withContext null
            try {
                val baked = bakeTransform(full, effectiveRotation, flipH, flipV)
                if (baked != full) {
                    try { full.recycle() } catch (_: Exception) {}
                }
                val px = rect.toPixelRect(baked.width, baked.height)
                val window = Bitmap.createBitmap(
                    baked,
                    px.left.toInt(), px.top.toInt(),
                    (px.right - px.left).toInt().coerceAtLeast(1),
                    (px.bottom - px.top).toInt().coerceAtLeast(1)
                )
                if (window != baked) {
                    try { baked.recycle() } catch (_: Exception) {}
                }
                window
            } catch (_: Exception) {
                try { full.recycle() } catch (_: Exception) {}
                null
            }
        } catch (_: Exception) {
            null
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

    /**
     * EXIF handling for export: when [strip] is on, writes a clean file with normalized
     * orientation (pixels are baked); otherwise carries capture metadata + GPS over.
     */
    private fun copyExifForExport(
        context: android.content.Context,
        srcUriString: String,
        dstFile: File,
        strip: Boolean
    ) {
        try {
            val exif = androidx.exifinterface.media.ExifInterface(dstFile.absolutePath)
            if (!strip) {
                val src = try {
                    val parsed = Uri.parse(srcUriString)
                    when (parsed.scheme) {
                        "file" -> parsed.path?.let { androidx.exifinterface.media.ExifInterface(it) }
                        "content" -> context.contentResolver.openInputStream(parsed)?.use {
                            androidx.exifinterface.media.ExifInterface(it)
                        }
                        else -> null
                    }
                } catch (_: Exception) {
                    null
                }
                if (src != null) {
                    val tags = listOf(
                        androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL,
                        androidx.exifinterface.media.ExifInterface.TAG_MAKE,
                        androidx.exifinterface.media.ExifInterface.TAG_MODEL,
                        androidx.exifinterface.media.ExifInterface.TAG_FOCAL_LENGTH,
                        androidx.exifinterface.media.ExifInterface.TAG_EXPOSURE_TIME,
                        androidx.exifinterface.media.ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY,
                        androidx.exifinterface.media.ExifInterface.TAG_FLASH,
                        androidx.exifinterface.media.ExifInterface.TAG_WHITE_BALANCE,
                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE,
                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF,
                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE,
                        androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF
                    )
                    tags.forEach { tag ->
                        src.getAttribute(tag)?.let { exif.setAttribute(tag, it) }
                    }
                }
            }
            exif.setAttribute(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL.toString()
            )
            exif.saveAttributes()
        } catch (_: Exception) {
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

    // ------------------------------------------------------------------
    // Full studio render pipeline (export + downscaled live preview share it).
    // Order: geometric bake -> tone matrix -> HSL/vignette/clarity pixel pass ->
    // denoise -> sharpen -> bokeh -> heal/redeye -> doodles -> shapes -> text.
    // ------------------------------------------------------------------

    /** Downscaled render for live preview of spatial effects (fast, ~1MP working set). */
    suspend fun renderPreviewBitmap(maxDim: Int = 1080): Bitmap? {
        val state = _editorState.value
        val original = state.mediaItem ?: return null
        if (!needsSpatialRender(state)) return null
        return try {
            renderFinalBitmap(getApplication(), state, maxDim)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Working bitmap for the embedded library canvas: tone + spatial baked at
     * display size, but NO geometry (preview layer handles it) and NO markup
     * (brush/text/emoji live as library overlays). Null when the photo is
     * pristine — then the lib source stays transparent over the base image.
     */
    suspend fun renderWorkingBitmap(maxDim: Int = 1080): Bitmap? {
        val state = _editorState.value
        if (state.mediaItem == null || !editorHasPixelWork()) return null
        return try {
            renderFinalBitmap(getApplication(), state, maxDim, bakeGeometry = false, bakeMarkup = false)
        } catch (_: Exception) {
            null
        }
    }

    /** True when tone/spatial work exists (lib source needs a baked working bitmap). */
    fun editorHasPixelWork(): Boolean {
        val s = _editorState.value
        if (s.selectedFilter != "Normal") return true
        if (s.brightness != 0f || s.contrast != 0f || s.saturation != 0f || s.warmth != 0f) return true
        if (s.tint != 0f || s.vibrance != 0f || s.highlights != 0f || s.shadows != 0f) return true
        if (s.whites != 0f || s.blacks != 0f) return true
        if (s.filterStrength != 1f) return true
        return needsSpatialRender(s)
    }

    /** True when the preview should use the downscaled CPU render (spatial/tone extras). */
    fun editorNeedsSpatialPreview(): Boolean = needsSpatialRender(_editorState.value)

    /**
     * Full-resolution working bitmap for export handoff to the embedded library
     * canvas: tone + spatial baked, no geometry (baked later), no markup (the
     * library overlays brush/text/emoji itself before capture).
     */
    suspend fun renderFullWorkingBitmap(): Bitmap? {
        val state = _editorState.value
        if (state.mediaItem == null) return null
        return try {
            renderFinalBitmap(getApplication(), state, 8192, bakeGeometry = false, bakeMarkup = false)
        } catch (_: Exception) {
            null
        }
    }

    private fun needsSpatialRender(state: EditorState): Boolean {
        if (state.sharpness != 0f || state.clarity != 0f || state.denoise != 0f) return true
        if (state.vignette != 0f) return true
        if (state.hsl.any { it != 0f }) return true
        if (state.bokehType != "OFF" && state.bokehStrength > 0f) return true
        if (state.retouchOps.isNotEmpty()) return true
        if (state.levelAngle != 0f) return true
        if (state.perspectiveHorizontal != 0f || state.perspectiveVertical != 0f) return true
        return false
    }

    private suspend fun renderFinalBitmap(
        context: android.content.Context,
        state: EditorState,
        maxDim: Int,
        bakeGeometry: Boolean = true,
        bakeMarkup: Boolean = true
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val original = state.mediaItem ?: return@withContext null
            var bmp = decodeSampledBitmap(context, original.uri, maxDim) ?: return@withContext null
            // 1. Geometric bake: rotation + flips + straighten + perspective.
            // Skipped for lib-canvas working bitmaps (preview layer handles geometry).
            if (bakeGeometry) {
                val rotation = ((state.rotation % 360f) + 360f) % 360f
                bmp = bakeGeometry(bmp, rotation, state.flipH, state.flipV, state.levelAngle,
                    state.perspectiveHorizontal, state.perspectiveVertical)
            }
            // 2. Tone matrix (filter + linear adjustments), GPU-identical math to preview.
            bmp = applyToneMatrix(bmp, state)
            // 3. Single pixel pass: HSL mixer + vignette + clarity (midtone structure).
            bmp = applyHslVignetteClarity(bmp, state)
            // 4. Denoise (3x3 blend), then sharpen (unsharp mask).
            if (state.denoise != 0f) bmp = applyDenoise(bmp, (state.denoise / 100f).coerceIn(0f, 1f))
            if (state.sharpness != 0f) bmp = applySharpness(bmp, state.sharpness / 100f)
            // 5. Bokeh depth-of-field composite.
            if (state.bokehType != "OFF" && state.bokehStrength > 0f) {
                bmp = applyBokeh(bmp, state)
            }
            // 6. Tap retouch ops.
            if (state.retouchOps.isNotEmpty()) bmp = applyRetouchOps(bmp, state)
            // 7. Doodles + shapes + text burn-in (skipped for lib-canvas working
            // bitmaps — markup lives as library overlays there).
            if (bakeMarkup) {
                bmp = burnDoodles(bmp, state)
                bmp = burnShapes(bmp, state)
                val overlay = state.textOverlay?.takeIf { it.text.isNotBlank() }
                if (overlay != null) {
                    val composed = renderTextComposition(context, bmp, overlay)
                    if (composed != null) {
                        if (composed != bmp) {
                            try { bmp.recycle() } catch (_: Exception) {}
                        }
                        bmp = composed
                    }
                }
            }
            bmp
        } catch (_: OutOfMemoryError) {
            try { System.gc() } catch (_: Exception) {}
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(
        context: android.content.Context,
        uriString: String,
        maxDim: Int
    ): Bitmap? {
        return try {
            val parsed = Uri.parse(uriString)
            if (parsed.scheme != "file" && parsed.scheme != "content") {
                return decodeFullBitmapBlocking(context, uriString)
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openImageStream(context, parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            val w = bounds.outWidth
            val h = bounds.outHeight
            if (w <= 0 || h <= 0) return decodeFullBitmapBlocking(context, uriString)
            while ((w / sample) > maxDim || (h / sample) > maxDim) sample *= 2
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            openImageStream(context, parsed)?.use { BitmapFactory.decodeStream(it, null, opts) }
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeFullBitmapBlocking(context: android.content.Context, uriString: String): Bitmap? {
        return try {
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
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /** Geometric bake: 90°-step rotation + mirrors + arbitrary straighten + keystone warp. */
    private fun bakeGeometry(
        src: Bitmap,
        rotation: Float,
        flipH: Boolean,
        flipV: Boolean,
        straightenDeg: Float,
        perspH: Float,
        perspV: Float
    ): Bitmap {
        var bmp = src
        val totalRot = (rotation + straightenDeg) % 360f
        if (totalRot != 0f || flipH || flipV) {
            val m = Matrix()
            if (totalRot != 0f) m.postRotate(totalRot)
            if (flipH || flipV) m.postScale(if (flipH) -1f else 1f, if (flipV) -1f else 1f)
            val out = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            if (out != bmp) {
                try { bmp.recycle() } catch (_: Exception) {}
            }
            bmp = out
        }
        // Keystone correction: PERSP-V narrows/widens the TOP edge (vertical tilt),
        // PERSP-H narrows/widens the LEFT edge (horizontal tilt). Combined quad
        // reduces exactly to each single-axis case; insets stay <=15% so no fold.
        val it = (perspV / 45f).coerceIn(-1f, 1f) * 0.15f
        val il = (perspH / 45f).coerceIn(-1f, 1f) * 0.15f
        if (it != 0f || il != 0f) {
            val w = bmp.width.toFloat()
            val h = bmp.height.toFloat()
            val srcPts = floatArrayOf(0f, 0f, w, 0f, 0f, h, w, h)
            // Pure vertical (il=0): top edge narrows, bottom fixed. Pure horizontal
            // (it=0): left edge tilts, right edge fixed.
            val dst = floatArrayOf(
                w * it, h * il,
                w - w * it, 0f,
                0f, h - h * il,
                w, h
            )
            val m = Matrix()
            if (!m.setPolyToPoly(srcPts, 0, dst, 0, 4)) return bmp
            val warped = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            try {
                android.graphics.Canvas(warped).drawBitmap(bmp, m, android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG))
            } catch (_: Exception) {
                try { warped.recycle() } catch (_: Exception) {}
                return bmp
            }
            try { bmp.recycle() } catch (_: Exception) {}
            bmp = warped
        }
        return bmp
    }

    /** Applies the filter + linear adjustments with the exact preview math (native canvas). */
    private fun applyToneMatrix(src: Bitmap, state: EditorState): Bitmap {
        return try {
            val arr = com.example.ui.screens.FilterHelper.buildAdjustmentMatrix(
                state.selectedFilter, state.brightness, state.contrast, state.saturation,
                state.warmth, state.tint, state.highlights, state.shadows, state.whites,
                state.blacks, state.vibrance, state.filterStrength
            )
            val cm = android.graphics.ColorMatrix(arr)
            val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = android.graphics.ColorMatrixColorFilter(cm)
            }
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(out).drawBitmap(src, 0f, 0f, paint)
            try { src.recycle() } catch (_: Exception) {}
            out
        } catch (_: Exception) {
            src
        }
    }

    /** One IntArray pass: HSL channel mixer + vignette + clarity (midtone structure). */
    private fun applyHslVignetteClarity(src: Bitmap, state: EditorState): Bitmap {
        val hasHsl = state.hsl.any { it != 0f }
        val vig = state.vignette / 100f
        val clarity = state.clarity / 100f
        if (!hasHsl && vig == 0f && clarity == 0f) return src
        return try {
            val w = src.width
            val h = src.height
            val px = IntArray(w * h)
            src.getPixels(px, 0, w, 0, 0, w, h)
            val cx = w / 2f
            val cy = h / 2f
            val maxD = sqrt(cx * cx + cy * cy)
            val hsv = FloatArray(3)
            var i = 0
            while (i < px.size) {
                val c = px[i]
                var r = (c shr 16) and 0xFF
                var g = (c shr 8) and 0xFF
                var b = c and 0xFF
                if (hasHsl) {
                    android.graphics.Color.RGBToHSV(r, g, b, hsv)
                    val ch = hslChannel(hsv[0])
                    val base = ch * 3
                    var hh = hsv[0] + state.hsl[base]
                    hh = ((hh % 360f) + 360f) % 360f
                    var ss = (hsv[1] * (1f + state.hsl[base + 1] / 100f)).coerceIn(0f, 1f)
                    var vv = (hsv[2] + state.hsl[base + 2] / 100f * (if (state.hsl[base + 2] >= 0) (1f - hsv[2]) else hsv[2]))
                        .coerceIn(0f, 1f)
                    val mixed = android.graphics.Color.HSVToColor(floatArrayOf(hh, ss, vv))
                    r = (mixed shr 16) and 0xFF
                    g = (mixed shr 8) and 0xFF
                    b = mixed and 0xFF
                }
                // Clarity: midtone S-curve (no spatial pass — structure lite).
                if (clarity != 0f) {
                    val midW = 1f - abs((r + g + b) / (3f * 255f) * 2f - 1f)
                    val lift = { v: Int ->
                        val n = v / 255f
                        val s = n * n * (3f - 2f * n)
                        ((n + clarity * 0.28f * (n - s) * midW).coerceIn(0f, 1f) * 255f).toInt()
                    }
                    r = lift(r); g = lift(g); b = lift(b)
                }
                // Vignette: radial falloff, strength signed (negative lightens edges).
                if (vig != 0f) {
                    val x = (i % w).toFloat()
                    val y = (i / w).toFloat()
                    val d = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maxD
                    val fall = (d * d * (0.55f + 0.45f * d)).coerceIn(0f, 1f)
                    val f = (1f - vig * fall).coerceAtLeast(0f)
                    r = (r * f).toInt().coerceIn(0, 255)
                    g = (g * f).toInt().coerceIn(0, 255)
                    b = (b * f).toInt().coerceIn(0, 255)
                }
                px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
                i++
            }
            src.setPixels(px, 0, w, 0, 0, w, h)
            src
        } catch (_: Exception) {
            src
        }
    }

    private fun hslChannel(hueDeg: Float): Int {
        val h = ((hueDeg % 360f) + 360f) % 360f
        return when {
            h < 15f || h >= 345f -> 0 // Red
            h < 45f -> 1 // Orange
            h < 75f -> 2 // Yellow
            h < 165f -> 3 // Green
            h < 195f -> 4 // Cyan
            h < 255f -> 5 // Blue
            h < 290f -> 6 // Purple
            else -> 7 // Magenta
        }
    }

    /** Fast separable box blur used by denoise/sharpen/bokeh working copies. */
    private fun boxBlur(src: Bitmap, radius: Int): Bitmap {
        val r = radius.coerceIn(1, 25)
        val w = src.width
        val h = src.height
        return try {
            val a = IntArray(w * h)
            val b = IntArray(w * h)
            src.getPixels(a, 0, w, 0, 0, w, h)
            // Horizontal pass.
            var y = 0
            while (y < h) {
                var rs = 0; var gs = 0; var bs = 0
                var x = -r
                while (x <= r) {
                    val c = a[y * w + x.coerceIn(0, w - 1)]
                    rs += (c shr 16) and 0xFF; gs += (c shr 8) and 0xFF; bs += c and 0xFF
                    x++
                }
                x = 0
                while (x < w) {
                    val n = (min(x + r, w - 1) - maxOf(x - r, 0) + 1).coerceAtLeast(1)
                    b[y * w + x] = (0xFF shl 24) or ((rs / n) shl 16) or ((gs / n) shl 8) or (bs / n)
                    val sub = a[y * w + maxOf(x - r, 0)]
                    val add = a[y * w + min(x + r + 1, w - 1)]
                    rs += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                    gs += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                    bs += (add and 0xFF) - (sub and 0xFF)
                    x++
                }
                y++
            }
            // Vertical pass into a new bitmap.
            val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = IntArray(w * h)
            var x2 = 0
            while (x2 < w) {
                var rs = 0; var gs = 0; var bs = 0
                var y2 = -r
                while (y2 <= r) {
                    val p = b[y2.coerceIn(0, h - 1) * w + x2]
                    rs += (p shr 16) and 0xFF; gs += (p shr 8) and 0xFF; bs += p and 0xFF
                    y2++
                }
                y2 = 0
                while (y2 < h) {
                    val n = (min(y2 + r, h - 1) - maxOf(y2 - r, 0) + 1).coerceAtLeast(1)
                    c[y2 * w + x2] = (0xFF shl 24) or ((rs / n) shl 16) or ((gs / n) shl 8) or (bs / n)
                    val sub = b[maxOf(y2 - r, 0) * w + x2]
                    val add = b[min(y2 + r + 1, h - 1) * w + x2]
                    rs += ((add shr 16) and 0xFF) - ((sub shr 16) and 0xFF)
                    gs += ((add shr 8) and 0xFF) - ((sub shr 8) and 0xFF)
                    bs += (add and 0xFF) - (sub and 0xFF)
                    y2++
                }
                x2++
            }
            out.setPixels(c, 0, w, 0, 0, w, h)
            out
        } catch (_: Exception) {
            src
        }
    }

    private fun applyDenoise(src: Bitmap, amount01: Float): Bitmap {
        if (amount01 <= 0f) return src
        return try {
            val blurred = boxBlur(src, 2)
            if (blurred == src) return src
            val w = src.width
            val h = src.height
            val a = IntArray(w * h)
            val bl = IntArray(w * h)
            src.getPixels(a, 0, w, 0, 0, w, h)
            blurred.getPixels(bl, 0, w, 0, 0, w, h)
            try { blurred.recycle() } catch (_: Exception) {}
            var i = 0
            while (i < a.size) {
                val o = a[i]; val n = bl[i]
                val r = (((o shr 16) and 0xFF) + (((n shr 16) and 0xFF) - ((o shr 16) and 0xFF)) * amount01).toInt().coerceIn(0, 255)
                val g = (((o shr 8) and 0xFF) + (((n shr 8) and 0xFF) - ((o shr 8) and 0xFF)) * amount01).toInt().coerceIn(0, 255)
                val bb = ((o and 0xFF) + ((n and 0xFF) - (o and 0xFF)) * amount01).toInt().coerceIn(0, 255)
                a[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bb
                i++
            }
            src.setPixels(a, 0, w, 0, 0, w, h)
            src
        } catch (_: Exception) {
            src
        }
    }

    /** Unsharp mask with threshold protection: only edges above threshold sharpen. */
    private fun applySharpness(src: Bitmap, amountSigned: Float): Bitmap {
        if (amountSigned == 0f) return src
        return try {
            val amt = abs(amountSigned).coerceAtMost(1f)
            val threshold = 6
            val blurred = boxBlur(src, 2)
            if (blurred == src) return src
            val w = src.width
            val h = src.height
            val a = IntArray(w * h)
            val bl = IntArray(w * h)
            src.getPixels(a, 0, w, 0, 0, w, h)
            blurred.getPixels(bl, 0, w, 0, 0, w, h)
            try { blurred.recycle() } catch (_: Exception) {}
            val sign = if (amountSigned >= 0f) 1f else -1f
            var i = 0
            while (i < a.size) {
                val o = a[i]; val n = bl[i]
                val dr = ((o shr 16) and 0xFF) - ((n shr 16) and 0xFF)
                val dg = ((o shr 8) and 0xFF) - ((n shr 8) and 0xFF)
                val db = (o and 0xFF) - (n and 0xFF)
                // Threshold gate on edge energy keeps flat areas (sky) clean.
                val energy = abs(dr) + abs(dg) + abs(db)
                if (energy > threshold * 3) {
                    val k = sign * amt * 0.9f
                    val r = ((o shr 16) and 0xFF) + (dr * k).toInt()
                    val g = ((o shr 8) and 0xFF) + (dg * k).toInt()
                    val bb = (o and 0xFF) + (db * k).toInt()
                    a[i] = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bb.coerceIn(0, 255)
                }
                i++
            }
            src.setPixels(a, 0, w, 0, 0, w, h)
            src
        } catch (_: Exception) {
            src
        }
    }

    /** Depth-of-field composite: blur computed at half res (low-frequency safe), mask at full res. */
    private fun applyBokeh(src: Bitmap, state: EditorState): Bitmap {
        return try {
            val strength = state.bokehStrength.coerceIn(0f, 1f)
            if (strength <= 0f) return src
            val w = src.width
            val h = src.height
            val half = Bitmap.createScaledBitmap(src, (w / 2).coerceAtLeast(1), (h / 2).coerceAtLeast(1), true)
            val blurredHalf = boxBlur(half, (2 + (strength * 10f).toInt()).coerceIn(2, 12))
            if (blurredHalf != half) {
                try { half.recycle() } catch (_: Exception) {}
            }
            val blurredFull = Bitmap.createScaledBitmap(blurredHalf, w, h, true)
            try { blurredHalf.recycle() } catch (_: Exception) {}
            val a = IntArray(w * h)
            val bl = IntArray(w * h)
            src.getPixels(a, 0, w, 0, 0, w, h)
            blurredFull.getPixels(bl, 0, w, 0, 0, w, h)
            try { blurredFull.recycle() } catch (_: Exception) {}
            val fx = state.bokehCenterX * w
            val fy = state.bokehCenterY * h
            val maxD = sqrt((w / 2f) * (w / 2f) + (h / 2f) * (h / 2f))
            var i = 0
            while (i < a.size) {
                val x = (i % w).toFloat()
                val y = (i / w).toFloat()
                val m: Float = if (state.bokehType == "LINEAR") {
                    // Tilt-shift band around the focus row.
                    ((abs(y - fy) / (h * 0.28f)).coerceIn(0f, 1f)).let { t -> t * t * (3f - 2f * t) }
                } else {
                    // Radial: sharp core, blurred surroundings.
                    val d = sqrt((x - fx) * (x - fx) + (y - fy) * (y - fy)) / maxD
                    ((d - 0.18f) / 0.45f).coerceIn(0f, 1f).let { t -> t * t * (3f - 2f * t) }
                }
                val k = (m * strength).coerceIn(0f, 1f)
                if (k > 0f) {
                    val o = a[i]; val n = bl[i]
                    val r = (((o shr 16) and 0xFF) + (((n shr 16) and 0xFF) - ((o shr 16) and 0xFF)) * k).toInt().coerceIn(0, 255)
                    val g = (((o shr 8) and 0xFF) + (((n shr 8) and 0xFF) - ((o shr 8) and 0xFF)) * k).toInt().coerceIn(0, 255)
                    val bb = ((o and 0xFF) + ((n and 0xFF) - (o and 0xFF)) * k).toInt().coerceIn(0, 255)
                    a[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bb
                }
                i++
            }
            src.setPixels(a, 0, w, 0, 0, w, h)
            src
        } catch (_: Exception) {
            src
        }
    }

    /** Tap retouch: heal (blurred-patch fill), spot blur, red-eye neutralization. */
    private fun applyRetouchOps(src: Bitmap, state: EditorState): Bitmap {
        if (state.retouchOps.isEmpty()) return src
        return try {
            val w = src.width
            val h = src.height
            val softBlur = boxBlur(src, 3)
            val useBlur = softBlur != src
            val px = IntArray(w * h)
            src.getPixels(px, 0, w, 0, 0, w, h)
            val bpx = if (useBlur) IntArray(w * h).also {
                try { softBlur.getPixels(it, 0, w, 0, 0, w, h) } catch (_: Exception) {}
            } else null
            state.retouchOps.forEach { op ->
                // Box-relative op coords -> image fractions (same Fit compensation as text).
                val anchorW = op.anchorW.takeIf { it > 0 } ?: w
                val anchorH = op.anchorH.takeIf { it > 0 } ?: h
                val fitted = fitRect(anchorW.toFloat(), anchorH.toFloat(), w.toFloat() / h.toFloat())
                val ix = (0.5f + (op.xNorm - 0.5f) * (anchorW / fitted.width())).coerceIn(0f, 1f)
                val iy = (0.5f + (op.yNorm - 0.5f) * (anchorH / fitted.height())).coerceIn(0f, 1f)
                val rcx = (ix * w).toInt()
                val rcy = (iy * h).toInt()
                val rad = (op.radiusNorm * anchorW * (w.toFloat() / anchorW)).toInt().coerceAtLeast(2)
                val r0x = (rcx - rad).coerceAtLeast(0)
                val r0y = (rcy - rad).coerceAtLeast(0)
                val r1x = (rcx + rad).coerceAtMost(w - 1)
                val r1y = (rcy + rad).coerceAtMost(h - 1)
                var y = r0y
                while (y <= r1y) {
                    var x = r0x
                    while (x <= r1x) {
                        val dx = x - rcx
                        val dy = y - rcy
                        if (dx * dx + dy * dy <= rad * rad) {
                            val idx = y * w + x
                            when (op.type) {
                                RetouchType.HEAL -> {
                                    // Content-aware lite: fill from the blurred twin (kills dust/spots,
                                    // keeps tone), blended at the rim to avoid seams.
                                    if (bpx != null) {
                                        val edge = (sqrt((dx * dx + dy * dy).toFloat()) / rad).coerceIn(0f, 1f)
                                        val k = (op.strength * (1f - edge * edge)).coerceIn(0f, 1f)
                                        if (k > 0f) {
                                            val o = px[idx]; val n = bpx[idx]
                                            val r = (((o shr 16) and 0xFF) + (((n shr 16) and 0xFF) - ((o shr 16) and 0xFF)) * k).toInt().coerceIn(0, 255)
                                            val g = (((o shr 8) and 0xFF) + (((n shr 8) and 0xFF) - ((o shr 8) and 0xFF)) * k).toInt().coerceIn(0, 255)
                                            val bb = ((o and 0xFF) + ((n and 0xFF) - (o and 0xFF)) * k).toInt().coerceIn(0, 255)
                                            px[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bb
                                        }
                                    }
                                }
                                RetouchType.BOKEH_SPOT -> {
                                    if (bpx != null) {
                                        val o = px[idx]; val n = bpx[idx]
                                        val k = op.strength.coerceIn(0f, 1f)
                                        val r = (((o shr 16) and 0xFF) + (((n shr 16) and 0xFF) - ((o shr 16) and 0xFF)) * k).toInt().coerceIn(0, 255)
                                        val g = (((o shr 8) and 0xFF) + (((n shr 8) and 0xFF) - ((o shr 8) and 0xFF)) * k).toInt().coerceIn(0, 255)
                                        val bb = ((o and 0xFF) + ((n and 0xFF) - (o and 0xFF)) * k).toInt().coerceIn(0, 255)
                                        px[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bb
                                    }
                                }
                                RetouchType.REDEYE -> {
                                    val o = px[idx]
                                    val r = (o shr 16) and 0xFF
                                    val g = (o shr 8) and 0xFF
                                    val b = o and 0xFF
                                    // Flash pupils: red dominates green/blue.
                                    if (r > 90 && r > (g * 1.4f).toInt() && r > (b * 1.4f).toInt()) {
                                        val dark = ((g + b) / 2 * 0.45f).toInt().coerceIn(0, 255)
                                        px[idx] = (0xFF shl 24) or (dark shl 16) or (dark shl 8) or dark
                                    }
                                }
                            }
                        }
                        x++
                    }
                    y++
                }
            }
            src.setPixels(px, 0, w, 0, 0, w, h)
            if (useBlur) {
                try { softBlur.recycle() } catch (_: Exception) {}
            }
            src
        } catch (_: Exception) {
            src
        }
    }

    /** Maps box-space drawing points onto fitted-image pixels (shared Fit math). */
    private fun mapBoxPointToImage(
        px: Float, py: Float, anchorW: Int, anchorH: Int, outW: Int, outH: Int
    ): Pair<Float, Float> {
        if (anchorW <= 0 || anchorH <= 0) {
            // No anchor recorded (legacy path): assume points are already image pixels.
            return Pair(px.coerceIn(0f, outW.toFloat()), py.coerceIn(0f, outH.toFloat()))
        }
        val fitted = fitRect(anchorW.toFloat(), anchorH.toFloat(), outW.toFloat() / outH.toFloat())
        val ix = (0.5f + (px / anchorW - 0.5f) * (anchorW / fitted.width())).coerceIn(0f, 1f)
        val iy = (0.5f + (py / anchorH - 0.5f) * (anchorH / fitted.height())).coerceIn(0f, 1f)
        return Pair(ix * outW, iy * outH)
    }

    private fun burnDoodles(src: Bitmap, state: EditorState): Bitmap {
        if (state.drawingPaths.isEmpty()) return src
        return try {
            val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
            val canvas = android.graphics.Canvas(out)
            val scaleOf = { anchorW: Int -> out.width.toFloat() / anchorW.coerceAtLeast(1).toFloat() }
            state.drawingPaths.forEach { dp ->
                if (dp.points.size < 2) return@forEach
                val s = scaleOf(dp.anchorW)
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeCap = android.graphics.Paint.Cap.ROUND
                    strokeJoin = android.graphics.Paint.Join.ROUND
                    strokeWidth = (dp.strokeWidth * s).coerceAtLeast(1f)
                    color = dp.color.toInt()
                }
                val path = android.graphics.Path()
                dp.points.forEachIndexed { i, p ->
                    val mapped = if (dp.anchorW > 0 && dp.anchorH > 0) {
                        mapBoxPointToImage(p.first, p.second, dp.anchorW, dp.anchorH, out.width, out.height)
                    } else {
                        Pair(p.first * s, p.second * s)
                    }
                    if (i == 0) path.moveTo(mapped.first, mapped.second)
                    else path.lineTo(mapped.first, mapped.second)
                }
                canvas.drawPath(path, paint)
            }
            try { src.recycle() } catch (_: Exception) {}
            out
        } catch (_: Exception) {
            src
        }
    }

    private fun burnShapes(src: Bitmap, state: EditorState): Bitmap {
        if (state.shapes.isEmpty()) return src
        return try {
            val out = src.copy(Bitmap.Config.ARGB_8888, true) ?: return src
            val canvas = android.graphics.Canvas(out)
            state.shapes.forEach { shape ->
                if (shape.points.size < 2) return@forEach
                val s = out.width.toFloat() / shape.anchorW.coerceAtLeast(1).toFloat()
                fun mapPt(p: Pair<Float, Float>): Pair<Float, Float> {
                    return if (shape.anchorW > 0 && shape.anchorH > 0) {
                        mapBoxPointToImage(p.first, p.second, shape.anchorW, shape.anchorH, out.width, out.height)
                    } else {
                        Pair(p.first * s, p.second * s)
                    }
                }
                val a = mapPt(shape.points.first())
                val b = mapPt(shape.points.last())
                val left = minOf(a.first, b.first)
                val top = minOf(a.second, b.second)
                val right = maxOf(a.first, b.first)
                val bottom = maxOf(a.second, b.second)
                val baseColor = shape.color.toInt()
                val strokePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = (shape.strokeWidth * s).coerceAtLeast(2f)
                    color = baseColor
                }
                val fillPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    style = android.graphics.Paint.Style.FILL
                    color = baseColor
                    alpha = (shape.fillAlpha.coerceIn(0f, 1f) * 255).toInt()
                }
                when (shape.type) {
                    ShapeType.RECT -> {
                        if (shape.fillAlpha > 0f) canvas.drawRect(left, top, right, bottom, fillPaint)
                        canvas.drawRect(left, top, right, bottom, strokePaint)
                    }
                    ShapeType.CIRCLE -> {
                        val cx = (left + right) / 2f
                        val cy = (top + bottom) / 2f
                        val rad = minOf(right - left, bottom - top) / 2f
                        if (shape.fillAlpha > 0f) canvas.drawCircle(cx, cy, rad, fillPaint)
                        canvas.drawCircle(cx, cy, rad, strokePaint)
                    }
                    ShapeType.HIGHLIGHT -> {
                        val hl = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                            style = android.graphics.Paint.Style.FILL
                            color = baseColor
                            alpha = ((0.35f + shape.fillAlpha * 0.4f).coerceIn(0f, 0.75f) * 255).toInt()
                        }
                        canvas.drawRect(left, top, right, bottom, hl)
                    }
                    ShapeType.ARROW -> {
                        canvas.drawLine(a.first, a.second, b.first, b.second, strokePaint)
                        val angle = kotlin.math.atan2(b.second - a.second, b.first - a.first)
                        val headLen = (24f * s).coerceAtLeast(12f)
                        val headAng = 0.5f
                        listOf(angle + Math.PI.toFloat() - headAng, angle + Math.PI.toFloat() + headAng).forEach { ha ->
                            canvas.drawLine(
                                b.first, b.second,
                                b.first + kotlin.math.cos(ha) * headLen,
                                b.second + kotlin.math.sin(ha) * headLen,
                                strokePaint
                            )
                        }
                    }
                }
            }
            try { src.recycle() } catch (_: Exception) {}
            out
        } catch (_: Exception) {
            src
        }
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

    fun updateFullAdjustments(
        brightness: Float, contrast: Float, saturation: Float, warmth: Float,
        highlights: Float, shadows: Float, whites: Float, blacks: Float,
        tint: Float, vibrance: Float, sharpness: Float, clarity: Float,
        denoise: Float, vignette: Float
    ) {
        pushEditorUndo()
        val prev = _editorState.value
        _editorState.value = prev.copy(
            brightness = brightness, contrast = contrast, saturation = saturation, warmth = warmth,
            highlights = highlights, shadows = shadows, whites = whites, blacks = blacks,
            tint = tint, vibrance = vibrance, sharpness = sharpness, clarity = clarity,
            denoise = denoise, vignette = vignette
        )
    }

    fun setHighlights(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(highlights = value)
    }

    fun setShadows(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(shadows = value)
    }

    fun setWhites(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(whites = value)
    }

    fun setBlacks(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(blacks = value)
    }

    fun setTint(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(tint = value)
    }

    fun setVibrance(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(vibrance = value)
    }

    fun setSharpness(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(sharpness = value)
    }

    fun setClarity(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(clarity = value)
    }

    fun setDenoise(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(denoise = value)
    }

    fun setVignette(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(vignette = value)
    }

    fun setLevelAngle(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(levelAngle = value.coerceIn(-45f, 45f))
    }

    fun setPerspectiveHorizontal(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(perspectiveHorizontal = value.coerceIn(-45f, 45f))
    }

    fun setPerspectiveVertical(value: Float) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(perspectiveVertical = value.coerceIn(-45f, 45f))
    }

    fun setFilterStrength(value: Float) {
        _editorState.value = _editorState.value.copy(filterStrength = value.coerceIn(0f, 1f))
    }

    fun toggleCompare() {
        _editorState.value = _editorState.value.copy(showCompare = !_editorState.value.showCompare)
    }

    fun addShape(shape: EditorShape) {
        pushEditorUndo()
        _editorState.value = _editorState.value.copy(shapes = _editorState.value.shapes + shape)
    }

    fun clearShapes() {
        if (_editorState.value.shapes.isNotEmpty()) {
            pushEditorUndo()
            _editorState.value = _editorState.value.copy(shapes = emptyList())
        }
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

    /**
     * @param composed library-captured bitmap (working pixels + brush/text/emoji
     * markup) when the embedded canvas holds edits; null falls back to the legacy
     * full render from parametric state.
     */
    fun saveEditedPhoto(asNew: Boolean = true, composed: Bitmap? = null) {
        val state = _editorState.value
        val original = state.mediaItem ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val adjustmentsJson = buildString {
                append(state.brightness.toInt()).append(",")
                append(state.contrast.toInt()).append(",")
                append(state.saturation.toInt()).append(",")
                append(state.warmth.toInt()).append(",")
                append(state.highlights.toInt()).append(",")
                append(state.shadows.toInt()).append(",")
                append(state.whites.toInt()).append(",")
                append(state.blacks.toInt()).append(",")
                append(state.tint.toInt()).append(",")
                append(state.vibrance.toInt()).append(",")
                append(state.sharpness.toInt()).append(",")
                append(state.clarity.toInt()).append(",")
                append(state.denoise.toInt()).append(",")
                append(state.vignette.toInt())
            }
            val format = _exportFormat.value
            val quality = _exportQuality.value
            val strip = _stripExif.value
            // Full-res studio render: geometry + tone + detail + retouch + markup + text.
            // Falls back to the legacy metadata-only path if rendering fails.
            val context = getApplication<Application>()
            showFeedback("Rendering…")
            // Prefer the library composition (working pixels + markup); it still needs
            // display-geometry baking. Otherwise fall back to the legacy full render.
            var rendered: Bitmap? = null
            var renderedOwned = false
            if (composed != null && composed.width > 0 && composed.height > 0) {
                val rotation = ((state.rotation % 360f) + 360f) % 360f
                rendered = try {
                    bakeGeometry(
                        composed, rotation, state.flipH, state.flipV,
                        state.levelAngle, state.perspectiveHorizontal, state.perspectiveVertical
                    )
                } catch (_: Exception) {
                    null
                }
                // bakeGeometry recycles the source when it creates a new bitmap.
                // On bake failure leave composed for GC and fall through to the
                // legacy render so the photo itself is still saved (minus markup).
                if (rendered != null) {
                    renderedOwned = true
                }
            }
            if (rendered == null && !renderedOwned) {
                rendered = renderFinalBitmap(context, state, maxDim = 8192)
            }
            if (asNew) {
                if (rendered != null) {
                    val ext = when (format) {
                        "PNG" -> "png"
                        "WEBP" -> "webp"
                        else -> "jpg"
                    }
                    val editsDir = File(context.filesDir, "edits").apply { mkdirs() }
                    val outFile = File(editsDir, "edit_${System.currentTimeMillis()}.$ext")
                    var ok = false
                    try {
                        FileOutputStream(outFile).use { out ->
                            when (format) {
                                "PNG" -> rendered.compress(Bitmap.CompressFormat.PNG, 100, out)
                                "WEBP" -> rendered.compress(Bitmap.CompressFormat.WEBP, quality, out)
                                else -> rendered.compress(Bitmap.CompressFormat.JPEG, quality, out)
                            }
                        }
                        copyExifForExport(context, original.uri, outFile, strip)
                        ok = true
                    } catch (_: Exception) {}
                    val outW = rendered.width
                    val outH = rendered.height
                    try { rendered.recycle() } catch (_: Exception) {}
                    if (ok) {
                        val newMedia = MediaItem(
                            title = "Edit_${original.title}",
                            uri = Uri.fromFile(outFile).toString(),
                            isVideo = false,
                            durationMs = 0,
                            sizeBytes = outFile.length(),
                            dateTaken = System.currentTimeMillis(),
                            width = outW,
                            height = outH,
                            albumName = "Edits",
                            isFavorite = false,
                            filterName = state.selectedFilter,
                            rotationDegrees = 0f,
                            flipHorizontal = false,
                            flipVertical = false,
                            editJson = adjustmentsJson
                        )
                        repository.insertMedia(newMedia)
                        showFeedback("Saved as new photo in 'Edits' album!")
                        closeEditor()
                        return@launch
                    }
                }
                // Render failed: legacy metadata-only save so nothing is lost.
                val newMedia = MediaItem(
                    title = "Edit_${original.title}",
                    uri = original.uri,
                    isVideo = false,
                    durationMs = 0,
                    sizeBytes = original.sizeBytes,
                    dateTaken = System.currentTimeMillis(),
                    width = original.width,
                    height = original.height,
                    albumName = "Edits",
                    isFavorite = false,
                    filterName = state.selectedFilter,
                    rotationDegrees = state.rotation,
                    flipHorizontal = state.flipH,
                    flipVertical = state.flipV,
                    editJson = adjustmentsJson
                )
                repository.insertMedia(newMedia)
                showFeedback("Saved as new photo in 'Edits' album!")
            } else {
                // Save Over Original (non-destructive): pixels replaced by the render,
                // parametric history stays in editJson for future reversion.
                if (rendered != null) {
                    val editsDir = File(context.filesDir, "edits").apply { mkdirs() }
                    val outFile = File(editsDir, "overwrite_${original.id}_${System.currentTimeMillis()}.jpg")
                    var ok = false
                    try {
                        FileOutputStream(outFile).use { out ->
                            rendered.compress(Bitmap.CompressFormat.JPEG, quality, out)
                        }
                        copyExifForExport(context, original.uri, outFile, strip)
                        ok = true
                    } catch (_: Exception) {}
                    val outW = rendered.width
                    val outH = rendered.height
                    try { rendered.recycle() } catch (_: Exception) {}
                    if (ok) {
                        val updated = original.copy(
                            uri = Uri.fromFile(outFile).toString(),
                            width = outW,
                            height = outH,
                            sizeBytes = outFile.length(),
                            filterName = state.selectedFilter,
                            rotationDegrees = 0f,
                            flipHorizontal = false,
                            flipVertical = false,
                            editJson = (adjustmentsJson ?: "") + "|prev:${original.uri}"
                        )
                        repository.updateMedia(updated)
                        showFeedback("Original updated — history kept for reversion!")
                        closeEditor()
                        return@launch
                    }
                }
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
