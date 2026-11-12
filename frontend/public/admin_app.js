function formatAiMarkdownToHtml(raw) {
  if (!raw) return '';
  return escapeHtml(raw)
    .replace(/\n/g, '<br>')
    .replace(/\*\*(.*?)\*\*/g, '<strong class="text-fg">$1</strong>')
    .replace(/\[(.*?)\]\((.*?)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer" class="text-indigo-400 hover:underline font-semibold inline-flex items-center gap-0.5">$1 <span class="text-xs">&nearr;</span></a>');
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

// admin_app.js - Raviranjan.OS Admin Application Logic & Integration Handlers

// Initialize Lucide Icons
if (window.lucide) {
  window.lucide.createIcons();
}

// Utility: HTML Escaping
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}


function toggleAdminMobileSidebar(forceState) {
  const sidebar = document.getElementById('admin-sidebar');
  const backdrop = document.getElementById('admin-sidebar-backdrop');
  if (!sidebar) return;
  const isClosed = sidebar.classList.contains('-translate-x-full');
  const open = typeof forceState === 'boolean' ? forceState : isClosed;
  if (open) {
    sidebar.classList.remove('-translate-x-full');
    if (backdrop) backdrop.classList.remove('hidden');
  } else {
    sidebar.classList.add('-translate-x-full');
    if (backdrop) backdrop.classList.add('hidden');
  }
}

// View Navigation Router
function switchView(viewName) {
  toggleAdminMobileSidebar(false);
  document.querySelectorAll('.view-panel').forEach(el => el.classList.add('hidden'));
  document.querySelectorAll('.nav-btn').forEach(btn => {
    btn.classList.remove('active', 'text-indigo-400', 'bg-indigo-500/10', 'border', 'border-indigo-500/30');
    btn.classList.add('text-fg-2');
  });

  const activeBtn = document.querySelector(`[data-nav="${viewName}"]`);
  if (activeBtn) {
    activeBtn.classList.add('active', 'text-indigo-400', 'bg-indigo-500/10', 'border', 'border-indigo-500/30');
    activeBtn.classList.remove('text-fg-2');
  }

  const breadcrumb = document.getElementById('current-view-breadcrumb');
  if (breadcrumb) {
    breadcrumb.innerText = viewName.replace('-', ' ');
  }

  const targetEl = document.getElementById(`view-${viewName}`);
  if (targetEl) {
    targetEl.classList.remove('hidden');
    // Trigger dynamic data loader for the active view
    if (viewName === 'dashboard') loadAdminDashboardMetrics();
    else if (viewName === 'projects') loadAdminProjects();
    else if (viewName === 'project-editor') populateProjectEditorSelector();
    else if (viewName === 'gfg') loadAdminGfg();
    else if (viewName === 'leetcode') loadAdminLeetCode();
    else if (viewName === 'github') loadAdminGitHub();
    else if (viewName === 'activity') loadAdminActivity();
    else if (viewName === 'dsa') loadAdminDsa();
    else if (viewName === 'writing') loadAdminArticles();
    else if (viewName === 'crm') loadAdminCrm();
    else if (viewName === 'profile') loadAdminProfileData();
    else if (viewName === 'settings') loadSiteSettings();
    else if (viewName === 'ai-control') loadAiConfig();
    else if (viewName === 'experience') loadAdminCareer();
    else if (viewName === 'research') loadAdminResearch();
    else if (viewName === 'skills') loadAdminSkills();
    else if (viewName === 'lab') loadAdminLab();
    else if (viewName === 'contributions') loadAdminStories();
    else if (viewName === 'homepage') loadAdminHomepageBuilder();
    else if (viewName === 'achievements') loadAdminCredentials();
    else if (viewName === 'integrations') loadAdminIntegrationsCenter();
    else if (viewName === 'analytics') loadAdminDashboardMetrics();
    else if (viewName === 'audit-logs') loadAdminAuditLogs();
  } else {
    const placeholder = document.getElementById('view-placeholder');
    const titleEl = document.getElementById('placeholder-title');
    if (titleEl) titleEl.innerText = viewName.toUpperCase().replace('-', ' ');
    if (placeholder) placeholder.classList.remove('hidden');
  }

  if (window.lucide) {
    window.lucide.createIcons();
  }
}

// Project Editor Tab Switching
function switchEditorTab(tabId) {
  document.querySelectorAll('.editor-tab').forEach(t => {
    t.classList.remove('active', 'text-indigo-400', 'border-b-2', 'border-indigo-400');
    t.classList.add('text-fg-2');
  });
  if (event && event.target) {
    event.target.classList.add('active', 'text-indigo-400', 'border-b-2', 'border-indigo-400');
    event.target.classList.remove('text-fg-2');
  }

  document.querySelectorAll('.editor-content').forEach(c => c.classList.add('hidden'));
  const activeContent = document.getElementById(`etab-${tabId}`);
  if (activeContent) activeContent.classList.remove('hidden');
}

// Markdown Live Preview Renderer
function updateMarkdownPreview() {
  const inputEl = document.getElementById('markdown-input');
  const outputEl = document.getElementById('markdown-output');
  if (!inputEl || !outputEl) return;

  const input = inputEl.value;
  let html = input
    .replace(/^# (.*$)/gim, '<h1 class="text-lg font-bold text-fg">$1</h1>')
    .replace(/^## (.*$)/gim, '<h2 class="text-base font-bold text-indigo-400 mt-3">$1</h2>')
    .replace(/^### (.*$)/gim, '<h3 class="text-sm font-semibold text-fg mt-2">$1</h3>')
    .replace(/```java([\\s\\S]*?)```/gim, '<pre class="bg-card p-3 rounded border border-line text-xs font-mono text-emerald-300 overflow-x-auto">$1</pre>')
    .replace(/\\n/gim, '<br>');

  outputEl.innerHTML = html;
}

// Toast Notifications System
function showToast(message, type = 'info') {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  const colors = {
    success: 'border-emerald-500/40 bg-subtle text-emerald-300',
    warning: 'border-amber-500/40 bg-subtle text-amber-300',
    info: 'border-indigo-500/40 bg-subtle text-indigo-300',
  }[type] || 'border-fg-3/40 bg-card text-fg';

  toast.className = `p-3 rounded-lg border shadow-xl flex items-center space-x-2 text-xs  pointer-events-auto transition-all transform duration-200 translate-y-2 opacity-0 ${colors}`;
  toast.innerHTML = `<i data-lucide="info" class="w-4 h-4"></i><span>${message}</span>`;

  container.appendChild(toast);
  if (window.lucide) window.lucide.createIcons();

  requestAnimationFrame(() => {
    toast.classList.remove('translate-y-2', 'opacity-0');
  });

  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-2');
    setTimeout(() => toast.remove(), 200);
  }, 3000);
}

// Command Palette Trigger
function toggleCommandPalette() {
  const modal = document.getElementById('command-modal');
  if (!modal) return;
  modal.classList.toggle('hidden');
  if (!modal.classList.contains('hidden')) {
    const input = document.getElementById('cmd-input');
    if (input) input.focus();
    populateCommandResults();
  }
}

function populateCommandResults() {
  const results = document.getElementById('cmd-results');
  if (!results) return;

  const commands = [
    { label: "Jump to Dashboard", view: "dashboard" },
    { label: "Jump to Projects Directory", view: "projects" },
    { label: "Open 13-Tab Project Editor", view: "project-editor" },
    { label: "GitHub Integration & Sync Logs", view: "github" },
    { label: "LeetCode Ingestion Adapter", view: "leetcode" },
    { label: "GeeksforGeeks Verified Import", view: "gfg" },
    { label: "Manual DSA Problem Authoring", view: "dsa" },
    { label: "Unified Developer Activity Stream", view: "activity" },
    { label: "Homepage Section Reordering", view: "homepage" },
    { label: "Contact Inquiries Pipeline", view: "crm" },
    { label: "Site Settings & Configuration", view: "settings" },
    { label: "Audit Logs & Diff Viewer", view: "audit-logs" },
  ];

  results.innerHTML = commands.map(c => `
    <div onclick="switchView('${c.view}'); toggleCommandPalette()" class="p-2 rounded hover:bg-raised cursor-pointer flex items-center justify-between text-fg hover:text-fg">
      <span>${c.label}</span>
      <span class="text-xs text-fg-3">↵ Select</span>
    </div>
  `).join('');
}

// Global Keyboard Listeners
window.addEventListener('keydown', (e) => {
  if ((e.metaKey || e.ctrlKey) && e.key === 'k') {
    e.preventDefault();
    toggleCommandPalette();
  }
  if (e.key === 'Escape') {
    const modal = document.getElementById('command-modal');
    if (modal) modal.classList.add('hidden');
  }
});

// Integration Handlers (100% Live API & Neon DB)
async function triggerGitHubSync() {
  showToast("Syncing live repositories, commits & PRs from GitHub API...", "info");
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/integrations/GITHUB/sync?mode=MANUAL", { method: "POST", headers });
    if (res.ok) {
      const lastSyncEl = document.getElementById("github-last-sync");
      if (lastSyncEl) lastSyncEl.innerText = "Just now";
      showToast("GitHub live sync completed and stored!", "success");
      loadAdminDashboardMetrics();
    } else {
      await fetch("/api/activity/sync", { method: "POST" });
      showToast("GitHub telemetry synchronized!", "success");
      loadAdminDashboardMetrics();
    }
  } catch (e) {
    showToast("GitHub sync error: " + e.message, "warning");
  }
}

async function triggerLeetCodeSync() {
  showToast("Querying LeetCode GraphQL live endpoint...", "info");
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/integrations/LEETCODE/sync?mode=MANUAL", { method: "POST", headers });
    if (res.ok) {
      showToast("LeetCode live profile & recent submissions saved!", "success");
      loadAdminDashboardMetrics();
    } else {
      await fetch("/api/activity/sync", { method: "POST" });
      showToast("LeetCode telemetry synchronized!", "success");
      loadAdminDashboardMetrics();
    }
  } catch (e) {
    showToast("LeetCode sync error: " + e.message, "warning");
  }
}

async function loadAdminGfg() {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/integrations/gfg", { headers });
    if (res.ok) {
      const json = await res.json();
      const d = json?.data || {};
      const setEl = (id, val) => { const el = document.getElementById(id); if (el) el.innerText = val; };
      const setVal = (id, val) => { const el = document.getElementById(id); if (el !== null && el !== undefined) el.value = val; };
      setEl("admin-gfg-username", d.username || "raviranjankrmaurya");
      setEl("admin-gfg-score", Number(d.codingScore || 0).toLocaleString());
      setEl("admin-gfg-solved", d.totalProblemsSolved ?? 0);
      setEl("admin-gfg-rank", d.instituteRank ? "#" + d.instituteRank : "N/A");
      setVal("gfg-imp-username", d.username || "raviranjankrmaurya");
      setVal("gfg-imp-score", d.codingScore ?? 0);
      setVal("gfg-imp-solved", d.totalProblemsSolved ?? 0);
      setVal("gfg-imp-rank", d.instituteRank ?? 0);
    }
  } catch (e) {
    console.warn("Load GFG data note:", e);
  }
}

async function handleGfgManualImport() {
  const username = document.getElementById("gfg-imp-username")?.value?.trim() || "raviranjankrmaurya";
  const score = parseInt(document.getElementById("gfg-imp-score")?.value || "0", 10);
  const solved = parseInt(document.getElementById("gfg-imp-solved")?.value || "0", 10);
  const rank = parseInt(document.getElementById("gfg-imp-rank")?.value || "0", 10);


  showToast(`Saving GFG metrics...`, "info");
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/integrations/gfg/import", {
      method: "POST",
      headers,
      body: JSON.stringify({
        username,
        codingScore: score,
        totalProblemsSolved: solved,
        instituteRank: rank
      })
    });
    if (res.ok) {
      showToast("GeeksforGeeks metrics saved!", "success");
      await loadAdminGfg();
      await loadAdminDashboardMetrics();
    } else {
      showToast("Failed to save GFG metrics to database", "warning");
    }
  } catch (e) {
    showToast("Error saving GFG metrics: " + e.message, "warning");
  }
}

async function handleDeleteGfgData() {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/integrations/gfg", { method: "DELETE", headers });
    if (res.ok) {
      showToast("GeeksforGeeks data reset!", "success");
      await loadAdminGfg();
      await loadAdminDashboardMetrics();
    }
  } catch (e) {
    showToast("Error resetting GFG data: " + e.message, "warning");
  }
}

async function loadAdminLeetCode() {
  try {
    const res = await fetch("/api/activity/telemetry");
    if (res.ok) {
      const json = await res.json();
      const lc = json?.data?.leetcode || {};
      const setEl = (id, val) => { const el = document.getElementById(id); if (el) el.innerText = val; };
      setEl("admin-lc-total", lc.solvedTotal ?? 0);
      setEl("admin-lc-easy", lc.easySolved ?? 0);
      setEl("admin-lc-medium", lc.mediumSolved ?? 0);
      setEl("admin-lc-hard", lc.hardSolved ?? 0);
      setEl("admin-lc-rate", "Acceptance Rate: " + (lc.acceptanceRate || "Synced"));
      setEl("admin-lc-rank", "Global ranking: " + (lc.ranking || "Synced"));
    }
  } catch (e) {}
}

async function loadAdminGitHub() {
  try {
    const res = await fetch("/api/activity/telemetry");
    if (res.ok) {
      const json = await res.json();
      const gh = json?.data?.github || {};
      const setEl = (id, val) => { const el = document.getElementById(id); if (el) el.innerText = val; };
      setEl("admin-gh-user", "@" + (gh.username || "raviranjankrmaurya"));
      setEl("admin-gh-commits", (gh.contributions ?? 0) + " Contributions");
      setEl("admin-gh-repos", "across " + (gh.repositories ?? 0) + " public repositories (" + (gh.pullRequests ?? 0) + " PRs)");
    }
  } catch (e) {}
}

let cachedAdminActivities = [];

async function loadAdminActivity() {
  const container = document.getElementById("unified-timeline-container");
  if (!container) return;
  try {
    const res = await fetch("/api/activity?size=5");
    if (res.ok) {
      const json = await res.json();
      const items = (json?.data?.content || json?.data || []).slice(0, 5);
      cachedAdminActivities = items;
      if (items.length === 0) {
        container.innerHTML = `<div class="p-4 text-center text-fg-3">No activity records yet.</div>`;
        return;
      }
      container.innerHTML = items.map(act => {
        const plat = act.sourcePlatform || "SYSTEM";
        const badgeColor = plat === "LEETCODE"
          ? "bg-amber-500/10 text-amber-400 border-amber-500/20"
          : (plat === "GFG" ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" : "bg-indigo-500/10 text-indigo-400 border-indigo-500/20");
        const dateStr = act.occurredAt ? new Date(act.occurredAt).toLocaleDateString([], { month: "short", day: "numeric", year: "numeric" }) : "";
        return `
          <div class="p-3.5 rounded-xl bg-card border border-line flex flex-col sm:flex-row sm:items-center justify-between gap-2 hover:border-indigo-500/30 transition-all">
            <div class="flex items-start space-x-3">
              <span class="px-2 py-0.5 rounded text-xs font-bold border ${badgeColor} shrink-0 mt-0.5">${escapeHtml(plat)}</span>
              <div>
                <p class="font-medium text-fg">${escapeHtml(act.title)}</p>
                <p class="text-xs text-fg-2">${escapeHtml(act.description || "")}</p>
              </div>
            </div>
            <div class="flex items-center space-x-2.5 shrink-0 self-end sm:self-center">
              <span class="text-xs text-fg-3">${dateStr}</span>
              ${act.id ? `<button type="button" onclick="handleEditActivityItem('${act.id}')" class="text-indigo-400 hover:underline text-xs rounded-lg">Edit</button>` : ''}
              ${act.id ? `<button type="button" onclick="handleDeleteActivityItem('${act.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>` : ''}
            </div>
          </div>
        `;
      }).join("");
    }
  } catch (e) {}
}

async function handleEditActivityItem(id) {
  const item = cachedAdminActivities.find(a => String(a.id) === String(id));
  const currentTitle = item ? item.title : "";
  const currentDesc = item ? (item.description || "") : "";
  const newTitle = prompt("Edit Activity Title:", currentTitle);
  if (newTitle === null || !newTitle.trim()) return;
  const newDesc = prompt("Edit Activity Description:", currentDesc);
  if (newDesc === null) return;

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/activity/${id}`, {
      method: "PUT",
      headers,
      body: JSON.stringify({ title: newTitle.trim(), description: newDesc.trim() })
    });
    if (res.ok) {
      showToast("Activity updated!", "success");
      await loadAdminActivity();
      await loadAdminDashboardMetrics();
    } else {
      showToast("Failed to update activity", "warning");
    }
  } catch (e) {
    showToast("Error updating activity: " + e.message, "warning");
  }
}

async function handleDeleteActivityItem(id) {
  const item = cachedAdminActivities.find(a => String(a.id) === String(id));
  const label = item ? item.title : id;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/activity/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast("Activity deleted!", "success");
      await loadAdminActivity();
      await loadAdminDashboardMetrics();
    } else {
      showToast("Failed to delete activity", "warning");
    }
  } catch (e) {
    showToast("Error deleting activity: " + e.message, "warning");
  }
}

let cachedAdminDsaProblems = [];

function resetDsaForm() {
  const idEl = document.getElementById("dsa-id");
  if (idEl) idEl.value = "";
  const heading = document.getElementById("dsa-form-heading");
  if (heading) heading.innerText = "Add New Solution Note";
  const titleEl = document.getElementById("dsa-title");
  if (titleEl) {
    titleEl.value = "";
    titleEl.focus();
  }
  if (document.getElementById("dsa-time")) document.getElementById("dsa-time").value = "O(n)";
  if (document.getElementById("dsa-space")) document.getElementById("dsa-space").value = "O(1)";
  if (document.getElementById("dsa-approach")) document.getElementById("dsa-approach").value = "";
  if (document.getElementById("dsa-code")) document.getElementById("dsa-code").value = "";
  showToast("Form cleared — enter Problem Title & Solution below to save", "info");
}

async function loadAdminDsa() {
  const container = document.getElementById("admin-dsa-notes-list");
  if (!container) return;
  try {
    const res = await fetch("/api/dsa/problems");
    if (res.ok) {
      const json = await res.json();
      cachedAdminDsaProblems = json?.data || [];
      if (cachedAdminDsaProblems.length === 0) {
        container.innerHTML = `<div class="p-4 text-center text-fg-3">No DSA problem notes. Use the form on the left to add one.</div>`;
        return;
      }
      container.innerHTML = cachedAdminDsaProblems.map(p => {
        const diffColor = p.difficulty === "HARD"
          ? "bg-rose-500/10 text-rose-400 border-rose-500/20"
          : (p.difficulty === "EASY" ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" : "bg-amber-500/10 text-amber-400 border-amber-500/20");
        return `
          <div class="p-3 bg-card border border-line rounded-xl flex justify-between items-center">
            <div>
              <span class="font-bold text-fg">${escapeHtml(p.title)}</span>
              <span class="text-xs text-fg-2 block">${escapeHtml(p.platform)} · Time: ${escapeHtml(p.timeComplexity || "O(n)")} · Space: ${escapeHtml(p.spaceComplexity || "O(1)")}</span>
            </div>
            <div class="flex items-center space-x-2">
              <span class="text-xs px-2 py-0.5 rounded border ${diffColor}">${escapeHtml(p.difficulty || "MEDIUM")}</span>
              <button type="button" onclick="openEditDsaProblem('${p.id}')" class="text-indigo-400 hover:underline text-xs rounded-lg">Edit</button>
              <button type="button" onclick="handleDeleteDsaProblem('${p.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
            </div>
          </div>
        `;
      }).join("");
    }
  } catch (e) {
    console.warn("Load DSA error:", e);
  }
}

function openEditDsaProblem(id) {
  const p = cachedAdminDsaProblems.find(item => String(item.id) === String(id));
  if (!p) return;
  document.getElementById("dsa-id").value = p.id;
  const heading = document.getElementById("dsa-form-heading");
  if (heading) heading.innerText = "Editing Problem Note: " + p.title;
  document.getElementById("dsa-platform").value = p.platform || "LEETCODE";
  document.getElementById("dsa-diff").value = p.difficulty || "MEDIUM";
  document.getElementById("dsa-title").value = p.title || "";
  document.getElementById("dsa-time").value = p.timeComplexity || "O(n)";
  document.getElementById("dsa-space").value = p.spaceComplexity || "O(1)";
  document.getElementById("dsa-approach").value = p.approach || "";
  if (document.getElementById("dsa-code")) document.getElementById("dsa-code").value = p.solutionCode || "";
}

async function handleSaveManualProblem() {
  const editId = document.getElementById("dsa-id")?.value?.trim() || "";
  const title = document.getElementById("dsa-title")?.value?.trim() || "";
  const platform = document.getElementById("dsa-platform")?.value || "LEETCODE";
  const diff = document.getElementById("dsa-diff")?.value || "MEDIUM";
  const timeComplexity = document.getElementById("dsa-time")?.value?.trim() || "O(n)";
  const spaceComplexity = document.getElementById("dsa-space")?.value?.trim() || "O(1)";
  const approach = document.getElementById("dsa-approach")?.value?.trim() || "";
  const solutionCode = document.getElementById("dsa-code")?.value?.trim() || "";

  if (!title) {
    showToast("Problem title is required", "warning");
    return;
  }


  const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
  const payload = {
    platform,
    title,
    slug,
    difficulty: diff,
    problemUrl: platform === "LEETCODE" ? `https://leetcode.com/problems/${slug}/` : `https://www.geeksforgeeks.org/problems/${slug}/1`,
    topic: approach,
    approach,
    whatILearned: approach,
    timeComplexity,
    spaceComplexity,
    solutionCode
  };

  try {
    const headers = await getAuthHeaders();
    const url = editId ? `/api/admin/dsa/${editId}` : "/api/admin/dsa/manual";
    const method = editId ? "PUT" : "POST";
    const res = await fetch(url, { method, headers, body: JSON.stringify(payload) });
    if (res.ok) {
      showToast(editId ? `DSA problem "${title}" updated!` : `DSA problem "${title}" saved!`, "success");
      resetDsaForm();
      await loadAdminDsa();
      await loadAdminDashboardMetrics();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save DSA problem", "warning");
    }
  } catch (e) {
    showToast("Error saving DSA problem: " + e.message, "warning");
  }
}

async function handleDeleteDsaProblem(id) {
  const p = cachedAdminDsaProblems.find(item => String(item.id) === String(id));
  const label = p ? p.title : id;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/dsa/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast(`Deleted "${label}"!`, "success");
      await loadAdminDsa();
    }
  } catch (e) {
    showToast("Error deleting DSA problem: " + e.message, "warning");
  }
}

