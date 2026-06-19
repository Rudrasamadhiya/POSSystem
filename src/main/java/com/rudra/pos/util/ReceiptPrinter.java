package com.rudra.pos.util;

import com.rudra.pos.model.Store;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.TransactionLine;

import java.time.format.DateTimeFormatter;

/** Renders a completed {@link Transaction} as a plain-text till receipt. */
public final class ReceiptPrinter {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private static final int WIDTH = 40;

    private ReceiptPrinter() {
    }

    public static String render(Store store, Transaction tx) {
        StringBuilder sb = new StringBuilder();
        line(sb, '=');
        center(sb, store.getName());
        if (store.getLocation() != null && !store.getLocation().isEmpty()) {
            center(sb, store.getLocation());
        }
        line(sb, '=');
        sb.append("Receipt #").append(tx.getId()).append('\n');
        sb.append(tx.getCreatedAt().format(TS)).append('\n');
        if (tx.getCustomerName() != null && !tx.getCustomerName().isEmpty()) {
            sb.append("Customer: ").append(tx.getCustomerName()).append('\n');
        }
        line(sb, '-');
        sb.append(String.format("%-20s %4s %12s%n", "Item", "Qty", "Amount"));
        line(sb, '-');
        for (TransactionLine l : tx.getLines()) {
            String name = l.getProductName();
            if (name.length() > 20) {
                name = name.substring(0, 19) + "…";
            }
            sb.append(String.format("%-20s %4d %12s%n",
                    name, l.getQuantity(), Money.format(l.getLineTotal())));
        }
        line(sb, '-');
        sb.append(String.format("%-20s %4d %12s%n",
                "TOTAL", tx.getItemCount(), Money.format(tx.getTotal())));
        sb.append("Paid via: ").append(tx.getPaymentMethod().getLabel()).append('\n');
        line(sb, '=');
        center(sb, "Thank you, visit again!");
        line(sb, '=');
        return sb.toString();
    }

    private static void line(StringBuilder sb, char c) {
        for (int i = 0; i < WIDTH; i++) {
            sb.append(c);
        }
        sb.append('\n');
    }

    private static void center(StringBuilder sb, String text) {
        if (text.length() >= WIDTH) {
            sb.append(text, 0, WIDTH).append('\n');
            return;
        }
        int pad = (WIDTH - text.length()) / 2;
        for (int i = 0; i < pad; i++) {
            sb.append(' ');
        }
        sb.append(text).append('\n');
    }
}
