package com.bansify.accounts

import android.app.Application
import com.bansify.accounts.core.BrokerPolicy
import com.bansify.accounts.core.BrokerTrustPolicy
import com.bansify.accounts.core.GothwadAccounts
import com.bansify.accounts.core.GothwadConfig
import com.bansify.accounts.core.Scope

class AccountsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GothwadAccounts.init(
            this,
            GothwadConfig(
                clientId = "gothwad-accounts-android",
                redirectUri = if (BuildConfig.DEBUG) {
                    "$packageName:/oauth/callback"
                } else {
                    "https://accounts.bansify.com/oauth/callback"
                },
                allowCustomSchemeRedirect = BuildConfig.DEBUG,
                scopes = setOf(
                    Scope.Profile,
                    Scope.Email,
                    Scope.OfflineAccess,
                    Scope.DriveRead,
                    Scope.AccountRead,
                    Scope.AccountProfileWrite,
                ),
                brokerPolicy = BrokerPolicy.PeerDiscovery(
                    trustPolicy = BrokerTrustPolicy.SameSigningCertificate(
                        allowDebugBuild = BuildConfig.DEBUG,
                    ),
                ),
            ),
        )
    }
}
