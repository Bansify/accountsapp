package com.gothwad.accounts.ui

import android.app.Activity
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.ConnectedAppGrant
import com.gothwad.accounts.core.DisconnectOutcome
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.state.AccountDashboardViewModel
import com.gothwad.accounts.ui.state.AccountSessionController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
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

        var lastSwitchedUserId: String? = null
        var lastSignedOutUserId: String? = null
        var lastDisconnectedUserId: String? = null
        var lastRemovedDeviceUserId: String? = null
        var refreshCalls = 0
        var syncCalls = 0
        var recoverCalls = 0
        var clearErrorCalls = 0
        var peers = listOf("com.gothwad.mail", "com.gothwad.drive")

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

        override suspend fun listWebSessions(): Result<List<WebSessionInfo>> =
            Result.success(listOf(WebSessionInfo(id = "ws-1", browser = "Chrome", os = "Android", current = true)))

        override suspend fun listActivity(): Result<List<AccountActivityEntry>> =
            Result.success(listOf(AccountActivityEntry(id = "act-1", type = "signin", title = "Signed in")))

        override suspend fun listAuthorizations(): Result<List<ConnectedAppGrant>> =
            Result.success(listOf(ConnectedAppGrant(appId = "gothwad-mail", name = "Gothwad Mail", scopes = listOf("profile"))))

        override suspend fun updateBasicProfile(
            firstName: String,
            lastName: String,
            dateOfBirth: String,
            gender: String,
        ): Result<GothwadAccount?> {
            val updated = GothwadAccount("u-1", "arjun", "$firstName $lastName".trim(), firstName, lastName, "arjun@gothwadtech.com", dateOfBirth = dateOfBirth, gender = gender)
            state.value = AccountState.SignedIn(updated, listOf(updated))
            return Result.success(updated)
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
                email = "arjun@gothwadtech.com",
                sourcePackage = "com.gothwad.mail",
                isAuthorizedLocally = false,
            ),
        )
        advanceUntilIdle()

        assertEquals(AccountState.SignedOut, vm.uiState.value.accountState)
        assertNull(vm.uiState.value.activeAccount)
        assertEquals(1, vm.uiState.value.unauthorizedPeerAccounts.size)

        val account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@gothwadtech.com")
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

        val account = GothwadAccount("u-1", "arjun", "Arjun", email = "arjun@gothwadtech.com")
        controller.state.value = AccountState.SignedIn(account, listOf(account))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.webSessions.size)
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
}
