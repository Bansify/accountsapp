package com.gothwad.accounts.ui.state

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.ConnectedAppGrant
import com.gothwad.accounts.core.DisconnectOutcome
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.SearchableSetting
import com.gothwad.accounts.ui.model.SignOutDialogTarget
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Testable adapter wrapping [GothwadAccounts] singleton calls. */
interface AccountSessionController {
    val state: StateFlow<AccountState>
    val discoveredAccounts: StateFlow<List<DiscoveredAccount>>
    val isRefreshing: StateFlow<Boolean>
    val error: StateFlow<AuthFailure?>
    val lastDisconnectOutcome: StateFlow<DisconnectOutcome?>
        get() = MutableStateFlow(null)

    fun signIn(activity: Activity, chooseAccount: Boolean = false, loginHint: String? = null)
    fun switchTo(userId: String)
    fun signOut(userId: String)
    fun disconnectFromServer(userId: String) = signOut(userId)
    fun removeFromDevice(userId: String)
    fun refreshProfile()
    fun syncDiscoveredAccounts()
    fun manageOnWeb(activity: Activity, section: String? = null)
    fun openWebPage(activity: Activity, path: String)
    fun recoverCorruptedStorage()
    fun clearError()
    fun clearDisconnectOutcome() = Unit
    fun installedVerifiedPeers(): List<String>

    fun canManageAccountNatively(): Boolean = false
    fun canWriteProfileNatively(): Boolean = false
    suspend fun listWebSessions(): Result<List<WebSessionInfo>> = Result.success(emptyList())
    suspend fun revokeOtherWebSessions(): Result<Int> = Result.success(0)
    suspend fun listActivity(): Result<List<AccountActivityEntry>> = Result.success(emptyList())
    suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> = Result.success(emptyList())
    suspend fun revokeAuthorization(appId: String): Result<Unit> = Result.success(Unit)
    suspend fun updateBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ): Result<GothwadAccount?> = Result.failure(UnsupportedOperationException())
}

object LiveGothwadAccountController : AccountSessionController {
    override val state: StateFlow<AccountState> get() = GothwadAccounts.state
    override val discoveredAccounts: StateFlow<List<DiscoveredAccount>> get() = GothwadAccounts.discoveredAccounts
    override val isRefreshing: StateFlow<Boolean> get() = GothwadAccounts.isRefreshing
    override val error: StateFlow<AuthFailure?> get() = GothwadAccounts.error
    override val lastDisconnectOutcome: StateFlow<DisconnectOutcome?> get() = GothwadAccounts.lastDisconnectOutcome

    override fun signIn(activity: Activity, chooseAccount: Boolean, loginHint: String?) {
        GothwadAccounts.signIn(activity, chooseAccount, loginHint)
    }

    override fun switchTo(userId: String) { GothwadAccounts.switchTo(userId) }
    override fun signOut(userId: String) { GothwadAccounts.signOut(userId, revokeOnServer = false) }
    override fun disconnectFromServer(userId: String) { GothwadAccounts.disconnectFromServerAsync(userId) }
    override fun removeFromDevice(userId: String) { GothwadAccounts.removeFromDevice(userId, revokeOnServer = false) }
    override fun refreshProfile() { GothwadAccounts.refreshProfile() }
    override fun syncDiscoveredAccounts() { GothwadAccounts.syncDiscoveredAccounts() }
    override fun manageOnWeb(activity: Activity, section: String?) { GothwadAccounts.manageOnWeb(activity, section) }
    override fun openWebPage(activity: Activity, path: String) { GothwadAccounts.openWebPage(activity, path) }
    override fun recoverCorruptedStorage() { GothwadAccounts.recoverCorruptedStorage() }
    override fun clearError() { GothwadAccounts.clearError() }
    override fun clearDisconnectOutcome() { GothwadAccounts.clearDisconnectOutcome() }
    override fun installedVerifiedPeers(): List<String> = GothwadAccounts.installedVerifiedPeers()

