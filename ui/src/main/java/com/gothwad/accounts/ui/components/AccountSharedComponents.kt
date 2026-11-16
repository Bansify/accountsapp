package com.gothwad.accounts.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PauseCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.model.AccountTab
import com.gothwad.accounts.ui.model.DemoAccountProfile

@Composable
fun AccountsTopAppBar(
    activeAccount: DemoAccountProfile,
    canNavigateBack: Boolean,
    subScreenTitle: String?,
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onHelpClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
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
                        contentDescription = "Navigate back",
                    )
                }
                Text(
                    text = subScreenTitle ?: "Account",
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = semantic.googleBlue, fontWeight = FontWeight.Bold)) { append("G") }
                            withStyle(SpanStyle(color = semantic.googleRed, fontWeight = FontWeight.Bold)) { append("o") }
                            withStyle(SpanStyle(color = semantic.googleYellow, fontWeight = FontWeight.Bold)) { append("t") }
                            withStyle(SpanStyle(color = semantic.googleBlue, fontWeight = FontWeight.Bold)) { append("h") }
                            withStyle(SpanStyle(color = semantic.googleGreen, fontWeight = FontWeight.Bold)) { append("w") }
                            withStyle(SpanStyle(color = semantic.googleRed, fontWeight = FontWeight.Bold)) { append("a") }
                            withStyle(SpanStyle(color = semantic.googleBlue, fontWeight = FontWeight.Bold)) { append("d") }
                            append(" ")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium,
                                ),
                            ) {
                                append("Account")
                            }
                        },
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = onSearchClick,
                modifier = Modifier.testTag("top_search_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Account",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onHelpClick,
                modifier = Modifier.testTag("top_help_button"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                    contentDescription = "Help and support",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .minimumInteractiveComponentSize()
                    .clip(CircleShape)
                    .clickable(onClick = onAvatarClick)
                    .padding(4.dp)
                    .testTag("top_avatar_button"),
                contentAlignment = Alignment.Center,
            ) {
                AccountAvatar(
                    account = activeAccount,
                    size = 36.dp,
                    showGoogleRing = true,
                )
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
    Surface(
        color = MaterialTheme.colorScheme.surface,
    ) {
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
                                text = tab.title,
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
    account: DemoAccountProfile,
    size: Dp,
    showGoogleRing: Boolean = false,
    showEditCameraBadge: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val semantic = LocalAccountColors.current
    val outerSize = if (showGoogleRing) size + 8.dp else size
    Box(
        modifier = Modifier
            .size(outerSize)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (showGoogleRing) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = (size.toPx() * 0.075f).coerceIn(2.2.dp.toPx(), 4.dp.toPx())
                val stroke = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
                drawArc(semantic.googleRed, startAngle = -140f, sweepAngle = 82f, useCenter = false, style = stroke)
                drawArc(semantic.googleYellow, startAngle = -58f, sweepAngle = 82f, useCenter = false, style = stroke)
                drawArc(semantic.googleGreen, startAngle = 24f, sweepAngle = 92f, useCenter = false, style = stroke)
                drawArc(semantic.googleBlue, startAngle = 116f, sweepAngle = 104f, useCenter = false, style = stroke)
            }
        }
        if (account.useDemoAvatarArtwork) {
            Image(
                painter = painterResource(id = R.drawable.img_avatar_pawan),
                contentDescription = "${account.name} profile picture",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape),
            )
        } else {
            val containerColor = if (account.isWorkAccount) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val textColor = if (account.isWorkAccount) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = account.avatarInitials,
                    style = if (size >= 64.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (showEditCameraBadge) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.cardBorder),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.CameraAlt,
                        contentDescription = "Change profile photo",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(16.dp),
                    )
                }
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
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
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
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (supportingText != null) {
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
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
fun AccountToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    testTag: String? = null,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .clickable {
                if (onRowClick != null) onRowClick() else onCheckedChange(!checked)
            }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
            ),
        )
    }
}

@Composable
fun StatusPill(
    label: String,
    tone: StatusTone,
) {
    val semantic = LocalAccountColors.current
    val (bg, fg, icon) = when (tone) {
        StatusTone.VERIFIED -> Triple(semantic.verifiedContainer, semantic.onVerifiedContainer, Icons.Rounded.CheckCircle)
        StatusTone.PAUSED -> Triple(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurfaceVariant, Icons.Rounded.PauseCircle)
        StatusTone.WARNING -> Triple(semantic.warningContainer, semantic.onWarningContainer, Icons.Rounded.WarningAmber)
        StatusTone.CRITICAL -> Triple(semantic.criticalContainer, semantic.onCriticalContainer, Icons.Rounded.WarningAmber)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = fg,
            )
        }
    }
}

enum class StatusTone { VERIFIED, PAUSED, WARNING, CRITICAL }

@Composable
fun SegmentedStorageBar(
    driveGb: Float,
    mailGb: Float,
    photosGb: Float,
    totalGb: Float,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalAccountColors.current
    val safeTotal = totalGb.coerceAtLeast(1f)
    val driveFrac = (driveGb / safeTotal).coerceIn(0f, 1f)
    val mailFrac = (mailGb / safeTotal).coerceIn(0f, 1f - driveFrac)
    val photosFrac = (photosGb / safeTotal).coerceIn(0f, 1f - driveFrac - mailFrac)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(12.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (driveFrac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(driveFrac)
                        .background(semantic.driveBlue),
                )
            }
            if (mailFrac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(mailFrac / (1f - driveFrac).coerceAtLeast(0.01f))
                        .background(semantic.mailRed),
                )
            }
            if (photosFrac > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(photosFrac / (1f - driveFrac - mailFrac).coerceAtLeast(0.01f))
                        .background(semantic.photosAmber),
                )
            }
        }
    }
}

@Composable
fun StorageLegendChip(label: String, gb: Float, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = "$label (${String.format("%.1f", gb)} GB)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
