package com.rudra.pos.app;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.Scanner;

/** Thin wrapper around a {@link Scanner}/{@link PrintStream} for tidy CLI I/O. */
public class Console {

    private final Scanner in;
    private final PrintStream out;

    public Console(Scanner in, PrintStream out) {
        this.in = in;
        this.out = out;
    }

    public void println(String s) {
        out.println(s);
    }

    public void println() {
        out.println();
    }

    public void printf(String fmt, Object... args) {
        out.printf(fmt, args);
    }

    public void header(String title) {
        out.println();
        out.println("==== " + title + " ====");
    }

    public String readLine(String prompt) {
        out.print(prompt);
        out.flush();
        return in.hasNextLine() ? in.nextLine().trim() : "";
    }

    public String readNonEmpty(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (!value.isEmpty()) {
                return value;
            }
            out.println("  (value cannot be empty)");
        }
    }

    /** Read an int, returning {@code defaultValue} on blank or invalid input. */
    public int readInt(String prompt, int defaultValue) {
        String value = readLine(prompt);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            out.println("  (not a number, using " + defaultValue + ")");
            return defaultValue;
        }
    }

    public BigDecimal readBigDecimal(String prompt, BigDecimal defaultValue) {
        String value = readLine(prompt);
        if (value.isEmpty()) {
            return defaultValue;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            out.println("  (not a valid amount, using " + defaultValue + ")");
            return defaultValue;
        }
    }

    public boolean confirm(String prompt) {
        String value = readLine(prompt + " [y/N]: ").toLowerCase();
        return value.equals("y") || value.equals("yes");
    }
}
