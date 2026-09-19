package com.sparesapp.register.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sparesapp.register.data.model.PhotoRef
import com.sparesapp.register.graph.GraphRepository
import com.sparesapp.register.ui.common.GraphImage

/** Fullscreen photo viewer: swipe between a part's photos, pinch/drag to zoom and pan — mirrors the HTML tool's lightbox. */
@Composable
fun PhotoLightbox(
    photos: List<PhotoRef>,
    startIndex: Int,
    graphRepository: GraphRepository,
    onDismiss: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = startIndex) { photos.size }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            ZoomableGraphImage(photoRef = photos[page], graphRepository = graphRepository)
        }
        IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
        }
        if (photos.size > 1) {
            Text(
                "${pagerState.currentPage + 1} / ${photos.size}",
                color = Color.White,
                modifier = Modifier.align(Alignment.BottomCenter).padding(24.dp),
            )
        }
    }
}

@Composable
private fun ZoomableGraphImage(photoRef: PhotoRef, graphRepository: GraphRepository) {
    var scale by remember(photoRef) { mutableFloatStateOf(1f) }
    var offsetX by remember(photoRef) { mutableFloatStateOf(0f) }
    var offsetY by remember(photoRef) { mutableFloatStateOf(0f) }

    GraphImage(
        photoRef = photoRef,
        graphRepository = graphRepository,
        contentScale = androidx.compose.ui.layout.ContentScale.Fit,
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
                scaleX = scale, scaleY = scale,
                translationX = offsetX, translationY = offsetY,
            )
            .pointerInput(photoRef) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 6f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f
                        offsetY = 0f
                    }
                }
            }
            .pointerInput(photoRef) {
                detectTapGestures(onDoubleTap = {
                    if (scale > 1f) { scale = 1f; offsetX = 0f; offsetY = 0f } else { scale = 2.5f }
                })
            },
    )
}
