package com.gothwad.accounts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.ManageAccounts
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.R

/**
 * Reusable host-app toolbar avatar button (inherits the host app's MaterialTheme).
 * Displays the active account's avatar/initials when signed in, or an account icon when signed out.
 */
@Composable
fun GothwadAccountAvatarButton(
    accountState: AccountState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val active = (accountState as? AccountState.SignedIn)?.account
    val label = active?.displayName() ?: stringResource(R.string.gothwad_accounts)
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.gothwad_cd_avatar, label),
                onClick = onClick,
            )
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (active != null) {
            AccountAvatar(
                displayName = active.displayName(),
                initials = active.initials(),
                avatarUrl = active.avatarUrl,
                size = 30.dp,
                showBrandRing = true,
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.AccountCircle,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
    }
}

/**
 * Reusable embeddable switcher modal for sibling Gothwad apps (Mail, Drive, Kiwi Store, PlusTube).
 * Selecting an account calls [onSwitchAccount] for THIS app only; [onManageGothwadAccount] opens
 * the shared management experience (`GothwadAccounts.openAccountManagement(activity)`).
 */
@Composable
fun GothwadAccountSwitcherSheet(
    accountState: AccountState,
    discoveredAccounts: List<DiscoveredAccount>,
    onDismiss: () -> Unit,
    onSwitchAccount: (String) -> Unit,
    onContinueWithPeerAccount: (DiscoveredAccount) -> Unit,
    onAddAnotherAccount: () -> Unit,
    onManageGothwadAccount: () -> Unit,
    onSignOutThisApp: (GothwadAccount) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    isRefreshing: Boolean = false,
    onRefreshClick: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
) {
    val active = (accountState as? AccountState.SignedIn)?.account
    val all = (accountState as? AccountState.SignedIn)?.all.orEmpty()
    if (active != null) {
        AccountSwitcherDialog(
            activeAccount = active,
            otherLocalAccounts = all.filter { it.userId != active.userId },
            peerAccountsToAuthorize = discoveredAccounts.filter { !it.isAuthorizedLocally },
            isRefreshing = isRefreshing,
            onRefreshClick = onRefreshClick,
            onOpenSettings = onOpenSettings,
            manageButtonLabel = stringResource(R.string.gothwad_manage_account),
            showManageExternalIcon = false,
            onDismiss = onDismiss,
            onSwitchAccount = onSwitchAccount,
            onContinueWithPeerAccount = onContinueWithPeerAccount,
            onAddAnotherAccount = onAddAnotherAccount,
            onRequestSignOut = onSignOutThisApp,
            onManageOnWeb = onManageGothwadAccount,
            onOpenPrivacy = onOpenPrivacy,
            onOpenTerms = onOpenTerms,
        )
    }
}

/**
 * Reusable drop-in card for host app Settings screens (Mail, Drive, Store, PlusTube).
 */
@Composable
fun GothwadAccountSettingsCard(
    accountState: AccountState,
    discoveredCount: Int,
    onSignInClick: () -> Unit,
    onSwitchOrManageClick: () -> Unit,
    onManageAccountClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (accountState) {
                AccountState.Initializing -> {
                    Text(
                        text = stringResource(R.string.gothwad_loading_account),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                is AccountState.StorageCorrupted -> {
                    Text(
                        text = stringResource(R.string.gothwad_storage_corrupted_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(onClick = onManageAccountClick) {
                        Text(stringResource(R.string.gothwad_manage_account))
                    }
                }

                AccountState.SignedOut -> {
                    Text(
                        text = stringResource(R.string.gothwad_accounts),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = if (discoveredCount > 0) {
                            stringResource(R.string.gothwad_discovered_on_device) + " ($discoveredCount)"
                        } else {
                            stringResource(R.string.gothwad_auth_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = onSignInClick, shape = RoundedCornerShape(50)) {
                        Text(stringResource(R.string.gothwad_sign_in))
                    }
                }

                is AccountState.SignedIn -> {
                    val active = accountState.account
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountAvatar(
                            displayName = active.displayName(),
                            initials = active.initials(),
                            avatarUrl = active.avatarUrl,
                            size = 40.dp,
                            showBrandRing = true,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(active.displayName(), style = MaterialTheme.typography.titleMedium)
                            Text(
                                active.displayEmail(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onSwitchOrManageClick,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.gothwad_choose_account))
                        }
                        Button(
                            onClick = onManageAccountClick,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Rounded.ManageAccounts, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gothwad_accounts))
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
            }
        }
    }
}
