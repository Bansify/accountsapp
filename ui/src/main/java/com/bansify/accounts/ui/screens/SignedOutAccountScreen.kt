package com.bansify.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bansify.accounts.core.DiscoveredAccount
import com.bansify.accounts.ui.LocalAccountColors
import com.bansify.accounts.ui.R
import com.bansify.accounts.ui.components.AccountAvatar
import com.bansify.accounts.ui.components.AccountSectionCard
import com.bansify.accounts.ui.components.displayEmail
import com.bansify.accounts.ui.components.displayName
import com.bansify.accounts.ui.components.initials

@Composable
fun InitializingAccountScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("initializing_screen"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(R.string.gothwad_loading_account),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun StorageCorruptedRecoveryScreen(
    reason: String,
    onRecoverClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("storage_corrupted_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = semantic.criticalContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(
                            Icons.Rounded.LockReset,
                            contentDescription = null,
                            tint = semantic.onCriticalContainer,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = stringResource(R.string.gothwad_storage_corrupted_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = semantic.onCriticalContainer,
                        )
                    }
                    Text(
                        text = stringResource(R.string.gothwad_storage_corrupted_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = semantic.onCriticalContainer,
                    )
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = semantic.onCriticalContainer.copy(alpha = 0.8f),
                    )
                    Button(
                        onClick = onRecoverClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("recover_corrupted_storage_button"),
                    ) {
                        Text(stringResource(R.string.gothwad_storage_reset_action))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignedOutAccountScreen(
    discoveredAccounts: List<DiscoveredAccount>,
    isPeerSilentSigningIn: Boolean = false,
    onSignInClick: () -> Unit,
    onBrowserSignInClick: () -> Unit = onSignInClick,
    onContinueAsPeer: (DiscoveredAccount) -> Unit,
    onRefreshPeers: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("signed_out_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column {
                    // Top illustrated welcome banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
                                    ),
                                ),
                            )
                            .padding(16.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.gothwad_accounts),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "One secure identity for Mail, Drive, Meet, Notes & Kiwi Store.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Image(
                                painter = painterResource(id = R.drawable.img_security_shield),
                                contentDescription = "Bansify Account security illustration",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(14.dp)),
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.gothwad_auth_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            SignedOutFeatureChip(Icons.Rounded.Fingerprint, "Passkeys & 2FA")
                            SignedOutFeatureChip(Icons.Rounded.VerifiedUser, "Encrypted tokens")
                            SignedOutFeatureChip(Icons.Rounded.CloudDone, "Drive & Mail sync")
                        }
                        Button(
                            onClick = onSignInClick,
                            enabled = !isPeerSilentSigningIn,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("signed_out_sign_in_button"),
                        ) {
                            Icon(Icons.Rounded.AccountCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.gothwad_sign_in))
                        }
                        OutlinedButton(
                            onClick = onBrowserSignInClick,
                            enabled = !isPeerSilentSigningIn,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .testTag("signed_out_browser_sign_in_button"),
                        ) {
                            Text(stringResource(R.string.gothwad_sign_in_browser))
                        }
                    }
                }
            }
        }

        if (isPeerSilentSigningIn) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp)
                        .padding(horizontal = 8.dp),
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
        }

        if (discoveredAccounts.isNotEmpty()) {
            item {
                AccountSectionCard(
                    title = stringResource(R.string.gothwad_discovered_on_device),
                    subtitle = stringResource(R.string.gothwad_discovered_helper),
                    modifier = Modifier.widthIn(max = 600.dp),
                ) {
                    discoveredAccounts.forEachIndexed { index, peer ->
                        if (index > 0) {
                            HorizontalDivider(
                                color = semantic.cardBorder,
                                modifier = Modifier.padding(horizontal = 20.dp),
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .clickable(enabled = !isPeerSilentSigningIn, role = Role.Button) {
                                    onContinueAsPeer(peer)
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .testTag("discovered_peer_${peer.userId}"),
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
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onRefreshPeers,
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.gothwad_refresh_profile))
                }
                Row {
                    TextButton(onClick = onOpenPrivacy) { Text(stringResource(R.string.gothwad_privacy)) }
                    TextButton(onClick = onOpenTerms) { Text(stringResource(R.string.gothwad_terms)) }
                }
            }
        }
    }
}

@Composable
private fun SignedOutFeatureChip(
    icon: ImageVector,
    label: String,
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
