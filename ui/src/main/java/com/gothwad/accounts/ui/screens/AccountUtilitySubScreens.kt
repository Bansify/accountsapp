package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.SearchableSetting

@Composable
fun SearchAccountScreen(
    query: String,
    items: List<SearchableSetting>,
    onQueryChange: (String) -> Unit,
    onSelectItem: (SearchableSetting) -> Unit,
) {
    val semantic = LocalAccountColors.current
    var selectedCategory by remember { mutableStateOf<AccountTab?>(null) }
    val normalized = query.trim().lowercase()

    val filtered = remember(normalized, selectedCategory, items) {
        items.filter { item ->
            val matchesCategory = selectedCategory == null || item.category == selectedCategory
            val matchesQuery = normalized.isEmpty() ||
                item.title.lowercase().contains(normalized) ||
                item.subtitle.lowercase().contains(normalized) ||
                item.keywords.lowercase().contains(normalized)
            matchesCategory && matchesQuery
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_account_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.gothwad_search_hint), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(19.dp)) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.gothwad_cd_close))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = semantic.cardBorder,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("search_input_field"),
            )
        }

        // Category quick filter chips
        item {
            val chipTabs = listOf(
                null to "All",
                AccountTab.PERSONAL_INFO to stringResource(R.string.gothwad_tab_personal),
                AccountTab.SECURITY to stringResource(R.string.gothwad_tab_security),
                AccountTab.DATA_PRIVACY to stringResource(R.string.gothwad_tab_data),
                AccountTab.CONNECTED_APPS to stringResource(R.string.gothwad_tab_apps),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chipTabs.forEach { (tab, label) ->
                    FilterChip(
                        selected = selectedCategory == tab,
                        onClick = { selectedCategory = tab },
                        label = { Text(label) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp),
                        )
                        Text(
                            text = stringResource(R.string.gothwad_search_empty),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Try searching for password, passkeys, export, or email.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            item {
                AccountSectionCard(
                    title = if (normalized.isEmpty()) "All account settings" else "Matching settings (${filtered.size})",
                    modifier = Modifier.widthIn(max = 600.dp),
                ) {
                    filtered.forEachIndexed { idx, entry ->
                        if (idx > 0) SectionDivider()
                        AccountSettingRow(
                            label = entry.subtitle,
                            value = entry.title,
                            trailingIcon = if (entry.webSection != null) Icons.AutoMirrored.Rounded.OpenInNew else null,
                            testTag = "search_result_${entry.id}",
                            onClick = { onSelectItem(entry) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HelpLegalScreen(
    packageName: String,
    installedPeersCount: Int,
    onOpenPrivacy: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenAccountsHelpWeb: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("help_legal_screen"),
        contentPadding = PaddingValues(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            AccountSectionCard(
                title = stringResource(R.string.gothwad_help_title),
                subtitle = "Privacy, terms, and account management documentation",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = stringResource(R.string.gothwad_privacy),
                    value = "Gothwad Privacy Policy",
                    supportingText = "accounts.gothwadtech.com/privacy",
                    icon = Icons.Rounded.PrivacyTip,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onOpenPrivacy,
                )
                SectionDivider()
                AccountSettingRow(
                    label = stringResource(R.string.gothwad_terms),
                    value = "Gothwad Terms of Service",
                    supportingText = "accounts.gothwadtech.com/terms",
                    icon = Icons.Rounded.Gavel,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onOpenTerms,
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Account management",
                    value = "Open Gothwad Accounts on web",
                    icon = Icons.Rounded.Description,
                    trailingIcon = Icons.AutoMirrored.Rounded.OpenInNew,
                    onClick = onOpenAccountsHelpWeb,
                )
            }
        }

        item {
            AccountSectionCard(
                title = stringResource(R.string.gothwad_about),
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Host application",
                    value = packageName,
                    supportingText = "Verified sibling peer apps detected: $installedPeersCount · Hardware-backed AES-GCM Tink storage",
                    icon = Icons.Rounded.Info,
                )
            }
        }
    }
}
