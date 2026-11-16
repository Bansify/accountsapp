package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Diversity3
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Recommend
import androidx.compose.material.icons.rounded.ShareLocation
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.AccountToggleRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.AccountDashboardUiState

@Composable
fun PeopleSharingTabScreen(
    state: AccountDashboardUiState,
    onToggleLocationSharing: (Boolean) -> Unit,
    onToggleSharedEndorsements: (Boolean) -> Unit,
    onUnblockUser: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "People & sharing",
                subtitle = "People you interact with and the info you make visible across services.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Contacts
        item {
            AccountSectionCard(
                title = "Contacts",
                subtitle = "Manage people you connect with across Mail, Calendar, and Meet.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Synced contacts",
                    value = "${state.contactsCount} contacts",
                    supportingText = "Automatically backed up to ${state.activeAccount.email}",
                    icon = Icons.Rounded.Contacts,
                    onClick = { onShowMessage("${state.contactsCount} contacts synced across your devices") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Contact info from interactions",
                    value = "Saved automatically when you reply or collaborate",
                    icon = Icons.Rounded.Group,
                    onClick = { onShowMessage("Interaction contact saving is enabled") },
                )
            }
        }

        // Family group
        item {
            AccountSectionCard(
                title = "Your family on Accounts",
                subtitle = "Share storage, subscriptions, and family calendar with up to 5 people.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                state.familyMembers.forEachIndexed { index, member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = member.name.take(1).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                member.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusPill(
                            label = member.role,
                            tone = if (member.role == "Family manager") StatusTone.VERIFIED else StatusTone.PAUSED,
                        )
                    }
                    if (index < state.familyMembers.lastIndex) {
                        SectionDivider()
                    }
                }
            }
        }

        // Blocked users
        item {
            AccountSectionCard(
                title = "Blocked",
                subtitle = "Users you've blocked across Mail, Drive, and Chat.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                if (state.blockedUsers.isEmpty()) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Rounded.Block,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "You haven't blocked any users.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    state.blockedUsers.forEachIndexed { index, blockedEmail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Rounded.Block,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = blockedEmail,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onUnblockUser(blockedEmail) }) {
                                Text("Unblock")
                            }
                        }
                        if (index < state.blockedUsers.lastIndex) {
                            SectionDivider()
                        }
                    }
                }
            }
        }

        // Location & Business & Shared endorsements
        item {
            AccountSectionCard(
                title = "Sharing preferences",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountToggleRow(
                    title = "Location sharing",
                    subtitle = "Choose who can see your real-time location in Maps",
                    icon = Icons.Rounded.ShareLocation,
                    checked = state.locationSharingEnabled,
                    onCheckedChange = onToggleLocationSharing,
                )
                SectionDivider()
                AccountToggleRow(
                    title = "Shared endorsements",
                    subtitle = "Show your reviews and recommendations next to ads for friends",
                    icon = Icons.Rounded.Recommend,
                    checked = state.sharedEndorsementsEnabled,
                    onCheckedChange = onToggleSharedEndorsements,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Business personalization",
                    value = "Turned off for personal Account",
                    icon = Icons.Rounded.Storefront,
                    onClick = { onShowMessage("Switch to ${state.accounts.firstOrNull { it.isWorkAccount }?.email ?: "a work account"} for Business features") },
                )
            }
        }
    }
}
