package com.rudra.pos.app;

import com.rudra.pos.exception.PosException;
import com.rudra.pos.model.Cart;
import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Product;
import com.rudra.pos.model.Store;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.seed.DemoData;
import com.rudra.pos.service.BillingService;
import com.rudra.pos.service.InventoryService;
import com.rudra.pos.service.ReportService;
import com.rudra.pos.service.TopProduct;
import com.rudra.pos.util.Money;
import com.rudra.pos.util.ReceiptPrinter;

import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Runs a scripted, non-interactive walk-through of the whole system (seed →
 * sell → report) against a throwaway temp directory. Used by CI to prove the
 * app runs end to end, and handy for a quick screenshot of real output.
 */
public final class DemoRunner {

    private DemoRunner() {
    }

    public static void run(PrintStream out) {
        try {
            Database db = new Database(Files.createTempDirectory("pos-demo"));
            DemoData.seed(db);

            InventoryService inventory = new InventoryService(db);
            BillingService billing = new BillingService(db);
            ReportService reports = new ReportService(db);

            Store store = db.stores().findByCode("BHOPAL01").orElseThrow(
                    () -> new PosException("demo store missing"));
            Long sid = store.getId();

            out.println("=== SEEDED DEMO STORE: " + store.getName() + " ===");
            out.println("Products in catalogue : " + inventory.listProducts(sid).size());
            out.println("Historical revenue    : " + Money.format(reports.totalSales(sid)));
            out.println("Transactions on record: " + reports.transactionCount(sid));

            out.println("\n=== LIVE SALE ===");
            Product atta = inventory.findByBarcode(sid, "8901001");
            Product milk = inventory.findByBarcode(sid, "8901004");
            out.println("Milk stock before sale: " + milk.getStock());

            Cart cart = new Cart();
            cart.add(atta, 1);
            cart.add(milk, 6);
            Transaction tx = billing.checkout(store, null, cart, PaymentMethod.CARD, "Demo Customer");
            out.println(ReceiptPrinter.render(store, tx));
            out.println("Milk stock after sale : "
                    + inventory.findByBarcode(sid, "8901004").getStock() + "  (auto-synced)");

            out.println("\n=== TOP PRODUCTS ===");
            for (TopProduct t : reports.topProducts(sid, 5)) {
                out.printf("%-28s %3d sold%n", t.getName(), t.getQuantitySold());
            }

            out.println("\n=== DAILY SALES ===");
            for (Map.Entry<LocalDate, BigDecimal> e : reports.dailySales(sid).entrySet()) {
                out.printf("%-12s %s%n", e.getKey(), Money.format(e.getValue()));
            }

            out.println("\n=== PAYMENT MIX ===");
            reports.paymentBreakdown(sid).forEach(
                    (m, amt) -> out.printf("%-6s %s%n", m.getLabel(), Money.format(amt)));

            out.println("\nDemo complete — all subsystems exercised successfully.");
        } catch (PosException e) {
            throw new IllegalStateException("Demo failed: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
