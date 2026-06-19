package com.rudra.pos.repository;

import com.rudra.pos.model.Product;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Persists {@link Product} catalogue items to {@code products.csv}. */
public class ProductRepository extends AbstractCsvRepository<Product> {

    public ProductRepository(Path dataDir) {
        super(dataDir, "products.csv");
    }

    public Optional<Product> findByBarcode(Long storeId, String barcode) {
        for (Product p : snapshot()) {
            if (p.getStoreId().equals(storeId) && p.getBarcode().equals(barcode)) {
                return Optional.of(p);
            }
        }
        return Optional.empty();
    }

    public List<Product> findByStore(Long storeId) {
        List<Product> result = new ArrayList<>();
        for (Product p : snapshot()) {
            if (p.getStoreId().equals(storeId)) {
                result.add(p);
            }
        }
        return result;
    }

    @Override
    protected List<String> header() {
        return Arrays.asList("id", "storeId", "barcode", "name", "price", "stock", "category", "reorderLevel");
    }

    @Override
    protected List<String> toRow(Product p) {
        return Arrays.asList(
                String.valueOf(p.getId()),
                String.valueOf(p.getStoreId()),
                p.getBarcode(),
                p.getName(),
                p.getPrice().toPlainString(),
                String.valueOf(p.getStock()),
                p.getCategory() == null ? "" : p.getCategory(),
                String.valueOf(p.getReorderLevel()));
    }

    @Override
    protected Product fromRow(List<String> r) {
        Product p = new Product();
        p.setId(Long.parseLong(r.get(0)));
        p.setStoreId(Long.parseLong(r.get(1)));
        p.setBarcode(r.get(2));
        p.setName(r.get(3));
        p.setPrice(new BigDecimal(r.get(4)));
        p.setStock(Integer.parseInt(r.get(5)));
        p.setCategory(r.get(6));
        p.setReorderLevel(Integer.parseInt(r.get(7)));
        return p;
    }
}
