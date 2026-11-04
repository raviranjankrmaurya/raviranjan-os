const http = require('http');
const fs = require('fs');
const path = require('path');
const url = require('url');
const dns = require('dns');

// Enforce IPv4-first resolution to prevent Node 18+ IPv6 (AAAA) issues in Alpine/Docker networks
try {
  dns.setDefaultResultOrder('ipv4first');
} catch (_) {}

const PORT = process.env.PORT || 3000;
const PUBLIC_DIR = path.join(__dirname, 'public');

const MIME_TYPES = {
  '.html': 'text/html; charset=UTF-8',
  '.css': 'text/css; charset=UTF-8',
  '.js': 'application/javascript; charset=UTF-8',
  '.json': 'application/json; charset=UTF-8',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon'
};

// Candidate backend targets in order of priority (Docker service, container name, localhost, host bridge)
const CANDIDATE_BACKEND_HOSTS = [
  process.env.BACKEND_INTERNAL_URL,
  process.env.NEXT_PUBLIC_API_URL,
  'http://backend:8080',
  'http://raviranjan-os-backend:8080',
  'http://127.0.0.1:8080',
  'http://localhost:8080',
  'http://host.docker.internal:8080'
].filter(Boolean);

let verifiedWorkingBackendUrl = CANDIDATE_BACKEND_HOSTS[0] || 'http://backend:8080';

// HTTP Keep-Alive Agent for high-throughput socket reuse
const keepAliveAgent = new http.Agent({
  keepAlive: true,
  maxSockets: 100,
  maxFreeSockets: 20,
  timeout: 60000
});

// In-memory store for multi-chunk GET tunnel payloads
const tunnelChunks = new Map();

function decodeBase64Url(b64) {
  if (!b64) return Buffer.alloc(0);
  let norm = b64.replace(/-/g, '+').replace(/_/g, '/');
  while (norm.length % 4 !== 0) norm += '=';
  return Buffer.from(norm, 'base64');
}

function isPrivateOrLocalIp(ip) {
  if (!ip) return true;
  const clean = ip.replace(/^::ffff:/, '').trim();
  return clean === '127.0.0.1' || clean === '::1' || clean === 'localhost' ||
         clean.startsWith('10.') || clean.startsWith('192.168.') ||
         clean.startsWith('172.16.') || clean.startsWith('172.17.') ||
         clean.startsWith('172.18.') || clean.startsWith('172.19.') ||
         clean.startsWith('172.2') || clean.startsWith('172.3') ||
         clean === '172.18.0.1';
}

