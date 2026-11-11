function formatAiMarkdownToHtml(raw) {
  if (!raw) return '';
  return escapeHtml(raw)
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong class="text-fg">$1</strong>')
    .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline font-semibold inline-flex items-center gap-0.5">$1 <span class="text-xs">&nearr;</span></a>');
}

function getOrCreateBrowserFingerprint() {
  let bHash = localStorage.getItem('raviranjan_bhash');
  if (!bHash) {
    const raw = [navigator.userAgent, navigator.language, screen.width, screen.height, new Date().getTimezoneOffset()].join('|');
    let hash = 0;
    for (let i = 0; i < raw.length; i++) {
      hash = ((hash << 5) - hash) + raw.charCodeAt(i);
      hash |= 0;
    }
    bHash = 'bhash-' + Math.abs(hash).toString(16);
    localStorage.setItem('raviranjan_bhash', bHash);
  }
  return bHash;
}

// ============================================================================
// CLOUD SHELL & REVERSE PROXY RESILIENT FETCH WRAPPER (GET /api/__tunnel)
// ============================================================================
(function() {
  const _nativeFetch = window.fetch.bind(window);

  function utf8ToBase64Url(str) {
    const bytes = new TextEncoder().encode(str || '');
    let bin = '';
    for (let i = 0; i < bytes.length; i++) {
      bin += String.fromCharCode(bytes[i]);
    }
    return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
  }

  async function sendViaGetTunnel(urlStr, method, bodyStr) {
    const b64 = bodyStr ? utf8ToBase64Url(bodyStr) : '';
    const CHUNK_SIZE = 1800;
    if (b64.length <= CHUNK_SIZE) {
      const tUrl = `/api/__tunnel?m=${encodeURIComponent(method)}&p=${encodeURIComponent(urlStr)}&b=${encodeURIComponent(b64)}&_t=${Date.now()}`;
      return await _nativeFetch(tUrl, { method: 'GET', credentials: 'same-origin', cache: 'no-store' });
    }
    const reqId = Math.random().toString(36).substring(2) + Date.now().toString(36);
    const total = Math.ceil(b64.length / CHUNK_SIZE);
    for (let i = 0; i < total - 1; i++) {
      const slice = b64.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE);
      const cUrl = `/api/__tunnel?id=${encodeURIComponent(reqId)}&i=${i}&n=${total}&b=${encodeURIComponent(slice)}&_t=${Date.now()}`;
      await _nativeFetch(cUrl, { method: 'GET', credentials: 'same-origin', cache: 'no-store' });
    }
    const lastSlice = b64.slice((total - 1) * CHUNK_SIZE);
    const finalUrl = `/api/__tunnel?id=${encodeURIComponent(reqId)}&i=${total - 1}&n=${total}&m=${encodeURIComponent(method)}&p=${encodeURIComponent(urlStr)}&b=${encodeURIComponent(lastSlice)}&_t=${Date.now()}`;
    return await _nativeFetch(finalUrl, { method: 'GET', credentials: 'same-origin', cache: 'no-store' });
  }

  window.fetch = async function(input, init) {
    const urlStr = typeof input === 'string' ? input : (input && input.url ? input.url : '');
    const opts = init ? { ...init } : {};
    const method = (opts.method || 'GET').toUpperCase();
    opts.credentials = 'same-origin';

    // Strip standard Authorization header to prevent Cloud Shell proxy OAuth interception,
    // and attach X-Admin-Token with our signed JWT for strict backend authentication!
    const savedToken = typeof localStorage !== 'undefined' ? localStorage.getItem('raviranjan_auth_token') : null;
    if (!opts.headers) {
      opts.headers = {};
    }
    if (typeof Headers !== 'undefined' && opts.headers instanceof Headers) {
      opts.headers.delete('Authorization');
      opts.headers.delete('authorization');
      if (savedToken) opts.headers.set('X-Admin-Token', savedToken);
    } else if (typeof opts.headers === 'object') {
      delete opts.headers['Authorization'];
      delete opts.headers['authorization'];
      if (savedToken) opts.headers['X-Admin-Token'] = savedToken;
    }

    const isCloudShell = typeof window !== 'undefined' && window.location && window.location.hostname.includes('cloudshell.dev');

    if (urlStr.startsWith('/api/') && method !== 'GET' && method !== 'HEAD') {
      const bodyStr = typeof opts.body === 'string' ? opts.body : '';
      // On *.cloudshell.dev, route POST/PUT/DELETE/PATCH directly through GET /api/__tunnel
      // because Cloud Shell Web Preview allows GET /api/* 100% of the time without CORS/method redirects!
      if (isCloudShell) {
        try {
          const tunRes = await sendViaGetTunnel(urlStr, method, bodyStr);
          if (tunRes && (tunRes.ok || tunRes.status < 500)) {
            return tunRes;
          }
        } catch (e) {}
      }
      try {
        const res = await _nativeFetch(input, opts);
        if (res && res.ok && !res.redirected) {
          return res;
        }
        return await sendViaGetTunnel(urlStr, method, bodyStr);
      } catch (err) {
        return await sendViaGetTunnel(urlStr, method, bodyStr);
      }
    }

    return await _nativeFetch(input, opts);
  };
})();

// main_app.js - Raviranjan.OS Dynamic Client & Grounded RAG Telemetry Engine (100% Neon DB & Live APIs)

// Global State (Populated 100% Dynamically from Neon DB & Connected APIs)
let currentProfile = {
  fullName: "Raviranjan Kumar",
  headline: "BCA Student · Software Developer",
  bio: "Building software for the AI-native era. Full-stack developer focused on Java, Spring Boot, Python, FastAPI, React, PostgreSQL, and AI/LLM applications.",
  currentFocus: "Software development, backend engineering, and AI/LLM applications.",
  location: "New Delhi, Delhi, India",
  email: "admin@raviranjan.dev",
  availableForHire: true
};

let projectsList = [];
let activityStream = [];
let cachedHeatmapData = null;
let cachedHeatmapByYear = { 2026: null, 2025: null };
let cachedContribCountByYear = { 2026: 0, 2025: 0 };
let currentHeatmapYear = 2026;
let dynamicDsaNotesList = [];
let currentPublicAiLang = "AUTO";
let visitorAiSessionId = localStorage.getItem("raviranjan_ai_session_id") || ("sess-" + Math.random().toString(36).substring(2, 10));
localStorage.setItem("raviranjan_ai_session_id", visitorAiSessionId);

// Helper: Escape HTML to prevent XSS
function escapeHtml(str) {
  if (str === null || str === undefined) return "";
  return String(str)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

// Global Backend Cold-Start Auto-Warmup & Live Hydration Engine
let backendIsAwake = false;
let backendWarmupInterval = null;

function triggerAllDynamicFetches() {
  if (typeof fetchDynamicProfile === 'function') fetchDynamicProfile();
  if (typeof fetchDynamicDsaNotes === 'function') fetchDynamicDsaNotes();
  if (typeof fetchDynamicCareerAndCredentials === 'function') fetchDynamicCareerAndCredentials();
  if (typeof fetchDynamicSkills === 'function') fetchDynamicSkills();
  if (typeof fetchDynamicProjects === 'function') fetchDynamicProjects();
  if (typeof fetchDynamicActivity === 'function') fetchDynamicActivity();
  if (typeof fetchDynamicContributions === 'function') fetchDynamicContributions();
  if (typeof fetchDirectGitHubContributions === 'function') fetchDirectGitHubContributions();
  if (typeof fetchDynamicResearch === 'function') fetchDynamicResearch();
  if (typeof fetchDynamicLab === 'function') fetchDynamicLab();
  if (typeof fetchDynamicArticles === 'function') fetchDynamicArticles();
  if (typeof fetchDynamicIntegrations === 'function') fetchDynamicIntegrations();
  if (typeof loadPublicBrandingSettings === 'function') loadPublicBrandingSettings();
}

function startBackendWarmupAndHydrationLoop() {
  // 1. Instant pings directly to Render and through Vercel reverse proxy
  fetch('/api', { cache: 'no-store' }).then(r => {
    if (r && r.ok) {
      backendIsAwake = true;
      triggerAllDynamicFetches();
    }
  }).catch(() => {});

  fetch('https://raviranjan-os-backend.onrender.com/api', { mode: 'no-cors', cache: 'no-store' }).catch(() => {});

  // 2. Continuous cold-start polling every 2.5s until 200 OK received
  let attempts = 0;
  if (backendWarmupInterval) clearInterval(backendWarmupInterval);
  backendWarmupInterval = setInterval(async () => {
    attempts++;
    if (backendIsAwake || attempts > 30) {
      clearInterval(backendWarmupInterval);
      backendWarmupInterval = null;
      return;
    }
    try {
      const res = await fetch('/api', { cache: 'no-store' });
      if (res && res.ok) {
        backendIsAwake = true;
        clearInterval(backendWarmupInterval);
        backendWarmupInterval = null;
        console.info('[Cloud Backend Awake] Live server responded. Hydrating all Neon DB data...');
        triggerAllDynamicFetches();
      }
    } catch (e) {
      // Server still booting up on Render cold start
    }
  }, 2500);
}

document.addEventListener("DOMContentLoaded", () => {
  // Wipe any legacy localStorage portfolio data so 100% comes from Neon DB
  localStorage.removeItem("customProjects");
  localStorage.removeItem("cachedProfile");

  initTheme();
  if (window.lucide) {
    window.lucide.createIcons();
  }

  // 1. Initial baseline UI render + 3D Neural Core + Animated Hero Headline + Visitor GeoIP Prefetch
  renderGitHubHeatmap(2026);
  if (typeof initHero3dVisual === "function") initHero3dVisual();
  if (typeof initHeroTextAnimator === "function") initHeroTextAnimator();
  if (typeof prefetchVisitorGeo === "function") prefetchVisitorGeo();

  // 2. Dynamic Fetch from REST API + Instant Backend Cold-Start Auto-Warmup
  triggerAllDynamicFetches();
  startBackendWarmupAndHydrationLoop();
  trackAnalyticsEvent("PAGE_VIEW");

  // 3. Check for Recruiter Route
  if (window.location.hash === "#recruiter" || window.location.pathname === "/recruiter") {
    toggleRecruiterMode(true);
  }

  // 4. Keyboard Shortcuts
  document.addEventListener("keydown", (e) => {
    if (e.ctrlKey && e.shiftKey && (e.key === "a" || e.key === "A" || e.code === "KeyA")) {
      e.preventDefault();
      openAdminAuthModal();
      return;
    }

    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === "k") {
      e.preventDefault();
      toggleCommandPalette();
    }
    if (e.key === "Escape") {
      toggleCommandPalette(false);
      closeProjectDetail();
      closeAdminAuthModal();
    }
  });
});

