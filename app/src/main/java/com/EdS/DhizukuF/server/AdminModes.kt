package com.EdS.DhizukuF.server

import android.os.Build

/**
 * Which administration modes Android itself allows on this device.
 *
 * Device Administrator exists since Android 2.2: the receiver and the system
 * toggle work on every version. Google deprecated most admin policies from
 * Android 9 (API 28) on, but the right itself still functions — it is simply
 * the smallest mode, never equal to an owner.
 *
 * Profile Owner and Device Owner appeared with Android 5.0 (API 21). Below
 * that the DPM owner APIs and the private `setDeviceOwner`/`setProfileOwner`
 * calls used by the Activate page do not exist at all, so owner paths must
 * stay hidden instead of failing at runtime.
 *
 * This cannot be decided by the Android version alone: provisioning also
 * depends on device state (existing accounts, an already provisioned device,
 * Android 14+ headless-system-user requirements). Those conditions are
 * enforced by the system at activation time and the Activate page reports
 * the real result, so the UI only gates what the OS cannot provide.
 */
object AdminModes {

    /** The standard device-admin right: system toggle, works on every version. */
    val deviceAdministrator: Boolean
        get() = true

    /**
     * Profile Owner / Device Owner provisioning. The DPM owner APIs exist only
     * from API 21 (Android 5.0); the app's minimum is higher anyway because the
     * UI needs Compose (API 21), so in practice this is always true — it is
     * kept explicit so the rule survives a future minSdk change.
     */
    val ownerProvisioning: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP

    /** Owner-granting cards (Dhizuku / Shizuku / ADB) are meaningless without owner APIs. */
    val ownerCards: Boolean
        get() = ownerProvisioning
}
