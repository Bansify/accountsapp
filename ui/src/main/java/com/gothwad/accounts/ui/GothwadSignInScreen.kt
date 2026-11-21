package com.gothwad.accounts.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import java.util.UUID
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.ui.components.AccountAvatar
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.GothwadAuthStatusBanner
import com.gothwad.accounts.ui.components.GothwadBrandBadge
import com.gothwad.accounts.ui.components.NativeAuthCardContent
import com.gothwad.accounts.ui.components.displayEmail
import com.gothwad.accounts.ui.components.displayName
import com.gothwad.accounts.ui.components.initials
import com.gothwad.accounts.ui.model.NativeAuthStep
import com.gothwad.accounts.ui.model.ThemePreference
import com.gothwad.accounts.ui.screens.InitializingAccountScreen
import com.gothwad.accounts.ui.screens.StorageCorruptedRecoveryScreen
import com.gothwad.accounts.ui.state.AccountAppearancePrefs
import com.gothwad.accounts.ui.state.AccountDashboardViewModel

/**
 * Launches the shared native Bansify Sign-In / Sign-Up / MFA / Peer-SSO flow inside the host app,
 * automatically returning when sign-in or adding another account completes.
 */
@JvmOverloads
fun GothwadAccounts.openSignIn(
    activity: Activity,
    startStep: NativeAuthStep = NativeAuthStep.IDENTIFIER,
    initialIdentifier: String = "",
    appName: String? = null,
    allowWhenAlreadySignedIn: Boolean = true,
) {
    val intent = Intent(activity, AccountSignInActivity::class.java).apply {
        putExtra(AccountSignInActivity.EXTRA_START_STEP, startStep.name)
        putExtra(AccountSignInActivity.EXTRA_INITIAL_IDENTIFIER, initialIdentifier)
        if (!appName.isNullOrBlank()) {
            putExtra(AccountSignInActivity.EXTRA_APP_NAME, appName)
        }
        putExtra(AccountSignInActivity.EXTRA_ALLOW_WHEN_SIGNED_IN, allowWhenAlreadySignedIn)
    }
    activity.startActivity(intent)
}

class AccountSignInActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.WHITE, DARK_CANVAS_COLOR),
            navigationBarStyle = SystemBarStyle.auto(Color.WHITE, DARK_CANVAS_COLOR),
        )
        val startStep = intent.getStringExtra(EXTRA_START_STEP)
            ?.let { raw -> NativeAuthStep.entries.firstOrNull { it.name == raw } }
            ?: NativeAuthStep.IDENTIFIER
        val initialIdentifier = intent.getStringExtra(EXTRA_INITIAL_IDENTIFIER).orEmpty()
        val appName = intent.getStringExtra(EXTRA_APP_NAME)
        val allowWhenAlreadySignedIn = intent.getBooleanExtra(EXTRA_ALLOW_WHEN_SIGNED_IN, true)

        setContent {
            GothwadSignInScreen(
                startStep = startStep,
                initialIdentifier = initialIdentifier,
                appName = appName,
                allowWhenAlreadySignedIn = allowWhenAlreadySignedIn,
                onSignedInComplete = {
                    setResult(RESULT_OK)
                    finish()
                },
                onClose = {
                    setResult(RESULT_CANCELED)
                    finish()
                },
            )
        }
    }

    internal companion object {
        const val EXTRA_START_STEP = "com.gothwad.accounts.ui.EXTRA_START_STEP"
        const val EXTRA_INITIAL_IDENTIFIER = "com.gothwad.accounts.ui.EXTRA_INITIAL_IDENTIFIER"
        const val EXTRA_APP_NAME = "com.gothwad.accounts.ui.EXTRA_APP_NAME"
        const val EXTRA_ALLOW_WHEN_SIGNED_IN = "com.gothwad.accounts.ui.EXTRA_ALLOW_WHEN_SIGNED_IN"
        const val DARK_CANVAS_COLOR = 0xFF212121.toInt()
    }
}

/**
 * Shared standalone Material 3 Sign-In / Sign-Up / MFA / Peer-SSO surface usable inside
 * [AccountSignInActivity] or directly embedded inside any Compose host app.
 */