    override fun canManageAccountNatively(): Boolean = GothwadAccounts.canManageAccountNatively()
    override fun canWriteProfileNatively(): Boolean = GothwadAccounts.canWriteProfileNatively()
    override suspend fun listWebSessions(): Result<List<WebSessionInfo>> = GothwadAccounts.listWebSessions()
    override suspend fun revokeOtherWebSessions(): Result<Int> = GothwadAccounts.revokeOtherWebSessions()
    override suspend fun listActivity(): Result<List<AccountActivityEntry>> = GothwadAccounts.listActivity()
    override suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> = GothwadAccounts.listAuthorizations()
    override suspend fun revokeAuthorization(appId: String): Result<Unit> = GothwadAccounts.revokeAuthorization(appId)
    override suspend fun updateBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ): Result<GothwadAccount?> = GothwadAccounts.updateBasicProfile(firstName, lastName, dateOfBirth, gender)
}

class AccountDashboardViewModel(
    private val controller: AccountSessionController = LiveGothwadAccountController,
) : ViewModel() {

    private var lastLoadedNativeUserId: String? = null

    private val mutableUiState = MutableStateFlow(
        AccountDashboardUiState(
            accountState = controller.state.value,
            discoveredAccounts = controller.discoveredAccounts.value,
            isRefreshing = controller.isRefreshing.value,
            authError = controller.error.value,
            installedVerifiedPeers = runCatching { controller.installedVerifiedPeers() }.getOrDefault(emptyList()),
            canManageNatively = runCatching { controller.canManageAccountNatively() }.getOrDefault(false),
            canWriteProfileNatively = runCatching { controller.canWriteProfileNatively() }.getOrDefault(false),
        ),
    )
    val uiState: StateFlow<AccountDashboardUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                controller.state,
                controller.discoveredAccounts,
                controller.isRefreshing,
                controller.error,
                controller.lastDisconnectOutcome,
            ) { accountState, discovered, refreshing, err, disconnectOutcome ->
                listOf(accountState, discovered, refreshing, err, disconnectOutcome)
            }.collect { values ->
                @Suppress("UNCHECKED_CAST")
                val accountState = values[0] as AccountState
                @Suppress("UNCHECKED_CAST")
                val discovered = values[1] as List<DiscoveredAccount>
                val refreshing = values[2] as Boolean
                val err = values[3] as AuthFailure?
                val disconnectOutcome = values[4] as DisconnectOutcome?
                val peers = runCatching { controller.installedVerifiedPeers() }.getOrDefault(emptyList())
                val canManage = runCatching { controller.canManageAccountNatively() }.getOrDefault(false)
                val canWrite = runCatching { controller.canWriteProfileNatively() }.getOrDefault(false)
                val activeId = (accountState as? AccountState.SignedIn)?.account?.userId

                val outcomeMsg = when (disconnectOutcome) {
                    DisconnectOutcome.RevokedOnServer ->
                        "Disconnected this app grant on Gothwad server and signed out locally."
                    DisconnectOutcome.OfflineRemovedLocally ->
                        "Offline: signed out locally only. Server grant could not be confirmed revoked."
                    DisconnectOutcome.AlreadyInvalidOnServer ->
                        "Server grant was already expired or revoked; removed locally."
                    null -> null
                }
                if (disconnectOutcome != null) {
                    controller.clearDisconnectOutcome()
                }

                mutableUiState.update { current ->
                    current.copy(
                        accountState = accountState,
                        discoveredAccounts = discovered,
                        isRefreshing = refreshing,
                        authError = err,
                        installedVerifiedPeers = peers,
                        canManageNatively = canManage,
                        canWriteProfileNatively = canWrite,
                        snackbarMessage = outcomeMsg ?: current.snackbarMessage,
                        webSessions = if (activeId == null) emptyList() else current.webSessions,
                        activityFeed = if (activeId == null) emptyList() else current.activityFeed,
                        authorizations = if (activeId == null) emptyList() else current.authorizations,
                    )
                }

                if (canManage && activeId != null && activeId != lastLoadedNativeUserId) {
                    lastLoadedNativeUserId = activeId
                    refreshNativeManagementData()
                } else if (activeId == null) {
                    lastLoadedNativeUserId = null
                }
            }
        }
    }

    fun selectTab(tab: AccountTab) {
        mutableUiState.update { it.copy(selectedTab = tab, subScreenStack = emptyList()) }
    }

    fun openSubScreen(subScreen: AccountSubScreen) {
        mutableUiState.update { it.copy(subScreenStack = it.subScreenStack + subScreen) }
    }

    fun navigateBack(): Boolean {
        var handled = false
        mutableUiState.update { state ->
            if (state.subScreenStack.isNotEmpty()) {
                handled = true
                state.copy(subScreenStack = state.subScreenStack.dropLast(1), searchQuery = "")
            } else {
                state
            }
        }
        return handled
    }

    fun updateSearchQuery(query: String) {
        mutableUiState.update { it.copy(searchQuery = query) }
    }

    fun toggleAccountSwitcher(show: Boolean) {
        if (show) controller.syncDiscoveredAccounts()
        mutableUiState.update { it.copy(showAccountSwitcher = show) }
    }

    fun switchAccount(userId: String) {
        controller.switchTo(userId)
        mutableUiState.update { it.copy(showAccountSwitcher = false) }
    }

    fun openSignOutDialog(account: GothwadAccount) {
        val displayName = account.name?.takeIf { it.isNotBlank() }
            ?: account.username?.takeIf { it.isNotBlank() }
            ?: account.userId
        val email = account.email?.takeIf { it.isNotBlank() }
            ?: account.username?.let { "$it@gothwadtech.com" }
            ?: account.userId
        mutableUiState.update {
            it.copy(
                showAccountSwitcher = false,
                signOutTarget = SignOutDialogTarget(account.userId, displayName, email),
            )
        }
    }

    fun dismissSignOutDialog() {
        mutableUiState.update { it.copy(signOutTarget = null) }
    }

    fun signOutThisApp(userId: String) {
        controller.signOut(userId)
        mutableUiState.update { it.copy(signOutTarget = null, showAccountSwitcher = false) }
    }

    fun disconnectGrantOnServer(userId: String) {
        controller.disconnectFromServer(userId)
        mutableUiState.update { it.copy(signOutTarget = null, showAccountSwitcher = false) }
    }

    fun removeFromDevice(userId: String) {
        controller.removeFromDevice(userId)
        mutableUiState.update { it.copy(signOutTarget = null, showAccountSwitcher = false) }
    }

    fun openEditProfileDialog(show: Boolean) {
        mutableUiState.update { it.copy(showEditProfileDialog = show) }
    }

    fun saveBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSavingProfile = true) }
            val result = controller.updateBasicProfile(firstName, lastName, dateOfBirth, gender)
            mutableUiState.update { current ->
                current.copy(
                    isSavingProfile = false,
                    showEditProfileDialog = result.isFailure,
                    snackbarMessage = if (result.isSuccess) {
                        "Personal info updated"
                    } else {
                        "Could not update personal info: ${result.exceptionOrNull()?.message ?: "try again"}"
                    },
                )
            }
        }
    }

    fun revokeOtherWebSessions() {
        viewModelScope.launch {
            val result = controller.revokeOtherWebSessions()
            result.onSuccess { count ->
                mutableUiState.update { it.copy(snackbarMessage = "Signed out $count other web session(s)") }
                refreshNativeManagementData()
            }.onFailure { err ->
                val msg = if (err.message?.contains("reauthentication_required") == true) {
                    "Recent browser login required to sign out other sessions — opening Security on web"
                } else {
                    "Could not sign out other web sessions (${err.message ?: "error"})"
                }
                mutableUiState.update { it.copy(snackbarMessage = msg) }
            }
        }
    }

    fun revokeAppAuthorization(appId: String) {
        viewModelScope.launch {
            val result = controller.revokeAuthorization(appId)
            result.onSuccess {
                mutableUiState.update { it.copy(snackbarMessage = "Disconnected $appId") }
                refreshNativeManagementData()
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(snackbarMessage = "Failed to disconnect app: ${err.message ?: "error"}")
                }
            }
        }
    }

    fun startSignIn(activity: Activity, chooseAccount: Boolean = false, loginHint: String? = null) {
        mutableUiState.update { it.copy(showAccountSwitcher = false) }
        controller.signIn(activity, chooseAccount, loginHint)
    }

    fun openWebSection(activity: Activity, section: String? = null) {
        controller.manageOnWeb(activity, section)
    }

    fun openWebPath(activity: Activity, path: String) {
        controller.openWebPage(activity, path)
    }

    fun refreshProfile() {
        controller.refreshProfile()
        if (controller.canManageAccountNatively()) {
            refreshNativeManagementData()
        }
    }

    private fun refreshNativeManagementData() {
        viewModelScope.launch {
            val sessionsDeferred = async { controller.listWebSessions().getOrNull() }
            val activityDeferred = async { controller.listActivity().getOrNull() }
            val authsDeferred = async { controller.listAuthorizations().getOrNull() }
            val sessions = sessionsDeferred.await()
            val activity = activityDeferred.await()
            val auths = authsDeferred.await()
            mutableUiState.update { current ->
                current.copy(
                    webSessions = sessions ?: current.webSessions,
                    activityFeed = activity ?: current.activityFeed,
                    authorizations = auths ?: current.authorizations,
                )
            }
        }
    }

    fun syncPeers() {
        controller.syncDiscoveredAccounts()
    }

    fun recoverCorruptedStorage() {
        controller.recoverCorruptedStorage()
    }

    fun clearAuthError() {
        controller.clearError()
    }

    fun showMessage(message: String) {
        mutableUiState.update { it.copy(snackbarMessage = message) }
    }

    fun clearSnackbar() {
        mutableUiState.update { it.copy(snackbarMessage = null) }
    }

    /** Exact settings index matching accounts/web/me/js/search-index.js (no invented settings). */
    fun getSearchIndex(): List<SearchableSetting> = listOf(
        SearchableSetting("home", "Home", "Overview", "dashboard overview home", AccountTab.HOME),
        SearchableSetting("name", "Change your name", "Personal info · Profile", "first last surname display name", AccountTab.PERSONAL_INFO, webSection = "personal"),
        SearchableSetting("photo", "Change your photo", "Personal info · Profile", "avatar picture image crop profile", AccountTab.PERSONAL_INFO, webSection = "personal"),
        SearchableSetting("dob", "Date of birth", "Personal info · Optional details", "birthday dob age birth date", AccountTab.PERSONAL_INFO, webSection = "personal"),
        SearchableSetting("gender", "Gender", "Personal info · Optional details", "optional male female gender", AccountTab.PERSONAL_INFO, webSection = "personal"),
        SearchableSetting("phone", "Phone number", "Personal info · Sign-in identifier", "mobile telephone number", AccountTab.PERSONAL_INFO, webSection = "personal"),
        SearchableSetting("recovery", "Recovery email", "Personal info · Account recovery", "backup email address verify recovery", AccountTab.PERSONAL_INFO, webSection = "personal"),
        SearchableSetting("email", "Account email address", "Personal info · Fixed identifier", "gothwad email username mailbox address", AccountTab.PERSONAL_INFO),
        SearchableSetting("visibility", "Profile visibility", "Personal info · Sharing information", "public private privacy sharing visibility", AccountTab.PERSONAL_INFO),
        SearchableSetting("password", "Add or change password", "Security · Sign-in methods", "password reset update login", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("mfa", "2-Step Verification", "Security · Authenticator", "2fa mfa two factor two step authenticator otp totp verification", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("passkeys", "Passkeys", "Security · Sign-in methods", "fingerprint face biometric security key passwordless", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("devices", "Your devices", "Security · Active sessions", "sessions browsers computers phones device sign out", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("sec_activity", "Recent security activity", "Security · Sign-in history", "login history events activity", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("storage", "Storage", "Data & privacy · Shared quota", "space quota drive mail usage", AccountTab.DATA_PRIVACY, webSection = "data"),
        SearchableSetting("export", "Export your data", "Data & privacy · Account data", "download export backup account data json", AccountTab.DATA_PRIVACY, webSection = "data"),
        SearchableSetting("delete", "Delete your account", "Data & privacy · Account data", "remove close erase delete account", AccountTab.DATA_PRIVACY, webSection = "data"),
        SearchableSetting("contacts", "Gothwad Contacts", "People & sharing", "people contacts address book", AccountTab.PEOPLE_SHARING),
        SearchableSetting("services", "Gothwad Services", "Connected apps", "services products applications mail drive store", AccountTab.CONNECTED_APPS),
        SearchableSetting("disconnect", "Disconnect an app", "Connected apps · Permissions", "revoke permission disconnect third party app oauth", AccountTab.CONNECTED_APPS, webSection = "apps"),
    )
}
