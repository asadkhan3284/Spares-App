package com.sparesapp.register.ui.common

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.sparesapp.register.data.model.PhotoRef
import com.sparesapp.register.graph.GraphRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A tiny in-memory (not persisted) LRU cache so scrolling a list doesn't re-download the same photo. */
private object GraphImageCache {
    private const val MAX_ENTRIES = 60
    private val map = object : LinkedHashMap<String, ImageBitmap>(MAX_ENTRIES, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, ImageBitmap>?): Boolean =
            size > MAX_ENTRIES
    }

    @Synchronized fun get(key: String): ImageBitmap? = map[key]

    @Synchronized fun put(key: String, bitmap: ImageBitmap) { map[key] = bitmap }
}

/**
 * Downloads and displays a photo directly from OneDrive/SharePoint via Microsoft Graph.
 * Photos are never cached to disk — only kept in a small in-memory LRU for the current session,
 * matching the "always fetch photos fresh" behavior chosen for this app.
 */
@Composable
fun GraphImage(
    photoRef: PhotoRef?,
    graphRepository: GraphRepository,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    var bitmap by remember(photoRef) { mutableStateOf(photoRef?.let { GraphImageCache.get(it.itemId) }) }
    var failed by remember(photoRef) { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(photoRef) {
        if (photoRef == null || bitmap != null) return@LaunchedEffect
        failed = false
        val result = withContext(Dispatchers.IO) {
            runCatching {
                val bytes = graphRepository.downloadContent(photoRef.driveId, photoRef.itemId)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            }
        }
        result.onSuccess { bmp ->
            if (bmp != null) {
                GraphImageCache.put(photoRef.itemId, bmp)
                bitmap = bmp
            } else failed = true
        }.onFailure { failed = true }
    }

    when {
        bitmap != null -> Image(bitmap = bitmap!!, contentDescription = null, modifier = modifier, contentScale = contentScale)
        photoRef == null -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        failed -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.BrokenImage, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}
