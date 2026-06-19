package com.rudra.pos;

import com.rudra.pos.app.Console;
import com.rudra.pos.app.DemoRunner;
import com.rudra.pos.app.PosApplication;
import com.rudra.pos.exception.PosException;
import com.rudra.pos.persistence.Database;
import com.rudra.pos.seed.DemoData;
import com.rudra.pos.web.WebServer;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Application entry point.
 *
 * <pre>
 *   java -jar pos-system.jar          # interactive CLI (seeds demo data on first run)
 *   java -jar pos-system.jar --demo   # scripted, non-interactive walk-through
 *   java -jar pos-system.jar --web    # start the web dashboard (uses PORT env, default 8080)
 * </pre>
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws Exception {
        PrintStream out = new PrintStream(System.out, true, StandardCharsets.UTF_8);

        if (args.length > 0 && ("--demo".equals(args[0]) || "demo".equals(args[0]))) {
            DemoRunner.run(out);
            return;
        }

        if (args.length > 0 && ("--web".equals(args[0]) || "web".equals(args[0]))) {
            int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
            new WebServer(Database.openDefault()).start(port);
            return; // HttpServer runs on its own non-daemon threads
        }

        Database db = Database.openDefault();
        try {
            DemoData.seedIfEmpty(db);
        } catch (PosException e) {
            out.println("Warning: could not seed demo data: " + e.getMessage());
        }

        try (Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8.name())) {
            new PosApplication(db, new Console(scanner, out)).run();
        }
    }
}
