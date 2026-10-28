package com.EdS.DhizukuF.hub;

import android.content.Context;
import android.content.pm.PackageManager;

/** The app this hub forwards to, and the check that it really is the DhizukuF built with this hub. */
final class Target {
    static final String PACKAGE = "com.EdS.DhizukuF";
    static final String[] AUTHORITIES = {
            PACKAGE + ".dhizuku_server.provider",
            PACKAGE + ".server.provider"
    };
    static final String REQUEST_ACTIVITY = PACKAGE + ".ui.activity.RequestPermissionActivity";

    private Target() {
    }

    /**
     * Only a DhizukuF signed with the same key as this hub is trusted. Without this check a look-alike
     * package named com.EdS.DhizukuF could receive the Dhizuku API calls of other apps.
     */
    static boolean trusted(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            return pm.checkSignatures(context.getPackageName(), PACKAGE) == PackageManager.SIGNATURE_MATCH;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