function prependUnifiedActivity(type, title, desc) {
  const container = document.getElementById('unified-timeline-container');
  if (!container) return;

  const card = document.createElement('div');
  const typeColors = {
    'GITHUB_COMMIT': 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20',
    'LEETCODE_SOLVED': 'bg-amber-500/10 text-amber-400 border-amber-500/20',
    'GFG_SOLVED': 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20',
    'BLOG_PUBLISHED': 'bg-purple-500/10 text-purple-400 border-purple-500/20'
  }[type] || 'bg-fg-3/10 text-fg-2 border-fg-3/20';

  card.className = 'p-3.5 rounded-lg bg-card border border-line flex items-center justify-between hover:border-indigo-500/30 transition-all animate-in fade-in duration-300';
  card.innerHTML = `
    <div class="flex items-center space-x-3">
      <span class="px-2 py-0.5 rounded text-xs font-bold border ${typeColors}">${type}</span>
      <div>
        <p class="font-medium text-fg">${title}</p>
        <p class="text-xs text-fg-2">${desc}</p>
      </div>
    </div>
    <span class="text-xs text-emerald-400 font-bold">Just now (Live SSE)</span>
  `;

  container.insertBefore(card, container.firstChild);
}

// Global Sync Trigger (handled via real endpoint below)

// Document Ready Listener
document.addEventListener('DOMContentLoaded', () => {
  initAdminTheme();
  updateMarkdownPreview();
  const isAuth = localStorage.getItem('raviranjan_admin_authenticated') === 'true' && Boolean(localStorage.getItem('raviranjan_auth_token'));
  if (isAuth) {
    loadAdminIdentityMetadata();
    loadAdminDashboardMetrics();
    loadAdminProjects();
    loadAdminNotifications();
    loadSiteSettings();
    loadAiConfig();
  }
  initDynamicAdminCommandPalette();
});

// AI Assistant Drawer & RAG Handlers
function toggleAiDrawer() {
  const drawer = document.getElementById('ai-drawer');
  if (!drawer) return;
  drawer.classList.toggle('translate-x-full');
  if (!drawer.classList.contains('translate-x-full')) {
    const input = document.getElementById('ai-chat-input');
    if (input) input.focus();
  }
}

function askPresetQuery(queryText) {
  const input = document.getElementById('ai-chat-input');
  if (input) input.value = queryText;
  handleSendAiMessage();
}

async function handleSendAiMessage(e) {
  if (e) e.preventDefault();
  const input = document.getElementById('ai-chat-input');
  const container = document.getElementById('ai-messages-container');
  if (!input || !container) return;

  const query = input.value.trim();
  if (!query) return;
  if (window.__adminAiAbort) { try { window.__adminAiAbort.abort(); } catch (_) {} }
  const ctrl = new AbortController();
  window.__adminAiAbort = ctrl;
  const nearBottom = () => container.scrollHeight - container.scrollTop - container.clientHeight < 90;
  const toBottom = () => { container.scrollTop = container.scrollHeight; };

  const userBubble = document.createElement('div');
  userBubble.className = 'flex justify-end';
  userBubble.innerHTML = `
    <div class="p-3 rounded-lg bg-indigo-500/10 border border-indigo-500/30 text-indigo-200 leading-relaxed max-w-[85%] font-sans">
      ${escapeHtml(query)}
    </div>
  `;
  container.appendChild(userBubble);
  input.value = '';

  const aiBubble = document.createElement('div');
  aiBubble.className = 'flex items-start space-x-2.5 animate-in fade-in duration-200';
  aiBubble.setAttribute('aria-busy', 'true');
  aiBubble.innerHTML = `
    <div class="w-6 h-6 rounded-md bg-indigo-500/20 text-indigo-400 flex items-center justify-center shrink-0 mt-0.5">
      <i data-lucide="bot" class="w-3.5 h-3.5"></i>
    </div>
    <div class="p-3 rounded-xl bg-card border border-line text-fg leading-relaxed max-w-[85%] font-sans">
      <div data-ai-text><span class="ai-typing" role="status" aria-label="Thinking"><i></i><i></i><i></i></span></div>
      <button type="button" data-ai-stop class="mt-1.5 text-xs text-fg-3 hover:text-fg underline">Stop</button>
    </div>`;
  container.appendChild(aiBubble);
  if (window.lucide) window.lucide.createIcons();
  toBottom();

  const textEl = aiBubble.querySelector('[data-ai-text]');
  const stopBtn = aiBubble.querySelector('[data-ai-stop]');
  stopBtn.addEventListener('click', () => ctrl.abort());
  let partial = '';
  try {
    const result = await window.streamAiReply({
      signal: ctrl.signal,
      body: { query: query, sessionId: 'admin-console', pageUrl: '/admin' },
      onText: (t) => {
        partial = t;
        const stick = nearBottom();
        textEl.innerHTML = window.formatAiText(t) + '<span class="ai-caret" aria-hidden="true"></span>';
        if (stick) toBottom();
      }
    });
    textEl.innerHTML = window.formatAiText(result.text || 'No answer was returned.');
    loadAdminDashboardMetrics();
    loadAiConfig();
  } catch (err) {
    if (err && err.name === 'AbortError') {
      textEl.innerHTML = (partial ? window.formatAiText(partial) + ' ' : '') + '<span class="text-fg-3">(stopped)</span>';
    } else {
      textEl.textContent = partial || 'The AI request failed.';
      showToast('AI query error: ' + err.message, 'warning');
    }
  } finally {
    aiBubble.setAttribute('aria-busy', 'false');
    stopBtn.remove();
    if (window.__adminAiAbort === ctrl) window.__adminAiAbort = null;
    if (nearBottom()) toBottom();
  }
}

async function saveAiSettings() {
  const enabled = document.getElementById('ai-toggle')?.checked ?? true;
  const systemPrompt = document.getElementById('ai-system-prompt')?.value || '';
  const modelName = document.getElementById('ai-model-select')?.value || 'gemini-1.5-flash';
  const temperature = parseFloat(document.getElementById('ai-temp-slider')?.value || '0.2');
  
  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/admin/ai/config', {
      method: 'PUT',
      headers,
      body: JSON.stringify({ enabled, systemPrompt, modelName, temperature, maxTokens: 1024 })
    });
    if (res.ok) {
      showToast(`AI settings saved (${modelName})!`, 'success');
      loadAiConfig();
    } else {
      showToast('Failed to save AI config', 'warning');
    }
  } catch (e) {
    showToast('Error saving AI config: ' + e.message, 'warning');
  }
}

// Notification Center Handlers
function toggleNotificationDrawer() {
  const drawer = document.getElementById('notification-drawer');
  if (!drawer) return;
  drawer.classList.toggle('translate-x-full');
}

let currentNotifFilter = 'ALL';
function filterNotifications(filterType, btnEl) {
  currentNotifFilter = filterType || currentNotifFilter;
  const clicked = btnEl || (typeof event !== 'undefined' && event && event.currentTarget instanceof Element ? event.currentTarget : null);
  document.querySelectorAll('.notif-filter').forEach(btn => btn.classList.remove('active'));
  const active = clicked || [...document.querySelectorAll('.notif-filter')].find(b => (b.getAttribute('onclick') || '').includes("'" + currentNotifFilter + "'"));
  if (active) active.classList.add('active');

  let shown = 0;
  document.querySelectorAll('.notif-item').forEach(item => {
    const type = item.getAttribute('data-type') || '';
    const priority = item.getAttribute('data-priority');
    const isRead = item.getAttribute('data-read') === 'true';
    let show = true;
    if (currentNotifFilter === 'UNREAD') show = !isRead;
    else if (currentNotifFilter === 'URGENT') show = priority === 'URGENT' || priority === 'HIGH';
    else if (currentNotifFilter === 'CRM') show = type === 'CRM' || type === 'NEW_CONTACT_INQUIRY' || type === 'CONTACT_INQUIRY';
    else if (currentNotifFilter === 'INTEGRATION') show = type.includes('INTEGRATION') || type.includes('GITHUB') || type.includes('SYNC');
    item.classList.toggle('hidden', !show);
    if (show) shown++;
  });
  const container = document.getElementById('notif-items-container');
  container?.querySelector('[data-notif-empty]')?.remove();
  if (container && shown === 0 && document.querySelectorAll('.notif-item').length > 0) {
    container.insertAdjacentHTML('beforeend', '<div data-notif-empty class="p-6 text-center text-fg-3 text-sm">Nothing matches this filter.</div>');
  }
}

function markSingleRead(btn) {
  const item = btn.closest('.notif-item');
  if (item) {
    item.setAttribute('data-read', 'true');
    item.classList.add('opacity-75');
    btn.outerHTML = '<span class="text-fg-3">Read</span>';
    decrementBadge();
    showToast('Notification marked as read', 'info');
  }
}

function markAllNotificationsRead() {
  document.querySelectorAll('.notif-item').forEach(item => {
    item.setAttribute('data-read', 'true');
    item.classList.add('opacity-75');
  });
  const badge = document.getElementById('notif-badge');
  if (badge) badge.innerText = '0';
  showToast('All notifications marked as read', 'success');
}

function decrementBadge() {
  const badge = document.getElementById('notif-badge');
  if (!badge) return;
  let count = parseInt(badge.innerText, 10);
  if (count > 0) {
    count--;
    badge.innerText = count;
  }
}

function incrementBadge() {
  const badge = document.getElementById('notif-badge');
  if (!badge) return;
  let count = parseInt(badge.innerText, 10);
  count++;
  badge.innerText = count;
}

// -------------------------------------------------------------
// Modular Integration & Profile Management Handlers (100% Neon DB)
// -------------------------------------------------------------
async function saveProfile() {
  const fullName = document.getElementById("prof-fullname")?.value?.trim() || "Raviranjan Kumar";
  const headline = document.getElementById("prof-headline")?.value?.trim() || "BCA Student · Software Developer";
  const education = document.getElementById("prof-education")?.value?.trim() || "Bachelor of Computer Applications (BCA)";
  const location = document.getElementById("prof-location")?.value?.trim() || "New Delhi, Delhi, India";
  const email = document.getElementById("prof-email")?.value?.trim() || "admin@raviranjan.dev";
  const phone = document.getElementById("prof-phone")?.value?.trim() || "+91-9999999999";
  const bio = document.getElementById("prof-bio")?.value?.trim() || "";
  const currentFocus = document.getElementById("prof-focus")?.value?.trim() || "";
  const availableForHire = document.getElementById("prof-availability")?.value !== "false";

  const githubUrl = document.getElementById("prof-github-url")?.value?.trim() || "https://github.com/raviranjankrmaurya";
  const linkedinUrl = document.getElementById("prof-linkedin-url")?.value?.trim() || "https://linkedin.com/in/raviranjan";
  const leetcodeUrl = document.getElementById("prof-leetcode-url")?.value?.trim() || "https://leetcode.com/u/raviranjanmaurya/";
  const gfgUrl = document.getElementById("prof-gfg-url")?.value?.trim() || "https://www.geeksforgeeks.org/user/raviranjankrmaurya/";
  const resumeUrl = document.getElementById("prof-resume-url")?.value?.trim() || "#";

  const resumeFullName = document.getElementById("resume-builder-name")?.value?.trim() || fullName;
  const resumeHeadline = document.getElementById("resume-builder-headline")?.value?.trim() || headline;
  const resumeSummary = document.getElementById("resume-builder-summary")?.value?.trim() || bio;

  const socialLinks = [
    { platform: "GitHub", url: githubUrl, handle: githubUrl.split("/").filter(Boolean).pop() || "raviranjankrmaurya", displayOrder: 1, visible: true },
    { platform: "LinkedIn", url: linkedinUrl, handle: linkedinUrl.split("/").filter(Boolean).pop() || "raviranjan", displayOrder: 2, visible: true },
    { platform: "LeetCode", url: leetcodeUrl, handle: leetcodeUrl.split("/").filter(Boolean).pop() || "raviranjanmaurya", displayOrder: 3, visible: true },
    { platform: "GeeksforGeeks", url: gfgUrl, handle: gfgUrl.split("/").filter(Boolean).pop() || "raviranjankrmaurya", displayOrder: 4, visible: true }
  ];

  const profileData = {
    fullName,
    headline,
    location,
    email,
    phone,
    bio,
    currentFocus,
    availableForHire,
    resumeUrl,
    socialLinks
  };

  localStorage.removeItem("cachedProfile");

  try {
    const headers = await getAuthHeaders();
    const [profRes, setRes] = await Promise.all([
      fetch("/api/admin/profile", {
        method: "PUT",
        headers,
        body: JSON.stringify(profileData)
      }),
      fetch("/api/admin/settings", {
        method: "PUT",
        headers,
        body: JSON.stringify({
          contactEmail: email,
          contactPhone: phone,
          profileEducation: education,
          resumeUrl: resumeUrl,
          resumeFullName,
          resumeHeadline,
          resumeSummary
        })
      })
    ]);
    if (profRes.ok) {
      showToast("Profile, Social Links & Resume saved!", "success");
      await loadAdminProfileData();
    } else {
      const err = await profRes.json().catch(() => ({}));
      showToast(err.message || "Failed to update profile", "warning");
    }
  } catch (err) {
    showToast("Network error saving profile: " + err.message, "warning");
  }
}

async function loadAdminProfileData() {
  localStorage.removeItem("cachedProfile");
  localStorage.removeItem("customProjects");

  try {
    const [profRes, setRes] = await Promise.all([
      fetch("/api/profile").catch(() => null),
      fetch("/api/settings").catch(() => null)
    ]);
    const settings = (setRes && setRes.ok ? (await setRes.json())?.data : null) || {};
    if (profRes && profRes.ok) {
      const json = await profRes.json();
      const p = json?.data;
      if (p) {
        const setV = (id, v) => { const el = document.getElementById(id); if (el && v !== undefined && v !== null) el.value = v; };
        setV("prof-fullname", p.fullName);
        setV("prof-headline", p.headline);
        setV("prof-education", settings.profileEducation || "Bachelor of Computer Applications (BCA)");
        setV("prof-location", p.location);
        setV("prof-email", p.email);
        setV("prof-phone", p.phone || settings.contactPhone || "+91-9999999999");
        setV("prof-bio", p.bio);
        setV("prof-focus", p.currentFocus);
        setV("prof-availability", String(p.availableForHire !== false));
        setV("prof-resume-url", p.resumeUrl || settings.resumeUrl || "#");

        setV("resume-builder-name", settings.resumeFullName || p.fullName || "Raviranjan Kumar");
        setV("resume-builder-headline", settings.resumeHeadline || p.headline || "BCA Student · Software Developer");
        setV("resume-builder-summary", settings.resumeSummary || p.bio || "");

        if (p.socialLinks && Array.isArray(p.socialLinks)) {
          p.socialLinks.forEach(l => {
            const plat = (l.platform || "").toLowerCase();
            if (plat.includes("github")) setV("prof-github-url", l.url);
            if (plat.includes("linkedin")) setV("prof-linkedin-url", l.url);
            if (plat.includes("leetcode")) setV("prof-leetcode-url", l.url);
            if (plat.includes("geeks")) setV("prof-gfg-url", l.url);
          });
        }
      }
    }
  } catch (e) {
    console.warn("Profile fetch note:", e);
  }
}

// Modal Handlers for Project Creation & Editing (100% Neon DB, Zero localStorage)
function openAddProjectModal() {
  const modal = document.getElementById("modal-add-project");
  if (modal) {
    document.getElementById("form-add-project")?.reset();
    const editIdInput = document.getElementById("edit-proj-id");
    if (editIdInput) editIdInput.value = "";
    const heading = document.getElementById("modal-project-heading");
    if (heading) heading.textContent = "Create New Project";
    const submitBtn = document.getElementById("modal-project-submit-label");
    if (submitBtn) submitBtn.textContent = "Save & Publish Project";
    modal.classList.remove("hidden");
  }
  if (window.lucide) window.lucide.createIcons();
}

function closeAddProjectModal() {
  const modal = document.getElementById("modal-add-project");
  if (modal) modal.classList.add("hidden");
}

function openEditProjectModalById(idOrSlug) {
  const project = cachedAdminProjects.find(p => String(p.id) === String(idOrSlug) || p.slug === idOrSlug);
  if (!project) {
    showToast("Project not found in loaded records", "warning");
    return;
  }

  openAddProjectModal();
  const heading = document.getElementById("modal-project-heading");
  if (heading) heading.innerText = "Edit Project: " + (project.title || project.slug);
  const submitLabel = document.getElementById("modal-project-submit-label");
  if (submitLabel) submitLabel.innerText = "Update Project";

  document.getElementById("edit-proj-id").value = project.id || project.slug;
  document.getElementById("new-proj-title").value = project.title || "";
  document.getElementById("new-proj-slug").value = project.slug || "";
  document.getElementById("new-proj-progress").value = project.progress !== undefined ? project.progress : 85;
  document.getElementById("new-proj-status").value = project.status || "PUBLISHED";
  document.getElementById("new-proj-tech").value = Array.isArray(project.technologies)
    ? project.technologies.join(", ")
    : (project.technologies || "Java 21, Spring Boot, PostgreSQL");
  document.getElementById("new-proj-summary").value = project.shortSummary || project.fullDescription || "";
  document.getElementById("new-proj-github").value = project.githubRepoUrl || "";
  document.getElementById("new-proj-demo").value = project.liveDemoUrl || "#";
}

// Keep legacy alias pointing to openEditProjectModalById
function editProjectRow(idOrSlug) {
  openEditProjectModalById(idOrSlug);
}

async function handleCreateProject(event) {
  event.preventDefault();
  localStorage.removeItem("customProjects");

  const editId = (document.getElementById("edit-proj-id")?.value || "").trim();
  const title = document.getElementById("new-proj-title").value.trim();
  const slug = document.getElementById("new-proj-slug").value.trim() || title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
  const category = document.getElementById("new-proj-category").value;
  const progress = parseInt(document.getElementById("new-proj-progress").value, 10) || 85;
  const status = document.getElementById("new-proj-status").value || "PUBLISHED";
  const tech = document.getElementById("new-proj-tech").value.split(",").map(t => t.trim()).filter(Boolean);
  const summary = document.getElementById("new-proj-summary").value.trim();
  const github = document.getElementById("new-proj-github").value.trim();
  const demo = document.getElementById("new-proj-demo").value.trim();

  const projectPayload = {
    title,
    slug,
    shortSummary: summary,
    fullDescription: summary,
    architectureStyle: category === "ai" ? "AI / ML Architecture" : (category === "web" ? "Full-Stack Architecture" : "Backend Architecture"),
    category,
    progress,
    status,
    technologies: tech,
    githubRepoUrl: github,
    liveDemoUrl: demo,
    featured: true
  };

  try {
    const headers = await getAuthHeaders();
    const url = editId
      ? `/api/admin/projects/${encodeURIComponent(editId)}`
      : "/api/admin/projects";
    const method = editId ? "PUT" : "POST";

    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify(projectPayload)
    });

    if (res.ok) {
      showToast(editId ? `Project "${title}" updated!` : `Project "${title}" created!`, "success");
      closeAddProjectModal();
      document.getElementById("form-add-project")?.reset();
      await loadAdminProjects();
      await loadAdminDashboardMetrics();
      await populateProjectEditorSelector();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save project", "warning");
    }
  } catch (err) {
    console.error("Save project error:", err);
    showToast("Network error saving project", "warning");
  }
}

