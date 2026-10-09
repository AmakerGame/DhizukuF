package com.EdS.DhizukuF.dish;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * dish client (like rish for Shizuku). Started by the "dish" script through app_process as the
 * uid of whatever terminal runs it. Pure Java on purpose: the dex stays tiny and has no
 * dependencies. All console output is English only.
 *
 * Transport (no sockets, no `am`, works from any terminal):
 *  1. send a broadcast to DhizukuF's DishReceiver through ActivityManager; it carries our own
 *     Binder;
 *  2. DhizukuF answers by calling our Binder with its own Binder;
 *  3. from then on every command is a direct Binder call, so DhizukuF learns our real uid
 *     from Binder.getCallingUid() and cannot be fooled.
 */
public final class DishMain {
    private static final String DEFAULT_PACKAGE = "com.EdS.DhizukuF";

    // Transaction codes of DhizukuF's binder (keep in sync with DishBinder.kt)
    private static final int TX_EXEC = 1;
    private static final int TX_WAIT_APPROVAL = 2;

    // Reply kinds (keep in sync with DishEngine.kt)
    private static final int KIND_EXECUTED = 0;
    private static final int KIND_NEED_APPROVAL = 1;
    private static final int KIND_DENIED = 2;
    private static final int KIND_APPROVED = 3;

    private static final int FLAGS = 0x10000000 | 0x20; // RECEIVER_FOREGROUND | INCLUDE_STOPPED_PACKAGES

    /** Kept in a static field so the binder object is never garbage collected. */
    private static CallbackBinder callback;
    private static IBinder server;

    private DishMain() {}

