# Persistent cache-first architecture (SWR) — Raviranjan.OS

## Why
Render puts the backend to sleep. The first request after a quiet period takes 30-60 s, so visitors saw blank sections.
Now every public section renders **immediately** from cache and is refreshed in the background.

## Architecture (3 layers)

| Layer | Where | What it stores | Shared? |
|---|---|---|---|
| 1. Shared public cache | `frontend/api/[...path].js` + `api/_lib/shared-cache.js` (Vercel function) | last good response of every public GET endpoint, in Vercel KV / Upstash (plain `fetch`, no npm package). Falls back to per-instance memory and to the build-time seeds `public/cache/*.json` | **Yes, all visitors** |
| 2. Browser persistent cache | `frontend/public/data-layer.js` (IndexedDB `raviranjan-os-cache`) | last good response per URL, `{version, updatedAt, fetchedAt, source, hash, data}` | per browser |
| 3. Fetching + revalidation | `data-layer.js` (wraps `window.fetch` for public `GET /api/*`) | nothing; de-duplicates, times out, validates, retries with back-off | – |

TanStack Query was **not** used: the site is plain HTML/JS (no React). The same behaviour is implemented in ~350 lines without a dependency.
Vercel's CDN adds a fourth, free layer: proxied 200 responses carry `s-maxage=20, stale-while-revalidate=300`.

## Data flow
```
visitor opens site
  └─ loader calls fetch('/api/profile')            (unchanged loader code)
       └─ data-layer.js
            1. IndexedDB entry?  ── yes ─► return it NOW  ──► background revalidate (if older than 60 s)
            2. no entry: /cache/profile.json (CDN seed) ──► return it NOW ──► background revalidate
            3. nothing at all: wait once for the network (max 12 s)
background revalidate:  GET /api/profile  ─► Vercel function
       a) backend answers within 7 s, valid + non-empty ─► respond "live", write KV snapshot
       b) asleep / 5xx / junk / empty ─► respond with KV snapshot ("shared-cache") or seed ("seed")
browser:  valid + changed ─► save to IndexedDB, re-run the loader (UI updates, no reload)
          source=shared-cache/seed or failure ─► keep cache, retry with exponential back-off (4s,8s,16s,32s,60s, max 5)
          tab becomes visible / network returns ─► one new revalidation cycle (no continuous polling)
```

## Rules that protect good data
* Only `{success !== false, data: non-empty}` replaces a cache entry. Junk, HTML, 5xx, timeouts and empty answers never overwrite it.
* An "empty" answer is accepted only after it was seen **3 times over ≥ 15 minutes** (`EMPTY_CONFIRM_MS`), so a bad moment cannot wipe the cache, but deleted content does eventually disappear.
* Older responses cannot overwrite newer ones (`fetchedAt` check in the shared cache).
* `version` mismatch, unparsable timestamp, future timestamp, > 90 days old or malformed entries are discarded.
* Only public GET endpoints from an allow-list are cached. Anything with `Authorization`, cookies, `/api/admin`, `/api/v1/auth`, POSTs and SSE is passed through untouched.
* Visitors can never write the shared cache; only server code that just received a verified backend response (or `/api/cache-refresh` with a secret) writes it.

## Indicator
A small pill in the status strip and the footer: **Updating…** (first revalidation), **Live · 2 min ago**, **Showing recently updated data** (backend unreachable; tooltip = `Last updated: <time>`). Empty hero fields show a shimmer skeleton until the profile is known.

## Files
New: `frontend/public/data-layer.js`, `frontend/api/_lib/shared-cache.js`, `frontend/api/cache-refresh.js`, `frontend/scripts/snapshot-cache.mjs`, `frontend/cache-paths.json`, `.github/workflows/cache-refresh.yml` (optional), `docs/*`.
Changed: `frontend/api/[...path].js`, `frontend/public/main_app.js` (loaders registered with the layer; old polling loop removed), `index.html`, `design-system.css/js`, `frontend/vercel.json`, `frontend/package.json`.
Dependencies: **none added or removed.**

## Environment variables (Vercel project)
| Variable | Purpose |
|---|---|
| `BACKEND_API_URL` | Render URL (already used) |
| `KV_REST_API_URL`, `KV_REST_API_TOKEN` (or `UPSTASH_REDIS_REST_URL/_TOKEN`) | durable shared cache. Create a free Vercel KV / Upstash Redis and connect it. Without it the shared layer is per-instance memory + seeds |
| `BACKEND_TIMEOUT_MS` | how long the proxy waits for a sleeping backend before serving the snapshot (default 7000) |
| `CACHE_REFRESH_SECRET` | enables `POST /api/cache-refresh` (Bearer secret) |
| `EMPTY_CONFIRM_MS` | how long an "empty" answer must persist before it is believed (default 900000) |
| `SNAPSHOT_TIMEOUT_MS` | build-time seed script patience for a cold backend (default 70000) |

## Deployment steps
1. Add the variables above in Vercel and redeploy. The build runs `scripts/snapshot-cache.mjs`, which writes `public/cache/*.json` (it never fails the build).
2. **Fill the shared cache once:** `curl -X POST https://<site>/api/cache-refresh -H "Authorization: Bearer $CACHE_REFRESH_SECRET"` (wakes Render and stores every endpoint). Without a seed, KV snapshot or IndexedDB entry the very first visitor of a brand-new deployment still has to wait for Render.
3. Optional: enable `.github/workflows/cache-refresh.yml` (secrets `SITE_URL`, `CACHE_REFRESH_SECRET`) to refresh every 30 min. This also keeps Render mostly awake; delete the file if you prefer it to sleep.

## Tests (all run in a real browser against the real Vercel handler, a mock backend and a mock KV)
Scenarios 1-10 of the brief: first visit (seed / shared cache), returning visit, backend sleeping, backend waking after 7 s (back-off verified, UI updates without reload), fresh data, junk/failed/5xx/empty responses, F5, multiple visitors, shared-cache update visible to the next visitor, cache-version change and a corrupted entry — **38/38 browser checks + 22/22 proxy checks passed**.

## Limitations
* Mock backend timings were scaled down; real Render cold starts (30-60 s) were not measured.
* A brand-new deployment with no seed, KV snapshot or IndexedDB entry still shows skeletons until Render answers (step 2 above avoids it).
* Without KV the shared layer is per serverless instance (not durable across cold starts); seeds still protect first visits.
* IndexedDB is unavailable in some private modes; the layer then keeps the cache in memory for the session.
* Cached data can be up to 20 s (CDN) + the revalidation interval old; deleted content disappears only after the 15-minute empty confirmation.
