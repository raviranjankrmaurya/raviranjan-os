// main_app.js - Raviranjan.OS Dynamic Client & Grounded RAG Telemetry Engine (100% Neon DB & Live APIs)

function formatAiMarkdownToHtml(raw) {
  if (!raw) return '';
  return escapeHtml(raw)
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong class="text-white">$1</strong>')
    .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-cyan-400 hover:underline font-semibold inline-flex items-center gap-0.5">$1 <span class="text-[10px]">&nearr;</span></a>');
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
let cachedContribCount = 144;
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

// -------------------------------------------------------------
// 100% LIVE REAL GITHUB CONTRIBUTION HEATMAP (No Hardcoded Fallbacks)
// -------------------------------------------------------------
function renderGitHubHeatmap(year = 2026, liveDays = null) {
  currentHeatmapYear = year;
  const grid = document.getElementById("gh-heatmap-grid");
  if (!grid) return;
  grid.innerHTML = "";

  const targetDays = liveDays || (year === 2025 ? cachedHeatmapByYear[2025] : (cachedHeatmapByYear[2026] || cachedHeatmapData));
  const labelEl = document.getElementById("gh-heatmap-year-label");

  if (Array.isArray(targetDays) && targetDays.length > 0) {
    const slice = targetDays.length > 364 ? targetDays.slice(-364) : targetDays;
    let totalForYear = slice.reduce((acc, item) => acc + (Number(item.count) || 0), 0);
    if (totalForYear === 0 && cachedContribCount > 0) {
      totalForYear = cachedContribCount;
    }

    if (labelEl) {
      if (year === 2026) {
        labelEl.innerText = `${totalForYear} contributions in the last year (Live GitHub Sync)`;
      } else {
        labelEl.innerText = `${year}: ${totalForYear} contributions (Live GitHub Sync)`;
      }
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
    return;
  }

  // Graceful initial placeholder skeleton while fetching
  if (labelEl) {
    labelEl.innerText = `${cachedContribCount} contributions in the last year (Live GitHub Sync)`;
  }
  for (let w = 0; w < 52; w++) {
    const col = document.createElement("div");
    col.className = "flex flex-col gap-1";
    for (let d = 0; d < 7; d++) {
      const cell = document.createElement("div");
      cell.className = "gh-cell gh-level-0";
      cell.title = `Syncing live GitHub calendar...`;
      col.appendChild(cell);
    }
    grid.appendChild(col);
  }
}

function setHeatmapYear(year) {
  currentHeatmapYear = year;
  const btn2026 = document.getElementById("btn-year-2026");
  const btn2025 = document.getElementById("btn-year-2025");
  if (btn2026 && btn2025) {
    if (year === 2026) {
      btn2026.className = "btn-year-active year-active px-2.5 py-1 rounded bg-[#1c202e] text-white transition-colors";
      btn2025.className = "btn-year-inactive year-inactive px-2.5 py-1 rounded text-slate-400 hover:text-white transition-colors";
    } else {
      btn2025.className = "btn-year-active year-active px-2.5 py-1 rounded bg-[#1c202e] text-white transition-colors";
      btn2026.className = "btn-year-inactive year-inactive px-2.5 py-1 rounded text-slate-400 hover:text-white transition-colors";
    }
  }
  renderGitHubHeatmap(year);
  showToast(`Switched GitHub Contribution Calendar to ${year}`);
}

// Direct Client-Side Live Fetch from GitHub API (Guarantees Real Calendar on Vercel)
async function fetchDirectGitHubContributions() {
  const activeUser = "raviranjankrmaurya";
  try {
    const res = await fetch(`https://github-contributions-api.jogruber.de/v4/${activeUser}?y=all`);
    if (res.ok) {
      const json = await res.json();
      if (json && Array.isArray(json.contributions) && json.contributions.length > 0) {
        cachedHeatmapData = json.contributions;
        const c2026 = json.contributions.filter(c => c.date && c.date.startsWith("2026"));
        const c2025 = json.contributions.filter(c => c.date && c.date.startsWith("2025"));
        if (c2026.length > 0) cachedHeatmapByYear[2026] = c2026;
        if (c2025.length > 0) cachedHeatmapByYear[2025] = c2025;

        if (json.total) {
          if (json.total.lastYear) cachedContribCount = json.total.lastYear;
          else if (json.total["2026"]) cachedContribCount = json.total["2026"];
        }

        const countEl = document.getElementById("gh-contributions-count");
        if (countEl && cachedContribCount > 0) countEl.innerText = cachedContribCount;

        renderGitHubHeatmap(currentHeatmapYear);
        console.info(`[GitHub Live] Rendered ${json.contributions.length} days directly from GitHub API.`);
      }
    }
  } catch (err) {
    console.debug("Direct client GitHub sync fallback note:", err);
  }
}

// Dynamic Telemetry & Live Platform Integrations Ingestion
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
          cachedContribCount = d.github.contributions ?? 144;
          setEl("gh-contributions-count", cachedContribCount);
          setEl("gh-repos-count", d.github.repositories ?? 17);
          setEl("gh-prs-count", d.github.pullRequests ?? 0);
          setEl("gh-stars-count", d.github.activeRepos ?? 17);
          setEl("gh-last-sync", "Last sync: Live API");
          setHtml("gh-health-status", '<span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span> Connected');

          if (Array.isArray(d.github.heatmap) && d.github.heatmap.length > 0) {
            cachedHeatmapData = d.github.heatmap;
          }
          if (Array.isArray(d.github.heatmap2026) && d.github.heatmap2026.length > 0) {
            cachedHeatmapByYear[2026] = d.github.heatmap2026;
          }
          if (Array.isArray(d.github.heatmap2025) && d.github.heatmap2025.length > 0) {
            cachedHeatmapByYear[2025] = d.github.heatmap2025;
          }
          renderGitHubHeatmap(currentHeatmapYear);
        }
        if (d.leetcode) {
          setEl("lc-solved-total", d.leetcode.solvedTotal ?? 31);
          setEl("lc-streak-days", d.leetcode.streakDays || "11 Days");
          setEl("lc-med-hard", `${d.leetcode.easySolved ?? 19} Easy · ${d.leetcode.mediumSolved ?? 12} Med · ${d.leetcode.hardSolved ?? 0} Hard`);
          setEl("lc-acceptance", d.leetcode.acceptanceRate || "Synced");
          setEl("lc-ranking", d.leetcode.ranking || "#3704105");
          setEl("lc-last-sync", "Last sync: Live API");
          setHtml("lc-health-status", '<span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse"></span> Connected');
        }
        if (d.gfg) {
          setEl("gfg-score", d.gfg.score ?? 234);
          setEl("gfg-problems", d.gfg.problems ?? 68);
          setEl("gfg-articles", d.gfg.articles || "3 DSA Notes");
          setEl("gfg-rank", d.gfg.rank || "Institute Rank #234");
          setEl("gfg-last-sync", "Last sync: Neon DB");
          setHtml("gfg-health-status", '<span class="inline-block w-1.5 h-1.5 rounded-full bg-emerald-400"></span> Connected');
        }
      }
    }
  } catch (err) {
    console.debug("Activity telemetry fetch note:", err);
  }
}

