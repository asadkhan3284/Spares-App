package com.sparesapp.register.data

import com.sparesapp.register.data.local.InventoryDao
import com.sparesapp.register.data.local.toEntity
import com.sparesapp.register.data.local.toRow
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.graph.GraphRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine

private data class PhotoIndexState(val driveId: String, val index: Map<String, List<com.sparesapp.register.data.IndexedPhoto>>)

class InventoryRepository(
    private val graphRepository: GraphRepository,
    private val preferencesRepository: PreferencesRepository,
    private val inventoryDao: InventoryDao,
) {
    private val photoIndex = MutableStateFlow<PhotoIndexState?>(null)

    private val _sourceLabel = MutableStateFlow<String?>(null)
    val sourceLabel: StateFlow<String?> = _sourceLabel

    private val _imagesLabel = MutableStateFlow<String?>(null)
    val imagesLabel: StateFlow<String?> = _imagesLabel

    /** Cached spreadsheet rows (works offline) with live photo matches merged in when a photos folder is loaded. */
    val rows = combine(inventoryDao.observeAll(), photoIndex) { entities, photoState ->
        entities.map { entity ->
            val row = entity.toRow()
            if (photoState == null) row
            else row.copy(images = PhotoMatcher.imagesFor(photoState.driveId, row, photoState.index))
        }
    }

    suspend fun refreshInventoryFrom(location: RememberedLocation): Result<Int> = runCatching {
        val bytes = graphRepository.downloadContent(location.driveId, location.itemId)
        val lower = location.name.lowercase()
        val parsed = when {
            lower.endsWith(".csv") -> InventoryParser.parseCsv(bytes)
            lower.endsWith(".xlsx") || lower.endsWith(".xls") -> InventoryParser.parseXlsx(bytes)
            else -> InventoryParser.parseXlsx(bytes)
        }
        inventoryDao.replaceAll(parsed.map { it.toEntity() })
        preferencesRepository.setInventoryLocation(location)
        preferencesRepository.setLastSyncAt(System.currentTimeMillis())
        _sourceLabel.value = "${location.name} (${parsed.size} rows)"
        parsed.size
    }

    suspend fun refreshPhotosFrom(location: RememberedLocation): Result<Int> = runCatching {
        val files = graphRepository.listAllFilesRecursive(location.driveId, location.itemId, location.isRoot)
        val index = PhotoMatcher.buildIndex(location.driveId, files)
        photoIndex.value = PhotoIndexState(location.driveId, index)
        preferencesRepository.setImagesLocation(location)
        _imagesLabel.value = "${location.name} (${files.size} images)"
        files.size
    }

    suspend fun hasCachedData(): Boolean = inventoryDao.count() > 0

    suspend fun currentRowsOnce(): List<InventoryRow> = inventoryDao.getAllOnce().map { it.toRow() }
}
