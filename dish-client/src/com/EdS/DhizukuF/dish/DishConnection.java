package com.EdS.DhizukuF.dish;

import android.content.ComponentName;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Handshake with DhizukuF and Binder command execution.
 * Broadcast path is tuned per Android API level; several IActivityManager
 * signatures are tried so the same dex works from API 26 upward.
 */
final class DishConnection {
    private static CallbackBinder callback;
    private static volatile IBinder server;
    private static final Object LOCK = new Object();

    private DishConnection() {}

    static void reset() {
        synchronized (LOCK) {
            server = null;
            callback = null;
        }
    }

    static boolean isConnected() {
        IBinder s = server;
        return s != null && binderAlive(s);
    }

    /**
     * Exchange binders with DhizukuF. Retries so the app process can start.
     * @return server binder, or null if DhizukuF never answered
     */
    static IBinder connect() {
        synchronized (LOCK) {
            if (server != null && binderAlive(server)) {
                return server;
            }
            server = null;
        }

        int attempts = DishProtocol.connectAttempts();
        int baseTimeout = DishProtocol.connectTimeoutSec();
        Throwable lastError = null;

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                CallbackBinder cb = new CallbackBinder();
                Bundle bundle = new Bundle();
                bundle.putBinder("cb", cb);
                Intent intent = new Intent();
                intent.setComponent(new ComponentName(
                        DishProtocol.appPackage(),
                        "com.EdS.DhizukuF.dish.DishReceiver"));
                intent.addFlags(DishProtocol.BROADCAST_FLAGS);
                intent.putExtra("dish", bundle);

                DishProtocol.log("connect attempt " + attempt + "/" + attempts
                        + " sdk=" + sdk() + " pkg=" + DishProtocol.appPackage());
                sendBroadcast(intent);

                long timeoutSec = baseTimeout + (attempt - 1) * 5L;
                if (cb.latch.await(timeoutSec, TimeUnit.SECONDS)) {
                    IBinder received = cb.received;
                    if (received != null && binderAlive(received)) {
                        synchronized (LOCK) {
                            callback = cb;
                            server = received;
                        }
                        DishProtocol.log("connected");
                        return received;
                    }
                    DishProtocol.log("empty or dead binder on attempt " + attempt);
                } else {
                    DishProtocol.log("timeout after " + timeoutSec + "s on attempt " + attempt);
                }
            } catch (Throwable e) {
                lastError = unwrap(e);
                DishProtocol.log("attempt " + attempt + " failed: " + lastError);
            }
        }

        if (lastError != null) {
            System.err.println("dish: cannot reach DhizukuF (Android API " + sdk() + "): " + lastError);
        } else {
            System.err.println("dish: DhizukuF did not answer. Make sure it is installed and "
                    + "activated (Device Owner), and that Enable dish is on.");
        }
        return null;
    }

    static int execute(List<String> args) {
        return execute(args, true);
    }

    private static int execute(List<String> args, boolean allowDeadRetry) {
        IBinder target = connect();
        if (target == null) return 2;
        try {
            DishProtocol.Reply r = call(target, DishProtocol.TX_EXEC, args);
            if (r.kind == DishProtocol.KIND_NEED_APPROVAL) {
                System.err.println(r.message);
                DishProtocol.Reply wait = call(
                        target, DishProtocol.TX_WAIT_APPROVAL, Collections.<String>emptyList());
                if (wait.kind != DishProtocol.KIND_APPROVED) {
                    System.err.println(wait.message);
                    return 1;
                }
                r = call(target, DishProtocol.TX_EXEC, args);
            }
            if (r.kind == DishProtocol.KIND_EXECUTED) {
                System.out.print(r.out);
                System.out.flush();
                System.err.print(r.err);
                System.err.flush();
                return r.code;
            }
            System.err.println(r.message);
            return 1;
        } catch (RemoteException e) {
            reset();
            if (allowDeadRetry && isDeadObject(e)) {
                DishProtocol.log("DeadObjectException, reconnecting");
                return execute(args, false);
            }
            System.err.println("dish: lost connection to DhizukuF (" + e + ")");
            return 2;
        }
    }

    private static boolean isDeadObject(RemoteException e) {
        String n = e.getClass().getSimpleName();
        return "DeadObjectException".equals(n) || "TransactionTooLargeException".equals(n);
    }

    private static boolean binderAlive(IBinder b) {
        if (b == null) return false;
        try {
            Method m = b.getClass().getMethod("pingBinder");
            Object r = m.invoke(b);
            if (r instanceof Boolean) return ((Boolean) r).booleanValue();
        } catch (Throwable ignored) {
        }
        return true;
    }

    private static Throwable unwrap(Throwable e) {
        if (e instanceof InvocationTargetException && e.getCause() != null) {
            return e.getCause();
        }
        return e;
    }

    private static int sdk() {
        return android.os.Build.VERSION.SDK_INT;
    }

    private static DishProtocol.Reply call(IBinder target, int code, List<String> args)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            String[] arr = args.toArray(new String[args.size()]);
            data.writeStringArray(arr);
            boolean ok = target.transact(code, data, reply, 0);
            if (!ok) {
                throw new RemoteException();
            }
            DishProtocol.Reply r = new DishProtocol.Reply();
            r.kind = reply.readInt();
            if (r.kind == DishProtocol.KIND_EXECUTED) {
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

    private static void sendBroadcast(Intent intent) throws Exception {
        IBinder amBinder = getActivityManagerBinder();
        if (amBinder == null) {
            throw new IllegalStateException("activity service not found");
        }

        Class<?> stub = Class.forName("android.app.IActivityManager$Stub");
        Object am = stub.getMethod("asInterface", IBinder.class).invoke(null, amBinder);

        List<Method> candidates = findBroadcastMethods(am.getClass(), sdk());
        if (candidates.isEmpty()) {
            throw new NoSuchMethodException("no broadcast method on IActivityManager");
        }
        DishProtocol.log("broadcast candidates: " + candidates.size() + " (sdk " + sdk() + ")");

        Throwable last = null;
        for (Method method : candidates) {
            try {
                Object[] args = buildArgs(method.getParameterTypes(), intent);
                DishProtocol.log("try " + method.getName() + " params=" + method.getParameterTypes().length);
                Object result = method.invoke(am, args);
                if (result instanceof Integer && ((Integer) result).intValue() < 0) {
                    throw new IllegalStateException("broadcast rejected, code " + result);
                }
                return;
            } catch (InvocationTargetException e) {
                last = e.getCause() != null ? e.getCause() : e;
                DishProtocol.log("reject " + method.getName() + ": " + last);
            } catch (Exception e) {
                last = e;
                DishProtocol.log("reject " + method.getName() + ": " + e);
            }
        }
        throw new IllegalStateException("every broadcast variant failed, last: " + last);
    }

    private static IBinder getActivityManagerBinder() throws Exception {
        Class<?> sm = Class.forName("android.os.ServiceManager");
        Method getService = sm.getMethod("getService", String.class);
        String[] names = {"activity", "activity_task"};
        for (String name : names) {
            Object b = getService.invoke(null, name);
            if (b instanceof IBinder) {
                DishProtocol.log("ServiceManager.getService(\"" + name + "\") ok");
                return (IBinder) b;
            }
        }
        return null;
    }

    private static Object[] buildArgs(Class<?>[] types, Intent intent) {
        Object[] args = new Object[types.length];
        int ints = 0;
        int userId = android.os.Process.myUid() / 100000;
        for (int i = 0; i < types.length; i++) {
            Class<?> t = types[i];
            if (t == Intent.class) {
                args[i] = intent;
            } else if (t == int.class) {
                if (ints == 0) {
                    args[i] = Integer.valueOf(0);
                } else if (ints == 1) {
                    args[i] = Integer.valueOf(-1);
                } else {
                    args[i] = Integer.valueOf(userId);
                }
                ints++;
            } else if (t == boolean.class) {
                args[i] = Boolean.FALSE;
            } else if (t == String.class) {
                args[i] = null;
            } else {
                args[i] = null;
            }
        }
        return args;
    }

    private static List<Method> findBroadcastMethods(Class<?> type, int sdk) {
        List<Method> found = new ArrayList<Method>();
        for (Method m : type.getMethods()) {
            String name = m.getName();
            if (!name.equals("broadcastIntent") && !name.equals("broadcastIntentWithFeature")) {
                continue;
            }
            boolean hasIntent = false;
            for (Class<?> p : m.getParameterTypes()) {
                if (p == Intent.class) {
                    hasIntent = true;
                    break;
                }
            }
            if (hasIntent) found.add(m);
        }

        final boolean preferFeature = sdk >= 30;
        Collections.sort(found, new Comparator<Method>() {
            @Override
            public int compare(Method x, Method y) {
                boolean fx = x.getName().equals("broadcastIntentWithFeature");
                boolean fy = y.getName().equals("broadcastIntentWithFeature");
                if (fx != fy) {
                    if (preferFeature) {
                        return fx ? -1 : 1;
                    }
                    return fx ? 1 : -1;
                }
                return y.getParameterTypes().length - x.getParameterTypes().length;
            }
        });
        return found;
    }

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
}
