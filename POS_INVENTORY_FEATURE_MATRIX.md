## Smart POS / Inventory — Feature Matrix

Dokumen ini merangkum fitur **yang sudah ada** di backend saat ini dan membandingkannya dengan fitur yang **umumnya ada** di aplikasi/website POS + Stock/Inventory Management.

Catatan:
- Base API path saat ini hardcoded di controller: `/api/v1/*`.
- Roles: `OWNER`, `CASHIER`, `WAREHOUSE`.

### 1) Fitur yang sudah ada (Implemented)

- **Auth & session**
  - **Login**: `POST /api/v1/auth/login` ([backend/src/main/java/com/smartpos/backend/auth/AuthController.java](backend/src/main/java/com/smartpos/backend/auth/AuthController.java))
  - **Refresh**: `POST /api/v1/auth/refresh`
  - **Logout**: `POST /api/v1/auth/logout`
  - **Me**: `GET /api/v1/auth/me`

- **Users (OWNER-only)**
  - CRUD + activate/deactivate ([backend/src/main/java/com/smartpos/backend/users/UserController.java](backend/src/main/java/com/smartpos/backend/users/UserController.java))

- **Catalog / Master data**
  - **Categories**: list + CRUD (role-based) ([backend/src/main/java/com/smartpos/backend/categories/CategoryController.java](backend/src/main/java/com/smartpos/backend/categories/CategoryController.java))
  - **Suppliers**: CRUD (OWNER/WAREHOUSE) ([backend/src/main/java/com/smartpos/backend/suppliers/SupplierController.java](backend/src/main/java/com/smartpos/backend/suppliers/SupplierController.java))
  - **Products**: list/search + CRUD (write: OWNER/WAREHOUSE) ([backend/src/main/java/com/smartpos/backend/products/ProductController.java](backend/src/main/java/com/smartpos/backend/products/ProductController.java))

- **Purchasing + receiving (OWNER/WAREHOUSE)**
  - **Create PO**: `POST /api/v1/purchases`
  - **Receive PO** (partial): `POST /api/v1/purchases/{id}/receive`
  - **List/detail**: `GET /api/v1/purchases`, `GET /api/v1/purchases/{id}`
  - Controller: [backend/src/main/java/com/smartpos/backend/purchases/PurchaseController.java](backend/src/main/java/com/smartpos/backend/purchases/PurchaseController.java)

- **POS / Sales (CASHIER + OWNER)**
  - **Create sale**: `POST /api/v1/sales`
  - **List/detail**: `GET /api/v1/sales`, `GET /api/v1/sales/{id}`
  - **Cancel sale** (OWNER-only): `POST /api/v1/sales/{id}/cancel`
  - Controller: [backend/src/main/java/com/smartpos/backend/sales/SaleController.java](backend/src/main/java/com/smartpos/backend/sales/SaleController.java)

- **Stock / Inventory ledger**
  - **On-hand** (read-only; cashier allowed): `GET /api/v1/stock/on-hand`
  - **Movements** (OWNER/WAREHOUSE): `GET /api/v1/stock/movements`
  - **Adjustments** (OWNER/WAREHOUSE): `POST /api/v1/stock/adjustments`
  - Controller: [backend/src/main/java/com/smartpos/backend/stock/StockController.java](backend/src/main/java/com/smartpos/backend/stock/StockController.java)

- **Reports (OWNER-only)**
  - **Daily summary**: `GET /api/v1/reports/daily-summary`
  - **Top products**: `GET /api/v1/reports/top-products`
  - Controller: [backend/src/main/java/com/smartpos/backend/reports/ReportController.java](backend/src/main/java/com/smartpos/backend/reports/ReportController.java)

- **Audit logs (OWNER-only)**
  - **List/filter**: `GET /api/v1/audit-logs`
  - Controller: [backend/src/main/java/com/smartpos/backend/audit/AuditController.java](backend/src/main/java/com/smartpos/backend/audit/AuditController.java)

### 2) Fitur POS/Inventory yang umum (Typical) tapi belum ada / belum lengkap

- **PPN / tax**: tax amount, net vs gross, rounding policy, laporan pajak.
- **Multi-location stock**: gudang/outlet >1, transfer stock antar lokasi, on-hand per lokasi.
- **Returns/refunds/exchange**: return setelah sale completed + refund record.
- **Customers** (opsional): customer entity + link ke sales + history.
- **Shift / cash drawer**: open/close shift, cash count, reconciliation, per-shift reporting.
- **Stock opname**: stock count + variance adjustments, snapshot/approval flow.
- **Inventory valuation / COGS**: metode costing (avg/FIFO) untuk laporan profit (opsional).

### 3) Area yang perlu “hardening” (risiko implementasi)

- **Invoice number collision**: invoice saat ini random 6 digit per hari dan mengandalkan unique constraint tanpa retry.
- **Stock race condition**: on-hand dihitung via SUM lalu insert movement (potensi oversell saat concurrency).
- **Migrasi harga V2**: `V2__normalize_small_prices.sql` mengalikan `price/cost` dengan `100000` untuk `0 < price < 1000` (perlu verifikasi intent).

