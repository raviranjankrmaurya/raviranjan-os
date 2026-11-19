const https = require("https");
const http = require("http");
const url = require("url");
const shared = require("./_lib/shared-cache");

const MAX_BODY = 2 * 1024 * 1024;

function setCors(res) {
  res.setHeader("Access-Control-Allow-Origin", "*");
  res.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, PATCH, OPTIONS");
  res.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Admin-Token, X-Requested-With, X-Visitor-Public-Ip, X-Visitor-City, X-Visitor-Region, X-Visitor-Country, X-Browser-Hash");
  res.setHeader("Access-Control-Expose-Headers", "X-Data-Source, X-Data-Updated-At");
}

function send(res, status, body, headers) {
  res.statusCode = status;
  Object.keys(headers || {}).forEach((h) => res.setHeader(h, headers[h]));
  res.end(body);
}

/** One buffered request to the backend with a hard time limit (a sleeping Render service must not block the page). */
function fetchBackend(targetUrl, headers, timeoutMs) {
  return new Promise((resolve) => {
    const t = url.parse(targetUrl);
    const transport = t.protocol === "https:" ? https : http;
    const fwd = Object.assign({}, headers, { host: t.host, "accept-encoding": "identity" });
    delete fwd.cookie;
    let settled = false;
    const finish = (v) => { if (!settled) { settled = true; clearTimeout(timer); resolve(v); } };
    const reqOut = transport.request({ hostname: t.hostname, port: t.port || (t.protocol === "https:" ? 443 : 80), path: t.path, method: "GET", headers: fwd }, (r) => {
      const chunks = []; let size = 0;
      r.on("data", (c) => { size += c.length; if (size > MAX_BODY) { reqOut.destroy(); finish({ ok: false, error: "too large" }); } else chunks.push(c); });
      r.on("end", () => finish({ ok: true, status: r.statusCode, body: Buffer.concat(chunks), headers: r.headers }));
      r.on("error", (e) => finish({ ok: false, error: e.message }));
    });
    const timer = setTimeout(() => { reqOut.destroy(); finish({ ok: false, error: "timeout" }); }, timeoutMs);
    reqOut.on("error", (e) => finish({ ok: false, error: e.message }));
    reqOut.end();
  });
}

/** Cache-first handling of PUBLIC GET endpoints (profile, projects, skills ...). */
async function handleCacheable(req, res, key, backendBase) {
  const startedAt = Date.now();
  const timeoutMs = Number(process.env.BACKEND_TIMEOUT_MS) || 7000;
  const live = await fetchBackend(backendBase + req.url, { accept: "application/json" }, timeoutMs);

  let json = null;
  if (live.ok && live.status === 200) { try { json = JSON.parse(live.body.toString("utf8")); } catch (e) { json = null; } }

  // 1) healthy backend answer
  if (json && shared.isUsable(json)) {
    const saved = await shared.saveLive(key, json, startedAt);
    const updatedAt = (saved.record && saved.stored ? saved.record.updatedAt : new Date().toISOString());
    return send(res, 200, live.body, {
      "Content-Type": "application/json; charset=utf-8",
      "X-Data-Source": "live", "X-Data-Updated-At": updatedAt,
      "Cache-Control": "public, s-maxage=20, stale-while-revalidate=300"
    });
  }

  // 2) backend asleep / failing / returned junk or an unconfirmed "empty": serve the last good snapshot
  if (json && shared.isStructurallyValid(json)) await shared.saveLive(key, json, startedAt);      // counts an "empty" confirmation
  let snap = await shared.get(key);
  let source = "shared-cache";
  if (!snap || !shared.isUsable(snap.data)) {            // an empty snapshot is not worth serving during an outage
    const origin = process.env.SEED_ORIGIN || ((req.headers["x-forwarded-proto"] || "https") + "://" + req.headers.host);
    snap = await shared.loadSeed(key, origin);
    source = "seed";
  }
  if (snap) {
    return send(res, 200, JSON.stringify(snap.data), {
      "Content-Type": "application/json; charset=utf-8",
      "X-Data-Source": source, "X-Data-Updated-At": snap.updatedAt,
      "X-Backend-Status": live.ok ? "empty-or-invalid" : "waking",
      "Cache-Control": "public, s-maxage=5, stale-while-revalidate=60"
    });
  }

  // 3) nothing cached anywhere: tell the truth
  if (live.ok) return send(res, live.status, live.body, { "Content-Type": live.headers["content-type"] || "application/json", "X-Data-Source": "live" });
  return send(res, 502, JSON.stringify({ success: false, message: "Backend is starting up and no cached copy exists yet.", error: live.error }), { "Content-Type": "application/json" });
}

module.exports = async (req, res) => {
  setCors(res);
  if (req.method === "OPTIONS") { res.statusCode = 204; res.end(); return; }

  const backendBase = (process.env.BACKEND_API_URL || process.env.NEXT_PUBLIC_API_URL || "https://raviranjan-os-backend.onrender.com").replace(/\/$/, "");

  const key = shared.cacheKeyFor(req.method, req.url, req.headers);
  if (key) return handleCacheable(req, res, key, backendBase);

  // everything else (admin, auth, POSTs, AI chat, SSE) is a plain pass-through exactly as before
  const targetUrl = url.parse(backendBase + req.url);
  const isHttps = targetUrl.protocol === "https:";
  const transport = isHttps ? https : http;

  const fwdHeaders = { ...req.headers };
  if (String(fwdHeaders["accept"] || "").includes("text/event-stream")) delete fwdHeaders["accept-encoding"];
  delete fwdHeaders["host"];
  fwdHeaders["host"] = targetUrl.host;

  const clientReq = transport.request({
    hostname: targetUrl.hostname,
    port: targetUrl.port || (isHttps ? 443 : 80),
    path: targetUrl.path,
    method: req.method,
    headers: fwdHeaders
  }, (clientRes) => {
    const outHeaders = Object.assign({}, clientRes.headers);
    const isSse = String(outHeaders["content-type"] || "").toLowerCase().includes("text/event-stream");
    if (isSse) {
      // Server-Sent Events (AI streaming): never buffer or transform, flush every chunk immediately
      outHeaders["cache-control"] = "no-cache, no-transform";
      outHeaders["x-accel-buffering"] = "no";
      delete outHeaders["content-length"];
    }
    res.writeHead(clientRes.statusCode, outHeaders);
    if (isSse && typeof res.flushHeaders === "function") res.flushHeaders();
    clientRes.pipe(res);
  });

  clientReq.on("error", (err) => {
    if (!res.headersSent) {
      send(res, 502, JSON.stringify({ success: false, message: "Render backend is starting up or temporarily unreachable.", error: err.message }), { "Content-Type": "application/json" });
    }
  });

  req.pipe(clientReq);
};
