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

- **Docker** (required for full-stack Docker; also used for PostgreSQL in the hybrid flow below)
- **JDK 21** and **Maven 3.9+** (hybrid local run: Postgres in Docker, backend/frontend on the host)
- **Node.js 20+** and **npm** (hybrid local run only)

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

## Run locally (full Docker)

PostgreSQL, Spring Boot, and the production-built frontend (Nginx) all run in
containers via [`docker-compose.prod.yml`](docker-compose.prod.yml). You do not
need JDK or Node on the host.

1. From the repository root, create `.env.prod`. Copy the example and set at
   least **`JWT_SECRET`** (minimum ~32 bytes):

   ```powershell
   copy .env.prod.example .env.prod
   ```

   On macOS or Linux, use `cp .env.prod.example .env.prod` instead.

   For first-time login with the seeded accounts in the table above, set
   **`APP_SEED_ENABLED=true`**. Use **`WEB_PORT=8080`** if binding port **80**
   is inconvenient (for example on Windows without elevation); then open
   `http://localhost:8080`. If you change **`WEB_PORT`** or the hostname,
   update **`CORS_ALLOWED_ORIGINS`** to comma-separated browser origins that
   match (for example `http://localhost:8080`).

2. Build and start:

   ```powershell
   docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
   ```

3. Open the app (**`<WEB_PORT>`** defaults to **80** in `.env.prod.example`):

   - **UI** — `http://localhost:<WEB_PORT>/`
   - **API** — `http://localhost:<WEB_PORT>/api/v1`
   - **Swagger** — `http://localhost:<WEB_PORT>/swagger-ui.html`

   Nginx serves the SPA and proxies `/api/v1` to the backend service.

4. Stop:

   ```powershell
   docker compose -f docker-compose.prod.yml --env-file .env.prod down
   ```

See [`DEPLOY_VPS_DOCKER.md`](DEPLOY_VPS_DOCKER.md) for VPS deployment, HTTPS,
and backups.

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
backend/                  Spring Boot API (Java 21)
smart-pos-frontend/       React + Vite web UI
docker-compose.prod.yml   Postgres + backend + Nginx (full Docker)
how-to-run.txt            One-page quick-start cheatsheet
```

See [`backend/README.md`](backend/README.md) for the full API reference and
error-envelope format.

## Tests

```powershell
cd backend
mvn test
```
