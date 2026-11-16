package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.TabletAndroid
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.SegmentedStorageBar
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.StorageLegendChip
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.DeviceType
import com.gothwad.accounts.ui.model.SearchableSetting

@Composable
fun SearchAccountScreen(
    query: String,
    searchIndex: List<SearchableSetting>,
    onQueryChange: (String) -> Unit,
    onSelectResult: (SearchableSetting) -> Unit,
) {
    val filtered = remember(query, searchIndex) {
        if (query.isBlank()) searchIndex
        else searchIndex.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true) ||
                it.category.title.contains(query, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search settings, devices, privacy...") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                shape = RoundedCornerShape(50),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp)
                    .testTag("search_account_input"),
            )
        }

        if (filtered.isEmpty()) {
            item {
                AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp),
                        )
                        Text("No matching settings found", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Try searching for 'password', '2-step', 'devices', or 'storage'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            item {
                AccountSectionCard(
                    title = if (query.isBlank()) "Popular settings" else "${filtered.size} result${if (filtered.size > 1) "s" else ""}",
                    modifier = Modifier.widthIn(max = 600.dp),
                ) {
                    filtered.forEachIndexed { index, item ->
                        AccountSettingRow(
                            label = item.category.title,
                            value = item.title,
                            supportingText = item.subtitle,
                            onClick = { onSelectResult(item) },
                        )
                        if (index < filtered.lastIndex) {
                            SectionDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceManagementScreen(
    state: AccountDashboardUiState,
    onSignOutDevice: (String) -> Unit,
    onShowMessage: (String) -> Unit,
) {
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Your devices",
                subtitle = "You're currently signed in to your Account on ${state.devices.size} devices.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        items(state.devices, key = { it.id }) { device ->
            val icon = when (device.deviceType) {
                DeviceType.PHONE -> Icons.Rounded.Smartphone
                DeviceType.TABLET -> Icons.Rounded.TabletAndroid
                DeviceType.LAPTOP, DeviceType.DESKTOP -> Icons.Rounded.Computer
            }
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(device.name, style = MaterialTheme.typography.titleMedium)
                                if (device.isCurrentDevice) {
                                    StatusPill("This device", StatusTone.VERIFIED)
                                }
                            }
                            Text(
                                "${device.platform} • ${device.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "${device.lastActive} • IP ${device.ipAddress}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton(onClick = { onShowMessage("Pinging ${device.name} via Find My Device...") }) {
                            Text("Find device")
                        }
                        if (!device.isCurrentDevice) {
                            OutlinedButton(
                                onClick = { onSignOutDevice(device.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("signout_${device.id}"),
                            ) {
                                Icon(Icons.Rounded.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign out")
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThirdPartyAppsScreen(
    state: AccountDashboardUiState,
    onRevokeApp: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Third-party apps & services",
                subtitle = "Manage apps and sites you've connected with Sign in with Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        if (state.thirdPartyApps.isEmpty()) {
            item {
                AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                    Text(
                        text = "No third-party apps currently have access to your Account.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp),
                    )
                }
            }
        } else {
            items(state.thirdPartyApps, key = { it.id }) { app ->
                AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Apps,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.appName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${app.developer} • Connected ${app.connectedSince}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            app.scopes.forEach { scope ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                ) {
                                    Text(
                                        text = scope,
                                        style = MaterialTheme.typography.labelSmall,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            OutlinedButton(
                                onClick = { onRevokeApp(app.id) },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.testTag("revoke_${app.id}"),
                            ) {
                                Text("Remove access")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PasswordManagerScreen(
    state: AccountDashboardUiState,
    onShowMessage: (String) -> Unit,
) {
    var revealedIds by remember { mutableStateOf(setOf<String>()) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Password Manager",
                subtitle = "${state.savedPasswords.size} saved passwords • 0 compromised or weak passwords found.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        items(state.savedPasswords, key = { it.id }) { entry ->
            val revealed = entry.id in revealedIds
            AccountSectionCard(modifier = Modifier.widthIn(max = 600.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.Password,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(entry.siteName, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${entry.domain} • ${entry.username}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (revealed) entry.passwordValue else "••••••••••••",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    IconButton(onClick = {
                        revealedIds = if (revealed) revealedIds - entry.id else revealedIds + entry.id
                    }) {
                        Icon(
                            if (revealed) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                            contentDescription = "Reveal password",
                        )
                    }
                    TextButton(onClick = { onShowMessage("Copied password for ${entry.siteName}") }) {
                        Text("Copy")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StorageDetailsScreen(
    state: AccountDashboardUiState,
    onShowMessage: (String) -> Unit,
) {
    val semantic = LocalAccountColors.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            TabIntroHeader(
                title = "Storage management",
                subtitle = "${String.format("%.1f", state.storageUsedGb)} GB of ${state.storageTotalGb.toInt()} GB used across your Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        item {
            AccountSectionCard(
                title = "Usage breakdown",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    SegmentedStorageBar(
                        driveGb = state.driveStorageGb,
                        mailGb = state.mailStorageGb,
                        photosGb = state.photosStorageGb,
                        totalGb = state.storageTotalGb,
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        StorageLegendChip("Drive", state.driveStorageGb, semantic.driveBlue)
                        StorageLegendChip("Mail", state.mailStorageGb, semantic.mailRed)
                        StorageLegendChip("Photos", state.photosStorageGb, semantic.photosAmber)
                    }
                }
            }
        }

        item {
            AccountSectionCard(
                title = "Free up space",
                subtitle = "Suggested items you can review and clean up.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Discarded items",
                    value = "Trash & Spam folders (1.4 GB)",
                    icon = Icons.Rounded.CleaningServices,
                    onClick = { onShowMessage("Cleaned 1.4 GB from Trash & Spam (Demo)") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Large files",
                    value = "Videos & Drive archives over 25 MB (6.8 GB)",
                    icon = Icons.Rounded.CleaningServices,
                    onClick = { onShowMessage("Reviewed 12 large files in Drive") },
                )
            }
        }
    }
}

@Composable
fun HelpSupportScreen(
    onOpenFeedback: () -> Unit,
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
                title = "Help & support",
                subtitle = "Get answers about managing your Account, recovery options, and privacy controls.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        item {
            AccountSectionCard(
                title = "Popular help topics",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Recovery",
                    value = "How to recover a lost password or 2-Step device",
                    icon = Icons.Rounded.SupportAgent,
                    onClick = { onShowMessage("Ensure both recovery email and phone remain verified.") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Security",
                    value = "Why passkeys are safer than SMS codes",
                    icon = Icons.Rounded.SupportAgent,
                    onClick = { onShowMessage("Passkeys are phishing-resistant and tied to your device.") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Feedback",
                    value = "Send feedback to the Accounts team",
                    icon = Icons.Rounded.Feedback,
                    onClick = onOpenFeedback,
                )
            }
        }
    }
}
