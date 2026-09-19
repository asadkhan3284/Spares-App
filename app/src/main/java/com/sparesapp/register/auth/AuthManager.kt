package com.sparesapp.register.auth

import android.app.Activity
import android.content.Context
import com.microsoft.identity.client.AcquireTokenParameters
import com.microsoft.identity.client.AcquireTokenSilentParameters
import com.microsoft.identity.client.AuthenticationCallback
import com.microsoft.identity.client.IAccount
import com.microsoft.identity.client.IAuthenticationResult
import com.microsoft.identity.client.ISingleAccountPublicClientApplication
import com.microsoft.identity.client.PublicClientApplication
import com.microsoft.identity.client.SilentAuthenticationCallback
import com.microsoft.identity.client.exception.MsalException
import com.sparesapp.register.R
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps MSAL's single-account PublicClientApplication. Supports signing in
 * with either a personal Microsoft account or a work/school (Azure AD)
 * account, since msal_config.json declares audience
 * AzureADandPersonalMicrosoftAccount against the "common" authority.
 *
 * Scopes requested are read-only Microsoft Graph scopes for OneDrive and
 * SharePoint: Files.Read, Files.Read.All, Sites.Read.All.
 */
class AuthManager(private val context: Context) {

    companion object {
        val GRAPH_SCOPES = arrayOf("Files.Read", "Files.Read.All", "Sites.Read.All", "User.Read")
    }

    private var pca: ISingleAccountPublicClientApplication? = null

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unknown)
    val authState: StateFlow<AuthState> = _authState

    sealed class AuthState {
        object Unknown : AuthState()
        object SignedOut : AuthState()
        data class SignedIn(val account: IAccount) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    suspend fun ensureInitialized(): ISingleAccountPublicClientApplication =
        pca ?: suspendCancellableCoroutine { cont ->
            PublicClientApplication.createSingleAccountPublicClientApplication(
                context,
                R.raw.msal_config,
                object : com.microsoft.identity.client.IPublicClientApplication.ISingleAccountApplicationCreatedListener {
                    override fun onCreated(application: ISingleAccountPublicClientApplication) {
                        pca = application
                        refreshCurrentAccount()
                        cont.resume(application)
                    }

                    override fun onError(exception: MsalException) {
                        _authState.value = AuthState.Error(exception.message ?: "Failed to initialize MSAL")
                        cont.resumeWithException(exception)
                    }
                }
            )
        }

    private fun refreshCurrentAccount() {
        pca?.getCurrentAccountAsync(object : ISingleAccountPublicClientApplication.CurrentAccountCallback {
            override fun onAccountLoaded(activeAccount: IAccount?) {
                _authState.value = if (activeAccount != null) AuthState.SignedIn(activeAccount) else AuthState.SignedOut
            }

            override fun onAccountChanged(priorAccount: IAccount?, currentAccount: IAccount?) {
                _authState.value = if (currentAccount != null) AuthState.SignedIn(currentAccount) else AuthState.SignedOut
            }

            override fun onError(exception: MsalException) {
                _authState.value = AuthState.Error(exception.message ?: "Failed to load account")
            }
        })
    }

    suspend fun signIn(activity: Activity) {
        val app = ensureInitialized()
        suspendCancellableCoroutine<Unit> { cont ->
            val params = AcquireTokenParameters.Builder()
                .startAuthorizationFromActivity(activity)
                .withScopes(GRAPH_SCOPES.toList())
                .withCallback(object : AuthenticationCallback {
                    override fun onSuccess(result: IAuthenticationResult) {
                        _authState.value = AuthState.SignedIn(result.account)
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onError(exception: MsalException) {
                        _authState.value = AuthState.Error(exception.message ?: "Sign-in failed")
                        if (cont.isActive) cont.resumeWithException(exception)
                    }

                    override fun onCancel() {
                        if (cont.isActive) cont.resume(Unit)
                    }
                })
                .build()
            app.acquireToken(params)
        }
    }

    suspend fun signOut() {
        val app = ensureInitialized()
        suspendCancellableCoroutine<Unit> { cont ->
            app.signOut(object : ISingleAccountPublicClientApplication.SignOutCallback {
                override fun onSignOut() {
                    _authState.value = AuthState.SignedOut
                    if (cont.isActive) cont.resume(Unit)
                }

                override fun onError(exception: MsalException) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            })
        }
    }

    /** Acquires a Graph access token silently, refreshing if needed. Throws if the user must interact. */
    suspend fun acquireTokenSilent(): String {
        val app = ensureInitialized()
        val account = (authState.value as? AuthState.SignedIn)?.account
            ?: throw IllegalStateException("Not signed in")
        val authority = account.authority
        return suspendCancellableCoroutine { cont ->
            val params = AcquireTokenSilentParameters.Builder()
                .forAccount(account)
                .fromAuthority(authority)
                .withScopes(GRAPH_SCOPES.toList())
                .withCallback(object : SilentAuthenticationCallback {
                    override fun onSuccess(result: IAuthenticationResult) {
                        if (cont.isActive) cont.resume(result.accessToken)
                    }

                    override fun onError(exception: MsalException) {
                        if (cont.isActive) cont.resumeWithException(exception)
                    }
                })
                .build()
            app.acquireTokenSilentAsync(params)
        }
    }
}
