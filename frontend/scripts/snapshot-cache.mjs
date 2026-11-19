#!/usr/bin/env node
/**
 * Build-time seeds: fetches the public API once and writes public/cache/*.json so that a first-time visitor
 * gets content from the CDN immediately, even while the Render backend is still asleep.
 *
 * Never fails the build: if the backend cannot be reached, the previous seeds (if any) are kept.
 *   BACKEND_API_URL      backend base URL (default: the Render service)
 *   SNAPSHOT_TIMEOUT_MS  total time to wait for a cold backend (default 70000)
 */
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const { paths } = JSON.parse(await readFile(path.join(root, 'cache-paths.json'), 'utf8'));
const base = (process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL || 'https://raviranjan-os-backend.onrender.com').replace(/\/$/, '');
const deadline = Date.now() + (Number(process.env.SNAPSHOT_TIMEOUT_MS) || 70000);
const outDir = path.join(root, 'public', 'cache');
await mkdir(outDir, { recursive: true });

const seedName = (p) => p.replace(/^\/api\//, '').replace(/[^a-z0-9]+/gi, '_').replace(/^_+|_+$/g, '') + '.json';
const hasContent = (d) => d !== null && d !== undefined && d !== '' && (Array.isArray(d) ? d.length > 0 : typeof d === 'object' ? (Array.isArray(d.content) ? d.content.length > 0 : Object.keys(d).length > 0) : true);

let written = 0, kept = 0;
for (const p of paths) {
  let json = null;
  while (!json && Date.now() < deadline) {
    try {
      const ctrl = new AbortController();
      const timer = setTimeout(() => ctrl.abort(), Math.min(25000, Math.max(3000, deadline - Date.now())));
      const res = await fetch(base + p, { signal: ctrl.signal, headers: { accept: 'application/json' } });
      clearTimeout(timer);
      if (res.ok) { const j = await res.json(); if (j && j.success !== false && hasContent(j.data)) json = j; else break; }
    } catch { /* backend still waking up */ }
    if (!json) await new Promise((r) => setTimeout(r, 4000));
  }
  if (json) {
    await writeFile(path.join(outDir, seedName(p)), JSON.stringify({ version: 1, updatedAt: new Date().toISOString(), data: json }));
    written++;
  } else kept++;
}
console.log(`[snapshot-cache] wrote ${written} seed file(s), kept ${kept} previous/missing`);
