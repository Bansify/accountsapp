package com.gothwad.accounts.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.ui.components.AccountSwitcherDialog
import com.gothwad.accounts.ui.components.AccountsTabBar
import com.gothwad.accounts.ui.components.AccountsTopAppBar
import com.gothwad.accounts.ui.components.AddPaymentMethodDialog
import com.gothwad.accounts.ui.components.ChangePasswordDialog
import com.gothwad.accounts.ui.components.DeleteAccountPreviewDialog
import com.gothwad.accounts.ui.components.EditFieldDialog
import com.gothwad.accounts.ui.components.PhotoPickerDemoDialog
import com.gothwad.accounts.ui.components.SendFeedbackDialog
import com.gothwad.accounts.ui.components.TakeoutExportDialog
import com.gothwad.accounts.ui.model.AccountSubScreen
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.EditableField
import com.gothwad.accounts.ui.screens.ActivityControlDetailScreen
import com.gothwad.accounts.ui.screens.DataPrivacyTabScreen
import com.gothwad.accounts.ui.screens.DeviceManagementScreen
import com.gothwad.accounts.ui.screens.HelpSupportScreen
import com.gothwad.accounts.ui.screens.HomeTabScreen
import com.gothwad.accounts.ui.screens.PasswordManagerScreen
import com.gothwad.accounts.ui.screens.PaymentsSubscriptionsTabScreen
import com.gothwad.accounts.ui.screens.PeopleSharingTabScreen
import com.gothwad.accounts.ui.screens.PersonalInfoTabScreen
import com.gothwad.accounts.ui.screens.PrivacyCheckupScreen
import com.gothwad.accounts.ui.screens.SearchAccountScreen
import com.gothwad.accounts.ui.screens.SecurityCheckupScreen
import com.gothwad.accounts.ui.screens.SecurityTabScreen
import com.gothwad.accounts.ui.screens.StorageDetailsScreen
import com.gothwad.accounts.ui.screens.ThirdPartyAppsScreen
import com.gothwad.accounts.ui.screens.TwoStepVerificationScreen
import com.gothwad.accounts.ui.state.AccountDashboardViewModel

@Composable
fun SignInButton(modifier: Modifier = Modifier) {
    val activity = LocalContext.current.activity()
    Button(
        onClick = { activity?.let { GothwadAccounts.signIn(it) } },
        enabled = activity != null,
        modifier = modifier,
    ) {
        Text(stringResource(R.string.gothwad_sign_in))
    }
}

/**
 * Full-level Google Account-inspired "Manage your Account" dashboard with
 * Home, Personal info, Data & privacy, Security, People & sharing, and
 * Payments & subscriptions screens backed by rich interactive demo data
 * (and synced if a live GothwadAccounts OAuth session is signed in).
 */