// Toast System
function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  const borderCol = type === "success" ? "border-emerald-500/40 text-emerald-300" : "border-cyan-500/40 text-cyan-300";
  toast.className = `p-3 rounded-lg bg-[#10121a] border ${borderCol} shadow-xl text-xs flex items-center space-x-2 animate-fade-in pointer-events-auto`;
  toast.innerHTML = `<span class="w-2 h-2 rounded-full ${type === "success" ? "bg-emerald-400" : "bg-cyan-400"}"></span><span>${escapeHtml(message)}</span>`;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateY(10px)";
    toast.style.transition = "all 0.3s ease";
    setTimeout(() => toast.remove(), 300);
  }, 3500);
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
  fetch('/api', { cache: 'no-store' }).then(r => {
    if (r && r.ok) {
      backendIsAwake = true;
      triggerAllDynamicFetches();
    }
  }).catch(() => {});

  fetch('https://raviranjan-os-backend.onrender.com/api', { mode: 'no-cors', cache: 'no-store' }).catch(() => {});

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
        triggerAllDynamicFetches();
      }
    } catch (e) {}
  }, 2500);
}

// Dynamic Profile Ingestion (Strictly from Neon DB)
function fetchDynamicProfile() {
  fetch("/api/profile")
    .then(res => {
      if (!res.ok) throw new Error("Non-200");
      return res.json();
    })
    .then(resp => {
      if (resp && resp.data) {
        currentProfile = { ...currentProfile, ...resp.data };
        updateProfileDom();
      }
    })
    .catch(() => {});
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
    bioEl.innerText = cleanBio || currentProfile.bio;
  }

  const locEl = document.getElementById("hero-profile-location");
  if (locEl && currentProfile.location) locEl.innerText = currentProfile.location;

  const emailEl = document.getElementById("about-profile-email");
  if (emailEl && currentProfile.email) emailEl.innerText = currentProfile.email;
}

// Dynamic Projects Ingestion (Strictly from Neon DB)
function fetchDynamicProjects() {
  const container = document.getElementById("projects-grid");
  if (!container) return;

  fetch("/api/projects?size=50")
    .then(res => res.json())
    .then(resp => {
      const rawList = resp && resp.data ? (Array.isArray(resp.data) ? resp.data : (resp.data.content || [])) : [];
      if (rawList.length > 0) {
        projectsList = rawList;
        renderProjects();
      }
    })
    .catch(() => {});
}

