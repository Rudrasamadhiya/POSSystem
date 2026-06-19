# 🏪 Multi-Store POS System

[![CI](https://github.com/Rudrasamadhiya/POSSystem/actions/workflows/ci.yml/badge.svg)](https://github.com/Rudrasamadhiya/POSSystem/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-11%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Build](https://img.shields.io/badge/build-zero%20dependencies-success)](#-build--run)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A **point-of-sale and inventory-management system** for multi-tenant retail
(think a chain of stores, each with its own catalogue, staff and sales),
written in **pure, object-oriented Java with zero third-party dependencies**.

It demonstrates clean layered architecture, classic design patterns, a
**transactional billing engine** that never oversells stock, salted password
hashing, and durable file-based persistence with **atomic, crash-safe writes** —
all built on nothing but the JDK standard library, compiled and tested in CI.

> Re-engineered from an earlier prototype into a proper OOP backend with a test
> suite and continuous integration.

---

## ✨ Features

**Multi-tenant core**
- Every store is an isolated tenant — its own products, staff and sales ledger.
- Data isolation enforced at the service layer (a store can only ever see its own rows).

**Authentication & roles**
- Salted **PBKDF2-HMAC-SHA256** password hashing with constant-time verification — no plaintext, no third-party crypto lib.
- Role-based access control: `ADMIN` ▸ `MANAGER` ▸ `CASHIER`, with privilege ordering.

**Inventory management**
- Add / update / delete products, barcode lookup, free-text search (name · barcode · category).
- **Low-stock alerts** driven by a per-product reorder threshold.
- Restock / stock-adjustment with validation (stock can never go negative).

**Billing engine** (the heart of the system)
- Cart with automatic line-merging and exact `BigDecimal` money math.
- **Transactional checkout**: validates all stock *before* mutating anything, so a sale is all-or-nothing.
- **Automatic rollback** if persistence fails mid-write — the catalogue is left exactly as it started.
- Concurrency-safe so two cashiers can't both pass validation and jointly oversell (the classic check-then-act race).
- Pluggable payment methods (Cash / Card / UPI) via the **Strategy pattern**.
- Printable text receipts.

**Reporting & analytics**
- Total & daily revenue, best-selling products, payment-method mix.
- One-click **CSV export** of the sales ledger.

**Persistence**
- Human-readable CSV tables, RFC-4180-correct quoting/escaping.
- **Atomic writes** (temp file + atomic move) → a reader never sees a half-written file, even on a crash.
- State survives restarts; verified by a dedicated reload test.

---

## 🏛️ Architecture

A strict, one-directional **layered architecture** — each layer depends only on
the layer beneath it:

```mermaid
flowchart TD
    A["CLI Layer<br/>PosApplication · Console · DemoRunner"] --> B
    B["Service Layer<br/>AuthService · InventoryService · BillingService · ReportService"] --> C
    B --> P["Payment Strategies<br/>Cash · Card · UPI"]
    C["Repository Layer<br/>Store · User · Product · Transaction"] --> D
    D["Persistence Layer<br/>Database · CsvFile (atomic) · Csv codec"]
    B --> M["Domain Models<br/>Store · User · Product · Cart · Transaction"]
    C --> M
```

### Design patterns used

| Pattern | Where | Why |
|---|---|---|
| **Repository** | `repository/*` | Decouples business logic from CSV storage behind a generic CRUD contract. |
| **Template Method** | `AbstractCsvRepository` | Base class handles loading, id-generation, locking & atomic persistence; subclasses only map a row ↔ entity. |
| **Strategy** | `service/payment/*` | Each tender type is its own class; adding one doesn't touch the billing engine. |
| **Factory** | `PaymentProcessor` | Resolves a `PaymentMethod` to its strategy. |
| **Facade / Unit of Work** | `Database` | Single entry point owning the four repositories and their shared state. |
| **Service Layer** | `service/*` | Encapsulates all business rules and validation. |

### Engineering highlights
- **Transactional consistency without a database** — validate-then-apply plus rollback gives all-or-nothing semantics over plain files.
- **Crash-safe persistence** — atomic file replacement means the on-disk table is always a complete, consistent snapshot.
- **Thread safety** — repositories guard state with a `ReentrantReadWriteLock`; checkout is synchronized to prevent oversell races.
- **Custom exception hierarchy** rooted at `PosException` for precise, catchable error handling.
- **`BigDecimal` money** throughout — no floating-point rounding bugs in totals.

---

## 📂 Project structure

```
POSSystem/
├── src/main/java/com/rudra/pos/
│   ├── Main.java                  # entry point (interactive or --demo)
│   ├── app/                       # CLI: PosApplication, Console, DemoRunner
│   ├── model/                     # Store, User, Product, Cart, Transaction, enums
│   ├── exception/                 # PosException hierarchy
│   ├── persistence/               # Database facade, CsvFile (atomic I/O)
│   ├── repository/                # generic + concrete repositories
│   ├── service/                   # Auth, Inventory, Billing, Report
│   │   └── payment/               # Strategy + Factory for tenders
│   ├── util/                      # PasswordHasher, Csv, Money, ReceiptPrinter
│   └── seed/                      # DemoData seeder
├── src/test/java/com/rudra/pos/   # zero-dependency test suite (TestMain, Assert)
├── .github/workflows/ci.yml       # compile + test + run demo on every push
├── build.sh / build.bat           # one-command build (JDK only)
└── pom.xml                        # optional Maven build
```

---

## 🚀 Build & Run

**Requirements:** a JDK 11 or newer. **No Maven, no internet, no dependencies.**

### Option A — build script (recommended)
```bash
# macOS / Linux
./build.sh

# Windows
build.bat
```
This compiles the sources, packages a runnable jar, then compiles and runs the
full test suite.

### Option B — Maven
```bash
mvn package
```

### Run it
```bash
# Interactive POS (seeds a demo store on first launch)
java -jar out/pos-system.jar

# Scripted, non-interactive walk-through (seed → sell → reports)
java -jar out/pos-system.jar --demo
```

### Demo credentials
| Account | Login | Password |
|---|---|---|
| Store admin | `BHOPAL01` | `admin123` |
| Cashier | `ravi` | `cashier123` |
| Manager | `neha` | `manager123` |

---

## ✅ Testing & CI

The project ships a hand-rolled, dependency-free test suite (`TestMain`) covering
the CSV codec, password hashing, authentication, inventory rules, the
transactional billing engine (including oversell rejection and rollback), a
persistence reload test, and reporting. It exits non-zero on any failure.

Every push runs **GitHub Actions CI** (`.github/workflows/ci.yml`): it compiles
the project on JDK 17, runs the test suite, and executes the end-to-end demo —
so the badge at the top reflects a genuinely working build.

```bash
# run just the tests locally
./build.sh        # tests run as the final step
```

---

## 🗺️ Roadmap

- [ ] REST API layer (Spring Boot) over the same service core
- [ ] Pluggable storage backend (swap CSV for SQLite/Postgres behind the `Repository` interface)
- [ ] Web/JavaFX front end reusing the service layer
- [ ] Receipt export to PDF / thermal printer
- [ ] Per-role permission checks enforced in the CLI menus

---

## 📄 License

Released under the MIT License — free to use and modify.

---

<div align="center">
Built by <b>Rudra Samadhiya</b> · IIIT Bhopal
</div>