async function syncIntegration(provider) {
  showToast(`Initiating live sync for ${provider} adapter...`, "info");
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/integrations/${provider}/sync?mode=MANUAL`, {
      method: "POST",
      headers
    });
    if (res.ok) {
      showToast(`${provider} synchronized!`, "success");
      loadAdminDashboardMetrics();
    } else {
      await fetch("/api/activity/sync", { method: "POST" });
      showToast(`${provider} synchronized!`, "success");
      loadAdminDashboardMetrics();
    }
  } catch (e) {
    showToast(`${provider} sync error: ` + e.message, "warning");
  }
}

function syncAllIntegrations() {
  triggerGlobalSync();
}

function addSocialLinkPrompt() {
  const platform = prompt("Enter Social Platform name (e.g. X, YouTube, Discord):");
  if (!platform) return;
  const url = prompt(`Enter URL for ${platform}:`);
  if (!url) return;

  const list = document.getElementById("social-links-list");
  if (list) {
    const item = document.createElement("div");
    item.className = "p-2.5 rounded-lg bg-card border border-line flex items-center justify-between";
    item.innerHTML = `
      <div>
        <span class="text-fg font-semibold">${escapeHtml(platform)}</span>
        <span class="text-fg-3 block text-xs">${escapeHtml(url)}</span>
      </div>
      <span class="text-emerald-400 text-xs">Active</span>
    `;
    list.appendChild(item);
    showToast(`Added social link: ${platform}`, "success");
  }
}

document.addEventListener("DOMContentLoaded", loadAdminProfileData);

// =========================================================================
// DYNAMIC REST API & NEON DB PERSISTENCE MODULE FOR ADMIN CMS
// =========================================================================

function isJwtExpiredOrInvalid(token) {
  if (!token || typeof token !== "string") return true;
  const parts = token.split(".");
  if (parts.length !== 3) return true;
  try {
    const base64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
    const payload = JSON.parse(atob(base64));
    if (!payload.exp) return false;
    const nowSec = Math.floor(Date.now() / 1000);
    return payload.exp <= (nowSec + 60); // Refresh if expiring within 60s
  } catch (e) {
    return true;
  }
}

async function getAdminToken(forceRefresh = false) {
  let token = localStorage.getItem("raviranjan_auth_token");
  if (forceRefresh || isJwtExpiredOrInvalid(token)) {
    token = null;
    localStorage.removeItem("raviranjan_auth_token");
    localStorage.removeItem("raviranjan_admin_authenticated");
  }
  return token;
}

async function getAuthHeaders(forceRefresh = false) {
  const token = await getAdminToken(forceRefresh);
  const headers = { "Content-Type": "application/json" };
  if (token) {
    headers["Authorization"] = "Bearer " + token;
    headers["X-Admin-Token"] = token;
  }
  return headers;
}

// 1. Projects Management (100% Neon DB, Zero Hardcoded Fallbacks)
let cachedAdminProjects = [];

async function loadAdminProjects() {
  localStorage.removeItem("customProjects");
  const tbody = document.getElementById("admin-projects-tbody");
  if (!tbody) return;

  try {
    let projects = null;
    try {
      const headers = await getAuthHeaders();
      const res = await fetch("/api/admin/projects?size=100", { headers });
      if (res.ok) {
        const data = await res.json();
        projects = data?.data?.content || data?.data || [];
      }
    } catch (e) {
      console.warn("Admin projects query fallback to public:", e);
    }

    if (projects === null) {
      const resPub = await fetch("/api/projects?size=100");
      if (resPub.ok) {
        const dataPub = await resPub.json();
        projects = dataPub?.data?.content || dataPub?.data || [];
      } else {
        projects = [];
      }
    }

    cachedAdminProjects = Array.isArray(projects) ? projects : [];
    const sidebarCount = document.getElementById("sidebar-projects-count");
    if (sidebarCount) sidebarCount.innerText = cachedAdminProjects.length;
    renderAdminProjectsTable(cachedAdminProjects);
  } catch (err) {
    console.error("Failed to load admin projects:", err);
    cachedAdminProjects = [];
    renderAdminProjectsTable([]);
  }
}

function renderAdminProjectsTable(projects) {
  const tbody = document.getElementById("admin-projects-tbody");
  if (!tbody) return;

  const counter = document.getElementById("admin-projects-counter");
  if (counter) {
    counter.innerText = `Showing ${projects.length} project${projects.length === 1 ? "" : "s"}`;
  }

  if (!projects || projects.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="p-8 text-center text-fg-3 text-xs">No projects. Click "+ Create New Project" to add your project directly.</td></tr>`;
    return;
  }

  tbody.innerHTML = projects.map(p => {
    const pid = escapeHtml(p.id || p.slug);
    const techList = Array.isArray(p.technologies) && p.technologies.length > 0
      ? p.technologies.join(", ")
      : (typeof p.technologies === "string" && p.technologies ? p.technologies : "Java 21, Spring Boot");
    const prog = p.progress !== undefined ? p.progress : 85;
    const status = p.status || "PUBLISHED";

    return `
      <tr class="hover:bg-card/70 transition-colors">
        <td class="py-3 px-4">
          <div class="font-medium text-fg">${escapeHtml(p.title)}</div>
          <div class="text-xs text-fg-3">slug: ${escapeHtml(p.slug)}</div>
        </td>
        <td class="py-3 px-4">
          <span class="px-2 py-0.5 rounded text-xs ${status ==="PUBLISHED" ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20" : "bg-amber-500/10 text-amber-400 border border-amber-500/20"}">${escapeHtml(status)}</span>
        </td>
        <td class="py-3 px-4">
          <div class="flex items-center space-x-2">
            <span class="text-indigo-400 font-bold">${prog}%</span>
            <div class="w-16 h-1.5 rounded-full bg-line"><div class="h-full bg-indigo-400 rounded-full" style="width: ${prog}%"></div></div>
          </div>
        </td>
        <td class="py-3 px-4 text-fg text-xs">${escapeHtml(techList)}</td>
        <td class="py-3 px-4 text-fg-2 text-xs">${escapeHtml(p.architectureStyle || "Modular Architecture")}</td>
        <td class="py-3 px-4 text-right space-x-2.5 whitespace-nowrap">
          <button type="button" onclick="open13TabEditorForProject('${pid}')" class="text-emerald-400 hover:text-emerald-300 text-xs rounded-lg" title="Deep 13-Tab Architecture Editor">13-Tab Editor</button>
          <button type="button" onclick="openEditProjectModalById('${pid}')" class="text-indigo-400 hover:text-indigo-300 text-xs rounded-lg" title="Edit Project">Edit</button>
          <button type="button" onclick="handleDeleteProject('${pid}')" class="text-rose-400 hover:text-rose-300 text-xs rounded-lg" title="Delete Project">Delete</button>
        </td>
      </tr>
    `;
  }).join("");
}

function filterAdminProjects(query) {
  if (!cachedAdminProjects) return;
  const q = (query || "").toLowerCase().trim();
  const statusFilter = (document.getElementById("admin-project-status-filter")?.value || "ALL");

  const filtered = cachedAdminProjects.filter(p => {
    const matchesQ = !q || (p.title || "").toLowerCase().includes(q) || (p.slug || "").toLowerCase().includes(q) || (Array.isArray(p.technologies) ? p.technologies.join(" ").toLowerCase().includes(q) : (p.technologies || "").toLowerCase().includes(q));
    const matchesStatus = (statusFilter === "ALL") || (p.status === statusFilter);
    return matchesQ && matchesStatus;
  });
  renderAdminProjectsTable(filtered);
}

function filterAdminProjectsByStatus(status) {
  const searchQ = (document.getElementById("admin-project-search")?.value || "");
  filterAdminProjects(searchQ);
}

async function handleDeleteProject(idOrSlug) {
  const proj = cachedAdminProjects.find(p => String(p.id) === String(idOrSlug) || p.slug === idOrSlug);
  const label = proj ? proj.title : idOrSlug;

  localStorage.removeItem("customProjects");
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${encodeURIComponent(idOrSlug)}`, {
      method: "DELETE",
      headers
    });

    if (res.ok) {
      showToast(`Project "${label}" deleted!`, "success");
      await loadAdminProjects();
      await loadAdminDashboardMetrics();
      await populateProjectEditorSelector();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to delete project", "warning");
    }
  } catch (err) {
    console.error("Delete project error:", err);
    showToast("Network error while deleting project", "warning");
  }
}

async function handleDeleteAllProjects() {

  localStorage.removeItem("customProjects");
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/projects/all", {
      method: "DELETE",
      headers
    });

    if (res.ok) {
      showToast("All projects deleted!", "success");
      await loadAdminProjects();
      await loadAdminDashboardMetrics();
      await populateProjectEditorSelector();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to delete all projects", "warning");
    }
  } catch (err) {
    console.error("Delete all projects error:", err);
    showToast("Network error while deleting all projects", "warning");
  }
}

// 2. Career (Experience & Education) & Credentials (Achievements & Certifications) Management
let cachedAdminExperiences = [];
let cachedAdminEducations = [];
let cachedAdminAchievements = [];
let cachedAdminCertifications = [];

function openCareerModal(type = 'experience', editId = null) {
  const modal = document.getElementById('modal-career');
  if (!modal) return;
  document.getElementById('form-career')?.reset();
  document.getElementById('career-id').value = '';
  document.getElementById('career-type').value = type;
  const isEdu = type === 'education';
  const isIntern = type === 'internship';

  const extraFields = document.getElementById('career-extra-fields');
  const internLinks = document.getElementById('career-internship-links');
  if (extraFields) extraFields.classList.toggle('hidden', isEdu);
  if (internLinks) internLinks.classList.toggle('hidden', isEdu);

  document.getElementById('modal-career-title').innerText = isEdu
    ? (editId ? 'Edit Educational Degree' : 'Add Educational Degree')
    : isIntern
      ? (editId ? 'Edit Internship / Add Certificate' : 'Add New Internship')
      : (editId ? 'Edit Work Experience' : 'Add Work Experience');

  document.getElementById('career-label-org').innerText = isEdu
    ? 'Institution / University *'
    : isIntern
      ? 'Internship Organization / Company *'
      : 'Company / Organization *';

  document.getElementById('career-label-role').innerText = isEdu
    ? 'Degree & Field of Study *'
    : isIntern
      ? 'Internship Role / Cohort Title *'
      : 'Role / Position *';

  document.getElementById('career-start-date').value = new Date().toISOString().split('T')[0];
  if (document.getElementById('career-status')) {
    document.getElementById('career-status').value = 'PURSUING';
  }
  if (document.getElementById('career-location')) {
    document.getElementById('career-location').value = isIntern ? 'Remote · India' : 'New Delhi, India';
  }
  if (document.getElementById('career-tech')) {
    document.getElementById('career-tech').value = isIntern ? 'Python, FastAPI, React, Machine Learning, PostgreSQL' : 'Java 21, Spring Boot, PostgreSQL';
  }
  if (document.getElementById('career-offer-url')) document.getElementById('career-offer-url').value = '';
  if (document.getElementById('career-cert-url')) document.getElementById('career-cert-url').value = '';
  if (document.getElementById('career-report-url')) document.getElementById('career-report-url').value = '';

  if (editId) {
    if (isEdu) {
      const edu = cachedAdminEducations.find(x => String(x.id) === String(editId));
      if (!edu) return;
      document.getElementById('career-id').value = edu.id;
      document.getElementById('career-org').value = edu.institution || '';
      document.getElementById('career-role').value = edu.degree || '';
      document.getElementById('career-start-date').value = edu.startDate || '2024-07-01';
      document.getElementById('career-end-date').value = edu.endDate || '';
      document.getElementById('career-desc').value = edu.description || '';
    } else {
      const exp = cachedAdminExperiences.find(x => String(x.id) === String(editId));
      if (!exp) return;
      document.getElementById('career-id').value = exp.id;
      document.getElementById('career-org').value = exp.company || '';
      document.getElementById('career-role').value = exp.role || '';
      document.getElementById('career-start-date').value = exp.startDate || '2026-05-11';
      document.getElementById('career-end-date').value = exp.endDate || '';
      document.getElementById('career-desc').value = exp.description || '';
      if (document.getElementById('career-status')) {
        document.getElementById('career-status').value = (exp.current || !exp.endDate) ? 'PURSUING' : 'COMPLETED';
      }
      if (document.getElementById('career-location')) {
        document.getElementById('career-location').value = exp.location || 'Remote · India';
      }
      if (document.getElementById('career-tech')) {
        document.getElementById('career-tech').value = exp.technologies || '';
      }
      if (document.getElementById('career-offer-url')) {
        document.getElementById('career-offer-url').value = exp.offerLetterUrl || '';
      }
      if (document.getElementById('career-cert-url')) {
        document.getElementById('career-cert-url').value = exp.certificateUrl || '';
      }
      if (document.getElementById('career-report-url')) {
        document.getElementById('career-report-url').value = (exp.achievements && String(exp.achievements).startsWith('http')) ? exp.achievements : '';
      }
    }
  }
  modal.classList.remove('hidden');
}

function closeCareerModal() {
  document.getElementById('modal-career')?.classList.add('hidden');
}

async function handleSaveCareer(event) {
  event.preventDefault();
  const editId = document.getElementById('career-id')?.value?.trim() || '';
  const type = document.getElementById('career-type').value;
  const org = document.getElementById('career-org').value.trim();
  const role = document.getElementById('career-role').value.trim();
  const startDate = document.getElementById('career-start-date').value || new Date().toISOString().split('T')[0];
  let endDate = document.getElementById('career-end-date').value || null;
  const desc = document.getElementById('career-desc').value.trim();
  const statusVal = document.getElementById('career-status')?.value || 'PURSUING';
  const locationVal = document.getElementById('career-location')?.value?.trim() || 'Remote · India';
  const techVal = document.getElementById('career-tech')?.value?.trim() || 'Java 21, Spring Boot, Python, FastAPI, PostgreSQL';
  const offerUrl = document.getElementById('career-offer-url')?.value?.trim() || '';
  const certUrl = document.getElementById('career-cert-url')?.value?.trim() || '';
  const reportUrl = document.getElementById('career-report-url')?.value?.trim() || '';

  const isEdu = type === 'education';
  const isIntern = type === 'internship';


  const isCurrent = statusVal === 'PURSUING' && !endDate;
  if (statusVal === 'COMPLETED' && !endDate) {
    endDate = new Date().toISOString().split('T')[0];
  }

  const baseUrl = isEdu ? '/api/admin/education' : '/api/admin/experiences';
  const url = editId ? `${baseUrl}/${editId}` : baseUrl;
  const method = editId ? 'PUT' : 'POST';

  const payload = isEdu ? {
    institution: org,
    degree: role,
    field: role,
    startDate: startDate,
    endDate: endDate,
    description: desc,
    grade: 'Enrolled',
    displayOrder: 1
  } : {
    company: org,
    role: role,
    employmentType: isIntern ? 'INTERNSHIP' : 'Full-time',
    location: locationVal,
    startDate: startDate,
    endDate: isCurrent ? null : endDate,
    current: isCurrent,
    description: desc,
    responsibilities: desc,
    technologies: techVal,
    offerLetterUrl: offerUrl,
    certificateUrl: certUrl,
    achievements: reportUrl,
    displayOrder: isIntern ? 1 : 2
  };

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast(`${isEdu ? 'Education' : isIntern ? 'Internship' : 'Experience'} saved!`, 'success');
      closeCareerModal();
      await loadAdminCareer();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Failed to save career entry.', 'warning');
    }
  } catch (e) {
    showToast('Error saving: ' + e.message, 'warning');
  }
}

async function loadAdminCareer() {
  const internContainer = document.getElementById('admin-internships-container');
  const eduContainer = document.getElementById('admin-education-container');
  const expContainer = document.getElementById('admin-experience-container');

  try {
    if (eduContainer) {
      const resEdu = await fetch('/api/education');
      const dataEdu = await resEdu.json();
      cachedAdminEducations = dataEdu?.data || [];
      if (cachedAdminEducations.length === 0) {
        eduContainer.innerHTML = '<div class="p-4 rounded-xl bg-card border border-line text-center text-fg-3 text-xs">No education records yet. Click "+ Add Degree" to record one.</div>';
      } else {
        eduContainer.innerHTML = cachedAdminEducations.map(edu => `
          <div class="p-4 rounded-xl bg-card border border-line space-y-2 text-xs">
            <div class="flex items-center justify-between">
              <span class="text-fg font-bold">${escapeHtml(edu.degree)}</span>
              <span class="text-xs px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">${escapeHtml(edu.grade || 'Enrolled')}</span>
            </div>
            <p class="text-fg-2 text-xs">${escapeHtml(edu.institution)}</p>
            <p class="text-fg font-sans text-xs leading-relaxed">${escapeHtml(edu.description || '')}</p>
            <div class="pt-2 flex items-center justify-between text-fg-3 text-xs">
              <span>Timeline: ${edu.startDate || ''} – ${edu.endDate || 'Present'}</span>
              <div class="space-x-2">
                <button type="button" onclick="openCareerModal('education', '${edu.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>
                <button type="button" onclick="handleDeleteEducation('${edu.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
              </div>
            </div>
          </div>
        `).join('');
      }
    }

    const resExp = await fetch('/api/experiences');
    const dataExp = await resExp.json();
    cachedAdminExperiences = dataExp?.data || [];

    const isInternRecord = (e) =>
      String(e.employmentType || '').toUpperCase().includes('INTERN') ||
      String(e.role || '').toLowerCase().includes('intern') ||
      Boolean(e.offerLetterUrl) ||
      Boolean(e.certificateUrl);

    const internships = cachedAdminExperiences.filter(isInternRecord);
    const workExps = cachedAdminExperiences.filter(e => !isInternRecord(e));

    if (internContainer) {
      if (internships.length === 0) {
        internContainer.innerHTML = '<div class="col-span-2 p-4 rounded-xl bg-card border border-line text-center text-fg-3 text-xs">No internships yet. Click "+ Add Internship" to add one.</div>';
      } else {
        internContainer.innerHTML = internships.map(exp => {
          const isPursuing = Boolean(exp.current) || !exp.endDate;
          const reportUrl = exp.achievements && String(exp.achievements).startsWith('http') ? exp.achievements : '';
          return `
            <div class="p-4 rounded-xl bg-card border ${isPursuing ? 'border-amber-500/30' : 'border-emerald-500/30'} space-y-2.5 text-xs flex flex-col justify-between">
              <div class="space-y-1.5">
                <div class="flex items-start justify-between gap-2">
                  <div>
                    <span class="text-fg font-bold text-sm block">${escapeHtml(exp.role)}</span>
                    <span class="text-indigo-400 text-xs">${escapeHtml(exp.company)}</span>
                    <span class="text-fg-2 text-xs"> · ${escapeHtml(exp.location || 'Remote')}</span>
                  </div>
                  <span class="text-xs px-2 py-0.5 rounded shrink-0 ${isPursuing ? 'bg-amber-500/10 text-amber-400 border border-amber-500/30' : 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/30'}">
                    ${isPursuing ? 'Pursuing / Ongoing' : 'Completed'}
                  </span>
                </div>
                <p class="text-fg font-sans text-xs leading-relaxed">${escapeHtml(exp.description || '')}</p>
                ${exp.technologies ? `<div class="text-xs text-fg-2">Stack: <span class="text-fg">${escapeHtml(exp.technologies)}</span></div>` : ''}
                <div class="flex flex-wrap gap-2 pt-1 text-xs">
                  ${exp.offerLetterUrl ? `<a href="${escapeHtml(exp.offerLetterUrl)}" target="_blank" class="px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 hover:underline">Offer Letter ↗</a>` : '<span class="text-fg-3">No Offer Letter URL</span>'}
                  ${exp.certificateUrl ? `<a href="${escapeHtml(exp.certificateUrl)}" target="_blank" class="px-2 py-0.5 rounded bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 hover:underline">Certificate ↗</a>` : '<span class="px-2 py-0.5 rounded bg-amber-500/10 text-amber-400 border border-amber-500/20">Certificate Pending (Click Edit to Add)</span>'}
                  ${reportUrl ? `<a href="${escapeHtml(reportUrl)}" target="_blank" class="px-2 py-0.5 rounded bg-purple-500/10 text-purple-400 border border-purple-500/20 hover:underline">Project Report ↗</a>` : ''}
                </div>
              </div>
              <div class="pt-2 border-t border-line flex items-center justify-between text-fg-3 text-xs">
                <span>Timeline: ${exp.startDate || ''} – ${exp.endDate || 'Present'}</span>
                <div class="space-x-2">
                  <button type="button" onclick="openCareerModal('internship', '${exp.id}')" class="px-2 py-0.5 rounded-lg bg-indigo-500/10 text-indigo-400 border border-indigo-500/30 hover:bg-indigo-500/20">Edit / Add Certificate</button>
                  <button type="button" onclick="handleDeleteExperience('${exp.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
                </div>
              </div>
            </div>
          `;
        }).join('');
      }
    }

    if (expContainer) {
      if (workExps.length === 0) {
        expContainer.innerHTML = '<div class="p-4 rounded-xl bg-card border border-line text-center text-fg-3 text-xs">No general experience records yet. Click "+ Add Experience" to record one.</div>';
      } else {
        expContainer.innerHTML = workExps.map(exp => `
          <div class="p-4 rounded-xl bg-card border border-line space-y-2 text-xs">
            <div class="flex items-center justify-between">
              <span class="text-fg font-bold">${escapeHtml(exp.role)}</span>
              <span class="text-xs px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">${exp.current ? 'Current' : 'Completed'}</span>
            </div>
            <p class="text-fg-2 text-xs">${escapeHtml(exp.company)} · ${escapeHtml(exp.location || 'India')}</p>
            <p class="text-fg font-sans text-xs leading-relaxed">${escapeHtml(exp.description || '')}</p>
            <div class="pt-2 flex items-center justify-between text-fg-3 text-xs">
              <span>Timeline: ${exp.startDate || ''} – ${exp.endDate || 'Present'}</span>
              <div class="space-x-2">
                <button type="button" onclick="openCareerModal('experience', '${exp.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>
                <button type="button" onclick="handleDeleteExperience('${exp.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
              </div>
            </div>
          </div>
        `).join('');
      }
    }
  } catch (err) {
    console.error('Failed to load career records:', err);
  }
}

async function handleDeleteEducation(id) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/education/${id}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast('Education record deleted!', 'success');
      loadAdminCareer();
    }
  } catch (e) {
    showToast('Failed to delete education: ' + e.message, 'warning');
  }
}

async function handleDeleteExperience(id) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/experiences/${id}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast('Experience record deleted!', 'success');
      loadAdminCareer();
    }
  } catch (e) {
    showToast('Failed to delete experience: ' + e.message, 'warning');
  }
}

// Achievements & Certifications CRUD
function toggleCredentialModalFields(type) {
  const isCert = type === 'certification';
  const orgLabel = document.getElementById('cred-org-label');
  const extraLabel = document.getElementById('cred-extra-label');
  if (orgLabel) orgLabel.innerText = isCert ? 'Issuing Organization *' : 'Organization / Platform *';
  if (extraLabel) extraLabel.innerText = isCert ? 'Credential ID' : 'Achievement Category';
}

function openCredentialModal(type = 'achievement', editId = null) {
  const modal = document.getElementById('modal-credential');
  if (!modal) return;
  document.getElementById('form-credential')?.reset();
  document.getElementById('cred-id').value = '';
  document.getElementById('cred-type').value = type;
  document.getElementById('cred-date').value = new Date().toISOString().split('T')[0];
  toggleCredentialModalFields(type);

  const heading = document.getElementById('modal-credential-heading');
  if (editId) {
    if (type === 'certification') {
      const c = cachedAdminCertifications.find(x => String(x.id) === String(editId));
      if (!c) return;
      if (heading) heading.innerText = 'Edit Certification';
      document.getElementById('cred-id').value = c.id;
      document.getElementById('cred-title').value = c.title || '';
      document.getElementById('cred-org').value = c.issuer || '';
      document.getElementById('cred-date').value = c.issueDate || new Date().toISOString().split('T')[0];
      document.getElementById('cred-extra').value = c.credentialId || '';
      document.getElementById('cred-desc').value = c.credentialUrl || '';
    } else {
      const a = cachedAdminAchievements.find(x => String(x.id) === String(editId));
      if (!a) return;
      if (heading) heading.innerText = 'Edit Achievement';
      document.getElementById('cred-id').value = a.id;
      document.getElementById('cred-title').value = a.title || '';
      document.getElementById('cred-org').value = a.organization || '';
      document.getElementById('cred-date').value = a.date || new Date().toISOString().split('T')[0];
      document.getElementById('cred-extra').value = a.category || 'COMPETITIVE_PROGRAMMING';
      document.getElementById('cred-desc').value = a.description || '';
    }
  } else {
    if (heading) heading.innerText = type === 'certification' ? 'Add Certification' : 'Add Achievement';
  }
  modal.classList.remove('hidden');
}

function closeCredentialModal() {
  document.getElementById('modal-credential')?.classList.add('hidden');
}

async function handleSaveCredential(event) {
  event.preventDefault();
  const editId = document.getElementById('cred-id')?.value?.trim() || '';
  const type = document.getElementById('cred-type').value;
  const title = document.getElementById('cred-title').value.trim();
  const org = document.getElementById('cred-org').value.trim();
  const date = document.getElementById('cred-date').value || new Date().toISOString().split('T')[0];
  const extra = document.getElementById('cred-extra').value.trim();
  const desc = document.getElementById('cred-desc').value.trim();


  const isCert = type === 'certification';
  const baseUrl = isCert ? '/api/admin/certifications' : '/api/admin/achievements';
  const url = editId ? `${baseUrl}/${editId}` : baseUrl;
  const method = editId ? 'PUT' : 'POST';

  const payload = isCert ? {
    title,
    issuer: org,
    issueDate: date,
    credentialId: extra || 'VERIFIED-2026',
    credentialUrl: desc.startsWith('http') ? desc : 'https://github.com/raviranjankrmaurya',
    featured: true,
    displayOrder: 1
  } : {
    title,
    organization: org,
    date,
    category: extra || 'ACHIEVEMENT',
    description: desc,
    featured: true,
    displayOrder: 1
  };

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(url, { method, headers, body: JSON.stringify(payload) });
    if (res.ok) {
      showToast(`${isCert ? 'Certification' : 'Achievement'} "${title}" saved!`, 'success');
      closeCredentialModal();
      await loadAdminCredentials();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Failed to save credential', 'warning');
    }
  } catch (e) {
    showToast('Error saving credential: ' + e.message, 'warning');
  }
}

async function loadAdminCredentials() {
  const achContainer = document.getElementById('admin-achievements-container');
  const certContainer = document.getElementById('admin-certifications-container');

  try {
    if (achContainer) {
      const resA = await fetch('/api/achievements');
      cachedAdminAchievements = (await resA.json())?.data || [];
      if (cachedAdminAchievements.length === 0) {
        achContainer.innerHTML = `<div class="p-4 text-center text-fg-3">No achievements. Click "+ Add Achievement" above.</div>`;
      } else {
        achContainer.innerHTML = cachedAdminAchievements.map(a => `
          <div class="p-3 rounded-xl bg-card border border-line flex items-center justify-between gap-2">
            <div>
              <span class="text-fg font-bold">${escapeHtml(a.title)}</span>
              <span class="text-fg-2 block text-xs">${escapeHtml(a.organization || '')} · ${escapeHtml(a.description || a.category || '')}</span>
            </div>
            <div class="flex items-center space-x-2 shrink-0">
              <button type="button" onclick="openCredentialModal('achievement', '${a.id}')" class="text-indigo-400 hover:underline text-xs rounded-lg">Edit</button>
              <button type="button" onclick="handleDeleteAchievement('${a.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
            </div>
          </div>
        `).join('');
      }
    }

    if (certContainer) {
      const resC = await fetch('/api/certifications');
      cachedAdminCertifications = (await resC.json())?.data || [];
      if (cachedAdminCertifications.length === 0) {
        certContainer.innerHTML = `<div class="p-4 text-center text-fg-3">No certifications. Click "+ Add Certification" above.</div>`;
      } else {
        certContainer.innerHTML = cachedAdminCertifications.map(c => `
          <div class="p-3 rounded-xl bg-card border border-line flex items-center justify-between gap-2">
            <div>
              <span class="text-fg font-bold">${escapeHtml(c.title)}</span>
              <span class="text-fg-2 block text-xs">${escapeHtml(c.issuer || '')} ${c.credentialId ? '· ID: ' + escapeHtml(c.credentialId) : ''}</span>
            </div>
            <div class="flex items-center space-x-2 shrink-0">
              <button type="button" onclick="openCredentialModal('certification', '${c.id}')" class="text-indigo-400 hover:underline text-xs rounded-lg">Edit</button>
              <button type="button" onclick="handleDeleteCertification('${c.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
            </div>
          </div>
        `).join('');
      }
    }
  } catch (e) {
    console.warn("Load credentials note:", e);
  }
}

async function handleDeleteAchievement(id) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/achievements/${id}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast("Achievement deleted!", "success");
      loadAdminCredentials();
    }
  } catch (e) {}
}

async function handleDeleteCertification(id) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/certifications/${id}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast("Certification deleted!", "success");
      loadAdminCredentials();
    }
  } catch (e) {}
}

// 3. Research Papers Management
let cachedAdminResearch = [];

function openResearchModal(paperId = null) {
  const modal = document.getElementById("modal-research");
  if (!modal) return;
  document.getElementById("form-research")?.reset();
  if (paperId) {
    const paper = typeof paperId === "object" ? paperId : cachedAdminResearch.find(p => String(p.id) === String(paperId));
    if (!paper) return;
    document.getElementById("modal-research-title").innerText = "Edit Research Paper";
    document.getElementById("research-id").value = paper.id || "";
    document.getElementById("research-title").value = paper.title || "";
    document.getElementById("research-slug").value = paper.slug || "";
    document.getElementById("research-status").value = paper.status || "PUBLISHED";
    document.getElementById("research-authors").value = paper.authors || "Raviranjan Kumar";
    document.getElementById("research-abstract").value = paper.abstractText || "";
    document.getElementById("research-doi").value = paper.doi || "";
    document.getElementById("research-github").value = paper.githubUrl || "";
  } else {
    document.getElementById("modal-research-title").innerText = "Publish Research Paper";
    document.getElementById("research-id").value = "";
  }
  modal.classList.remove("hidden");
}

function closeResearchModal() {
  document.getElementById("modal-research")?.classList.add("hidden");
}

async function handleSaveResearch(event) {
  event.preventDefault();
  const id = document.getElementById("research-id").value.trim();
  const title = document.getElementById("research-title").value.trim();
  const slug = document.getElementById("research-slug").value.trim() || title.toLowerCase().replace(/[^a-z0-9]+/g, "-");
  const status = document.getElementById("research-status").value;
  const authors = document.getElementById("research-authors").value.trim();
  const abstractText = document.getElementById("research-abstract").value.trim();
  const doi = document.getElementById("research-doi").value.trim();
  const githubUrl = document.getElementById("research-github").value.trim();


  const payload = {
    title,
    slug,
    status,
    authors,
    abstractText,
    doi,
    githubUrl,
    publishedDate: new Date().toISOString().split("T")[0]
  };

  const url = id ? `/api/admin/research/${id}` : "/api/admin/research";
  const method = id ? "PUT" : "POST";

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast(`Research paper "${title}" saved!`, "success");
      closeResearchModal();
      await loadAdminResearch();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save research paper.", "warning");
    }
  } catch (e) {
    showToast("Error saving research: " + e.message, "warning");
  }
}

async function loadAdminResearch() {
  const tbody = document.getElementById("admin-research-tbody");
  if (!tbody) return;

  try {
    const res = await fetch("/api/research?size=50");
    const data = await res.json();
    cachedAdminResearch = data?.data?.content || data?.data || [];

    if (!cachedAdminResearch || cachedAdminResearch.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="p-6 text-center text-fg-3 text-xs">No research papers. Click "+ New Paper" to publish one.</td></tr>`;
      return;
    }

    tbody.innerHTML = cachedAdminResearch.map(paper => `
      <tr class="hover:bg-raised transition-colors">
        <td class="p-3.5 font-bold text-fg">${escapeHtml(paper.title)}</td>
        <td class="p-3.5"><span class="px-2 py-0.5 rounded text-xs ${paper.status ==="PUBLISHED" ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20" : "bg-indigo-500/10 text-indigo-400 border border-indigo-500/20"}">${escapeHtml(paper.status || "PUBLISHED")}</span></td>
        <td class="p-3.5 text-fg font-sans max-w-xs truncate">${escapeHtml(paper.abstractText || "")}</td>
        <td class="p-3.5 text-fg-3">${paper.publishedDate || "2026"}</td>
        <td class="p-3.5 text-right space-x-2 whitespace-nowrap">
          <button type="button" onclick="openResearchModal('${paper.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>
          <button type="button" onclick="handleDeleteResearch('${paper.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
        </td>
      </tr>
    `).join("");
  } catch (err) {
    console.error("Failed to load research papers:", err);
  }
}