    private static String appPackage() {
        String pkg = System.getenv("DISH_PACKAGE");
        return (pkg == null || pkg.isEmpty()) ? DEFAULT_PACKAGE : pkg;
    }

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (Throwable e) {
            System.err.println("dish: " + e);
            code = 1;
        }
        System.out.flush();
        System.err.flush();
        System.exit(code);
    }

    static int run(String[] args) throws IOException {
        if (args.length == 0) return interactive();
        if (args[0].equals("-c")) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                if (i > 1) sb.append(' ');
                sb.append(args[i]);
            }
            try {
                return execute(tokenize(sb.toString()));
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                return 1;
            }
        }
        List<String> list = new ArrayList<String>();
        for (String a : args) list.add(a);
        return execute(list);
    }

    private static int interactive() throws IOException {
        boolean tty = "1".equals(System.getenv("DISH_TTY"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        if (tty) {
            System.err.println("dish - Device Owner console via DhizukuF. Type 'help', 'exit' to quit.");
        }
        int last = 0;
        while (true) {
            if (tty) {
                System.out.print("dish$ ");
                System.out.flush();
            }
            String line = reader.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equals("exit") || line.equals("quit")) break;
            List<String> tokens;
            try {
                tokens = tokenize(line);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                last = 1;
                continue;
            }
            last = execute(tokens);
        }
        return last;
    }

    // ------------------------------------------------------------------ binder plumbing

    private static final class CallbackBinder extends Binder {
        final CountDownLatch latch = new CountDownLatch(1);
        volatile IBinder received;

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags)
                throws RemoteException {
            if (code == 1) {
                received = data.readStrongBinder();
                latch.countDown();
                return true;
            }
            return super.onTransact(code, data, reply, flags);
        }
    }

    private static final class Reply {
        int kind;
        int code;
        String out = "";
        String err = "";
        String message = "";
    }

    /** Hands our binder to DhizukuF and waits for its binder. Returns null if it never answers. */
    private static IBinder connect() {
        if (server != null) return server;
        try {
            callback = new CallbackBinder();
            Bundle bundle = new Bundle();
            bundle.putBinder("cb", callback);
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(appPackage(), "com.EdS.DhizukuF.dish.DishReceiver"));
            intent.addFlags(FLAGS);
            intent.putExtra("dish", bundle);
            sendBroadcast(intent);
            if (!callback.latch.await(20, TimeUnit.SECONDS)) {
                System.err.println("dish: DhizukuF did not answer. Make sure it is installed and "
                        + "activated (Device Owner).");
                return null;
            }
            server = callback.received;
            return server;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            System.err.println("dish: cannot send the request to DhizukuF: " + cause);
            return null;
        } catch (Throwable e) {
            System.err.println("dish: cannot send the request to DhizukuF: " + e);
            return null;
        }
    }

    /** Sends an explicit, unordered broadcast as our own uid through the ActivityManager binder. */
    private static void sendBroadcast(Intent intent) throws Exception {
        Class<?> serviceManager = Class.forName("android.os.ServiceManager");
        IBinder binder = (IBinder) serviceManager.getMethod("getService", String.class)
                .invoke(null, "activity");
        if (binder == null) throw new IllegalStateException("activity service not found");
        Class<?> stub = Class.forName("android.app.IActivityManager$Stub");
        Object am = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);

        Method method = findBroadcastMethod(am.getClass());
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        int ints = 0;
        int userId = android.os.Process.myUid() / 100000;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == Intent.class) {
                args[i] = intent;
            } else if (t == int.class) {
                // order in every Android version: resultCode, appOp, userId
                args[i] = ints == 0 ? 0 : (ints == 1 ? -1 : userId);
                ints++;
            } else if (t == boolean.class) {
                args[i] = false; // serialized, sticky
            } else {
                args[i] = null; // caller, strings, receiver, bundles, permissions
            }
        }
        Object result = method.invoke(am, args);
        if (result instanceof Integer && ((Integer) result) < 0) {
            throw new IllegalStateException("broadcast rejected, code " + result);
        }
    }

    private static Method findBroadcastMethod(Class<?> type) throws NoSuchMethodException {
        String[] names = {"broadcastIntentWithFeature", "broadcastIntent"};
        for (String name : names) {
            Method best = null;
            for (Method m : type.getMethods()) {
                if (!m.getName().equals(name)) continue;
                boolean hasIntent = false;
                for (Class<?> p : m.getParameterTypes()) if (p == Intent.class) hasIntent = true;
                if (!hasIntent) continue;
                if (best == null || m.getParameterTypes().length > best.getParameterTypes().length) {
                    best = m;
                }
            }
            if (best != null) return best;
        }
        throw new NoSuchMethodException("no broadcast method on IActivityManager");
    }

    private static Reply call(IBinder target, int code, List<String> args) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            data.writeStringArray(args.toArray(new String[0]));
            target.transact(code, data, reply, 0);
            Reply r = new Reply();
            r.kind = reply.readInt();
            if (r.kind == KIND_EXECUTED) {
                r.code = reply.readInt();
                r.out = nonNull(reply.readString());
                r.err = nonNull(reply.readString());
            } else {
                r.message = nonNull(reply.readString());
            }
            return r;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }

    private static int execute(List<String> args) {
        IBinder target = connect();
        if (target == null) return 2;
        try {
            Reply r = call(target, TX_EXEC, args);
            if (r.kind == KIND_NEED_APPROVAL) {
                System.err.println(r.message);
                Reply wait = call(target, TX_WAIT_APPROVAL, new ArrayList<String>());
                if (wait.kind != KIND_APPROVED) {
                    System.err.println(wait.message);
                    return 1;
                }
                r = call(target, TX_EXEC, args);
            }
            if (r.kind == KIND_EXECUTED) {
                System.out.print(r.out);
                System.out.flush();
                System.err.print(r.err);
                System.err.flush();
                return r.code;
            }
            System.err.println(r.message);
            return 1;
        } catch (RemoteException e) {
            server = null;
            System.err.println("dish: lost connection to DhizukuF (" + e + ")");
            return 2;
        }
    }

    static List<String> tokenize(String line) {
        List<String> out = new ArrayList<String>();
        StringBuilder cur = new StringBuilder();
        char quote = 0;
        boolean has = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quote != 0) {
                if (c == quote) {
                    quote = 0;
                } else if (c == '\\' && quote == '"' && i + 1 < line.length()) {
                    cur.append(line.charAt(++i));
                } else {
                    cur.append(c);
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
                has = true;
            } else if (c == '\\' && i + 1 < line.length()) {
                cur.append(line.charAt(++i));
                has = true;
            } else if (Character.isWhitespace(c)) {
                if (has || cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    has = false;
                }
            } else {
                cur.append(c);
            }
        }
        if (quote != 0) {
            throw new IllegalArgumentException("dish: unterminated quote");
        }
        if (has || cur.length() > 0) out.add(cur.toString());
        return out;
    }
}
