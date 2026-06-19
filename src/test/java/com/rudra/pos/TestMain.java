package com.rudra.pos;

import com.rudra.pos.exception.AuthenticationException;
import com.rudra.pos.exception.DuplicateEntityException;
import com.rudra.pos.exception.InsufficientStockException;
import com.rudra.pos.exception.ValidationException;
import com.rudra.pos.model.Cart;
import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Product;
import com.rudra.pos.model.Role;
import com.rudra.pos.model.Store;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.User;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.service.AuthService;
import com.rudra.pos.service.BillingService;
import com.rudra.pos.service.InventoryService;
import com.rudra.pos.service.ReportService;
import com.rudra.pos.util.Csv;
import com.rudra.pos.util.PasswordHasher;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Hand-rolled test suite covering the persistence layer, security, inventory
 * rules, the transactional billing engine and reporting. Exit code is non-zero
 * if any assertion fails, so CI fails the build on a regression.
 */
public class TestMain {

    public static void main(String[] args) throws Exception {
        Assert a = new Assert();

        System.out.println("[CSV codec]");
        csvTests(a);

        System.out.println("[Password hashing]");
        passwordTests(a);

        System.out.println("[Auth service]");
        authTests(a);

        System.out.println("[Inventory service]");
        inventoryTests(a);

        System.out.println("[Billing engine]");
        billingTests(a);

        System.out.println("[Persistence reload]");
        persistenceTests(a);

        System.out.println("[Reports]");
        reportTests(a);

        System.out.println();
        System.out.printf("RESULT: %d passed, %d failed%n", a.passed(), a.failed());
        if (!a.allPassed()) {
            System.exit(1);
        }
    }

    private static Database freshDb() throws Exception {
        Path dir = Files.createTempDirectory("pos-test");
        return new Database(dir);
    }

    // --------------------------------------------------------------- CSV

    private static void csvTests(Assert a) {
        List<String> fields = Arrays.asList("a", "b,c", "d\"e", "f\ng", "");
        String encoded = Csv.encodeRow(fields);
        List<List<String>> parsed = Csv.parse(encoded + "\n");
        a.assertEquals("csv round-trips one row", 1, parsed.size());
        a.assertEquals("csv preserves tricky fields", fields, parsed.get(0));
        a.assertEquals("csv empty input -> no rows", 0, Csv.parse("").size());
    }

    // ----------------------------------------------------------- passwords

    private static void passwordTests(Assert a) {
        String hash = PasswordHasher.hash("s3cret");
        a.check("correct password verifies", PasswordHasher.verify("s3cret", hash));
        a.check("wrong password rejected", !PasswordHasher.verify("nope", hash));
        a.check("salting makes hashes unique",
                !PasswordHasher.hash("same").equals(PasswordHasher.hash("same")));
    }

    // --------------------------------------------------------------- auth

    private static void authTests(Assert a) throws Exception {
        Database db = freshDb();
        AuthService auth = new AuthService(db);

        Store store = auth.registerStore("S1", "Store One", "pw123", "Bhopal", "");
        a.check("store gets an id", store.getId() != null);

        a.assertThrows("duplicate store code rejected", DuplicateEntityException.class,
                () -> auth.registerStore("S1", "Dup", "x", "", ""));
        a.assertThrows("blank password rejected", ValidationException.class,
                () -> auth.registerStore("S2", "Store Two", "", "", ""));

        a.check("admin authenticates",
                auth.authenticateStore("S1", "pw123").getId().equals(store.getId()));
        a.assertThrows("bad admin password rejected", AuthenticationException.class,
                () -> auth.authenticateStore("S1", "wrong"));

        User cashier = auth.createUser(store.getId(), "ravi", "cash1", Role.CASHIER);
        a.check("cashier authenticates",
                auth.authenticateUser("ravi", "cash1").getId().equals(cashier.getId()));
        a.assertThrows("duplicate username rejected", DuplicateEntityException.class,
                () -> auth.createUser(store.getId(), "ravi", "y", Role.CASHIER));

        auth.setUserActive(cashier.getId(), false);
        a.assertThrows("deactivated user blocked", AuthenticationException.class,
                () -> auth.authenticateUser("ravi", "cash1"));
    }

    // ----------------------------------------------------------- inventory

