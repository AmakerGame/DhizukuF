package com.EdS.DhizukuF.dish;

import android.os.Process;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * dish client (like rish for Shizuku). Started by the "dish" script through app_process as the
 * uid of whatever terminal runs it. Pure Java on purpose: the dex stays tiny and has no
 * dependencies. All console output is English only.
 *
 * Transport: `am broadcast` to DhizukuF's DishReceiver. It works from any terminal (no sockets,
 * no SELinux problems) and starts the DhizukuF process when it is not running. The reply is the
 * broadcast result: result code + base64 body.
 */
public final class DishMain {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final int CODE_NEED_AUTH = 100; // identity not proven yet
    private static final int CODE_WAIT = 101;      // waiting for the user's approval
    private static final int CODE_DENIED = 102;    // final refusal, body = reason

    private static final long APPROVAL_TIMEOUT_MS = 60000L;

    private static final String DEFAULT_PACKAGE = "com.EdS.DhizukuF";

    private static final Pattern RESULT = Pattern.compile(
            "Broadcast completed: result=(-?\\d+)(?:, data=\"([^\"]*)\")?");

    /** Random per-process token; the app learns who owns it from Android, not from us. */
    private static final String TOKEN = newToken();

    private DishMain() {}

    private static String newToken() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

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

    private static final class Reply {
        final int code;
        final String body;

        Reply(int code, String body) {
            this.code = code;
            this.body = body;
        }
    }

    private static int execute(List<String> args) {
        String encoded = encodeArgs(args);
        long deadline = System.currentTimeMillis() + APPROVAL_TIMEOUT_MS;
        boolean dialogOpened = false;
        boolean hinted = false;

        while (true) {
            Reply reply = send(encoded);
            if (reply == null) {
                System.err.println("dish: cannot reach DhizukuF. Make sure DhizukuF is installed, "
                        + "activated (Device Owner) and not force-stopped.");
                return 2;
            }

            if (reply.code == CODE_DENIED) {
                System.err.println(reply.body);
                return 1;
            }
            if (reply.code != CODE_NEED_AUTH && reply.code != CODE_WAIT) {
                int split = reply.body.indexOf('\u0000');
                String out = split < 0 ? reply.body : reply.body.substring(0, split);
                String err = split < 0 ? "" : reply.body.substring(split + 1);
                System.out.print(out);
                System.out.flush();
                System.err.print(err);
                System.err.flush();
                return reply.code;
            }

            // Not authorized yet: ask DhizukuF to show its approval dialog (once) and keep polling.
            if (reply.code == CODE_WAIT && !hinted) {
                hinted = true;
                System.err.println(reply.body);
            }
            if (!dialogOpened) {
                dialogOpened = true;
                requestApproval();
            }
            if (System.currentTimeMillis() > deadline) {
                System.err.println("dish: timed out waiting for approval in DhizukuF.");
                return 1;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
            }
        }
    }

    private static String encodeArgs(List<String> args) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.size(); i++) {
            if (i > 0) sb.append('\u0000');
            sb.append(args.get(i));
        }
        if (sb.length() == 0) return "";
        return Base64.getUrlEncoder().withoutPadding().encodeToString(sb.toString().getBytes(UTF8));
    }

    /** One request/response round trip. Returns null if DhizukuF could not be reached. */
    private static Reply send(String encodedArgs) {
        List<String> cmd = new ArrayList<String>();
        cmd.add("/system/bin/am");
        cmd.add("broadcast");
        cmd.add("-f");
        cmd.add("32"); // FLAG_INCLUDE_STOPPED_PACKAGES: also wake a stopped DhizukuF
        cmd.add("-n");
        cmd.add(appPackage() + "/com.EdS.DhizukuF.dish.DishReceiver");
        cmd.add("--es");
        cmd.add("t");
        cmd.add(TOKEN);
        if (!encodedArgs.isEmpty()) {
            cmd.add("--es");
            cmd.add("a");
            cmd.add(encodedArgs);
        }
        String output = run(cmd);
        if (output == null) return null;
        Matcher m = RESULT.matcher(output);
        if (!m.find()) return null;
        int code;
        try {
            code = Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return null;
        }
        String data = m.group(2);
        String body = "";
        if (data != null && !data.isEmpty()) {
            try {
                body = new String(Base64.getUrlDecoder().decode(data), UTF8);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return new Reply(code, body);
    }

    /**
     * Opens the DhizukuF permission dialog on behalf of this terminal (same dialog every client
     * app gets). Android tells DhizukuF which app launched it, so the identity cannot be faked.
     */
    private static void requestApproval() {
        String pkg = appPackage();
        List<String> cmd = new ArrayList<String>();
        cmd.add("/system/bin/am");
        cmd.add("start");
        cmd.add("-n");
        cmd.add(pkg + "/com.EdS.DhizukuF.ui.activity.RequestPermissionActivity");
        cmd.add("--ei");
        cmd.add("uid");
        cmd.add(String.valueOf(Process.myUid()));
        cmd.add("--es");
        cmd.add("dish_token");
        cmd.add(TOKEN);
        if (run(cmd) == null) {
            System.err.println("dish: could not open the dialog automatically, "
                    + "enable this app in DhizukuF > App management.");
        }
    }

    private static String run(List<String> command) {
        try {
            java.lang.Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
            String out = readAll(process.getInputStream());
            process.waitFor();
            return out;
        } catch (Exception e) {
            return null;
        }
    }

    private static String readAll(InputStream stream) throws IOException {
        ByteArrayOutputStream sink = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = stream.read(buf)) != -1) sink.write(buf, 0, n);
        return new String(sink.toByteArray(), UTF8);
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
