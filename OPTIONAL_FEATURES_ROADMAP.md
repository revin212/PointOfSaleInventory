## Optional Features Roadmap (POS + Inventory)

Dokumen ini berisi fitur-fitur opsional yang umum di POS/Inventory, beserta **dependency order** supaya implementasinya tetap kecil, reviewable, dan tidak mengganggu MVP yang sudah ada.

### A) Returns / Refunds (paling “POS-native” setelah cancel)

- **Problem**: saat ini ada `cancel sale` (void) tapi tidak ada return setelah transaksi selesai (mis. H+1).
- **Minimal design**:
  - Tambah entitas `sale_returns` + `sale_return_items` yang mereferensikan `sales(id)` dan item-level.
  - Saat return: tulis `stock_movements` qty positif (`RETURN`) untuk mengembalikan stok.
  - Jika ingin refund uang: tambah `refund_amount` + `refund_method` (bisa reuse `PaymentMethod`).
- **Dependencies**:
  - Perlu stok ledger + product_stocks (sudah ada).
  - Perlu aturan validasi: qty return ≤ qty sold − qty already returned.

### B) Customers (opsional, tapi membantu untuk invoice/histori)

- **Minimal design**:
  - Table `customers` (name, phone/email, notes).
  - Tambah `customer_id` nullable pada `sales`.
  - Endpoint list/create/update customer (OWNER/STAFF sesuai kebutuhan).
- **Dependencies**:
  - Tidak wajib untuk returns.
  - Berguna untuk loyalty dan histori transaksi.

### C) Shift / Cash Drawer (operasional kasir)

- **Goal**: open/close shift, expected cash vs counted cash, rekonsiliasi.
- **Minimal design**:
  - `shifts`: opened_by, opened_at, closed_by, closed_at, opening_cash, closing_cash, status.
  - `cash_movements`: shift_id, type (IN/OUT), amount, note.
  - Sales CASH bisa (opsional) dikaitkan ke `shift_id`.
- **Dependencies**:
  - Auth/roles (sudah ada).
  - Reporting: daily summary bisa di-extend ke per shift.

### D) Stock Transfer (multi-location)

- **Prereq**: modul `locations` + stock per location (sudah ada).
- **Minimal design**:
  - `stock_transfers`: from_location_id, to_location_id, status (DRAFT/SENT/RECEIVED).
  - `stock_transfer_items`: product_id, qty.
  - Saat `RECEIVED`: tulis ledger dua sisi (`TRANSFER_OUT` negatif di from, `TRANSFER_IN` positif di to) + update `product_stocks`.

### E) Stock Opname (stock count)

- **Problem**: adjustment manual sudah ada, tapi belum ada mekanisme “count & approve”.
- **Minimal design**:
  - `stock_counts`: location_id, status (DRAFT/SUBMITTED/APPROVED), counted_by, approved_by.
  - `stock_count_items`: product_id, counted_qty, system_qty_snapshot, variance.
  - Saat approve: tulis `ADJUSTMENT` sebesar variance per item.
- **Dependencies**:
  - multi-location optional; bisa dimulai dari default location.

### F) Inventory valuation / COGS (opsional, biasanya “advanced”)

- **Note**: saat ini `stock_movements` menyimpan `unit_cost` pada receiving, tapi belum ada policy costing.
- **Pilihan**:
  - Average cost (paling mudah)
  - FIFO (lebih kompleks)
- **Dependencies**:
  - Purchasing receiving cost (sudah ada).
  - Perlu aturan rounding + laporan COGS per periode.

