package com.gothwad.accounts.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AddCard
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.FlightTakeoff
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.ui.LocalAccountColors
import com.gothwad.accounts.ui.R
import com.gothwad.accounts.ui.components.AccountSectionCard
import com.gothwad.accounts.ui.components.AccountSettingRow
import com.gothwad.accounts.ui.components.SectionDivider
import com.gothwad.accounts.ui.components.SegmentedStorageBar
import com.gothwad.accounts.ui.components.StatusPill
import com.gothwad.accounts.ui.components.StatusTone
import com.gothwad.accounts.ui.components.StorageLegendChip
import com.gothwad.accounts.ui.components.TabIntroHeader
import com.gothwad.accounts.ui.model.AccountDashboardUiState
import com.gothwad.accounts.ui.model.AccountSubScreen

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaymentsSubscriptionsTabScreen(
    state: AccountDashboardUiState,
    onOpenSubScreen: (AccountSubScreen) -> Unit,
    onOpenAddPayment: () -> Unit,
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
                title = "Payments & subscriptions",
                subtitle = "Your payment info, transactions, recurring subscriptions, and cloud storage.",
                modifier = Modifier.widthIn(max = 600.dp),
            )
        }

        // Account Storage card
        item {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, semantic.cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 600.dp),
            ) {
                Column {
                    Row(
                        modifier = Modifier.padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text("Account storage", style = MaterialTheme.typography.titleLarge)
                            Text(
                                "${String.format("%.1f", state.storageUsedGb)} GB of ${state.storageTotalGb.toInt()} GB used",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            SegmentedStorageBar(
                                driveGb = state.driveStorageGb,
                                mailGb = state.mailStorageGb,
                                photosGb = state.photosStorageGb,
                                totalGb = state.storageTotalGb,
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Image(
                            painter = painterResource(id = R.drawable.img_cloud_storage),
                            contentDescription = "Cloud storage artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(16.dp)),
                        )
                    }
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StorageLegendChip("Drive", state.driveStorageGb, semantic.driveBlue)
                        StorageLegendChip("Mail", state.mailStorageGb, semantic.mailRed)
                        StorageLegendChip("Photos", state.photosStorageGb, semantic.photosAmber)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = semantic.cardBorder)
                    TextButton(
                        onClick = { onOpenSubScreen(AccountSubScreen.StorageDetails) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    ) {
                        Text("Clean up & manage storage", modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }

        // Payment methods
        item {
            AccountSectionCard(
                title = "Payment methods",
                subtitle = "Saved cards and UPI handles for Store, Play, and subscriptions.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                state.paymentMethods.forEachIndexed { index, method ->
                    AccountSettingRow(
                        label = method.type,
                        value = method.label,
                        supportingText = method.detail,
                        icon = if (method.type == "UPI") Icons.Rounded.AccountBalanceWallet else Icons.Rounded.CreditCard,
                        statusBadge = if (method.isPrimary) {
                            { StatusPill("Primary", StatusTone.VERIFIED) }
                        } else null,
                        onClick = { onShowMessage("${method.label} is verified for payments") },
                    )
                    if (index < state.paymentMethods.lastIndex) {
                        SectionDivider()
                    }
                }
                SectionDivider()
                AccountSettingRow(
                    label = "New method",
                    value = "Add payment method",
                    icon = Icons.Rounded.AddCard,
                    testTag = "payments_add_method_row",
                    onClick = onOpenAddPayment,
                )
            }
        }

        // Subscriptions
        item {
            AccountSectionCard(
                title = "Subscriptions",
                subtitle = "Your recurring plans managed through your Account.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                state.subscriptions.forEachIndexed { index, sub ->
                    AccountSettingRow(
                        label = sub.plan,
                        value = "${sub.name} • ${sub.price}",
                        supportingText = sub.nextBilling,
                        icon = Icons.Rounded.Subscriptions,
                        statusBadge = { StatusPill("Active", StatusTone.VERIFIED) },
                        onClick = { onShowMessage("${sub.name} is active (${sub.nextBilling})") },
                    )
                    if (index < state.subscriptions.lastIndex) {
                        SectionDivider()
                    }
                }
            }
        }

        // Purchases & Reservations
        item {
            AccountSectionCard(
                title = "Purchases & reservations",
                subtitle = "Orders, bookings, and receipts found in your Mail.",
                modifier = Modifier.widthIn(max = 600.dp),
            ) {
                AccountSettingRow(
                    label = "Transactions",
                    value = "View recent receipts & invoices",
                    supportingText = "3 recent orders in the last 30 days",
                    icon = Icons.Rounded.ReceiptLong,
                    onClick = { onShowMessage("All 3 receipts are archived in your Mail") },
                )
                SectionDivider()
                AccountSettingRow(
                    label = "Reservations",
                    value = "Upcoming flights, hotels & dining",
                    supportingText = "No upcoming reservations",
                    icon = Icons.Rounded.FlightTakeoff,
                    onClick = { onShowMessage("Travel and event reservations sync automatically from Mail") },
                )
            }
        }
    }
}
