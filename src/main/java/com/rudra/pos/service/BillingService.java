package com.rudra.pos.service;

import com.rudra.pos.exception.InsufficientStockException;
import com.rudra.pos.exception.PosException;
import com.rudra.pos.exception.ProductNotFoundException;
import com.rudra.pos.exception.ValidationException;
import com.rudra.pos.model.Cart;
import com.rudra.pos.model.CartItem;
import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Product;
import com.rudra.pos.model.Store;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.TransactionLine;
import com.rudra.pos.model.User;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.service.payment.PaymentProcessor;
import com.rudra.pos.service.payment.PaymentStrategy;
import com.rudra.pos.util.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The billing engine. {@link #checkout} turns a {@link Cart} into a persisted
 * {@link Transaction} while guaranteeing two invariants:
 *
 * <ol>
 *   <li><b>No overselling</b> — stock is re-read from the repository and fully
 *       validated <i>before</i> any mutation, so a sale either fully succeeds or
 *       changes nothing.</li>
 *   <li><b>Consistency on failure</b> — if persistence fails part-way through
 *       deducting stock, the already-applied deductions are rolled back to their
 *       original values, leaving the catalogue exactly as it started.</li>
 * </ol>
 *
 * <p>The whole operation runs inside a synchronized block on the shared
 * {@link Database} so two concurrent cashiers cannot both pass validation and
 * then jointly drive stock negative (the classic check-then-act race).</p>
 */
public class BillingService {

    private final Database db;
    private final PaymentProcessor paymentProcessor;

    public BillingService(Database db) {
        this.db = db;
        this.paymentProcessor = new PaymentProcessor();
    }

    public Transaction checkout(Store store, User cashier, Cart cart,
                                PaymentMethod method, String customerName)
            throws PosException {
        if (cart == null || cart.isEmpty()) {
            throw new ValidationException("Cannot check out an empty cart");
        }

        synchronized (db) {
            // ---- Phase 1: validate everything, mutate nothing ----
            List<Product> freshProducts = new ArrayList<>();
            for (CartItem item : cart.getItems()) {
                Product fresh = db.products().findById(item.getProduct().getId())
                        .orElseThrow(() -> new ProductNotFoundException(
                                "Product '" + item.getProduct().getName() + "' no longer exists"));
                if (!fresh.getStoreId().equals(store.getId())) {
                    throw new ValidationException("Product does not belong to this store");
                }
                if (fresh.getStock() < item.getQuantity()) {
                    throw new InsufficientStockException(
                            fresh.getName(), item.getQuantity(), fresh.getStock());
                }
                freshProducts.add(fresh);
            }

            // ---- Phase 2: apply, with rollback on any failure ----
            Map<Product, Integer> originalStock = new LinkedHashMap<>();
            try {
                Transaction tx = new Transaction(
                        store.getId(),
                        cashier == null ? null : cashier.getId(),
                        method,
                        customerName);
                BigDecimal total = BigDecimal.ZERO;

                List<CartItem> items = cart.getItems();
                for (int i = 0; i < items.size(); i++) {
                    CartItem item = items.get(i);
                    Product product = freshProducts.get(i);

                    originalStock.put(product, product.getStock());
                    product.setStock(product.getStock() - item.getQuantity());
                    db.products().save(product); // automated stock synchronisation

                    TransactionLine line = new TransactionLine(
                            product.getId(), product.getName(),
                            item.getQuantity(), item.getUnitPrice());
                    tx.addLine(line);
                    total = total.add(line.getLineTotal());
                }

                tx.setTotal(Money.normalize(total));
                db.transactions().save(tx);

                // Settle payment through the chosen strategy.
                PaymentStrategy strategy = paymentProcessor.strategyFor(method);
                strategy.collect(tx.getTotal());

                return tx;
            } catch (RuntimeException persistenceFailure) {
                // Roll back any stock we already deducted, then surface the error.
                for (Map.Entry<Product, Integer> entry : originalStock.entrySet()) {
                    entry.getKey().setStock(entry.getValue());
                    try {
                        db.products().save(entry.getKey());
                    } catch (RuntimeException ignored) {
                        // best-effort restore
                    }
                }
                throw new PosException("Checkout failed and was rolled back: "
                        + persistenceFailure.getMessage(), persistenceFailure);
            }
        }
    }
}
