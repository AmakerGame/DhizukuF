package com.EdS.DhizukuF.dish;

/**
 * Wire format shared with DhizukuF (DishBinder / DishEngine). Keep codes in sync.
 */
final class DishProtocol {
    static final String DEFAULT_PACKAGE = "com.EdS.DhizukuF";
    /** Client protocol label (independent of app version). */
    static final String CLIENT_VERSION = "1.1";

    static final int TX_EXEC = 1;
    static final int TX_WAIT_APPROVAL = 2;

    static final int KIND_EXECUTED = 0;
    static final int KIND_NEED_APPROVAL = 1;
    static final int KIND_DENIED = 2;
    static final int KIND_APPROVED = 3;

    /** RECEIVER_FOREGROUND | INCLUDE_STOPPED_PACKAGES */
    static final int BROADCAST_FLAGS = 0x10000000 | 0x20;

    private DishProtocol() {}

    static String appPackage() {
        String pkg = System.getenv("DISH_PACKAGE");
        return (pkg == null || pkg.isEmpty()) ? DEFAULT_PACKAGE : pkg;
    }

    static boolean debug() {
        String v = System.getenv("DISH_DEBUG");
        return v != null && (v.equals("1") || v.equalsIgnoreCase("true"));
    }

    /** Connect attempts (default 3). Env: DISH_CONNECT_ATTEMPTS */
    static int connectAttempts() {
        return envInt("DISH_CONNECT_ATTEMPTS", 3, 1, 8);
    }

    /** First attempt wait seconds (default 8). Env: DISH_CONNECT_TIMEOUT */
    static int connectTimeoutSec() {
        return envInt("DISH_CONNECT_TIMEOUT", 8, 2, 60);
    }

    private static int envInt(String key, int def, int min, int max) {
        String v = System.getenv(key);
        if (v == null || v.isEmpty()) return def;
        try {
            int n = Integer.parseInt(v.trim());
            if (n < min) return min;
            if (n > max) return max;
            return n;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    static void log(String msg) {
        if (debug()) {
            System.err.println("dish[debug]: " + msg);
        }
    }

    static final class Reply {
        int kind;
        int code;
        String out = "";
        String err = "";
        String message = "";
    }
}