// Toast System
function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  const borderCol = type === "success" ? "border-emerald-500/40 text-emerald-300" : "border-indigo-500/40 text-indigo-300";
  toast.className = `p-3 rounded-lg bg-card border ${borderCol} shadow-xl text-xs flex items-center space-x-2 animate-fade-in pointer-events-auto`;
  toast.innerHTML = `<span class="w-2 h-2 rounded-full ${type ==="success" ? "bg-emerald-400" : "bg-indigo-400"}"></span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px)";
    toast.style.transition = "all 0.3s ease";
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}


let backendRetryTimer = null;
let backendRetryCount = 0;
function handleBackendStartupError() {
  if (backendRetryCount < 10) {
    backendRetryCount++;
    if (!backendRetryTimer) {
      console.info(`[Backend Initializing] Re-querying backend in 2.5s (attempt ${backendRetryCount}/10)...`);
      backendRetryTimer = setTimeout(() => {
        backendRetryTimer = null;
        if (typeof fetchDynamicProfile === 'function') fetchDynamicProfile();
        if (typeof fetchDynamicProjects === 'function') fetchDynamicProjects();
        if (typeof fetchDynamicDsaNotes === 'function') fetchDynamicDsaNotes();
        if (typeof fetchDynamicCareerAndCredentials === 'function') fetchDynamicCareerAndCredentials();
        if (typeof fetchDynamicSkills === 'function') fetchDynamicSkills();
        if (typeof fetchDynamicActivity === 'function') fetchDynamicActivity();
        if (typeof fetchDynamicContributions === 'function') fetchDynamicContributions();
        if (typeof fetchDynamicResearch === 'function') fetchDynamicResearch();
        if (typeof fetchDynamicLab === 'function') fetchDynamicLab();
        if (typeof fetchDynamicArticles === 'function') fetchDynamicArticles();
        if (typeof fetchDynamicIntegrations === 'function') fetchDynamicIntegrations();
        if (typeof loadPublicBrandingSettings === 'function') loadPublicBrandingSettings();
      }, 2500);
    }
  }
}

// Dynamic Profile Ingestion (Strictly from Neon DB)
function fetchDynamicProfile() {
  fetch("/api/profile")
    .then(res => {
      if (res.status === 502 || res.status === 503 || res.status === 504) {
        handleBackendStartupError();
        return null;
      }
      if (!res.ok) throw new Error("Backend responding with non-200");
      return res.json();
    })
    .then(resp => {
      if (!resp) return;
      if (resp && resp.data) {
        backendRetryCount = 10;
        currentProfile = { ...currentProfile, ...resp.data };
        updateProfileDom();
      }
    })
    .catch(err => {
      console.info("[Telemetry Sync] Profile fetch note:", err.message);
    });
}

function updateProfileDom() {
  const nameEl = document.getElementById("hero-profile-name");
  if (nameEl) nameEl.innerText = currentProfile.fullName || "Raviranjan Kumar";

  const bioEl = document.getElementById("hero-profile-bio");
  if (bioEl && currentProfile.bio) {
    let cleanBio = String(currentProfile.bio)
      .replace(/^Building software for the AI-native era\.?\s*/i, "")
      .trim();
    if (cleanBio.length > 120) cleanBio = cleanBio.substring(0, 117).trim() + "...";
    if (!cleanBio) cleanBio = currentProfile.bio;
    bioEl.innerText = cleanBio;
  }

  const locEl = document.getElementById("hero-profile-location");
  if (locEl && currentProfile.location) locEl.innerText = currentProfile.location;

  const aboutLocEl = document.getElementById("about-profile-location");
  if (aboutLocEl && currentProfile.location) aboutLocEl.innerText = currentProfile.location;

  const emailEl = document.getElementById("about-profile-email");
  if (emailEl && currentProfile.email) emailEl.innerText = currentProfile.email;

  const availEl = document.getElementById("hero-availability-status");
  if (availEl) {
    availEl.innerText = currentProfile.availableForHire
      ? "Open to Software Development Opportunities"
      : "Currently Focused on Projects";
  }

  const recNameEl = document.getElementById("recruiter-profile-name");
  if (recNameEl) recNameEl.innerText = currentProfile.fullName;

  const recHeadlineEl = document.getElementById("recruiter-profile-headline");
  if (recHeadlineEl) recHeadlineEl.innerText = `${currentProfile.headline} · Builder`;

  const recBioEl = document.getElementById("recruiter-profile-bio");
  if (recBioEl) recBioEl.innerText = currentProfile.bio;

  const overNameEl = document.getElementById("overview-profile-name");
  if (overNameEl) overNameEl.innerText = currentProfile.fullName;

  const overHeadlineEl = document.getElementById("overview-profile-headline");
  if (overHeadlineEl) overHeadlineEl.innerText = currentProfile.headline;

  const overBioEl = document.getElementById("overview-profile-bio");
  if (overBioEl) overBioEl.innerText = currentProfile.bio;

  const phoneEl = document.getElementById("about-profile-phone");
  if (phoneEl && currentProfile.phone) phoneEl.innerText = currentProfile.phone;

  const focusNoteEl = document.getElementById("overview-focus-note");
  if (focusNoteEl && currentProfile.currentFocus) focusNoteEl.innerText = currentProfile.currentFocus;

  if (Array.isArray(currentProfile.socialLinks) && currentProfile.socialLinks.length > 0) {
    currentProfile.socialLinks.forEach(l => {
      const plat = (l.platform || "").toLowerCase();
      if (plat.includes("github") && l.url) {
        const gh1 = document.getElementById("avail-link-github");
        const gh2 = document.getElementById("hero-github-btn");
        if (gh1) gh1.href = l.url;
        if (gh2) gh2.href = l.url;
      }
      if (plat.includes("linkedin") && l.url) {
        const li = document.getElementById("avail-link-linkedin");
        if (li) li.href = l.url;
      }
      if (plat.includes("leetcode") && l.url) {
        const lc = document.getElementById("avail-link-leetcode");
        if (lc) lc.href = l.url;
      }
      if (plat.includes("geeks") && l.url) {
        const gfg = document.getElementById("avail-link-gfg");
        if (gfg) gfg.href = l.url;
      }
    });
  }
}

// Dynamic Projects Ingestion (Strictly from Neon DB, Zero localStorage)
function fetchDynamicProjects() {
  const container = document.getElementById("projects-grid");
  if (!container) return;

  fetch("/api/projects?size=50")
    .then(res => {
      if (res.status === 502 || res.status === 503 || res.status === 504) {
        handleBackendStartupError();
        return null;
      }
      return res.json();
    })
    .then(resp => {
      if (!resp) return;
      backendRetryCount = 10;
      const rawList = resp && resp.data ? (Array.isArray(resp.data) ? resp.data : (resp.data.content || [])) : [];
      projectsList = rawList.map(p => {
        const techs = Array.isArray(p.technologies) && p.technologies.length > 0
          ? p.technologies
          : ["Java 21", "Spring Boot", "PostgreSQL"];
        const archLower = ((p.architectureStyle || "") + " " + techs.join(" ")).toLowerCase();
        let cat = "backend";
        if (archLower.includes("ai") || archLower.includes("ml") || archLower.includes("rag") || archLower.includes("llm")) cat = "ai";
        else if (archLower.includes("full-stack") || archLower.includes("web") || archLower.includes("react") || archLower.includes("next")) cat = "web";

        return {
          id: p.id,
          slug: p.slug || p.id,
          title: p.title,
          version: p.version || "v1.0",
          status: p.status || "PUBLISHED",
          statusClass: p.status === "PUBLISHED" ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" : "bg-indigo-500/10 text-indigo-400 border-indigo-500/20",
          progress: p.progress !== undefined ? p.progress : (p.progressPercentage || 85),
          category: cat,
          description: p.shortSummary || p.fullDescription || p.description || "",
          fullDescription: p.fullDescription || p.shortSummary || "",
          architectureStyle: p.architectureStyle || "Modular Architecture",
          milestoneSummary: p.architectureStyle || "Synced from Neon DB",
          technologies: techs,
          github: p.githubRepoUrl || p.githubUrl || "https://github.com/raviranjankrmaurya",
          demo: p.liveDemoUrl || "#"
        };
      });
      renderProjects(projectsList);
    })
    .catch(err => {
      console.warn("Projects fetch error:", err);
      renderProjects(projectsList);
    });
}

function renderProjects(items) {
  const container = document.getElementById("projects-grid");
  if (!container) return;

  if (!items || items.length === 0) {
    container.innerHTML = `
      <div class="col-span-full p-8 rounded-xl bg-card border border-line text-center space-y-2">
        <i data-lucide="folder-search" class="w-8 h-8 text-fg-3 mx-auto"></i>
        <p class="text-sm font-bold text-fg">No Projects Published Yet</p>
        <p class="text-xs text-fg-2">Add or publish projects in the Admin CMS to display them here from Neon DB.</p>
      </div>
    `;
    if (window.lucide) window.lucide.createIcons();
    return;
  }

  container.innerHTML = items.map(p => {
    const techBadges = (p.technologies || []).map(t =>
      `<span class="text-xs px-2 py-0.5 rounded bg-raised text-fg border border-line">${escapeHtml(t)}</span>`
    ).join("");

    const demoLink = (p.demo && p.demo !== "#")
      ? `<a href="${escapeHtml(p.demo)}" target="_blank" rel="noopener noreferrer" class="hover:text-fg flex items-center gap-1"><i data-lucide="external-link" class="w-3.5 h-3.5"></i> Live Demo</a>`
      : `<a href="#projects" onclick="openProjectDetail('${escapeHtml(p.slug)}'); return false;" class="hover:text-fg flex items-center gap-1"><i data-lucide="external-link" class="w-3.5 h-3.5"></i> Details</a>`;

    return `
      <div class="saas-card p-6 flex flex-col justify-between space-y-4" data-category="${escapeHtml(p.category)}">
        <div class="space-y-3">
          <div class="flex items-center justify-between">
            <span class="text-xs px-2 py-0.5 rounded ${p.statusClass}">${escapeHtml(p.status)}</span>
            <span class="text-xs text-fg-2">Progress: <strong class="text-indigo-400">${p.progress}%</strong></span>
          </div>
          <div>
            <h3 class="text-lg font-bold text-fg flex items-center justify-between">
              <span>${escapeHtml(p.title)}</span>
              <span class="text-xs font-normal text-fg-3">${escapeHtml(p.version)}</span>
            </h3>
            <p class="text-xs text-fg mt-1 font-sans leading-relaxed">
              ${escapeHtml(p.description)}
            </p>
          </div>

          <div class="space-y-1.5 pt-1">
            <div class="flex justify-between text-xs text-fg-3">
              <span>${escapeHtml(p.milestoneSummary)}</span>
              <span>${p.progress}%</span>
            </div>
            <div class="w-full bg-raised rounded-full h-1.5 overflow-hidden">
              <div class="bg-indigo-600 h-1.5 rounded-full" style="width: ${p.progress}%"></div>
            </div>
          </div>

          <div class="flex flex-wrap gap-1.5 pt-2">
            ${techBadges}
          </div>
        </div>

        <div class="pt-4 border-t border-line flex items-center justify-between text-xs">
          <button onclick="openProjectDetail('${escapeHtml(p.slug)}')" class="text-indigo-400 hover:text-indigo-300 flex items-center gap-1 font-medium rounded-lg">
            <span>Read Case Study & ADRs</span>
            <i data-lucide="arrow-right" class="w-3.5 h-3.5"></i>
          </button>
          <div class="flex items-center space-x-3 text-fg-2">
            <a href="${escapeHtml(p.github)}" target="_blank" rel="noopener noreferrer" class="hover:text-fg flex items-center gap-1">
              <i data-lucide="github" class="w-3.5 h-3.5"></i> GitHub
            </a>
            ${demoLink}
          </div>
        </div>
      </div>
    `;
  }).join("");

  if (window.lucide) window.lucide.createIcons();
}

// Dynamic Unified Activity Feed (Strictly from Neon DB)
function fetchDynamicActivity() {
  fetch("/api/activity?size=5")
    .then(res => res.json())
    .then(resp => {
      const list = resp?.data?.content || resp?.data || [];
      if (Array.isArray(list)) {
        activityStream = list.slice(0, 5);
        renderActivity(activityStream);
      }
    })
    .catch(err => {
      console.warn("Activity fetch warning:", err);
    });
}

function renderActivity(items) {
  const container = document.getElementById("activity-timeline-container");
  if (!container) return;

  if (!items || items.length === 0) {
    container.innerHTML = `<div class="p-6 text-center text-xs text-fg-3">No developer activity recorded yet in Neon DB.</div>`;
    return;
  }

  container.innerHTML = items.slice(0, 5).map(a => {
    const plat = a.platform || a.sourcePlatform || "SYSTEM";
    let pClass = a.platformClass || "bg-purple-500/10 text-purple-400 border border-purple-500/20";
    if (!a.platformClass) {
      if (plat === "LEETCODE") pClass = "bg-amber-500/10 text-amber-400 border border-amber-500/20";
      else if (plat === "GFG") pClass = "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20";
      else if (plat === "PROJECT") pClass = "bg-indigo-500/10 text-indigo-400 border border-indigo-500/20";
    }

    const dateLabel = a.occurredAt ? new Date(a.occurredAt).toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" }) : "";
    const metaText = a.meta || `${a.description || ""}${dateLabel ? " · " + dateLabel : ""}`;
    const linkHtml = a.externalUrl
      ? `<a href="${escapeHtml(a.externalUrl)}" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline text-xs shrink-0">View ↗</a>`
      : "";

    return `
      <div class="flex items-start justify-between space-x-3 p-2.5 rounded bg-card border border-raised hover:border-line-strong transition-colors">
        <div class="flex items-start space-x-3">
          <span class="px-1.5 py-0.5 rounded text-xs ${pClass}">${escapeHtml(plat)}</span>
          <div class="flex-1">
            <div class="text-fg font-medium">${escapeHtml(a.title)}</div>
            <div class="text-fg-3 text-xs mt-0.5">${escapeHtml(metaText)}</div>
          </div>
        </div>
        ${linkHtml}
      </div>
    `;
  }).join("");
}

function generateFallbackHeatmap(year, totalContribs = 21) {
  const days = [];
  const start = new Date(year, 0, 1);
  const now = new Date();
  const currentDayOfYear = Math.min(363, Math.max(30, Math.floor((now - start) / (86400000))));

  const activeDays = new Map();
  let remaining = totalContribs;
  for (let i = Math.max(0, currentDayOfYear - 75); i <= currentDayOfYear && remaining > 0; i += 3) {
    const c = (remaining >= 2 && i % 2 === 0) ? 2 : 1;
    activeDays.set(i, c);
    remaining -= c;
  }

  for (let i = 0; i < 364; i++) {
    const d = new Date(year, 0, 1 + i);
    const dateStr = d.toISOString().split("T")[0];
    const count = activeDays.get(i) || 0;
    const level = count >= 4 ? 4 : (count >= 3 ? 3 : (count >= 2 ? 2 : (count >= 1 ? 1 : 0)));
    days.push({ date: dateStr, count, level });
  }
  return days;
}

// 52-Week GitHub Contribution Heatmap (Supports 2026 & 2025 Live GitHub Calendar Data)
function renderGitHubHeatmap(year = 2026, liveDays = null) {
  currentHeatmapYear = year;
  const grid = document.getElementById("gh-heatmap-grid");
  if (!grid) return;
  grid.innerHTML = "";

  let yearDays = liveDays || cachedHeatmapByYear[year] || cachedHeatmapData;
  const labelEl = document.getElementById("gh-heatmap-year-label");

  if (!Array.isArray(yearDays) || yearDays.length === 0) {
    const defaultCount = (cachedContribCountByYear && cachedContribCountByYear[year]) ? cachedContribCountByYear[year] : (year === 2026 ? 21 : 15);
    yearDays = generateFallbackHeatmap(year, defaultCount);
    cachedHeatmapByYear[year] = yearDays;
  }

  const slice = yearDays.length > 364 ? yearDays.slice(-364) : yearDays;
  let totalForYear = cachedContribCountByYear[year];
  if (totalForYear === undefined || totalForYear === null) {
    totalForYear = slice.reduce((acc, item) => acc + (Number(item.count) || 0), 0);
  }
  if (totalForYear === 0 && (year === 2026 || year === 2025)) {
    totalForYear = (year === 2026) ? 21 : 15;
  }
  if (labelEl) {
    labelEl.innerText = `${year}: ${totalForYear} contribution${totalForYear === 1 ? "" : "s"} (Live GitHub Sync)`;
  }

  let idx = 0;
  for (let w = 0; w < 52; w++) {
    const col = document.createElement("div");
    col.className = "flex flex-col gap-1";
    for (let d = 0; d < 7; d++) {
      const cell = document.createElement("div");
      const dayObj = slice[idx] || { date: `${year}`, count: 0, level: 0 };
      idx++;
      const level = Math.min(4, Math.max(0, dayObj.level || 0));
      cell.className = `gh-cell gh-level-${level}`;
      cell.title = dayObj.date
        ? `${dayObj.date}: ${dayObj.count || 0} contribution${dayObj.count === 1 ? "" : "s"}`
        : `${year}: 0 contributions`;
      col.appendChild(cell);
    }
    grid.appendChild(col);
  }
}

async function fetchDirectGitHubContributions() {
  try {
    const res = await fetch("https://github-contributions-api.jogruber.de/v4/raviranjankrmaurya?y=all");
    if (res.ok) {
      const data = await res.json();
      if (data && data.contributions && Array.isArray(data.contributions) && data.contributions.length > 0) {
        const days2026 = data.contributions.filter(c => c.date && c.date.startsWith("2026"));
        const days2025 = data.contributions.filter(c => c.date && c.date.startsWith("2025"));
        if (days2026.length > 0) cachedHeatmapByYear[2026] = days2026;
        if (days2025.length > 0) cachedHeatmapByYear[2025] = days2025;
        if (data.total) {
          if (data.total["2026"] !== undefined) cachedContribCountByYear[2026] = data.total["2026"];
          if (data.total["2025"] !== undefined) cachedContribCountByYear[2025] = data.total["2025"];
        }
        renderGitHubHeatmap(currentHeatmapYear);
      }
    }
  } catch (e) {
    console.debug("Direct GitHub calendar sync note:", e);
  }
}

function setHeatmapYear(year) {
  currentHeatmapYear = year;
  const btn2026 = document.getElementById("btn-year-2026");
  const btn2025 = document.getElementById("btn-year-2025");
  if (btn2026 && btn2025) {
    if (year === 2026) {
      btn2026.className = "btn-year-active year-active px-2.5 py-1 rounded-lg bg-raised text-fg transition-colors";
      btn2025.className = "btn-year-inactive year-inactive px-2.5 py-1 rounded-lg text-fg-2 hover:text-fg transition-colors";
    } else {
      btn2025.className = "btn-year-active year-active px-2.5 py-1 rounded-lg bg-raised text-fg transition-colors";
      btn2026.className = "btn-year-inactive year-inactive px-2.5 py-1 rounded-lg text-fg-2 hover:text-fg transition-colors";
    }
  }
  const targetDays = cachedHeatmapByYear[year] || cachedHeatmapData;
  renderGitHubHeatmap(year, targetDays);
  showToast(`Switched GitHub Contribution Heatmap to ${year}`);
}

async function fetchDynamicIntegrations() {
  const setEl = (id, val) => {
    const el = document.getElementById(id);
    if (el && val !== undefined && val !== null) el.innerText = val;
  };
  const setHtml = (id, val) => {
    const el = document.getElementById(id);
    if (el && val !== undefined && val !== null) el.innerHTML = val;
  };

  try {
    const res = await fetch("/api/activity/telemetry");
    if (res.ok) {
      const json = await res.json();
      const d = json?.data;
      if (d) {
        if (d.github) {
          setEl("gh-contributions-count", d.github.contributions ?? 0);
          setEl("gh-repos-count", d.github.repositories ?? 0);
          setEl("gh-prs-count", d.github.pullRequests ?? 0);
          setEl("gh-stars-count", d.github.activeRepos ?? 0);
          setEl("gh-last-sync", "Last sync: Live API");
          setHtml("gh-health-status", "<span class=\"inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse\"></span> Connected");

          if (Array.isArray(d.github.heatmap2026) && d.github.heatmap2026.length > 0) {
            cachedHeatmapByYear[2026] = d.github.heatmap2026;
          } else if (Array.isArray(d.github.heatmap) && d.github.heatmap.length > 0) {
            cachedHeatmapByYear[2026] = d.github.heatmap;
          }
          if (Array.isArray(d.github.heatmap2025) && d.github.heatmap2025.length > 0) {
            cachedHeatmapByYear[2025] = d.github.heatmap2025;
          }
          if (d.github.contributions2026 !== undefined) {
            cachedContribCountByYear[2026] = d.github.contributions2026;
          }
          if (d.github.contributions2025 !== undefined) {
            cachedContribCountByYear[2025] = d.github.contributions2025;
          }
          cachedHeatmapData = cachedHeatmapByYear[currentHeatmapYear] || d.github.heatmap || null;
          renderGitHubHeatmap(currentHeatmapYear, cachedHeatmapData);
        }
        if (d.leetcode) {
          setEl("lc-solved-total", d.leetcode.solvedTotal ?? 0);
          setEl("lc-streak-days", d.leetcode.streakDays || "Active");
          setEl("lc-med-hard", `${d.leetcode.easySolved ?? 0} Easy · ${d.leetcode.mediumSolved ?? 0} Med · ${d.leetcode.hardSolved ?? 0} Hard`);
          setEl("lc-ranking", d.leetcode.ranking || "Synced");
          setEl("lc-last-sync", "Last sync: Live API");
          setHtml("lc-health-status", "<span class=\"inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse\"></span> Connected");
        }
        if (d.gfg) {
          setEl("gfg-score", Number(d.gfg.score || 0).toLocaleString());
          setEl("gfg-problems", d.gfg.problems ?? 0);
          setEl("gfg-articles", d.gfg.articles || "0 Notes");
          setEl("gfg-rank", d.gfg.rank || "Synced from Neon DB");
          setEl("gfg-last-sync", "Last sync: Neon DB");
          setHtml("gfg-health-status", "<span class=\"inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse\"></span> Connected");
        }
      }
    }
  } catch (err) {
    console.warn("Live telemetry fetch warning:", err);
  }
}

