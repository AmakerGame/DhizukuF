package com.EdS.DhizukuF.dish;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Interactive console and line tokenizer.
 * Local builtins never touch DhizukuF; everything else goes through DishConnection.
 */
final class DishShell {
    private static final int HISTORY_LIMIT = 100;

    private DishShell() {}

    static int interactive() throws IOException {
        boolean tty = "1".equals(System.getenv("DISH_TTY"));
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        if (tty) {
            System.err.println("dish " + DishProtocol.CLIENT_VERSION
                    + " - Device Owner console via DhizukuF. Type 'help', 'exit' to quit.");
        }

        int last = 0;
        List<String> history = new ArrayList<String>();

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

            if (line.equals("clear")) {
                System.out.print("\u001b[H\u001b[2J");
                System.out.flush();
                continue;
            }

            if (line.equals("history")) {
                printHistory(history);
                continue;
            }

            if (line.equals("reconnect")) {
                DishConnection.reset();
                if (DishConnection.connect() != null) {
                    if (tty) System.err.println("dish: reconnected");
                } else {
                    System.err.println("dish: reconnect failed");
                }
                continue;
            }

            if (line.equals("client-version") || line.equals("client_version")) {
                System.out.println("dish client " + DishProtocol.CLIENT_VERSION
                        + " protocol 1  sdk " + android.os.Build.VERSION.SDK_INT
                        + " pkg " + DishProtocol.appPackage());
                continue;
            }

            String expanded = expandHistory(line, history, tty);
            if (expanded == null) continue;
            line = expanded;

            addHistory(history, line);

            List<String> tokens;
            try {
                tokens = tokenize(line);
            } catch (IllegalArgumentException e) {
                System.err.println(e.getMessage());
                last = 1;
                continue;
            }
            if (tokens.isEmpty()) continue;

            List<List<String>> parts = splitOnSemicolon(tokens);
            for (int i = 0; i < parts.size(); i++) {
                List<String> part = parts.get(i);
                if (part.isEmpty()) continue;
                last = DishConnection.execute(part);
                if (last != 0 && i + 1 < parts.size()) {
                    System.err.println("dish: stopped at failed command (exit " + last + ")");
                    break;
                }
            }
        }
        return last;
    }

    private static void printHistory(List<String> history) {
        for (int i = 0; i < history.size(); i++) {
            System.out.println((i + 1) + "  " + history.get(i));
        }
    }

    private static void addHistory(List<String> history, String line) {
        if (!history.isEmpty() && history.get(history.size() - 1).equals(line)) {
            return;
        }
        history.add(line);
        if (history.size() > HISTORY_LIMIT) {
            history.remove(0);
        }
    }

    private static String expandHistory(String line, List<String> history, boolean tty) {
        if (line.equals("!!")) {
            if (history.isEmpty()) {
                System.err.println("dish: no previous command");
                return null;
            }
            String prev = history.get(history.size() - 1);
            if (tty) System.out.println(prev);
            return prev;
        }
        if (line.length() > 1 && line.charAt(0) == '!' && isDigits(line.substring(1))) {
            int n;
            try {
                n = Integer.parseInt(line.substring(1));
            } catch (NumberFormatException e) {
                System.err.println("dish: invalid history index");
                return null;
            }
            if (n < 1 || n > history.size()) {
                System.err.println("dish: history index out of range (1.." + history.size() + ")");
                return null;
            }
            String prev = history.get(n - 1);
            if (tty) System.out.println(prev);
            return prev;
        }
        return line;
    }

    private static boolean isDigits(String s) {
        if (s.isEmpty()) return false;
        for (int i = 0; i < s.length(); i++) {
            if (!Character.isDigit(s.charAt(i))) return false;
        }
        return true;
    }

    static List<List<String>> splitOnSemicolon(List<String> tokens) {
        List<List<String>> parts = new ArrayList<List<String>>();
        List<String> cur = new ArrayList<String>();
        for (String t : tokens) {
            if (t.equals(";")) {
                if (!cur.isEmpty()) {
                    parts.add(cur);
                    cur = new ArrayList<String>();
                }
            } else {
                cur.add(t);
            }
        }
        if (!cur.isEmpty()) parts.add(cur);
        return parts;
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
                    char n = line.charAt(i + 1);
                    // Shell-like: inside double quotes only ", \, $ and ` are special.
                    if (n == '"' || n == '\\' || n == '$' || n == '`') {
                        cur.append(n);
                        i++;
                    } else {
                        cur.append('\\');
                    }
                } else {
                    cur.append(c);
                }
            } else if (c == '\'' || c == '"') {
                quote = c;
                has = true;
            } else if (c == '\\' && i + 1 < line.length()) {
                cur.append(line.charAt(++i));
                has = true;
            } else if (c == ';') {
                if (has || cur.length() > 0) {
                    out.add(cur.toString());
                    cur.setLength(0);
                    has = false;
                }
                out.add(";");
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
        if (has || cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }
}
