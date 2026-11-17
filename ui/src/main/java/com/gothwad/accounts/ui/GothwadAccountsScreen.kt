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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.ui.components.AccountSwitcherDialog
import com.gothwad.accounts.ui.components.AccountsBottomBar
import com.gothwad.accounts.ui.components.AccountsSidebarContent
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
import com.gothwad.accounts.ui.model.NavigationLayoutMode
import com.gothwad.accounts.ui.model.ThemePreference
import com.gothwad.accounts.ui.screens.AppSettingsScreen
import com.gothwad.accounts.ui.screens.ConnectedAppsTabScreen
import com.gothwad.accounts.ui.screens.DataPrivacyTabScreen
import com.gothwad.accounts.ui.screens.HelpLegalScreen
import com.gothwad.accounts.ui.screens.HomeTabScreen
import com.gothwad.accounts.ui.screens.InitializingAccountScreen
import com.gothwad.accounts.ui.screens.OthersHubScreen
import com.gothwad.accounts.ui.screens.PeopleSharingTabScreen
import com.gothwad.accounts.ui.screens.PersonalInfoTabScreen
import com.gothwad.accounts.ui.screens.SearchAccountScreen
import com.gothwad.accounts.ui.screens.SecurityTabScreen
import com.gothwad.accounts.ui.screens.SignedOutAccountScreen
import com.gothwad.accounts.ui.screens.StorageCorruptedRecoveryScreen
import com.gothwad.accounts.ui.state.AccountAppearancePrefs
import com.gothwad.accounts.ui.state.AccountDashboardViewModel
import kotlinx.coroutines.launch

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
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var prefsInitialized by remember { mutableStateOf(false) }
    val initialTheme = remember { AccountAppearancePrefs.loadTheme(context) }
    val initialLayout = remember { AccountAppearancePrefs.loadNavigationLayout(context) }

    LaunchedEffect(Unit) {
        accountViewModel.setThemePreference(initialTheme)
        accountViewModel.setNavigationLayout(initialLayout)
        prefsInitialized = true
    }

    val effectiveTheme = if (prefsInitialized) uiState.themePreference else initialTheme
    val effectiveLayout = if (prefsInitialized) uiState.navigationLayout else initialLayout
    val darkTheme = when (effectiveTheme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val currentSubScreen = uiState.currentSubScreen
    val activeAccount = uiState.activeAccount
    val isFromOthersSubPage = currentSubScreen == null &&
        activeAccount != null &&
        effectiveLayout == NavigationLayoutMode.BOTTOM_BAR &&
        !uiState.showOthersHub &&
        (uiState.selectedTab == AccountTab.PEOPLE_SHARING || uiState.selectedTab == AccountTab.CONNECTED_APPS)

    BackHandler(enabled = drawerState.isOpen || currentSubScreen != null || isFromOthersSubPage) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            accountViewModel.navigateBack()
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val message = uiState.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        accountViewModel.clearSnackbar()
    }

    GothwadAccountsTheme(darkTheme = darkTheme) {
        val semantic = LocalAccountColors.current

        val subScreenTitle = when (currentSubScreen) {
            AccountSubScreen.SearchAccount -> stringResource(R.string.gothwad_search_title)
            AccountSubScreen.HelpLegal -> stringResource(R.string.gothwad_help_title)
            AccountSubScreen.AppSettings -> stringResource(R.string.gothwad_settings_title)
            null -> if (isFromOthersSubPage) stringResource(uiState.selectedTab.titleRes) else null
        }

        val canNavigateBack = currentSubScreen != null || isFromOthersSubPage

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val usePermanentSidebar = effectiveLayout == NavigationLayoutMode.LEFT_SIDEBAR && maxWidth >= 720.dp
            val useModalSidebar = effectiveLayout == NavigationLayoutMode.LEFT_SIDEBAR && !usePermanentSidebar

            val renderScaffold: @Composable (Modifier) -> Unit = { scaffoldModifier ->
                Scaffold(
                    modifier = scaffoldModifier,
                    contentWindowInsets = WindowInsets.safeDrawing.only(
                        WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                    ),
                    containerColor = semantic.canvasBackground,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        Column {
                            AccountsTopAppBar(
                                activeAccount = activeAccount,
                                canNavigateBack = canNavigateBack,
                                subScreenTitle = subScreenTitle,
                                isRefreshing = uiState.isRefreshing,
                                showMenuButton = useModalSidebar && !canNavigateBack,
                                onMenuClick = { scope.launch { drawerState.open() } },
                                onBackClick = {
                                    if (!accountViewModel.navigateBack()) onClose()
                                },
                                onSearchClick = { accountViewModel.openSubScreen(AccountSubScreen.SearchAccount) },
                                onSettingsClick = { accountViewModel.openSubScreen(AccountSubScreen.AppSettings) },
                                onHelpClick = { accountViewModel.openSubScreen(AccountSubScreen.HelpLegal) },
                                onAvatarClick = { accountViewModel.toggleAccountSwitcher(true) },
                            )
                            if (activeAccount != null &&
                                currentSubScreen == null &&
                                effectiveLayout == NavigationLayoutMode.HEADER_PILLS
                            ) {
                                AccountsTabBar(
                                    selectedTab = uiState.selectedTab,
                                    onSelectTab = accountViewModel::selectTab,
                                )
                            }
                        }
                    },
                    bottomBar = {
                        if (activeAccount != null &&
                            currentSubScreen == null &&
                            effectiveLayout == NavigationLayoutMode.BOTTOM_BAR
                        ) {
                            val isOthersSelected = uiState.showOthersHub ||
                                uiState.selectedTab == AccountTab.PEOPLE_SHARING ||
                                uiState.selectedTab == AccountTab.CONNECTED_APPS
                            AccountsBottomBar(
                                selectedTab = uiState.selectedTab,
                                isOthersSelected = isOthersSelected,
                                onSelectTab = accountViewModel::selectTab,
                                onSelectOthers = accountViewModel::openOthersHub,
                            )
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

                            AccountSubScreen.AppSettings -> {
                                AppSettingsScreen(
                                    themePreference = effectiveTheme,
                                    navigationLayout = effectiveLayout,
                                    packageName = context.packageName,
                                    installedPeersCount = uiState.installedVerifiedPeers.size,
                                    onSelectTheme = { pref ->
                                        AccountAppearancePrefs.saveTheme(context, pref)
                                        accountViewModel.setThemePreference(pref)
                                    },
                                    onSelectNavigationLayout = { mode ->
                                        AccountAppearancePrefs.saveNavigationLayout(context, mode)
                                        accountViewModel.setNavigationLayout(mode)
                                    },
                                    onOpenHelpLegal = { accountViewModel.openSubScreen(AccountSubScreen.HelpLegal) },
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
                                        val activeTarget: Any = if (effectiveLayout == NavigationLayoutMode.BOTTOM_BAR && uiState.showOthersHub) {
                                            "others_hub"
                                        } else {
                                            uiState.selectedTab
                                        }

                                        AnimatedContent(
                                            targetState = activeTarget,
                                            transitionSpec = {
                                                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
                                            },
                                            label = "account_tab_transition",
                                        ) { destination ->
                                            if (destination == "others_hub") {
                                                OthersHubScreen(
                                                    themePreference = effectiveTheme,
                                                    installedPeersCount = uiState.installedVerifiedPeers.size,
                                                    onOpenTab = accountViewModel::selectTab,
                                                    onOpenSettings = { accountViewModel.openSubScreen(AccountSubScreen.AppSettings) },
                                                    onOpenHelpLegal = { accountViewModel.openSubScreen(AccountSubScreen.HelpLegal) },
                                                )
                                            } else {
                                                when (destination as AccountTab) {
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
                }
            }

            if (usePermanentSidebar) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        color = semantic.canvasBackground,
                        modifier = Modifier
                            .width(264.dp)
                            .fillMaxHeight(),
                    ) {
                        AccountsSidebarContent(
                            activeAccount = activeAccount,
                            selectedTab = uiState.selectedTab,
                            isSettingsSelected = currentSubScreen == AccountSubScreen.AppSettings,
                            isHelpSelected = currentSubScreen == AccountSubScreen.HelpLegal,
                            isRefreshing = uiState.isRefreshing,
                            onSelectTab = accountViewModel::selectTab,
                            onOpenSettings = { accountViewModel.openSubScreen(AccountSubScreen.AppSettings) },
                            onOpenHelp = { accountViewModel.openSubScreen(AccountSubScreen.HelpLegal) },
                            onRefreshClick = { accountViewModel.refreshProfile() },
                        )
                    }
                    VerticalDivider(color = semantic.cardBorder)
                    renderScaffold(Modifier.weight(1f).fillMaxHeight())
                }
            } else if (useModalSidebar) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    drawerContent = {
                        ModalDrawerSheet(
                            drawerContainerColor = semantic.canvasBackground,
                            modifier = Modifier.width(288.dp),
                        ) {
                            AccountsSidebarContent(
                                activeAccount = activeAccount,
                                selectedTab = uiState.selectedTab,
                                isSettingsSelected = currentSubScreen == AccountSubScreen.AppSettings,
                                isHelpSelected = currentSubScreen == AccountSubScreen.HelpLegal,
                                isRefreshing = uiState.isRefreshing,
                                onSelectTab = { tab ->
                                    accountViewModel.selectTab(tab)
                                    scope.launch { drawerState.close() }
                                },
                                onOpenSettings = {
                                    accountViewModel.openSubScreen(AccountSubScreen.AppSettings)
                                    scope.launch { drawerState.close() }
                                },
                                onOpenHelp = {
                                    accountViewModel.openSubScreen(AccountSubScreen.HelpLegal)
                                    scope.launch { drawerState.close() }
                                },
                                onRefreshClick = { accountViewModel.refreshProfile() },
                            )
                        }
                    },
                ) {
                    renderScaffold(Modifier.fillMaxSize())
                }
            } else {
                renderScaffold(Modifier.fillMaxSize())
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
                isRefreshing = uiState.isRefreshing,
                onRefreshClick = { accountViewModel.refreshProfile() },
                onOpenSettings = {
                    accountViewModel.toggleAccountSwitcher(false)
                    accountViewModel.openSubScreen(AccountSubScreen.AppSettings)
                },
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
