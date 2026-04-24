## Smart POS Acceptance Checklist (real backend mode)

Source of truth:
- Backend contract: `C:\Users\Revin Dennis\.cursor\plans\backend-ai-api-contract.md`
- Frontend contract: `C:\Users\Revin Dennis\.cursor\plans\frontend-ai-api-contract.md`

Notes:
- All non-auth endpoints require `Authorization: Bearer <accessToken>`.
- Money values are **numbers** over the wire; UI formats as `Rp` (no decimals).
- Roles: `OWNER`, `CASHIER`, `WAREHOUSE` (must match backend enums exactly).

### 0) Environment & boot
- [ ] Backend: `docker compose up -d` + `mvn spring-boot:run` works on a fresh machine.
- [ ] Frontend: `npm install` + `npm run dev` works, `VITE_USE_MOCKS=false` integrates to backend at `/api/v1`.
- [ ] Swagger UI loads and authenticated calls work.

### 1) Auth & session
- [ ] Login succeeds with seeded users:
  - [ ] `owner@smartpos.local` / `Password123!`
  - [ ] `cashier@smartpos.local` / `Password123!`
  - [ ] `warehouse@smartpos.local` / `Password123!`
- [ ] Login response shape matches: `accessToken`, `refreshToken`, `expiresIn`, `user { id, name, email, role }`.
- [ ] `GET /auth/me` returns current user (role/active).
- [ ] When access token expires, frontend refreshes on 401 and retries once.
- [ ] Logout revokes refresh token; subsequent refresh fails.
- [ ] Route access aligns with role navigation (no “dead” modules).

### 2) Users (OWNER only)
- [ ] OWNER can list users (`GET /users`).
- [ ] OWNER can create user (`POST /users`) and newly created user can log in.
- [ ] OWNER can update user (`PUT /users/{id}`).
- [ ] OWNER can deactivate/reactivate (`PATCH /users/{id}/active`).
- [ ] CASHIER/WAREHOUSE are forbidden from all user-admin endpoints.

### 3) Catalog (Categories / Suppliers / Products)
Categories:
- [ ] All roles can list categories.
- [ ] OWNER/WAREHOUSE can create/update/delete categories.
- [ ] CASHIER cannot write categories.

Suppliers:
- [ ] OWNER/WAREHOUSE can CRUD suppliers.
- [ ] CASHIER forbidden from supplier writes.

Products:
- [ ] All roles can list/search products.
- [ ] OWNER/WAREHOUSE can create/update/delete products.
- [ ] SKU uniqueness errors are displayed clearly in the UI.
- [ ] Price/cost are realistic IDR amounts (no “Rp 2”).

### 4) POS / Sales create (CASHIER + OWNER)
- [ ] Product search works and shows price formatted `Rp 1.000.000` with tabular numerals.
- [ ] Add to cart increments qty; qty input clamps to >= 1.
- [ ] Line discount clamps to >= 0; order discount clamps to >= 0.
- [ ] Totals math is correct: `subtotal - lineDiscountTotal - discount`, floored at 0.
- [ ] Paid amount must be >= total; change is `paid - total` floored at 0.
- [ ] POST `/sales` request shape matches contract:
  - [ ] items: `{ productId, qty, unitPrice, lineDiscount? }`
  - [ ] `discount`, `paymentMethod`, `paidAmount`
- [ ] Successful sale shows invoice no + clears cart and inputs.
- [ ] Insufficient stock is handled with a friendly error (no blank screen).

### 5) Sales list/detail/cancel
- [ ] CASHIER/OWNER can list sales and open details.
- [ ] Detail view shows items, unitPrice, totals, status.
- [ ] OWNER can cancel sale (`POST /sales/{id}/cancel`).
- [ ] CASHIER cannot cancel sale.
- [ ] Cancelling writes compensating stock movements (`SALE_CANCEL`) and status updates to `CANCELLED`.

### 6) Purchases create/receive (WAREHOUSE + OWNER)
- [ ] Create purchase with supplier + items.
- [ ] Receive purchase validates quantities and transitions status:
  - [ ] `OPEN` → `PARTIALLY_RECEIVED` → `RECEIVED`
- [ ] Receiving creates `PURCHASE_RECEIVE` stock movements.
- [ ] Invalid receive (over receive / negative / empty) shows correct validation messages.

### 7) Inventory / stock ledger (WAREHOUSE + OWNER + reads)
- [ ] On-hand view lists products and current stock; low-only filter works.
- [ ] Movements view filters by product/type/date; paging is sane.
- [ ] Adjustments create `ADJUSTMENT` movements and update on-hand.
- [ ] CASHIER can view on-hand but is forbidden from movements/adjustments writes (per backend policy).

### 8) Reports & dashboard (OWNER)
- [ ] Daily summary (`/reports/daily?date=...`) renders gross/discount/net + breakdown.
- [ ] Top products (`/reports/top-products?from=...&to=...`) renders qtySold + revenue.
- [ ] Dashboard composes KPIs + recent sales + low stock with realistic numbers.
- [ ] Non-OWNER users cannot access reports/dashboard endpoints (and UI hides/guards).

### 9) Audit logs (OWNER)
- [ ] Audit log page loads and filters work (entityType/action/userId/from/to).
- [ ] Core actions write audit entries: sale create/cancel, purchase receive, stock adjustment.

