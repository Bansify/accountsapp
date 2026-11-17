package com.gothwad.accounts.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.ui.components.AccountSwitcherDialog
import com.gothwad.accounts.ui.components.AccountsTabBar
import com.gothwad.accounts.ui.components.AccountsTopAppBar
import com.gothwad.accounts.ui.components.AvatarManagementDialog
import com.gothwad.accounts.ui.components.DeleteAccountDialog
import com.gothwad.accounts.ui.components.EditBasicProfileDialog
import com.gothwad.accounts.ui.components.ExportDataDialog
import com.gothwad.accounts.ui.components.GothwadAuthStatusBanner
import com.gothwad.accounts.ui.components.NativeAuthDialog
import com.gothwad.accounts.ui.components.PasswordManagementDialog
import com.gothwad.accounts.ui.components.PhoneManagementDialog
import com.gothwad.accounts.ui.components.RecoveryEmailDialog
import com.gothwad.accounts.ui.components.SignOutOrRemoveDialog
import com.gothwad.accounts.ui.components.TwoFactorManagementDialog
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.ActiveManagementDialog
import com.gothwad.accounts.ui.screens.ConnectedAppsTabScreen
import com.gothwad.accounts.ui.screens.DataPrivacyTabScreen
import com.gothwad.accounts.ui.screens.HelpLegalScreen
import com.gothwad.accounts.ui.screens.HomeTabScreen
import com.gothwad.accounts.ui.screens.InitializingAccountScreen
import com.gothwad.accounts.ui.screens.PeopleSharingTabScreen
import com.gothwad.accounts.ui.screens.PersonalInfoTabScreen
import com.gothwad.accounts.ui.screens.SearchAccountScreen
import com.gothwad.accounts.ui.screens.SecurityTabScreen
import com.gothwad.accounts.ui.screens.SignedOutAccountScreen
import com.gothwad.accounts.ui.screens.StorageCorruptedRecoveryScreen
import com.gothwad.accounts.ui.state.AccountDashboardViewModel

/**
 * Full-screen Material 3 Gothwad Account management surface driven 100% by real `:core` state
 * (`GothwadAccounts.state`, `GothwadAccounts.discoveredAccounts`, `GothwadAccounts.error`).
 */
