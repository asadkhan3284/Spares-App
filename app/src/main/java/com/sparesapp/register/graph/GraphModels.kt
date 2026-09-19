package com.sparesapp.register.graph

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DriveItem(
    val id: String,
    val name: String,
    val size: Long? = null,
    val folder: FolderFacet? = null,
    val file: FileFacet? = null,
    val image: ImageFacet? = null,
    @Json(name = "parentReference") val parentReference: ParentReference? = null,
    @Json(name = "webUrl") val webUrl: String? = null,
    @Json(name = "@microsoft.graph.downloadUrl") val downloadUrl: String? = null,
) {
    val isFolder: Boolean get() = folder != null
    val childCount: Int get() = folder?.childCount ?: 0
}

@JsonClass(generateAdapter = true)
data class FolderFacet(@Json(name = "childCount") val childCount: Int = 0)

@JsonClass(generateAdapter = true)
data class FileFacet(@Json(name = "mimeType") val mimeType: String? = null)

@JsonClass(generateAdapter = true)
data class ImageFacet(val width: Int? = null, val height: Int? = null)

@JsonClass(generateAdapter = true)
data class ParentReference(
    @Json(name = "driveId") val driveId: String? = null,
    @Json(name = "path") val path: String? = null,
)

@JsonClass(generateAdapter = true)
data class DriveItemList(val value: List<DriveItem> = emptyList())

@JsonClass(generateAdapter = true)
data class Drive(
    val id: String,
    val name: String? = null,
    @Json(name = "driveType") val driveType: String? = null,
    val owner: Owner? = null,
)

@JsonClass(generateAdapter = true)
data class Owner(val user: OwnerUser? = null)

@JsonClass(generateAdapter = true)
data class OwnerUser(@Json(name = "displayName") val displayName: String? = null)

@JsonClass(generateAdapter = true)
data class DriveList(val value: List<Drive> = emptyList())

@JsonClass(generateAdapter = true)
data class Site(
    val id: String,
    val name: String? = null,
    @Json(name = "displayName") val displayName: String? = null,
    @Json(name = "webUrl") val webUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class SiteList(val value: List<Site> = emptyList())

/**
 * A single browseable node in the OneDrive/SharePoint picker: either the
 * user's personal drive, a SharePoint site's document library, or a
 * folder/file within one of those, always addressed as (driveId, itemId).
 */
data class GraphNode(
    val driveId: String,
    val itemId: String,
    val name: String,
    val isFolder: Boolean,
    val path: String,
)
