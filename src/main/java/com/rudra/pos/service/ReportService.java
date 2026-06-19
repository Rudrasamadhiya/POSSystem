package com.rudra.pos.service;

import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.TransactionLine;
import com.rudra.pos.persistence.CsvFile;
import com.rudra.pos.persistence.Database;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Read-only analytics over a store's transactions: revenue totals, daily
 * trends, best sellers, tender mix, plus CSV export of the sales ledger.
 */
public class ReportService {

    private final Database db;

    public ReportService(Database db) {
        this.db = db;
    }

    public BigDecimal totalSales(Long storeId) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : db.transactions().findByStore(storeId)) {
            total = total.add(t.getTotal());
        }
        return total;
    }

    public BigDecimal salesOn(Long storeId, LocalDate date) {
        BigDecimal total = BigDecimal.ZERO;
        for (Transaction t : db.transactions().findByStore(storeId)) {
            if (t.getCreatedAt().toLocalDate().equals(date)) {
                total = total.add(t.getTotal());
            }
        }
        return total;
    }

    public BigDecimal todaySales(Long storeId) {
        return salesOn(storeId, LocalDate.now());
    }

    public int transactionCount(Long storeId) {
        return db.transactions().findByStore(storeId).size();
    }

    /** Revenue per day, most recent first. */
    public Map<LocalDate, BigDecimal> dailySales(Long storeId) {
        Map<LocalDate, BigDecimal> byDay = new TreeMap<>(Comparator.reverseOrder());
        for (Transaction t : db.transactions().findByStore(storeId)) {
            LocalDate day = t.getCreatedAt().toLocalDate();
            byDay.merge(day, t.getTotal(), BigDecimal::add);
        }
        return byDay;
    }

    public List<TopProduct> topProducts(Long storeId, int limit) {
        Map<String, Integer> sold = new HashMap<>();
        for (Transaction t : db.transactions().findByStore(storeId)) {
            for (TransactionLine line : t.getLines()) {
                sold.merge(line.getProductName(), line.getQuantity(), Integer::sum);
            }
        }
        List<TopProduct> result = new ArrayList<>();
        for (Map.Entry<String, Integer> e : sold.entrySet()) {
            result.add(new TopProduct(e.getKey(), e.getValue()));
        }
        result.sort(Comparator.comparingInt(TopProduct::getQuantitySold).reversed());
        return result.size() > limit ? result.subList(0, limit) : result;
    }

    public Map<PaymentMethod, BigDecimal> paymentBreakdown(Long storeId) {
        Map<PaymentMethod, BigDecimal> mix = new EnumMap<>(PaymentMethod.class);
        for (Transaction t : db.transactions().findByStore(storeId)) {
            mix.merge(t.getPaymentMethod(), t.getTotal(), BigDecimal::add);
        }
        return mix;
    }

    /** Export the full sales ledger of a store to a CSV file. */
    public Path exportSalesCsv(Long storeId, Path target) {
        CsvFile out = new CsvFile(target);
        List<String> header = Arrays.asList(
                "transactionId", "date", "time", "customer", "paymentMethod", "items", "total");
        List<List<String>> rows = new ArrayList<>();
        for (Transaction t : db.transactions().findByStore(storeId)) {
            rows.add(Arrays.asList(
                    String.valueOf(t.getId()),
                    t.getCreatedAt().toLocalDate().toString(),
                    t.getCreatedAt().toLocalTime().withNano(0).toString(),
                    t.getCustomerName() == null ? "" : t.getCustomerName(),
                    t.getPaymentMethod().getLabel(),
                    String.valueOf(t.getItemCount()),
                    t.getTotal().toPlainString()));
        }
        out.writeAll(header, rows);
        return target;
    }
}
