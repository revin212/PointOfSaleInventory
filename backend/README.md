# Smart POS Backend

Spring Boot (Java 21) backend for the Smart POS + Inventory app.

## Stack
- Spring Boot 3.4 (Web, Security, Validation, Data JPA)
- PostgreSQL 16 + Flyway
- JWT auth (access + refresh, with rotation)
- BCrypt password hashing
- springdoc-openapi (Swagger UI)
- JUnit 5 + Spring Security Test (H2 in PostgreSQL mode for tests)

## Prerequisites
- JDK 21
- Maven 3.9+
- Docker (for local Postgres)

## Run locally
```powershell
# 1) Start Postgres
docker compose up -d

# 2) Run the backend
mvn spring-boot:run
```

API base path: `http://localhost:8080/api/v1`
Swagger UI: `http://localhost:8080/swagger-ui.html`

## Default seeded users
On first start (when `app.seed.enabled=true` and `users` is empty), the seeder creates:

| Role      | Email                         | Password       |
|-----------|-------------------------------|----------------|
| OWNER     | owner@smartpos.local          | Password123!   |
| CASHIER   | cashier@smartpos.local        | Password123!   |
| WAREHOUSE | warehouse@smartpos.local      | Password123!   |

Change the seed password via `APP_SEED_PASSWORD` env var.

## Configuration
Environment variables (with defaults shown in `application.yml`):
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `JWT_SECRET` (min 32 bytes)
- `JWT_ACCESS_TTL` (seconds, default 900)
- `JWT_REFRESH_TTL` (seconds, default 1209600)
- `CORS_ALLOWED_ORIGINS` (default `http://localhost:5173`)
- `APP_SEED_ENABLED`, `APP_SEED_PASSWORD`

## Running tests
```powershell
mvn test
```

## API contract
See `c:\Users\Revin Dennis\.cursor\plans\backend-ai-api-contract.md` (canonical) and
`c:\Users\Revin Dennis\.cursor\plans\frontend-ai-api-contract.md` (frontend view).

## Module status
- [x] Foundation (project, security, error envelope, Flyway baseline)
- [x] Auth + Users (RBAC: OWNER/CASHIER/WAREHOUSE)
- [x] Categories / Suppliers / Products
- [x] Purchases (create + receive + stock movements)
- [x] Sales (create + cancel + stock movements + reversal)
- [x] Stock (on-hand / movements / adjustments)
- [x] Reports (daily / top-products)
- [x] Audit logging + OpenAPI bearer scheme

## Endpoints (v1)
All paths are prefixed with `/api/v1`. All non-auth endpoints require `Authorization: Bearer <accessToken>`.

### Auth
- `POST /auth/login`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`

### Users (OWNER only)
- `GET /users`, `POST /users`, `GET /users/{id}`, `PUT /users/{id}`, `PATCH /users/{id}/active`

### Master data
- Categories (OWNER, WAREHOUSE write; all read): `GET|POST /categories`, `GET|PUT|DELETE /categories/{id}`
- Suppliers (OWNER, WAREHOUSE): `GET|POST /suppliers`, `GET|PUT|DELETE /suppliers/{id}`
- Products (OWNER, WAREHOUSE write; all read): `GET|POST /products`, `GET|PUT|DELETE /products/{id}`

### Purchases (OWNER, WAREHOUSE)
- `GET|POST /purchases`, `GET /purchases/{id}`, `POST /purchases/{id}/receive`

### Sales (OWNER, CASHIER read/create; OWNER-only cancel)
- `GET|POST /sales`, `GET /sales/{id}`, `POST /sales/{id}/cancel`

### Stock
- `GET /stock/on-hand` (all roles), `GET /stock/movements` (OWNER, WAREHOUSE), `POST /stock/adjustments` (OWNER, WAREHOUSE)

### Reports (OWNER)
- `GET /reports/daily-summary?date=YYYY-MM-DD`
- `GET /reports/top-products?from=YYYY-MM-DD&to=YYYY-MM-DD&limit=10`

### Audit (OWNER)
- `GET /audit-logs` (filters: `entityType`, `action`, `userId`, `from`, `to`)

All error responses use the canonical envelope:
```json
{ "timestamp": "...", "status": 422, "code": "INSUFFICIENT_STOCK", "message": "...", "path": "/api/v1/sales", "details": [] }
```
