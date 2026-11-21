package com.bansify.accounts.sample

import android.app.Application
import com.bansify.accounts.core.BrokerPolicy
import com.bansify.accounts.core.BrokerTrustPolicy
import com.bansify.accounts.core.GothwadAccounts
import com.bansify.accounts.core.GothwadConfig

class AccountsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        GothwadAccounts.init(
            this,
            GothwadConfig(
                clientId = "gothwad-accounts-sample-${BuildConfig.FLAVOR}",
                redirectUri = "$packageName:/oauth/callback",
                brokerPolicy = BrokerPolicy.PeerDiscovery(
                    trustPolicy = BrokerTrustPolicy.SameSigningCertificate(
                        allowDebugBuild = BuildConfig.DEBUG,
                    ),
                ),
            ),
        )
    }
}
