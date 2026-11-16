package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.components.displayEmail
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.GothwadEcosystemCatalog

@Composable
fun PeopleSharingTabScreen(
    account: GothwadAccount,
    onManagePeopleOnWeb: () -> Unit,
    onNavigateTab: (AccountTab) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("people_sharing_tab_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = stringResource(R.string.gothwad_tab_people),
                subtitle = "Contacts and product sharing status for your Gothwad Account.",
            )
        }

        // Honest Coming Soon Contacts card matching accounts/web/me#people
        item {
            AccountSectionCard(
                title = "Gothwad Contacts",
                subtitle = "Centralized people and address book across Gothwad products.",
            ) {
                AccountSettingRow(
                    label = "Contacts directory",
                    value = "Gothwad Contacts",
                    supportingText = "Currently in development — contact lists are not yet stored on Accounts.",
                    icon = Icons.Rounded.Contacts,
                    statusBadge = { StatusPill("Coming soon", StatusTone.NEUTRAL) },
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onManagePeopleOnWeb,
                )
            }
        }

        // Sharing summary card
        item {
            AccountSectionCard(
                title = "Sharing summary",
                subtitle = "Identity shared when you use connected Gothwad services",
            ) {
                AccountSettingRow(
                    label = "Account identifier",
                    value = account.displayEmail(),
                    icon = Icons.Rounded.AlternateEmail,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Included Gothwad services",
                    value = "${GothwadEcosystemCatalog.coreServices.size} core services in Gothwad catalog",
                    supportingText = "View services and revocable OAuth app permissions",
                    icon = Icons.Rounded.Apps,
                    onClick = { onNavigateTab(AccountTab.CONNECTED_APPS) },
                )
            }
        }
    }
}
