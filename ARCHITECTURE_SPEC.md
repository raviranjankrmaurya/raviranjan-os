# Architecture Specification — Raviranjan.OS (v3.2)

## 1. System Overview
Raviranjan.OS is an enterprise-grade developer operating system and live telemetry platform engineered for Raviranjan Kumar. It bridges high-throughput Spring Boot microservices with a modern, high-contrast reactive web interface and a comprehensive 15-module Executive Admin CMS.

```
                           [ Public Client / Browser ]
                                        |
                         +--------------+--------------+
                         |                             |
                 (Vercel Edge Proxy)          (Direct localhost)
                         |                             |
                         +--------------+--------------+
                                        |
                             [ Express Gateway: 3000 ]
                                        |
                     +------------------+------------------+
                     |                                     |
               Static Assets                        /api Proxy
          (index.html, admin_cms.html)                     |
                                                           v
                                            [ Spring Boot Backend: 8080 ]
                                            - Java 21 Virtual Threads
                                            - JWT Security Filter Chain
                                            - Flyway DB Migrations
                                            - GitHubProvider Telemetry
                                                           |
                                      +--------------------+--------------------+
                                      |                                         |
                            (Neon PostgreSQL DB)                          (External APIs)
                            - contact_inquiries                          - GitHub REST & Scrape
                            - ai_query_logs                              - LeetCode GraphQL
                            - projects (NOT NULL slug)                   - GeeksforGeeks Telemetry
                            - developer_profile & settings
```

---

## 2. Core Modules & Endpoints

### 2.1 Public User Portfolio (`index.html`)
- **Hero & Identity**: Real-time terminal command box (`curl -s https://raviranjan.dev/api/v1/profile | jq`), 3 hero cards (`01 - IDENTITY`, `02 - CORE TECH MATRIX`, `03 - AVAILABILITY`).
- **Telemetry Engine**: 100% real live sync with GitHub (144 contributions in the last year), LeetCode (31 solved, 11-day streak), and GeeksforGeeks (234 score, 68 solved).
- **Skills Matrix**: 5 dedicated sections:
  1. Languages & Core Runtimes
  2. Backend & Microservices
  3. Databases, Storage & Vectors
  4. DevOps, Containers & Systems
  5. AI, RAG & LLM Engineering
- **AI Intelligence Widget**: Connected to `POST /api/ai/chat`. Transmits client IP, geolocation, and browser user agent. Enforces rate-limiting (max 10 queries per 10 mins).
- **Contact & CRM**: Connected to `POST /api/contact`. Transmits client IP, geolocation, browser user agent, subject, and message. Enforces rate-limiting (max 3 messages per 10 mins).

### 2.2 Executive Admin CMS (`admin_cms.html`)
- **Restricted Access Gate**: Unauthorized visits to `/admin` show strictly:
  `"Restricted, sorry you don’t have permission to load this page."`
  Zero credentials or OTP inputs exposed. Secret unlock via triple-click on portfolio logo or typing `admin` on the keyboard.
- **Homepage Builder**: Direct in-place editing of headlines, taglines, badges, 3 hero cards, and live CSS theme colors (`--brand`, `--surface`, `--bg`, `--text-main`, `--border`).
- **Profile & Bio Editor**: Live customization of developer bio, contact details, social URLs, and the terminal curl command.
- **AI Config & User Chat Logs**: Live audit table tracking every visitor chat query, AI response, IP address, location, browser OS, and working delete buttons.
- **Inquiries & CRM Leads**: Live table of contact form submissions with client IP, location, browser, and working delete buttons.
- **Universal CRUD Engine with "Are you sure?" Modal**:
  - Deleting any Project, Skill, Lab Experiment, DSA Note, Career Entry, Achievement, Writing Article, Research Paper, Story, Inquiry, or AI Log triggers an **"Are you sure?" confirmation popup modal** before deletion.
  - Project creation automatically generates valid `slug` (`title.toLowerCase().replace(...)`) to satisfy PostgreSQL `slug VARCHAR(255) NOT NULL UNIQUE`.
  - Skills Matrix editor allows in-place adding, editing, and deleting across all 5 sections without redirecting to `project-editor`.

---

## 3. Database Schema (Neon PostgreSQL)

```sql
-- 1. Contact Inquiries Table
CREATE TABLE contact_inquiries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    message TEXT NOT NULL,
    user_ip VARCHAR(64),
    location VARCHAR(255),
    browser TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. AI Query Audit Logs
CREATE TABLE ai_query_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id VARCHAR(128),
    user_query TEXT NOT NULL,
    ai_response TEXT NOT NULL,
    user_ip VARCHAR(64),
    location VARCHAR(255),
    browser TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. Projects Repository
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    slug VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    technologies VARCHAR(255),
    description TEXT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    github_url VARCHAR(512),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. Developer Profile & Theme Settings
CREATE TABLE site_settings (
    id VARCHAR(64) PRIMARY KEY,
    brand_color VARCHAR(16) DEFAULT '#10b981',
    card_color VARCHAR(16) DEFAULT '#0f172a',
    bg_color VARCHAR(16) DEFAULT '#030712',
    text_color VARCHAR(16) DEFAULT '#f8fafc',
    border_color VARCHAR(16) DEFAULT '#1e293b',
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

---

## 4. Security & Compliance
- **JWT Authentication**: RS256 / HS256 stateless tokens with 24-hour expiration.
- **Rate-Limiting**:
  - Public Contact: Max 3 messages in 10 minutes per IP/session.
  - Public AI Chat: Max 10 messages in 10 minutes per IP/session.
  - Admin OTP Generation: Max 2 attempts in 10 minutes.
- **Audit Stream**: Real-time telemetry tracking Flyway V5 migrations, database connection pools, and GitHub commits.
