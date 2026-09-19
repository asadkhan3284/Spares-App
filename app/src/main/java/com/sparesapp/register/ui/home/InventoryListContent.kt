package com.sparesapp.register.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparesapp.register.data.model.InventoryRow
import com.sparesapp.register.graph.GraphRepository
import com.sparesapp.register.ui.common.GraphImage
import com.sparesapp.register.ui.common.StatusChip
import com.sparesapp.register.ui.common.fmtQty

@Composable
fun InventoryListContent(rows: List<InventoryRow>, graphRepository: GraphRepository, onOpenRow: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 96.dp)) {
        items(rows, key = { it.mat }) { row ->
            Surface(onClick = { onOpenRow(row.mat) }, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    GraphImage(
                        photoRef = row.images.firstOrNull(),
                        graphRepository = graphRepository,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(row.mat, style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                        Text(
                            row.sd.ifBlank { row.ld }.ifBlank { "(no description)" },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                        )
                        Spacer(Modifier.width(4.dp))
                        StatusChip(row.status)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(horizontalAlignment = Alignment.End) {
                        Text(fmtQty(row.qty), fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                        Text(row.unit, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Divider()
        }
    }
}