    private static void inventoryTests(Assert a) throws Exception {
        Database db = freshDb();
        AuthService auth = new AuthService(db);
        InventoryService inv = new InventoryService(db);
        Store store = auth.registerStore("S1", "Store", "pw", "", "");
        Long sid = store.getId();

        Product p = inv.addProduct(sid, "111", "Milk", new BigDecimal("33.00"), 10, "Dairy", 5);
        a.check("product saved", p.getId() != null);
        a.assertThrows("duplicate barcode rejected", DuplicateEntityException.class,
                () -> inv.addProduct(sid, "111", "Milk2", new BigDecimal("1"), 1, "", 0));

        a.assertEquals("search by name finds product", 1, inv.search(sid, "milk").size());
        a.assertEquals("search miss returns none", 0, inv.search(sid, "zzz").size());

        inv.adjustStock(p.getId(), -7);
        a.assertEquals("stock adjusted to 3", 3,
                inv.findByBarcode(sid, "111").getStock());
        a.check("product now low stock", inv.findByBarcode(sid, "111").isLowStock());
        a.assertEquals("low-stock list has 1", 1, inv.lowStockProducts(sid).size());
        a.assertThrows("over-removing stock rejected", ValidationException.class,
                () -> inv.adjustStock(p.getId(), -100));

        inv.updateProduct(p.getId(), "Toned Milk", new BigDecimal("35.00"), "Dairy", 5);
        a.assertEquals("price updated", 0,
                new BigDecimal("35.00").compareTo(inv.findByBarcode(sid, "111").getPrice()));
    }

    // ------------------------------------------------------------ billing

    private static void billingTests(Assert a) throws Exception {
        Database db = freshDb();
        AuthService auth = new AuthService(db);
        InventoryService inv = new InventoryService(db);
        BillingService billing = new BillingService(db);

        Store store = auth.registerStore("S1", "Store", "pw", "", "");
        Long sid = store.getId();
        Product soap = inv.addProduct(sid, "200", "Soap", new BigDecimal("100.00"), 5, "Care", 2);

        Cart cart = new Cart();
        cart.add(soap, 2);
        Transaction tx = billing.checkout(store, null, cart, PaymentMethod.CASH, "Amit");
        a.assertEquals("total is 200", 0, new BigDecimal("200.00").compareTo(tx.getTotal()));
        a.assertEquals("stock reduced to 3", 3, inv.findByBarcode(sid, "200").getStock());
        a.assertEquals("transaction has 1 line", 1, tx.getLines().size());

        Cart tooBig = new Cart();
        tooBig.add(inv.findByBarcode(sid, "200"), 10);
        a.assertThrows("oversell rejected", InsufficientStockException.class,
                () -> billing.checkout(store, null, tooBig, PaymentMethod.UPI, ""));
        a.assertEquals("stock unchanged after failed sale", 3,
                inv.findByBarcode(sid, "200").getStock());

        a.assertThrows("empty cart rejected", ValidationException.class,
                () -> billing.checkout(store, null, new Cart(), PaymentMethod.CASH, ""));
    }

    // --------------------------------------------------------- persistence

    private static void persistenceTests(Assert a) throws Exception {
        Path dir = Files.createTempDirectory("pos-reload");
        Long sid;
        {
            Database db = new Database(dir);
            AuthService auth = new AuthService(db);
            InventoryService inv = new InventoryService(db);
            BillingService billing = new BillingService(db);
            Store store = auth.registerStore("S1", "Store", "pw", "", "");
            sid = store.getId();
            Product p = inv.addProduct(sid, "300", "Pen", new BigDecimal("10.00"), 20, "Stationery", 5);
            Cart cart = new Cart();
            cart.add(p, 4);
            billing.checkout(store, null, cart, PaymentMethod.CARD, "Reload");
        }
        // Re-open from the same directory: state must survive a restart.
        Database reopened = new Database(dir);
        a.assertEquals("store reloaded", 1, reopened.stores().count());
        a.assertEquals("stock persisted (20-4=16)", 16,
                reopened.products().findByBarcode(sid, "300").get().getStock());
        a.assertEquals("transaction persisted", 1, reopened.transactions().count());
        a.assertEquals("transaction line reloaded", 1,
                reopened.transactions().findAll().get(0).getLines().size());
    }

    // ------------------------------------------------------------- reports

    private static void reportTests(Assert a) throws Exception {
        Database db = freshDb();
        AuthService auth = new AuthService(db);
        InventoryService inv = new InventoryService(db);
        BillingService billing = new BillingService(db);
        ReportService reports = new ReportService(db);

        Store store = auth.registerStore("S1", "Store", "pw", "", "");
        Long sid = store.getId();
        Product a1 = inv.addProduct(sid, "401", "Chips", new BigDecimal("20.00"), 50, "Snacks", 5);
        Product a2 = inv.addProduct(sid, "402", "Cola", new BigDecimal("40.00"), 50, "Drinks", 5);

        Cart c1 = new Cart();
        c1.add(a1, 3);
        c1.add(a2, 1);
        billing.checkout(store, null, c1, PaymentMethod.CASH, "");

        a.assertEquals("total sales 100", 0, new BigDecimal("100.00").compareTo(reports.totalSales(sid)));
        a.check("today sales recorded", reports.todaySales(sid).signum() > 0);
        a.check("top products non-empty", !reports.topProducts(sid, 5).isEmpty());
        a.check("payment mix has CASH", reports.paymentBreakdown(sid).containsKey(PaymentMethod.CASH));

        Path csv = Files.createTempDirectory("pos-csv").resolve("sales.csv");
        reports.exportSalesCsv(sid, csv);
        a.check("CSV export created", Files.exists(csv));
    }
}
