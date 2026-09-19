package com.sparesapp.register.data

import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.data.model.PhotoRef
import com.sparesapp.register.graph.DriveItem

/** One indexed photo: the Graph item plus its parsed base key and ordering suffix (n). */
data class IndexedPhoto(val item: DriveItem, val baseKey: String, val order: Int)

/**
 * Port of the HTML tool's image indexing: IMG_EXT_RE matches
 * "BASE.jpg", "BASE (1).jpg", "BASE (2).jpg", grouping photos by BASE
 * (normalized upper-case), sorted by their (n) suffix. getImagesFor()
 * looks up by Old Material # first, falling back to Material #.
 */
object PhotoMatcher {

    private val IMG_EXT_RE = Regex("^(.*?)(?:\\s*\\((\\d+)\\))?\\.(jpe?g|png|webp|gif)$", RegexOption.IGNORE_CASE)

    fun normKey(s: String?): String = (s ?: "").trim().uppercase()

    fun buildIndex(driveId: String, files: List<DriveItem>): Map<String, List<IndexedPhoto>> {
        val idx = mutableMapOf<String, MutableList<IndexedPhoto>>()
        for (item in files) {
            val m = IMG_EXT_RE.find(item.name) ?: continue
            val base = normKey(m.groupValues[1])
            val n = m.groupValues[2].toIntOrNull() ?: 0
            idx.getOrPut(base) { mutableListOf() } += IndexedPhoto(item, base, n)
        }
        idx.values.forEach { list -> list.sortBy { it.order } }
        return idx
    }

    fun imagesFor(driveId: String, row: InventoryRow, index: Map<String, List<IndexedPhoto>>): List<PhotoRef> {
        val keys = listOfNotNull(row.old.takeIf { it.isNotBlank() }, row.mat.takeIf { it.isNotBlank() }).map(::normKey)
        for (k in keys) {
            index[k]?.let { photos -> return photos.map { PhotoRef(driveId, it.item.id, it.item.name) } }
        }
        return emptyList()
    }
}
