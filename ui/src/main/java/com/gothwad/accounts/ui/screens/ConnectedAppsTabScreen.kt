package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.ConnectedAppGrant
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.GothwadEcosystemCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectedAppsTabScreen(
    installedVerifiedPeers: List<String>,
    discoveredAccounts: List<DiscoveredAccount>,
    canManageNatively: Boolean = false,
    authorizations: List<ConnectedAppGrant> = emptyList(),
    onRevokeAuthorization: (String) -> Unit = {},
    onManageAppsOnWeb: () -> Unit,
    onOpenCatalogUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("connected_apps_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_apps),
                subtitle = "Core Gothwad services, installed device apps, and revocable OAuth permissions.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // OAuth permissions (live from /mobile/authorizations when available + web link)
        item {
            AccountSectionCard(
                title = "Authorized apps & OAuth grants",
                subtitle = "Revoking an OAuth grant stops refresh tokens for that app across all devices.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                if (canManageNatively && authorizations.isNotEmpty()) {
                    authorizations.forEachIndexed { idx, grant ->
                        if (idx > 0) SectionDivider()
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = grant.name.take(1).uppercase(),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(grant.name, style = MaterialTheme.typography.titleMedium)
                                    grant.grantedAt?.let {
                                        Text(
                                            text = "Granted $it",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            if (grant.scopes.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(10.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    grant.scopes.forEach { scope ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        ) {
                                            Text(
                                                text = scope,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                OutlinedButton(
                                    onClick = { onRevokeAuthorization(grant.appId) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                    shape = RoundedCornerShape(50),
                                ) {
                                    Icon(Icons.Rounded.LinkOff, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Disconnect")
                                }
                            }
                        }
                    }
                    SectionDivider()
                }
                AccountSettingRow(
                    label = "OAuth grants & connected sites",
                    value = if (canManageNatively && authorizations.isEmpty()) {
                        "No active OAuth grants · Open on Accounts web"
                    } else {
                        "Review and revoke app access on Accounts web"
                    },
                    supportingText = "Revoking an OAuth grant stops refresh tokens across devices",
                    icon = Icons.Rounded.Security,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManageAppsOnWeb,
                )
            }
        }

        // Verified Gothwad apps detected on THIS device via PackageManager + signing cert policy
        item {
            AccountSectionCard(
                title = "Installed Gothwad apps on this device",
                subtitle = "Verified by signing certificate; each app stores its own scoped OAuth tokens.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                if (installedVerifiedPeers.isEmpty()) {
                    AccountSettingRow(
                        label = "Device discovery",
                        value = "No verified sibling apps installed",
                        supportingText = "${discoveredAccounts.size} discovered device account record(s)",
                        icon = Icons.Rounded.Smartphone,
                    )
                } else {
                    installedVerifiedPeers.forEachIndexed { idx, pkg ->
                        if (idx > 0) SectionDivider()
                        AccountSettingRow(
                            label = "Verified package",
                            value = pkg,
                            icon = Icons.Rounded.Smartphone,
                            statusBadge = { StatusPill("Verified", StatusTone.VERIFIED) },
                        )
                    }
                }
            }
        }

        // Core Gothwad services (exact list from accounts/web/js/gothwad-apps.js)
        item {
            AccountSectionCard(
                title = "Gothwad Services",
                subtitle = "Included services covered by your Gothwad Account",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                GothwadEcosystemCatalog.coreServices.forEachIndexed { index, service ->
                    if (index > 0) SectionDivider()
                    AccountSettingRow(
                        label = if (service.inDevelopment) "Service · In development" else "Included service",
                        value = service.name,
                        supportingText = service.description,
                        icon = iconForCatalogService(service.id),
                        statusBadge = {
                            if (service.inDevelopment) {
                                StatusPill("Soon", StatusTone.NEUTRAL)
                            } else {
                                StatusPill("Included", StatusTone.VERIFIED)
                            }
                        },
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { onOpenCatalogUrl(service.url) },
                    )
                }
            }
        }

        // Optional catalog products
        item {
            AccountSectionCard(
                title = "More from Gothwad",
                subtitle = "Other apps you can sign in to with your Gothwad Account",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                GothwadEcosystemCatalog.optionalProducts.forEachIndexed { index, product ->
                    if (index > 0) SectionDivider()
                    AccountSettingRow(
                        label = "Product",
                        value = product.name,
                        supportingText = product.description,
                        icon = iconForCatalogService(product.id),
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { onOpenCatalogUrl(product.url) },
                    )
                }
            }
        }
    }
}

private fun iconForCatalogService(id: String): ImageVector = when (id) {
    "gothwad-meet" -> Icons.Rounded.VideoCall
    "gothwad-mail" -> Icons.Rounded.Mail
    "kiwi-store" -> Icons.Rounded.Storefront
    "gothwad-drive" -> Icons.Rounded.Folder
    "gothwad-notes" -> Icons.Rounded.EditNote
    "gothwad-calendar" -> Icons.Rounded.CalendarMonth
    "gothwad-contacts" -> Icons.Rounded.Contacts
    "gothwad-tube" -> Icons.Rounded.PlayCircle
    "gothwad-chat" -> Icons.Rounded.Forum
    "gothwad-indogram" -> Icons.Rounded.PhotoLibrary
    else -> Icons.Rounded.Apps
}
