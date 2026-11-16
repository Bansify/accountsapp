package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.model.SearchableSetting

@Composable
fun SearchAccountScreen(
    query: String,
    items: List<SearchableSetting>,
    onQueryChange: (String) -> Unit,
    onSelectItem: (SearchableSetting) -> Unit,
) {
    val semantic = LocalAccountColors.current
    val normalized = query.trim().lowercase()
    val filtered = if (normalized.isEmpty()) {
        items
    } else {
        items.filter { item ->
            item.title.lowercase().contains(normalized) ||
                item.subtitle.lowercase().contains(normalized) ||
                item.keywords.lowercase().contains(normalized)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("search_account_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(stringResource(R.string.gothwad_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.gothwad_cd_close))
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input_field"),
            )
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, semantic.cardBorder),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.gothwad_search_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        } else {
            item {
                AccountSectionCard(title = if (normalized.isEmpty()) "All account settings" else "Matching settings") {
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
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AccountSectionCard(title = stringResource(R.string.gothwad_help_title)) {
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
            AccountSectionCard(title = stringResource(R.string.gothwad_about)) {
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
