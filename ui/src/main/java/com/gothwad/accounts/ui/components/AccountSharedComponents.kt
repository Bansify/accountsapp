package com.gothwad.accounts.ui.components

import android.content.Context
import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.AuthFailure
import com.gothwad.accounts.core.DiscoveredAccount
import com.gothwad.accounts.core.GothwadAccount
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.model.AccountTab

fun GothwadAccount.displayName(): String =
    name?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: userId

fun GothwadAccount.displayEmail(): String =
    email?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }?.let { "$it@gothwadtech.com" }
        ?: userId

fun GothwadAccount.initials(): String {
    val parts = displayName().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

fun DiscoveredAccount.displayName(): String =
    name?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }
        ?: userId

fun DiscoveredAccount.displayEmail(): String =
    email?.takeIf { it.isNotBlank() }
        ?: username?.takeIf { it.isNotBlank() }?.let { "$it@gothwadtech.com" }
        ?: userId

fun DiscoveredAccount.initials(): String {
    val parts = displayName().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> "${parts[0].first()}${parts[1].first()}".uppercase()
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "?"
    }
}

@Composable
fun AccountsTopAppBar(
    activeAccount: GothwadAccount?,
    canNavigateBack: Boolean,
    subScreenTitle: String?,
    isRefreshing: Boolean,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canNavigateBack) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("top_back_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.gothwad_cd_back),
                        )
                    }
                    Text(
                        text = subScreenTitle ?: stringResource(R.string.gothwad_account_brand),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                            .semantics { heading() },
                    )
                } else {
                    Text(
                        text = stringResource(R.string.gothwad_account_brand),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                            .semantics { heading() },
                    )
                }

                if (activeAccount != null && !canNavigateBack) {
                    IconButton(
                        onClick = onSearchClick,
                        modifier = Modifier.testTag("top_search_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.gothwad_cd_search),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = onRefreshClick,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("top_refresh_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.gothwad_refresh_profile),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = onHelpClick,
                    modifier = Modifier.testTag("top_help_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = stringResource(R.string.gothwad_cd_help),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (activeAccount != null) {
                    val accountLabel = activeAccount.displayName()
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = stringResource(R.string.gothwad_cd_avatar, accountLabel),
                                onClick = onAvatarClick,
                            )
                            .padding(4.dp)
                            .testTag("top_avatar_button"),
                        contentAlignment = Alignment.Center,
                    ) {
                        AccountAvatar(
                            displayName = accountLabel,
                            initials = activeAccount.initials(),
                            avatarUrl = activeAccount.avatarUrl,
                            size = 34.dp,
                            showBrandRing = true,
                        )
                    }
                }
            }
            if (isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp))
            }
        }
    }
}

@Composable
fun AccountsTabBar(
    selectedTab: AccountTab,
    onSelectTab: (AccountTab) -> Unit,
) {
    val tabs = AccountTab.entries
    val selectedIndex = tabs.indexOf(selectedTab).coerceAtLeast(0)
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                edgePadding = 16.dp,
                divider = {},
                indicator = {
                    TabRowDefaults.PrimaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(selectedIndex, matchContentSize = true),
                        width = 36.dp,
                        height = 3.dp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab == selectedTab
                    Tab(
                        selected = isSelected,
                        onClick = { onSelectTab(tab) },
                        modifier = Modifier.testTag("tab_${tab.name.lowercase()}"),
                        text = {
                            Text(
                                text = stringResource(tab.titleRes),
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                            )
                        },
                    )
                }
            }
            HorizontalDivider(color = LocalAccountColors.current.cardBorder)
        }
    }
}