@Composable
fun GothwadSignInScreen(
    startStep: NativeAuthStep = NativeAuthStep.IDENTIFIER,
    initialIdentifier: String = "",
    appName: String? = null,
    allowWhenAlreadySignedIn: Boolean = true,
    onSignedInComplete: () -> Unit = {},
    onClose: () -> Unit = {},
    accountViewModel: AccountDashboardViewModel = viewModel(),
) {
    val uiState by accountViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    val initialTheme = remember { AccountAppearancePrefs.loadTheme(context) }
    var prefsInitialized by remember { mutableStateOf(false) }

    val sessionKey = rememberSaveable { UUID.randomUUID().toString() }
    var entryPrepared by remember { mutableStateOf(false) }
    var baselineCaptured by rememberSaveable { mutableStateOf(false) }
    var baselineVmId by rememberSaveable { mutableStateOf("") }
    var baselineNativeSuccessCount by rememberSaveable { mutableIntStateOf(0) }
    var baselineBrowserEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var baselineUserIdsList by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    var baselineActiveUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var startedBrowserFlow by rememberSaveable { mutableStateOf(false) }
    var completionDispatched by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(sessionKey) {
        accountViewModel.setThemePreference(initialTheme)
        accountViewModel.ensureSignInSessionInitialized(sessionKey, startStep, initialIdentifier)
        entryPrepared = true
        prefsInitialized = true
    }

    LaunchedEffect(
        uiState.accountState,
        uiState.authSuccessCount,
        uiState.latestBrowserAuthEventId,
        entryPrepared,
        startedBrowserFlow,
    ) {
        if (!entryPrepared || completionDispatched) return@LaunchedEffect
        val accountState = uiState.accountState
        if (accountState == AccountState.Initializing) return@LaunchedEffect

        if (!baselineCaptured) {
            baselineCaptured = true
            baselineVmId = accountViewModel.viewModelInstanceId
            baselineNativeSuccessCount = uiState.authSuccessCount
            baselineBrowserEventId = uiState.latestBrowserAuthEventId
            val existing = (accountState as? AccountState.SignedIn)
            baselineUserIdsList = existing?.all?.map { it.userId }.orEmpty()
            baselineActiveUserId = existing?.account?.userId
            if (!allowWhenAlreadySignedIn && existing != null) {
                completionDispatched = true
                onSignedInComplete()
            }
            return@LaunchedEffect
        }

        val baseline = AccountDashboardViewModel.SignInBaseline(
            viewModelInstanceId = baselineVmId,
            nativeSuccessCount = baselineNativeSuccessCount,
            browserEventId = baselineBrowserEventId,
            userIds = baselineUserIdsList.toSet(),
            activeUserId = baselineActiveUserId,
        )
        if (accountViewModel.evaluateSignInCompletion(baseline, startedBrowserFlow)) {
            completionDispatched = true
            onSignedInComplete()
        }
    }

    val effectiveTheme = if (prefsInitialized) uiState.themePreference else initialTheme
    val darkTheme = when (effectiveTheme) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    val currentStep = uiState.nativeAuth.step
    BackHandler(enabled = uiState.nativeAuth.isBusy || currentStep != startStep) {
        if (uiState.nativeAuth.isBusy) return@BackHandler
        when (currentStep) {
            NativeAuthStep.MFA -> accountViewModel.setNativeAuthStep(NativeAuthStep.PASSWORD)
            NativeAuthStep.RESET_PASSWORD -> accountViewModel.setNativeAuthStep(NativeAuthStep.FORGOT_PASSWORD)
            else -> accountViewModel.setNativeAuthStep(startStep)
        }
    }

    GothwadAccountsTheme(darkTheme = darkTheme) {
        val semantic = LocalAccountColors.current
        val topBarInsets = WindowInsets.statusBars.union(
            WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        )
        val subtitle = appName?.takeIf { it.isNotBlank() }?.let {
            stringResource(R.string.gothwad_sign_in_continue_to, it)
        } ?: stringResource(R.string.gothwad_native_auth_subtitle)

        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .testTag("shared_sign_in_screen"),
            contentWindowInsets = WindowInsets.safeDrawing.only(
                WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
            ),
            containerColor = semantic.canvasBackground,
            topBar = {
                Surface(color = semantic.canvasBackground, tonalElevation = 0.dp) {
                    Row(
                        modifier = Modifier
                            .windowInsetsPadding(topBarInsets)
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { if (!uiState.nativeAuth.isBusy) onClose() },
                            modifier = Modifier.testTag("top_sign_in_close_button"),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.gothwad_cd_back),
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        GothwadBrandBadge(size = 22.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.gothwad_account_brand),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            },
        ) { innerPadding ->
            val accountState = uiState.accountState
            when (accountState) {
                AccountState.Initializing -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        InitializingAccountScreen()
                    }
                }

                is AccountState.StorageCorrupted -> {
                    Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        StorageCorruptedRecoveryScreen(
                            reason = accountState.reason,
                            onRecoverClick = accountViewModel::recoverCorruptedStorage,
                        )
                    }
                }

                AccountState.SignedOut,
                is AccountState.SignedIn -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .imePadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        uiState.authError?.let { failure ->
                            GothwadAuthStatusBanner(
                                failure = failure,
                                onRetry = {
                                    accountViewModel.clearAuthError()
                                    accountViewModel.syncPeers()
                                },
                                onSignInAgain = {
                                    accountViewModel.clearAuthError()
                                    activity?.let {
                                        startedBrowserFlow = true
                                        accountViewModel.startSignIn(it, chooseAccount = true)
                                    }
                                },
                                onDismiss = accountViewModel::clearAuthError,
                                modifier = Modifier.widthIn(max = 440.dp),
                            )
                        }

                        if (uiState.isPeerSilentSigningIn) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .widthIn(max = 440.dp)
                                    .padding(horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(
                                    text = "Signing in silently with your Bansify device session…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }

                        val peerAccounts = uiState.unauthorizedPeerAccounts
                        if (peerAccounts.isNotEmpty() && currentStep == NativeAuthStep.IDENTIFIER) {
                            AccountSectionCard(
                                title = stringResource(R.string.gothwad_discovered_on_device),
                                subtitle = stringResource(R.string.gothwad_discovered_helper),
                                modifier = Modifier.widthIn(max = 440.dp),
                            ) {
                                peerAccounts.forEachIndexed { index, peer ->
                                    if (index > 0) {
                                        HorizontalDivider(
                                            color = semantic.cardBorder,
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                        )
                                    }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .clickable(
                                                enabled = !uiState.isPeerSilentSigningIn && !uiState.nativeAuth.isBusy,
                                                role = Role.Button,
                                            ) {
                                                accountViewModel.continueWithPeerAccount(peer)
                                            }
                                            .padding(horizontal = 16.dp, vertical = 10.dp)
                                            .testTag("sign_in_peer_${peer.userId}"),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        AccountAvatar(
                                            displayName = peer.displayName(),
                                            initials = peer.initials(),
                                            avatarUrl = peer.avatarUrl,
                                            size = 38.dp,
                                            showBrandRing = true,
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(R.string.gothwad_continue_as, peer.displayName()),
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Text(
                                                text = "${peer.displayEmail()} · ${peer.sourcePackage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, semantic.cardBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 440.dp),
                        ) {
                            NativeAuthCardContent(
                                state = uiState.nativeAuth,
                                subtitle = subtitle,
                                showCloseButton = false,
                                scrollable = false,
                                onDismiss = onClose,
                                onSwitchStep = accountViewModel::setNativeAuthStep,
                                onSubmitIdentifier = accountViewModel::submitNativeIdentifier,
                                onCheckUsername = accountViewModel::checkNativeUsername,
                                onSubmitPassword = accountViewModel::submitNativePassword,
                                onSubmitMfa = accountViewModel::submitNativeMfa,
                                onSubmitSignUp = accountViewModel::submitNativeSignUp,
                                onSubmitForgotPassword = accountViewModel::submitForgotPassword,
                                onSubmitResetPassword = accountViewModel::submitResetPassword,
                                onSignInWithBrowser = {
                                    activity?.let {
                                        startedBrowserFlow = true
                                        accountViewModel.startSignIn(it, chooseAccount = true)
                                    }
                                },
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            modifier = Modifier.widthIn(max = 440.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            TextButton(
                                onClick = { activity?.let { accountViewModel.openWebPath(it, "/privacy") } },
                            ) {
                                Text(stringResource(R.string.gothwad_privacy))
                            }
                            TextButton(
                                onClick = { activity?.let { accountViewModel.openWebPath(it, "/terms") } },
                            ) {
                                Text(stringResource(R.string.gothwad_terms))
                            }
                        }
                    }
                }
            }
        }
    }
}