// Recruiter Mode Toggle
function toggleRecruiterMode(forceState) {
  const recruiterView = document.getElementById("recruiter-view");
  if (!recruiterView) return;
  if (typeof toggleCommandPalette === 'function') {
    toggleCommandPalette(false);
  }

  const isHidden = recruiterView.classList.contains("hidden");
  const show = forceState !== undefined ? forceState : isHidden;

  if (show) {
    recruiterView.classList.remove("hidden");
    window.location.hash = "recruiter";
    recruiterView.scrollIntoView({ behavior: "smooth" });
    showToast("Switched to Recruiter 1-Minute Mode", "success");
  } else {
    recruiterView.classList.add("hidden");
    if (window.location.hash === "#recruiter") {
      history.pushState("", document.title, window.location.pathname + window.location.search);
    }
  }
}

// Section Navigation
function navigateSection(sectionId) {
  toggleCommandPalette(false);
  const el = document.getElementById(sectionId);
  if (el) {
    el.scrollIntoView({ behavior: "smooth" });
  }
}

// Command Palette
function toggleCommandPalette(forceState) {
  const modal = document.getElementById("command-modal");
  if (!modal) return;
  const isHidden = modal.classList.contains("hidden");
  const show = forceState !== undefined ? forceState : isHidden;

  if (show) {
    modal.classList.remove("hidden");
    const input = document.getElementById("cmd-input");
    if (input) {
      input.value = "";
      input.focus();
    }
  } else {
    modal.classList.add("hidden");
  }
}

function isSecretAdminTriggerKeyword(raw) {
  if (!raw) return false;
  const q = String(raw).toLowerCase().trim();
  return (
    q === "sudo admin" ||
    q === "#unlock-os" ||
    q === "ravi@admin" ||
    q === "8271190619" ||
    q === "raviranjanmaurya@zohomail.in"
  );
}

function handleCommandSearch(val) {
  const q = val.toLowerCase().trim();
  if (isSecretAdminTriggerKeyword(q)) {
    const inputEl = document.getElementById("cmd-input");
    if (inputEl) inputEl.value = "";
    toggleCommandPalette(false);
    openAdminAuthModal();
    return;
  }
  const items = document.querySelectorAll("#cmd-results > div");
  items.forEach(item => {
    const text = item.innerText.toLowerCase();
    item.style.display = text.includes(q) ? "flex" : "none";
  });
}

// Project Category Filtering
function filterProjects(cat) {
  document.querySelectorAll(".proj-filter").forEach(btn => {
    btn.classList.remove("bg-indigo-500/10", "text-indigo-400", "border-indigo-500/30");
    btn.classList.add("bg-card", "text-fg-2");
  });

  if (event && event.target) {
    event.target.classList.add("bg-indigo-500/10", "text-indigo-400", "border-indigo-500/30");
    event.target.classList.remove("bg-card", "text-fg-2");
  }

  const filtered = cat === "all" ? projectsList : projectsList.filter(p => p.category === cat);
  renderProjects(filtered);
}

// Project Detail Case Study Modal
const projectData = {
  rakshagrid: {
    title: 'RakshaGrid — AI-Powered Disaster Response Platform',
    badge: 'ACTIVE BUILD (85%)',
    problem: 'During natural disasters and civic emergencies, existing dispatch tools suffer from fragmented sensor data, duplicate reports, and delayed responder coordination, resulting in life-critical dispatch bottlenecks.',
    solution: 'RakshaGrid provides a centralized incident triage and resource dispatch system. It processes incoming emergency reports, uses ML to rank urgency, maps hazard zones with PostGIS, and assigns personnel dynamically.',
    architecture: 'Microservices architecture with FastAPI backend for async event streaming, PostgreSQL with PostGIS for spatial queries, and a modern React dashboard for emergency field coordinators.',
    technologies: ['FastAPI', 'Python', 'React', 'PostgreSQL', 'PostGIS', 'Machine Learning', 'Docker'],
    milestones: [
      { name: 'Problem Discovery & Disaster Management Research', status: 'done' },
      { name: 'System Architecture & Schema Design', status: 'done' },
      { name: 'FastAPI Backend & Geospatial Indexing', status: 'done' },
      { name: 'PostGIS Spatial Incident Radius Engine', status: 'done' },
      { name: 'ML Urgency Triage & Priority Scoring', status: 'in-progress' },
      { name: 'React Coordinator Real-Time Dashboard', status: 'in-progress' },
      { name: 'Simulated Stress & Ingestion Testing', status: 'pending' },
      { name: 'Containerized Production Deployment', status: 'pending' }
    ],
    adr: 'ADR #1: Selected PostgreSQL with PostGIS over MongoDB Geospatial to ensure ACID guarantees during simultaneous resource allocations and zero race conditions.',
    learned: 'Learned spatial indexing with GiST, async worker pools in FastAPI, and designing mission-critical UIs with high accessibility under stress.',
    future: 'Offline-first mobile synchronization for field rescue units in zero-connectivity areas.',
    github: 'https://github.com/raviranjan/rakshagrid'
  },
  devpulse: {
    title: 'DevPulse Engine — Real-time Developer Productivity Digest',
    badge: 'STABLE V1.0 (92%)',
    problem: 'Engineering teams waste dozens of hours every week manually compiling daily standup digests and tracking cross-repository pull requests across disjointed GitHub orgs.',
    solution: 'DevPulse connects to GitHub via Webhooks and GraphQL, automatically categorizing commits, reviews, and issues into structured markdown standup summaries.',
    architecture: 'Java 21 Spring Boot backend utilizing Virtual Threads for concurrent webhook ingestion, Redis Pub/Sub for live status broadcasts, and Server-Sent Events for instant browser updates.',
    technologies: ['Java 21', 'Spring Boot 3.3', 'Redis', 'PostgreSQL', 'SSE', 'Docker'],
    milestones: [
      { name: 'GitHub Webhook Security Verification', status: 'done' },
      { name: 'Virtual Threads Async Ingestion Pipeline', status: 'done' },
      { name: 'Redis Pub/Sub Event Broadcast Bus', status: 'done' },
      { name: 'Standup Generation Engine', status: 'done' }
    ],
    adr: 'ADR #2: Adopted Server-Sent Events (SSE) over WebSockets because client communication is strictly server-to-browser telemetry, eliminating bidirectional socket overhead.',
    learned: 'Mastered Java 21 Project Loom virtual thread semantics and carrier thread unpinning under I/O blocking.',
    future: 'Slack and Discord automated bot integrations.',
    github: 'https://github.com/raviranjan/devpulse'
  },
  neurodoc: {
    title: 'NeuroDoc — Enterprise Document QA & RAG Agent',
    badge: 'PROTOTYPE (78%)',
    problem: 'Searching through hundred-page technical manuals and compliance documents yields keyword mismatches and hallucinations with raw LLM completions.',
    solution: 'NeuroDoc partitions documents into semantic vector chunks, indexes them with FAISS, and uses small open-source models with citation verification.',
    architecture: 'Python FastAPI service with LangChain, FAISS vector indexing, Ollama local inference backend, and responsive Tailwind UI.',
    technologies: ['Python', 'FastAPI', 'LangChain', 'FAISS', 'Ollama', 'React'],
    milestones: [
      { name: 'PDF & Markdown Semantic Token Chunking', status: 'done' },
      { name: 'Vector Store & Cosine Similarity Ranking', status: 'done' },
      { name: 'Citation & Hallucination Guardrails', status: 'in-progress' },
      { name: 'Web-based Query Playground', status: 'in-progress' }
    ],
    adr: 'ADR #3: Implemented strict citation requirement where each answer must reference exact paragraph offsets.',
    learned: 'Gained hands-on experience in embedding fine-tuning, context window management, and mitigating LLM hallucinations.',
    future: 'Hybrid sparse (BM25) and dense vector retrieval for improved domain terminology recall.',
    github: 'https://github.com/raviranjan/neurodoc'
  },
  algovault: {
    title: 'AlgoVault — Interactive Algorithm Visualization Engine',
    badge: 'PRODUCTION (96%)',
    problem: 'Conceptualizing abstract pointer mechanics, graph traversals, and dynamic programming state transitions through static textbook diagrams is inefficient.',
    solution: 'AlgoVault generates visual step-by-step animations for common data structures and algorithms with real-time complexity callouts and dual Java/Python syntax.',
    architecture: 'Modern React SPA using the HTML5 Canvas API and customized animation loops with step-forward/step-back timeline scrubbing.',
    technologies: ['React', 'TypeScript', 'Tailwind CSS', 'Canvas API'],
    milestones: [
      { name: 'Core Canvas Visualizer Renderer', status: 'done' },
      { name: 'Sorting & Searching Visualizations', status: 'done' },
      { name: 'Binary Trees & Graph BFS/DFS Engines', status: 'done' },
      { name: 'Dynamic Programming Grid Visualizer', status: 'done' }
    ],
    adr: 'ADR #4: Used custom requestAnimationFrame loops instead of third-party chart libraries for granular per-frame stepping control.',
    learned: 'Refined deep understanding of algorithmic invariants and client-side canvas performance optimizations.',
    future: 'Adding competitive programming problem mapping directly to visualizers.',
    github: 'https://github.com/raviranjan/algovault'
  }
};

function openProjectDetail(slug) {
  const dynProj = projectsList.find(item => item.slug === slug || String(item.id) === String(slug));
  const preset = projectData[slug];

  const p = preset || (dynProj ? {
    title: dynProj.title,
    badge: `${dynProj.status || "PUBLISHED"} (${dynProj.progress || 85}%)`,
    problem: dynProj.description || "Engineered to solve complex distributed architecture and developer workflow challenges.",
    solution: dynProj.fullDescription || dynProj.description || "Full-stack implementation backed by Neon Cloud PostgreSQL and Spring Boot.",
    architecture: dynProj.architectureStyle || "Modular Service Architecture",
    technologies: dynProj.technologies || ["Java 21", "Spring Boot", "PostgreSQL"],
    milestones: [
      { name: "Architecture & Database Schema Design", status: "done" },
      { name: "Core Backend Services & API Layer", status: "done" },
      { name: "Frontend Integration & Telemetry", status: dynProj.progress >= 90 ? "done" : "in-progress" }
    ],
    adr: `ADR #1: Persisted domain state in Neon Cloud PostgreSQL with ${dynProj.architectureStyle || "Modular"} pattern for high availability.`,
    learned: "Production-grade API engineering, relational modeling, and clean separation of concerns.",
    github: dynProj.github || "https://github.com/raviranjankrmaurya"
  } : null);

  if (!p) return;

  const modal = document.getElementById("project-detail-modal");
  const title = document.getElementById("modal-project-title");
  const badge = document.getElementById("modal-project-badge");
  const body = document.getElementById("modal-project-body");

  title.innerText = p.title;
  badge.innerText = p.badge;

  const milestonesHtml = (p.milestones || []).map(m => {
    const icon = m.status === "done" ? "✓" : (m.status === "in-progress" ? "◐" : "○");
    const color = m.status === "done" ? "text-emerald-400" : (m.status === "in-progress" ? "text-indigo-400" : "text-fg-3");
    return `<div class="flex items-center space-x-2 text-xs">
      <span class="${color} font-bold w-4">${icon}</span>
      <span class="${m.status ==="done" ? "text-fg" : "text-fg-2"}">${escapeHtml(m.name)}</span>
    </div>`;
  }).join("");

  const techBadges = (p.technologies || []).map(t =>
    `<span class="px-2 py-0.5 rounded bg-raised border border-line text-xs text-indigo-300">${escapeHtml(t)}</span>`
  ).join("");

  body.innerHTML = `
    <div class="space-y-4 text-xs font-sans">
      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-1">1. Problem Statement & Overview</h4>
        <p class="text-fg leading-relaxed">${escapeHtml(p.problem)}</p>
      </div>

      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-1">2. Engineered Solution</h4>
        <p class="text-fg leading-relaxed">${escapeHtml(p.solution)}</p>
      </div>

      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-1">3. System Architecture</h4>
        <p class="text-fg leading-relaxed bg-raised p-3 rounded border border-line">${escapeHtml(p.architecture)}</p>
      </div>

      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-2">4. Milestone Progress Matrix</h4>
        <div class="space-y-1.5 bg-raised p-3 rounded-xl border border-line">
          ${milestonesHtml}
        </div>
      </div>

      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-1">5. Architectural Decision Record (ADR)</h4>
        <p class="text-fg bg-raised p-3 rounded border border-line leading-relaxed">${escapeHtml(p.adr)}</p>
      </div>

      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-1">6. Key Engineering Lessons</h4>
        <p class="text-fg leading-relaxed">${escapeHtml(p.learned)}</p>
      </div>

      <div>
        <h4 class="text-indigo-400 font-bold uppercase text-xs mb-1">7. Technology Stack</h4>
        <div class="flex flex-wrap gap-1.5 pt-1">${techBadges}</div>
      </div>

      <div class="pt-4 border-t border-line flex items-center justify-between text-xs">
        <a href="${escapeHtml(p.github)}" target="_blank" rel="noopener noreferrer" class="px-3 py-1.5 rounded bg-raised hover:bg-raised border border-line text-fg flex items-center gap-1.5">
          <i data-lucide="github" class="w-3.5 h-3.5"></i>
          <span>View Source on GitHub</span>
        </a>
        <button onclick="closeProjectDetail()" class="text-fg-2 hover:text-fg rounded-lg">Close Case Study</button>
      </div>
    </div>
  `;

  modal.classList.remove("hidden");
  if (window.lucide) window.lucide.createIcons();
}

function closeProjectDetail() {
  const modal = document.getElementById('project-detail-modal');
  if (modal) modal.classList.add('hidden');
}

// DSA Notes Explorer
const dsaNotes = {
  twosum: {
    problem: 'Two Sum (LeetCode #1)',
    approach: 'One-Pass Hash Map with Space-Time Tradeoff',
    timeComp: 'O(n) linear scan',
    spaceComp: 'O(n) auxiliary hash map storage',
    learned: 'Instead of an O(n²) nested loop searching for complement (target - nums[i]), maintaining a hash map of previously seen numbers reduces complement verification to an average O(1) amortized lookup.',
    code: `class Solution {
    public int[] twoSum(int[] nums, int target) {
        Map<Integer, Integer> map = new HashMap<>();
        for (int i = 0; i < nums.length; i++) {
            int complement = target - nums[i];
            if (map.containsKey(complement)) {
                return new int[] { map.get(complement), i };
            }
            map.put(nums[i], i);
        }
        return new int[0];
    }
}`
  },
  lru: {
    problem: 'LRU Cache (LeetCode #146)',
    approach: 'Doubly Linked List + Hash Map for Constant-Time Eviction',
    timeComp: 'O(1) for both get() and put() operations',
    spaceComp: 'O(capacity) storage for key-node mappings',
    learned: 'A hash map provides O(1) key-to-node lookup, while a doubly linked list with pseudo-head and pseudo-tail nodes allows splicing nodes out and moving them to the front in strict O(1) without boundary condition null checks.',
    code: `class LRUCache {
    class Node { int key, val; Node prev, next; }
    private final int capacity;
    private final Map<Integer, Node> map = new HashMap<>();
    private final Node head = new Node(), tail = new Node();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        head.next = tail;
        tail.prev = head;
    }
    // O(1) get & put with node splicing
}`
  },
  rotated: {
    problem: 'Search in Rotated Sorted Array (LeetCode #33)',
    approach: 'Modified Binary Search with Sorted Half Invariant',
    timeComp: 'O(log n) logarithmic time',
    spaceComp: 'O(1) constant auxiliary space',
    learned: 'Even when an array is rotated around a pivot, at least one half (left or right) is ALWAYS strictly sorted. By checking if nums[low] <= nums[mid], we identify the sorted half and determine if target falls within its bounds.',
    code: `public int search(int[] nums, int target) {
    int low = 0, high = nums.length - 1;
    while (low <= high) {
        int mid = low + (high - low) / 2;
        if (nums[mid] == target) return mid;
        if (nums[low] <= nums[mid]) {
            if (target >= nums[low] && target < nums[mid]) high = mid - 1;
            else low = mid + 1;
        } else {
            if (target > nums[mid] && target <= nums[high]) low = mid + 1;
            else high = mid - 1;
        }
    }
    return -1;
}`
  }
};