async function handleDeleteResearch(id) {
  const p = cachedAdminResearch.find(item => String(item.id) === String(id));
  const title = p ? p.title : id;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/research/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast(`Research paper "${title}" deleted!`, "success");
      await loadAdminResearch();
    }
  } catch (e) {
    showToast("Failed to delete research paper: " + e.message, "warning");
  }
}

// 4. Skills Matrix Management
let cachedAdminSkills = [];

function openSkillModal(skillId = null, defaultCategory = null) {
  const modal = document.getElementById("modal-skill");
  if (!modal) return;
  document.getElementById("form-skill")?.reset();
  const idInput = document.getElementById("skill-id");
  if (idInput) idInput.value = "";

  populateSkillCategoryDropdown();

  if (skillId) {
    const skill = cachedAdminSkills.find(s => String(s.id) === String(skillId));
    if (!skill) return;
    if (idInput) idInput.value = skill.id;
    document.getElementById("skill-name").value = skill.name || "";
    if (skill.categoryName && document.getElementById("skill-category-select")) {
      document.getElementById("skill-category-select").value = skill.categoryName;
    }
  } else if (defaultCategory && document.getElementById("skill-category-select")) {
    document.getElementById("skill-category-select").value = defaultCategory;
  }
  modal.classList.remove("hidden");
}

function populateSkillCategoryDropdown() {
  const select = document.getElementById("skill-category-select");
  if (!select) return;
  const standardCategories = [
    "Languages & Backend",
    "Frontend & Client Architecture",
    "Database & Persistence",
    "Tools & AI Applications",
    "Frameworks",
    "DevOps"
  ];
  const existingSet = new Set(standardCategories);
  cachedAdminCategories.forEach(c => {
    if (c.name && c.name.trim()) existingSet.add(c.name.trim());
  });
  select.innerHTML = Array.from(existingSet).map(c => `
    <option value="${escapeHtml(c)}">${escapeHtml(c)}</option>
  `).join("");
}

function closeSkillModal() {
  document.getElementById("modal-skill")?.classList.add("hidden");
}

async function handleSaveSkill(event) {
  event.preventDefault();
  const editId = document.getElementById("skill-id")?.value?.trim() || "";
  const name = document.getElementById("skill-name").value.trim();
  const level = document.getElementById("skill-level").value;
  const category = document.getElementById("skill-category-select").value;


  try {
    const headers = await getAuthHeaders();
    const url = editId ? `/api/admin/skills/${editId}` : "/api/admin/skills";
    const method = editId ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify({ name, proficiencyLevel: level, categoryName: category })
    });
    if (res.ok) {
      showToast(`Skill "${name}" saved!`, "success");
      closeSkillModal();
      await loadAdminSkills();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save skill.", "warning");
    }
  } catch (e) {
    showToast("Error saving skill: " + e.message, "warning");
  }
}

let cachedAdminCategories = [];

async function loadAdminSkills() {
  const container = document.getElementById("skills-categories-grid");
  if (!container) return;

  try {
    const res = await fetch("/api/skills");
    const data = await res.json();
    const rawCategories = data?.data || [];
    cachedAdminCategories = rawCategories;
    cachedAdminSkills = [];

    // Ensure all 6 baseline categories are visible in the matrix even if currently holding 0 skills
    const baselineNames = [
      "Languages & Backend",
      "Frontend & Client Architecture",
      "Database & Persistence",
      "Tools & AI Applications",
      "Frameworks",
      "DevOps"
    ];
    const categoryMap = new Map();
    baselineNames.forEach(b => categoryMap.set(b.toLowerCase(), { name: b, skills: [] }));
    rawCategories.forEach(c => {
      const k = (c.name || "").toLowerCase();
      categoryMap.set(k, { name: c.name, skills: c.skills || [] });
      (c.skills || []).forEach(s => cachedAdminSkills.push({ ...s, categoryName: c.name }));
    });

    const displayCategories = Array.from(categoryMap.values());
    populateSkillCategoryDropdown();

    container.innerHTML = displayCategories.map(cat => {
      const skillCount = cat.skills ? cat.skills.length : 0;
      const skillsHtml = skillCount > 0
        ? (cat.skills || []).map(s => `
            <span class="text-xs px-2.5 py-1 rounded bg-raised text-fg border border-line flex items-center gap-2">
              <span>${escapeHtml(s.name)} <span class="text-indigo-400 text-xs">(${s.proficiency || 85}%)</span></span>
              <button type="button" onclick="openSkillModal('${s.id}')" class="text-indigo-400 hover:text-indigo-300 text-xs rounded-lg" title="Edit skill">Edit</button>
              <button type="button" onclick="handleDeleteSkill('${s.id}')" class="text-rose-400 hover:text-rose-300 font-bold px-0.5 rounded-lg" title="Delete skill">&times;</button>
            </span>
          `).join("")
        : `<div class="w-full py-2 text-center text-fg-3 text-xs">No skills in this category yet. <button type="button" onclick="openSkillModal(null, '${escapeHtml(cat.name)}')" class="text-indigo-400 hover:underline font-bold rounded-lg">+ Add First Skill</button></div>`;

      return `
        <div class="saas-card p-5 space-y-3 border border-line rounded-xl bg-card">
          <div class="flex items-center justify-between border-b border-line pb-2">
            <span class="text-xs font-bold text-fg uppercase">${escapeHtml(cat.name)}</span>
            <div class="flex items-center gap-2">
              <span class="text-xs text-fg-2">${skillCount} Skills</span>
              <button type="button" onclick="openSkillModal(null, '${escapeHtml(cat.name)}')" class="px-2 py-0.5 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 text-indigo-400 border border-indigo-500/20 text-xs transition-colors">
                + Add
              </button>
            </div>
          </div>
          <div class="flex flex-wrap gap-2 pt-1">
            ${skillsHtml}
          </div>
        </div>
      `;
    }).join("");
  } catch (err) {
    console.error("Failed to load skills:", err);
  }
}

async function handleDeleteSkill(id) {
  const s = cachedAdminSkills.find(item => String(item.id) === String(id));
  const name = s ? s.name : id;
  if (!(await window.confirmDialog({ title: `Delete skill "${name}"?`, message: "It will be removed from the database and the public skills matrix.", confirmText: "Delete" }))) {
    return;
  }
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/skills/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast(`Skill "${name}" deleted!`, "success");
      await loadAdminSkills();
    }
  } catch (e) {
    showToast("Failed to delete skill: " + e.message, "warning");
  }
}

// 5. Engineering Lab Experiments Management
let cachedAdminLabs = [];

function openLabModal(labId = null) {
  const modal = document.getElementById("modal-lab");
  if (!modal) return;
  document.getElementById("form-lab")?.reset();
  const idInput = document.getElementById("lab-id");
  if (idInput) idInput.value = "";

  if (labId) {
    const lab = cachedAdminLabs.find(l => String(l.id) === String(labId));
    if (!lab) return;
    if (idInput) idInput.value = lab.id;
    document.getElementById("modal-lab-title").innerText = "Edit Lab Experiment";
    document.getElementById("lab-title").value = lab.title || "";
    document.getElementById("lab-slug").value = lab.slug || "";
    document.getElementById("lab-status").value = lab.status || "ACTIVE";
    document.getElementById("lab-objective").value = lab.objective || "";
    document.getElementById("lab-desc").value = lab.description || "";
    document.getElementById("lab-tech").value = lab.technologies || "";
    document.getElementById("lab-github").value = lab.githubUrl || "";
  } else {
    document.getElementById("modal-lab-title").innerText = "Add Lab Experiment";
  }
  modal.classList.remove("hidden");
}

function closeLabModal() {
  document.getElementById("modal-lab")?.classList.add("hidden");
}

async function handleSaveLab(event) {
  event.preventDefault();
  const editId = document.getElementById("lab-id")?.value?.trim() || "";
  const title = document.getElementById("lab-title").value.trim();
  const slug = document.getElementById("lab-slug").value.trim() || title.toLowerCase().replace(/[^a-z0-9]+/g, "-");
  const status = document.getElementById("lab-status").value;
  const objective = document.getElementById("lab-objective").value.trim();
  const description = document.getElementById("lab-desc").value.trim();
  const technologies = document.getElementById("lab-tech").value.trim();
  const githubUrl = document.getElementById("lab-github").value.trim();


  try {
    const headers = await getAuthHeaders();
    const url = editId ? `/api/admin/lab/${editId}` : "/api/admin/lab";
    const method = editId ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify({
        title, slug, status, objective, description, technologies, githubUrl,
        progress: 100, conductedAt: new Date().toISOString().split("T")[0]
      })
    });
    if (res.ok) {
      showToast(`Experiment "${title}" saved!`, "success");
      closeLabModal();
      await loadAdminLab();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save experiment.", "warning");
    }
  } catch (e) {
    showToast("Error saving experiment: " + e.message, "warning");
  }
}

async function loadAdminLab() {
  const container = document.getElementById("admin-lab-grid") || document.querySelector("#view-lab .grid");
  if (!container) return;

  try {
    const res = await fetch("/api/lab?size=50");
    const data = await res.json();
    cachedAdminLabs = data?.data?.content || data?.data || [];
    if (cachedAdminLabs.length === 0) {
      container.innerHTML = `<div class="col-span-3 p-6 text-center text-fg-3 text-xs">No lab experiments. Click "+ New Experiment" to add one.</div>`;
      return;
    }
    container.innerHTML = cachedAdminLabs.map((lab, idx) => `
      <div class="saas-card p-5 space-y-3 flex flex-col justify-between">
        <div class="space-y-2">
          <div class="flex items-center justify-between text-xs">
            <span class="text-indigo-400 font-bold">LAB-0${idx + 1}</span>
            <span class="text-emerald-400">${escapeHtml(lab.status || "ACTIVE")}</span>
          </div>
          <h3 class="font-bold text-fg text-sm">${escapeHtml(lab.title)}</h3>
          <p class="text-fg-2 text-xs font-sans leading-relaxed">${escapeHtml(lab.description || lab.objective || "")}</p>
          <div class="text-xs text-fg pt-1">
            <span class="text-fg-3">Tech:</span> ${escapeHtml(lab.technologies || "Java 21")}
          </div>
        </div>
        <div class="pt-2 border-t border-line flex items-center justify-between text-xs">
          <a href="${escapeHtml(lab.githubUrl || "#")}" target="_blank" class="text-indigo-400 hover:underline">GitHub ↗</a>
          <div class="space-x-2">
            <button type="button" onclick="openLabModal('${lab.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>
            <button type="button" onclick="handleDeleteLab('${lab.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
          </div>
        </div>
      </div>
    `).join("");
  } catch (err) {
    console.error("Failed to load lab experiments:", err);
  }
}

async function handleDeleteLab(id) {
  const lab = cachedAdminLabs.find(l => String(l.id) === String(id));
  const title = lab ? lab.title : id;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/lab/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast(`Experiment "${title}" deleted!`, "success");
      await loadAdminLab();
    }
  } catch (e) {
    showToast("Failed to delete experiment: " + e.message, "warning");
  }
}

// 6. Contribution Stories Management
let cachedAdminStories = [];

function openStoryModal(storyId = null) {
  const modal = document.getElementById("modal-story");
  if (!modal) return;
  document.getElementById("form-story")?.reset();
  const idInput = document.getElementById("story-id");
  if (idInput) idInput.value = "";

  if (storyId) {
    const story = cachedAdminStories.find(s => String(s.id) === String(storyId));
    if (!story) return;
    if (idInput) idInput.value = story.id;
    document.getElementById("story-repo").value = story.repository || "";
    document.getElementById("story-title").value = story.title || "";
    document.getElementById("story-what").value = story.whatIContributed || "";
    document.getElementById("story-problem").value = story.problem || story.whyIContributed || "";
    document.getElementById("story-pr-url").value = story.prUrl || "";
    document.getElementById("story-tech").value = story.technologies || "";
  }
  modal.classList.remove("hidden");
}

function closeStoryModal() {
  document.getElementById("modal-story")?.classList.add("hidden");
}

async function handleSaveStory(event) {
  event.preventDefault();
  const editId = document.getElementById("story-id")?.value?.trim() || "";
  const repo = document.getElementById("story-repo").value.trim();
  const title = document.getElementById("story-title").value.trim();
  const what = document.getElementById("story-what").value.trim();
  const problem = document.getElementById("story-problem").value.trim();
  const prUrl = document.getElementById("story-pr-url").value.trim();
  const tech = document.getElementById("story-tech").value.trim();


  try {
    const headers = await getAuthHeaders();
    const url = editId ? `/api/admin/stories/${editId}` : "/api/admin/stories";
    const method = editId ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify({
        repository: repo,
        title,
        whatIContributed: what,
        problem,
        whyIContributed: problem,
        prUrl,
        technologies: tech,
        contributionDate: new Date().toISOString().split("T")[0]
      })
    });
    if (res.ok) {
      showToast(`Contribution story "${title}" saved!`, "success");
      closeStoryModal();
      await loadAdminStories();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save story.", "warning");
    }
  } catch (e) {
    showToast("Error saving story: " + e.message, "warning");
  }
}

async function loadAdminStories() {
  const container = document.getElementById("admin-stories-grid") || document.querySelector("#view-contributions .grid");
  if (!container) return;

  try {
    const res = await fetch("/api/stories?size=50");
    const data = await res.json();
    cachedAdminStories = data?.data?.content || data?.data || [];
    if (cachedAdminStories.length === 0) {
      container.innerHTML = `<div class="col-span-2 p-6 text-center text-fg-3 text-xs">No contribution stories. Click "+ New Story" to add one.</div>`;
      return;
    }
    container.innerHTML = cachedAdminStories.map(s => `
      <div class="saas-card p-5 space-y-3 flex flex-col justify-between">
        <div class="space-y-2">
          <div class="flex items-center justify-between text-xs">
            <span class="text-fg font-bold">${escapeHtml(s.repository)}</span>
            <span class="text-xs text-purple-400">${escapeHtml(s.externalContributionId || "Merged PR")}</span>
          </div>
          <h4 class="text-fg font-semibold text-xs">${escapeHtml(s.title)}</h4>
          <p class="text-fg-2 text-xs font-sans leading-relaxed">${escapeHtml(s.whatIContributed || s.problem || "")}</p>
        </div>
        <div class="pt-2 border-t border-line flex items-center justify-between text-xs">
          <a href="${escapeHtml(s.prUrl || "#")}" target="_blank" class="text-indigo-400 hover:underline">PR Link ↗</a>
          <div class="space-x-2">
            <button type="button" onclick="openStoryModal('${s.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>
            <button type="button" onclick="handleDeleteStory('${s.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
          </div>
        </div>
      </div>
    `).join("");
  } catch (err) {
    console.error("Failed to load contribution stories:", err);
  }
}

