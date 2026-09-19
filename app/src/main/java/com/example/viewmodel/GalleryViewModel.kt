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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GalleryTab {
    PICTURES,
    ALBUMS,
    VIDEOS,
    HUB
}

enum class ActiveScreen {
    MAIN,
    FULLSCREEN_VIEWER,
    VIDEO_PLAYER,
    PHOTO_EDITOR,
    TRASH_BIN,
    ALBUM_DETAIL,
    FONTS_REPO
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
    val xOffsetNorm: Float = 0.5f,
    val yOffsetNorm: Float = 0.5f
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
    val cropRatio: String = "Original", // Original, 1:1, 4:3, 16:9
    val drawingPaths: List<DrawingPath> = emptyList(),
    val textOverlay: TextOverlay? = null,
    val activeTool: String = "FILTERS" // CROP, ADJUST, FILTERS, DOODLE, TEXT
)

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GalleryRepository

    init {
        val db = AppDatabase.getDatabase(application, viewModelScope)
        repository = GalleryRepository(db.mediaDao())
    }

    // Flows from DB
    val allMedia: StateFlow<List<MediaItem>> = repository.allMedia
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

    // Navigation State
    private val _currentTab = MutableStateFlow(GalleryTab.PICTURES)
    val currentTab: StateFlow<GalleryTab> = _currentTab.asStateFlow()

    private val _currentScreen = MutableStateFlow(ActiveScreen.MAIN)
    val currentScreen: StateFlow<ActiveScreen> = _currentScreen.asStateFlow()

    private val _selectedAlbumName = MutableStateFlow<String?>(null)
    val selectedAlbumName: StateFlow<String?> = _selectedAlbumName.asStateFlow()

    // Grid Scaling (Finger gestures: 2, 3, 4, 5 columns)
    private val _gridColumnCount = MutableStateFlow(3)
    val gridColumnCount: StateFlow<Int> = _gridColumnCount.asStateFlow()

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

    // Toast / Feedback message
    private val _feedbackMessage = MutableStateFlow<String?>(null)
    val feedbackMessage: StateFlow<String?> = _feedbackMessage.asStateFlow()

    fun clearFeedback() {
        _feedbackMessage.value = null
    }

    fun showFeedback(msg: String) {
        _feedbackMessage.value = msg
    }

    // Tab Navigation
    fun selectTab(tab: GalleryTab) {
        _currentTab.value = tab
        if (_isSelectionMode.value) {
            clearSelection()
        }
    }

    fun setGridColumnCount(count: Int) {
        _gridColumnCount.value = count.coerceIn(2, 5)
    }

    fun zoomInGrid() {
        if (_gridColumnCount.value > 2) {
            _gridColumnCount.value -= 1
        }
    }

    fun zoomOutGrid() {
        if (_gridColumnCount.value < 5) {
            _gridColumnCount.value += 1
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
            // Keep in selection mode or let user control
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

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Open Fullscreen Viewer
    fun openViewer(media: MediaItem, contextList: List<MediaItem>) {
        _viewerMediaList.value = contextList
        val index = contextList.indexOfFirst { it.id == media.id }
        _activeMediaIndex.value = if (index >= 0) index else 0

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
        _currentScreen.value = ActiveScreen.MAIN
    }

    // Video Player
    fun openVideoPlayer(media: MediaItem) {
        _currentVideoItem.value = media
        _currentScreen.value = ActiveScreen.VIDEO_PLAYER
    }

    fun closeVideoPlayer() {
        _currentVideoItem.value = null
        _currentScreen.value = ActiveScreen.MAIN
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
            showFeedback("Moved ${ids.size} items to $albumName")
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
            repository.deletePermanently(id)
            showFeedback("Permanently deleted")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.emptyTrash()
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
        _currentScreen.value = ActiveScreen.PHOTO_EDITOR
    }

    fun closeEditor() {
        _currentScreen.value = if (currentViewerMedia.value != null) {
            ActiveScreen.FULLSCREEN_VIEWER
        } else {
            ActiveScreen.MAIN
        }
    }

    fun setEditorTool(tool: String) {
        _editorState.value = _editorState.value.copy(activeTool = tool)
    }

    fun setEditorFilter(filter: String) {
        _editorState.value = _editorState.value.copy(selectedFilter = filter)
    }

    fun rotateEditor90() {
        val current = _editorState.value.rotation
        _editorState.value = _editorState.value.copy(rotation = (current + 90f) % 360f)
    }

    fun flipEditorH() {
        _editorState.value = _editorState.value.copy(flipH = !_editorState.value.flipH)
    }

    fun flipEditorV() {
        _editorState.value = _editorState.value.copy(flipV = !_editorState.value.flipV)
    }

    fun setEditorCropRatio(ratio: String) {
        _editorState.value = _editorState.value.copy(cropRatio = ratio)
    }

    fun updateEditorAdjustment(brightness: Float, contrast: Float, saturation: Float, warmth: Float) {
        _editorState.value = _editorState.value.copy(
            brightness = brightness,
            contrast = contrast,
            saturation = saturation,
            warmth = warmth
        )
    }

    fun addDrawingPath(path: DrawingPath) {
        val current = _editorState.value.drawingPaths
        _editorState.value = _editorState.value.copy(drawingPaths = current + path)
    }

    fun undoDrawingPath() {
        val current = _editorState.value.drawingPaths
        if (current.isNotEmpty()) {
            _editorState.value = _editorState.value.copy(drawingPaths = current.dropLast(1))
        }
    }

    fun clearDrawingPaths() {
        _editorState.value = _editorState.value.copy(drawingPaths = emptyList())
    }

    fun setEditorTextOverlay(textOverlay: TextOverlay?) {
        _editorState.value = _editorState.value.copy(textOverlay = textOverlay)
    }

    fun saveEditedPhoto(asNew: Boolean = true) {
        val state = _editorState.value
        val original = state.mediaItem ?: return

        viewModelScope.launch(Dispatchers.IO) {
            if (asNew) {
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
                    flipVertical = state.flipV
                )
                repository.insertMedia(newMedia)
                showFeedback("Saved as new photo in 'Edits' album!")
            } else {
                val updated = original.copy(
                    filterName = state.selectedFilter,
                    rotationDegrees = state.rotation,
                    flipHorizontal = state.flipH,
                    flipVertical = state.flipV
                )
                repository.updateMedia(updated)
                showFeedback("Photo changes saved!")
            }
            closeEditor()
        }
    }
}
