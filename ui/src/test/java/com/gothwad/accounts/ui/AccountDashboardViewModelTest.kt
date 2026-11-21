package com.gothwad.accounts.ui

import android.app.Activity
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.BrowserAuthCompletion
import com.gothwad.accounts.core.ConnectedAppGrant
import com.gothwad.accounts.core.DisconnectOutcome
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.IdentifierCheckResult
import com.gothwad.accounts.core.MfaEnrollmentSetup
import com.gothwad.accounts.core.MfaStatusInfo
import com.gothwad.accounts.core.MobileApiException
import com.gothwad.accounts.core.MobileAuthSignInOutcome
import com.gothwad.accounts.core.MobileAuthUser
import com.gothwad.accounts.core.NativeSignUpInput
import com.gothwad.accounts.core.UsernameCheckResult
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.ActiveManagementDialog
import com.gothwad.accounts.ui.model.NativeAuthStep
import com.gothwad.accounts.ui.model.NavigationLayoutMode
import com.gothwad.accounts.ui.model.ThemePreference
import com.gothwad.accounts.ui.state.AccountDashboardViewModel
import com.gothwad.accounts.ui.state.AccountSessionController
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountDashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private class FakeController(
        private val nativeManage: Boolean = false,
        private val nativeWrite: Boolean = false,
    ) : AccountSessionController {
        override val state = MutableStateFlow<AccountState>(AccountState.Initializing)
        override val discoveredAccounts = MutableStateFlow<List<DiscoveredAccount>>(emptyList())
        override val isRefreshing = MutableStateFlow(false)
        override val error = MutableStateFlow<AuthFailure?>(null)
        override val lastDisconnectOutcome = MutableStateFlow<DisconnectOutcome?>(null)
        override val browserAuthCompletion = MutableStateFlow<BrowserAuthCompletion?>(null)

        var lastSwitchedUserId: String? = null
        var lastSignedOutUserId: String? = null
        var lastDisconnectedUserId: String? = null
        var lastRemovedDeviceUserId: String? = null
        var refreshCalls = 0
        var syncCalls = 0
        var recoverCalls = 0
        var clearErrorCalls = 0
        var peers = listOf("com.gothwad.mail", "com.gothwad.drive")

        var failPeerSilentSso = false
        var requireMfaOnSignIn = false
        var requireMfaOnPhoneUpdate = false
        var rejectMobileScopesWith403 = false
        var failProfileSaveValidation = false
        var pendingAvatarGate: CompletableDeferred<Unit>? = null
        var pendingRefreshGate: CompletableDeferred<Boolean>? = null
        var lastRevokedSessionId: String? = null

        override fun signIn(activity: Activity, chooseAccount: Boolean, loginHint: String?) = Unit
        override fun switchTo(userId: String) { lastSwitchedUserId = userId }
        override fun signOut(userId: String) { lastSignedOutUserId = userId }
        override fun disconnectFromServer(userId: String) {
            lastDisconnectedUserId = userId
            lastDisconnectOutcome.value = DisconnectOutcome.OfflineRemovedLocally
        }

        override fun removeFromDevice(userId: String) { lastRemovedDeviceUserId = userId }
        override fun refreshProfile() { refreshCalls++ }
        override fun syncDiscoveredAccounts() { syncCalls++ }
        override fun manageOnWeb(activity: Activity, section: String?) = Unit
        override fun openWebPage(activity: Activity, path: String) = Unit
        override fun recoverCorruptedStorage() { recoverCalls++ }
        override fun clearError() {
            clearErrorCalls++
            error.value = null
        }

        override fun clearDisconnectOutcome() {
            lastDisconnectOutcome.value = null
        }

        override fun installedVerifiedPeers(): List<String> = peers
        override fun canManageAccountNatively(): Boolean = nativeManage
        override fun canWriteProfileNatively(): Boolean = nativeWrite

        override suspend fun checkIdentifier(identifier: String) =
            Result.success(IdentifierCheckResult(exists = identifier == "arjun", kind = "username"))

        override suspend fun checkUsername(username: String) =
            Result.success(UsernameCheckResult(available = true, valid = true, username = username, email = "$username@bansify.com"))

        override suspend fun signInNative(identifier: String, password: String): Result<MobileAuthSignInOutcome> {
            return if (requireMfaOnSignIn) {
                Result.success(MobileAuthSignInOutcome.MfaRequired("chal-1", 300L, "Enter 6-digit code"))
            } else {
                val acc = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com")
                state.value = AccountState.SignedIn(acc, listOf(acc))
                Result.success(
                    MobileAuthSignInOutcome.Authenticated(
                        accessToken = "at-1",
                        refreshToken = "rt-1",
                        expiresIn = 3600L,
                        scope = "profile",
                        clientId = "gothwad-accounts-android",
                        sessionId = "s-1",
                        user = MobileAuthUser("u-1", "arjun", "arjun@bansify.com", "Arjun"),
                    ),
                )
            }
        }

        override suspend fun verifyNativeMfa(challenge: String, code: String): Result<GothwadAccount> {
            val acc = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com", mfaEnabled = true)
            state.value = AccountState.SignedIn(acc, listOf(acc))
            return Result.success(acc)
        }

        override suspend fun signUpNative(input: NativeSignUpInput): Result<GothwadAccount> {
            val acc = GothwadAccount("u-2", input.username, "${input.firstName} ${input.lastName}", input.firstName, input.lastName, "${input.username}@bansify.com")
            state.value = AccountState.SignedIn(acc, listOf(acc))
            return Result.success(acc)
        }

        override suspend fun forgotPasswordNative(username: String) =
            Result.success("Reset token sent.")

        override suspend fun resetPasswordNative(token: String, newPassword: String) =
            Result.success("Password updated.")

        override suspend fun signInSilentlyWithPeer(peer: DiscoveredAccount): Result<GothwadAccount> {
            if (failPeerSilentSso) return Result.failure(IllegalStateException("peer_code_failed"))
            val acc = GothwadAccount(peer.userId, peer.username, peer.name, email = peer.email)
            state.value = AccountState.SignedIn(acc, listOf(acc))
            return Result.success(acc)
        }

        override suspend fun listWebSessions(): Result<List<WebSessionInfo>> {
            val gate = pendingRefreshGate
            val gateRejects = gate?.await() ?: false
            return if (rejectMobileScopesWith403 || gateRejects) {
                Result.failure(MobileApiException(403, "insufficient_scope", "insufficient_scope"))
            } else {
                Result.success(
                    listOf(
                        WebSessionInfo(id = "ws-1", browser = "Chrome", os = "Android", current = true),
                        WebSessionInfo(id = "ws-2", browser = "Firefox", os = "Linux", current = false),
                    ),
                )
            }
        }

        override suspend fun revokeWebSession(sessionId: String): Result<Int> {
            lastRevokedSessionId = sessionId
            return Result.success(1)
        }

        override suspend fun listActivity(): Result<List<AccountActivityEntry>> =
            Result.success(listOf(AccountActivityEntry(id = "act-1", type = "signin", title = "Signed in")))

        override suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> =
            Result.success(listOf(ConnectedAppGrant(appId = "gothwad-mail", name = "Bansify Mail", scopes = listOf("profile"))))

        override suspend fun updateBasicProfile(
            firstName: String,
            lastName: String,
            dateOfBirth: String,
            gender: String,
        ): Result<GothwadAccount?> {
            if (rejectMobileScopesWith403) {
                return Result.failure(MobileApiException(403, "insufficient_scope", "insufficient_scope"))
            }
            if (failProfileSaveValidation) {
                return Result.failure(MobileApiException(400, "invalid_request", "invalid_request"))
            }
            val updated = GothwadAccount("u-1", "arjun", "$firstName $lastName".trim(), firstName, lastName, "arjun@bansify.com", dateOfBirth = dateOfBirth, gender = gender)
            state.value = AccountState.SignedIn(updated, listOf(updated))
            return Result.success(updated)
        }

        override suspend fun uploadAvatar(imageDataOrBase64: String, mime: String): Result<GothwadAccount?> {
            pendingAvatarGate?.await()
            val cur = (state.value as AccountState.SignedIn).account.copy(avatarUrl = "https://accounts.bansify.com/api/avatars/u-1?v=new")
            state.value = AccountState.SignedIn(cur, listOf(cur))
            return Result.success(cur)
        }

        override suspend fun removeAvatar(): Result<GothwadAccount?> {
            val cur = (state.value as AccountState.SignedIn).account.copy(avatarUrl = null)
            state.value = AccountState.SignedIn(cur, listOf(cur))
            return Result.success(cur)
        }

        override suspend fun updatePhoneNumber(phoneNumber: String, currentPassword: String, mfaCode: String?): Result<GothwadAccount?> {
            if (requireMfaOnPhoneUpdate && mfaCode.isNullOrBlank()) {
                return Result.failure(MobileApiException(400, "Enter a fresh authenticator code.", "mfa_required"))
            }
            val cur = (state.value as AccountState.SignedIn).account.copy(phoneNumber = phoneNumber)
            state.value = AccountState.SignedIn(cur, listOf(cur))
            return Result.success(cur)
        }

        override suspend fun requestRecoveryEmailChange(recoveryEmail: String, currentPassword: String, mfaCode: String?) =
            Result.success("Verification token sent.")

        override suspend fun verifyRecoveryEmailChange(token: String): Result<GothwadAccount?> {
            val cur = (state.value as AccountState.SignedIn).account.copy(recoveryEmail = "verified@example.com", recoveryEmailVerified = true)
            state.value = AccountState.SignedIn(cur, listOf(cur))
            return Result.success(cur)
        }

        override suspend fun changePassword(currentPassword: String, newPassword: String, mfaCode: String?) =
            Result.success("Password saved.")

        override suspend fun getMfaStatus() =
            Result.success(MfaStatusInfo(enabled = false, enrollmentAvailable = true, recoveryCodesRemaining = 0))

        override suspend fun beginMfaEnrollment(currentPassword: String) =
            Result.success(MfaEnrollmentSetup("enr-1", "SECRET123", "otpauth://totp/Gothwad:arjun?secret=SECRET123"))

        override suspend fun confirmMfaEnrollment(currentPassword: String, enrollmentId: String, code: String) =
            Result.success(listOf("REC-1111", "REC-2222"))

        override suspend fun disableMfa(currentPassword: String, code: String) =
            Result.success(Unit)

        override suspend fun exportAccountDataJson() =
            Result.success("""{"ok":true,"export":"gothwad-accounts"}""")

        override suspend fun deleteAccount(password: String, mfaCode: String?): Result<Unit> {
            state.value = AccountState.SignedOut
            return Result.success(Unit)
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun reflectsRealCoreStatesAndNeverExposesFakeAccountsWhenSignedOut() = runTest {
        val controller = FakeController()
        val vm = AccountDashboardViewModel(controller)

        assertEquals(AccountState.Initializing, vm.uiState.value.accountState)
        assertNull(vm.uiState.value.activeAccount)

        controller.state.value = AccountState.SignedOut
        controller.discoveredAccounts.value = listOf(
            DiscoveredAccount(
                userId = "u-1",
                username = "arjun",
                name = "Arjun",
                email = "arjun@bansify.com",
                sourcePackage = "com.gothwad.mail",
                isAuthorizedLocally = false,
            ),
        )
        advanceUntilIdle()

        assertEquals(AccountState.SignedOut, vm.uiState.value.accountState)
        assertNull(vm.uiState.value.activeAccount)
        assertEquals(1, vm.uiState.value.unauthorizedPeerAccounts.size)

        val account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com")
        controller.state.value = AccountState.SignedIn(account, listOf(account))
        advanceUntilIdle()
        assertEquals("u-1", vm.uiState.value.activeAccount?.userId)

        controller.state.value = AccountState.StorageCorrupted("Keystore invalidated")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.accountState is AccountState.StorageCorrupted)
        vm.recoverCorruptedStorage()
        assertEquals(1, controller.recoverCalls)
    }

    @Test
    fun tabsAndSearchIndexMatchExactSixWebSectionsWithoutDemoCrud() = runTest {
        val controller = FakeController()
        val vm = AccountDashboardViewModel(controller)

        assertEquals(
            listOf("home", "personal", "security", "data", "people", "apps"),
            AccountTab.entries.map { it.webSection },
        )

        val searchTitles = vm.getSearchIndex().map { it.title.lowercase() }
        assertFalse(searchTitles.any { it.contains("payment") || it.contains("subscription") })
        assertFalse(searchTitles.any { it.contains("password manager") })
        assertTrue(searchTitles.any { it.contains("2-step verification") })
        assertTrue(searchTitles.any { it.contains("export") })
    }

    @Test
    fun localSignOutVsDeviceRemovalAndServerDisconnectSurfaceTruthfully() = runTest {
        val controller = FakeController(nativeManage = true, nativeWrite = true)
        val vm = AccountDashboardViewModel(controller)

        val account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com")
        controller.state.value = AccountState.SignedIn(account, listOf(account))
        advanceUntilIdle()

        assertEquals(2, vm.uiState.value.webSessions.size)
        assertEquals(1, vm.uiState.value.activityFeed.size)
        assertEquals(1, vm.uiState.value.authorizations.size)

        vm.saveBasicProfile("Arjun", "Verma", "1999-01-01", "Male")
        advanceUntilIdle()
        assertEquals("Arjun Verma", vm.uiState.value.activeAccount?.name)
        assertEquals("Personal info updated", vm.uiState.value.snackbarMessage)

        vm.openSignOutDialog(account)
        assertEquals("u-1", vm.uiState.value.signOutTarget?.userId)

        vm.signOutThisApp("u-1")
        assertEquals("u-1", controller.lastSignedOutUserId)
        assertNull(vm.uiState.value.signOutTarget)

        vm.openSignOutDialog(account)
        vm.removeFromDevice("u-1")
        assertEquals("u-1", controller.lastRemovedDeviceUserId)

        vm.openSignOutDialog(account)
        vm.disconnectGrantOnServer("u-1")
        advanceUntilIdle()
        assertEquals("u-1", controller.lastDisconnectedUserId)
        assertTrue(vm.uiState.value.snackbarMessage?.contains("Offline") == true)

        vm.openSubScreen(AccountSubScreen.SearchAccount)
        assertEquals(AccountSubScreen.SearchAccount, vm.uiState.value.currentSubScreen)
        assertTrue(vm.navigateBack())
        assertNull(vm.uiState.value.currentSubScreen)
        assertFalse(vm.navigateBack())
    }

    @Test
    fun nativeSignInMfaAndSilentPeerSsoWithFallbackWorkEndToEnd() = runTest {
        val controller = FakeController(nativeManage = true, nativeWrite = true).apply {
            requireMfaOnSignIn = true
        }
        val vm = AccountDashboardViewModel(controller)
        controller.state.value = AccountState.SignedOut
        advanceUntilIdle()

        // 1. Native sign-in flow with MFA step
        vm.openNativeAuthDialog()
        assertTrue(vm.uiState.value.nativeAuth.visible)
        assertEquals(NativeAuthStep.IDENTIFIER, vm.uiState.value.nativeAuth.step)

        vm.submitNativeIdentifier("arjun")
        advanceUntilIdle()
        assertEquals(NativeAuthStep.PASSWORD, vm.uiState.value.nativeAuth.step)

        vm.submitNativePassword("arjun", "Pass123!")
        advanceUntilIdle()
        assertEquals(NativeAuthStep.MFA, vm.uiState.value.nativeAuth.step)

        vm.submitNativeMfa("123456")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.nativeAuth.visible)
        assertEquals("u-1", vm.uiState.value.activeAccount?.userId)

        // 2. Peer silent SSO success & fallback
        controller.state.value = AccountState.SignedOut
        advanceUntilIdle()
        val peer = DiscoveredAccount("u-3", "kiran", "Kiran", "kiran@bansify.com", sourcePackage = "com.gothwad.mail")
        vm.continueWithPeerAccount(peer)
        advanceUntilIdle()
        assertEquals("u-3", vm.uiState.value.activeAccount?.userId)
        assertFalse(vm.uiState.value.nativeAuth.visible)

        controller.state.value = AccountState.SignedOut
        controller.failPeerSilentSso = true
        advanceUntilIdle()
        vm.continueWithPeerAccount(peer)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.nativeAuth.visible)
        assertEquals("kiran@bansify.com", vm.uiState.value.nativeAuth.identifier)
        assertEquals(NativeAuthStep.PASSWORD, vm.uiState.value.nativeAuth.step)
    }

    @Test
    fun nativeManagementActionsCoverAvatarPhoneRecoveryPasswordMfaSessionsExportAndDelete() = runTest {
        val controller = FakeController(nativeManage = true, nativeWrite = true).apply {
            requireMfaOnPhoneUpdate = true
        }
        val vm = AccountDashboardViewModel(controller)
        val account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@bansify.com")
        controller.state.value = AccountState.SignedIn(account, listOf(account))
        advanceUntilIdle()

        // Avatar upload & remove
        vm.uploadAvatar("data:image/jpeg;base64,/9j/4AAQ")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.activeAccount?.avatarUrl?.contains("avatars") == true)
        vm.removeAvatar()
        advanceUntilIdle()
        assertNull(vm.uiState.value.activeAccount?.avatarUrl)

        // Phone update triggers mfa_required prompt then succeeds with MFA code
        vm.openManagementDialog(ActiveManagementDialog.Phone)
        vm.updatePhoneNumber("+919876543210", "Pass123!", null)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.managementRequiresMfaPrompt)
        vm.updatePhoneNumber("+919876543210", "Pass123!", "123456")
        advanceUntilIdle()
        assertEquals("+919876543210", vm.uiState.value.activeAccount?.phoneNumber)
        assertNull(vm.uiState.value.activeManagementDialog)

        // Recovery email 2-step flow
        vm.openManagementDialog(ActiveManagementDialog.RecoveryEmail)
        vm.requestRecoveryEmailChange("verified@example.com", "Pass123!", null)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.recoveryEmailPendingVerification)
        vm.verifyRecoveryEmailChange("a".repeat(64))
        advanceUntilIdle()
        assertEquals("verified@example.com", vm.uiState.value.activeAccount?.recoveryEmail)

        // Password change
        vm.changePassword("Pass123!", "NewPass123!", null)
        advanceUntilIdle()
        assertEquals("Password saved.", vm.uiState.value.snackbarMessage)

        // 2FA setup & confirm & disable
        vm.openManagementDialog(ActiveManagementDialog.TwoFactor)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.mfaStatus?.enabled ?: true)
        vm.beginMfaEnrollment("Pass123!")
        advanceUntilIdle()
        assertEquals("enr-1", vm.uiState.value.mfaSetup?.enrollmentId)
        vm.confirmMfaEnrollment("Pass123!", "enr-1", "123456")
        advanceUntilIdle()
        assertEquals(listOf("REC-1111", "REC-2222"), vm.uiState.value.mfaRecoveryCodes)
        vm.disableMfa("Pass123!", "123456")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.mfaStatus?.enabled ?: true)

        // Single session revoke
        vm.revokeWebSession("ws-2")
        advanceUntilIdle()
        assertEquals("ws-2", controller.lastRevokedSessionId)

        // Data export
        vm.exportAccountData()
        advanceUntilIdle()
        val exportDialog = vm.uiState.value.activeManagementDialog as ActiveManagementDialog.ExportData
        assertTrue(exportDialog.jsonContent.contains("gothwad-accounts"))

        // Permanent account deletion
        vm.deleteAccount("Pass123!", null)
        advanceUntilIdle()
        assertEquals(AccountState.SignedOut, vm.uiState.value.accountState)
    }

    @Test
    fun themePreferencesAndAdaptiveNavigationLayoutUpdateCleanly() = runTest {
        val controller = FakeController()
        val vm = AccountDashboardViewModel(controller)

        vm.setThemePreference(ThemePreference.DARK)
        assertEquals(ThemePreference.DARK, vm.uiState.value.themePreference)

        vm.setNavigationLayout(NavigationLayoutMode.BOTTOM_BAR)
        assertEquals(NavigationLayoutMode.BOTTOM_BAR, vm.uiState.value.navigationLayout)

        vm.openOthersHub()
        assertTrue(vm.uiState.value.showOthersHub)

        vm.selectTab(AccountTab.CONNECTED_APPS)
        assertFalse(vm.uiState.value.showOthersHub)
        assertEquals(AccountTab.CONNECTED_APPS, vm.uiState.value.selectedTab)

        // Back from Others child tab returns to OthersHub in BOTTOM_BAR layout
        assertTrue(vm.navigateBack())
        assertTrue(vm.uiState.value.showOthersHub)

        vm.openSubScreen(AccountSubScreen.AppSettings)
        vm.openSubScreen(AccountSubScreen.AppSettings) // deduplicated
        assertEquals(listOf(AccountSubScreen.AppSettings), vm.uiState.value.subScreenStack)
        assertTrue(vm.navigateBack())
        assertNull(vm.uiState.value.currentSubScreen)
    }

    @Test
    fun authSuccessCountAndInsufficientScopeDegradationBehaveDeterministically() = runTest {
        val controller = FakeController(nativeManage = true, nativeWrite = true).apply {
            rejectMobileScopesWith403 = true
        }
        val vm = AccountDashboardViewModel(controller)

        vm.configureNativeAuthEntry(NativeAuthStep.SIGN_UP, "meera")
        assertFalse(vm.uiState.value.nativeAuth.visible)
        assertEquals(NativeAuthStep.SIGN_UP, vm.uiState.value.nativeAuth.step)
        assertEquals("meera", vm.uiState.value.nativeAuth.identifier)
        assertEquals(0, vm.uiState.value.authSuccessCount)

        // Native sign-up increments authSuccessCount cleanly.
        vm.submitNativeSignUp(
            NativeSignUpInput(
                firstName = "Meera",
                lastName = "Rao",
                username = "meera",
                password = "Password123!",
                recoveryEmail = "meera@example.com",
            ),
        )
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.authSuccessCount)

        // 403 insufficient_scope automatically degrades native management to web /me links without crash.
        assertFalse(vm.uiState.value.canManageNatively)
        assertFalse(vm.uiState.value.canWriteProfileNatively)

        // Once server grants account scopes and user refreshes, native management activates cleanly.
        controller.rejectMobileScopesWith403 = false
        vm.refreshProfile()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canManageNatively)
        assertTrue(vm.uiState.value.canWriteProfileNatively)
        assertTrue(vm.uiState.value.webSessions.isNotEmpty())

        // Validation failure during profile save keeps dialog open without downgrading scopes.
        controller.failProfileSaveValidation = true
        vm.openManagementDialog(ActiveManagementDialog.EditBasicProfile)
        vm.saveBasicProfile("Meera", "Rao", "1995-01-01", "female")
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canManageNatively)
        assertEquals(ActiveManagementDialog.EditBasicProfile, vm.uiState.value.activeManagementDialog)

        // 403 insufficient_scope during profile save downgrades cleanly and closes native dialog.
        controller.failProfileSaveValidation = false
        controller.rejectMobileScopesWith403 = true
        vm.saveBasicProfile("Meera", "Rao", "1995-01-01", "female")
        advanceUntilIdle()
        assertFalse(vm.uiState.value.canManageNatively)
        assertFalse(vm.uiState.value.canWriteProfileNatively)
        assertNull(vm.uiState.value.activeManagementDialog)

        // Browser auth completion advances authSuccessCount and clears denial for the completed userId.
        val countBeforeBrowser = vm.uiState.value.authSuccessCount
        controller.rejectMobileScopesWith403 = false
        val completedUserId = vm.uiState.value.activeAccount!!.userId
        controller.browserAuthCompletion.value = BrowserAuthCompletion(generation = 1, userId = completedUserId)
        advanceUntilIdle()
        assertEquals(countBeforeBrowser + 1, vm.uiState.value.authSuccessCount)
        assertTrue(vm.uiState.value.canManageNatively)

        // Late 403 from an obsolete refresh generation does not poison newer refresh state.
        val firstRefreshGate = CompletableDeferred<Boolean>()
        controller.pendingRefreshGate = firstRefreshGate
        vm.refreshProfile()
        runCurrent()
        controller.pendingRefreshGate = null
        vm.refreshProfile()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canManageNatively)
        firstRefreshGate.complete(true)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.canManageNatively)

        // Switching active user clears busy state and prevents stale upload completions from sticking.
        val uploadGate = CompletableDeferred<Unit>()
        controller.pendingAvatarGate = uploadGate
        vm.uploadAvatar("data:image/jpeg;base64,abc")
        runCurrent()
        assertTrue(vm.uiState.value.isManagementBusy)
        controller.pendingAvatarGate = null
        val userB = GothwadAccount("u-b", "bella", "Bella", email = "bella@bansify.com")
        controller.state.value = AccountState.SignedIn(userB, listOf(userB))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isManagementBusy)
        uploadGate.complete(Unit)
        advanceUntilIdle()
        assertFalse(vm.uiState.value.isManagementBusy)
        assertEquals("u-b", vm.uiState.value.activeAccount?.userId)

        // ViewModel-bound sign-in session tracker preserves rotation state while resetting on process recreation.
        val sessionKey = "sess-1"
        vm.ensureSignInSessionInitialized(sessionKey, NativeAuthStep.SIGN_UP, "meera")
        assertFalse(vm.evaluateSignInCompletion(sessionKey, allowWhenAlreadySignedIn = true, startedBrowserFlow = false))
        vm.setNativeAuthStep(NativeAuthStep.MFA)
        vm.ensureSignInSessionInitialized(sessionKey, NativeAuthStep.SIGN_UP, "meera")
        assertEquals(NativeAuthStep.MFA, vm.uiState.value.nativeAuth.step)

        val recreatedVm = AccountDashboardViewModel(controller)
        recreatedVm.ensureSignInSessionInitialized(sessionKey, NativeAuthStep.SIGN_UP, "meera")
        assertEquals(NativeAuthStep.SIGN_UP, recreatedVm.uiState.value.nativeAuth.step)
        assertEquals("meera", recreatedVm.uiState.value.nativeAuth.identifier)
    }
}
