package com.rudra.pos.web;

import com.rudra.pos.exception.PosException;
import com.rudra.pos.model.Cart;
import com.rudra.pos.model.PaymentMethod;
import com.rudra.pos.model.Product;
import com.rudra.pos.model.Store;
import com.rudra.pos.model.Transaction;
import com.rudra.pos.model.TransactionLine;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.seed.DemoData;
import com.rudra.pos.service.BillingService;
import com.rudra.pos.service.InventoryService;
import com.rudra.pos.service.ReportService;
import com.rudra.pos.service.TopProduct;
import com.rudra.pos.util.Json;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

/**
 * A tiny web front end built on the JDK's own {@link HttpServer} — no Spring,
 * no servlet container, no third-party jars. It exposes a read-mostly dashboard
 * plus a quick-sale endpoint over the same service layer the CLI uses, which is
 * what makes the project deployable to a real URL while staying dependency-free.
 *
 * <p>For a frictionless public demo the dashboard operates on the seeded demo
 * store (no login), so a visitor can immediately see live data and ring up a
 * sale.</p>
 */
public class WebServer {

    private final Database db;
    private final Store store;
    private final InventoryService inventory;
    private final BillingService billing;
    private final ReportService reports;

    public WebServer(Database db) throws PosException {
        this.db = db;
        DemoData.seedIfEmpty(db);
        this.store = db.stores().findByCode("BHOPAL01")
                .orElseThrow(() -> new PosException("Demo store not found"));
        this.inventory = new InventoryService(db);
        this.billing = new BillingService(db);
        this.reports = new ReportService(db);
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", this::handleRoot);
        server.createContext("/api/summary", e -> safe(e, this::summary));
        server.createContext("/api/products", e -> safe(e, this::products));
        server.createContext("/api/top", e -> safe(e, this::top));
        server.createContext("/api/daily", e -> safe(e, this::daily));
        server.createContext("/api/payments", e -> safe(e, this::payments));
        server.createContext("/api/sell", e -> safe(e, this::sell));
        server.setExecutor(Executors.newFixedThreadPool(8));
        server.start();
        System.out.println("POS web server running on http://localhost:" + port
                + "  (store: " + store.getName() + ")");
    }

    // ------------------------------------------------------------- handlers

    private void handleRoot(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (!"/".equals(path) && !"/index.html".equals(path)) {
            sendText(ex, 404, "Not found");
            return;
        }
        sendHtml(ex, 200, pageHtml());
    }

    private String summary(HttpExchange ex) {
        Long sid = store.getId();
        Json.Arr low = new Json.Arr();
        for (Product p : inventory.lowStockProducts(sid)) {
            low.add(new Json.Obj()
                    .str("name", p.getName())
                    .num("stock", p.getStock())
                    .num("reorder", p.getReorderLevel())
                    .end());
        }
        return new Json.Obj()
                .str("store", store.getName())
                .str("location", store.getLocation())
                .raw("totalSales", reports.totalSales(sid).toPlainString())
                .raw("todaySales", reports.todaySales(sid).toPlainString())
                .num("transactions", reports.transactionCount(sid))
                .num("products", inventory.listProducts(sid).size())
                .raw("lowStock", low.end())
                .end();
    }

    private String products(HttpExchange ex) {
        Json.Arr arr = new Json.Arr();
        for (Product p : inventory.listProducts(store.getId())) {
            arr.add(new Json.Obj()
                    .num("id", p.getId())
                    .str("barcode", p.getBarcode())
                    .str("name", p.getName())
                    .raw("price", p.getPrice().toPlainString())
                    .num("stock", p.getStock())
                    .str("category", p.getCategory())
                    .bool("low", p.isLowStock())
                    .end());
        }
        return arr.end();
    }

    private String top(HttpExchange ex) {
        Json.Arr arr = new Json.Arr();
        for (TopProduct t : reports.topProducts(store.getId(), 8)) {
            arr.add(new Json.Obj()
                    .str("name", t.getName())
                    .num("qty", t.getQuantitySold())
                    .end());
        }
        return arr.end();
    }

