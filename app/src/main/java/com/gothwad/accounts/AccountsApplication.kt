package com.gothwad.accounts

import android.app.Application
import com.gothwad.accounts.core.BrokerPolicy
import com.gothwad.accounts.core.BrokerTrustPolicy
import com.gothwad.accounts.core.GothwadAccounts
import com.gothwad.accounts.core.GothwadConfig
import com.gothwad.accounts.core.Scope

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
                    "https://accounts.gothwadtech.com/oauth/callback"
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
