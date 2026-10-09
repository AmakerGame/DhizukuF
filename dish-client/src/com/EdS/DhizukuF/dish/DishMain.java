package com.EdS.DhizukuF.dish;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * dish client (like rish for Shizuku). Started by the "dish" script through app_process as the
 * uid of whatever terminal runs it. Pure Java on purpose: the dex stays tiny and has no
 * dependencies. All console output is English only. Keep the protocol in sync with
 * DishProtocol.kt inside the app.
 */
public final class DishMain {
    private static final int MAGIC = 0x44495348; // "DISH"
    private static final int VERSION = 1;

    private static final int ST_OK = 0;
    private static final int ST_NEED_PERMISSION = 1;
    private static final int ST_DENIED = 2;

    private static final int FR_END = 0;
    private static final int FR_OUT = 1;
    private static final int FR_ERR = 2;

    /** Key of the client uid extra read by DhizukuF's permission dialog. */
    private static final String PARAM_CLIENT_UID = "uid";

    private static final String DEFAULT_PACKAGE = "com.EdS.DhizukuF";

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
                System.out.print("dish> ");
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

    private static int execute(List<String> args) {
        LocalSocket socket = connect();
        try {
            if (socket == null) {
                System.err.println("dish: cannot reach DhizukuF. Make sure DhizukuF is activated "
                        + "(Device Owner) and running.");
                return 2;
            }
            socket.setSoTimeout(120000);

            String bad = verifyServer(socket);
            if (bad != null) {
                System.err.println(bad);
                return 2;
            }

            DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));

            out.writeInt(MAGIC);
            out.writeInt(VERSION);
            out.flush();

            int status = in.readUnsignedByte();
            if (status == ST_NEED_PERMISSION) {
                System.err.println(in.readUTF());
                requestApproval();
                status = in.readUnsignedByte();
            }
            if (status == ST_DENIED) {
                System.err.println(in.readUTF());
                return 1;
            }
            if (status != ST_OK) {
                System.err.println("dish: protocol error");
                return 2;
            }

            out.writeInt(args.size());
            for (String a : args) out.writeUTF(a);
            out.flush();

            while (true) {
                int frame = in.readUnsignedByte();
                if (frame == FR_OUT) {
                    System.out.print(in.readUTF());
                    System.out.flush();
                } else if (frame == FR_ERR) {
                    System.err.print(in.readUTF());
                    System.err.flush();
                } else if (frame == FR_END) {
                    return in.readInt();
                } else {
                    System.err.println("dish: protocol error");
                    return 2;
                }
            }
        } catch (EOFException e) {
            System.err.println("dish: connection closed by DhizukuF");
            return 2;
        } catch (IOException e) {
            System.err.println("dish: " + e.getMessage());
            return 2;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    /** Connects to the server; if the app process is not running, wakes it up and retries. */
    private static LocalSocket connect() {
        for (int attempt = 0; attempt < 4; attempt++) {
            LocalSocket candidate = new LocalSocket();
            try {
                candidate.connect(new LocalSocketAddress(appPackage() + ".dish",
                        LocalSocketAddress.Namespace.ABSTRACT));
                return candidate;
            } catch (IOException e) {
                try {
                    candidate.close();
                } catch (IOException ignored) {
                }
            }
            if (attempt == 0) wakeApp();
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
        return null;
    }

    private static void wakeApp() {
        String pkg = appPackage();
        try {
            java.lang.Process process = new ProcessBuilder(
                    "/system/bin/am", "broadcast",
                    "-n", pkg + "/com.EdS.DhizukuF.dish.DishWakeReceiver")
                    .redirectErrorStream(true).start();
            drain(process.getInputStream());
            process.waitFor();
        } catch (Exception ignored) {
        }
    }

    /** Anti-squatting: the listener must run under the uid of the DhizukuF app. */
    private static String verifyServer(LocalSocket socket) {
        String expectedStr = System.getenv("DISH_SERVER_UID");
        if (expectedStr == null || expectedStr.isEmpty()) return null;
        int expected;
        try {
            expected = Integer.parseInt(expectedStr.trim());
        } catch (NumberFormatException e) {
            return null;
        }
        int actual;
        try {
            actual = socket.getPeerCredentials().getUid();
        } catch (IOException e) {
            return "dish: cannot verify DhizukuF";
        }
        if (actual != expected) {
            return "dish: refusing to talk to an untrusted socket owner (uid " + actual + ")";
        }
        return null;
    }

    /**
     * Opens the DhizukuF permission dialog on behalf of this terminal (same dialog every client
     * app gets). It is started from here because the terminal is the foreground app.
     */
    private static void requestApproval() {
        String pkg = appPackage();
        try {
            java.lang.Process process = new ProcessBuilder(
                    "/system/bin/am", "start",
                    "-n", pkg + "/com.EdS.DhizukuF.ui.activity.RequestPermissionActivity",
                    "--ei", PARAM_CLIENT_UID, String.valueOf(android.os.Process.myUid()))
                    .redirectErrorStream(true).start();
            drain(process.getInputStream());
            process.waitFor();
        } catch (Exception e) {
            System.err.println("dish: could not open the dialog automatically, "
                    + "enable this app in DhizukuF > App management.");
        }
    }

    private static void drain(InputStream stream) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = stream.read(buf)) != -1) sink.write(buf, 0, n);
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
