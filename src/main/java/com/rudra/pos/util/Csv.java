package com.rudra.pos.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free CSV codec following the essentials of RFC 4180.
 *
 * <p>A field is quoted when it contains a comma, double-quote, CR or LF;
 * embedded double-quotes are escaped by doubling them. The parser is a small
 * state machine so values containing commas or newlines round-trip safely.</p>
 */
public final class Csv {

    private Csv() {
    }

    /** Encode a single record (list of fields) into one CSV line (no trailing newline). */
    public static String encodeRow(List<String> fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(encodeField(fields.get(i)));
        }
        return sb.toString();
    }

    private static String encodeField(String value) {
        if (value == null) {
            value = "";
        }
        boolean mustQuote = value.contains(",") || value.contains("\"")
                || value.contains("\n") || value.contains("\r");
        if (!mustQuote) {
            return value;
        }
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    /**
     * Parse the full text of a CSV file into rows of fields. Empty input
     * yields an empty list. A trailing newline does not create a blank row.
     */
    public static List<List<String>> parse(String text) {
        List<List<String>> rows = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return rows;
        }
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        boolean rowHasContent = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                    rowHasContent = true;
                } else if (c == ',') {
                    current.add(field.toString());
                    field.setLength(0);
                    rowHasContent = true;
                } else if (c == '\n' || c == '\r') {
                    // Treat \r\n as a single terminator.
                    if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    if (rowHasContent || field.length() > 0 || !current.isEmpty()) {
                        current.add(field.toString());
                        rows.add(current);
                    }
                    current = new ArrayList<>();
                    field.setLength(0);
                    rowHasContent = false;
                } else {
                    field.append(c);
                    rowHasContent = true;
                }
            }
        }
        // Flush the final field/row if the file did not end with a newline.
        if (rowHasContent || field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            rows.add(current);
        }
        return rows;
    }
}