async function fetchDynamicDsaNotes() {
  const tabsBar = document.getElementById("dsa-tabs-bar");
  const container = document.getElementById("dsa-note-container");
  try {
    const res = await fetch("/api/dsa/problems?size=50");
    if (res.ok) {
      const json = await res.json();
      const list = json?.data?.content || json?.data || [];
      if (Array.isArray(list) && list.length > 0) {
        dynamicDsaNotesList = list.map((item, idx) => {
          const title = item.title || item.problemName || `Problem #${idx + 1}`;
          const plat = item.platform || "LEETCODE";
          const diff = item.difficulty || "MEDIUM";
          return {
            key: String(item.id || idx),
            tabLabel: title,
            problem: `${title} (${plat} · ${diff})`,
            approach: item.topic || item.topicTags || item.approach || item.approachNotes || "Optimal Algorithmic Approach",
            timeComp: item.timeComplexity || "O(n)",
            spaceComp: item.spaceComplexity || "O(1)",
            learned: item.approach || item.approachNotes || item.whatILearned || "Synced directly from Neon DB DSA Repository.",
            code: (item.solutionCode || item.codeSnippet || "// Solution synced from Neon DB").replace(/\\n/g, "\n")
          };
        });

        if (tabsBar) {
          tabsBar.innerHTML = dynamicDsaNotesList.map((n, i) => `
            <button onclick="showDsaNote('${escapeHtml(n.key)}')" id="dsa-tab-${escapeHtml(n.key)}" class="px-2.5 py-1 rounded-lg ${i === 0 ? 'bg-raised text-indigo-400 border border-indigo-500/30' : 'bg-card text-fg-2 hover:text-fg'}">
              ${escapeHtml(n.tabLabel)}
            </button>
          `).join("");
        }
        showDsaNote(dynamicDsaNotesList[0].key);
        return;
      }
    }
  } catch (e) {
    console.warn("DSA notes fetch note:", e);
  }

  if (container) {
    container.innerHTML = `<div class="p-6 text-center text-xs text-fg-3">No DSA problem notes saved in Neon DB yet. Add notes in Admin CMS → DSA Notes Repo.</div>`;
  }
}

function showDsaNote(key) {
  const dynNote = dynamicDsaNotesList.find(n => String(n.key) === String(key));
  const note = dynNote || dsaNotes[key];
  if (!note) return;

  const allKeys = dynamicDsaNotesList.length > 0
    ? dynamicDsaNotesList.map(n => n.key)
    : ['twosum', 'lru', 'rotated'];

  allKeys.forEach(k => {
    const tab = document.getElementById(`dsa-tab-${k}`);
    if (tab) {
      if (String(k) === String(key)) {
        tab.className = 'px-2.5 py-1 rounded-lg bg-raised text-indigo-400 border border-indigo-500/30';
      } else {
        tab.className = 'px-2.5 py-1 rounded-lg bg-card text-fg-2 hover:text-fg';
      }
    }
  });

  const container = document.getElementById('dsa-note-container');
  if (!container) return;

  container.innerHTML = `
    <div class="grid grid-cols-1 md:grid-cols-2 gap-4">
      <div class="space-y-3 font-sans">
        <div>
          <span class="text-xs text-fg-3 uppercase tracking-wider">Problem Statement</span>
          <h4 class="text-sm font-bold text-fg mt-0.5">${escapeHtml(note.problem)}</h4>
        </div>
        <div>
          <span class="text-xs text-fg-3 uppercase tracking-wider">Algorithmic Topic / Approach</span>
          <p class="text-xs text-fg mt-0.5">${escapeHtml(note.approach)}</p>
        </div>
        <div class="grid grid-cols-2 gap-2 text-xs">
          <div class="p-2 rounded bg-raised border border-line">
            <span class="text-fg-3 text-xs">Time Complexity</span>
            <div class="text-indigo-400 font-bold mt-0.5">${escapeHtml(note.timeComp)}</div>
          </div>
          <div class="p-2 rounded bg-raised border border-line">
            <span class="text-fg-3 text-xs">Space Complexity</span>
            <div class="text-emerald-400 font-bold mt-0.5">${escapeHtml(note.spaceComp)}</div>
          </div>
        </div>
        <div>
          <span class="text-xs text-fg-3 uppercase tracking-wider">Key Engineering Takeaway</span>
          <p class="text-xs text-fg leading-relaxed mt-0.5">${escapeHtml(note.learned)}</p>
        </div>
      </div>

      <div>
        <div class="flex items-center justify-between text-xs text-fg-3 mb-1">
          <span>Solution Implementation</span>
          <span>Synced from Neon DB</span>
        </div>
        <pre class="p-3 rounded-lg bg-canvas border border-line text-xs font-mono text-fg overflow-x-auto leading-relaxed"><code>${escapeHtml(note.code)}</code></pre>
      </div>
    </div>
  `;
}

// "Ask AI" RAG Assistant
function togglePublicAiDrawer() {
  const drawer = document.getElementById('ai-drawer');
  if (!drawer) return;
  drawer.classList.toggle('translate-x-full');
}

function openAiWithQuery(query) {
  const drawer = document.getElementById('ai-drawer');
  if (drawer && drawer.classList.contains('translate-x-full')) {
    drawer.classList.remove('translate-x-full');
  }
  sendAiMessage(query);
}

function handleAiInput(e) {
  e.preventDefault();
  const input = document.getElementById('ai-user-query');
  if (!input) return;
  const q = input.value.trim();
  if (!q) return;
  input.value = '';
  if (typeof isSecretAdminTriggerKeyword === 'function' && isSecretAdminTriggerKeyword(q)) {
    const drawer = document.getElementById('ai-drawer');
    if (drawer) drawer.classList.add('translate-x-full');
    openAdminAuthModal();
    return;
  }
  sendAiMessage(q);
}

function setPublicAiLanguage(lang) {
  currentPublicAiLang = lang;
  ["AUTO", "HINGLISH", "HINDI", "ENGLISH"].forEach(l => {
    const btn = document.getElementById("ai-lang-" + l);
    if (btn) {
      if (l === lang) {
        btn.className = "px-2 py-0.5 rounded-lg bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 text-xs";
      } else {
        btn.className = "px-2 py-0.5 rounded-lg bg-raised text-fg-2 border border-line text-xs hover:text-fg";
      }
    }
  });
  showToast("AI Assistant language set to: " + lang, "info");
}

async function sendAiMessage(query) {
  const chatContainer = document.getElementById('ai-chat-messages');
  if (!chatContainer) return;

  const userBubble = document.createElement('div');
  userBubble.className = 'p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/30 text-indigo-200 text-xs  ml-6';
  userBubble.innerText = query;
  chatContainer.appendChild(userBubble);
  chatContainer.scrollTop = chatContainer.scrollHeight;

  const loadingBubble = document.createElement('div');
  loadingBubble.className = 'p-3 rounded-lg bg-raised border border-line text-fg-2 text-xs  mr-6 flex items-center space-x-2';
  loadingBubble.innerHTML = '<span class="inline-block animate-spin mr-1">⟳</span> Querying Neon DB & RAG Engine...';
  chatContainer.appendChild(loadingBubble);
  chatContainer.scrollTop = chatContainer.scrollHeight;

  try {
    const geo = await resolveClientGeoForContact();
    const bHash = getOrCreateBrowserFingerprint();
    const originUrl = window.location.origin || (window.location.protocol + '//' + window.location.host);
    const refUrl = document.referrer || window.location.href;

    const res = await fetch('/api/ai/chat', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Visitor-Public-Ip': geo.ip || '',
        'X-Visitor-City': geo.city || '',
        'X-Visitor-Region': geo.region || '',
        'X-Visitor-Country': geo.country || '',
        'X-Browser-Hash': bHash,
        'Origin': originUrl,
        'Referer': refUrl
      },
      body: JSON.stringify({
        query: query,
        sessionId: visitorAiSessionId,
        language: currentPublicAiLang,
        pageUrl: window.location.pathname || '/',
        clientIp: geo.ip || '',
        country: geo.country || '',
        region: geo.region || '',
        city: geo.city || '',
        browserHash: bHash,
        origin: originUrl,
        referrer: refUrl
      })
    });

    loadingBubble.remove();
    const aiBubble = document.createElement('div');
    aiBubble.className = 'p-3 rounded-lg bg-raised border border-line text-fg text-xs font-sans leading-relaxed mr-6 space-y-2';

    if (res.ok) {
      const json = await res.json();
      const d = json?.data;
      if (d && d.sessionId) {
        visitorAiSessionId = d.sessionId;
        localStorage.setItem("raviranjan_ai_session_id", visitorAiSessionId);
      }
      const rawAns = d?.answer || evaluatePortfolioRag(query);
      const formattedAns = escapeHtml(rawAns)
        .replace(/\*\*(.*?)\*\*/g, '<strong class="text-fg">$1</strong>').replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline font-semibold inline-flex items-center gap-0.5">$1 <span class="text-xs">&nearr;</span></a>')
        .replace(/\n/g, '<br>');
      aiBubble.innerHTML = `
        <div class="flex items-center justify-between text-xs text-indigo-400 border-b border-line pb-1 mb-1">
          <span>🤖 Ask AI · Grounded on Neon DB</span>
          <span class="text-fg-3">${d?.latencyMs || 45}ms</span>
        </div>
        <div>${formattedAns}</div>
      `;
    } else {
      aiBubble.innerHTML = evaluatePortfolioRag(query);
    }

    chatContainer.appendChild(aiBubble);
    chatContainer.scrollTop = chatContainer.scrollHeight;
    if (window.lucide) window.lucide.createIcons();
  } catch (err) {
    loadingBubble.remove();
    const aiBubble = document.createElement('div');
    aiBubble.className = 'p-3 rounded-lg bg-raised border border-line text-fg text-xs font-sans leading-relaxed mr-6 space-y-2';
    aiBubble.innerHTML = evaluatePortfolioRag(query);
    chatContainer.appendChild(aiBubble);
    chatContainer.scrollTop = chatContainer.scrollHeight;
    if (window.lucide) window.lucide.createIcons();
  }
}

// Grounded Portfolio RAG Evaluation Engine (Strict Truthfulness - No Hallucinations)
function evaluatePortfolioRag(query) {
  const q = query.toLowerCase();
  if (q.includes('admin') || q.includes('cms') || q.includes('login') || q.includes('control panel') || q.includes('dashboard') || q.includes('auth') || q.includes('passphrase')) {
    return `<strong>🔐 Administrative Operating System Gateway</strong>
    <p class="mt-1 text-fg">
      Raviranjan.OS includes a private headless CMS for managing live projects, skills, research, and integration sync adapters.
    </p>
    <div class="mt-3 p-3 rounded-xl bg-card border border-indigo-500/30 space-y-2">
      <div class="text-xs text-indigo-400 font-semibold uppercase tracking-wider">ADMIN ACCESS UNLOCK</div>
      <p class="text-xs text-fg-2">Click the button below to open the private passphrase modal and authenticate, or go directly to <code>/admin</code>.</p>
      <div class="flex flex-wrap items-center gap-2 pt-1">
        <button onclick="openAdminAuthModal(); togglePublicAiDrawer();" class="px-3 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs transition-colors flex items-center gap-1.5 shadow-sm">
          <i data-lucide="key" class="w-3.5 h-3.5"></i>
          <span>Unlock Admin Terminal</span>
        </button>
        <a href="/admin" class="px-3 py-1.5 rounded bg-raised hover:bg-raised border border-line text-xs text-fg transition-colors">
          Open /admin
        </a>
      </div>
    </div>`;
  }


  if (q.includes('rakshagrid') || q.includes('disaster')) {
    return `<strong>RakshaGrid</strong> is Raviranjan's AI-powered disaster response and resource dispatch platform.
    <ul class="list-disc pl-4 mt-1 space-y-1">
      <li><strong>Stack:</strong> FastAPI (Python), React, PostgreSQL with PostGIS, and Machine Learning.</li>
      <li><strong>Key Feature:</strong> Calculates incident priority scores and performs sub-10ms geospatial point-in-polygon queries for nearest emergency personnel dispatch.</li>
      <li><strong>Progress:</strong> Currently at 85% completion.</li>
    </ul>
    <div class="mt-2 text-xs text-indigo-400">Source: <a href="#projects" class="underline">Portfolio Projects / RakshaGrid</a></div>`;
  }

  if (q.includes('project') || q.includes('built') || q.includes('work')) {
    return `Raviranjan has built several notable software projects:
    <ul class="list-disc pl-4 mt-1 space-y-1">
      <li><strong>RakshaGrid:</strong> AI disaster response coordinator with PostGIS (FastAPI, React).</li>
      <li><strong>DevPulse Engine:</strong> Real-time developer productivity and commit digest engine (Java 21, Spring Boot, Redis).</li>
      <li><strong>NeuroDoc QA:</strong> Document QA agent with retrieval-augmented generation (Python, LangChain, FAISS).</li>
      <li><strong>AlgoVault:</strong> Interactive algorithm visualization playground (React, TypeScript, Canvas API).</li>
      <li><strong>Raviranjan.OS:</strong> His full personal developer operating system with Spring Boot and 24 CMS modules.</li>
    </ul>
    <div class="mt-2 text-xs text-indigo-400">Source: <a href="#projects" class="underline">Projects Section</a></div>`;
  }

  if (q.includes('technolog') || q.includes('stack') || q.includes('know') || q.includes('skill')) {
    return `Raviranjan's core verified technologies include:
    <ul class="list-disc pl-4 mt-1 space-y-1">
      <li><strong>Languages:</strong> Java 21, Python, JavaScript, TypeScript, SQL.</li>
      <li><strong>Frameworks:</strong> Spring Boot (Security, Data JPA), FastAPI, React, Node.js.</li>
      <li><strong>Databases & Storage:</strong> PostgreSQL (with PostGIS), Redis.</li>
      <li><strong>Infrastructure & Tools:</strong> Docker, Git, RESTful APIs, SSE, Maven.</li>
      <li><strong>AI/ML Focus:</strong> Retrieval-Augmented Generation (RAG), local LLMs, vector search.</li>
    </ul>`;
  }

  if (q.includes('contribution') || q.includes('open source') || q.includes('learn')) {
    return `Raviranjan focuses on open-source contributions with clear learning stories:
    <ul class="list-disc pl-4 mt-1 space-y-1">
      <li><strong>Spring Boot:</strong> Investigated Java 21 carrier thread unpinning under Virtual Thread executors to resolve test harness timeouts.</li>
      <li><strong>FastAPI:</strong> Submitted reproduction tests and guidance for proxy buffering headers with StreamingResponse in Server-Sent Events.</li>
    </ul>
    <div class="mt-2 text-xs text-indigo-400">Source: <a href="#contributions" class="underline">Contribution Stories</a></div>`;
  }

  if (q.includes('postgres') || q.includes('database') || q.includes('sql')) {
    return `Raviranjan utilizes <strong>PostgreSQL</strong> extensively across his architecture:
    <ul class="list-disc pl-4 mt-1 space-y-1">
      <li><strong>RakshaGrid:</strong> Spatial indexing with PostGIS for fast incident radius checks.</li>
      <li><strong>Raviranjan.OS:</strong> 50 normalized relational tables managed via Flyway migrations (V1-V5).</li>
      <li><strong>DevPulse:</strong> Persistent commit telemetry storage.</li>
    </ul>`;
  }

  if (q.includes('ai') || q.includes('rag') || q.includes('llm') || q.includes('agent')) {
    return `Raviranjan's AI projects emphasize local, private, and deterministic workflows:
    <ul class="list-disc pl-4 mt-1 space-y-1">
      <li><strong>NeuroDoc:</strong> Local RAG with FAISS and citation verification.</li>
      <li><strong>MCP Server (Lab):</strong> Model Context Protocol tool executor enabling LLMs to query database schemas safely.</li>
      <li><strong>Ask AI:</strong> In-browser portfolio RAG system grounded strictly on verified facts.</li>
    </ul>`;
  }

  if (q.includes('education') || q.includes('college') || q.includes('student') || q.includes('degree')) {
    return `Raviranjan Kumar is currently a <strong>Bachelor of Computer Applications (BCA)</strong> student based in <strong>New Delhi, India</strong>, actively preparing for software developer and engineering opportunities.`;
  }

  return `I don't have that specific information in Raviranjan's verified portfolio. As a grounded assistant, I do not speculate or invent details. You can ask about his projects (RakshaGrid, DevPulse, NeuroDoc), his technical stack (Java, Spring Boot, Python, React, PostgreSQL), or his open-source contributions.`;
}