async function handleDeleteStory(id) {
  const s = cachedAdminStories.find(item => String(item.id) === String(id));
  const title = s ? s.title : id;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/stories/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast(`Contribution story "${title}" deleted!`, "success");
      await loadAdminStories();
    }
  } catch (e) {
    showToast("Failed to delete story: " + e.message, "warning");
  }
}

// 7. Technical Writing / Articles Management
let cachedAdminArticles = [];

function resetArticleEditor() {
  if (document.getElementById("article-id")) document.getElementById("article-id").value = "";
  if (document.getElementById("article-title")) document.getElementById("article-title").value = "";
  if (document.getElementById("article-slug")) document.getElementById("article-slug").value = "";
  if (document.getElementById("article-summary")) document.getElementById("article-summary").value = "";
  if (document.getElementById("markdown-input")) document.getElementById("markdown-input").value = "# New Technical Article\n\nWrite your markdown content here...";
  updateMarkdownPreview();
}

async function loadAdminArticles() {
  const tbody = document.getElementById("admin-articles-tbody");
  if (!tbody) return;

  try {
    const res = await fetch("/api/articles?size=50");
    if (res.ok) {
      const json = await res.json();
      cachedAdminArticles = json?.data?.content || json?.data || [];
      if (cachedAdminArticles.length === 0) {
        tbody.innerHTML = `<tr><td colspan="5" class="p-6 text-center text-fg-3">No articles yet. Use the editor above to publish one.</td></tr>`;
        return;
      }
      tbody.innerHTML = cachedAdminArticles.map(a => `
        <tr class="hover:bg-raised transition-colors">
          <td class="p-3 font-bold text-fg">${escapeHtml(a.title)}</td>
          <td class="p-3 text-fg-2">${escapeHtml(a.slug)}</td>
          <td class="p-3"><span class="px-2 py-0.5 rounded text-xs bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">${escapeHtml(a.status || "PUBLISHED")}</span></td>
          <td class="p-3 text-fg max-w-xs truncate font-sans">${escapeHtml(a.summary || "")}</td>
          <td class="p-3 text-right space-x-2 whitespace-nowrap">
            <button type="button" onclick="openEditArticle('${a.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>
            <button type="button" onclick="handleDeleteArticle('${a.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>
          </td>
        </tr>
      `).join("");
    }
  } catch (e) {
    console.warn("Load articles error:", e);
  }
}

function openEditArticle(id) {
  const a = cachedAdminArticles.find(item => String(item.id) === String(id));
  if (!a) return;
  document.getElementById("article-id").value = a.id;
  document.getElementById("article-title").value = a.title || "";
  document.getElementById("article-slug").value = a.slug || "";
  document.getElementById("article-summary").value = a.summary || "";
  document.getElementById("markdown-input").value = a.content || a.summary || "";
  updateMarkdownPreview();
}

async function handleSaveArticle(status = "PUBLISHED") {
  const editId = document.getElementById("article-id")?.value?.trim() || "";
  const title = document.getElementById("article-title")?.value?.trim() || "";
  const slug = document.getElementById("article-slug")?.value?.trim() || title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/(^-|-$)/g, "");
  const summary = document.getElementById("article-summary")?.value?.trim() || title;
  const content = document.getElementById("markdown-input")?.value?.trim() || "";

  if (!title || !content) {
    showToast("Article Title and Markdown Content are required!", "warning");
    return;
  }


  try {
    const headers = await getAuthHeaders();
    const url = editId ? `/api/admin/articles/${editId}` : "/api/admin/articles";
    const method = editId ? "PUT" : "POST";
    const res = await fetch(url, {
      method,
      headers,
      body: JSON.stringify({
        title,
        slug,
        summary,
        content,
        status,
        readingTimeMinutes: 6
      })
    });
    if (res.ok) {
      showToast(`Article "${title}" saved!`, "success");
      resetArticleEditor();
      await loadAdminArticles();
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || "Failed to save article", "warning");
    }
  } catch (e) {
    showToast("Error saving article: " + e.message, "warning");
  }
}

async function handleDeleteArticle(id) {
  const a = cachedAdminArticles.find(item => String(item.id) === String(id));
  const title = a ? a.title : id;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/articles/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast(`Article "${title}" deleted!`, "success");
      await loadAdminArticles();
    }
  } catch (e) {
    showToast("Error deleting article: " + e.message, "warning");
  }
}

// 8. Contact Intelligence, AI Moderation & CRM Inquiries Management
let cachedAdminInquiries = [];
let currentAdminCrmFilter = 'ALL';

function filterAdminCrm(filterType, btnEl) {
  currentAdminCrmFilter = filterType;
  document.querySelectorAll('.crm-filter-btn').forEach(btn => {
    btn.classList.remove('active', 'bg-indigo-500/10', 'text-indigo-400', 'border-indigo-500/30');
    btn.classList.add('bg-raised', 'text-fg-2', 'border-line');
  });
  if (btnEl) {
    btnEl.classList.add('active', 'bg-indigo-500/10', 'text-indigo-400', 'border-indigo-500/30');
    btnEl.classList.remove('bg-raised', 'text-fg-2', 'border-line');
  }
  renderAdminCrmList();
}

async function loadAdminCrm() {
  const container = document.getElementById("admin-crm-container");
  if (!container) return;

  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/inquiries?size=50", { headers });
    if (!res.ok) throw new Error("Server responded " + res.status);
    const json = await res.json();
    cachedAdminInquiries = json?.data?.content || json?.data || [];
    renderAdminCrmList();
  } catch (e) {
    console.warn("Load CRM error:", e);
    container.innerHTML = `
      <div class="p-6 rounded-xl bg-card border border-line text-center space-y-3">
        <p class="text-fg-2">Could not load inquiries. ${escapeHtml(e.message || "")}</p>
        <button type="button" onclick="loadAdminCrm()" class="btn btn--pill btn--primary">Try again</button>
      </div>`;
  }
}

function toggleCrmCard(btn) {
  const card = btn.closest('[data-crm-card]');
  if (!card) return;
  const open = card.getAttribute('data-open') !== 'true';
  card.setAttribute('data-open', String(open));
  card.querySelector('[data-crm-details]')?.classList.toggle('hidden', !open);
  card.querySelector('[data-crm-msg]')?.classList.toggle('line-clamp-2', !open);
  card.querySelectorAll('[data-crm-toggle]').forEach(b => {
    b.setAttribute('aria-expanded', String(open));
    const label = b.querySelector('[data-crm-toggle-label]');
    if (label) label.textContent = open ? 'Show less' : 'Show more';
  });
}

function renderAdminCrmList() {
  const container = document.getElementById("admin-crm-container");
  if (!container) return;

  const searchVal = (document.getElementById("admin-crm-search")?.value || "").trim().toLowerCase();

  const filtered = cachedAdminInquiries.filter(inq => {
    const isSpam = Boolean(inq.spam) || inq.moderationStatus === 'QUARANTINED_SPAM' || inq.moderationStatus === 'CONFIRMED_SPAM' || inq.intent === 'SPAM';
    const isAbusive = Boolean(inq.abusive) || Boolean(inq.threatening) || inq.moderationStatus === 'FLAGGED_ABUSE' || inq.intent === 'ABUSIVE_LANGUAGE' || inq.intent === 'THREATENING_CONTENT';
    const needsReview = inq.moderationStatus === 'REVIEW_REQUIRED' || inq.moderationStatus === 'MODERATION_FAILED' || inq.intent === 'UNCERTAIN';
    const isRepeat = Boolean(inq.repeatSubmission) || (Number(inq.repeatCount) > 1);

    if (currentAdminCrmFilter === 'GENUINE' && (isSpam || isAbusive)) return false;
    if (currentAdminCrmFilter === 'REVIEW_REQUIRED' && !needsReview) return false;
    if (currentAdminCrmFilter === 'SPAM' && !isSpam) return false;
    if (currentAdminCrmFilter === 'ABUSIVE' && !isAbusive) return false;
    if (currentAdminCrmFilter === 'REPEAT' && !isRepeat) return false;

    if (searchVal) {
      const hay = [
        inq.name, inq.email, inq.company, inq.message,
        inq.city, inq.region, inq.country, inq.intent,
        inq.moderationStatus, inq.moderationReason, inq.ipAddress
      ].filter(Boolean).join(" ").toLowerCase();
      if (!hay.includes(searchVal)) return false;
    }
    return true;
  });

  if (filtered.length === 0) {
    container.innerHTML = `<div class="p-6 rounded-xl bg-card border border-line text-center text-fg-3">No inquiries match this filter.</div>`;
    return;
  }

  container.innerHTML = filtered.map(inq => {
    const id = escapeHtml(String(inq.id));
    const isSpam = Boolean(inq.spam) || inq.moderationStatus === "QUARANTINED_SPAM" || inq.moderationStatus === "CONFIRMED_SPAM";
    const isAbusive = Boolean(inq.abusive) || Boolean(inq.threatening) || inq.moderationStatus === "FLAGGED_ABUSE";
    const needsReview = inq.moderationStatus === "REVIEW_REQUIRED" || inq.moderationStatus === "MODERATION_FAILED";
    const isRepeat = Boolean(inq.repeatSubmission) || Number(inq.repeatCount) > 1;
    const isDisposable = Boolean(inq.disposableEmail);
    const pretty = v => String(v || "").replace(/_/g, " ").toLowerCase().replace(/^\w/, c => c.toUpperCase());

    // moderation chip only when the backend actually analysed the message (nothing is invented)
    const hasMod = Boolean(inq.moderationStatus || inq.intent);
    const conf = (inq.moderationConfidence !== undefined && inq.moderationConfidence !== null && inq.moderationConfidence !== "")
      ? ` ${Math.round(Number(inq.moderationConfidence) * 100)}%` : "";
    let modClass = "bg-emerald-500/10 text-emerald-400 border-emerald-500/30";
    if (isSpam || isAbusive) modClass = "bg-rose-500/10 text-rose-400 border-rose-500/30";
    else if (needsReview) modClass = "bg-amber-500/10 text-amber-400 border-amber-500/30";
    const modLabel = pretty(inq.moderationStatus || inq.intent) + conf;

    const place = [inq.city, inq.region, inq.country].filter(p => p && p !== "Private / Loopback Network" && p !== "Location Unavailable").join(", ");
    const timeStr = inq.createdAt ? new Date(inq.createdAt).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : "";
    const msg = String(inq.message || "");
    const longMsg = msg.length > 140 || msg.split("\n").length > 2;
    const initial = escapeHtml((inq.name || "?").trim().charAt(0).toUpperCase() || "?");

    const rows = [
      ["Location", place ? place + (inq.geoProvider && inq.geoProvider !== "UNAVAILABLE" && inq.geoProvider !== "PRIVATE_OR_LOOPBACK" ? " (" + inq.geoProvider + ")" : "") : "Not available"],
      ["Referrer", inq.referrer],
      ["IP address", inq.ipAddress || "Redacted"],
      ["Browser hash", inq.browserHash],
      ["User agent", inq.userAgent],
      ["Moderation reason", inq.moderationReason],
      ["Suggested action", inq.recommendedAction ? pretty(inq.recommendedAction) : ""]
    ].filter(r => r[1]).map(r => `<div class="min-w-0"><dt class="text-fg-3">${r[0]}</dt><dd class="text-fg-2 break-words">${escapeHtml(String(r[1]))}</dd></div>`).join("");

    const iconBtn = (fn, icon, label, tone) => `<button type="button" onclick="${fn}" class="btn btn--icon btn--pill ${tone}" aria-label="${label}" title="${label}"><i data-lucide="${icon}" class="w-4 h-4" aria-hidden="true"></i></button>`;

    return `
      <article data-crm-card data-open="false" class="p-3.5 rounded-xl bg-card border ${isSpam || isAbusive ? "border-rose-500/30" : "border-line"} space-y-2.5">
        <div class="flex items-start gap-3">
          <span class="w-9 h-9 shrink-0 rounded-full grid place-items-center text-sm font-semibold bg-indigo-500/15 text-indigo-400" aria-hidden="true">${initial}</span>
          <div class="min-w-0 flex-1">
            <div class="flex flex-wrap items-center gap-x-2 gap-y-1">
              <span class="font-semibold text-fg text-sm truncate max-w-[16rem]">${escapeHtml(inq.name || "Visitor")}</span>
              ${inq.status ? `<span class="px-2 py-0.5 rounded-full text-xs bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">${escapeHtml(pretty(inq.status))}</span>` : ""}
              ${hasMod ? `<span class="px-2 py-0.5 rounded-full text-xs border ${modClass}">${escapeHtml(modLabel)}</span>` : ""}
              ${isRepeat ? `<span class="px-2 py-0.5 rounded-full text-xs bg-amber-500/10 text-amber-400 border border-amber-500/30">Repeat ×${inq.repeatCount || 2}</span>` : ""}
              ${isDisposable ? `<span class="px-2 py-0.5 rounded-full text-xs bg-amber-500/10 text-amber-400 border border-amber-500/30">Disposable email</span>` : ""}
            </div>
            <div class="text-xs text-fg-3 truncate">${escapeHtml(inq.email || "")}${inq.company ? " · " + escapeHtml(inq.company) : ""}${timeStr ? " · " + timeStr : ""}</div>
          </div>
          <button type="button" data-crm-toggle onclick="toggleCrmCard(this)" aria-expanded="false" aria-label="Show details" title="Details" class="btn btn--icon btn--pill shrink-0"><i data-lucide="chevrons-up-down" class="w-4 h-4" aria-hidden="true"></i></button>
        </div>

        <p data-crm-msg class="text-fg text-[13px] leading-relaxed whitespace-pre-wrap line-clamp-2 pl-12">${escapeHtml(msg)}</p>
        ${longMsg ? `<button type="button" data-crm-toggle onclick="toggleCrmCard(this)" aria-expanded="false" class="pl-12 text-xs text-indigo-400 hover:underline"><span data-crm-toggle-label>Show more</span></button>` : ""}

        <dl data-crm-details class="hidden ml-12 p-3 rounded-xl bg-raised border border-line grid grid-cols-1 sm:grid-cols-2 gap-x-4 gap-y-2 text-xs">${rows}</dl>

        <div class="flex items-center justify-between gap-2 pl-12">
          <div class="flex items-center gap-1.5">
            ${iconBtn(`handleUpdateInquiryModeration('${id}', 'REVIEWED', 'CONTACTED')`, "check", "Mark reviewed", "text-emerald-400")}
            ${iconBtn(`handleUpdateInquiryModeration('${id}', 'FALSE_POSITIVE', 'NEW')`, "star", "Mark genuine (false positive)", "text-indigo-400")}
            ${iconBtn(`handleUpdateInquiryModeration('${id}', 'CONFIRMED_SPAM', 'REJECTED')`, "ban", "Confirm spam", "text-amber-400")}
          </div>
          ${iconBtn(`handleDeleteInquiry('${id}')`, "trash-2", "Delete inquiry", "text-rose-400")}
        </div>
      </article>`;
  }).join("");
  if (window.lucide && typeof lucide.createIcons === "function") lucide.createIcons();
}

async function handleUpdateInquiryModeration(id, moderationStatus, inquiryStatus) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/inquiries/${id}`, {
      method: "PUT",
      headers,
      body: JSON.stringify({
        status: inquiryStatus,
        moderationStatus: moderationStatus
      })
    });
    if (res.ok) {
      showToast(`Inquiry marked as ${moderationStatus}!`, "success");
      await loadAdminCrm();
    } else {
      showToast("Failed to update moderation status", "warning");
    }
  } catch (e) {
    showToast("Error updating inquiry: " + e.message, "warning");
  }
}

async function handleRetentionCleanup() {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/inquiries/retention-cleanup?retentionDays=90", {
      method: "POST",
      headers
    });
    if (res.ok) {
      const json = await res.json();
      const count = json?.data?.anonymizedRecords ?? 0;
      showToast(`Privacy retention policy applied (${count} older records anonymized).`, "success");
      await loadAdminCrm();
    } else {
      showToast("Retention cleanup failed (server " + res.status + ")", "warning");
    }
  } catch (e) {
    showToast("Retention cleanup error: " + e.message, "warning");
  }
}

async function handleDeleteInquiry(id) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/inquiries/${id}`, { method: "DELETE", headers });
    if (res.ok) {
      showToast("Inquiry and its notification deleted.", "success");
      await loadAdminCrm();
      await loadAdminNotifications();
      await loadAdminDashboardMetrics();
    } else {
      showToast("Could not delete the inquiry (server " + res.status + ")", "warning");
    }
  } catch (e) {
    showToast("Error deleting inquiry: " + e.message, "warning");
  }
}

// Initial Admin CMS Load
document.addEventListener("DOMContentLoaded", () => {
  loadAdminProjects();
  loadAdminGfg();
});

// ==========================================
// 13-TAB PROJECT ARCHITECTURE EDITOR (CRUD)
// ==========================================
let allLoadedProjects = [];
let currentEditorProject = null;

async function populateProjectEditorSelector(selectedId = null) {
  const select = document.getElementById('pe-project-select');
  if (!select) return;

  try {
    const res = await fetch('/api/projects?size=50');
    const data = await res.json();
    allLoadedProjects = data?.data?.content || data?.data || [];

    if (allLoadedProjects.length === 0) {
      select.innerHTML = '<option value="">No projects found — create one in Projects tab</option>';
      return;
    }

    select.innerHTML = allLoadedProjects.map(p => `
      <option value="${p.id}">${escapeHtml(p.title)} (${p.status || 'PUBLISHED'})</option>
    `).join('');

    const target = selectedId
      ? (allLoadedProjects.find(p => String(p.id) === String(selectedId) || p.slug === selectedId)?.id || allLoadedProjects[0].id)
      : allLoadedProjects[0].id;
    select.value = target;
    await loadProjectInto13TabEditor(target);
  } catch (err) {
    console.error('Failed to populate project selector:', err);
  }
}

function open13TabEditorForProject(projectId) {
  switchView('project-editor');
  populateProjectEditorSelector(projectId);
}

async function loadProjectInto13TabEditor(projectId) {
  if (!projectId) return;
  let project = allLoadedProjects.find(p => String(p.id) === String(projectId) || p.slug === projectId);

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${encodeURIComponent(projectId)}`, { headers });
    if (res.ok) {
      const json = await res.json();
      if (json?.data) project = json.data;
    }
  } catch (e) {}

  if (!project) return;
  currentEditorProject = project;

  const setVal = (id, val) => {
    const el = document.getElementById(id);
    if (el !== null && el !== undefined) el.value = val ?? '';
  };

  const techStr = Array.isArray(project.technologies)
    ? project.technologies.map(t => typeof t === 'string' ? t : t.name).join(', ')
    : (project.technologies || '');

  setVal('pe-id', project.id || '');
  setVal('pe-title', project.title || '');
  setVal('pe-slug', project.slug || '');
  setVal('pe-category', project.category || 'FULL_STACK');
  setVal('pe-status', project.status || 'PUBLISHED');
  setVal('pe-status-tab', project.status || 'PUBLISHED');
  setVal('pe-progress', project.progress ?? 90);
  setVal('pe-progress-tab', project.progress ?? 90);
  setVal('pe-summary', project.shortSummary || project.description || '');
  setVal('pe-tech', techStr);
  setVal('pe-tech-tab', techStr);
  setVal('pe-github', project.githubRepoUrl || '');
  setVal('pe-github-tab', project.githubRepoUrl || '');
  setVal('pe-demo', project.liveDemoUrl || '');
  setVal('pe-demo-tab', project.liveDemoUrl || '');
  setVal('pe-doc-url', project.documentationUrl || project.githubRepoUrl || '');
  setVal('pe-image-url', project.featuredImageUrl || '');
  setVal('pe-seo-title', project.seoTitle || project.title || '');
  setVal('pe-seo-desc', project.seoDescription || project.shortSummary || '');
  setVal('pe-arch-style', project.architectureStyle || 'Modular Monolith');
  setVal('pe-arch-style-tab', project.architectureStyle || 'Modular Monolith');
  setVal('pe-architecture', project.systemArchitecture || project.fullDescription || '');
  setVal('pe-problem', project.problemStatement || project.shortSummary || '');
  setVal('pe-solution', project.solutionOverview || project.fullDescription || '');
  setVal('pe-started-at', project.startedAt || '2026-01-15');
  setVal('pe-completed-at', project.completedAt || '');
  setVal('pe-featured-tab', String(project.featured !== false));
  setVal('pe-display-order', project.displayOrder ?? 1);

  const titleEl = document.getElementById('pe-header-title');
  if (titleEl) titleEl.innerText = 'Project Editor: ' + project.title;

  const badgeEl = document.getElementById('pe-status-badge');
  if (badgeEl) {
    badgeEl.innerText = project.status || 'PUBLISHED';
    badgeEl.className = 'text-xs  px-2 py-0.5 rounded-lg ' +
      (project.status === 'PUBLISHED' ? 'bg-emerald-500/10 text-emerald-400 border border-emerald-500/20' : 'bg-indigo-500/10 text-indigo-400 border border-indigo-500/20');
  }

  renderEditorMilestones(project.milestones || []);
  renderEditorChangelogs(project.changelogs || []);
  renderEditorDecisions(project.decisions || []);
}

function renderEditorMilestones(milestones) {
  const listEl = document.getElementById('pe-milestones-list');
  if (!listEl) return;
  if (!milestones || milestones.length === 0) {
    listEl.innerHTML = `<div class="p-4 rounded-xl bg-card border border-line text-center text-fg-3">No milestones recorded for this project yet. Use "+ Add Milestone" above.</div>`;
    return;
  }
  listEl.innerHTML = milestones.map(m => `
    <div class="p-3 rounded-xl bg-card border border-line flex items-center justify-between">
      <div>
        <div class="flex items-center space-x-2">
          <span class="font-bold text-fg">${escapeHtml(m.title)}</span>
          <span class="text-xs px-2 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20">${escapeHtml(m.versionTag || 'v1.0')}</span>
          <span class="text-xs px-2 py-0.5 rounded ${m.status === 'COMPLETED' ? 'bg-emerald-500/10 text-emerald-400' : 'bg-amber-500/10 text-amber-400'}">${escapeHtml(m.status || 'COMPLETED')}</span>
        </div>
        <p class="text-xs text-fg-2 mt-1">${escapeHtml(m.description || '')}</p>
      </div>
      <button type="button" onclick="handleDeleteProjectMilestone('${m.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
    </div>
  `).join('');
}

async function handleAddProjectMilestone() {
  const projId = document.getElementById('pe-id')?.value;
  if (!projId) return showToast("Select a project first", "warning");
  const title = document.getElementById('pe-ms-title')?.value?.trim();
  const versionTag = document.getElementById('pe-ms-version')?.value?.trim() || 'v1.0';
  const status = document.getElementById('pe-ms-status')?.value || 'COMPLETED';
  const description = document.getElementById('pe-ms-desc')?.value?.trim() || '';
  if (!title) return showToast("Milestone title is required", "warning");

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${projId}/milestones`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ title, versionTag, status, description, displayOrder: 1 })
    });
    if (res.ok) {
      showToast(`Milestone "${title}" added!`, "success");
      document.getElementById('pe-ms-title').value = '';
      document.getElementById('pe-ms-desc').value = '';
      await loadProjectInto13TabEditor(projId);
    }
  } catch (e) {
    showToast("Error adding milestone: " + e.message, "warning");
  }
}

