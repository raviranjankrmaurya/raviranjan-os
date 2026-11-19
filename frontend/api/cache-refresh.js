'use strict';
/**
 * Secure server-side refresh of the shared cache (and a way to wake the Render backend before visitors arrive).
 *
 *   curl -X POST https://<site>/api/cache-refresh -H "Authorization: Bearer $CACHE_REFRESH_SECRET"
 *
 * Disabled (404) unless CACHE_REFRESH_SECRET is configured. Visitors can never call this successfully.
 */
const crypto = require('crypto');
const https = require('https');
const http = require('http');
const urlLib = require('url');
const shared = require('./_lib/shared-cache');
const { paths } = require('../cache-paths.json');

function safeEqual(a, b) {
  const x = Buffer.from(String(a)), y = Buffer.from(String(b));
  return x.length === y.length && crypto.timingSafeEqual(x, y);
}

function getJson(target, timeoutMs) {
  return new Promise((resolve) => {
    const t = urlLib.parse(target);
    const transport = t.protocol === 'https:' ? https : http;
    let done = false;
    const finish = (v) => { if (!done) { done = true; clearTimeout(timer); resolve(v); } };
    const r = transport.request({ hostname: t.hostname, port: t.port || (t.protocol === 'https:' ? 443 : 80), path: t.path, method: 'GET', headers: { accept: 'application/json', 'accept-encoding': 'identity' } }, (res) => {
      const chunks = [];
      res.on('data', (c) => chunks.push(c));
      res.on('end', () => { try { finish({ ok: res.statusCode === 200, json: JSON.parse(Buffer.concat(chunks).toString('utf8')) }); } catch (e) { finish({ ok: false }); } });
    });
    const timer = setTimeout(() => { r.destroy(); finish({ ok: false }); }, timeoutMs);
    r.on('error', () => finish({ ok: false }));
    r.end();
  });
}

module.exports = async (req, res) => {
  const secret = process.env.CACHE_REFRESH_SECRET;
  res.setHeader('Content-Type', 'application/json');
  if (!secret) { res.statusCode = 404; return res.end('{"success":false}'); }
  const given = String(req.headers.authorization || '').replace(/^Bearer\s+/i, '');
  if (!safeEqual(given, secret)) { res.statusCode = 401; return res.end('{"success":false}'); }

  const base = (process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL || 'https://raviranjan-os-backend.onrender.com').replace(/\/$/, '');
  const budgetMs = Number(process.env.CACHE_REFRESH_BUDGET_MS) || 50000;
  const deadline = Date.now() + budgetMs;
  const refreshed = [], failed = [];

  // The first call wakes a sleeping backend; the rest are fast.
  for (const p of paths) {
    const key = shared.cacheKeyFor('GET', p, {});
    if (!key) { failed.push(p); continue; }
    let ok = false;
    while (!ok && Date.now() < deadline) {
      const startedAt = Date.now();
      const r = await getJson(base + p, Math.min(20000, Math.max(2000, deadline - Date.now())));
      if (r.ok && shared.isUsable(r.json)) { await shared.saveLive(key, r.json, startedAt); ok = true; }
      else if (r.ok) { break; }                          // reachable but empty: don't hammer it
      else await new Promise((s) => setTimeout(s, 3000));
    }
    (ok ? refreshed : failed).push(p);
  }
  res.statusCode = 200;
  res.end(JSON.stringify({ success: true, refreshed, failed, kv: !!shared.kvConfig() }));
};
