package com.sparesapp.register.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sparesapp.register.data.InventoryRepository
import com.sparesapp.register.data.PreferencesRepository
import com.sparesapp.register.data.RememberedLocation
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.data.model.StockStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SortOption(val label: String) {
    MAT_ASC("Material # (A-Z)"),
    SD_ASC("Description (A-Z)"),
    QTY_DESC("Qty (High-Low)"),
    QTY_ASC("Qty (Low-High)"),
    PRICE_DESC("Price (High-Low)"),
    PRICE_ASC("Price (Low-High)"),
}

enum class PhotoFilter { ALL, HAS, NONE }

data class FilterState(
    val query: String = "",
    val status: StockStatus? = null,
    val photo: PhotoFilter = PhotoFilter.ALL,
    val mrp: String? = null,
    val unit: String? = null,
    val area: String? = null,
    val sort: SortOption = SortOption.MAT_ASC,
)

enum class ViewMode { LIST, GRID, DASHBOARD }

class MainViewModel(
    private val inventoryRepository: InventoryRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {

    private val _filter = MutableStateFlow(FilterState())
    val filter: StateFlow<FilterState> = _filter

    private val _viewMode = MutableStateFlow(ViewMode.LIST)
    val viewMode: StateFlow<ViewMode> = _viewMode

    val sourceLabel = inventoryRepository.sourceLabel
    val imagesLabel = inventoryRepository.imagesLabel
    val inventoryLocation = preferencesRepository.inventoryLocation
    val imagesLocation = preferencesRepository.imagesLocation

    val allRows: StateFlow<List<InventoryRow>> =
        inventoryRepository.rows.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredRows: StateFlow<List<InventoryRow>> =
        combine(allRows, _filter) { rows, f -> applyFilter(rows, f) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val statusCounts: StateFlow<Map<StockStatus, Int>> =
        allRows.map { rows -> rows.groupingBy { it.status }.eachCount() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val mrpOptions: StateFlow<List<String>> = allRows.map { rows ->
        rows.map { it.mrp }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unitOptions: StateFlow<List<String>> = allRows.map { rows ->
        rows.map { it.unit }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val areaOptions: StateFlow<List<String>> = allRows.map { rows ->
        rows.map { it.area }.filter { it.isNotBlank() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun setQuery(q: String) { _filter.value = _filter.value.copy(query = q) }
    fun setStatus(s: StockStatus?) { _filter.value = _filter.value.copy(status = s) }
    fun setPhotoFilter(p: PhotoFilter) { _filter.value = _filter.value.copy(photo = p) }
    fun setMrp(v: String?) { _filter.value = _filter.value.copy(mrp = v) }
    fun setUnit(v: String?) { _filter.value = _filter.value.copy(unit = v) }
    fun setArea(v: String?) { _filter.value = _filter.value.copy(area = v) }
    fun setSort(s: SortOption) { _filter.value = _filter.value.copy(sort = s) }
    fun resetFilters() { _filter.value = FilterState() }
    fun setViewMode(m: ViewMode) { _viewMode.value = m }

    fun findByCode(code: String): InventoryRow? {
        val norm = code.trim()
        return allRows.value.firstOrNull { it.mat.equals(norm, true) || it.old.equals(norm, true) }
    }

    fun refreshAll() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            val loc = preferencesRepository.inventoryLocation.first()
            if (loc != null) {
                inventoryRepository.refreshInventoryFrom(loc).onFailure {
                    _errorMessage.value = "Could not refresh inventory: ${it.message}"
                }
            }
            val imgLoc = preferencesRepository.imagesLocation.first()
            if (imgLoc != null) {
                inventoryRepository.refreshPhotosFrom(imgLoc).onFailure {
                    _errorMessage.value = "Could not refresh photos: ${it.message}"
                }
            }
            _isRefreshing.value = false
        }
    }

    fun setInventorySource(loc: RememberedLocation) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            inventoryRepository.refreshInventoryFrom(loc).onFailure {
                _errorMessage.value = "Could not read that file: ${it.message}"
            }
            _isRefreshing.value = false
        }
    }

    fun setImagesSource(loc: RememberedLocation) {
        viewModelScope.launch {
            _isRefreshing.value = true
            _errorMessage.value = null
            inventoryRepository.refreshPhotosFrom(loc).onFailure {
                _errorMessage.value = "Could not index that folder: ${it.message}"
            }
            _isRefreshing.value = false
        }
    }

    fun clearError() { _errorMessage.value = null }

    companion object {
        fun applyFilter(rows: List<InventoryRow>, f: FilterState): List<InventoryRow> {
            var out = rows.asSequence()
            if (f.query.isNotBlank()) {
                val q = f.query.trim().lowercase()
                out = out.filter { it.searchBlob.contains(q) }
            }
            f.status?.let { s -> out = out.filter { it.status == s } }
            when (f.photo) {
                PhotoFilter.HAS -> out = out.filter { it.hasImages }
                PhotoFilter.NONE -> out = out.filter { !it.hasImages }
                PhotoFilter.ALL -> {}
            }
            f.mrp?.let { v -> out = out.filter { it.mrp == v } }
            f.unit?.let { v -> out = out.filter { it.unit == v } }
            f.area?.let { v -> out = out.filter { it.area == v } }

            val list = out.toMutableList()
            when (f.sort) {
                SortOption.MAT_ASC -> list.sortBy { it.mat.lowercase() }
                SortOption.SD_ASC -> list.sortBy { it.sd.lowercase() }
                SortOption.QTY_DESC -> list.sortByDescending { it.qty ?: Double.NEGATIVE_INFINITY }
                SortOption.QTY_ASC -> list.sortBy { it.qty ?: Double.POSITIVE_INFINITY }
                SortOption.PRICE_DESC -> list.sortByDescending { it.price ?: Double.NEGATIVE_INFINITY }
                SortOption.PRICE_ASC -> list.sortBy { it.price ?: Double.POSITIVE_INFINITY }
            }
            return list
        }
    }
}