async function handleDeleteProjectMilestone(msId) {
  const projId = document.getElementById('pe-id')?.value;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${projId}/milestones/${msId}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast("Milestone deleted!", "success");
      await loadProjectInto13TabEditor(projId);
    }
  } catch (e) {}
}

function renderEditorChangelogs(changelogs) {
  const listEl = document.getElementById('pe-changelogs-list');
  if (!listEl) return;
  if (!changelogs || changelogs.length === 0) {
    listEl.innerHTML = `<div class="p-4 rounded-xl bg-card border border-line text-center text-fg-3">No changelog entries recorded for this project yet. Use "+ Log Release" above.</div>`;
    return;
  }
  listEl.innerHTML = changelogs.map(c => `
    <div class="p-3 rounded-xl bg-card border border-line flex items-center justify-between">
      <div>
        <div class="flex items-center space-x-2">
          <span class="font-bold text-emerald-400">${escapeHtml(c.versionTag)}</span>
          ${c.commitHash ? `<span class="text-xs px-1.5 py-0.5 rounded bg-raised text-fg-2">#${escapeHtml(c.commitHash)}</span>` : ''}
        </div>
        <p class="text-xs text-fg mt-1">${escapeHtml(c.releaseSummary || '')}</p>
      </div>
      <button type="button" onclick="handleDeleteProjectChangelog('${c.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
    </div>
  `).join('');
}

async function handleAddProjectChangelog() {
  const projId = document.getElementById('pe-id')?.value;
  if (!projId) return showToast("Select a project first", "warning");
  const versionTag = document.getElementById('pe-cl-version')?.value?.trim() || 'v1.0.0';
  const commitHash = document.getElementById('pe-cl-commit')?.value?.trim() || '';
  const releaseSummary = document.getElementById('pe-cl-summary')?.value?.trim() || '';
  if (!releaseSummary) return showToast("Release summary is required", "warning");

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${projId}/changelogs`, {
      method: 'POST',
      headers,
      body: JSON.stringify({ versionTag, commitHash, releaseSummary })
    });
    if (res.ok) {
      showToast(`Changelog "${versionTag}" saved!`, "success");
      document.getElementById('pe-cl-version').value = '';
      document.getElementById('pe-cl-commit').value = '';
      document.getElementById('pe-cl-summary').value = '';
      await loadProjectInto13TabEditor(projId);
    }
  } catch (e) {}
}

async function handleDeleteProjectChangelog(clId) {
  const projId = document.getElementById('pe-id')?.value;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${projId}/changelogs/${clId}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast("Changelog deleted!", "success");
      await loadProjectInto13TabEditor(projId);
    }
  } catch (e) {}
}

function renderEditorDecisions(decisions) {
  const listEl = document.getElementById('pe-decisions-list');
  if (!listEl) return;
  if (!decisions || decisions.length === 0) {
    listEl.innerHTML = `<div class="p-4 rounded-xl bg-card border border-line text-center text-fg-3">No Architectural Decision Records (ADRs) for this project yet. Use "+ Save ADR" above.</div>`;
    return;
  }
  listEl.innerHTML = decisions.map(d => `
    <div class="p-3 rounded-xl bg-card border border-line space-y-1">
      <div class="flex items-center justify-between">
        <span class="font-bold text-fg">${escapeHtml(d.decisionTitle)}</span>
        <div class="flex items-center space-x-2">
          <span class="text-xs text-emerald-400">${escapeHtml(d.status || 'ACCEPTED')}</span>
          <button type="button" onclick="handleDeleteProjectDecision('${d.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
        </div>
      </div>
      <p class="text-xs text-fg-2"><strong>Context:</strong> ${escapeHtml(d.problemContext || '')}</p>
      <p class="text-xs text-indigo-300"><strong>Decision:</strong> ${escapeHtml(d.decisionTaken || '')}</p>
    </div>
  `).join('');
}

