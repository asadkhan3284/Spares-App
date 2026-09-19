package com.sparesapp.register.ui.signin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sparesapp.register.SparesApp
import com.sparesapp.register.auth.AuthManager
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(app: SparesApp, onSignedIn: () -> Unit) {
    val authState by app.authManager.authState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isSigningIn by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { app.authManager.ensureInitialized() }
    LaunchedEffect(authState) {
        if (authState is AuthManager.AuthState.SignedIn) onSignedIn()
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.height(64.dp))
        androidx.compose.foundation.layout.Spacer(Modifier.height(16.dp))
        Text("Spares Register", style = MaterialTheme.typography.headlineMedium)
        androidx.compose.foundation.layout.Spacer(Modifier.height(8.dp))
        Text(
            "Sign in with your Microsoft account to browse your spares register spreadsheet and part photos in OneDrive or SharePoint.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(32.dp))

        if (isSigningIn) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    val activity = context as? android.app.Activity ?: return@Button
                    isSigningIn = true
                    error = null
                    scope.launch {
                        runCatching { app.authManager.signIn(activity) }
                            .onFailure { error = it.message ?: "Sign-in failed" }
                        isSigningIn = false
                    }
                },
                modifier = Modifier.fillMaxWidth(0.8f)
            ) {
                Text("Sign in with Microsoft")
            }
        }

        error?.let {
            androidx.compose.foundation.layout.Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
}
