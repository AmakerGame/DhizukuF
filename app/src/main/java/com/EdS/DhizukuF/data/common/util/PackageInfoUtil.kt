/**
 * Removing the warning is not possible for compatibility reasons.
 */
@file:Suppress("DEPRECATION")

package com.EdS.DhizukuF.data.common.util

import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build

fun PackageManager.getPackageNameForUid(uid: Int): String? =
    getPackagesForUid(uid)?.firstOrNull()

fun PackageManager.getPackageInfoForUid(
    uid: Int,
    flags: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES
    else PackageManager.GET_SIGNATURES
): PackageInfo? = kotlin.runCatching {
    getPackageInfo(getPackageNameForUid(uid) ?: return null, flags)
}.getOrNull()

/**
 * SHA-256 of the signing certificate. For a single-signer app this is the same value as before
 * (stored entries stay valid). An app with several signers is identified by ALL of them, not just
 * the first one.
 */
@OptIn(ExperimentalStdlibApi::class)
val PackageInfo.signature: String?
    get() {
        val signers = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) signingInfo?.apkContentsSigners
        else signatures)
        if (signers.isNullOrEmpty()) return null
        return signers.map { it.toByteArray().digest("sha256").toHexString(HexFormat.UpperCase) }
            .sorted()
            .joinToString(":")
    }