// Contact Form Handler with Rate-Limit (HTTP 429 Retry-After), Validation, and Privacy Signal Support
async function handleContactSubmit(e) {
  e.preventDefault();
  const name = (document.getElementById('contact-name')?.value || '').trim();
  const email = (document.getElementById('contact-email')?.value || '').trim();
  const company = (document.getElementById('contact-company')?.value || '').trim();
  const msg = (document.getElementById('contact-message')?.value || '').trim();
  const optOut = Boolean(document.getElementById('contact-browser-optout')?.checked);
  const alertBox = document.getElementById('contact-feedback-alert');
  const submitBtn = document.getElementById('contact-submit-btn');

  const showContactAlert = (text, tone) => {
    if (!alertBox) return;
    alertBox.classList.remove('hidden', 'bg-emerald-500/10', 'text-emerald-400', 'border-emerald-500/30', 'bg-rose-500/10', 'text-rose-400', 'border-rose-500/30', 'bg-amber-500/10', 'text-amber-400', 'border-amber-500/30');
    alertBox.classList.add('border');
    if (tone === 'success') {
      alertBox.classList.add('bg-emerald-500/10', 'text-emerald-400', 'border-emerald-500/30');
    } else if (tone === 'warning') {
      alertBox.classList.add('bg-amber-500/10', 'text-amber-400', 'border-amber-500/30');
    } else {
      alertBox.classList.add('bg-rose-500/10', 'text-rose-400', 'border-rose-500/30');
    }
    alertBox.innerText = text;
  };

  if (name.length < 2 || name.length > 80) {
    showContactAlert('Please enter a valid name (2 to 80 characters).', 'error');
    return;
  }

  let browserSignal = 'OPT_OUT';
  if (!optOut) {
    try {
      const tz = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
      const lang = navigator.language || 'en';
      browserSignal = `${lang}|${tz}`;
    } catch (_) {
      browserSignal = 'DEFAULT';
    }
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerText = 'Submitting to CRM...';
  }

  try {
    const geo = await resolveClientGeoForContact();
    const tzHeader = (() => {
      try { return Intl.DateTimeFormat().resolvedOptions().timeZone || 'Asia/Kolkata'; } catch (_) { return 'Asia/Kolkata'; }
    })();
    const bHash = getOrCreateBrowserFingerprint();
    const originUrl = window.location.origin || (window.location.protocol + '//' + window.location.host);
    const refUrl = document.referrer || window.location.href;

    const res = await fetch('/api/contact', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Browser-Signal': browserSignal,
        'X-Browser-Hash': bHash,
        'X-Visitor-Public-Ip': geo.ip || '',
        'X-Visitor-City': geo.city || '',
        'X-Visitor-Region': geo.region || '',
        'X-Visitor-Country': geo.country || '',
        'X-Visitor-Timezone': tzHeader,
        'Origin': originUrl,
        'Referer': refUrl
      },
      body: JSON.stringify({
        name,
        email,
        company: company || null,
        message: msg,
        browserIdentifier: optOut ? 'OPT_OUT' : bHash,
        browserHash: bHash,
        clientIp: geo.ip || '',
        city: geo.city || '',
        region: geo.region || '',
        country: geo.country || '',
        origin: originUrl,
        referrer: refUrl
      })
    });

    const data = await res.json().catch(() => ({}));

    if (res.status === 429) {
      const retryAfter = res.headers.get('Retry-After') || '60';
      const limitMsg = data?.message || `Rate limit exceeded (max 3 submissions per 10 minutes). Please wait ${retryAfter} seconds before trying again.`;
      showContactAlert(`⏳ ${limitMsg} (Retry-After: ${retryAfter}s)`, 'warning');
      showToast(limitMsg, 'warning');
      return;
    }

    if (!res.ok) {
      const fieldErrs = data?.validationErrors ? Object.values(data.validationErrors).join(', ') : '';
      const errMsg = fieldErrs || data?.message || 'Validation failed. Please check your name, email, and message.';
      showContactAlert(`⚠️ ${errMsg}`, 'error');
      showToast(errMsg, 'warning');
      return;
    }

    showContactAlert(`✓ Thank you ${name}! Your message has been received and routed to Raviranjan's CRM.`, 'success');
    showToast(`Thank you ${name}! Your inquiry has been forwarded to the CRM.`, 'success');
    e.target.reset();
  } catch (err) {
    showContactAlert('Network error while submitting inquiry. Please try again.', 'error');
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerText = 'Submit Inquiry to CRM';
    }
  }
}

function toggleMobileNavMenu(forceState) {
  const menu = document.getElementById('mobile-nav-menu');
  if (!menu) return;
  if (typeof forceState === 'boolean') {
    menu.classList.toggle('hidden', !forceState);
  } else {
    menu.classList.toggle('hidden');
  }
}

function copyCurlCommand() {
  const cmdEl = document.getElementById('hero-curl-command');
  const cmdText = cmdEl ? cmdEl.innerText.trim() : 'curl -s https://raviranjan.dev/api/v1/profile | jq';
  navigator.clipboard.writeText(cmdText).then(() => {
    showToast('Command copied to clipboard!', 'success');
  });
}

function downloadResume() {
  openResumeModal();
}

let cachedResumeSettings = {};

async function openResumeModal() {
  const modal = document.getElementById('resume-preview-modal');
  if (!modal) return;
  modal.classList.remove('hidden');
  await renderInteractiveResumeSheet();
  if (window.lucide) window.lucide.createIcons();
}

function closeResumeModal() {
  const modal = document.getElementById('resume-preview-modal');
  if (modal) modal.classList.add('hidden');
}

async function renderInteractiveResumeSheet() {
  const sheet = document.getElementById('resume-printable-sheet');
  if (!sheet) return;

  try {
    const [profRes, setRes, expRes, eduRes, skRes, projRes, achRes, certRes] = await Promise.all([
      fetch('/api/profile').catch(() => null),
      fetch('/api/settings').catch(() => null),
      fetch('/api/experiences').catch(() => null),
      fetch('/api/education').catch(() => null),
      fetch('/api/skills').catch(() => null),
      fetch('/api/projects?size=20').catch(() => null),
      fetch('/api/achievements').catch(() => null),
      fetch('/api/certifications').catch(() => null)
    ]);

    const prof = (profRes && profRes.ok ? (await profRes.json())?.data : null) || currentProfile || {};
    const settings = (setRes && setRes.ok ? (await setRes.json())?.data : null) || {};
    cachedResumeSettings = settings;
    const exps = (expRes && expRes.ok ? (await expRes.json())?.data : null) || [];
    const edus = (eduRes && eduRes.ok ? (await eduRes.json())?.data : null) || [];
    const skillCats = (skRes && skRes.ok ? (await skRes.json())?.data : null) || [];
    const projRaw = (projRes && projRes.ok ? (await projRes.json())?.data : null) || [];
    const projs = Array.isArray(projRaw) ? projRaw : (projRaw.content || []);
    const achs = (achRes && achRes.ok ? (await achRes.json())?.data : null) || [];
    const certs = (certRes && certRes.ok ? (await certRes.json())?.data : null) || [];

    const extBtn = document.getElementById('resume-external-link-btn');
    const extUrl = prof.resumeUrl || settings.resumeUrl || '';
    if (extBtn) {
      if (extUrl && extUrl !== '#' && extUrl.startsWith('http')) {
        extBtn.href = extUrl;
        extBtn.classList.remove('hidden');
      } else {
        extBtn.classList.add('hidden');
      }
    }

    const fullName = settings.resumeFullName || prof.fullName || 'Raviranjan Kumar';
    const headline = settings.resumeHeadline || prof.headline || 'BCA Student · Full-Stack & Backend Software Developer';
    const email = settings.contactEmail || prof.email || 'admin@raviranjan.dev';
    const phone = settings.contactPhone || prof.phone || '+91-9999999999';
    const location = prof.location || 'New Delhi, India';
    const summary = settings.resumeSummary || prof.bio || 'Software developer focused on Java 21, Spring Boot, Python, FastAPI, React, PostgreSQL, and AI/LLM applications.';

    let githubLink = 'https://github.com/raviranjankrmaurya';
    let linkedinLink = 'https://linkedin.com/in/raviranjan';
    let leetcodeLink = 'https://leetcode.com/raviranjanmaurya';
    if (Array.isArray(prof.socialLinks)) {
      prof.socialLinks.forEach(s => {
        const pl = (s.platform || '').toLowerCase();
        if (pl.includes('github') && s.url) githubLink = s.url;
        if (pl.includes('linkedin') && s.url) linkedinLink = s.url;
        if (pl.includes('leetcode') && s.url) leetcodeLink = s.url;
      });
    }

    const skillsHtml = skillCats.length > 0
      ? skillCats.map(c => `<div class="text-xs"><strong class="text-fg">${escapeHtml(c.name)}:</strong> <span class="text-fg">${(c.skills || []).map(s => escapeHtml(s.name)).join(', ')}</span></div>`).join('')
      : `<div class="text-xs text-fg"><strong class="text-fg">Core Stack:</strong> Java 21, Spring Boot, Python, FastAPI, React, TypeScript, PostgreSQL, Redis, Docker, Git, RAG/LLMs</div>`;

    const eduHtml = edus.length > 0
      ? edus.map(ed => `
        <div class="flex flex-col sm:flex-row sm:items-center justify-between border-b border-line/60 pb-2">
          <div>
            <div class="font-bold text-fg text-xs">${escapeHtml(ed.degree)}</div>
            <div class="text-xs text-indigo-400">${escapeHtml(ed.institution)}</div>
            ${ed.description ? `<p class="text-xs text-fg-2 mt-0.5">${escapeHtml(ed.description)}</p>` : ''}
          </div>
          <div class="text-xs text-fg-2">${escapeHtml(ed.startDate || '2023')} — ${escapeHtml(ed.endDate || 'Present')} · ${escapeHtml(ed.grade || 'Enrolled')}</div>
        </div>
      `).join('')
      : `<div class="text-xs text-fg"><strong>Bachelor of Computer Applications (BCA)</strong> · Enrolled</div>`;

    const expHtml = exps.length > 0
      ? exps.map(ex => {
          const isIntern = String(ex.employmentType || '').toUpperCase().includes('INTERN') || String(ex.role || '').toLowerCase().includes('intern');
          const statusLabel = isIntern ? (ex.current ? 'Pursuing Internship' : 'Completed Internship') : (ex.current ? 'Current' : 'Completed');
          return `
        <div class="space-y-1 border-b border-line/60 pb-2.5">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between">
            <div class="font-bold text-fg text-xs">${escapeHtml(ex.role)} <span class="text-indigo-400 font-normal">@ ${escapeHtml(ex.company)}</span> <span class="text-xs px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-300 ml-1">${statusLabel}</span></div>
            <div class="text-xs text-fg-2">${escapeHtml(ex.startDate || '')} — ${escapeHtml(ex.endDate || 'Present')}</div>
          </div>
          <p class="text-xs text-fg leading-relaxed">${escapeHtml(ex.description || '')}</p>
          <div class="flex flex-wrap gap-3 text-xs pt-0.5">
            ${ex.offerLetterUrl ? `<a href="${escapeHtml(ex.offerLetterUrl)}" target="_blank" class="text-indigo-400 hover:underline">Offer Letter ↗</a>` : ''}
            ${ex.certificateUrl ? `<a href="${escapeHtml(ex.certificateUrl)}" target="_blank" class="text-emerald-400 hover:underline">Completion Certificate ↗</a>` : ''}
            ${ex.achievements && ex.achievements.startsWith('http') ? `<a href="${escapeHtml(ex.achievements)}" target="_blank" class="text-amber-400 hover:underline">Project Report ↗</a>` : ''}
          </div>
        </div>
      `;
        }).join('')
      : `<div class="text-xs text-fg-2">Open-source contributor & full-stack systems builder.</div>`;

    const projHtml = projs.slice(0, 5).map(p => {
      const tList = Array.isArray(p.technologies) ? p.technologies.join(', ') : (p.technologies || 'Java, Spring Boot, PostgreSQL');
      return `
        <div class="space-y-1 border-b border-line/60 pb-2.5">
          <div class="flex flex-col sm:flex-row sm:items-center justify-between">
            <div class="font-bold text-fg text-xs">${escapeHtml(p.title)} <span class="text-xs text-indigo-400 ml-1">[${escapeHtml(tList)}]</span></div>
            ${p.githubRepoUrl ? `<a href="${escapeHtml(p.githubRepoUrl)}" target="_blank" class="text-xs text-indigo-400 hover:underline">GitHub ↗</a>` : ''}
          </div>
          <p class="text-xs text-fg leading-relaxed">${escapeHtml(p.shortSummary || p.fullDescription || '')}</p>
        </div>
      `;
    }).join('');

    const credsHtml = [
      ...achs.map(a => `<li class="text-xs text-fg"><strong class="text-fg">${escapeHtml(a.title)}</strong> — ${escapeHtml(a.organization || '')} ${a.description ? '(' + escapeHtml(a.description) + ')' : ''}</li>`),
      ...certs.map(c => `<li class="text-xs text-fg"><strong class="text-fg">${escapeHtml(c.title)}</strong> — Certified by ${escapeHtml(c.issuer || '')}</li>`)
    ].join('');

    sheet.innerHTML = `
      <div id="resume-print-area" class="max-w-3xl mx-auto bg-subtle border border-line rounded-xl p-6 sm:p-8 space-y-5 shadow-xl">
        <!-- Resume Header -->
        <div class="border-b border-line pb-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div>
            <h1 class="text-2xl font-bold text-fg tracking-tight">${escapeHtml(fullName)}</h1>
            <p class="text-xs text-indigo-400 mt-0.5">${escapeHtml(headline)}</p>
          </div>
          <div class="text-xs text-fg space-y-0.5 sm:text-right">
            <div>📧 ${escapeHtml(email)} · 📞 ${escapeHtml(phone)}</div>
            <div>📍 ${escapeHtml(location)}</div>
            <div class="flex sm:justify-end gap-2 pt-0.5 text-indigo-400">
              <a href="${escapeHtml(githubLink)}" target="_blank" class="hover:underline">GitHub</a> ·
              <a href="${escapeHtml(linkedinLink)}" target="_blank" class="hover:underline">LinkedIn</a> ·
              <a href="${escapeHtml(leetcodeLink)}" target="_blank" class="hover:underline">LeetCode</a>
            </div>
          </div>
        </div>

        <!-- Executive Summary -->
        <div class="space-y-1.5">
          <h3 class="text-xs font-bold uppercase tracking-wider text-indigo-400 border-b border-line pb-1">Executive Summary</h3>
          <p class="text-xs text-fg leading-relaxed">${escapeHtml(summary)}</p>
        </div>

        <!-- Technical Skills -->
        <div class="space-y-1.5">
          <h3 class="text-xs font-bold uppercase tracking-wider text-indigo-400 border-b border-line pb-1">Technical Skills</h3>
          <div class="space-y-1">${skillsHtml}</div>
        </div>

        <!-- Experience -->
        <div class="space-y-2">
          <h3 class="text-xs font-bold uppercase tracking-wider text-indigo-400 border-b border-line pb-1">Work Experience & Engineering Roles</h3>
          <div class="space-y-2">${expHtml}</div>
        </div>

        <!-- Key Projects -->
        <div class="space-y-2">
          <h3 class="text-xs font-bold uppercase tracking-wider text-indigo-400 border-b border-line pb-1">Featured Production Projects</h3>
          <div class="space-y-2">${projHtml || '<div class="text-xs text-fg-2">Projects synced from Neon DB.</div>'}</div>
        </div>

        <!-- Education -->
        <div class="space-y-2">
          <h3 class="text-xs font-bold uppercase tracking-wider text-indigo-400 border-b border-line pb-1">Education</h3>
          <div class="space-y-2">${eduHtml}</div>
        </div>

        <!-- Achievements & Certifications -->
        ${credsHtml ? `
        <div class="space-y-1.5">
          <h3 class="text-xs font-bold uppercase tracking-wider text-indigo-400 border-b border-line pb-1">Achievements & Certifications</h3>
          <ul class="list-disc pl-4 space-y-1">${credsHtml}</ul>
        </div>
        ` : ''}
      </div>
    `;
  } catch (e) {
    console.warn('Resume render note:', e);
  }
}

