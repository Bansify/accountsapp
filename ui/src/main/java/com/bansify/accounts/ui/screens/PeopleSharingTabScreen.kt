package com.bansify.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bansify.accounts.core.GothwadAccount
import com.bansify.accounts.ui.LocalAccountColors
import com.bansify.accounts.ui.R
import com.bansify.accounts.ui.components.AccountAvatar
import com.bansify.accounts.ui.components.AccountSectionCard
import com.bansify.accounts.ui.components.AccountSettingRow
import com.bansify.accounts.ui.components.SectionDivider
import com.bansify.accounts.ui.components.StatusPill
import com.bansify.accounts.ui.components.StatusTone
import com.bansify.accounts.ui.components.TabIntroHeader
import com.bansify.accounts.ui.components.displayEmail
import com.bansify.accounts.ui.components.displayName
import com.bansify.accounts.ui.components.initials
import com.bansify.accounts.ui.model.AccountTab
import com.bansify.accounts.ui.model.GothwadEcosystemCatalog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PeopleSharingTabScreen(
    account: GothwadAccount,
    onManagePeopleOnWeb: () -> Unit,
    onNavigateTab: (AccountTab) -> Unit,
) {
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("people_sharing_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_people),
                subtitle = "Contacts and product sharing status for your Bansify Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Honest Coming Soon Contacts card matching accounts/web/me#people
        item {
            AccountSectionCard(
                title = "Bansify Contacts",
                subtitle = "Centralized people and address book across Bansify products.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Contacts directory",
                    value = "Bansify Contacts",
                    supportingText = "Currently in development — contact lists are not yet stored on Accounts.",
                    icon = Icons.Rounded.Contacts,
                    statusBadge = { StatusPill("Coming soon", StatusTone.NEUTRAL) },
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePeopleOnWeb,
                )
            }
        }

        // Shared identity card
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "How others see you",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AccountAvatar(
                            displayName = account.displayName(),
                            initials = account.initials(),
                            avatarUrl = account.avatarUrl,
                            size = 40.dp,
                            showBrandRing = true,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(account.displayName(), style = MaterialTheme.typography.titleSmall)
                            Text(
                                account.displayEmail(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        GothwadEcosystemCatalog.coreServices.filterNot { it.inDevelopment }.forEach { svc ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            ) {
                                Text(
                                    text = svc.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sharing summary card
        item {
            AccountSectionCard(
                title = "Sharing summary",
                subtitle = "Identity shared when you use connected Bansify services",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Account identifier",
                    value = account.displayEmail(),
                    icon = Icons.Rounded.AlternateEmail,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Included Bansify services",
                    value = "${GothwadEcosystemCatalog.coreServices.size} core services in Bansify catalog",
                    supportingText = "View services and revocable OAuth app permissions",
                    icon = Icons.Rounded.Apps,
                    onClick = { onNavigateTab(AccountTab.CONNECTED_APPS) },
                )
            }
        }
    }
}
