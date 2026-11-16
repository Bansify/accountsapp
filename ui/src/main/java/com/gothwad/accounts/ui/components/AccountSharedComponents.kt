package com.gothwad.accounts.ui.components

import android.content.Context
import android.text.format.Formatter
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
fun GothwadBrandBadge(
    size: Dp = 28.dp,
    modifier: Modifier = Modifier,
) {
    val semantic = LocalAccountColors.current
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = (size.toPx() * 0.11f).coerceAtLeast(2.4.dp.toPx()), cap = StrokeCap.Round)
            drawArc(semantic.brandHighlight, startAngle = -140f, sweepAngle = 80f, useCenter = false, style = stroke)
            drawArc(semantic.brandTertiary, startAngle = -52f, sweepAngle = 80f, useCenter = false, style = stroke)
            drawArc(semantic.brandSecondary, startAngle = 36f, sweepAngle = 86f, useCenter = false, style = stroke)
            drawArc(semantic.brandPrimary, startAngle = 130f, sweepAngle = 82f, useCenter = false, style = stroke)
        }
        Text(
            text = "G",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = semantic.brandPrimary,
        )
    }
}

fun accountTabIcon(tab: AccountTab): ImageVector = when (tab) {
    AccountTab.HOME -> Icons.Rounded.Home
    AccountTab.PERSONAL_INFO -> Icons.Rounded.Person
    AccountTab.SECURITY -> Icons.Rounded.Lock
    AccountTab.DATA_PRIVACY -> Icons.Rounded.PrivacyTip
    AccountTab.PEOPLE_SHARING -> Icons.Rounded.Group
    AccountTab.CONNECTED_APPS -> Icons.Rounded.Apps
}

