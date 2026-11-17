package com.gothwad.accounts.core.broker

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import android.os.Process
import com.gothwad.accounts.core.BrokerTrustPolicy
import java.security.MessageDigest

internal object TrustVerifier {

    fun isTrustedUid(
        context: Context,
        callingUid: Int,
        policy: BrokerTrustPolicy,
    ): Boolean = verifyBinderCaller(context, callingUid, policy) != null

    fun verifyBinderCaller(
        context: Context,
        callingUid: Int,
        policy: BrokerTrustPolicy,
    ): VerifiedCaller? {
        if (callingUid < 0) return null
        if (callingUid == Process.myUid()) {
            return VerifiedCaller(callingUid, listOf(context.packageName))
        }
        val packages = context.packageManager.getPackagesForUid(callingUid) ?: return null
        if (packages.isEmpty()) return null
        // Reject ambiguous multi-package UIDs unless EVERY package sharing that UID passes verification.
        if (!packages.all { pkg -> verifyPackage(context, pkg, policy) }) {
            return null
        }
        return VerifiedCaller(callingUid, packages.toList())
    }

    fun verifyPackage(
        context: Context,
        packageName: String,
        policy: BrokerTrustPolicy,
    ): Boolean {
        if (packageName.isBlank()) return false
        val peerSigners = extractCurrentSignersSha256(context.packageManager, packageName)
        if (peerSigners.isEmpty()) return false

        return when (policy) {
            is BrokerTrustPolicy.PinnedSigners -> {
                val allowed = policy.packageCertSha256[packageName]
                    ?.map { it.lowercase() }
                    ?.toSet()
                    ?: return false
                peerSigners.all { it in allowed }
            }

            is BrokerTrustPolicy.SameSigningCertificate -> {
                if (packageName !in policy.knownPackages) return false
                val isSelfDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isSelfDebuggable && !policy.allowDebugBuild) return false
                val peerAppFlags = runCatching {
                    context.packageManager.getApplicationInfo(packageName, 0).flags
                }.getOrNull() ?: return false
                val isPeerDebuggable = (peerAppFlags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
                if (isPeerDebuggable && !policy.allowDebugBuild) return false

                val ownSigners = extractCurrentSignersSha256(context.packageManager, context.packageName)
                ownSigners.isNotEmpty() && peerSigners == ownSigners
            }
        }
    }

    internal fun extractCurrentSignersSha256(
        packageManager: PackageManager,
        packageName: String,
    ): Set<String> = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val pkgInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNING_CERTIFICATES,
            )
            val signingInfo = pkgInfo.signingInfo ?: return@runCatching emptySet()
            val signatures: Array<Signature> = if (signingInfo.hasMultipleSigners()) {
                signingInfo.apkContentsSigners
            } else {
                signingInfo.signingCertificateHistory
                    ?.lastOrNull()
                    ?.let { arrayOf(it) }
                    ?: signingInfo.apkContentsSigners
            }
            signatures.map(::sha256Hex).toSet()
        } else {
            @Suppress("DEPRECATION")
            val pkgInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_SIGNATURES,
            )
            @Suppress("DEPRECATION")
            val signatures = pkgInfo.signatures ?: emptyArray()
            signatures.map(::sha256Hex).toSet()
        }
    }.getOrDefault(emptySet())

    private fun sha256Hex(signature: Signature): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
