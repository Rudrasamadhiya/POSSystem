package com.rudra.pos.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * A completed sale: the header (who, when, how paid, total) plus its sold
 * lines. Persisted relationally — the header lives in {@code transactions.csv}
 * and the lines in {@code transaction_lines.csv}, joined on transaction id.
 */
public class Transaction implements Entity {

    private Long id;
    private Long storeId;
    private Long userId;
    private BigDecimal total;
    private PaymentMethod paymentMethod;
    private String customerName;
    private LocalDateTime createdAt;
    private final List<TransactionLine> lines = new ArrayList<>();

    public Transaction() {
    }

    public Transaction(Long storeId, Long userId, PaymentMethod paymentMethod, String customerName) {
        this.storeId = storeId;
        this.userId = userId;
        this.paymentMethod = paymentMethod;
        this.customerName = customerName;
        this.createdAt = LocalDateTime.now();
        this.total = BigDecimal.ZERO;
    }

    public void addLine(TransactionLine line) {
        lines.add(line);
    }

    public int getItemCount() {
        int count = 0;
        for (TransactionLine line : lines) {
            count += line.getQuantity();
        }
        return count;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public void setId(Long id) {
        this.id = id;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public List<TransactionLine> getLines() {
        return lines;
    }
}
