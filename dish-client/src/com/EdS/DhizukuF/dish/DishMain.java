package com.EdS.DhizukuF.dish;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * dish client (like rish for Shizuku). Started by the "dish" script via app_process.
 * Pure Java, no dependencies - keeps the dex small.
 *
 * Usage:
 *   dish                  interactive console
 *   dish help             run a Device Owner command
 *   dish -c "status"      one line (quotes optional)
 *   dish -c ""            empty command, exit 0
 *   dish --version        client version only (no connection)
 *   dish --help           this help (no connection)
 *
 * Env: DISH_PACKAGE, DISH_TTY, DISH_DEBUG, DISH_CONNECT_ATTEMPTS,
 *      DISH_CONNECT_TIMEOUT, DISH_SKIP_CHECK
 */
public final class DishMain {
    private DishMain() {}

    public static void main(String[] args) {
        int code;
        try {
            code = run(args);
        } catch (Throwable e) {
            System.err.println("dish: " + e);
            if (DishProtocol.debug()) {
                e.printStackTrace(System.err);
            }
            code = 1;
        }
        System.out.flush();
        System.err.flush();
        System.exit(code);
    }

    static int run(String[] args) throws IOException {
        if (args.length == 0) {
            return DishShell.interactive();
        }

        if (isHelpFlag(args[0])) {
            printUsage();
            return 0;
        }
        // --version / -V always; short -v only when it is the sole argument
        // (so "dish -v status" is not swallowed as a version request).
        if (isVersionFlag(args[0]) && (args[0].startsWith("--") || args[0].equals("-V") || args.length == 1)) {
            System.out.println("dish client " + DishProtocol.CLIENT_VERSION
                    + " protocol 1");
            System.out.println("sdk " + android.os.Build.VERSION.SDK_INT
                    + " package " + DishProtocol.appPackage());
            return 0;
        }

        if (args[0].equals("-c") || args[0].equals("--command")) {
            return runCommandLine(args, 1);
        }

        if (args[0].equals("--")) {
            if (args.length == 1) return 0;
            return runArgList(args, 1);
        }

        return runArgList(args, 0);
    }

    private static boolean isHelpFlag(String a) {
        return a.equals("-h") || a.equals("--help") || a.equals("-?");
    }

    private static boolean isVersionFlag(String a) {
        return a.equals("-V") || a.equals("--version") || a.equals("-v");
    }

    /**
     * -c mode: join remaining args into one line, tokenize, run.
     * dish -c           -> exit 0
     * dish -c ""        -> exit 0
     * dish -c status    -> execute status
     * dish -c "hide x"  -> execute hide x
     * dish -c 'a ; b'   -> a then b (stop on first failure)
     */
    private static int runCommandLine(String[] args, int from) {
        if (from >= args.length) {
            return 0;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = from; i < args.length; i++) {
            if (i > from) sb.append(' ');
            sb.append(args[i]);
        }
        String line = sb.toString().trim();
        if (line.isEmpty()) {
            return 0;
        }
        try {
            List<String> tokens = DishShell.tokenize(line);
            if (tokens.isEmpty()) return 0;
            return runTokenGroups(tokens);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return 1;
        }
    }

    private static int runArgList(String[] args, int from) {
        List<String> list = new ArrayList<String>(args.length - from);
        for (int i = from; i < args.length; i++) {
            list.add(args[i]);
        }
        if (list.isEmpty()) return 0;
        return runTokenGroups(list);
    }

    private static int runTokenGroups(List<String> tokens) {
        List<List<String>> parts = DishShell.splitOnSemicolon(tokens);
        int last = 0;
        for (int i = 0; i < parts.size(); i++) {
            List<String> part = parts.get(i);
            if (part.isEmpty()) continue;
            last = DishConnection.execute(part);
            if (last != 0 && i + 1 < parts.size()) {
                System.err.println("dish: stopped at failed command (exit " + last + ")");
                break;
            }
        }
        return last;
    }

    private static void printUsage() {
        System.out.println("dish " + DishProtocol.CLIENT_VERSION
                + " - Device Owner console for DhizukuF");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  dish                    interactive console");
        System.out.println("  dish COMMAND [ARGS...]  run one Device Owner command");
        System.out.println("  dish -c \"LINE\"          run a command line (quotes optional)");
        System.out.println("  dish -c \"\"              empty line, exit 0");
        System.out.println("  dish -c \"a ; b\"         run a, then b (stop on failure)");
        System.out.println("  dish --version          client version (no connection)");
        System.out.println("  dish --help             this help (no connection)");
        System.out.println();
        System.out.println("Interactive builtins:");
        System.out.println("  exit, quit, clear, history, !!, !n, reconnect, client-version");
        System.out.println();
        System.out.println("Environment:");
        System.out.println("  DISH_PACKAGE            app package (default com.EdS.DhizukuF)");
        System.out.println("  DISH_TTY=1              show prompt and banners");
        System.out.println("  DISH_DEBUG=1            verbose connect / broadcast log");
        System.out.println("  DISH_CONNECT_ATTEMPTS   handshake tries (default 3, max 8)");
        System.out.println("  DISH_CONNECT_TIMEOUT    first try wait seconds (default 8)");
        System.out.println("  DISH_SKIP_CHECK=1       skip access ping in the launcher script");
        System.out.println();
        System.out.println("Device Owner commands: run  dish help  after DhizukuF is activated.");
    }
}
