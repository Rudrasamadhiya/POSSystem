package com.rudra.pos.repository;

import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.TransactionLine;
import com.rudra.pos.persistence.CsvFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CSV-backed {@link TransactionRepository}. Persists relationally across
 * {@code transactions.csv} (headers) and {@code transaction_lines.csv} (lines).
 */
public class CsvTransactionRepository extends AbstractCsvRepository<Transaction>
        implements TransactionRepository {

    private static final String LINES_FILE = "transaction_lines.csv";

    public CsvTransactionRepository(Path dataDir) {
        super(dataDir, "transactions.csv");
    }

    @Override
    public List<Transaction> findByStore(Long storeId) {
        List<Transaction> result = new ArrayList<>();
        for (Transaction t : snapshot()) {
            if (t.getStoreId().equals(storeId)) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    protected List<String> header() {
        return Arrays.asList("id", "storeId", "userId", "total", "paymentMethod", "customerName", "createdAt");
    }

    @Override
    protected List<String> toRow(Transaction t) {
        return Arrays.asList(
                String.valueOf(t.getId()),
                String.valueOf(t.getStoreId()),
                t.getUserId() == null ? "" : String.valueOf(t.getUserId()),
                t.getTotal().toPlainString(),
                t.getPaymentMethod().name(),
                t.getCustomerName() == null ? "" : t.getCustomerName(),
                t.getCreatedAt().toString());
    }

    @Override
    protected Transaction fromRow(List<String> r) {
        Transaction t = new Transaction();
        t.setId(Long.parseLong(r.get(0)));
        t.setStoreId(Long.parseLong(r.get(1)));
        t.setUserId(r.get(2).isEmpty() ? null : Long.parseLong(r.get(2)));
        t.setTotal(new BigDecimal(r.get(3)));
        t.setPaymentMethod(PaymentMethod.valueOf(r.get(4)));
        t.setCustomerName(r.get(5));
        t.setCreatedAt(LocalDateTime.parse(r.get(6)));
        return t;
    }

    @Override
    protected void onLoaded() {
        CsvFile linesFile = new CsvFile(dataDir().resolve(LINES_FILE));
        List<List<String>> rows = linesFile.readAll();
        for (int i = 1; i < rows.size(); i++) {
            List<String> r = rows.get(i);
            Long txId = Long.parseLong(r.get(0));
            TransactionLine line = new TransactionLine(
                    r.get(1).isEmpty() ? null : Long.parseLong(r.get(1)),
                    r.get(2),
                    Integer.parseInt(r.get(3)),
                    new BigDecimal(r.get(4)));
            findById(txId).ifPresent(t -> t.addLine(line));
        }
    }

    @Override
    protected void onPersist() {
        CsvFile linesFile = new CsvFile(dataDir().resolve(LINES_FILE));
        List<String> header = Arrays.asList("transactionId", "productId", "productName", "quantity", "unitPrice");
        List<List<String>> rows = new ArrayList<>();
        for (Transaction t : snapshot()) {
            for (TransactionLine line : t.getLines()) {
                rows.add(Arrays.asList(
                        String.valueOf(t.getId()),
                        line.getProductId() == null ? "" : String.valueOf(line.getProductId()),
                        line.getProductName(),
                        String.valueOf(line.getQuantity()),
                        line.getUnitPrice().toPlainString()));
            }
        }
        linesFile.writeAll(header, rows);
    }
}
