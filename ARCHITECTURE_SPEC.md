# Raviranjan.OS — Production System Architecture & Technical Specification

**Version:** 1.0.0-PROD-SPEC  
**Status:** Approved  
**Author:** Raviranjan / Systems Architect  
**Platform:** Raviranjan.OS (Personal Developer Operating System)

---

## Executive Summary

Raviranjan.OS is a unified developer platform combining a public technical portfolio, a personal CMS, project progress tracking, a technical writing engine, an engineering lab, and a multi-platform integration subsystem. It enforces strict separation between manual content authored via the admin CMS and synchronized external telemetry from platforms like GitHub, LeetCode, and GeeksforGeeks.

---

## 1. System Architecture & Tech Stack

### 1.1 Technology Decisions

| Tier | Technology | Rationale |
| :--- | :--- | :--- |
| **Frontend Framework** | Next.js (App Router, React 18/19, TypeScript) | Server Components for instant initial paint, Client Components for interactive telemetry dashboards and CMS. |
| **UI & Styling** | Tailwind CSS + shadcn/ui | Minimal, dark-first, dense developer aesthetic inspired by Linear and Vercel. |
| **Client State & Data Fetching** | TanStack Query v5 | Automated caching, background refetching, and optimistic mutations for admin CRUD. |
| **Visualizations** | Recharts | Composable SVG-based activity heatmaps, commit charts, and problem-solving distributions. |
| **Animations** | Framer Motion | Subtle micro-interactions, layout transitions, and status indicator pulses. |
| **Backend Framework** | Java 21 + Spring Boot 3.3.x | Enterprise-grade concurrency (Virtual Threads), robust security filters, strong typing, and rich JPA ecosystem. |
| **Security & Auth** | Spring Security + JJWT | Dual-token authentication (HttpOnly refresh cookie + short-lived JWT) and method-level RBAC. |
| **Relational Database** | PostgreSQL 16 | ACID compliance, JSONB capabilities for schema flexibility, and reliable indexing. |
| **Cache & Distributed Bus** | Redis 7 | L2 cache for public endpoints, distributed rate-limiting tokens, and pub/sub message bus. |
| **Realtime Telemetry** | Server-Sent Events (SSE) | Lightweight, single-direction HTTP streaming for live developer activity and sync logs. |
| **Artificial Intelligence** | Spring AI | RAG (Retrieval-Augmented Generation) query engine over verified portfolio knowledge chunks. |
| **Container & CI/CD** | Docker, Docker Compose, GitHub Actions | Isolated local development, immutable production multi-stage builds, automated linting and testing. |

---

## 2. Invariant Architectural Principles

### 2.1 Content Provenance Separation
1. **Manual Content Subsystem**: Everything authored by the developer (articles, research papers, project case studies, architectural decision records, career milestones) is managed exclusively via the Admin CMS (`/admin`), stored in PostgreSQL, and exposed through `/api/v1/public/**`.
2. **External Data Integration Subsystem**: Third-party activity (commits, pull requests, algorithmic problem counts, live telemetry) is ingested exclusively through `IntegrationProvider` implementations, normalized into standard entities, and stored in PostgreSQL with Redis caching.
3. **No Frontend Hardcoding**: Every UI section on the public site reads dynamically from API endpoints. If an external service is unavailable, stale-while-revalidate data is shown with a degraded health badge.

---

## 3. Database Schema Overview (27 Functional Domains)

The database schema is managed via Flyway migrations and partitioned into logical schemas:

1. `core`: `profiles`, `social_links`, `site_settings`, `homepage_sections`
2. `projects`: `projects`, `project_milestones`, `project_tasks`, `project_changelogs`, `project_technical_decisions`, `engineering_lab`
3. `career`: `skill_categories`, `skills`, `experiences`, `educations`, `achievements`, `certifications`, `dsa_notes`
4. `publications`: `articles`, `article_tags`, `article_tag_mappings`, `research`, `research_tags`, `research_tag_mappings`
5. `integrations`: `integration_configs`, `integration_sync_logs`, `github_repos`, `github_contributions`, `leetcode_stats`, `gfg_stats`, `unified_activities`, `contribution_stories`
6. `operations`: `contact_inquiries`, `whatsapp_conversations`, `whatsapp_messages`, `notifications`, `site_analytics`, `audit_logs`, `ai_interactions`

---

## 4. Integration Provider Abstraction

Each external service implements:

```java
public interface IntegrationProvider<C extends ProviderConfig, D extends SyncResult> {
    ProviderType getProviderType();
    ConnectionStatus connect(C config);
    void disconnect();
    D sync();
    ProviderHealth getStatus();
    Instant getLastSyncTimestamp();
    void handleError(Throwable throwable, SyncContext context);
    RetryPolicy getRetryPolicy();
}
```

- **GitHubProvider**: Reads public repositories, stars, commit distribution, and PRs.
- **LeetCodeProvider**: Ingests total solved counts, ranking, and difficulty distribution via GraphQL.
- **GFGProvider**: Ingests verified contest scores, coding scores, and problem counts.
- **WhatsAppProvider**: Dispatches notification alerts and ingests recruiter messages via Meta Cloud API.
- **EmailProvider**: Sends automated confirmations and notifications.
- **AnalyticsProvider**: Tracks privacy-friendly, anonymized visitor events.
- **AIProvider**: Answers recruiter queries using grounded RAG knowledge retrieval.

---

## 5. Security & RBAC Model

- `ROLE_PUBLIC`: Access to `/api/v1/public/**` (read-only, cached).
- `ROLE_RECRUITER`: Access to `/api/v1/public/recruiter/**` and verified dossier downloads.
- `ROLE_ADMIN`: Access to `/api/v1/admin/**` and `/api/v1/integrations/**` (full CRUD, configuration, sync triggering).

---

## 6. Implementation Phases

- **Phase 1**: Core Foundation & Infrastructure (Database, Docker Compose, Spring Boot setup, Security).
- **Phase 2**: Domain Models, Repositories, Services, and Admin CRUD APIs.
- **Phase 3**: Integration Engine, External Providers (GitHub, LeetCode, GFG), and Unified Activity Stream.
- **Phase 4**: Next.js Frontend Architecture, Design System, and Public Portfolio Routes.
- **Phase 5**: Admin CMS Dashboard, Live Telemetry, Real-time SSE, and CRM Module.
- **Phase 6**: AI Portfolio Assistant, Production Hardening, and CI/CD Automation.
