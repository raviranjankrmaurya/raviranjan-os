'use strict';
/**
 * Layer 1 — the shared public cache used by every visitor.
 *
 *  - Durable store : any Redis-compatible REST store (Vercel KV / Upstash). Enabled when KV_REST_API_URL and
 *                    KV_REST_API_TOKEN (or UPSTASH_REDIS_REST_URL / _TOKEN) are set. No npm dependency: plain fetch().
 *  - Fallback      : per-instance memory (not durable) + the static seed files /cache/*.json written at build time.
 *
 * Only server code in this folder writes to the store. There is NO public endpoint that accepts cache data.
 */
const crypto = require('crypto');

const PREFIX = 'raviranjan-os:cache:v1:';
const TTL_SECONDS = 60 * 60 * 24 * 30;
const MAX_BYTES = 1024 * 1024;
const VERSION = 1;
const EMPTY_ACCEPT_AFTER = 3;
/** An "empty" answer is only believed after it persisted for this long (default 15 min), so a few quick retries during a bad moment can never wipe good data. */
const emptyConfirmMs = () => (process.env.EMPTY_CONFIRM_MS !== undefined ? Number(process.env.EMPTY_CONFIRM_MS) : 15 * 60 * 1000);
const memory = new Map();
const lastWritten = new Map();

/** Mirror of PUBLIC_PATHS in public/data-layer.js. */
const PUBLIC_PATHS = [
  /^\/api\/profile$/, /^\/api\/settings$/, /^\/api\/projects(\/[A-Za-z0-9._-]+)?$/, /^\/api\/skills$/,
  /^\/api\/experiences$/, /^\/api\/education$/, /^\/api\/achievements$/, /^\/api\/certifications$/,
  /^\/api\/activity$/, /^\/api\/activity\/telemetry$/, /^\/api\/dsa\/problems$/, /^\/api\/research$/,
  /^\/api\/lab$/, /^\/api\/articles$/, /^\/api\/stories$/
];

/** @returns {string|null} normalised cache key, or null when the request must not be cached. */
function cacheKeyFor(method, rawUrl, headers) {
  if (String(method || 'GET').toUpperCase() !== 'GET') return null;
  const h = headers || {};
  if (h.authorization || h['x-admin-token'] || h.cookie) return null;           // personalised / private
  if (String(h.accept || '').includes('text/event-stream')) return null;
  let u;
  try { u = new URL(rawUrl, 'http://local'); } catch (e) { return null; }
  if (!PUBLIC_PATHS.some((re) => re.test(u.pathname))) return null;
  const params = Array.from(u.searchParams.entries()).sort((a, b) => (a[0] < b[0] ? -1 : 1));
  return u.pathname + (params.length ? '?' + params.map((p) => p[0] + '=' + p[1]).join('&') : '');
}

function hasContent(d) {
  if (d === null || d === undefined || d === '') return false;
  if (Array.isArray(d)) return d.length > 0;
  if (typeof d === 'object') return Array.isArray(d.content) ? d.content.length > 0 : Object.keys(d).length > 0;
  return true;
}
const isStructurallyValid = (j) => !!j && typeof j === 'object' && j.success !== false && 'data' in j;
const isUsable = (j) => isStructurallyValid(j) && hasContent(j.data);
const hashOf = (obj) => crypto.createHash('sha1').update(JSON.stringify(obj)).digest('hex');
const seedName = (key) => key.replace(/^\/api\//, '').replace(/[^a-z0-9]+/gi, '_').replace(/^_+|_+$/g, '') + '.json';

function kvConfig() {
  const url = process.env.KV_REST_API_URL || process.env.UPSTASH_REDIS_REST_URL;
  const token = process.env.KV_REST_API_TOKEN || process.env.UPSTASH_REDIS_REST_TOKEN;
  return url && token ? { url: url.replace(/\/$/, ''), token } : null;
}

async function kvCommand(cfg, command, timeoutMs) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), timeoutMs || 1500);
  try {
    const res = await fetch(cfg.url, {
      method: 'POST', signal: ctrl.signal,
      headers: { Authorization: 'Bearer ' + cfg.token, 'Content-Type': 'application/json' },
      body: JSON.stringify(command)
    });
    if (!res.ok) throw new Error('KV HTTP ' + res.status);
    return (await res.json()).result;
  } finally { clearTimeout(timer); }
}

