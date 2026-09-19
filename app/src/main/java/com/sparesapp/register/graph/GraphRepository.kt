package com.sparesapp.register.graph

import com.sparesapp.register.auth.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A "source" the picker can browse: the user's own OneDrive, a followed
 * SharePoint site's document library, or a site found by search.
 */
data class GraphSource(val driveId: String, val label: String, val kind: Kind) {
    enum class Kind { MY_ONEDRIVE, SHAREPOINT_SITE }
}

class GraphRepository(authManager: AuthManager) {

    private val api: GraphApi = GraphClient.build(authManager)

    suspend fun listTopLevelSources(): List<GraphSource> = withContext(Dispatchers.IO) {
        val sources = mutableListOf<GraphSource>()
        runCatching { api.myDrive() }.onSuccess { drive ->
            sources += GraphSource(drive.id, "My OneDrive", GraphSource.Kind.MY_ONEDRIVE)
        }
        runCatching { api.followedSites() }.onSuccess { sites ->
            sites.value.forEach { site ->
                runCatching { api.siteDrives(site.id) }.onSuccess { drives ->
                    drives.value.forEach { drive ->
                        val label = "${site.displayName ?: site.name ?: "SharePoint site"} — ${drive.name ?: "Documents"}"
                        sources += GraphSource(drive.id, label, GraphSource.Kind.SHAREPOINT_SITE)
                    }
                }
            }
        }
        sources
    }

    suspend fun searchSharePointSites(query: String): List<GraphSource> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val sites = runCatching { api.searchSites(query) }.getOrElse { SiteList() }
        sites.value.flatMap { site ->
            val drives = runCatching { api.siteDrives(site.id) }.getOrElse { DriveList() }
            drives.value.map { drive ->
                val label = "${site.displayName ?: site.name ?: "SharePoint site"} — ${drive.name ?: "Documents"}"
                GraphSource(drive.id, label, GraphSource.Kind.SHAREPOINT_SITE)
            }
        }
    }

    suspend fun listRootChildren(driveId: String): List<DriveItem> = withContext(Dispatchers.IO) {
        api.driveRootChildren(driveId).value.sortedFoldersFirst()
    }

    suspend fun listChildren(driveId: String, itemId: String): List<DriveItem> = withContext(Dispatchers.IO) {
        api.driveItemChildren(driveId, itemId).value.sortedFoldersFirst()
    }

    /** Recursively lists every file (not folder) under a folder, for indexing a photos folder. */
    suspend fun listAllFilesRecursive(driveId: String, itemId: String, isRoot: Boolean): List<DriveItem> =
        withContext(Dispatchers.IO) {
            val out = mutableListOf<DriveItem>()
            val queue = ArrayDeque<Pair<String, Boolean>>()
            queue.add(itemId to isRoot)
            while (queue.isNotEmpty()) {
                val (id, root) = queue.removeFirst()
                val children = if (root) api.driveRootChildren(driveId).value else api.driveItemChildren(driveId, id).value
                for (child in children) {
                    if (child.isFolder) queue.add(child.id to false) else out += child
                }
            }
            out
        }

    suspend fun getItem(driveId: String, itemId: String): DriveItem = withContext(Dispatchers.IO) {
        api.driveItem(driveId, itemId)
    }

    /** Downloads a file's raw bytes via Graph, following the redirect to the pre-authenticated blob URL. */
    suspend fun downloadContent(driveId: String, itemId: String): ByteArray = withContext(Dispatchers.IO) {
        api.downloadItemContent(driveId, itemId).use { body -> body.bytes() }
    }

    private fun List<DriveItem>.sortedFoldersFirst(): List<DriveItem> =
        sortedWith(compareByDescending<DriveItem> { it.isFolder }.thenBy { it.name.lowercase() })
}