@Composable
fun GothwadAccountsScreen(
    onClose: () -> Unit = {},
    accountViewModel: AccountDashboardViewModel = viewModel(),
) {
    val uiState by accountViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    val snackbarHostState = remember { SnackbarHostState() }
    val emailCopiedMsg = stringResource(R.string.gothwad_email_copied)

    BackHandler(enabled = uiState.currentSubScreen != null) {
        accountViewModel.navigateBack()
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        accountViewModel.clearSnackbar()
    }

    GothwadAccountsTheme {
        val semantic = LocalAccountColors.current
        val currentSubScreen = uiState.currentSubScreen
        val activeAccount = uiState.activeAccount

        val subScreenTitle = when (currentSubScreen) {
            AccountSubScreen.SearchAccount -> stringResource(R.string.gothwad_search_title)
            AccountSubScreen.HelpLegal -> stringResource(R.string.gothwad_help_title)
            null -> null
        }

        Scaffold(
            containerColor = semantic.canvasBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                Column {
                    AccountsTopAppBar(
                        activeAccount = activeAccount,
                        canNavigateBack = currentSubScreen != null,
                        subScreenTitle = subScreenTitle,
                        isRefreshing = uiState.isRefreshing,
                        onBackClick = {
                            if (!accountViewModel.navigateBack()) onClose()
                        },
                        onSearchClick = { accountViewModel.openSubScreen(AccountSubScreen.SearchAccount) },
                        onRefreshClick = { accountViewModel.refreshProfile() },
                        onHelpClick = { accountViewModel.openSubScreen(AccountSubScreen.HelpLegal) },
                        onAvatarClick = { accountViewModel.toggleAccountSwitcher(true) },
                    )
                    if (activeAccount != null && currentSubScreen == null) {
                        AccountsTabBar(
                            selectedTab = uiState.selectedTab,
                            onSelectTab = accountViewModel::selectTab,
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                uiState.authError?.let { failure ->
                    GothwadAuthStatusBanner(
                        failure = failure,
                        onRetry = {
                            accountViewModel.clearAuthError()
                            accountViewModel.refreshProfile()
                        },
                        onSignInAgain = {
                            accountViewModel.clearAuthError()
                            accountViewModel.openNativeAuthDialog()
                        },
                        onDismiss = accountViewModel::clearAuthError,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                when (currentSubScreen) {
                    AccountSubScreen.SearchAccount -> {
                        SearchAccountScreen(
                            query = uiState.searchQuery,
                            items = accountViewModel.getSearchIndex(),
                            onQueryChange = accountViewModel::updateSearchQuery,
                            onSelectItem = { item ->
                                accountViewModel.selectTab(item.category)
                                val canManage = uiState.canManageNatively
                                val canWrite = uiState.canWriteProfileNatively
                                when {
                                    canWrite && item.id in setOf("name", "dob", "gender") ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.EditBasicProfile)
                                    canWrite && item.id == "photo" ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.Avatar)
                                    canWrite && item.id == "phone" ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.Phone)
                                    canWrite && item.id == "recovery" ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.RecoveryEmail)
                                    canWrite && item.id == "password" ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.Password)
                                    canWrite && item.id == "mfa" ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.TwoFactor)
                                    canManage && item.id == "export" ->
                                        accountViewModel.exportAccountData()
                                    canWrite && item.id == "delete" ->
                                        accountViewModel.openManagementDialog(ActiveManagementDialog.DeleteAccount)
                                    !canManage -> {
                                        item.webSection?.let { section ->
                                            activity?.let { accountViewModel.openWebSection(it, section) }
                                        }
                                    }
                                    item.id == "passkeys" -> {
                                        activity?.let { accountViewModel.openWebSection(it, "security") }
                                    }
                                }
                            },
                        )
                    }

                    AccountSubScreen.HelpLegal -> {
                        HelpLegalScreen(
                            packageName = context.packageName,
                            installedPeersCount = uiState.installedVerifiedPeers.size,
                            onOpenPrivacy = { activity?.let { accountViewModel.openWebPath(it, "/privacy") } },
                            onOpenTerms = { activity?.let { accountViewModel.openWebPath(it, "/terms") } },
                            onOpenAccountsHelpWeb = { activity?.let { accountViewModel.openWebSection(it, null) } },
                        )
                    }

                    null -> {
                        when (val accountState = uiState.accountState) {
                            AccountState.Initializing -> InitializingAccountScreen()

                            is AccountState.StorageCorrupted -> {
                                StorageCorruptedRecoveryScreen(
                                    reason = accountState.reason,
                                    onRecoverClick = accountViewModel::recoverCorruptedStorage,
                                )
                            }

                            AccountState.SignedOut -> {
                                SignedOutAccountScreen(
                                    discoveredAccounts = uiState.discoveredAccounts,
                                    isPeerSilentSigningIn = uiState.isPeerSilentSigningIn,
                                    onSignInClick = { accountViewModel.openNativeAuthDialog() },
                                    onBrowserSignInClick = {
                                        activity?.let { accountViewModel.startSignIn(it, chooseAccount = true) }
                                    },
                                    onContinueAsPeer = accountViewModel::continueWithPeerAccount,
                                    onRefreshPeers = accountViewModel::syncPeers,
                                    onOpenPrivacy = { activity?.let { accountViewModel.openWebPath(it, "/privacy") } },
                                    onOpenTerms = { activity?.let { accountViewModel.openWebPath(it, "/terms") } },
                                )
                            }

                            is AccountState.SignedIn -> {
                                val current = accountState.account
                                Crossfade(
                                    targetState = uiState.selectedTab,
                                    label = "account_tab_transition",
                                ) { tab ->
                                    when (tab) {
                                        AccountTab.HOME -> HomeTabScreen(
                                            account = current,
                                            localAccountCount = accountState.all.size,
                                            onOpenAvatarMenu = { accountViewModel.toggleAccountSwitcher(true) },
                                            onCopyEmail = { email ->
                                                copyToClipboard(context, "Account email", email)
                                                accountViewModel.showMessage(emailCopiedMsg)
                                            },
                                            onOpenSearch = { accountViewModel.openSubScreen(AccountSubScreen.SearchAccount) },
                                            onNavigateTab = accountViewModel::selectTab,
                                            onManageOnWeb = { section ->
                                                activity?.let { accountViewModel.openWebSection(it, section) }
                                            },
                                        )

                                        AccountTab.PERSONAL_INFO -> PersonalInfoTabScreen(
                                            account = current,
                                            canEditBasicProfileNatively = uiState.canWriteProfileNatively,
                                            onEditAvatarClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.Avatar)
                                            },
                                            onEditBasicProfileClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.EditBasicProfile)
                                            },
                                            onEditPhoneClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.Phone)
                                            },
                                            onEditRecoveryEmailClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.RecoveryEmail)
                                            },
                                            onManagePersonalOnWeb = {
                                                activity?.let { accountViewModel.openWebSection(it, "personal") }
                                            },
                                        )

                                        AccountTab.SECURITY -> SecurityTabScreen(
                                            account = current,
                                            localAccounts = accountState.all,
                                            installedVerifiedPeers = uiState.installedVerifiedPeers,
                                            canManageNatively = uiState.canManageNatively,
                                            canWriteNatively = uiState.canWriteProfileNatively,
                                            webSessions = uiState.webSessions,
                                            activityFeed = uiState.activityFeed,
                                            onEditPasswordClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.Password)
                                            },
                                            onManageTwoFactorClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.TwoFactor)
                                            },
                                            onEditRecoveryEmailClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.RecoveryEmail)
                                            },
                                            onRevokeWebSession = accountViewModel::revokeWebSession,
                                            onRevokeOtherWebSessions = accountViewModel::revokeOtherWebSessions,
                                            onManageSecurityOnWeb = {
                                                activity?.let { accountViewModel.openWebSection(it, "security") }
                                            },
                                            onRequestSignOutOrRemove = accountViewModel::openSignOutDialog,
                                            onNavigateTab = accountViewModel::selectTab,
                                        )

                                        AccountTab.DATA_PRIVACY -> DataPrivacyTabScreen(
                                            account = current,
                                            canManageNatively = uiState.canManageNatively,
                                            canWriteNatively = uiState.canWriteProfileNatively,
                                            isExportingData = uiState.isExportingData,
                                            activityFeed = uiState.activityFeed,
                                            onExportDataClick = accountViewModel::exportAccountData,
                                            onDeleteAccountClick = {
                                                accountViewModel.openManagementDialog(ActiveManagementDialog.DeleteAccount)
                                            },
                                            onManageDataOnWeb = {
                                                activity?.let { accountViewModel.openWebSection(it, "data") }
                                            },
                                        )

                                        AccountTab.PEOPLE_SHARING -> PeopleSharingTabScreen(
                                            account = current,
                                            onManagePeopleOnWeb = {
                                                activity?.let { accountViewModel.openWebSection(it, "people") }
                                            },
                                            onNavigateTab = accountViewModel::selectTab,
                                        )

                                        AccountTab.CONNECTED_APPS -> ConnectedAppsTabScreen(
                                            installedVerifiedPeers = uiState.installedVerifiedPeers,
                                            discoveredAccounts = uiState.discoveredAccounts,
                                            canManageNatively = uiState.canManageNatively,
                                            authorizations = uiState.authorizations,
                                            onRevokeAuthorization = accountViewModel::revokeAppAuthorization,
                                            onManageAppsOnWeb = {
                                                activity?.let { accountViewModel.openWebSection(it, "apps") }
                                            },
                                            onOpenCatalogUrl = { url -> openExternalHttpsUrl(context, url) },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        NativeAuthDialog(
            state = uiState.nativeAuth,
            onDismiss = accountViewModel::dismissNativeAuthDialog,
            onSwitchStep = accountViewModel::setNativeAuthStep,
            onSubmitIdentifier = accountViewModel::submitNativeIdentifier,
            onCheckUsername = accountViewModel::checkNativeUsername,
            onSubmitPassword = accountViewModel::submitNativePassword,
            onSubmitMfa = accountViewModel::submitNativeMfa,
            onSubmitSignUp = accountViewModel::submitNativeSignUp,
            onSubmitForgotPassword = accountViewModel::submitForgotPassword,
            onSubmitResetPassword = accountViewModel::submitResetPassword,
            onSignInWithBrowser = {
                activity?.let { accountViewModel.startSignIn(it, chooseAccount = true) }
            },
        )

        if (uiState.showAccountSwitcher && activeAccount != null) {
            AccountSwitcherDialog(
                activeAccount = activeAccount,
                otherLocalAccounts = uiState.otherLocalAccounts,
                peerAccountsToAuthorize = uiState.unauthorizedPeerAccounts,
                onDismiss = { accountViewModel.toggleAccountSwitcher(false) },
                onSwitchAccount = accountViewModel::switchAccount,
                onContinueWithPeerAccount = accountViewModel::continueWithPeerAccount,
                onAddAnotherAccount = { accountViewModel.openNativeAuthDialog() },
                onRequestSignOut = accountViewModel::openSignOutDialog,
                onManageOnWeb = {
                    accountViewModel.toggleAccountSwitcher(false)
                    activity?.let { accountViewModel.openWebSection(it, uiState.selectedTab.webSection) }
                },
                onOpenPrivacy = {
                    accountViewModel.toggleAccountSwitcher(false)
                    activity?.let { accountViewModel.openWebPath(it, "/privacy") }
                },
                onOpenTerms = {
                    accountViewModel.toggleAccountSwitcher(false)
                    activity?.let { accountViewModel.openWebPath(it, "/terms") }
                },
            )
        }

        uiState.signOutTarget?.let { target ->
            SignOutOrRemoveDialog(
                target = target,
                onDismiss = accountViewModel::dismissSignOutDialog,
                onSignOutThisApp = { accountViewModel.signOutThisApp(target.userId) },
                onRemoveFromDevice = { accountViewModel.removeFromDevice(target.userId) },
                onDisconnectGrantOnServer = { accountViewModel.disconnectGrantOnServer(target.userId) },
                onManageWebSessions = {
                    accountViewModel.dismissSignOutDialog()
                    activity?.let { accountViewModel.openWebSection(it, "security") }
                },
            )
        }

        val currentDialog = uiState.activeManagementDialog
        if (currentDialog != null && activeAccount != null) {
            when (currentDialog) {
                ActiveManagementDialog.EditBasicProfile -> EditBasicProfileDialog(
                    account = activeAccount,
                    isSaving = uiState.isSavingProfile,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onSave = accountViewModel::saveBasicProfile,
                )

                ActiveManagementDialog.Avatar -> AvatarManagementDialog(
                    account = activeAccount,
                    isBusy = uiState.isManagementBusy,
                    errorMessage = uiState.managementError,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onUploadAvatar = accountViewModel::uploadAvatar,
                    onRemoveAvatar = accountViewModel::removeAvatar,
                )

                ActiveManagementDialog.Phone -> PhoneManagementDialog(
                    account = activeAccount,
                    isBusy = uiState.isManagementBusy,
                    requiresMfa = uiState.managementRequiresMfaPrompt,
                    errorMessage = uiState.managementError,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onSavePhone = accountViewModel::updatePhoneNumber,
                )

                ActiveManagementDialog.RecoveryEmail -> RecoveryEmailDialog(
                    account = activeAccount,
                    isBusy = uiState.isManagementBusy,
                    pendingVerification = uiState.recoveryEmailPendingVerification,
                    requiresMfa = uiState.managementRequiresMfaPrompt,
                    errorMessage = uiState.managementError,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onRequestChange = accountViewModel::requestRecoveryEmailChange,
                    onVerifyToken = accountViewModel::verifyRecoveryEmailChange,
                )

                ActiveManagementDialog.Password -> PasswordManagementDialog(
                    account = activeAccount,
                    isBusy = uiState.isManagementBusy,
                    requiresMfa = uiState.managementRequiresMfaPrompt,
                    errorMessage = uiState.managementError,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onChangePassword = accountViewModel::changePassword,
                )

                ActiveManagementDialog.TwoFactor -> TwoFactorManagementDialog(
                    account = activeAccount,
                    mfaStatus = uiState.mfaStatus,
                    mfaSetup = uiState.mfaSetup,
                    recoveryCodes = uiState.mfaRecoveryCodes,
                    isBusy = uiState.isManagementBusy,
                    errorMessage = uiState.managementError,
                    onCopyText = { label, value ->
                        copyToClipboard(context, label, value)
                        accountViewModel.showMessage("$label copied")
                    },
                    onShowMessage = accountViewModel::showMessage,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onBeginEnroll = accountViewModel::beginMfaEnrollment,
                    onConfirmEnroll = accountViewModel::confirmMfaEnrollment,
                    onRegenerateCodes = accountViewModel::regenerateMfaRecoveryCodes,
                    onDisableMfa = accountViewModel::disableMfa,
                )

                ActiveManagementDialog.DeleteAccount -> DeleteAccountDialog(
                    account = activeAccount,
                    isBusy = uiState.isManagementBusy,
                    requiresMfa = uiState.managementRequiresMfaPrompt,
                    errorMessage = uiState.managementError,
                    onDismiss = accountViewModel::dismissManagementDialog,
                    onConfirmDelete = accountViewModel::deleteAccount,
                )

                is ActiveManagementDialog.ExportData -> ExportDataDialog(
                    jsonContent = currentDialog.jsonContent,
                    onCopyJson = { json ->
                        copyToClipboard(context, "Gothwad Account Data", json)
                        accountViewModel.showMessage("Account export JSON copied")
                    },
                    onShowMessage = accountViewModel::showMessage,
                    onDismiss = accountViewModel::dismissManagementDialog,
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
}

private fun openExternalHttpsUrl(context: Context, url: String) {
    if (!url.startsWith("https://")) return
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (_: ActivityNotFoundException) {
        }
    }
}
