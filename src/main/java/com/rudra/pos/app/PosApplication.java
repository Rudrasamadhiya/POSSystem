package com.rudra.pos.app;

import com.rudra.pos.exception.PosException;
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
import com.rudra.pos.service.TopProduct;
import com.rudra.pos.util.Money;
import com.rudra.pos.util.ReceiptPrinter;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interactive command-line front end. Wires the services together and drives
 * the menus for store admins and cashiers. All business-rule violations bubble
 * up as {@link PosException} and are shown as friendly one-line messages.
 */
public class PosApplication {

    private final Console c;
    private final AuthService auth;
    private final InventoryService inventory;
    private final BillingService billing;
    private final ReportService reports;
    private final Database db;

    public PosApplication(Database db, Console console) {
        this.db = db;
        this.c = console;
        this.auth = new AuthService(db);
        this.inventory = new InventoryService(db);
        this.billing = new BillingService(db);
        this.reports = new ReportService(db);
    }

    public void run() {
        c.println("");
        c.println("########################################");
        c.println("#     MULTI-STORE POS  —  Java CLI      #");
        c.println("########################################");
        c.println("Demo login →  store: BHOPAL01 / admin123   |   cashier: ravi / cashier123");

        boolean running = true;
        while (running) {
            c.header("Main Menu");
            c.println("1) Store admin login");
            c.println("2) Cashier login");
            c.println("3) Register new store");
            c.println("0) Exit");
            switch (c.readLine("Choose: ")) {
                case "1":
                    adminLogin();
                    break;
                case "2":
                    cashierLogin();
                    break;
                case "3":
                    registerStore();
                    break;
                case "0":
                    running = false;
                    c.println("Goodbye.");
                    break;
                default:
                    c.println("Invalid choice.");
            }
        }
    }

    // ---------------------------------------------------------------- auth

