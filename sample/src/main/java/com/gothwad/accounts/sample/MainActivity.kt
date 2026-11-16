package com.gothwad.accounts.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.gothwad.accounts.core.AccountState
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.ui.components.GothwadAccountAvatarButton
import com.gothwad.accounts.ui.components.GothwadAccountSettingsCard
import com.gothwad.accounts.ui.components.GothwadAccountSwitcherSheet
import com.gothwad.accounts.ui.components.GothwadAuthStatusBanner
import com.gothwad.accounts.ui.openAccountManagement

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val accountState by GothwadAccounts.state.collectAsState()
                val discovered by GothwadAccounts.discoveredAccounts.collectAsState()
                val error by GothwadAccounts.error.collectAsState()
                var showSwitcher by remember { mutableStateOf(false) }

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Sample Host (${BuildConfig.FLAVOR})") },
                            actions = {
                                GothwadAccountAvatarButton(
                                    accountState = accountState,
                                    onClick = {
                                        GothwadAccounts.syncDiscoveredAccounts()
                                        showSwitcher = true
                                    },
                                )
                            },
                        )
                    },
                ) { padding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        error?.let { failure ->
                            GothwadAuthStatusBanner(
                                failure = failure,
                                onRetry = {
                                    GothwadAccounts.clearError()
                                    GothwadAccounts.refreshProfile()
                                },
                                onSignInAgain = {
                                    GothwadAccounts.clearError()
                                    GothwadAccounts.signIn(this@MainActivity, chooseAccount = true)
                                },
                                onDismiss = { GothwadAccounts.clearError() },
                            )
                        }

                        GothwadAccountSettingsCard(
                            accountState = accountState,
                            discoveredCount = discovered.size,
                            onSignInClick = { GothwadAccounts.signIn(this@MainActivity, chooseAccount = true) },
                            onSwitchOrManageClick = {
                                GothwadAccounts.syncDiscoveredAccounts()
                                showSwitcher = true
                            },
                            onManageAccountClick = { GothwadAccounts.openAccountManagement(this@MainActivity) },
                        )

                        if (accountState is AccountState.SignedIn) {
                            OutlinedButton(
                                onClick = { GothwadAccounts.refreshProfile() },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Refresh Profile")
                            }
                        }

                        Button(
                            onClick = { GothwadAccounts.openAccountManagement(this@MainActivity) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Open Full Gothwad Account UI")
                        }
                    }
                }

                if (showSwitcher) {
                    GothwadAccountSwitcherSheet(
                        accountState = accountState,
                        discoveredAccounts = discovered,
                        onDismiss = { showSwitcher = false },
                        onSwitchAccount = {
                            GothwadAccounts.switchTo(it)
                            showSwitcher = false
                        },
                        onContinueWithPeerAccount = { peer ->
                            showSwitcher = false
                            GothwadAccounts.signIn(
                                this@MainActivity,
                                chooseAccount = false,
                                loginHint = peer.email ?: peer.username,
                            )
                        },
                        onAddAnotherAccount = {
                            showSwitcher = false
                            GothwadAccounts.signIn(this@MainActivity, chooseAccount = true)
                        },
                        onManageGothwadAccount = {
                            showSwitcher = false
                            GothwadAccounts.openAccountManagement(this@MainActivity)
                        },
                        onSignOutThisApp = { acc ->
                            GothwadAccounts.signOut(acc.userId)
                            showSwitcher = false
                        },
                        onOpenPrivacy = { GothwadAccounts.openWebPage(this@MainActivity, "/privacy") },
                        onOpenTerms = { GothwadAccounts.openWebPage(this@MainActivity, "/terms") },
                    )
                }
            }
        }
    }
}
