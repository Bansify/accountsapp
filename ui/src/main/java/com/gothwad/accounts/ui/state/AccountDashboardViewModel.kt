package com.gothwad.accounts.ui.state

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.BrowserAuthCompletion
import com.gothwad.accounts.core.ConnectedAppGrant
import com.gothwad.accounts.core.DisconnectOutcome
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.core.IdentifierCheckResult
import com.gothwad.accounts.core.MfaEnrollmentSetup
import com.gothwad.accounts.core.MfaStatusInfo
import com.gothwad.accounts.core.MobileApiException
import com.gothwad.accounts.core.MobileAuthSignInOutcome
import com.gothwad.accounts.core.NativeSignUpInput
import com.gothwad.accounts.core.UsernameCheckResult
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.ActiveManagementDialog
import com.gothwad.accounts.ui.model.NativeAuthDialogState
import com.gothwad.accounts.ui.model.NativeAuthStep
import com.gothwad.accounts.ui.model.NavigationLayoutMode
import com.gothwad.accounts.ui.model.SearchableSetting
import com.gothwad.accounts.ui.model.SignOutDialogTarget
import com.gothwad.accounts.ui.model.ThemePreference
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
    val browserAuthCompletion: StateFlow<BrowserAuthCompletion?>
        get() = MutableStateFlow(null)

    fun signIn(activity: Activity, chooseAccount: Boolean = false, loginHint: String? = null)
    fun signInWithBrowser(activity: Activity, chooseAccount: Boolean = false, loginHint: String? = null) =
        signIn(activity, chooseAccount, loginHint)

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

    suspend fun checkIdentifier(identifier: String): Result<IdentifierCheckResult> =
        Result.failure(UnsupportedOperationException())

    suspend fun checkUsername(username: String): Result<UsernameCheckResult> =
        Result.failure(UnsupportedOperationException())

    suspend fun signInNative(identifier: String, password: String): Result<MobileAuthSignInOutcome> =
        Result.failure(UnsupportedOperationException())

    suspend fun verifyNativeMfa(challenge: String, code: String): Result<GothwadAccount> =
        Result.failure(UnsupportedOperationException())

    suspend fun signUpNative(input: NativeSignUpInput): Result<GothwadAccount> =
        Result.failure(UnsupportedOperationException())

    suspend fun forgotPasswordNative(username: String): Result<String> =
        Result.failure(UnsupportedOperationException())

    suspend fun resetPasswordNative(token: String, newPassword: String): Result<String> =
        Result.failure(UnsupportedOperationException())

    suspend fun signInSilentlyWithPeer(peer: DiscoveredAccount): Result<GothwadAccount> =
        Result.failure(UnsupportedOperationException())

    suspend fun listWebSessions(): Result<List<WebSessionInfo>> = Result.success(emptyList())
    suspend fun revokeOtherWebSessions(): Result<Int> = Result.success(0)
    suspend fun revokeWebSession(sessionId: String): Result<Int> = Result.success(0)
    suspend fun listActivity(): Result<List<AccountActivityEntry>> = Result.success(emptyList())
    suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> = Result.success(emptyList())
    suspend fun revokeAuthorization(appId: String): Result<Unit> = Result.success(Unit)
    suspend fun updateBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ): Result<GothwadAccount?> = Result.failure(UnsupportedOperationException())

    suspend fun uploadAvatar(imageDataOrBase64: String, mime: String = "image/jpeg"): Result<GothwadAccount?> =
        Result.failure(UnsupportedOperationException())

    suspend fun removeAvatar(): Result<GothwadAccount?> =
        Result.failure(UnsupportedOperationException())

    suspend fun updatePhoneNumber(
        phoneNumber: String,
        currentPassword: String,
        mfaCode: String? = null,
    ): Result<GothwadAccount?> = Result.failure(UnsupportedOperationException())

    suspend fun requestRecoveryEmailChange(
        recoveryEmail: String,
        currentPassword: String,
        mfaCode: String? = null,
    ): Result<String> = Result.failure(UnsupportedOperationException())

    suspend fun verifyRecoveryEmailChange(token: String): Result<GothwadAccount?> =
        Result.failure(UnsupportedOperationException())

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
        mfaCode: String? = null,
    ): Result<String> = Result.failure(UnsupportedOperationException())

    suspend fun getMfaStatus(): Result<MfaStatusInfo> =
        Result.failure(UnsupportedOperationException())

    suspend fun beginMfaEnrollment(currentPassword: String): Result<MfaEnrollmentSetup> =
        Result.failure(UnsupportedOperationException())

    suspend fun confirmMfaEnrollment(
        currentPassword: String,
        enrollmentId: String,
        code: String,
    ): Result<List<String>> = Result.failure(UnsupportedOperationException())

    suspend fun regenerateMfaRecoveryCodes(currentPassword: String, code: String): Result<List<String>> =
        Result.failure(UnsupportedOperationException())

    suspend fun disableMfa(currentPassword: String, code: String): Result<Unit> =
        Result.failure(UnsupportedOperationException())

    suspend fun exportAccountDataJson(): Result<String> =
        Result.failure(UnsupportedOperationException())

    suspend fun deleteAccount(password: String, mfaCode: String? = null): Result<Unit> =
        Result.failure(UnsupportedOperationException())
}

object LiveGothwadAccountController : AccountSessionController {
    override val state: StateFlow<AccountState> get() = GothwadAccounts.state
    override val discoveredAccounts: StateFlow<List<DiscoveredAccount>> get() = GothwadAccounts.discoveredAccounts
    override val isRefreshing: StateFlow<Boolean> get() = GothwadAccounts.isRefreshing
    override val error: StateFlow<AuthFailure?> get() = GothwadAccounts.error
    override val lastDisconnectOutcome: StateFlow<DisconnectOutcome?> get() = GothwadAccounts.lastDisconnectOutcome
    override val browserAuthCompletion: StateFlow<BrowserAuthCompletion?> get() = GothwadAccounts.browserAuthCompletion

