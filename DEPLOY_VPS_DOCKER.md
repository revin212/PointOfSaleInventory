## Deploy Smart POS (frontend + backend + DB) ke VPS pakai Docker

Dokumen ini memakai `docker-compose.prod.yml` (Postgres + Spring Boot + Nginx).
Frontend di-serve oleh Nginx dan request `/api/v1/*` diproxy ke backend.

### 0) Prasyarat VPS

- OS: Ubuntu 22.04/24.04 (panduan ini contoh Ubuntu; untuk distro lain konsepnya sama)
- Domain (opsional tapi sangat disarankan): `pos.example.com`
- Port yang dibuka: `80` (dan `443` kalau pakai HTTPS)

### 1) Install Docker + Compose plugin

Ikuti dokumentasi resmi Docker (disarankan). Setelah terpasang, pastikan:

```bash
docker --version
docker compose version
```

### 2) Upload kode ke VPS

Opsi paling umum:

- `git clone` repo ke VPS, atau
- CI/CD build & pull image (lebih advanced)

Contoh:

```bash
git clone <repo-url> smart-pos
cd smart-pos
```

### 3) Siapkan environment production

Copy template env:

```bash
cp .env.prod.example .env.prod
```

Lalu edit `.env.prod`:

- **wajib**: `JWT_SECRET` (minimal 32 bytes) dan `POSTGRES_PASSWORD` yang kuat
- set `WEB_PORT=80` (atau kalau ada reverse-proxy lain, bisa `8080`)

### 4) Jalankan stack (build & up)

Dari root repo:

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

Catatan:
- Untuk produksi, **disarankan HTTPS** (lihat bagian 7).

### 6) Update / redeploy

```bash
git pull
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d --build
```

### 7) HTTPS (opsi disarankan)

Pendekatan termudah: taruh reverse proxy di depan stack ini (Caddy / Nginx / Traefik)
yang handle Let's Encrypt. Dua opsi praktis:

#### Opsi A: Caddy (paling sederhana)

Jalankan Caddy di host (bukan di compose) atau sebagai container terpisah, lalu proxy ke `WEB_PORT` kamu.
Jika stack ini expose `WEB_PORT=8080`, Caddy bisa listen di 80/443 dan proxy ke `127.0.0.1:8080`.

Contoh `Caddyfile`:

```caddyfile
pos.example.com {
  reverse_proxy 127.0.0.1:8080
}
```

#### Opsi B: Nginx + Certbot

Sama konsepnya: Nginx host listen 80/443 lalu proxy ke `127.0.0.1:8080`.

### 8) Backup & restore database (Postgres volume)

Backup:

```bash
docker exec -t smart_pos_db pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" > smartpos_$(date +%F).sql
```

Restore (contoh):

```bash
cat smartpos_2026-04-30.sql | docker exec -i smart_pos_db psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

### 9) Hardening minimum yang disarankan

- Jangan publish port Postgres ke publik (compose ini tidak publish)
- Pastikan `.env.prod` tidak di-commit
- Pakai HTTPS
- Atur firewall (UFW): buka hanya 22, 80, 443
- Rotasi `JWT_SECRET` bila perlu (akan logout semua sesi)