    private String daily(HttpExchange ex) {
        Json.Arr arr = new Json.Arr();
        for (Map.Entry<LocalDate, java.math.BigDecimal> e : reports.dailySales(store.getId()).entrySet()) {
            arr.add(new Json.Obj()
                    .str("date", e.getKey().toString())
                    .raw("total", e.getValue().toPlainString())
                    .end());
        }
        return arr.end();
    }

    private String payments(HttpExchange ex) {
        Json.Arr arr = new Json.Arr();
        reports.paymentBreakdown(store.getId()).forEach((method, total) ->
                arr.add(new Json.Obj()
                        .str("method", method.getLabel())
                        .raw("total", total.toPlainString())
                        .end()));
        return arr.end();
    }

    private String sell(HttpExchange ex) throws IOException {
        if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) {
            return new Json.Obj().bool("ok", false).str("error", "POST required").end();
        }
        Map<String, String> form = parseForm(readBody(ex));
        String barcode = form.getOrDefault("barcode", "").trim();
        int qty;
        try {
            qty = Integer.parseInt(form.getOrDefault("qty", "1").trim());
        } catch (NumberFormatException e) {
            qty = 1;
        }
        PaymentMethod method;
        try {
            method = PaymentMethod.fromString(form.getOrDefault("payment", "CASH"));
        } catch (IllegalArgumentException e) {
            method = PaymentMethod.CASH;
        }
        try {
            Product product = inventory.findByBarcode(store.getId(), barcode);
            Cart cart = new Cart();
            cart.add(product, Math.max(1, qty));
            Transaction tx = billing.checkout(store, null, cart, method, "Web Customer");
            Json.Arr lines = new Json.Arr();
            for (TransactionLine l : tx.getLines()) {
                lines.add(new Json.Obj()
                        .str("name", l.getProductName())
                        .num("qty", l.getQuantity())
                        .raw("amount", l.getLineTotal().toPlainString())
                        .end());
            }
            return new Json.Obj()
                    .bool("ok", true)
                    .num("transactionId", tx.getId())
                    .raw("total", tx.getTotal().toPlainString())
                    .raw("lines", lines.end())
                    .end();
        } catch (PosException e) {
            return new Json.Obj().bool("ok", false).str("error", e.getMessage()).end();
        }
    }

    // --------------------------------------------------------------- plumbing

    private interface JsonHandler {
        String handle(HttpExchange ex) throws Exception;
    }

    private void safe(HttpExchange ex, JsonHandler handler) throws IOException {
        try {
            String body = handler.handle(ex);
            sendJson(ex, 200, body);
        } catch (Exception e) {
            sendJson(ex, 500, new Json.Obj().bool("ok", false)
                    .str("error", String.valueOf(e.getMessage())).end());
        }
    }

    private static Map<String, String> parseForm(String body) {
        Map<String, String> map = new LinkedHashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            int eq = pair.indexOf('=');
            if (eq < 0) {
                continue;
            }
            String k = urlDecode(pair.substring(0, eq));
            String v = urlDecode(pair.substring(eq + 1));
            map.put(k, v);
        }
        return map;
    }

    private static String urlDecode(String s) {
        try {
            return java.net.URLDecoder.decode(s, StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            return s;
        }
    }

    private static String readBody(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            return new String(readAll(in), StandardCharsets.UTF_8);
        }
    }

    private static byte[] readAll(InputStream in) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
        return out.toByteArray();
    }

    private static void sendJson(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void sendHtml(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    private static void sendText(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    /** Load the dashboard HTML from resources, falling back to a minimal page. */
    private String pageHtml() {
        try (InputStream in = WebServer.class.getResourceAsStream("/web/index.html")) {
            if (in != null) {
                return new String(readAll(in), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // fall through to inline fallback
        }
        return "<!doctype html><html><body style='font-family:sans-serif;padding:2rem'>"
                + "<h1>Multi-Store POS</h1><p>API is live. Try "
                + "<a href='/api/summary'>/api/summary</a>.</p></body></html>";
    }
}