function forwardToBackend(method, targetPath, bodyBuffer, req, res, candidateIndex = 0, retryCount = 0) {
  const currentHost = (candidateIndex === 0 && verifiedWorkingBackendUrl)
    ? verifiedWorkingBackendUrl
    : (CANDIDATE_BACKEND_HOSTS[candidateIndex] || 'http://backend:8080');
  const targetUrl = url.parse(currentHost + targetPath);

  const visitorPublicIp = req.headers['x-visitor-public-ip'];
  const existingXff = req.headers['x-forwarded-for'];
  const socketIp = req.socket && req.socket.remoteAddress ? req.socket.remoteAddress : '';

  let realClientIp = '';
  if (existingXff) {
    const parts = existingXff.split(',');
    for (const p of parts) {
      const candidate = p.trim();
      if (!isPrivateOrLocalIp(candidate)) {
        realClientIp = candidate;
        break;
      }
    }
  }
  if (!realClientIp && req.headers['x-real-ip'] && !isPrivateOrLocalIp(req.headers['x-real-ip'])) {
    realClientIp = req.headers['x-real-ip'].trim();
  }
  if (!realClientIp && req.headers['cf-connecting-ip'] && !isPrivateOrLocalIp(req.headers['cf-connecting-ip'])) {
    realClientIp = req.headers['cf-connecting-ip'].trim();
  }
  if (!realClientIp && visitorPublicIp && !isPrivateOrLocalIp(visitorPublicIp)) {
    realClientIp = visitorPublicIp.trim();
  }
  if (!realClientIp) {
    realClientIp = existingXff ? existingXff.split(',')[0].trim() : socketIp;
  }

  const forwardedChain = realClientIp ? `${realClientIp}, ${socketIp}` : socketIp;

  const cleanHeaders = {
    'host': targetUrl.host,
    'accept': req.headers['accept'] || 'application/json, text/plain, */*',
    'user-agent': req.headers['user-agent'] || 'RaviranjanOS-Proxy/1.0',
    'x-forwarded-for': forwardedChain,
    'x-real-ip': realClientIp || socketIp
  };

  if (req.headers['x-admin-token']) cleanHeaders['x-admin-token'] = req.headers['x-admin-token'];
  if (visitorPublicIp) cleanHeaders['x-visitor-public-ip'] = visitorPublicIp;
  if (req.headers['x-visitor-city']) cleanHeaders['x-visitor-city'] = req.headers['x-visitor-city'];
  if (req.headers['x-visitor-region']) cleanHeaders['x-visitor-region'] = req.headers['x-visitor-region'];
  if (req.headers['x-visitor-country']) cleanHeaders['x-visitor-country'] = req.headers['x-visitor-country'];
  if (req.headers['x-browser-hash']) cleanHeaders['x-browser-hash'] = req.headers['x-browser-hash'];
  if (req.headers['x-browser-signal']) cleanHeaders['x-browser-signal'] = req.headers['x-browser-signal'];
  if (req.headers['origin']) cleanHeaders['origin'] = req.headers['origin'];
  if (req.headers['referer']) cleanHeaders['referer'] = req.headers['referer'];
  if (req.headers['authorization']) cleanHeaders['authorization'] = req.headers['authorization'];

  if (bodyBuffer && bodyBuffer.length > 0) {
    cleanHeaders['content-type'] = req.headers['content-type'] || 'application/json; charset=UTF-8';
    cleanHeaders['content-length'] = Buffer.byteLength(bodyBuffer);
  } else if (method === 'POST' || method === 'PUT' || method === 'PATCH') {
    cleanHeaders['content-type'] = req.headers['content-type'] || 'application/json; charset=UTF-8';
    cleanHeaders['content-length'] = 0;
  }

  const options = {
    hostname: targetUrl.hostname,
    port: targetUrl.port || 8080,
    path: targetUrl.path,
    method: method,
    headers: cleanHeaders,
    agent: keepAliveAgent
  };

  const clientReq = http.request(options, (clientRes) => {
    // Successfully connected to backend host; cache as verified working
    verifiedWorkingBackendUrl = currentHost;

    const respHeaders = { ...clientRes.headers };
    delete respHeaders['access-control-allow-origin'];
    delete respHeaders['access-control-allow-methods'];
    delete respHeaders['access-control-allow-headers'];
    delete respHeaders['access-control-allow-credentials'];

    if (targetPath.includes('/stream')) {
      res.writeHead(clientRes.statusCode, {
        ...respHeaders,
        'Content-Type': 'text/event-stream',
        'Cache-Control': 'no-cache',
        'Connection': 'keep-alive'
      });
    } else {
      res.writeHead(clientRes.statusCode, {
        ...respHeaders,
        'Cache-Control': 'no-store, no-cache, must-revalidate'
      });
    }
    clientRes.pipe(res, { end: true });
  });

  clientReq.on('error', (err) => {
    // If current candidate failed to connect, attempt next candidate
    if (candidateIndex + 1 < CANDIDATE_BACKEND_HOSTS.length) {
      console.warn(`[Proxy Warning] ${currentHost} unreachable (${err.message}). Trying fallback ${CANDIDATE_BACKEND_HOSTS[candidateIndex + 1]}...`);
      forwardToBackend(method, targetPath, bodyBuffer, req, res, candidateIndex + 1, retryCount);
      return;
    }

    // If all hosts exhausted and it's a transient connection error, retry with backoff
    if ((err.code === 'ECONNREFUSED' || err.code === 'EHOSTUNREACH' || err.code === 'ETIMEDOUT') && retryCount < 2) {
      setTimeout(() => {
        forwardToBackend(method, targetPath, bodyBuffer, req, res, 0, retryCount + 1);
      }, 1000);
      return;
    }

    console.error(`[Proxy Error] ${method} ${targetPath} -> ${currentHost}:`, err.message);
    if (!res.headersSent) {
      res.writeHead(502, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({
        success: false,
        message: 'Backend API service is starting up or unreachable.',
        error: err.message
      }));
    }
  });

  if (bodyBuffer && bodyBuffer.length > 0) {
    clientReq.write(bodyBuffer);
  }
  clientReq.end();
}

