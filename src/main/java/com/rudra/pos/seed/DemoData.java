package com.rudra.pos.seed;

import com.rudra.pos.exception.PosException;
import com.rudra.pos.model.Cart;
import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Product;
import com.rudra.pos.model.Role;
import com.rudra.pos.model.Store;
import com.rudra.pos.model.User;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.service.AuthService;
import com.rudra.pos.service.BillingService;
import com.rudra.pos.service.InventoryService;

import java.math.BigDecimal;

/**
 * Seeds a realistic demo store with staff, a product catalogue and a handful of
 * historical sales — so a first-time user (or a recruiter) sees a populated
 * dashboard instead of empty tables. Idempotent: does nothing if data exists.
 *
 * <p>Demo credentials:
 * <pre>
 *   Store code : BHOPAL01     admin password : admin123
 *   Cashier    : ravi         password       : cashier123
 *   Manager    : neha         password       : manager123
 * </pre>
 */
public final class DemoData {

    private DemoData() {
    }

    public static void seedIfEmpty(Database db) throws PosException {
        if (!db.isEmpty()) {
            return;
        }
        seed(db);
    }

    public static void seed(Database db) throws PosException {
        AuthService auth = new AuthService(db);
        InventoryService inventory = new InventoryService(db);
        BillingService billing = new BillingService(db);

        Store store = auth.registerStore("BHOPAL01", "Sanskaar Mart", "admin123",
                "MP Nagar, Bhopal", "+91 90000 00000");
        Long sid = store.getId();

        auth.createUser(sid, "ravi", "cashier123", Role.CASHIER);
        User cashier = auth.authenticateUser("ravi", "cashier123");
        auth.createUser(sid, "neha", "manager123", Role.MANAGER);

        Product[] p = new Product[]{
                inventory.addProduct(sid, "8901001", "Aashirvaad Atta 5kg", bd("265.00"), 40, "Staples", 8),
                inventory.addProduct(sid, "8901002", "Tata Salt 1kg", bd("28.00"), 120, "Staples", 20),
                inventory.addProduct(sid, "8901003", "Fortune Oil 1L", bd("145.00"), 60, "Staples", 12),
                inventory.addProduct(sid, "8901004", "Amul Milk 500ml", bd("33.00"), 90, "Dairy", 24),
                inventory.addProduct(sid, "8901005", "Amul Butter 100g", bd("56.00"), 50, "Dairy", 10),
                inventory.addProduct(sid, "8901006", "Maggi Noodles 70g", bd("14.00"), 200, "Snacks", 30),
                inventory.addProduct(sid, "8901007", "Lays Classic 52g", bd("20.00"), 150, "Snacks", 25),
                inventory.addProduct(sid, "8901008", "Coca-Cola 750ml", bd("40.00"), 80, "Beverages", 15),
                inventory.addProduct(sid, "8901009", "Tata Tea Gold 250g", bd("130.00"), 35, "Beverages", 8),
                inventory.addProduct(sid, "8901010", "Colgate MaxFresh 150g", bd("99.00"), 45, "Personal Care", 10),
                inventory.addProduct(sid, "8901011", "Dettol Soap 125g", bd("45.00"), 70, "Personal Care", 15),
                inventory.addProduct(sid, "8901012", "Surf Excel 1kg", bd("110.00"), 6, "Household", 10),
        };

        // A few historical sales so analytics are non-empty on first launch.
        sell(billing, store, cashier, PaymentMethod.UPI, "Anil", p[0], 1, p[1], 2, p[3], 4);
        sell(billing, store, cashier, PaymentMethod.CASH, "Walk-in", p[5], 5, p[6], 3, p[7], 2);
        sell(billing, store, cashier, PaymentMethod.CARD, "Priya", p[2], 1, p[8], 1, p[9], 1);
        sell(billing, store, cashier, PaymentMethod.UPI, "Walk-in", p[6], 4, p[7], 6, p[3], 6);
        sell(billing, store, cashier, PaymentMethod.CASH, "Rohit", p[10], 2, p[4], 1, p[1], 3);
    }

    private static void sell(BillingService billing, Store store, User cashier,
                             PaymentMethod method, String customer, Object... pairs)
            throws PosException {
        Cart cart = new Cart();
        for (int i = 0; i < pairs.length; i += 2) {
            Product product = (Product) pairs[i];
            int qty = (Integer) pairs[i + 1];
            cart.add(product, qty);
        }
        billing.checkout(store, cashier, cart, method, customer);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