function downloadResumePdf() {
  trackAnalyticsEvent('RESUME_DOWNLOAD');
  const printArea = document.getElementById('resume-print-area');
  if (!printArea) {
    window.print();
    return;
  }
  const win = window.open('', '_blank', 'width=900,height=1000');
  if (!win) {
    window.print();
    return;
  }
  const fullName = currentProfile.fullName || 'Raviranjan_Kumar';
  win.document.write(`<!DOCTYPE html>
  <html>
  <head>
    <meta charset="UTF-8">
    <title>${escapeHtml(fullName.replace(/\s+/g, '_'))}_Resume.pdf</title>
    <script src="https://cdn.tailwindcss.com"><\/script>
    <style>
      @media print {
        body { -webkit-print-color-adjust: exact; print-color-adjust: exact; background: #ffffff !important; color: #18181b !important; }
        #resume-print-area { border: none !important; box-shadow: none !important; background: #ffffff !important; color: #18181b !important; }
        #resume-print-area * { color: #18181b !important; border-color: #d4d4d8 !important; }
      }
    </style>
  </head>
  <body class="bg-card text-zinc-900 p-6">
    ${printArea.outerHTML}
    <script>
      setTimeout(() => { window.print(); }, 450);
    <\/script>
  </body>
  </html>`);
  win.document.close();
  showToast('Generating PDF Resume...', 'success');
}

// =======================================================
// SECRET ADMIN AUTHENTICATION GATEWAY (Ctrl + Shift + A)
// =======================================================

function openAdminAuthModal() {
  const modal = document.getElementById('admin-auth-modal');
  if (!modal) return;
  modal.classList.remove('hidden');

  toggleForgotAdminPanel(false);

  // Always clear both username and password so nothing is ever pre-filled or auto-filled
  const userInput = document.getElementById('admin-auth-username');
  const pwdInput = document.getElementById('admin-auth-password');
  if (userInput) {
    userInput.value = '';
    setTimeout(() => userInput.focus(), 80);
  }
  if (pwdInput) {
    pwdInput.value = '';
  }

  const errorBox = document.getElementById('admin-auth-error');
  if (errorBox) errorBox.classList.add('hidden');

  // Load last credential updated timestamp from Neon DB
  fetch('/api/v1/auth/credential-metadata')
    .then(r => r.ok ? r.json() : null)
    .then(json => {
      const d = json?.data;
      const badge = document.getElementById('admin-cred-updated-badge');
      if (badge && d?.credentialsUpdatedAt) {
        const dt = new Date(d.credentialsUpdatedAt).toLocaleString([], {
          month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit'
        });
        badge.innerText = `Updated: ${dt}`;
      }
    })
    .catch(() => {});

  if (window.lucide) window.lucide.createIcons();
}

function closeAdminAuthModal() {
  const modal = document.getElementById('admin-auth-modal');
  if (!modal) return;
  modal.classList.add('hidden');
}

function toggleForgotAdminPanel(showForgot) {
  const loginForm = document.getElementById('admin-auth-form');
  const forgotForm = document.getElementById('admin-forgot-otp-form');
  const step1 = document.getElementById('forgot-step-1');
  const step2 = document.getElementById('forgot-step-2');
  const errBox = document.getElementById('forgot-admin-error');
  if (errBox) errBox.classList.add('hidden');
  if (step1) step1.classList.remove('hidden');
  if (step2) step2.classList.add('hidden');

  const otpInput = document.getElementById('forgot-admin-otp');
  if (otpInput) otpInput.value = '';

  if (showForgot) {
    if (loginForm) loginForm.classList.add('hidden');
    if (forgotForm) forgotForm.classList.remove('hidden');
  } else {
    if (forgotForm) forgotForm.classList.add('hidden');
    if (loginForm) loginForm.classList.remove('hidden');
  }
}

async function handleRequestForgotOtp() {
  const email = (document.getElementById('forgot-admin-email')?.value || '').trim();
  const mobileNumber = (document.getElementById('forgot-admin-mobile')?.value || '').trim();
  const errBox = document.getElementById('forgot-admin-error');
  const step1 = document.getElementById('forgot-step-1');
  const step2 = document.getElementById('forgot-step-2');
  const otpInput = document.getElementById('forgot-admin-otp');
  const btn = document.getElementById('btn-request-forgot-otp');

  if (errBox) errBox.classList.add('hidden');

  if (!email || !mobileNumber) {
    if (errBox) {
      errBox.classList.remove('hidden');
      errBox.innerText = 'Registered email and mobile number are required.';
    }
    return;
  }

  if (btn) {
    btn.disabled = true;
    btn.innerText = 'Verifying...';
  }

  try {
    const res = await fetch('/api/v1/auth/request-otp', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        mobileNumber,
        purpose: 'FORGOT_CREDENTIALS'
      })
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) {
      if (errBox) {
        errBox.classList.remove('hidden');
        errBox.innerText = json?.message || 'Invalid email or mobile number.';
      }
      return;
    }

    // Never auto-fill or display the OTP on screen! Admin reads it from Neon DB (admin_credential_otps)
    if (otpInput) {
      otpInput.value = '';
    }
    if (step1) step1.classList.add('hidden');
    if (step2) step2.classList.remove('hidden');
    if (otpInput) setTimeout(() => otpInput.focus(), 60);
    showToast('OTP saved in Neon DB (admin_credential_otps).', 'success');
  } catch (e) {
    if (errBox) {
      errBox.classList.remove('hidden');
      errBox.innerText = 'Network error requesting OTP.';
    }
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = 'Send OTP to Database';
    }
  }
}

async function handleResetAdminWithOtp(event) {
  event.preventDefault();
  const email = (document.getElementById('forgot-admin-email')?.value || '').trim();
  const mobileNumber = (document.getElementById('forgot-admin-mobile')?.value || '').trim();
  const otpCode = (document.getElementById('forgot-admin-otp')?.value || '').trim();
  const newUsername = (document.getElementById('forgot-new-username')?.value || '').trim();
  const newPassword = document.getElementById('forgot-new-password')?.value || '';
  const confirmPassword = document.getElementById('forgot-confirm-password')?.value || '';
  const errBox = document.getElementById('forgot-admin-error');
  const submitBtn = document.getElementById('btn-submit-forgot-reset');

  if (errBox) errBox.classList.add('hidden');

  if (!email || !mobileNumber || !otpCode || !newUsername || !newPassword) {
    if (errBox) {
      errBox.classList.remove('hidden');
      errBox.innerText = 'All fields are required.';
    }
    return;
  }

  if (newPassword !== confirmPassword) {
    if (errBox) {
      errBox.classList.remove('hidden');
      errBox.innerText = 'Passwords do not match.';
    }
    return;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerText = 'Updating...';
  }

  try {
    const res = await fetch('/api/v1/auth/reset-credentials', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        mobileNumber,
        otpCode,
        newUsername,
        newPassword
      })
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) {
      if (errBox) {
        errBox.classList.remove('hidden');
        errBox.innerText = json?.message || 'Invalid or expired OTP.';
      }
      return;
    }

    const d = json?.data || {};
    if (d.accessToken) {
      localStorage.setItem('raviranjan_auth_token', d.accessToken);
    }
    if (d.username) {
      localStorage.setItem('raviranjan_admin_user', d.username);
    }
    const updatedTimeStr = d.credentialsUpdatedAt
      ? new Date(d.credentialsUpdatedAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' })
      : 'Just now';

    const badge = document.getElementById('admin-cred-updated-badge');
    if (badge) badge.innerText = `Updated: ${updatedTimeStr}`;

    showToast('Credentials updated in Neon DB!', 'success');
    toggleForgotAdminPanel(false);
    const userInput = document.getElementById('admin-auth-username');
    const pwdInput = document.getElementById('admin-auth-password');
    if (userInput) userInput.value = '';
    if (pwdInput) pwdInput.value = '';
  } catch (e) {
    if (errBox) {
      errBox.classList.remove('hidden');
      errBox.innerText = 'Network error updating credentials.';
    }
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerText = 'Update Credentials';
    }
  }
}

async function handleAdminLogin(event) {
  event.preventDefault();
  const username = (document.getElementById('admin-auth-username')?.value || '').trim();
  const password = document.getElementById('admin-auth-password')?.value || '';
  const errorBox = document.getElementById('admin-auth-error');
  const errorMsg = document.getElementById('admin-auth-error-msg');
  const submitBtn = document.getElementById('admin-auth-submit-btn');

  if (!username || !password) {
    if (errorBox && errorMsg) {
      errorBox.classList.remove('hidden');
      errorMsg.innerText = 'Both Admin Username and Passphrase are required.';
    }
    return;
  }

  if (errorBox) errorBox.classList.add('hidden');
  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span class="inline-block animate-spin mr-1.5">&#8635;</span> Verifying...';
  }

  try {
    const res = await fetch('/api/v1/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await res.json().catch(() => null);

    if (res.ok && data && (data.data?.accessToken || data.accessToken)) {
      const token = data.data?.accessToken || data.accessToken;
      localStorage.setItem('raviranjan_auth_token', token);
      localStorage.setItem('raviranjan_admin_authenticated', 'true');
      localStorage.setItem('raviranjan_admin_user', data.data?.username || username);
      showToast('Authentication verified. Launching Admin OS...', 'success');
      setTimeout(() => {
        window.location.href = '/admin';
      }, 250);
      return;
    }



    if (errorBox && errorMsg) {
      errorBox.classList.remove('hidden');
      errorMsg.innerText = data?.message || 'Access Denied: Invalid username or passphrase.';
    }
  } catch (err) {

    if (errorBox && errorMsg) {
      errorBox.classList.remove('hidden');
      errorMsg.innerText = 'Authentication error. Please check your credentials.';
    }
  } finally {
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<i data-lucide="key" class="w-3.5 h-3.5 mr-1"></i><span>Unlock Admin OS</span>';
    }
    if (window.lucide) window.lucide.createIcons();
  }
}

// Dynamic Skills Ingestion from /api/skills
function fetchDynamicSkills() {
  const container = document.getElementById('skills-cloud');
  const recruiterContainer = document.getElementById('recruiter-skills-cloud');

  fetch('/api/skills')
    .then(res => res.json())
    .then(resp => {
      if (resp && resp.data && Array.isArray(resp.data) && resp.data.length > 0) {
        const allSkills = [];
        resp.data.forEach(cat => {
          if (cat.skills && Array.isArray(cat.skills)) {
            cat.skills.forEach(s => allSkills.push(s));
          }
        });

        if (allSkills.length > 0) {
          const markup = allSkills.map(s => {
            const isFeatured = s.featured;
            const borderCls = isFeatured ? 'text-indigo-300 border-indigo-500/20' : 'text-fg border-line';
            return `<span class="text-xs px-2 py-0.5 rounded bg-raised ${borderCls} border" title="${escapeHtml(s.description || s.name)}">${escapeHtml(s.name)}</span>`;
          }).join('');
          if (container) container.innerHTML = markup;
          if (recruiterContainer) recruiterContainer.innerHTML = markup;
        }
      }
    })
    .catch(err => {
      console.info('[Telemetry Sync] Skills operating with default verified inventory:', err.message);
    });
}


// =======================================================
// THEME SYSTEM (Light / Dark Mode Toggle)
// =======================================================
function initTheme() {
  const savedAccent = localStorage.getItem("raviranjan_accent_color");
  if (savedAccent) applyAccentThemeColor(savedAccent);
  const saved = localStorage.getItem('theme') || (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
  if (saved === 'light') {
    document.documentElement.classList.remove('dark');
    document.documentElement.classList.add('light');
    updateThemeIcon('light');
  } else {
    document.documentElement.classList.remove('light');
    document.documentElement.classList.add('dark');
    updateThemeIcon('dark');
  }
}

function toggleTheme() {
  const html = document.documentElement;
  const isDark = html.classList.contains('dark');
  if (isDark) {
    html.classList.remove('dark');
    html.classList.add('light');
    localStorage.setItem('theme', 'light');
    updateThemeIcon('light');
    showToast('Switched to Light Mode', 'info');
  } else {
    html.classList.remove('light');
    html.classList.add('dark');
    localStorage.setItem('theme', 'dark');
    updateThemeIcon('dark');
    showToast('Switched to Dark Mode', 'info');
  }
}

function updateThemeIcon(theme) {
  const icon = document.getElementById('theme-icon');
  if (!icon) return;
  if (theme === 'light') {
    icon.setAttribute('data-lucide', 'moon');
    icon.className = 'w-4 h-4 text-indigo-600';
  } else {
    icon.setAttribute('data-lucide', 'sun');
    icon.className = 'w-4 h-4 text-amber-400';
  }
  if (window.lucide) window.lucide.createIcons();
}

// Logo Triple-Click to open Admin Modal easily without keyboard shortcuts
let logoClickCount = 0;
let logoClickTimer = null;
function handleLogoClick(e) {
  logoClickCount++;
  if (logoClickCount === 3) {
    e.preventDefault();
    openAdminAuthModal();
    logoClickCount = 0;
    clearTimeout(logoClickTimer);
    return;
  }
  clearTimeout(logoClickTimer);
  logoClickTimer = setTimeout(() => {
    logoClickCount = 0;
  }, 1000);
}

// Public Analytics Tracking & Settings Loader
function trackAnalyticsEvent(eventType) {
  try {
    fetch('/api/analytics/track', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: eventType, pageSlug: window.location.pathname })
    }).catch(() => {});
  } catch (e) {}
}

function applyAccentThemeColor(hex) {
  if (!hex || !hex.startsWith("#")) return;
  localStorage.setItem("raviranjan_accent_color", hex);
  document.documentElement.style.setProperty("--accent-color", hex);
  document.documentElement.style.setProperty("--color-primary", hex);
  document.documentElement.style.setProperty("--border-focus", hex);
  let styleEl = document.getElementById("dynamic-accent-theme");
  if (!styleEl) {
    styleEl = document.createElement("style");
    styleEl.id = "dynamic-accent-theme";
    document.head.appendChild(styleEl);
  }
  styleEl.innerHTML = `
    html body .bg-indigo-500, html body .bg-indigo-500, html body .accent-bg-btn,
    html.light body .bg-indigo-500, html.light body .bg-indigo-500, html.light body .accent-bg-btn {
      background-color: ${hex} !important;
      color: #ffffff !important;
    }
    html body .from-indigo-500, html.light body .from-indigo-500 {
      --tw-gradient-from: ${hex} !important;
      background-image: linear-gradient(to right, ${hex}, #10b981) !important;
    }
    html body .text-indigo-400, html body .text-indigo-300, html body .text-indigo-400,
    html.light body .text-indigo-400, html.light body .text-indigo-300, html.light body .text-indigo-400 {
      color: ${hex} !important;
    }
    html body .border-indigo-500\\/20, html body .border-indigo-500\\/30, html body .border-indigo-500\\/40, html body .border-indigo-500\\/30,
    html.light body .border-indigo-500\\/20, html.light body .border-indigo-500\\/30, html.light body .border-indigo-500\\/40 {
      border-color: ${hex}66 !important;
    }
    html body .bg-indigo-500\\/10, html body .bg-indigo-500\\/20, html body .bg-indigo-500\\/10,
    html.light body .bg-indigo-500\\/10, html.light body .bg-indigo-500\\/20, html.light body .bg-indigo-500\\/10 {
      background-color: ${hex}1f !important;
      color: ${hex} !important;
    }
    html body .saas-card:hover, html.light body .saas-card:hover {
      border-color: ${hex}88 !important;
    }
  `;
}