    override fun signIn(activity: Activity, chooseAccount: Boolean, loginHint: String?) {
        GothwadAccounts.signIn(activity, chooseAccount, loginHint)
    }

    override fun signInWithBrowser(activity: Activity, chooseAccount: Boolean, loginHint: String?) {
        GothwadAccounts.signInWithBrowser(activity, chooseAccount, loginHint)
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

    override suspend fun checkIdentifier(identifier: String) = GothwadAccounts.checkIdentifier(identifier)
    override suspend fun checkUsername(username: String) = GothwadAccounts.checkUsername(username)
    override suspend fun signInNative(identifier: String, password: String) =
        GothwadAccounts.signInNative(identifier, password)

    override suspend fun verifyNativeMfa(challenge: String, code: String) =
        GothwadAccounts.verifyNativeMfa(challenge, code)

    override suspend fun signUpNative(input: NativeSignUpInput) = GothwadAccounts.signUpNative(input)
    override suspend fun forgotPasswordNative(username: String) = GothwadAccounts.forgotPasswordNative(username)
    override suspend fun resetPasswordNative(token: String, newPassword: String) =
        GothwadAccounts.resetPasswordNative(token, newPassword)

    override suspend fun signInSilentlyWithPeer(peer: DiscoveredAccount) =
        GothwadAccounts.signInSilentlyWithPeer(peer)

    override suspend fun listWebSessions(): Result<List<WebSessionInfo>> = GothwadAccounts.listWebSessions()
    override suspend fun revokeOtherWebSessions(): Result<Int> = GothwadAccounts.revokeOtherWebSessions()
    override suspend fun revokeWebSession(sessionId: String): Result<Int> = GothwadAccounts.revokeWebSession(sessionId)
    override suspend fun listActivity(): Result<List<AccountActivityEntry>> = GothwadAccounts.listActivity()
    override suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> = GothwadAccounts.listAuthorizations()
    override suspend fun revokeAuthorization(appId: String): Result<Unit> = GothwadAccounts.revokeAuthorization(appId)
    override suspend fun updateBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ): Result<GothwadAccount?> = GothwadAccounts.updateBasicProfile(firstName, lastName, dateOfBirth, gender)

    override suspend fun uploadAvatar(imageDataOrBase64: String, mime: String) =
        GothwadAccounts.uploadAvatar(imageDataOrBase64, mime)

    override suspend fun removeAvatar() = GothwadAccounts.removeAvatar()
    override suspend fun updatePhoneNumber(phoneNumber: String, currentPassword: String, mfaCode: String?) =
        GothwadAccounts.updatePhoneNumber(phoneNumber, currentPassword, mfaCode)

    override suspend fun requestRecoveryEmailChange(recoveryEmail: String, currentPassword: String, mfaCode: String?) =
        GothwadAccounts.requestRecoveryEmailChange(recoveryEmail, currentPassword, mfaCode)

    override suspend fun verifyRecoveryEmailChange(token: String) =
        GothwadAccounts.verifyRecoveryEmailChange(token)

    override suspend fun changePassword(currentPassword: String, newPassword: String, mfaCode: String?) =
        GothwadAccounts.changePassword(currentPassword, newPassword, mfaCode)

    override suspend fun getMfaStatus() = GothwadAccounts.getMfaStatus()
    override suspend fun beginMfaEnrollment(currentPassword: String) =
        GothwadAccounts.beginMfaEnrollment(currentPassword)

    override suspend fun confirmMfaEnrollment(currentPassword: String, enrollmentId: String, code: String) =
        GothwadAccounts.confirmMfaEnrollment(currentPassword, enrollmentId, code)

    override suspend fun regenerateMfaRecoveryCodes(currentPassword: String, code: String) =
        GothwadAccounts.regenerateMfaRecoveryCodes(currentPassword, code)

    override suspend fun disableMfa(currentPassword: String, code: String) =
        GothwadAccounts.disableMfa(currentPassword, code)

    override suspend fun exportAccountDataJson() = GothwadAccounts.exportAccountDataJson()
    override suspend fun deleteAccount(password: String, mfaCode: String?) =
        GothwadAccounts.deleteAccount(password, mfaCode)
}

