# Raviranjan.OS — Personal Developer Operating System & Headless CMS

> **A production-grade developer platform combining public technical portfolio, headless personal CMS, architectural decision records (ADRs), real-time external telemetry (GitHub, LeetCode, GFG), Contact Intelligence & AI Abuse Moderation, and a grounded RAG assistant.**

## 1. Quick Start (Single Command)

### Prerequisites
* Docker Engine & Docker Compose (v2.0+)

### Start Full Stack:
```bash
docker compose up --build -d
```

### Access Endpoints:
* **Public Developer OS**: `http://localhost:3000` (or `http://localhost:8080/index.html`)
* **Admin Operating System**: `http://localhost:3000/admin` (or `http://localhost:8080/admin_cms.html`)
* **Spring Boot REST APIs**: `http://localhost:8080`
* **OpenAPI 3.0 / Swagger UI**: `http://localhost:8080/swagger-ui.html`

## 2. Default Credentials & Stealth Admin Access

* **Secret Shortcut Trigger**: Press **`Ctrl + Shift + A`** (or **`Cmd + Shift + A`** on macOS) anywhere on the public website to summon the terminal authentication modal.
* **Default Username**: `raviranjan`
* **Default Email**: `admin@raviranjan.dev`
* **Default Password**: `Admin@123456`
* **Assigned Role**: `ROLE_SUPER_ADMIN`

## 3. Contact Intelligence, Rate Limiting & AI Abuse Prevention

### Database Migration Instructions
* **Automatic Schema Sync**: In `application.yml`, `spring.jpa.hibernate.ddl-auto=update` automatically adds the Contact Intelligence columns (`country`, `region`, `city`, `latitude`, `longitude`, `geo_provider`, `referrer`, `browser_hash`, `message_hash`, `disposable_email`, `repeat_submission`, `repeat_count`, `intent`, `is_spam`, `is_abusive`, `is_threatening`, `moderation_confidence`, `moderation_reason`, `recommended_action`, `moderation_status`) to the existing `contact_inquiries` table without losing any existing records.
* **Explicit SQL Migration**: For manual or Flyway-managed PostgreSQL deployments, apply `backend/src/main/resources/db/migration/V5__contact_intelligence_and_moderation.sql`:
  ```bash
  psql "$SPRING_DATASOURCE_URL" -f backend/src/main/resources/db/migration/V5__contact_intelligence_and_moderation.sql
  ```

### Environment Variables
| Variable | Default | Purpose |
| :--- | :--- | :--- |
| `CONTACT_RATE_LIMIT_MAX` | `3` | Maximum allowed contact submissions per client IP in the rolling window |
| `CONTACT_RATE_LIMIT_WINDOW_SECONDS` | `600` | Rolling rate-limit window in seconds (10 minutes) |
| `TRUST_INTERNAL_PROXIES` | `true` | Trust loopback and internal Docker reverse proxy hops when resolving `X-Forwarded-For` |
| `TRUSTED_PROXY_IPS` | `""` | Comma-separated list of explicit trusted reverse proxy IPs |
| `GEOIP_EXTERNAL_ENABLED` | `false` | Enable optional external HTTPS GeoIP fallback (`ipwho.is`) with 1.5s timeout |
| `CONTACT_METADATA_RETENTION_DAYS` | `90` | Retention window in days before IP, User-Agent, and browser hash are scrubbed |
| `GEMINI_API_KEY` | `""` | Gemini API key for LLM-based structured JSON moderation and RAG chat |

### How to Test the Rate Limit (3 Submissions per 10 Minutes)
Run the following command 4 times within 10 minutes:
```bash
for i in 1 2 3 4; do
  echo "--- Request #$i ---"
  curl -i -s -X POST http://localhost:8080/api/contact \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"Alice Recruiter\",\"email\":\"alice$i@company.com\",\"message\":\"Interested in discussing a backend software engineer role (#$i).\"}" | head -n 12
done
```
* Requests **#1, #2, and #3** return `HTTP/1.1 201 Created`.
* Request **#4** returns `HTTP/1.1 429 Too Many Requests` along with a `Retry-After: <seconds>` HTTP header.
* Malformed requests (e.g. `"name":"A"` or `"email":"invalid"`) return `HTTP/1.1 400 Bad Request` and **do not** increment the rate-limit counter.

### How to Verify Client IP Detection
* When behind the trusted frontend proxy (`frontend/server.js`), test forwarding a public IP:
  ```bash
  curl -s -X POST http://localhost:3000/api/contact \
    -H "Content-Type: application/json" \
    -H "X-Forwarded-For: 49.36.128.15" \
    -d '{"name":"Hiring Lead","email":"lead@tech.in","message":"We have an internship opportunity at our Greater Noida office."}'
  ```
* Open `/admin` -> **Contact & CRM** to verify that `49.36.128.15` (and not the internal Docker container bridge IP `172.18.0.x`) is recorded along with approximate city/country (`New Delhi, Delhi, India`).

### Limitations of IP Geolocation, Email Validation & Browser Signals
* **IP Geolocation**: Provides only coarse, approximate city/region/country routing based on ISP allocation blocks. It never identifies a visitor's physical home address, can reflect VPNs/mobile carrier gateways, and is intentionally skipped for private/loopback IPs (`127.0.0.1`, `172.18.x.x`, `192.168.x.x`).
* **Name & Email Validation**: Syntax validation and disposable-domain checks confirm structural validity only; they do not prove real-world identity or mailbox ownership.
* **Browser Signal Correlation**: Used strictly as an optional, privacy-disclosed signal to correlate repeated spam bursts; different names from a shared corporate/campus NAT IP are never assumed to be the same individual.