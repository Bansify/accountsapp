package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.AccountActivityEntry
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.GothwadQuotaSummary
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader

@Composable
fun DataPrivacyTabScreen(
    account: GothwadAccount,
    canManageNatively: Boolean = false,
    canWriteNatively: Boolean = canManageNatively,
    isExportingData: Boolean = false,
    activityFeed: List<AccountActivityEntry> = emptyList(),
    onExportDataClick: () -> Unit = {},
    onDeleteAccountClick: () -> Unit = {},
    onManageDataOnWeb: () -> Unit,
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("data_privacy_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_data),
                subtitle = "Download a copy of your account data, view shared storage usage, or permanently delete your account.",
            )
        }

        // Export & Delete
        item {
            AccountSectionCard(
                title = "Data from Gothwad Accounts",
                subtitle = if (canManageNatively && canWriteNatively) {
                    "Export a JSON snapshot or manage account deletion right inside this app."
                } else {
                    "Manage account export and deletion on accounts.gothwadtech.com."
                },
            ) {
                AccountSettingRow(
                    label = "Export your account data",
                    value = if (isExportingData) "Preparing JSON archive…" else "Download Accounts JSON snapshot",
                    supportingText = "Exports your profile, sessions, connected apps, and security log",
                    icon = Icons.Rounded.Download,
                    trailingIcon = if (canManageNatively) null else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively && !isExportingData) onExportDataClick else onManageDataOnWeb,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Delete your Gothwad Account",
                    value = "Permanently close this account",
                    supportingText = "Deleting your account removes core identity access, sessions, and app authorizations",
                    icon = Icons.Rounded.DeleteForever,
                    statusBadge = { StatusPill("Irreversible", StatusTone.CRITICAL) },
                    trailingIcon = if (canManageNatively && canWriteNatively) null else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively && canWriteNatively) onDeleteAccountClick else onManageDataOnWeb,
                )
            }
        }

        // Shared storage quota
        item {
            AccountSectionCard(
                title = "Storage across Drive & Mail",
                subtitle = "Quota breakdown and Drive access details",
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)) {
                    GothwadQuotaSummary(
                        context = context,
                        usedBytes = account.storageUsedBytes,
                        limitBytes = account.storageLimitBytes,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
                if (!canManageNatively) {
                    SectionDivider()
                    AccountSettingRow(
                        label = "Manage storage on Accounts web",
                        value = "Open storage & data summary",
                        trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = onManageDataOnWeb,
                    )
                }
            }
        }

        // Recent activity
        item {
            AccountSectionCard(title = "Recent account activity") {
                if (canManageNatively && activityFeed.isNotEmpty()) {
                    activityFeed.take(6).forEachIndexed { idx, entry ->
                        if (idx > 0) SectionDivider()
                        AccountSettingRow(
                            label = entry.title,
                            value = entry.detail ?: entry.type,
                            supportingText = listOfNotNull(entry.ipAddress, entry.createdAt).joinToString(" · "),
                            icon = Icons.Rounded.History,
                            trailingIcon = null,
                        )
                    }
                } else {
                    AccountSettingRow(
                        label = "Account activity log",
                        value = if (canManageNatively) "No recent account events" else "View login & account events on web",
                        icon = Icons.Rounded.History,
                        trailingIcon = if (canManageNatively) null else Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = if (canManageNatively) null else onManageDataOnWeb,
                    )
                }
            }
        }
    }
}