class AccountDashboardViewModel(
    private val controller: AccountSessionController = LiveGothwadAccountController,
) : ViewModel() {

    private data class ControllerSnapshot(
        val accountState: AccountState,
        val discoveredAccounts: List<DiscoveredAccount>,
        val isRefreshing: Boolean,
        val error: AuthFailure?,
        val disconnectOutcome: DisconnectOutcome?,
    )

    private data class SignInSessionTracker(
        val sessionKey: String,
        val baselineCaptured: Boolean = false,
        val baselineSuccessCount: Int = 0,
        val baselineUserIds: Set<String> = emptySet(),
        val baselineActiveUserId: String? = null,
        val completionDispatched: Boolean = false,
    )

    private var lastLoadedNativeUserId: String? = null
    private var lastActiveUserId: String? = (controller.state.value as? AccountState.SignedIn)?.account?.userId
    private var lastObservedBrowserAuthGeneration: Int = controller.browserAuthCompletion.value?.generation ?: 0
    private var nativeRefreshGeneration: Long = 0L
    private var activeManagementOpId: Long = 0L
    private var activeProfileSaveOpId: Long = 0L
    private var activeExportOpId: Long = 0L
    private var signInSessionTracker: SignInSessionTracker? = null
    private val nativeScopeDeniedUserIds = mutableSetOf<String>()

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
                combine(
                    controller.state,
                    controller.discoveredAccounts,
                    controller.isRefreshing,
                    controller.error,
                    controller.lastDisconnectOutcome,
                ) { accountState, discovered, refreshing, err, disconnectOutcome ->
                    ControllerSnapshot(accountState, discovered, refreshing, err, disconnectOutcome)
                },
                controller.browserAuthCompletion,
            ) { snap, browserCompletion ->
                snap to browserCompletion
            }.collect { (snap, browserCompletion) ->
                val accountState = snap.accountState
                val discovered = snap.discoveredAccounts
                val refreshing = snap.isRefreshing
                val err = snap.error
                val disconnectOutcome = snap.disconnectOutcome
                val peers = runCatching { controller.installedVerifiedPeers() }.getOrDefault(emptyList())
                val activeId = (accountState as? AccountState.SignedIn)?.account?.userId
                val browserCompleted =
                    browserCompletion != null && browserCompletion.generation > lastObservedBrowserAuthGeneration
                if (browserCompleted) {
                    lastObservedBrowserAuthGeneration = browserCompletion.generation
                    nativeScopeDeniedUserIds.remove(browserCompletion.userId)
                }
                val activeUserChanged = activeId != lastActiveUserId
                if (activeUserChanged) {
                    lastActiveUserId = activeId
                    activeManagementOpId++
                    activeProfileSaveOpId++
                    activeExportOpId++
                }
                val denied = activeId != null && activeId in nativeScopeDeniedUserIds
                val canManage = !denied && runCatching { controller.canManageAccountNatively() }.getOrDefault(false)
                val canWrite = !denied && runCatching { controller.canWriteProfileNatively() }.getOrDefault(false)

                val outcomeMsg = when (disconnectOutcome) {
                    DisconnectOutcome.RevokedOnServer ->
                        "Disconnected this app grant on Bansify server and signed out locally."
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
                        authSuccessCount = if (browserCompleted) current.authSuccessCount + 1 else current.authSuccessCount,
                        installedVerifiedPeers = peers,
                        canManageNatively = canManage,
                        canWriteProfileNatively = canWrite,
                        snackbarMessage = outcomeMsg ?: current.snackbarMessage,
                        isSavingProfile = if (activeUserChanged) false else current.isSavingProfile,
                        isExportingData = if (activeUserChanged) false else current.isExportingData,
                        isManagementBusy = if (activeUserChanged) false else current.isManagementBusy,
                        activeManagementDialog = if (activeUserChanged) null else current.activeManagementDialog,
                        managementError = if (activeUserChanged) null else current.managementError,
                        managementRequiresMfaPrompt = if (activeUserChanged) false else current.managementRequiresMfaPrompt,
                        recoveryEmailPendingVerification = if (activeUserChanged) false else current.recoveryEmailPendingVerification,
                        mfaSetup = if (activeUserChanged) null else current.mfaSetup,
                        mfaRecoveryCodes = if (activeUserChanged) emptyList() else current.mfaRecoveryCodes,
                        webSessions = if (activeId == null) emptyList() else current.webSessions,
                        activityFeed = if (activeId == null) emptyList() else current.activityFeed,
                        authorizations = if (activeId == null) emptyList() else current.authorizations,
                    )
                }

                val completedForActive = browserCompleted && browserCompletion.userId == activeId
                if (canManage && activeId != null && (activeId != lastLoadedNativeUserId || completedForActive)) {
                    lastLoadedNativeUserId = activeId
                    refreshNativeManagementData()
                } else if (activeId == null) {
                    lastLoadedNativeUserId = null
                    nativeRefreshGeneration++
                }
            }
        }
    }

    fun selectTab(tab: AccountTab) {
        mutableUiState.update {
            it.copy(
                selectedTab = tab,
                subScreenStack = emptyList(),
                showOthersHub = false,
            )
        }
    }

    fun openOthersHub() {
        mutableUiState.update {
            it.copy(
                showOthersHub = true,
                subScreenStack = emptyList(),
            )
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        mutableUiState.update { it.copy(themePreference = preference) }
    }

    fun setNavigationLayout(mode: NavigationLayoutMode) {
        mutableUiState.update { current ->
            current.copy(
                navigationLayout = mode,
                showOthersHub = if (mode == NavigationLayoutMode.BOTTOM_BAR) current.showOthersHub else false,
            )
        }
    }

    fun openSubScreen(subScreen: AccountSubScreen) {
        mutableUiState.update { current ->
            if (current.subScreenStack.lastOrNull() == subScreen) {
                current
            } else {
                current.copy(subScreenStack = current.subScreenStack + subScreen)
            }
        }
    }

    fun navigateBack(): Boolean {
        var handled = false
        mutableUiState.update { state ->
            when {
                state.subScreenStack.isNotEmpty() -> {
                    handled = true
                    state.copy(subScreenStack = state.subScreenStack.dropLast(1), searchQuery = "")
                }
                state.navigationLayout == NavigationLayoutMode.BOTTOM_BAR &&
                    !state.showOthersHub &&
                    (state.selectedTab == AccountTab.PEOPLE_SHARING || state.selectedTab == AccountTab.CONNECTED_APPS) -> {
                    handled = true
                    state.copy(showOthersHub = true)
                }
                else -> state
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

    // -------------------------------------------------------------------------
    // Direct Native Authentication + Cross-App Silent Peer SSO
    // -------------------------------------------------------------------------

    fun openNativeAuthDialog(
        initialIdentifier: String = "",
        startStep: NativeAuthStep = NativeAuthStep.IDENTIFIER,
    ) {
        mutableUiState.update {
            it.copy(
                showAccountSwitcher = false,
                nativeAuth = NativeAuthDialogState(
                    visible = true,
                    step = startStep,
                    identifier = initialIdentifier.trim(),
                ),
            )
        }
    }

    /** Configures inline native auth flow state (e.g. for [AccountSignInActivity] / [GothwadSignInScreen]). */
    fun configureNativeAuthEntry(
        startStep: NativeAuthStep = NativeAuthStep.IDENTIFIER,
        initialIdentifier: String = "",
    ) {
        mutableUiState.update {
            it.copy(
                showAccountSwitcher = false,
                nativeAuth = NativeAuthDialogState(
                    visible = false,
                    step = startStep,
                    identifier = initialIdentifier.trim(),
                ),
            )
        }
    }

    /**
     * Binds an embedded or standalone [GothwadSignInScreen] session to this ViewModel so configuration
     * changes preserve in-flight MFA/form state while process death recreates entry state and a matching
     * completion baseline cleanly.
     */
    fun ensureSignInSessionInitialized(
        sessionKey: String,
        startStep: NativeAuthStep = NativeAuthStep.IDENTIFIER,
        initialIdentifier: String = "",
    ) {
        if (signInSessionTracker?.sessionKey == sessionKey) return
        signInSessionTracker = SignInSessionTracker(sessionKey = sessionKey)
        configureNativeAuthEntry(startStep, initialIdentifier)
        syncPeers()
    }

    fun evaluateSignInCompletion(
        sessionKey: String,
        allowWhenAlreadySignedIn: Boolean,
        startedBrowserFlow: Boolean,
    ): Boolean {
        val currentState = mutableUiState.value
        val accountState = currentState.accountState
        if (accountState == AccountState.Initializing) return false

        val existingTracker = signInSessionTracker?.takeIf { it.sessionKey == sessionKey }
            ?: SignInSessionTracker(sessionKey = sessionKey).also { signInSessionTracker = it }

        if (!existingTracker.baselineCaptured) {
            val signedIn = accountState as? AccountState.SignedIn
            val shouldFinishImmediately = !allowWhenAlreadySignedIn && signedIn != null
            signInSessionTracker = existingTracker.copy(
                baselineCaptured = true,
                baselineSuccessCount = currentState.authSuccessCount,
                baselineUserIds = signedIn?.all?.map { it.userId }?.toSet().orEmpty(),
                baselineActiveUserId = signedIn?.account?.userId,
                completionDispatched = shouldFinishImmediately,
            )
            return shouldFinishImmediately
        }

        if (existingTracker.completionDispatched) return false

        val signedIn = accountState as? AccountState.SignedIn
        val currentIds = signedIn?.all?.map { it.userId }?.toSet().orEmpty()
        val hasNewAuthSuccess = currentState.authSuccessCount > existingTracker.baselineSuccessCount
        val becameSignedInFromSignedOut = existingTracker.baselineUserIds.isEmpty() && signedIn != null
        val addedNewLocalAccount = (currentIds - existingTracker.baselineUserIds).isNotEmpty()
        val switchedAfterBrowser =
            startedBrowserFlow && signedIn != null && signedIn.account.userId != existingTracker.baselineActiveUserId

        if (hasNewAuthSuccess || becameSignedInFromSignedOut || addedNewLocalAccount || switchedAfterBrowser) {
            signInSessionTracker = existingTracker.copy(completionDispatched = true)
            return true
        }
        return false
    }

    fun dismissNativeAuthDialog() {
        mutableUiState.update { it.copy(nativeAuth = NativeAuthDialogState()) }
    }

    fun setNativeAuthStep(step: NativeAuthStep) {
        mutableUiState.update {
            it.copy(
                nativeAuth = it.nativeAuth.copy(
                    step = step,
                    errorMessage = null,
                    infoMessage = null,
                    usernameAvailable = null,
                    usernameStatusMessage = null,
                ),
            )
        }
    }

    fun submitNativeIdentifier(identifier: String) {
        val clean = identifier.trim()
        if (clean.isEmpty()) {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(errorMessage = "Enter your username, email, or phone number."))
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(isBusy = true, errorMessage = null, infoMessage = null, identifier = clean))
            }
            val result = controller.checkIdentifier(clean)
            result.onSuccess { check ->
                if (check.exists) {
                    mutableUiState.update {
                        it.copy(
                            nativeAuth = it.nativeAuth.copy(
                                isBusy = false,
                                step = NativeAuthStep.PASSWORD,
                                identifier = clean,
                                errorMessage = null,
                            ),
                        )
                    }
                } else {
                    mutableUiState.update {
                        it.copy(
                            nativeAuth = it.nativeAuth.copy(
                                isBusy = false,
                                errorMessage = check.error ?: "Account not found. Check spelling or create an account.",
                            ),
                        )
                    }
                }
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(nativeAuth = it.nativeAuth.copy(isBusy = false, errorMessage = err.message ?: "Could not verify account."))
                }
            }
        }
    }

    fun checkNativeUsername(username: String) {
        val clean = username.trim().lowercase()
        if (clean.isEmpty()) {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(usernameAvailable = null, usernameStatusMessage = null))
            }
            return
        }
        viewModelScope.launch {
            val result = controller.checkUsername(clean)
            result.onSuccess { check ->
                val msg = when {
                    !check.valid -> check.error ?: "Use 3-24 letters and numbers only"
                    check.available -> "Available: ${check.email ?: "$clean@bansify.com"}"
                    else -> check.error ?: "Username already taken"
                }
                mutableUiState.update {
                    it.copy(
                        nativeAuth = it.nativeAuth.copy(
                            usernameAvailable = check.valid && check.available,
                            usernameStatusMessage = msg,
                        ),
                    )
                }
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(
                        nativeAuth = it.nativeAuth.copy(
                            usernameAvailable = false,
                            usernameStatusMessage = err.message ?: "Could not check username",
                        ),
                    )
                }
            }
        }
    }

    fun submitNativePassword(identifier: String, password: String) {
        val cleanId = identifier.trim()
        if (cleanId.isEmpty() || password.isEmpty()) {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(errorMessage = "Enter your account and password."))
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(isBusy = true, errorMessage = null, infoMessage = null))
            }
            val result = controller.signInNative(cleanId, password)
            result.onSuccess { outcome ->
                when (outcome) {
                    is MobileAuthSignInOutcome.Authenticated -> {
                        val signedUserId = outcome.user?.id ?: uiState.value.activeAccount?.userId
                        if (signedUserId != null) nativeScopeDeniedUserIds.remove(signedUserId)
                        mutableUiState.update {
                            it.copy(
                                nativeAuth = NativeAuthDialogState(),
                                authSuccessCount = it.authSuccessCount + 1,
                                snackbarMessage = "Signed in as ${outcome.user?.email ?: cleanId}",
                            )
                        }
                        refreshNativeManagementData()
                    }
                    is MobileAuthSignInOutcome.MfaRequired -> {
                        mutableUiState.update {
                            it.copy(
                                nativeAuth = it.nativeAuth.copy(
                                    isBusy = false,
                                    step = NativeAuthStep.MFA,
                                    mfaChallenge = outcome.challenge,
                                    mfaPrompt = outcome.message,
                                    errorMessage = null,
                                ),
                            )
                        }
                    }
                }
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(nativeAuth = it.nativeAuth.copy(isBusy = false, errorMessage = err.message ?: "Sign-in failed."))
                }
            }
        }
    }

    fun submitNativeMfa(code: String) {
        val challenge = mutableUiState.value.nativeAuth.mfaChallenge
        val cleanCode = code.trim()
        if (challenge.isBlank() || cleanCode.isEmpty()) {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(errorMessage = "Enter your 6-digit authenticator or recovery code."))
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(nativeAuth = it.nativeAuth.copy(isBusy = true, errorMessage = null)) }
            val result = controller.verifyNativeMfa(challenge, cleanCode)
            result.onSuccess { acc ->
                nativeScopeDeniedUserIds.remove(acc.userId)
                mutableUiState.update {
                    it.copy(
                        nativeAuth = NativeAuthDialogState(),
                        authSuccessCount = it.authSuccessCount + 1,
                        snackbarMessage = "Signed in as ${acc.email ?: acc.username ?: acc.userId}",
                    )
                }
                refreshNativeManagementData()
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(nativeAuth = it.nativeAuth.copy(isBusy = false, errorMessage = err.message ?: "Verification failed."))
                }
            }
        }
    }

    fun submitNativeSignUp(input: NativeSignUpInput) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(nativeAuth = it.nativeAuth.copy(isBusy = true, errorMessage = null)) }
            val result = controller.signUpNative(input)
            result.onSuccess { acc ->
                nativeScopeDeniedUserIds.remove(acc.userId)
                mutableUiState.update {
                    it.copy(
                        nativeAuth = NativeAuthDialogState(),
                        authSuccessCount = it.authSuccessCount + 1,
                        snackbarMessage = "Welcome to Bansify, ${acc.firstName ?: acc.username ?: acc.userId}",
                    )
                }
                refreshNativeManagementData()
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(nativeAuth = it.nativeAuth.copy(isBusy = false, errorMessage = err.message ?: "Could not create account."))
                }
            }
        }
    }

    fun submitForgotPassword(username: String) {
        val clean = username.trim().lowercase()
        if (clean.isEmpty()) {
            mutableUiState.update {
                it.copy(nativeAuth = it.nativeAuth.copy(errorMessage = "Enter your username first."))
            }
            return
        }
        viewModelScope.launch {
            mutableUiState.update { it.copy(nativeAuth = it.nativeAuth.copy(isBusy = true, errorMessage = null)) }
            val result = controller.forgotPasswordNative(clean)
            result.onSuccess { message ->
                mutableUiState.update {
                    it.copy(
                        nativeAuth = it.nativeAuth.copy(
                            isBusy = false,
                            step = NativeAuthStep.RESET_PASSWORD,
                            identifier = clean,
                            infoMessage = message,
                            errorMessage = null,
                        ),
                    )
                }
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(nativeAuth = it.nativeAuth.copy(isBusy = false, errorMessage = err.message ?: "Reset request failed."))
                }
            }
        }
    }

    fun submitResetPassword(token: String, newPassword: String) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(nativeAuth = it.nativeAuth.copy(isBusy = true, errorMessage = null)) }
            val result = controller.resetPasswordNative(token, newPassword)
            result.onSuccess { message ->
                mutableUiState.update {
                    it.copy(
                        nativeAuth = it.nativeAuth.copy(
                            isBusy = false,
                            step = NativeAuthStep.PASSWORD,
                            infoMessage = message,
                            errorMessage = null,
                        ),
                    )
                }
            }.onFailure { err ->
                mutableUiState.update {
                    it.copy(nativeAuth = it.nativeAuth.copy(isBusy = false, errorMessage = err.message ?: "Could not reset password."))
                }
            }
        }
    }

    /**
     * Attempts zero-browser silent cross-app peer SSO via `issue_peer_code` + PKCE `/oauth/token`.
     * Falls back to prefilled native Sign-In dialog only when silent peer SSO fails.
     */
    fun continueWithPeerAccount(peer: DiscoveredAccount) {
        viewModelScope.launch {
            mutableUiState.update { it.copy(showAccountSwitcher = false, isPeerSilentSigningIn = true) }
            val result = controller.signInSilentlyWithPeer(peer)
            result.onSuccess { acc ->
                nativeScopeDeniedUserIds.remove(acc.userId)
                mutableUiState.update {
                    it.copy(
                        isPeerSilentSigningIn = false,
                        authSuccessCount = it.authSuccessCount + 1,
                        snackbarMessage = "Signed in silently as ${acc.email ?: acc.username ?: acc.userId}",
                    )
                }
                refreshNativeManagementData()
            }.onFailure {
                val hint = peer.email ?: peer.username.orEmpty()
                mutableUiState.update {
                    it.copy(
                        isPeerSilentSigningIn = false,
                        nativeAuth = NativeAuthDialogState(
                            visible = true,
                            step = if (hint.isNotBlank()) NativeAuthStep.PASSWORD else NativeAuthStep.IDENTIFIER,
                            identifier = hint,
                            infoMessage = "Verify your password to sign in as ${peer.email ?: peer.username ?: peer.userId}.",
                        ),
                    )
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Local Sign-out, Device Removal & Server Disconnect
    // -------------------------------------------------------------------------

    fun openSignOutDialog(account: GothwadAccount) {
        val displayName = account.name?.takeIf { it.isNotBlank() }
            ?: account.username?.takeIf { it.isNotBlank() }
            ?: account.userId
        val email = account.email?.takeIf { it.isNotBlank() }
            ?: account.username?.let { "$it@bansify.com" }
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

    // -------------------------------------------------------------------------
    // Native Account & Security Management Dialogs (`/api/mobile/*`)
    // -------------------------------------------------------------------------

    fun openEditProfileDialog(show: Boolean) {
        if (show) {
            openManagementDialog(ActiveManagementDialog.EditBasicProfile)
        } else {
            dismissManagementDialog()
        }
    }

    fun openManagementDialog(dialog: ActiveManagementDialog) {
        mutableUiState.update {
            it.copy(
                activeManagementDialog = dialog,
                isManagementBusy = false,
                managementError = null,
                managementRequiresMfaPrompt = it.activeAccount?.mfaEnabled == true,
                recoveryEmailPendingVerification = false,
                mfaSetup = null,
                mfaRecoveryCodes = emptyList(),
            )
        }
        if (dialog is ActiveManagementDialog.TwoFactor) {
            loadMfaStatus()
        }
    }

    fun dismissManagementDialog() {
        mutableUiState.update {
            it.copy(
                activeManagementDialog = null,
                isManagementBusy = false,
                managementError = null,
                managementRequiresMfaPrompt = false,
                recoveryEmailPendingVerification = false,
                mfaSetup = null,
                mfaRecoveryCodes = emptyList(),
            )
        }
    }

    fun saveBasicProfile(
        firstName: String,
        lastName: String,
        dateOfBirth: String,
        gender: String,
    ) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeProfileSaveOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isSavingProfile = true) }
            val result = controller.updateBasicProfile(firstName, lastName, dateOfBirth, gender)
            val err = result.exceptionOrNull()
            if (isInsufficientScopeError(err)) {
                if (opId == activeProfileSaveOpId && initiatingUserId == mutableUiState.value.activeAccount?.userId) {
                    mutableUiState.update { it.copy(isSavingProfile = false) }
                }
                downgradeNativeManagementForUser(
                    initiatingUserId,
                    "Native management requires account scopes; use Manage on web.",
                )
                return@launch
            }
            if (opId != activeProfileSaveOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                return@launch
            }
            mutableUiState.update { current ->
                current.copy(
                    isSavingProfile = false,
                    activeManagementDialog = if (result.isFailure) ActiveManagementDialog.EditBasicProfile else null,
                    snackbarMessage = if (result.isSuccess) {
                        "Personal info updated"
                    } else {
                        "Could not update personal info: ${err?.message ?: "try again"}"
                    },
                )
            }
        }
    }

    fun uploadAvatar(imageDataOrBase64: String, mime: String = "image/jpeg") {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.uploadAvatar(imageDataOrBase64, mime)
            result.onSuccess {
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        activeManagementDialog = null,
                        snackbarMessage = "Profile photo updated",
                    )
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Failed to upload photo", initiatingUserId, opId)
            }
        }
    }

    fun removeAvatar() {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.removeAvatar()
            result.onSuccess {
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        activeManagementDialog = null,
                        snackbarMessage = "Profile photo removed",
                    )
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Failed to remove photo", initiatingUserId, opId)
            }
        }
    }

    fun updatePhoneNumber(phoneNumber: String, currentPassword: String, mfaCode: String?) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.updatePhoneNumber(phoneNumber, currentPassword, mfaCode)
            result.onSuccess {
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        activeManagementDialog = null,
                        managementRequiresMfaPrompt = false,
                        snackbarMessage = if (phoneNumber.isBlank()) "Phone number removed" else "Phone number updated",
                    )
                }
            }.onFailure { err ->
                handleManagementFailure(err, "Could not update phone number", initiatingUserId, opId)
            }
        }
    }

    fun requestRecoveryEmailChange(recoveryEmail: String, currentPassword: String, mfaCode: String?) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.requestRecoveryEmailChange(recoveryEmail, currentPassword, mfaCode)
            result.onSuccess { msg ->
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        recoveryEmailPendingVerification = true,
                        managementError = null,
                        snackbarMessage = msg,
                    )
                }
            }.onFailure { err ->
                handleManagementFailure(err, "Could not request recovery email change", initiatingUserId, opId)
            }
        }
    }

    fun verifyRecoveryEmailChange(token: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.verifyRecoveryEmailChange(token)
            result.onSuccess {
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        activeManagementDialog = null,
                        recoveryEmailPendingVerification = false,
                        snackbarMessage = "Recovery email verified",
                    )
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Verification token invalid or expired", initiatingUserId, opId)
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String, mfaCode: String?) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.changePassword(currentPassword, newPassword, mfaCode)
            result.onSuccess { message ->
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        activeManagementDialog = null,
                        managementRequiresMfaPrompt = false,
                        snackbarMessage = message,
                    )
                }
                refreshNativeManagementData()
            }.onFailure { err ->
                handleManagementFailure(err, "Could not update password", initiatingUserId, opId)
            }
        }
    }

    fun loadMfaStatus() {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        viewModelScope.launch {
            val result = controller.getMfaStatus()
            result.onSuccess { status ->
                if (initiatingUserId != mutableUiState.value.activeAccount?.userId) return@onSuccess
                mutableUiState.update { it.copy(mfaStatus = status) }
            }.onFailure { err ->
                if (isInsufficientScopeError(err)) {
                    downgradeNativeManagementForUser(initiatingUserId)
                }
            }
        }
    }

    fun beginMfaEnrollment(currentPassword: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.beginMfaEnrollment(currentPassword)
            result.onSuccess { setup ->
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(isManagementBusy = false, mfaSetup = setup, managementError = null)
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Could not start 2FA enrollment", initiatingUserId, opId)
            }
        }
    }

    fun confirmMfaEnrollment(currentPassword: String, enrollmentId: String, code: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.confirmMfaEnrollment(currentPassword, enrollmentId, code)
            result.onSuccess { codes ->
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        mfaSetup = null,
                        mfaRecoveryCodes = codes,
                        mfaStatus = MfaStatusInfo(enabled = true, enrollmentAvailable = true, recoveryCodesRemaining = codes.size),
                        snackbarMessage = "2-Step Verification enabled",
                    )
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Invalid verification code", initiatingUserId, opId)
            }
        }
    }

    fun regenerateMfaRecoveryCodes(currentPassword: String, code: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.regenerateMfaRecoveryCodes(currentPassword, code)
            result.onSuccess { codes ->
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        mfaRecoveryCodes = codes,
                        mfaStatus = MfaStatusInfo(enabled = true, enrollmentAvailable = true, recoveryCodesRemaining = codes.size),
                        snackbarMessage = "New recovery codes generated",
                    )
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Could not regenerate recovery codes", initiatingUserId, opId)
            }
        }
    }

    fun disableMfa(currentPassword: String, code: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.disableMfa(currentPassword, code)
            result.onSuccess {
                if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        mfaSetup = null,
                        mfaRecoveryCodes = emptyList(),
                        mfaStatus = MfaStatusInfo(enabled = false, enrollmentAvailable = true, recoveryCodesRemaining = 0),
                        activeManagementDialog = null,
                        snackbarMessage = "2-Step Verification disabled",
                    )
                }
            }.onFailure { err ->
                handleSimpleManagementFailure(err, "Could not disable 2-Step Verification", initiatingUserId, opId)
            }
        }
    }

    fun exportAccountData() {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeExportOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isExportingData = true) }
            val result = controller.exportAccountDataJson()
            result.onSuccess { jsonText ->
                if (opId != activeExportOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isExportingData = false,
                        activeManagementDialog = ActiveManagementDialog.ExportData(jsonText),
                    )
                }
            }.onFailure { err ->
                if (opId == activeExportOpId && initiatingUserId == mutableUiState.value.activeAccount?.userId) {
                    mutableUiState.update { it.copy(isExportingData = false) }
                }
                if (isInsufficientScopeError(err)) {
                    downgradeNativeManagementForUser(
                        initiatingUserId,
                        "Native management requires account scopes; use Manage on web.",
                    )
                } else if (opId == activeExportOpId && initiatingUserId == mutableUiState.value.activeAccount?.userId) {
                    mutableUiState.update {
                        it.copy(snackbarMessage = "Export failed: ${err.message ?: "try again"}")
                    }
                }
            }
        }
    }

    fun deleteAccount(password: String, mfaCode: String?) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        val opId = ++activeManagementOpId
        viewModelScope.launch {
            mutableUiState.update { it.copy(isManagementBusy = true, managementError = null) }
            val result = controller.deleteAccount(password, mfaCode)
            result.onSuccess {
                val currentActiveId = mutableUiState.value.activeAccount?.userId
                if (currentActiveId != null && (opId != activeManagementOpId || initiatingUserId != currentActiveId)) {
                    return@onSuccess
                }
                mutableUiState.update {
                    it.copy(
                        isManagementBusy = false,
                        activeManagementDialog = null,
                        managementRequiresMfaPrompt = false,
                        snackbarMessage = "Bansify Account permanently deleted",
                    )
                }
            }.onFailure { err ->
                handleManagementFailure(err, "Could not delete account", initiatingUserId, opId)
            }
        }
    }

    fun revokeWebSession(sessionId: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        viewModelScope.launch {
            val result = controller.revokeWebSession(sessionId)
            result.onSuccess {
                if (initiatingUserId != mutableUiState.value.activeAccount?.userId) return@onSuccess
                mutableUiState.update { it.copy(snackbarMessage = "Session signed out") }
                refreshNativeManagementData()
            }.onFailure { err ->
                if (isInsufficientScopeError(err)) {
                    downgradeNativeManagementForUser(
                        initiatingUserId,
                        "Native management requires account scopes; use Manage on web.",
                    )
                } else if (initiatingUserId == mutableUiState.value.activeAccount?.userId) {
                    mutableUiState.update {
                        it.copy(snackbarMessage = "Could not sign out session: ${err.message ?: "error"}")
                    }
                }
            }
        }
    }

    fun revokeOtherWebSessions() {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        viewModelScope.launch {
            val result = controller.revokeOtherWebSessions()
            result.onSuccess { count ->
                if (initiatingUserId != mutableUiState.value.activeAccount?.userId) return@onSuccess
                mutableUiState.update { it.copy(snackbarMessage = "Signed out $count other web session(s)") }
                refreshNativeManagementData()
            }.onFailure { err ->
                if (isInsufficientScopeError(err)) {
                    downgradeNativeManagementForUser(
                        initiatingUserId,
                        "Native management requires account scopes; use Manage on web.",
                    )
                } else if (initiatingUserId == mutableUiState.value.activeAccount?.userId) {
                    mutableUiState.update {
                        it.copy(snackbarMessage = "Could not sign out other web sessions: ${err.message ?: "error"}")
                    }
                }
            }
        }
    }

    fun revokeAppAuthorization(appId: String) {
        val initiatingUserId = mutableUiState.value.activeAccount?.userId
        viewModelScope.launch {
            val result = controller.revokeAuthorization(appId)
            result.onSuccess {
                if (initiatingUserId != mutableUiState.value.activeAccount?.userId) return@onSuccess
                mutableUiState.update { it.copy(snackbarMessage = "Disconnected $appId") }
                refreshNativeManagementData()
            }.onFailure { err ->
                if (isInsufficientScopeError(err)) {
                    downgradeNativeManagementForUser(
                        initiatingUserId,
                        "Native management requires account scopes; use Manage on web.",
                    )
                } else if (initiatingUserId == mutableUiState.value.activeAccount?.userId) {
                    mutableUiState.update {
                        it.copy(snackbarMessage = "Failed to disconnect app: ${err.message ?: "error"}")
                    }
                }
            }
        }
    }

    fun startSignIn(activity: Activity, chooseAccount: Boolean = false, loginHint: String? = null) {
        mutableUiState.update { it.copy(showAccountSwitcher = false, nativeAuth = NativeAuthDialogState()) }
        controller.signInWithBrowser(activity, chooseAccount, loginHint)
    }

    fun openWebSection(activity: Activity, section: String? = null) {
        controller.manageOnWeb(activity, section)
    }

    fun openWebPath(activity: Activity, path: String) {
        controller.openWebPage(activity, path)
    }

    fun refreshProfile() {
        mutableUiState.value.activeAccount?.userId?.let { nativeScopeDeniedUserIds.remove(it) }
        controller.refreshProfile()
        if (controller.canManageAccountNatively()) {
            refreshNativeManagementData()
        }
    }

    private fun isInsufficientScopeError(err: Throwable?): Boolean =
        err is MobileApiException && (err.code == 403 || err.errorCode == "insufficient_scope")

    private fun downgradeNativeManagementForUser(targetUserId: String?, message: String? = null) {
        if (targetUserId != null) {
            nativeScopeDeniedUserIds.add(targetUserId)
        }
        val stillOwnsActive = targetUserId == null || mutableUiState.value.activeAccount?.userId == targetUserId
        if (!stillOwnsActive) return
        nativeRefreshGeneration++
        mutableUiState.update {
            it.copy(
                canManageNatively = false,
                canWriteProfileNatively = false,
                isManagementBusy = false,
                activeManagementDialog = null,
                managementError = null,
                snackbarMessage = message ?: it.snackbarMessage,
            )
        }
    }

    private fun handleSimpleManagementFailure(
        err: Throwable,
        fallback: String,
        initiatingUserId: String?,
        opId: Long = activeManagementOpId,
    ) {
        if (isInsufficientScopeError(err)) {
            downgradeNativeManagementForUser(
                initiatingUserId,
                "Native management requires account scopes; use Manage on web.",
            )
            return
        }
        if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) return
        mutableUiState.update {
            it.copy(isManagementBusy = false, managementError = err.message ?: fallback)
        }
    }

    private fun handleManagementFailure(
        err: Throwable,
        fallback: String,
        initiatingUserId: String?,
        opId: Long = activeManagementOpId,
    ) {
        if (isInsufficientScopeError(err)) {
            downgradeNativeManagementForUser(
                initiatingUserId,
                "Native management requires account scopes; use Manage on web.",
            )
            return
        }
        if (opId != activeManagementOpId || initiatingUserId != mutableUiState.value.activeAccount?.userId) return
        val needsMfa = (err as? MobileApiException)?.errorCode == "mfa_required" ||
            err.message?.contains("authenticator", ignoreCase = true) == true
        mutableUiState.update {
            it.copy(
                isManagementBusy = false,
                managementRequiresMfaPrompt = it.managementRequiresMfaPrompt || needsMfa,
                managementError = err.message ?: fallback,
            )
        }
    }

    private fun refreshNativeManagementData() {
        val activeId = mutableUiState.value.activeAccount?.userId
        val requestGen = ++nativeRefreshGeneration
        viewModelScope.launch {
            val sessionsDeferred = async { controller.listWebSessions() }
            val activityDeferred = async { controller.listActivity() }
            val authsDeferred = async { controller.listAuthorizations() }
            val sessionsRes = sessionsDeferred.await()
            val activityRes = activityDeferred.await()
            val authsRes = authsDeferred.await()
            if (requestGen != nativeRefreshGeneration || activeId != mutableUiState.value.activeAccount?.userId) {
                return@launch
            }
            val scopeRejected = listOf(sessionsRes, activityRes, authsRes)
                .mapNotNull { it.exceptionOrNull() }
                .any { isInsufficientScopeError(it) }
            if (scopeRejected) {
                downgradeNativeManagementForUser(activeId)
                return@launch
            }
            val sessions = sessionsRes.getOrNull()
            val activity = activityRes.getOrNull()
            val auths = authsRes.getOrNull()
            val denied = activeId != null && activeId in nativeScopeDeniedUserIds
            val canManage = !denied && runCatching { controller.canManageAccountNatively() }.getOrDefault(false)
            val canWrite = !denied && runCatching { controller.canWriteProfileNatively() }.getOrDefault(false)
            mutableUiState.update { current ->
                current.copy(
                    canManageNatively = canManage,
                    canWriteProfileNatively = canWrite,
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
        SearchableSetting("email", "Account email address", "Personal info · Fixed identifier", "bansify email username mailbox address", AccountTab.PERSONAL_INFO),
        SearchableSetting("visibility", "Profile visibility", "Personal info · Sharing information", "public private privacy sharing visibility", AccountTab.PERSONAL_INFO),
        SearchableSetting("password", "Add or change password", "Security · Sign-in methods", "password reset update login", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("mfa", "2-Step Verification", "Security · Authenticator", "2fa mfa two factor two step authenticator otp totp verification", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("passkeys", "Passkeys", "Security · Sign-in methods", "fingerprint face biometric security key passwordless", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("devices", "Your devices", "Security · Active sessions", "sessions browsers computers phones device sign out", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("sec_activity", "Recent security activity", "Security · Sign-in history", "login history events activity", AccountTab.SECURITY, webSection = "security"),
        SearchableSetting("storage", "Storage", "Data & privacy · Shared quota", "space quota drive mail usage", AccountTab.DATA_PRIVACY, webSection = "data"),
        SearchableSetting("export", "Export your data", "Data & privacy · Account data", "download export backup account data json", AccountTab.DATA_PRIVACY, webSection = "data"),
        SearchableSetting("delete", "Delete your account", "Data & privacy · Account data", "remove close erase delete account", AccountTab.DATA_PRIVACY, webSection = "data"),
        SearchableSetting("contacts", "Bansify Contacts", "People & sharing", "people contacts address book", AccountTab.PEOPLE_SHARING),
        SearchableSetting("services", "Bansify Services", "Connected apps", "services products applications mail drive store", AccountTab.CONNECTED_APPS),
        SearchableSetting("disconnect", "Disconnect an app", "Connected apps · Permissions", "revoke permission disconnect third party app oauth", AccountTab.CONNECTED_APPS, webSection = "apps"),
    )
}