@Composable
fun AccountAvatar(
    displayName: String,
    initials: String,
    avatarUrl: String?,
    size: Dp,
    showBrandRing: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val semantic = LocalAccountColors.current
    val bitmap = rememberAvatarBitmap(avatarUrl)
    val outerSize = if (showBrandRing) size + 8.dp else size

    Box(
        modifier = Modifier
            .size(outerSize)
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (showBrandRing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = (size.toPx() * 0.075f).coerceIn(2.dp.toPx(), 4.dp.toPx())
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                drawArc(semantic.brandHighlight, startAngle = -140f, sweepAngle = 82f, useCenter = false, style = stroke)
                drawArc(semantic.brandTertiary, startAngle = -58f, sweepAngle = 82f, useCenter = false, style = stroke)
                drawArc(semantic.brandSecondary, startAngle = 24f, sweepAngle = 92f, useCenter = false, style = stroke)
                drawArc(semantic.brandPrimary, startAngle = 116f, sweepAngle = 104f, useCenter = false, style = stroke)
            }
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = displayName,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initials,
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun AccountSectionCard(
    title: String? = null,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val semantic = LocalAccountColors.current
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, semantic.cardBorder),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            if (title != null || subtitle != null) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (title != null) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun SectionDivider() {
    HorizontalDivider(
        color = LocalAccountColors.current.cardBorder,
        modifier = Modifier.padding(horizontal = 20.dp),
    )
}

@Composable
fun AccountSettingRow(
    label: String,
    value: String,
    icon: ImageVector? = null,
    supportingText: String? = null,
    statusBadge: (@Composable () -> Unit)? = null,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
    testTag: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supportingText != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (statusBadge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            statusBadge()
        }
        if (trailingIcon != null && onClick != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

enum class StatusTone { VERIFIED, WARNING, CRITICAL, NEUTRAL }

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
) {
    val semantic = LocalAccountColors.current
    val (container, content, icon) = when (tone) {
        StatusTone.VERIFIED -> Triple(semantic.verifiedContainer, semantic.onVerifiedContainer, Icons.Rounded.CheckCircle)
        StatusTone.WARNING -> Triple(semantic.warningContainer, semantic.onWarningContainer, Icons.Rounded.WarningAmber)
        StatusTone.CRITICAL -> Triple(semantic.criticalContainer, semantic.onCriticalContainer, Icons.Rounded.ErrorOutline)
        StatusTone.NEUTRAL -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.Info)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = container,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = content,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun TabIntroHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun GothwadQuotaSummary(
    context: Context,
    usedBytes: Long?,
    limitBytes: Long?,
) {
    val knownUsed = usedBytes != null && usedBytes >= 0L
    val knownLimit = limitBytes != null && limitBytes > 0L
    val fraction = if (knownUsed && knownLimit) {
        (usedBytes!!.toFloat() / limitBytes!!.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (knownUsed) "${Formatter.formatShortFileSize(context, usedBytes!!)} used" else "Usage not synced",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = if (knownLimit) "${Formatter.formatShortFileSize(context, limitBytes!!)} total" else "View on Accounts web",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun GothwadAuthStatusBanner(
    failure: AuthFailure,
    onRetry: (() -> Unit)? = null,
    onSignInAgain: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalAccountColors.current
    val (messageRes, showSignInAgain, isCritical) = when (failure) {
        AuthFailure.BrowserUnavailable -> Triple(R.string.gothwad_browser_error, false, true)
        AuthFailure.InvalidCallback -> Triple(R.string.gothwad_callback_error, true, true)
        AuthFailure.LoginCancelled -> Triple(R.string.gothwad_cancelled, false, false)
        AuthFailure.SessionExpired -> Triple(R.string.gothwad_expired, true, true)
        AuthFailure.StorageUnavailable -> Triple(R.string.gothwad_storage_error, false, true)
        AuthFailure.Unavailable -> Triple(R.string.gothwad_unavailable, false, false)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isCritical) semantic.criticalContainer else semantic.warningContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        val textColor = if (isCritical) semantic.onCriticalContainer else semantic.onWarningContainer
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = if (isCritical) Icons.Rounded.ErrorOutline else Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = textColor,
                )
                Text(
                    text = stringResource(messageRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = textColor),
                ) {
                    Text(stringResource(R.string.gothwad_dismiss))
                }
                if (failure == AuthFailure.Unavailable && onRetry != null) {
                    TextButton(
                        onClick = onRetry,
                        colors = ButtonDefaults.textButtonColors(contentColor = textColor),
                    ) {
                        Text(stringResource(R.string.gothwad_retry))
                    }
                }
                if (showSignInAgain && onSignInAgain != null) {
                    TextButton(
                        onClick = onSignInAgain,
                        colors = ButtonDefaults.textButtonColors(contentColor = textColor),
                    ) {
                        Text(stringResource(R.string.gothwad_sign_in_again))
                    }
                }
            }
        }
    }
}