@Composable
fun AccountsTopAppBar(
    activeAccount: GothwadAccount?,
    canNavigateBack: Boolean,
    subScreenTitle: String?,
    isRefreshing: Boolean,
    showMenuButton: Boolean = false,
    onMenuClick: () -> Unit = {},
    onBackClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit = {},
    onHelpClick: () -> Unit,
    onAvatarClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val topBarInsets = WindowInsets.statusBars.union(
        WindowInsets.displayCutout.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
    )

    // Paints the exact #FFFFFF / #212121 canvas color behind the system status bar while keeping
    // all header controls cleanly below the battery/network/clock bar.
    Surface(
        color = semantic.canvasBackground,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.windowInsetsPadding(topBarInsets)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
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
                    if (showMenuButton) {
                        IconButton(
                            onClick = onMenuClick,
                            modifier = Modifier.testTag("top_menu_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Menu,
                                contentDescription = stringResource(R.string.gothwad_cd_menu),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = if (showMenuButton) 2.dp else 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (!showMenuButton) {
                            GothwadBrandBadge(size = 26.dp)
                        }
                        Text(
                            text = stringResource(R.string.gothwad_account_brand),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
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
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("top_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.gothwad_cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else if (!canNavigateBack) {
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.testTag("top_settings_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = stringResource(R.string.gothwad_cd_settings),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                }

                if (activeAccount != null) {
                    val accountLabel = activeAccount.displayName()
                    Box(
                        modifier = Modifier
                            .padding(end = 4.dp)
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
    val semantic = LocalAccountColors.current
    val tabs = AccountTab.entries
    val listState = rememberLazyListState()

    LaunchedEffect(selectedTab) {
        listState.animateScrollToItem(selectedTab.ordinal)
    }

    Surface(color = semantic.canvasBackground) {
        Column {
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items(tabs, key = { it.name }) { tab ->
                    val isSelected = tab == selectedTab
                    val containerColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                        label = "tab_bg_${tab.name}",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        label = "tab_fg_${tab.name}",
                    )
                    val icon = when (tab) {
                        AccountTab.HOME -> Icons.Rounded.Home
                        AccountTab.PERSONAL_INFO -> Icons.Rounded.Person
                        AccountTab.SECURITY -> Icons.Rounded.Lock
                        AccountTab.DATA_PRIVACY -> Icons.Rounded.PrivacyTip
                        AccountTab.PEOPLE_SHARING -> Icons.Rounded.Group
                        AccountTab.CONNECTED_APPS -> Icons.Rounded.Apps
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = containerColor,
                        border = if (isSelected) null else BorderStroke(1.dp, semantic.cardBorder),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(50))
                            .clickable(role = Role.Tab) { onSelectTab(tab) }
                            .testTag("tab_${tab.name.lowercase()}"),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Text(
                                text = stringResource(tab.titleRes),
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                ),
                                color = contentColor,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = semantic.cardBorder)
        }
    }
}

@Composable
fun AccountsBottomBar(
    selectedTab: AccountTab,
    isOthersSelected: Boolean,
    onSelectTab: (AccountTab) -> Unit,
    onSelectOthers: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column {
        HorizontalDivider(color = semantic.cardBorder)
        NavigationBar(
            containerColor = semantic.canvasBackground,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            NavigationBarItem(
                selected = !isOthersSelected && selectedTab == AccountTab.HOME,
                onClick = { onSelectTab(AccountTab.HOME) },
                icon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                label = { Text("Home", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                colors = itemColors,
                modifier = Modifier.testTag("bottom_tab_home"),
            )
            NavigationBarItem(
                selected = !isOthersSelected && selectedTab == AccountTab.PERSONAL_INFO,
                onClick = { onSelectTab(AccountTab.PERSONAL_INFO) },
                icon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                label = { Text("Personal", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                colors = itemColors,
                modifier = Modifier.testTag("bottom_tab_personal"),
            )
            NavigationBarItem(
                selected = !isOthersSelected && selectedTab == AccountTab.SECURITY,
                onClick = { onSelectTab(AccountTab.SECURITY) },
                icon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                label = { Text("Security", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                colors = itemColors,
                modifier = Modifier.testTag("bottom_tab_security"),
            )
            NavigationBarItem(
                selected = !isOthersSelected && selectedTab == AccountTab.DATA_PRIVACY,
                onClick = { onSelectTab(AccountTab.DATA_PRIVACY) },
                icon = { Icon(Icons.Rounded.PrivacyTip, contentDescription = null) },
                label = { Text("Privacy", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) },
                colors = itemColors,
                modifier = Modifier.testTag("bottom_tab_data"),
            )
            NavigationBarItem(
                selected = isOthersSelected,
                onClick = onSelectOthers,
                icon = { Icon(Icons.Rounded.GridView, contentDescription = null) },
                label = {
                    Text(
                        text = stringResource(R.string.gothwad_tab_others),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                colors = itemColors,
                modifier = Modifier.testTag("bottom_tab_others"),
            )
        }
    }
}

@Composable
fun AccountsSidebarContent(
    activeAccount: GothwadAccount?,
    selectedTab: AccountTab,
    isSettingsSelected: Boolean,
    isHelpSelected: Boolean,
    isRefreshing: Boolean,
    onSelectTab: (AccountTab) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHelp: () -> Unit,
    onRefreshClick: () -> Unit,
) {
    val semantic = LocalAccountColors.current
    val navInsets = WindowInsets.safeDrawing.only(
        WindowInsetsSides.Vertical + WindowInsetsSides.Start,
    )
    val itemColors = NavigationDrawerItemDefaults.colors(
        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
        unselectedContainerColor = semantic.canvasBackground,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
        unselectedTextColor = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .windowInsetsPadding(navInsets)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GothwadBrandBadge(size = 26.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.gothwad_account_brand),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (activeAccount != null) {
                IconButton(
                    onClick = onRefreshClick,
                    enabled = !isRefreshing,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.gothwad_refresh_profile),
                        tint = if (isRefreshing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        if (activeAccount != null) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AccountAvatar(
                        displayName = activeAccount.displayName(),
                        initials = activeAccount.initials(),
                        avatarUrl = activeAccount.avatarUrl,
                        size = 40.dp,
                        showBrandRing = true,
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = activeAccount.displayName(),
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = activeAccount.displayEmail(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }

        if (activeAccount != null) {
            AccountTab.entries.forEach { tab ->
                val selected = !isSettingsSelected && !isHelpSelected && selectedTab == tab
                NavigationDrawerItem(
                    label = { Text(stringResource(tab.titleRes), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = { Icon(accountTabIcon(tab), contentDescription = null) },
                    colors = itemColors,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.testTag("sidebar_tab_${tab.name.lowercase()}"),
                )
            }
            HorizontalDivider(
                color = semantic.cardBorder,
                modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp),
            )
        }

        NavigationDrawerItem(
            label = { Text(stringResource(R.string.gothwad_settings_title)) },
            selected = isSettingsSelected,
            onClick = onOpenSettings,
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
            colors = itemColors,
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("sidebar_settings_item"),
        )
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.gothwad_help_title)) },
            selected = isHelpSelected,
            onClick = onOpenHelp,
            icon = { Icon(Icons.AutoMirrored.Rounded.HelpOutline, contentDescription = null) },
            colors = itemColors,
            shape = RoundedCornerShape(50),
            modifier = Modifier.testTag("sidebar_help_item"),
        )
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
        color = MaterialTheme.colorScheme.surfaceContainerLow,
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
    val semantic = LocalAccountColors.current
    val knownUsed = usedBytes != null && usedBytes >= 0L
    val knownLimit = limitBytes != null && limitBytes > 0L
    val fraction = if (knownUsed && knownLimit) {
        (usedBytes!!.toFloat() / limitBytes!!.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }
    val percent = (fraction * 100f).toInt()
    val barColor = when {
        fraction >= 0.9f -> semantic.criticalRed
        fraction >= 0.75f -> semantic.warningAmber
        else -> MaterialTheme.colorScheme.primary
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (knownUsed && knownLimit) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${Formatter.formatShortFileSize(context, usedBytes!!)} of ${Formatter.formatShortFileSize(context, limitBytes!!)}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = barColor.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = "$percent% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            strokeCap = StrokeCap.Round,
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
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
