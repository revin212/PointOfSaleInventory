# Smart POS + Inventory

A small point-of-sale and inventory management app with a role-based web UI
and a REST backend. Cashiers ring up sales, warehouse staff manage stock and
purchases, and owners see reports and manage users.

## What's inside

- **Frontend** — `smart-pos-frontend/` — React 19 + Vite + TypeScript, React
  Router, Tanstack Query, React Hook Form + Zod, Tailwind. All feature
  services use a dispatcher pattern that switches between in-memory mocks
  and the real backend based on the `VITE_USE_MOCKS` flag.
- **Backend** — `backend/` — Spring Boot 3.4 on Java 21, PostgreSQL 16 +
  Flyway, JWT auth (access + refresh with rotation), BCrypt, Springdoc
  OpenAPI. API base path: `/api/v1`.

## Features

- **Auth & Users** — JWT login, silent refresh on 401, logout with refresh-
  token revocation. Three seeded roles: `OWNER`, `CASHIER`, `WAREHOUSE`.
- **Catalog** — categories, suppliers, products (CRUD, low-stock threshold).
- **POS / Sales** — ring up sales, multiple payment methods, cancel sales
  (stock is reversed automatically).
- **Purchases** — create purchase orders, receive stock against them.
- **Inventory** — on-hand view, stock-movement ledger, manual adjustments.
- **Reports** — daily summary and top-products.
- **Dashboard** — composed from reports + stock + recent sales endpoints.
- **Audit log** — owner-only view of system actions.

## Prerequisites

- **JDK 21** and **Maven 3.9+**
- **Node.js 20+** and **npm**
- **Docker** (for local PostgreSQL)

## Run locally

Run each block in its own terminal.

### 1. Start PostgreSQL

```powershell
cd backend
docker compose up -d
```

### 2. Start the backend

```powershell
cd backend
mvn spring-boot:run
```

Backend is served at `http://localhost:8080/api/v1`.
Swagger UI: `http://localhost:8080/swagger-ui.html`.

On the first start, the data seeder creates three users:

| Role      | Email                       | Password       |
|-----------|-----------------------------|----------------|
| OWNER     | `owner@smartpos.local`      | `Password123!` |
| CASHIER   | `cashier@smartpos.local`    | `Password123!` |
| WAREHOUSE | `warehouse@smartpos.local`  | `Password123!` |

### 3. Start the frontend

```powershell
cd smart-pos-frontend
npm install
npm run dev
```

Frontend is served at `http://localhost:5173`. Log in with any of the seeded
users above.

## Environment configuration

### Frontend (`smart-pos-frontend/.env.development`)

```
VITE_API_BASE_URL=http://localhost:8080/api/v1
VITE_USE_MOCKS=false
```

Flip `VITE_USE_MOCKS=true` to run the UI entirely against in-memory mocks,
without a backend — useful for UI-only work. No code changes required.

### Backend (see `backend/src/main/resources/application.yml` for defaults)

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `JWT_SECRET` (min 32 bytes), `JWT_ACCESS_TTL`, `JWT_REFRESH_TTL`
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`)
- `APP_SEED_ENABLED`, `APP_SEED_PASSWORD`

## Project layout

```
backend/                Spring Boot API (Java 21)
smart-pos-frontend/     React + Vite web UI
how-to-run.txt          One-page quick-start cheatsheet
```

See [`backend/README.md`](backend/README.md) for the full API reference and
error-envelope format.

## Tests

```powershell
cd backend
mvn test
```
