## Deploy Smart POS ke VPS (backend + web + Postgres bersama)

Stack aplikasi memakai [`docker-compose.prod.yml`](docker-compose.prod.yml): Spring Boot + Nginx untuk frontend. **PostgreSQL tidak lagi di bundle**; satu container Postgres di VPS dipakai bersama banyak proyek (tiap aplikasi punya **database sendiri** di instance yang sama).

Request `/api/v1/*` dari Nginx diproxy ke backend. Backend menyambung ke Postgres lewat **Docker network bersama** (`shared_db_net`).

### 0) Prasyarat VPS

- OS: Ubuntu 22.04/24.04 (contoh; distro lain konsepnya sama)
- Domain (opsional, disarankan): `pos.example.com`
- Port yang dibuka: `80` (dan `443` jika HTTPS)
- **Postgres bersama** sudah jalan dan backend Smart POS satu jaringan Docker dengannya (lihat bagian 2–3)

### 1) Install Docker + Compose plugin

```bash
docker --version
docker compose version
```

### 2) Postgres bersama (sekali per VPS, dipakai banyak proyek)

**Jaringan Docker** — buat sekali (abaikan jika sudah ada dari stack infra lain):

```bash
docker network create shared_db_net
```

Atau pastikan network yang sama dipakai oleh container Postgres Anda (nama harus cocok dengan [`docker-compose.prod.yml`](docker-compose.prod.yml): default external `shared_db_net`).

**Contoh compose infra** — salin [`docker-compose.postgres.shared.example.yml`](docker-compose.postgres.shared.example.yml) ke lokasi terpisah di VPS (misalnya `/opt/shared-postgres/`), buat `.env.postgres` dari [`.env.postgres.example`](.env.postgres.example) (set `POSTGRES_SUPERUSER_PASSWORD`), lalu:

```bash
docker compose -f docker-compose.postgres.shared.example.yml --env-file .env.postgres up -d
```

Jangan publish port `5432` ke internet; cukup antar-container di jaringan Docker.

**Bootstrap database untuk Smart POS** (jalankan sebagai superuser Postgres, misalnya user `postgres` di dalam container):

```bash
docker exec -it postgres_shared psql -U postgres -c "CREATE USER smart_pos WITH PASSWORD 'GANTI_PASSWORD_KUAT';"
docker exec -it postgres_shared psql -U postgres -c "CREATE DATABASE smart_pos OWNER smart_pos;"
```

Sesuaikan nama container (`postgres_shared` mengikuti contoh compose infra), user, dan password. Untuk produksi, **disarankan** user aplikasi hanya punya hak pada database proyek ini, bukan superuser.

### 3) Siapkan environment aplikasi

Dari root repo:

```bash
git clone <repo-url> smart-pos
cd smart-pos
cp .env.prod.example .env.prod
```

Edit `.env.prod`:

- **Wajib**: `JWT_SECRET` (minimal ~32 byte), dan kredensial yang sama dengan yang dipakai di Postgres untuk user/database Smart POS
- **`DATABASE_URL`**: host harus **nama service atau hostname container Postgres di jaringan `shared_db_net`** (contoh: `jdbc:postgresql://postgres:5432/smart_pos` jika service bernama `postgres`)
- **`DATABASE_USERNAME`** / **`DATABASE_PASSWORD`**: user SQL untuk database `smart_pos`
- `WEB_PORT=80` (atau `8080` jika ada reverse proxy di depan)

### 4) Jalankan stack aplikasi (build & up)

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Cek status:

```bash
docker compose -f docker-compose.prod.yml --env-file .env.prod ps
docker logs smart_pos_backend --tail=200
docker logs smart_pos_web --tail=200
```

### 5) Akses aplikasi

- UI: `http://<IP-VPS>/`
- API: `http://<IP-VPS>/api/v1`
- Swagger: `http://<IP-VPS>/swagger-ui.html`

Untuk produksi, **disarankan HTTPS** (bagian 7).

### 6) Update / redeploy (hanya aplikasi)

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

Upgrade major PostgreSQL atau backup volume instance bersama dilakukan **di stack Postgres infra**, terpisah dari deploy repo ini.

### 7) HTTPS (disarankan)

Reverse proxy di host (Caddy / Nginx / Traefik) dengan Let's Encrypt, proxy ke `WEB_PORT` stack ini.

**Caddy** — jika aplikasi listen di `8080`:

```caddyfile
pos.example.com {
  reverse_proxy 127.0.0.1:8080
}
```

### 8) Backup & restore (per database `smart_pos`)

Gunakan **nama container Postgres bersama** Anda (contoh: `postgres_shared`), dan **satu database** agar tidak tercampur proyek lain.

Backup:

```bash
docker exec -t postgres_shared pg_dump -U smart_pos -d smart_pos > smartpos_$(date +%F).sql
```

Restore:

```bash
cat smartpos_2026-04-30.sql | docker exec -i postgres_shared psql -U smart_pos -d smart_pos
```

Backup seluruh volume data Postgres (semua database di instance) tetap dilakukan di tingkat volume/container infra jika Anda ingin recovery penuh.

### 9) Hardening minimum

- Jangan publish Postgres ke publik
- Jangan commit `.env.prod`
- HTTPS + firewall (UFW): misalnya hanya 22, 80, 443
- Rotasi `JWT_SECRET` akan logout semua sesi

### 10) Full Docker di mesin lokal (Postgres ikut di compose)

Tanpa Postgres bersama, Anda bisa menjalankan DB lokal dengan override:

```bash
docker compose -f docker-compose.prod.yml -f docker-compose.local-db.override.yml --env-file .env.prod up -d --build
```

Lihat [`docker-compose.local-db.override.yml`](docker-compose.local-db.override.yml). Untuk hybrid dev (Postgres di [`backend/docker-compose.yml`](backend/docker-compose.yml) + `mvn`/`npm`), tidak perlu file ini.
