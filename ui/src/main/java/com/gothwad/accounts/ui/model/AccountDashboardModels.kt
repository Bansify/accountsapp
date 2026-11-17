package com.gothwad.accounts.ui.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.ConnectedAppGrant
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.core.MfaEnrollmentSetup
import com.gothwad.accounts.core.MfaStatusInfo
import com.gothwad.accounts.core.WebSessionInfo
import com.gothwad.accounts.ui.R

/** Matches exact 6 sections on accounts.gothwadtech.com/me. */
enum class AccountTab(
    @StringRes val titleRes: Int,
    val webSection: String,
) {
    HOME(R.string.gothwad_tab_home, "home"),
    PERSONAL_INFO(R.string.gothwad_tab_personal, "personal"),
    SECURITY(R.string.gothwad_tab_security, "security"),
    DATA_PRIVACY(R.string.gothwad_tab_data, "data"),
    PEOPLE_SHARING(R.string.gothwad_tab_people, "people"),
    CONNECTED_APPS(R.string.gothwad_tab_apps, "apps"),
}

sealed interface AccountSubScreen {
    data object SearchAccount : AccountSubScreen
    data object HelpLegal : AccountSubScreen
}

enum class NativeAuthStep {
    IDENTIFIER,
    PASSWORD,
    MFA,
    SIGN_UP,
    FORGOT_PASSWORD,
    RESET_PASSWORD,
}

@Immutable
data class NativeAuthDialogState(
    val visible: Boolean = false,
    val step: NativeAuthStep = NativeAuthStep.IDENTIFIER,
    val identifier: String = "",
    val mfaChallenge: String = "",
    val mfaPrompt: String = "",
    val usernameAvailable: Boolean? = null,
    val usernameStatusMessage: String? = null,
    val isBusy: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
)

sealed interface ActiveManagementDialog {
    data object EditBasicProfile : ActiveManagementDialog
    data object Avatar : ActiveManagementDialog
    data object Phone : ActiveManagementDialog
    data object RecoveryEmail : ActiveManagementDialog
    data object Password : ActiveManagementDialog
    data object TwoFactor : ActiveManagementDialog
    data object DeleteAccount : ActiveManagementDialog
    data class ExportData(val jsonContent: String) : ActiveManagementDialog
}

@Immutable
data class GothwadCatalogEntry(
    val id: String,
    val name: String,
    val description: String,
    val url: String,
    val isCoreService: Boolean,
    val inDevelopment: Boolean = false,
)

@Immutable
data class SearchableSetting(
    val id: String,
    val title: String,
    val subtitle: String,
    val keywords: String,
    val category: AccountTab,
    val webSection: String? = null,
    val targetSubScreen: AccountSubScreen? = null,
)

@Immutable
data class SignOutDialogTarget(
    val userId: String,
    val displayName: String,
    val email: String,
)

@Immutable
data class AccountDashboardUiState(
    val accountState: AccountState = AccountState.Initializing,
    val discoveredAccounts: List<DiscoveredAccount> = emptyList(),
    val isRefreshing: Boolean = false,
    val isPeerSilentSigningIn: Boolean = false,
    val authError: AuthFailure? = null,
    val installedVerifiedPeers: List<String> = emptyList(),
    val selectedTab: AccountTab = AccountTab.HOME,
    val subScreenStack: List<AccountSubScreen> = emptyList(),
    val searchQuery: String = "",
    val showAccountSwitcher: Boolean = false,
    val signOutTarget: SignOutDialogTarget? = null,
    val snackbarMessage: String? = null,
    val canManageNatively: Boolean = false,
    val canWriteProfileNatively: Boolean = false,
    val webSessions: List<WebSessionInfo> = emptyList(),
    val activityFeed: List<AccountActivityEntry> = emptyList(),
    val authorizations: List<ConnectedAppGrant> = emptyList(),
    val isSavingProfile: Boolean = false,
    val nativeAuth: NativeAuthDialogState = NativeAuthDialogState(),
    val activeManagementDialog: ActiveManagementDialog? = null,
    val isManagementBusy: Boolean = false,
    val managementError: String? = null,
    val managementRequiresMfaPrompt: Boolean = false,
    val recoveryEmailPendingVerification: Boolean = false,
    val mfaStatus: MfaStatusInfo? = null,
    val mfaSetup: MfaEnrollmentSetup? = null,
    val mfaRecoveryCodes: List<String> = emptyList(),
    val isExportingData: Boolean = false,
) {
    val showEditProfileDialog: Boolean
        get() = activeManagementDialog is ActiveManagementDialog.EditBasicProfile

    val activeAccount: GothwadAccount?
        get() = (accountState as? AccountState.SignedIn)?.account

    val localAccounts: List<GothwadAccount>
        get() = (accountState as? AccountState.SignedIn)?.all.orEmpty()

    val otherLocalAccounts: List<GothwadAccount>
        get() = activeAccount?.let { active -> localAccounts.filter { it.userId != active.userId } }.orEmpty()

    val unauthorizedPeerAccounts: List<DiscoveredAccount>
        get() = discoveredAccounts.filter { !it.isAuthorizedLocally }

    val currentSubScreen: AccountSubScreen?
        get() = subScreenStack.lastOrNull()
}

/** Exact ecosystem service & product catalog matching accounts/web/js/gothwad-apps.js. */
object GothwadEcosystemCatalog {
    val coreServices: List<GothwadCatalogEntry> = listOf(
        GothwadCatalogEntry("gothwad-meet", "Gothwad Meet", "Video meetings for your Gothwad Account", "https://meet.gothwadtech.com", isCoreService = true),
        GothwadCatalogEntry("gothwad-mail", "Gothwad Mail", "Account address at gothwadtech.com; mailbox availability is managed by Mail", "https://mail.gothwadtech.com", isCoreService = true),
        GothwadCatalogEntry("kiwi-store", "Kiwi Store", "Apps and add-ons for Kiwi Store", "https://kiwistore.gothwadtech.com", isCoreService = true),
        GothwadCatalogEntry("gothwad-drive", "Gothwad Drive", "Files and folders on your account", "https://drive.gothwadtech.com", isCoreService = true),
        GothwadCatalogEntry("gothwad-notes", "Gothwad Notes", "Notes and lists, synced", "https://notes.gothwadtech.com", isCoreService = true),
        GothwadCatalogEntry("gothwad-calendar", "Gothwad Calendar", "Events and reminders", "https://calender.gothwadtech.com", isCoreService = true),
        GothwadCatalogEntry("gothwad-contacts", "Gothwad Contacts", "People and addresses", "https://contacts.gothwadtech.com", isCoreService = true, inDevelopment = true),
    )

    val optionalProducts: List<GothwadCatalogEntry> = listOf(
        GothwadCatalogEntry("gothwad-tube", "Gothwad Tube (PlusTube)", "Video — PlusTube", "https://tube.gothwadtech.com", isCoreService = false),
        GothwadCatalogEntry("gothwad-chat", "GrixChat (Gothwad Chat)", "Messaging", "https://grixchat.gothwadtech.com", isCoreService = false),
        GothwadCatalogEntry("gothwad-indogram", "Indogram", "Messaging & communities", "https://indogram.gothwadtech.com", isCoreService = false),
    )
}