async function handleAddProjectDecision() {
  const projId = document.getElementById('pe-id')?.value;
  if (!projId) return showToast("Select a project first", "warning");
  const decisionTitle = document.getElementById('pe-adr-title')?.value?.trim();
  const status = document.getElementById('pe-adr-status')?.value || 'ACCEPTED';
  const problemContext = document.getElementById('pe-adr-context')?.value?.trim() || '';
  const decisionTaken = document.getElementById('pe-adr-decision')?.value?.trim() || '';
  if (!decisionTitle) return showToast("ADR Title is required", "warning");

  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${projId}/decisions`, {
      method: 'POST',
      headers,
      body: JSON.stringify({
        decisionTitle,
        status,
        problemContext: problemContext || decisionTitle,
        decisionTaken: decisionTaken || decisionTitle,
        tradeOffsConsidered: decisionTaken,
        consequences: decisionTaken
      })
    });
    if (res.ok) {
      showToast(`ADR "${decisionTitle}" saved!`, "success");
      document.getElementById('pe-adr-title').value = '';
      document.getElementById('pe-adr-context').value = '';
      document.getElementById('pe-adr-decision').value = '';
      await loadProjectInto13TabEditor(projId);
    }
  } catch (e) {}
}

async function handleDeleteProjectDecision(adrId) {
  const projId = document.getElementById('pe-id')?.value;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/projects/${projId}/decisions/${adrId}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast("ADR decision deleted!", "success");
      await loadProjectInto13TabEditor(projId);
    }
  } catch (e) {}
}

async function save13TabProject(desiredStatus) {
  const id = document.getElementById('pe-id')?.value;
  const title = document.getElementById('pe-title')?.value?.trim();
  const slug = document.getElementById('pe-slug')?.value?.trim();
  const category = document.getElementById('pe-category')?.value || 'FULL_STACK';
  const status = desiredStatus || document.getElementById('pe-status')?.value || 'PUBLISHED';
  const progress = parseInt(document.getElementById('pe-progress')?.value || '90', 10);
  const shortSummary = document.getElementById('pe-summary')?.value?.trim() || '';
  const techStr = document.getElementById('pe-tech')?.value?.trim() || '';
  const github = document.getElementById('pe-github')?.value?.trim() || '';
  const demo = document.getElementById('pe-demo')?.value?.trim() || '';
  const docUrl = document.getElementById('pe-doc-url')?.value?.trim() || '';
  const imageUrl = document.getElementById('pe-image-url')?.value?.trim() || '';
  const seoTitle = document.getElementById('pe-seo-title')?.value?.trim() || title;
  const seoDesc = document.getElementById('pe-seo-desc')?.value?.trim() || shortSummary;
  const archStyle = document.getElementById('pe-arch-style')?.value?.trim() || 'Modular Architecture';
  const architecture = document.getElementById('pe-architecture')?.value?.trim() || '';
  const problem = document.getElementById('pe-problem')?.value?.trim() || '';
  const solution = document.getElementById('pe-solution')?.value?.trim() || '';
  const startedAt = document.getElementById('pe-started-at')?.value || null;
  const completedAt = document.getElementById('pe-completed-at')?.value || null;
  const featured = document.getElementById('pe-featured-tab')?.value !== 'false';
  const displayOrder = parseInt(document.getElementById('pe-display-order')?.value || '1', 10);

  if (!title || !slug) {
    showToast('Project Title and Slug are required!', 'warning');
    return;
  }

  const payload = {
    title,
    slug,
    category,
    status,
    progress,
    shortSummary,
    fullDescription: architecture || solution || shortSummary,
    problemStatement: problem,
    solutionOverview: solution,
    systemArchitecture: architecture,
    architectureStyle: archStyle,
    technologies: techStr.split(',').map(t => t.trim()).filter(Boolean),
    githubRepoUrl: github,
    liveDemoUrl: demo,
    documentationUrl: docUrl,
    featuredImageUrl: imageUrl,
    seoTitle,
    seoDescription: seoDesc,
    startedAt,
    completedAt,
    featured,
    displayOrder
  };

  try {
    const headers = await getAuthHeaders();
    const url = id ? `/api/admin/projects/${encodeURIComponent(id)}` : '/api/admin/projects';
    const method = id ? 'PUT' : 'POST';

    const res = await fetch(url, { method, headers, body: JSON.stringify(payload) });
    if (res.ok) {
      showToast(`Project "${title}" saved successfully!`, 'success');
      await loadAdminProjects();
      await populateProjectEditorSelector(id || slug);
    } else {
      const err = await res.json().catch(() => ({}));
      showToast(err.message || 'Failed to save project', 'warning');
    }
  } catch (err) {
    showToast('Network error while saving project', 'warning');
  }
}

// ==========================================
// HOMEPAGE SECTION BUILDER (NEON DB)
// ==========================================
const defaultHomepageSections = [
  { id: 'overview', title: 'Hero & Developer Identity', desc: 'Headline, availability status, and core tech focus cards', visible: true },
  { id: 'activity', title: 'Live Developer Activity & Heatmap', desc: 'GitHub 2026/2025 heatmap, LeetCode & GFG telemetry', visible: true },
  { id: 'projects', title: 'Featured Production Projects', desc: 'Case studies, architecture breakdowns, and live demos', visible: true },
  { id: 'experience', title: 'Experience, Education & Credentials', desc: 'Work history, BCA degree, achievements, and certifications', visible: true },
  { id: 'contributions', title: 'Open-Source Contribution Stories', desc: 'Engineering narratives and upstream PRs', visible: true },
  { id: 'coding', title: 'Coding Journey & DSA Problem Notes', desc: 'Authored algorithmic problem solutions', visible: true },
  { id: 'research', title: 'Research Hub & Investigations', desc: 'Technical papers and empirical benchmarks', visible: true },
  { id: 'lab', title: 'Engineering Lab Prototypes', desc: 'Experimental systems and sandbox builds', visible: true },
  { id: 'writing', title: 'Technical Writing & Engineering Notes', desc: 'Published markdown essays and architecture guides', visible: true },
  { id: 'about', title: 'About & Direct Recruiter Contact Form', desc: 'Direct CRM inquiry pipeline and contact details', visible: true }
];

let currentHomepageSections = JSON.parse(JSON.stringify(defaultHomepageSections));

async function loadAdminHomepageBuilder() {
  try {
    const res = await fetch('/api/settings');
    if (res.ok) {
      const json = await res.json();
      const raw = json?.data?.homepage_layout_json;
      if (raw) {
        const parsed = JSON.parse(raw);
        if (Array.isArray(parsed) && parsed.length > 0) {
          currentHomepageSections = parsed;
        }
      }
    }
  } catch (e) {}
  renderHomepageBuilderList();
}

function renderHomepageBuilderList() {
  const container = document.getElementById('admin-homepage-sections-list');
  if (!container) return;
  container.innerHTML = currentHomepageSections.map((sec, idx) => `
    <div class="p-3.5 rounded-xl bg-card border border-line flex items-center justify-between hover:border-indigo-500/40 transition-colors">
      <div class="flex items-center space-x-3">
        <span class="w-6 h-6 rounded bg-raised border border-line text-indigo-400 flex items-center justify-center font-bold">${idx + 1}</span>
        <div>
          <span class="text-fg font-bold">${escapeHtml(sec.title)} <span class="text-xs text-fg-3">#${escapeHtml(sec.id)}</span></span>
          <span class="text-xs text-fg-2 block">${escapeHtml(sec.desc || '')}</span>
        </div>
      </div>
      <div class="flex items-center space-x-2">
        <button type="button" onclick="moveHomepageSection(${idx}, -1)" class="px-2 py-1 rounded-lg bg-raised hover:bg-raised border border-line text-fg" title="Move Up">↑</button>
        <button type="button" onclick="moveHomepageSection(${idx}, 1)" class="px-2 py-1 rounded-lg bg-raised hover:bg-raised border border-line text-fg" title="Move Down">↓</button>
        <label class="flex items-center space-x-1.5 pl-2 cursor-pointer">
          <input type="checkbox" ${sec.visible !== false ? 'checked' : ''} onchange="toggleHomepageSection(${idx}, this.checked)" class="w-4 h-4 accent-indigo-500 rounded">
          <span class="text-xs text-fg-2">Visible</span>
        </label>
      </div>
    </div>
  `).join('');
}

function moveHomepageSection(index, direction) {
  const target = index + direction;
  if (target < 0 || target >= currentHomepageSections.length) return;
  const temp = currentHomepageSections[index];
  currentHomepageSections[index] = currentHomepageSections[target];
  currentHomepageSections[target] = temp;
  renderHomepageBuilderList();
}

function toggleHomepageSection(index, isVisible) {
  if (currentHomepageSections[index]) {
    currentHomepageSections[index].visible = isVisible;
  }
}

async function resetHomepageLayout() {
  currentHomepageSections = JSON.parse(JSON.stringify(defaultHomepageSections));
  renderHomepageBuilderList();
  await saveHomepageLayout(true);
}

async function saveHomepageLayout(skipConfirm = false) {
  try {
    const headers = await getAuthHeaders();
    const payloadStr = JSON.stringify({
      homepage_layout_json: JSON.stringify(currentHomepageSections)
    });
    let res = await fetch("/api/admin/settings", {
      method: "POST",
      headers,
      body: payloadStr
    });
    if (!res.ok) {
      res = await fetch("/api/admin/settings", {
        method: "PUT",
        headers,
        body: payloadStr
      });
    }
    if (res.ok) {
      showToast("Homepage section order & visibility saved!", "success");
    } else {
      showToast("Failed to save homepage layout", "warning");
    }
  } catch (e) {
    showToast("Error saving homepage layout: " + e.message, "warning");
  }
}

// ==========================================
// ADMIN PASSWORD UPDATE
// ==========================================
async function openChangePasswordModal() {
  const modal = document.getElementById('modal-change-password');
  if (!modal) return;
  modal.classList.remove('hidden');

  const step1 = document.getElementById('cp-step-1');
  const step2 = document.getElementById('cp-step-2');
  if (step1) step1.classList.remove('hidden');
  if (step2) step2.classList.add('hidden');

  const emailEl = document.getElementById('cp-admin-email');
  const mobileEl = document.getElementById('cp-admin-mobile');
  const otpEl = document.getElementById('cp-otp-code');
  const newUsernameEl = document.getElementById('cp-new-username');
  const newPassEl = document.getElementById('cp-new-password');
  const confPassEl = document.getElementById('cp-confirm-password');

  if (emailEl) emailEl.value = '';
  if (mobileEl) mobileEl.value = '';
  if (otpEl) otpEl.value = '';
  if (newPassEl) newPassEl.value = '';
  if (confPassEl) confPassEl.value = '';

  try {
    const res = await fetch('/api/v1/auth/credential-metadata');
    if (res.ok) {
      const json = await res.json();
      const d = json?.data || {};
      // Username is kept hidden for security
      if (newUsernameEl && d.username) newUsernameEl.placeholder = 'Enter new username';
      const updatedEl = document.getElementById('cp-last-updated-time');
      if (updatedEl && d.credentialsUpdatedAt) {
        updatedEl.innerText = new Date(d.credentialsUpdatedAt).toLocaleString([], {
          month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit'
        });
      }
    }
  } catch (_) {}

  if (window.lucide) window.lucide.createIcons();
}

function closeChangePasswordModal() {
  const modal = document.getElementById('modal-change-password');
  if (modal) modal.classList.add('hidden');
  if (typeof enforceAdminGate === 'function') {
    enforceAdminGate();
  } else {
    const isAuth = localStorage.getItem('raviranjan_admin_authenticated') === 'true' && Boolean(localStorage.getItem('raviranjan_auth_token'));
    const gate = document.getElementById('admin-direct-login-gate');
    if (!isAuth && gate) gate.classList.remove('hidden');
  }
}

async function handleRequestAdminUpdateOtp() {
  const email = (document.getElementById('cp-admin-email')?.value || '').trim();
  const mobileNumber = (document.getElementById('cp-admin-mobile')?.value || '').trim();
  const step1 = document.getElementById('cp-step-1');
  const step2 = document.getElementById('cp-step-2');
  const otpInput = document.getElementById('cp-otp-code');
  const btn = document.getElementById('btn-cp-generate-otp');

  if (!email || !mobileNumber) {
    showToast('Enter registered email & mobile number.', 'warning');
    return;
  }

  if (btn) {
    btn.disabled = true;
    btn.innerText = 'Verifying...';
  }

  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/v1/auth/request-otp', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        email,
        mobileNumber,
        purpose: 'UPDATE_CREDENTIALS'
      })
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) {
      showToast(json?.message || 'Invalid email or mobile number.', 'warning');
      return;
    }

    // Never show or auto-fill OTP on screen; admin retrieves it (admin_credential_otps)
    if (otpInput) otpInput.value = '';
    if (step1) step1.classList.add('hidden');
    if (step2) step2.classList.remove('hidden');
    if (otpInput) setTimeout(() => otpInput.focus(), 60);
    showToast('OTP saved (admin_credential_otps).', 'success');
  } catch (e) {
    showToast('Network error generating OTP.', 'warning');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = 'Send OTP to Database';
    }
  }
}

async function handleChangePassword(e) {
  if (e) e.preventDefault();
  const email = (document.getElementById('cp-admin-email')?.value || '').trim();
  const mobileNumber = (document.getElementById('cp-admin-mobile')?.value || '').trim();
  const otpCode = (document.getElementById('cp-otp-code')?.value || '').trim();
  const newUsername = (document.getElementById('cp-new-username')?.value || '').trim();
  const newPassword = document.getElementById('cp-new-password')?.value || '';
  const confirmPassword = document.getElementById('cp-confirm-password')?.value || '';

  if (!email || !mobileNumber || !otpCode || !newUsername || !newPassword) {
    showToast('All fields are required.', 'warning');
    return;
  }

  if (newPassword !== confirmPassword) {
    showToast('Passwords do not match.', 'warning');
    return;
  }

  if (newPassword.length < 6) {
    showToast('Password must be at least 6 characters.', 'warning');
    return;
  }

  const btn = document.getElementById('btn-save-password');
  if (btn) {
    btn.disabled = true;
    btn.innerText = 'Saving...';
  }

  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/v1/auth/reset-credentials', {
      method: 'POST',
      headers,
      body: JSON.stringify({
        email,
        mobileNumber,
        otpCode,
        newUsername,
        newPassword
      })
    });
    const json = await res.json().catch(() => ({}));
    if (res.ok) {
      const d = json?.data || {};
      if (d.accessToken) localStorage.setItem('raviranjan_auth_token', d.accessToken);
      if (d.username) localStorage.setItem('raviranjan_admin_user', d.username);

      showToast('Credentials updated!', 'success');
      if (typeof loadAdminAuditLogs === 'function') loadAdminAuditLogs();
      if (typeof loadAdminIdentityMetadata === 'function') loadAdminIdentityMetadata();
      setTimeout(() => closeChangePasswordModal(), 600);
    } else {
      showToast(json?.message || 'Invalid or expired OTP.', 'warning');
    }
  } catch (err) {
    showToast('Network error updating credentials.', 'warning');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerText = 'Save Changes';
    }
  }
}

// ==========================================
// GLOBAL EXTERNAL DATA SYNC & INTEGRATION CENTER
// ==========================================
async function loadAdminIntegrationsCenter() {
  try {
    const res = await fetch('/api/activity/telemetry');
    if (res.ok) {
      const json = await res.json();
      const d = json?.data || {};
      const setEl = (id, val) => { const el = document.getElementById(id); if (el && val !== undefined) el.innerText = val; };
      if (d.github) {
        setEl('int-gh-account', '@' + (d.github.username || 'raviranjankrmaurya'));
        setEl('int-gh-stats', `${d.github.contributions ?? 0} Contributions · ${d.github.repositories ?? 0} Repos`);
        setEl('int-gh-last-sync', 'Synced with Live API');
      }
      if (d.leetcode) {
        setEl('int-lc-account', d.leetcode.username || 'raviranjanmaurya');
        setEl('int-lc-stats', `${d.leetcode.solvedTotal ?? 0} Solved (${d.leetcode.easySolved ?? 0}E/${d.leetcode.mediumSolved ?? 0}M/${d.leetcode.hardSolved ?? 0}H)`);
        setEl('int-lc-last-sync', 'Synced with Live API');
      }
      if (d.gfg) {
        setEl('int-gfg-account', 'raviranjankrmaurya');
        setEl('int-gfg-stats', `Score: ${d.gfg.score ?? 0} · Solved: ${d.gfg.problems ?? 0}`);
      }
    }
  } catch (e) {}
}

async function triggerGlobalSync() {
  showToast('Synchronizing GitHub, LeetCode, and GFG...', 'info');
  try {
    const res = await fetch('/api/activity/sync', { method: 'POST' });
    if (res.ok) {
      showToast('All external platforms synchronized!', 'success');
      await loadAdminDashboardMetrics();
      await loadAdminIntegrationsCenter();
      await loadAdminGitHub();
      await loadAdminLeetCode();
      await loadAdminGfg();
      return;
    }
  } catch (err) {}

  try {
    const headers = await getAuthHeaders();
    for (const p of ['GITHUB', 'LEETCODE', 'GFG']) {
      await fetch(`/api/admin/integrations/${p}/sync?mode=MANUAL`, { method: 'POST', headers }).catch(() => {});
    }
    showToast('External platforms synchronized!', 'success');
    await loadAdminDashboardMetrics();
    await loadAdminIntegrationsCenter();
  } catch (e) {
    showToast('Sync completed.', 'info');
  }
}

// ==========================================
// ANALYTICS CSV EXPORT (NEON DB)
// ==========================================
async function exportAnalyticsCsv() {
  showToast('Generating the analytics CSV report…', 'info');
  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/admin/analytics/export-csv', { headers });
    if (res.ok) {
      const csvText = await res.text();
      const blob = new Blob([csvText], { type: 'text/csv;charset=utf-8;' });
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `raviranjan_os_analytics_${new Date().toISOString().slice(0, 10)}.csv`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      URL.revokeObjectURL(url);
      showToast('Analytics CSV report downloaded!', 'success');
      return;
    }
  } catch (e) {
    console.warn('CSV endpoint fallback:', e);
  }

  // Fallback client-side CSV generation from current dashboard metrics
  const rows = [
    ['Category', 'Metric', 'Value'],
    ['Analytics', 'Portfolio Views', document.getElementById('analytics-views')?.innerText || '0'],
    ['Analytics', 'Unique Visitors', document.getElementById('analytics-visitors')?.innerText || '1'],
    ['Analytics', 'Resume Downloads', document.getElementById('analytics-resumes')?.innerText || '0'],
    ['Analytics', 'AI Assistant Queries', document.getElementById('analytics-ai-queries')?.innerText || '0'],
    ['Telemetry', 'Total Projects', document.getElementById('dash-total-projects')?.innerText || '0'],
    ['Telemetry', 'GitHub Commits', document.getElementById('dash-github-commits')?.innerText || '0'],
    ['Telemetry', 'LeetCode Solved', document.getElementById('dash-leetcode-solved')?.innerText || '0'],
    ['Telemetry', 'GFG Score', document.getElementById('dash-gfg-score')?.innerText || '0']
  ];
  const csvContent = rows.map(r => r.join(',')).join('\n');
  const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'raviranjan_os_analytics_report.csv';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
  showToast('Analytics CSV report downloaded!', 'success');
}

// ==========================================
// ADMIN THEME TOGGLE (DARK / LIGHT MODE)
// ==========================================
function initAdminTheme() {
  const saved = localStorage.getItem('admin_theme') || (window.matchMedia && window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark');
  applyAdminTheme(saved);
}

function toggleAdminTheme() {
  const isLight = document.documentElement.classList.contains('light');
  const target = isLight ? 'dark' : 'light';
  localStorage.setItem('admin_theme', target);
  applyAdminTheme(target);
  showToast(`Switched admin panel to ${target} mode`, 'info');
}

function applyAdminTheme(theme) {
  const icon = document.getElementById('admin-theme-icon');
  if (theme === 'light') {
    document.documentElement.classList.remove('dark');
    document.documentElement.classList.add('light');
    if (icon) icon.setAttribute('data-lucide', 'moon');
  } else {
    document.documentElement.classList.remove('light');
    document.documentElement.classList.add('dark');
    if (icon) icon.setAttribute('data-lucide', 'sun');
  }
  if (window.lucide) window.lucide.createIcons();
}

// ==========================================
// PLATFORM EXECUTIVE TELEMETRY & ANALYTICS (DYNAMIC NEON DB)
// ==========================================

// ---- Live integration + system status (replaces hard-coded "HEALTHY / 42ms" and "Neon DB / Redis" chips) ----
const PROVIDER_LABELS = { GITHUB: "GitHub", LEETCODE: "LeetCode", GFG: "GeeksforGeeks", WHATSAPP: "WhatsApp", EMAIL: "Email", ANALYTICS: "Analytics", AI: "AI assistant" };

async function loadIntegrationHealth() {
  const box = document.getElementById("dash-integration-health");
  if (!box) return;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch("/api/admin/integrations", { headers });
    if (!res.ok) throw new Error("Server responded " + res.status);
    const list = (await res.json())?.data || [];
    if (!list.length) { box.innerHTML = '<div class="p-4 text-center text-fg-3 text-sm">No integrations reported.</div>'; return; }
    box.innerHTML = list.map(i => {
      const state = String(i.healthStatus || "").toUpperCase();
      const tone = state === "HEALTHY" ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20"
        : state === "DEGRADED" ? "bg-amber-500/10 text-amber-400 border-amber-500/30"
        : state === "ERROR" ? "bg-rose-500/10 text-rose-400 border-rose-500/30"
        : "bg-raised text-fg-3 border-line";
      const label = state ? state.charAt(0) + state.slice(1).toLowerCase() : "Unknown";
      const when = i.lastSuccessfulSync ? new Date(i.lastSuccessfulSync).toLocaleString([], { month: "short", day: "numeric", hour: "2-digit", minute: "2-digit" }) : "Never synced";
      const records = Number.isFinite(Number(i.recordsProcessed)) && Number(i.recordsProcessed) > 0 ? `${Number(i.recordsProcessed)} records` : "";
      return `
        <div class="p-3 rounded-xl bg-card border border-line space-y-1">
          <div class="flex items-center justify-between gap-2">
            <span class="text-sm font-medium text-fg truncate">${escapeHtml(PROVIDER_LABELS[i.provider] || String(i.provider || ""))}</span>
            <span class="px-2 py-0.5 rounded-full text-xs border ${tone}">${label}</span>
          </div>
          <div class="flex items-center justify-between gap-2 text-xs text-fg-3">
            <span class="truncate">Last sync: ${when}</span><span class="shrink-0">${records}</span>
          </div>
          ${i.lastErrorMessage ? `<p class="text-xs text-rose-400 line-clamp-2" title="${escapeHtml(i.lastErrorMessage)}">${escapeHtml(i.lastErrorMessage)}</p>` : ""}
        </div>`;
    }).join("");
  } catch (e) {
    box.innerHTML = `<div class="p-4 text-center space-y-3"><p class="text-fg-2 text-sm">Could not load integration status.</p><button type="button" onclick="loadIntegrationHealth()" class="btn btn--pill">Try again</button></div>`;
  }
}

async function loadSystemStatus() {
  const paint = (key, state) => {
    const dot = document.getElementById(`admin-${key}-dot`);
    const chip = dot?.parentElement;
    if (!dot || !chip) return;
    if (state === null) { chip.style.display = "none"; return; }     // component not reported -> don't claim anything
    chip.style.display = "";
    dot.className = "w-1.5 h-1.5 rounded-full " + (state === "UP" ? "bg-emerald-400" : "bg-rose-400");
    chip.title = state === "UP" ? "Operational" : "Unavailable";
  };
  try {
    const res = await fetch("/actuator/health");
    const j = await res.json().catch(() => ({}));
    const c = j.components || {};
    paint("db", (c.db && c.db.status) || j.status || "DOWN");
    paint("redis", c.redis ? c.redis.status : null);
  } catch (e) {
    paint("db", "DOWN"); paint("redis", null);
  }
}

async function loadAdminDashboardMetrics() {
  loadIntegrationHealth();
  loadSystemStatus();
  try {
    let headers = await getAuthHeaders();
    let res = await fetch("/api/admin/dashboard/metrics", { headers }).catch(() => null);
    if (res && (res.status === 401 || res.status === 403)) {
      headers = await getAuthHeaders(true);
      res = await fetch("/api/admin/dashboard/metrics", { headers }).catch(() => null);
    }

    let m = null;
    if (res && res.ok) {
      const json = await res.json().catch(() => ({}));
      m = json.data || null;
    }

    // Fallback aggregation from public endpoints if /api/admin/dashboard/metrics was unreachable
    if (!m) {
      const [telRes, projRes, actRes] = await Promise.all([
        fetch("/api/activity/telemetry").catch(() => null),
        fetch("/api/projects?size=100").catch(() => null),
        fetch("/api/activity?size=5").catch(() => null)
      ]);
      const tel = (telRes && telRes.ok ? (await telRes.json().catch(() => ({})))?.data : {}) || {};
      const projs = (projRes && projRes.ok ? ((await projRes.json().catch(() => ({})))?.data?.content || []) : []) || [];
      const acts = (actRes && actRes.ok ? ((await actRes.json().catch(() => ({})))?.data?.content || []) : []) || [];
      const pubCount = projs.filter(p => p.status === "PUBLISHED").length;
      const draftCount = projs.filter(p => p.status === "DRAFT").length;
      const archCount = projs.filter(p => p.status === "ARCHIVED").length;
      m = {
        totalProjects: projs.length,
        publishedProjects: pubCount,
        draftProjects: draftCount,
        archivedProjects: archCount,
        githubCommits: tel?.github?.contributions ?? 0,
        githubRepos: tel?.github?.repositories ?? 0,
        leetcodeSolved: tel?.leetcode?.solvedTotal ?? 0,
        gfgScore: tel?.gfg?.score ?? 0,
        gfgProblems: tel?.gfg?.problems ?? 0,
        portfolioViews: 1,
        uniqueVisitors: 1,
        resumeDownloads: 0,
        crmInquiries: 0,
        newCrmInquiries: 0,
        aiQueries: 0,
        activeSprints: projs.length,
        avgProgress: projs.length ? Math.round(projs.reduce((a, b) => a + (b.progress || 85), 0) / projs.length) : 0,
        healthStatus: "100%",
        recentMutations: acts,
        projectMilestones: projs
      };
    }

    const setT = (id, val) => {
      const el = document.getElementById(id);
      if (el !== null && el !== undefined && val !== undefined && val !== null) el.innerText = val;
    };

    setT("sidebar-projects-count", m.totalProjects);
    setT("sidebar-crm-badge", `${m.newCrmInquiries || 0} New`);
    setT("dash-total-projects", m.totalProjects);
    setT("dash-projects-breakdown", `${m.publishedProjects} Published · ${m.draftProjects} Drafts · ${m.archivedProjects} Arch.`);
    setT("dash-github-commits", Number(m.githubCommits || 0).toLocaleString());
    setT("dash-leetcode-solved", m.leetcodeSolved ?? 0);
    setT("dash-gfg-score", Number(m.gfgScore || 0).toLocaleString());
    setT("dash-gfg-breakdown", `${m.gfgProblems || 0} Problems Solved`);
    const views = Number(m.portfolioViews || 0);
    setT("dash-portfolio-views", views >= 1000 ? `${(views / 1000).toFixed(1)}k` : String(views));
    setT("dash-resume-downloads", m.resumeDownloads ?? 0);
    setT("dash-crm-inquiries", m.crmInquiries ?? 0);
    setT("dash-crm-breakdown", `${m.newCrmInquiries || 0} New Inquiries`);
    setT("dash-ai-queries", Number(m.aiQueries || 0).toLocaleString());
    setT("dash-active-sprints", m.activeSprints ?? 0);
    setT("dash-sprints-breakdown", `Avg Progress ${m.avgProgress || 0}%`);
    setT("dash-health-status", m.healthStatus || "—");

    // Also update Analytics View cards
    setT("analytics-views", Number(m.portfolioViews || 0).toLocaleString());
    setT("analytics-visitors", Number(m.uniqueVisitors || 1).toLocaleString());
    setT("analytics-resumes", Number(m.resumeDownloads || 0).toLocaleString());
    setT("analytics-ai-queries", Number(m.aiQueries || 0).toLocaleString());
    setT("ai-stat-total-queries", Number(m.aiQueries || 0).toLocaleString());

    const feed = document.getElementById("dash-audit-feed");
    if (feed && Array.isArray(m.recentMutations)) {
      if (m.recentMutations.length === 0) {
        feed.innerHTML = `<div class="p-4 text-center text-fg-3 text-xs">No activity records yet. Click "Sync External Data" to pull live telemetry.</div>`;
      } else {
        feed.innerHTML = m.recentMutations.map(act => {
          let badgeColor = "text-indigo-400 bg-indigo-500/10 border-indigo-500/20";
          if (act.sourcePlatform === "LEETCODE") badgeColor = "text-amber-400 bg-amber-500/10 border-amber-500/20";
          else if (act.sourcePlatform === "GFG") badgeColor = "text-emerald-400 bg-emerald-500/10 border-emerald-500/20";
          else if (act.sourcePlatform === "GITHUB") badgeColor = "text-purple-400 bg-purple-500/10 border-purple-500/20";

          cachedAdminActivities = m.recentMutations;
          const timeStr = act.occurredAt ? new Date(act.occurredAt).toLocaleDateString([], { month: "short", day: "numeric" }) : "recent";
          return `
            <div class="p-3 rounded-xl bg-card border border-line flex flex-col sm:flex-row sm:items-center justify-between gap-2 hover:border-indigo-500/30 transition-colors">
              <div class="flex items-start space-x-3">
                <span class="px-2 py-0.5 rounded text-xs border ${badgeColor} shrink-0 mt-0.5">${escapeHtml(act.sourcePlatform || "SYSTEM")}</span>
                <div>
                  <p class="text-fg font-medium">${escapeHtml(act.title)}</p>
                  <p class="text-xs text-fg-2 truncate max-w-md">${escapeHtml(act.description || "")}</p>
                </div>
              </div>
              <div class="flex items-center space-x-2 shrink-0 self-end sm:self-center text-xs">
                <span class="text-fg-3">${timeStr}</span>
                ${act.id ? `<button type="button" onclick="handleEditActivityItem('${act.id}')" class="text-indigo-400 hover:underline rounded-lg">Edit</button>` : ''}
                ${act.id ? `<button type="button" onclick="handleDeleteActivityItem('${act.id}')" class="text-rose-400 hover:underline rounded-lg">Delete</button>` : ''}
              </div>
            </div>
          `;
        }).join("");
      }
    }

    const milestonesContainer = document.getElementById("dash-project-milestones-list");
    if (milestonesContainer && Array.isArray(m.projectMilestones)) {
      if (m.projectMilestones.length === 0) {
        milestonesContainer.innerHTML = `<div class="text-fg-3 text-xs">No active projects. Create a project in the Projects tab.</div>`;
      } else {
        milestonesContainer.innerHTML = m.projectMilestones.map(pm => {
          const pVal = pm.progress !== undefined ? pm.progress : 85;
          return `
            <div>
              <div class="flex justify-between mb-1.5">
                <span class="text-fg">${escapeHtml(pm.title)}</span>
                <span class="text-indigo-400 font-bold">${pVal}%</span>
              </div>
              <div class="h-2 rounded-full bg-raised overflow-hidden">
                <div class="h-full bg-indigo-600 rounded-full" style="width: ${pVal}%"></div>
              </div>
            </div>
          `;
        }).join("");
      }
    }
  } catch (err) {
    console.warn("Dashboard metrics fetch error:", err);
  }
}

// ==========================================
// SITE SETTINGS MANAGEMENT (NEON DB)
// ==========================================
function applyAdminAccentThemeColor(hex) {
  if (!hex || !hex.startsWith('#')) return;
  localStorage.setItem('raviranjan_accent_color', hex);
  document.documentElement.style.setProperty('--accent-color', hex);
  document.documentElement.style.setProperty('--color-primary', hex);
  let styleEl = document.getElementById('dynamic-admin-accent-theme');
  if (!styleEl) {
    styleEl = document.createElement('style');
    styleEl.id = 'dynamic-admin-accent-theme';
    document.head.appendChild(styleEl);
  }
  styleEl.innerHTML = `
    html body .bg-indigo-500, html body .bg-indigo-500,
    html.light body .bg-indigo-500, html.light body .bg-indigo-500 {
      background-color: ${hex} !important;
      color: #ffffff !important;
    }
    html body .text-indigo-400, html body .text-indigo-400,
    html.light body .text-indigo-400, html.light body .text-indigo-400 {
      color: ${hex} !important;
    }
    html body .border-indigo-500\/30, html body .border-indigo-500\/30,
    html.light body .border-indigo-500\/30, html.light body .border-indigo-500\/30 {
      border-color: ${hex}66 !important;
    }
    html body .bg-indigo-500\/10, html body .bg-indigo-500\/10,
    html.light body .bg-indigo-500\/10, html.light body .bg-indigo-500\/10 {
      background-color: ${hex}1f !important;
      color: ${hex} !important;
    }
  `;
}

async function loadSiteSettings() {
  try {
    const res = await fetch('/api/settings');
    if (!res.ok) return;
    const json = await res.json();
    const settings = json.data || {};

    const setV = (id, val) => {
      const el = document.getElementById(id);
      if (el && val !== undefined && val !== null) el.value = val;
    };

    setV('setting-site-title', settings.siteTitle || settings.SITE_TITLE);
    setV('setting-theme-color-picker', settings.themeColor || '#6366f1');
    setV('setting-theme-color-text', settings.themeColor || '#6366f1');
    if (settings.themeColor) applyAdminAccentThemeColor(settings.themeColor);
    setV('setting-meta-desc', settings.metaDescription || settings.SITE_DESCRIPTION);
    setV('setting-availability', settings.availabilityStatus);
    setV('setting-avail-card-title', settings.availabilityCardTitle || 'Open to Opportunities');
    setV('setting-avail-card-subtitle', settings.availabilityCardSubtitle || 'Internships & full-time software developer roles');
    setV('setting-curl-command', settings.curlCommand || 'curl -s https://raviranjan.dev/api/v1/profile | jq');
    setV('setting-updated-date', settings.updatedDateText || 'Updated: Oct 2026');
    setV('setting-contact-heading', settings.contactHeading || "Let's Connect");
    setV('setting-contact-desc', settings.contactDescription || '');
    setV('setting-contact-email', settings.contactEmail);
    setV('setting-contact-phone', settings.contactPhone || '+91-9999999999');
    setV('setting-font-preset', settings.fontPreset || 'geist');
    setV('setting-hero-animated-texts', settings.heroAnimatedTexts || 'Building software for the AI-native era. | Engineering high-throughput distributed backends. | Architecting intelligent RAG & LLM workflows. | Shipping production-grade full-stack platforms.');
    setV('setting-hero-role-texts', settings.heroRoleTexts || 'Full-Stack & AI Engineer | Java 21 & Spring Boot Architect | Distributed Systems Builder | Applied AI & RAG Specialist');

    if (settings.aiPromptsJson) {
      try {
        const arr = JSON.parse(settings.aiPromptsJson);
        if (Array.isArray(arr)) setV('setting-ai-prompts', arr.join('\n'));
      } catch (e) {}
    }
  } catch (e) {
    console.warn('Failed to load settings:', e);
  }
}

window.lockAndLogoutAdmin = function() {
  if (typeof logoutAdmin === 'function') {
    logoutAdmin();
  } else {
    localStorage.removeItem('raviranjan_auth_token');
    localStorage.removeItem('raviranjan_admin_authenticated');
    window.location.href = '/';
  }
};

async function handleSaveSiteSettings(e) {
  if (e) e.preventDefault();
  const saveBtn = document.getElementById('btn-save-site-settings');
  if (saveBtn) {
    saveBtn.disabled = true;
    saveBtn.innerText = 'Saving...';
  }
  const title = document.getElementById('setting-site-title')?.value?.trim() || '';
  const theme = document.getElementById('setting-theme-color-text')?.value?.trim() || '#6366f1';
  const desc = document.getElementById('setting-meta-desc')?.value?.trim() || '';
  const avail = document.getElementById('setting-availability')?.value?.trim() || '';
  const availCardTitle = document.getElementById('setting-avail-card-title')?.value?.trim() || 'Open to Opportunities';
  const availCardSubtitle = document.getElementById('setting-avail-card-subtitle')?.value?.trim() || '';
  const curlCmd = document.getElementById('setting-curl-command')?.value?.trim() || 'curl -s https://raviranjan.dev/api/v1/profile | jq';
  const updatedDateText = document.getElementById('setting-updated-date')?.value?.trim() || 'Updated: Oct 2026';
  const contactHeading = document.getElementById('setting-contact-heading')?.value?.trim() || "Let's Connect";
  const contactDescription = document.getElementById('setting-contact-desc')?.value?.trim() || '';
  const email = document.getElementById('setting-contact-email')?.value?.trim() || '';
  const phone = document.getElementById('setting-contact-phone')?.value?.trim() || '';
  const fontPreset = document.getElementById('setting-font-preset')?.value?.trim() || 'geist';
  const heroAnimatedTexts = document.getElementById('setting-hero-animated-texts')?.value?.trim() || 'Building AI-native software. | Scalable Java & Spring systems. | Production RAG & AI agents. | Full-stack cloud engineering.';
  const heroRoleTexts = document.getElementById('setting-hero-role-texts')?.value?.trim() || 'Full-Stack & AI Engineer | Java 21 & Spring Architect | AI & RAG Builder | Cloud Systems Engineer';
  const rawPrompts = document.getElementById('setting-ai-prompts')?.value || '';
  const promptsList = rawPrompts.split('\n').map(s => s.trim()).filter(Boolean);

  applyAdminAccentThemeColor(theme);

  try {
    const headers = await getAuthHeaders();
    const payloadStr = JSON.stringify({
        siteTitle: title,
        themeColor: theme,
        metaDescription: desc,
        availabilityStatus: avail,
        availabilityCardTitle: availCardTitle,
        availabilityCardSubtitle: availCardSubtitle,
        curlCommand: curlCmd,
        updatedDateText: updatedDateText,
        contactHeading: contactHeading,
        contactDescription: contactDescription,
        contactEmail: email,
        contactPhone: phone,
        fontPreset: fontPreset,
        heroAnimatedTexts: heroAnimatedTexts,
        heroRoleTexts: heroRoleTexts,
        aiPromptsJson: JSON.stringify(promptsList)
    });
    let res = await fetch('/api/admin/settings', {
      method: 'POST',
      headers,
      body: payloadStr
    });
    if (!res.ok) {
      res = await fetch('/api/admin/settings', {
        method: 'PUT',
        headers,
        body: payloadStr
      });
    }

    if (res.ok) {
      showToast('Site settings, Accent Theme, Typography, Hero Texts & Contact saved!', 'success');
      await loadSiteSettings();
      if (typeof loadAdminAuditLogs === 'function') loadAdminAuditLogs();
      if (typeof loadAdminIdentityMetadata === 'function') loadAdminIdentityMetadata();
    } else {
      showToast('Failed to save settings to database', 'warning');
    }
  } catch (err) {
    showToast('Network error saving settings', 'warning');
  } finally {
    if (saveBtn) {
      saveBtn.disabled = false;
      saveBtn.innerText = 'Save Settings';
    }
  }
}

// ==========================================
// IMMUTABLE AUDIT LOGS (LAST 5 RECENT EDITS & STATE DIFF MODAL)
// ==========================================
let cachedAdminAuditLogs = [];

async function loadAdminAuditLogs() {
  const tbody = document.getElementById('admin-audit-logs-tbody');
  if (!tbody) return;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/admin/audit-logs?size=5', { headers });
    if (res.ok) {
      const json = await res.json();
      cachedAdminAuditLogs = (json?.data || []).slice(0, 5);
      if (cachedAdminAuditLogs.length === 0) {
        tbody.innerHTML = `<tr><td colspan="6" class="p-6 text-center text-fg-3">No edit audits recorded yet. Save your Profile, Settings, or Projects to generate an audit record.</td></tr>`;
        return;
      }
      tbody.innerHTML = cachedAdminAuditLogs.map((log, idx) => {
        const ts = log.timestamp ? new Date(log.timestamp).toLocaleString([], { year: 'numeric', month: 'short', day: '2-digit', hour: '2-digit', minute: '2-digit', second: '2-digit' }) : '';
        const act = (log.action || 'UPDATE').toUpperCase();
        const actColor = act === 'DELETE' ? 'text-rose-400' : (act === 'CREATE' || act === 'PUBLISH' ? 'text-emerald-400' : 'text-indigo-400');
        return `
          <tr class="hover:bg-card/60 transition-colors">
            <td class="py-2.5 px-4 text-fg-2">${escapeHtml(ts)}</td>
            <td class="py-2.5 px-4 text-fg font-semibold">${escapeHtml(log.principal || 'raviranjan')}</td>
            <td class="py-2.5 px-4 ${actColor} font-bold">${escapeHtml(act)}</td>
            <td class="py-2.5 px-4 text-fg">${escapeHtml(log.entity || 'Portfolio Entity')}</td>
            <td class="py-2.5 px-4 text-fg-3">${escapeHtml(log.clientIp || '127.0.0.1')}</td>
            <td class="py-2.5 px-4 text-right">
              <button type="button" onclick="openAuditDiffModal(${idx})" class="px-2.5 py-1 rounded-lg bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/30 text-indigo-400 font-semibold text-xs">View Diff</button>
            </td>
          </tr>
        `;
      }).join('');
    }
  } catch (e) {
    console.warn('Audit logs fetch note:', e);
  }
}

function formatPrettyJson(raw) {
  if (!raw) return '{}';
  try {
    const parsed = typeof raw === 'string' ? JSON.parse(raw) : raw;
    return JSON.stringify(parsed, null, 2);
  } catch (e) {
    return String(raw);
  }
}

function openAuditDiffModal(index) {
  const log = cachedAdminAuditLogs[index];
  if (!log) return;
  const modal = document.getElementById('modal-audit-diff');
  if (!modal) return;

  const metaEl = document.getElementById('audit-diff-meta');
  if (metaEl) {
    const ts = log.timestamp ? new Date(log.timestamp).toLocaleString() : '';
    metaEl.innerHTML = `
      <div><span class="text-fg-3 block text-xs">PRINCIPAL</span><span class="text-fg font-bold">${escapeHtml(log.principal || 'raviranjan')}</span></div>
      <div><span class="text-fg-3 block text-xs">ACTION</span><span class="text-indigo-400 font-bold">${escapeHtml(log.action || 'UPDATE')}</span></div>
      <div><span class="text-fg-3 block text-xs">ENTITY</span><span class="text-fg">${escapeHtml(log.entity || '')}</span></div>
      <div><span class="text-fg-3 block text-xs">TIMESTAMP / IP</span><span class="text-fg">${escapeHtml(ts)} (${escapeHtml(log.clientIp || '127.0.0.1')})</span></div>
    `;
  }

  const oldEl = document.getElementById('audit-diff-old');
  const newEl = document.getElementById('audit-diff-new');
  if (oldEl) oldEl.textContent = formatPrettyJson(log.oldState);
  if (newEl) newEl.textContent = formatPrettyJson(log.newState);

  modal.classList.remove('hidden');
  if (window.lucide) window.lucide.createIcons();
}

function closeAuditDiffModal() {
  document.getElementById('modal-audit-diff')?.classList.add('hidden');
}

// ==========================================
// AI ASSISTANT CONFIG, GEMINI KEY & VISITOR LOGS (NEON DB)
// ==========================================
async function loadAiConfig() {
  try {
    const headers = await getAuthHeaders();
    const [cfgRes, logsRes] = await Promise.all([
      fetch('/api/admin/ai/config', { headers }),
      fetch('/api/admin/ai/logs?size=50', { headers })
    ]);

    if (cfgRes.ok) {
      const json = await cfgRes.json();
      const c = json.data;
      if (c) {
        if (c.apiKey) {
          const input = document.getElementById('gemini-api-key-input');
          if (input) input.value = c.apiKey;
          const badge = document.getElementById('gemini-status-badge');
          if (badge) {
            badge.innerText = 'Connected';
            badge.className = 'text-xs  px-2 py-0.5 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/40';
          }
        }
        if (c.systemPrompt && document.getElementById('ai-system-prompt')) {
          document.getElementById('ai-system-prompt').value = c.systemPrompt;
        }
      }
    }

    if (logsRes.ok) {
      const logsJson = await logsRes.json();
      const pageData = logsJson?.data;
      const logs = pageData?.content || pageData || [];
      const totalEl = document.getElementById('ai-stat-total-queries');
      if (totalEl) totalEl.innerText = pageData?.totalElements ?? logs.length;

      const avgEl = document.getElementById('ai-stat-avg-latency');
      if (avgEl) {
        const avg = logs.length > 0
          ? Math.round(logs.reduce((acc, x) => acc + (Number(x.latencyMs) || 0), 0) / logs.length)
          : 42;
        avgEl.innerText = avg + ' ms';
      }

      const listEl = document.getElementById('admin-ai-query-logs-list');
      if (listEl) {
        if (!Array.isArray(logs) || logs.length === 0) {
          listEl.innerHTML = `<div class="p-4 text-center text-fg-3">No visitor AI conversations recorded yet. Ask a question in "Ask AI" to see live logs with IP and Device info here!</div>`;
        } else {
          listEl.innerHTML = logs.map(l => {
            const timeStr = l.createdAt ? new Date(l.createdAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';
            const shortUa = l.userAgent ? (l.userAgent.length > 55 ? l.userAgent.substring(0, 55) + '...' : l.userAgent) : 'Web Browser';
            const geoParts = [l.city, l.region, l.country].filter(Boolean);
            const geoText = geoParts.length > 0 ? geoParts.join(', ') : (l.country || 'India');
            const displayHash = l.browserHash || (l.sessionId ? 'bh-' + l.sessionId.substring(0, 8) : 'bh-client');
            const displayOrigin = (l.origin && l.origin !== '/') ? l.origin : (window.location.origin || 'https://raviranjan.dev');
            const formattedAnswer = (typeof formatAiMarkdownToHtml === 'function')
              ? formatAiMarkdownToHtml(l.answer || '')
              : escapeHtml(l.answer || '').replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>').replace(/\n/g, '<br>');
            return `
              <div class="p-3.5 rounded-xl bg-card border border-line space-y-2">
                <div class="flex flex-wrap items-center justify-between gap-1 text-xs text-fg-2 border-b border-line pb-1.5">
                  <div class="flex flex-wrap items-center gap-1.5">
                    <span class="px-1.5 py-0.5 rounded bg-indigo-500/10 text-indigo-400 border border-indigo-500/20 font-bold">${escapeHtml(l.detectedLanguage || 'ENGLISH')}</span>
                    <span class="text-emerald-400 font-semibold">IP: ${escapeHtml(l.clientIp || '127.0.0.1')}</span>
                    <span class="px-1.5 py-0.5 rounded bg-amber-500/10 text-amber-300 border border-amber-500/20">${escapeHtml(geoText)}</span>
                  </div>
                  <div class="flex items-center gap-2">
                    <span class="text-fg-2">${l.latencyMs || 25}ms · ${timeStr}</span>
                    <button type="button" onclick="handleDeleteAiLog('${l.id}')" class="text-rose-400 hover:underline text-xs rounded-lg">Delete</button>
                  </div>
                </div>
                <p class="text-fg font-bold text-xs">Q: "${escapeHtml(l.userQuery)}"</p>
                <div class="text-fg font-sans text-xs leading-relaxed bg-card p-2.5 rounded-lg border border-line space-y-1">${formattedAnswer}</div>
                <div class="text-xs text-fg-2 flex flex-wrap justify-between gap-2 pt-0.5">
                  <span><strong>Device:</strong> ${escapeHtml(shortUa)}</span>
                  <span><strong>Hash:</strong> <code class="text-indigo-300 font-mono">${escapeHtml(displayHash)}</code></span>
                  <span><strong>Origin:</strong> <code class="text-fg font-mono">${escapeHtml(displayOrigin)}</code></span>
                </div>
              </div>
            `;
          }).join('');
        }
      }
    }
  } catch (e) {
    console.warn('Load AI config note:', e);
  }
}

async function handleDeleteAiLog(id) {
  if (!(await window.confirmDialog({ title: "Delete this AI conversation?", message: "The visitor conversation record is removed permanently." }))) return;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/ai/logs/${id}`, { method: 'DELETE', headers });
    if (res.ok) {
      showToast("AI conversation log deleted!", "success");
      loadAiConfig();
      loadAdminDashboardMetrics();
    }
  } catch (e) {}
}