@Composable
fun GothwadAccountsScreen(
    viewModel: AccountDashboardViewModel = viewModel(),
) {
    val coreState by GothwadAccounts.state.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current.activity()

    // Sync any live OAuth signed-in account from :core while preserving demo showcase readiness
    LaunchedEffect(coreState) {
        val signedIn = coreState as? AccountState.SignedIn
        if (signedIn != null) {
            viewModel.syncCoreSignedInAccount(signedIn.account)
        }
    }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val currentSubScreen = uiState.currentSubScreen
    BackHandler(enabled = currentSubScreen != null) {
        viewModel.navigateBack()
    }

    val subScreenTitle = when (currentSubScreen) {
        AccountSubScreen.SearchAccount -> "Search Account"
        AccountSubScreen.PrivacyCheckup -> "Privacy Checkup"
        AccountSubScreen.SecurityCheckup -> "Security Checkup"
        AccountSubScreen.DeviceManagement -> "Your devices"
        is AccountSubScreen.ActivityControlDetail -> {
            uiState.activityControls.firstOrNull { it.id == currentSubScreen.controlId }?.title ?: "Activity controls"
        }
        AccountSubScreen.PasswordManager -> "Password Manager"
        AccountSubScreen.ThirdPartyApps -> "Third-party apps"
        AccountSubScreen.TwoStepVerification -> "2-Step Verification"
        AccountSubScreen.StorageDetails -> "Account storage"
        AccountSubScreen.HelpSupport -> "Help & support"
        null -> null
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        containerColor = LocalAccountColors.current.canvasBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Column {
                AccountsTopAppBar(
                    activeAccount = uiState.activeAccount,
                    canNavigateBack = currentSubScreen != null,
                    subScreenTitle = subScreenTitle,
                    onBackClick = { viewModel.navigateBack() },
                    onSearchClick = { viewModel.openSubScreen(AccountSubScreen.SearchAccount) },
                    onHelpClick = { viewModel.openSubScreen(AccountSubScreen.HelpSupport) },
                    onAvatarClick = { viewModel.toggleAccountSwitcher(true) },
                )
                if (currentSubScreen == null) {
                    AccountsTabBar(
                        selectedTab = uiState.selectedTab,
                        onSelectTab = { viewModel.selectTab(it) },
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(LocalAccountColors.current.canvasBackground),
        ) {
            AnimatedContent(
                targetState = Pair(currentSubScreen, uiState.selectedTab),
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "account_screen_transition",
            ) { (subScreen, tab) ->
                if (subScreen != null) {
                    when (subScreen) {
                        AccountSubScreen.SearchAccount -> SearchAccountScreen(
                            query = uiState.searchQuery,
                            searchIndex = remember { viewModel.getSearchIndex() },
                            onQueryChange = { viewModel.updateSearchQuery(it) },
                            onSelectResult = { item ->
                                viewModel.selectTab(item.category)
                                item.targetSubScreen?.let { viewModel.openSubScreen(it) }
                                item.targetEditField?.let { viewModel.openEditField(it) }
                            },
                        )

                        AccountSubScreen.PrivacyCheckup -> PrivacyCheckupScreen(
                            state = uiState,
                            onToggleStepDone = { viewModel.togglePrivacyCheckupStep(it) },
                            onToggleActivityControl = { viewModel.toggleActivityControl(it) },
                            onTogglePersonalizedAds = { viewModel.togglePersonalizedAds(it) },
                            onDone = {
                                viewModel.navigateBack()
                                viewModel.showToastMessage("Privacy Checkup saved")
                            },
                        )

                        AccountSubScreen.SecurityCheckup -> SecurityCheckupScreen(
                            state = uiState,
                            onResolveRecommendation = { viewModel.resolveSecurityRecommendation(it) },
                            onEnableTwoStep = { viewModel.toggleTwoStepVerification(true) },
                            onEnableSafeBrowsing = { viewModel.toggleEnhancedSafeBrowsing(true) },
                        )

                        AccountSubScreen.DeviceManagement -> DeviceManagementScreen(
                            state = uiState,
                            onSignOutDevice = { viewModel.signOutDevice(it) },
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )

                        is AccountSubScreen.ActivityControlDetail -> ActivityControlDetailScreen(
                            controlId = subScreen.controlId,
                            state = uiState,
                            onToggleControl = { viewModel.toggleActivityControl(it) },
                            onUpdateAutoDelete = { id, period -> viewModel.updateAutoDeletePeriod(id, period) },
                        )

                        AccountSubScreen.ThirdPartyApps -> ThirdPartyAppsScreen(
                            state = uiState,
                            onRevokeApp = { viewModel.revokeThirdPartyApp(it) },
                        )

                        AccountSubScreen.TwoStepVerification -> TwoStepVerificationScreen(
                            state = uiState,
                            onToggleTwoStep = { viewModel.toggleTwoStepVerification(it) },
                            onAddPasskey = { viewModel.addPasskey() },
                            onRegenerateBackupCodes = { viewModel.regenerateBackupCodes() },
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )

                        AccountSubScreen.PasswordManager -> PasswordManagerScreen(
                            state = uiState,
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )

                        AccountSubScreen.StorageDetails -> StorageDetailsScreen(
                            state = uiState,
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )

                        AccountSubScreen.HelpSupport -> HelpSupportScreen(
                            onOpenFeedback = { viewModel.setDialogFlags(feedback = true) },
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )
                    }
                } else {
                    when (tab) {
                        AccountTab.HOME -> HomeTabScreen(
                            state = uiState,
                            onAvatarEditClick = { viewModel.setDialogFlags(photoPicker = true) },
                            onOpenSearch = { viewModel.openSubScreen(AccountSubScreen.SearchAccount) },
                            onSelectTab = { viewModel.selectTab(it) },
                            onOpenSubScreen = { viewModel.openSubScreen(it) },
                            onOpenFeedback = { viewModel.setDialogFlags(feedback = true) },
                        )

                        AccountTab.PERSONAL_INFO -> PersonalInfoTabScreen(
                            state = uiState,
                            onOpenPhotoPicker = { viewModel.setDialogFlags(photoPicker = true) },
                            onEditField = { viewModel.openEditField(it) },
                            onToggleProfileVisibility = { viewModel.toggleProfileVisibility(it) },
                            onOpenChangePassword = { viewModel.setDialogFlags(changePassword = true) },
                            onOpenPasskeys = { viewModel.openSubScreen(AccountSubScreen.TwoStepVerification) },
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )

                        AccountTab.DATA_PRIVACY -> DataPrivacyTabScreen(
                            state = uiState,
                            onOpenSubScreen = { viewModel.openSubScreen(it) },
                            onSelectTab = { viewModel.selectTab(it) },
                            onTogglePersonalizedAds = { viewModel.togglePersonalizedAds(it) },
                            onToggleLocationSharing = { viewModel.toggleLocationSharing(it) },
                            onTogglePartnerSharing = { viewModel.togglePartnerSharing(it) },
                            onOpenTakeout = { viewModel.setDialogFlags(takeout = true) },
                            onOpenDeleteAccount = { viewModel.setDialogFlags(deleteAccount = true) },
                        )

                        AccountTab.SECURITY -> SecurityTabScreen(
                            state = uiState,
                            onOpenSubScreen = { viewModel.openSubScreen(it) },
                            onEditField = { viewModel.openEditField(it) },
                            onOpenChangePassword = { viewModel.setDialogFlags(changePassword = true) },
                            onToggleEnhancedSafeBrowsing = { viewModel.toggleEnhancedSafeBrowsing(it) },
                        )

                        AccountTab.PEOPLE_SHARING -> PeopleSharingTabScreen(
                            state = uiState,
                            onToggleLocationSharing = { viewModel.toggleLocationSharing(it) },
                            onToggleSharedEndorsements = { viewModel.toggleSharedEndorsements(it) },
                            onUnblockUser = { viewModel.unblockUser(it) },
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )

                        AccountTab.PAYMENTS_SUBSCRIPTIONS -> PaymentsSubscriptionsTabScreen(
                            state = uiState,
                            onOpenSubScreen = { viewModel.openSubScreen(it) },
                            onOpenAddPayment = { viewModel.setDialogFlags(addPayment = true) },
                            onShowMessage = { viewModel.showToastMessage(it) },
                        )
                    }
                }
            }
        }
    }

    // Interactive Modals
    if (uiState.showAccountSwitcher) {
        AccountSwitcherDialog(
            activeAccount = uiState.activeAccount,
            otherAccounts = uiState.otherAccounts,
            onDismiss = { viewModel.toggleAccountSwitcher(false) },
            onSwitchAccount = { viewModel.switchAccount(it) },
            onAddDemoAccount = { name, email -> viewModel.addDemoAccount(name, email) },
            onSignInWithBrowser = {
                viewModel.toggleAccountSwitcher(false)
                activity?.let { GothwadAccounts.signIn(it, chooseAccount = true) }
            },
            onOpenDeviceAccountsTip = {
                viewModel.toggleAccountSwitcher(false)
                viewModel.openSubScreen(AccountSubScreen.DeviceManagement)
            },
        )
    }

    uiState.activeEditField?.let { field ->
        val account = uiState.activeAccount
        val currentValue = when (field) {
            EditableField.NAME -> account.name
            EditableField.BIRTHDAY -> account.birthday
            EditableField.GENDER -> account.gender
            EditableField.PRONOUNS -> account.pronouns
            EditableField.RECOVERY_EMAIL -> account.recoveryEmail
            EditableField.RECOVERY_PHONE -> account.phone
            EditableField.HOME_ADDRESS -> account.homeAddress
            EditableField.WORK_ADDRESS -> account.workAddress
            EditableField.LANGUAGE -> account.language
        }
        EditFieldDialog(
            field = field,
            currentValue = currentValue,
            onDismiss = { viewModel.openEditField(null) },
            onSave = { viewModel.saveEditedField(field, it) },
        )
    }

    if (uiState.showChangePasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { viewModel.setDialogFlags(changePassword = false) },
            onConfirmChange = { viewModel.updatePassword() },
        )
    }

    if (uiState.showPhotoPickerDialog) {
        PhotoPickerDemoDialog(
            account = uiState.activeAccount,
            onDismiss = { viewModel.setDialogFlags(photoPicker = false) },
            onSavePhoto = {
                viewModel.setDialogFlags(photoPicker = false)
                viewModel.showToastMessage("Profile picture updated")
            },
        )
    }

    if (uiState.showTakeoutDialog) {
        TakeoutExportDialog(
            onDismiss = { viewModel.setDialogFlags(takeout = false) },
            onStartExport = {
                viewModel.setDialogFlags(takeout = false)
                viewModel.showToastMessage("Archive export started — link will be emailed to ${uiState.activeAccount.email}")
            },
        )
    }

    if (uiState.showDeleteAccountDialog) {
        DeleteAccountPreviewDialog(
            email = uiState.activeAccount.email,
            onDismiss = { viewModel.setDialogFlags(deleteAccount = false) },
            onConfirmDemo = {
                viewModel.setDialogFlags(deleteAccount = false)
                viewModel.showToastMessage("Demo mode: Account deletion simulation completed")
            },
        )
    }

    if (uiState.showAddPaymentDialog) {
        AddPaymentMethodDialog(
            onDismiss = { viewModel.setDialogFlags(addPayment = false) },
            onAdd = { label, detail, type -> viewModel.addPaymentMethod(label, detail, type) },
        )
    }

    if (uiState.showFeedbackDialog) {
        SendFeedbackDialog(
            onDismiss = { viewModel.setDialogFlags(feedback = false) },
            onSend = {
                viewModel.setDialogFlags(feedback = false)
                viewModel.showToastMessage("Thank you! Feedback submitted")
            },
        )
    }
}

private tailrec fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.activity()
    else -> null
}