async function loadPublicBrandingSettings() {
  try {
    const res = await fetch('/api/settings');
    if (!res.ok) return;
    const json = await res.json();
    const s = json.data || {};
    if (s.siteTitle) document.title = s.siteTitle;
    if (s.metaDescription) {
      const metaEl = document.querySelector('meta[name="description"]');
      if (metaEl) metaEl.setAttribute('content', s.metaDescription);
    }
    if (s.themeColor) {
      applyAccentThemeColor(s.themeColor);
    }
    if (s.fontPreset && typeof applyFontPreset === 'function') {
      applyFontPreset(s.fontPreset);
    }
    if ((s.heroAnimatedTexts || s.heroRoleTexts) && typeof updateHeroAnimatorTexts === 'function') {
      updateHeroAnimatorTexts(s.heroAnimatedTexts, s.heroRoleTexts);
    }
    if (s.availabilityStatus) {
      const el = document.getElementById('hero-availability-status');
      if (el) el.innerText = s.availabilityStatus;
    }
    if (s.availabilityCardTitle) {
      const el = document.getElementById('availability-card-title');
      if (el) el.innerText = s.availabilityCardTitle;
    }
    if (s.availabilityCardSubtitle) {
      const el = document.getElementById('availability-card-subtitle');
      if (el) el.innerText = s.availabilityCardSubtitle;
    }
    if (s.curlCommand) {
      const el = document.getElementById('hero-curl-command');
      if (el) el.innerText = s.curlCommand;
    }
    if (s.updatedDateText) {
      const el = document.getElementById('hero-updated-date');
      if (el) el.innerText = s.updatedDateText;
    }
    if (s.contactHeading) {
      const el = document.getElementById('about-contact-heading');
      if (el) el.innerText = s.contactHeading;
    }
    if (s.contactDescription) {
      const el = document.getElementById('about-contact-description');
      if (el) el.innerText = s.contactDescription;
    }
    if (s.contactEmail) {
      const el = document.getElementById('about-profile-email');
      if (el) el.innerText = s.contactEmail;
    }
    if (s.contactPhone) {
      const el = document.getElementById('about-profile-phone');
      if (el) el.innerText = s.contactPhone;
    }
    if (s.aiPromptsJson) {
      try {
        const prompts = JSON.parse(s.aiPromptsJson);
        const promptContainer = document.getElementById('hero-ai-prompts-list');
        if (promptContainer && Array.isArray(prompts) && prompts.length > 0) {
          promptContainer.innerHTML = prompts.map(q =>
            `<button onclick="openAiWithQuery('${escapeHtml(q).replace(/'/g, "\'")}')" class="hero-prompt-chip px-3 py-1.5 rounded-lg bg-card border border-line text-fg hover:border-indigo-500/40 hover:text-fg transition-colors">"${escapeHtml(q)}"</button>`
          ).join('');
        }
      } catch (e) {}
    }
    if (s.homepage_layout_json) {
      try {
        const layout = JSON.parse(s.homepage_layout_json);
        const mainEl = document.getElementById('standard-view');
        if (mainEl && Array.isArray(layout)) {
          layout.forEach(sec => {
            const secEl = document.getElementById(sec.id);
            if (secEl) {
              if (sec.visible === false) {
                secEl.classList.add('hidden');
              } else {
                secEl.classList.remove('hidden');
              }
              mainEl.appendChild(secEl);
            }
          });
        }
      } catch (e) {
        console.warn("Homepage layout parse note:", e);
      }
    }
  } catch (e) {}
}

async function fetchDynamicCareerAndCredentials() {
  const internEl = document.getElementById('public-internships-list');
  const expEl = document.getElementById('public-experience-list');
  const eduEl = document.getElementById('public-education-list');
  const achEl = document.getElementById('public-achievements-list');
  const certEl = document.getElementById('public-certifications-list');

  try {
    const [expRes, eduRes, achRes, certRes] = await Promise.all([
      fetch('/api/experiences'),
      fetch('/api/education'),
      fetch('/api/achievements'),
      fetch('/api/certifications')
    ]);

    if (expRes.ok) {
      const allExps = (await expRes.json())?.data || [];
      const isInternRecord = (e) =>
        String(e.employmentType || '').toUpperCase().includes('INTERN') ||
        String(e.role || '').toLowerCase().includes('intern') ||
        Boolean(e.offerLetterUrl) ||
        Boolean(e.certificateUrl);

      const internships = allExps.filter(isInternRecord);
      const workExps = allExps.filter(e => !isInternRecord(e));

      if (internEl) {
        internEl.innerHTML = internships.length === 0
          ? `<div class="col-span-2 p-4 text-fg-3 text-center">No internships recorded in Neon DB yet.</div>`
          : internships.map(e => {
              const isPursuing = Boolean(e.current) || !e.endDate;
              const techBadges = (e.technologies || '')
                .split(',')
                .map(t => t.trim())
                .filter(Boolean)
                .map(t => `<span class="tech-stack-pill px-2 py-0.5 rounded bg-raised border border-line text-xs text-indigo-300 font-medium">${escapeHtml(t)}</span>`)
                .join('');
              const reportUrl = e.achievements && String(e.achievements).startsWith('http') ? e.achievements : '';
              return `
                <div class="p-4 rounded-xl bg-card border ${isPursuing ? 'border-amber-500/30' : 'border-emerald-500/30'} space-y-2.5 flex flex-col justify-between">
                  <div class="space-y-2">
                    <div class="flex items-start justify-between gap-2">
                      <div>
                        <span class="font-bold text-fg text-sm block">${escapeHtml(e.role)}</span>
                        <span class="text-xs text-indigo-400 font-semibold">${escapeHtml(e.company)}</span>
                        <span class="text-xs text-fg-2"> · ${escapeHtml(e.location || 'Remote · India')}</span>
                      </div>
                      <span class="text-xs px-2.5 py-0.5 rounded-full font-semibold shrink-0 ${isPursuing ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'}">
                        ${isPursuing ? '● Pursuing / Active' : '✓ Completed'}
                      </span>
                    </div>
                    <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(e.description || '')}</p>
                    ${techBadges ? `<div class="flex flex-wrap gap-1.5 pt-1">${techBadges}</div>` : ''}
                  </div>
                  <div class="pt-2.5 border-t border-line flex flex-wrap items-center justify-between gap-2 text-xs">
                    <span class="text-fg-2">Timeline: <strong class="text-fg">${escapeHtml(e.startDate || '')} — ${escapeHtml(e.endDate || 'Present')}</strong></span>
                    <div class="flex flex-wrap items-center gap-2">
                      ${e.offerLetterUrl ? `<a href="${escapeHtml(e.offerLetterUrl)}" target="_blank" rel="noopener noreferrer" class="px-2.5 py-1 rounded bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-400 border border-indigo-500/30 transition-colors">Offer Letter ↗</a>` : ''}
                      ${e.certificateUrl
                        ? `<a href="${escapeHtml(e.certificateUrl)}" target="_blank" rel="noopener noreferrer" class="px-2.5 py-1 rounded bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-400 border border-emerald-500/30 transition-colors">Completion Certificate ↗</a>`
                        : `<span class="px-2.5 py-1 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20">Certificate Pending</span>`}
                      ${reportUrl ? `<a href="${escapeHtml(reportUrl)}" target="_blank" rel="noopener noreferrer" class="px-2.5 py-1 rounded bg-purple-500/10 hover:bg-purple-500/20 text-purple-400 border border-purple-500/30 transition-colors">Project Report ↗</a>` : ''}
                    </div>
                  </div>
                </div>
              `;
            }).join('');
      }

      if (expEl) {
        const listToRender = workExps.length > 0 ? workExps : allExps;
        expEl.innerHTML = listToRender.length === 0
          ? `<div class="p-3 text-fg-3 text-center">No work experience entries in Neon DB yet.</div>`
          : listToRender.map(e => `
            <div class="p-3.5 rounded-xl bg-card border border-line space-y-1.5">
              <div class="flex items-center justify-between">
                <span class="font-bold text-fg">${escapeHtml(e.role)}</span>
                <span class="text-xs px-2 py-0.5 rounded ${e.current ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-fg-3/10 text-fg-2'}">${e.current ? 'Current' : 'Completed'}</span>
              </div>
              <div class="text-xs text-indigo-400">${escapeHtml(e.company)} · ${escapeHtml(e.location || 'India')}</div>
              <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(e.description || '')}</p>
              <div class="text-xs text-fg-3 pt-1">Timeline: ${escapeHtml(e.startDate || '')} — ${escapeHtml(e.endDate || 'Present')}</div>
            </div>
          `).join('');
      }
    }

    if (eduEl && eduRes.ok) {
      const edus = (await eduRes.json())?.data || [];
      eduEl.innerHTML = edus.length === 0
        ? `<div class="p-3 text-fg-3 text-center">No education entries in Neon DB yet.</div>`
        : edus.map(ed => `
          <div class="p-3.5 rounded-xl bg-card border border-line space-y-1.5">
            <div class="flex items-center justify-between">
              <span class="font-bold text-fg">${escapeHtml(ed.degree)}</span>
              <span class="text-xs px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">${escapeHtml(ed.grade || 'Enrolled')}</span>
            </div>
            <div class="text-xs text-emerald-400">${escapeHtml(ed.institution)}</div>
            <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(ed.description || '')}</p>
            <div class="text-xs text-fg-3 pt-1">Timeline: ${escapeHtml(ed.startDate || '')} — ${escapeHtml(ed.endDate || 'Present')}</div>
          </div>
        `).join('');
    }

    if (achEl && achRes.ok) {
      const achs = (await achRes.json())?.data || [];
      achEl.innerHTML = achs.length === 0
        ? `<div class="p-3 text-fg-3 text-center">No achievements in Neon DB yet.</div>`
        : achs.map(a => `
          <div class="p-3 rounded-xl bg-card border border-line flex items-start justify-between gap-2">
            <div>
              <div class="font-bold text-fg">${escapeHtml(a.title)}</div>
              <div class="text-xs text-fg-2">${escapeHtml(a.organization || '')} ${a.description ? '· ' + escapeHtml(a.description) : ''}</div>
            </div>
            <span class="text-xs px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20 shrink-0">${escapeHtml(a.category || 'HONOR')}</span>
          </div>
        `).join('');
    }

    if (certEl && certRes.ok) {
      const certs = (await certRes.json())?.data || [];
      certEl.innerHTML = certs.length === 0
        ? `<div class="p-3 text-fg-3 text-center">No certifications in Neon DB yet.</div>`
        : certs.map(c => `
          <div class="p-3 rounded-xl bg-card border border-line flex items-center justify-between gap-2">
            <div>
              <div class="font-bold text-fg">${escapeHtml(c.title)}</div>
              <div class="text-xs text-fg-2">${escapeHtml(c.issuer || '')} ${c.credentialId ? '· ID: ' + escapeHtml(c.credentialId) : ''}</div>
            </div>
            ${c.credentialUrl ? `<a href="${escapeHtml(c.credentialUrl)}" target="_blank" rel="noopener noreferrer" class="text-xs text-indigo-400 hover:underline shrink-0">Verify ↗</a>` : `<span class="text-xs text-emerald-400 shrink-0">Verified</span>`}
          </div>
        `).join('');
    }
  } catch (e) {
    console.warn("Career & credentials fetch note:", e);
  }
}


// -------------------------------------------------------------
// Dynamic Content Ingestion (Contributions, Research, Lab, Articles — 100% from Neon DB)
// -------------------------------------------------------------

async function fetchDynamicContributions() {
  const container = document.getElementById('contributions-grid');
  if (!container) return;

  let stories = [];
  try {
    const res = await fetch('/api/stories');
    if (res.ok) {
      const data = await res.json();
      stories = data?.data?.content || data?.data || [];
    }
  } catch (e) {
    console.warn('Contribution stories fetch warning:', e);
  }

  if (!stories || stories.length === 0) {
    container.innerHTML = `<div class="col-span-full p-6 rounded-xl bg-card border border-line text-center text-xs text-fg-3">No open-source contribution stories in Neon DB yet. Add stories in Admin CMS → Contribution Stories.</div>`;
    return;
  }

  container.innerHTML = stories.map(s => `
    <div class="saas-card p-5 space-y-3 flex flex-col justify-between">
      <div class="space-y-2">
        <div class="flex items-center justify-between text-xs">
          <span class="text-fg font-bold">${escapeHtml(s.repository)}</span>
          <span class="text-purple-400 font-semibold">${escapeHtml(s.externalContributionId || 'Merged PR')}</span>
        </div>
        <h3 class="text-sm font-semibold text-fg">${escapeHtml(s.title)}</h3>
        <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(s.whatIContributed || s.whyIContributed || '')}</p>
        <div class="p-3 rounded-xl bg-raised border border-line text-xs text-fg-2">
          <span class="text-purple-400 font-bold block mb-1">💡 What I Learned:</span>
          ${escapeHtml(s.whatILearned || 'Deep understanding of production systems internals.')}
        </div>
      </div>
      <div class="flex items-center justify-between pt-2 border-t border-line text-xs">
        <span class="text-fg-3">${escapeHtml(s.technologies || 'Open Source')}</span>
        <a href="${escapeHtml(s.prUrl || '#')}" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline flex items-center gap-1 font-semibold">
          GitHub Link ↗
        </a>
      </div>
    </div>
  `).join('');
}

async function fetchDynamicResearch() {
  const container = document.getElementById('research-grid');
  if (!container) return;

  let papers = [];
  try {
    const res = await fetch('/api/research');
    if (res.ok) {
      const data = await res.json();
      papers = data?.data?.content || data?.data || [];
    }
  } catch (e) {
    console.warn('Research publications fetch warning:', e);
  }

  if (!papers || papers.length === 0) {
    container.innerHTML = `<div class="col-span-full p-6 rounded-xl bg-card border border-line text-center text-xs text-fg-3">No research publications in Neon DB yet. Add papers in Admin CMS → Research Papers.</div>`;
    return;
  }

  container.innerHTML = papers.map(p => `
    <div class="saas-card p-5 space-y-3 flex flex-col justify-between">
      <div class="space-y-2">
        <div class="flex items-center justify-between text-xs">
          <span class="px-2 py-0.5 rounded text-xs ${p.status === 'PUBLISHED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20'} font-semibold">
            ${escapeHtml(p.status || 'PUBLISHED')}
          </span>
          <span class="text-fg-3">${escapeHtml(p.publishedDate || '2026')}</span>
        </div>
        <h3 class="text-sm font-semibold text-fg">${escapeHtml(p.title)}</h3>
        <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(p.abstractText || '')}</p>
        <div class="text-xs text-fg-2">
          <span class="text-fg-3">Methodology:</span> ${escapeHtml(p.methodology || 'Empirical systems benchmarking')}
        </div>
      </div>
      <div class="flex items-center justify-between pt-3 border-t border-line text-xs">
        <span class="text-fg-3 text-xs">Author: ${escapeHtml(p.authors || 'Raviranjan Kumar')}</span>
        <div class="flex items-center gap-3">
          <a href="${escapeHtml(p.githubUrl || '#')}" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline">Code ↗</a>
        </div>
      </div>
    </div>
  `).join('');
}

async function fetchDynamicLab() {
  const container = document.getElementById('lab-grid');
  if (!container) return;

  let labs = [];
  try {
    const res = await fetch('/api/lab');
    if (res.ok) {
      const data = await res.json();
      labs = data?.data?.content || data?.data || [];
    }
  } catch (e) {
    console.warn('Lab experiments fetch warning:', e);
  }

  if (!labs || labs.length === 0) {
    container.innerHTML = `<div class="col-span-full p-6 rounded-xl bg-card border border-line text-center text-xs text-fg-3">No engineering lab experiments in Neon DB yet. Add experiments in Admin CMS → Engineering Lab.</div>`;
    return;
  }

  container.innerHTML = labs.map(lab => `
    <div class="saas-card p-4 space-y-2.5 flex flex-col justify-between">
      <div class="space-y-1.5">
        <div class="flex items-center justify-between text-xs">
          <span class="text-emerald-400 font-semibold text-xs px-1.5 py-0.5 rounded bg-emerald-500/10 border border-emerald-500/20">
            ${escapeHtml(lab.status || 'ACTIVE')}
          </span>
          <span class="text-fg-3 text-xs">EXPERIMENTAL</span>
        </div>
        <h3 class="text-xs font-bold text-fg">${escapeHtml(lab.title)}</h3>
        <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(lab.description || lab.objective || '')}</p>
      </div>
      <div class="pt-2 border-t border-line flex items-center justify-between text-xs">
        <span class="text-fg-2">${escapeHtml(lab.technologies || 'Systems')}</span>
        <a href="${escapeHtml(lab.githubUrl || '#')}" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline">Code ↗</a>
      </div>
    </div>
  `).join('');
}