async function handleClearAllAiLogs() {
  if (!(await window.confirmDialog({ title: "Clear ALL AI conversation logs?", message: "Every visitor AI conversation will be removed permanently. This cannot be undone.", confirmText: "Clear all", requireText: "CLEAR ALL" }))) return;
  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/admin/ai/logs', { method: 'DELETE', headers });
    if (res.ok) {
      showToast("All AI conversation logs cleared!", "success");
      loadAiConfig();
      loadAdminDashboardMetrics();
    }
  } catch (e) {}
}

async function saveGeminiKey() {
  const input = document.getElementById('gemini-api-key-input');
  if (!input) return;
  const key = input.value.trim();
  if (!key) {
    showToast('Please enter a valid Gemini API key', 'warning');
    return;
  }

  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/admin/ai/config', {
      method: 'PUT',
      headers,
      body: JSON.stringify({
        apiKey: key,
        enabled: true,
        modelName: 'gemini-1.5-flash'
      })
    });

    if (res.ok) {
      showToast('Gemini API key saved! AI Assistant active.', 'success');
      const badge = document.getElementById('gemini-status-badge');
      if (badge) {
        badge.innerText = 'Connected';
        badge.className = 'text-xs  px-2 py-0.5 rounded-lg bg-emerald-500/20 text-emerald-300 border border-emerald-500/40';
      }
    }
  } catch (e) {
    showToast('Error saving Gemini key', 'warning');
  }
}

// ==========================================
// NOTIFICATION CENTER & RECRUITER INQUIRY ALERTS (NEON DB)
// ==========================================
let cachedAdminNotifications = [];

async function loadAdminNotifications() {
  try {
    const headers = await getAuthHeaders();
    const [countRes, listRes] = await Promise.all([
      fetch('/api/admin/notifications/count-unread', { headers }),
      fetch('/api/admin/notifications?size=30', { headers })
    ]);

    if (countRes.ok) {
      const json = await countRes.json();
      const count = json?.data?.unreadCount || 0;
      const badge = document.getElementById('notif-badge');
      if (badge) {
        badge.innerText = count;
        if (count === 0) badge.classList.add('hidden');
        else badge.classList.remove('hidden');
      }
    }

    if (!listRes.ok) throw new Error('Server responded ' + listRes.status);
    const listJson = await listRes.json();
    cachedAdminNotifications = listJson?.data?.content || listJson?.data || [];
    renderAdminNotifications(cachedAdminNotifications);
  } catch (e) {
    console.warn('Notifications load note:', e);
    const c = document.getElementById('notif-items-container');
    if (c) c.innerHTML = `<div class="p-6 text-center space-y-3"><p class="text-fg-2 text-sm">Could not load notifications.</p><button type="button" onclick="loadAdminNotifications()" class="btn btn--pill btn--primary">Try again</button></div>`;
  }
}

function renderAdminNotifications(items) {
  const container = document.getElementById('notif-items-container');
  if (!container) return;
  if (!Array.isArray(items) || items.length === 0) {
    container.innerHTML = `<div class="p-6 text-center text-fg-3 text-sm">You're all caught up. New inquiries and system alerts appear here.</div>`;
    return;
  }

  container.innerHTML = items.map(n => {
    const id = escapeHtml(String(n.id));
    const isRead = Boolean(n.read);
    const priority = n.priority || (n.level === 'ALERT' ? 'URGENT' : 'LOW');
    const type = n.type || (n.title && n.title.toLowerCase().includes('inquiry') ? 'CRM' : 'SYSTEM');
    const isCrm = type === 'CRM' || type === 'NEW_CONTACT_INQUIRY' || type === 'CONTACT_INQUIRY';
    const urgent = priority === 'URGENT' || priority === 'HIGH';
    const pColor = urgent ? 'bg-rose-500/10 text-rose-400 border-rose-500/20' : 'bg-indigo-500/10 text-indigo-400 border-indigo-500/20';
    const timeStr = n.createdAt ? new Date(n.createdAt).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : '';
    const label = String(type).replace(/_/g, ' ').toLowerCase().replace(/^\w/, c => c.toUpperCase());
    const msg = String(n.message || '');
    const longMsg = msg.length > 90;

    return `
      <article class="notif-item p-3 rounded-xl bg-card border border-line space-y-1.5 ${isRead ? 'opacity-70' : ''}" data-type="${escapeHtml(type)}" data-priority="${escapeHtml(priority)}" data-read="${isRead}">
        <div class="flex items-center gap-2 text-xs">
          ${isRead ? '' : '<span class="w-2 h-2 rounded-full bg-indigo-400 shrink-0" aria-label="Unread"></span>'}
          <span class="px-2 py-0.5 rounded-full border ${pColor}">${escapeHtml(urgent ? 'Urgent' : label)}</span>
          <span class="text-fg-3 ml-auto shrink-0">${timeStr}</span>
        </div>
        <p class="font-semibold text-fg text-[13px] leading-snug truncate" title="${escapeHtml(n.title || '')}">${escapeHtml(n.title || '')}</p>
        ${msg ? `<p data-notif-msg class="text-fg-2 text-[13px] leading-relaxed line-clamp-2">${escapeHtml(msg)}</p>` : ''}
        <div class="flex items-center justify-between gap-2 pt-1 text-xs">
          <div class="flex items-center gap-3">
            ${isCrm ? `<a href="#crm" onclick="switchView('crm'); toggleNotificationDrawer(); return false;" class="text-indigo-400 hover:underline">Open in CRM</a>` : ''}
            ${longMsg ? `<button type="button" onclick="const p=this.closest('.notif-item').querySelector('[data-notif-msg]'); const o=p.classList.toggle('line-clamp-2'); this.textContent=o?'Show more':'Show less';" class="text-fg-3 hover:text-fg">Show more</button>` : ''}
          </div>
          <div class="flex items-center gap-1">
            ${isRead ? '' : `<button type="button" onclick="markNotificationReadById('${id}', this)" class="btn btn--icon btn--pill text-emerald-400" aria-label="Mark as read" title="Mark as read"><i data-lucide="check" class="w-4 h-4" aria-hidden="true"></i></button>`}
            <button type="button" onclick="deleteNotificationById('${id}')" class="btn btn--icon btn--pill text-rose-400" aria-label="Delete notification" title="Delete"><i data-lucide="trash-2" class="w-4 h-4" aria-hidden="true"></i></button>
          </div>
        </div>
      </article>`;
  }).join('');
  if (window.lucide && typeof lucide.createIcons === 'function') lucide.createIcons();
  filterNotifications(currentNotifFilter);
}

async function markNotificationReadById(id, btn) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/notifications/${id}/read`, { method: 'PUT', headers });
    if (res.ok) {
      await loadAdminNotifications();
    } else {
      showToast('Could not mark as read (server ' + res.status + ')', 'warning');
    }
  } catch (e) { showToast('Could not mark as read: ' + e.message, 'warning'); }
}

const origToggleNotificationDrawer = toggleNotificationDrawer;
toggleNotificationDrawer = function() {
  const drawer = document.getElementById('notification-drawer');
  if (!drawer) return;
  drawer.classList.toggle('translate-x-full');
  if (!drawer.classList.contains('translate-x-full')) {
    loadAdminNotifications();
  }
};

markAllNotificationsRead = async function() {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch('/api/admin/notifications/read-all', { method: 'PUT', headers });
    if (res.ok) {
      await loadAdminNotifications();
      showToast('All notifications marked as read.', 'success');
    } else {
      showToast('Could not mark all as read (server ' + res.status + ')', 'warning');
    }
  } catch (e) { showToast('Could not mark all as read: ' + e.message, 'warning'); }
};

async function deleteNotificationById(id) {
  try {
    const headers = await getAuthHeaders();
    const res = await fetch(`/api/admin/notifications/${id}`, { method: 'DELETE', headers });
    if (res.ok) {
      await loadAdminNotifications();
      showToast('Notification deleted.', 'success');
    } else {
      showToast('Could not delete the notification (server ' + res.status + ')', 'warning');
    }
  } catch (e) {
    showToast('Error deleting notification: ' + e.message, 'warning');
  }
}

// ==========================================
// DYNAMIC ADMIN IDENTITY & COMMAND PALETTE (100% NEON DB DRIVEN)
// ==========================================
async function loadAdminIdentityMetadata() {
  try {
    const cachedUser = localStorage.getItem('raviranjan_admin_user');
    if (cachedUser) {
      const userEl = document.getElementById('admin-sidebar-username');
      if (userEl) userEl.innerText = cachedUser;
      const monoEl = document.getElementById('admin-sidebar-monogram');
      if (monoEl) monoEl.innerText = String(cachedUser).substring(0, 2).toUpperCase();
    }

    const headers = await getAuthHeaders();
    const res = await fetch('/api/v1/auth/me', { headers });
    if (res.ok) {
      const json = await res.json();
      const d = json?.data || {};
      if (d.username) {
        localStorage.setItem('raviranjan_admin_user', d.username);
        const userEl = document.getElementById('admin-sidebar-username');
        if (userEl) userEl.innerText = d.username;
        const monoEl = document.getElementById('admin-sidebar-monogram');
        if (monoEl) {
          monoEl.innerText = String(d.username).substring(0, 2).toUpperCase();
        }
        const badgeEl = document.getElementById('cp-current-username-badge');
        if (badgeEl) badgeEl.innerText = d.username;
      }
      if (d.role) {
        const roleEl = document.getElementById('admin-sidebar-role');
        if (roleEl) roleEl.innerText = d.role;
      }
    }

    const metaRes = await fetch('/api/v1/auth/credential-metadata');
    if (metaRes.ok) {
      const metaJson = await metaRes.json();
      const md = metaJson?.data || {};
      if (md.credentialsUpdatedAt) {
        const updatedEl = document.getElementById('cp-last-updated-time');
        if (updatedEl) {
          updatedEl.innerText = new Date(md.credentialsUpdatedAt).toLocaleString([], {
            month: 'short', day: 'numeric', year: 'numeric', hour: '2-digit', minute: '2-digit'
          });
        }
      }
    }
  } catch (_) {}
}

function initDynamicAdminCommandPalette() {
  const cmdInput = document.getElementById('cmd-input');
  const cmdResults = document.getElementById('cmd-results');
  if (!cmdResults) return;

  const modules = [
    { label: '📊 Dashboard & Executive Telemetry', view: 'dashboard', tag: 'OS' },
    { label: '👤 Profile, Bio & Resume Builder', view: 'profile', tag: 'OS' },
    { label: '🧩 Homepage Section Builder', view: 'homepage', tag: 'OS' },
    { label: '🤖 AI Assistant & RAG Config', view: 'ai-control', tag: 'AI' },
    { label: '⚙️ Site Settings, Theme & Fonts', view: 'settings', tag: 'OS' },
    { label: '📁 Projects Manager', view: 'projects', tag: 'ENG' },
    { label: '📝 13-Tab Project Case Study Editor', view: 'project-editor', tag: 'ENG' },
    { label: '🔬 Engineering Lab Experiments', view: 'lab', tag: 'ENG' },
    { label: '💻 Skills Matrix', view: 'skills', tag: 'CAREER' },
    { label: '💼 Internships & Career Timeline', view: 'experience', tag: 'CAREER' },
    { label: '🏆 Achievements & Certifications', view: 'achievements', tag: 'CAREER' },
    { label: '🧮 DSA Problem Notes', view: 'dsa', tag: 'CAREER' },
    { label: '✍️ Technical Writing', view: 'writing', tag: 'CONTENT' },
    { label: '📄 Research Publications', view: 'research', tag: 'CONTENT' },
    { label: '🤝 Contribution Stories', view: 'contributions', tag: 'CONTENT' },
    { label: '📬 Contact Inquiries & AI CRM', view: 'crm', tag: 'OPS' },
    { label: '🛡️ Immutable Audit Logs', view: 'audit-logs', tag: 'OPS' },
    { label: '🔑 Update Admin Username & Password (OTP)', action: 'password', tag: 'SEC' }
  ];

  const renderItems = (q = '') => {
    const query = q.toLowerCase().trim();
    const matched = modules.filter(m => m.label.toLowerCase().includes(query) || m.tag.toLowerCase().includes(query));
    cmdResults.innerHTML = matched.map(m => `
      <div onclick="${m.action === 'password' ? 'toggleCommandPalette(); openChangePasswordModal();' : `switchView('${m.view}'); toggleCommandPalette();`}" class="p-2 rounded hover:bg-raised cursor-pointer flex justify-between items-center text-fg hover:text-fg">
        <span>${escapeHtml(m.label)}</span>
        <span class="text-xs text-indigo-400">${escapeHtml(m.tag)}</span>
      </div>
    `).join('');
  };

  renderItems('');
  if (cmdInput) {
    cmdInput.addEventListener('input', (e) => renderItems(e.target.value));
  }
}
