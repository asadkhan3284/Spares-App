package com.sparesapp.register.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sparesapp.register.data.RememberedLocation
import com.sparesapp.register.graph.DriveItem
import com.sparesapp.register.graph.GraphRepository
import com.sparesapp.register.graph.GraphSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PickMode { INVENTORY_FILE, IMAGES_FOLDER }

data class Crumb(val itemId: String?, val name: String, val isRoot: Boolean)

data class BrowserUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val sources: List<GraphSource> = emptyList(),
    val siteSearchQuery: String = "",
    val siteSearchResults: List<GraphSource> = emptyList(),
    val currentSource: GraphSource? = null,
    val breadcrumbs: List<Crumb> = emptyList(),
    val children: List<DriveItem> = emptyList(),
)

private val SPREADSHEET_EXTENSIONS = setOf("xlsx", "xls", "csv")

class GraphBrowserViewModel(
    private val graphRepository: GraphRepository,
    val mode: PickMode,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowserUiState())
    val state: StateFlow<BrowserUiState> = _state.asStateFlow()

    init {
        loadSources()
    }

    private fun loadSources() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { graphRepository.listTopLevelSources() }
                .onSuccess { sources -> _state.value = _state.value.copy(loading = false, sources = sources) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message ?: "Could not load OneDrive/SharePoint sources") }
        }
    }

    fun searchSites(query: String) {
        _state.value = _state.value.copy(siteSearchQuery = query)
        if (query.length < 2) {
            _state.value = _state.value.copy(siteSearchResults = emptyList())
            return
        }
        viewModelScope.launch {
            val results = runCatching { graphRepository.searchSharePointSites(query) }.getOrElse { emptyList() }
            _state.value = _state.value.copy(siteSearchResults = results)
        }
    }

    fun openSource(source: GraphSource) {
        _state.value = _state.value.copy(
            currentSource = source,
            breadcrumbs = listOf(Crumb(null, source.label, true)),
        )
        loadRoot(source.driveId)
    }

    private fun loadRoot(driveId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { graphRepository.listRootChildren(driveId) }
                .onSuccess { children -> _state.value = _state.value.copy(loading = false, children = filterForMode(children)) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message ?: "Could not list that drive") }
        }
    }

    fun openFolder(item: DriveItem) {
        val source = _state.value.currentSource ?: return
        _state.value = _state.value.copy(breadcrumbs = _state.value.breadcrumbs + Crumb(item.id, item.name, false))
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { graphRepository.listChildren(source.driveId, item.id) }
                .onSuccess { children -> _state.value = _state.value.copy(loading = false, children = filterForMode(children)) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message ?: "Could not list that folder") }
        }
    }

    fun goToBreadcrumb(index: Int) {
        val source = _state.value.currentSource ?: return
        val target = _state.value.breadcrumbs.getOrNull(index) ?: return
        _state.value = _state.value.copy(breadcrumbs = _state.value.breadcrumbs.take(index + 1))
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val children = if (target.isRoot || target.itemId == null) {
                runCatching { graphRepository.listRootChildren(source.driveId) }
            } else {
                runCatching { graphRepository.listChildren(source.driveId, target.itemId) }
            }
            children
                .onSuccess { c -> _state.value = _state.value.copy(loading = false, children = filterForMode(c)) }
                .onFailure { e -> _state.value = _state.value.copy(loading = false, error = e.message ?: "Could not list that folder") }
        }
    }

    private fun filterForMode(children: List<DriveItem>): List<DriveItem> = when (mode) {
        PickMode.IMAGES_FOLDER -> children.filter { it.isFolder }
        PickMode.INVENTORY_FILE -> children.filter { item ->
            item.isFolder || SPREADSHEET_EXTENSIONS.any { ext -> item.name.lowercase().endsWith(".$ext") }
        }
    }

    fun currentFolderLocation(): RememberedLocation? {
        val source = _state.value.currentSource ?: return null
        val last = _state.value.breadcrumbs.lastOrNull() ?: return null
        val path = _state.value.breadcrumbs.joinToString(" / ") { it.name }
        return if (last.isRoot || last.itemId == null) {
            RememberedLocation(source.driveId, "root", last.name, path, isRoot = true)
        } else {
            RememberedLocation(source.driveId, last.itemId, last.name, path, isRoot = false)
        }
    }

    fun locationForFile(item: DriveItem): RememberedLocation? {
        val source = _state.value.currentSource ?: return null
        val path = (_state.value.breadcrumbs.map { it.name } + item.name).joinToString(" / ")
        return RememberedLocation(source.driveId, item.id, item.name, path, isRoot = false)
    }
}
