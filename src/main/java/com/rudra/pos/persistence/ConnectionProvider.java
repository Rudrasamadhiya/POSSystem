package com.rudra.pos.persistence;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Turns a connection string into JDBC {@link Connection}s.
 *
 * <p>Accepts the URL shapes you actually meet in the wild:</p>
 * <ul>
 *   <li>Render / Heroku style: {@code postgres://user:pass@host:port/db}</li>
 *   <li>A ready JDBC URL: {@code jdbc:postgresql://host:port/db} (credentials
 *       then read from {@code PGUSER}/{@code PGPASSWORD} if not embedded)</li>
 * </ul>
 *
 * <p>Uses the standard {@link DriverManager} — the PostgreSQL driver registers
 * itself via the JDBC4 service loader when its jar is on the runtime classpath,
 * so this class compiles with only the JDK ({@code java.sql}).</p>
 */
public class ConnectionProvider {

    private final String jdbcUrl;
    private final String user;
    private final String password;

    public ConnectionProvider(String rawUrl) {
        // Best-effort: ensure the driver is loaded even on older setups.
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ignored) {
            // JDBC4 SPI will still find it if the jar is present.
        }

        if (rawUrl.startsWith("jdbc:")) {
            this.jdbcUrl = rawUrl;
            this.user = envOrEmpty("PGUSER");
            this.password = envOrEmpty("PGPASSWORD");
        } else {
            URI uri = URI.create(rawUrl);
            String userInfo = uri.getUserInfo() == null ? "" : uri.getUserInfo();
            String[] parts = userInfo.split(":", 2);
            this.user = parts.length > 0 ? parts[0] : "";
            this.password = parts.length > 1 ? parts[1] : "";
            int port = uri.getPort() == -1 ? 5432 : uri.getPort();
            String path = uri.getPath() == null ? "" : uri.getPath();
            String host = uri.getHost();
            boolean local = "localhost".equals(host) || "127.0.0.1".equals(host);
            String ssl = local ? "" : "?sslmode=require";
            this.jdbcUrl = "jdbc:postgresql://" + host + ":" + port + path + ssl;
        }
    }

    public Connection getConnection() throws SQLException {
        if (user == null || user.isEmpty()) {
            return DriverManager.getConnection(jdbcUrl);
        }
        return DriverManager.getConnection(jdbcUrl, user, password);
    }

    private static String envOrEmpty(String key) {
        String v = System.getenv(key);
        return v == null ? "" : v;
    }
}
