package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.GothwadEcosystemCatalog

@Composable
fun ConnectedAppsTabScreen(
    installedVerifiedPeers: List<String>,
    discoveredAccounts: List<DiscoveredAccount>,
    onManageAppsOnWeb: () -> Unit,
    onOpenCatalogUrl: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("connected_apps_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_apps),
                subtitle = "Core Gothwad services, installed device apps, and revocable OAuth permissions.",
            )
        }

        // OAuth permissions on Accounts web
        item {
            AccountSectionCard(
                title = "Authorized third-party & OAuth apps",
                subtitle = "View or revoke OAuth token grants issued to apps and websites.",
            ) {
                AccountSettingRow(
                    label = "OAuth grants & connected sites",
                    value = "Review and revoke app access on Accounts web",
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
            ) {
                GothwadEcosystemCatalog.coreServices.forEachIndexed { index, service ->
                    if (index > 0) SectionDivider()
                    AccountSettingRow(
                        label = if (service.inDevelopment) "Service · In development" else "Included service",
                        value = service.name,
                        supportingText = service.description,
                        icon = Icons.Rounded.Apps,
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
            ) {
                GothwadEcosystemCatalog.optionalProducts.forEachIndexed { index, product ->
                    if (index > 0) SectionDivider()
                    AccountSettingRow(
                        label = "Product",
                        value = product.name,
                        supportingText = product.description,
                        icon = Icons.Rounded.Extension,
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = { onOpenCatalogUrl(product.url) },
                    )
                }
            }
        }
    }
}