    private void registerStore() {
        c.header("Register New Store");
        try {
            String code = c.readNonEmpty("Store code (login id): ");
            String name = c.readNonEmpty("Store name: ");
            String password = c.readNonEmpty("Admin password: ");
            String location = c.readLine("Location: ");
            String contact = c.readLine("Contact: ");
            Store store = auth.registerStore(code, name, password, location, contact);
            c.println("✓ Registered '" + store.getName() + "'. You can now log in as admin.");
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void adminLogin() {
        c.header("Store Admin Login");
        try {
            String code = c.readNonEmpty("Store code: ");
            String password = c.readNonEmpty("Password: ");
            Store store = auth.authenticateStore(code, password);
            c.println("✓ Welcome, " + store.getName());
            adminSession(store);
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void cashierLogin() {
        c.header("Cashier Login");
        try {
            String username = c.readNonEmpty("Username: ");
            String password = c.readNonEmpty("Password: ");
            User user = auth.authenticateUser(username, password);
            Store store = db.stores().findById(user.getStoreId())
                    .orElseThrow(() -> new PosException("Store not found for this user"));
            c.println("✓ Welcome, " + user.getUsername() + " (" + user.getRole().getLabel() + ")");
            billingSession(store, user);
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    // ------------------------------------------------------------- sessions

    private void adminSession(Store store) {
        boolean active = true;
        while (active) {
            c.header(store.getName() + " — Admin");
            c.println("1) Dashboard");
            c.println("2) Products & inventory");
            c.println("3) Staff / users");
            c.println("4) New sale (billing)");
            c.println("5) Reports");
            c.println("0) Logout");
            switch (c.readLine("Choose: ")) {
                case "1": dashboard(store); break;
                case "2": productsMenu(store); break;
                case "3": usersMenu(store); break;
                case "4": billingSession(store, null); break;
                case "5": reportsMenu(store); break;
                case "0": active = false; break;
                default: c.println("Invalid choice.");
            }
        }
    }

    private void dashboard(Store store) {
        c.header("Dashboard — " + store.getName());
        Long sid = store.getId();
        c.printf("Total revenue : %s%n", Money.format(reports.totalSales(sid)));
        c.printf("Today's sales : %s%n", Money.format(reports.todaySales(sid)));
        c.printf("Transactions  : %d%n", reports.transactionCount(sid));
        c.printf("Products      : %d%n", inventory.listProducts(sid).size());
        c.printf("Staff accounts: %d%n", db.users().findByStore(sid).size());
        List<Product> low = inventory.lowStockProducts(sid);
        if (!low.isEmpty()) {
            c.println("⚠ Low stock (" + low.size() + "):");
            for (Product p : low) {
                c.printf("   - %s: %d left (reorder at %d)%n",
                        p.getName(), p.getStock(), p.getReorderLevel());
            }
        }
    }

    // ------------------------------------------------------------- products

    private void productsMenu(Store store) {
        boolean active = true;
        while (active) {
            c.header("Products — " + store.getName());
            c.println("1) List all   2) Search   3) Add   4) Update   5) Delete   6) Restock   7) Low stock   0) Back");
            switch (c.readLine("Choose: ")) {
                case "1": printProducts(inventory.listProducts(store.getId())); break;
                case "2": printProducts(inventory.search(store.getId(), c.readLine("Search: "))); break;
                case "3": addProduct(store); break;
                case "4": updateProduct(store); break;
                case "5": deleteProduct(store); break;
                case "6": restock(store); break;
                case "7": printProducts(inventory.lowStockProducts(store.getId())); break;
                case "0": active = false; break;
                default: c.println("Invalid choice.");
            }
        }
    }

    private void printProducts(List<Product> products) {
        if (products.isEmpty()) {
            c.println("(no products)");
            return;
        }
        c.printf("%-5s %-12s %-26s %10s %7s %-14s%n",
                "ID", "Barcode", "Name", "Price", "Stock", "Category");
        for (Product p : products) {
            String flag = p.isLowStock() ? " ⚠" : "";
            c.printf("%-5d %-12s %-26s %10s %7d %-14s%s%n",
                    p.getId(), p.getBarcode(), trim(p.getName(), 26),
                    Money.format(p.getPrice()), p.getStock(),
                    p.getCategory() == null ? "" : p.getCategory(), flag);
        }
    }

    private void addProduct(Store store) {
        try {
            String barcode = c.readNonEmpty("Barcode: ");
            String name = c.readNonEmpty("Name: ");
            BigDecimal price = c.readBigDecimal("Price: ", BigDecimal.ZERO);
            int stock = c.readInt("Initial stock: ", 0);
            String category = c.readLine("Category: ");
            int reorder = c.readInt("Reorder level [5]: ", 5);
            Product p = inventory.addProduct(store.getId(), barcode, name, price, stock, category, reorder);
            c.println("✓ Added product #" + p.getId());
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void updateProduct(Store store) {
        try {
            int id = c.readInt("Product id to update: ", -1);
            Product existing = inventory.listProducts(store.getId()).stream()
                    .filter(p -> p.getId() == (long) id).findFirst()
                    .orElse(null);
            if (existing == null) {
                c.println("✗ No such product in this store.");
                return;
            }
            String name = c.readLine("Name [" + existing.getName() + "]: ");
            if (name.isEmpty()) name = existing.getName();
            BigDecimal price = c.readBigDecimal("Price [" + existing.getPrice() + "]: ", existing.getPrice());
            String category = c.readLine("Category [" + existing.getCategory() + "]: ");
            if (category.isEmpty()) category = existing.getCategory();
            int reorder = c.readInt("Reorder level [" + existing.getReorderLevel() + "]: ", existing.getReorderLevel());
            inventory.updateProduct(existing.getId(), name, price, category, reorder);
            c.println("✓ Updated.");
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void deleteProduct(Store store) {
        try {
            int id = c.readInt("Product id to delete: ", -1);
            boolean ownedByStore = inventory.listProducts(store.getId()).stream()
                    .anyMatch(p -> p.getId() == (long) id);
            if (!ownedByStore) {
                c.println("✗ No such product in this store.");
                return;
            }
            if (c.confirm("Delete product #" + id + "?")) {
                inventory.deleteProduct((long) id);
                c.println("✓ Deleted.");
            }
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void restock(Store store) {
        try {
            int id = c.readInt("Product id: ", -1);
            boolean ownedByStore = inventory.listProducts(store.getId()).stream()
                    .anyMatch(p -> p.getId() == (long) id);
            if (!ownedByStore) {
                c.println("✗ No such product in this store.");
                return;
            }
            int delta = c.readInt("Units to add (negative to remove): ", 0);
            Product p = inventory.adjustStock((long) id, delta);
            c.println("✓ New stock for " + p.getName() + ": " + p.getStock());
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- users

    private void usersMenu(Store store) {
        boolean active = true;
        while (active) {
            c.header("Staff — " + store.getName());
            c.println("1) List   2) Add   3) Activate/Deactivate   0) Back");
            switch (c.readLine("Choose: ")) {
                case "1": printUsers(store); break;
                case "2": addUser(store); break;
                case "3": toggleUser(store); break;
                case "0": active = false; break;
                default: c.println("Invalid choice.");
            }
        }
    }

    private void printUsers(Store store) {
        List<User> users = db.users().findByStore(store.getId());
        if (users.isEmpty()) {
            c.println("(no staff yet)");
            return;
        }
        c.printf("%-5s %-16s %-10s %-8s%n", "ID", "Username", "Role", "Active");
        for (User u : users) {
            c.printf("%-5d %-16s %-10s %-8s%n",
                    u.getId(), u.getUsername(), u.getRole().getLabel(), u.isActive() ? "yes" : "no");
        }
    }

    private void addUser(Store store) {
        try {
            String username = c.readNonEmpty("Username: ");
            String password = c.readNonEmpty("Password: ");
            c.println("Roles: 1) Cashier  2) Manager  3) Admin");
            Role role;
            switch (c.readLine("Role [1]: ")) {
                case "2": role = Role.MANAGER; break;
                case "3": role = Role.ADMIN; break;
                default: role = Role.CASHIER;
            }
            User u = auth.createUser(store.getId(), username, password, role);
            c.println("✓ Created user #" + u.getId());
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void toggleUser(Store store) {
        int id = c.readInt("User id: ", -1);
        User u = db.users().findById((long) id).orElse(null);
        if (u == null || !u.getStoreId().equals(store.getId())) {
            c.println("✗ No such user in this store.");
            return;
        }
        auth.setUserActive(u.getId(), !u.isActive());
        c.println("✓ " + u.getUsername() + " is now " + (!u.isActive() ? "active" : "inactive"));
    }

    // -------------------------------------------------------------- billing

    private void billingSession(Store store, User cashier) {
        c.header("New Sale — " + store.getName());
        Cart cart = new Cart();
        boolean selling = true;
        while (selling) {
            c.println("Scan/enter barcode, or: 'cart', 'remove', 'pay', 'cancel'");
            String input = c.readLine("> ");
            switch (input.toLowerCase()) {
                case "cart":
                    printCart(cart);
                    break;
                case "remove":
                    removeFromCart(store, cart);
                    break;
                case "pay":
                    if (checkout(store, cashier, cart)) {
                        selling = false;
                    }
                    break;
                case "cancel":
                    selling = false;
                    c.println("Sale cancelled.");
                    break;
                default:
                    addByBarcode(store, cart, input);
            }
        }
    }

    private void addByBarcode(Store store, Cart cart, String barcode) {
        if (barcode.isEmpty()) {
            return;
        }
        try {
            Product p = inventory.findByBarcode(store.getId(), barcode);
            int qty = c.readInt("Quantity [1]: ", 1);
            if (qty <= 0) {
                c.println("Quantity must be positive.");
                return;
            }
            cart.add(p, qty);
            c.printf("✓ %s x%d added. Cart total: %s%n", p.getName(), qty, Money.format(cart.getTotal()));
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void removeFromCart(Store store, Cart cart) {
        printCart(cart);
        String barcode = c.readLine("Barcode to remove: ");
        try {
            Product p = inventory.findByBarcode(store.getId(), barcode);
            cart.removeProduct(p.getId());
            c.println("✓ Removed. Cart total: " + Money.format(cart.getTotal()));
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
        }
    }

    private void printCart(Cart cart) {
        if (cart.isEmpty()) {
            c.println("(cart is empty)");
            return;
        }
        c.printf("%-26s %5s %12s%n", "Item", "Qty", "Total");
        cart.getItems().forEach(i ->
                c.printf("%-26s %5d %12s%n", trim(i.getProduct().getName(), 26),
                        i.getQuantity(), Money.format(i.getLineTotal())));
        c.printf("%-26s %5s %12s%n", "TOTAL", "", Money.format(cart.getTotal()));
    }

    private boolean checkout(Store store, User cashier, Cart cart) {
        if (cart.isEmpty()) {
            c.println("Cart is empty — nothing to pay.");
            return false;
        }
        printCart(cart);
        c.println("Payment: 1) Cash  2) Card  3) UPI");
        PaymentMethod method;
        switch (c.readLine("Method [1]: ")) {
            case "2": method = PaymentMethod.CARD; break;
            case "3": method = PaymentMethod.UPI; break;
            default: method = PaymentMethod.CASH;
        }
        String customer = c.readLine("Customer name (optional): ");
        try {
            Transaction tx = billing.checkout(store, cashier, cart, method, customer);
            c.println("");
            c.println(ReceiptPrinter.render(store, tx));
            List<Product> low = inventory.lowStockProducts(store.getId());
            if (!low.isEmpty()) {
                c.println("⚠ " + low.size() + " item(s) now at/below reorder level.");
            }
            return true;
        } catch (PosException e) {
            c.println("✗ " + e.getMessage());
            return false;
        }
    }

    // --------------------------------------------------------------- reports

    private void reportsMenu(Store store) {
        boolean active = true;
        while (active) {
            c.header("Reports — " + store.getName());
            c.println("1) Summary   2) Daily sales   3) Top products   4) Payment mix   5) Export CSV   0) Back");
            switch (c.readLine("Choose: ")) {
                case "1": dashboard(store); break;
                case "2": dailySales(store); break;
                case "3": topProducts(store); break;
                case "4": paymentMix(store); break;
                case "5": exportCsv(store); break;
                case "0": active = false; break;
                default: c.println("Invalid choice.");
            }
        }
    }

    private void dailySales(Store store) {
        c.header("Daily Sales");
        Map<LocalDate, BigDecimal> daily = reports.dailySales(store.getId());
        if (daily.isEmpty()) {
            c.println("(no sales yet)");
            return;
        }
        daily.forEach((day, total) -> c.printf("%-12s %12s%n", day, Money.format(total)));
    }

    private void topProducts(Store store) {
        c.header("Top Products");
        List<TopProduct> top = reports.topProducts(store.getId(), 10);
        if (top.isEmpty()) {
            c.println("(no sales yet)");
            return;
        }
        for (TopProduct t : top) {
            c.printf("%-28s %4d sold%n", trim(t.getName(), 28), t.getQuantitySold());
        }
    }

    private void paymentMix(Store store) {
        c.header("Payment Mix");
        Map<PaymentMethod, BigDecimal> mix = reports.paymentBreakdown(store.getId());
        if (mix.isEmpty()) {
            c.println("(no sales yet)");
            return;
        }
        mix.forEach((method, total) -> c.printf("%-8s %12s%n", method.getLabel(), Money.format(total)));
    }

    private void exportCsv(Store store) {
        String fileName = "sales_" + store.getCode() + ".csv";
        Path target = Paths.get(db.getDataDir().toString(), fileName);
        reports.exportSalesCsv(store.getId(), target);
        c.println("✓ Exported sales ledger to " + target.toAbsolutePath());
    }

    private static String trim(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max - 1) + "…";
    }
}
