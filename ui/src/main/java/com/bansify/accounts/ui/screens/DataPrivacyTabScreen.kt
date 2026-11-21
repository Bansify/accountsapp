package com.bansify.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bansify.accounts.core.AccountActivityEntry
import com.bansify.accounts.core.GothwadAccount
import com.bansify.accounts.ui.LocalAccountColors
import com.bansify.accounts.ui.R
import com.bansify.accounts.ui.components.AccountSectionCard
import com.bansify.accounts.ui.components.AccountSettingRow
import com.bansify.accounts.ui.components.GothwadQuotaSummary
import com.bansify.accounts.ui.components.SectionDivider
import com.bansify.accounts.ui.components.StatusPill
import com.bansify.accounts.ui.components.StatusTone
import com.bansify.accounts.ui.components.TabIntroHeader

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
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("data_privacy_tab_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_data),
                subtitle = "Download a copy of your account data, view shared storage usage, or permanently delete your account.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Privacy Hero Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            StatusPill("Your data", StatusTone.VERIFIED)
                            Text(
                                text = "Data transparency & control",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Export your account snapshot or review account lifecycle actions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_privacy_checkup),
                            contentDescription = "Data and privacy illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = if (canManageNatively && !isExportingData) onExportDataClick else onManageDataOnWeb,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                if (canManageNatively) {
                                    if (isExportingData) "Preparing export…" else "Export account data JSON"
                                } else {
                                    stringResource(R.string.gothwad_open_on_web)
                                },
                            )
                            if (!canManageNatively) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }
            }
        }

        // Shared storage quota with illustration
        item {
            AccountSectionCard(
                title = "Storage across Drive & Mail",
                subtitle = "Quota breakdown and Drive access details",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        GothwadQuotaSummary(
                            context = context,
                            usedBytes = account.storageUsedBytes,
                            limitBytes = account.storageLimitBytes,
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Image(
                        painter = painterResource(id = R.drawable.img_cloud_storage),
                        contentDescription = "Cloud storage illustration",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp)),
                    )
                }
                if (!canManageNatively) {
                    Spacer(modifier = Modifier.height(4.dp))
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

        // Export & Recent activity
        item {
            AccountSectionCard(
                title = "Data from Bansify Accounts",
                subtitle = if (canManageNatively) {
                    "Export actions and recent account activity events."
                } else {
                    "Export actions and security event logs on accounts.bansify.com."
                },
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Export your account data",
                    value = if (isExportingData) "Preparing JSON archive…" else "Download Accounts JSON snapshot",
                    supportingText = "Exports your profile, sessions, connected apps, and security log",
                    icon = Icons.Rounded.Download,
                    trailingIcon = if (canManageNatively) null else Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = if (canManageNatively && !isExportingData) onExportDataClick else onManageDataOnWeb,
                )
                if (canManageNatively && activityFeed.isNotEmpty()) {
                    activityFeed.take(5).forEach { entry ->
                        SectionDivider()
                        AccountSettingRow(
                            label = entry.title,
                            value = entry.detail ?: entry.type,
                            supportingText = listOfNotNull(entry.ipAddress, entry.createdAt).joinToString(" · "),
                            icon = Icons.Rounded.History,
                            trailingIcon = null,
                        )
                    }
                } else {
                    SectionDivider()
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

        // Separated Danger Zone Card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column(modifier = Modifier.padding(vertical = 2.dp)) {
                    AccountSettingRow(
                        label = "Account deletion",
                        value = "Delete your Bansify Account",
                        supportingText = "Deleting your account removes core identity access, sessions, and app authorizations",
                        icon = Icons.Rounded.DeleteForever,
                        statusBadge = { StatusPill("Irreversible", StatusTone.CRITICAL) },
                        trailingIcon = if (canManageNatively && canWriteNatively) null else Icons.AutoMirrored.Rounded.OpenInNew,
                        onClick = if (canManageNatively && canWriteNatively) onDeleteAccountClick else onManageDataOnWeb,
                    )
                }
            }
        }
    }
}
