package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountAvatar
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.GothwadQuotaSummary
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.displayEmail
import com.gothwad.accounts.ui.components.displayName
import com.gothwad.accounts.ui.components.initials
import com.gothwad.accounts.ui.model.AccountTab

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeTabScreen(
    account: GothwadAccount,
    localAccountCount: Int,
    onOpenAvatarMenu: () -> Unit,
    onCopyEmail: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onNavigateTab: (AccountTab) -> Unit,
    onManageOnWeb: (String?) -> Unit,
) {
    val context = LocalContext.current
    val semantic = LocalAccountColors.current
    val displayEmail = account.displayEmail()
    val headerGlow = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.26f),
            semantic.canvasBackground,
        ),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("home_tab_screen"),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Atmospheric Welcome Hero
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerGlow)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 600.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    AccountAvatar(
                        displayName = account.displayName(),
                        initials = account.initials(),
                        avatarUrl = account.avatarUrl,
                        size = 72.dp,
                        showBrandRing = true,
                        onClick = onOpenAvatarMenu,
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Welcome, ${account.displayName()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier
                            .clickable(role = Role.Button) { onCopyEmail(displayEmail) }
                            .testTag("home_copy_email_chip"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = displayEmail,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(R.string.gothwad_cd_copy_email),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp),
                            )
                        }
                    }

                    // Live account security status pills when fields are available
                    val hasAnyStatus = account.mfaEnabled != null || account.hasPassword != null || account.passkeyCount != null
                    if (hasAnyStatus) {
                        Spacer(modifier = Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            account.mfaEnabled?.let { mfa ->
                                StatusPill(
                                    label = if (mfa) "2-Step On" else "2-Step Off",
                                    tone = if (mfa) StatusTone.VERIFIED else StatusTone.WARNING,
                                )
                            }
                            account.passkeyCount?.takeIf { it > 0 }?.let { count ->
                                StatusPill(
                                    label = "$count Passkey${if (count > 1) "s" else ""}",
                                    tone = StatusTone.VERIFIED,
                                )
                            }
                            account.hasPassword?.let { hasPwd ->
                                StatusPill(
                                    label = if (hasPwd) "Password set" else "No password",
                                    tone = if (hasPwd) StatusTone.NEUTRAL else StatusTone.WARNING,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Search pill
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, semantic.cardBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable(role = Role.Button, onClick = onOpenSearch)
                            .testTag("home_search_bar_pill"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.gothwad_search_hint),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }

        // Security checkup hero card with illustration
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
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
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            StatusPill(
                                label = if (account.mfaEnabled == true) "Account protected" else "Security overview",
                                tone = if (account.mfaEnabled == true) StatusTone.VERIFIED else StatusTone.NEUTRAL,
                            )
                            Text(
                                text = "Keep your account protected",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Review password, 2-Step Verification, passkeys, recovery email, and active sessions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_security_shield),
                            contentDescription = "Account security shield",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Button(
                            onClick = { onNavigateTab(AccountTab.SECURITY) },
                            shape = RoundedCornerShape(50),
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Review security", style = MaterialTheme.typography.labelLarge)
                        }
                        OutlinedButton(
                            onClick = { onManageOnWeb("security") },
                            shape = RoundedCornerShape(50),
                        ) {
                            Text(stringResource(R.string.gothwad_open_on_web), style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Quick links grid (4 real sections matching accounts/web/me)
        item {
            Column(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomeQuickLinkTile(
                        title = stringResource(R.string.gothwad_tab_personal),
                        subtitle = "Photo, name & contact",
                        icon = Icons.Rounded.Person,
                        accent = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AccountTab.PERSONAL_INFO) },
                    )
                    HomeQuickLinkTile(
                        title = stringResource(R.string.gothwad_tab_security),
                        subtitle = "2FA, passkeys & sessions",
                        icon = Icons.Rounded.Lock,
                        accent = semantic.verifiedGreen,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AccountTab.SECURITY) },
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HomeQuickLinkTile(
                        title = stringResource(R.string.gothwad_tab_data),
                        subtitle = "Export, storage & privacy",
                        icon = Icons.Rounded.PrivacyTip,
                        accent = semantic.warningAmber,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AccountTab.DATA_PRIVACY) },
                    )
                    HomeQuickLinkTile(
                        title = stringResource(R.string.gothwad_tab_apps),
                        subtitle = "Services & OAuth access",
                        icon = Icons.Rounded.Apps,
                        accent = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigateTab(AccountTab.CONNECTED_APPS) },
                    )
                }
            }
        }

        // Privacy & personalization card with illustration
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
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
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Data & privacy controls",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Export your Bansify Account snapshot, review activity, or manage shared Drive & Mail quota.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_privacy_checkup),
                            contentDescription = "Privacy controls illustration",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = { onNavigateTab(AccountTab.DATA_PRIVACY) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        Text("Manage data & privacy", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Drive & Mail shared storage quota with illustration
        item {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .padding(horizontal = 12.dp)
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
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "Drive & Mail storage",
                                style = MaterialTheme.typography.titleMedium,
                            )
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
                                .size(60.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = { onManageOnWeb("data") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.gothwad_manage_web))
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // Account details (only verified fields from /oauth/userinfo)
        item {
            AccountSectionCard(
                title = "Account details",
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Account email",
                    value = displayEmail,
                    icon = Icons.Rounded.Badge,
                )
                account.username?.takeIf { it.isNotBlank() }?.let { handle ->
                    SectionDivider()
                    AccountSettingRow(
                        label = "Username",
                        value = "@$handle",
                        icon = Icons.Rounded.Info,
                    )
                }
                SectionDivider()
                AccountSettingRow(
                    label = "User ID",
                    value = account.userId,
                    icon = Icons.Rounded.CloudQueue,
                    supportingText = "Signed-in accounts in this app: $localAccountCount",
                )
                account.createdAt?.takeIf { it.isNotBlank() }?.let { created ->
                    SectionDivider()
                    AccountSettingRow(
                        label = "Account created",
                        value = created,
                    )
                }
                account.updatedAt?.takeIf { it.isNotBlank() }?.let { updated ->
                    SectionDivider()
                    AccountSettingRow(
                        label = "Profile last updated",
                        value = updated,
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeQuickLinkTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
