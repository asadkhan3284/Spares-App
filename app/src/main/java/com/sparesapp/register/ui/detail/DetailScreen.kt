package com.sparesapp.register.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sparesapp.register.SparesApp
import com.sparesapp.register.data.model.ExportColumn
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.data.model.exportValue
import com.sparesapp.register.ui.common.GraphImage
import com.sparesapp.register.ui.common.StatusChip
import com.sparesapp.register.ui.main.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(app: SparesApp, vm: MainViewModel, mat: String, onBack: () -> Unit) {
    val rows by vm.allRows.collectAsState()
    val row = rows.firstOrNull { it.mat == mat }
    var lightboxIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(row?.mat ?: mat, fontFamily = FontFamily.Monospace) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        if (row == null) {
            Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
                Text("Item not found — it may have been removed from the last sync.", modifier = Modifier.padding(24.dp))
            }
            return@Scaffold
        }

        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 32.dp)) {
            item {
                Text(
                    row.sd.ifBlank { row.ld }.ifBlank { "(no description)" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                )
                if (row.ld.isNotBlank() && row.ld != row.sd) {
                    Text(row.ld, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 16.dp))
                }
                Row(Modifier.padding(16.dp, 8.dp)) { StatusChip(row.status) }
            }

            if (row.images.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(horizontal = 4.dp),
                    ) {
                        itemsIndexed(row.images) { idx, photo ->
                            Surface(
                                onClick = { lightboxIndex = idx },
                                modifier = Modifier.padding(6.dp),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                GraphImage(
                                    photoRef = photo,
                                    graphRepository = app.graphRepository,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(8.dp)),
                                )
                            }
                        }
                    }
                }
            }

            item { Divider(Modifier.padding(vertical = 8.dp)) }

            items(detailFields(row)) { (label, value) ->
                DetailRow(label, value)
            }
        }
    }

    val idx = lightboxIndex
    if (idx != null && row != null) {
        PhotoLightbox(
            photos = row.images,
            startIndex = idx,
            graphRepository = app.graphRepository,
            onDismiss = { lightboxIndex = null },
        )
    }
}

private fun detailFields(row: InventoryRow): List<Pair<String, String>> =
    ExportColumn.entries
        .filter { it != ExportColumn.SD && it != ExportColumn.LD }
        .map { col -> col.label to row.exportValue(col).ifBlank { "—" } }

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
    }
    Divider()
}
