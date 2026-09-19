package com.sparesapp.register.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sparesapp.register.SparesApp
import com.sparesapp.register.data.model.StockStatus
import com.sparesapp.register.ui.main.MainViewModel
import com.sparesapp.register.ui.main.PhotoFilter
import com.sparesapp.register.ui.main.SortOption
import com.sparesapp.register.ui.main.ViewMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    app: SparesApp,
    vm: MainViewModel,
    onOpenRow: (String) -> Unit,
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onExport: () -> Unit,
) {
    val filter by vm.filter.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val rows by vm.filteredRows.collectAsState()
    val statusCounts by vm.statusCounts.collectAsState()
    val isRefreshing by vm.isRefreshing.collectAsState()
    val sourceLabel by vm.sourceLabel.collectAsState()
    var showFilters by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Spares Register") },
                    navigationIcon = {
                        IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Menu, contentDescription = "Sources") }
                    },
                    actions = {
                        IconButton(onClick = { vm.refreshAll() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                        IconButton(onClick = onExport) {
                            Icon(Icons.Default.FileDownload, contentDescription = "Export filtered list")
                        }
                        IconButton(onClick = { showFilters = !showFilters }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filters")
                        }
                    }
                )
                if (isRefreshing) LinearProgressIndicator(Modifier.fillMaxWidth())
                sourceLabel?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedTextField(
                    value = filter.query,
                    onValueChange = vm::setQuery,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    placeholder = { Text("Search material #, description, BOM…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                )
                StatusChipRow(
                    selected = filter.status,
                    counts = statusCounts,
                    total = statusCounts.values.sum(),
                    onSelect = vm::setStatus,
                )
                ViewModeRow(viewMode = viewMode, onChange = vm::setViewMode)
                if (showFilters) {
                    FiltersPanel(vm)
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenScan) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Scan barcode")
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            when (viewMode) {
                ViewMode.LIST -> InventoryListContent(rows, app.graphRepository, onOpenRow)
                ViewMode.GRID -> InventoryGridContent(rows, app.graphRepository, onOpenRow)
                ViewMode.DASHBOARD -> com.sparesapp.register.ui.dashboard.DashboardScreen(vm)
            }
            if (rows.isEmpty() && !isRefreshing) {
                Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                    Text(
                        if (sourceLabel == null) "No inventory loaded yet — open the menu to pick a file from OneDrive/SharePoint."
                        else "No items match the current filters.",
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusChipRow(
    selected: StockStatus?,
    counts: Map<StockStatus, Int>,
    total: Int,
    onSelect: (StockStatus?) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        items(listOf(null) + StockStatus.entries) { s ->
            val label = s?.label ?: "All items"
            val count = if (s == null) total else counts[s] ?: 0
            FilterChip(
                selected = selected == s,
                onClick = { onSelect(s) },
                label = { Text("$label ($count)") },
            )
        }
    }
}

@Composable
private fun ViewModeRow(viewMode: ViewMode, onChange: (ViewMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        SegmentedButton(
            selected = viewMode == ViewMode.LIST,
            onClick = { onChange(ViewMode.LIST) },
            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(0, 3),
            icon = {},
        ) { Icon(Icons.Default.ViewList, contentDescription = null); Text(" List") }
        SegmentedButton(
            selected = viewMode == ViewMode.GRID,
            onClick = { onChange(ViewMode.GRID) },
            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(1, 3),
            icon = {},
        ) { Icon(Icons.Default.GridView, contentDescription = null); Text(" Grid") }
        SegmentedButton(
            selected = viewMode == ViewMode.DASHBOARD,
            onClick = { onChange(ViewMode.DASHBOARD) },
            shape = androidx.compose.material3.SegmentedButtonDefaults.itemShape(2, 3),
            icon = {},
        ) { Icon(Icons.Default.SpaceDashboard, contentDescription = null); Text(" Dashboard") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersPanel(vm: MainViewModel) {
    val filter by vm.filter.collectAsState()
    val mrpOptions by vm.mrpOptions.collectAsState()
    val unitOptions by vm.unitOptions.collectAsState()
    val areaOptions by vm.areaOptions.collectAsState()

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown("Controller", filter.mrp, mrpOptions, vm::setMrp, Modifier.weight(1f))
            Dropdown("Unit", filter.unit, unitOptions, vm::setUnit, Modifier.weight(1f))
        }
        Spacer(Modifier.padding(top = 6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown("Area", filter.area, areaOptions, vm::setArea, Modifier.weight(1f))
            SortDropdown(filter.sort, vm::setSort, Modifier.weight(1f))
        }
        Spacer(Modifier.padding(top = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selected = filter.photo == PhotoFilter.ALL, onClick = { vm.setPhotoFilter(PhotoFilter.ALL) }, label = { Text("All") })
            FilterChip(selected = filter.photo == PhotoFilter.HAS, onClick = { vm.setPhotoFilter(PhotoFilter.HAS) }, label = { Text("Has photos") })
            FilterChip(selected = filter.photo == PhotoFilter.NONE, onClick = { vm.setPhotoFilter(PhotoFilter.NONE) }, label = { Text("No photos") })
        }
        androidx.compose.material3.TextButton(onClick = vm::resetFilters) { Text("Reset all filters") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Dropdown(
    label: String,
    selected: String?,
    options: List<String>,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected ?: "All",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            androidx.compose.material3.DropdownMenuItem(text = { Text("All") }, onClick = { onSelect(null); expanded = false })
            options.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortDropdown(selected: SortOption, onSelect: (SortOption) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = modifier) {
        OutlinedTextField(
            value = selected.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sort by") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SortOption.entries.forEach { opt ->
                androidx.compose.material3.DropdownMenuItem(text = { Text(opt.label) }, onClick = { onSelect(opt); expanded = false })
            }
        }
    }
}