function validRecord(r) {
  return !!r && r.version === VERSION && typeof r.updatedAt === 'string' && !isNaN(Date.parse(r.updatedAt)) && isStructurallyValid(r.data);
}

async function get(key) {
  const cfg = kvConfig();
  if (cfg) {
    try {
      const raw = await kvCommand(cfg, ['GET', PREFIX + key]);
      if (raw) { const rec = JSON.parse(raw); if (validRecord(rec)) return rec; }
    } catch (e) { /* fall through to memory */ }
  }
  const mem = memory.get(key);
  return validRecord(mem) ? mem : null;
}

async function put(key, record) {
  const serialised = JSON.stringify(record);
  if (Buffer.byteLength(serialised) > MAX_BYTES) return false;
  memory.set(key, record);
  const cfg = kvConfig();
  if (cfg) {
    try { await kvCommand(cfg, ['SET', PREFIX + key, serialised, 'EX', String(TTL_SECONDS)]); } catch (e) { return false; }
  }
  return true;
}

/**
 * Store a verified live response. Refuses junk, never lets an older request overwrite newer data, and only accepts
 * "empty" results after several consecutive confirmations.
 * @returns {Promise<{stored:boolean, record:object|null}>}
 */
async function saveLive(key, json, startedAtMs) {
  if (!isStructurallyValid(json)) return { stored: false, record: null };
  const existing = await get(key);
  if (!hasContent(json.data)) {
    if (!existing) return { stored: false, record: null };
    const streak = (existing.emptyStreak || 0) + 1;
    const since = existing.emptySince || Date.now();
    if (streak >= EMPTY_ACCEPT_AFTER && Date.now() - since >= emptyConfirmMs()) {
      const rec = { version: VERSION, updatedAt: new Date().toISOString(), fetchedAt: startedAtMs, hash: hashOf(json), emptyStreak: 0, data: json };
      await put(key, rec); lastWritten.delete(key);
      return { stored: true, record: rec };
    }
    await put(key, Object.assign({}, existing, { emptyStreak: streak, emptySince: since }));
    return { stored: false, record: existing };
  }
  if (existing && existing.fetchedAt > startedAtMs) return { stored: false, record: existing };          // a newer response is already stored
  const hash = hashOf(json);
  const memo = lastWritten.get(key);
  if (memo && memo.hash === hash && Date.now() - memo.at < 5 * 60 * 1000 && existing) return { stored: false, record: existing };
  const rec = { version: VERSION, updatedAt: new Date().toISOString(), fetchedAt: startedAtMs, hash, emptyStreak: 0, data: json };
  const ok = await put(key, rec);
  if (ok) lastWritten.set(key, { hash, at: Date.now() });
  return { stored: ok, record: rec };
}

/** Build-time seed (public/cache/*.json) served by the CDN. */
async function loadSeed(key, origin) {
  const ctrl = new AbortController();
  const timer = setTimeout(() => ctrl.abort(), 2000);
  try {
    const res = await fetch(origin + '/cache/' + seedName(key), { signal: ctrl.signal });
    if (!res.ok) return null;
    const rec = await res.json();
    return rec && rec.version === VERSION && typeof rec.updatedAt === 'string' && isUsable(rec.data) ? rec : null;
  } catch (e) { return null; } finally { clearTimeout(timer); }
}

/** Test helper: forget the per-instance memory copy. */
function clearMemory() { memory.clear(); lastWritten.clear(); }

module.exports = { clearMemory, cacheKeyFor, isUsable, isStructurallyValid, hasContent, hashOf, seedName, get, put, saveLive, loadSeed, kvConfig, VERSION };
