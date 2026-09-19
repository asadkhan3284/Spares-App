package com.sparesapp.register.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sparesapp.register.SparesApp
import com.sparesapp.register.auth.AuthManager
import com.sparesapp.register.ui.main.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    app: SparesApp,
    vm: MainViewModel,
    onPickInventoryFile: () -> Unit,
    onPickImagesFolder: () -> Unit,
    onSignedOut: () -> Unit,
    onBack: () -> Unit,
) {
    val invLoc by vm.inventoryLocation.collectAsState(initial = null)
    val imgLoc by vm.imagesLocation.collectAsState(initial = null)
    val scope = rememberCoroutineScope()
    val account = (app.authManager.authState.collectAsState().value as? AuthManager.AuthState.SignedIn)?.account

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data sources") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxWidth().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            account?.username?.let {
                Text("Signed in as $it", style = MaterialTheme.typography.bodyMedium)
            }

            Card {
                ListItem(
                    headlineContent = { Text("Inventory file") },
                    supportingContent = { Text(invLoc?.path?.ifBlank { invLoc?.name.orEmpty() } ?: "Not set") },
                    leadingContent = { Icon(Icons.Default.Description, contentDescription = null) },
                )
                OutlinedButton(onClick = onPickInventoryFile, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Choose inventory file (.xlsx / .csv)")
                }
            }

            Card {
                ListItem(
                    headlineContent = { Text("Photos folder") },
                    supportingContent = { Text(imgLoc?.path?.ifBlank { imgLoc?.name.orEmpty() } ?: "Not set") },
                    leadingContent = { Icon(Icons.Default.Folder, contentDescription = null) },
                )
                OutlinedButton(onClick = onPickImagesFolder, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    Text("Choose photos folder")
                }
            }

            Text(
                "Photos are matched by Old Material # first, falling back to Material #, the same as the desktop tool — files named like PARTNO.jpg, PARTNO (1).jpg, PARTNO (2).jpg are grouped together.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = {
                    scope.launch {
                        app.authManager.signOut()
                        onSignedOut()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Default.Logout, contentDescription = null)
                Text("  Sign out")
            }
        }
    }
}
