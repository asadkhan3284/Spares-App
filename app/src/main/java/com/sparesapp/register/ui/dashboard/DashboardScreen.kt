package com.sparesapp.register.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sparesapp.register.data.model.StockStatus
import com.sparesapp.register.ui.common.fmtMoney
import com.sparesapp.register.ui.common.fmtQty
import com.sparesapp.register.ui.common.statusColors
import com.sparesapp.register.ui.main.MainViewModel

private data class Kpi(val label: String, val value: String, val color: androidx.compose.ui.graphics.Color? = null)

@Composable
fun DashboardScreen(vm: MainViewModel) {
    val rows by vm.allRows.collectAsState()
    val statusCounts by vm.statusCounts.collectAsState()

    val total = rows.size
    val totalValue = rows.sumOf { it.invValue ?: 0.0 }
    val totalYearlyUsage = rows.sumOf { it.yearly ?: 0.0 }
    val distinctAreas = rows.map { it.area }.filter { it.isNotBlank() }.distinct().size
    val distinctControllers = rows.map { it.mrp }.filter { it.isNotBlank() }.distinct().size
    val withPhotos = rows.count { it.hasImages }

    val stockKpis = listOf(
        Kpi("Total line items", total.toString()),
        Kpi("Zero stock", (statusCounts[StockStatus.ZERO] ?: 0).toString(), statusColors(StockStatus.ZERO).first),
        Kpi("Below safety stock", (statusCounts[StockStatus.BELOW_SAFETY_STOCK] ?: 0).toString(), statusColors(StockStatus.BELOW_SAFETY_STOCK).first),
        Kpi("Below ROP", (statusCounts[StockStatus.BELOW_ROP] ?: 0).toString(), statusColors(StockStatus.BELOW_ROP).first),
        Kpi("Healthy stock", (statusCounts[StockStatus.OK] ?: 0).toString(), statusColors(StockStatus.OK).first),
    )
    val otherKpis = listOf(
        Kpi("Inventory value", fmtMoney(totalValue)),
        Kpi("Yearly usage (units)", fmtQty(totalYearlyUsage)),
        Kpi("Responsible areas", distinctAreas.toString()),
        Kpi("MRP controllers", distinctControllers.toString()),
        Kpi("Items with photos", "$withPhotos / $total"),
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 96.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text("Stock status", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
        }
        items(stockKpis) { KpiCard(it) }
        item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(2) }) {
            Text("Overview", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        }
        items(otherKpis) { KpiCard(it) }
    }
}

@Composable
private fun KpiCard(kpi: Kpi) {
    Card(shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Text(
                kpi.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                kpi.value,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = kpi.color ?: MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