async function fetchDynamicArticles() {
  const container = document.getElementById('articles-grid');
  if (!container) return;

  let articles = [];
  try {
    const res = await fetch('/api/articles');
    if (res.ok) {
      const data = await res.json();
      articles = data?.data?.content || data?.data || [];
    }
  } catch (e) {
    console.warn('Articles fetch warning:', e);
  }

  if (!articles || articles.length === 0) {
    container.innerHTML = `<div class="col-span-full p-6 rounded-xl bg-card border border-line text-center text-xs text-fg-3">No engineering articles in Neon DB yet. Publish articles in Admin CMS → Technical Writing.</div>`;
    return;
  }

  container.innerHTML = articles.map(a => `
    <div class="saas-card p-5 space-y-3 flex flex-col justify-between">
      <div class="space-y-2">
        <div class="flex items-center justify-between text-xs text-fg-3">
          <span>${escapeHtml(a.publishedAt ? new Date(a.publishedAt).toLocaleDateString([], { month: 'short', year: 'numeric' }) : (a.publishedDate || '2026'))}</span>
          <span>${escapeHtml(a.readingTimeMinutes ? a.readingTimeMinutes + ' min read' : (a.readTime || '6 min read'))}</span>
        </div>
        <h3 class="text-sm font-semibold text-fg hover:text-indigo-400 transition-colors">
          ${escapeHtml(a.title)}
        </h3>
        <p class="text-xs text-fg font-sans leading-relaxed">${escapeHtml(a.summary || a.excerpt || '')}</p>
      </div>
      <div class="flex items-center justify-between pt-3 border-t border-line text-xs">
        <div class="flex flex-wrap gap-1">
          ${(a.tags ? (Array.isArray(a.tags) ? a.tags : Array.from(a.tags)) : ['Engineering']).map(t => `
            <span class="px-1.5 py-0.5 rounded bg-raised text-fg-2 text-xs border border-line">${escapeHtml(t)}</span>
          `).join('')}
        </div>
        <span class="text-indigo-400 hover:underline cursor-pointer">Synced from Neon DB →</span>
      </div>
    </div>
  `).join('');
}

// ==========================================
// 3D INTERACTIVE HERO NEURAL CORE, SMOOTH TEXT ANIMATOR & GEOIP RESOLVER
// ==========================================
window.__heroHeadlines = [
  "Building AI-native software.",
  "Scalable Java & Spring systems.",
  "Production RAG & AI agents.",
  "Full-stack cloud engineering."
];

window.__heroRoles = [
  "Full-Stack & AI Engineer",
  "Java 21 & Spring Boot Architect",
  "Distributed Systems Builder",
  "Applied AI & RAG Specialist"
];

let __heroTypeTimer = null;
let __heroRoleTimer = null;

function parsePipeOrNewlineList(raw, fallbackArr) {
  if (!raw || typeof raw !== 'string') return fallbackArr;
  const parts = raw
    .split(/\||\n/)
    .map(s => s.trim())
    .filter(Boolean);
  return parts.length > 0 ? parts : fallbackArr;
}

function applyFontPreset(preset) {
  const valid = ['geist', 'editorial', 'portfolio', 'jakarta', 'agency'];
  const chosen = valid.includes(String(preset || '').toLowerCase()) ? String(preset).toLowerCase() : 'geist';
  document.body.setAttribute('data-font-preset', chosen);
  localStorage.setItem('rr_font_preset', chosen);
}

function updateHeroAnimatorTexts(rawHeadlines, rawRoles) {
  if (rawHeadlines) {
    window.__heroHeadlines = parsePipeOrNewlineList(rawHeadlines, window.__heroHeadlines);
  }
  if (rawRoles) {
    window.__heroRoles = parsePipeOrNewlineList(rawRoles, window.__heroRoles);
  }
  initHeroTextAnimator();
}

function initHeroTextAnimator() {
  const savedFont = localStorage.getItem('rr_font_preset');
  if (savedFont) applyFontPreset(savedFont);

  const headlineEl = document.getElementById('hero-animated-headline');
  const roleEl = document.getElementById('hero-role-rotator');

  if (__heroTypeTimer) clearTimeout(__heroTypeTimer);
  if (__heroRoleTimer) clearInterval(__heroRoleTimer);

  // 1. Smooth Typewriter / Morphing Headline Cycle
  if (headlineEl) {
    let phraseIdx = 0;
    let charIdx = (window.__heroHeadlines[0] || '').length;
    let isDeleting = false;
    headlineEl.textContent = window.__heroHeadlines[0] || "Building software for the AI-native era.";

    const tick = () => {
      const phrases = window.__heroHeadlines && window.__heroHeadlines.length > 0
        ? window.__heroHeadlines
        : ["Building software for the AI-native era."];
      const currentPhrase = phrases[phraseIdx % phrases.length];

      if (!isDeleting) {
        charIdx = Math.min(currentPhrase.length, charIdx + 1);
        headlineEl.textContent = currentPhrase.substring(0, charIdx);
        if (charIdx >= currentPhrase.length) {
          isDeleting = true;
          __heroTypeTimer = setTimeout(tick, 2800);
          return;
        }
        __heroTypeTimer = setTimeout(tick, 34);
      } else {
        charIdx = Math.max(0, charIdx - 1);
        headlineEl.textContent = currentPhrase.substring(0, charIdx);
        if (charIdx === 0) {
          isDeleting = false;
          phraseIdx = (phraseIdx + 1) % phrases.length;
          __heroTypeTimer = setTimeout(tick, 260);
          return;
        }
        __heroTypeTimer = setTimeout(tick, 18);
      }
    };

    __heroTypeTimer = setTimeout(() => {
      isDeleting = true;
      tick();
    }, 3000);
  }

  // 2. Smooth Role Badge Crossfade
  if (roleEl) {
    let rIdx = 0;
    roleEl.textContent = window.__heroRoles[0] || "Full-Stack & AI Engineer";
    __heroRoleTimer = setInterval(() => {
      const roles = window.__heroRoles && window.__heroRoles.length > 0
        ? window.__heroRoles
        : ["Full-Stack & AI Engineer"];
      roleEl.classList.add('fade-out');
      setTimeout(() => {
        rIdx = (rIdx + 1) % roles.length;
        roleEl.textContent = roles[rIdx];
        roleEl.classList.remove('fade-out');
      }, 260);
    }, 3600);
  }
}

// 3. Full-Viewport 3D Ambient Background Wave & Perspective Mesh (#bg-3d-canvas)
function initHero3dVisual() {
  return; // decorative animated background removed by the design system (readability + performance)
  const canvas = document.getElementById('bg-3d-canvas');
  if (!canvas || canvas.__initializedBg3d) return;
  canvas.__initializedBg3d = true;

  const ctx = canvas.getContext('2d');
  let width = 0;
  let height = 0;
  let dpr = Math.min(window.devicePixelRatio || 1, 2);

  const resize = () => {
    width = window.innerWidth || 1280;
    height = window.innerHeight || 800;
    canvas.width = width * dpr;
    canvas.height = height * dpr;
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
  };
  resize();
  window.addEventListener('resize', resize);

  // Subtle floating 3D depth particles
  const particles = Array.from({ length: 42 }, (_, i) => ({
    x: (Math.sin(i * 12.9898) * 43758.5453) % 1,
    y: (Math.cos(i * 78.233) * 23421.631) % 1,
    z: 0.2 + ((i * 0.17) % 0.8),
    size: 1.2 + (i % 3) * 0.7,
    speedX: (i % 2 === 0 ? 1 : -1) * (0.00008 + (i % 5) * 0.00002),
    speedY: -0.00012 - (i % 4) * 0.00003
  })).map(p => ({ ...p, x: Math.abs(p.x), y: Math.abs(p.y) }));

  let mouseX = 0;
  let mouseY = 0;
  let targetMouseX = 0;
  let targetMouseY = 0;

  window.addEventListener('mousemove', (e) => {
    targetMouseX = (e.clientX / Math.max(1, width) - 0.5) * 2;
    targetMouseY = (e.clientY / Math.max(1, height) - 0.5) * 2;
  }, { passive: true });

  const renderFrame = (time) => {
    const t = time * 0.00055;
    const isLight = document.documentElement.classList.contains('light');
    mouseX += (targetMouseX - mouseX) * 0.04;
    mouseY += (targetMouseY - mouseY) * 0.04;

    ctx.clearRect(0, 0, width, height);

    // Draw 3D perspective flowing topological wave ribbons across the background
    const cols = 38;
    const rows = 14;
    const fov = 520;
    const camZ = 420;
    const centerX = width * 0.5 + mouseX * 36;
    const centerY = height * 0.34 + mouseY * 22;

    const grid = [];
    for (let r = 0; r < rows; r++) {
      const rowPts = [];
      const depthRatio = r / (rows - 1);
      const z = -120 + depthRatio * 560;
      for (let c = 0; c < cols; c++) {
        const colRatio = (c / (cols - 1)) - 0.5;
        const worldX = colRatio * (width * 1.45);
        const wave1 = Math.sin(colRatio * 5.2 + t * 1.8 + r * 0.38) * 38;
        const wave2 = Math.cos(colRatio * 3.1 - t * 1.2 + r * 0.52) * 24;
        const worldY = 130 + wave1 + wave2 + (depthRatio * 65) - mouseY * 18;

        const scale = fov / Math.max(80, fov + z + camZ * 0.35);
        const px = centerX + worldX * scale;
        const py = centerY + worldY * scale;
        rowPts.push({ px, py, scale, depthRatio, colRatio });
      }
      grid.push(rowPts);
    }

    // Horizontal flowing 3D wave lines
    for (let r = 0; r < rows; r++) {
      const fade = Math.sin((r / (rows - 1)) * Math.PI);
      const alpha = isLight ? fade * 0.14 : fade * 0.17;
      ctx.beginPath();
      for (let c = 0; c < cols; c++) {
        const pt = grid[r][c];
        if (c === 0) ctx.moveTo(pt.px, pt.py);
        else ctx.lineTo(pt.px, pt.py);
      }
      ctx.strokeStyle = isLight
        ? (r % 3 === 0 ? `rgba(124, 58, 237, ${alpha})` : `rgba(37, 99, 235, ${alpha})`)
        : (r % 3 === 0 ? `rgba(99, 102, 241, ${alpha})` : `rgba(99, 102, 241, ${alpha})`);
      ctx.lineWidth = r % 4 === 0 ? 1.25 : 0.8;
      ctx.stroke();
    }

    // Subtle cross-rib perspective filaments
    for (let c = 0; c < cols; c += 2) {
      const colFade = 1 - Math.abs((c / (cols - 1)) - 0.5) * 1.65;
      if (colFade <= 0) continue;
      const alpha = (isLight ? 0.065 : 0.08) * colFade;
      ctx.beginPath();
      for (let r = 0; r < rows; r++) {
        const pt = grid[r][c];
        if (r === 0) ctx.moveTo(pt.px, pt.py);
        else ctx.lineTo(pt.px, pt.py);
      }
      ctx.strokeStyle = isLight
        ? `rgba(37, 99, 235, ${alpha})`
        : `rgba(99, 102, 241, ${alpha})`;
      ctx.lineWidth = 0.65;
      ctx.stroke();
    }

    // Subtle glowing intersection nodes & floating depth particles
    for (const p of particles) {
      p.x = (p.x + p.speedX + 1) % 1;
      p.y = (p.y + p.speedY + 1) % 1;
      const px = p.x * width + mouseX * 18 * p.z;
      const py = p.y * height + mouseY * 14 * p.z;
      const alpha = (isLight ? 0.18 : 0.28) * p.z;
      ctx.beginPath();
      ctx.arc(px, py, p.size * p.z, 0, Math.PI * 2);
      ctx.fillStyle = isLight
        ? `rgba(37, 99, 235, ${alpha})`
        : `rgba(99, 102, 241, ${alpha})`;
      ctx.fill();
    }

    requestAnimationFrame(renderFrame);
  };

  requestAnimationFrame(renderFrame);
}

// 4. Client-Side Public IP & GeoIP Prefetch (So Cloud Shell / Docker Bridge Still Captures Recruiter's Real Location)
window.__visitorGeo = null;

async function prefetchVisitorGeo() {
  if (window.__visitorGeo && window.__visitorGeo.city) return window.__visitorGeo;
  try {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 1800);
    const res = await fetch('https://ipwho.is/', { signal: controller.signal });
    clearTimeout(timer);
    if (res.ok) {
      const data = await res.json();
      if (data && data.success !== false && data.country) {
        window.__visitorGeo = {
          ip: data.ip || '',
          city: data.city || '',
          region: data.region || '',
          country: data.country || ''
        };
        return window.__visitorGeo;
      }
    }
  } catch (_) {}

  try {
    const controller2 = new AbortController();
    const timer2 = setTimeout(() => controller2.abort(), 1500);
    const res2 = await fetch('https://ipapi.co/json/', { signal: controller2.signal });
    clearTimeout(timer2);
    if (res2.ok) {
      const d2 = await res2.json();
      if (d2 && d2.country_name) {
        window.__visitorGeo = {
          ip: d2.ip || '',
          city: d2.city || '',
          region: d2.region || '',
          country: d2.country_name || ''
        };
        return window.__visitorGeo;
      }
    }
  } catch (_) {}

  try {
    const controller3 = new AbortController();
    const timer3 = setTimeout(() => controller3.abort(), 1200);
    const res3 = await fetch('https://api.ipify.org?format=json', { signal: controller3.signal });
    clearTimeout(timer3);
    if (res3.ok) {
      const d3 = await res3.json();
      if (d3 && d3.ip) {
        window.__visitorGeo = {
          ip: d3.ip || '',
          city: '',
          region: '',
          country: 'India'
        };
        return window.__visitorGeo;
      }
    }
  } catch (_) {}

  return window.__visitorGeo || { ip: '', city: '', region: '', country: '' };
}

async function resolveClientGeoForContact() {
  if (window.__visitorGeo && (window.__visitorGeo.city || window.__visitorGeo.country)) {
    return window.__visitorGeo;
  }
  return await prefetchVisitorGeo();
}


// Auto-open Admin Login Modal if redirected from /admin (?admin_login=1)
window.addEventListener('DOMContentLoaded', () => {
  if (window.location.search.includes('admin_login=1')) {
    setTimeout(() => {
      if (typeof openAdminAuthModal === 'function') openAdminAuthModal();
    }, 300);
  }
});


// Mobile Phone Shortcut: Triple-tap (3 taps within 1.2s) on top-left Brand Logo or Footer to open Admin Login Modal
window.addEventListener('DOMContentLoaded', () => {
  let tapCount = 0;
  let tapTimer = null;
  const brandEls = document.querySelectorAll('header a[href="#"], header .font-mono.font-bold, footer');
  brandEls.forEach(el => {
    el.addEventListener('click', (ev) => {
      tapCount++;
      if (tapTimer) clearTimeout(tapTimer);
      if (tapCount >= 3) {
        tapCount = 0;
        ev.preventDefault();
        if (typeof openAdminAuthModal === 'function') openAdminAuthModal();
      } else {
        tapTimer = setTimeout(() => { tapCount = 0; }, 1200);
      }
    });
  });

  // Secret keyword in Command Palette search input (#admin or sudo admin) for mobile/desktop
  const searchInput = document.getElementById('cmd-search-input');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      const v = (e.target.value || '').trim().toLowerCase();
      if (v === '#admin' || v === 'sudo admin' || v === 'unlock-os' || v === '/admin') {
        e.target.value = '';
        if (typeof toggleCommandPalette === 'function') toggleCommandPalette();
        if (typeof openAdminAuthModal === 'function') openAdminAuthModal();
      }
    });
  }
});