const server = http.createServer((req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, PATCH, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, X-Admin-Token, X-Requested-With, X-Visitor-Public-Ip, X-Visitor-City, X-Visitor-Region, X-Visitor-Country, X-Browser-Hash, X-Browser-Signal, Origin, Referer');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // 0. GET Tunnel Endpoint (/api/__tunnel) for Cloud Shell Web Preview & strict proxies
  //    Converts GET /api/__tunnel?m=POST&p=/api/admin/settings&b=<base64url> into a direct backend call!
  if (req.url.startsWith('/api/__tunnel')) {
    const parsed = url.parse(req.url, true);
    const q = parsed.query || {};
    const chunkId = q.id;
    const idx = parseInt(q.i || '0', 10);
    const total = parseInt(q.n || '1', 10);
    const bPart = q.b || '';

    if (chunkId && total > 1) {
      if (!tunnelChunks.has(chunkId)) {
        tunnelChunks.set(chunkId, new Array(total).fill(''));
        setTimeout(() => tunnelChunks.delete(chunkId), 60000);
      }
      const arr = tunnelChunks.get(chunkId);
      arr[idx] = bPart;
      if (!q.m || !q.p) {
        res.writeHead(200, { 'Content-Type': 'application/json', 'Cache-Control': 'no-store' });
        res.end(JSON.stringify({ ok: true, chunk: idx }));
        return;
      }
      const fullB64 = arr.join('');
      tunnelChunks.delete(chunkId);
      const bodyBuf = decodeBase64Url(fullB64);
      req.headers['content-type'] = 'application/json; charset=UTF-8';
      forwardToBackend(String(q.m).toUpperCase(), String(q.p), bodyBuf, req, res);
      return;
    }

    const targetMethod = String(q.m || 'POST').toUpperCase();
    const targetPath = String(q.p || '/api/settings');
    const bodyBuf = decodeBase64Url(bPart);
    req.headers['content-type'] = 'application/json; charset=UTF-8';
    forwardToBackend(targetMethod, targetPath, bodyBuf, req, res);
    return;
  }

  // 1. Reverse Proxy for /api, /v3, /swagger-ui, /actuator to Spring Boot Backend
  if (req.url.startsWith('/api') || req.url.startsWith('/v3') || req.url.startsWith('/swagger-ui') || req.url.startsWith('/actuator')) {
    const chunks = [];
    req.on('data', (chunk) => chunks.push(chunk));
    req.on('end', () => {
      const bodyBuffer = chunks.length > 0 ? Buffer.concat(chunks) : Buffer.alloc(0);
      forwardToBackend(req.method, req.url, bodyBuffer, req, res);
    });
    return;
  }

  // 2. Serve Static Frontend Files (Vanilla JS / HTML / CSS)
  let safePath = req.url.split('?')[0];
  const queryPart = req.url.includes('?') ? req.url.split('?')[1].toLowerCase() : '';

  if (safePath.endsWith('/admin_app.js')) {
    safePath = '/admin_app.js';
  } else if (safePath.endsWith('/admin_styles.css')) {
    safePath = '/admin_styles.css';
  } else if (safePath.endsWith('/main_app.js')) {
    safePath = '/main_app.js';
  } else if (safePath.endsWith('/styles.css')) {
    safePath = '/styles.css';
  } else if (safePath === '/admin' || safePath === '/admin/' || (safePath === '/' && queryPart.includes('/admin'))) {
    safePath = '/admin_cms.html';
  } else if (safePath === '/') {
    safePath = '/index.html';
  }

  const filePath = path.join(PUBLIC_DIR, safePath);

  if (!filePath.startsWith(PUBLIC_DIR)) {
    res.writeHead(403);
    res.end('Forbidden');
    return;
  }

  fs.stat(filePath, (err, stats) => {
    if (err || !stats.isFile()) {
      const indexFallback = path.join(PUBLIC_DIR, 'index.html');
      fs.readFile(indexFallback, (err2, content) => {
        if (err2) {
          res.writeHead(404);
          res.end('404 Not Found');
          return;
        }
        res.writeHead(200, {
          'Content-Type': 'text/html; charset=UTF-8',
          'Cache-Control': 'no-store, no-cache, must-revalidate'
        });
        res.end(content);
      });
      return;
    }

    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    fs.readFile(filePath, (readErr, content) => {
      if (readErr) {
        res.writeHead(500);
        res.end('Internal Server Error');
        return;
      }
      res.writeHead(200, {
        'Content-Type': contentType,
        'Cache-Control': 'no-store, no-cache, must-revalidate'
      });
      res.end(content);
    });
  });
});

server.listen(PORT, '0.0.0.0', () => {
  console.log(`====================================================`);
  console.log(`🚀 Raviranjan.OS Frontend Server running on port ${PORT}`);
  console.log(`🔗 Proxying /api requests with multi-target fallback:`);
  CANDIDATE_BACKEND_HOSTS.forEach((h, i) => console.log(`   ${i + 1}. ${h}`));
  console.log(`====================================================`);
});
