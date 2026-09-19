package com.sparesapp.register.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.graph.GraphRepository
import com.sparesapp.register.ui.common.GraphImage
import com.sparesapp.register.ui.common.StatusChip
import com.sparesapp.register.ui.common.fmtQty

@Composable
fun InventoryGridContent(rows: List<InventoryRow>, graphRepository: GraphRepository, onOpenRow: (String) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 160.dp),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp, 12.dp, 12.dp, 96.dp),
    ) {
        items(rows, key = { it.mat }) { row ->
            Card(
                onClick = { onOpenRow(row.mat) },
                modifier = Modifier.padding(6.dp).fillMaxWidth(),
            ) {
                GraphImage(
                    photoRef = row.images.firstOrNull(),
                    graphRepository = graphRepository,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)),
                )
                Column(Modifier.padding(8.dp)) {
                    Text(row.mat, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                    Text(
                        row.sd.ifBlank { row.ld }.ifBlank { "(no description)" },
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                    )
                    Spacer(Modifier.padding(2.dp))
                    StatusChip(row.status)
                    Text(fmtQty(row.qty) + " " + row.unit, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