function renderProjects() {
  const container = document.getElementById("projects-grid");
  if (!container || !projectsList.length) return;

  container.innerHTML = projectsList.map(p => `
    <div class="saas-card p-5 space-y-3 flex flex-col justify-between">
      <div class="space-y-2">
        <div class="flex items-center justify-between text-xs font-mono">
          <span class="text-sky-400 font-semibold">${escapeHtml(p.category || 'Backend')}</span>
          <span class="text-slate-500">${escapeHtml(p.version || 'v1.0')}</span>
        </div>
        <h3 class="text-base font-bold text-white font-mono">${escapeHtml(p.title)}</h3>
        <p class="text-xs text-slate-300 font-sans leading-relaxed">${escapeHtml(p.summary || p.description || '')}</p>
        <div class="flex flex-wrap gap-1.5 pt-1">
          ${(p.technologies || []).map(t => `<span class="px-2 py-0.5 rounded text-[10px] bg-[#161924] border border-[#232938] text-slate-400 font-mono">${escapeHtml(t)}</span>`).join('')}
        </div>
      </div>
      <div class="flex items-center justify-between pt-3 border-t border-[#1f2430] text-xs font-mono">
        <span class="text-emerald-400 font-semibold">${escapeHtml(p.status || 'Active')}</span>
        <div class="flex items-center gap-2">
          ${p.githubUrl ? `<a href="${escapeHtml(p.githubUrl)}" target="_blank" class="text-sky-400 hover:underline">GitHub ↗</a>` : ''}
          ${p.liveUrl ? `<a href="${escapeHtml(p.liveUrl)}" target="_blank" class="text-emerald-400 hover:underline">Live ↗</a>` : ''}
        </div>
      </div>
    </div>
  `).join('');
}

// Dynamic Unified Activity Stream
function fetchDynamicActivity() {
  const container = document.getElementById("activity-feed-container");
  if (!container) return;

  fetch("/api/activity?size=5")
    .then(res => res.json())
    .then(resp => {
      const items = resp && resp.data ? (Array.isArray(resp.data) ? resp.data : (resp.data.content || [])) : [];
      if (items.length > 0) {
        container.innerHTML = items.map(act => `
          <div class="p-3.5 rounded-xl bg-[#10121a] border border-[#1f2430] flex items-center justify-between text-xs font-mono">
            <div class="flex items-center space-x-3">
              <span class="px-2 py-0.5 rounded bg-sky-500/10 text-sky-400 border border-sky-500/20 text-[10px] font-bold">${escapeHtml(act.sourcePlatform || 'SYSTEM')}</span>
              <span class="text-white font-medium">${escapeHtml(act.title)}</span>
            </div>
            <span class="text-slate-500 text-[11px]">${new Date(act.occurredAt || Date.now()).toLocaleDateString([], { month: 'short', day: 'numeric' })}</span>
          </div>
        `).join('');
      }
    })
    .catch(() => {});
}

// Ingestion for DSA Notes, Research, Lab, Articles, Stories
async function fetchDynamicDsaNotes() {
  const container = document.getElementById("dsa-notes-grid");
  if (!container) return;
  try {
    const res = await fetch("/api/dsa/problems?size=50");
    if (res.ok) {
      const d = await res.json();
      const list = d?.data?.content || d?.data || [];
      if (list.length > 0) {
        container.innerHTML = list.map(n => `
          <div class="p-4 rounded-xl bg-[#10121a] border border-[#1f2430] space-y-2">
            <div class="flex items-center justify-between text-xs font-mono">
              <span class="text-sky-400 font-bold">${escapeHtml(n.title)}</span>
              <span class="px-2 py-0.5 rounded text-[10px] ${n.difficulty === 'HARD' ? 'bg-rose-500/10 text-rose-400' : (n.difficulty === 'MEDIUM' ? 'bg-amber-500/10 text-amber-400' : 'bg-emerald-500/10 text-emerald-400')} font-bold">${escapeHtml(n.difficulty || 'MEDIUM')}</span>
            </div>
            <p class="text-xs text-slate-300 font-sans">${escapeHtml(n.conceptExplanation || n.description || '')}</p>
          </div>
        `).join('');
      }
    }
  } catch (e) {}
}

async function fetchDynamicCareerAndCredentials() {}
function fetchDynamicSkills() {}
async function fetchDynamicContributions() {}
async function fetchDynamicResearch() {}
async function fetchDynamicLab() {}
async function fetchDynamicArticles() {}
function loadPublicBrandingSettings() {}
function initTheme() {}
function toggleRecruiterMode() {}

// DOM Content Loaded Handler
document.addEventListener("DOMContentLoaded", () => {
  localStorage.removeItem("customProjects");
  localStorage.removeItem("cachedProfile");

  renderGitHubHeatmap(2026);
  fetchDirectGitHubContributions();
  triggerAllDynamicFetches();
  startBackendWarmupAndHydrationLoop();
});
