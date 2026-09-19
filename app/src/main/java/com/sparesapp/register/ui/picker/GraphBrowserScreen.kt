package com.sparesapp.register.ui.picker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sparesapp.register.SparesApp
import com.sparesapp.register.data.RememberedLocation
import com.sparesapp.register.graph.DriveItem
import com.sparesapp.register.graph.GraphSource
import com.sparesapp.register.ui.common.GraphBrowserViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphBrowserScreen(
    app: SparesApp,
    mode: PickMode,
    onPicked: (RememberedLocation) -> Unit,
    onBack: () -> Unit,
) {
    val vm = viewModel<GraphBrowserViewModel>(factory = GraphBrowserViewModelFactory(app, mode))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (mode == PickMode.INVENTORY_FILE) "Pick inventory file" else "Pick photos folder") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (state.currentSource == null) {
                SourcePicker(state = state, onSearch = vm::searchSites, onOpen = vm::openSource)
            } else {
                Breadcrumbs(names = state.breadcrumbs.map { it.name }, onCrumbClick = vm::goToBreadcrumb)

                if (mode == PickMode.IMAGES_FOLDER) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { vm.currentFolderLocation()?.let(onPicked) }) {
                            Text("Use this folder")
                        }
                    }
                }

                when {
                    state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.error ?: "Error", color = MaterialTheme.colorScheme.error)
                    }
                    else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
                        items(state.children, key = { it.id }) { item ->
                            DriveItemRow(item) {
                                if (item.isFolder) {
                                    vm.openFolder(item)
                                } else if (mode == PickMode.INVENTORY_FILE) {
                                    vm.locationForFile(item)?.let(onPicked)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePicker(
    state: BrowserUiState,
    onSearch: (String) -> Unit,
    onOpen: (GraphSource) -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Choose a location", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.siteSearchQuery,
            onValueChange = onSearch,
            label = { Text("Search SharePoint sites") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            val list = if (state.siteSearchQuery.length >= 2) state.siteSearchResults else state.sources
            LazyColumn {
                items(list, key = { it.driveId }) { source ->
                    Surface(onClick = { onOpen(source) }, modifier = Modifier.fillMaxWidth()) {
                        ListItem(
                            headlineContent = { Text(source.label) },
                            supportingContent = {
                                Text(if (source.kind == GraphSource.Kind.MY_ONEDRIVE) "Your OneDrive" else "SharePoint document library")
                            },
                            leadingContent = { Icon(Icons.Default.CloudQueue, contentDescription = null) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Breadcrumbs(names: List<String>, onCrumbClick: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        names.forEachIndexed { i, name ->
            TextButton(onClick = { onCrumbClick(i) }) { Text(name) }
            if (i != names.lastIndex) Text("/", modifier = Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun DriveItemRow(item: DriveItem, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        ListItem(
            headlineContent = { Text(item.name) },
            supportingContent = if (item.isFolder) {
                { Text("${item.childCount} item(s)") }
            } else null,
            leadingContent = {
                Icon(
                    if (item.isFolder) Icons.Default.Folder else Icons.Default.Description,
                    contentDescription = null
                )
            },
        )
    }
}
