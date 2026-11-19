# Backend review — what was changed, what to add, what to remove

## Fixed in this update
| # | Problem found | Fix |
|---|---|---|
| 1 | **`/api/admin/**` was `permitAll()`** and the JWT filter never rejected anything: anyone could read CRM data and delete content | `SecurityConfig`: admin API requires `ROLE_ADMIN`/`ROLE_SUPER_ADMIN`; unauthenticated calls get `401`. Kill-switch `APP_SECURITY_ENFORCE_ADMIN_AUTH=false` (debugging only) |
| 2 | **JWT secret default is published in the repo** (`404E6352...`). While in use, admin tokens can be forged and stored integration credentials decrypted | Startup now logs an ERROR; set `APP_SECURITY_REQUIRE_CUSTOM_JWT_SECRET=true` to refuse to start. **You must set `JWT_SECRET` on Render** |
| 3 | GitHub webhook accepted unsigned requests (anyone could trigger syncs) | Signature is mandatory; configure the webhook secret in Admin → Integrations |
| 4 | CORS allowed any origin **with credentials** | credentials only with an explicit list: `APP_CORS_ALLOWED_ORIGINS=https://oneaios.vercel.app` (default `*` without credentials, so nothing breaks) |
| 5 | AI: "hello" dumped the portfolio (retriever returned the first chunks when nothing matched), every message ran 8 `findAll()` queries and sent the **whole** portfolio to the model, 3 different identities, invented fallbacks ("Java 21, Spring Boot…", "India") | `IntentClassifier`, `AnswerBuilder`, `ResponsePolicy`, strict retriever, topic-only data loading, one identity, exact unknown reply, drafts/archived projects never exposed, Gemini key sent as header |
| 6 | GitHub heatmap: fabricated contributions when a year was empty, 2025 data relabelled as 2026, years hard-coded | `HeatmapSupport` (real data only), dynamic years `heatmapByYear`/`heatmapYears`, provider keeps last good data |
| 7 | Telemetry returned `"Connected"`, `"Real-time"`, `"Synced"`, `"Active Profile"` and a hard-coded GfG username | real `status` / `lastSync` (null when never synced) |
| 8 | `DatabaseSeeder` wrote fake column defaults (`India`, `Delhi NCR / Bihar`, `New Delhi`) and back-filled `origin='https://raviranjan.dev'` on every start; first sync blocked startup | defaults dropped, back-fill removed, first sync runs in the background (faster Render wake-up) |

## Add (recommended next)
1. `JWT_SECRET`, `APP_CORS_ALLOWED_ORIGINS`, `GITHUB webhook secret` on Render.
2. `management.endpoint.health.show-details: never` and expose only `health` (the admin status chips work with either).
3. Rate limiting for `/api/contact` and `/api/ai/chat` (verify it exists; the AI endpoint calls an external LLM).
4. A `heatmapByYear`-style API for LeetCode/GfG calendars if you want the same treatment.
5. Optional: real token streaming from the LLM into `PublicAiStreamController` (today it chunks the finished answer).

## Remove / switch off
1. Demo content seeding: set `APP_SEED_DEMO_CONTENT=false` once your real content exists (otherwise deleted sample rows can reappear on restart).
2. Browser-side visitor IP lookups in `main_app.js` (`ipwho.is`, `ipapi.co`, `ipify`): the backend already has GeoIP; these leak visitor IPs to third parties.
3. Secret keywords in the public JS that reveal the admin gate (`sudo admin`, …) — obscurity, not security.
4. The dead `legacy heatmap2025/heatmap2026` response keys once all clients are updated.

## Not verified
No Maven/Spring toolchain was available. Pure classes (`IntentClassifier`, `AnswerBuilder`, `ResponsePolicy`, `TechNames`, `HeatmapSupport`) are unit-tested; `AiChatEngine`, `PortfolioRagRetriever` and `AiAssistantServiceImpl` were compiled and exercised against stubs. `SecurityConfig`, `GitHubProvider`, `PublicActivityController`, `DatabaseSeeder` and `JwtTokenProvider` edits could **not** be compiled here — run `mvn test` and log in to the admin before deploying.
